package com.termux.app.workspace;

import android.app.*;import android.content.*;import android.widget.*;
import com.termux.shared.termux.TermuxConstants;import com.termux.shared.shell.TermuxShellUtils;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;import java.util.function.*;

/** Read-only inventory before a user-approved launch; installation runs in the new terminal only. */
final class AgentPreparation {
    static final String GUEST="/tmp/termux-jobs-setup";
    static final String[] ASSETS={"agent-dependencies.sh","agent-host-setup.sh","claude-bootstrap.sh","gemini-bootstrap.sh","ai-agents-bootstrap.sh","agent-preparation-status.sh","ubuntu-ai-installer.py","ubuntu-ai-proot-distro-5.9.0.zip","codex-bootstrap.sh","vertex-bootstrap.sh","vertex-project.sh","environment-runtime.sh","environment-image.py","environment-images.json"};
    static final Set<String> COMPONENTS=new HashSet<>(Arrays.asList("proot-distro","ubuntu","curl","tar","sha256sum","flock","xz","find","sort","xargs","ca-certificates","node","codex-cli","claude-cli","gemini-cli","opencode-cli","gcloud","python3"));
    interface Runner {Plan run(Context c,WorkspaceStore.Session p)throws Exception;}
    static final Runner SYSTEM=AgentPreparation::query;
    static final class Plan {
        String environment="ubuntu",path="";
        final LinkedHashSet<String> ready=new LinkedHashSet<>(),missing=new LinkedHashSet<>();boolean complete,authNeeded;String error="";Integer exitCode;
        boolean ready(){return complete&&error.isEmpty()&&missing.isEmpty();}
        void record(String line){
            if(line.equals("CHECK:done"))complete=true;
            else if(line.equals("AUTH:needed"))authNeeded=true;
            else for(String prefix:new String[]{"READY:","MISSING:"})if(line.startsWith(prefix)){
                String id=line.substring(prefix.length());if(COMPONENTS.contains(id))(prefix.equals("READY:")?ready:missing).add(id);
            }
        }
        void finish(int exit){exitCode=exit;if(exit!=0||!complete)error="CHECK_FAILED";}
        static Plan parse(String text,int exit){Plan p=new Plan();if(text.length()>32768){p.error="CHECK_TOO_LARGE";return p;}try{AgentPreparationOutput.drain(new StringReader(text),p::record);}catch(IOException impossible){p.error="CHECK_FAILED";}p.finish(exit);return p;}

    }
    static String agent(WorkspaceStore.Session p){return p.geminiAgent?"gemini":p.claudeAgent?"claude":p.aiAgent.equals("opencode")?"opencode":p.environment.equals("codex")?"codex":"";}
    static boolean supports(WorkspaceStore.Session p){return !agent(p).isEmpty()&&AgentEnvironments.ubuntu(p.codexEnvironment)&&(p.environment.equals("ubuntu")||p.environment.equals("codex"));}
    static String key(WorkspaceStore.Session p){return agent(p)+"|"+p.environment+"|"+p.codexEnvironment+"|"+p.geminiAuth+"|"+p.geminiCloudProject+"|"+p.geminiCloudLocation;}
    static File directory(File files){return new File(files,".agent-preparation");}
    static void prepare(Context c)throws IOException{File dir=directory(c.getFilesDir());if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create setup directory");protect(dir);for(String name:ASSETS){File temp=File.createTempFile("setup-",".tmp",dir);try{try(InputStream in=c.getAssets().open(name);OutputStream out=new FileOutputStream(temp)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}protect(temp);if(!temp.renameTo(new File(dir,name)))throw new IOException("Cannot publish setup asset");}finally{temp.delete();}}}
    private static void protect(File f)throws IOException{if(!f.setReadable(false,false)||!f.setWritable(false,false)||!f.setExecutable(false,false)||!f.setReadable(true,true)||!f.setWritable(true,true)||f.isDirectory()&&!f.setExecutable(true,true))throw new IOException("Cannot protect setup file");}
    static String language(Context c){return WorkspaceLanguage.code(c);}
    static String tr(Context c,String ru,String en){return language(c).equals("ru")?ru:en;}
    static String environment(WorkspaceStore.Session p){return "export TERMUX_JOBS_AGENT_ENVIRONMENT="+SessionShell.quote(AgentEnvironments.alias(p))+"; export JOBS_AGENT_UBUNTU="+SessionShell.quote(AgentEnvironments.alias(p))+"; export JOBS_AGENT_SETUP_ALLOWED="+(p.agentSetupAllowed?"1":"0")+"; export JOBS_AGENT_LANGUAGE="+(WorkspaceText.tr("Агент: ").startsWith("Агент")?"ru":"en")+"; ";}
    static String host(File files,WorkspaceStore.Session p){if(!supports(p))return "";return environment(p)+"export JOBS_AGENT_PREPARATION_STATUS="+SessionShell.quote(new File(SessionShell.directory(files,p.id),"agent-preparation-status").getAbsolutePath())+"; source "+SessionShell.quote(new File(directory(files),"agent-host-setup.sh").getAbsolutePath())+"\njobs_agent_host_prepare || return $?\n";}
    static String wrap(WorkspaceStore.Session p,String command){if(!supports(p))return command;return "( set -eE; "+environment(p)+"export JOBS_AGENT_PREPARATION_STATUS="+SessionShell.quote("/tmp/termux-fork-session-"+p.id+"/agent-preparation-status")+"; source "+SessionShell.quote(GUEST+"/agent-dependencies.sh")+"; trap 'jobs_agent_stage_failure' ERR; jobs_agent_prepare "+SessionShell.quote(agent(p))+" "+SessionShell.quote(p.geminiAgent?p.geminiAuth:"api")+"; "+(p.geminiAgent&&p.geminiAuth.equals("vertex")?"export JOBS_GEMINI_VERTEX_PROJECT="+SessionShell.quote(p.geminiCloudProject)+"; export JOBS_GEMINI_VERTEX_LOCATION="+SessionShell.quote(p.geminiCloudLocation)+"; source "+SessionShell.quote(GUEST+"/vertex-bootstrap.sh")+"; "+VertexConnection.exports(p)+"source "+SessionShell.quote(GUEST+"/vertex-project.sh")+"; ":"")+command+" )";}
    private static String readStatus(java.nio.file.Path path)throws IOException{try(InputStream in=java.nio.file.Files.newInputStream(path,java.nio.file.LinkOption.NOFOLLOW_LINKS)){byte[] b=new byte[65];int total=0,n;while(total<b.length&&(n=in.read(b,total,b.length-total))>0)total+=n;return total>64?"":new String(b,0,total,StandardCharsets.UTF_8).trim();}}
    static String status(File files,WorkspaceStore.Session p,boolean ru){
        if(!supports(p))return "";
        try{java.nio.file.Path path=new File(SessionShell.directory(files,p.id),"agent-preparation-status").toPath();if(java.nio.file.Files.isSymbolicLink(path)||java.nio.file.Files.size(path)>64)return "";
            String state=readStatus(path);
            return stageText(state,ru);
        }catch(IOException|SecurityException error){return "";}
    }
    static String stageText(String state,boolean ru){
            switch(state){
            case "checking-ubuntu":return ru?"Проверка Ubuntu AI…":"Checking Ubuntu AI…";
            case "installing-ubuntu":return ru?"Установка Ubuntu AI через proot-distro 5.9.0…":"Installing Ubuntu AI with proot-distro 5.9.0…";
            case "failed-ubuntu":return ru?"Подготовка Ubuntu не завершена. Причина — ниже в терминале. Компоненты агента не устанавливались.":"Ubuntu preparation failed. See the terminal below. Agent components were not installed.";
            case "checking-components":return ru?"Проверка компонентов агента…":"Checking agent components…";
            case "installing-components":return ru?"Подготовка компонентов выбранного агента…":"Preparing selected agent components…";
            case "ready":return ru?"Компоненты готовы. Запуск агента и авторизация — в терминале.":"Components ready. Agent startup and sign-in are shown in the terminal.";
            case "checking-cli":return ru?"Проверка запуска CLI…":"Checking CLI startup…";
            case "failed-cli":return ru?"CLI не прошёл проверку запуска. Причина — в терминале.":"CLI startup check failed. See the terminal.";
            case "failed-components":return ru?"Подготовка компонентов не завершена. Причина — в терминале.":"Component preparation failed. See the terminal.";
            default:return "";
            }
    }
    static String codexPath(){return "if ! command -v node >/dev/null 2>&1 || ! node -e 'process.exit(Number(process.versions.node.split(\".\")[0])>=20?0:1)' >/dev/null 2>&1; then export PATH=\"$HOME/.local/share/termux-jobs/agent-tools/node-v22.22.1-linux-arm64/bin:$PATH\"; fi; if ! command -v codex >/dev/null 2>&1; then export PATH=\"$HOME/.local/share/termux-jobs/codex-cli/0.163.0/vendor/aarch64-unknown-linux-musl/bin:$HOME/.local/share/termux-jobs/codex-cli/0.163.0/vendor/aarch64-unknown-linux-musl/codex-path:$PATH\"; fi; ";}
    static Plan query(Context c,WorkspaceStore.Session p)throws Exception{
        prepare(c);File shell=new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"bash"),timer=new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"timeout");if(!shell.isFile()||!timer.isFile()){Plan plan=new Plan();plan.error="TERMUX_NOT_READY";return plan;}
        String script="exec "+SessionShell.quote(timer.getAbsolutePath())+" -k 2 25 "+SessionShell.quote(shell.getAbsolutePath())+" --noprofile --norc "+SessionShell.quote(new File(directory(c.getFilesDir()),"agent-host-setup.sh").getAbsolutePath())+" check "+SessionShell.quote(agent(p))+" "+SessionShell.quote(p.geminiAgent?p.geminiAuth:"api")+" "+SessionShell.quote(AgentEnvironments.alias(p));
        ProcessBuilder pb=new ProcessBuilder(shell.getAbsolutePath(),"--noprofile","--norc","-c",script);pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();for(String item:TermuxShellUtils.buildEnvironment(c,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int i=item.indexOf('=');if(i>0)pb.environment().put(item.substring(0,i),item.substring(i+1));}pb.redirectErrorStream(true);Process process=pb.start();try{Plan result=new Plan();try(Reader in=new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8)){AgentPreparationOutput.drain(in,result::record);}if(!process.waitFor(3,TimeUnit.SECONDS))throw new IOException("Environment check timeout");result.finish(process.exitValue());result.environment=AgentEnvironments.alias(p);File fs=WorkspaceEnvironments.root(new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"),result.environment);result.path=fs==null?"":fs.getAbsolutePath();return result;}finally{process.destroy();}
    }
    private static List<String> componentLabels(Plan p,Collection<String> ids){List<String> labels=new ArrayList<>();for(String id:ids)labels.add(id.equals("ubuntu")?AgentEnvironments.label(p.environment):id);return labels;}
    static String summary(Context c,Plan p){String body=p.error.equals("UNSUPPORTED_ENVIRONMENT")?tr(c,"Автоподготовка доступна для AI Agents в Ubuntu. Старый профиль Codex в Termux сохранён без изменений.","Automatic preparation is available for AI Agents in Ubuntu. The legacy Codex Termux profile is unchanged."):p.ready()?tr(c,"Компоненты выбранного агента найдены.","The selected agent components were found."):!p.error.isEmpty()?tr(c,"Не удалось проверить выбранное окружение. Оно может быть занято установкой или недоступно для входа. Дождитесь завершения установки и повторите проверку. Переустановка не запускалась; действующие сессии не изменены.","Could not check the selected environment. It may be busy installing or unavailable for login. Wait for installation to finish and retry. No reinstall was started; running sessions are unchanged."):tr(c,"Не хватает компонентов:","Missing components:");if(!p.error.isEmpty()&&p.exitCode!=null)body+="\n"+tr(c,"Код завершения: ","Exit code: ")+p.exitCode;if(!p.missing.isEmpty())body+="\n• "+String.join("\n• ",componentLabels(p,p.missing));if(!p.ready.isEmpty())body+="\n\n"+tr(c,"Найдены: ","Found: ")+String.join(", ",componentLabels(p,p.ready));return AgentEnvironments.label(p.environment)+(p.path.isEmpty()?"":"\n"+p.path)+"\n\n"+body+"\n\n"+tr(c,"Это проверка компонентов, а не входа в аккаунт, доступности модели или оплаты. Недостающее подготавливается по явной проверке или при запуске сессии.","This checks components, not account sign-in, model access or billing. Missing components are prepared by an explicit check or when launching a session.");}
    static void check(Activity a,WorkspaceStore.Session p,Consumer<Plan> result){check(a,p,SYSTEM,result);}
    static void check(Activity a,WorkspaceStore.Session p,Runner runner,Consumer<Plan> result){
        if(!supports(p)){Plan plan=new Plan();plan.error="UNSUPPORTED_ENVIRONMENT";result.accept(plan);return;}
        final boolean[] cancelled={false};AlertDialog wait=WorkspaceDialogs.builder(a).setTitle(tr(a,"Проверка среды запуска","Launch environment check")).setMessage(tr(a,"Проверяем компоненты: ","Checking components: ")+AgentEnvironments.label(p.codexEnvironment)).setNegativeButton(tr(a,"Отмена","Cancel"),(d,w)->cancelled[0]=true).create();wait.setOnCancelListener(d->cancelled[0]=true);wait.show();ExecutorService worker=Executors.newSingleThreadExecutor();worker.execute(()->{Plan plan;try{plan=runner.run(a,p);}catch(Exception e){plan=new Plan();plan.error="CHECK_FAILED";}Plan checked=plan;a.runOnUiThread(()->{boolean active=wait.isShowing()&&!cancelled[0]&&!a.isFinishing()&&!a.isDestroyed();wait.dismiss();if(active)result.accept(checked);});worker.shutdown();});
    }
    static void beforeLaunch(Activity a,WorkspaceStore.Session p,Consumer<Boolean> launch){if(!supports(p)){launch.accept(false);return;}VertexConnection.applyDefaults(a,p);String captured=key(p);check(a,p,plan->{if(!captured.equals(key(p)))return;if(!plan.error.isEmpty()){WorkspaceDialogs.builder(a).setTitle(tr(a,"Проверка среды запуска","Launch environment check")).setMessage(summary(a,plan)).setPositiveButton(tr(a,"Назад","Back"),null).show();return;}if(plan.ready()){p.agentSetupAllowed=false;launch.accept(plan.authNeeded);return;}WorkspaceDialogs.builder(a).setTitle(tr(a,"Подготовить среду запуска?","Prepare the launch environment?"))
        .setMessage(summary(a,plan)+"\n\n"+tr(a,"После подтверждения установим только недостающие пакеты Ubuntu/Termux и отдельный CLI выбранного агента. Если выбранная Ubuntu отсутствует, установим именно её независимо от других окружений. Существующие среды не заменяются. Этапы и результат подготовки показаны в окне проверки. Vertex потребует подтвердить вход Google Cloud в браузере. Облачный проект и оплату приложение не создаёт.","After confirmation, install only missing Ubuntu/Termux packages and a private CLI for the selected agent. If the selected Ubuntu is absent, install that environment independently of other environments. Existing environments are not replaced. Preparation stages and results are shown in the check window. Vertex requires Google Cloud approval in your browser. The app does not create a cloud project or set up billing."))
        .setPositiveButton(tr(a,"Подготовить и запустить","Prepare and launch"),(d,w)->{if(captured.equals(key(p))){p.agentSetupAllowed=true;AgentPreparationOperation.show(a,p,result->{if(captured.equals(key(p)))launch.accept(result.authNeeded);});}}).setNegativeButton(tr(a,"Назад","Back"),null).show();});}
}
