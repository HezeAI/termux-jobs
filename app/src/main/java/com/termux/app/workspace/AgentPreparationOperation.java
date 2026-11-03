package com.termux.app.workspace;

import android.app.*;
import android.content.Context;
import android.os.*;
import android.widget.*;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.*;

/** Shared, serialized preparation. Closing its view never cancels a package transaction. */
final class AgentPreparationOperation {
    interface Worker {AgentPreparation.Plan run(Context context,WorkspaceStore.Session session,Consumer<String> output)throws Exception;}
    static final Map<String,Job> jobs=new HashMap<>();
    private static final ExecutorService queue=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());
    static final Pattern IMAGE=Pattern.compile("IMAGE_PROGRESS: ([0-9]+)% \\(([0-9]+)/([0-9]+) bytes\\)");
    static String key(WorkspaceStore.Session p){return AgentEnvironments.alias(p)+"|"+AgentPreparation.agent(p);}
    static Job start(Context c,WorkspaceStore.Session p,Worker worker){return start(c,p,AgentPreparation.SYSTEM,worker);}
    static synchronized Job start(Context c,WorkspaceStore.Session p,AgentPreparation.Runner checker,Worker worker){
        String key=key(p);Job old=jobs.get(key);if(old!=null&&old.active)return old;
        Job job=new Job(c.getApplicationContext(),p.copy(),checker,worker);jobs.put(key,job);queue.execute(job);return job;
    }
    static final class Job implements Runnable {
        final Context context;final WorkspaceStore.Session session;final Worker worker;final AgentPreparation.Runner checker;
        final List<Runnable> observers=new ArrayList<>();
        volatile boolean active=true;volatile String stage="queued",item="",log="",error="";
        volatile int percent=-1;volatile long received,total;volatile AgentPreparation.Plan inventory,result;
        private long notificationTime,lastLogTime;
        Job(Context c,WorkspaceStore.Session p,AgentPreparation.Runner check,Worker w){context=c;checker=check;session=p;session.agentSetupAllowed=true;worker=w;}
        void output(String raw){
            String clean=EnvironmentInstallLog.clean(raw).trim();if(clean.isEmpty())return;
            String next=log+clean+"\n";log=next.length()>65536?next.substring(next.length()-65536):next;
            if(clean.startsWith("PREPARATION:")){stage=clean.substring(12);percent=-1;received=total=0;item="";}
            else if(clean.startsWith("STAGE:")){stage=clean;percent=-1;received=total=0;item="";}
            else {Matcher m=IMAGE.matcher(clean);if(m.matches())try{long r=Long.parseLong(m.group(2)),t=Long.parseLong(m.group(3));if(t>0&&r>=0&&r<=t){received=r;total=t;percent=(int)(100*r/t);stage="download";}}catch(NumberFormatException ignored){}else item=clean.length()>240?clean.substring(0,240):clean;}
            long now=SystemClock.elapsedRealtime();if(now-lastLogTime>2000){lastLogTime=now;saveLog();}if(now-notificationTime>200){notificationTime=now;changed();}
        }
        void changed(){main.post(()->{for(Runnable r:new ArrayList<>(observers))r.run();});}
        public void run(){
            try{output("PREPARATION:checking-ubuntu");inventory=checker.run(context,session);changed();
                if(!inventory.error.isEmpty())throw new IOException("EXISTING_ENVIRONMENT_CHECK_FAILED");
                result=worker.run(context,session,this::output);
                if(!result.ready())throw new IOException("COMPONENT_CHECK_FAILED");
                stage="complete";
            }catch(Exception failure){stage="failed";String detail=EnvironmentInstallProgress.failureLine(log);error=detail.isEmpty()?(failure instanceof IOException?failure.getMessage():failure.getClass().getSimpleName()):detail;}
            finally{active=false;saveLog();changed();}
        }
        private void saveLog(){try{File dir=new File(AgentPreparation.directory(context.getFilesDir()),"operations");if(!dir.isDirectory()&&!dir.mkdirs())return;File file=new File(dir,AgentEnvironments.alias(session)+"-"+AgentPreparation.agent(session)+".log");File temp=File.createTempFile("preparation-",".tmp",dir);try{try(OutputStream out=new FileOutputStream(temp)){out.write((log+(error.isEmpty()?"":"ERROR:"+error+"\n")).getBytes(StandardCharsets.UTF_8));}java.nio.file.Files.move(temp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}finally{temp.delete();}}catch(IOException ignored){}}
    }
    static AgentPreparation.Plan prepare(Context context,WorkspaceStore.Session p,Consumer<String> output)throws Exception{
        AgentPreparation.prepare(context);
        String source=SessionShell.quote(new File(AgentPreparation.directory(context.getFilesDir()),"agent-host-setup.sh").getAbsolutePath());
        String guest="set -eE; export JOBS_AGENT_UBUNTU="+SessionShell.quote(AgentEnvironments.alias(p))+" JOBS_AGENT_SETUP_ALLOWED=1 JOBS_AGENT_COMPONENTS_ONLY=1 JOBS_AGENT_LANGUAGE="+AgentPreparation.language(context)+"; source "+SessionShell.quote(AgentPreparation.GUEST+"/agent-dependencies.sh")+"; trap 'jobs_agent_stage_failure' ERR; jobs_agent_prepare "+SessionShell.quote(AgentPreparation.agent(p))+" "+SessionShell.quote(p.geminiAgent?p.geminiAuth:"api")+"; ";
        switch(AgentPreparation.agent(p)){
            case "claude":guest+="source "+SessionShell.quote(AgentPreparation.GUEST+"/claude-bootstrap.sh")+"; ";break;
            case "gemini":guest+="source "+SessionShell.quote(AgentPreparation.GUEST+"/gemini-bootstrap.sh")+"; ";if(p.geminiAuth.equals("vertex"))guest+="export JOBS_AGENT_COMPONENTS_ONLY=1; source "+SessionShell.quote(AgentPreparation.GUEST+"/vertex-bootstrap.sh")+"; ";break;
            case "opencode":guest+="source "+SessionShell.quote(AgentPreparation.GUEST+"/ai-agents-bootstrap.sh")+"; ";break;
            default:break;
        }
        String script="set -e; export JOBS_AGENT_UBUNTU="+SessionShell.quote(AgentEnvironments.alias(p))+" JOBS_AGENT_SETUP_ALLOWED=1 JOBS_AGENT_LANGUAGE="+AgentPreparation.language(context)+"; source "+source+"; jobs_agent_host_prepare; exec proot-distro login --bind "+SessionShell.quote(AgentPreparation.directory(context.getFilesDir()).getAbsolutePath()+":"+AgentPreparation.GUEST)+" "+SessionShell.quote(AgentEnvironments.alias(p))+" -- /bin/bash --noprofile --norc -c "+SessionShell.quote(guest);
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",script);
        builder.environment().clear();for(String e:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int n=e.indexOf('=');if(n>0)builder.environment().put(e.substring(0,n),e.substring(n+1));}
        builder.environment().put("PD_FORCE_NO_COLORS","1");builder.redirectErrorStream(true);
        builder.redirectInput(new File("/dev/null"));
        java.lang.Process child=builder.start();
        IOException readFailure=null;int exit=0;
        try(Reader reader=new InputStreamReader(child.getInputStream(),StandardCharsets.UTF_8)){
            char[] chars=new char[2048];StringBuilder line=new StringBuilder();int count;
            while((count=reader.read(chars))!=-1)for(int i=0;i<count;i++){char ch=chars[i];if(ch=='\n'||ch=='\r'){if(line.length()>0){output.accept(line.toString());line.setLength(0);}}else if(line.length()<8192)line.append(ch);}
            if(line.length()>0)output.accept(line.toString());
        }catch(IOException failure){readFailure=failure;output.accept("ERROR:PREPARATION_LOG_READ_FAILED");}
        finally{
            // Keep the job active until its own child exits, including reader failures.
            boolean interrupted=false;
            for(;;)try{exit=child.waitFor();break;}catch(InterruptedException stop){interrupted=true;}
            if(interrupted)Thread.currentThread().interrupt();
        }
        if(readFailure!=null)throw readFailure;
        if(exit!=0)throw new IOException("PREPARATION_EXIT_"+exit);
        return AgentPreparation.SYSTEM.run(context,p);
    }
    static String title(Context c,String ru,String en){return AgentPreparation.tr(c,ru,en);}
    static String stage(Job job,Context c){
        boolean ru=AgentPreparation.language(c).equals("ru");
        if(job.stage.equals("queued"))return title(c,"Ожидание другой операции подготовки…","Waiting for another preparation operation…");
        if(job.stage.equals("complete")&&job.result!=null&&job.result.authNeeded)return title(c,"Компоненты готовы. Авторизация: требуется отдельный вход Google Cloud при запуске.","Components ready. Authorization: Google Cloud sign-in is required separately on launch.");
        if(job.stage.equals("complete"))return title(c,"Компоненты готовы. Авторизация выполняется отдельно при запуске агента.","Components ready. Sign-in is handled separately when starting the agent.");
        if(job.stage.equals("failed"))return title(c,"Подготовка не завершена: ","Preparation failed: ")+job.error;
        if(job.stage.equals("download"))return title(c,"Скачивание базового образа Ubuntu AI","Downloading Ubuntu AI base image");
        if(job.stage.startsWith("STAGE:")){EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.line=job.stage;return p.displayLine(ru);}
        return AgentPreparation.stageText(job.stage,ru);
    }
    static void show(Activity a,WorkspaceStore.Session p,Consumer<AgentPreparation.Plan> completion){
        if(!AgentPreparation.supports(p)){WorkspaceDialogs.builder(a).setTitle(title(a,"Проверка среды запуска","Launch environment check")).setMessage(title(a,"Подготовка доступна для выбранной Ubuntu. Для Termux используйте настройки его компонентов.","Preparation is available for a selected Ubuntu environment. For Termux, use its component settings.")).setPositiveButton(title(a,"Закрыть","Close"),null).show();return;}
        Job job=start(a,p,AgentPreparationOperation::prepare);
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);int gap=WorkspaceDialogs.dp(a,12);body.setPadding(gap,gap,gap,gap);
        TextView found=new TextView(a),phase=new TextView(a),item=new TextView(a),amount=new TextView(a),details=new TextView(a),log=new TextView(a);
        for(TextView v:new TextView[]{phase,item,amount,found,details,log}){v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(14);body.addView(v);}
        ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);body.addView(progress,1,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(a,12)));progress.setMax(100);
        details.setText(title(a,"Подробности ▾","Details ▾"));details.setPadding(0,gap,0,gap);log.setTextIsSelectable(true);log.setVisibility(android.view.View.GONE);details.setOnClickListener(v->{log.setText(job.log);log.setVisibility(log.getVisibility()==android.view.View.GONE?android.view.View.VISIBLE:android.view.View.GONE);});
        ScrollView scroll=new ScrollView(a);scroll.addView(body);
        AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(title(a,"Проверка среды запуска","Launch environment check")).setView(scroll).setNegativeButton(title(a,"Закрыть","Close"),null).create();
        boolean[] delivered={false};Runnable update=()->{if(a.isFinishing()||a.isDestroyed())return;
            found.setText(job.inventory==null?title(a,"Проверка выбранной среды…","Checking the selected environment…"):AgentPreparation.summary(a,job.result==null?job.inventory:job.result));phase.setText(stage(job,a));item.setText(job.item);item.setVisibility(job.item.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
            progress.setIndeterminate(job.percent<0);progress.setProgress(Math.max(0,job.percent));progress.setVisibility(job.active?android.view.View.VISIBLE:android.view.View.GONE);
            amount.setText(job.total>0?String.format(Locale.ROOT,title(a,"%d / %d байт · %d%%","%d / %d bytes · %d%%"),job.received,job.total,job.percent):"");if(log.getVisibility()==android.view.View.VISIBLE)log.setText(job.log);
            if(!job.active&&job.result!=null&&job.result.ready()&&!delivered[0]&&dialog.isShowing()){delivered[0]=true;if(completion!=null)completion.accept(job.result);}
        };
        job.observers.add(update);dialog.setOnDismissListener(d->job.observers.remove(update));dialog.show();update.run();
    }
}
