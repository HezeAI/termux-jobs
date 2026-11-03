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
    static String language(Context c){return WorkspaceLanguage.code(c).equals("ru")?"ru":"en";}
    static String template(String language){return language.equals("ru")?"agent-instructions-ubuntu-ai.md":"agent-instructions-ubuntu-ai.en.md";}
    static String builtInTemplate(Context context,String language)throws IOException{return builtInTemplate(context,language,"codex");}
    static String template(String language,String agent){return agent.equals("claude")?(language.equals("ru")?"agent-instructions-claude.md":"agent-instructions-claude.en.md"):template(language);}
    static String builtInTemplate(Context context,String language,String agent)throws IOException{
        try(InputStream in=context.getAssets().open(template(language,agent));ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>256*1024)throw new IOException("TEMPLATE_UNAVAILABLE");bytes.write(buffer,0,n);}
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    static String memoryTemplate(Context context,String language)throws IOException{
        String file=language.equals("ru")?"agent-learning.md":"agent-learning.en.md";
        try(InputStream in=context.getAssets().open(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    static boolean hasStorageAddition(String value){return java.util.regex.Pattern.compile("(?m)^## (?:Хранение файлов и временных данных|File and temporary data storage)[ \t]*\r?$").matcher(value).find();}
    static String storageAddition(String value){
        java.util.regex.Matcher section=java.util.regex.Pattern.compile("(?m)^## (?:Хранение файлов и временных данных|File and temporary data storage)[ \t]*\r?$").matcher(value);
        if(!section.find())return "";int start=section.start(),end=value.indexOf("\n## ",start+1);return end<0?value.substring(start):value.substring(start,end+1);
    }
    static String guest(){return "/tmp/termux-jobs-codex/"+FOLDER;}
    static void prepare(Context context)throws IOException{
        File root=CodexSessionShell.socketDirectory(context.getFilesDir());
        File folder=new File(root,FOLDER);
        if(Files.isSymbolicLink(root.toPath())||Files.isSymbolicLink(folder.toPath()))throw new IOException("UNSAFE_DIRECTORY");
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("PRIVATE_STORAGE");
        for(String name:new String[]{"agent-skills.py","agent-instructions.py","agent-instructions-ubuntu-ai.md","agent-instructions-ubuntu-ai.en.md","agent-instructions-claude.md","agent-instructions-claude.en.md"}){
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
    static String initialization(WorkspaceStore.Session profile){
        if(!profile.codexEnvironment.equals("ubuntu-ai"))return "";
        return "printf '%s' '{\"operation\":\"init\"}' | python3 -I "+SessionShell.quote(guest()+"/agent-instructions.py")
            +" "+SessionShell.quote(guest()+"/"+template(WorkspaceText.tr("Агент: ").startsWith("Агент")?"ru":"en"))
            +" >/dev/null || printf '%s\\n' 'Ubuntu AI instructions could not be prepared. Open Agent instructions in settings.' >&2\n";
    }
    static String command(File files,String language){return command(files,language,"codex","ubuntu-ai");}
    static String command(File files,String language,String agent,String environment){return command(files,language,agent,environment,"/root");}
    static String command(File files,String language,String agent,String environment,String project){
        if(!java.util.Arrays.asList("codex","claude").contains(agent)||!(AgentEnvironments.ubuntu(environment)||environment.equals("termux")&&agent.equals("codex")))throw new IllegalArgumentException("Invalid instruction environment");
        if(environment.equals("termux")){
            File folder=new File(CodexSessionShell.socketDirectory(files),FOLDER);
            String nativeCode="if [ -f \"$HOME/.bashrc\" ];then . \"$HOME/.bashrc\" >/dev/null 2>&1;fi;exec python3 -I "+SessionShell.quote(new File(folder,"agent-instructions.py").getAbsolutePath())+" "+SessionShell.quote(new File(folder,template(language,agent)).getAbsolutePath());
            return "exec "+SessionShell.quote(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash")+" --noprofile --norc -i -c "+SessionShell.quote(nativeCode);
        }
        String code="if [ -f \"$HOME/.bashrc\" ]; then . \"$HOME/.bashrc\" >/dev/null 2>&1; fi; "
            +(agent.equals("codex")?AgentPreparation.codexPath():"")+"exec python3 -I "+SessionShell.quote(guest()+"/agent-instructions.py")+" "+SessionShell.quote(guest()+"/"+template(language,agent));
        return "exec proot-distro login --bind "+SessionShell.quote(CodexSessionShell.socketDirectory(files).getAbsolutePath()+":/tmp/termux-jobs-codex")
            +(agent.equals("claude")?" --work-dir "+SessionShell.quote(project):"")+" "+environment+" -- /bin/bash --noprofile --norc -i -c "+SessionShell.quote(code);
    }
    static JSONObject request(Context context,JSONObject input)throws Exception{
        prepare(context);
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",command(context.getFilesDir(),language(context),input.optString("agent","codex"),input.optString("environment","ubuntu-ai"),input.optString("project","/root")));
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
