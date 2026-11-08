package com.termux.app.workspace;
import android.app.Application;
import android.animation.ValueAnimator;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentStopPulseTest {
 @Test public void compactOutlineBreathesWithoutRestartOrGeometryChanges()throws Exception{
  AgentStopButton button=new AgentStopButton(RuntimeEnvironment.application);button.compact(true);button.layout(0,0,44,44);button.indicator("Claude",AgentIndicatorState.State.WORKING);assertTrue(button.animated());
  button.frame(1000000000L,0);assertEquals(WorkspaceDialogs.LINE,button.compactBorderColor());button.frame(2250000000L,0);assertEquals(0xff9a9a9a,button.compactBorderColor());
  for(int n=0;n<4;n++)button.indicator("Claude",AgentIndicatorState.State.WORKING);assertEquals(0xff9a9a9a,button.compactBorderColor());button.frame(3500000000L,0);assertEquals(WorkspaceDialogs.LINE,button.compactBorderColor());
  assertEquals(44,button.getWidth());assertEquals(44,button.getHeight());assertEquals(1,button.getScaleX(),0);assertEquals(1,button.getScaleY(),0);assertEquals(0,button.getTranslationX(),0);assertEquals(0,button.getTranslationY(),0);
  button.setVisibility(View.INVISIBLE);button.frame(4750000000L,0);assertEquals(WorkspaceDialogs.LINE,button.compactBorderColor());
  button.indicator("Claude",AgentIndicatorState.State.IDLE);assertFalse(button.animated());assertEquals(WorkspaceDialogs.LINE,button.compactBorderColor());
 }
 @Test public void disabledSystemAnimationsAndWaitingUseStaticOutline()throws Exception{
  AgentStopButton button=new AgentStopButton(RuntimeEnvironment.application);button.compact(true);button.indicator("Codex",AgentIndicatorState.State.WORKING);
  java.lang.reflect.Method method=ValueAnimator.class.getDeclaredMethod("setDurationScale",float.class);method.setAccessible(true);method.invoke(null,0f);try{button.frame(1000000000L,0);button.frame(2250000000L,0);assertEquals(WorkspaceDialogs.LINE,button.compactBorderColor());}finally{method.invoke(null,1f);}
  button.indicator("Codex",AgentIndicatorState.State.WAITING);assertFalse(button.animated());assertEquals(WorkspaceDialogs.LINE,button.compactBorderColor());
 }
}
