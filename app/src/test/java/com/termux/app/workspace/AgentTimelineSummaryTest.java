package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.atomic.AtomicReference;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentTimelineSummaryTest {
 Activity activity;AgentChatView view;AtomicReference<JSONObject> opened=new AtomicReference<>();
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(activity,"ru");view=new AgentChatView(activity,opened::set,()->{},()->{},()->{},()->{});view.actionCards(true);activity.setContentView(view);}
 @After public void close(){activity.finish();}
 List<JSONObject> source(String agent)throws Exception{try(InputStream in=getClass().getResourceAsStream("/timeline-adapter-events.json")){assertNotNull(in);ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))>=0)bytes.write(b,0,n);JSONArray a=new JSONObject(bytes.toString("UTF-8")).getJSONArray(agent);List<JSONObject> out=new ArrayList<>();for(int i=0;i<a.length();i++)out.add(a.getJSONObject(i));return out;}}

 AgentCompactActionView card(int index){return(AgentCompactActionView)view.timeline.getChildAt(index+1);}
 void layout(){for(int i=0;i<3;i++){view.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY));view.layout(0,0,320,500);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}}
 void adapter(String agent)throws Exception{for(String lang:new String[]{"ru","en"}){
  WorkspaceLanguage.select(activity,lang);view.clear();List<JSONObject> rows=source(agent);String nativeJson=rows.toString();view.rows(rows,true);
  List<JSONObject> visible=AgentTimelineRows.project(rows);assertEquals(8,visible.size());
  boolean declined=false,bad=false,retry=false,unknown=false;
  for(int i=0;i<visible.size();i++){
   AgentCompactActionView compact=card(i);CodexActionsView.ActionRow item=compact.item;String text=item.text().toString();assertFalse(text,text.contains("ONLY_TEST_TIMELINE"));assertEquals(View.GONE,item.command.getVisibility());assertEquals(View.GONE,item.meta.getVisibility());compact.performClick();JSONObject row=opened.get();assertNotNull(row);assertFalse(row.optString("message").isEmpty());assertFalse(row.optString("part").isEmpty());
   if(AgentActionPresentation.userDeclined(row)){declined=true;assertEquals(0xfff0f0ee,item.title.getCurrentTextColor());}
   if(row.optString("command").contains("fixture-bad")){bad=true;assertEquals(0xffffb08f,item.title.getCurrentTextColor());}
   if(row.optString("command").contains("fixture-retry")){retry=true;assertFalse(CodexActionTimeline.failed(row));}
   if(row.optString("toolName").equals("unrelated.Read")){unknown=true;assertTrue(text.contains("unrelated.Read"));}
  }
  assertTrue(declined&&bad&&retry&&unknown);assertEquals(nativeJson,rows.toString());View first=card(0);view.rows(rows,true);assertSame(first,card(0));assertEquals(View.VISIBLE,view.older.getVisibility());
 }}
 @Test public void codexActualProjectionAndColdReopen()throws Exception{adapter("codex");}
 @Test public void claudeActualProjectionCountsCallAndResultOnce()throws Exception{adapter("claude");}
 @Test public void geminiActualProjectionAndColdReopen()throws Exception{adapter("gemini");}
 @Test public void openCodeActualProjectionAndColdReopen()throws Exception{adapter("opencode");}
 JSONObject row(String key,String status)throws Exception{return new JSONObject().put("key",key).put("kind","tool").put("actionType","shell").put("command","fixture "+key).put("status",status).put("body","original output");}
 @Test public void longCommandsAreBoundedButDetailsRetainOriginal()throws Exception{
  String command="git diff --stat\n"+String.join("",Collections.nCopies(80,"long argument "));JSONObject row=row("long","running").put("command",command).put("body",command+"\noriginal output");view.rows(Collections.singletonList(row),false);AgentCompactActionView compact=card(0);assertTrue(compact.item.title.getText().length()<400);compact.performClick();assertSame(row,opened.get());assertEquals(command,opened.get().getString("command"));row.put("status","completed").put("exitCode",0);view.rows(Collections.singletonList(row),false);assertSame(compact,card(0));assertTrue(compact.item.title.getText().toString().startsWith("✓"));
 }
 @Test public void imageExpansionUsesTheBoundedChatCacheAndNeverAnotherChat()throws Exception{
  JSONObject image=row("image","completed").put("actionPreview",new JSONObject().put("kind","image").put("image",new JSONObject().put("path","/project/image.png").put("type","image")));
  view.rows(Collections.singletonList(image),false);AgentImageActionView compact=(AgentImageActionView)view.timeline.getChildAt(1);compact.heading.performClick();assertEquals(View.VISIBLE,compact.image.getVisibility());AgentChatCache.State saved=new AgentChatCache.State();saved.rows.put("image",image);saved.timelineExpansion.putAll(view.timelineExpansion);AgentChatCache cache=new AgentChatCache();cache.put("chat-A",saved);view.clear();view.rows(Collections.singletonList(image),false);assertEquals(View.GONE,((AgentImageActionView)view.timeline.getChildAt(1)).image.getVisibility());assertNull(cache.take("chat-B"));saved=cache.take("chat-A");view.clear();view.timelineExpansion.putAll(saved.timelineExpansion);view.rows(new ArrayList<>(saved.rows.values()),false);assertEquals(View.VISIBLE,((AgentImageActionView)view.timeline.getChildAt(1)).image.getVisibility());
  for(int i=0;i<1500;i++)view.rememberActions(Collections.singletonList(row("k"+i,"completed")),false);assertEquals(1024,view.timelineExpansion.size());
 }
 @Test public void runningUpdatesAndOlderPagesRetainTheReadingAnchor()throws Exception{
  List<JSONObject> rows=new ArrayList<>();for(int i=0;i<24;i++)rows.add(row("r"+i,i==0?"running":"completed"));view.rows(rows,true);layout();view.scroll.scrollTo(0,500);AgentChatView.Position before=view.position();rows.get(0).put("status","awaiting_approval");view.rows(rows,true);layout();assertEquals(before.anchor,view.position().anchor);assertEquals(before.offset,view.position().offset);List<JSONObject> older=new ArrayList<>();older.add(row("prepend","completed"));older.addAll(rows);view.olderRows(older,false);layout();assertEquals(before.anchor,view.position().anchor);assertEquals(before.offset,view.position().offset);
 }
}
