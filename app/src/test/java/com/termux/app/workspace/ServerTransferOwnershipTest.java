package com.termux.app.workspace;

import android.Manifest;
import android.app.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

/** Capture queued work without running it: no socket, SSH, installed shell or user file access. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class ServerTransferOwnershipTest {
 static final class Queue extends AbstractExecutorService {
  final List<Runnable> jobs=new ArrayList<>();boolean closed;
  public void execute(Runnable r){if(closed)throw new RejectedExecutionException();jobs.add(r);}
  public void shutdown(){closed=true;}public List<Runnable> shutdownNow(){closed=true;return new ArrayList<>(jobs);}
  public boolean isShutdown(){return closed;}public boolean isTerminated(){return closed;}public boolean awaitTermination(long n,TimeUnit unit){return closed;}
 }
 private TermuxActivity activity;
 @Before public void setup(){activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"en");Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);}
 static Field field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
 static Queue queue(Object o)throws Exception{Field f=field(o,"worker");((ExecutorService)f.get(o)).shutdown();Queue q=new Queue();f.set(o,q);return q;}
 static ServerProfiles.Profile profile(String name){ServerProfiles.Profile p=new ServerProfiles.Profile();p.name=name;p.host="example.invalid";p.user="fixture";return p;}
 static JSONObject entry()throws Exception{return new JSONObject().put("path","/fixture/file.txt").put("name","file.txt").put("size",1);}
 static AlertDialog latest(){return ShadowAlertDialog.getLatestAlertDialog();}
 static void yes(AlertDialog d){d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
 static void confirmUpload(WorkspaceServerUploads u)throws Exception{
  Class<?> type=Class.forName(WorkspaceServerUploads.class.getName()+"$Document");Constructor<?> ctor=type.getDeclaredConstructor(WorkspaceStore.Session.class,ProjectFiles.Entry.class);ctor.setAccessible(true);
  List<Object> docs=(List<Object>)field(u,"documents").get(u);docs.clear();docs.add(ctor.newInstance(new WorkspaceStore.Session(),new ProjectFiles.Entry("/fixture","file.txt",false,1)));
  Method confirm=u.getClass().getDeclaredMethod("confirm");confirm.setAccessible(true);confirm.invoke(u);
 }
 @Test public void connectionSettingsSnapshotDoesNotFollowLaterProfileEdits(){
  ServerProfiles.Profile p=profile("A");p.auth="key";p.identity="/fixture/key";p.encrypted="encrypted-fixture";p.port=2222;p.diagnosticsPaused=true;p.favorites.add("/fixture");
  ServerProfiles.Profile copy=ServerProfiles.connectionSnapshot(p);p.host="changed.invalid";p.identity="/changed";p.encrypted="changed";p.port=22;
  assertEquals(p.id,copy.id);assertEquals("example.invalid",copy.host);assertEquals("/fixture/key",copy.identity);assertEquals("encrypted-fixture",copy.encrypted);assertEquals(2222,copy.port);assertTrue(copy.diagnosticsPaused);assertTrue(copy.favorites.isEmpty());
 }
 @Test public void oldDownloadConfirmationCannotStartAfterAnotherServerWasChosen()throws Exception{
  WorkspaceServerDownloads d=new WorkspaceServerDownloads(activity);Queue q=queue(d);try{
   ServerProfiles.Profile a=profile("A"),b=profile("B");d.quick(a,entry());AlertDialog old=latest();d.quick(b,entry());AlertDialog current=latest();yes(old);assertEquals(0,q.jobs.size());assertFalse(d.presentation.active());
   yes(current);assertEquals(1,q.jobs.size());assertTrue(d.presentation.belongs(b.id));
  }finally{d.close();}
 }
 @Test public void dismissedDownloadOrMoveConfirmationCannotStartLater()throws Exception{
  WorkspaceServerDownloads d=new WorkspaceServerDownloads(activity);Queue q=queue(d);try{
   ServerProfiles.Profile p=profile("A");d.quick(p,entry());AlertDialog old=latest();old.dismiss();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();yes(old);assertEquals(0,q.jobs.size());
   d.move(p,Collections.singletonList(entry()),"/fixture/to",()->{});old=latest();old.dismiss();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();yes(old);assertEquals(0,q.jobs.size());
  }finally{d.close();}
 }
 @Test public void disconnectedDownloadConfirmationStaysInvalidAfterReconnect()throws Exception{
  WorkspaceServerDownloads d=new WorkspaceServerDownloads(activity);Queue q=queue(d);ServerProfiles.Profile p=profile("A");try{
   d.quick(p,entry());AlertDialog old=latest();d.disconnect(p.id);ServerConnection.allow(activity,p.id);yes(old);assertEquals(0,q.jobs.size());
   d.quick(p,entry());yes(latest());assertEquals(1,q.jobs.size());
  }finally{d.close();}
 }
 @Test public void moveConfirmationCannotQueueAfterControllerWasClosed()throws Exception{
  WorkspaceServerDownloads d=new WorkspaceServerDownloads(activity);Queue q=queue(d);d.move(profile("A"),Collections.singletonList(entry()),"/fixture/to",()->{});AlertDialog old=latest();d.close();yes(old);assertEquals(0,q.jobs.size());
 }
 @Test public void oldMoveConfirmationCannotSupersedeNewDownloadChoice()throws Exception{
  WorkspaceServerDownloads d=new WorkspaceServerDownloads(activity);Queue q=queue(d);try{
   d.move(profile("A"),Collections.singletonList(entry()),"/fixture/to",()->{});AlertDialog old=latest();d.quick(profile("B"),entry());yes(old);assertEquals(0,q.jobs.size());
  }finally{d.close();}
 }
 @Test public void oldUploadConfirmationCannotUseNewProfileAndPreviousDocuments()throws Exception{
  WorkspaceServerUploads u=new WorkspaceServerUploads(activity,()->{});Queue q=queue(u);try{
   u.pick(profile("A"),"/first");confirmUpload(u);AlertDialog old=latest();ServerProfiles.Profile b=profile("B");u.pick(b,"/second");yes(old);assertEquals(0,q.jobs.size());assertFalse(u.active());
   confirmUpload(u);yes(latest());assertEquals(1,q.jobs.size());assertTrue(u.presentation.belongs(b.id));
  }finally{u.close();}
 }
 @Test public void dismissedUploadConfirmationCannotStartLater()throws Exception{
  WorkspaceServerUploads u=new WorkspaceServerUploads(activity,()->{});Queue q=queue(u);try{
   u.pick(profile("A"),"/first");confirmUpload(u);AlertDialog old=latest();old.dismiss();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();yes(old);assertEquals(0,q.jobs.size());
  }finally{u.close();}
 }
 @Test public void oldUploadConfirmationCannotRestartAfterDisconnectAndReconnect()throws Exception{
  WorkspaceServerUploads u=new WorkspaceServerUploads(activity,()->{});Queue q=queue(u);ServerProfiles.Profile p=profile("A");try{
   u.pick(p,"/first");confirmUpload(u);AlertDialog old=latest();u.disconnect(p.id);ServerConnection.allow(activity,p.id);yes(old);assertEquals(0,q.jobs.size());
   u.pick(p,"/first");confirmUpload(u);yes(latest());assertEquals(1,q.jobs.size());
  }finally{u.close();}
 }
}
