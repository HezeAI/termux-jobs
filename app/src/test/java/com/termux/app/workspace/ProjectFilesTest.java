package com.termux.app.workspace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE)
public class ProjectFilesTest {
    @Test public void listRealFixtureHandlesSpacesQuotesUnicodeAndNoShellExpansion() throws Exception {
        File dir=Files.createTempDirectory("project-files-").toFile();
        try {
            File folder=new File(dir,"папка ' $(echo unsafe)");assertTrue(folder.mkdir());
            File text=new File(dir,"note with spaces.md");Files.write(text.toPath(),"Привет".getBytes(StandardCharsets.UTF_8));
            Process process=new ProcessBuilder("bash","--noprofile","--norc","-c",ProjectFiles.listScript(dir.getPath())).start();
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;try(InputStream in=process.getInputStream()){while((n=in.read(b))!=-1)bytes.write(b,0,n);}
            assertTrue(process.waitFor(10,TimeUnit.SECONDS));assertEquals(0,process.exitValue());ProjectFiles.Listing listing=ProjectFiles.parse(bytes.toByteArray());
            assertEquals(dir.getCanonicalPath(),listing.path);assertEquals(2,listing.entries.size());assertEquals(folder.getName(),listing.entries.get(0).name);assertTrue(listing.entries.get(0).directory);assertEquals(text.getName(),listing.entries.get(1).name);
        }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
    }
    @Test public void malformedAndTraversalMetadataRejected() throws Exception {
        for(String value:new String[]{"relative\0f\0name\01\0","/root\0f\0../key\0"+"1\0","/root\0f\0file\0oops\0"}) {
            try{ProjectFiles.parse(value.getBytes(StandardCharsets.UTF_8));fail("invalid listing accepted");}catch(IOException expected){}
        }
    }
    @Test public void filterKeepsFoldersAndSelectsTypes() {
        ProjectFiles.Entry folder=new ProjectFiles.Entry("/root","src",true,0),apk=new ProjectFiles.Entry("/root","app.apk",false,20),log=new ProjectFiles.Entry("/root","build.log",false,5);
        assertTrue(ProjectFiles.visible(folder,"APK"));assertTrue(ProjectFiles.visible(apk,"APK"));assertFalse(ProjectFiles.visible(log,"APK"));assertTrue(ProjectFiles.visible(log,"Текст"));assertFalse(ProjectFiles.visible(apk,"Текст"));
    }
    @Test public void environmentCommandsUseSeparateTransportAndQuoteUntrustedArguments() {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="codex";s.codexEnvironment="ubuntu";
        assertTrue(ProjectFiles.command(s,"printf 'hello'").startsWith("proot-distro login ubuntu -- /bin/bash --noprofile --norc -c "));
        s.environment="ssh";s.sshHost="example.org";s.sshUser="root";s.sshPort=2222;s.sshAuth="key";s.sshIdentity="~/key with spaces";
        String command=ProjectFiles.command(s,"cat -- '/tmp/quote'\"'\"'file'");assertTrue(command.contains("StrictHostKeyChecking=yes"));assertTrue(command.contains("BatchMode=yes"));assertTrue(command.contains(" -p 2222 "));assertTrue(command.contains("-T"));assertTrue(command.contains("IdentitiesOnly=yes"));assertTrue(command.contains("ServerAliveInterval=15"));assertTrue(command.contains("ServerAliveCountMax=2"));assertFalse(ProjectFiles.command(new WorkspaceStore.Session(),"true").contains("ServerAlive"));
        try{ProjectFiles.target("/tmp/file\nother");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void downloadNameDoesNotOverwriteExistingFile() throws Exception {
        File dir=Files.createTempDirectory("project-export-").toFile();File existing=new File(dir,"app.apk");Files.write(existing.toPath(),new byte[]{1});
        try{File saved=ProjectFileExport.save(new ByteArrayInputStream(new byte[]{2}),dir,"app.apk",16,()->false);assertEquals("app-2.apk",saved.getName());saved.delete();assertArrayEquals(new byte[]{1},Files.readAllBytes(existing.toPath()));}finally{existing.delete();dir.delete();}
    }
    @Test public void danglingDownloadNameCountsAsOccupied()throws Exception{
        java.nio.file.Path dir=Files.createTempDirectory("download-link-");java.nio.file.Path link=dir.resolve("app.apk");Files.createSymbolicLink(link,java.nio.file.Paths.get("missing"));
        try{File saved=ProjectFileExport.save(new ByteArrayInputStream(new byte[]{2}),dir.toFile(),"app.apk",16,()->false);assertEquals("app-2.apk",saved.getName());saved.delete();assertTrue(Files.isSymbolicLink(link));}
        finally{Files.delete(link);Files.delete(dir);}
    }
    @Test public void versionedListingCarriesModificationTimeAndRejectsMalformedTimes()throws Exception{
        String value="JOBS_FILES_2\0/root\0f\0photo.jpg\0"+"42\0"+"1761234567.1234567890\0";ProjectFiles.Listing list=ProjectFiles.parse(value.getBytes(StandardCharsets.UTF_8));assertEquals(1,list.entries.size());assertEquals(1761234567123L,list.entries.get(0).modified);
        for(String invalid:new String[]{"NaN","Infinity","bad","999999999999999999999999"})try{ProjectFiles.parse(value.replace("1761234567.1234567890",invalid).getBytes(StandardCharsets.UTF_8));fail(invalid);}catch(IOException expected){}
    }
    @Test public void portableListingReadsRealMtimeAndHandlesQuotedNames()throws Exception{
        File dir=Files.createTempDirectory("portable-media-").toFile();File photo=new File(dir,"photo ' $(no-exec).jpg");Files.write(photo.toPath(),new byte[]{1,2,3});assertTrue(photo.setLastModified(1700000000000L));
        try{Process p=new ProcessBuilder("sh","-c",ProjectFiles.portableListScript(dir.getPath())).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=p.getInputStream().read(b))!=-1)out.write(b,0,n);assertEquals(0,p.waitFor());ProjectFiles.Entry entry=ProjectFiles.parse(out.toByteArray()).entries.get(0);assertEquals(photo.getName(),entry.name);assertEquals(3,entry.size);assertEquals(1700000000000L,entry.modified);}finally{photo.delete();dir.delete();}
    }

    @Test public void portableListingIncludesActualDirectoryDate()throws Exception{
        File dir=Files.createTempDirectory("portable-sort-").toFile(),child=new File(dir,"folder ' $(no-exec)");assertTrue(child.mkdir());assertTrue(child.setLastModified(1700000012000L));
        try{Process p=new ProcessBuilder("sh","-c",ProjectFiles.portableListScript(dir.getPath())).start();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;while((n=p.getInputStream().read(buffer))!=-1)bytes.write(buffer,0,n);assertEquals(0,p.waitFor());ProjectFiles.Entry e=ProjectFiles.parse(bytes.toByteArray()).entries.get(0);assertTrue(e.directory);assertEquals(child.getName(),e.name);assertEquals(1700000012000L,e.modified);}finally{child.delete();dir.delete();}
    }

}
