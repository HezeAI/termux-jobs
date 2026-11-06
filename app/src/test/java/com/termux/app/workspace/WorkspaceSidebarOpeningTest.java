package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.time.Duration;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=29,application=Application.class)
public class WorkspaceSidebarOpeningTest {
 @Test public void selectedSessionStartsExpandedAndVisibleWithoutExecutingActions(){Activity a=Robolectric.buildActivity(Activity.class).setup().visible().get();WorkspaceSidebar menu=new WorkspaceSidebar(a);LinearLayout chosen=null;
  for(int i=0;i<30;i++){LinearLayout actions=menu.session("id"+i,"session "+i,"/fixture",i==0,false,i==24,()->false,()->fail("save"),()->fail("unsave"),()->fail("close"));menu.action(actions,"Open",0,()->fail("launch"));menu.action(actions,"Restart",0,()->fail("restart"));if(i==24)chosen=actions;else assertEquals(View.GONE,actions.getVisibility());}
  try{menu.show();Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350));int w=360,h=640;menu.scroll.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));menu.scroll.layout(0,0,w,h);menu.scroll.getViewTreeObserver().dispatchOnGlobalLayout();assertEquals(View.VISIBLE,chosen.getVisibility());View frame=(View)chosen.getParent();assertTrue(menu.scroll.getScrollY()>0);assertTrue(frame.getTop()-menu.scroll.getScrollY()>=0);assertTrue(frame.getTop()-menu.scroll.getScrollY()<64);assertTrue(menu.scroll.getSystemGestureExclusionRects().isEmpty());menu.dialog.onBackPressed();assertFalse(menu.dialog.isShowing());}finally{menu.dialog.dismiss();a.finish();}
 }
 @org.robolectric.annotation.Implements(value=com.termux.shared.view.KeyboardUtils.class,isInAndroidSdk=false)
 public static class VisibleKeyboard {
  static int hides;
  @org.robolectric.annotation.Implementation protected static boolean isSoftKeyboardVisible(Activity a){return true;}
  @org.robolectric.annotation.Implementation protected static void hideSoftKeyboard(android.content.Context a,View v){hides++;}
 }
 @Test @Config(shadows=VisibleKeyboard.class) public void systemBackWithKeyboardOnlyHidesItAndKeepsDraft(){
  com.termux.app.TermuxActivity a=Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();a.setTheme(com.termux.R.style.Theme_Termux_Black);EditText draft=new EditText(a);draft.setText("preserved draft");a.setContentView(draft);VisibleKeyboard.hides=0;
  a.onBackPressed();assertEquals(1,VisibleKeyboard.hides);assertFalse(a.isFinishing());assertEquals("preserved draft",draft.getText().toString());a.onBackPressed();assertEquals(2,VisibleKeyboard.hides);assertFalse(a.isFinishing());a.finish();
 }
 @Test public void tapOutsideAndLeftSwipeCloseWithoutOpeningAnySession(){for(boolean swipe:new boolean[]{false,true}){Activity a=Robolectric.buildActivity(Activity.class).setup().visible().get();WorkspaceSidebar menu=new WorkspaceSidebar(a);menu.session("selected","Active","/fixture",false,false,true,()->false,()->fail(),()->fail(),()->fail());menu.show();Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350));if(swipe){MotionEvent down=MotionEvent.obtain(0,0,0,180,200,0),move=MotionEvent.obtain(0,100,2,30,201,0);menu.scroll.dispatchTouchEvent(down);menu.scroll.dispatchTouchEvent(move);down.recycle();move.recycle();}else((View)menu.scroll.getParent()).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(350));assertFalse(menu.dialog.isShowing());a.finish();}}
}
