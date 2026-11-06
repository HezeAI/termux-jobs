package com.termux.app.workspace;

import android.app.*;import android.os.Looper;import android.view.*;import android.widget.*;
import com.termux.app.TermuxActivity;import com.termux.R;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class AgentInstructionsUiTest {
 android.content.res.Resources priorResources,priorEnglishResources;TermuxActivity activity;AgentInstructionsUi.Editor ui;int writes;String lastText="",sourceText="current\r\n";boolean conflict,storageOffer,emptyHistory,verifiedCli;String historyText="older revision",projectText="current\r\n";boolean projectExists=true;String projectSource="/project/AGENTS.md";JSONObject last;
 @Before public void before(){priorResources=WorkspaceText.resources;priorEnglishResources=WorkspaceText.englishResources;activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"ru");WorkspaceLanguage.apply(activity);}
 @After public void after(){if(ui!=null&&ui.dialog!=null)ui.dialog.dismiss();activity.finish();WorkspaceText.resources=priorResources;WorkspaceText.englishResources=priorEnglishResources;}
 JSONObject backend(JSONObject q)throws Exception{
  last=new JSONObject(q.toString());String op=q.getString("operation");
  if(op.equals("save")){writes++;lastText=q.getString("text");if(conflict)throw new java.io.IOException("EDIT_CONFLICT");if(q.optString("scope").equals("project")){projectText=lastText;projectExists=true;}else sourceText=lastText;}
  if(op.equals("history"))return new JSONObject().put("history",emptyHistory?new JSONArray():new JSONArray().put(new JSONObject().put("id","00000000000000000000000000000001").put("time",1700000000).put("sha256","aabbccdd")));
  if(op.equals("previous"))return new JSONObject().put("text",historyText);
  if(op.equals("diagnostics"))return new JSONObject().put("codexHome","/custom/codex").put("sources",new JSONArray()).put("cli",new JSONObject().put("version",verifiedCli?"0.164.0":"99.0.0").put("behaviorVerified",verifiedCli));
  return new JSONObject().put("projectBaseExists",projectExists).put("projectSource",projectSource==null?JSONObject.NULL:projectSource).put("storageAddition",storageOffer?"## Storage addition\nRules\n":"").put("storageAdditionAvailable",storageOffer).put("path",q.optString("scope").equals("project")?(q.optString("name").equals("LEARNING.md")&&!q.optString("memoryPath").isEmpty()?q.optString("memoryPath"):q.optString("project")+"/"+q.optString("name")):(q.optString("agent").equals("claude")?"/custom/claude/CLAUDE.md":"/custom/codex/AGENTS.override.md")).put("maskedBy",q.optString("scope").equals("project")&&q.optString("name").equals("AGENTS.md")&&"/project/AGENTS.override.md".equals(projectSource)?"/project/AGENTS.override.md":JSONObject.NULL).put("exists",!q.optString("scope").equals("project")||projectExists).put("text",q.optString("scope").equals("project")?projectText:sourceText).put("revision","fixture-revision").put("template","template text\n").put("templateAvailable",true);
 }
 void show()throws Exception{ui=new AgentInstructionsUi.Editor(activity,"/project",this::backend);ui.show();waitIdle();}
 void waitIdle()throws Exception{for(int n=0;n<150;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!ui.busy)return;Thread.sleep(10);}fail("UI operation did not finish");}
 View find(String tag){return ui.root.findViewWithTag(tag);}
 void measured(View view,int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}
 android.graphics.Rect bounds(View parent,View child){android.graphics.Rect r=new android.graphics.Rect();child.getDrawingRect(r);((ViewGroup)parent).offsetDescendantRectToMyCoords(child,r);return r;}
 @Test public void suppliedTemplatesAreExactAndOnlyExplicitlySavedForBothAgentsAndLanguages()throws Exception{
  String[] hashes={"f0ee1dabfdd25388231f5a5b7c483259dd2fa2f6d6049b8539245128081df6d6","98af576b97c8b71865539ed30c8166be8e189a650cb9e5aaee4e5958430beb0b","56a96b8c94ab11eeb397cbb338d078aee9917810f48162dc1f598f7bd75d36ea","42ef6e255c619315c196a013e0208f0d29e9969b507b07eabc28a7c5fae1794b"};int index=0;
  for(String agent:new String[]{"codex","claude"})for(String lang:new String[]{"ru","en"}){
   if(ui!=null)ui.dialog.dismiss();sourceText="";conflict=false;writes=0;WorkspaceLanguage.select(activity,lang);WorkspaceLanguage.apply(activity);show();ui.switchAgent(agent);waitIdle();assertEquals(0,writes);assertEquals("",sourceText);
   String expected=AgentInstructions.builtInTemplate(activity,lang,agent);byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));assertEquals(hashes[index++],hex.toString());
   ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(expected,((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString());assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("",sourceText);assertEquals(0,writes);
   ui.editor.setText("draft retained on conflict");conflict=true;sourceText="external change";ui.template();preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("external change",sourceText);assertEquals("draft retained on conflict",ui.editor.getText().toString());assertEquals(agent,last.getString("agent"));
   conflict=false;ui.load();waitIdle();ui.template();preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(expected,sourceText);assertEquals(agent,last.getString("agent"));assertTrue(last.getString("path").endsWith(agent.equals("claude")?"CLAUDE.md":"AGENTS.override.md"));
  }
 }
 @Test public void claudeNarrowLargeFontFilesAndEnvironmentStayUsable()throws Exception{
  for(String lang:new String[]{"ru","en"}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,lang);android.content.res.Resources res=activity.getResources();android.content.res.Configuration c=new android.content.res.Configuration(res.getConfiguration());c.fontScale=1.5f;res.updateConfiguration(c,res.getDisplayMetrics());res.getDisplayMetrics().scaledDensity=res.getDisplayMetrics().density*1.5f;
   show();ui.switchAgent("claude");waitIdle();find("instructions-tab-project").performClick();waitIdle();
   for(String file:new String[]{"CLAUDE.md","CLAUDE.local.md",".claude/CLAUDE.md","LEARNING.md"}){find("instructions-file-"+file).performClick();waitIdle();measured(ui.root,ui.dp(280),ui.dp(360));TextView active=(TextView)find("instructions-file-"+file);assertTrue(active.isSelected());assertEquals(1,active.getLineCount());assertTrue(ui.save.getHeight()>0);assertTrue(bounds(ui.root,ui.close).bottom<=ui.dp(360));}
  }
 }
 @Test public void staleTemplatePreviewCannotWriteIntoAnotherAgent()throws Exception{
  show();ui.template();AlertDialog p=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();ui.switchAgent("claude");waitIdle();p.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(0,writes);assertEquals("claude",ui.agent);
 }
 @Test public void claudeHistoryRestoreUsesClaudeBackendAndKeepsDraftOnCancel()throws Exception{
  show();ui.switchAgent("claude");waitIdle();ui.history(true);waitIdle();AlertDialog p=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();p.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);ui.history(true);waitIdle();p=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();p.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("older revision",lastText);assertEquals("claude",last.getString("agent"));assertEquals("/custom/claude/CLAUDE.md",last.getString("path"));
 }
 @Test public void claudeSelectorUsesAdditiveFilesAndKeepsMemoryBinding()throws Exception{
  show();ui.activity.getSharedPreferences("agent-instructions-memory-paths",0).edit().putString("/project","/shared/LEARNING.md").commit();
  find("instructions-agent-claude").performClick();waitIdle();assertTrue(find("instructions-agent-claude").isSelected());assertEquals("/custom/claude/CLAUDE.md",ui.document.getString("path"));assertEquals("/shared/LEARNING.md",ui.memoryPath());
  find("instructions-tab-project").performClick();waitIdle();assertNotNull(find("instructions-file-CLAUDE.md"));assertNotNull(find("instructions-file-CLAUDE.local.md"));assertNull(find("instructions-file-AGENTS.override.md"));
  find("instructions-file-CLAUDE.local.md").performClick();waitIdle();assertTrue(((TextView)find("instructions-project-explanation")).getText().toString().contains("Дополняют CLAUDE.md"));assertEquals("claude",last.getString("agent"));assertEquals("ubuntu-ai",last.getString("environment"));
  find("instructions-file-LEARNING.md").performClick();waitIdle();assertEquals("/shared/LEARNING.md",ui.document.getString("path"));
  find("instructions-agent-codex").performClick();waitIdle();assertEquals("/shared/LEARNING.md",ui.memoryPath());assertNotNull(find("instructions-file-AGENTS.override.md"));
 }
 @Test public void agentSwitchRequiresExplicitDiscardAndNeverSavesToOtherAgent()throws Exception{
  show();ui.editor.setText("retained draft");find("instructions-agent-claude").performClick();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("codex",ui.agent);assertEquals("retained draft",ui.editor.getText().toString());assertEquals(0,writes);
  find("instructions-agent-claude").performClick();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("claude",ui.agent);assertEquals(0,writes);
 }
 @Test public void claudeTemplatePreviewLanguageApplyAndConflict()throws Exception{
  WorkspaceLanguage.select(activity,"en");show();ui.switchAgent("claude");waitIdle();ui.template();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();String text=((TextView)d.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString();assertTrue(text.contains("CLAUDE.md"));assertFalse(text.contains("AGENTS.md"));assertEquals(0,writes);d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("claude",last.getString("agent"));assertEquals(AgentInstructions.builtInTemplate(activity,"en","claude"),lastText);
  ui.editor.setText("keep on conflict");conflict=true;find("instructions-save").performClick();waitIdle();assertEquals("keep on conflict",ui.editor.getText().toString());
 }
 @Test public void claudeProjectImportTargetsBaseAndEnvironmentIsReadOnly()throws Exception{
  show();ui.switchAgent("claude");waitIdle();find("instructions-tab-project").performClick();waitIdle();find("instructions-file-CLAUDE.local.md").performClick();waitIdle();assertEquals("CLAUDE.md",ui.importTarget("read").getString("name"));
  assertFalse(find("instructions-environment").isClickable());assertEquals("ubuntu-ai",ui.environment());assertEquals("ubuntu-ai",last.getString("environment"));
 }
 @Test public void explicitMemorySelectionPersistsPathAndRejectsOtherEnvironment()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();find("instructions-file-LEARNING.md").performClick();waitIdle();
  WorkspaceStore.Session source=new WorkspaceStore.Session();source.environment="termux";ui.selectMemoryFile(source,new ProjectFiles.Entry("/shared","LEARNING.md",false,2));assertEquals("",ui.memoryPath());
  source.environment="proot:ubuntu-ai";ui.selectMemoryFile(source,new ProjectFiles.Entry("/shared","LEARNING.md",false,2));waitIdle();
  assertEquals("/shared/LEARNING.md",ui.memoryPath());assertEquals("/shared/LEARNING.md",ui.document.getString("path"));ui.load();waitIdle();assertEquals("/shared/LEARNING.md",ui.document.getString("path"));assertEquals(0,writes);
 }
 @Test public void learningIsSeparateSelectedAndDoesNotCreateOnOpen()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();projectExists=false;projectText="";
  find("instructions-file-LEARNING.md").performClick();waitIdle();assertTrue(find("instructions-file-LEARNING.md").isSelected());assertEquals(0,writes);
  assertNotNull(find("instructions-memory-path"));assertNotNull(find("instructions-memory-template"));
  assertTrue(((TextView)find("instructions-project-explanation")).getText().toString().contains("не автоматическое обучение"));
  find("instructions-memory-template").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  assertTrue(((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString().contains("Подтверждённые уроки"));preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);
 }
 @Test public void learningImportCancelConflictAndTarget()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();find("instructions-file-LEARNING.md").performClick();waitIdle();
  importedPreview("lessons");AlertDialog p=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  assertTrue(((TextView)p.getWindow().getDecorView().findViewWithTag("instructions-import-target")).getText().toString().endsWith("/project/LEARNING.md"));
  p.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);
  importedPreview("lessons");p=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();conflict=true;p.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();
  assertEquals("current\r\n",ui.editor.getText().toString());assertEquals("LEARNING.md",last.getString("name"));
 }
 @Test public void learningSwitchProtectsDraftAndKeepsThreeNamesUnwrapped()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();ui.editor.setText("draft AGENTS");find("instructions-file-LEARNING.md").performClick();
  AlertDialog guard=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();guard.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("draft AGENTS",ui.editor.getText().toString());assertEquals("AGENTS.md",ui.name);
  find("instructions-file-LEARNING.md").performClick();guard=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();guard.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("current\r\n",ui.editor.getText().toString());
  measured(ui.root,280,500);for(String n:new String[]{"AGENTS.md","AGENTS.override.md","LEARNING.md"})assertEquals(1,((TextView)find("instructions-file-"+n)).getMaxLines());
 }
 @Test public void narrowLargeFontTabsDoNotWrapAndEditingControlsSurviveReducedImeViewport()throws Exception{
  for(String language:new String[]{"ru","en"})for(float font:new float[]{1f,1.3f,1.5f})for(int width:new int[]{280,320,360}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,language);android.content.res.Resources res=activity.getResources();android.content.res.Configuration config=new android.content.res.Configuration(res.getConfiguration());config.fontScale=font;res.updateConfiguration(config,res.getDisplayMetrics());res.getDisplayMetrics().scaledDensity=res.getDisplayMetrics().density*font;
   show();int w=ui.dp(width),h=ui.dp(360);measured(ui.root,w,h);
   for(String tab:new String[]{"general","project","diagnostics"}){
    find("instructions-tab-"+tab).performClick();waitIdle();measured(ui.root,w,h);
    TextView active=(TextView)find("instructions-tab-"+tab);assertTrue(active.isSelected());assertEquals(1,active.getLineCount());assertTrue("unclipped label: "+active.getText(),active.getMeasuredWidth()>=Math.ceil(active.getPaint().measureText(active.getText().toString()))+active.getPaddingLeft()+active.getPaddingRight()-1);
    assertTrue(find("instructions-tabs-scroll") instanceof HorizontalScrollView);assertTrue(bounds(ui.root,ui.close).bottom<=h);assertTrue(ui.close.getHeight()>0);
    if(!tab.equals("diagnostics")){
     assertTrue(ui.editor.getHeight()>0);assertTrue(find("instructions-content-scroll") instanceof ScrollView);assertFalse("footer actions must not overlap",android.graphics.Rect.intersects(bounds(ui.root,ui.save),bounds(ui.root,ui.close)));assertTrue(ui.save.getHeight()>0);
     if(tab.equals("project"))for(String file:new String[]{"AGENTS.md","AGENTS.override.md","LEARNING.md"}){find("instructions-file-"+file).performClick();waitIdle();measured(ui.root,w,h);HorizontalScrollView files=(HorizontalScrollView)find("instructions-files-scroll");TextView selectedFile=(TextView)find("instructions-file-"+file);assertTrue(selectedFile.isSelected());assertEquals(1,selectedFile.getLineCount());assertTrue(selectedFile.getMeasuredWidth()>=Math.ceil(selectedFile.getPaint().measureText(file))+selectedFile.getPaddingLeft()+selectedFile.getPaddingRight()-1);assertTrue("selected file intersects viewport",selectedFile.getRight()>files.getScrollX()&&selectedFile.getLeft()<files.getScrollX()+files.getWidth());}
    }
   }
  }
 }
 @Test public void previousEqualWidthTabsReportConstrainedTextArea()throws Exception{
  LinearLayout old=new LinearLayout(activity);for(String name:new String[]{"Общие","Проект","Диагностика"}){TextView v=WorkspaceDialogs.action(activity,name,0);v.setGravity(Gravity.CENTER);v.setSingleLine(false);v.setMaxLines(Integer.MAX_VALUE);old.addView(v,new LinearLayout.LayoutParams(0,-2,1));}measured(old,Math.round(280*activity.getResources().getDisplayMetrics().density),ui==null?500:ui.dp(500));TextView diagnostics=(TextView)old.getChildAt(2);assertTrue(diagnostics.getWidth()>diagnostics.getPaddingLeft()+diagnostics.getPaddingRight());assertTrue(diagnostics.getWidth()<=Math.ceil(280*activity.getResources().getDisplayMetrics().density/3));System.out.println("OLD_TABS width="+diagnostics.getWidth()+" textWidth="+diagnostics.getPaint().measureText("Диагностика")+" padding="+(diagnostics.getPaddingLeft()+diagnostics.getPaddingRight())+" lines="+diagnostics.getLineCount());
 }
 @Test public void narrowLargeFontTemplateAndAdditionKeepSwitchesScrollAndButtonsAccessible()throws Exception{
  for(String language:new String[]{"ru","en"}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,language);android.content.res.Resources res=activity.getResources();android.content.res.Configuration config=new android.content.res.Configuration(res.getConfiguration());config.fontScale=1.5f;res.updateConfiguration(config,res.getDisplayMetrics());res.getDisplayMetrics().scaledDensity=res.getDisplayMetrics().density*1.5f;show();
   for(boolean addition:new boolean[]{false,true}){
    if(addition)ui.storageAddition();else ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    checkPreviewBounds(preview,ui.dp(280),ui.dp(420));for(String code:new String[]{"ru","en"}){View toggle=preview.getWindow().getDecorView().findViewWithTag("instructions-preview-"+code);assertEquals(1,((TextView)toggle).getLineCount());toggle.performClick();assertTrue(toggle.isSelected());}preview.dismiss();assertEquals(0,writes);
   }
  }
 }
 @Test public void generalEditorShowsActualOverrideAndKeepsExactTextOnSave()throws Exception{
  show();assertEquals("current\r\n",ui.editor.getText().toString());assertTrue(((TextView)find("instructions-source")).getText().toString().contains("/custom/codex/AGENTS.override.md"));
  String text="\ufeffТочный\r\nтекст  \r\n";ui.editor.setText(text);find("instructions-save").performClick();waitIdle();assertEquals(text,lastText);assertEquals(1,writes);assertEquals("/custom/codex/AGENTS.override.md",last.getString("path"));assertEquals("fixture-revision",last.getString("revision"));assertTrue(ui.status.getText().toString().contains("Выполняющийся запрос не изменяется"));
 }
 @Test public void closeWithUnsavedDraftDoesNotSave()throws Exception{show();ui.editor.setText("draft");find("instructions-close").performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,writes);assertEquals("current\r\n",sourceText);assertFalse(ui.closed);AlertDialog confirmation=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(ui.closed);assertEquals(0,writes);}
 @Test public void templatePreviewCancelDoesNotChangeFileOrDraft()throws Exception{show();ui.editor.setText("draft");ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);assertEquals("draft",ui.editor.getText().toString());}
 @Test public void templateIsVisibleBeforeExplicitApplyAndSave()throws Exception{show();ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(preview);assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(AgentInstructions.builtInTemplate(activity,"ru"),lastText);assertEquals(1,writes);assertTrue(ui.status.getText().toString().startsWith("Сохранено."));}
 @Test public void parallelConflictKeepsDraftAndDoesNotClaimSaved()throws Exception{show();conflict=true;ui.editor.setText("my draft");find("instructions-save").performClick();waitIdle();assertEquals("my draft",ui.editor.getText().toString());assertTrue(ui.status.getText().toString().contains("не перезаписана"));assertFalse(ui.status.getText().toString().startsWith("Сохранено"));assertEquals("current\r\n",sourceText);}
 @Test public void previousRevisionRequiresPreviewAndExplicitRestore()throws Exception{show();ui.history(true);waitIdle();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("older revision",lastText);}
 @Test public void projectFilesAndDiagnosticsAreSeparateTabsAndDetailsCollapse()throws Exception{show();find("instructions-tab-project").performClick();waitIdle();assertEquals("/project/AGENTS.md",ui.document.getString("path"));find("instructions-file-AGENTS.override.md").performClick();waitIdle();assertEquals("/project/AGENTS.override.md",ui.document.getString("path"));find("instructions-tab-diagnostics").performClick();waitIdle();View toggle=find("instructions-details-toggle");assertNotNull(toggle);assertEquals(0,writes);assertNull(ui.editor);}
 @Test public void profileOnlySuggestsProjectAndDoesNotChangeGeneralSource()throws Exception{WorkspaceStore.Session s=new WorkspaceStore.Session();s.codexEnvironment="ubuntu-ai";s.directory="~/work";s.agentConnectionCommand="/private/launcher";assertEquals("/root/work",AgentInstructionsUi.suggested(s));show();assertEquals("/custom/codex/AGENTS.override.md",ui.document.getString("path"));assertFalse(last.has("agentConnectionCommand"));}
 @Test public void englishLabelsAndFactualApplicationNotice()throws Exception{WorkspaceLanguage.select(activity,"en");show();assertEquals("General",((TextView)find("instructions-tab-general")).getText().toString());assertTrue(AgentInstructionsUi.saved(activity).contains("Shared rules may apply"));}
 @Test public void entryIsInGlobalSettingsWithoutCreatingAChat()throws Exception{
  activity.setContentView(R.layout.activity_termux);java.lang.reflect.Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController(activity);
  try{java.lang.reflect.Method open=WorkspaceController.class.getDeclaredMethod("globalSettings");open.setAccessible(true);open.invoke(controller);View settings=activity.findViewById(R.id.workspace_content).findViewWithTag("workspace-global-settings");assertNotNull(settings);assertTrue(hasText(settings,"Инструкции агентов"));assertNull(activity.getCurrentSession());assertTrue(controller.onBack());Shadows.shadowOf(Looper.getMainLooper()).idle();}finally{controller.close();}
 }
 boolean hasText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return true;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++)if(hasText(((ViewGroup)view).getChildAt(n),text))return true;return false;}

 @Test public void storageAdditionIsPreviewedAndAppendedWithoutReplacingCustomPrefix()throws Exception{
  storageOffer=true;show();String before=ui.editor.getText().toString();ui.storageAddition();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(before,ui.editor.getText().toString());assertEquals(0,writes);
  ui.storageAddition();preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(before+"\n"+AgentInstructions.storageAddition(AgentInstructions.builtInTemplate(activity,"ru")),lastText);assertEquals(1,writes);ui.storageAddition();assertEquals(1,writes);
 }

 @Test public void projectExplanationAndActualSourceStayIndependentOfEditingInBothLanguages()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();
  assertTrue(((TextView)find("instructions-project-explanation")).getText().toString().contains("Дополняют общие правила"));
  assertEquals("Codex читает в этой папке: AGENTS.md",((TextView)find("instructions-project-active")).getText().toString());
  find("instructions-file-AGENTS.override.md").performClick();waitIdle();
  assertEquals("Codex читает в этой папке: AGENTS.md",((TextView)find("instructions-project-active")).getText().toString());
  assertTrue(((TextView)find("instructions-source")).getText().toString().startsWith("Редактирование:"));assertEquals(0,writes);
  projectSource="/project/AGENTS.override.md";ui.load();waitIdle();assertTrue(((TextView)find("instructions-project-active")).getText().toString().endsWith("AGENTS.override.md"));
  projectSource=null;ui.load();waitIdle();assertEquals("В этой папке проектные инструкции отсутствуют.",((TextView)find("instructions-project-active")).getText().toString());
  WorkspaceLanguage.select(activity,"en");projectSource="/project/AGENTS.md";ui.load();waitIdle();
  assertTrue(((TextView)find("instructions-project-explanation")).getText().toString().contains("Shared rules remain in effect"));
  assertEquals("Codex reads in this folder: AGENTS.md",((TextView)find("instructions-project-active")).getText().toString());
  projectSource=null;ui.load();waitIdle();assertEquals("No project instructions in this folder.",((TextView)find("instructions-project-active")).getText().toString());
 }
 @Test public void projectStateRefreshesAfterSaveWithoutSwitchingEditTarget()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();find("instructions-file-AGENTS.override.md").performClick();waitIdle();
  projectSource="/project/AGENTS.override.md";ui.editor.setText("override rules");find("instructions-save").performClick();waitIdle();
  assertTrue(((TextView)find("instructions-project-active")).getText().toString().endsWith("AGENTS.override.md"));assertEquals("AGENTS.override.md",ui.name);assertTrue(ui.status.getText().toString().startsWith("Сохранено."));
 }
 String layoutTree(View v,String prefix){String result=prefix+v.getClass().getSimpleName()+" measured="+v.getMeasuredWidth()+"x"+v.getMeasuredHeight()+" actual="+v.getWidth()+"x"+v.getHeight()+" at="+v.getLeft()+","+v.getTop()+" tag="+v.getTag()+"\n";if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++)result+=layoutTree(((ViewGroup)v).getChildAt(n),prefix+" ");return result;}
 void checkPreviewBounds(AlertDialog preview,int width,int height){
  // Framework layout fixture, not a claim of phone/IME acceptance.
  Shadows.shadowOf(Looper.getMainLooper()).idle();
  preview.getWindow().setLayout(width,height);View decor=preview.getWindow().getDecorView();
  decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));decor.layout(0,0,width,height);
  ScrollView scroll=decor.findViewWithTag("instructions-preview-scroll");assertNotNull(scroll);assertTrue("scroll viewport height "+scroll.getHeight()+"\n"+layoutTree(decor,""),scroll.getHeight()>0);assertTrue("text must exceed viewport: "+scroll.getChildAt(0).getHeight()+" / "+scroll.getHeight(),scroll.getChildAt(0).getHeight()>scroll.getHeight());
  for(int button:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE}){
   View b=preview.getButton(button);android.graphics.Rect rect=new android.graphics.Rect();b.getDrawingRect(rect);((ViewGroup)decor).offsetDescendantRectToMyCoords(b,rect);
   assertTrue("button must fit the preview: "+rect,rect.top>=0&&rect.bottom<=height);assertTrue(b.getHeight()>0);assertEquals(View.VISIBLE,b.getVisibility());
  }
  android.graphics.Rect scrollBounds=new android.graphics.Rect(),buttonBounds=new android.graphics.Rect();scroll.getDrawingRect(scrollBounds);((ViewGroup)decor).offsetDescendantRectToMyCoords(scroll,scrollBounds);View apply=preview.getButton(AlertDialog.BUTTON_POSITIVE);apply.getDrawingRect(buttonBounds);((ViewGroup)decor).offsetDescendantRectToMyCoords(apply,buttonBounds);assertTrue("scroll "+scrollBounds+" overlaps apply "+buttonBounds,scrollBounds.bottom<=buttonBounds.top);
  for(String language:new String[]{"ru","en"}){View toggle=decor.findViewWithTag("instructions-preview-"+language);if(toggle!=null){android.graphics.Rect bounds=new android.graphics.Rect();toggle.getDrawingRect(bounds);((ViewGroup)decor).offsetDescendantRectToMyCoords(toggle,bounds);assertTrue("language toggle must stay above scrolling text",bounds.top>=0&&bounds.bottom<=scrollBounds.top);assertTrue(toggle.getHeight()>0);}}
  scroll.fullScroll(View.FOCUS_DOWN);assertTrue(scroll.getScrollY()>0);
 }
 @Test @Config(qualifiers="w360dp-h640dp") public void longTemplateKeepsButtonsBelowScrollInPortraitAndLandscape()throws Exception{
  show();String longText=AgentInstructions.builtInTemplate(activity,"ru");
  ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(preview.getWindow().getAttributes().height>0);assertTrue(preview.getWindow().getAttributes().height<=activity.getResources().getDisplayMetrics().heightPixels);
  checkPreviewBounds(preview,360,576);checkPreviewBounds(preview,640,288);
  preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(longText,lastText);assertTrue(ui.status.getText().toString().startsWith("Сохранено."));
 }
 @Test @Config(qualifiers="w360dp-h640dp") public void storageAndHistoryUseSameBoundedPreviewWithLargeTextAndCancel()throws Exception{
  storageOffer=true;show();StringBuilder value=new StringBuilder();for(int n=0;n<500;n++)value.append("Storage rule ").append(n).append("\n");ui.document.put("storageAddition",value.toString());
  ui.storageAddition();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).setTextSize(24);
  checkPreviewBounds(preview,360,576);checkPreviewBounds(preview,640,288);preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);assertEquals("current\r\n",ui.editor.getText().toString());
  WorkspaceLanguage.select(activity,"en");ui.preview("Previous revision",value.toString(),"Restore and save");preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();checkPreviewBounds(preview,640,288);assertEquals("Cancel",preview.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());preview.dismiss();assertEquals(0,writes);
 }

 @Test public void languageSwitchOnlyChangesPreviewAndCancelKeepsDraftLocaleAndSource()throws Exception{
  show();ui.editor.setText("my unsaved text");ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=preview.getWindow().getDecorView();
  assertEquals(AgentInstructions.builtInTemplate(activity,"ru"),((TextView)decor.findViewWithTag("instructions-preview-text")).getText().toString());assertTrue(decor.findViewWithTag("instructions-preview-ru").isSelected());
  assertTrue(((TextView)decor.findViewWithTag("instructions-preview-target")).getText().toString().contains("/custom/codex/AGENTS.override.md"));
  decor.findViewWithTag("instructions-preview-en").performClick();assertEquals(AgentInstructions.builtInTemplate(activity,"en"),((TextView)decor.findViewWithTag("instructions-preview-text")).getText().toString());assertTrue(((TextView)decor.findViewWithTag("instructions-preview-language")).getText().toString().endsWith("EN"));
  assertEquals("ru",WorkspaceLanguage.code(activity));assertEquals("my unsaved text",ui.editor.getText().toString());assertEquals("current\r\n",sourceText);assertEquals(0,writes);
  preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("my unsaved text",ui.editor.getText().toString());assertEquals(0,writes);
 }
 @Test public void englishDefaultCanExplicitlyApplyRussianAssetWithoutChangingAppLanguage()throws Exception{
  WorkspaceLanguage.select(activity,"en");show();ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=preview.getWindow().getDecorView();
  assertTrue(decor.findViewWithTag("instructions-preview-en").isSelected());assertEquals(AgentInstructions.builtInTemplate(activity,"en"),((TextView)decor.findViewWithTag("instructions-preview-text")).getText().toString());
  decor.findViewWithTag("instructions-preview-ru").performClick();assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(AgentInstructions.builtInTemplate(activity,"ru"),lastText);assertEquals("en",WorkspaceLanguage.code(activity));assertEquals("/custom/codex/AGENTS.override.md",last.getString("path"));assertTrue(ui.status.getText().toString().startsWith("Saved."));
 }
 @Test public void storageLanguageSelectionPreservesPrefixAndBlocksCrossLanguageDuplicate()throws Exception{
  storageOffer=true;show();String prefix="\ufeffCustom rules  \r\n";ui.editor.setText(prefix);ui.storageAddition();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=preview.getWindow().getDecorView();
  decor.findViewWithTag("instructions-preview-en").performClick();assertEquals(AgentInstructions.storageAddition(AgentInstructions.builtInTemplate(activity,"en")),((TextView)decor.findViewWithTag("instructions-preview-text")).getText().toString());assertEquals(prefix,ui.editor.getText().toString());assertEquals(0,writes);
  preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(prefix+"\n"+AgentInstructions.storageAddition(AgentInstructions.builtInTemplate(activity,"en")),lastText);assertEquals("ru",WorkspaceLanguage.code(activity));assertTrue(ui.status.getText().toString().startsWith("Сохранено."));ui.storageAddition();assertEquals(1,writes);assertTrue(ui.status.getText().toString().contains("RU или EN"));
 }
 @Test public void russianStorageSectionAlsoBlocksDuplicateInEnglishUi()throws Exception{
  sourceText="User prefix\n"+AgentInstructions.storageAddition(AgentInstructions.builtInTemplate(activity,"ru"));WorkspaceLanguage.select(activity,"en");show();ui.storageAddition();assertEquals(0,writes);assertTrue(ui.status.getText().toString().contains("RU or EN"));assertEquals(sourceText,ui.editor.getText().toString());
 }
 @Test public void applyingSelectedTemplateStillRefusesConflictAndRetainsDraft()throws Exception{
  show();ui.editor.setText("my draft");ui.template();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getWindow().getDecorView().findViewWithTag("instructions-preview-en").performClick();conflict=true;preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("my draft",ui.editor.getText().toString());assertEquals("current\r\n",sourceText);assertFalse(ui.status.getText().toString().startsWith("Сохранено"));
 }

 void reopenIn(String language)throws Exception{if(ui!=null){ui.dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();}WorkspaceLanguage.select(activity,language);WorkspaceLanguage.apply(activity);show();}
 String allText(View v){String text=v instanceof TextView?((TextView)v).getText().toString()+"\n":"";if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++)text+=allText(((ViewGroup)v).getChildAt(n));return text;}
 @Test public void instructionMenusHistoryAndDiscardConfirmationsAreLocalized()throws Exception{
  for(String language:new String[]{"ru","en"}){
   reopenIn(language);boolean ru=language.equals("ru");assertEquals(ru?"Отмена / Закрыть":"Cancel / Close",((TextView)find("instructions-close")).getText().toString());
   find("instructions-menu").performClick();AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(hasText(menu.getWindow().getDecorView(),ru?"Редакции и импорт":"Revisions and import"));
   String[] expected=ru?new String[]{"История редакций","Восстановить предыдущую версию","Импортировать инструкции"}:new String[]{"Revision history","Restore previous version","Import instructions"};
   for(int n=0;n<expected.length;n++)assertEquals(expected[n],menu.getListView().getItemAtPosition(n));menu.dismiss();
   ui.history(false);waitIdle();AlertDialog history=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(hasText(history.getWindow().getDecorView(),ru?"История редакций":"Revision history"));assertEquals(ru?"Закрыть":"Close",history.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());history.dismiss();
   ui.history(true);waitIdle();AlertDialog previous=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(hasText(previous.getWindow().getDecorView(),ru?"Предыдущая редакция":"Previous revision"));assertEquals(ru?"Восстановить и сохранить":"Restore and save",previous.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());assertEquals(ru?"Отмена":"Cancel",previous.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());previous.dismiss();
   ui.editor.setText("draft");find("instructions-tab-project").performClick();AlertDialog guard=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(hasText(guard.getWindow().getDecorView(),ru?"Несохранённые изменения":"Unsaved changes"));assertEquals(ru?"Не сохранять":"Discard",guard.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());guard.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("draft",ui.editor.getText().toString());assertEquals(0,writes);
   ui.editor.setText(sourceText);emptyHistory=true;ui.history(false);waitIdle();assertEquals(ru?"Предыдущих редакций нет.":"No previous revisions.",ui.status.getText().toString());emptyHistory=false;
  }
 }
 @Test public void diagnosticsKnownUnknownAndTechnicalDisclosureAreLocalized()throws Exception{
  for(String language:new String[]{"ru","en"})for(boolean verified:new boolean[]{false,true}){
   verifiedCli=verified;reopenIn(language);boolean ru=language.equals("ru");find("instructions-tab-diagnostics").performClick();waitIdle();String text=allText(ui.body);
   assertTrue(text.contains(verified?(ru?"Для этой версии проверено":"Tested for this version"):(ru?"Поведение этой версии не проверено":"This version's behavior is unverified")));
   assertTrue(text.contains(ru?"Выполняющийся запрос не изменяется":"A running request is unchanged"));assertTrue(text.contains(ru?"Общие правила берутся":"Shared rules come"));
   TextView toggle=(TextView)find("instructions-details-toggle");assertEquals(ru?"Пути и технические сведения ▾":"Paths and technical details ▾",toggle.getText().toString());toggle.performClick();TextView details=(TextView)((ViewGroup)toggle.getParent()).getChildAt(((ViewGroup)toggle.getParent()).getChildCount()-1);assertEquals(View.VISIBLE,details.getVisibility());assertEquals("/custom/codex",new JSONObject(details.getText().toString()).getString("codexHome"));assertEquals(0,writes);
  }
 }
 @Test public void failureMessagesAndCodesAreLocalizedWithoutExposingExceptionContents()throws Exception{
  for(String language:new String[]{"ru","en"}){
   reopenIn(language);boolean ru=language.equals("ru");assertTrue(ui.error("EDIT_CONFLICT").startsWith(ru?"Файл или источник изменился.":"The file or source changed."));assertEquals(ui.error("EDIT_CONFLICT"),ui.error("SOURCE_CHANGED"));
   for(String code:new String[]{"TEMPLATE_UNAVAILABLE","INSTRUCTIONS_UNAVAILABLE","UNSAFE_FILE","PRIVATE_STORAGE","TEXT_TOO_LARGE_OR_INVALID"}){String message=ui.error(code);assertTrue(message.startsWith(ru?"Не удалось выполнить операцию.":"Operation unavailable."));assertTrue(message.endsWith(code));}
   assertFalse(ui.error("unsafe exception body /private/secret").contains("/private/secret"));assertTrue(AgentInstructionsUi.saved(activity).startsWith(ru?"Сохранено.":"Saved."));
  }
 }
 @Test public void changingUiLanguagePreservesExactUserAndHistoryText()throws Exception{
  String custom="\ufeffМои правила / My rules  \r\n\tkeep this\r\n";historyText="\ufeffСтарая редакция / Prior revision  \r\n";
  reopenIn("ru");ui.editor.setText(custom);find("instructions-save").performClick();waitIdle();assertEquals(1,writes);
  for(String language:new String[]{"en","ru"}){
   reopenIn(language);assertEquals(custom,ui.editor.getText().toString());assertEquals(custom,sourceText);assertEquals(1,writes);
   ui.history(true);waitIdle();AlertDialog previous=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(historyText,((TextView)previous.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString());previous.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(custom,ui.editor.getText().toString());assertEquals(1,writes);
  }
 }
 @Test public void storagePreviewDefaultsToUiLanguageAndBothSwitchesRemainPreviewOnly()throws Exception{
  storageOffer=true;
  for(String language:new String[]{"ru","en"}){
   reopenIn(language);String prefix="Custom unsaved instructions  \r\n";ui.editor.setText(prefix);ui.storageAddition();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=preview.getWindow().getDecorView();assertTrue(decor.findViewWithTag("instructions-preview-"+language).isSelected());
   for(String selected:new String[]{"ru","en"}){decor.findViewWithTag("instructions-preview-"+selected).performClick();assertEquals(AgentInstructions.storageAddition(AgentInstructions.builtInTemplate(activity,selected)),((TextView)decor.findViewWithTag("instructions-preview-text")).getText().toString());assertEquals(prefix,ui.editor.getText().toString());assertEquals(language,WorkspaceLanguage.code(activity));assertEquals(0,writes);}
   assertEquals(language.equals("ru")?"Добавить и сохранить":"Append and save",preview.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(prefix,ui.editor.getText().toString());assertEquals(0,writes);
  }
 }
 @Test public void projectChooserValidationIsLocalizedAndCancelDoesNotSave()throws Exception{
  for(String language:new String[]{"ru","en"}){
   reopenIn(language);boolean ru=language.equals("ru");find("instructions-tab-project").performClick();waitIdle();find("instructions-choose-project").performClick();AlertDialog picker=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   assertTrue(hasText(picker.getWindow().getDecorView(),ru?"Проект Ubuntu AI":"Ubuntu AI project"));assertEquals(ru?"Открыть":"Open",picker.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());assertEquals(ru?"Папка":"Folder",picker.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());
   EditText input=picker.getWindow().getDecorView().findViewWithTag("instructions-project-input");input.setText("relative");picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertEquals(ru?"Нужен абсолютный путь":"Absolute path required",input.getError().toString());picker.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);
  }
 }

 ProjectFiles.Entry importFile(){return new ProjectFiles.Entry("/sdcard/Download","rules.md",false,32);}
 WorkspaceStore.Session importSource(){WorkspaceStore.Session source=new WorkspaceStore.Session();source.environment="termux";return source;}
 void importedPreview(String value)throws Exception{ui.importReader=(source,file)->value;ui.importSelected(importSource(),importFile());waitIdle();}
 @Test public void menusOmitReloadAndAdditionAndRetainLocalizedImports()throws Exception{
  storageOffer=true;
  for(String language:new String[]{"ru","en"}){
   reopenIn(language);boolean ru=language.equals("ru");assertNull(find("instructions-storage-offer"));find("instructions-menu").performClick();AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,menu.getListView().getCount());assertEquals(ru?"Импортировать инструкции":"Import instructions",menu.getListView().getItemAtPosition(2));menu.dismiss();
   find("instructions-tab-project").performClick();waitIdle();find("instructions-menu").performClick();menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,menu.getListView().getCount());assertEquals(ru?"Импортировать AGENTS.md":"Import AGENTS.md",menu.getListView().getItemAtPosition(2));menu.dismiss();assertEquals(0,writes);
  }
 }
 @Test public void importUsesBuiltInChooserAndDirtyDraftRequiresExplicitDecision()throws Exception{
  show();ui.editor.setText("draft");ui.importInstructions();AlertDialog prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals("Сохранить черновик",prompt.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());assertEquals("Отказаться от черновика",prompt.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("draft",ui.editor.getText().toString());assertEquals(0,writes);
  ui.importInstructions();prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(sourceText,ui.editor.getText().toString());AlertDialog chooser=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(chooser.getListView());assertEquals("Termux",chooser.getListView().getItemAtPosition(0));chooser.dismiss();assertEquals(0,writes);
 }
 @Test public void saveDraftBeforeImportUsesExistingSaveAndStopsOnConflict()throws Exception{
  show();ui.editor.setText("draft to save");ui.importInstructions();AlertDialog prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();conflict=true;prompt.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("draft to save",ui.editor.getText().toString());assertNull(ui.importPicker);assertEquals("current\r\n",sourceText);
  conflict=false;ui.importInstructions();prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("draft to save",sourceText);assertNotNull(ui.importPicker);assertNotNull(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getListView());
 }
 @Test public void projectImportCreatesOnlyBaseFileEvenWhenEditingOverride()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();find("instructions-file-AGENTS.override.md").performClick();waitIdle();projectExists=false;projectText="";String value="\ufeffImported rules  \r\n";importedPreview(value);AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-import-target")).getText().toString().endsWith("/project/AGENTS.md"));assertEquals(value,((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString());assertEquals(0,writes);
  preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("project",last.getString("scope"));assertEquals("AGENTS.md",last.getString("name"));assertEquals("/project/AGENTS.md",last.getString("path"));assertEquals(value,projectText);assertEquals("AGENTS.md",ui.name);assertEquals(value,ui.editor.getText().toString());assertEquals("current\r\n",sourceText);assertTrue(ui.status.getText().toString().startsWith("Сохранено."));
 }
 @Test public void projectImportWarnsAboutActiveOverrideAndCancelPreservesFile()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();projectSource="/project/AGENTS.override.md";importedPreview("replacement");AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-import-warning")).getText().toString().contains("останется перекрыт"));preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("current\r\n",projectText);assertEquals(0,writes);
  importedPreview("replacement");preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertTrue(((TextView)find("instructions-project-active")).getText().toString().endsWith("AGENTS.override.md"));assertEquals("replacement",ui.editor.getText().toString());
 }
 @Test public void generalImportUsesActualSharedOverrideAndNeverChangesProjectOrLocale()throws Exception{
  WorkspaceLanguage.select(activity,"en");show();String value="\ufeffМои инструкции / User rules  \r\n";importedPreview(value);AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=preview.getWindow().getDecorView();assertTrue(((TextView)decor.findViewWithTag("instructions-import-target")).getText().toString().endsWith("/custom/codex/AGENTS.override.md"));assertEquals("This replaces the active shared AGENTS.override.md.",((TextView)decor.findViewWithTag("instructions-import-warning")).getText().toString());assertEquals("Import and save",preview.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("general",last.getString("scope"));assertEquals("/custom/codex/AGENTS.override.md",last.getString("path"));assertEquals(value,sourceText);assertEquals("current\r\n",projectText);assertEquals("en",WorkspaceLanguage.code(activity));assertTrue(ui.status.getText().toString().startsWith("Saved."));
 }
 @Test public void importConflictAndReadFailureDoNotOverwriteFileOrDraft()throws Exception{
  show();importedPreview("imported");AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();conflict=true;preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("current\r\n",sourceText);assertEquals("current\r\n",ui.editor.getText().toString());assertFalse(ui.status.getText().toString().startsWith("Сохранено"));
  int attempted=writes;ui.importReader=(source,entry)->{throw new java.io.IOException("must not expose source details");};ui.importSelected(importSource(),importFile());waitIdle();assertEquals(attempted,writes);assertEquals("current\r\n",sourceText);assertFalse(ui.status.getText().toString().contains("must not expose"));assertTrue(ui.status.getText().toString().contains("Не удалось прочитать"));
 }
 @Test public void importRejectsOversizedDirectoryBinaryAndClosedEditor()throws Exception{
  show();ui.importReader=(source,entry)->"binary\0data";ui.importSelected(importSource(),importFile());waitIdle();assertEquals(0,writes);assertTrue(ui.status.getText().toString().contains("UTF-8"));
  ui.importSelected(importSource(),new ProjectFiles.Entry("/","oversized",false,TextFileEdits.MAX_BYTES+1));waitIdle();assertEquals(0,writes);ui.importSelected(importSource(),new ProjectFiles.Entry("/","directory",true,0));waitIdle();assertEquals(0,writes);
  ui.dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();ui.importSelected(importSource(),importFile());assertEquals(0,writes);
 }

 @Test public void closingDuringImportReadPreventsLatePreviewOrDestinationRequest()throws Exception{
  show();java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);JSONObject before=last;
  ui.importReader=(source,entry)->{entered.countDown();if(!release.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new java.io.IOException("fixture timeout");return "late text";};
  ui.importSelected(importSource(),importFile());assertTrue(entered.await(2,java.util.concurrent.TimeUnit.SECONDS));ui.dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();release.countDown();assertTrue(ui.worker.awaitTermination(3,java.util.concurrent.TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(ui.closed);assertEquals(0,writes);assertSame(before,last);assertEquals("current\r\n",sourceText);
 }


 @Test public void outsideCancelKeepsDraftUntilExplicitDiscard()throws Exception{
  show();ui.editor.setText("unsaved outside draft");ui.dialog.cancel();Shadows.shadowOf(Looper.getMainLooper()).idle();
  assertFalse(ui.closed);assertEquals("unsaved outside draft",ui.editor.getText().toString());assertEquals(0,writes);
  AlertDialog prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertTrue(ui.dialog.isShowing());
  ui.dialog.cancel();prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(ui.closed);assertEquals(0,writes);
 }
 @Test public void backProtectsDraftAndBusyCloseDoesNothing()throws Exception{
  show();ui.editor.setText("draft");ui.dialog.onBackPressed();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(ui.closed);assertEquals("draft",ui.editor.getText().toString());
  AlertDialog prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
  ui.busy=true;ui.dialog.cancel();ui.dialog.onBackPressed();assertFalse(ui.closed);assertEquals(0,writes);ui.busy=false;
 }
 @Test public void cleanOutsideCloseRemainsImmediate()throws Exception{show();ui.dialog.cancel();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(ui.closed);assertEquals(0,writes);}

 @Test @Config(qualifiers="w360dp-h640dp") public void selectedTabsAndEditedFileRemainVisibleAndAccessibleInBothLanguages()throws Exception{
  float old=activity.getResources().getDisplayMetrics().scaledDensity;
  try{activity.getResources().getDisplayMetrics().scaledDensity=old*1.6f;
   for(String language:new String[]{"ru","en"}){
    WorkspaceLanguage.select(activity,language);show();assertTrue(find("instructions-tab-general").isSelected());assertFalse(find("instructions-tab-project").isSelected());
    find("instructions-tab-project").performClick();waitIdle();assertFalse(find("instructions-tab-general").isSelected());assertTrue(find("instructions-tab-project").isSelected());
    projectSource="/project/AGENTS.override.md";ui.load();waitIdle();assertTrue(find("instructions-file-AGENTS.md").isSelected());assertFalse(find("instructions-file-AGENTS.override.md").isSelected());assertTrue(((TextView)find("instructions-project-active")).getText().toString().endsWith("AGENTS.override.md"));
    find("instructions-file-AGENTS.override.md").performClick();waitIdle();assertTrue(find("instructions-file-AGENTS.override.md").isSelected());assertFalse(find("instructions-file-AGENTS.md").isSelected());
    android.view.accessibility.AccessibilityNodeInfo info=android.view.accessibility.AccessibilityNodeInfo.obtain();find("instructions-file-AGENTS.override.md").onInitializeAccessibilityNodeInfo(info);assertTrue(info.isSelected());info.recycle();
    ui.root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(576,View.MeasureSpec.EXACTLY));ui.root.layout(0,0,360,576);
    for(String tab:new String[]{"general","project","diagnostics"}){View v=find("instructions-tab-"+tab);assertTrue(v.getHeight()>0);assertEquals(View.VISIBLE,v.getVisibility());assertTrue(v.getBottom()<=576);}
    find("instructions-tab-diagnostics").performClick();waitIdle();assertTrue(find("instructions-tab-diagnostics").isSelected());assertFalse(find("instructions-tab-project").isSelected());ui.dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();
    show();assertTrue("new editor starts on the actual General tab",find("instructions-tab-general").isSelected());assertEquals("current\r\n",ui.editor.getText().toString());assertEquals(0,writes);ui.dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();
   }
  }finally{activity.getResources().getDisplayMetrics().scaledDensity=old;}
 }
 @Test public void openingTemplatePreservesDraftUntilExplicitApply()throws Exception{
  for(String lang:new String[]{"ru","en"})for(String agent:new String[]{"codex","claude"}){
   WorkspaceLanguage.select(activity,lang);if(ui!=null)ui.dialog.dismiss();show();ui.switchAgent(agent);waitIdle();ui.editor.setText("UNSAVED exact text\r\n");int before=writes;String path=ui.document.getString("path");find("instructions-template-view").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(preview.isShowing());assertEquals(View.VISIBLE,preview.getButton(AlertDialog.BUTTON_POSITIVE).getVisibility());
   TextView text=preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text");assertEquals(AgentInstructions.builtInTemplate(activity,lang,agent),text.getText().toString());preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(before,writes);assertEquals("UNSAVED exact text\r\n",ui.editor.getText().toString());assertEquals(path,ui.document.getString("path"));
  }
 }
 @Test public void retainedEnvironmentIsUsedForBothAgentsAndWrites()throws Exception{
  show();ui.selectedEnvironment="ubuntu";ui.load();waitIdle();assertEquals("ubuntu",last.getString("environment"));assertFalse(find("instructions-environment").isClickable());assertTrue(((TextView)find("instructions-environment")).getText().toString().contains("Ubuntu"));
  for(String agent:new String[]{"codex","claude"}){ui.switchAgent(agent);waitIdle();assertEquals("ubuntu",last.getString("environment"));ui.editor.setText("same environment rules");find("instructions-save").performClick();waitIdle();assertEquals("ubuntu",last.getString("environment"));}
 }

 @Test public void templateOnlyAvailableInGeneralWithoutProjectOrDiagnosticsRouting()throws Exception{
  for(String agent:new String[]{"codex","claude"}){
   if(ui!=null)ui.dialog.dismiss();show();ui.switchAgent(agent);waitIdle();assertEquals(View.VISIBLE,find("instructions-template-view").getVisibility());
   find("instructions-tab-project").performClick();waitIdle();assertEquals(View.GONE,find("instructions-template-view").getVisibility());ui.editor.setText("project draft");JSONObject prior=last;ui.viewTemplate();ui.template();assertSame(prior,last);assertEquals("project draft",ui.editor.getText().toString());assertEquals(0,writes);
   ui.editor.setText(ui.document.optString("text"));find("instructions-tab-diagnostics").performClick();waitIdle();assertEquals(View.GONE,find("instructions-template-view").getVisibility());prior=last;ui.viewTemplate();assertSame(prior,last);find("instructions-tab-general").performClick();waitIdle();assertEquals(View.VISIBLE,find("instructions-template-view").getVisibility());
  }
 }
 @Test public void templateViewExplicitApplyRetainsDraftOnConflict()throws Exception{
  show();ui.editor.setText("preserve on failure");conflict=true;ui.viewTemplate();AlertDialog p=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,writes);p.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("preserve on failure",ui.editor.getText().toString());assertEquals(1,writes);
 }

 @Test public void projectActionDistinguishesExistingBaseFromMissingFileForBothAgentsAndEntries()throws Exception{
  for(String language:new String[]{"ru","en"})for(String agent:new String[]{"codex","claude"})for(boolean session:new boolean[]{false,true}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,language);projectExists=true;projectText="existing project instructions";
   if(session){WorkspaceStore.Session source=new WorkspaceStore.Session();source.environment=agent.equals("codex")?"codex":"ubuntu";source.claudeAgent=agent.equals("claude");source.codexEnvironment="ubuntu-ai";source.directory="/project";ui=AgentInstructionsUi.sessionEditor(activity,source,this::backend);ui.show();waitIdle();}
   else{show();if(agent.equals("claude")){ui.switchAgent(agent);waitIdle();}find("instructions-tab-project").performClick();waitIdle();}
   String edit=language.equals("ru")?"Открыть инструкцию проекта":"Open project instruction";assertEquals(edit,ui.projectCreate.getText().toString());ui.editor.setText("preserved draft");ui.editor.setSelection(3);JSONObject before=last;int heightBefore=ui.status.getVisibility();ui.projectCreate.performClick();assertNull(ui.creationDialog);assertSame(before,last);assertEquals("preserved draft",ui.editor.getText().toString());assertEquals(3,ui.editor.getSelectionStart());assertTrue(ui.editor.hasFocus());assertEquals(heightBefore,ui.status.getVisibility());assertTrue(Shadows.shadowOf((android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).isSoftInputVisible());assertEquals(0,writes);
   // The same action reads the base when another project file is selected.
   ui.editor.setText(ui.document.optString("text"));String other=agent.equals("codex")?"AGENTS.override.md":"CLAUDE.local.md";find("instructions-file-"+other).performClick();waitIdle();assertEquals(edit,ui.projectCreate.getText().toString());ui.projectCreate.performClick();waitIdle();assertEquals(ui.baseName(),ui.name);assertNull(ui.creationDialog);
   projectExists=false;projectText="";ui.load();waitIdle();assertEquals(edit,ui.projectCreate.getText().toString());ui.projectCreate.performClick();waitIdle();assertNotNull(ui.creationDialog);assertTrue(ui.creationDialog.isShowing());assertEquals(0,writes);ui.creationDialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();
  }
 }
 @Test public void creationFormRemainsOpenAcrossParentRefreshAndRetainsItsDraft()throws Exception{
  for(String agent:new String[]{"codex","claude"})for(boolean fromSession:new boolean[]{false,true}){
   if(ui!=null)ui.dialog.dismiss();projectExists=false;projectText="";sourceText="SECRET_GENERAL_INSTRUCTIONS";
   if(fromSession){WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment=agent.equals("codex")?"codex":"ubuntu";profile.claudeAgent=agent.equals("claude");profile.codexEnvironment="ubuntu-ai";profile.directory="/project";ui=AgentInstructionsUi.sessionEditor(activity,profile,this::backend);ui.show();waitIdle();}
   else{show();if(agent.equals("claude")){ui.switchAgent(agent);waitIdle();}find("instructions-tab-project").performClick();waitIdle();}
   ui.createProjectInstructions();waitIdle();AlertDialog form=ui.creationDialog;assertNotNull(form);assertTrue(form.isShowing());EditText input=form.getWindow().getDecorView().findViewWithTag("instructions-create-field-0");input.setText("SECRET_FORM_DRAFT");View formBody=(View)input.getParent().getParent().getParent();View decor=form.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.AT_MOST));decor.layout(0,0,decor.getMeasuredWidth(),decor.getMeasuredHeight());assertTrue(formBody.getWidth()>0);assertTrue(formBody.getHeight()>0);
   // Parent rendering and asynchronous reload must not dismiss or replace the child.
   ui.render(new JSONObject(ui.document.toString()));assertSame(form,ui.creationDialog);assertTrue(form.isShowing());assertEquals("SECRET_FORM_DRAFT",input.getText().toString());ui.load();waitIdle();assertSame(form,ui.creationDialog);assertTrue(form.isShowing());assertEquals("SECRET_FORM_DRAFT",input.getText().toString());
   // An unexpected child dismissal preserves fields when reopened in this editor.
   form.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();ui.createProjectInstructions();waitIdle();AlertDialog reopened=ui.creationDialog;assertNotSame(form,reopened);assertEquals("SECRET_FORM_DRAFT",((EditText)reopened.getWindow().getDecorView().findViewWithTag("instructions-create-field-0")).getText().toString());
   ui.dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(reopened.isShowing());
  }
 }
 @Test public void creationSaveKeepsOriginalRevisionAfterParentRefreshAndCloseButtonProtectsDraft()throws Exception{
  projectExists=false;projectText="";show();find("instructions-tab-project").performClick();waitIdle();ui.createProjectInstructions();AlertDialog form=ui.creationDialog;EditText purpose=form.getWindow().getDecorView().findViewWithTag("instructions-create-field-0");purpose.setText("keep");AgentManagerLayout box=(AgentManagerLayout)purpose.getParent().getParent().getParent();ui.render(new JSONObject(ui.document.toString()).put("revision","new-parent-revision"));((TextView)box.footer.getChildAt(0)).performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();conflict=true;preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("fixture-revision",last.optString("revision"));assertTrue(form.isShowing());assertEquals("keep",purpose.getText().toString());preview.dismiss();form.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();ui.editor.setText("keep parent draft");find("instructions-close").performClick();AlertDialog guard=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();guard.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(ui.dialog.isShowing());assertEquals("keep parent draft",ui.editor.getText().toString());
 }
 @Test public void projectCreationQueuesEarlyTapAndCoalescesRepeatedOpensFromBothEntries()throws Exception{
  for(String agent:new String[]{"codex","claude"})for(boolean session:new boolean[]{false,true}){
   if(ui!=null)ui.dialog.dismiss();projectExists=false;projectText="";java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicInteger reads=new java.util.concurrent.atomic.AtomicInteger();
   AgentInstructions.Backend delayed=q->{if(q.optString("scope").equals("project")&&q.optString("operation").equals("read")){reads.incrementAndGet();entered.countDown();if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new java.io.IOException("TEST_TIMEOUT");}return backend(q);};
   if(session){WorkspaceStore.Session source=new WorkspaceStore.Session();source.environment=agent.equals("codex")?"codex":"ubuntu";source.claudeAgent=agent.equals("claude");source.codexEnvironment=agent.equals("claude")?"ubuntu":"ubuntu-ai";source.directory="/project";ui=AgentInstructionsUi.sessionEditor(activity,source,delayed);ui.show();}
   else{ui=new AgentInstructionsUi.Editor(activity,"/project",delayed);ui.agent=agent;ui.name=ui.baseName();ui.show();waitIdle();find("instructions-tab-project").performClick();}
   assertTrue(entered.await(2,java.util.concurrent.TimeUnit.SECONDS));assertTrue(ui.busy);find("instructions-project-create").performClick();find("instructions-project-create").performClick();assertTrue(ui.status.length()>0);release.countDown();waitIdle();
   AlertDialog form=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();EditText field=form.getWindow().getDecorView().findViewWithTag("instructions-create-field-0");assertNotNull("An early tap must open the form after the read",field);assertTrue(form.isShowing());assertEquals(1,reads.get());field.setText("keep entered purpose");
   ui.createProjectInstructions();waitIdle();assertSame("Repeated open must reuse the same form",form,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());assertEquals("keep entered purpose",field.getText().toString());assertEquals(1,reads.get());assertEquals("/project/"+ui.baseName(),ui.document.optString("path"));assertEquals(session&&agent.equals("claude")?"ubuntu":"ubuntu-ai",last.optString("environment"));assertEquals(0,writes);form.dismiss();
  }
 }
 @Test public void projectCreationReadErrorCanRetryAndMissingFileDraftNeedsConfirmation()throws Exception{
  projectExists=false;
projectText="";
java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
java.util.concurrent.atomic.AtomicInteger reads=new java.util.concurrent.atomic.AtomicInteger();

  ui=new AgentInstructionsUi.Editor(activity,"/project",q->{if(q.optString("operation").equals("read")&&reads.incrementAndGet()==1){entered.countDown();
release.await(5,java.util.concurrent.TimeUnit.SECONDS);
throw new java.io.IOException("PROJECT_CHECK_FAILED");
}return backend(q);
});
ui.scope="project";
ui.show();
assertTrue(entered.await(2,java.util.concurrent.TimeUnit.SECONDS));
ui.createProjectInstructions();
release.countDown();
waitIdle();
assertTrue(ui.status.getText().toString().contains("PROJECT_CHECK_FAILED"));
ui.createProjectInstructions();
waitIdle();

  AlertDialog form=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
assertNotNull("Retry must open a form",form.getWindow().getDecorView().findViewWithTag("instructions-create-field-0"));
form.dismiss();
ui.editor.setText("unsaved project draft");
ui.createProjectInstructions();
ui.createProjectInstructions();
AlertDialog guard=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
guard.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
Shadows.shadowOf(Looper.getMainLooper()).idle();
assertEquals("unsaved project draft",ui.editor.getText().toString());
ui.createProjectInstructions();
guard=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
guard.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
Shadows.shadowOf(Looper.getMainLooper()).idle();
assertNotNull("Confirmed draft guard must open the form",org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView().findViewWithTag("instructions-create-field-0"));
assertEquals("unsaved project draft",ui.editor.getText().toString());
assertEquals(0,writes);

 }
 @Test public void projectCreationForBothAgentsPreviewsOnlyKnownFieldsAndUsesCas()throws Exception{
  for(String lang:new String[]{"ru","en"})for(String agent:new String[]{"codex","claude"}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,lang);show();ui.switchAgent(agent);waitIdle();projectExists=false;projectText="";find("instructions-tab-project").performClick();waitIdle();assertEquals(View.VISIBLE,find("instructions-project-create").getVisibility());int before=writes;ui.createProjectInstructions();waitIdle();AlertDialog form=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();EditText purpose=form.findViewById(android.R.id.content).findViewWithTag("instructions-create-field-0");assertNotNull(purpose);purpose.setText("Confirmed purpose");assertEquals(before,writes);
   AgentManagerLayout box=(AgentManagerLayout)purpose.getParent().getParent().getParent();TextView review=(TextView)box.footer.getChildAt(0);review.performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();String text=((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString();assertTrue(text.contains("Confirmed purpose"));assertFalse(text.contains("LEARNING.md"));String expected="/project/"+(agent.equals("codex")?"AGENTS.md":"CLAUDE.md");assertTrue(allText(preview.getWindow().getDecorView()).contains(expected));assertEquals(before,writes);
   conflict=true;preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertTrue(form.isShowing());assertTrue(((TextView)form.findViewById(android.R.id.content).findViewWithTag("instructions-create-status")).length()>0);assertEquals("Confirmed purpose",purpose.getText().toString());assertEquals(expected,last.optString("path"));assertEquals("fixture-revision",last.optString("revision"));assertEquals("project",last.optString("scope"));conflict=false;review.performClick();preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(form.isShowing());assertEquals(text,lastText);assertTrue(ui.document.optBoolean("exists"));
  }
 }
 @Test public void existingProjectInstructionsKeepDraftAndNeverOpenCreationForm()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();ui.editor.setText("retained project draft");JSONObject before=last;ui.createProjectInstructions();assertSame(before,last);assertEquals("retained project draft",ui.editor.getText().toString());assertEquals(0,writes);
 }
 @Test public void projectCreationCanSaveEmptyOptionalSectionsAndCancelProtectsForm()throws Exception{
  show();projectExists=false;projectText="";find("instructions-tab-project").performClick();waitIdle();ui.createProjectInstructions();waitIdle();AlertDialog form=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();EditText input=form.findViewById(android.R.id.content).findViewWithTag("instructions-create-field-0");AgentManagerLayout box=(AgentManagerLayout)input.getParent().getParent().getParent();((TextView)box.footer.getChildAt(0)).performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals("# Инструкции проекта\n",((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString());preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();input.setText("keep");form.cancel();AlertDialog guard=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();guard.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertTrue(form.isShowing());assertEquals("keep",input.getText().toString());assertEquals(0,writes);form.dismiss();
 }

 @Test public void titleRefreshRetainsSelectedFileAndProtectsDraftInBothScopes()throws Exception{
  for(String lang:new String[]{"ru","en"})for(String agent:new String[]{"codex","claude"}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,lang);show();ui.switchAgent(agent);waitIdle();assertEquals(lang.equals("ru")?"Обновить":"Refresh",ui.refresh.getContentDescription());assertEquals(ui.refresh.getContentDescription(),ui.refresh.getTooltipText());assertNotNull(ui.refresh.getDrawable());assertEquals(View.VISIBLE,ui.refresh.getVisibility());
   sourceText="updated general";ui.refresh.performClick();waitIdle();assertEquals(sourceText,ui.editor.getText().toString());
   find("instructions-tab-project").performClick();waitIdle();String selected=agent.equals("codex")?"AGENTS.override.md":"CLAUDE.local.md";find("instructions-file-"+selected).performClick();waitIdle();String path=ui.document.optString("path");ui.editor.setText("keep this draft");JSONObject prior=last;ui.refresh.performClick();AlertDialog prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertSame(prior,last);assertEquals("keep this draft",ui.editor.getText().toString());projectText="disk changed";ui.refresh.performClick();prompt=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("disk changed",ui.editor.getText().toString());assertEquals(path,ui.document.optString("path"));assertEquals(selected,last.optString("name"));assertEquals("project",last.optString("scope"));assertEquals(0,writes);
   find("instructions-tab-diagnostics").performClick();waitIdle();assertEquals(View.GONE,ui.refresh.getVisibility());
  }
 }
 @Test public void sessionEntryRetainsOriginWhileAllowingGeneralSaveAndCopy()throws Exception{
  for(String agent:new String[]{"codex","claude"})for(String env:new String[]{"ubuntu-ai","ubuntu"}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceStore.Session origin=new WorkspaceStore.Session();origin.environment=agent.equals("codex")?"codex":"ubuntu";origin.claudeAgent=agent.equals("claude");origin.codexEnvironment=env;origin.directory="/project/кириллица with spaces";
   ui=AgentInstructionsUi.sessionEditor(activity,origin,this::backend);assertNotNull(ui);origin.directory="/other";origin.codexEnvironment="ubuntu-ai";ui.show();waitIdle();assertEquals("project",ui.scope);assertEquals(agent,ui.agent);assertEquals(env,last.optString("environment"));assertEquals("/project/кириллица with spaces",last.optString("project"));assertEquals(ui.project+"/"+ui.baseName(),ui.document.optString("path"));
   ui.editor.setText("copy and save");ui.editor.selectAll();assertTrue(ui.editor.onTextContextMenuItem(android.R.id.copy));android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);assertEquals("copy and save",clipboard.getPrimaryClip().getItemAt(0).getText().toString());ui.save.performClick();waitIdle();assertEquals("copy and save",lastText);assertEquals(env,last.optString("environment"));
   find("instructions-tab-general").performClick();waitIdle();assertEquals("general",last.optString("scope"));assertEquals(env,last.optString("environment"));
  }
 }
 @Test public void sessionEntryMissingFileOffersCreationAndLegacyTermuxIsNotRedirected()throws Exception{
  show();ui.dialog.dismiss();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexEnvironment="termux";p.directory="/legacy/project";projectExists=false;projectText="";ui=AgentInstructionsUi.sessionEditor(activity,p,this::backend);ui.show();waitIdle();assertEquals("termux",last.optString("environment"));assertNotNull(find("instructions-project-missing"));assertEquals(View.VISIBLE,find("instructions-project-create").getVisibility());assertEquals(0,writes);
 }

 AlertDialog latest(){return org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();}
 void openProjectTemplate(String agent,boolean exists)throws Exception{show();if(agent.equals("claude")){ui.switchAgent(agent);waitIdle();}projectExists=exists;projectText=exists?"ORIGINAL PROJECT":"";find("instructions-tab-project").performClick();waitIdle();find("instructions-project-template").performClick();waitIdle();}
 void editProjectTemplate(boolean exists)throws Exception{if(exists){latest().getButton(AlertDialog.BUTTON_NEUTRAL).performClick();waitIdle();}latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertTrue(ui.projectTemplateEditing);}
 @Test public void projectTemplateIsBeforeOpenForBothAgentsAndNeverShownInGeneral()throws Exception{
  for(String language:new String[]{"ru","en"})for(String agent:new String[]{"codex","claude"}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,language);show();if(agent.equals("claude")){ui.switchAgent(agent);waitIdle();}assertEquals(View.GONE,find("instructions-project-template").getVisibility());find("instructions-tab-project").performClick();waitIdle();assertEquals(View.VISIBLE,ui.projectTemplate.getVisibility());ViewGroup group=(ViewGroup)ui.projectTemplate.getParent();assertEquals(group.indexOfChild(ui.projectTemplate)+1,group.indexOfChild(ui.projectCreate));assertEquals(language.equals("ru")?"Шаблон проектных инструкций":"Project instructions template",ui.projectTemplate.getText().toString());assertEquals(0,writes);
  }
 }
 @Test public void projectTemplatePreviewAndCancelDoNotWriteAndKeepExactAsset()throws Exception{
  for(String agent:new String[]{"codex","claude"}){if(ui!=null)ui.dialog.dismiss();openProjectTemplate(agent,false);AlertDialog preview=latest();assertEquals(AgentInstructions.projectTemplate(activity),((TextView)preview.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString());assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(projectExists);assertEquals("",ui.editor.getText().toString());}
 }
 @Test public void projectTemplateCreatesOnlyAfterEditedReviewAndUsesChosenAgentPath()throws Exception{
  for(String agent:new String[]{"codex","claude"}){if(ui!=null)ui.dialog.dismiss();int before=writes;openProjectTemplate(agent,false);editProjectTemplate(false);String value=ui.editor.getText().toString().replace("[название]","Synthetic project");ui.editor.setText(value);assertEquals(before,writes);ui.save.performClick();AlertDialog review=latest();assertEquals(value,((TextView)review.getWindow().getDecorView().findViewWithTag("instructions-preview-text")).getText().toString());assertEquals(before,writes);review.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(before+1,writes);assertEquals(value,lastText);assertEquals(agent,last.getString("agent"));assertEquals("/project/"+(agent.equals("codex")?"AGENTS.md":"CLAUDE.md"),last.getString("path"));assertEquals("fixture-revision",last.getString("revision"));assertFalse(ui.projectTemplateEditing);}
 }
 @Test public void projectTemplateExistingOpenAndDeclinedReplacementKeepOriginal()throws Exception{
  for(String agent:new String[]{"codex","claude"}){if(ui!=null)ui.dialog.dismiss();int before=writes;openProjectTemplate(agent,true);latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("ORIGINAL PROJECT",ui.editor.getText().toString());assertEquals(before,writes);ui.viewProjectTemplate();waitIdle();editProjectTemplate(true);ui.editor.append("\nCUSTOM DRAFT");ui.save.performClick();AlertDialog review=latest();assertEquals((WorkspaceLanguage.code(activity).equals("ru")?"Заменить ":"Replace ")+ui.baseName(),review.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());review.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(before,writes);assertEquals("ORIGINAL PROJECT",projectText);assertTrue(ui.editor.getText().toString().endsWith("CUSTOM DRAFT"));ui.save.performClick();latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(before+1,writes);assertTrue(projectText.endsWith("CUSTOM DRAFT"));}
 }
 @Test public void projectTemplateExternalConflictPreservesOtherRevisionAndEditedDraft()throws Exception{
  for(String agent:new String[]{"codex","claude"}){if(ui!=null)ui.dialog.dismiss();openProjectTemplate(agent,true);editProjectTemplate(true);ui.editor.append("\nKEEP ON CONFLICT");String draft=ui.editor.getText().toString();projectText="OTHER PROCESS REVISION";conflict=true;ui.save.performClick();latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("OTHER PROCESS REVISION",projectText);assertEquals(draft,ui.editor.getText().toString());assertTrue(ui.projectTemplateEditing);conflict=false;}
 }
 @Test public void projectTemplateRequiresExplicitProjectAndPreservesExistingDraft()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();ui.projectExplicit=false;ui.viewProjectTemplate();AlertDialog chooser=latest();assertNotNull(chooser.getWindow().getDecorView().findViewWithTag("instructions-project-input"));assertEquals("",((EditText)chooser.getWindow().getDecorView().findViewWithTag("instructions-project-input")).getText().toString());chooser.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);ui.projectExplicit=true;ui.editor.setText("EXISTING UNSAVED DRAFT");ui.viewProjectTemplate();waitIdle();latest().getButton(AlertDialog.BUTTON_NEUTRAL).performClick();waitIdle();latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();latest().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();waitIdle();assertEquals("EXISTING UNSAVED DRAFT",ui.editor.getText().toString());assertEquals(0,writes);
 }
 @Test public void projectTemplateWriteFailureKeepsDraftAndRetryRequiresConfirmation()throws Exception{
  for(String agent:new String[]{"codex","claude"}){if(ui!=null)ui.dialog.dismiss();ui=new AgentInstructionsUi.Editor(activity,"/project",q->{if(q.optString("operation").equals("save"))throw new java.io.IOException("WRITE_FAILED");return backend(q);});ui.show();waitIdle();if(agent.equals("claude")){ui.switchAgent(agent);waitIdle();}projectExists=false;projectText="";find("instructions-tab-project").performClick();waitIdle();ui.viewProjectTemplate();waitIdle();editProjectTemplate(false);ui.editor.append("\nRETAIN AFTER FAILURE");String draft=ui.editor.getText().toString();ui.save.performClick();latest().getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(draft,ui.editor.getText().toString());assertTrue(ui.projectTemplateEditing);assertFalse(projectExists);assertTrue(ui.status.getText().toString().contains("WRITE_FAILED"));ui.save.performClick();assertTrue(latest().isShowing());assertEquals(0,writes);latest().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();}
 }

 @Test public void projectTemplateSharedSaveEntryCannotBypassReplacementReview()throws Exception{
  openProjectTemplate("claude",true);editProjectTemplate(true);ui.editor.append("\nCENTRAL SAVE");ui.save(ui.editor.getText().toString());assertEquals(0,writes);assertEquals("ORIGINAL PROJECT",projectText);assertEquals("Заменить CLAUDE.md",latest().getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());latest().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);
 }

}
