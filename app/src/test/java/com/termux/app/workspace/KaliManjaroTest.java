package com.termux.app.workspace;
import android.app.*;import android.os.*;import android.widget.*;import com.termux.app.TermuxActivity;import com.termux.R;import com.termux.x11.*;import java.io.*;import java.nio.file.*;import java.util.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class KaliManjaroTest {
 @Test public void freshInventoryAndShellsKeepBothNewAliasesSeparate()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();File files=a.getFilesDir(),proot=new File(files,"fixture-proot");List<WorkspaceEnvironments.Choice> choices=WorkspaceEnvironments.choices(new File(files,"home"),proot);
  for(String target:new String[]{"kali","manjaro"}){
   WorkspaceEnvironments.Choice c=null;for(WorkspaceEnvironments.Choice row:choices)if(row.alias.equals(target))c=row;assertNotNull(target,c);assertFalse(c.installed);assertEquals("proot:"+target,c.environment);assertTrue(WorkspaceAliases.ENVIRONMENTS.contains(target));
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias=target;p.directory="/root";p.name=target;
   String shell=new String(Files.readAllBytes(SessionShell.prepare(files,p).toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(shell.contains("'"+target+"' -- /bin/sh"));assertFalse(shell.contains(" login ubuntu "));assertEquals(target,WorkspaceDesktop.target(p));
  }
 }
 @Test public void kaliAndManjaroHaveSeparateTurnipAndBionicRendererChoices(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);
  for(String target:new String[]{"kali","manjaro"}){
   WorkspaceDesktopGpu.select(a,target,()->false,v->{},()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();ListAdapter adapter=d.getListView().getAdapter();assertTrue(adapter.isEnabled(0));assertTrue(adapter.isEnabled(2));assertTrue(adapter.isEnabled(1));assertEquals(4,adapter.getCount());assertTrue(WorkspaceDesktopWrapper.bridgeSupported(target));d.dismiss();
  }
  a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:kali","bionic-glibc").putString("renderer:manjaro","turnip").commit();assertEquals("bionic-glibc",WorkspaceDesktopGpu.mode(a,"kali"));assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"manjaro"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"ubuntu"));
 }
 @Test public void programsAndScreenSettingsUseIndependentTargets(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();for(String target:new String[]{"kali","manjaro"}){
   assertTrue(DesktopPrograms.supportsTarget(target));assertTrue(DesktopPrograms.supportsStyle(target));assertTrue(VirglInstaller.supported(target));assertEquals(target.equals("kali")?106:107,X11Environment.forTarget(target).display);
   assertEquals(target.equals("kali")?"libreoffice-writer":"libreoffice-fresh",DesktopPrograms.mainPackage("office",target));
  }
  assertNotEquals(X11Environment.forTarget("kali").service,X11Environment.forTarget("manjaro").service);assertFalse(WorkspaceDesktopWrapper.base(a,"kali").equals(WorkspaceDesktopWrapper.base(a,"debian")));
 }
}
