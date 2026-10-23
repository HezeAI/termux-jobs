package com.termux.app.workspace;
import android.app.*;import android.os.*;import android.widget.*;import com.termux.app.TermuxActivity;import com.termux.R;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import java.lang.reflect.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopGpuTest {
 @Test public void selectionDefaultsAndPersistenceAreSeparatePerEnvironment(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.getSharedPreferences("workspace-x11",0).edit().clear().commit();assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"ubuntu"));a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:ubuntu","turnip").commit();assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"ubuntu"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"termux"));assertTrue(WorkspaceDesktopGpu.supported("ubuntu"));assertTrue(WorkspaceDesktopGpu.supported("termux"));assertTrue(WorkspaceDesktopGpu.supported("alpine"));assertTrue(WorkspaceDesktopGpu.supported("debian"));assertFalse(WorkspaceDesktopGpu.supported("ubuntu-ai"));a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:ubuntu","invalid").commit();assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"ubuntu"));}
 @Test public void addedEnvironmentsHaveSeparateComponentsAndPreferences(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();
  a.getSharedPreferences("workspace-x11",0).edit().clear().putString("renderer:debian","turnip").commit();
  assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"debian"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"alpine"));
  assertEquals("debian-13-mesa26.3",WorkspaceDesktopGpu.directory("debian"));assertEquals("alpine-3.24-mesa26.3",WorkspaceDesktopGpu.directory("alpine"));assertEquals("fedora-44-mesa26.3",WorkspaceDesktopGpu.directory("fedora"));assertTrue(WorkspaceDesktopGpu.supported("fedora"));
 }
 @Test public void voidGpuHasPrivateComponentsAndDoesNotChangeAnotherEnvironment(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();assertEquals("void-mesa26.3",WorkspaceDesktopGpu.directory("void"));assertTrue(WorkspaceDesktopGpu.supported("void"));
  a.getSharedPreferences("workspace-x11",0).edit().clear().putString("renderer:void","turnip").commit();assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"void"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"ubuntu"));
 }
 @Test public void rendererCannotChangeUntilStateKnownOrWhileRunning()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->false,new TextView(a),()->{},()->{});
  Field remote=WorkspaceDesktop.class.getDeclaredField("remote"),state=WorkspaceDesktop.class.getDeclaredField("state");remote.setAccessible(true);state.setAccessible(true);
  try{ui.menu();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();TextView row=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-renderer");assertNotNull(row);assertFalse(row.isEnabled());d.dismiss();remote.set(ui,new Binder());Bundle b=new Bundle();b.putBoolean("started",true);state.set(ui,b);ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();row=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-renderer");assertFalse(row.isEnabled());d.dismiss();b.putBoolean("started",false);b.putString("phase","installed");ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();row=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-renderer");assertTrue(row.isEnabled());d.dismiss();}finally{ui.close();}
 }
 @Test public void unsupportedGpuChoiceIsGrayAndBusyChooserDoesNotOpen(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktopGpu.choose(a,"ubuntu-ai",()->false,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertTrue(d.getListView().getAdapter().isEnabled(0));assertFalse(d.getListView().getAdapter().isEnabled(1));d.dismiss();WorkspaceDesktopGpu.choose(a,"ubuntu",()->true,()->fail());assertSame(d,ShadowAlertDialog.getLatestAlertDialog());assertFalse(d.isShowing());}
 @Test public void serviceRefusesLiveRendererChangeWithoutTouchingOtherServers()throws Exception {
  com.termux.x11.X11Service service=Robolectric.buildService(com.termux.x11.X11Service.class).get();
  Field target=com.termux.x11.X11Service.class.getDeclaredField("target"),started=com.termux.x11.X11Service.class.getDeclaredField("started"),renderer=com.termux.x11.X11Service.class.getDeclaredField("renderer");for(Field f:new Field[]{target,started,renderer})f.setAccessible(true);target.set(service,"termux");started.setBoolean(service,true);
  Method command=com.termux.x11.X11Service.class.getDeclaredMethod("command",Bundle.class);command.setAccessible(true);Bundle q=new Bundle();q.putString("op","launch");q.putString("target","termux");q.putString("renderer","turnip");
  try{command.invoke(service,q);fail("live change accepted");}catch(InvocationTargetException expected){assertTrue(expected.getCause() instanceof IllegalStateException);}assertEquals("cpu",renderer.get(service));assertTrue(started.getBoolean(service));
  q.putString("renderer","invalid");try{command.invoke(service,q);fail();}catch(InvocationTargetException expected){assertTrue(expected.getCause() instanceof IllegalArgumentException);}assertEquals("cpu",renderer.get(service));
  Field worker=com.termux.x11.X11Service.class.getDeclaredField("worker");worker.setAccessible(true);((java.util.concurrent.ExecutorService)worker.get(service)).shutdown();
 }

 @Test public void choosingRendererForLaunchDoesNotCommitItBeforeDesktopCheck(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.getSharedPreferences("workspace-x11",0).edit().clear().commit();String[] chosen={""};WorkspaceDesktopGpu.select(a,"debian",()->false,value->chosen[0]=value,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();d.getListView().performItemClick(d.getListView().getAdapter().getView(1,null,d.getListView()),1,1);org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("turnip",chosen[0]);assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"debian"));
 }

 @Test public void virglChoiceIsEnabledForAllEightDesktopsAndDoesNotStartAnyProcess(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.getSharedPreferences("workspace-x11",0).edit().clear().commit();
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
   String[] selected={""};WorkspaceDesktopGpu.select(a,target,()->false,v->selected[0]=v,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(target.equals("termux")?4:3,d.getListView().getAdapter().getCount());assertTrue(target,d.getListView().getAdapter().isEnabled(2));
   if(d.getListView().getAdapter().isEnabled(2)){d.getListView().performItemClick(d.getListView().getAdapter().getView(2,null,d.getListView()),2,2);org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("virgl",selected[0]);assertEquals("cpu",WorkspaceDesktopGpu.mode(a,target));}d.dismiss();
  }
 }

 @Test public void archGpuChoicesAndPreferencesStaySeparate(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();assertEquals("archlinux-mesa26.3",WorkspaceDesktopGpu.directory("archlinux"));
  a.getSharedPreferences("workspace-x11",0).edit().clear().putString("renderer:archlinux","virgl").commit();assertEquals("virgl",WorkspaceDesktopGpu.mode(a,"archlinux"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"void"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"ubuntu"));
  WorkspaceDesktopGpu.select(a,"archlinux",()->false,v->{},()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertTrue(d.getListView().getAdapter().isEnabled(1));assertTrue(d.getListView().getAdapter().isEnabled(2));d.dismiss();
 }

}
