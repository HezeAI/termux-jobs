package com.termux.app.workspace;

import org.junit.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ProjectFileRecoveryTest {
    Path root,source,target;
    @Before public void setup()throws Exception{root=Files.createTempDirectory("file-recovery-");source=Files.createDirectory(root.resolve("source"));target=Files.createDirectory(root.resolve("target"));}
    @After public void cleanup()throws Exception{try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(path);}}
    private ProjectFiles.Entry entry(String name)throws Exception{Path p=Files.write(source.resolve(name),name.getBytes(StandardCharsets.UTF_8));return new ProjectFiles.Entry(source.toString(),name,false,Files.size(p));}
    private byte[] run(String script,String shell)throws Exception{
        Process process=new ProcessBuilder(shell,"-c",script).redirectError(new File("/dev/null")).start();
        try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->process,null,ProjectFileReceipt.MAX_OUTPUT,5000)){
            ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] b=new byte[512];int n;while((n=in.read(b))!=-1)output.write(b,0,n);return output.toByteArray();
        }finally{process.destroyForcibly();process.waitFor();}
    }
    @Test public void copySuccessRequiresBothZeroExitAndCompleteReceipt()throws Exception{
        ProjectFiles.Entry entry=entry("quote ' $(no-command).txt");
        ProjectFileTransfer.execute(Arrays.asList(entry),target.toString(),false,false,(script,out,max)->out.write(run(script,"bash")));
        assertArrayEquals(Files.readAllBytes(source.resolve(entry.name)),Files.readAllBytes(target.resolve(entry.name)));assertTrue(Files.exists(source.resolve(entry.name)));
    }
    @Test public void portableShellMoveUsesSameReceipt()throws Exception{
        ProjectFiles.Entry entry=entry("a");ProjectFileTransfer.execute(Arrays.asList(entry),target.toString(),true,false,(script,out,max)->out.write(run(script,"sh")));
        assertFalse(Files.exists(source.resolve("a")));assertTrue(Files.exists(target.resolve("a")));
    }
    @Test public void committedCopyWithAllRepliesLostIsUnknownAndNotReplayed()throws Exception{
        ProjectFiles.Entry entry=entry("a");AtomicInteger runs=new AtomicInteger();
        try{ProjectFileTransfer.execute(Arrays.asList(entry),target.toString(),false,false,(script,out,max)->{runs.incrementAndGet();run(script,"bash");});fail();}
        catch(ProjectFiles.TransferFailure expected){assertEquals(0,expected.acknowledged);assertEquals("TRANSFER_NOT_CONFIRMED",expected.getCause().getMessage());}
        assertEquals(1,runs.get());assertTrue(Files.exists(target.resolve("a")));assertTrue(Files.exists(source.resolve("a")));
        try(java.util.stream.Stream<Path> files=Files.list(target)){assertEquals(1,files.count());}
    }
    @Test public void committedMoveWithoutReplyDoesNotClaimSourceWasUntouched()throws Exception{
        ProjectFiles.Entry entry=entry("a");
        try{ProjectFileTransfer.execute(Arrays.asList(entry),target.toString(),true,false,(script,out,max)->run(script,"bash"));fail();}
        catch(ProjectFiles.TransferFailure expected){assertEquals(0,expected.acknowledged);}
        assertFalse(Files.exists(source.resolve("a")));assertTrue(Files.exists(target.resolve("a")));
    }
    @Test public void fullReceiptDoesNotHideTransportFailure()throws Exception{
        ProjectFiles.Entry entry=entry("a");AtomicInteger runs=new AtomicInteger();
        try{ProjectFileTransfer.execute(Arrays.asList(entry),target.toString(),false,false,(script,out,max)->{runs.incrementAndGet();out.write(run(script,"bash"));throw new IOException("fixture-connection-lost");});fail();}
        catch(ProjectFiles.TransferFailure expected){assertEquals(1,expected.acknowledged);assertEquals("fixture-connection-lost",expected.getCause().getMessage());}
        assertEquals(1,runs.get());assertTrue(Files.exists(target.resolve("a")));
    }
    @Test public void missingFinalReceiptUnderstatesAlreadyCommittedBatch()throws Exception{
        List<ProjectFiles.Entry> entries=Arrays.asList(entry("a"),entry("b"));
        try{ProjectFileTransfer.execute(entries,target.toString(),false,false,(script,out,max)->{
            byte[] complete=run(script,"bash");int newline=0;while(complete[newline]!='\n')newline++;out.write(complete,0,newline+1);
        });fail();}catch(ProjectFiles.TransferFailure expected){assertEquals(1,expected.acknowledged);}
        assertTrue(Files.exists(target.resolve("a")));assertTrue(Files.exists(target.resolve("b")));
    }
    @Test public void jobCanContinueAfterConsumerLosesItsConnection()throws Exception{
        List<ProjectFiles.Entry> entries=Arrays.asList(entry("a"),entry("b"));Path gate=root.resolve("gate"),log=root.resolve("reply");Process[] worker={null};AtomicInteger runs=new AtomicInteger();
        try{
            try{ProjectFileTransfer.execute(entries,target.toString(),false,false,(script,out,max)->{
                runs.incrementAndGet();String prefix="cp() { for arg; do if [ \"$arg\" = "+SessionShell.quote(source.resolve("b").toString())+" ]; then while ! [ -f "+SessionShell.quote(gate.toString())+" ]; do sleep 0.02; done; fi; done; command cp \"$@\"; };\n";
                worker[0]=new ProcessBuilder("bash","-c",prefix+script).redirectOutput(log.toFile()).redirectError(new File("/dev/null")).start();
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);byte[] reply;
                while(true){reply=Files.readAllBytes(log);if(new String(reply,StandardCharsets.UTF_8).endsWith("\n"))break;if(System.nanoTime()>deadline)throw new IOException("fixture-stalled");Thread.sleep(5);}
                out.write(reply);throw new IOException("fixture-connection-lost");
            });fail();}catch(ProjectFiles.TransferFailure expected){assertEquals(1,expected.acknowledged);assertEquals("fixture-connection-lost",expected.getCause().getMessage());}
            assertEquals(1,runs.get());assertTrue(worker[0].isAlive());assertTrue(Files.exists(target.resolve("a")));assertFalse(Files.exists(target.resolve("b")));
            Files.write(gate,new byte[0]);assertTrue(worker[0].waitFor(5,TimeUnit.SECONDS));assertEquals(0,worker[0].exitValue());assertTrue(Files.exists(target.resolve("b")));
        }finally{Files.write(gate,new byte[0]);if(worker[0]!=null){if(!worker[0].waitFor(5,TimeUnit.SECONDS))worker[0].destroyForcibly();worker[0].waitFor();}}
    }
    @Test public void failedSecondCopyReportsOnlyAcknowledgedFirstItemAndCleansStage()throws Exception{
        List<ProjectFiles.Entry> entries=Arrays.asList(entry("a"),entry("b"));
        try{ProjectFileTransfer.execute(entries,target.toString(),false,false,(script,out,max)->{
            String prefix="cp() { for arg; do if [ \"$arg\" = "+SessionShell.quote(source.resolve("b").toString())+" ]; then return 1; fi; done; command cp \"$@\"; };\n";
            Process process=new ProcessBuilder("bash","-c",prefix+script).redirectError(new File("/dev/null")).start();
            try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->process,null,max,5000)){
                byte[] bytes=new byte[512];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
            }finally{process.destroyForcibly();process.waitFor();}
        });fail();}catch(ProjectFiles.TransferFailure expected){assertEquals(1,expected.acknowledged);assertEquals("SOURCE_UNAVAILABLE",expected.getCause().getMessage());}
        assertTrue(Files.exists(target.resolve("a")));assertFalse(Files.exists(target.resolve("b")));assertTrue(Files.exists(source.resolve("b")));
        try(java.util.stream.Stream<Path> files=Files.list(target)){assertEquals(1,files.count());}
    }

}
