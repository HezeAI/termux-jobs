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
 private final List<Bundle> calls=new CopyOnWriteArrayList<>();private Bundle state;
 private void set(String key,Object value)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField(key);f.setAccessible(true);f.set(ui,value);}
 @Before public void setup()throws Exception{
  activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);
  first=new WorkspaceStore.Session();first.id="one";first.handle="h1";first.environment="proot:debian";
  other=new WorkspaceStore.Session();other.id="two";other.handle="h2";other.environment="proot:debian";selected=new WorkspaceStore.Session[]{first};
  ui=new WorkspaceDesktop(activity,()->selected[0],()->false,new TextView(activity),()->{},()->{});
  state=new Bundle();state.putString("target","debian");state.putString("phase","running");state.putBoolean("started",true);state.putString("desktopOwner","one:h1");state.putStringArrayList("desktopOwners",new ArrayList<>(Arrays.asList("one:h1")));set("state",state);
  set("remote",new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){data.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);Bundle q=data.readBundle(getClass().getClassLoader());calls.add(q);reply.writeNoException();reply.writeBundle(state);return true;}});
 }
 @After public void close(){ui.close();}
 @Test public void presentationSwitchAndOtherSelectionNeverSendLifecycleCommands()throws Exception{
  ui.toggle();ui.toggle();selected[0]=other;ui.toggle();selected[0]=first;ui.toggle();ui.terminal(first);ui.sync();
  assertTrue(calls.isEmpty());assertTrue(ui.usesDesktop(first));assertFalse(ui.usesDesktop(other));assertTrue(ui.owns(first));
  state.getStringArrayList("desktopOwners").add("two:h2");assertTrue(ui.usesDesktop(other));assertFalse(ui.owns(first));assertEquals(2,ui.ownerCount());
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
}
