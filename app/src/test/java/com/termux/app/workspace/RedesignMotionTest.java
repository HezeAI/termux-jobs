package com.termux.app.workspace;

import android.app.*;
import android.animation.ValueAnimator;
import android.view.*;
import android.widget.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class RedesignMotionTest {
 private void layout(View v,int width,int height){v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);}
 @Test public void disabledAnimationsUseImmediateDisclosureAndNoDotFrames()throws Exception{
  java.lang.reflect.Method scale=ValueAnimator.class.getDeclaredMethod("setDurationScale",float.class);scale.setAccessible(true);scale.invoke(null,0f);Activity a=Robolectric.buildActivity(Activity.class).setup().visible().get();
  try{LinearLayout host=new LinearLayout(a),body=new LinearLayout(a);body.addView(new View(a),new LinearLayout.LayoutParams(40,90));host.addView(body);AgentProgressView dots=new AgentProgressView(a);host.addView(dots);a.setContentView(host);layout(host,360,600);Object attach=ReflectionHelpers.getField(dots,"mAttachInfo");ReflectionHelpers.setField(attach,"mWindowVisibility",View.VISIBLE);dots.onWindowVisibilityChanged(View.VISIBLE);WorkspaceExpansion expansion=new WorkspaceExpansion(body);expansion.setOpen(true);assertEquals(View.VISIBLE,body.getVisibility());assertNull(ReflectionHelpers.getField(expansion,"animation"));expansion.setOpen(false);assertEquals(View.GONE,body.getVisibility());dots.present(AgentIndicatorState.State.WORKING,true);assertFalse((Boolean)ReflectionHelpers.getField(dots,"framePending"));assertTrue(WorkspaceRipple.background(a,null,10) instanceof android.graphics.drawable.StateListDrawable);dots.present(AgentIndicatorState.State.IDLE,false);}finally{a.finish();scale.invoke(null,1f);}
 }
 @Test public void disclosureChangesHeightAndReversesAtCurrentPositionWithoutReplacingRows(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().visible().get();LinearLayout list=new LinearLayout(a);list.setOrientation(1);LinearLayout body=new LinearLayout(a);body.setOrientation(1);body.addView(new View(a),new LinearLayout.LayoutParams(-1,160));list.addView(body,new LinearLayout.LayoutParams(-1,-2));View next=new View(a);list.addView(next,new LinearLayout.LayoutParams(-1,50));a.setContentView(list);WorkspaceExpansion expansion=new WorkspaceExpansion(body);layout(list,360,600);
  try{expansion.setOpen(true);ValueAnimator first=ReflectionHelpers.getField(expansion,"animation");assertNotNull(first);assertEquals(260,first.getDuration());first.setCurrentPlayTime(110);layout(list,360,600);int middle=body.getHeight();assertTrue(middle>0&&middle<160);assertEquals(middle,next.getTop());expansion.setOpen(false);assertEquals(middle,body.getLayoutParams().height);ValueAnimator closing=ReflectionHelpers.getField(expansion,"animation");closing.setCurrentPlayTime(90);layout(list,360,600);assertTrue(body.getHeight()<middle);int reverse=body.getHeight();expansion.setOpen(true);assertEquals(reverse,body.getLayoutParams().height);((ValueAnimator)ReflectionHelpers.getField(expansion,"animation")).end();layout(list,360,600);assertEquals(160,body.getHeight());assertEquals(160,next.getTop());assertEquals(2,list.getChildCount());assertSame(body,list.getChildAt(0));}finally{a.finish();}
 }
 @Test public void footerDrawsDifferentBrightnessWithoutHiddenHeaderClockAndPauses(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().visible().get();FrameLayout host=new FrameLayout(a);AgentProgressView dots=new AgentProgressView(a);host.addView(dots);a.setContentView(host);Object attach=ReflectionHelpers.getField(dots,"mAttachInfo");ReflectionHelpers.setField(attach,"mWindowVisibility",View.VISIBLE);dots.onWindowVisibilityChanged(View.VISIBLE);
  try{dots.present(AgentIndicatorState.State.WORKING,true);assertTrue((Boolean)ReflectionHelpers.getField(dots,"framePending"));dots.frame(1500000000L);int[] first={dots.dotAlpha(0),dots.dotAlpha(1),dots.dotAlpha(2)};dots.frame(2000000000L);assertEquals(first[0],dots.dotAlpha(1));assertNotEquals(first[0],dots.dotAlpha(0));
   class Recording extends android.graphics.Canvas {int count;int[] alpha=new int[3];@Override public void drawCircle(float x,float y,float r,android.graphics.Paint p){alpha[count++]=p.getAlpha();}}
   Recording canvas=new Recording();dots.onDraw(canvas);assertEquals(3,canvas.count);assertEquals(dots.dotAlpha(1),canvas.alpha[1]);assertNotEquals(canvas.alpha[0],canvas.alpha[1]);
   host.setVisibility(View.GONE);assertFalse((Boolean)ReflectionHelpers.getField(dots,"framePending"));host.setVisibility(View.VISIBLE);assertTrue((Boolean)ReflectionHelpers.getField(dots,"framePending"));dots.present(AgentIndicatorState.State.IDLE,true);assertFalse((Boolean)ReflectionHelpers.getField(dots,"framePending"));
  }finally{a.finish();}
 }
 @Test public void modelMicrophoneAndSendStayTogetherWithSpaceBeforeModel(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();ComposerActionLayout row=new ComposerActionLayout(a);View[] views=new View[6];for(int i=0;i<6;i++){views[i]=new TextView(a);views[i].setMinimumWidth(i==3?70:44);views[i].setMinimumHeight(44);row.addView(views[i]);}row.rightGroup(views[3]);layout(row,360,100);assertEquals(views[3].getRight(),views[4].getLeft());assertEquals(views[4].getRight(),views[5].getLeft());assertEquals(360,views[5].getRight());assertTrue(views[3].getLeft()>views[2].getRight());layout(row,240,100);assertTrue(views[3].getTop()>=views[2].getBottom());assertEquals(240,views[5].getRight());a.finish();
 }
}
