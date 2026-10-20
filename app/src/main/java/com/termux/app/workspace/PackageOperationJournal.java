package com.termux.app.workspace;

import org.json.*;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exit-code receipts in an existing session bind; no PTY parsing, output capture or polling. */
final class PackageOperationJournal {
    static final class Intent {
        final String action,name,version;
        Intent(String action,String name,String version){this.action=action;this.name=name;this.version=version;}
    }
    private static boolean sessionId(String value){return value!=null&&value.matches("[A-Za-z0-9][A-Za-z0-9-]{0,79}");}
    private static Path dir(File files)throws Exception{
        Path parent=files.getCanonicalFile().toPath(),dir=parent.resolve("workspace-package-jobs");
        if(!Files.exists(dir,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(dir);
        if(Files.isSymbolicLink(dir)||!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS)||!dir.toRealPath().equals(dir))throw new IOException();return dir;
    }
    private static byte[] read(Path file,int max)throws Exception{
        if(Files.size(file)>max)throw new IOException();ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(SeekableByteChannel in=Files.newByteChannel(file,StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS)){ByteBuffer buffer=ByteBuffer.allocate(1024);int n;while((n=in.read(buffer))!=-1){if(out.size()+n>max)throw new IOException();out.write(buffer.array(),0,n);buffer.clear();}}return out.toByteArray();
    }
    private static File sessionDirectory(File files,String id,boolean create)throws Exception{
        Path root=files.getCanonicalFile().toPath().resolve("workspace-sessions");
        if(create&&!Files.exists(root,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(root);
        if(Files.isSymbolicLink(root)||!root.toFile().getCanonicalFile().toPath().equals(root))throw new IOException();
        Path session=root.resolve(id);if(create&&!Files.exists(session,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(session);
        if(Files.isSymbolicLink(session)||!session.toFile().getCanonicalFile().toPath().equals(session))throw new IOException();return session.toFile();
    }
    static String wrap(String command,String result){
        return "(\n"+command+"\n_jobs_package_exit=$?\n( umask 077; set -C; printf '%s\\n' \"$_jobs_package_exit\" > "+SessionShell.quote(result)+" ) 2>/dev/null || :\nexit \"$_jobs_package_exit\"\n)";
    }
    static synchronized String prepare(File files,String historyKey,String id,boolean guest,Intent intent,String command)throws Exception{
        if(!sessionId(id)||!historyKey.matches("[a-f0-9]{64}")||intent==null||!EnvironmentPackages.validName(intent.name)||!Arrays.asList("install","upgrade","remove","version").contains(intent.action))throw new IOException();
        File session=sessionDirectory(files,id,true);
        String token=UUID.randomUUID().toString().replace("-","");String result="package-result-"+token;
        JSONObject value=new JSONObject().put("id",token).put("key",historyKey).put("session",id).put("name",intent.name).put("operation",intent.action).put("version",intent.version).put("started",System.currentTimeMillis());
        Path path=dir(files).resolve(token+".json");byte[] data=value.toString().getBytes(StandardCharsets.UTF_8);
        try(SeekableByteChannel out=Files.newByteChannel(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)){ByteBuffer buffer=ByteBuffer.wrap(data);while(buffer.hasRemaining())out.write(buffer);}
        return wrap(command,guest?"/tmp/termux-fork-session-"+id+"/"+result:new File(session,result).getAbsolutePath());
    }
    private static JSONObject metadata(Path path)throws Exception{
        JSONObject value=new JSONObject(new String(read(path,16384),StandardCharsets.UTF_8));
        if(!path.getFileName().toString().equals(value.getString("id")+".json")||!value.getString("id").matches("[a-f0-9]{32}")||!value.getString("key").matches("[a-f0-9]{64}")||!sessionId(value.getString("session"))||!EnvironmentPackages.validName(value.getString("name")))throw new IOException();
        if(!Arrays.asList("install","upgrade","remove","version").contains(value.getString("operation")))throw new IOException();return value;
    }
    static synchronized void collect(File files,String closingSession)throws Exception{
        try(DirectoryStream<Path> paths=Files.newDirectoryStream(dir(files),"*.json")){
            int count=0;
            for(Path path:paths){if(++count>1000)break;
                try{
                    JSONObject meta=metadata(path);Path result=sessionDirectory(files,meta.getString("session"),false).toPath().resolve("package-result-"+meta.getString("id"));
                    Integer exit=null;
                    if(Files.exists(result,LinkOption.NOFOLLOW_LINKS)){
                        String code=new String(read(result,16),StandardCharsets.US_ASCII).trim();if(code.isEmpty())continue;
                        if(!code.matches("[0-9]{1,3}")||Integer.parseInt(code)>255)throw new IOException();exit=Integer.parseInt(code);
                    }
                    if(exit==null&&!meta.getString("session").equals(closingSession))continue;
                    WorkspacePackageHistory.operation(files,meta.getString("key"),meta,exit,System.currentTimeMillis());
                    Files.delete(path);if(exit!=null)Files.deleteIfExists(result);
                }catch(Exception ignored){/* Leave the receipt for a later retry; never read a linked result. */}
            }
        }
    }
    static synchronized JSONArray pending(File files,String key)throws Exception{
        JSONArray result=new JSONArray();try(DirectoryStream<Path> paths=Files.newDirectoryStream(dir(files),"*.json")){
            for(Path path:paths){if(result.length()>=WorkspacePackageHistory.LIMIT)break;try{JSONObject meta=metadata(path);if(!meta.getString("key").equals(key))continue;result.put(new JSONObject().put("name",meta.getString("name")).put("action","operation").put("operation",meta.getString("operation")).put("phase","pending").put("after",meta.optString("version")).put("observed",meta.getLong("started")));}catch(Exception ignored){}}
        }return result;
    }
}
