package com.termux.x11;

import android.content.Context;
import android.system.Os;
import com.droiddeck.launcher.wayland.WaylandCompositor;
import java.io.*;
import java.nio.file.*;
import org.json.*;

/** Same-UID private compositor data. Imported code stays separate from the guest Linux ICD. */
final class WaylandRuntime {
    final File runtime,driver;
    final String library,nativeDirectory;
    private WaylandRuntime(File runtime,File driver,String library,String nativeDirectory){this.runtime=runtime;this.driver=driver;this.library=library;this.nativeDirectory=nativeDirectory;}
    static WaylandRuntime prepare(Context context,File control)throws Exception{
        if(!DesktopProfiles.waylandEnabled())throw new IOException("Wayland preview is disabled in this build");
        if(!X11Environment.context(context).target.equals("archlinux"))throw new IOException("Wayland is Arch-only");
        File base=new File(context.getFilesDir(),"workspace-wayland-driver-archlinux");
        existingDirectory(base);
        File lock=new File(base,"operation.lock");regular(lock,4096);
        if(!WaylandCompositor.nativeLockDriver(lock.getPath()))throw new IOException("Bionic driver library is busy");
        File current=new File(base,"current.json");regular(current,32768);
        JSONObject meta=new JSONObject(new String(Files.readAllBytes(current.toPath()),java.nio.charset.StandardCharsets.UTF_8));
        String id=meta.getString("id"),lib=meta.getString("library");
        if(!id.matches("[a-f0-9]{64}")||!lib.matches("[A-Za-z0-9_][A-Za-z0-9_.+-]{0,127}\\.so"))throw new IOException("Invalid Bionic driver metadata");
        if(meta.optInt("minApi",0)>android.os.Build.VERSION.SDK_INT)throw new IOException("Bionic driver needs a newer Android version");
        File drivers=new File(base,"drivers");existingDirectory(drivers);File driver=new File(drivers,id);existingDirectory(driver);
        JSONObject hashes=meta.getJSONObject("files");if(hashes.length()<1||hashes.length()>16||!hashes.has(lib))throw new IOException("Invalid Bionic file list");
        long total=0;java.util.Iterator<String> keys=hashes.keys();
        while(keys.hasNext()){
            String name=keys.next(),sha=hashes.getString(name);
            if(!name.matches("[A-Za-z0-9_][A-Za-z0-9_.+-]{0,127}\\.so")||!sha.matches("[a-f0-9]{64}"))throw new IOException("Invalid Bionic file identity");
            File file=new File(driver,name);regular(file,192L*1024*1024);total+=file.length();if(total>192L*1024*1024)throw new IOException("Bionic file limit");
            java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
            try(InputStream in=new FileInputStream(file)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))>=0){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();digest.update(buf,0,n);}}
            StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            if(!sha.equals(hex.toString()))throw new IOException("Bionic driver checksum mismatch");
        }
        File runtime=new File(control,"wl-"+java.util.UUID.randomUUID());directory(runtime);
        try(InputStream in=context.getAssets().open("wayland-keymap.xkb");OutputStream out=new FileOutputStream(new File(runtime,"keymap.xkb"))){byte[] buf=new byte[8192];int n,keymapBytes=0;while((n=in.read(buf))>=0){keymapBytes+=n;if(keymapBytes>512*1024)throw new IOException("Wayland keymap limit");out.write(buf,0,n);}}
        if(new File(runtime,"wayland-0").getPath().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>107)throw new IOException("Wayland runtime path too long");
        return new WaylandRuntime(runtime,driver,lib,context.getApplicationInfo().nativeLibraryDir);
    }
    private static void regular(File file,long limit)throws IOException{
        if(!Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)||file.length()>limit)throw new IOException("Invalid private Wayland file");
    }
    private static void existingDirectory(File dir)throws IOException{if(!Files.isDirectory(dir.toPath(),LinkOption.NOFOLLOW_LINKS))throw new IOException("Bionic directory is missing or a link");}
    private static void directory(File dir)throws Exception{
        if(Files.isSymbolicLink(dir.toPath())||!dir.isDirectory()&&!dir.mkdir())throw new IOException("Invalid private Wayland directory");Os.chmod(dir.getPath(),0700);
    }
}
