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
}
