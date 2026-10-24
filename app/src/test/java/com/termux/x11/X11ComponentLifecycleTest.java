package com.termux.x11;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

/** Exercise real service worker/callback ordering without launching X11 or installing packages. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows=X11ComponentLifecycleTest.FakeChild.class)
public class X11ComponentLifecycleTest {
 @Implements(DesktopProcess.class) public static class FakeChild {
  static CountDownLatch entered,closed,waiting,exit;static boolean readerFails;
  @Implementation protected static DesktopProcess start(Context c,File base,String op,String target,String token,String desktop,boolean filter,String renderer,boolean style){return Shadow.newInstanceOf(DesktopProcess.class);}
  @Implementation protected void read(Consumer<String> output)throws IOException{
   entered.countDown();if(!readerFails)try{if(!closed.await(5,TimeUnit.SECONDS))throw new IOException("Fixture close timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();}
   throw new IOException("Synthetic output failure");
  }
  @Implementation protected void close(){closed.countDown();}
  @Implementation protected int waitFor()throws InterruptedException{waiting.countDown();if(!exit.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Fixture exit timeout");return 137;}
 }
 private X11Service service;private ExecutorService worker;
 @Before public void setup()throws Exception{
  FakeChild.entered=new CountDownLatch(1);FakeChild.closed=new CountDownLatch(1);FakeChild.waiting=new CountDownLatch(1);FakeChild.exit=new CountDownLatch(1);FakeChild.readerFails=true;
  service=Robolectric.buildService(X11Service.class).get();Constructor<X11Runtime> constructor=X11Runtime.class.getDeclaredConstructor(File.class,File.class,File.class,File.class,byte[].class,X11Environment.class);constructor.setAccessible(true);ReflectionHelpers.setField(service,"runtime",constructor.newInstance(null,null,null,null,new byte[16],X11Environment.forTarget("termux")));ReflectionHelpers.setField(service,"target","termux");worker=ReflectionHelpers.getField(service,"worker");
 }
 @After public void cleanup()throws Exception{FakeChild.closed.countDown();FakeChild.exit.countDown();worker.shutdown();assertTrue(worker.awaitTermination(5,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private void start()throws Exception{Method m=X11Service.class.getDeclaredMethod("runComponents",String.class);m.setAccessible(true);m.invoke(service,"install");assertTrue(FakeChild.entered.await(3,TimeUnit.SECONDS));}
 private void finished()throws Exception{FakeChild.exit.countDown();worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
 @Test public void outputFailureDoesNotPublishRetryableStateBeforeOwnedCleanupFinishes()throws Exception{
  start();assertTrue("cleanup must wait for the owned guardian",FakeChild.waiting.await(3,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("installing",service.snapshot().getString("phase"));assertEquals(0,FakeChild.closed.getCount());finished();assertEquals("error",service.snapshot().getString("phase"));assertEquals("exception",service.snapshot().getString("componentResult"));
 }
 @Test public void cancellationRemainsPendingUntilOwnedChildExitAndThenBecomesRetryable()throws Exception{
  FakeChild.readerFails=false;start();BundleCall.call(service,"cancel");assertTrue(FakeChild.waiting.await(3,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("cancelling",service.snapshot().getString("phase"));finished();assertEquals("cancelled",service.snapshot().getString("phase"));assertFalse(service.snapshot().getBoolean("components"));
 }
 private static final class BundleCall {
  static void call(X11Service s,String op)throws Exception{Method m=X11Service.class.getDeclaredMethod("command",android.os.Bundle.class);m.setAccessible(true);m.invoke(s,X11Remote.command(op));}
 }
}
