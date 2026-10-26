package com.termux.app.workspace;
import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.TextView;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentChatViewTest {
 private JSONObject row(String key,String kind,String body)throws Exception{return new JSONObject().put("key",key).put("kind",kind).put("body",body).put("message","msg_one").put("part",key);}
 @Test public void streamedTextUpdatesOnlyItsViewAndChipsOpenTheirOwnData()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView view=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});a.setContentView(view);
  view.assistantName("OpenCode");JSONObject thought=row("thought","reasoning","private reasoning"),answer=row("answer","text","first");view.rows(Arrays.asList(thought,answer),false);
  View chip=view.timeline.getChildAt(1),text=view.timeline.getChildAt(2);assertTrue(((TextView)chip).getText().toString().contains("Thinking")||((TextView)chip).getText().toString().contains("Рассуждение"));assertFalse(((TextView)chip).getText().toString().contains("private reasoning"));chip.performClick();assertEquals("private reasoning",opened.get().getString("body"));
  opened.set(null);text.performClick();assertNull(opened.get());
  answer.put("body","second");view.rows(Arrays.asList(thought,answer),false);assertSame(chip,view.timeline.getChildAt(1));assertSame(text,view.timeline.getChildAt(2));assertTrue(((TextView)text).getText().toString().equals("OpenCode: second"));assertSame(view.requests,view.timeline.getChildAt(3));
 }
 @Test public void receiptInsertionKeepsToolAndFollowingAnswerInOrder()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(view);
  JSONObject tool=row("tool","tool","result"),answer=row("answer","text","answer"),receipt=row("receipt","receipt","details").put("title","Allowed once");view.rows(Arrays.asList(tool,answer),false);View toolView=view.timeline.getChildAt(1),answerView=view.timeline.getChildAt(2);
  for(int i=0;i<3;i++){view.rows(Arrays.asList(receipt,tool,answer),false);assertSame(toolView,view.timeline.getChildAt(2));assertSame(answerView,view.timeline.getChildAt(3));assertEquals(5,view.timeline.getChildCount());}
 }
 @Test public void sendingFollowsNewRowsButReadingHistoryDoesNot()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(v);
  java.util.List<JSONObject> rows=new java.util.ArrayList<>();for(int i=0;i<30;i++)rows.add(row("r"+i,"text","line "+i+"\nmore text"));v.rows(rows,false);layout(v);v.scroll.scrollTo(0,0);
  rows.get(29).put("body","changed");v.rows(rows,false);layout(v);assertEquals(0,v.scroll.getScrollY());
  v.followAfterSend();rows.add(row("sent","user","new message"));v.rows(rows,false);layout(v);assertTrue(v.scroll.getScrollY()>0);assertEquals(v.timeline.getHeight()-v.scroll.getHeight(),v.scroll.getScrollY());assertEquals(View.GONE,v.status.getVisibility());assertSame(v.scroll,v.getChildAt(1));
 }
 private void layout(AgentChatView v){for(int i=0;i<3;i++){v.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));v.layout(0,0,600,700);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}}

 @Test public void providerErrorShowsGuidanceAndRetainsOriginalDetailsForBothAgents()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();
  for(String name:new String[]{"Gemini","OpenCode"}){AgentChatView v=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});v.assistantName(name);String raw="[API Error: {\"error\":{\"code\":400,\"message\":\"User location is not supported for the API use.\"}}]";JSONObject error=row("error","error",raw);v.rows(Arrays.asList(error),false);TextView text=(TextView)v.timeline.getChildAt(1);String shown=text.getText().toString();assertTrue(shown.contains(name));assertTrue(shown.contains("регион")||shown.contains("region"));assertFalse(shown.contains("[API Error:"));text.performClick();assertEquals(raw,opened.get().getString("body"));assertEquals("error",opened.get().getString("kind"));}
 }
 @Test public void obsoleteNativeKeyboardHintIsExplainedInChat()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});v.rows(Arrays.asList(row("hint","info","This request failed. Press F12 for diagnostics, or run /settings and change \"Error Verbosity\" to full for full details.")),false);String shown=((TextView)v.timeline.getChildAt(1)).getText().toString();assertFalse(shown.contains("F12"));assertTrue(shown.contains("подробности")||shown.contains("details"));
 }

 @Test public void cachedPositionSurvivesClearRepopulateAndLaterTextUpdates()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(v);
  List<JSONObject> rows=new ArrayList<>();for(int i=0;i<40;i++)rows.add(row("r"+i,"text","line "+i+"\nextra line\nmore"));v.rows(rows,false);layout(v);v.scroll.scrollTo(0,600);AgentChatView.Position p=v.position();int y=v.scroll.getScrollY();assertTrue(y>0);assertFalse(p.bottom);
  v.clear();v.rows(rows,false);v.restorePosition(p);layout(v);v.getViewTreeObserver().dispatchOnPreDraw();assertEquals(y,v.scroll.getScrollY());rows.get(39).put("body","streamed at end");v.rows(rows,false);layout(v);assertEquals(y,v.scroll.getScrollY());
 }
 @Test public void cachedBottomFollowsCurrentLayoutWithoutOldQueuedScrolls()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(v);List<JSONObject> rows=new ArrayList<>();for(int i=0;i<30;i++)rows.add(row("r"+i,"text","line "+i+"\nmore"));v.rows(rows,false);layout(v);v.scroll.fullScroll(View.FOCUS_DOWN);AgentChatView.Position p=v.position();assertTrue(p.bottom);v.clear();v.rows(rows,false);v.restorePosition(p);layout(v);v.getViewTreeObserver().dispatchOnPreDraw();assertEquals(v.timeline.getHeight()-v.scroll.getHeight(),v.scroll.getScrollY());
 }

 @Test public void olderPageStartsAtTopWhenPreviousContentFitsViewport()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(view);List<JSONObject> rows=new ArrayList<>();rows.add(row("current","text","newest"));view.rows(rows,true);layout(view);AgentChatView.Position held=view.position();assertTrue(held.bottom);
  List<JSONObject> expanded=new ArrayList<>();for(int i=0;i<35;i++)expanded.add(row("old"+i,"text","older\nmessage\ntext"));expanded.addAll(rows);view.olderRows(expanded,false);layout(view);view.getViewTreeObserver().dispatchOnPreDraw();assertEquals(0,view.scroll.getScrollY());assertFalse(view.position().bottom);assertEquals("old0",view.position().anchor);
 }
 @Test public void firstDrawAfterMessageGrowthAlreadyHasFinalScrollWithoutRunningPostedTasks()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(v);
  List<JSONObject> rows=new ArrayList<>();for(int i=0;i<30;i++)rows.add(row("r"+i,"text","line "+i+"\nmore"));v.rows(rows,false);layout(v);
  v.shellEnabled(true);v.shellInput.requestFocus();v.shellInput.setText("draft");
  for(String kind:new String[]{"user","text","tool"}){
   rows.add(row("new-"+kind,kind,"new content\nline2\nline3\nline4"));v.rows(rows,false);
   v.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));v.layout(0,0,600,700);
   v.scroll.getViewTreeObserver().dispatchOnPreDraw();
   assertEquals(kind,v.timeline.getHeight()-v.scroll.getHeight(),v.scroll.getScrollY());assertTrue(v.shellInput.hasFocus());assertEquals("draft",v.shellInput.getText().toString());
  }
 }
 @Test public void manualTouchCancelsPendingAutomaticScrollBeforeFirstDraw()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(v);List<JSONObject> rows=new ArrayList<>();for(int i=0;i<30;i++)rows.add(row("r"+i,"text","line "+i+"\nmore"));v.rows(rows,false);layout(v);
  rows.add(row("new","text","growing\nmessage"));v.rows(rows,false);
  android.view.MotionEvent e=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,50,100,0);v.scroll.dispatchTouchEvent(e);e.recycle();v.scroll.scrollTo(0,50);
  layout(v);v.scroll.getViewTreeObserver().dispatchOnPreDraw();assertEquals(50,v.scroll.getScrollY());
 }

 @Test public void attachmentCardsKeepIdentityFocusAndTheirOwnOpenTarget()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});a.setContentView(v);
  JSONObject attachment=row("file","attachment","report.log").put("attachment",new JSONObject().put("type","file").put("name","report.log").put("path","/tmp/report.log"));JSONObject answer=row("reply","text","first");v.rows(Arrays.asList(attachment,answer),false);layout(v);View card=v.timeline.getChildAt(1);card.performClick();assertSame(attachment,opened.get());
  v.shellEnabled(true);v.shellInput.setText("keep draft");v.shellInput.requestFocus();answer.put("body","streamed");v.rows(Arrays.asList(attachment,answer),false);layout(v);assertSame(card,v.timeline.getChildAt(1));assertTrue(v.shellInput.hasFocus());assertEquals("keep draft",v.shellInput.getText().toString());
  v.shellEnabled(false);v.attachmentPicker(true);assertEquals(View.VISIBLE,v.folder.getVisibility());assertTrue(v.folder.isEnabled());assertFalse(v.shellInput.isEnabled());v.attachmentPicker(false);assertEquals(View.GONE,v.folder.getVisibility());
 }

 @Test public void apkCardPlacesBadgeLeftAndMetadataRightWithoutReplacingComposer()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});a.setContentView(v);
  java.util.concurrent.atomic.AtomicInteger loads=new java.util.concurrent.atomic.AtomicInteger();v.apkDetails=(apk,label)->{loads.incrementAndGet();label.setText("44 MB\nSHA-256: abc123");};
  JSONObject apk=row("apk","attachment","build.apk").put("attachment",new JSONObject().put("type","apk").put("name","build.apk").put("bytes",44000000).put("path","/tmp/build.apk"));v.rows(Arrays.asList(apk),false);layout(v);
  android.widget.LinearLayout card=(android.widget.LinearLayout)v.timeline.getChildAt(1);assertEquals(android.widget.LinearLayout.HORIZONTAL,card.getOrientation());assertEquals(2,card.getChildCount());android.widget.LinearLayout badge=(android.widget.LinearLayout)card.getChildAt(0),info=(android.widget.LinearLayout)card.getChildAt(1);assertTrue(badge.getChildAt(0) instanceof android.widget.ImageView);assertEquals("APK",((TextView)badge.getChildAt(1)).getText().toString());assertEquals("build.apk",((TextView)info.getChildAt(0)).getText().toString());assertTrue(((TextView)info.getChildAt(1)).getText().toString().contains("SHA-256"));assertEquals(2,info.getChildCount());card.performClick();assertSame(apk,opened.get());
  v.rows(Arrays.asList(apk),false);assertEquals(1,loads.get());assertSame(card,v.timeline.getChildAt(1));
 }

 @Test public void genericFilesHaveCompactBadgesAndNoApkPreparation()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});a.setContentView(v);v.apkDetails=(meta,label)->fail("Generic files must not start APK preparation");
  for(String name:new String[]{"archive.zip","model.3mf","part.STL","setup.exe","build.bat","README"}){
   JSONObject r=row("file-"+name,"attachment",name).put("attachment",new JSONObject().put("type","binary").put("name",name).put("bytes",123).put("path","/tmp/"+name));v.rows(Arrays.asList(r),false);layout(v);
   android.widget.LinearLayout card=(android.widget.LinearLayout)v.timeline.getChildAt(1),badge=(android.widget.LinearLayout)card.getChildAt(0),info=(android.widget.LinearLayout)card.getChildAt(1);assertEquals(android.widget.LinearLayout.HORIZONTAL,card.getOrientation());assertEquals(ChatAttachments.badge(name),((TextView)badge.getChildAt(1)).getText().toString());assertEquals(name,((TextView)info.getChildAt(0)).getText().toString());assertFalse(((TextView)info.getChildAt(1)).getText().toString().contains("SHA-256"));card.performClick();assertSame(r,opened.get());
  }
 }


 @Test public void markdownLinksStayClickableAndRichStreamingKeepsComposerFocus()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});a.setContentView(v);
  JSONObject answer=row("answer","text","Read [docs](https://example.org/docs)");v.rows(Arrays.asList(answer),false);TextView plain=(TextView)v.timeline.getChildAt(1);
  assertNotNull(plain.getMovementMethod());assertTrue(plain.getText() instanceof android.text.Spanned);android.text.Spanned rendered=(android.text.Spanned)plain.getText();android.text.style.ClickableSpan[] links=rendered.getSpans(0,rendered.length(),android.text.style.ClickableSpan.class);assertEquals(1,links.length);links[0].onClick(plain);assertEquals("https://example.org/docs",Shadows.shadowOf(a).getNextStartedActivity().getDataString());
  answer.put("body","## Ready\n\n```python\nprint(1)");v.rows(Arrays.asList(answer),false);View rich=v.timeline.getChildAt(1);assertTrue(rich instanceof AgentMarkdownView);View heading=((android.view.ViewGroup)rich).getChildAt(1);v.shellEnabled(true);v.shellInput.setText("new draft");v.shellInput.requestFocus();answer.put("body","## Ready\n\n```python\nprint(12)\n```");v.rows(Arrays.asList(answer),false);layout(v);assertSame(rich,v.timeline.getChildAt(1));assertSame(heading,((android.view.ViewGroup)rich).getChildAt(1));assertTrue(v.shellInput.hasFocus());assertEquals("new draft",v.shellInput.getText().toString());
 }
}
