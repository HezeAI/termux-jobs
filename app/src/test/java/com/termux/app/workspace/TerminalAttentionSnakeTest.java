package com.termux.app.workspace;
import android.animation.ValueAnimator;import android.os.Looper;import android.view.View;import java.lang.reflect.*;import java.util.concurrent.TimeUnit;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @LooperMode(LooperMode.Mode.LEGACY)
public class TerminalAttentionSnakeTest {
 // A visible host isolates the clock from Robolectric's synthetic window visibility.
 // Real window attachment, painting and unattended playback remain phone acceptance.
 static class Host extends View {boolean ready=true;int frames;Host(){super(RuntimeEnvironment.application);}@Override public boolean isShown(){return ready;}@Override public boolean isAttachedToWindow(){return ready;}@Override public void invalidate(){frames++;}}
 @Before public void frameClock()throws Exception{org.robolectric.shadows.ShadowChoreographer.setPostFrameCallbackDelay(16);org.robolectric.shadows.ShadowChoreographer.setPostCallbackDelay(16);scale(1);}
 @After public void resetClock()throws Exception{org.robolectric.shadows.ShadowChoreographer.setPostFrameCallbackDelay(0);org.robolectric.shadows.ShadowChoreographer.setPostCallbackDelay(0);scale(1);}
 void scale(float value)throws Exception{Method m=ValueAnimator.class.getDeclaredMethod("setDurationScale",float.class);m.setAccessible(true);m.invoke(null,value);}
 Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 @Test public void clockMovesSegmentAndInvalidatesHostWithoutTouchThenStopsAndResumes()throws Exception{
  Host host=new Host();WorkspaceTransferBorder border=new WorkspaceTransferBorder(1,true);border.follow(host);border.active(true);
  try{ValueAnimator clock=(ValueAnimator)get(border,"clock");assertNotNull(clock);assertEquals(ValueAnimator.INFINITE,((org.robolectric.shadows.ShadowValueAnimator)org.robolectric.shadow.api.Shadow.extract(clock)).getActualRepeatCount());
   int before=host.frames;float start=(float)get(border,"phase");Shadows.shadowOf(Looper.getMainLooper()).idleFor(450,TimeUnit.MILLISECONDS);assertTrue("Autonomous frames invalidate the actual host target",host.frames>before);assertTrue("Segment position advances without input",(float)get(border,"phase")>start+.05);
   // Robolectric caps INFINITE at one repeat. Seek its second traversal explicitly;
   // this does not claim a long unattended physical-device run.
   clock.setCurrentPlayTime(2500);assertEquals(.25f,(float)get(border,"phase"),.03);
   host.ready=false;border.active(true);assertNull(get(border,"clock"));host.ready=true;border.active(true);assertNotNull(get(border,"clock"));border.active(false);assertNull(get(border,"clock"));assertFalse(border.active());
  }finally{border.active(false);}
 }
 @Test public void reducedMotionLeavesStaticBorderWithoutAnAnimationClock()throws Exception{scale(0);Host host=new Host();WorkspaceTransferBorder border=new WorkspaceTransferBorder(1,true);border.follow(host);border.active(true);assertNull(get(border,"clock"));assertTrue(border.active());border.active(false);}
}
