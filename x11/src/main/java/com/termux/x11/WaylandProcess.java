package com.termux.x11;
import android.content.Context;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.file.Files;
/** The existing Arch rootfs; EOF of the guardian pipe owns only this process group. */
final class WaylandProcess {
    static DesktopProcess start(Context context,File base,String operation,String token,WaylandRuntime runtime)throws Exception{
        if(!DesktopProfiles.waylandEnabled())throw new IllegalStateException("Wayland preview is disabled in this build");
        if(!X11Environment.context(context).target.equals("archlinux")||!operation.matches("check|install|run"))throw new IllegalArgumentException("Wayland operation");
        if(operation.equals("run")&&(runtime==null||token==null||!token.matches("[a-f0-9-]{36}")))throw new IllegalArgumentException("Wayland ownership");
        for(String name:new String[]{"wayland-desktop.sh","wayland-components.sh","wayland-root.py","wayland-session.sh","wayland-ready.py","wayland-guest.so","x11-audio.sh","x11-gpu.py","x11-gpu-env.sh","x11-guardian.sh"}){
            File file=new File(base,name);if(Files.isSymbolicLink(file.toPath()))throw new IOException("Wayland script is a link");
            try(InputStream in=context.getAssets().open(name);OutputStream out=new FileOutputStream(file)){byte[] b=new byte[16384];int n;while((n=in.read(b))>=0)out.write(b,0,n);}
        }
        String bin=TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
        if(!new File(bin,"setsid").canExecute())throw new IOException("Termux util-linux/setsid is required");
        java.util.List<String> command=new java.util.ArrayList<>(java.util.Arrays.asList(bin+"/setsid",bin+"/bash","--noprofile","--norc",new File(base,"x11-guardian.sh").getPath()));
        if(operation.equals("run"))command.addAll(java.util.Arrays.asList(bin+"/flock","--no-fork","--nonblocking","--conflict-exit-code","75",new File(base,"desktop-session.lock").getPath()));
        command.addAll(java.util.Arrays.asList(bin+"/bash","--noprofile","--norc",new File(base,"wayland-desktop.sh").getPath(),operation,"archlinux",base.getPath()));
        ProcessBuilder pb=new ProcessBuilder(command);
        pb.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));pb.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(context,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)pb.environment().put(item.substring(0,eq),item.substring(eq+1));}
        if(token!=null)pb.environment().put("JOBS_X11_FOCUS_TOKEN",token);
        if(runtime!=null)pb.environment().put("JOBS_WAYLAND_RUNTIME",runtime.runtime.getPath());
        pb.redirectErrorStream(true);return new DesktopProcess(pb.start());
    }
}
