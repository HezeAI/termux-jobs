package com.termux.app.workspace;
import android.app.*;import android.graphics.drawable.RippleDrawable;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentComposerControlsTest {
 @Test public void horizontalToolsKeepIndependentTargetsDraftAndBottomSendOnNarrowLargeFont(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);EditText input=new EditText(a);WorkspaceIconButton star=new WorkspaceIconButton(a),send=new WorkspaceIconButton(a),clip=new WorkspaceIconButton(a);AtomicInteger stars=new AtomicInteger(),files=new AtomicInteger(),sends=new AtomicInteger();star.setOnClickListener(v->stars.incrementAndGet());clip.setOnClickListener(v->files.incrementAndGet());send.setOnClickListener(v->sends.incrementAndGet());
  row.addView(star);row.addView(input,new LinearLayout.LayoutParams(0,-2,1));row.addView(send);AgentComposerControls c=new AgentComposerControls(a,row,input,star,send,clip);
  for(String language:new String[]{"ru","en"})for(int width:new int[]{240,280,360})for(int size:new int[]{14,24,36})for(String draft:new String[]{"draft","first line\nsecond line\nthird line"}){
   WorkspaceLanguage.select(a,language);input.setText(draft);input.setTextSize(size);input.setSelection(2);c.present(true);row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.AT_MOST));row.layout(0,0,width,row.getMeasuredHeight());
   assertEquals(LinearLayout.HORIZONTAL,c.pair.getOrientation());assertEquals(0xff0b1319,((android.graphics.drawable.GradientDrawable)c.pair.getBackground()).getColor().getDefaultColor());assertSame(star,c.pair.getChildAt(0));assertEquals("/",((TextView)c.pair.getChildAt(1)).getText().toString());assertFalse(c.pair.getChildAt(1).isClickable());assertSame(clip,c.pair.getChildAt(2));assertEquals(48,clip.getWidth());assertEquals(c.pair.getHeight(),clip.getHeight());assertEquals(48,star.getWidth());assertEquals(c.pair.getHeight(),star.getHeight());assertEquals(input.getBottom(),c.pair.getBottom());assertEquals(clip.getTop(),star.getTop());assertEquals(108,c.pair.getWidth());
   assertSame(input,row.getChildAt(1));assertSame(send,row.getChildAt(2));assertTrue(input.getWidth()>=width-168);assertEquals(row.getHeight(),send.getBottom());assertEquals(input.getBottom(),send.getBottom());assertTrue(send.getRight()<=width);assertTrue(clip.getBackground()instanceof RippleDrawable);assertTrue(star.getBackground()instanceof RippleDrawable);assertFalse(row.isBaselineAligned());
   c.present(true);assertEquals(draft,input.getText().toString());assertEquals(2,input.getSelectionStart());clip.performClick();star.performClick();assertEquals(draft,input.getText().toString());c.present(false);assertTrue(row.isBaselineAligned());assertSame(star,row.getChildAt(0));assertSame(input,row.getChildAt(1));assertSame(send,row.getChildAt(2));assertNull(clip.getParent());assertEquals(draft,input.getText().toString());assertEquals(2,input.getSelectionStart());
  }
  assertEquals(36,stars.get());assertEquals(36,files.get());assertEquals(0,sends.get());a.finish();
 }
 @Test public void toolsMatchOneLineHeightAndStayAtFieldBottomAsDraftGrows(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();
  for(int size:new int[]{12,24,36}){LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.HORIZONTAL);EditText input=new EditText(a);input.setPadding(12,10,12,10);input.setMinLines(1);input.setMinHeight(48);input.setMaxLines(6);input.setTextSize(size);WorkspaceIconButton star=new WorkspaceIconButton(a),send=new WorkspaceIconButton(a),clip=new WorkspaceIconButton(a);row.addView(star);row.addView(input,new LinearLayout.LayoutParams(0,-2,1));row.addView(send);AgentComposerControls c=new AgentComposerControls(a,row,input,star,send,clip);input.setText("one");c.present(true);
   for(int n=0;n<2;n++){row.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST));row.layout(0,0,320,row.getMeasuredHeight());}
   int initial=input.getHeight();assertEquals(initial,c.pair.getHeight());input.setText("one\ntwo\nthree\nfour");input.setSelection(3);c.present(true);for(int n=0;n<2;n++){row.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST));row.layout(0,0,320,row.getMeasuredHeight());}assertTrue(input.getHeight()>initial);assertEquals(initial,c.pair.getHeight());assertEquals(input.getBottom(),c.pair.getBottom());assertEquals(3,input.getSelectionStart());
  }a.finish();
 }
 @Test public void overlayTracksMultilineHeightAndRestoresTerminalGeometryWithoutChangingDraft()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();RelativeLayout root=new RelativeLayout(a);FrameLayout host=new FrameLayout(a);host.setId(9001);LinearLayout row=new LinearLayout(a);row.setId(9002);row.setOrientation(LinearLayout.HORIZONTAL);View toolbar=new View(a);toolbar.setId(9003);
  RelativeLayout.LayoutParams hostParams=new RelativeLayout.LayoutParams(-1,-1);hostParams.addRule(RelativeLayout.ABOVE,9003);root.addView(host,hostParams);
  RelativeLayout.LayoutParams rowParams=new RelativeLayout.LayoutParams(-1,-2);rowParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);root.addView(row,rowParams);
  RelativeLayout.LayoutParams keys=new RelativeLayout.LayoutParams(-1,30);keys.addRule(RelativeLayout.ABOVE,9002);root.addView(toolbar,keys);
  EditText input=new EditText(a);input.setMinHeight(48);input.setBackgroundColor(0xff123456);WorkspaceIconButton star=new WorkspaceIconButton(a),send=new WorkspaceIconButton(a),clip=new WorkspaceIconButton(a);row.addView(star);row.addView(input,new LinearLayout.LayoutParams(0,-2,1));row.addView(send);
  AgentChatView chat=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});host.addView(chat,new FrameLayout.LayoutParams(-1,-1));AgentComposerControls controls=new AgentComposerControls(a,row,input,star,send,clip);controls.overlay(host,chat::composerInset);
  a.setContentView(root);input.setText("draft\nsecond\nthird");input.setSelection(3);controls.present(true);
  for(int height:new int[]{800,430,800}){
   root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,320,height);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,320,height);
   assertEquals(height,host.getBottom());assertEquals(height,row.getBottom());assertNull(row.getBackground());assertNotNull(input.getBackground());assertTrue(chat.timeline.getPaddingBottom()>=row.getHeight()+16);assertEquals(input.getBottom(),send.getBottom());assertEquals("draft\nsecond\nthird",input.getText().toString());assertEquals(3,input.getSelectionStart());
  }
  controls.present(false);assertEquals(9003,((RelativeLayout.LayoutParams)host.getLayoutParams()).getRule(RelativeLayout.ABOVE));assertEquals(0,((RelativeLayout.LayoutParams)host.getLayoutParams()).getRule(RelativeLayout.ALIGN_PARENT_BOTTOM));a.finish();
 }
}
