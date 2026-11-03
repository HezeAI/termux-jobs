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
        volatile boolean active=true;volatile String stage="queued",failedStage="",component="",error="";
        volatile Integer exitCode;
        volatile int percent=-1;volatile long received,total;volatile AgentPreparation.Plan inventory,result;
        private long notificationTime;
        Job(Context c,WorkspaceStore.Session p,AgentPreparation.Runner check,Worker w){context=c;checker=check;session=p;session.agentSetupAllowed=true;worker=w;}
        void output(String raw){
            if(raw.length()>256)return;
            String nextStage=AgentPreparationOutput.stage(raw),nextError=AgentPreparationOutput.error(raw);
            if(!nextStage.isEmpty()){stage=nextStage;percent=-1;received=total=0;component="";}
            else if(!nextError.isEmpty())error=nextError;
            else if(raw.startsWith("READY:")||raw.startsWith("MISSING:")||raw.startsWith("COMPONENT:")){
                String id=raw.substring(raw.indexOf(':')+1);if(AgentPreparation.COMPONENTS.contains(id))component=id;
            }else {
                Matcher m=IMAGE.matcher(raw);if(m.matches())try{
                    long r=Long.parseLong(m.group(2)),t=Long.parseLong(m.group(3));
                    if(t>0&&t<=2147483648L&&r>=0&&r<=t){received=r;total=t;percent=(int)(100*r/t);stage="download";}
                }catch(NumberFormatException ignored){}
            }
            long now=SystemClock.elapsedRealtime();if(now-notificationTime>200){notificationTime=now;changed();}
        }
        void changed(){main.post(()->{for(Runnable r:new ArrayList<>(observers))r.run();});}
        public void run(){
            try{output("PREPARATION:checking-ubuntu");inventory=checker.run(context,session);changed();
                if(!inventory.error.isEmpty())throw new PreparationFailure("EXISTING_ENVIRONMENT_CHECK_FAILED",null);
                result=worker.run(context,session,this::output);
                if(!result.ready())throw new PreparationFailure("COMPONENT_CHECK_FAILED",null);
                exitCode=0;stage="complete";error="";
            }catch(Exception failure){
                failedStage=stage;stage="failed";
                if(failure instanceof PreparationFailure){PreparationFailure f=(PreparationFailure)failure;exitCode=f.exitCode;if(error.isEmpty())error=f.code;}
                if(error.isEmpty())error="PREPARATION_FAILED";
            }finally{active=false;changed();}
        }
    }
    static final class PreparationFailure extends IOException {
        final String code;final Integer exitCode;
        PreparationFailure(String code,Integer exitCode){super(code);this.code=code;this.exitCode=exitCode;}
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
            AgentPreparationOutput.drain(reader,output);
        }catch(IOException failure){readFailure=failure;}
        finally{
            // Keep the job active until its own child exits, including reader failures.
            boolean interrupted=false;
            for(;;)try{exit=child.waitFor();break;}catch(InterruptedException stop){interrupted=true;}
            if(interrupted)Thread.currentThread().interrupt();
        }
        if(readFailure!=null)throw new PreparationFailure("PREPARATION_OUTPUT_READ_FAILED",exit);
        if(exit!=0)throw new PreparationFailure(exit==130?"INSTALL_CANCELLED":"PREPARATION_EXIT_FAILED",exit);
        return AgentPreparation.SYSTEM.run(context,p);
    }
    static String title(Context c,String ru,String en){return AgentPreparation.tr(c,ru,en);}
    static String stage(Job job,Context c){
        boolean ru=AgentPreparation.language(c).equals("ru");
        if(job.stage.equals("queued"))return title(c,"Ожидание другой операции подготовки…","Waiting for another preparation operation…");
        if(job.stage.equals("complete")&&job.result!=null&&job.result.authNeeded)return title(c,"Компоненты готовы. Авторизация: требуется отдельный вход Google Cloud при запуске.","Components ready. Authorization: Google Cloud sign-in is required separately on launch.");
        if(job.stage.equals("complete"))return title(c,"Компоненты готовы. Авторизация выполняется отдельно при запуске агента.","Components ready. Sign-in is handled separately when starting the agent.");
        if(job.stage.equals("failed")){String phase=phaseText(job.failedStage,c);return (phase.isEmpty()?"":phase+"\n")+AgentPreparationOutput.description(job.error,ru)+(job.exitCode==null?"":" ("+job.exitCode+")");}
        return phaseText(job.stage,c);
    }
    private static String phaseText(String stage,Context c){
        boolean ru=AgentPreparation.language(c).equals("ru");
        if(stage.equals("download")||stage.equals("STAGE:download-pinned-ubuntu-ai")||stage.equals("STAGE:download-pinned-ubuntu"))return title(c,"Скачивание базового образа Ubuntu AI","Downloading Ubuntu AI base image");
        if(stage.startsWith("STAGE:")){EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.line=stage;return p.displayLine(ru);}
        switch(stage){
            case "STAGE:download-agent-component":return title(c,"Загрузка компонента агента","Downloading agent component");
            case "STAGE:retry-agent-component":return title(c,"Повтор загрузки компонента","Retrying component download");
            case "failed-ubuntu":return title(c,"Этап: подготовка Ubuntu","Stage: Ubuntu preparation");
            case "failed-components":return title(c,"Этап: компоненты агента","Stage: agent components");
            case "failed-cli":return title(c,"Этап: проверка CLI","Stage: CLI startup check");
            default:return AgentPreparation.stageText(stage,ru);
        }
    }
    static void show(Activity a,WorkspaceStore.Session p,Consumer<AgentPreparation.Plan> completion){
        if(!AgentPreparation.supports(p)){WorkspaceDialogs.builder(a).setTitle(title(a,"Проверка среды запуска","Launch environment check")).setMessage(title(a,"Подготовка доступна для выбранной Ubuntu. Для Termux используйте настройки его компонентов.","Preparation is available for a selected Ubuntu environment. For Termux, use its component settings.")).setPositiveButton(title(a,"Закрыть","Close"),null).show();return;}
        Job job=start(a,p,AgentPreparationOperation::prepare);
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);int gap=WorkspaceDialogs.dp(a,12);body.setPadding(gap,gap,gap,gap);
        TextView found=new TextView(a),phase=new TextView(a),item=new TextView(a),amount=new TextView(a);
        for(TextView v:new TextView[]{phase,item,amount,found}){v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(14);body.addView(v);}
        ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);body.addView(progress,1,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(a,12)));progress.setMax(100);
        ScrollView scroll=new ScrollView(a);scroll.addView(body);
        AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(title(a,"Проверка среды запуска","Launch environment check")).setView(scroll).setNegativeButton(title(a,"Закрыть","Close"),null).setPositiveButton(title(a,"Повторить проверку","Retry check"),null).create();
        boolean[] delivered={false};Runnable update=()->{if(a.isFinishing()||a.isDestroyed())return;
            found.setText(job.inventory==null?title(a,"Проверка выбранной среды…","Checking the selected environment…"):AgentPreparation.summary(a,job.result==null?job.inventory:job.result));phase.setText(stage(job,a));item.setText(job.component);item.setVisibility(job.component.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
            progress.setIndeterminate(job.percent<0);progress.setProgress(Math.max(0,job.percent));progress.setVisibility(job.active?android.view.View.VISIBLE:android.view.View.GONE);
            amount.setText(job.total>0?String.format(Locale.ROOT,title(a,"%d / %d байт · %d%%","%d / %d bytes · %d%%"),job.received,job.total,job.percent):"");
            if(dialog.getButton(-1)!=null){dialog.getButton(-1).setEnabled(!job.active);dialog.getButton(-1).setVisibility(job.stage.equals("failed")?android.view.View.VISIBLE:android.view.View.GONE);}
            if(!job.active&&job.result!=null&&job.result.ready()&&!delivered[0]&&dialog.isShowing()){delivered[0]=true;if(completion!=null)completion.accept(job.result);}
        };
        job.observers.add(update);dialog.setOnDismissListener(d->job.observers.remove(update));dialog.show();dialog.getButton(-1).setOnClickListener(v->{if(job.active)return;dialog.dismiss();show(a,job.session,completion);});update.run();
    }
}
