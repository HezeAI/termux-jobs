package com.termux.x11;

import android.app.Application;
import android.content.*;
import android.os.Bundle;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=DesktopProgramResumeTest.FakeChild.class)
public class DesktopProgramResumeTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder(new File(System.getProperty("java.io.tmpdir")));
 @Implements(DesktopProcess.class) public static class FakeChild {
  static String operation;static boolean fail,prepared,readerFails;static int calls,waited,closed;
  @Implementation protected static DesktopProcess programs(Context c,File base,String op,String target,String id){operation=op;calls++;return Shadow.newInstanceOf(DesktopProcess.class);}
  @Implementation protected void read(Consumer<String> output){if(readerFails)throw new IllegalStateException("Test output failure");if(prepared)output.accept("JOBS_PROGRAM_INDEX_"+"READY\n");if(!fail){for(String id:DesktopPrograms.IDS)output.accept("PROGRAM:"+id+":missing\n");output.accept("READY:programs\nDESKTOP_CHILD_EXIT=0\n");}}
  @Implementation protected int waitFor(){waited++;return fail?1:137;}
  @Implementation protected void close(){closed++;}
 }
 private Context context;private DesktopProgramResume journal;
 @Before public void setup()throws Exception{File files=temp.newFolder();context=new ContextWrapper(RuntimeEnvironment.application){@Override public File getFilesDir(){return files;}};journal=new DesktopProgramResume(new File(files,"workspace-x11-program-resume"));FakeChild.calls=0;FakeChild.waited=0;FakeChild.closed=0;FakeChild.readerFails=false;FakeChild.operation="";FakeChild.prepared=true;FakeChild.fail=true;}
 private void drain(X11ProgramInstaller installer)throws Exception{ExecutorService worker=ReflectionHelpers.getField(installer,"worker");worker.submit(()->{}).get(10,TimeUnit.SECONDS);}
 @Test public void failedPreparedInstallSurvivesRestartAndOnlyExplicitRetrySkipsRefresh()throws Exception{
  X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{installer.start("install","void","editor");drain(installer);assertEquals("install",FakeChild.operation);assertEquals("error",installer.snapshot().getString("phase"));assertTrue(journal.read("void").getBoolean("editor"));assertTrue(journal.read("ubuntu").isEmpty());}finally{installer.close();}
  installer=new X11ProgramInstaller(context);
  try{installer.scope("void");assertEquals("void",installer.snapshot().getString("target"));assertTrue(installer.snapshot().getBundle("resumePrograms").containsKey("editor"));assertEquals(1,FakeChild.calls);FakeChild.fail=false;installer.start("install","void","editor");drain(installer);assertEquals("resume",FakeChild.operation);assertEquals("complete",installer.snapshot().getString("phase"));assertTrue(new DesktopProgramResume(new File(context.getFilesDir(),"workspace-x11-program-resume")).read("void").isEmpty());}finally{installer.close();}
 }
 @Test public void failedRefreshKeepsConsentButCannotSkipRefreshAndUnknownPackagesStillRejected()throws Exception{
  FakeChild.prepared=false;X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{installer.start("install","debian","editor");drain(installer);assertTrue(journal.read("debian").containsKey("editor"));assertFalse(journal.read("debian").getBoolean("editor"));installer.start("install","debian","editor");drain(installer);assertEquals("install",FakeChild.operation);
   try{installer.start("install","debian","pkg:unreviewed");fail();}catch(IllegalArgumentException expected){}
   ((DesktopProgramResume)ReflectionHelpers.getField(installer,"resume")).put("debian","pkg:mousepad",true);installer.start("install","debian","pkg:mousepad");drain(installer);assertEquals("resume",FakeChild.operation);
   try{installer.start("install","ubuntu","pkg:mousepad");fail();}catch(IllegalArgumentException expected){}
  }finally{installer.close();}
 }
 @Test public void cancellationBeforeGuestLaunchRetainsPendingRequestWithoutRunningAnything()throws Exception{
  X11ProgramInstaller installer=new X11ProgramInstaller(context);ExecutorService worker=ReflectionHelpers.getField(installer,"worker");CountDownLatch blocked=new CountDownLatch(1),release=new CountDownLatch(1);worker.execute(()->{blocked.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});assertTrue(blocked.await(5,TimeUnit.SECONDS));
  try{installer.start("install","alpine","editor");installer.cancel();release.countDown();drain(installer);assertEquals(0,FakeChild.calls);assertTrue(journal.read("alpine").containsKey("editor"));assertEquals("cancelled",installer.snapshot().getString("phase"));}finally{release.countDown();installer.close();}
 }
 @Test public void invalidStateAndSymlinksCannotAuthorizePackagesOrOverwriteOtherFiles()throws Exception{
  File dir=new File(context.getFilesDir(),"workspace-x11-program-resume");dir.mkdirs();File file=new File(dir,"ubuntu.json");Files.write(file.toPath(),"{\"pkg:../secret\":true}".getBytes());assertTrue(journal.read("ubuntu").isEmpty());Files.delete(file.toPath());File other=temp.newFile();Files.write(other.toPath(),"keep".getBytes());Files.createSymbolicLink(file.toPath(),other.toPath());assertTrue(journal.read("ubuntu").isEmpty());try{journal.put("ubuntu","editor",true);fail();}catch(IllegalStateException expected){}assertEquals("keep",new String(Files.readAllBytes(other.toPath())));
 }
 @Test public void outputFailureStillClosesAndWaitsForOwnedJobBeforeRetry()throws Exception{
  FakeChild.readerFails=true;X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{installer.start("install","void","editor");drain(installer);assertEquals("error",installer.snapshot().getString("phase"));assertEquals(1,FakeChild.closed);assertEquals(1,FakeChild.waited);assertTrue(journal.read("void").containsKey("editor"));FakeChild.readerFails=false;FakeChild.fail=false;installer.start("install","void","editor");drain(installer);assertEquals("complete",installer.snapshot().getString("phase"));}finally{installer.close();}
 }

 @Test public void failedRemovalRetainsOtherCardsButRechecksAffectedProgram()throws Exception{
  X11ProgramInstaller installer=new X11ProgramInstaller(context);Bundle state=new Bundle(),items=new Bundle();state.putString("target","opensuse");state.putString("phase","complete");for(String id:DesktopPrograms.IDS)items.putBoolean(id,true);state.putBundle("items",items);ReflectionHelpers.setField(installer,"state",state);
  try{installer.start("remove","opensuse","pkg:evince");drain(installer);Bundle result=installer.snapshot();assertEquals("error",result.getString("phase"));Bundle retained=result.getBundle("items");assertTrue(retained.getBoolean("editor"));assertTrue(retained.getBoolean("media"));assertFalse(retained.containsKey("pdf"));assertTrue(items.getBoolean("pdf"));}finally{installer.close();}
 }

}
