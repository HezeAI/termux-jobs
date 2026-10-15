package com.termux.app.workspace;

import org.junit.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Real OpenSSH SFTP subsystem, local pipes only; all test files live in verification staging. */
public class ServerSftpTest {
    private Path directory;private ServerSftp client;
    @Before public void setup()throws Exception{
        String executable="/data/data/com.termux/files/usr/libexec/sftp-server";
        Assume.assumeTrue("Local SFTP subsystem unavailable",new File(executable).canExecute());
        directory=Files.createTempDirectory(Paths.get("build"),"sftp-test-");Process process=new ProcessBuilder(executable).redirectError(new File("/dev/null")).start();
        client=new ServerSftp(process.getInputStream(),process.getOutputStream(),process::destroy);
    }
    @After public void cleanup()throws Exception{if(client!=null)client.close();if(directory!=null)try(java.util.stream.Stream<Path> paths=Files.walk(directory)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception ignored){}});}}
    private String path(){return directory.toAbsolutePath().toString();}
    private byte[] bytes(String value)throws Exception{return value.getBytes("UTF-8");}
    private void upload(String name,String value,ServerSftp.Info expected)throws Exception{client.upload(new ByteArrayInputStream(bytes(value)),path(),name,expected,b->{});}
    private long temporaryCount()throws Exception{try(java.util.stream.Stream<Path> files=Files.list(directory)){return files.filter(p->p.getFileName().toString().startsWith(".termux-jobs-upload-")).count();}}
    @Test public void streamsUnicodeAndSpacesWithAcknowledgedProgress()throws Exception{
        String name="заметка ' $ \\ file.txt";byte[] data=new byte[32768*19+1003];new Random(7).nextBytes(data);List<Long> progress=new ArrayList<>();client.upload(new ByteArrayInputStream(data),client.canonical(path()),name,client.stat(path()+"/"+name),progress::add);assertArrayEquals(data,Files.readAllBytes(directory.resolve(name)));assertEquals(Long.valueOf(data.length),progress.get(progress.size()-1));assertEquals(0,temporaryCount());assertNull(client.pendingTemp());
    }
    @Test public void existingFileIsNeverImplicitlyOverwritten()throws Exception{
        Files.write(directory.resolve("keep"),bytes("original"));try{upload("keep","new",new ServerSftp.Info(false,0,0,0));fail();}catch(IOException expected){}assertEquals("original",new String(Files.readAllBytes(directory.resolve("keep")),"UTF-8"));assertEquals(0,temporaryCount());
    }
    @Test public void explicitReplacementAndZeroLengthWork()throws Exception{
        assertTrue(client.canReplace());Files.write(directory.resolve("keep"),bytes("original"));ServerSftp.Info info=client.stat(path()+"/keep");upload("keep","",info);assertEquals(0,Files.size(directory.resolve("keep")));assertEquals(0,temporaryCount());
    }
    @Test public void detectsRemoteChangeBeforePublication()throws Exception{
        Files.write(directory.resolve("keep"),bytes("original"));ServerSftp.Info before=client.stat(path()+"/keep");Files.write(directory.resolve("keep"),bytes("changed externally"));try{upload("keep","new",before);fail();}catch(IOException expected){assertEquals("REMOTE_CHANGED",expected.getMessage());}assertEquals("changed externally",new String(Files.readAllBytes(directory.resolve("keep")),"UTF-8"));assertEquals(0,temporaryCount());
    }
    @Test public void readFailureCleansPartialAndKeepsDestination()throws Exception{
        InputStream broken=new InputStream(){int n;public int read()throws IOException{if(n++<300)return 'x';throw new IOException("source lost");}};try{client.upload(broken,path(),"new",client.stat(path()+"/new"),b->{});fail();}catch(IOException expected){}assertFalse(Files.exists(directory.resolve("new")));assertEquals(0,temporaryCount());
    }
    @Test public void rejectsSymlinkAndUnsafeNames()throws Exception{
        Files.write(directory.resolve("real"),bytes("secret"));Files.createSymbolicLink(directory.resolve("link"),directory.resolve("real").toAbsolutePath());assertFalse(client.stat(path()+"/link").regular());try{upload("link","new",client.stat(path()+"/link"));fail();}catch(IOException expected){}for(String name:new String[]{"../file","a/b",".","..","bad\nname","bad\u0000name",""})assertFalse(name,ServerSftp.validName(name));assertTrue(ServerSftp.validName("файл.txt"));assertEquals("secret",new String(Files.readAllBytes(directory.resolve("real")),"UTF-8"));
    }
    @Test public void cancellationLeavesOnlyIdentifiedTemporaryAndCanCleanIt()throws Exception{
        try{client.upload(new ByteArrayInputStream(new byte[65536]),path(),"new",client.stat(path()+"/new"),b->{client.close();throw new java.util.concurrent.CancellationException();});fail();}catch(java.util.concurrent.CancellationException expected){}
        assertFalse(Files.exists(directory.resolve("new")));String temp=client.pendingTemp();assertNotNull(temp);Process p=new ProcessBuilder("/data/data/com.termux/files/usr/libexec/sftp-server").redirectError(new File("/dev/null")).start();try(ServerSftp cleanup=new ServerSftp(p.getInputStream(),p.getOutputStream(),p::destroy)){cleanup.removeTemporary(temp);try{cleanup.removeTemporary(path()+"/real");fail();}catch(IOException expected){assertEquals("INVALID_PATH",expected.getMessage());}}assertEquals(0,temporaryCount());
    }
    @Test public void malformedAndOversizedPacketsFailClosed()throws Exception{
        for(byte[] data:new byte[][]{{0,0,0,0},{0,16,0,0},{0,0,0,5,2,0,0,0,4}}){try{new ServerSftp(new ByteArrayInputStream(data),new ByteArrayOutputStream(),()->{});fail();}catch(IOException expected){}}
    }
}
