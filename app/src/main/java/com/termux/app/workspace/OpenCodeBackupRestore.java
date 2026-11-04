package com.termux.app.workspace;

import android.app.Activity;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

/** Copies verified staged connections; never merges into a pre-existing connection. */
final class OpenCodeBackupRestore implements AutoCloseable {
    private static final Set<String> PARTS=new HashSet<>(Arrays.asList("config","data","state","session-links","receipts"));
    private static final class Copy {String id;File temporary,target;boolean published;}
    private final Map<String,Copy> copies=new LinkedHashMap<>();
    private boolean committed;
    private int files;
    private long bytes;
    String stage(Activity activity,JSONObject restored,String original)throws Exception{
        JSONObject sources=restored.optJSONObject("opencodeConnections");
        if(sources==null||!sources.has(original))return UUID.randomUUID().toString(); // Old archive/profile-only import.
        if(copies.containsKey(original))return copies.get(original).id;
        if(!original.equals(UUID.fromString(original).toString()))throw new IOException("INVALID_OPENCODE_CONNECTION");
        File base=new File(restored.getString("path")).getCanonicalFile();
        File source=new File(sources.getString(original));
        if(!source.getAbsolutePath().startsWith(base.getPath()+"/")||!source.getCanonicalPath().startsWith(base.getPath()+"/")||!source.getName().equals(original))throw new IOException("INVALID_OPENCODE_SOURCE");
        String relative=source.getAbsolutePath().substring(base.getAbsolutePath().length()+1);
        File cursor=base;for(String part:relative.split("/")){if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IOException("INVALID_OPENCODE_SOURCE");cursor=new File(cursor,part);if(WorkspaceBackupEngine.symlink(cursor)||!cursor.isDirectory())throw new IOException("INVALID_OPENCODE_SOURCE");}
        File control=directory(activity.getFilesDir(),".ai-control"),connections=directory(control,"connections");
        Copy copy=new Copy();copy.id=UUID.randomUUID().toString();copy.temporary=new File(connections,".backup-"+copy.id);copy.target=new File(connections,copy.id);
        if(copy.target.exists()||WorkspaceBackupEngine.symlink(copy.target)||!copy.temporary.mkdir())throw new IOException("OPENCODE_RESTORE_COLLISION");
        copies.put(original,copy);protect(copy.temporary,true);
        for(File child:children(source)){if(!PARTS.contains(child.getName())||!child.isDirectory()||WorkspaceBackupEngine.symlink(child))throw new IOException("INVALID_OPENCODE_SOURCE");copy(child,new File(copy.temporary,child.getName()),source.getCanonicalPath());}
        return copy.id;
    }
    private static File directory(File parent,String name)throws IOException{
        File result=new File(parent,name);
        if(WorkspaceBackupEngine.symlink(result))throw new IOException("UNSAFE_OPENCODE_TARGET");
        if(!result.exists()){if(!result.mkdir())throw new IOException("OPENCODE_RESTORE_DIRECTORY");protect(result,true);}
        if(!result.isDirectory())throw new IOException("UNSAFE_OPENCODE_TARGET");
        return result;
    }
    private static File[] children(File path)throws IOException{File[] result=path.listFiles();if(result==null)throw new IOException("OPENCODE_SOURCE_UNREADABLE");return result;}
    private static void protect(File file,boolean directory)throws IOException{
        if(!file.setReadable(false,false)||!file.setWritable(false,false)||!file.setExecutable(false,false)||!file.setReadable(true,true)||!file.setWritable(true,true)||(directory&&!file.setExecutable(true,true)))throw new IOException("OPENCODE_RESTORE_PERMISSIONS");
    }
    private void copy(File source,File target,String root)throws IOException{
        if(++files>500000||WorkspaceBackupEngine.symlink(source)||!source.getCanonicalPath().startsWith(root+"/"))throw new IOException("INVALID_OPENCODE_SOURCE");
        if(source.isDirectory()){if(!target.mkdir())throw new IOException("OPENCODE_RESTORE_DIRECTORY");protect(target,true);for(File child:children(source))copy(child,new File(target,child.getName()),root);return;}
        if(!source.isFile())throw new IOException("INVALID_OPENCODE_SOURCE");
        long size=source.length();bytes+=size;if(bytes>512L*1024*1024*1024)throw new IOException("OPENCODE_RESTORE_TOO_LARGE");
        try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(target)){byte[] buffer=new byte[65536];int n;long total=0;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);total+=n;if(total>size)throw new IOException("OPENCODE_SOURCE_CHANGED");}if(total!=size)throw new IOException("OPENCODE_SOURCE_CHANGED");}
        protect(target,false);if(source.canExecute()&&!target.setExecutable(true,true))throw new IOException("OPENCODE_RESTORE_PERMISSIONS");
    }
    void publish()throws IOException{for(Copy copy:copies.values()){if(copy.target.exists()||WorkspaceBackupEngine.symlink(copy.target)||!copy.temporary.renameTo(copy.target))throw new IOException("OPENCODE_RESTORE_COLLISION");copy.published=true;}}
    void commit(){committed=true;}
    private static void removeOwned(File file){if(!WorkspaceBackupEngine.symlink(file)&&file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File child:children)removeOwned(child);}file.delete();}
    @Override public void close(){if(!committed)for(Copy copy:copies.values())removeOwned(copy.published?copy.target:copy.temporary);copies.clear();}
}
