package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Implementation;
import static org.junit.Assert.*;

/** No installed executable is launched; only the real engine worker, files and callbacks run. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=WorkspaceBackupEngineTest.FixtureEnvironment.class)
public class WorkspaceBackupEngineTest {
 @Implements(com.termux.shared.shell.TermuxShellUtils.class) public static class FixtureEnvironment {
  @Implementation protected static String[] buildEnvironment(android.content.Context a,boolean failSafe,String cwd){return new String[]{"HOME="+a.getFilesDir().getAbsolutePath()};}
 }
 static final class Child extends Process {
  final CountDownLatch terminated=new CountDownLatch(1),released=new CountDownLatch(1),waited=new CountDownLatch(1);final AtomicInteger signals=new AtomicInteger();final boolean valid;volatile File work;
  Child(boolean valid,boolean exited){this.valid=valid;if(exited)released.countDown();}
  public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
  public InputStream getInputStream(){return new ByteArrayInputStream((valid?"{\"stage\":\"DONE\",\"result\":{\"fixture\":true}}\n":"invalid JSON\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  public InputStream getErrorStream(){return new ByteArrayInputStream(new byte[0]);}
  public int waitFor()throws InterruptedException{waited.countDown();released.await();return 0;}
  public int exitValue(){if(released.getCount()!=0)throw new IllegalThreadStateException();return 0;}
  public void destroy(){signals.incrementAndGet();terminated.countDown();}
 }
 private Activity activity;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();}
 private WorkspaceBackupEngine engine(Child child){return new WorkspaceBackupEngine(pb->{child.work=new File(pb.command().get(pb.command().size()-1)).getParentFile();return child;});}
 private WorkspaceBackupEngine.Callback callbacks(AtomicInteger success,AtomicInteger errors){return new WorkspaceBackupEngine.Callback(){public void progress(JSONObject e){}public void done(JSONObject r){success.incrementAndGet();}public void error(String c){errors.incrementAndGet();}};}
 private void drainUntil(AtomicInteger count)throws Exception{for(int i=0;i<100&&count.get()==0;i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertEquals(1,count.get());}
 private void finish(Child child,AtomicInteger count)throws Exception{child.released.countDown();drainUntil(count);}
 @Test public void failureCallbackAndPrivateWorkRemainPendingUntilOwnedProcessExit()throws Exception{
  Child child=new Child(false,false);WorkspaceBackupEngine engine=engine(child);AtomicInteger errors=new AtomicInteger(),success=new AtomicInteger();
  try{assertTrue(engine.start(activity,new JSONObject(),callbacks(success,errors)));assertTrue(child.terminated.await(3,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("terminal callback must wait for cleanup",0,errors.get());assertTrue(child.work.isDirectory());finish(child,errors);assertFalse(child.work.exists());assertEquals(0,success.get());}finally{child.released.countDown();for(int i=0;i<100&&child.work!=null&&child.work.exists();i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}}
 }
 @Test public void eightSecondCleanupTimeoutCannotAdmitASecondStorageOperation()throws Exception{
  Child child=new Child(false,false);AtomicInteger errors=new AtomicInteger();WorkspaceBackupEngine engine=engine(child);Child other=new Child(true,true);AtomicInteger second=new AtomicInteger();
  try{assertTrue(engine.start(activity,new JSONObject(),callbacks(new AtomicInteger(),errors)));assertTrue(child.terminated.await(3,TimeUnit.SECONDS));Thread.sleep(9000);
   assertFalse("shared storage lock must outlive the old eight-second grace",engine(other).start(activity,new JSONObject(),callbacks(second,second)));
   finish(child,errors);assertTrue(engine(other).start(activity,new JSONObject(),callbacks(second,second)));drainUntil(second);assertFalse(child.work.exists());
  }finally{child.released.countDown();other.released.countDown();for(int i=0;i<100;i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();if(child.work!=null&&!child.work.exists()&&(other.work==null||!other.work.exists()))break;}}
 }
 @Test public void cancelAfterCompletionWasQueuedStillDeliversOneTerminalCallback()throws Exception{
  Child child=new Child(true,true);AtomicInteger errors=new AtomicInteger(),success=new AtomicInteger();WorkspaceBackupEngine engine=engine(child);
  assertTrue(engine.start(activity,new JSONObject(),callbacks(success,errors)));assertTrue(child.waited.await(3,TimeUnit.SECONDS));for(int i=0;i<100&&child.work.exists();i++)Thread.sleep(10);assertFalse(child.work.exists());engine.cancel();drainUntil(success);assertEquals(0,errors.get());
 }
 @Test public void cancellationAndInterruptedCleanupKeepLockUntilOwnedChildExits()throws Exception{
  Child child=new Child(true,false);AtomicReference<Thread> worker=new AtomicReference<>();AtomicInteger success=new AtomicInteger(),errors=new AtomicInteger();AtomicReference<String> code=new AtomicReference<>();
  WorkspaceBackupEngine engine=new WorkspaceBackupEngine(pb->{worker.set(Thread.currentThread());child.work=new File(pb.command().get(pb.command().size()-1)).getParentFile();return child;});
  try{assertTrue(engine.start(activity,new JSONObject(),new WorkspaceBackupEngine.Callback(){public void progress(JSONObject e){}public void done(JSONObject r){success.incrementAndGet();}public void error(String c){code.set(c);errors.incrementAndGet();}}));
   assertTrue(child.waited.await(3,TimeUnit.SECONDS));engine.cancel();engine.cancel();assertTrue(child.terminated.await(3,TimeUnit.SECONDS));worker.get().interrupt();Thread.sleep(100);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("one signal lets Python finish child cleanup",1,child.signals.get());
   assertEquals(0,errors.get());assertTrue(child.work.exists());assertFalse(engine(new Child(true,true)).start(activity,new JSONObject(),callbacks(success,errors)));
   finish(child,errors);assertEquals("CANCELLED",code.get());assertEquals(0,success.get());assertFalse(child.work.exists());
  }finally{child.released.countDown();if(worker.get()!=null)worker.get().join(3000);}
 }
}
