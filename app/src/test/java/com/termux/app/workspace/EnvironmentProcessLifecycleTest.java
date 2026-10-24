package com.termux.app.workspace;

import java.io.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=android.app.Application.class)
public class EnvironmentProcessLifecycleTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private ExecutorService executor;private Child child;private File marker;
    private static final class Child extends Process {
        final CountDownLatch waiting=new CountDownLatch(1),finish=new CountDownLatch(1);
        InputStream input=new ByteArrayInputStream("READY:environment\nREADY:shell\nCHECK:done\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        OutputStream stdin=new ByteArrayOutputStream();int signals;int code;
        public InputStream getInputStream(){return input;}
        public InputStream getErrorStream(){return new ByteArrayInputStream(new byte[0]);}
        public OutputStream getOutputStream(){return stdin;}
        public int waitFor()throws InterruptedException{waiting.countDown();finish.await();return code;}
        public int exitValue(){if(finish.getCount()!=0)throw new IllegalThreadStateException();return code;}
        public boolean isAlive(){return finish.getCount()!=0;}
        public void destroy(){signals++;}
    }
    @Before public void setup(){executor=Executors.newSingleThreadExecutor();child=new Child();marker=new File(temporary.getRoot(),"cancel");}
    @After public void cleanup()throws Exception{child.finish.countDown();executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
    private Future<Throwable> collect(java.util.function.Consumer<String> update){return executor.submit(()->{try{EnvironmentSetup.collectProcess(child,update,marker);return null;}catch(Throwable e){return e;}});}
    private void assertWaiting(Future<?> result)throws Exception{
        assertTrue("owned child was not awaited",child.waiting.await(2,TimeUnit.SECONDS));assertTrue("cancel marker missing",marker.isFile());
        assertFalse("operation became retryable before exit",result.isDone());assertEquals(0,child.signals);
    }
    @Test public void overlappingInvocationsCannotRewriteOrCleanEachOthersScripts()throws Exception{
        File first=EnvironmentSetup.executionDirectory(temporary.getRoot()),second=EnvironmentSetup.executionDirectory(temporary.getRoot());
        assertNotEquals(first,second);File firstScript=new File(first,"runtime.sh"),secondScript=new File(second,"runtime.sh");
        java.nio.file.Files.write(firstScript.toPath(),new byte[]{1});java.nio.file.Files.write(secondScript.toPath(),new byte[]{2});
        File firstCancel=new File(first,"cancel-own"),secondCancel=new File(second,"cancel-other");assertTrue(firstCancel.createNewFile());assertTrue(secondCancel.createNewFile());
        File downloads=new File(temporary.getRoot(),"downloads");assertTrue(downloads.mkdir());File retained=new File(downloads,"layer");assertTrue(retained.createNewFile());
        EnvironmentSetup.cleanExecution(first,firstCancel);assertFalse(first.exists());assertTrue(secondCancel.exists());
        assertArrayEquals(new byte[]{2},java.nio.file.Files.readAllBytes(secondScript.toPath()));assertTrue(retained.exists());
        EnvironmentSetup.cleanExecution(second,secondCancel);assertFalse(second.exists());assertTrue(retained.exists());
    }
    @Test public void readFailureRequestsOwnedStopAndWaitsBeforeReturning()throws Exception{
        child.input=new InputStream(){public int read()throws IOException{throw new IOException("synthetic read error");}};
        Future<Throwable> result=collect(report->{});assertWaiting(result);child.finish.countDown();assertTrue(result.get(5,TimeUnit.SECONDS) instanceof IOException);
    }
    @Test public void progressCallbackFailureAlsoWaitsForOwnedExit()throws Exception{
        Future<Throwable> result=collect(report->{throw new IllegalStateException("synthetic callback error");});assertWaiting(result);child.finish.countDown();assertTrue(result.get(5,TimeUnit.SECONDS) instanceof IllegalStateException);
    }
    @Test public void stdinCloseFailureCannotAbandonStartedChild()throws Exception{
        child.stdin=new OutputStream(){public void write(int b){}public void close()throws IOException{throw new IOException("synthetic stdin close error");}};
        Future<Throwable> result=collect(report->{});assertWaiting(result);child.finish.countDown();assertTrue(result.get(5,TimeUnit.SECONDS) instanceof IOException);
    }
    @Test public void interruptedWaitStillDrainsChildAndRestoresInterrupt()throws Exception{
        java.util.concurrent.atomic.AtomicReference<Thread> owner=new java.util.concurrent.atomic.AtomicReference<>();
        Future<Boolean> result=executor.submit(()->{owner.set(Thread.currentThread());try{EnvironmentSetup.collectProcess(child,s->{},marker);return false;}catch(InterruptedException e){return Thread.currentThread().isInterrupted();}});
        assertTrue(child.waiting.await(2,TimeUnit.SECONDS));owner.get().interrupt();
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(!marker.exists()&&!result.isDone()&&System.nanoTime()<until)Thread.sleep(5);
        assertTrue(marker.exists());assertFalse(result.isDone());child.finish.countDown();assertTrue(result.get(5,TimeUnit.SECONDS));assertEquals(0,child.signals);
    }
    @Test public void successfulReadWaitsForExitWithoutRequestingCancel()throws Exception{
        Future<EnvironmentSetup.Result> result=executor.submit(()->EnvironmentSetup.collectProcess(child,s->{},marker));
        assertTrue(child.waiting.await(2,TimeUnit.SECONDS));assertFalse(result.isDone());assertFalse(marker.exists());
        child.finish.countDown();assertTrue(result.get(5,TimeUnit.SECONDS).ready());assertFalse(marker.exists());assertEquals(0,child.signals);
    }
}
