package com.termux.app.workspace;
import android.app.*;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.x11.DesktopPrograms;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopProgramsTest {
 @Test public void gpuToolsRecommendedTitleStaysEnglishAndDescriptionUsesSelectedLanguage(){
  assertTrue(Arrays.asList(DesktopPrograms.RECOMMENDED).contains("gpu-tools"));for(boolean ru:new boolean[]{true,false}){assertEquals("GPU & GPU INFO",DesktopPrograms.title("gpu-tools",ru));assertTrue(DesktopPrograms.description("gpu-tools",ru).contains("Alpine"));assertTrue(DesktopPrograms.description("gpu-tools",ru).contains("glxinfo -B"));}
  assertEquals("mesa-utils",DesktopPrograms.mainPackage("gpu-tools","alpine"));assertEquals("glmark2-x11",DesktopPrograms.mainPackage("gpu-tools","debian"));assertEquals("glmark2",DesktopPrograms.mainPackage("gpu-tools","void"));
 }
 @Test public void programsHaveDescriptionsAndOnlyVerifiedInstalledButtonsAreDisabled(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->calls.add(op+":"+id),()->{});
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:jobs-x11-ubuntu";
  try{ui.open(p,null);assertTrue(calls.isEmpty());Bundle ready=new Bundle();ready.putInt("job",1);ready.putString("target","jobs-x11-ubuntu");ready.putString("phase","needs-tools");ui.accept(ready);AlertDialog main=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();main.getWindow().getDecorView().findViewWithTag("agent-status-programs-recommended").performClick();assertEquals("programs-check:",calls.get(0));AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   for(String id:DesktopPrograms.RECOMMENDED){assertFalse(DesktopPrograms.description(id,true).isEmpty());assertFalse(((TextView)d.getWindow().getDecorView().findViewWithTag("desktop-program-"+id)).isEnabled());}
   Bundle state=new Bundle(),items=new Bundle();state.putInt("job",2);state.putString("target","ubuntu");state.putString("phase","complete");items.putBoolean("editor",true);state.putBundle("items",items);ui.accept(state);
   assertFalse(d.getWindow().getDecorView().findViewWithTag("desktop-program-pdf").isEnabled());
   state.putString("target","jobs-x11-ubuntu");items.putBoolean("pdf",false);ui.accept(state);
   assertFalse(d.getWindow().getDecorView().findViewWithTag("desktop-program-editor").isEnabled());assertTrue(d.getWindow().getDecorView().findViewWithTag("desktop-program-pdf").isEnabled());
   d.getWindow().getDecorView().findViewWithTag("desktop-program-pdf").performClick();AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("programs-install:pdf",calls.get(1));
   assertTrue(d.isShowing());assertTrue(Arrays.asList("Stop","Стоп").contains(((TextView)d.getWindow().getDecorView().findViewWithTag("desktop-program-pdf")).getText().toString()));
   assertFalse(confirm.isShowing());android.view.View card=(android.view.View)d.getWindow().getDecorView().findViewWithTag("desktop-program-pdf").getParent().getParent().getParent();
   assertEquals(android.view.View.VISIBLE,card.findViewWithTag("desktop-program-progress").getVisibility());
   d.getWindow().getDecorView().findViewWithTag("desktop-program-pdf").performClick();assertEquals("programs-cancel:",calls.get(2));assertFalse(d.getWindow().getDecorView().findViewWithTag("desktop-program-pdf").isEnabled());
   state.putInt("job",3);state.putString("phase","cancelled");state.remove("items");ui.accept(state);assertTrue(d.isShowing());

  }finally{ui.close();}
 }
 @Test public void failedBrowserSettingPreservesKnownProgramInstallationStates()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->{},()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:alpine";
  try{Bundle ready=new Bundle(),items=new Bundle();ready.putString("target","alpine");ready.putString("phase","complete");ready.putInt("job",1);items.putBoolean("editor",true);items.putBoolean("pdf",false);ready.putBundle("items",items);ui.open(p,null);ui.accept(ready);
   java.lang.reflect.Method show=WorkspaceDesktopPrograms.class.getDeclaredMethod("show",boolean.class);show.setAccessible(true);show.invoke(ui,true);
   Bundle error=new Bundle();error.putInt("job",2);error.putString("target","alpine");error.putString("phase","error");error.putString("action","firefox-dns-on");ui.accept(error);
   java.lang.reflect.Field field=WorkspaceDesktopPrograms.class.getDeclaredField("items");field.setAccessible(true);Bundle after=(Bundle)field.get(ui);assertTrue(after.getBoolean("editor"));assertTrue(after.containsKey("pdf"));assertFalse(after.getBoolean("pdf"));
   error.putString("action","remove");Bundle retained=new Bundle();retained.putBoolean("editor",true);error.putBundle("items",retained);ui.accept(error);assertTrue(((Bundle)field.get(ui)).getBoolean("editor"));
   error.remove("items");error.putString("action","install");ui.accept(error);assertTrue(((Bundle)field.get(ui)).isEmpty());
  }finally{ui.close();}
 }
 @Test public void installedFirefoxKeepsItsStatusAndCheckboxAboveDelete()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,arg)->{},()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:jobs-x11-ubuntu";
  try{Bundle state=new Bundle();state.putString("target","jobs-x11-ubuntu");state.putString("phase","complete");state.putBoolean("firefoxCompatKnown",true);state.putBoolean("firefoxCompat",true);ui.openInstalled(p,state);
   java.lang.reflect.Field data=WorkspaceDesktopPrograms.class.getDeclaredField("state");data.setAccessible(true);data.set(ui,state);java.lang.reflect.Field field=WorkspaceDesktopPrograms.class.getDeclaredField("rows");field.setAccessible(true);List<Bundle> rows=(List<Bundle>)field.get(ui);Bundle entry=new Bundle();entry.putString("package","firefox");entry.putString("name","Firefox");rows.add(entry);
   AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.widget.ListView list=dialog.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list");android.view.View view=list.getAdapter().getView(0,null,list);
   android.widget.CheckBox check=view.findViewWithTag("desktop-firefox-compat-checkbox");assertNotNull(check);assertTrue(check.isChecked());assertNotNull(view.findViewWithTag("desktop-package-firefox"));assertNotNull(view.findViewWithTag("desktop-remove-firefox"));dialog.dismiss();
  }finally{ui.close();}
 }
 @Test public void existingDebianGetsRepairAndFirefoxCompatibilityWithoutAcceptingUbuntuState()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> actions=new ArrayList<>();
  WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->{},()->{},actions::add);WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias="debian";
  try{Bundle state=new Bundle();state.putString("target","debian");state.putString("phase","complete");state.putBoolean("firefoxCompatKnown",true);ui.openInstalled(p,state);
   AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d.getWindow().getDecorView().findViewWithTag("agent-status-programs-repair-launchers"));
   java.lang.reflect.Field data=WorkspaceDesktopPrograms.class.getDeclaredField("state");data.setAccessible(true);data.set(ui,state);java.lang.reflect.Field field=WorkspaceDesktopPrograms.class.getDeclaredField("rows");field.setAccessible(true);List<Bundle> rows=(List<Bundle>)field.get(ui);Bundle entry=new Bundle();entry.putString("package","firefox");entry.putString("name","Firefox");rows.add(entry);
   android.widget.ListView list=d.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list");android.view.View row=list.getAdapter().getView(0,null,list);android.widget.CheckBox check=row.findViewWithTag("desktop-firefox-compat-checkbox");assertNotNull(check);assertTrue(check.isEnabled());check.performClick();assertEquals(Arrays.asList("firefox-compat-enable"),actions);assertFalse(check.isChecked());
   Bundle other=new Bundle(state);other.putString("target","jobs-x11-ubuntu");other.putBoolean("firefoxCompat",true);ui.accept(other);assertFalse(check.isChecked());
  }finally{ui.close();}
 }
 @Test public void searchUpdatesOnlyListAndDiscardsLatePagesFromPreviousQuery()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,arg)->calls.add(op+":"+arg),()->{});
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:jobs-x11-ubuntu";Bundle state=new Bundle();state.putString("target","jobs-x11-ubuntu");state.putString("phase","complete");state.putInt("job",7);state.putBoolean("catalogReady",true);state.putInt("catalogRevision",1);
  try{ui.open(p,state);AlertDialog d=openCatalog();android.widget.EditText search=d.getWindow().getDecorView().findViewWithTag("desktop-program-search");android.widget.ListView list=d.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list");
   android.widget.ImageButton previous=d.getWindow().getDecorView().findViewWithTag("desktop-program-previous"),next=d.getWindow().getDecorView().findViewWithTag("desktop-program-next");
   assertNotNull(previous.getDrawable());assertNotNull(next.getDrawable());assertEquals(180f,previous.getRotation(),0f);assertEquals(0f,next.getRotation(),0f);assertFalse(previous.isEnabled());assertFalse(next.isEnabled());assertEquals(WorkspaceDialogs.dp(a,48),next.getLayoutParams().height);
   assertTrue(calls.get(0).startsWith("programs-page:"));search.setText("drawing");org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(251));assertTrue(calls.get(calls.size()-1).contains("drawing"));
   Bundle entry=new Bundle();entry.putString("package","drawing-app");entry.putString("name","Drawing App");entry.putString("summary","Draw pictures");entry.putBoolean("available",true);java.util.ArrayList<Bundle> rows=new java.util.ArrayList<>();rows.add(entry);
   Bundle page=new Bundle();page.putString("query","");page.putInt("offset",0);page.putInt("total",1);page.putParcelableArrayList("rows",rows);state.putBundle("catalogPage",page);ui.accept(state);assertEquals(0,list.getAdapter().getCount());
   page.putString("query","drawing");ui.accept(state);assertEquals(1,list.getAdapter().getCount());assertSame(search,d.getWindow().getDecorView().findViewWithTag("desktop-program-search"));assertSame(list,d.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list"));
   android.view.View row=list.getAdapter().getView(0,null,list);assertTrue(row.findViewWithTag("desktop-package-drawing-app").isEnabled());
  }finally{ui.close();}
 }

 @Test public void returningFromRecommendedKeepsMainCompactAndLoadsCatalogOnlyWhenEntered(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,arg)->calls.add(op+":"+arg),()->{});
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:jobs-x11-ubuntu";Bundle state=new Bundle();state.putString("target","jobs-x11-ubuntu");state.putString("phase","complete");state.putInt("job",7);state.putBoolean("catalogReady",true);state.putInt("catalogRevision",1);
  try{ui.open(p,state);AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-recommended").performClick();assertEquals("programs-check:",calls.get(calls.size()-1));
   org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();int before=calls.size();
   state.putInt("job",8);ui.accept(state);assertEquals(before,calls.size());openCatalog();assertEquals(before+1,calls.size());assertTrue(calls.get(calls.size()-1).startsWith("programs-page:"));
  }finally{ui.close();}
 }

 @Test public void installedViewShowsTrashProtectsCoreAndRequiresConfirmation(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,arg)->calls.add(op+":"+arg),()->{});
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:jobs-x11-ubuntu";
  try{ui.open(p,null);Bundle state=new Bundle();state.putString("target","jobs-x11-ubuntu");state.putString("phase","needs-tools");state.putInt("job",1);ui.accept(state);
   AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.view.View installed=d.getWindow().getDecorView().findViewWithTag("agent-status-programs-installed");assertNotNull(installed);installed.performClick();assertEquals("programs-installed:",calls.get(calls.size()-1));
   d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();state.putInt("job",2);state.putString("phase","complete");state.putBoolean("installedReady",true);state.putInt("catalogRevision",1);ui.accept(state);assertTrue(calls.get(calls.size()-1).contains("\"installed\":true"));
   ArrayList<Bundle> entries=new ArrayList<>();for(String pkg:new String[]{"mousepad","thunar"}){Bundle entry=new Bundle();entry.putString("package",pkg);entry.putString("name",pkg);entry.putString("summary","Program");entry.putBoolean("installed",true);entries.add(entry);}Bundle page=new Bundle();page.putString("query","");page.putInt("offset",0);page.putBoolean("installed",true);page.putInt("total",2);page.putParcelableArrayList("rows",entries);state.putBundle("catalogPage",page);ui.accept(state);
   android.widget.ListView list=d.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list");assertEquals(2,list.getAdapter().getCount());android.view.View row=list.getAdapter().getView(0,null,list);android.widget.ImageButton trash=row.findViewWithTag("desktop-remove-mousepad");assertTrue(trash.isEnabled());assertFalse(list.getAdapter().getView(1,null,list).findViewWithTag("desktop-remove-thunar").isEnabled());
   int before=calls.size();trash.performClick();assertEquals(before,calls.size());AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("programs-remove:pkg:mousepad",calls.get(calls.size()-1));assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());
  }finally{ui.close();}
 }

 @Test public void manualRefreshBypassesCacheForBothLists(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,arg)->calls.add(op+":"+arg),()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:jobs-x11-ubuntu";
  Bundle state=new Bundle();state.putString("target","jobs-x11-ubuntu");state.putString("phase","complete");state.putInt("job",7);state.putInt("catalogRevision",1);state.putBoolean("catalogReady",true);
  try{ui.open(p,state);AlertDialog d=openCatalog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-refresh").performClick();AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("programs-catalog-install:catalog-tools",calls.get(calls.size()-1));state.putInt("job",8);state.putInt("catalogRevision",2);ui.accept(state);
   d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-installed").performClick();assertEquals("programs-installed:",calls.get(calls.size()-1));state.putInt("job",9);state.putBoolean("installedReady",true);state.putInt("catalogRevision",3);ui.accept(state);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-refresh").performClick();assertEquals("programs-installed-refresh:",calls.get(calls.size()-1));
  }finally{ui.close();}
 }

 @Test public void installationSurvivesClosingUiAndReopensInItsOwnCardWithoutCancelling()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->calls.add(op+":"+id),()->{});
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:alpine";Bundle state=new Bundle();state.putString("target","alpine");state.putInt("job",4);state.putString("phase","installing");state.putString("action","install");state.putString("program","images");state.putString("stage","downloading");state.putInt("percent",40);state.putLong("downloadBytes",4000);state.putLong("downloadTotal",10000);state.putString("detail","old file\nnew-file.apk\n");
  try{ui.open(p,state);AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d.getWindow().getDecorView().findViewWithTag("desktop-program-images"));assertEquals(0,calls.size());
   android.widget.ProgressBar bar=findProgress(d.getWindow().getDecorView());assertEquals(40,bar.getProgress());assertSame(d,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
   ui.close();assertEquals(0,calls.size());state.putInt("percent",80);ui.open(p,state);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(80,findProgress(d.getWindow().getDecorView()).getProgress());assertEquals(0,calls.size());
   state.putString("phase","complete");Bundle installed=new Bundle();installed.putBoolean("images",true);state.putBundle("items",installed);ui.accept(state);android.view.View button=d.getWindow().getDecorView().findViewWithTag("desktop-program-images"),trash=d.getWindow().getDecorView().findViewWithTag("desktop-remove-ristretto");assertFalse(button.isEnabled());assertTrue(Arrays.asList("Installed","Установлено").contains(((TextView)button).getText().toString()));assertEquals(android.view.View.VISIBLE,trash.getVisibility());assertEquals(0,calls.size());
   state.putString("phase","installing");state.putInt("job",5);state.remove("items");ui.accept(state);d.getWindow().getDecorView().findViewWithTag("desktop-program-images").performClick();assertEquals(Arrays.asList("programs-cancel:"),calls);
  }finally{ui.close();}
 }
 private static android.widget.ProgressBar findProgress(android.view.View v){if(v instanceof android.widget.ProgressBar)return (android.widget.ProgressBar)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){android.widget.ProgressBar b=findProgress(g.getChildAt(i));if(b!=null)return b;}}return null;}
 @Test public void inlineProgressUsesLatestLineAndHidesStaleDownloadVolumeOutsideDownloadStage(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();DesktopProgramProgress progress=new DesktopProgramProgress(a);Bundle b=new Bundle();b.putInt("percent",25);b.putString("stage","downloading");b.putLong("downloadBytes",1000);b.putLong("downloadTotal",4000);b.putString("detail","first\nsecond\npmstatus:pending\n");progress.update(b,true);assertEquals("second",progress.file.getText().toString());assertTrue(progress.remaining.getText().toString().contains("1.0 kB / 4.0 kB"));
  b.putString("stage","installing");progress.update(b,true);assertFalse(progress.remaining.getText().toString().contains("kB"));progress.update(b,false);assertEquals(android.view.View.GONE,progress.view.getVisibility());assertFalse(progress.file.isSelected());
 }

 @Test public void installedRecommendedCardHasTrashToTheRightAndAsksBeforeDeletion()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->calls.add(op+":"+id),()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:alpine";
  try{ui.open(p,null);Bundle state=new Bundle(),items=new Bundle();state.putString("target","alpine");state.putInt("job",1);state.putString("phase","complete");items.putBoolean("images",true);state.putBundle("items",items);ui.accept(state);java.lang.reflect.Method show=WorkspaceDesktopPrograms.class.getDeclaredMethod("show",boolean.class);show.setAccessible(true);show.invoke(ui,true);
   AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.view.View status=d.getWindow().getDecorView().findViewWithTag("desktop-program-images"),trash=d.getWindow().getDecorView().findViewWithTag("desktop-remove-ristretto");assertFalse(status.isEnabled());assertEquals(android.view.View.VISIBLE,trash.getVisibility());assertTrue(trash.isEnabled());android.view.ViewGroup row=(android.view.ViewGroup)status.getParent();assertSame(row,trash.getParent());assertTrue(row.indexOfChild(status)<row.indexOfChild(trash));int before=calls.size();trash.performClick();assertEquals(before,calls.size());AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("programs-remove:pkg:ristretto",calls.get(calls.size()-1));
  }finally{ui.close();}
 }
 @Test public void progressPollDoesNotRebuildCatalogRows()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->{},()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:alpine";Bundle state=new Bundle();state.putString("target","alpine");state.putInt("job",4);state.putString("phase","installing");state.putString("action","install");state.putString("program","images");
  try{ui.open(p,state);AlertDialog d=openCatalog();android.widget.ListView list=d.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list");int[] changes={0};list.getAdapter().registerDataSetObserver(new android.database.DataSetObserver(){@Override public void onChanged(){changes[0]++;}});
   state.putInt("percent",20);ui.accept(state);state.putInt("percent",30);ui.accept(state);assertEquals(0,changes[0]);assertSame(list,d.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list"));
   state.putString("phase","complete");ui.accept(state);assertEquals(1,changes[0]);ui.accept(state);assertEquals(1,changes[0]);
  }finally{ui.close();}
 }
 @Test public void refreshingAndCheckingKeepNavigationOpenWithoutRestartingOrCancellingJobs(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->calls.add(op+":"+id),()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:alpine";
  try{for(String phase:new String[]{"cataloguing","checking"}){
   Bundle state=new Bundle();state.putString("target","alpine");state.putString("phase",phase);state.putString("action",phase.equals("checking")?"installed":"catalog");state.putInt("job",4);state.putBoolean("catalogReady",true);state.putBoolean("installedReady",true);ui.open(p,state);calls.clear();
   AlertDialog main=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.view.View recommended=main.getWindow().getDecorView().findViewWithTag("agent-status-programs-recommended"),installed=main.getWindow().getDecorView().findViewWithTag("agent-status-programs-installed");assertTrue(recommended.isEnabled());assertTrue(installed.isEnabled());recommended.performClick();AlertDialog rec=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(rec.getWindow().getDecorView().findViewWithTag("desktop-program-editor"));assertTrue(calls.isEmpty());
   rec.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();main=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();main.getWindow().getDecorView().findViewWithTag("agent-status-programs-installed").performClick();assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());assertTrue(ui.busy());for(String call:calls)assertTrue("Unexpected job mutation: "+call,call.startsWith("programs-page:"));
   ui.close();assertTrue(ui.busy());
  }}finally{ui.close();}
 }
 @Test public void networkFailureKeepsMenuCompactAndFullLogInSeparateDetailsWindow(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->{},()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";
  try{ui.open(p,null);Bundle error=new Bundle();error.putInt("job",1);error.putString("target","termux");error.putString("phase","error");StringBuilder log=new StringBuilder();for(int i=0;i<100;i++)log.append("dlstatus:103:93.7:Retrieving file\n");log.append("E: Failed to fetch ghostscript.deb Error reading from server");error.putString("detail",log.toString());ui.accept(error);AlertDialog main=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();TextView status=main.getWindow().getDecorView().findViewWithTag("desktop-programs-status");assertTrue(status.getText().length()<400);assertFalse(status.getText().toString().contains("dlstatus"));assertTrue(main.getWindow().getDecorView().findViewWithTag("agent-status-programs-recommended").isEnabled());android.view.View details=main.getWindow().getDecorView().findViewWithTag("agent-status-programs-error-details");assertEquals(android.view.View.VISIBLE,details.getVisibility());details.performClick();AlertDialog full=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(main,full);TextView output=full.getWindow().getDecorView().findViewWithTag("agent-status-info");assertTrue(output.getText().toString().contains("ghostscript.deb"));assertTrue(main.isShowing());full.dismiss();
  }finally{ui.close();}
 }
 private static AlertDialog openCatalog(){AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-catalog").performClick();return org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();}
 @Test public void mainHasOnlyThreeSectionsAndCatalogUsesOneFixedPreparationAction(){
  for(boolean ready:new boolean[]{false,true}){
   TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->calls.add(op+":"+id),()->{});WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:debian";Bundle state=new Bundle();state.putString("target","debian");state.putString("phase",ready?"complete":"needs-tools");state.putInt("job",1);state.putBoolean("catalogReady",ready);
   try{ui.open(p,state);AlertDialog main=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.view.View root=main.getWindow().getDecorView();assertNull(root.findViewWithTag("desktop-program-search"));assertNull(root.findViewWithTag("desktop-program-catalog-list"));android.view.View installed=root.findViewWithTag("agent-status-programs-installed"),catalog=root.findViewWithTag("agent-status-programs-catalog");assertNotNull(root.findViewWithTag("agent-status-programs-recommended"));android.view.ViewGroup parent=(android.view.ViewGroup)installed.getParent();assertTrue(parent.indexOfChild(installed)<parent.indexOfChild(catalog));AlertDialog search=openCatalog();assertNotNull(search.getWindow().getDecorView().findViewWithTag("desktop-program-search"));assertNotNull(search.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list"));assertNull(search.getWindow().getDecorView().findViewWithTag("agent-status-programs-catalog-setup"));if(!ready){state.putInt("job",2);ui.accept(state);}TextView prepare=search.getWindow().getDecorView().findViewWithTag("agent-status-programs-refresh");assertTrue(Arrays.asList("Подготовить и обновить каталог","Prepare and refresh catalog").contains(prepare.getText().toString()));prepare.performClick();AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("programs-catalog-install:catalog-tools",calls.get(calls.size()-1));
   }finally{ui.close();}
  }
 }
 @Test public void ordinaryUbuntuInstalledProgramsExposeExplicitLauncherRepair(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> actions=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->{},()->{},actions::add);WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias="ubuntu";
  try{Bundle b=new Bundle();b.putString("target","ubuntu");b.putString("phase","complete");b.putBoolean("installedReady",true);ui.openInstalled(p,b);AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.view.View repair=d.getWindow().getDecorView().findViewWithTag("agent-status-programs-repair-launchers");assertNotNull(repair);assertTrue(repair.isEnabled());repair.performClick();assertEquals(Arrays.asList("repair-launchers"),actions);}finally{ui.close();}
 }
 @Test public void firefoxControlsAppearForAllConnectedTargetsWithIndependentState()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);
  for(String target:new String[]{"ubuntu","debian","alpine","termux"}){
   List<String> actions=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->{},()->{},actions::add);WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias=target.equals("termux")?"":target;
   try{Bundle state=new Bundle();state.putString("target",target);state.putString("phase","complete");state.putBoolean("firefoxCompatKnown",true);state.putBoolean("firefoxDnsKnown",true);ui.openInstalled(p,state);
    AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();java.lang.reflect.Field data=WorkspaceDesktopPrograms.class.getDeclaredField("state");data.setAccessible(true);data.set(ui,state);java.lang.reflect.Field field=WorkspaceDesktopPrograms.class.getDeclaredField("rows");field.setAccessible(true);List<Bundle> rows=(List<Bundle>)field.get(ui);Bundle entry=new Bundle();entry.putString("package","firefox");entry.putString("name","Firefox");rows.add(entry);
    android.widget.ListView list=d.getWindow().getDecorView().findViewWithTag("desktop-program-catalog-list");android.view.View row=list.getAdapter().getView(0,null,list);android.widget.CheckBox compat=row.findViewWithTag("desktop-firefox-compat-checkbox"),dns=row.findViewWithTag("desktop-firefox-dns-checkbox");assertNotNull(compat);assertNotNull(dns);assertTrue(compat.isEnabled());assertTrue(dns.isEnabled());dns.performClick();assertEquals(Arrays.asList("firefox-dns-enable"),actions);assertFalse(dns.isChecked());d.dismiss();
   }finally{ui.close();}
  }
  assertFalse(DesktopPrograms.supportsFirefox("ubuntu-ai"));assertTrue(DesktopPrograms.supportsFirefox("fedora"));
 }

 @Test public void cachedListsReopenWithoutChecksAndManualRefreshIsExplicit(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);
  for(String target:new String[]{"ubuntu","debian","alpine","termux","fedora","opensuse","void"}){
   List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->calls.add(op+":"+id),()->{});
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=target.equals("termux")?"termux":"proot:"+target;
   Bundle state=new Bundle(),items=new Bundle();state.putString("target",target);state.putString("phase","complete");state.putInt("job",7);state.putBoolean("installedReady",true);state.putInt("catalogRevision",1);
   for(String id:DesktopPrograms.IDS)items.putBoolean(id,id.equals("editor"));state.putBundle("items",items);
   try{
    for(int i=0;i<2;i++){
     ui.open(p,state);assertTrue(calls.isEmpty());AlertDialog main=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();main.getWindow().getDecorView().findViewWithTag("agent-status-programs-recommended").performClick();
     assertTrue(calls.isEmpty());AlertDialog rec=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertFalse(rec.getWindow().getDecorView().findViewWithTag("desktop-program-editor").isEnabled());ui.close();
    }
    ui.openInstalled(p,state);assertEquals(1,calls.size());assertTrue(calls.get(0).startsWith("programs-page:"));
    AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-refresh").performClick();assertEquals("programs-installed-refresh:",calls.get(1));
    calls.clear();ui.open(p,state);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-recommended").performClick();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getWindow().getDecorView().findViewWithTag("agent-status-programs-refresh").performClick();assertEquals(Arrays.asList("programs-check-refresh:"),calls);
   }finally{ui.close();}
  }
 }

 @Test public void pendingProgramReopensWithContinueAndRetryDoesNotRequireAnotherScan(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);List<String> calls=new ArrayList<>();WorkspaceDesktopPrograms ui=new WorkspaceDesktopPrograms(a,(op,id)->calls.add(op+":"+id),()->{});
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="proot:opensuse";Bundle state=new Bundle(),pending=new Bundle();state.putString("target","opensuse");state.putString("phase","idle");pending.putBoolean("office",true);pending.putBoolean("pkg:mousepad",false);state.putBundle("resumePrograms",pending);
  try{ui.open(p,state);assertTrue(calls.isEmpty());AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();TextView office=d.getWindow().getDecorView().findViewWithTag("desktop-program-office");assertTrue(Arrays.asList("Continue","Продолжить").contains(office.getText().toString()));assertTrue(office.isEnabled());assertNotNull(d.getWindow().getDecorView().findViewWithTag("desktop-package-mousepad"));office.performClick();assertEquals(Arrays.asList("programs-install:office"),calls);assertTrue(Arrays.asList("Stop","Стоп").contains(office.getText().toString()));assertTrue(d.isShowing());}finally{ui.close();}
 }

}
