package com.termux.x11;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=android.app.Application.class)
public class X11StartupUiTest {
 @Test public void progressWaitsForMilestonesAndCannotFinishBeforeReadiness(){
  X11StartupView view=new X11StartupView(RuntimeEnvironment.application,()->{});
  try{
   ProgressBar bar=view.findViewWithTag("x11-startup-progress");TextView step=view.findViewWithTag("x11-startup-step");assertFalse(bar.isIndeterminate());
   view.update(false,false,2);assertEquals(2,bar.getProgress());assertTrue(step.getText().toString().contains("XFCE"));
   Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(10));assertEquals(2,bar.getProgress());
   view.update(false,false,999);assertEquals(3,bar.getProgress());assertTrue(bar.getProgress()<bar.getMax());
   view.update(true,false,0);assertEquals(bar.getMax(),bar.getProgress());
  }finally{view.removeAllViews();}
 }
 @Test public void emblemUsesThreeItalicLinesAndDetailStaysOnOneLine(){
  X11StartupView v=new X11StartupView(RuntimeEnvironment.application,()->{});
  for(String label:new String[]{"Termux Job’s","+","X11"}){TextView t=v.findViewWithTag("x11-emblem-"+label);assertEquals(label,t.getText().toString());assertEquals(android.graphics.Typeface.ITALIC,((android.text.Spanned)t.getText()).getSpans(0,t.length(),android.text.style.StyleSpan.class)[0].getStyle());}
  v.preparation("Install","42%",42,"package detail","Cancel",true,()->{});
  assertEquals(1,((TextView)v.findViewWithTag("x11-preparation-detail")).getMaxLines());assertEquals(42,((ProgressBar)v.findViewWithTag("x11-startup-progress")).getProgress());
 }
 @Test public void timeoutStopsProgressAndRetryRestoresIt(){
  X11StartupView view=new X11StartupView(RuntimeEnvironment.application,()->{});
  try{ProgressBar bar=view.findViewWithTag("x11-startup-progress");view.update(false,true,2);assertEquals(android.view.View.GONE,bar.getVisibility());view.update(false,false,1);assertEquals(android.view.View.VISIBLE,bar.getVisibility());assertEquals(1,bar.getProgress());}finally{view.removeAllViews();}
 }
}
