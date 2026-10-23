package com.termux.app.workspace;
import android.app.*;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.lang.reflect.*;
import java.util.Map;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopUiTest {
 private WorkspaceDesktop create(TermuxActivity a,WorkspaceStore.Session p){a.setTheme(R.style.Theme_Termux_Black);return new WorkspaceDesktop(a,()->p,()->false,new TextView(a),()->{},()->{});}
 private WorkspaceStore.Session profile(){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias="jobs-x11-ubuntu";return p;}
 @Test public void automaticKeyboardPersistsPerEnvironmentAcrossSessionRecreation(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();WorkspaceDesktop ui=create(a,p);
  try{a.getSharedPreferences("workspace-x11",0).edit().clear().commit();ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();CheckBox toggle=d.getWindow().getDecorView().findViewWithTag("desktop-automatic-keyboard");assertNotNull(toggle);TextView help=d.getWindow().getDecorView().findViewWithTag("desktop-automatic-keyboard-help");assertNotNull(help);assertTrue(help.getText() instanceof android.text.Spanned);assertEquals(1,((android.text.Spanned)help.getText()).getSpans(0,help.length(),android.text.style.ImageSpan.class).length);assertTrue(toggle.isChecked());toggle.performClick();assertFalse(toggle.isChecked());d.dismiss();ui.close();ui=create(a,p);ui.menu();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();toggle=d.getWindow().getDecorView().findViewWithTag("desktop-automatic-keyboard");assertFalse(toggle.isChecked());d.dismiss();ui.close();WorkspaceStore.Session other=profile();other.prootAlias="debian";ui=create(a,other);ui.menu();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();toggle=d.getWindow().getDecorView().findViewWithTag("desktop-automatic-keyboard");assertTrue(toggle.isChecked());d.dismiss();}
  finally{ui.close();a.getSharedPreferences("workspace-x11",0).edit().clear().commit();}
 }
 @Test public void retiredDesktopChoicesFilterAndDiagnosticsAreAbsentEvenWithOldPreferences()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{a.getSharedPreferences("workspace-x11",0).edit().putString("desktop-test","lxqt").commit();
   Field f=WorkspaceDesktop.class.getDeclaredField("state");f.setAccessible(true);Bundle b=new Bundle();b.putString("target","jobs-x11-ubuntu");b.putString("detail","previous diagnostic capture");f.set(ui,b);
   ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   for(String tag:new String[]{"desktop-choice","desktop-garcon-filter","desktop-check-details","desktop-repair-launchers"})assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-"+tag));
   Method desktop=WorkspaceDesktop.class.getDeclaredMethod("desktop",WorkspaceStore.Session.class);desktop.setAccessible(true);assertEquals("xfce",desktop.invoke(ui,profile()));d.dismiss();
  }finally{a.getSharedPreferences("workspace-x11",0).edit().clear().commit();ui.close();}
 }
 @Test public void launcherRepairLivesInInstalledProgramsAndRequiresConfirmation()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Field f=WorkspaceDesktop.class.getDeclaredField("programs");f.setAccessible(true);WorkspaceDesktopPrograms programs=(WorkspaceDesktopPrograms)f.get(ui);programs.openInstalled(profile(),null);
   AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-programs-remove-lxqt"));assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-programs-firefox-compat"));
   d.getWindow().getDecorView().findViewWithTag("agent-status-programs-repair-launchers").performClick();AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(d,confirm);assertNotNull(confirm.getButton(AlertDialog.BUTTON_POSITIVE));confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertTrue(d.isShowing());d.dismiss();
  }finally{ui.close();}
 }
 @Test public void maintenanceRefusesRunningDesktopWithoutOpeningConfirmation()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Field f=WorkspaceDesktop.class.getDeclaredField("programs");f.setAccessible(true);WorkspaceDesktopPrograms programs=(WorkspaceDesktopPrograms)f.get(ui);programs.openInstalled(profile(),null);AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();Field state=WorkspaceDesktop.class.getDeclaredField("state");state.setAccessible(true);Bundle b=new Bundle();b.putBoolean("started",true);state.set(ui,b);
   Method action=WorkspaceDesktop.class.getDeclaredMethod("maintenance",String.class);action.setAccessible(true);action.invoke(ui,"repair-launchers");assertSame(d,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());d.dismiss();
  }finally{ui.close();}
 }
 @Test public void completedMaintenanceReturnsToInstalledPrograms()throws Exception{
  for(String phase:new String[]{"installed","error","missing","cancelled"}){
   TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();WorkspaceDesktop ui=create(a,p);
   try{Field origin=WorkspaceDesktop.class.getDeclaredField("progressOrigin"),back=WorkspaceDesktop.class.getDeclaredField("maintenanceReturn");origin.setAccessible(true);back.setAccessible(true);origin.set(ui,p);back.set(ui,true);
    Method accept=WorkspaceDesktop.class.getDeclaredMethod("accept",Bundle.class);accept.setAccessible(true);Bundle b=new Bundle();b.putString("target",p.prootAlias);b.putString("phase","installing");accept.invoke(ui,b);b.putString("phase",phase);accept.invoke(ui,b);
    AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();if(phase.equals("error")||phase.equals("missing")){assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-programs-repair-launchers"));d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();}assertTrue(d.isShowing());assertNotNull(d.getWindow().getDecorView().findViewWithTag("agent-status-programs-repair-launchers"));assertFalse(back.getBoolean(ui));d.dismiss();
   }finally{ui.close();}
  }
 }
 @Test public void firefoxCheckboxShowsRealStateAndConfirmationDoesNotRequireDesktopStop()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Field f=WorkspaceDesktop.class.getDeclaredField("programs");f.setAccessible(true);WorkspaceDesktopPrograms programs=(WorkspaceDesktopPrograms)f.get(ui);programs.openInstalled(profile(),null);
   Field data=WorkspaceDesktopPrograms.class.getDeclaredField("state");data.setAccessible(true);Bundle ps=new Bundle();ps.putString("phase","complete");ps.putBoolean("firefoxCompatKnown",true);ps.putBoolean("firefoxCompat",true);data.set(programs,ps);
   Method show=WorkspaceDesktopPrograms.class.getDeclaredMethod("show",boolean.class);show.setAccessible(true);show.invoke(programs,true);AlertDialog page=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   assertNull(page.getWindow().getDecorView().findViewWithTag("agent-status-programs-firefox-compat"));CheckBox checkbox=page.getWindow().getDecorView().findViewWithTag("desktop-firefox-compat-checkbox");assertNotNull(checkbox);assertTrue(checkbox.isChecked());assertEquals(page.getWindow().getDecorView().findViewWithTag("desktop-program-browser").getParent().getParent(),checkbox.getParent());
   Field state=WorkspaceDesktop.class.getDeclaredField("state");state.setAccessible(true);Bundle running=new Bundle();running.putBoolean("started",true);running.putString("phase","running");state.set(ui,running);
   checkbox.performClick();assertTrue(checkbox.isChecked());AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(page,confirm);assertTrue(((TextView)confirm.findViewById(android.R.id.message)).getText().toString().contains("seals"));confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(checkbox.isChecked());page.dismiss();
  }finally{ui.close();}
 }
 @Test public void compactDiagnosticsKeepExportOutsideScrollAndRefreshExistingDialog()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Field f=WorkspaceDesktop.class.getDeclaredField("state");f.setAccessible(true);Bundle b=new Bundle();b.putString("detail",new String(new char[8000]).replace('\0','x'));f.set(ui,b);
   Method m=WorkspaceDesktop.class.getDeclaredMethod("diagnostics");m.setAccessible(true);m.invoke(ui);AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   android.view.View download=d.getWindow().getDecorView().findViewWithTag("diagnostics-download");assertNotNull(download);
   for(android.view.ViewParent parent=download.getParent();parent!=null;parent=parent.getParent())assertFalse(parent instanceof ScrollView);
   b.putString("detail","JOBS_X11_OBSERVE focus-change:2");org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2));assertTrue(d.isShowing());assertSame(download,d.getWindow().getDecorView().findViewWithTag("diagnostics-download"));d.dismiss();
  }finally{ui.close();}
 }
 @Test public void installedRowIsDisabledAndBelongsToSelectedUbuntu()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Field f=WorkspaceDesktop.class.getDeclaredField("installed");f.setAccessible(true);((Map<String,Boolean>)f.get(ui)).put("jobs-x11-ubuntu",true);ui.menu();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   assertTrue(d.isShowing());TextView installed=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-components");assertNull(installed);TextView change=d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-switch");assertTrue(change.isEnabled());assertEquals(WorkspaceDialogs.TEXT,change.getCurrentTextColor());d.dismiss();
  }finally{ui.close();}
 }
 @Test public void completedCheckAndInstallReturnToDesktopCardInsteadOfTerminal()throws Exception{
  for(String phase:new String[]{"installed","missing","error","cancelled"}){
   TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();WorkspaceDesktop ui=create(a,p);
   try{Field origin=WorkspaceDesktop.class.getDeclaredField("progressOrigin");origin.setAccessible(true);origin.set(ui,p);
    Method accept=WorkspaceDesktop.class.getDeclaredMethod("accept",Bundle.class);accept.setAccessible(true);Bundle b=new Bundle();b.putString("phase",phase.equals("installed")?"installing":"checking");b.putString("target",p.prootAlias);accept.invoke(ui,b);
    b.putString("phase",phase);b.putString("status",phase);accept.invoke(ui,b);
    AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(menu.isShowing());TextView components=menu.getWindow().getDecorView().findViewWithTag("agent-status-desktop-components");assertNull(components);assertNull(origin.get(ui));menu.dismiss();
   }finally{ui.close();}
  }
 }
 @Test public void lateConnectionCallbacksAfterClosingAreIgnored()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());ui.close();
  Field f=WorkspaceDesktop.class.getDeclaredField("connection");f.setAccessible(true);android.content.ServiceConnection c=(android.content.ServiceConnection)f.get(ui);
  android.content.ComponentName name=new android.content.ComponentName("com.termux","com.termux.x11.X11Service");c.onServiceConnected(name,new android.os.Binder());c.onServiceDisconnected(name);
 }
 @Test public void outsideTapDoesNotDismissInstallationButCancelDoes()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Bundle state=new Bundle();state.putString("phase","installing");state.putString("target","jobs-x11-ubuntu");state.putString("status","Installing");Method m=WorkspaceDesktop.class.getDeclaredMethod("accept",Bundle.class);m.setAccessible(true);m.invoke(ui,state);
   AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(d.isShowing());MotionEvent event=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,-100,-100,0);try{d.onTouchEvent(event);assertTrue(d.isShowing());}finally{event.recycle();}
   d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(d.isShowing());assertFalse(d.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled());state.putString("phase","cancelled");m.invoke(ui,state);assertFalse(d.isShowing());
  }finally{ui.close();}
 }
 @Test public void nativeEnvironmentsShowTheSameDesktopCardAndSeparateInstallationState()throws Exception{
  for(String alias:new String[]{"debian","alpine","termux"}){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();p.prootAlias=alias.equals("termux")?"":alias;WorkspaceDesktop ui=create(a,p);
  try{Field f=WorkspaceDesktop.class.getDeclaredField("installed");f.setAccessible(true);((Map<String,Boolean>)f.get(ui)).put("jobs-x11-ubuntu",true);ui.menu();
   AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-switch"));assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-components"));assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-programs"));d.dismiss();
   ((Map<String,Boolean>)f.get(ui)).put(alias,true);ui.menu();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-components"));d.dismiss();
  }finally{ui.close();}
  }
 }
 @Test public void stopAppearsOnlyForStartedSelectedDesktopEvenWhenReturnedToTerminal()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Field field=WorkspaceDesktop.class.getDeclaredField("state");field.setAccessible(true);
   for(String phase:new String[]{"idle","checking","installed","preparing","running","error","ended"})for(boolean started:new boolean[]{false,true})for(String target:new String[]{"ubuntu","jobs-x11-ubuntu"}){
    Bundle b=new Bundle();b.putString("phase",phase);b.putString("target",target);b.putBoolean("started",started);field.set(ui,b);ui.menu();
    AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(started&&target.equals("jobs-x11-ubuntu"),d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-stop")!=null);
    assertNull(d.getWindow().getDecorView().findViewWithTag("agent-status-desktop-programs"));d.dismiss();
   }
  }finally{ui.close();}
 }

 @Test public void unsupportedAndroidDoesNotLeaveProgramsStuckInChecking(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{ui.openPrograms();
   AlertDialog programs=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(programs.isShowing());programs.getWindow().getDecorView().findViewWithTag("agent-status-programs-recommended").performClick();programs=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(programs.getWindow().getDecorView().findViewWithTag("desktop-program-editor").isEnabled());assertTrue(programs.getWindow().getDecorView().findViewWithTag("agent-status-programs-refresh").isEnabled());
  }finally{ui.close();}
 }

 @Test public void previouslyShownDesktopSurvivesTabReattachmentButNotServerDisconnect()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceDesktop ui=create(a,profile());
  try{Field f=WorkspaceDesktop.class.getDeclaredField("state");f.setAccessible(true);Bundle state=new Bundle();state.putString("target","jobs-x11-ubuntu");state.putBoolean("ready",true);state.putBoolean("desktopStartupTimedOut",true);f.set(ui,state);
   assertFalse(ui.desktopReady("jobs-x11-ubuntu"));Field shown=WorkspaceDesktop.class.getDeclaredField("shownTargets");shown.setAccessible(true);((java.util.Set<String>)shown.get(ui)).add("jobs-x11-ubuntu");assertTrue(ui.desktopReady("jobs-x11-ubuntu"));assertFalse(ui.desktopReady("ubuntu"));
   state.putBoolean("ready",false);assertFalse(ui.desktopReady("jobs-x11-ubuntu"));state.putBoolean("ready",true);
   Field conn=WorkspaceDesktop.class.getDeclaredField("connection");conn.setAccessible(true);((android.content.ServiceConnection)conn.get(ui)).onServiceDisconnected(new android.content.ComponentName("com.termux","com.termux.x11.X11Service"));assertFalse(ui.desktopReady("jobs-x11-ubuntu"));assertTrue(((java.util.Set<?>)shown.get(ui)).isEmpty());
  }finally{ui.close();}
 }

 @Test public void closingSessionCancelsItsUnsentLaunchButKeepsOtherPendingLaunch()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();p.id="owner";p.handle="old";WorkspaceDesktop ui=create(a,p);
  try{
   Field pending=WorkspaceDesktop.class.getDeclaredField("pending"),origin=WorkspaceDesktop.class.getDeclaredField("pendingProfile"),state=WorkspaceDesktop.class.getDeclaredField("state");for(Field f:new Field[]{pending,origin,state})f.setAccessible(true);
   Bundle b=new Bundle();b.putString("desktopOwner","owner:old");b.putString("target",p.prootAlias);b.putBoolean("started",true);state.set(ui,b);assertTrue(ui.owns(p));
   WorkspaceStore.Session other=profile();other.id="other";other.handle="new";assertFalse(ui.owns(other));
   pending.set(ui,"launch");origin.set(ui,p);ui.sessionClosed(other);assertEquals("launch",pending.get(ui));
   ui.sessionClosed(p);assertEquals("",pending.get(ui));assertNull(origin.get(ui));
   p.handle="new";assertFalse(ui.owns(p));
  }finally{ui.close();}
 }

 @Test public void oldSnapshotCannotCompleteNewCheck()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();WorkspaceDesktop ui=create(a,p);
  try{Field pending=WorkspaceDesktop.class.getDeclaredField("launchAfterCheck"),ack=WorkspaceDesktop.class.getDeclaredField("launchCheckAcknowledged"),graphical=WorkspaceDesktop.class.getDeclaredField("graphical");for(Field f:new Field[]{pending,ack,graphical})f.setAccessible(true);pending.set(ui,p);
   Method accept=WorkspaceDesktop.class.getDeclaredMethod("accept",Bundle.class);accept.setAccessible(true);Bundle b=new Bundle();b.putString("target",p.prootAlias);b.putString("phase","installed");accept.invoke(ui,b);assertSame(p,pending.get(ui));assertTrue(((java.util.Set<?>)graphical.get(ui)).isEmpty());
   ack.setBoolean(ui,true);b.putString("phase","error");accept.invoke(ui,b);assertNull(pending.get(ui));assertTrue(((java.util.Set<?>)graphical.get(ui)).isEmpty());
  }finally{ui.close();}
 }
 @Test public void closingTheOriginSessionCancelsAutomaticLaunchAfterItsCheck()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();WorkspaceDesktop ui=create(a,p);
  try{Field pending=WorkspaceDesktop.class.getDeclaredField("launchAfterCheck");pending.setAccessible(true);pending.set(ui,p);ui.sessionClosed(p);assertNull(pending.get(ui));}finally{ui.close();}
 }
 @Test public void installIndicatorBelongsOnlyToSelectedEnvironmentAndStopsAtCompletion()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();WorkspaceDesktop ui=create(a,p);
  try{Field state=WorkspaceDesktop.class.getDeclaredField("state");state.setAccessible(true);Bundle b=new Bundle(),job=new Bundle();b.putBundle("programs",job);state.set(ui,b);
   for(String phase:new String[]{"installing","cancelling","checking","cataloguing","complete","cancelled","error"}){job.putString("phase",phase);job.putString("target",p.prootAlias);assertEquals(phase.equals("installing")||phase.equals("cancelling"),ui.programsInstalling());job.putString("target","debian");assertFalse(ui.programsInstalling());}
  }finally{ui.close();}
 }
 @Test public void statusPollingIsVisibleEnvironmentOnlyAndDoesNotOwnTheInstallJob()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore.Session p=profile();boolean[] visible={true};WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->visible[0],new TextView(a),()->{},()->{});
  try{Field state=WorkspaceDesktop.class.getDeclaredField("state");state.setAccessible(true);Bundle b=new Bundle(),job=new Bundle();b.putBundle("programs",job);job.putString("phase","installing");job.putString("target",p.prootAlias);state.set(ui,b);assertTrue(ui.programStatusPolling());visible[0]=false;assertFalse(ui.programStatusPolling());assertTrue(ui.programsInstalling());visible[0]=true;assertTrue(ui.programStatusPolling());job.putString("phase","complete");assertFalse(ui.programStatusPolling());
  }finally{ui.close();}
 }
 private void startDesktop(WorkspaceDesktop ui,WorkspaceStore.Session p)throws Exception{Method method=WorkspaceDesktop.class.getDeclaredMethod("menuAction",WorkspaceStore.Session.class,int.class);method.setAccessible(true);method.invoke(ui,p,0);}
 private boolean chosen(WorkspaceDesktop ui,String id)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField("graphical");f.setAccessible(true);return ((java.util.Set<?>)f.get(ui)).contains(id);}
 @Test public void missingStorageCanBeSkippedAndIsAskedAgainOnNextLaunchForAllTargets()throws Exception{
  Shadows.shadowOf(RuntimeEnvironment.application).denyPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
   TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session p=profile();p.prootAlias=target;WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
   try{startDesktop(ui,p);AlertDialog first=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(first.isShowing());assertFalse(chosen(ui,p.id));first.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(chosen(ui,p.id));ui.close();ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});startDesktop(ui,p);AlertDialog second=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(first,second);assertTrue(second.isShowing());second.dismiss();}finally{ui.close();}
  }
 }
 @Test public void grantedStorageStartsWithoutIntroDialog()throws Exception{
  Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session p=profile();WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
  try{AlertDialog before=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();startDesktop(ui,p);assertSame(before,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());assertTrue(chosen(ui,p.id));}finally{ui.close();}
 }
 @Test public void allowWaitsForGrantBeforeContinuingDesktopLaunch()throws Exception{
  Shadows.shadowOf(RuntimeEnvironment.application).denyPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session p=profile();WorkspaceDesktop ui=new WorkspaceDesktop(a,()->p,()->true,new TextView(a),()->{},()->{});
  try{startDesktop(ui,p);AlertDialog intro=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();intro.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertFalse(chosen(ui,p.id));Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);a.onRequestPermissionsResult(WorkspaceStorageAccess.REQUEST,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(chosen(ui,p.id));}finally{ui.close();}
 }

}
