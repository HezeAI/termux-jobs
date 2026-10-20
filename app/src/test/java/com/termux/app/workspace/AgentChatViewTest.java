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
}
