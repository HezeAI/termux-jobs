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
  return new JSONObject().put("projectSource",projectSource==null?JSONObject.NULL:projectSource).put("storageAddition",storageOffer?"## Storage addition\nRules\n":"").put("storageAdditionAvailable",storageOffer).put("path",q.optString("scope").equals("project")?q.optString("project")+"/"+q.optString("name"):"/custom/codex/AGENTS.override.md").put("maskedBy",q.optString("scope").equals("project")&&q.optString("name").equals("AGENTS.md")&&"/project/AGENTS.override.md".equals(projectSource)?"/project/AGENTS.override.md":JSONObject.NULL).put("exists",!q.optString("scope").equals("project")||projectExists).put("text",q.optString("scope").equals("project")?projectText:sourceText).put("revision","fixture-revision").put("template","template text\n").put("templateAvailable",true);
 }
 void show()throws Exception{ui=new AgentInstructionsUi.Editor(activity,"/project",this::backend);ui.show();waitIdle();}
 void waitIdle()throws Exception{for(int n=0;n<150;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!ui.busy)return;Thread.sleep(10);}fail("UI operation did not finish");}
 View find(String tag){return ui.root.findViewWithTag(tag);}
 void measured(View view,int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}
 android.graphics.Rect bounds(View parent,View child){android.graphics.Rect r=new android.graphics.Rect();child.getDrawingRect(r);((ViewGroup)parent).offsetDescendantRectToMyCoords(child,r);return r;}
 @Test public void narrowLargeFontTabsDoNotWrapAndEditingControlsSurviveReducedImeViewport()throws Exception{
  for(String language:new String[]{"ru","en"})for(float font:new float[]{1f,1.3f,1.5f})for(int width:new int[]{280,320,360}){
   if(ui!=null)ui.dialog.dismiss();WorkspaceLanguage.select(activity,language);android.content.res.Resources res=activity.getResources();android.content.res.Configuration config=new android.content.res.Configuration(res.getConfiguration());config.fontScale=font;res.updateConfiguration(config,res.getDisplayMetrics());res.getDisplayMetrics().scaledDensity=res.getDisplayMetrics().density*font;
   show();int w=ui.dp(width),h=ui.dp(360);measured(ui.root,w,h);
   for(String tab:new String[]{"general","project","diagnostics"}){
    find("instructions-tab-"+tab).performClick();waitIdle();measured(ui.root,w,h);
    TextView active=(TextView)find("instructions-tab-"+tab);assertTrue(active.isSelected());assertEquals(1,active.getLineCount());assertTrue("unclipped label: "+active.getText(),active.getMeasuredWidth()>=Math.ceil(active.getPaint().measureText(active.getText().toString()))+active.getPaddingLeft()+active.getPaddingRight()-1);
    assertTrue(find("instructions-tabs-scroll") instanceof HorizontalScrollView);assertTrue(bounds(ui.root,ui.close).bottom<=h);assertTrue(ui.close.getHeight()>0);
    if(!tab.equals("diagnostics")){
     assertTrue(ui.editor.getHeight()>0);assertTrue(find("instructions-content-scroll") instanceof ScrollView);assertTrue(bounds(ui.root,ui.save).bottom<=bounds(ui.root,ui.close).top);assertTrue(ui.save.getHeight()>0);
     if(tab.equals("project")){assertTrue(find("instructions-files-scroll") instanceof HorizontalScrollView);assertEquals(1,((TextView)find("instructions-file-AGENTS.override.md")).getLineCount());find("instructions-file-AGENTS.override.md").performClick();waitIdle();measured(ui.root,w,h);HorizontalScrollView files=(HorizontalScrollView)find("instructions-files-scroll");View selectedFile=find("instructions-file-AGENTS.override.md");assertTrue(selectedFile.isSelected());assertTrue("selected file intersects viewport",selectedFile.getRight()>files.getScrollX()&&selectedFile.getLeft()<files.getScrollX()+files.getWidth());}
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
 @Test public void closeWithUnsavedDraftDoesNotSave()throws Exception{show();ui.editor.setText("draft");find("instructions-close").performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,writes);assertEquals("current\r\n",sourceText);assertTrue(ui.closed);}
 @Test public void templatePreviewCancelDoesNotChangeFileOrDraft()throws Exception{show();ui.editor.setText("draft");find("instructions-template-offer").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,writes);assertEquals("draft",ui.editor.getText().toString());}
 @Test public void templateIsVisibleBeforeExplicitApplyAndSave()throws Exception{show();find("instructions-template-offer").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(preview);assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(AgentInstructions.builtInTemplate(activity,"ru"),lastText);assertEquals(1,writes);assertTrue(ui.status.getText().toString().startsWith("Сохранено."));}
 @Test public void parallelConflictKeepsDraftAndDoesNotClaimSaved()throws Exception{show();conflict=true;ui.editor.setText("my draft");find("instructions-save").performClick();waitIdle();assertEquals("my draft",ui.editor.getText().toString());assertTrue(ui.status.getText().toString().contains("не перезаписана"));assertFalse(ui.status.getText().toString().startsWith("Сохранено"));assertEquals("current\r\n",sourceText);}
 @Test public void previousRevisionRequiresPreviewAndExplicitRestore()throws Exception{show();ui.history(true);waitIdle();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("older revision",lastText);}
 @Test public void projectFilesAndDiagnosticsAreSeparateTabsAndDetailsCollapse()throws Exception{show();find("instructions-tab-project").performClick();waitIdle();assertEquals("/project/AGENTS.md",ui.document.getString("path"));find("instructions-file-AGENTS.override.md").performClick();waitIdle();assertEquals("/project/AGENTS.override.md",ui.document.getString("path"));find("instructions-tab-diagnostics").performClick();waitIdle();View toggle=find("instructions-details-toggle");assertNotNull(toggle);assertEquals(0,writes);assertNull(ui.editor);}
 @Test public void profileOnlySuggestsProjectAndDoesNotChangeGeneralSource()throws Exception{WorkspaceStore.Session s=new WorkspaceStore.Session();s.codexEnvironment="ubuntu-ai";s.directory="~/work";s.agentConnectionCommand="/private/launcher";assertEquals("/root/work",AgentInstructionsUi.suggested(s));show();assertEquals("/custom/codex/AGENTS.override.md",ui.document.getString("path"));assertFalse(last.has("agentConnectionCommand"));}
 @Test public void englishLabelsAndFactualApplicationNotice()throws Exception{WorkspaceLanguage.select(activity,"en");show();assertEquals("General",((TextView)find("instructions-tab-general")).getText().toString());assertTrue(AgentInstructionsUi.saved(activity).contains("Shared rules may apply"));}
 @Test public void entryIsInGlobalSettingsWithoutCreatingAChat()throws Exception{
  activity.setContentView(R.layout.activity_termux);java.lang.reflect.Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController(activity);
  try{java.lang.reflect.Method open=WorkspaceController.class.getDeclaredMethod("globalSettings");open.setAccessible(true);open.invoke(controller);AlertDialog settings=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(settings.isShowing());assertTrue(hasText(settings.getWindow().getDecorView(),"Инструкции агентов"));assertNull(activity.getCurrentSession());settings.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();}finally{controller.close();}
 }
 boolean hasText(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return true;if(view instanceof ViewGroup)for(int n=0;n<((ViewGroup)view).getChildCount();n++)if(hasText(((ViewGroup)view).getChildAt(n),text))return true;return false;}

 @Test public void storageAdditionIsPreviewedAndAppendedWithoutReplacingCustomPrefix()throws Exception{
  storageOffer=true;show();String before=ui.editor.getText().toString();ui.storageAddition();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,writes);preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(before,ui.editor.getText().toString());assertEquals(0,writes);
  ui.storageAddition();preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals(before+"\n"+AgentInstructions.storageAddition(AgentInstructions.builtInTemplate(activity,"ru")),lastText);assertEquals(1,writes);ui.storageAddition();assertEquals(1,writes);
 }

 @Test public void projectExplanationAndActualSourceStayIndependentOfEditingInBothLanguages()throws Exception{
  show();find("instructions-tab-project").performClick();waitIdle();
  assertTrue(((TextView)find("instructions-project-explanation")).getText().toString().contains("Общие инструкции продолжают действовать"));
  assertEquals("Codex читает в этой папке: AGENTS.md",((TextView)find("instructions-project-active")).getText().toString());
  find("instructions-file-AGENTS.override.md").performClick();waitIdle();
  assertEquals("Codex читает в этой папке: AGENTS.md",((TextView)find("instructions-project-active")).getText().toString());
  assertTrue(((TextView)find("instructions-source")).getText().toString().startsWith("Редактирование:"));assertEquals(0,writes);
  projectSource="/project/AGENTS.override.md";ui.load();waitIdle();assertTrue(((TextView)find("instructions-project-active")).getText().toString().endsWith("AGENTS.override.md"));
  projectSource=null;ui.load();waitIdle();assertEquals("В этой папке проектные инструкции отсутствуют.",((TextView)find("instructions-project-active")).getText().toString());
  WorkspaceLanguage.select(activity,"en");projectSource="/project/AGENTS.md";ui.load();waitIdle();
  assertTrue(((TextView)find("instructions-project-explanation")).getText().toString().contains("Shared instructions continue to apply"));
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
  find("instructions-template-offer").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
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
  show();ui.editor.setText("my unsaved text");find("instructions-template-offer").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=preview.getWindow().getDecorView();
  assertEquals(AgentInstructions.builtInTemplate(activity,"ru"),((TextView)decor.findViewWithTag("instructions-preview-text")).getText().toString());assertTrue(decor.findViewWithTag("instructions-preview-ru").isSelected());
  assertTrue(((TextView)decor.findViewWithTag("instructions-preview-target")).getText().toString().contains("/custom/codex/AGENTS.override.md"));
  decor.findViewWithTag("instructions-preview-en").performClick();assertEquals(AgentInstructions.builtInTemplate(activity,"en"),((TextView)decor.findViewWithTag("instructions-preview-text")).getText().toString());assertTrue(((TextView)decor.findViewWithTag("instructions-preview-language")).getText().toString().endsWith("EN"));
  assertEquals("ru",WorkspaceLanguage.code(activity));assertEquals("my unsaved text",ui.editor.getText().toString());assertEquals("current\r\n",sourceText);assertEquals(0,writes);
  preview.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals("my unsaved text",ui.editor.getText().toString());assertEquals(0,writes);
 }
 @Test public void englishDefaultCanExplicitlyApplyRussianAssetWithoutChangingAppLanguage()throws Exception{
  WorkspaceLanguage.select(activity,"en");show();find("instructions-template-offer").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View decor=preview.getWindow().getDecorView();
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
  show();ui.editor.setText("my draft");find("instructions-template-offer").performClick();AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();preview.getWindow().getDecorView().findViewWithTag("instructions-preview-en").performClick();conflict=true;preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();waitIdle();assertEquals("my draft",ui.editor.getText().toString());assertEquals("current\r\n",sourceText);assertFalse(ui.status.getText().toString().startsWith("Сохранено"));
 }

 void reopenIn(String language)throws Exception{if(ui!=null){ui.dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();}WorkspaceLanguage.select(activity,language);WorkspaceLanguage.apply(activity);show();}
 String allText(View v){String text=v instanceof TextView?((TextView)v).getText().toString()+"\n":"";if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++)text+=allText(((ViewGroup)v).getChildAt(n));return text;}
 @Test public void instructionMenusHistoryAndDiscardConfirmationsAreLocalized()throws Exception{
  for(String language:new String[]{"ru","en"}){
   reopenIn(language);boolean ru=language.equals("ru");assertEquals(ru?"Отмена / Закрыть":"Cancel / Close",((TextView)find("instructions-close")).getText().toString());
   find("instructions-menu").performClick();AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(hasText(menu.getWindow().getDecorView(),ru?"Редакции и шаблон":"Revisions and template"));
   String[] expected=ru?new String[]{"История редакций","Восстановить предыдущую версию","Просмотреть встроенный шаблон","Дополнение: хранение файлов","Открыть файл заново","Импортировать инструкции"}:new String[]{"Revision history","Restore previous version","View built-in template","Addition: file storage","Reload file","Import instructions"};
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
 @Test public void storageAdditionOnlyAppearsInGeneralMenuAndImportEntriesAreLocalized()throws Exception{
  storageOffer=true;
  for(String language:new String[]{"ru","en"}){
   reopenIn(language);boolean ru=language.equals("ru");assertNull(find("instructions-storage-offer"));find("instructions-menu").performClick();AlertDialog menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(ru?"Импортировать инструкции":"Import instructions",menu.getListView().getItemAtPosition(5));menu.getListView().performItemClick(null,3,3);AlertDialog preview=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(preview.getWindow().getDecorView().findViewWithTag("instructions-preview-ru"));preview.dismiss();
   find("instructions-tab-project").performClick();waitIdle();find("instructions-menu").performClick();menu=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(ru?"Импортировать AGENTS.md":"Import AGENTS.md",menu.getListView().getItemAtPosition(3));menu.dismiss();assertEquals(0,writes);
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
}
