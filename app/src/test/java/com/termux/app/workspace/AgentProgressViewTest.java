package com.termux.app.workspace;
import android.app.Application;import android.view.View;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentProgressViewTest {
 @Test public void footerKeepsHeightAndUsesRequestStatesWithoutAddingHistory(){android.content.Context c=RuntimeEnvironment.application;AgentChatView chat=new AgentChatView(c,r->{},()->{},()->{},()->{},()->{});AgentProgressView p=chat.execution;int height=-1;for(AgentIndicatorState.State state:AgentIndicatorState.State.values()){p.present(state,true);p.frame(1230000000L);p.measure(View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.AT_MOST));if(height<0)height=p.getMeasuredHeight();assertEquals(height,p.getMeasuredHeight());assertEquals(state==AgentIndicatorState.State.IDLE||state==AgentIndicatorState.State.ERROR?View.INVISIBLE:View.VISIBLE,p.getVisibility());}p.present(AgentIndicatorState.State.WORKING,true);p.frame(0);assertFalse(p.getText().toString().isEmpty());p.present(AgentIndicatorState.State.WORKING,false);assertEquals(View.INVISIBLE,p.getVisibility());assertFalse(p.isTextSelectable());}

 @Test public void requestClockKeepsIndependentAnchorsAcrossHiddenTabsAndRequests()throws Exception{
  java.util.concurrent.atomic.AtomicLong now=new java.util.concurrent.atomic.AtomicLong(1000);AgentProgressView p=new AgentProgressView(RuntimeEnvironment.application,now::get);
  org.json.JSONObject first=new org.json.JSONObject().put("request","a").put("elapsedMs",37000);
  p.timing("session-a",first,true);p.present(AgentIndicatorState.State.WORKING,true);p.frame(1);assertEquals("00:37",p.getText().toString());
  now.addAndGet(1000);p.timing("session-a",first,true);assertEquals("00:38",p.timerText());p.present(AgentIndicatorState.State.WAITING,true);assertTrue(p.getText().toString().startsWith("00:38"));
  p.present(AgentIndicatorState.State.WAITING,false);String hidden=p.getText().toString();now.addAndGet(5000);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1,java.util.concurrent.TimeUnit.SECONDS);assertEquals(hidden,p.getText().toString());
  p.timing("session-b",new org.json.JSONObject().put("request","b").put("elapsedMs",1000),true);p.present(AgentIndicatorState.State.RECONNECTING,true);assertTrue(p.getText().toString().startsWith("00:01"));now.addAndGet(1000);
  p.timing("session-a",first,true);p.present(AgentIndicatorState.State.WORKING,true);assertEquals("00:44",p.getText().toString());
  p.timing("session-a",new org.json.JSONObject().put("request","next").put("elapsedMs",2000),true);assertEquals("00:02",p.timerText());p.timing("session-a",null,false);assertEquals("",p.timerText());p.present(AgentIndicatorState.State.IDLE,true);assertEquals(View.INVISIBLE,p.getVisibility());
 }
 @Test public void unknownStartIsHiddenAndHoursAreFormattedWithoutWallClock()throws Exception{
  AgentProgressView p=new AgentProgressView(RuntimeEnvironment.application,()->1000L);p.timing("unknown",null,true);p.present(AgentIndicatorState.State.WORKING,true);assertEquals("",p.timerText());p.timing("unknown",new org.json.JSONObject().put("request","bad").put("elapsedMs","1000"),true);assertEquals("",p.timerText());
  assertEquals("00:37",AgentProgressView.elapsed(37000));assertEquals("59:59",AgentProgressView.elapsed(3599000));assertEquals("1:02:15",AgentProgressView.elapsed(3735000));
 }
 @Test public void oneSecondTickerStopsWhenWindowOrParentIsHidden()throws Exception{
  android.app.Activity a=Robolectric.buildActivity(android.app.Activity.class).setup().visible().get();android.widget.FrameLayout host=new android.widget.FrameLayout(a);java.util.concurrent.atomic.AtomicLong now=new java.util.concurrent.atomic.AtomicLong(0);AgentProgressView p=new AgentProgressView(a,now::get);host.addView(p);a.setContentView(host);
  Object attach=org.robolectric.util.ReflectionHelpers.getField(p,"mAttachInfo");org.robolectric.util.ReflectionHelpers.setField(attach,"mWindowVisibility",View.VISIBLE);p.onWindowVisibilityChanged(View.VISIBLE);
  try{p.timing("a",new org.json.JSONObject().put("request","r").put("elapsedMs",37000),true);p.present(AgentIndicatorState.State.WORKING,true);assertEquals("00:37",p.getText().toString());now.set(1000);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(999,java.util.concurrent.TimeUnit.MILLISECONDS);assertEquals("00:37",p.getText().toString());Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1,java.util.concurrent.TimeUnit.MILLISECONDS);assertEquals("00:38",p.getText().toString());host.setVisibility(View.GONE);now.set(5000);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals("00:38",p.getText().toString());host.setVisibility(View.VISIBLE);assertEquals("00:42",p.getText().toString());
   org.robolectric.util.ReflectionHelpers.setField(attach,"mWindowVisibility",View.GONE);p.onWindowVisibilityChanged(View.GONE);now.set(9000);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(3,java.util.concurrent.TimeUnit.SECONDS);assertEquals("00:42",p.getText().toString());
  }finally{p.timing("a",null,false);a.finish();}
 }
}
