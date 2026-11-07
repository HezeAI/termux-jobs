package com.termux.app.workspace;
import android.app.*;import android.widget.*;import com.termux.R;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.json.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=ClaudePermissionsTest.LocalSupport.class)
public class ClaudePermissionsTest {
 @org.robolectric.annotation.Implements(value=ClaudePermissions.class,isInAndroidSdk=false)
 public static class LocalSupport {
  @org.robolectric.annotation.Implementation protected static ClaudePermissions.Controls show(Activity a,LinearLayout body,WorkspaceStore.Session draft){ClaudePermissions.Controls c=new ClaudePermissions.Controls(a,body,draft);probe(a,body,draft,c);return c;}
  @org.robolectric.annotation.Implementation protected static void probe(Activity a,LinearLayout body,WorkspaceStore.Session draft,ClaudePermissions.Controls c){c.support(a,"UID=1000\n\"auto\" \"bypassPermissions\"");}
 }
 @Test public void rightsCancelAndSaveAreLaunchOnlyAndKeepRunIdentity(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceStore.Session p=new WorkspaceStore.Session();p.claudeAgent=true;p.environment="ubuntu";p.claudeMode="plan";String run=p.claudeSocketId;java.util.concurrent.atomic.AtomicInteger saved=new java.util.concurrent.atomic.AtomicInteger();
  AgentPermissionsUi.show(a,p,null,false,()->true,x->saved.incrementAndGet());AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();((CheckBox)d.getWindow().getDecorView().findViewWithTag("claude-mode-auto")).performClick();d.getButton(-2).performClick();assertEquals("plan",p.claudeMode);assertEquals(0,saved.get());
  AgentPermissionsUi.show(a,p,null,false,()->true,x->saved.incrementAndGet());d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();((CheckBox)d.getWindow().getDecorView().findViewWithTag("claude-mode-auto")).performClick();d.getButton(-1).performClick();assertEquals("auto",p.claudeMode);assertEquals("plan",p.claudeOrdinaryMode);assertEquals(run,p.claudeSocketId);assertEquals(1,saved.get());a.finish();
 }

 @Test public void mutuallyExclusiveModesRestoreEveryOrdinaryChoiceAcrossStoreReload()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();a.setTheme(R.style.Theme_Termux_Black);
  for(String language:new String[]{"ru","en"})for(String original:ClaudePermissions.ORDINARY){
   WorkspaceLanguage.select(a,language);WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.claudeAgent=true;p.claudeMode=original;
   WorkspaceStore.Session draft=WorkspaceRestart.snapshot(p);LinearLayout body=new LinearLayout(a);ClaudePermissions.Controls controls=new ClaudePermissions.Controls(a,body,draft);controls.support(a,"UID=1000\n\"auto\" \"bypassPermissions\"");
   controls.auto.performClick();assertTrue(controls.auto.isChecked());assertFalse(controls.bypass.isChecked());assertEquals("auto",draft.claudeMode);assertFalse(controls.ordinary.isEnabled());assertEquals(original,draft.claudeOrdinaryMode);assertEquals(original,p.claudeMode);
   controls.bypass.performClick();assertFalse(controls.auto.isChecked());assertTrue(controls.bypass.isChecked());assertEquals("bypassPermissions",draft.claudeMode);assertEquals(original,draft.claudeOrdinaryMode);
   AgentPermissionsUi.copyPolicy(draft,p);WorkspaceStore store=new WorkspaceStore(a,x->{});store.sessions.clear();store.sessions.add(p);String saved=store.encode().toString();store.decode(saved);WorkspaceStore.Session loaded=store.sessions.get(0);assertEquals(original,loaded.claudeOrdinaryMode);assertEquals("bypassPermissions",loaded.claudeMode);assertTrue(ClaudeCode.valid(loaded));assertTrue(ClaudeCode.command(a.getFilesDir(),loaded).contains("'bypassPermissions'"));
   ClaudePermissions.toggle(loaded,"bypassPermissions",false);assertEquals(original,loaded.claudeMode);
   controls.bypass.performClick();assertEquals(original,draft.claudeMode);assertTrue(controls.ordinary.isEnabled());assertFalse(controls.auto.isChecked());assertFalse(controls.bypass.isChecked());
   controls.auto.performClick();controls.auto.performClick();assertEquals(original,draft.claudeMode);
   ClaudePermissions.select(draft,"auto");ClaudePermissions.select(draft,"plan");assertEquals("plan",draft.claudeMode);assertEquals("plan",draft.claudeOrdinaryMode);
  }a.finish();
 }
 @Test public void rootAndUnsupportedCliHaveReasonsAndCannotEnableBypass(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"en");WorkspaceStore.Session p=new WorkspaceStore.Session();ClaudePermissions.Controls c=new ClaudePermissions.Controls(a,new LinearLayout(a),p);
  c.support(a,"UID=0\n\"auto\" \"bypassPermissions\"");assertTrue(c.auto.isEnabled());assertFalse(c.bypass.isEnabled());assertTrue(c.status.getText().toString().contains("root"));
  c.support(a,"UID=1000\nNOT_INSTALLED");assertFalse(c.auto.isEnabled());assertFalse(c.bypass.isEnabled());assertTrue(c.status.getText().toString().contains("not confirmed"));
  ClaudePermissions.select(p,"bypassPermissions");c.support(a,"UID=0\n\"auto\" \"bypassPermissions\"");assertFalse(c.bypass.isEnabled());assertFalse(c.bypass.isChecked());assertEquals("manual",p.claudeMode);assertFalse(c.bypass.isEnabled());a.finish();
 }
 @Test public void rejectedAutoIsUncheckedAndRestoresPreviousOrdinaryOnlyInDraft(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceStore.Session p=new WorkspaceStore.Session();p.claudeMode="plan";ClaudePermissions.select(p,"auto");WorkspaceStore.Session draft=WorkspaceRestart.snapshot(p);ClaudePermissions.Controls c=new ClaudePermissions.Controls(a,new LinearLayout(a),draft);c.rejected="auto";c.support(a,"UID=0\n\"auto\" \"bypassPermissions\"");assertFalse(c.auto.isEnabled());assertFalse(c.auto.isChecked());assertTrue(c.ordinary.isEnabled());assertEquals("plan",draft.claudeMode);assertEquals("auto",p.claudeMode);a.finish();
 }

}
