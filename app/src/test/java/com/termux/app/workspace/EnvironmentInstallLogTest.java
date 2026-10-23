package com.termux.app.workspace;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;

public class EnvironmentInstallLogTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void capturesMoreThanUiTailAndMarksBoundedOverflow(){
        EnvironmentInstallLog.Capture c=new EnvironmentInstallLog.Capture();char[] data=new char[20000];java.util.Arrays.fill(data,'x');c.append(data,data.length);
        assertEquals(20000,c.snapshot().length());assertEquals(12000,c.recent().length());
        for(int i=0;i<20;i++)c.append(data,data.length);
        char[] end="\nFailed to install: no space\n".toCharArray();c.append(end,end.length);
        assertEquals(EnvironmentInstallLog.LIMIT+EnvironmentInstallLog.TRUNCATED.length(),c.snapshot().length());assertTrue(c.snapshot().startsWith(EnvironmentInstallLog.TRUNCATED));assertTrue(c.snapshot().endsWith(new String(end)));
    }
    @Test public void redactsCredentialsAndTerminalEscapesWithoutHidingFailure(){
        String report=EnvironmentInstallLog.report("ubuntu",1,"\u001b[31mFailed to install: https://user:pass@host/image?token=private returned 403\u001b[0m\nAuthorization: Bearer abc\npassword=supersecret\n-----BEGIN OPENSSH PRIVATE KEY-----\nprivate-data\n-----END OPENSSH PRIVATE KEY-----\nghp_secretvalue\n");
        assertTrue(report.contains("Failed to install: [url] returned 403"));for(String hidden:new String[]{"user:pass","private-data","supersecret","Bearer abc","ghp_secretvalue","\u001b"})assertFalse(hidden,report.contains(hidden));assertTrue(report.contains("Exit status: 1"));assertEquals("[token redacted]",EnvironmentInstallLog.clean("github_"+"pat_fixturesecret"));
    }
    @Test public void allEnvironmentRecordsAreIndependentAndReplaceLastAttempt()throws Exception{
        File files=temp.getRoot();for(String alias:new String[]{"ubuntu","ubuntu-ai","debian","alpine","archlinux","fedora","opensuse","void"})EnvironmentInstallLog.save(files,alias,EnvironmentInstallLog.report(alias,1,"Failed to install: "+alias));
        EnvironmentInstallLog.save(files,"ubuntu","new Ubuntu attempt");assertEquals("new Ubuntu attempt",EnvironmentInstallLog.read(files,"ubuntu"));assertTrue(EnvironmentInstallLog.read(files,"debian").contains("Failed to install: debian"));
        assertEquals(8,new File(files,".environment-control/logs").list().length);EnvironmentInstallLog.clear(files,"ubuntu");assertFalse(EnvironmentInstallLog.exists(files,"ubuntu"));assertTrue(EnvironmentInstallLog.exists(files,"debian"));
    }
    @Test public void rejectsTraversalOversizedFilesAndSymlinks()throws Exception{
        File files=temp.getRoot();try{EnvironmentInstallLog.save(files,"../outside","bad");fail();}catch(IOException expected){}
        EnvironmentInstallLog.save(files,"ubuntu","ok");Path file=new File(files,".environment-control/logs/ubuntu.txt").toPath();assertEquals("rw-------",java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        Files.delete(file);Path outside=temp.newFile("outside").toPath();Files.write(outside,"private".getBytes());Files.createSymbolicLink(file,outside);
        assertFalse(EnvironmentInstallLog.exists(files,"ubuntu"));try{EnvironmentInstallLog.read(files,"ubuntu");fail();}catch(IOException expected){}try{EnvironmentInstallLog.save(files,"ubuntu","overwrite");fail();}catch(IOException expected){}assertEquals("private",new String(Files.readAllBytes(outside)));
        Files.delete(file);try(RandomAccessFile out=new RandomAccessFile(file.toFile(),"rw")){out.setLength(4L*EnvironmentInstallLog.LIMIT+4097);}try{EnvironmentInstallLog.read(files,"ubuntu");fail();}catch(IOException expected){}
    }
}
