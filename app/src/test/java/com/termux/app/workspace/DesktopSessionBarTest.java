package com.termux.app.workspace;
import android.app.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Implementation;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class DesktopSessionBarTest {
 // Legacy Robolectric does not measure native Paths. This fixture tests only
 // scheduling/lifecycle, not actual pixels or frame rate on an Android device.
 @Implements(PathMeasure.class) public static class MeasuredPath {
  @Implementation protected float getLength(){return 200f;}
  @Implementation protected boolean getSegment(float start,float end,Path dst,boolean move){return true;}
 }
 @Test public void separateProgramsActionStaysTheSameDuringInstallationAndNeverRebuildsChildren(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();int[] clicks={0,0};DesktopSessionBar bar=new DesktopSessionBar(a,"Desktop",()->clicks[0]++,()->clicks[1]++);a.setContentView(bar);bar.measure(View.MeasureSpec.makeMeasureSpec(220,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.AT_MOST));bar.layout(0,0,220,bar.getMeasuredHeight());View first=bar.getChildAt(0),second=bar.getChildAt(1);assertTrue(first.getRight()<=second.getLeft());assertTrue(second.getRight()<=220);
  bar.programs.performClick();bar.working(true);assertTrue(bar.border.active());bar.programs.performClick();assertArrayEquals(new int[]{0,2},clicks);assertSame(first,bar.getChildAt(0));assertSame(second,bar.getChildAt(1));assertFalse(bar.isLayoutRequested());bar.desktop.performClick();assertArrayEquals(new int[]{1,2},clicks);bar.working(false);assertFalse(bar.border.active());assertEquals(2,bar.getChildCount());
 }
 // Render explicitly with a paused looper; an automatic layout/draw must not
 // consume the one pending frame before the test installs its callback.
 @Test @org.robolectric.annotation.LooperMode(org.robolectric.annotation.LooperMode.Mode.PAUSED)
 @Config(shadows=MeasuredPath.class) public void reusedServerBorderSchedulesAtSixteenMillisecondsAndStopsWhenHiddenOrDetached(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();DesktopSessionBar bar=new DesktopSessionBar(a,"Terminal",()->{},()->{});a.setContentView(bar);final long[] next={-1};final int[] cancellations={0};bar.border.setCallback(new Drawable.Callback(){public void invalidateDrawable(Drawable d){}public void scheduleDrawable(Drawable d,Runnable r,long when){next[0]=when;}public void unscheduleDrawable(Drawable d,Runnable r){cancellations[0]++;}});bar.border.setBounds(0,0,220,40);bar.border.setVisible(true,false);bar.working(true);long before=SystemClock.uptimeMillis();bar.border.draw(new Canvas(Bitmap.createBitmap(220,40,Bitmap.Config.ARGB_8888)));assertEquals(before+16,next[0]);bar.setVisibility(View.GONE);assertFalse(bar.border.active());assertTrue(cancellations[0]>0);bar.setVisibility(View.VISIBLE);bar.working(true);bar.onDetachedFromWindow();assertFalse(bar.border.active());
 }
}
