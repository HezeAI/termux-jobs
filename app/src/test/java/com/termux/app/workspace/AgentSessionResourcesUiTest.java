package com.termux.app.workspace;

import android.app.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentSessionResourcesUiTest {
 TermuxActivity activity;WorkspaceController controller;AgentSkillsUi.Screen skills;AgentInstructionsUi.Editor editor;
 final List<JSONObject> calls=new java.util.concurrent.CopyOnWriteArrayList<>();
 @Before public void before()throws Exception{activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceLanguage.select(activity,"ru");WorkspaceLanguage.apply(activity);}
 @After public void after(){if(editor!=null)editor.dialog.dismiss();if(skills!=null)skills.dialog.dismiss();if(controller!=null)controller.close();activity.finish();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 WorkspaceStore.Session profile(String agent,String env){WorkspaceStore.Session p=new WorkspaceStore.Session();p.saved=true;p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";p.codexEnvironment=env;p.directory="/session/проект with spaces";return p;}
 JSONObject backend(JSONObject q)throws Exception{calls.add(new JSONObject(q.toString()));String root=q.optString("scope").equals("project")?q.optString("project")+"/."+(q.optString("agent").equals("claude")?"claude":"agents")+"/skills":"/actual/home/.agents/skills";
  if(q.optString("operation").equals("prepare"))return new JSONObject().put("source","fixture").put("token","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").put("candidates",new JSONArray().put(new JSONObject().put("name","fixture").put("candidate","content/fixture").put("path",root+"/fixture").put("text","---\nname: fixture\ndescription: harmless fixture\n---\nInstructions")));
  return new JSONObject().put("target",root).put("skills",new JSONArray()).put("roots",new JSONArray()).put("path",q.optString("project")+"/"+q.optString("name")).put("exists",true).put("revision","fixture-revision").put("text","existing instructions");
 }
 void idle()throws Exception{for(int n=0;n<100;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if((skills==null||!skills.busy)&&(editor==null||!editor.busy))return;Thread.sleep(10);}fail("operation timeout");}
 void settle()throws Exception{for(int n=0;n<30;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}}
 TextView action(ViewGroup parent,String label){for(int i=0;i<parent.getChildCount();i++){View v=parent.getChildAt(i);if(v instanceof TextView&&v.hasOnClickListeners()&&((TextView)v).getText().toString().equals(label))return(TextView)v;if(v instanceof ViewGroup){TextView found=action((ViewGroup)v,label);if(found!=null)return found;}}return null;}
 String text(View v){String s=v instanceof TextView?((TextView)v).getText().toString():"";if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s+="\n"+text(((ViewGroup)v).getChildAt(i));return s;}
 @Test public void gearItemFollowsRestartAndCapturesOriginInBothLanguages()throws Exception{
  controller=new WorkspaceController(activity);Method menu=WorkspaceController.class.getDeclaredMethod("sessionMenu",WorkspaceStore.Session.class,boolean.class);menu.setAccessible(true);final WorkspaceStore.Session[] opened={null};controller.agentResourcesOpener=s->opened[0]=s;
  for(String lang:new String[]{"ru","en"}){WorkspaceLanguage.select(activity,lang);WorkspaceLanguage.apply(activity);for(String agent:new String[]{"codex","claude"}){
   WorkspaceStore.Session p=profile(agent,"ubuntu");menu.invoke(controller,p,true);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();String expected=WorkspaceText.tr("Инструкции проекта");assertEquals(WorkspaceText.tr("Перезапустить"),d.getListView().getItemAtPosition(0));assertEquals(expected,d.getListView().getItemAtPosition(1));p.directory="/other-chat";p.codexEnvironment="ubuntu-ai";d.getListView().performItemClick(null,1,1);assertNotSame(p,opened[0]);assertEquals("ubuntu",opened[0].codexEnvironment);assertEquals("/session/проект with spaces",opened[0].directory);
  }}
 }
 @Test public void unsupportedAgentsNeverOpenCodexOrClaudeResources()throws Exception{
  controller=new WorkspaceController(activity);Method menu=WorkspaceController.class.getDeclaredMethod("sessionMenu",WorkspaceStore.Session.class,boolean.class);menu.setAccessible(true);
  for(String agent:new String[]{"gemini","opencode"}){WorkspaceStore.Session p=profile(agent,"ubuntu-ai");menu.invoke(controller,p,true);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();for(int i=0;i<d.getListView().getCount();i++)assertFalse(d.getListView().getItemAtPosition(i).toString().contains("SKILL.md"));d.dismiss();assertNull(AgentSessionResourcesUi.show(activity,p,this::backend));assertNull(AgentInstructionsUi.sessionEditor(activity,p,this::backend));}
  assertTrue(calls.isEmpty());assertNull(AgentSessionResourcesUi.context(activity,profile("codex","unsupported-env")));
 }
 @Test public void actualHubButtonsOpenExistingManagersWithImmutableContext()throws Exception{
  for(String agent:new String[]{"codex","claude"}){WorkspaceStore.Session p=profile(agent,"ubuntu");AlertDialog hub=AgentSessionResourcesUi.show(activity,p,this::backend);assertTrue(text(hub.getWindow().getDecorView()).contains("/session/проект with spaces"));assertTrue(text(hub.getWindow().getDecorView()).contains("Ubuntu"));p.directory="/other-chat";p.codexEnvironment="ubuntu-ai";
   hub.findViewById(android.R.id.content).findViewWithTag("agent-resources-instructions").performClick();settle();assertEquals("project",calls.get(calls.size()-1).optString("scope"));assertEquals(agent,calls.get(calls.size()-1).optString("agent"));assertEquals("ubuntu",calls.get(calls.size()-1).optString("environment"));AlertDialog e=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(e.findViewById(android.R.id.content).findViewWithTag("instructions-editor"));e.dismiss();settle();
   hub.findViewById(android.R.id.content).findViewWithTag("agent-resources-skills").performClick();settle();JSONObject q=calls.get(calls.size()-1);assertEquals("skills",q.optString("kind"));assertEquals("list",q.optString("operation"));assertEquals("/session/проект with spaces",q.optString("project"));assertEquals("ubuntu",q.optString("environment"));assertEquals(agent,q.optString("agent"));ShadowAlertDialog.getLatestAlertDialog().dismiss();hub.dismiss();settle();
  }
 }
 @Test public void sessionEditorsLockAgentProjectAndScopeWhileSkillsKeepOnlyTwoScopes()throws Exception{
  for(String agent:new String[]{"codex","claude"}){
   AgentSessionResourcesUi.Context c=AgentSessionResourcesUi.context(activity,profile(agent,"ubuntu-ai"));editor=c.editor(this::backend);editor.show();idle();assertTrue(editor.sessionBound);assertNull(editor.root.findViewWithTag("instructions-agents-scroll"));assertNull(editor.root.findViewWithTag("instructions-tab-general"));assertNull(editor.root.findViewWithTag("instructions-choose-project"));editor.switchAgent(agent.equals("codex")?"claude":"codex");assertEquals(agent,editor.agent);assertEquals("project",editor.scope);editor.dialog.dismiss();editor=null;
   skills=c.skills(this::backend);skills.show();idle();assertTrue(skills.sessionBound);assertNull(action(skills.selectors,"Codex"));assertNull(action(skills.selectors,"Claude Code"));assertNotNull(action(skills.frame.header,"Общие"));assertNotNull(action(skills.frame.header,"Проектные"));skills.dialog.dismiss();skills=null;
  }
 }
 @Test public void allSkillSourcesAndInstallationKeepHubAgentEnvironmentScopeAndPath()throws Exception{
  for(String agent:new String[]{"codex","claude"}){if(skills!=null)skills.dialog.dismiss();AgentSessionResourcesUi.Context context=AgentSessionResourcesUi.context(activity,profile(agent,"ubuntu"));skills=context.skills(this::backend);skills.show();idle();
   skills.add(false,null);AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();assertEquals(4,menu.getListView().getCount());assertEquals("Создать навык",menu.getListView().getItemAtPosition(3));menu.dismiss();
   for(String kind:new String[]{"github","folder","zip"}){skills.prepare(kind,kind.equals("github")?"https://github.com/example/fixture":"/source/кириллица with spaces"+(kind.equals("zip")?".zip":""),"review-ref","",false,null);idle();JSONObject q=calls.stream().filter(v->v.optString("operation").equals("prepare")).reduce((a,b)->b).get();assertEquals(kind,q.optString("sourceKind"));assertEquals(context.agent,q.optString("agent"));assertEquals(context.environment,q.optString("environment"));assertEquals(context.project,q.optString("project"));assertEquals("project",q.optString("scope"));ShadowAlertDialog.getLatestAlertDialog().dismiss();}
   skills.create();Dialog form=skills.children.get(skills.children.size()-1);((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-name")).setText("fixture");((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-description")).setText("Use for fixture tasks");((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-instructions")).setText("Harmless instructions");action((ViewGroup)form.findViewById(android.R.id.content),"Просмотреть").performClick();idle();Dialog preview=skills.children.get(skills.children.size()-1);assertTrue(text(preview.getWindow().getDecorView()).contains("Проектные"));assertTrue(text(preview.getWindow().getDecorView()).contains(context.project+"/."+(agent.equals("claude")?"claude":"agents")+"/skills/fixture/SKILL.md"));action((ViewGroup)preview.findViewById(android.R.id.content),"Создать навык").performClick();idle();assertFalse(form.isShowing());assertTrue(calls.stream().anyMatch(v->v.optString("operation").equals("install")&&v.optString("environment").equals("ubuntu")&&v.optString("agent").equals(agent)));
   action(skills.frame.header,"Общие").performClick();idle();assertEquals("user",skills.scope);skills.prepare("folder","/source/shared","","",false,null);idle();JSONObject q=calls.stream().filter(v->v.optString("operation").equals("prepare")).reduce((a,b)->b).get();assertEquals("user",q.optString("scope"));ShadowAlertDialog.getLatestAlertDialog().dismiss();
   JSONObject installed=new JSONObject().put("name","fixture").put("path","/actual/home/.agents/skills/fixture").put("revision","exact-revision");skills.add(true,installed);menu=ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,menu.getListView().getCount());menu.dismiss();skills.prepare("folder","/source/update","","",true,installed);idle();ShadowAlertDialog.getLatestAlertDialog().dismiss();JSONObject prepared=new JSONObject().put("candidates",new JSONArray().put(new JSONObject().put("name","fixture").put("candidate","content/fixture").put("path",installed.optString("path"))));skills.reviewNext(prepared,new boolean[]{true},0,true,installed);idle();preview=skills.children.get(skills.children.size()-1);action((ViewGroup)preview.findViewById(android.R.id.content),"Применить обновление").performClick();idle();assertTrue(calls.stream().anyMatch(v->v.optString("operation").equals("update")&&v.optString("revision").equals("exact-revision")&&v.optString("path").equals(installed.optString("path"))));
   for(String op:new String[]{"rollback","delete"}){skills.mutation(op,new JSONObject().put("name","fixture").put("path","/actual/home/.agents/skills/fixture").put("revision","exact-revision"));idle();assertTrue(calls.stream().anyMatch(v->v.optString("operation").equals(op)&&v.optString("environment").equals("ubuntu")&&v.optString("agent").equals(agent)));}
  }
 }
}
