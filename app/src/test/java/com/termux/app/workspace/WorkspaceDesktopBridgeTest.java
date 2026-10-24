package com.termux.app.workspace;
import android.app.*;
import android.os.*;
import android.view.View;
import android.widget.TextView;
import com.termux.app.TermuxActivity;
import com.termux.R;
import java.lang.reflect.*;
import java.util.concurrent.ExecutorService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopBridgeTest {
 private Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 private TermuxActivity activity(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.getSharedPreferences("workspace-x11",0).edit().clear().commit();return a;}
 @Test public void fourthChoiceIsSeparateUbuntuBridgeWithIndependentCache(){
  TermuxActivity a=activity();String[] selected={""};WorkspaceDesktopGpu.select(a,"ubuntu",()->false,c->selected[0]=c,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();
  assertEquals(4,d.getListView().getAdapter().getCount());assertEquals("Bionic for glibc (libhybris + Jobs bridge)",d.getListView().getAdapter().getItem(3).toString());
  d.getListView().performItemClick(d.getListView().getAdapter().getView(3,null,d.getListView()),3,3);assertEquals("bionic-glibc",selected[0]);
  assertNotEquals(WorkspaceDesktopWrapper.base(a),WorkspaceDesktopWrapper.base(a,"ubuntu"));
  for(String target:new String[]{"ubuntu","termux","debian","alpine","fedora","opensuse","void","archlinux","ubuntu-ai"}){
   a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,"bionic-glibc").commit();assertEquals(target,(target.equals("ubuntu")||target.equals("debian")||target.equals("fedora")||target.equals("archlinux")||target.equals("opensuse")||target.equals("void"))?"bionic-glibc":"cpu",WorkspaceDesktopGpu.mode(a,target));
  }
 }
 @Test public void bridgeFolderAppearsOnlyForSelectedModeAndLocksWhenRunning()throws Exception{
  TermuxActivity a=activity();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";
  a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:ubuntu","bionic-glibc").commit();
  WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
  try{
   field(WorkspaceDesktop.class,"remote").set(ui,new Binder());Bundle state=new Bundle();state.putString("phase","installed");field(WorkspaceDesktop.class,"state").set(ui,state);
   ui.menu();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();View folder=d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder");assertNotNull(folder);assertTrue(folder.isEnabled());d.dismiss();
   state.putBoolean("started",true);ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();assertFalse(d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder").isEnabled());d.dismiss();
   state.putBoolean("started",false);a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:ubuntu","cpu").commit();ui.menu();d=ShadowAlertDialog.getLatestAlertDialog();assertNull(d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder"));d.dismiss();
  }finally{ui.close();}
 }
 @Test public void debianBridgeKeepsItsOwnDriverRootPreferenceAndUiChannel()throws Exception{
  TermuxActivity a=activity();assertNotEquals(WorkspaceDesktopWrapper.base(a,"ubuntu"),WorkspaceDesktopWrapper.base(a,"debian"));assertNotEquals(WorkspaceDesktopWrapper.base(a),WorkspaceDesktopWrapper.base(a,"debian"));
  assertEquals("workspace-x11-bionic-glibc",WorkspaceDesktopWrapper.base(a,"ubuntu").getName());assertEquals("workspace-x11-bionic-glibc-debian",WorkspaceDesktopWrapper.base(a,"debian").getName());
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:debian";a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:debian","bionic-glibc").putString("renderer:ubuntu","turnip").commit();
  WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
  try{Object wrapper=field(WorkspaceDesktop.class,"wrapper").get(ui);assertEquals("debian",field(WorkspaceDesktopWrapper.class,"target").get(wrapper));field(WorkspaceDesktop.class,"remote").set(ui,new Binder());Bundle state=new Bundle();state.putString("phase","installed");field(WorkspaceDesktop.class,"state").set(ui,state);ui.menu();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder"));d.dismiss();assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"ubuntu"));}finally{ui.close();}
 }
 @Test public void fedoraBridgeHasFourthChoiceAndIndependentCacheAndPreference(){
  TermuxActivity a=activity();String[] chosen={""};WorkspaceDesktopGpu.select(a,"fedora",()->false,v->chosen[0]=v,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(4,d.getListView().getAdapter().getCount());d.getListView().performItemClick(d.getListView().getAdapter().getView(3,null,d.getListView()),3,3);assertEquals("bionic-glibc",chosen[0]);
  assertEquals("workspace-x11-bionic-glibc-fedora",WorkspaceDesktopWrapper.base(a,"fedora").getName());assertNotEquals(WorkspaceDesktopWrapper.base(a,"ubuntu"),WorkspaceDesktopWrapper.base(a,"fedora"));assertNotEquals(WorkspaceDesktopWrapper.base(a,"debian"),WorkspaceDesktopWrapper.base(a,"fedora"));
  a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:fedora","bionic-glibc").commit();assertEquals("bionic-glibc",WorkspaceDesktopGpu.mode(a,"fedora"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"ubuntu"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"debian"));
 }
 @Test public void archBridgeKeepsItsOwnCachePreferenceAndDesktopUiChannel()throws Exception{
  TermuxActivity a=activity();assertEquals("workspace-x11-bionic-glibc-archlinux",WorkspaceDesktopWrapper.base(a,"archlinux").getName());for(String t:new String[]{"ubuntu","debian","fedora","termux"})assertNotEquals(WorkspaceDesktopWrapper.base(a,t),WorkspaceDesktopWrapper.base(a,"archlinux"));
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:archlinux";a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:archlinux","bionic-glibc").putString("renderer:fedora","turnip").commit();WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
  try{assertEquals("archlinux",field(WorkspaceDesktopWrapper.class,"target").get(field(WorkspaceDesktop.class,"wrapper").get(ui)));field(WorkspaceDesktop.class,"remote").set(ui,new Binder());Bundle state=new Bundle();state.putString("phase","installed");field(WorkspaceDesktop.class,"state").set(ui,state);ui.menu();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder"));d.dismiss();assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"fedora"));}finally{ui.close();}
 }
 @Test public void newGlibcTargetsKeepTheirOwnDriverUiAndPreferences()throws Exception{
  TermuxActivity a=activity();for(String target:new String[]{"opensuse","void"}){
   assertEquals("workspace-x11-bionic-glibc-"+target,WorkspaceDesktopWrapper.base(a,target).getName());for(String other:new String[]{"ubuntu","debian","fedora","archlinux","termux"})assertNotEquals(WorkspaceDesktopWrapper.base(a,other),WorkspaceDesktopWrapper.base(a,target));
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:"+target;a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,"bionic-glibc").commit();WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
   try{assertEquals(target,field(WorkspaceDesktopWrapper.class,"target").get(field(WorkspaceDesktop.class,"wrapper").get(ui)));field(WorkspaceDesktop.class,"remote").set(ui,new Binder());Bundle state=new Bundle();state.putString("phase","installed");field(WorkspaceDesktop.class,"state").set(ui,state);ui.menu();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d.getWindow().getDecorView().findViewWithTag("desktop-wrapper-folder"));d.dismiss();}finally{ui.close();}
  }
 }
 @Test public void serviceRejectsBridgeForOtherTargetsWithoutChangingRenderer()throws Exception{
  Class<?> type=com.termux.x11.X11Service.class;com.termux.x11.X11Service service=Robolectric.buildService(com.termux.x11.X11Service.class).get();Method command=type.getDeclaredMethod("command",Bundle.class);command.setAccessible(true);
  try{for(String target:new String[]{"termux","alpine"}){
   Bundle q=new Bundle();q.putString("op","check");q.putString("target",target);q.putString("renderer","bionic-glibc");try{command.invoke(service,q);fail(target);}catch(InvocationTargetException e){assertTrue(e.getCause() instanceof IllegalArgumentException);}assertEquals("cpu",field(type,"renderer").get(service));
  }}finally{((ExecutorService)field(type,"worker").get(service)).shutdown();}
 }
}
