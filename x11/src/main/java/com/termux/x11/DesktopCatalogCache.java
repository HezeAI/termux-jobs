package com.termux.x11;

import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** One replaceable metadata snapshot per environment/list. No programs, icons or user files. */
final class DesktopCatalogCache {
    private static final int MAX_BYTES=32*1024*1024;
    private final File directory;
    DesktopCatalogCache(File directory){this.directory=directory;}
    private File file(String target,boolean installed)throws IOException{return file(target,installed?"-installed":"-catalog");}
    private File file(String target,String kind)throws IOException{
        if(!DesktopProcess.supported(target))throw new IOException("Unsupported cache environment");
        if(Files.isSymbolicLink(directory.toPath()))throw new IOException("Cache directory is a link");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cache directory unavailable");
        File file=new File(directory,target+kind+".jsonl");
        for(String suffix:new String[]{"",".new",".bak"})if(Files.isSymbolicLink(new File(file.getPath()+suffix).toPath()))throw new IOException("Cache file is a link");
        return file;
    }
    private static int itemsSchema(String target){return target.equals("fedora")?3:2;}
    /** Only complete fixed program statuses, never package commands or credentials. */
    synchronized android.os.Bundle loadItems(String target){
        try(FileInputStream in=new AtomicFile(file(target,target.equals("opensuse")?"-recommended-media2":"-recommended")).openRead()){
            if(in.getChannel().size()>4096)return null;
            byte[] bytes=new byte[4097];int n=0,count;
            while(n<bytes.length&&(count=in.read(bytes,n,bytes.length-n))!=-1)n+=count;
            if(n>4096)return null;
            JSONObject data=new JSONObject(new String(bytes,0,n,StandardCharsets.UTF_8));
            if(data.length()!=DesktopPrograms.IDS.length+1||data.optInt("_schema",-1)!=itemsSchema(target))return null;
            android.os.Bundle items=new android.os.Bundle();
            for(String id:DesktopPrograms.IDS){Object value=data.get(id);if(!(value instanceof Boolean))return null;items.putBoolean(id,(Boolean)value);}
            return items;
        }catch(Exception error){return null;}
    }
    synchronized void saveItems(String target,android.os.Bundle items){
        AtomicFile atomic=null;FileOutputStream stream=null;
        try{
            JSONObject data=new JSONObject();
            if(items!=null){
                if(items.size()!=DesktopPrograms.IDS.length)return;
                for(String id:DesktopPrograms.IDS){Object value=items.get(id);if(!(value instanceof Boolean))return;data.put(id,value);}
                data.put("_schema",itemsSchema(target));
            }
            atomic=new AtomicFile(file(target,target.equals("opensuse")?"-recommended-media2":"-recommended"));stream=atomic.startWrite();
            stream.write(data.toString().getBytes(StandardCharsets.UTF_8));atomic.finishWrite(stream);stream=null;
        }catch(Exception error){if(atomic!=null&&stream!=null)atomic.failWrite(stream);}
    }
    synchronized DesktopCatalog load(String target,boolean installed){
        try{
            AtomicFile atomic=new AtomicFile(file(target,installed));
            try(FileInputStream in=atomic.openRead();BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
                if(in.getChannel().size()>MAX_BYTES+64L)return null;
                // Header is bounded independently of the record parser.
                for(char c:"JOBS_X11_CATALOG_V1\n".toCharArray())if(reader.read()!=c)return null;
                DesktopCatalog.Parser parser=new DesktopCatalog.Parser(installed);char[] chars=new char[2048];int n,total=0;
                while((n=reader.read(chars))!=-1){total+=n;if(total>MAX_BYTES)return null;parser.feed(new String(chars,0,n));}
                return parser.result();
            }
        }catch(IOException|RuntimeException error){return null;}
    }
    synchronized void save(String target,boolean installed,DesktopCatalog data){write(target,installed,data);}
    synchronized void invalidate(String target,boolean installed){write(target,installed,null);}
    private void write(String target,boolean installed,DesktopCatalog data){
        AtomicFile atomic=null;FileOutputStream stream=null;
        try{
            atomic=new AtomicFile(file(target,installed));stream=atomic.startWrite();
            stream.write((data==null?"STALE\n":"JOBS_X11_CATALOG_V1\n").getBytes(StandardCharsets.UTF_8));int total=0;
            if(data!=null){for(DesktopCatalog.Entry entry:data.entries){
                JSONObject row=new JSONObject();row.put("package",entry.pkg);row.put("name",entry.name);row.put("summary",entry.summary);row.put("installed",entry.installed);row.put("available",entry.available);row.put("snap",entry.snap);
                byte[] record=("CATALOG:"+row+"\n").getBytes(StandardCharsets.UTF_8);total+=record.length;if(total>MAX_BYTES)throw new IOException("Catalog cache too large");stream.write(record);
            }stream.write("READY:catalog\nDESKTOP_CHILD_EXIT=0\n".getBytes(StandardCharsets.UTF_8));}
            atomic.finishWrite(stream);stream=null;
        }catch(Exception error){if(atomic!=null&&stream!=null)atomic.failWrite(stream);android.util.Log.w("JobsX11","Catalog cache write failed: "+error.getClass().getSimpleName());}
    }
}
