package com.termux.x11;

import android.content.Context;
import java.io.*;
import java.nio.file.Files;
import java.util.function.Consumer;

/** One consented renderer setup, bounded by the same pipe-owned guardian as X11. */
public final class VirglInstaller implements AutoCloseable {
    private final DesktopProcess child;
    private final File directory;
    private VirglInstaller(DesktopProcess child,File directory){this.child=child;this.directory=directory;}
    public static boolean supported(String target){return target!=null&&java.util.Arrays.asList("termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux").contains(target);}
    public static boolean installed(Context context,String target){
        if(!supported(target))return false;
        File bin=new File(com.termux.shared.termux.TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"virgl_test_server_android");
        File marker=new File(context.getFilesDir(),"workspace-x11-virgl-"+target+"/ready");
        File angle=new File(com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH,"opt/angle-android/"+((target.equals("void")||target.equals("fedora")||target.equals("debian")||target.equals("archlinux")||target.equals("termux"))?"vulkan":"gl")+"/libEGL_angle.so");
        try{return bin.canExecute()&&angle.isFile()&&!Files.isSymbolicLink(marker.toPath())&&marker.isFile()&&marker.length()<128&&new String(Files.readAllBytes(marker.toPath()),java.nio.charset.StandardCharsets.UTF_8).equals("virgl-android-v2\n");}catch(IOException error){return false;}
    }
    public static VirglInstaller start(Context context,String target)throws Exception{
        if(!supported(target))throw new IllegalArgumentException("Unsupported VirGL target");
        File parent=new File(context.getFilesDir(),"workspace-x11-virgl-"+target);
        if(Files.isSymbolicLink(parent.toPath())||(!parent.isDirectory()&&!parent.mkdirs()))throw new IOException("Unsafe VirGL control directory");
        File directory=Files.createTempDirectory(parent.toPath(),"job-").toFile();
        try{return new VirglInstaller(DesktopProcess.virgl(context,directory,target),directory);}
        catch(Exception error){for(String name:new String[]{"x11-guardian.sh","x11-virgl-setup.sh","x11-virgl-client.py","x11-extras.py","x11-virgl-alpine.zip"})new File(directory,name).delete();directory.delete();throw error;}
    }
    public void read(Consumer<String> output)throws IOException{child.read(output);}
    public int waitFor()throws InterruptedException{return child.waitFor();}
    public void markReady()throws IOException{
        File marker=new File(directory.getParentFile(),"ready");
        if(Files.isSymbolicLink(marker.toPath()))throw new IOException("VirGL marker is a link");
        try(FileOutputStream out=new FileOutputStream(marker)){out.write("virgl-android-v2\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}
    }
    @Override public void close(){child.close();}
    public void cleanup(){
        // Assets only, no recursively followed links. Call after waitFor.
        for(String name:new String[]{"x11-guardian.sh","x11-virgl-setup.sh","x11-virgl-client.py","x11-extras.py","x11-virgl-alpine.zip"})new File(directory,name).delete();directory.delete();
    }
}
