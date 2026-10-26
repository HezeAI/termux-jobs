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

/** Download failures/cancellation cannot start X11 or expose an uncleared owned child. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows=X11ExtrasLifecycleTest.FakeChild.class)
public class X11ExtrasLifecycleTest {
 @Implements(DesktopProcess.class) public static class FakeChild {
  static CountDownLatch entered,closed,waiting,exit;
  @Implementation protected static DesktopProcess extras(Context c,File base){return Shadow.newInstanceOf(DesktopProcess.class);}
  @Implementation protected void read(Consumer<String> output)throws IOException{
   output.accept("EXTRA_STAGE:kali-style\nEXTRA_PROGRESS:100:1000\n");entered.countDown();
   throw new IOException("Synthetic download failure");
  }
  @Implementation protected void close(){closed.countDown();}
  @Implementation protected int waitFor()throws InterruptedException{
   waiting.countDown();if(!exit.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Fixture exit timeout");return 137;
  }
 }
 private X11Service service;private ExecutorService worker;
 @Before public void setup()throws Exception{
  FakeChild.entered=new CountDownLatch(1);FakeChild.closed=new CountDownLatch(1);FakeChild.waiting=new CountDownLatch(1);FakeChild.exit=new CountDownLatch(1);
  service=Robolectric.buildService(X11Service.class).get();
  Constructor<X11Runtime> c=X11Runtime.class.getDeclaredConstructor(File.class,File.class,File.class,File.class,byte[].class,X11Environment.class);c.setAccessible(true);
  ReflectionHelpers.setField(service,"runtime",c.newInstance(null,null,null,null,new byte[16],X11Environment.forTarget("termux")));
  ReflectionHelpers.setField(service,"target","termux");ReflectionHelpers.setField(service,"phase","preparing");ReflectionHelpers.setField(service,"termuxStyle",true);
  worker=ReflectionHelpers.getField(service,"worker");
 }
 @After public void cleanup()throws Exception{FakeChild.exit.countDown();worker.shutdown();assertTrue(worker.awaitTermination(5,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private void start()throws Exception{
  Method m=X11Service.class.getDeclaredMethod("prepare");m.setAccessible(true);m.invoke(service);
  assertTrue(FakeChild.entered.await(3,TimeUnit.SECONDS));assertTrue(FakeChild.waiting.await(3,TimeUnit.SECONDS));
 }
 private void finish()throws Exception{FakeChild.exit.countDown();worker.submit(()->{}).get(5,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
 @Test public void failedThemeWaitsForCleanupBeforeReportingFailure()throws Exception{
  start();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("preparing",service.snapshot().getString("phase"));
  assertEquals(100,service.snapshot().getLong("downloadBytes"));assertEquals(0,FakeChild.closed.getCount());
  finish();assertEquals("error",service.snapshot().getString("phase"));assertFalse(service.snapshot().getBoolean("ready"));assertNull(ReflectionHelpers.getField(service,"child"));
 }
 @Test public void cancellingDuringThemePreparationCannotStartServerOrPublishError()throws Exception{
  start();ReflectionHelpers.setField(service,"stopping",true);ReflectionHelpers.setField(service,"phase","stopping");
  finish();assertEquals("stopping",service.snapshot().getString("phase"));assertFalse(service.snapshot().getBoolean("ready"));assertNull(ReflectionHelpers.getField(service,"child"));
 }
}
