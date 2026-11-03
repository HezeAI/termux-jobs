package com.termux.app.workspace;
import android.app.*;import android.graphics.drawable.RippleDrawable;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentComposerControlsTest {
 @Test public void verticalToolsKeepIndependentTargetsDraftAndBottomSendOnNarrowLargeFont(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);EditText input=new EditText(a);WorkspaceIconButton star=new WorkspaceIconButton(a),send=new WorkspaceIconButton(a),clip=new WorkspaceIconButton(a);AtomicInteger stars=new AtomicInteger(),files=new AtomicInteger(),sends=new AtomicInteger();star.setOnClickListener(v->stars.incrementAndGet());clip.setOnClickListener(v->files.incrementAndGet());send.setOnClickListener(v->sends.incrementAndGet());
  row.addView(star);row.addView(input,new LinearLayout.LayoutParams(0,-2,1));row.addView(send);AgentComposerControls c=new AgentComposerControls(a,row,input,star,send,clip);
  for(String language:new String[]{"ru","en"})for(int width:new int[]{240,280,360})for(int size:new int[]{14,24,36})for(String draft:new String[]{"draft","first line\nsecond line\nthird line"}){
   WorkspaceLanguage.select(a,language);input.setText(draft);input.setTextSize(size);input.setSelection(2);c.present(true);row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.AT_MOST));row.layout(0,0,width,row.getMeasuredHeight());
   assertEquals(LinearLayout.VERTICAL,c.pair.getOrientation());assertSame(clip,c.pair.getChildAt(0));assertSame(star,c.pair.getChildAt(1));assertEquals(48,clip.getWidth());assertEquals(48,clip.getHeight());assertEquals(48,star.getWidth());assertEquals(48,star.getHeight());assertEquals(clip.getBottom(),star.getTop());assertEquals(48,c.pair.getWidth());
   assertSame(input,row.getChildAt(1));assertSame(send,row.getChildAt(2));assertTrue(input.getWidth()>=width-108);assertEquals(row.getHeight(),send.getBottom());assertEquals(input.getBottom(),send.getBottom());assertTrue(send.getRight()<=width);assertTrue(clip.getBackground()instanceof RippleDrawable);assertTrue(star.getBackground()instanceof RippleDrawable);assertFalse(row.isBaselineAligned());
   c.present(true);assertEquals(draft,input.getText().toString());assertEquals(2,input.getSelectionStart());clip.performClick();star.performClick();assertEquals(draft,input.getText().toString());c.present(false);assertTrue(row.isBaselineAligned());assertSame(star,row.getChildAt(0));assertSame(input,row.getChildAt(1));assertSame(send,row.getChildAt(2));assertNull(clip.getParent());assertEquals(draft,input.getText().toString());assertEquals(2,input.getSelectionStart());
  }
  assertEquals(36,stars.get());assertEquals(36,files.get());assertEquals(0,sends.get());a.finish();
 }
}
