package com.termux.app.workspace;

import java.io.IOException;
import java.util.*;

/** Read-only bounded tree inventory. Never executes remote shell filenames. */
final class ServerDownloadPlan {
    interface Source {ServerSftp.Info stat(String path)throws Exception;List<ServerSftp.Entry> list(String path)throws Exception;String canonical(String path)throws Exception;}
    interface Progress {void scanned(int entries,String path)throws Exception;}
    static final int MAX_ENTRIES=20000,MAX_DEPTH=64;
    static final long MAX_TOTAL=256L*1024*1024*1024;
    static final class Item {final String remote,relative;final ServerSftp.Info info;Item(String p,String r,ServerSftp.Info i){remote=p;relative=r;info=i;}}
    final List<Item> files=new ArrayList<>();final List<String> directories=new ArrayList<>();long bytes;int scanned,skipped;
    static ServerDownloadPlan scan(Source source,List<String> paths,Progress progress)throws Exception{
        if(paths.isEmpty()||paths.size()>500)throw new IOException("SELECTION_LIMIT");ServerDownloadPlan plan=new ServerDownloadPlan();Set<String> roots=new HashSet<>();
        for(String path:new LinkedHashSet<>(paths)){if(path==null||!path.startsWith("/")||path.indexOf(0)>=0)throw new IOException("INVALID_PATH");String name=new java.io.File(path).getName();if(!ServerSftp.validName(name)||!roots.add(name))throw new IOException("DUPLICATE_OR_UNSAFE_NAME");plan.walk(source,path,name,0,progress);}
        return plan;
    }
    private void walk(Source source,String path,String relative,int depth,Progress progress)throws Exception{
        if(depth>MAX_DEPTH||++scanned>MAX_ENTRIES)throw new IOException("TREE_LIMIT");progress.scanned(scanned,path);ServerSftp.Info info=source.stat(path);
        if(!info.exists)throw new IOException("REMOTE_CHANGED");if(!info.regular()&&!info.directory()){skipped++;return;}
        if(!source.canonical(path).equals(path))throw new IOException("REMOTE_SYMLINK_OR_CHANGED");
        if(info.directory()){directories.add(relative);for(ServerSftp.Entry child:source.list(path)){if(!ServerSftp.validName(child.name))throw new IOException("UNSAFE_NAME");walk(source,path+(path.equals("/")?"":"/")+child.name,relative+"/"+child.name,depth+1,progress);}}
        else{if(info.size>ServerSftp.MAX_DOWNLOAD_BYTES||info.size>MAX_TOTAL-bytes)throw new IOException("DOWNLOAD_SIZE_LIMIT");bytes+=info.size;files.add(new Item(path,relative,info));}
    }
    static String checkedRelative(String relative)throws IOException{if(relative==null||relative.isEmpty()||relative.startsWith("/"))throw new IOException("INVALID_DESTINATION");for(String name:relative.split("/",-1))if(!ServerSftp.validName(name))throw new IOException("INVALID_DESTINATION");return relative;}
}
