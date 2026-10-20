package com.termux.app.workspace;

import org.json.*;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.termux.shared.termux.TermuxConstants;

/** Observed installed-list changes, shared by presets using the same concrete rootfs. */
final class WorkspacePackageHistory {
    static final int LIMIT=300,MAX_BYTES=2*1024*1024;
    static String key(String scope,String root,long device,long inode)throws Exception{
        byte[] hash=MessageDigest.getInstance("SHA-256").digest((scope+"\n"+root+"\n"+device+":"+inode).getBytes(StandardCharsets.UTF_8));StringBuilder value=new StringBuilder();for(byte b:hash)value.append(String.format(Locale.ROOT,"%02x",b&255));return value.toString();
    }
    static String key(WorkspaceStore.Session profile)throws Exception{
        String scope=WorkspaceStorageCleanup.scope(profile);if(scope.isEmpty())throw new IOException();
        File root=scope.equals("termux")?new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH):WorkspaceEnvironments.root(new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"),scope);
        if(root==null)throw new IOException();root=root.getCanonicalFile();android.system.StructStat st=android.system.Os.lstat(root.getPath());
        return key(scope,root.getPath(),st.st_dev,st.st_ino);
    }
    private static Path file(File files,String key)throws Exception{
        if(!key.matches("[a-f0-9]{64}"))throw new IOException();Path parent=files.getCanonicalFile().toPath();Path dir=parent.resolve("workspace-package-history");
        if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(dir);
        if(Files.isSymbolicLink(dir)||!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS)||!dir.toRealPath().equals(dir))throw new IOException();
        return dir.resolve(key+".json");
    }
    private static JSONObject load(Path file)throws Exception{
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))return new JSONObject().put("schema",1).put("baseline",false).put("packages",new JSONObject()).put("events",new JSONArray());
        if(Files.size(file)>MAX_BYTES)throw new IOException();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(SeekableByteChannel in=Files.newByteChannel(file,StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS)){
            ByteBuffer buffer=ByteBuffer.allocate(8192);int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>MAX_BYTES)throw new IOException();bytes.write(buffer.array(),0,n);buffer.clear();}
        }
        JSONObject data=new JSONObject(bytes.toString("UTF-8"));if(data.optInt("schema")!=1||data.optJSONObject("packages")==null||data.optJSONArray("events")==null||data.getJSONArray("events").length()>LIMIT)throw new IOException();return data;
    }
    private static void save(Path target,JSONObject data)throws Exception{
        byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException();Path temp=target.resolveSibling(UUID.randomUUID()+".tmp");
        try{
            try(SeekableByteChannel out=Files.newByteChannel(temp,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);}
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
    static synchronized void observe(File files,String key,List<PackageCatalog.Entry> installed,long now)throws Exception{
        Path path=file(files,key);JSONObject old=load(path),previous=old.getJSONObject("packages"),current=new JSONObject();TreeSet<String> names=new TreeSet<>();
        for(PackageCatalog.Entry entry:installed){if(!EnvironmentPackages.validName(entry.name)||entry.version==null||entry.version.length()>256)throw new IOException();current.put(entry.name,entry.version);names.add(entry.name);}
        Iterator<String> it=previous.keys();while(it.hasNext())names.add(it.next());JSONArray events=old.getJSONArray("events");
        if(old.optBoolean("baseline"))for(String name:names){
            String before=previous.has(name)?previous.getString(name):null,after=current.has(name)?current.getString(name):null;
            if(Objects.equals(before,after))continue;
            boolean combined=false;
            for(int i=events.length()-1;i>=0;i--){JSONObject event=events.getJSONObject(i);if(name.equals(event.optString("name"))&&event.optString("phase").equals("success")&&event.optLong("observed")>=old.optLong("snapshotAt")){event.put("before",before==null?"":before).put("after",after==null?"":after).put("reconciled",true);combined=true;break;}}
            if(!combined)events.put(new JSONObject().put("name",name).put("action",before==null?"installed":after==null?"removed":"changed").put("before",before==null?"":before).put("after",after==null?"":after).put("observed",now));
        }
        JSONArray trimmed=new JSONArray();for(int i=Math.max(0,events.length()-LIMIT);i<events.length();i++)trimmed.put(events.get(i));
        save(path,new JSONObject().put("schema",1).put("baseline",true).put("snapshotAt",now).put("packages",current).put("events",trimmed));
    }
    static synchronized void operation(File files,String key,JSONObject metadata,Integer exit,long now)throws Exception{
        Path path=file(files,key);JSONObject data=load(path);JSONArray events=data.getJSONArray("events");
        for(int i=0;i<events.length();i++)if(metadata.getString("id").equals(events.getJSONObject(i).optString("operationId")))return;
        events.put(new JSONObject().put("operationId",metadata.getString("id")).put("name",metadata.getString("name")).put("action","operation").put("operation",metadata.getString("operation")).put("after",metadata.optString("version")).put("observed",now).put("exit",exit==null?-1:exit).put("phase",exit==null?"unknown":exit==0?"success":exit==130?"cancelled":"failure"));
        JSONArray trimmed=new JSONArray();for(int i=Math.max(0,events.length()-LIMIT);i<events.length();i++)trimmed.put(events.get(i));data.put("events",trimmed);save(path,data);
    }
    static JSONArray merge(JSONArray finished,JSONArray pending)throws Exception{
        List<JSONObject> rows=new ArrayList<>();for(JSONArray source:new JSONArray[]{finished,pending})for(int i=0;i<source.length();i++)rows.add(source.getJSONObject(i));rows.sort(Comparator.comparingLong(v->v.optLong("observed")));
        JSONArray result=new JSONArray();for(int i=Math.max(0,rows.size()-LIMIT);i<rows.size();i++)result.put(rows.get(i));return result;
    }
    static synchronized JSONArray read(File files,String key)throws Exception{return load(file(files,key)).getJSONArray("events");}
}
