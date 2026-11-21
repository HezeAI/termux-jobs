package com.termux.app.workspace;

import android.content.Context;
import com.termux.BuildConfig;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** File editing inside Ubuntu AI. Separate bounded helper; never adopts a chat process. */
final class AgentInstructions {
    interface Backend { JSONObject call(JSONObject request) throws Exception; }
    static final String FOLDER="instructions-"+BuildConfig.VERSION_CODE;
    private static String template(String agent){
        switch(agent){
            case "claude":return "agent-instructions-claude.en.md";
            case "gemini":return "agent-instructions-gemini.en.md";
            default:return "agent-instructions-ubuntu-ai.en.md";
        }
    }
    static boolean supports(String agent,String environment){
        return agent.equals("codex")?(AgentEnvironments.ubuntu(environment)||environment.equals("termux"))
            :(agent.equals("claude")||agent.equals("gemini"))&&AgentEnvironments.ubuntu(environment);
    }
    static String geminiRuntime(WorkspaceStore.Session profile){
        return profile!=null&&profile.geminiAgent&&!GeminiCli.antigravity(profile)?"legacy":"antigravity";
    }
    static String builtInTemplate(Context context,String agent)throws IOException{return readTemplate(context,template(agent));}
    static String projectTemplate(Context context)throws IOException{return readTemplate(context,"agent-instructions-project.md");}
    private static String readTemplate(Context context,String asset)throws IOException{
        try(InputStream in=context.getAssets().open(asset);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>256*1024)throw new IOException("TEMPLATE_UNAVAILABLE");bytes.write(buffer,0,n);}
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    static String memoryTemplate(Context context)throws IOException{return readTemplate(context,"agent-learning.en.md");}
    static boolean hasStorageAddition(String value){return java.util.regex.Pattern.compile("(?m)^## (?:Хранение файлов и временных данных|File and temporary data storage)[ \t]*\r?$").matcher(value).find();}
    static String storageAddition(String value){
        java.util.regex.Matcher section=java.util.regex.Pattern.compile("(?m)^## (?:Хранение файлов и временных данных|File and temporary data storage)[ \t]*\r?$").matcher(value);
        if(!section.find())return "";int start=section.start(),end=value.indexOf("\n## ",start+1);return end<0?value.substring(start):value.substring(start,end+1);
    }
    static String guest(){return "/tmp/termux-jobs-codex/"+FOLDER;}
    private static String instructionMount(){return "/tmp/termux-jobs-"+FOLDER;}
    private static File directory(File files){return new File(CodexSessionShell.socketDirectory(files),FOLDER);}
    static String launchBind(File files){return directory(files).getAbsolutePath()+":"+instructionMount();}
    static void prepare(Context context)throws IOException{
        File root=CodexSessionShell.socketDirectory(context.getFilesDir());
        File folder=directory(context.getFilesDir());
        if(Files.isSymbolicLink(root.toPath())||Files.isSymbolicLink(folder.toPath()))throw new IOException("UNSAFE_DIRECTORY");
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("PRIVATE_STORAGE");
        for(String name:new String[]{"agent-skills.py","agent-instructions.py",AgentPreparation.CLI_RESOLVER,template("codex"),template("claude"),template("gemini")}){
            File file=new File(folder,name);if(Files.isSymbolicLink(file.toPath()))throw new IOException("UNSAFE_FILE");
            // Versioned assets are immutable to already running helpers.
            if(file.isFile())continue;
            File tmp=File.createTempFile("asset-",".tmp",folder);
            try{
                try(InputStream in=context.getAssets().open(name);FileOutputStream out=new FileOutputStream(tmp)){
                    byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1)out.write(data,0,n);out.getFD().sync();
                }
                tmp.setReadable(false,false);tmp.setWritable(false,false);
                if(!tmp.setReadable(true,true)||!tmp.setWritable(true,true))throw new IOException("PRIVATE_STORAGE");
                Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE);
            }finally{tmp.delete();}
        }
    }
    static String initialization(File files,WorkspaceStore.Session profile){
        String agent=AgentPreparation.agent(profile),environment=profile.codexEnvironment;
        if(!supports(agent,environment))return "";
        String source=environment.equals("termux")?directory(files).getAbsolutePath()
            :agent.equals("codex")?guest():instructionMount();
        String request="{\"operation\":\"init\",\"agent\":\""+agent+"\",\"geminiRuntime\":\""+geminiRuntime(profile)+"\"}";
        return "printf '%s' "+SessionShell.quote(request)+" | timeout -k 1 10 python3 -I "
            +SessionShell.quote(source+"/agent-instructions.py")+" "+SessionShell.quote(source+"/"+template(agent))
            +" >/dev/null 2>/dev/null || printf '%s\\n' "+SessionShell.quote(
                WorkspaceText.tr("Агент: ").startsWith("Агент")
                    ?"Не удалось подготовить общие инструкции. Откройте «Инструкции агентов» в настройках."
                    :"Could not prepare shared instructions. Open Agent instructions in settings.")+" >&2\n";
    }
    static String command(File files){return command(files,"codex","ubuntu-ai");}
    static String command(File files,String agent,String environment){return command(files,agent,environment,"/root");}
    static String command(File files,String agent,String environment,String project){
        if(!supports(agent,environment))throw new IllegalArgumentException("Invalid instruction environment");
        if(environment.equals("termux")){
            File folder=new File(CodexSessionShell.socketDirectory(files),FOLDER);
            String nativeCode="if [ -f \"$HOME/.bashrc\" ];then . \"$HOME/.bashrc\" >/dev/null 2>&1;fi;exec python3 -I "+SessionShell.quote(new File(folder,"agent-instructions.py").getAbsolutePath())+" "+SessionShell.quote(new File(folder,template(agent)).getAbsolutePath());
            return "exec "+SessionShell.quote(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash")+" --noprofile --norc -i -c "+SessionShell.quote(nativeCode);
        }
        String guest=instructionMount();
        String runtime=agent.equals("codex")?AgentPreparation.codexPath(guest,environment)
            :agent.equals("claude")?AgentPreparation.cliResolver(guest,environment)+"JOBS_CLAUDE_MANAGED=$(jobs_agent_cli_managed claude) || JOBS_CLAUDE_MANAGED=; export JOBS_CLAUDE_MANAGED; ":"";
        String code="if [ -f \"$HOME/.bashrc\" ]; then . \"$HOME/.bashrc\" >/dev/null 2>&1; fi; "
            +runtime+"exec python3 -I "+SessionShell.quote(guest+"/agent-instructions.py")+" "+SessionShell.quote(guest+"/"+template(agent));
        return "exec proot-distro login --bind "+SessionShell.quote(launchBind(files))
            +(!agent.equals("codex")?" --work-dir "+SessionShell.quote(project):"")+" "+environment+" -- /bin/bash --noprofile --norc -i -c "+SessionShell.quote(code);
    }
    static JSONObject request(Context context,JSONObject input)throws Exception{
        prepare(context);
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",command(context.getFilesDir(),input.optString("agent","codex"),input.optString("environment","ubuntu-ai"),input.optString("project","/root")));
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){
            int split=item.indexOf('=');if(split>0)builder.environment().put(item.substring(0,split),item.substring(split+1));
        }
        builder.redirectError(new File("/dev/null"));
        Process process=builder.start();
        // The worker owns only this diagnostic helper. No process-tree or session signals.
        java.util.concurrent.ScheduledExecutorService timer=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        java.util.concurrent.ScheduledFuture<?> deadline=timer.schedule(process::destroy,input.optString("kind").equals("skills")?100:25,TimeUnit.SECONDS);
        try{
            try(OutputStream out=process.getOutputStream()){out.write(input.toString().getBytes(StandardCharsets.UTF_8));}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=process.getInputStream()){
                byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1){if(bytes.size()+n>3*1024*1024)throw new IOException("RESULT_TOO_LARGE");bytes.write(chunk,0,n);}
            }
            JSONObject response=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            if(!response.optBoolean("ok"))throw new IOException(response.optString("error","INSTRUCTIONS_UNAVAILABLE"));
            if(process.waitFor()!=0)throw new IOException("INSTRUCTIONS_UNAVAILABLE");
            return response.getJSONObject("result");
        }finally{deadline.cancel(false);timer.shutdownNow();process.destroy();}
    }
}
