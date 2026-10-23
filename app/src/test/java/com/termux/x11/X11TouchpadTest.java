package com.termux.x11;
import android.app.Application;
import android.os.Looper;
import android.view.MotionEvent;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class X11TouchpadTest {
 private int taps;
 private final List<String> events=new ArrayList<>();
 private final X11Touchpad pad=new X11Touchpad(new X11Touchpad.Sink(){public void tap(){taps++;}public void move(float x,float y){events.add("move");}public void button(int b,boolean down){events.add(b+":"+down);}public void scroll(float x,float y){events.add("scroll");}},8,500);
 @After public void cleanup(){pad.cancel();}
 private void one(int action,long time,float x,float y){MotionEvent e=MotionEvent.obtain(0,time,action,x,y,0);try{pad.event(e);}finally{e.recycle();}}
 private void two(int action,long time,float y){MotionEvent.PointerProperties[] p={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] c={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};for(int i=0;i<2;i++){p[i].id=i;p[i].toolType=MotionEvent.TOOL_TYPE_FINGER;c[i].x=10+i*40;c[i].y=y;c[i].pressure=1;}MotionEvent e=MotionEvent.obtain(0,time,action,2,p,c,0,0,1,1,0,0,0,0);try{pad.event(e);}finally{e.recycle();}}
 @Test public void movingFingerDoesNotHoldMouseButton(){one(0,0,10,10);assertTrue(events.isEmpty());one(2,30,40,40);one(1,60,40,40);assertEquals(Arrays.asList("move"),events);assertEquals(0,taps);}
 @Test public void tapClicksAndDoubleTapProducesTwoClicks(){one(0,0,10,10);one(1,40,10,10);one(0,80,10,10);one(1,120,10,10);assertEquals(Arrays.asList("1:true","1:false","1:true","1:false"),events);assertEquals(2,taps);}
 @Test public void holdingThenMovingDragsAndCancelReleases(){one(0,0,10,10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(501));one(2,520,30,30);one(3,550,30,30);assertEquals(Arrays.asList("1:true","move","1:false"),events);assertEquals(0,taps);}
 @Test public void dragReleasesOnNormalUpAndRepeatedCancellationCannotPressAgain(){one(0,0,10,10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(501));one(2,520,40,40);one(1,550,40,40);pad.cancel();pad.cancel();Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertEquals(Arrays.asList("1:true","move","1:false"),events);}
 @Test public void secondFingerReleasesDragBeforeChangingToScroll(){one(0,0,10,10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(501));two(5|(1<<8),520,10);two(2,540,50);one(1,600,10,50);assertEquals(Arrays.asList("1:true","1:false","scroll"),events);}
 @Test public void movedFingerCannotBecomeDelayedDrag(){one(0,0,10,10);one(2,20,40,40);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));one(1,1100,40,40);assertEquals(Arrays.asList("move"),events);assertEquals(0,taps);}
 @Test public void twoFingerSwipeScrollsWithoutClicks(){one(0,0,10,10);two(5|(1<<8),10,10);two(2,40,50);two(6|(1<<8),70,50);one(1,80,10,50);assertEquals(Arrays.asList("scroll"),events);}
 @Test public void twoFingerTapIsRightClick(){one(0,0,10,10);two(5|(1<<8),10,10);two(6|(1<<8),40,10);one(1,50,10,10);assertEquals(Arrays.asList("3:true","3:false"),events);assertEquals(0,taps);}
 @Test public void cancelledGestureCannotClickOrLeaveAHoldTimer(){one(0,0,10,10);one(3,10,10,10);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertTrue(events.isEmpty());}
}
