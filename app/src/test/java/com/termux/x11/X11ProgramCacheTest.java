package com.termux.x11;

import android.app.Application;
import android.content.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

/** A cache hit must not create any guest process. Even a failing path cannot reach real tools. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=X11ProgramCacheTest.NoGuest.class)
public class X11ProgramCacheTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder(new File(System.getProperty("java.io.tmpdir")));
 @Implements(DesktopProcess.class) public static class NoGuest {
  static final AtomicInteger calls=new AtomicInteger();
  @Implementation protected static DesktopProcess programs(Context c,File base,String op,String target,String program)throws IOException{calls.incrementAndGet();throw new IOException("Test refuses guest launch");}
 }
 private Context context;private DesktopCatalogCache cache;
 @Before public void setup()throws Exception{File files=temp.newFolder();context=new ContextWrapper(RuntimeEnvironment.application){@Override public File getFilesDir(){return files;}};cache=new DesktopCatalogCache(new File(files,"workspace-x11-catalog"));NoGuest.calls.set(0);}
 private DesktopCatalog data(boolean installed){DesktopCatalog.Parser p=new DesktopCatalog.Parser();p.feed("CATALOG:{\"package\":\"mousepad\",\"name\":\"Mousepad\",\"summary\":\"Editor\",\"installed\":"+installed+",\"available\":true}\nREADY:catalog\nDESKTOP_CHILD_EXIT=0\n");return p.result();}
 private void drain(X11ProgramInstaller installer)throws Exception{ExecutorService worker=ReflectionHelpers.getField(installer,"worker");worker.submit(()->{}).get(10,TimeUnit.SECONDS);}
 @Test public void reopeningAndSwitchingEnvironmentUseTheirOwnPersistentListsWithoutGuestCommands()throws Exception{
  cache.save("ubuntu",false,data(false));cache.save("jobs-x11-ubuntu",true,data(true));X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{installer.start("catalog","ubuntu","");drain(installer);assertTrue(installer.snapshot().getBoolean("cacheHit"));assertTrue(installer.snapshot().getBoolean("catalogReady"));installer.start("installed","jobs-x11-ubuntu","");drain(installer);assertTrue(installer.snapshot().getBoolean("installedReady"));assertEquals(1,installer.page("jobs-x11-ubuntu","",0,true).getInt("total"));assertEquals(0,NoGuest.calls.get());}finally{installer.close();}
  installer=new X11ProgramInstaller(context);try{installer.start("catalog","ubuntu","");drain(installer);assertEquals("complete",installer.snapshot().getString("phase"));assertEquals(0,NoGuest.calls.get());}finally{installer.close();}
 }
 @Test public void debianCatalogAndInstalledCachesAreIndependentFromUbuntu()throws Exception{
  cache.save("ubuntu",false,data(false));cache.save("debian",false,data(true));cache.save("debian",true,data(true));X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{installer.start("catalog","debian","");drain(installer);assertTrue(installer.snapshot().getBoolean("cacheHit"));assertEquals(1,installer.page("debian","",0,false).getInt("total"));
   try{installer.page("ubuntu","",0,false);fail("Wrong rootfs page");}catch(IllegalStateException expected){}
   installer.start("installed","debian","");drain(installer);assertTrue(installer.snapshot().getBoolean("installedReady"));assertEquals(0,NoGuest.calls.get());
   cache.invalidate("debian",false);assertNotNull(cache.load("ubuntu",false));assertNotNull(cache.load("debian",true));
  }finally{installer.close();}
 }
 @Test public void explicitRefreshBypassesCacheAndAFailedScanKeepsTheLastCompleteDiskSnapshot()throws Exception{
  cache.save("ubuntu",false,data(false));X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{installer.start("catalog","ubuntu","",true);drain(installer);assertEquals(1,NoGuest.calls.get());assertEquals("error",installer.snapshot().getString("phase"));assertNotNull(cache.load("ubuntu",false));installer.start("catalog","ubuntu","");drain(installer);assertEquals("complete",installer.snapshot().getString("phase"));assertEquals(1,NoGuest.calls.get());}finally{installer.close();}
 }
 @Test public void recommendedDeletionRequiresVerifiedInstallationInTheSameEnvironment()throws Exception{
  X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{
   android.os.Bundle state=new android.os.Bundle(),items=new android.os.Bundle();state.putString("target","alpine");state.putString("phase","complete");items.putBoolean("images",true);state.putBundle("items",items);ReflectionHelpers.setField(installer,"state",state);
   for(String[] request:new String[][]{{"alpine","pkg:thunar"},{"alpine","pkg:evince"},{"debian","pkg:ristretto"},{"alpine","pkg:--bad"}}){try{installer.start("remove",request[0],request[1]);fail("Unverified deletion accepted");}catch(IllegalArgumentException expected){}}
   assertEquals(0,NoGuest.calls.get());installer.start("remove","alpine","pkg:ristretto");drain(installer);assertEquals(1,NoGuest.calls.get());assertEquals("error",installer.snapshot().getString("phase"));
  }finally{installer.close();}
 }
 @Test public void recommendedCacheSurvivesRestartIsTargetScopedAndExplicitlyRefreshed()throws Exception{
  android.os.Bundle items=new android.os.Bundle();for(String id:DesktopPrograms.IDS)items.putBoolean(id,id.equals("editor"));cache.saveItems("opensuse",items);
  X11ProgramInstaller installer=new X11ProgramInstaller(context);
  try{installer.start("check","opensuse","");drain(installer);assertTrue(installer.snapshot().getBoolean("cacheHit"));assertTrue(installer.snapshot().getBundle("items").getBoolean("editor"));assertEquals(0,NoGuest.calls.get());}finally{installer.close();}
  installer=new X11ProgramInstaller(context);
  try{
   installer.start("check","opensuse","");drain(installer);assertEquals(0,NoGuest.calls.get());
   installer.start("check","opensuse","",true);drain(installer);assertEquals("error",installer.snapshot().getString("phase"));assertEquals(1,NoGuest.calls.get());assertNotNull(cache.loadItems("opensuse"));
   installer.start("check","debian","");drain(installer);assertEquals(2,NoGuest.calls.get());
   installer.start("install","opensuse","office");drain(installer);assertNull(cache.loadItems("opensuse"));
   installer.start("check","opensuse","");drain(installer);assertEquals(4,NoGuest.calls.get());
  }finally{installer.close();}
 }
 @Test public void recommendedCacheRejectsIncompleteInvalidAndLinkedSnapshots()throws Exception{
  File dir=new File(context.getFilesDir(),"workspace-x11-catalog");dir.mkdirs();File file=new File(dir,"opensuse-recommended-media2.jsonl");
  for(String bad:new String[]{"{}","{\"editor\":true}","{broken"}){java.nio.file.Files.write(file.toPath(),bad.getBytes(java.nio.charset.StandardCharsets.UTF_8));assertNull(cache.loadItems("opensuse"));}
  File outside=temp.newFile();java.nio.file.Files.write(outside.toPath(),"unchanged".getBytes(java.nio.charset.StandardCharsets.UTF_8));assertTrue(file.delete());java.nio.file.Files.createSymbolicLink(file.toPath(),outside.toPath());
  assertNull(cache.loadItems("opensuse"));cache.saveItems("opensuse",null);assertEquals("unchanged",new String(java.nio.file.Files.readAllBytes(outside.toPath()),java.nio.charset.StandardCharsets.UTF_8));
 }

}
