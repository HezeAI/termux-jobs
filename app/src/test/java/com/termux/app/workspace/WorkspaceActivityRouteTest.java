package com.termux.app.workspace;
import android.app.*;import android.graphics.*;import android.view.*;import android.widget.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceActivityRouteTest {
 @Test public void routeContainsFullCircleAndKeepsStraightLineEnds(){Path path=WorkspaceActivityLine.routePath(600,90,400,40,20);PathMeasure p=new PathMeasure(path,false);assertTrue(p.getLength()>600+2*Math.PI*20);java.util.List<org.robolectric.shadows.ShadowPath.Point> points=Shadows.shadowOf(path).getPoints();assertEquals(0,points.get(0).getX(),.01);assertEquals(90,points.get(0).getY(),.01);assertEquals(600,points.get(points.size()-1).getX(),.01);assertEquals(90,points.get(points.size()-1).getY(),.01);}

 @Test public void disabledStopCannotInterruptAndDoesNotChangeItsDimensions(){Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentStopButton button=new AgentStopButton(a);final int[] calls={0};button.setOnClickListener(v->{if(button.isEnabled())calls[0]++;});button.layout(0,0,48,48);button.draw(new Canvas(Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888)));button.performClick();assertEquals(0,calls[0]);button.setEnabled(true);button.performClick();assertEquals(1,calls[0]);assertEquals(48,button.getWidth());assertEquals(48,button.getHeight());assertEquals(Button.class.getName(),button.getAccessibilityClassName());}

 @Test public void activityRouteStopsOnIdleAndDetach(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().visible().get();
  FrameLayout host=new FrameLayout(a);AgentStopButton stop=new AgentStopButton(a);stop.setEnabled(true);
  WorkspaceActivityLine line=new WorkspaceActivityLine(a,null);host.addView(stop,new FrameLayout.LayoutParams(44,44));host.addView(line,new FrameLayout.LayoutParams(600,1));a.setContentView(host);
  host.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.EXACTLY));host.layout(0,0,600,100);line.layout(0,90,600,91);line.stopTarget(stop);
  try{
   line.working(true);Canvas canvas=new Canvas(Bitmap.createBitmap(600,100,Bitmap.Config.ARGB_8888));
   // Robolectric's View.draw shadow does not invoke a custom view's onDraw.
   line.onDraw(canvas);assertTrue("Route must attach while both views are visible",line.routing());
   line.working(false);assertFalse(line.routing());
   line.working(true);line.onDraw(canvas);assertTrue("Route must reattach after idle",line.routing());
   host.removeView(line);assertFalse(line.routing());
  }finally{line.working(false);host.removeView(line);}
 }
}
