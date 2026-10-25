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
 @Test public void everyDesktopMenuUsesTheExistingX11Controls()throws Exception{
  set("remote",new Binder());
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
   p.environment=target.equals("termux")?"termux":"proot:"+target;ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   assertNotNull(target,tagged(d,"agent-status-desktop-switch"));assertNotNull(target,tagged(d,"agent-status-desktop-renderer"));assertNotNull(target,tagged(d,"agent-status-desktop-screen"));
   assertNull(tagged(d,"agent-status-desktop-plasma"));assertNull(tagged(d,"agent-status-desktop-xfce"));assertNull(tagged(d,"desktop-wayland-driver-folder"));assertEquals("xfce",profile());d.dismiss();
  }
 }
 @Test public void savedRetiredProfileCannotRedirectArchOrReplaceItsX11Renderer()throws Exception{
  a.getSharedPreferences("workspace-x11",0).edit().putString("desktop-profile:archlinux","plasma").putString("renderer:archlinux","bionic-glibc").putBoolean("termux-style:archlinux",true).commit();
  assertEquals("xfce",profile());assertEquals("bionic-glibc",invoke("renderer",new Class<?>[]{WorkspaceStore.Session.class},p));
  String report=ui.sessionDiagnostic(p);assertTrue(report.startsWith("X11 transport"));assertFalse(report.contains("Wayland"));
  assertTrue(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style:archlinux",false));
 }
}
