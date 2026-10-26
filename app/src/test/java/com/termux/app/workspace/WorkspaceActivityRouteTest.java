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

 @Test public void circleEntersRightHalfThenCrossesTopToLeftAndExitsRight(){
  float[] points=WorkspaceActivityLine.routePath(600,90,400,40,20).approximate(.01f);int entry=-1;
  for(int i=0;i<points.length;i+=3)if(Math.abs(points[i+1]-400)<.01&&Math.abs(points[i+2]-60)<.01){entry=i;break;}
  assertTrue("Circle must start at its lower tangent",entry>=0);int first=entry+3;
  while(first<points.length&&Math.hypot(points[first+1]-400,points[first+2]-60)<.1)first+=3;
  assertTrue("Enter the right half before travelling across the top",points[first+1]>400);
  int right=-1,top=-1,left=-1;
  for(int i=first;i<points.length;i+=3){float x=points[i+1],y=points[i+2];if(right<0&&Math.abs(x-420)<.1&&Math.abs(y-40)<.1)right=i;if(top<0&&Math.abs(x-400)<.1&&Math.abs(y-20)<.1)top=i;if(left<0&&Math.abs(x-380)<.1&&Math.abs(y-40)<.1)left=i;}
  assertTrue(right>entry&&top>right&&left>top);assertEquals(600,points[points.length-2],.01);assertEquals(90,points[points.length-1],.01);
 }
 @Test public void frameCadenceTargets60AcrossDifferentDisplayRefreshRates(){
  for(int hz:new int[]{60,90,120,144}){WorkspaceActivityLine.FrameCadence clock=new WorkspaceActivityLine.FrameCadence();int redraws=0;for(int frame=0;frame<hz*10;frame++)if(clock.due(1000000000L+Math.round(frame*1000000000d/hz)))redraws++;assertTrue("Refresh "+hz+": "+redraws,Math.abs(redraws-600)<=1);}
 }
 @Test public void missedFramesResumeWithoutBurstAndResetStartsImmediately(){WorkspaceActivityLine.FrameCadence clock=new WorkspaceActivityLine.FrameCadence();assertTrue(clock.due(1000000000L));assertFalse(clock.due(1000000000L));assertTrue(clock.due(5000000000L));assertFalse(clock.due(5000000000L));clock.reset();assertTrue(clock.due(5000000000L));}
}
