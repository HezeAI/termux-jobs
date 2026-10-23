package com.termux.app.workspace;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;
import android.app.AlertDialog;import android.view.*;import android.widget.*;import com.termux.app.TermuxActivity;import java.util.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=android.app.Application.class)
public class AgentEnvironmentUiTest {
 static View find(View v,String description){if(description.equals(String.valueOf(v.getContentDescription())))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View child=find(((ViewGroup)v).getChildAt(i),description);if(child!=null)return child;}return null;}
 @Test public void trashOnlyForInstalledSystemsAndDoesNotInterceptRowSelection(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();List<WorkspaceEnvironments.Choice> source=new ArrayList<>();source.add(new WorkspaceEnvironments.Choice("termux","","Termux","/",true));source.add(new WorkspaceEnvironments.Choice("proot:imported","imported","Imported","/",true));source.add(new WorkspaceEnvironments.Choice("proot:alpine","alpine","Alpine","/",false));
  EnvironmentSetup setup=new EnvironmentSetup(a,()->source);String[] removed={""},selected={""};setup.removal=(choice,refresh)->removed[0]=choice.alias;
  try{setup.choose(choice->selected[0]=choice.alias);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();ListView list=d.getListView();android.widget.ListAdapter adapter=list.getAdapter();View base=adapter.getView(0,null,list),imported=adapter.getView(1,null,list),missing=adapter.getView(2,null,list);
   assertNull(find(base,"Удалить Termux"));assertNull(find(missing,"Удалить Alpine"));View trash=find(imported,"Удалить Imported");if(trash==null)trash=find(imported,"Delete Imported");assertNotNull(trash);assertFalse(trash.isFocusable());trash.performClick();assertEquals("imported",removed[0]);assertEquals("",selected[0]);list.performItemClick(imported,1,1);assertEquals("imported",selected[0]);d.dismiss();
  }finally{setup.close();}
 }
 @Test public void choosingAgentUbuntuOnlySelectsItWithoutMigrationOrInstall(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore store=new WorkspaceStore(a,x->{});AgentEnvironmentManager manager=new AgentEnvironmentManager(a,store,x->true);String[] selected={""};int[] installs={0};manager.freshInstall=done->installs[0]++;
  try{manager.choose("ubuntu",alias->selected[0]=alias);AlertDialog chooser=ShadowAlertDialog.getLatestAlertDialog();chooser.getListView().performItemClick(null,1,1);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("ubuntu-ai",selected[0]);assertEquals(0,installs[0]);assertSame(chooser,ShadowAlertDialog.getLatestAlertDialog());assertFalse(chooser.isShowing());assertTrue(AgentEnvironmentManager.operating.isEmpty());}finally{manager.close();store.close();}
 }
 @Test public void referencedImportedEnvironmentCannotReachDeletion(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore store=new WorkspaceStore(a,x->{});store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias="restored-ubuntu";p.name="My imported system";p.saved=true;store.sessions.add(p);
  AgentEnvironmentManager manager=new AgentEnvironmentManager(a,store,x->false);
  try{assertEquals(Collections.singletonList(p.name),manager.references(p.prootAlias));manager.remove(new WorkspaceEnvironments.Choice("proot:restored-ubuntu","restored-ubuntu","Imported","/",true),()->fail("must not delete"));AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();String message=((TextView)d.findViewById(android.R.id.message)).getText().toString();assertTrue(message,message.contains(p.name));assertTrue(AgentEnvironmentManager.operating.isEmpty());d.dismiss();}finally{manager.close();store.close();}
 }
 @Test public void orphanWithoutHandleIsNotADeletionReferenceButSavedAndAttachedRemainProtected(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore store=new WorkspaceStore(a,x->{});store.sessions.clear();
  WorkspaceStore.Session ghost=AgentEnvironmentsTest.profile("codex");ghost.name="Codex";store.sessions.add(ghost);
  WorkspaceStore.Session saved=ghost.copy();saved.id="saved";saved.name="Saved Codex";saved.saved=true;store.sessions.add(saved);
  WorkspaceStore.Session attached=ghost.copy();attached.id="attached";attached.name="Attached Codex";attached.handle="live";store.sessions.add(attached);
  AgentEnvironmentManager manager=new AgentEnvironmentManager(a,store,x->false);
  try{assertEquals(Arrays.asList("Saved Codex","Attached Codex"),manager.references("ubuntu"));assertEquals(3,store.sessions.size());}finally{manager.close();store.close();}
 }
 @Test public void liveEnvironmentCannotStartMigrationOrDeletion(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore store=new WorkspaceStore(a,x->{});AgentEnvironmentManager manager=new AgentEnvironmentManager(a,store,x->true);
  try{manager.migrate(alias->fail());assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());assertTrue(AgentEnvironmentManager.operating.isEmpty());ShadowAlertDialog.getLatestAlertDialog().dismiss();manager.remove(new WorkspaceEnvironments.Choice("ubuntu","ubuntu","Ubuntu","/",true),()->fail());assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());assertTrue(AgentEnvironmentManager.operating.isEmpty());ShadowAlertDialog.getLatestAlertDialog().dismiss();}finally{manager.close();store.close();}
 }
 @Test public void automaticPreparationIsOnlyOfferedForMissingToolsNotAccessErrors()throws Exception {
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore store=new WorkspaceStore(a,x->{});AgentEnvironmentManager manager=new AgentEnvironmentManager(a,store,x->false);int[] prepared={0};manager.prepareTools=done->{prepared[0]++;done.accept(false);};
  try{java.lang.reflect.Method problem=AgentEnvironmentManager.class.getDeclaredMethod("problem",String.class,Runnable.class);problem.setAccessible(true);Runnable retry=()->fail("must not retry without successful preparation");problem.invoke(manager,"STORAGE_ACCESS_DENIED",retry);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertFalse(((TextView)d.findViewById(android.R.id.message)).getText().toString().contains("Python/proot-distro 5"));d.dismiss();problem.invoke(manager,"MIGRATION_TOOLS_REQUIRED",retry);d=ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,prepared[0]);assertTrue(manager.error("FILES_ACCESS_DENIED_ERRNO_13").contains("13"));}finally{manager.close();store.close();}
 }
 @Test public void scanDetailShowsRelativePathAndEscapesControlCharacters()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceStore store=new WorkspaceStore(a,x->{});AgentEnvironmentManager manager=new AgentEnvironmentManager(a,store,x->false);
  try{org.json.JSONObject e=new org.json.JSONObject();e.put("code","SCAN_ACCESS_DENIED");e.put("scanPath","rootfs/root/projects");e.put("scanOperation","open-directory");assertTrue(manager.scanDetails(e).contains("rootfs/root/projects"));e.put("scanPath","rootfs/root/evil\npath");String detail=manager.scanDetails(e);assertFalse(detail.contains("evil\npath"));assertTrue(detail.contains("evil\\u000apath"));e.put("scanPath","../escape");assertEquals("",manager.scanDetails(e));e.put("scanPath","/outside");assertEquals("",manager.scanDetails(e));}finally{manager.close();store.close();}
 }
}
