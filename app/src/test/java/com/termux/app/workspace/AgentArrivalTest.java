package com.termux.app.workspace;
import android.app.*;import android.os.Looper;import android.view.*;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.util.ReflectionHelpers;import java.util.*;import java.util.concurrent.TimeUnit;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class) @LooperMode(LooperMode.Mode.PAUSED)
public class AgentArrivalTest {
 Activity a;AgentChatView view;
 void create(String agent){a=Robolectric.buildActivity(Activity.class).setup().visible().get();view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});view.assistantName(agent);view.actionCards(true);a.setContentView(view);Object info=ReflectionHelpers.getField(view,"mAttachInfo");ReflectionHelpers.setField(info,"mWindowVisibility",View.VISIBLE);layout();}
 JSONObject row(String id,String kind,String body)throws Exception{return new JSONObject().put("key",id).put("message",id).put("kind",kind).put("body",body);}
 void layout(){view.measure(View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(650,View.MeasureSpec.EXACTLY));view.layout(0,0,440,650);view.getViewTreeObserver().dispatchOnPreDraw();}
 void tick(int ms){Shadows.shadowOf(Looper.getMainLooper()).idleFor(ms,TimeUnit.MILLISECONDS);}
 View item(String key){Map<String,View> views=ReflectionHelpers.getField(view,"views");return views.get(key);}
 @After public void end(){if(view!=null)view.clear();if(a!=null)a.finish();}
 @Test public void newUserAndTimelineLandOnceForAllFourAgentsWithoutChangingReadingAnchor()throws Exception{
  for(String agent:new String[]{"Codex","Claude","Gemini","OpenCode"}){
   create(agent);List<JSONObject> rows=new ArrayList<>();for(int i=0;i<25;i++)rows.add(row("old"+i,"user","old message\nsecond line\nthird line"));view.liveRows(rows,true,false);layout();tick(300);view.scroll.scrollTo(0,300);AgentChatView.Position before=view.position();
   rows.add(row("new","user","new message"));rows.add(row("action","tool","command output").put("title","Shell").put("tool","Bash"));view.liveRows(rows,true,true);
   View user=item("new"),group=item("codex-actions:action");assertNotNull(group);for(View v:new View[]{user,group}){assertEquals(0,v.getAlpha(),.001f);assertEquals(-view.dp(8),v.getTranslationY(),.001f);}
   layout();Map<View,android.animation.ValueAnimator> animations=ReflectionHelpers.getField(view,"landings");android.animation.ValueAnimator moving=animations.get(user);assertNotNull(moving);assertEquals(210,moving.getDuration());moving.setCurrentPlayTime(100);assertTrue("intermediate alpha="+user.getAlpha(),user.getAlpha()>0&&user.getAlpha()<1);assertTrue(user.getTranslationY()<0);assertEquals(before.anchor,view.position().anchor);assertEquals(before.offset,view.position().offset);
   rows.get(rows.size()-1).put("body","more output");float alpha=user.getAlpha();view.liveRows(rows,true,true);assertEquals(alpha,user.getAlpha(),.001f);for(android.animation.ValueAnimator animation:new ArrayList<>(animations.values()))animation.end();assertEquals(1,user.getAlpha(),.001f);assertEquals(0,group.getTranslationY(),.001f);
   view.liveRows(rows,true,false);assertSame(user,item("new"));assertEquals(1,user.getAlpha(),.001f);
   List<JSONObject> earlier=new ArrayList<>();earlier.add(row("earlier","user","old history"));earlier.addAll(rows);view.olderRows(earlier,true);assertEquals(1,item("earlier").getAlpha(),.001f);layout();assertEquals(before.anchor,view.position().anchor);
   view.clear();view.liveRows(earlier,true,false);assertEquals(1,item("new").getAlpha(),.001f);view.clear();a.finish();
  }
 }
 @Test public void regroupedExistingActionsDoNotLandAgain()throws Exception{
  create("Gemini");view.actionCards(false);JSONObject step=row("step","tool","read");JSONObject group=row("first-group","actionGroup","").put("steps",new JSONArray().put(step));view.liveRows(Collections.emptyList(),false,false);view.liveRows(Collections.singletonList(group),false,true);Map<View,android.animation.ValueAnimator> animations=ReflectionHelpers.getField(view,"landings");assertEquals(1,animations.size());for(android.animation.ValueAnimator animation:new ArrayList<>(animations.values()))animation.end();group.put("key","rebuilt-group");view.liveRows(Collections.singletonList(group),false,false);assertEquals(0,animations.size());assertEquals(1,item("rebuilt-group").getAlpha(),.001f);
 }
 @Test public void userAttachmentLandsWithoutOpeningOrLoadingOriginal()throws Exception{
  create("OpenCode");view.thumbnail=(file,image)->{};view.liveRows(Collections.emptyList(),false,false);
  JSONObject file=row("image","attachment","").put("messageSender","user").put("attachment",new JSONObject().put("name","fixture.gif").put("type","image"));view.liveRows(Collections.singletonList(file),false,false);assertEquals(0,item("image").getAlpha(),.001f);view.clear();view.liveRows(Collections.singletonList(file),false,false);assertEquals(1,item("image").getAlpha(),.001f);
 }
 @Test public void hiddenDisabledAndReplacedRowsDoNotReplayAndIdenticalTextsRemainSeparate()throws Exception{
  create("Claude");List<JSONObject> rows=new ArrayList<>();view.liveRows(rows,false,false);rows.add(row("one","user","same"));view.liveRows(rows,false,true);assertEquals(0,item("one").getAlpha(),.001f);view.setVisibility(View.GONE);assertEquals(1,item("one").getAlpha(),.001f);
  rows.add(row("two","user","same"));view.liveRows(rows,false,true);assertEquals(1,item("two").getAlpha(),.001f);view.setVisibility(View.VISIBLE);view.liveRows(rows,false,true);assertNotSame(item("one"),item("two"));assertEquals(1,item("two").getAlpha(),.001f);
  java.lang.reflect.Method scale=android.animation.ValueAnimator.class.getDeclaredMethod("setDurationScale",float.class);scale.setAccessible(true);scale.invoke(null,0f);try{rows.add(row("three","user","same"));view.liveRows(rows,false,true);assertEquals(1,item("three").getAlpha(),.001f);assertEquals(0,item("three").getTranslationY(),.001f);}finally{scale.invoke(null,1f);}
 }
}
