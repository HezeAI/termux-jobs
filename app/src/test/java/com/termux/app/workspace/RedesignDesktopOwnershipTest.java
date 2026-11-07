package com.termux.app.workspace;
import android.app.*;
import android.os.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class RedesignDesktopOwnershipTest {
 private TermuxActivity activity;private WorkspaceDesktop ui;private WorkspaceStore.Session first,other;private WorkspaceStore.Session[] selected;
 private boolean visible;
 private final List<Bundle> calls=new CopyOnWriteArrayList<>();private Bundle state;
 private void set(String key,Object value)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField(key);f.setAccessible(true);f.set(ui,value);}
 @Before public void setup()throws Exception{
  activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);
  first=new WorkspaceStore.Session();first.id="one";first.handle="h1";first.environment="proot:debian";
  other=new WorkspaceStore.Session();other.id="two";other.handle="h2";other.environment="proot:debian";selected=new WorkspaceStore.Session[]{first};
  ui=new WorkspaceDesktop(activity,()->selected[0],()->visible,new TextView(activity),()->{},()->{});
  state=new Bundle();state.putString("target","debian");state.putString("phase","running");state.putBoolean("started",true);state.putString("desktopOwner","one:h1");state.putStringArrayList("desktopOwners",new ArrayList<>(Arrays.asList("one:h1")));set("state",state);
  set("remote",new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){data.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);Bundle q=data.readBundle(getClass().getClassLoader());calls.add(q);reply.writeNoException();reply.writeBundle(state);return true;}});
 }
 @After public void close(){ui.close();}
 @Test public void presentationSwitchAndOtherSelectionNeverSendLifecycleCommands()throws Exception{
  ui.toggle();ui.toggle();selected[0]=other;ui.toggle();selected[0]=first;ui.toggle();ui.terminal(first);ui.sync();
  assertTrue(calls.isEmpty());assertTrue(ui.usesDesktop(first));assertFalse(ui.usesDesktop(other));assertTrue(ui.owns(first));
  state.getStringArrayList("desktopOwners").add("two:h2");assertTrue(ui.usesDesktop(other));assertFalse(ui.owns(first));assertEquals(2,ui.ownerCount());
 }
 @Test public void headerCancelAndChangedSelectionNeverStartDesktop(){
  state.putBoolean("started",false);visible=true;ui.toggle();AlertDialog prompt=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(prompt);prompt.getButton(-2).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(calls.isEmpty());
  ui.toggle();prompt=ShadowAlertDialog.getLatestAlertDialog();selected[0]=other;prompt.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(calls.isEmpty());assertEquals("h1",first.handle);assertEquals("h2",other.handle);
 }
 @Test public void runningDesktopUsesOnlyAtomicOpenExistingAndReturnSendsNothing()throws Exception{
  visible=true;assertTrue(ui.running(first));ui.toggle();Field f=WorkspaceDesktop.class.getDeclaredField("calls");f.setAccessible(true);((ExecutorService)f.get(ui)).submit(()->{}).get(3,TimeUnit.SECONDS);
  assertEquals(1,calls.size());assertEquals("open-existing",calls.get(0).getString("op"));assertEquals("one:h1",calls.get(0).getString("owner"));ui.toggle();assertFalse(ui.showing());assertEquals(1,calls.size());
  state.putBoolean("started",false);ui.openExisting(first);assertEquals(1,calls.size());assertFalse(ui.running(first));
 }
 @Test public void cancellingStopAndStaleConfirmationSendNothing(){
  ui.stopDesktop(first);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);dialog.getButton(-2).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(calls.isEmpty());
  ui.stopDesktop(first);dialog=ShadowAlertDialog.getLatestAlertDialog();state.getStringArrayList("desktopOwners").add("two:h2");dialog.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(calls.isEmpty());
  state.getStringArrayList("desktopOwners").remove("two:h2");ui.stopDesktop(first);dialog=ShadowAlertDialog.getLatestAlertDialog();first.handle="new";dialog.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(calls.isEmpty());
 }
 @Test public void confirmationStaysWithOriginAfterSelectionChanges()throws Exception{
  ui.stopDesktop(first);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();selected[0]=other;dialog.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
  Field f=WorkspaceDesktop.class.getDeclaredField("calls");f.setAccessible(true);((ExecutorService)f.get(ui)).submit(()->{}).get(3,TimeUnit.SECONDS);
  assertEquals(1,calls.size());assertEquals("stop",calls.get(0).getString("op"));assertEquals("one:h1",calls.get(0).getString("owner"));assertEquals("debian",calls.get(0).getString("target"));assertEquals("h1",first.handle);assertEquals("h2",other.handle);
 }
 private String debugStop()throws Exception{StringBuilder text=new StringBuilder();for(String n:new String[]{"stoppingOwner","stoppingTarget","stoppingRemote"}){Field f=WorkspaceDesktop.class.getDeclaredField(n);f.setAccessible(true);text.append(n).append('=').append(f.get(ui)).append(' ');}return text+"state="+state+" calls="+calls;}
 private void drainCalls()throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField("calls");f.setAccessible(true);((ExecutorService)f.get(ui)).submit(()->{}).get(3,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();}
 @Test public void stopRemainsPendingAcrossStoppingSnapshotAndCompletesOnlyOnDisconnect()throws Exception{
  ui.stopDesktop(first);ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("Pending after confirm: "+debugStop(),ui.stopping(first));assertFalse(ui.owns(other));ui.stopDesktop(first);drainCalls();assertEquals(1,calls.stream().filter(b->"stop".equals(b.getString("op"))).count());assertEquals("h1",first.handle);
  Bundle stopping=new Bundle(state);stopping.putString("phase","stopping");stopping.putBoolean("started",false);Method accept=WorkspaceDesktop.class.getDeclaredMethod("accept",Bundle.class);accept.setAccessible(true);accept.invoke(ui,stopping);assertTrue("Pending after confirm: "+debugStop(),ui.stopping(first));
  Field f=WorkspaceDesktop.class.getDeclaredField("connection");f.setAccessible(true);((android.content.ServiceConnection)f.get(ui)).onServiceDisconnected(new android.content.ComponentName("fixture","fixture.Service"));assertFalse(ui.stopping(first));assertFalse(ui.usesDesktop(first));assertEquals("h1",first.handle);
 }
 @Test public void stopRpcFailureRestoresRetryAndKeepsSelectedTerminal()throws Exception{
  set("remote",new Binder(){@Override protected boolean onTransact(int c,Parcel d,Parcel r,int flags){throw new IllegalStateException("fixture ownership changed");}});ui.stopDesktop(first);ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();drainCalls();assertFalse(ui.stopping(first));assertTrue(ui.owns(first));assertEquals("h1",first.handle);
 }
 @Test public void stoppingRowIsDisabledAndOnlyHiddenAfterConfirmation(){TextView row=new TextView(activity);DesktopStopAction view=new DesktopStopAction(row);view.update(true,false,true,1);assertEquals(android.view.View.VISIBLE,row.getVisibility());assertFalse(row.isEnabled());assertTrue(row.getText().toString().contains("…"));assertNotNull(row.getCompoundDrawables()[0]);view.update(false,true,true,1);assertTrue(row.isEnabled());view.update(false,false,false,0);assertEquals(android.view.View.GONE,row.getVisibility());}

 @Test public void internalPreparationStopFailureRetainsItsOriginalErrorPath()throws Exception{
  set("preparationProfile",first);set("preparationCancelling",true);set("remote",new Binder(){@Override protected boolean onTransact(int code,Parcel d,Parcel r,int flags){d.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);calls.add(d.readBundle(getClass().getClassLoader()));throw new IllegalStateException("fixture preparation stop failed");}});
  Method send=WorkspaceDesktop.class.getDeclaredMethod("send",String.class,WorkspaceStore.Session.class);send.setAccessible(true);send.invoke(ui,"stop",first);drainCalls();assertFalse(ui.stopping(first));Field f=WorkspaceDesktop.class.getDeclaredField("state");f.setAccessible(true);assertEquals("error",((Bundle)f.get(ui)).getString("phase"));assertEquals(1,calls.size());assertEquals("stop",calls.get(0).getString("op"));assertEquals("h1",first.handle);
 }

}
