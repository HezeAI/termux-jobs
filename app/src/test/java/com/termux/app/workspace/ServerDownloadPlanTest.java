package com.termux.app.workspace;
import org.junit.*;import java.io.*;import java.util.*;import static org.junit.Assert.*;
public class ServerDownloadPlanTest {
    private static final class Source implements ServerDownloadPlan.Source {
        final Map<String,ServerSftp.Info> nodes=new HashMap<>();final Map<String,List<ServerSftp.Entry>> children=new HashMap<>();
        public ServerSftp.Info stat(String path){return nodes.getOrDefault(path,new ServerSftp.Info(false,0,0,0));}
        public String canonical(String path){return path;}
        public List<ServerSftp.Entry> list(String path){return children.getOrDefault(path,Collections.emptyList());}
        void add(String path,int mode,long size){ServerSftp.Info i=new ServerSftp.Info(true,size,mode,0);nodes.put(path,i);int slash=path.lastIndexOf('/');children.computeIfAbsent(path.substring(0,slash),k->new ArrayList<>()).add(new ServerSftp.Entry(path.substring(slash+1),i));}
    }
    @Test public void nestedAndEmptyDirectoriesPreservedWithoutFollowingLinks()throws Exception{
        Source s=new Source();s.add("/srv/dir",0040755,0);s.add("/srv/dir/empty",0040755,0);s.add("/srv/dir/file.txt",0100600,12);s.add("/srv/dir/link",0120777,4);
        ServerDownloadPlan p=ServerDownloadPlan.scan(s,Collections.singletonList("/srv/dir"),(n,path)->{});assertEquals(Arrays.asList("dir","dir/empty"),p.directories);assertEquals(1,p.files.size());assertEquals("dir/file.txt",p.files.get(0).relative);assertEquals(12,p.bytes);assertEquals(1,p.skipped);
    }
    @Test public void threeGigabyteModelAcceptedButAboveTenGiBRejected()throws Exception{
        Source s=new Source();s.add("/model",0100600,3016294400L);assertEquals(3016294400L,ServerDownloadPlan.scan(s,Collections.singletonList("/model"),(n,p)->{}).bytes);s.add("/large",0100600,ServerSftp.MAX_DOWNLOAD_BYTES+1);try{ServerDownloadPlan.scan(s,Collections.singletonList("/large"),(n,p)->{});fail();}catch(IOException e){assertEquals("DOWNLOAD_SIZE_LIMIT",e.getMessage());}
    }
    @Test public void cancellationStopsScanBeforeDestinationCreation()throws Exception{
        Source s=new Source();s.add("/fixture",0100600,1);try{ServerDownloadPlan.scan(s,Collections.singletonList("/fixture"),(n,p)->{throw new java.util.concurrent.CancellationException();});fail();}catch(java.util.concurrent.CancellationException expected){}
    }
    @Test public void unsafeRelativeNamesCannotEscapeDestination()throws Exception{
        for(String path:new String[]{"../x","a/../x","/root","a//b","a/./b","a/evil\nname"})try{ServerDownloadPlan.checkedRelative(path);fail(path);}catch(IOException expected){}
        assertEquals("a/русский файл.md",ServerDownloadPlan.checkedRelative("a/русский файл.md"));
    }
    @Test public void changingSymlinkAncestorAndDuplicateRootsRejected()throws Exception{
        Source s=new Source();s.add("/a/file",0100600,1);s.add("/b/file",0100600,1);try{ServerDownloadPlan.scan(s,Arrays.asList("/a/file","/b/file"),(n,p)->{});fail();}catch(IOException expected){assertEquals("DUPLICATE_OR_UNSAFE_NAME",expected.getMessage());}
    }
}
