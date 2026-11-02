package com.termux.app.workspace;

import android.app.*;import android.os.Looper;import android.view.*;import android.widget.*;
import android.content.res.Resources;
import com.termux.R;import com.termux.app.TermuxActivity;
import java.lang.reflect.*;import java.util.*;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentSettingsPresentationTest {
 TermuxActivity activity;WorkspaceController controller;Resources prior,priorEnglish;AlertDialog editor;
 @Before public void before()throws Exception{prior=WorkspaceText.resources;priorEnglish=WorkspaceText.englishResources;activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);Field view=TermuxActivity.class.getDeclaredField("mTerminalView");view.setAccessible(true);view.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceLanguage.select(activity,"ru");WorkspaceLanguage.apply(activity);controller=new WorkspaceController(activity);}
 @After public void after(){if(editor!=null)editor.dismiss();idle();controller.close();activity.finish();WorkspaceText.resources=prior;WorkspaceText.englishResources=priorEnglish;}
 void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 WorkspaceStore.Session profile(String agent){WorkspaceStore.Session p=new WorkspaceStore.Session();p.name=agent;p.environment=agent.equals("codex")?"codex":"ubuntu";p.codexEnvironment="ubuntu-ai";p.directory="/root/project";p.geminiAgent=agent.equals("gemini");p.claudeAgent=agent.equals("claude");if(agent.equals("opencode"))p.aiAgent=agent;return p;}
 AlertDialog open(WorkspaceStore.Session p,boolean launch)throws Exception{Method method=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);method.setAccessible(true);method.invoke(controller,p,launch);idle();editor=ShadowAlertDialog.getLatestAlertDialog();return editor;}
 View tag(String tag){return editor.getWindow().getDecorView().findViewWithTag(tag);}
 String visible(View view){if(view.getVisibility()!=View.VISIBLE)return "";String text=view instanceof TextView?((TextView)view).getText().toString()+"\n":"";if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)text+=visible(((ViewGroup)view).getChildAt(i));return text;}
 @Test public void allAgentEditorsHideDebugAndUnrelatedHelpInBothLanguages()throws Exception{
  for(String language:new String[]{"ru","en"})for(String agent:new String[]{"codex","gemini","claude","opencode"})for(boolean launch:new boolean[]{false,true}){
   WorkspaceLanguage.select(activity,language);WorkspaceLanguage.apply(activity);WorkspaceStore.Session p=profile(agent);p.extendedDebug=true;open(p,launch);
   assertEquals(View.GONE,tag("agent-extended-debug").getVisibility());assertEquals(View.GONE,tag("agent-extended-debug-hint").getVisibility());assertFalse(((CheckBox)tag("agent-extended-debug")).isChecked());assertEquals(View.GONE,tag("terminal-launch-hint").getVisibility());
   String text=visible(editor.getWindow().getDecorView());assertTrue(text.contains("Ubuntu AI"));assertTrue(text.contains("/root/project"));assertTrue(text.contains(language.equals("ru")?"root Android":"Android root"));assertFalse(text.contains("workspace-write"));assertFalse(text.contains("on-request"));assertFalse(text.contains("--remote"));assertFalse(text.contains("SSH:"));assertFalse(p.extendedDebugEnabled());editor.dismiss();idle();
  }
 }
 @Test public void creationFormsToggleOnlyPresentationAndOrdinaryDebugRemainsAvailable()throws Exception{
  for(boolean launch:new boolean[]{false,true}){
   open(null,launch);CheckBox debug=(CheckBox)tag("agent-extended-debug");assertEquals(View.VISIBLE,debug.getVisibility());debug.setChecked(true);
   tag("session-launch-environment").performClick();AlertDialog kinds=ShadowAlertDialog.getLatestAlertDialog();kinds.getListView().performItemClick(null,2,2);idle();assertEquals(View.GONE,debug.getVisibility());assertEquals(View.GONE,tag("terminal-launch-hint").getVisibility());
   tag("session-launch-environment").performClick();kinds=ShadowAlertDialog.getLatestAlertDialog();kinds.getListView().performItemClick(null,0,0);idle();assertEquals(View.VISIBLE,debug.getVisibility());assertTrue(debug.isChecked());assertEquals(View.VISIBLE,tag("terminal-launch-hint").getVisibility());editor.dismiss();idle();
  }
 }
 @Test public void codexDetailsTrackScopeAndApprovalWithoutConflatingThem()throws Exception{
  WorkspaceStore.Session p=profile("codex");p.codexAccess="project";p.codexConfirmActions=false;open(p,false);TextView details=(TextView)tag("codex-launch-details");assertEquals(View.GONE,details.getVisibility());assertTrue(details.getText().toString().contains("workspace-write"));assertTrue(details.getText().toString().contains("never"));tag("codex-launch-details-toggle").performClick();assertEquals(View.VISIBLE,details.getVisibility());
  CheckBox confirm=findCheck(editor.getWindow().getDecorView(),"Codex action confirmations");confirm.setChecked(true);assertTrue(details.getText().toString().contains("on-request"));assertTrue(details.getText().toString().contains("workspace-write"));
  findCheck(editor.getWindow().getDecorView(),"Codex project write scope").setChecked(false);assertTrue(details.getText().toString().contains("danger-full-access"));assertTrue(details.getText().toString().contains("on-request"));assertEquals("project",p.codexAccess);assertEquals(Boolean.FALSE,p.codexConfirmActions);
 }
 CheckBox findCheck(View view,String label){if(view instanceof CheckBox&&label.equals(view.getContentDescription()))return (CheckBox)view;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++){CheckBox result=findCheck(((ViewGroup)view).getChildAt(n),label);if(result!=null)return result;}return null;}
 @Test public void oldAgentFlagsAreClearedOnReadCopyEncodeAndLaunchButOrdinaryFlagsSurvive()throws Exception{
  WorkspaceStore store=new WorkspaceStore(activity,e->fail(e));try{
   for(String agent:new String[]{"codex","gemini","claude","opencode","shell"}){
    WorkspaceStore.Session p=profile(agent);p.extendedDebug=true;boolean ordinary=agent.equals("shell");store.sessions.clear();store.sessions.add(p);JSONObject encoded=store.encode();assertEquals(ordinary,encoded.getJSONArray("sessions").getJSONObject(0).getBoolean("extendedDebug"));
    encoded.getJSONArray("sessions").getJSONObject(0).put("extendedDebug",true);store.decode(encoded.toString());WorkspaceStore.Session read=store.sessions.get(0);assertEquals(ordinary,read.extendedDebug);assertEquals(ordinary,p.copy().extendedDebug);WorkspaceDiagnostics.register(p,true);assertTrue(WorkspaceDiagnostics.report(p,null).contains("Extended active at launch: "+ordinary));assertTrue(WorkspaceDiagnostics.report(p,null).contains("START"));if(agent.equals("gemini"))assertTrue(GeminiCli.command(activity.getFilesDir(),p).contains("JOBS_GEMINI_DEBUG=0"));
   }
  }finally{store.close();}
 }
 @Test public void saveExistingAgentPreservesPermissionsConnectionAndWorkingDirectory()throws Exception{
  for(String agent:new String[]{"codex","gemini","claude","opencode"})for(boolean preset:new boolean[]{false,true}){
   WorkspaceStore.Session p=profile(agent);p.extendedDebug=true;p.saved=preset;p.codexAccess="full";p.codexConfirmActions=true;p.aiAccess="ask";p.aiProjectOnly=false;p.agentConnectionCommand="/root/launcher";open(p,!preset);editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();assertFalse(p.extendedDebug);assertEquals("full",p.codexAccess);assertEquals(Boolean.TRUE,p.codexConfirmActions);assertEquals("ask",p.aiAccess);assertFalse(p.aiProjectOnly);assertEquals("/root/launcher",p.agentConnectionCommand);assertEquals("/root/project",p.directory);assertNull(activity.getCurrentSession());
  }
 }
 @Test public void locationReflectsEditedFolderAndLegacyTermuxInsteadOfAssumingUbuntu()throws Exception{
  WorkspaceStore.Session p=profile("codex");p.codexEnvironment="termux";p.directory="/data/data/com.termux/files/home/project";open(p,false);TextView location=(TextView)tag("agent-location-summary");assertTrue(location.getText().toString().contains("Termux"));assertFalse(location.getText().toString().contains("Ubuntu AI"));((EditText)tag("session-working-directory")).setText("/new/path");assertTrue(location.getText().toString().contains("/new/path"));assertEquals("/data/data/com.termux/files/home/project",p.directory);
 }
}
