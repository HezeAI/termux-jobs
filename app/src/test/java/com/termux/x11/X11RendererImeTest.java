package com.termux.x11;

import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Stub only native setup and the Android IME service; exercise the actual renderer methods. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows={X11RendererImeTest.NativeLoad.class,X11RendererImeTest.NativeRenderer.class,X11RendererImeTest.Ime.class})
public class X11RendererImeTest {
 @Implements(X11Native.class) public static class NativeLoad {@Implementation protected static void load(){}}
 @Implements(LorieView.class) public static class NativeRenderer extends org.robolectric.shadows.ShadowSurfaceView {
  @Implementation public boolean post(Runnable action){new android.os.Handler(Looper.getMainLooper()).post(action);return true;}
  static int disconnects,destroys,suspends;
  static final java.util.List<String> mouse=new java.util.ArrayList<>();
  @Implementation protected void sendMouseEvent(long ptr,float x,float y,int button,boolean down,boolean relative){if(button>0)mouse.add(button+":"+down);}
  @Implementation protected long nativeInit(){return 1;}
  @Implementation protected static void connect(long pointer,int fd){if(fd==-1)disconnects++;}
  @Implementation protected void nativeDestroy(long pointer){destroys++;}
  @Implementation protected void surfaceChanged(long pointer,android.view.Surface surface){if(surface==null)suspends++;}
  @Implementation protected void setFiltering(long pointer,int value){}
  @Implementation protected void setClipboardSyncEnabled(long pointer,boolean enabled,boolean ignored){}
 }
 @Implements(InputMethodManager.class) public static class Ime extends org.robolectric.shadows.ShadowInputMethodManager {
  static int restarts;
  @Implementation protected void restartInput(View view){restarts++;}
 }
 @Before public void reset(){Ime.restarts=0;NativeRenderer.disconnects=NativeRenderer.destroys=NativeRenderer.suspends=0;NativeRenderer.mouse.clear();}
 @Test public void embeddedRendererCannotAdvertiseOrCreateAnImeOrRestartItOnMouseCallbacks(){
  LorieView renderer=new LorieView(RuntimeEnvironment.application);
  assertFalse(renderer.onCheckIsTextEditor());EditorInfo info=new EditorInfo();info.inputType=123;
  assertNull(renderer.onCreateInputConnection(info));assertEquals(123,info.inputType);
  renderer.resetIme();renderer.resetIme();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,Ime.restarts);
 }
 @Test public void serviceRetainedRendererSurvivesDetachAndClosesExactlyOnceAtExplicitRelease(){
  LorieView renderer=new LorieView(RuntimeEnvironment.application);renderer.retainRendererByService();renderer.onDetachedFromWindow();renderer.onDetachedFromWindow();
  assertEquals(0,NativeRenderer.disconnects);assertEquals(0,NativeRenderer.destroys);assertEquals(2,NativeRenderer.suspends);
  assertEquals(1L,(long)org.robolectric.util.ReflectionHelpers.getField(renderer,"pointer"));renderer.release();renderer.release();
  assertEquals(1,NativeRenderer.disconnects);assertEquals(1,NativeRenderer.destroys);
 }
 @Test public void nonRetainedRendererStillClosesOnDetach(){
  LorieView renderer=new LorieView(RuntimeEnvironment.application);renderer.onDetachedFromWindow();assertEquals(1,NativeRenderer.disconnects);assertEquals(1,NativeRenderer.destroys);
 }
 @Test public void surfaceLossReleasesHeldMouseButtonAndCancelsFingerHold()throws Exception{
  LorieView renderer=new LorieView(RuntimeEnvironment.application);
  java.lang.reflect.Method button=LorieView.class.getDeclaredMethod("mouseButton",int.class,boolean.class,boolean.class);button.setAccessible(true);button.invoke(renderer,1,true,false);
  renderer.surfaceDestroyed(renderer.getHolder());renderer.suspendSurface();assertEquals(java.util.Arrays.asList("1:true","1:false"),NativeRenderer.mouse);
  android.view.MotionEvent e=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,10,10,0);renderer.onTouchEvent(e);e.recycle();renderer.surfaceDestroyed(renderer.getHolder());Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertEquals(2,NativeRenderer.mouse.size());
 }
 @Test public void standaloneRendererKeepsItsOwnImeConnectionAndRestart(){
  MainActivity activity=Robolectric.buildActivity(MainActivity.class).get();LorieView renderer=new LorieView(activity);
  assertTrue(renderer.onCheckIsTextEditor());assertNotNull(renderer.onCreateInputConnection(new EditorInfo()));
  assertEquals(1L,(long)org.robolectric.util.ReflectionHelpers.getField(renderer,"pointer"));renderer.resetIme();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,Ime.restarts);
 }
}
