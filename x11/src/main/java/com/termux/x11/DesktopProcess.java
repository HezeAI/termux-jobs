package com.termux.x11;

import android.content.Context;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.file.Files;
import java.util.function.Consumer;

/** A pipe-owned, separate process group. Never adopts an existing shell or PID. */
final class DesktopProcess implements Closeable {
    private final Process process;
    static boolean supportsStyle(String target){return DesktopPrograms.supportsStyle(target);}
    static boolean supported(String target){return DesktopPrograms.supportsTarget(target);}
    DesktopProcess(Process process){this.process=process;}
    static DesktopProcess start(Context context,File base,String operation,String target,String focusToken)throws Exception{
        return start(context,base,operation,target,focusToken,"xfce");
    }
    static DesktopProcess start(Context context,File base,String operation,String target,String focusToken,String desktop)throws Exception{
        return start(context,base,operation,target,focusToken,desktop,true);
    }
    static DesktopProcess start(Context context,File base,String operation,String target,String focusToken,String desktop,boolean garconFilter)throws Exception{
        return start(context,base,operation,target,focusToken,desktop,garconFilter,"cpu");
    }
    static DesktopProcess start(Context context,File base,String operation,String target,String focusToken,String desktop,boolean garconFilter,String renderer)throws Exception{
        return start(context,base,operation,target,focusToken,desktop,garconFilter,renderer,false);
    }
    static DesktopProcess start(Context context,File base,String operation,String target,String focusToken,String desktop,boolean garconFilter,String renderer,boolean termuxStyle)throws Exception{
        return start(context,base,operation,target,focusToken,desktop,garconFilter,renderer,termuxStyle,100);
    }
    static DesktopProcess start(Context context,File base,String operation,String target,String focusToken,String desktop,boolean garconFilter,String renderer,boolean termuxStyle,int interfaceScale)throws Exception{
        X11ScreenSettings.validateScale(interfaceScale);
        DesktopProfiles.validate(target,desktop,renderer,termuxStyle);
        if(termuxStyle&&!supportsStyle(target))throw new IllegalArgumentException("Style requires a supported X11 desktop");
        if(!renderer.equals("cpu")&&!renderer.equals("turnip")&&!renderer.equals("virgl")&&!renderer.equals("wrapper")&&!renderer.equals("bionic-glibc"))throw new IllegalArgumentException("Invalid renderer");
        if(renderer.equals("turnip")&&!target.equals("ubuntu")&&!target.equals("termux")&&!target.equals("debian")&&!target.equals("alpine")&&!target.equals("fedora")&&!target.equals("opensuse")&&!target.equals("void")&&!target.equals("archlinux"))throw new IllegalArgumentException("Unsupported GPU target");
        if(renderer.equals("bionic-glibc")&&!target.equals("ubuntu")&&!target.equals("debian")&&!target.equals("fedora")&&!target.equals("archlinux")&&!target.equals("opensuse")&&!target.equals("void"))throw new IllegalArgumentException("Bionic for glibc requires a supported glibc desktop");
        if(renderer.equals("wrapper")&&!target.equals("termux"))throw new IllegalArgumentException("Wrapper is Termux-only");
        if(renderer.equals("virgl")&&!VirglInstaller.supported(target))throw new IllegalArgumentException("Unsupported VirGL target");
        if(!desktop.equals("xfce"))throw new IllegalArgumentException("Invalid desktop");
        if(!supported(target)||!(operation.equals("check")||operation.equals("install")||(operation.equals("repair-launchers")||(operation.equals("firefox-compat-on")||operation.equals("firefox-compat-off")))||operation.equals("run")))throw new IllegalArgumentException("Unsupported desktop target");
        if((operation.equals("repair-launchers")&&!DesktopPrograms.supportsMaintenance(target))||((operation.equals("firefox-compat-on")||operation.equals("firefox-compat-off"))&&!DesktopPrograms.supportsFirefox(target)))throw new IllegalArgumentException("Maintenance requires a supported desktop environment");
        File script=asset(context,base,"x11-desktop.sh"),guardian=asset(context,base,"x11-guardian.sh");
        if(renderer.equals("bionic-glibc")){asset(context,base,"x11-wrapper.py");asset(context,base,"x11-bionic-glibc.py");asset(context,base,"x11-bionic-glibc-env.sh");}
        if(target.equals("termux")&&renderer.equals("wrapper"))asset(context,base,"x11-wrapper.py");
        if(target.equals("alpine")&&renderer.equals("virgl")){asset(context,base,"x11-virgl-client.py");asset(context,base,"x11-extras.py");}
        asset(context,base,"x11-virgl.sh");asset(context,base,"x11-virgl-env.sh");asset(context,base,"x11-virgl-setup.sh");asset(context,base,"x11-gpu.py");asset(context,base,"x11-gpu-env.sh");asset(context,base,"x11-session.sh");asset(context,base,"x11-session-bus.py");asset(context,base,"x11-ready.py");asset(context,base,"x11-focus.py");if(target.equals("alpine"))asset(context,base,"x11-alpine-compat.so");if(target.equals("fedora")||target.equals("archlinux"))asset(context,base,"x11-fedora-glycin.so");asset(context,base,"x11-audio.sh");asset(context,base,"x11-native-desktop.sh");asset(context,base,"x11-native-programs.py");asset(context,base,"x11-firefox-dns.py");
        asset(context,base,"x11-gpu-tools.py");asset(context,base,"x11-gpu-test.py");asset(context,base,"x11-gpu-test-setup.py");asset(context,base,"x11-bookmarks.py");asset(context,base,"x11-wallpaper.py");asset(context,base,"x11-default-wallpaper.png");asset(context,base,"x11-launchers.py");asset(context,base,"x11-maintenance.py");
        if(target.equals("opensuse")||target.equals("fedora")){asset(context,base,"x11-openh264.py");asset(context,base,"x11-openh264.so");if(target.equals("fedora"))asset(context,base,"x11-gst-openh264.so");}
        if(target.equals("termux")&&operation.equals("run")){
            // Own old control copies only; never follow a link or scan user home.
            for(String retired:new String[]{"x11-touch-style.zip","x11-touch-workspace"}){
                File file=new File(base,retired);
                if(Files.isRegularFile(file.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS))Files.delete(file.toPath());
            }
        }
        if(supportsStyle(target)&&operation.equals("run")){asset(context,base,"x11-style.py");asset(context,base,"x11-scale.py");asset(context,base,"x11-scale-live.py");}
        String bin=TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
        if(!new File(bin,"setsid").canExecute())throw new IOException("setsid is required (Termux util-linux)");
        java.util.List<String> command=new java.util.ArrayList<>(java.util.Arrays.asList(bin+"/setsid",bin+"/bash","--noprofile","--norc",guardian.getPath()));
        if(target.equals("archlinux")&&operation.equals("run"))command.addAll(java.util.Arrays.asList(bin+"/flock","--no-fork","--nonblocking","--conflict-exit-code","75",new File(base,"desktop-session.lock").getPath()));
        command.addAll(java.util.Arrays.asList(bin+"/bash","--noprofile","--norc",script.getPath(),operation,target,base.getPath(),desktop));
        ProcessBuilder builder=new ProcessBuilder(command);
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        builder.environment().put("JOBS_X11_STYLE",termuxStyle?"1":"0");builder.environment().put("JOBS_X11_SCALE",Integer.toString(interfaceScale));builder.environment().put("JOBS_X11_RENDERER",renderer);builder.environment().put("JOBS_X11_GARCON_FILTER","0");builder.environment().put("JOBS_X11_DISPLAY",Integer.toString(X11Environment.forTarget(target).display));
        if(focusToken!=null)builder.environment().put("JOBS_X11_FOCUS_TOKEN",focusToken);
        builder.redirectErrorStream(true);return new DesktopProcess(builder.start());
    }
    static DesktopProcess extras(Context context,File base)throws Exception{
        File script=asset(context,base,"x11-extras.py"),guardian=asset(context,base,"x11-guardian.sh");
        String bin=TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
        if(!new File(bin,"setsid").canExecute())throw new IOException("setsid is required (Termux util-linux)");
        ProcessBuilder builder=new ProcessBuilder(bin+"/setsid",bin+"/bash","--noprofile","--norc",guardian.getPath(),bin+"/python3","-B",script.getPath(),"kali-style",base.getPath());
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        builder.redirectErrorStream(true);return new DesktopProcess(builder.start());
    }
    static DesktopProcess scale(Context context,File base,String target,String token,int percent)throws Exception{
        X11ScreenSettings.validateScale(percent);if(!supported(target)||!token.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Scale scope");
        for(String name:new String[]{"x11-desktop.sh","x11-guardian.sh","x11-scale-live.py","x11-scale.py","x11-style.py","x11-focus.py"})if(!Files.isRegularFile(new File(base,name).toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS))throw new IOException("Scale runtime unavailable");
        String bin=TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
        ProcessBuilder b=new ProcessBuilder(bin+"/setsid",bin+"/bash","--noprofile","--norc",new File(base,"x11-guardian.sh").getPath(),bin+"/bash","--noprofile","--norc",new File(base,"x11-desktop.sh").getPath(),"scale",target,base.getPath(),"xfce");
        b.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));b.environment().clear();for(String value:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=value.indexOf('=');if(eq>0)b.environment().put(value.substring(0,eq),value.substring(eq+1));}
        b.environment().put("JOBS_X11_FOCUS_TOKEN",token);b.environment().put("JOBS_X11_SCALE",Integer.toString(percent));b.redirectErrorStream(true);return new DesktopProcess(b.start());
    }
    static DesktopProcess programs(Context context,File base,String operation,String target,String program)throws Exception{
        if(!supported(target)||!(operation.equals("check")||operation.equals("install")||operation.equals("resume")||operation.equals("catalog")||operation.equals("catalog-install")||operation.equals("installed")||operation.equals("remove")||operation.equals("firefox-compat-on")||operation.equals("firefox-compat-off")||operation.equals("firefox-dns-on")||operation.equals("firefox-dns-off"))||(!program.isEmpty()&&!DesktopPrograms.requestId(program))||((operation.equals("install")||operation.equals("resume"))&&program.isEmpty()))throw new IllegalArgumentException("Unsupported desktop program");
        File script=asset(context,base,"x11-programs.sh"),guardian=asset(context,base,"x11-guardian.sh");asset(context,base,"x11-gpu-tools.py");asset(context,base,"x11-gpu-test.py");asset(context,base,"x11-gpu-test-setup.py");asset(context,base,"x11-catalog.py");asset(context,base,"x11-firefox.py");asset(context,base,"mozilla-repo.asc");asset(context,base,"x11-maintenance.py");asset(context,base,"x11-native-programs.py");asset(context,base,"x11-firefox-dns.py");asset(context,base,"x11-launchers.py");
        if(target.equals("opensuse")||target.equals("fedora")){asset(context,base,"x11-openh264.py");asset(context,base,"x11-openh264.so");if(target.equals("fedora"))asset(context,base,"x11-gst-openh264.so");}
        String bin=TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
        if(!new File(bin,"setsid").canExecute())throw new IOException("setsid is required (Termux util-linux)");
        ProcessBuilder builder=new ProcessBuilder(bin+"/setsid",bin+"/bash","--noprofile","--norc",guardian.getPath(),bin+"/bash","--noprofile","--norc",script.getPath(),operation,target,base.getPath(),program,X11Text.tr(context,"ru","en"));
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        builder.redirectErrorStream(true);return new DesktopProcess(builder.start());
    }
    static DesktopProcess virgl(Context context,File base,String target)throws Exception{
        if(!VirglInstaller.supported(target))throw new IllegalArgumentException("Unsupported VirGL target");
        File script=asset(context,base,"x11-virgl-setup.sh"),guardian=asset(context,base,"x11-guardian.sh");
        if(target.equals("alpine")){asset(context,base,"x11-virgl-client.py");asset(context,base,"x11-extras.py");}
        String bin=TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
        if(!new File(bin,"setsid").canExecute())throw new IOException("setsid is required (Termux util-linux)");
        ProcessBuilder builder=new ProcessBuilder(bin+"/setsid",bin+"/bash","--noprofile","--norc",guardian.getPath(),bin+"/bash","--noprofile","--norc",script.getPath(),"install",target,base.getPath());
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        builder.redirectErrorStream(true);return new DesktopProcess(builder.start());
    }
    void read(Consumer<String> output)throws IOException{
        try(Reader in=new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8)){
            char[] chars=new char[2048];int n;while((n=in.read(chars))!=-1)output.accept(new String(chars,0,n));
        }
    }
    int waitFor()throws InterruptedException{return process.waitFor();}
    @Override public void close(){try{process.getOutputStream().close();}catch(IOException ignored){} }
    /** EOF cancels only our guardian group; do not release the worker until it has exited. */
    void closeAndWait(){
        close();boolean interrupted=false;
        while(true){try{waitFor();break;}catch(InterruptedException error){interrupted=true;}}
        if(interrupted)Thread.currentThread().interrupt();
    }
    private static File asset(Context context,File base,String name)throws Exception{
        File file=new File(base,name);if(Files.isSymbolicLink(file.toPath()))throw new IOException("Desktop script is a link");
        try(InputStream in=context.getAssets().open(name);OutputStream out=new FileOutputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
        return file;
    }
}
