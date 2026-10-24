package com.termux.app.workspace;

import android.app.*;
import android.os.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopStyleTest {
 private TermuxActivity a;private WorkspaceDesktop ui;private WorkspaceStore.Session p;
 private void set(String name,Object value)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField(name);f.setAccessible(true);f.set(ui,value);}
 private AlertDialog menu(){ui.menu();return ShadowAlertDialog.getLatestAlertDialog();}
 private CheckBox checkbox(AlertDialog d){return d.getWindow().getDecorView().findViewWithTag("desktop-termux-style");}
 @Before public void setup()throws Exception{
  a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.getSharedPreferences("workspace-x11",0).edit().clear().commit();
  p=new WorkspaceStore.Session();p.environment="termux";ui=new WorkspaceDesktop(a,()->p,()->false,new TextView(a),()->{},()->{});set("remote",new Binder());
 }
 @After public void close(){ui.close();a.getSharedPreferences("workspace-x11",0).edit().clear().commit();}
 @Test public void optInPersistsAndIsPlacedAfterSwitchBeforeRenderer(){
  AlertDialog d=menu();CheckBox c=checkbox(d);assertNotNull(c);assertTrue(c.isEnabled());assertFalse(c.isChecked());
  android.view.ViewGroup body=(android.view.ViewGroup)c.getParent().getParent();
  android.view.View change=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-switch"),renderer=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-renderer");
  assertTrue(body.indexOfChild(change)<body.indexOfChild((android.view.View)c.getParent()));assertTrue(body.indexOfChild((android.view.View)c.getParent())<body.indexOfChild(renderer));
  c.performClick();assertTrue(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style",false));d.dismiss();
  d=menu();assertTrue(checkbox(d).isChecked());d.dismiss();
 }
 @Test public void busyAndDisconnectedStatesCannotChangeSavedStyle()throws Exception{
  for(String phase:new String[]{"checking","installing","preparing","stopping","running"}){
   Bundle b=new Bundle();b.putString("phase",phase);b.putBoolean("started",phase.equals("running"));set("state",b);
   AlertDialog d=menu();CheckBox c=checkbox(d);assertFalse(phase,c.isEnabled());c.setChecked(true);assertFalse(c.isChecked());assertFalse(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style",false));d.dismiss();
  }
  set("state",new Bundle());set("remote",null);AlertDialog d=menu();assertFalse(checkbox(d).isEnabled());d.dismiss();
 }
 @Test public void unsupportedGuestsNeverShowStyle(){
  a.getSharedPreferences("workspace-x11",0).edit().putBoolean("termux-style",true).commit();
  for(String target:new String[]{"ubuntu-ai","jobs-ai-ubuntu","jobs-x11-ubuntu","unknown","../ubuntu"}){
   p.environment="proot:"+target;AlertDialog d=menu();assertNull(target,checkbox(d));assertNull(d.getWindow().getDecorView().findViewWithTag("desktop-touch-workspace"));d.dismiss();
  }
 }
 @Test public void ubuntuStyleHasIndependentOptInAndBusyGuard()throws Exception{
  a.getSharedPreferences("workspace-x11",0).edit().putBoolean("termux-style",true).commit();
  p.environment="ubuntu";AlertDialog d=menu();CheckBox c=checkbox(d);assertNotNull(c);assertFalse(c.isChecked());
  c.performClick();assertTrue(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style:ubuntu",false));d.dismiss();
  p.environment="termux";d=menu();assertTrue(checkbox(d).isChecked());checkbox(d).performClick();d.dismiss();
  p.environment="proot:ubuntu";d=menu();assertTrue(checkbox(d).isChecked());d.dismiss();
  for(String phase:new String[]{"checking","installing","cancelling","preparing","stopping","running"}){
   Bundle b=new Bundle();b.putString("phase",phase);b.putBoolean("started",phase.equals("running"));set("state",b);
   d=menu();c=checkbox(d);assertFalse(phase,c.isEnabled());c.setChecked(false);assertTrue(c.isChecked());d.dismiss();
   assertTrue(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style:ubuntu",false));
   assertFalse(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style",true));
  }
 }
 @Test public void everyDesktopHasItsOwnStyleSelectionAndBusyGuard()throws Exception{
  String[] targets={"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"};
  for(String target:targets){
   p.environment=target.equals("termux")?"termux":"proot:"+target;set("state",new Bundle());
   AlertDialog d=menu();CheckBox c=checkbox(d);assertNotNull(target,c);assertFalse(target,c.isChecked());c.performClick();d.dismiss();
   String key=target.equals("termux")?"termux-style":"termux-style:"+target;
   assertTrue(target,a.getSharedPreferences("workspace-x11",0).getBoolean(key,false));
   for(String phase:new String[]{"checking","installing","cancelling","preparing","stopping","running"}){
    Bundle b=new Bundle();b.putString("phase",phase);b.putBoolean("started",phase.equals("running"));set("state",b);
    d=menu();c=checkbox(d);assertFalse(target+":"+phase,c.isEnabled());c.setChecked(false);assertTrue(c.isChecked());d.dismiss();
   }
  }
  set("state",new Bundle());p.environment="proot:opensuse";AlertDialog d=menu();checkbox(d).performClick();d.dismiss();
  for(String target:targets){
   p.environment=target.equals("termux")?"termux":"proot:"+target;d=menu();assertEquals(target,!target.equals("opensuse"),checkbox(d).isChecked());d.dismiss();
  }
 }
 @Test public void serviceRefreshDisablesAndReenablesExistingCheckbox()throws Exception{
  AlertDialog d=menu();CheckBox c=checkbox(d);assertTrue(c.isEnabled());
  Method accept=WorkspaceDesktop.class.getDeclaredMethod("accept",Bundle.class);accept.setAccessible(true);
  Bundle b=new Bundle();b.putString("target","termux");b.putString("phase","running");b.putBoolean("started",true);accept.invoke(ui,b);assertFalse(c.isEnabled());
  b.putString("phase","ended");b.putBoolean("started",false);accept.invoke(ui,b);assertTrue(c.isEnabled());assertSame(c,checkbox(d));d.dismiss();
 }
 @Test public void retiredTouchSelectionMigratesOnceToKali()throws Exception{
  ui.close();a.getSharedPreferences("workspace-x11",0).edit().putBoolean("touch-workspace",true).putBoolean("termux-style",false).commit();
  ui=new WorkspaceDesktop(a,()->p,()->false,new TextView(a),()->{},()->{});set("remote",new Binder());
  assertTrue(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style",false));assertFalse(a.getSharedPreferences("workspace-x11",0).contains("touch-workspace"));
  AlertDialog d=menu();assertTrue(checkbox(d).isChecked());assertNull(d.getWindow().getDecorView().findViewWithTag("desktop-touch-workspace"));checkbox(d).performClick();d.dismiss();
  ui.close();ui=new WorkspaceDesktop(a,()->p,()->false,new TextView(a),()->{},()->{});assertFalse(a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style",false));
 }
 @Test public void inactiveRetiredChoicePreservesExplicitKaliPreference()throws Exception{
  for(boolean value:new boolean[]{true,false}){
   ui.close();a.getSharedPreferences("workspace-x11",0).edit().putBoolean("touch-workspace",false).putBoolean("termux-style",value).commit();
   ui=new WorkspaceDesktop(a,()->p,()->false,new TextView(a),()->{},()->{});
   assertEquals(value,a.getSharedPreferences("workspace-x11",0).getBoolean("termux-style",!value));assertFalse(a.getSharedPreferences("workspace-x11",0).contains("touch-workspace"));
  }
 }
}
