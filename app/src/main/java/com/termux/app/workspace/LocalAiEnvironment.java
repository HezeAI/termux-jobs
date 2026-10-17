package com.termux.app.workspace;

import android.content.Context;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/** Exact routing to main Termux or one app-owned Ubuntu. Not an Android security sandbox. */
final class LocalAiEnvironment {
    static final String ALIAS="jobs-local-ai",UBUNTU="proot:"+ALIAS,INTERNAL="local-ai-ubuntu";
    static String backend(String selected){validate(selected);return selected.equals(UBUNTU)?INTERNAL:"termux";}
    static boolean valid(String value){return "termux".equals(value)||UBUNTU.equals(value);}
    static void validate(String value){if(!valid(value))throw new SecurityException("LOCAL_ENVIRONMENT_RESELECT: choose Main Termux or Ubuntu for local AI; existing Ubuntu is not used");}
    static String label(String value,boolean ru){return value.equals("termux")?(ru?"Основной Termux":"Main Termux"):value.equals(UBUNTU)?(ru?"Ubuntu для локального ИИ":"Ubuntu for local AI"):(ru?"Требуется выбор окружения":"Select an environment again");}
    static String login(String script){String host="set --; for path in /sdcard /storage/emulated/0 /storage/self/primary; do if [ -d \"$path\" ] && [ -r \"$path\" ] && [ -x \"$path\" ]; then set -- \"$@\" --bind \"$path:$path\"; fi; done; exec proot-distro login --isolated \"$@\" "+SessionShell.quote(ALIAS)+" -- /bin/sh -c "+SessionShell.quote(script);return "bash --noprofile --norc -c "+SessionShell.quote(host);}

    static String command(String script){return login("test -f /etc/termux-jobs-local-ai-ready || { echo LOCAL_ENVIRONMENT_NOT_READY; exit 2; }; "+script);}
    static File runtime(){return new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro");}
    static File root(File runtime)throws IOException{
        File current=new File(runtime,"containers/"+ALIAS+"/rootfs"),legacy=new File(runtime,"installed-rootfs/"+ALIAS);File selected=current.isDirectory()?current:legacy.isDirectory()?legacy:null;
        if(current.exists()&&legacy.exists())throw new IOException("LOCAL_ENVIRONMENT_CONFLICT: two container roots exist");if(selected==null)return null;
        File expected=selected.equals(current)?new File(new File(new File(runtime.getCanonicalFile(),"containers"),ALIAS),"rootfs"):new File(new File(runtime.getCanonicalFile(),"installed-rootfs"),ALIAS);
        if(!selected.getCanonicalFile().equals(expected.getAbsoluteFile()))throw new IOException("LOCAL_ENVIRONMENT_UNSAFE_LINK: container root must not redirect to another environment");return selected.getCanonicalFile();
    }
    static boolean managerAvailable(){return new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"proot-distro").canExecute();}
    static File state(Context context){return new File(context.getFilesDir(),"local-ai/environment-ubuntu.json");}
    static JSONObject record(Context context)throws Exception{File file=state(context);return file.isFile()?new JSONObject(LocalAiFiles.read(new FileInputStream(file),8192)):new JSONObject();}
    static File owner(File root)throws IOException{File marker=new File(root,"etc/termux-jobs-local-ai-owner");if(!marker.getCanonicalFile().equals(new File(root,"etc/termux-jobs-local-ai-owner").getAbsoluteFile()))throw new IOException("LOCAL_ENVIRONMENT_UNSAFE_LINK");return marker;}
    static File readyMarker(File root)throws IOException{File marker=new File(root,"etc/termux-jobs-local-ai-ready");if(!marker.getCanonicalFile().equals(marker.getAbsoluteFile()))throw new IOException("LOCAL_ENVIRONMENT_UNSAFE_LINK");return marker;}
    static boolean owned(File root,JSONObject saved)throws Exception{File marker=owner(root);return saved.optString("token").matches("[a-f0-9-]{36}")&&marker.isFile()&&LocalAiFiles.read(new FileInputStream(marker),256).trim().equals(saved.optString("token"));}
    static boolean ready(Context context,File runtime)throws Exception{File root=root(runtime);JSONObject saved=record(context);return root!=null&&saved.optString("status").equals("ready")&&owned(root,saved)&&saved.optString("alias").equals(ALIAS)&&saved.optString("root").equals(root.getPath())&&readyMarker(root).isFile();}
    static void require(Context context,String environment)throws Exception{validate(environment);if(environment.equals(UBUNTU)&&(!managerAvailable()||!ready(context,runtime())))throw new IOException("LOCAL_ENVIRONMENT_NOT_READY: prepare Ubuntu for local AI in session settings; no fallback is used");}
    static String installCommand(String help){if(help.contains("--name"))return "proot-distro install --name "+SessionShell.quote(ALIAS)+" ubuntu:24.04";if(help.contains("--override-alias"))return "proot-distro install --override-alias "+SessionShell.quote(ALIAS)+" ubuntu";throw new IllegalArgumentException("PROOT_DISTRO_UNSUPPORTED: custom container aliases are required");}
    static String bootstrap(){return "set -eu; . /etc/os-release; test \"$ID\" = ubuntu || { echo NOT_UBUNTU; exit 2; }; export DEBIAN_FRONTEND=noninteractive; apt-get -o Acquire::Retries=0 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 update; apt-get -y --no-remove -o DPkg::Lock::Timeout=15 -o Acquire::Retries=0 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Dpkg::Options::=--force-confold install bash coreutils findutils ca-certificates python3 git; mkdir -p /root/projects; printf '%s\\n' jobs-local-ai-v1 > /etc/termux-jobs-local-ai-ready";}
    static synchronized void prepare(Context context,Consumer<String> progress)throws Exception{
        File base=runtime(),root=root(base);JSONObject saved=record(context);
        if(managerAvailable()&&ready(context,base)){progress.accept("READY");return;}
        if(root!=null&&(!saved.optString("alias").equals(ALIAS)||!(saved.optString("status").equals("preparing")||saved.optString("status").equals("ready"))||!owned(root,saved)))throw new IOException("LOCAL_ENVIRONMENT_NAME_IN_USE: refusing to adopt or replace an existing container");
        String manager=host(context,"if command -v proot-distro >/dev/null; then echo PRESENT; else echo MISSING; fi",text->{});if(manager.trim().equals("MISSING")){progress.accept("INSTALL_PROOT_DISTRO");host(context,"export DEBIAN_FRONTEND=noninteractive; apt-get -y --no-remove -o DPkg::Lock::Timeout=15 -o Acquire::Retries=0 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 install proot-distro",progress);}
        String loginHelp=host(context,"proot-distro login --help",text->{});if(!loginHelp.contains("--isolated")||!loginHelp.contains("--bind"))throw new IOException("PROOT_DISTRO_UNSUPPORTED: isolated login and explicit bindings required");
        if(root==null){String help=host(context,"proot-distro install --help",text->{});File state=state(context);File parent=state.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create environment state");saved=new JSONObject().put("alias",ALIAS).put("status","preparing").put("token",UUID.randomUUID().toString());LocalAiFiles.atomic(state,saved.toString());progress.accept("INSTALL_UBUNTU");host(context,installCommand(help),progress);root=root(base);if(root==null)throw new IOException("LOCAL_ENVIRONMENT_INSTALL_INCOMPLETE");LocalAiFiles.atomic(owner(root),saved.getString("token"));}
        progress.accept("PREPARE_PACKAGES");host(context,login(bootstrap()),progress);root=root(base);if(root==null)throw new IOException("LOCAL_ENVIRONMENT_INSTALL_INCOMPLETE");saved.put("root",root.getPath()).put("status","ready");LocalAiFiles.atomic(state(context),saved.toString());progress.accept("READY");
    }
    /** Confirmed setup is allowed to finish, even when a view closes. Never destroys another session. */
    static String host(Context context,String script,Consumer<String> progress)throws Exception{
        ProcessBuilder pb=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",script);pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();for(String item:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int at=item.indexOf('=');if(at>0)pb.environment().put(item.substring(0,at),item.substring(at+1));}pb.environment().put("LC_ALL","C");File log=File.createTempFile("local-env-",".log",context.getCacheDir());pb.redirectErrorStream(true);pb.redirectOutput(log);Process process;try{process=pb.start();}catch(Exception e){log.delete();throw e;}try{process.getOutputStream().close();}catch(IOException ignored){}boolean interrupted=false;
        try{while(true){boolean ended=false;try{ended=process.waitFor(400,java.util.concurrent.TimeUnit.MILLISECONDS);}catch(InterruptedException e){interrupted=true;}try{String tail=tail(log,1000).trim();int at=tail.lastIndexOf('\n');progress.accept(at>=0?tail.substring(at+1):tail);}catch(Exception ignored){}if(ended)break;}String tail=tail(log,65536);if(process.exitValue()!=0)throw new IOException("ENVIRONMENT_SETUP_FAILED exit="+process.exitValue()+"\n"+tail);return tail;}finally{log.delete();if(interrupted)Thread.currentThread().interrupt();}
    }
    static String tail(File file,int limit)throws IOException{try(RandomAccessFile input=new RandomAccessFile(file,"r")){long length=input.length(),offset=Math.max(0,length-limit);input.seek(offset);byte[] bytes=new byte[(int)(length-offset)];input.readFully(bytes);return new String(bytes,java.nio.charset.StandardCharsets.UTF_8);}}
}
