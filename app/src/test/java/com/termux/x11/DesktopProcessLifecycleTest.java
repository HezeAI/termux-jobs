package com.termux.x11;
import android.app.Application;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class DesktopProcessLifecycleTest {
 private DesktopProcess owned(Process p)throws Exception{Constructor<DesktopProcess> c=DesktopProcess.class.getDeclaredConstructor(Process.class);c.setAccessible(true);return c.newInstance(p);}
 @Test public void outputFailureClosesAndReapsRealGuardianWithoutStoppingAnotherProcess()throws Exception{
  String source=System.getenv("TERMUX_FORK_SOURCE_ROOT");assertNotNull("Use scripts/build-env.sh for fixture source path",source);
  File guardian=new File(source,"x11/src/main/assets/x11-guardian.sh");assertTrue(guardian.isFile());
  Process sentinel=new ProcessBuilder("sleep","30").start(),child=null;
  try{
   child=new ProcessBuilder("setsid","bash",guardian.getPath(),"bash","-c","trap '' TERM; printf 'ready\\n'; while :; do sleep 30; done").redirectErrorStream(true).start();DesktopProcess process=owned(child);
   try{process.read(chunk->{throw new IllegalStateException("Synthetic output consumer failure");});fail("Expected output failure");}catch(IllegalStateException expected){assertEquals("Synthetic output consumer failure",expected.getMessage());}finally{process.closeAndWait();}
   assertFalse("cleanup must reap the guardian before allowing another job",child.isAlive());assertEquals(137,child.exitValue());assertTrue("unrelated process must survive",sentinel.isAlive());
  }finally{if(child!=null&&child.isAlive()){child.getOutputStream().close();assertTrue(child.waitFor(5,TimeUnit.SECONDS));}sentinel.destroy();assertTrue(sentinel.waitFor(5,TimeUnit.SECONDS));}
 }
 @Test public void interruptedWaitStillCompletesCleanupAndRestoresInterruptFlag()throws Exception{
  final boolean[] closed={false},finished={false};Process p=new Process(){
   int waits;
   public OutputStream getOutputStream(){return new ByteArrayOutputStream(){public void close(){closed[0]=true;}};}
   public InputStream getInputStream(){return new ByteArrayInputStream(new byte[0]);}
   public InputStream getErrorStream(){return getInputStream();}
   public int waitFor()throws InterruptedException{assertTrue(closed[0]);if(waits++==0)throw new InterruptedException("fixture interruption");finished[0]=true;return 137;}
   public int exitValue(){if(!finished[0])throw new IllegalThreadStateException();return 137;}
   public void destroy(){fail("must use the pipe-owned guardian, not an inferred PID");}
  };
  try{owned(p).closeAndWait();assertTrue(finished[0]);assertTrue(Thread.currentThread().isInterrupted());}finally{Thread.interrupted();}
 }
}
