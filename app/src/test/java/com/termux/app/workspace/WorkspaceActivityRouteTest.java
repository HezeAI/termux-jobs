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

 @Test public void heartPulseIsSmoothStrongerDuringBouncesAndDoesNotResizeTheButton(){
  assertEquals(1,AgentStopButton.pulseScale(0,1),0);float maximum=1,strong=1,prior=1;
  for(long t=1000000000L;t<7600000000L;t+=16666667L){float value=AgentStopButton.pulseScale(t,0);assertTrue(value>=1&&value<=1.12);assertTrue(Math.abs(value-prior)<.08);prior=value;maximum=Math.max(maximum,value);strong=Math.max(strong,AgentStopButton.pulseScale(t,1));}
  assertTrue(maximum>1.10);assertTrue(strong>1.2);Activity a=Robolectric.buildActivity(Activity.class).get();AgentStopButton b=new AgentStopButton(a);b.layout(0,0,44,44);b.setEnabled(true);b.frame(3150000000L,1);assertEquals(44,b.getWidth());assertEquals(44,b.getHeight());b.setEnabled(false);b.frame(3150000000L,1);assertEquals(44,b.getWidth());
 }
 @Test public void disabledSystemAnimationsDoNotScheduleAFrameLoop()throws Exception{
  java.lang.reflect.Method scale=android.animation.ValueAnimator.class.getDeclaredMethod("setDurationScale",float.class);scale.setAccessible(true);scale.invoke(null,0f);
  Activity a=Robolectric.buildActivity(Activity.class).setup().visible().get();FrameLayout host=new FrameLayout(a);WorkspaceActivityLine line=new WorkspaceActivityLine(a,null);host.addView(line,new FrameLayout.LayoutParams(300,2));a.setContentView(host);line.layout(0,0,300,2);
  try{line.working(true);line.onDraw(new Canvas(Bitmap.createBitmap(300,2,Bitmap.Config.ARGB_8888)));java.lang.reflect.Field frame=WorkspaceActivityLine.class.getDeclaredField("framePending");frame.setAccessible(true);assertFalse(frame.getBoolean(line));}finally{line.working(false);scale.invoke(null,1f);}
 }
 @Test public void innerContactsAreAsymmetricAndStayOnOneContinuousCircleRoute(){
  for(float radius:new float[]{9,19.5f,36}){
   WorkspaceActivityLine.HeartRoute route=new WorkspaceActivityLine.HeartRoute(600,130,360,60,radius);PathMeasure m=new PathMeasure(route.path,false);float[] approximate=route.path.approximate(.005f);assertEquals(6,route.hits.length);assertTrue("entry "+radius+" "+route.insideStart+" / "+route.hits[0],route.insideStart<route.hits[0]);assertTrue("exit "+route.insideEnd+" / "+route.hits[5],route.insideEnd>route.hits[5]);float[] point=new float[2],prior=new float[2];float last=route.insideStart;int strikes=0;
   for(float hit:route.hits){assertTrue(hit>last);sample(approximate,hit/m.getLength(),point);assertEquals(radius*.84,Math.hypot(point[0]-360,point[1]-60),.6);assertTrue(route.strength(hit,radius)>.2);last=hit;strikes++;}assertEquals(6,strikes);
   sample(approximate,route.insideStart/m.getLength(),prior);assertEquals(360,prior[0],.1);assertEquals(60+radius,prior[1],.1);
   for(float d=route.insideStart+.15f;d<route.insideEnd;d+=.15f){sample(approximate,d/m.getLength(),point);assertTrue(Math.hypot(point[0]-360,point[1]-60)<=radius+.6);assertTrue(Math.hypot(point[0]-prior[0],point[1]-prior[1])<.5);prior=point.clone();}
   sample(approximate,route.insideEnd/m.getLength(),point);assertEquals(360,point[0],.1);assertEquals(60+radius,point[1],.1);assertEquals(0,route.strength(route.insideStart,radius),0);assertEquals(0,route.strength(route.insideEnd,radius),0);
   sample(approximate,(route.insideEnd+.2f)/m.getLength(),point);assertTrue(point[0]>360);assertFalse(m.nextContour());
  }
 }
 // Robolectric legacy graphics does not implement PathMeasure.getPosTan.
 // Interpolate Android Path.approximate instead; native rendering remains a device check.
 private static void sample(float[] p,float fraction,float[] out){for(int i=3;i<p.length;i+=3)if(p[i]>=fraction){float range=p[i]-p[i-3],t=range==0?0:(fraction-p[i-3])/range;out[0]=p[i-2]+(p[i+1]-p[i-2])*t;out[1]=p[i-1]+(p[i+2]-p[i-1])*t;return;}out[0]=p[p.length-2];out[1]=p[p.length-1];}
}
