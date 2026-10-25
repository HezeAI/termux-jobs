package com.termux.app.workspace;

import org.junit.Test;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class ProjectFileTransportTest {
    private Process child(String script) throws IOException {
        return new ProcessBuilder("bash", "--noprofile", "--norc", "-c", script).redirectError(new File("/dev/null")).start();
    }
    private void code(String expected, RunnableIO action) throws Exception {
        try { action.run(); fail(expected); } catch (IOException error) { assertEquals(expected,error.getMessage()); }
    }
    interface RunnableIO { void run() throws Exception; }

    @Test public void fiveGbStreamCountsBeyondSignedIntAndRejectsOneExtraByte()throws Exception{
        final long limit=5L*1024*1024*1024;
        for(boolean excess:new boolean[]{false,true}){
            InputStream generated=new InputStream(){
                long remaining=limit+(excess?1:0);
                public int read(){if(remaining==0)return -1;remaining--;return 0;}
                public int read(byte[] b,int off,int len){if(remaining==0)return -1;int n=(int)Math.min(remaining,len);remaining-=n;return n;}
            };
            Process fixture=new Process(){
                public InputStream getInputStream(){return generated;}
                public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
                public InputStream getErrorStream(){return new ByteArrayInputStream(new byte[0]);}
                public int waitFor(){return 0;}public int exitValue(){return 0;}
                public void destroy(){}public Process destroyForcibly(){return this;}
            };
            try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->fixture,null,limit,10000)){
                long total=0;byte[] buffer=new byte[65536];
                try{int count;while((count=in.read(buffer))!=-1)total+=count;assertFalse(excess);}
                catch(IOException e){assertTrue(excess);assertEquals("FILE_LIMIT",e.getMessage());}
                assertEquals(limit,total);
            }
        }
    }

    @Test public void binarySingleByteReadsHandleZeroAnd255AndEof() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->child("printf '\\000\\377a'"),null,3,3000)) {
            assertEquals(0,in.read());assertEquals(255,in.read());assertEquals(97,in.read());assertEquals(-1,in.read());assertEquals(-1,in.read());
        }
    }
    @Test public void growingSourceCannotBypassBoundAndOldCloseCannotCancelNextChild() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            InputStream old=transport.open(()->child("printf abcdef"),null,3,3000);
            assertEquals(3,old.read(new byte[3]));code("FILE_LIMIT",()->old.read());
            Process next=child("printf next; exec sleep 20");
            try(InputStream current=transport.open(()->next,null,4,3000)) {
                old.close();old.close();assertEquals('n',current.read());assertTrue(next.isAlive());
            }
        }
    }
    @Test public void skipIsCountedAndBulkDoesNotDeliverExcessChunk() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            try(InputStream in=transport.open(()->child("printf abcdef"),null,4,3000)) {
                assertEquals(3,in.skip(3));code("FILE_LIMIT",()->in.read(new byte[4]));
                assertFalse(in.markSupported());code("MARK_NOT_SUPPORTED",in::reset);
            }
        }
    }
    @Test public void zeroBoundAllowsEmptyFileButRejectsAnyByte() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            try(InputStream empty=transport.open(()->child("exit 0"),null,0,3000)){assertEquals(0,empty.read(new byte[0]));assertEquals(-1,empty.read());}
            try(InputStream in=transport.open(()->child("printf x"),null,0,3000)){code("FILE_LIMIT",in::read);}
        }
    }
    @Test public void busyStreamRejectsConcurrentLaunchBeforeStartingIt() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->child("printf x"),null,1,3000)) {
            code("FILE_OPERATION_BUSY",()->transport.open(()->{fail("overlapping child started");return null;},null,1,3000));
        }
    }
    @Test public void sourceFailureReleasesSlotAndDoesNotReportSuccessfulEof() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            try(InputStream broken=transport.open(()->child("exit 7"),null,4,3000)){code("SOURCE_UNAVAILABLE",broken::read);}
            try(InputStream next=transport.open(()->child("printf ok"),null,2,3000)){assertEquals('o',next.read());}
        }
    }
    @Test public void launchFailureReleasesSlot() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            code("fixture",()->transport.open(()->{throw new IOException("fixture");},null,0,3000));
            try(InputStream next=transport.open(()->child("exit 0"),null,0,3000)){assertEquals(-1,next.read());}
        }
    }
    @Test public void timeoutStopsDirectChildAndDoesNotTouchUnrelatedProcess() throws Exception {
        Process unrelated=child("exec sleep 20"), owned=child("exec sleep 20");
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            try(InputStream in=transport.open(()->owned,null,1,150)){code("FILE_OPERATION_TIMEOUT",in::read);}
            assertTrue(owned.waitFor(2,TimeUnit.SECONDS));assertTrue(unrelated.isAlive());
            try(InputStream next=transport.open(()->child("printf ok"),null,2,3000)){assertEquals('o',next.read());}
        }finally{owned.destroyForcibly();unrelated.destroyForcibly();owned.waitFor();unrelated.waitFor();}
    }
    @Test public void closeDuringBlockedReadUnblocksAndRejectsNewWork() throws Exception {
        ProjectFileTransport transport=new ProjectFileTransport();Process owned=child("exec sleep 20");
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try(InputStream in=transport.open(()->owned,null,1,10000)) {
            Future<?> result=worker.submit(()->{try{in.read();throw new AssertionError();}catch(IOException expected){assertEquals("CANCELLED",expected.getMessage());}});
            transport.close();result.get(4,TimeUnit.SECONDS);assertFalse(owned.isAlive());
            code("CANCELLED",()->transport.open(()->{fail();return null;},null,1,3000));
        }finally{transport.close();owned.destroyForcibly();worker.shutdownNow();}
    }
    @Test public void closeWhileLaunchingStopsOnlyTheNewlyAttachedChild() throws Exception {
        ProjectFileTransport transport=new ProjectFileTransport();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        ExecutorService worker=Executors.newSingleThreadExecutor();AtomicReference<Process> owned=new AtomicReference<>();
        try {
            Future<?> result=worker.submit(()->{try{transport.open(()->{entered.countDown();assertTrue(release.await(3,TimeUnit.SECONDS));Process p=child("exec sleep 20");owned.set(p);return p;},null,1,3000);throw new AssertionError();}catch(Exception expected){assertEquals("CANCELLED",expected.getMessage());}});
            assertTrue(entered.await(3,TimeUnit.SECONDS));transport.close();release.countDown();result.get(4,TimeUnit.SECONDS);assertFalse(owned.get().isAlive());
        }finally{release.countDown();transport.close();worker.shutdownNow();if(owned.get()!=null)owned.get().destroyForcibly();}
    }
    @Test public void interruptedReaderRetainsFlagAndWaitsForItsChild() throws Exception {
        Process owned=child("exec sleep 20");
        try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->owned,null,1,3000)) {
            Thread.currentThread().interrupt();code("CANCELLED",in::read);assertTrue(Thread.currentThread().isInterrupted());assertFalse(owned.isAlive());
        }finally{Thread.interrupted();owned.destroyForcibly();}
    }
    @Test public void inputPayloadIsClosedAndDeliveredWithoutShellExpansion() throws Exception {
        try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->child("exec cat"),"$(no-execute)\nПривет".getBytes("UTF-8"),128,3000)) {
            ByteArrayOutputStream output=new ByteArrayOutputStream();byte[] b=new byte[32];int n;while((n=in.read(b))!=-1)output.write(b,0,n);
            assertEquals("$(no-execute)\nПривет",output.toString("UTF-8"));
        }
    }
    @Test public void ignoredTermRetainsBusySlotUntilForcedChildExit() throws Exception {
        Process owned=child("trap '' TERM; printf R; exec sleep 20");
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            InputStream in=transport.open(()->owned,null,1,5000);assertEquals('R',in.read());
            Future<?> closing=worker.submit(()->{in.close();return null;});
            // The child ignores TERM, so cleanup must not release admission early.
            Thread.sleep(100);assertFalse(closing.isDone());
            code("FILE_OPERATION_BUSY",()->transport.open(()->{fail("child overlapped cleanup");return null;},null,1,3000));
            closing.get(4,TimeUnit.SECONDS);assertFalse(owned.isAlive());
            try(InputStream next=transport.open(()->child("exit 0"),null,0,3000)){assertEquals(-1,next.read());}
        }finally{owned.destroyForcibly();worker.shutdownNow();}
    }
    @Test public void stdoutEofDoesNotBypassExitStatusOrTimeout() throws Exception {
        Process owned=child("exec 1>&-; exec sleep 20");
        try(ProjectFileTransport transport=new ProjectFileTransport();InputStream in=transport.open(()->owned,null,0,150)) {
            code("FILE_OPERATION_TIMEOUT",in::read);assertFalse(owned.isAlive());
        }finally{owned.destroyForcibly();}
    }
    @Test public void outputFailureThroughTryResourceWaitsBeforeNextOperation() throws Exception {
        Process owned=child("printf R; exec sleep 20");
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            code("output-full",()->{
                try(InputStream in=transport.open(()->owned,null,1,3000)) {
                    assertEquals('R',in.read());throw new IOException("output-full");
                }
            });
            assertFalse(owned.isAlive());
            try(InputStream next=transport.open(()->child("exit 0"),null,0,3000)){assertEquals(-1,next.read());}
        }finally{owned.destroyForcibly();}
    }

    @Test public void unreadStreamTimeoutReapsChildAndReleasesSlotWithoutConsumer() throws Exception {
        Process owned=child("exec sleep 20");
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            InputStream abandoned=transport.open(()->owned,null,1,100);
            assertTrue(owned.waitFor(3,TimeUnit.SECONDS));
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);InputStream next=null;
            while(next==null)try{next=transport.open(()->child("printf x"),null,1,3000);}
            catch(IOException e){assertEquals("FILE_OPERATION_BUSY",e.getMessage());if(System.nanoTime()>until)throw e;Thread.sleep(5);}
            try(InputStream in=next){abandoned.close();assertEquals('x',in.read());}
        }finally{owned.destroyForcibly();}
    }

    private Process faulty(Process process, boolean failStdin) {
        return new Process() {
            public OutputStream getOutputStream(){
                if(!failStdin)return process.getOutputStream();
                return new FilterOutputStream(process.getOutputStream()) {
                    public void close() throws IOException {out.close();throw new IOException("stdin-close");}
                };
            }
            public InputStream getInputStream(){
                if(failStdin)return process.getInputStream();
                return new FilterInputStream(process.getInputStream()) {
                    public int read(byte[] b,int off,int len)throws IOException{throw new IOException("source-read");}
                };
            }
            public InputStream getErrorStream(){return process.getErrorStream();}
            public int waitFor()throws InterruptedException{return process.waitFor();}
            public int exitValue(){return process.exitValue();}
            public void destroy(){process.destroy();}
            public Process destroyForcibly(){process.destroyForcibly();return this;}
        };
    }
    @Test public void stdinCloseFailureStillWaitsAndReleasesOperation() throws Exception {
        Process child=child("exec sleep 20");
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            code("stdin-close",()->transport.open(()->faulty(child,true),null,1,3000));assertFalse(child.isAlive());
            try(InputStream next=transport.open(()->child("exit 0"),null,0,3000)){assertEquals(-1,next.read());}
        }finally{child.destroyForcibly();}
    }
    @Test public void sourceReadFailureStillWaitsAndReleasesOperation() throws Exception {
        Process child=child("exec sleep 20");
        try(ProjectFileTransport transport=new ProjectFileTransport()) {
            try(InputStream in=transport.open(()->faulty(child,false),null,1,3000)){code("source-read",in::read);}
            assertFalse(child.isAlive());
            try(InputStream next=transport.open(()->child("exit 0"),null,0,3000)){assertEquals(-1,next.read());}
        }finally{child.destroyForcibly();}
    }

}
