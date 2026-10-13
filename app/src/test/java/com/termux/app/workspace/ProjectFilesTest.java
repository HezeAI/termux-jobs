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
        String command=ProjectFiles.command(s,"cat -- '/tmp/quote'\"'\"'file'");assertTrue(command.contains("StrictHostKeyChecking=yes"));assertTrue(command.contains("BatchMode=yes"));assertTrue(command.contains(" -p 2222 "));assertTrue(command.contains("-T"));assertTrue(command.contains("IdentitiesOnly=yes"));
        try{ProjectFiles.target("/tmp/file\nother");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void downloadNameDoesNotOverwriteExistingFile() throws Exception {
        File dir=Files.createTempDirectory("project-export-").toFile();File existing=new File(dir,"app.apk");Files.write(existing.toPath(),new byte[]{1});
        try{assertEquals("app-2.apk",WorkspaceFileBrowser.unique(dir,"app.apk").getName());assertArrayEquals(new byte[]{1},Files.readAllBytes(existing.toPath()));}finally{existing.delete();dir.delete();}
    }
}
