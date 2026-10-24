package com.termux.x11;

import android.content.Context;
import android.os.Build;
import android.system.Os;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Private app data only. Never installs packages or writes into an existing Linux rootfs. */
public final class X11Runtime {
    public static final int DISPLAY=97, DISCOVERY_PORT=7997;
    final File base,tmp,keyboard,authority;
    final byte[] cookie;
    final X11Environment environment;
    private X11Runtime(File base,File tmp,File keyboard,File authority,byte[] cookie,X11Environment environment){this.environment=environment;this.base=base;this.tmp=tmp;this.keyboard=keyboard;this.authority=authority;this.cookie=cookie;}
    public static boolean expectedProcess(String process,String packageName){return (packageName+":jobs_x11").equals(process);}
    static File target(File root,String name) throws IOException {
        if(name.isEmpty()||name.startsWith("/")||name.indexOf('\\')>=0)throw new IOException("Unsafe keyboard path");
        for(String part:name.split("/"))if(part.equals(".."))throw new IOException("Unsafe keyboard path");
        File f=new File(root,name).getCanonicalFile();if(!f.getPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("Unsafe keyboard path");return f;
    }
    static String digest(byte[] bytes) throws Exception {StringBuilder value=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return value.toString();}
    static File controlDirectory(Context context)throws Exception{File base=new File(context.getFilesDir(),X11Environment.context(context).directory);privateDirectory(base);return base;}
    static X11Runtime prepare(Context context) throws Exception {
        X11Environment environment=X11Environment.context(context);
        File base=controlDirectory(context);
        File tmp=new File(base,"tmp");privateDirectory(tmp);
        File keyboard=new File(base,"keyboard-"+BuildConfig.KEYBOARD_SHA256.substring(0,16));
        if(Files.isSymbolicLink(keyboard.toPath()))throw new IOException("Keyboard directory is a link");
        if(!new File(keyboard,".complete").isFile()) {
            File stage=new File(base,"extract-"+java.util.UUID.randomUUID());privateDirectory(stage);
            try {
                ByteArrayOutputStream packed=new ByteArrayOutputStream();byte[] buffer=new byte[16384];
                try(InputStream in=context.getAssets().open("x11-keyboard.zip")){int count;while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();packed.write(buffer,0,count);if(packed.size()>2*1024*1024)throw new IOException("Keyboard archive too large");}}
                byte[] bytes=packed.toByteArray();if(!digest(bytes).equals(BuildConfig.KEYBOARD_SHA256))throw new IOException("Keyboard archive checksum mismatch");
                int count=0,total=0;HashSet<String> seen=new HashSet<>();
                try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;while((entry=zip.getNextEntry())!=null){
                    if(entry.isDirectory())continue;
                    if(++count>512||!seen.add(entry.getName()))throw new IOException("Invalid keyboard archive");
                    File f=target(stage,entry.getName());if(!f.getParentFile().isDirectory()&&!f.getParentFile().mkdirs())throw new IOException("Cannot create keyboard directory");
                    try(OutputStream out=new FileOutputStream(f)){int n;while((n=zip.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();total+=n;if(total>16*1024*1024)throw new IOException("Keyboard data too large");out.write(buffer,0,n);}}
                }}
                if(!new File(stage,"rules/base").isFile()||!new File(stage,"keycodes/evdev").isFile()||!new File(stage,"symbols/us").isFile())throw new IOException("Incomplete keyboard data");
                try(FileOutputStream marker=new FileOutputStream(new File(stage,".complete"))){marker.write(BuildConfig.KEYBOARD_SHA256.getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
                if(keyboard.exists())throw new IOException("Incomplete keyboard directory; no existing data will be replaced");
                if(!stage.renameTo(keyboard))throw new IOException("Cannot publish keyboard data");
            } finally { if(stage.exists())removeStage(stage); }
        }
        byte[] cookie=new byte[16];new SecureRandom().nextBytes(cookie);
        File authority=new File(base,"authority");if(Files.isSymbolicLink(authority.toPath()))throw new IOException("Authority file is a link");
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(authority))){out.writeShort(65535);field(out,new byte[0]);field(out,Integer.toString(environment.display).getBytes(java.nio.charset.StandardCharsets.US_ASCII));field(out,"MIT-MAGIC-COOKIE-1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));field(out,cookie);}
        Os.chmod(authority.getPath(),0600);
        return new X11Runtime(base,tmp,keyboard,authority,cookie,environment);
    }
    private static void field(DataOutputStream out,byte[] bytes)throws IOException{out.writeShort(bytes.length);out.write(bytes);}
    private static void privateDirectory(File dir)throws Exception{if(Files.isSymbolicLink(dir.toPath()))throw new IOException("Runtime directory is a link");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create X11 runtime");Os.chmod(dir.getPath(),0700);}
    private static void removeStage(File f)throws IOException{if(f.isDirectory()&&!Files.isSymbolicLink(f.toPath())){File[] children=f.listFiles();if(children!=null)for(File child:children)removeStage(child);}if(!f.delete())throw new IOException("Cannot remove temporary keyboard data");}
    String[] arguments(){return arguments("cpu");}
    String[] arguments(String renderer){
        java.util.List<String> args=new java.util.ArrayList<>(java.util.Arrays.asList(":"+environment.display,"-tag","jobs-internal-"+environment.target,"-nolisten","tcp","-auth",authority.getPath(),"-noreset","-retro"));
        // Pinned native GPU Present copies bypass the window clip when no compositor
        // redirects windows. Retain GPU rendering, but use X's correctly clipped copy.
        if(renderer.equals("bionic-glibc")||renderer.equals("wrapper")||renderer.equals("virgl")||renderer.equals("turnip")&&!environment.target.equals("termux"))args.add("-disable-gpu-present");
        return args.toArray(new String[0]);
    }
}
