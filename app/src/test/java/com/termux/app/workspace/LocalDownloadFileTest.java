package com.termux.app.workspace;

import org.junit.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class LocalDownloadFileTest {
    @Rule public org.junit.rules.TemporaryFolder tmp=new org.junit.rules.TemporaryFolder();
    private int run(String script,byte[] data)throws Exception{Process p=new ProcessBuilder("sh","-c",script).start();try(OutputStream out=p.getOutputStream()){out.write(data);}while(p.getInputStream().read()!=-1){}return p.waitFor();}
    @Test public void streamsBinaryAndPublishesOnlyAfterExplicitSuccess()throws Exception{
        File folder=tmp.newFolder("dir ' with spaces");String name="download-fixture.bin";File part=new File(folder,name+".part"),target=new File(folder,name);byte[] bytes={0,1,2,3,(byte)255};
        assertEquals(0,run(LocalDownloadFile.writeScript(folder.getAbsolutePath(),name+".part"),bytes));assertArrayEquals(bytes,Files.readAllBytes(part.toPath()));assertFalse(target.exists());
        assertEquals(0,run(LocalDownloadFile.publishScript(part.getAbsolutePath(),target.getAbsolutePath()),new byte[0]));assertFalse(part.exists());assertArrayEquals(bytes,Files.readAllBytes(target.toPath()));
    }
    @Test public void writerDoesNotClobberExistingTemporaryFile()throws Exception{
        File folder=tmp.newFolder("exclusive"),part=new File(folder,"download-test.part");byte[] original={3,2,1};Files.write(part.toPath(),original);
        assertNotEquals(0,run(LocalDownloadFile.writeScript(folder.getAbsolutePath(),part.getName()),new byte[0]));assertArrayEquals(original,Files.readAllBytes(part.toPath()));
    }
    @Test public void publicationRefusesExistingFileAndSymlink()throws Exception{
        File folder=tmp.newFolder("publish"),part=new File(folder,"download-test.part"),target=new File(folder,"download-test");Files.write(part.toPath(),new byte[]{1});Files.write(target.toPath(),new byte[]{2});assertNotEquals(0,run(LocalDownloadFile.publishScript(part.getAbsolutePath(),target.getAbsolutePath()),new byte[0]));assertArrayEquals(new byte[]{2},Files.readAllBytes(target.toPath()));assertTrue(part.exists());
        Files.delete(target.toPath());Files.createSymbolicLink(target.toPath(),part.toPath());assertNotEquals(0,run(LocalDownloadFile.publishScript(part.getAbsolutePath(),target.getAbsolutePath()),new byte[0]));assertTrue(Files.isSymbolicLink(target.toPath()));assertTrue(part.exists());
    }
    @Test public void racedInDirectoryCannotReceiveTemporaryFileOrReportSuccess()throws Exception{publicationRace(false);}
    @Test public void racedInDirectorySymlinkCannotSendFileOutsideDestination()throws Exception{publicationRace(true);}
    private void publicationRace(boolean symlink)throws Exception{
        File folder=tmp.newFolder("race"),part=new File(folder,"download.part"),target=new File(folder,"download.bin"),bin=tmp.newFolder("bin"),outside=tmp.newFolder("outside");Files.write(part.toPath(),new byte[]{7});
        Process lookup=new ProcessBuilder("sh","-c","command -v mv").start();String realMv;try(BufferedReader in=new BufferedReader(new InputStreamReader(lookup.getInputStream()))){realMv=in.readLine();}assertEquals(0,lookup.waitFor());
        String race=symlink?"ln -s -- "+SessionShell.quote(outside.getPath())+" "+SessionShell.quote(target.getPath()):"mkdir -- "+SessionShell.quote(target.getPath());
        File wrapper=new File(bin,"mv");Files.write(wrapper.toPath(),("#!/bin/sh\n"+race+"\nexec "+SessionShell.quote(realMv)+" \"$@\"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));assertTrue(wrapper.setExecutable(true));
        ProcessBuilder pb=new ProcessBuilder("sh","-c",LocalDownloadFile.publishScript(part.getAbsolutePath(),target.getAbsolutePath()));pb.environment().put("PATH",bin.getAbsolutePath()+File.pathSeparator+System.getenv("PATH"));Process p=pb.start();
        assertTrue(p.waitFor(5,java.util.concurrent.TimeUnit.SECONDS));assertNotEquals("raced directory must not be treated as completed download",0,p.exitValue());assertTrue(part.isFile());assertTrue(target.isDirectory());assertEquals(0,target.list().length);assertEquals(0,outside.list().length);
    }
}
