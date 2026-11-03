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
    static final List<Runnable> listeners=new ArrayList<>();
    static synchronized Job current(WorkspaceStore.Session p){return jobs.get(key(p));}
    private static final ExecutorService queue=Executors.newSingleThreadExecutor();
    private static final Handler main=new Handler(Looper.getMainLooper());
    static final Pattern IMAGE=Pattern.compile("IMAGE_PROGRESS: ([0-9]+)% \\(([0-9]+)/([0-9]+) bytes\\)");
    static String key(WorkspaceStore.Session p){return AgentEnvironments.alias(p)+"|"+AgentPreparation.agent(p)+(p.geminiAgent?"|"+p.geminiAuth:"");}
    static Job start(Context c,WorkspaceStore.Session p,Worker worker){return start(c,p,AgentPreparation.SYSTEM,worker);}
    static synchronized Job start(Context c,WorkspaceStore.Session p,AgentPreparation.Runner checker,Worker worker){
        String key=key(p);Job old=jobs.get(key);if(old!=null&&old.active)return old;
        Job job=new Job(c.getApplicationContext(),p.copy(),checker,worker);jobs.put(key,job);queue.execute(job);job.changed();return job;
    }
    /** Counts plan milestones, never transfer bytes or remaining time. */
    static final class PlanProgress {
        final List<String> phases=new ArrayList<>();
        volatile int completed;
        PlanProgress(AgentPreparation.Plan inventory){
            phases.add("check");
            boolean ubuntu=inventory.missing.contains("ubuntu");
            if(ubuntu||inventory.missing.contains("proot-distro"))phases.add("termux");
            if(ubuntu){phases.add("image");phases.add("extract");}
            if(!inventory.missing.isEmpty())phases.add("components");
            phases.add("verify");completed=inventory.error.isEmpty()?1:0;
        }
        int total(){return phases.size();}
        void event(String stage){
            String phase;
            switch(stage){
                case "preparing-termux-packages":case "STAGE:install-python":case "STAGE:install-proot-distro":case "STAGE:prepare-operation-tools":case "installing-ubuntu":case "STAGE:install-environment":phase="termux";break;
                case "download":case "STAGE:download-pinned-ubuntu-ai":case "STAGE:download-pinned-ubuntu":case "STAGE:ubuntu-ai-installer-5.9.0":case "STAGE:checking-image-cache":case "STAGE:connecting-image-download":case "STAGE:reconnecting-image-download":case "STAGE:verifying-image-download":case "STAGE:verified-rootfs-sha256":phase="image";break;
                case "STAGE:extracting-environment":case "STAGE:verify":phase="extract";break;
                case "checking-components":case "installing-components":case "checking-cli":case "ready":case "STAGE:download-agent-component":case "STAGE:retry-agent-component":phase=phases.contains("components")?"components":"verify";break;
                case "verifying-components":phase="verify";break;
                default:return;
            }
            int index=phases.indexOf(phase);if(index>=0)completed=Math.max(completed,index);
        }
        void finish(){completed=total();}
    }
    static String overall(Job job,Context c){
        PlanProgress plan=job.plan;
        return plan==null?title(c,"План уточняется после проверки…","Planning after the initial check…"):
            String.format(Locale.ROOT,title(c,"Выполнено %d из %d этапов","Completed %d of %d stages"),plan.completed,plan.total());
    }
    static final class Job implements Runnable {
        final Context context;final WorkspaceStore.Session session;final Worker worker;final AgentPreparation.Runner checker;
        final List<Runnable> observers=new ArrayList<>();
        volatile boolean active=true;volatile String stage="queued",failedStage="",component="",error="";
        volatile Integer exitCode;
        volatile int percent=-1;volatile long received,total;volatile AgentPreparation.Plan inventory,result;volatile PlanProgress plan;
        private long notificationTime;
        Job(Context c,WorkspaceStore.Session p,AgentPreparation.Runner check,Worker w){context=c;checker=check;session=p;session.agentSetupAllowed=true;worker=w;}
        void output(String raw){
            if(raw.length()>256)return;
            String nextStage=AgentPreparationOutput.stage(raw),nextError=AgentPreparationOutput.error(raw);
            boolean transition=!nextStage.isEmpty()&&!nextStage.equals(stage);
            if(!nextStage.isEmpty()){
                if(nextStage.startsWith("failed-")){if(failedStage.isEmpty())failedStage=stage;}
                else failedStage="";
                stage=nextStage;percent=-1;received=total=0;component="";
            }
            else if(!nextError.isEmpty())error=nextError;
            else if(raw.startsWith("READY:")||raw.startsWith("MISSING:")||raw.startsWith("COMPONENT:")){
                String id=raw.substring(raw.indexOf(':')+1);if(AgentPreparation.COMPONENTS.contains(id))component=id;
            }else {
                Matcher m=IMAGE.matcher(raw);if(m.matches())try{
                    long r=Long.parseLong(m.group(2)),t=Long.parseLong(m.group(3));
                    if(t>0&&t<=2147483648L&&r>=0&&r<=t){received=r;total=t;percent=(int)(100*r/t);stage="download";}
                }catch(NumberFormatException ignored){}
            }
            if(plan!=null)plan.event(stage);
            long now=SystemClock.elapsedRealtime();if(transition||!nextError.isEmpty()||now-notificationTime>200){notificationTime=now;changed();}
        }
        void changed(){main.post(()->{for(Runnable r:new ArrayList<>(observers))r.run();for(Runnable r:new ArrayList<>(listeners))r.run();});}
        public void run(){
            try{output("PREPARATION:checking-ubuntu");inventory=checker.run(context,session);plan=new PlanProgress(inventory);changed();
                if(!inventory.error.isEmpty())throw new PreparationFailure("EXISTING_ENVIRONMENT_CHECK_FAILED",null);
                result=worker.run(context,session,this::output);
                if(!result.ready())throw new PreparationFailure("COMPONENT_CHECK_FAILED",null);
                exitCode=0;stage="complete";error="";plan.finish();
            }catch(Exception failure){
                if(failedStage.isEmpty())failedStage=stage;stage="failed";
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
        output.accept("PREPARATION:verifying-components");
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
        switch(stage){
            case "ready":return title(c,"Завершение подготовки компонентов…","Finishing component preparation…");
            case "STAGE:download-agent-component":return title(c,"Загрузка компонента агента","Downloading agent component");
            case "STAGE:retry-agent-component":return title(c,"Повтор загрузки компонента","Retrying component download");
            case "failed-ubuntu":return title(c,"Этап: подготовка Ubuntu","Stage: Ubuntu preparation");
            case "failed-components":return title(c,"Этап: компоненты агента","Stage: agent components");
            case "failed-cli":return title(c,"Этап: проверка CLI","Stage: CLI startup check");
            default:
                if(stage.equals("STAGE:install-proot-distro")||stage.equals("STAGE:install-python")||stage.equals("STAGE:prepare-operation-tools"))return title(c,"Подготовка пакетов Termux…","Preparing Termux packages…");
                if(stage.equals("STAGE:verify"))return title(c,"Проверка установленной Ubuntu AI…","Checking installed Ubuntu AI…");
                if(stage.startsWith("STAGE:")){EnvironmentInstallProgress p=new EnvironmentInstallProgress();p.line=stage;return p.displayLine(ru);}
                return AgentPreparation.stageText(stage,ru);
        }
    }
    static void show(Activity a,WorkspaceStore.Session p,Consumer<AgentPreparation.Plan> completion){
        if(!AgentPreparation.supports(p)){WorkspaceDialogs.builder(a).setTitle(title(a,"Проверка среды запуска","Launch environment check")).setMessage(title(a,"Подготовка доступна для выбранной Ubuntu. Для Termux используйте настройки его компонентов.","Preparation is available for a selected Ubuntu environment. For Termux, use its component settings.")).setPositiveButton(title(a,"Закрыть","Close"),null).show();return;}
        Job existing=current(p);Job job=existing==null?start(a,p,AgentPreparationOperation::prepare):existing;
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);int gap=WorkspaceDialogs.dp(a,12);body.setPadding(gap,gap,gap,gap);
        TextView overallText=new TextView(a);overallText.setTextColor(WorkspaceDialogs.TEXT);overallText.setTextSize(14);overallText.setTag("preparation-overall-count");body.addView(overallText);
        ProgressBar overallBar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);overallBar.setTag("preparation-overall-progress");body.addView(overallBar,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(a,10)));
        TextView explanation=new TextView(a);explanation.setText(title(a,"Этапы плана, не оценка оставшегося времени.","Plan stages, not an estimate of remaining time."));explanation.setTextColor(WorkspaceDialogs.MUTED);explanation.setTextSize(12);body.addView(explanation);
        TextView found=new TextView(a),phase=new TextView(a),item=new TextView(a),amount=new TextView(a);phase.setTag("preparation-current-stage");item.setTag("preparation-current-component");
        for(TextView v:new TextView[]{phase,item,amount,found}){v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(14);body.addView(v);}
        ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setTag("preparation-stage-progress");body.addView(progress,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(a,12)));progress.setMax(100);
        ScrollView scroll=new ScrollView(a);scroll.addView(body);
        AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(title(a,"Проверка среды запуска","Launch environment check")).setView(scroll).setNegativeButton(title(a,"Закрыть","Close"),null).setPositiveButton(title(a,"Повторить проверку","Retry check"),null).create();
        boolean[] delivered={false};Runnable update=()->{if(a.isFinishing()||a.isDestroyed())return;
            PlanProgress plan=job.plan;overallText.setText(overall(job,a));overallBar.setIndeterminate(plan==null);overallBar.setMax(plan==null?1:plan.total());overallBar.setProgress(plan==null?0:plan.completed);overallBar.setContentDescription(overallText.getText());
            found.setText(job.inventory==null?title(a,"Проверка выбранной среды…","Checking the selected environment…"):AgentPreparation.summary(a,job.result==null?job.inventory:job.result));phase.setText(stage(job,a));item.setText(job.component);item.setVisibility(job.component.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
            progress.setIndeterminate(job.percent<0);progress.setProgress(Math.max(0,job.percent));progress.setVisibility(job.active?android.view.View.VISIBLE:android.view.View.GONE);
            amount.setText(job.total>0?String.format(Locale.ROOT,title(a,"%d / %d байт · %d%%","%d / %d bytes · %d%%"),job.received,job.total,job.percent):"");
            if(dialog.getButton(-1)!=null){dialog.getButton(-1).setEnabled(!job.active);dialog.getButton(-1).setVisibility(job.active?android.view.View.GONE:android.view.View.VISIBLE);}
            if(!job.active&&job.result!=null&&job.result.ready()&&!delivered[0]&&dialog.isShowing()){delivered[0]=true;if(completion!=null)completion.accept(job.result);}
        };
        job.observers.add(update);dialog.setOnDismissListener(d->job.observers.remove(update));dialog.show();dialog.getButton(-1).setOnClickListener(v->{if(job.active)return;dialog.dismiss();start(a,job.session,AgentPreparationOperation::prepare);show(a,job.session,completion);});update.run();
    }
}
