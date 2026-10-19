package com.termux.app.workspace;
import android.app.*;import com.termux.R;import com.termux.app.TermuxActivity;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import java.util.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceBackupRemovalTest {
 @Test public void cleanupTargetsOnlyRemovedAliasAndBusyCheckUsesLiveHandle(){
  WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,m->{});store.sessions.clear();
  WorkspaceStore.Session target=new WorkspaceStore.Session();target.prootAlias="jobs-abcdef123456";target.handle="live";target.saved=true;store.sessions.add(target);
  WorkspaceStore.Session sibling=target.copy();sibling.id="sibling";sibling.handle="stale";store.sessions.add(sibling);
  WorkspaceStore.Session other=new WorkspaceStore.Session();other.prootAlias="jobs-112233445566";other.handle="other";store.sessions.add(other);
  WorkspaceStore.Session source=new WorkspaceStore.Session();source.prootAlias="debian";store.sessions.add(source);
  WorkspaceStore.Session agent=new WorkspaceStore.Session();agent.environment="codex";store.sessions.add(agent);
  assertTrue(WorkspaceBackupUi.restoredProfileInUse(store,target.prootAlias,Collections.singleton("live")));
  assertFalse(WorkspaceBackupUi.restoredProfileInUse(store,target.prootAlias,Collections.singleton("other")));
  try{WorkspaceBackupUi.removeRestoredPresets(store,"debian");fail();}catch(IllegalArgumentException expected){}
  assertEquals(5,store.sessions.size());WorkspaceBackupUi.removeRestoredPresets(store,target.prootAlias);
  assertEquals(Arrays.asList(other,source,agent),store.sessions);
 }
 @Test public void backupMenuOffersRemovalInBothLanguagesWithoutTestingLabel(){
  TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceStore store=new WorkspaceStore(activity,m->{});
  for(String lang:new String[]{"ru","en"}){WorkspaceLanguage.select(activity,lang);WorkspaceBackupUi.show(activity,store,()->fail("menu must not mutate"),cmd->fail("menu must not install"));AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals(4,dialog.getListView().getCount());String label=dialog.getListView().getAdapter().getItem(3).toString();assertTrue(label.contains(lang.equals("ru")?"Удаление":"Delete"));assertFalse(WorkspaceBackupUi.label(activity).contains("("));dialog.dismiss();}
 }
}
