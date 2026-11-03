package com.termux.app.workspace;
import android.app.*;import android.os.Looper;import android.view.*;import android.widget.*;
import com.termux.app.TermuxActivity;import com.termux.R;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class AgentSkillsUiTest {
 TermuxActivity activity;AgentSkillsUi.Screen ui;int calls,writes;JSONObject last;String fail="";java.util.List<JSONObject> requests=new java.util.ArrayList<>();
 @Before public void before(){activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"ru");}
 @After public void after(){if(ui!=null)ui.dialog.dismiss();activity.finish();}
 JSONObject backend(JSONObject q)throws Exception{calls++;last=new JSONObject(q.toString());requests.add(last);if(!fail.isEmpty())throw new java.io.IOException(fail);if(!q.optString("operation").equals("list"))writes++;if(q.optString("operation").equals("prepare"))return new JSONObject().put("token","abc123").put("candidates",new JSONArray().put(new JSONObject().put("candidate","content/authored").put("path","/actual/home/.agents/skills/authored").put("name","authored").put("text","---\nname: authored\ndescription: fixture\n---\nbody")));return new JSONObject().put("target","/actual/home/.agents/skills").put("skills",new JSONArray()).put("roots",new JSONArray());}
 void show()throws Exception{ui=new AgentSkillsUi.Screen(activity,"/project","codex");ui.backend=this::backend;ui.show();idle();}
 void idle()throws Exception{for(int n=0;n<150;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!ui.busy)return;Thread.sleep(10);}fail("timeout");}
 TextView button(ViewGroup parent,String label){for(int n=0;n<parent.getChildCount();n++){View v=parent.getChildAt(n);if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return(TextView)v;if(v instanceof ViewGroup){TextView found=button((ViewGroup)v,label);if(found!=null)return found;}}return null;}
 @Test public void openingOnlyListsFiles()throws Exception{show();assertEquals(1,calls);assertEquals(0,writes);assertEquals("skills",last.getString("kind"));assertEquals("ubuntu-ai",last.getString("environment"));assertEquals("/project",last.getString("project"));}
 @Test public void agentAndScopeAreExplicitAndSelected()throws Exception{show();TextView claude=button(ui.selectors,"Claude Code");claude.performClick();idle();assertEquals("claude",last.getString("agent"));assertTrue(button(ui.selectors,"Claude Code").isSelected());button(ui.frame.header,"Проектные").performClick();idle();assertEquals("project",last.getString("scope"));assertEquals(0,writes);}
 @Test public void errorsRemainVisibleWithoutSuccess()throws Exception{show();fail="LOCAL_CHANGES";ui.load();idle();assertTrue(ui.status.getText().toString().contains("изменён локально"));assertEquals(0,writes);}
 @Test public void narrowLargeFontAndReducedHeight()throws Exception{for(String lang:new String[]{"ru","en"}){WorkspaceLanguage.select(activity,lang);if(ui!=null)ui.dialog.dismiss();android.content.res.Configuration c=new android.content.res.Configuration(activity.getResources().getConfiguration());c.fontScale=1.6f;activity.getResources().updateConfiguration(c,activity.getResources().getDisplayMetrics());show();ui.root.measure(View.MeasureSpec.makeMeasureSpec(280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));ui.root.layout(0,0,280,400);TextView codex=button(ui.selectors,"Codex");assertTrue(codex.isSelected());assertEquals(1,codex.getMaxLines());assertTrue(ui.frame.scroll.getChildAt(0)==ui.frame.content);assertTrue(ui.root.getChildAt(ui.root.getChildCount()-1).getBottom()<=400);}}
 @Test public void previewScrollsWithFixedFooter()throws Exception{show();LinearLayout actions=ui.column();ui.button(actions,"INSTALL",()->{});Dialog d=ui.preview("long-name","x\n".repeat(1000),actions,null);ViewGroup content=(ViewGroup)actions.getParent();content.measure(View.MeasureSpec.makeMeasureSpec(280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(350,View.MeasureSpec.EXACTLY));content.layout(0,0,280,350);assertTrue(content.getChildAt(1)instanceof ScrollView);assertTrue(actions.getBottom()<=350);assertTrue(actions.getTop()>0);d.dismiss();}
 @Test public void mutationCarriesMessageIndependentIdentity()throws Exception{show();ui.mutation("delete",new JSONObject().put("name","sample").put("revision","exact-hash").put("path","/actual/home/.agents/skills/sample"));idle();assertEquals(1,writes);}
 @Test public void retainedEnvironmentRoutesListingAndMutationWithoutRelocation()throws Exception{
  show();ui.environment="ubuntu";ui.select();ui.load();idle();assertEquals("ubuntu",last.getString("environment"));assertTrue(button(ui.selectors,AgentInstructionsUi.runtimeNotice(activity,"ubuntu"))!=null);
  button(ui.selectors,"Claude Code").performClick();idle();assertEquals("ubuntu",last.getString("environment"));ui.mutation("delete",new JSONObject().put("name","fixture").put("path","/actual/fixture").put("revision","same"));idle();assertEquals("ubuntu",last.getString("environment"));
 }

 TextView clickable(ViewGroup parent,String label){for(int n=0;n<parent.getChildCount();n++){View v=parent.getChildAt(n);if(v instanceof TextView&&v.hasOnClickListeners()&&((TextView)v).getText().toString().equals(label))return (TextView)v;if(v instanceof ViewGroup){TextView found=clickable((ViewGroup)v,label);if(found!=null)return found;}}return null;}
 Dialog creation(){return ui.children.get(ui.children.size()-1);}
 void fill(Dialog form){((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-name")).setText("authored");((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-description")).setText("When useful");((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-instructions")).setText("Exact instructions\nКириллица");}
 @Test public void creationMenuAndValidationKeepFormAndDraft()throws Exception{
  for(String lang:new String[]{"ru","en"}){WorkspaceLanguage.select(activity,lang);if(ui!=null)ui.dialog.dismiss();show();int before=calls;ui.add(false,null);AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(lang.equals("ru")?"Создать навык":"Create skill",menu.getListView().getAdapter().getItem(3));menu.dismiss();ui.create();Dialog form=creation();fill(form);((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-name")).setText("../bad");button((ViewGroup)form.findViewById(android.R.id.content),lang.equals("ru")?"Просмотреть":"Review").performClick();assertEquals(before,calls);assertTrue(form.isShowing());assertEquals("Exact instructions\nКириллица",((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-instructions")).getText().toString());form.dismiss();}
 }
 @Test public void createPreviewUsesCurrentBindingAndConflictPreservesText()throws Exception{
  show();
ui.agent="claude";
ui.scope="project";
ui.environment="ubuntu";
ui.create();
Dialog form=creation();
fill(form);
button((ViewGroup)form.findViewById(android.R.id.content),"Просмотреть").performClick();
idle();
assertEquals("prepare",last.optString("operation"));
assertEquals("create",last.optString("sourceKind"));
assertEquals("claude",last.optString("agent"));
assertEquals("project",last.optString("scope"));
assertEquals("ubuntu",last.optString("environment"));
Dialog preview=creation();
assertNotSame(form,preview);
assertNotNull(clickable((ViewGroup)preview.findViewById(android.R.id.content),"Создать навык"));
fail="NAME_CONFLICT";
clickable((ViewGroup)preview.findViewById(android.R.id.content),"Создать навык").performClick();
idle();
assertTrue("form retained on conflict",form.isShowing());
assertTrue("preview retained on conflict",preview.isShowing());
assertEquals("Exact instructions\nКириллица",((EditText)form.findViewById(android.R.id.content).findViewWithTag("skill-create-instructions")).getText().toString());
assertTrue("conflict explanation visible",((TextView)form.findViewById(android.R.id.content).findViewWithTag("skill-create-error")).getText().toString().contains("занят"));
fail="";
clickable((ViewGroup)preview.findViewById(android.R.id.content),"Создать навык").performClick();
idle();
assertFalse(form.isShowing());
assertFalse(preview.isShowing());
assertEquals("list",last.optString("operation"));
for(JSONObject q:requests)assertFalse(q.optString("operation").equals("execute"));

 }

}
