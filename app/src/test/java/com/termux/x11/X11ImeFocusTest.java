package com.termux.x11;
import android.app.Activity;
import android.graphics.Rect;
import android.view.*;
import android.widget.FrameLayout;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
public class X11ImeFocusTest {
 @Test public void realFocusGainAndLossReturnFocusToEditorWithoutLeavingSurfaceFocusable(){
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();List<Boolean> changes=new ArrayList<>();
  SurfaceView surface=new SurfaceView(activity){@Override protected void onFocusChanged(boolean focus,int direction,Rect previous){super.onFocusChanged(focus,direction,previous);changes.add(focus);}};
  surface.setFocusable(false);View editor=new View(activity);editor.setFocusableInTouchMode(true);
  FrameLayout body=new FrameLayout(activity);body.addView(surface,new FrameLayout.LayoutParams(100,100));body.addView(editor,new FrameLayout.LayoutParams(1,1));activity.setContentView(body);
  editor.requestFocus();changes.clear();assertTrue(X11ImeFocus.take(surface,editor));
  assertEquals(Arrays.asList(true,false),changes);assertTrue(editor.hasFocus());assertFalse(surface.isFocusable());assertFalse(surface.isFocusableInTouchMode());
  changes.clear();assertTrue(X11ImeFocus.take(surface,editor));assertEquals(Arrays.asList(true,false),changes);
 }
 @Test public void duplicateFocusNotificationsDoNotRestartImeButLostFocusAndTransitionsDo(){
  assertFalse(X11ImeFocus.needsRequest(true,true,true,true));
  assertFalse(X11ImeFocus.needsRequest(false,false,false,false));
  assertTrue(X11ImeFocus.needsRequest(true,false,true,true));
  assertTrue(X11ImeFocus.needsRequest(false,true,true,true));
  assertTrue(X11ImeFocus.needsRequest(true,true,true,false));
  assertTrue(X11ImeFocus.needsRequest(true,true,false,true));
 }
 @Test public void keyboardInputViewNotifiesWindowGainButNotWindowLoss(){
  int[] calls={0};X11InputView view=new X11InputView(RuntimeEnvironment.application,new X11InputView.Sink(){public void text(String text){}public void key(int code,boolean down){}});
  view.onWindowFocused(()->calls[0]++);view.onWindowFocusChanged(false);assertEquals(0,calls[0]);view.onWindowFocusChanged(true);assertEquals(1,calls[0]);
 }
}
