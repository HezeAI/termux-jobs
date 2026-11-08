package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class TerminalModeRowTest {
 @Test public void actualWidthSelectsLabelAndRotationRestoresItWithoutShrinkingTargets(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();
  try{for(boolean agentTerminal:new boolean[]{false,true})for(float scale:new float[]{1f,1.5f,2f}){
   LinearLayout composer=new LinearLayout(a);AgentComposerControls controls=new AgentComposerControls(a,composer,new EditText(a),new TextView(a),new TextView(a),new View(a));WorkspaceLanguage.select(a,"ru");controls.present(false);if(agentTerminal)controls.agentTerminalLimits();
   TerminalModeRow row=composer.findViewWithTag("terminal-inline-controls");Switch first=(Switch)row.getChildAt(0),second=(Switch)row.getChildAt(1);first.setTextSize(11*scale);second.setTextSize(11*scale);float firstSize=first.getTextSize(),secondSize=second.getTextSize();
   for(int width:new int[]{1000,280,320,640,1000}){
    row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));row.layout(0,0,width,row.getMeasuredHeight());
    assertTrue(first.getRight()<=second.getLeft());assertTrue(second.getRight()<=width);assertEquals(first.getTop()+first.getHeight()/2,second.getTop()+second.getHeight()/2,1);assertTrue(first.getHeight()>=44);assertTrue(second.getHeight()>=44);assertEquals(firstSize,first.getTextSize(),0);assertEquals(secondSize,second.getTextSize(),0);assertEquals("Дополнительные клавиши",second.getContentDescription());
    if(width==1000)assertEquals("Дополнительные клавиши",second.getText().toString());
   }
   // This threshold comes from actual native measurements, not screen categories.
   int fullWidth=first.getWidth()+second.getWidth();row.measure(View.MeasureSpec.makeMeasureSpec(fullWidth-1,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));assertEquals("Доп. клавиши",second.getText().toString());
  }}finally{a.finish();}
 }
}
