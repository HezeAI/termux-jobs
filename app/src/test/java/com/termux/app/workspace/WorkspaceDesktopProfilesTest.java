package com.termux.app.workspace;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopProfilesTest {
 private TermuxActivity a;private WorkspaceStore.Session p;private WorkspaceDesktop ui;
 private void set(String key,Object value)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField(key);f.setAccessible(true);f.set(ui,value);}
 private Object get(String key)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField(key);f.setAccessible(true);return f.get(ui);}
 private Object invoke(String key,Class<?>[] types,Object... args)throws Exception{Method m=WorkspaceDesktop.class.getDeclaredMethod(key,types);m.setAccessible(true);return m.invoke(ui,args);}
 private String profile()throws Exception{return (String)invoke("desktop",new Class<?>[]{WorkspaceStore.Session.class},p);}
 @Before public void setup(){a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.getSharedPreferences("workspace-x11",0).edit().clear().commit();p=new WorkspaceStore.Session();p.id="arch-owner";p.handle="arch-handle";p.environment="proot:archlinux";ui=new WorkspaceDesktop(a,()->p,()->false,new TextView(a),()->{},()->{});}
 @After public void cleanup(){ui.close();a.getSharedPreferences("workspace-x11",0).edit().clear().commit();}
 private View tagged(AlertDialog d,String tag){return d.getWindow().getDecorView().findViewWithTag(tag);}
 @Test public void twoProfilesAndRendererHeadingsAreOrderedUnderTheirOwnDesktop()throws Exception{
  set("remote",new Binder());ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  View xfce=tagged(d,"agent-status-desktop-xfce"),xrender=tagged(d,"agent-status-desktop-renderer"),plasma=tagged(d,"agent-status-desktop-plasma"),prender=tagged(d,"agent-status-desktop-wayland-linux-driver");
  assertNotNull(xfce);assertNotNull(plasma);assertNotNull(xrender);assertNotNull(prender);ViewGroup body=(ViewGroup)xfce.getParent();
  assertTrue(body.indexOfChild(xfce)<body.indexOfChild(tagged(d,"desktop-renderer-label-xfce")));
  assertTrue(body.indexOfChild(tagged(d,"desktop-renderer-label-xfce"))<body.indexOfChild(xrender));assertTrue(body.indexOfChild(xrender)<body.indexOfChild(plasma));
  assertTrue(body.indexOfChild(plasma)<body.indexOfChild(tagged(d,"desktop-renderer-label-plasma")));assertTrue(body.indexOfChild(tagged(d,"desktop-renderer-label-plasma"))<body.indexOfChild(prender));assertFalse(prender.isEnabled());d.dismiss();
 }
 @Test public void waylandImportAndSelectionAreAvailableBeforeDesktopInstallation()throws Exception{
  Assume.assumeTrue(com.termux.x11.DesktopProfiles.waylandEnabled());
  // No bound backend and no desktop/driver metadata: import must still be offered.
  assertNull(get("remote"));ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  assertTrue(tagged(d,"desktop-wayland-bionic").isEnabled());assertTrue(tagged(d,"desktop-wayland-driver-folder").isEnabled());assertTrue(tagged(d,"agent-status-desktop-wayland-selected-driver").isEnabled());d.dismiss();
  for(String phase:new String[]{"running","checking","installing","cancelling","preparing","stopping"}){
   Bundle state=new Bundle();state.putString("phase",phase);state.putBoolean("started",phase.equals("running"));set("state",state);ui.menu();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertFalse(phase,tagged(d,"desktop-wayland-bionic").isEnabled());assertFalse(phase,tagged(d,"desktop-wayland-driver-folder").isEnabled());d.dismiss();
  }
 }
 @Test public void x11RendererAndDriverImportAreAvailableBeforeInstallWithoutLaunchingAnything()throws Exception{
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
   p.environment=target.equals("termux")?"termux":"proot:"+target;assertNull(get("remote"));
   String mode=target.equals("termux")?"wrapper":target.equals("alpine")?"virgl":"bionic-glibc";
   invoke("selectRenderer",new Class<?>[]{WorkspaceStore.Session.class,String.class},p,mode);assertEquals(mode,WorkspaceDesktopGpu.mode(a,target));
   ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(target,tagged(d,"agent-status-desktop-renderer").isEnabled());
   if(!target.equals("alpine")){assertTrue(target,tagged(d,"desktop-wrapper-folder").isEnabled());assertTrue(target,tagged(d,"agent-status-desktop-wrapper-driver").isEnabled());}
   d.dismiss();assertEquals("",get("pending"));assertNull(get("remote"));
  }
 }
 @Test public void selectingPlasmaPreservesX11DriverAndStyleAndDoesNotAffectOtherTargets()throws Exception{
  Assume.assumeTrue(com.termux.x11.DesktopProfiles.waylandEnabled());
  a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:archlinux","bionic-glibc").putBoolean("termux-style:archlinux",true).commit();
  ((Set<String>)get("graphical")).add(p.id);invoke("chooseProfile",new Class<?>[]{WorkspaceStore.Session.class,String.class},p,"plasma");assertEquals("plasma",profile());
  assertEquals("turnip",invoke("renderer",new Class<?>[]{WorkspaceStore.Session.class},p));assertEquals(false,invoke("styleEnabled",new Class<?>[]{WorkspaceStore.Session.class},p));assertEquals("bionic-glibc",WorkspaceDesktopGpu.mode(a,"archlinux"));
  WorkspaceStore.Session debian=new WorkspaceStore.Session();debian.environment="proot:debian";assertEquals("xfce",invoke("desktop",new Class<?>[]{WorkspaceStore.Session.class},debian));
  invoke("chooseProfile",new Class<?>[]{WorkspaceStore.Session.class,String.class},p,"xfce");assertEquals("bionic-glibc",invoke("renderer",new Class<?>[]{WorkspaceStore.Session.class},p));assertEquals(true,invoke("styleEnabled",new Class<?>[]{WorkspaceStore.Session.class},p));
 }
 @Test public void activeOrCancellingProfileCannotBeSwitchedButCanBeReopened()throws Exception{
  for(String phase:new String[]{"running","checking","installing","cancelling","preparing","stopping"}){
   Bundle b=new Bundle();b.putString("target","archlinux");b.putString("desktop","xfce");b.putString("phase",phase);b.putBoolean("started",phase.equals("running"));set("state",b);
   invoke("chooseProfile",new Class<?>[]{WorkspaceStore.Session.class,String.class},p,"plasma");assertEquals("xfce",profile());assertFalse(a.getSharedPreferences("workspace-x11",0).contains("desktop-profile:archlinux"));
  }
 }
 @Test public void plasmaBinderRequestNeverUsesSavedX11DriverOrStyle()throws Exception{
  Assume.assumeTrue(com.termux.x11.DesktopProfiles.waylandEnabled());
  a.getSharedPreferences("workspace-x11",0).edit().putString("desktop-profile:archlinux","plasma").putString("renderer:archlinux","bionic-glibc").putBoolean("termux-style:archlinux",true).commit();
  java.util.concurrent.CountDownLatch seen=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<Bundle> request=new java.util.concurrent.atomic.AtomicReference<>();
  Binder fake=new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){data.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);request.set(data.readBundle(getClass().getClassLoader()));reply.writeNoException();reply.writeBundle(new Bundle());seen.countDown();return true;}};
  set("remote",fake);invoke("send",new Class<?>[]{String.class,WorkspaceStore.Session.class,String.class},"check",p,"");assertTrue(seen.await(3,java.util.concurrent.TimeUnit.SECONDS));
  assertEquals("archlinux",request.get().getString("target"));assertEquals("plasma",request.get().getString("desktop"));assertEquals("turnip",request.get().getString("renderer"));assertFalse(request.get().getBoolean("termuxStyle"));assertEquals("bionic-glibc",WorkspaceDesktopGpu.mode(a,"archlinux"));
 }
 @Test public void otherEnvironmentsDoNotExposeWaylandChoice()throws Exception{
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void"}){
   p.environment=target.equals("termux")?"termux":"proot:"+target;ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNull(tagged(d,"agent-status-desktop-plasma"));assertNotNull(tagged(d,"agent-status-desktop-renderer"));assertEquals("xfce",profile());d.dismiss();
  }
 }
 @Test public void plasmaSessionReportIncludesBoundedFailureSectionAndWaylandLabel()throws Exception{
  Assume.assumeTrue(com.termux.x11.DesktopProfiles.waylandEnabled());
  a.getSharedPreferences("workspace-x11",0).edit().putString("desktop-profile:archlinux","plasma").commit();Bundle b=new Bundle();b.putString("target","archlinux");b.putString("backend","wayland");b.putString("phase","ended");b.putString("waylandStartupErrors","kwin failed to initialize [path]");set("diagnosticState",b);String report=ui.sessionDiagnostic(p);assertTrue(report.startsWith("Wayland transport"));assertTrue(report.contains("Redacted Wayland startup errors"));assertTrue(report.contains("kwin failed to initialize [path]"));assertFalse(report.contains("X11 transport"));
 }
 @Test public void publicBuildDisablesEveryWaylandActionAndIgnoresSavedPreviewWithoutDeletingIt()throws Exception{
  Assume.assumeFalse(com.termux.x11.DesktopProfiles.waylandEnabled());
  a.getSharedPreferences("workspace-x11",0).edit().putString("desktop-profile:archlinux","plasma").putString("renderer:archlinux","bionic-glibc").commit();
  assertEquals("xfce",profile());assertEquals("bionic-glibc",invoke("renderer",new Class<?>[]{WorkspaceStore.Session.class},p));
  ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  for(String tag:new String[]{"agent-status-desktop-plasma","desktop-wayland-bionic","desktop-wayland-driver-folder","agent-status-desktop-wayland-selected-driver","agent-status-desktop-wayland-linux-driver"}){
   View v=tagged(d,tag);assertNotNull(tag,v);assertFalse(tag,v.isEnabled());assertTrue(tag,v.getAlpha()<1f);
  }
  invoke("chooseProfile",new Class<?>[]{WorkspaceStore.Session.class,String.class},p,"plasma");assertEquals("xfce",profile());assertEquals("",get("pending"));
  assertEquals("plasma",a.getSharedPreferences("workspace-x11",0).getString("desktop-profile:archlinux",""));d.dismiss();
 }
}
