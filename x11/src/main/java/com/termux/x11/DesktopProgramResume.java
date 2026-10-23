package com.termux.x11;

import android.os.Bundle;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Per-environment consent journal. Contains identifiers only, never shell commands. */
final class DesktopProgramResume {
    private final File directory;
    private final java.util.Map<String,Bundle> memory=new java.util.HashMap<>();
    DesktopProgramResume(File directory){this.directory=directory;}
    private File file(String target)throws IOException{
        if(!DesktopPrograms.supportsTarget(target))throw new IOException("Invalid program resume target");
        if(Files.isSymbolicLink(directory.toPath()))throw new IOException("Program resume directory is a link");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Program resume directory unavailable");
        File file=new File(directory,target+".json");
        for(String suffix:new String[]{"",".new",".bak"})if(Files.isSymbolicLink(new File(file.getPath()+suffix).toPath()))throw new IOException("Program resume file is a link");
        return file;
    }
    synchronized Bundle read(String target){
        if(!DesktopPrograms.supportsTarget(target))return new Bundle();
        Bundle saved=memory.get(target);if(saved==null){saved=load(target);memory.put(target,saved);}
        return new Bundle(saved);
    }
    private Bundle load(String target){
        Bundle result=new Bundle();
        try(FileInputStream in=new AtomicFile(file(target)).openRead()){
            if(in.getChannel().size()>32768)return result;
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[1024];int n;
            while((n=in.read(bytes))!=-1){out.write(bytes,0,n);if(out.size()>32768)return new Bundle();}
            JSONObject data=new JSONObject(out.toString("UTF-8"));if(data.length()>128)return result;
            java.util.Iterator<String> keys=data.keys();while(keys.hasNext()){String key=keys.next();Object value=data.get(key);if(!DesktopPrograms.requestId(key)||key.isEmpty()||!(value instanceof Boolean))return new Bundle();result.putBoolean(key,(Boolean)value);}
        }catch(Exception ignored){}return result;
    }
    synchronized void put(String target,String id,Boolean prepared){
        if(id.isEmpty()||!DesktopPrograms.requestId(id))throw new IllegalArgumentException("Invalid program resume id");
        AtomicFile atomic=null;FileOutputStream stream=null;
        try{
            Bundle pending=read(target);if(prepared==null)pending.remove(id);else pending.putBoolean(id,prepared);
            if(pending.size()>128)throw new IOException("Too many interrupted program installations");
            JSONObject data=new JSONObject();for(String key:pending.keySet())data.put(key,pending.getBoolean(key));
            atomic=new AtomicFile(file(target));stream=atomic.startWrite();stream.write(data.toString().getBytes(StandardCharsets.UTF_8));atomic.finishWrite(stream);stream=null;
            memory.put(target,new Bundle(pending));
        }catch(Exception error){if(atomic!=null&&stream!=null)atomic.failWrite(stream);throw new IllegalStateException("Cannot save program installation state",error);}
    }
}
