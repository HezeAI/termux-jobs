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
   assertNull(tag("agent-extended-debug"));assertNull(tag("agent-extended-debug-hint"));assertEquals(View.GONE,tag("terminal-launch-hint").getVisibility());
   String text=visible(editor.getWindow().getDecorView());assertTrue(text.contains("Ubuntu AI"));assertTrue(text.contains("/root/project"));assertFalse(text.contains("workspace-write"));assertFalse(text.contains("on-request"));assertFalse(text.contains("--remote"));assertFalse(text.contains("SSH:"));assertFalse(p.extendedDebugEnabled());editor.dismiss();idle();
  }
 }
 @Test public void terminalCreateRetainsEnvironmentAndReadinessAndCancelDoesNotStart()throws Exception{
  for(String language:new String[]{"ru","en"}){WorkspaceLanguage.select(activity,language);WorkspaceLanguage.apply(activity);
   open(null,true);assertNull(tag("agent-extended-debug"));assertNull(tag("session-launch-environment"));assertNotNull(tag("agent-environment-check"));
   assertEquals(language.equals("ru")?"Создать и открыть":"Create and open",editor.getButton(-1).getText().toString());assertEquals(View.GONE,editor.getButton(-2).getVisibility());assertEquals(language.equals("ru")?"Отмена":"Cancel",editor.getButton(-3).getText().toString());
   assertFalse(visible(editor.getWindow().getDecorView()).contains(language.equals("ru")?"Пресеты":"Presets"));editor.getButton(-3).performClick();idle();assertFalse(editor.isShowing());assertNull(activity.getCurrentSession());
  }
 }
 @Test public void sessionEditorsDoNotExposePermissionsAndPreserveIndependentChanges()throws Exception{
  for(String agent:new String[]{"codex","claude","gemini","opencode"}){
   WorkspaceStore.Session p=profile(agent);open(p,false);
   assertNull(tag("codex-command-confirmations"));assertNull(tag("codex-launch-details"));assertNull(tag("codex-launch-details-toggle"));assertNull(tag("agent-unavailable-scope"));
   String text=visible(editor.getWindow().getDecorView());assertFalse(text.contains("Подтверждать недоверенные команды"));assertFalse(text.contains("Права: "));assertNotNull(tag("session-working-directory"));
   p.codexAccess="read";p.codexConfirmActions=true;p.claudeMode="plan";p.geminiMode="plan";p.aiAccess="read";p.aiMode="plan";p.aiProjectOnly=false;
   editor.getButton(-1).performClick();idle();assertEquals("read",p.codexAccess);assertEquals(Boolean.TRUE,p.codexConfirmActions);assertEquals("plan",p.claudeMode);assertEquals("plan",p.geminiMode);assertEquals("read",p.aiAccess);assertEquals("plan",p.aiMode);assertFalse(p.aiProjectOnly);
  }
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
   WorkspaceStore.Session p=profile(agent);p.extendedDebug=true;p.saved=preset;p.codexAccess="full";p.codexConfirmActions=true;p.aiAccess="ask";p.aiProjectOnly=false;p.agentConnectionCommand="/root/launcher";open(p,!preset);editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();assertTrue("removing UI must not rewrite stored diagnostic preference",p.extendedDebug);assertFalse(p.extendedDebugEnabled());assertEquals("full",p.codexAccess);assertEquals(Boolean.TRUE,p.codexConfirmActions);assertEquals("ask",p.aiAccess);assertFalse(p.aiProjectOnly);assertEquals("/root/launcher",p.agentConnectionCommand);assertEquals("/root/project",p.directory);assertNull(activity.getCurrentSession());
  }
 }
 @Test public void locationReflectsEditedFolderAndLegacyTermuxInsteadOfAssumingUbuntu()throws Exception{
  WorkspaceStore.Session p=profile("codex");p.codexEnvironment="termux";p.directory="/data/data/com.termux/files/home/project";open(p,false);TextView location=(TextView)tag("agent-location-summary");assertTrue(location.getText().toString().contains("Termux"));assertFalse(location.getText().toString().contains("Ubuntu AI"));((EditText)tag("session-working-directory")).setText("/new/path");assertTrue(location.getText().toString().contains("/new/path"));assertEquals("/data/data/com.termux/files/home/project",p.directory);
 }
 @Test public void runtimeHintIsNotASelectorAndLegacyBindingsSurviveSaving()throws Exception{
  for(String language:new String[]{"ru","en"})for(String agent:new String[]{"codex","gemini","claude","opencode"})for(String env:new String[]{"ubuntu-ai","ubuntu","termux"}){
   if(env.equals("termux")&&!agent.equals("codex"))continue;
   WorkspaceLanguage.select(activity,language);WorkspaceLanguage.apply(activity);WorkspaceStore.Session p=profile(agent);p.codexEnvironment=env;open(p,false);
   TextView hint=(TextView)tag("agent-runtime-explanation");assertNotNull(hint);assertFalse(hint.isClickable());String text=hint.getText().toString();
   if(env.equals("ubuntu-ai")){assertTrue(text.contains("Ubuntu AI"));assertTrue(text.contains("Termux"));}
   else{assertFalse(text.contains("Ubuntu AI"));assertTrue(text.contains(env.equals("termux")?"Termux":"Ubuntu"));}
   editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();assertEquals(env,p.codexEnvironment);assertNull(activity.getCurrentSession());
  }
 }
 @Test public void newAiEditorDefaultsToAiWithoutEnvironmentSelector()throws Exception{
  open(null,false);tag("session-launch-environment").performClick();AlertDialog kinds=ShadowAlertDialog.getLatestAlertDialog();kinds.getListView().performItemClick(null,2,2);idle();TextView hint=(TextView)tag("agent-runtime-explanation");assertEquals(View.VISIBLE,hint.getVisibility());assertTrue(hint.getText().toString().contains("Ubuntu AI"));assertFalse(hint.isClickable());
 }

 @Test public void settingsCardReopensSamePreparationAndTracksAllSelectedAgentVariants()throws Exception{
  for(String agent:new String[]{"codex","claude","gemini","gemini-vertex","opencode"}){
   WorkspaceStore.Session p=profile(agent.equals("gemini-vertex")?"gemini":agent);if(agent.endsWith("vertex"))p.geminiAuth="vertex";
   AgentPreparationOperation.Job job=new AgentPreparationOperation.Job(activity,p,(c,x)->AgentPreparation.Plan.parse("MISSING:ubuntu\nCHECK:done",0),(c,x,o)->AgentPreparation.Plan.parse("READY:ubuntu\nCHECK:done",0));job.plan=new AgentPreparationOperation.PlanProgress(AgentPreparation.Plan.parse("MISSING:ubuntu\nCHECK:done",0));job.output("STAGE:extracting-environment");AgentPreparationOperation.jobs.put(AgentPreparationOperation.key(p),job);
   try{open(p,false);AgentPreparationCard card=(AgentPreparationCard)tag("agent-preparation-card");assertNotNull(card);assertTrue(card.phase.getText().toString().contains("Распаковка"));card.performClick();idle();AlertDialog details=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(editor,details);assertTrue(details.isShowing());assertSame(job,AgentPreparationOperation.current(p));details.dismiss();job.output("PREPARATION:verifying-components");idle();assertTrue(card.phase.getText().toString().contains("проверка"));editor.dismiss();idle();open(p,false);card=(AgentPreparationCard)tag("agent-preparation-card");assertTrue(card.phase.getText().toString().contains("проверка"));editor.dismiss();idle();}
   finally{job.active=false;AgentPreparationOperation.jobs.remove(AgentPreparationOperation.key(p));}
  }
 }

 @Test public void newCodexPolicySurvivesUiCreateSaveAndReopen()throws Exception{
  WorkspaceLanguage.select(activity,"ru");WorkspaceLanguage.apply(activity);open(null,false);tag("session-launch-environment").performClick();AlertDialog kinds=ShadowAlertDialog.getLatestAlertDialog();kinds.getListView().performItemClick(null,2,2);idle();CheckBox confirm=(CheckBox)tag("codex-command-confirmations");assertTrue(confirm.isChecked());assertEquals("Подтверждать недоверенные команды",confirm.getText().toString());editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
  Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore store=(WorkspaceStore)f.get(controller);WorkspaceStore.Session fresh=store.sessions.stream().filter(p->p.environment.equals("codex")&&p.codexUntrustedApproval).findFirst().orElseThrow(AssertionError::new);String id=fresh.id;assertEquals("untrusted",CodexSessionShell.approval(fresh));store.awaitWrites();
  open(fresh,false);assertNull(tag("codex-command-confirmations"));editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();store.awaitWrites();WorkspaceStore reloaded=new WorkspaceStore(activity,x->{throw new AssertionError(x);});try{WorkspaceStore.Session saved=reloaded.sessions.stream().filter(p->p.id.equals(id)).findFirst().orElseThrow(AssertionError::new);assertEquals("untrusted",CodexSessionShell.approval(saved));assertTrue(CodexSessionShell.invocation(saved,"/fixture.sock").contains("--ask-for-approval untrusted"));}finally{reloaded.close();}
 }

 @Test public void allNewAgentPoliciesSurviveUiSaveReopenAndSave()throws Exception{
  Field field=WorkspaceController.class.getDeclaredField("store");field.setAccessible(true);WorkspaceStore store=(WorkspaceStore)field.get(controller);
  for(int choice=0;choice<4;choice++){
   Set<String> before=new HashSet<>();for(WorkspaceStore.Session old:store.sessions)before.add(old.id);
   open(null,false);tag("session-launch-environment").performClick();AlertDialog kinds=ShadowAlertDialog.getLatestAlertDialog();kinds.getListView().performItemClick(null,2,2);idle();
   tag("agent-selector").performClick();AlertDialog agents=ShadowAlertDialog.getLatestAlertDialog();agents.getListView().performItemClick(null,choice,choice);idle();
   editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();store.awaitWrites();
   WorkspaceStore.Session fresh=store.sessions.stream().filter(p->!before.contains(p.id)).findFirst().orElseThrow(AssertionError::new);String id=fresh.id;
   assertEquals("ubuntu-ai",fresh.codexEnvironment);assertEquals("untrusted",CodexSessionShell.approval(fresh));assertEquals("manual",fresh.claudeMode);assertEquals("default",fresh.geminiMode);assertEquals("ask",fresh.aiAccess);
   assertEquals(choice==0,fresh.environment.equals("codex"));assertEquals(choice==1,fresh.aiAgent.equals("opencode"));assertEquals(choice==2,fresh.claudeAgent);assertEquals(choice==3,fresh.geminiAgent);
   open(fresh,false);editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();store.awaitWrites();
   WorkspaceStore loaded=new WorkspaceStore(activity,x->{throw new AssertionError(x);});try{WorkspaceStore.Session saved=loaded.sessions.stream().filter(p->p.id.equals(id)).findFirst().orElseThrow(AssertionError::new);assertEquals("untrusted",CodexSessionShell.approval(saved));assertEquals(fresh.codexEnvironment,saved.codexEnvironment);assertEquals(fresh.claudeMode,saved.claudeMode);assertEquals(fresh.geminiMode,saved.geminiMode);assertEquals(fresh.aiAccess,saved.aiAccess);}finally{loaded.close();}
  }
 }

}
