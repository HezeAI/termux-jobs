package com.termux.app.workspace;
import android.app.Activity;import android.app.Application;import android.graphics.Color;import android.graphics.drawable.ColorDrawable;import android.os.Looper;import android.view.*;import android.widget.*;
import org.json.JSONObject;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.util.*;import java.util.concurrent.atomic.AtomicInteger;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentChatHeaderOverlayTest {
 Activity a;RelativeLayout root;FrameLayout selector,host,header;View line,stop;TextView limits,server;EditText input;AgentChatView chat;AgentChatHeaderOverlay overlay;AtomicInteger opened=new AtomicInteger(),limitClicks=new AtomicInteger(),serverClicks=new AtomicInteger(),stopClicks=new AtomicInteger();
 @Before public void setup()throws Exception{
  a=Robolectric.buildActivity(Activity.class).setup().visible().get();root=new RelativeLayout(a);a.setContentView(root);
  selector=new FrameLayout(a);selector.setId(10);selector.setBackgroundColor(0xff335577);root.addView(selector,new RelativeLayout.LayoutParams(-1,56));
  header=new FrameLayout(a);header.setId(20);header.setMinimumHeight(52);header.setBackgroundColor(Color.BLACK);RelativeLayout.LayoutParams hp=new RelativeLayout.LayoutParams(-1,-2);hp.addRule(RelativeLayout.BELOW,10);root.addView(header,hp);
  limits=new TextView(a);limits.setText("Limits");limits.setTextColor(Color.WHITE);limits.setBackgroundColor(0xff123456);limits.setOnClickListener(v->limitClicks.incrementAndGet());header.addView(limits,new FrameLayout.LayoutParams(100,48,Gravity.START));
  server=new TextView(a);server.setText("Server + Agent");server.setBackgroundColor(0xff456789);server.setOnClickListener(v->serverClicks.incrementAndGet());header.addView(server,new FrameLayout.LayoutParams(106,48,Gravity.END));
  stop=new View(a);stop.setBackgroundColor(0xff999999);stop.setOnClickListener(v->stopClicks.incrementAndGet());FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(44,44,Gravity.RIGHT);sp.rightMargin=112;header.addView(stop,sp);
  line=new View(a);line.setId(30);RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams(-1,1);lp.addRule(RelativeLayout.BELOW,20);root.addView(line,lp);
  input=new EditText(a);input.setId(40);input.setText("draft");RelativeLayout.LayoutParams ip=new RelativeLayout.LayoutParams(-1,64);ip.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);root.addView(input,ip);
  host=new FrameLayout(a);host.setId(50);RelativeLayout.LayoutParams vp=new RelativeLayout.LayoutParams(-1,-1);vp.addRule(RelativeLayout.BELOW,30);vp.addRule(RelativeLayout.ABOVE,40);root.addView(host,vp);
  chat=new AgentChatView(a,r->opened.incrementAndGet(),()->{},()->{},()->{},()->{});host.addView(chat,new FrameLayout.LayoutParams(-1,-1));List<JSONObject> rows=new ArrayList<>();for(int i=0;i<30;i++)rows.add(new JSONObject().put("key","m"+i).put("kind","reasoning").put("body","Synthetic message "+i).put("message","msg_"+i).put("part","reasoning"));chat.rows(rows,false);
  overlay=new AgentChatHeaderOverlay(host,header,line,10,chat::headerInset);layout(360,700);chat.restorePosition(new AgentChatView.Position(null,0,0,false));layout(360,700);
 }
 void layout(int width,int height){for(int i=0;i<4;i++){root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);root.getViewTreeObserver().dispatchOnPreDraw();Shadows.shadowOf(Looper.getMainLooper()).idle();}}
 View first(){return chat.timeline.getChildAt(1);}
 int firstY(){int[] p=new int[2];first().getLocationOnScreen(p);return p[1];}
 int headerBottom(){int[] p=new int[2];header.getLocationOnScreen(p);return p[1]+header.getHeight()+line.getHeight();}
 void tap(View v){int[] p=new int[2];v.getLocationOnScreen(p);int[] parent=new int[2];root.getLocationOnScreen(parent);float x=p[0]-parent[0]+v.getWidth()/2f,y=p[1]-parent[1]+v.getHeight()/2f;MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(0,10,MotionEvent.ACTION_UP,x,y,0);assertTrue(root.dispatchTouchEvent(down));assertTrue(root.dispatchTouchEvent(up));down.recycle();up.recycle();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 @After public void close(){overlay.close();a.finish();}
 @Test public void chatExtendsUnderTransparentHeaderWithoutChangingSelectorOrButtonFills(){
  Object selectedBackground=selector.getBackground(),limitsBackground=limits.getBackground(),serverBackground=server.getBackground();int selectedBottom=selector.getBottom();overlay.present(true);layout(360,700);
  assertEquals(selectedBottom,host.getTop());assertEquals(selectedBottom,header.getTop());assertNull(header.getBackground());assertSame(selectedBackground,selector.getBackground());assertSame(limitsBackground,limits.getBackground());assertSame(serverBackground,server.getBackground());assertTrue(header.getElevation()>host.getElevation());
  assertTrue(firstY()>=headerBottom());chat.scroll.scrollTo(0,first().getTop());assertTrue(firstY()<headerBottom());
  overlay.present(false);layout(360,700);assertEquals(line.getBottom(),host.getTop());assertEquals(0xff000000,((ColorDrawable)header.getBackground()).getColor());assertEquals(0,header.getElevation(),0);
 }
 @Test public void firstMessageClearsExpandedHeaderAndInsetsStayFixedAtEveryScale(){
  overlay.present(true);layout(320,700);chat.restorePosition(new AgentChatView.Position(null,0,0,false));layout(320,700);assertTrue(firstY()>=headerBottom());
  limits.getLayoutParams().height=104;limits.requestLayout();layout(320,700);assertTrue(header.getHeight()>=104);assertEquals(0,chat.scroll.getScrollY());assertTrue(firstY()>=headerBottom());
  for(int scale:new int[]{50,100,150}){chat.scale(scale);layout(320,700);chat.restorePosition(new AgentChatView.Position(null,0,0,false));layout(320,700);assertTrue("scale "+scale,firstY()>=headerBottom());}
 }
 @Test public void visibleConnectionPillAlsoClearsFirstMessageAndConsumesItsTap(){
  overlay.present(true);chat.status.setText("Provider status");chat.status.setVisibility(View.VISIBLE);chat.status.setOnClickListener(v->serverClicks.incrementAndGet());chat.updateHeaderInset();layout(320,700);chat.restorePosition(new AgentChatView.Position(null,0,0,false));layout(320,700);
  int[] p=new int[2];chat.status.getLocationOnScreen(p);assertTrue(p[1]>=headerBottom());assertTrue(firstY()>=p[1]+chat.status.getHeight());
  chat.scroll.scrollTo(0,first().getTop());tap(chat.status);assertEquals(1,serverClicks.get());assertEquals(0,opened.get());
  chat.status.setVisibility(View.GONE);chat.updateHeaderInset();layout(320,700);chat.restorePosition(new AgentChatView.Position(null,0,0,false));layout(320,700);assertTrue(firstY()>=headerBottom());
 }
 @Test public void headerButtonsConsumeTouchesWhileMessagesScrollUnderneath(){
  overlay.present(true);layout(360,700);chat.scroll.scrollTo(0,first().getTop());assertTrue(firstY()<headerBottom());
  tap(limits);tap(stop);tap(server);assertEquals(1,limitClicks.get());assertEquals(1,stopClicks.get());assertEquals(1,serverClicks.get());assertEquals(0,opened.get());
  stop.setVisibility(View.GONE);layout(360,700);assertEquals(0,opened.get());stop.setVisibility(View.VISIBLE);layout(360,700);tap(stop);assertEquals(2,stopClicks.get());assertEquals(0,opened.get());
 }
 @Test public void headerChangesAndKeyboardResizeKeepDraftAndReadingAnchor(){
  overlay.present(true);layout(360,700);chat.scroll.scrollTo(0,400);AgentChatView.Position before=chat.position();assertNotNull(before.anchor);input.requestFocus();input.setText("draft\nsecond line");input.setSelection(4);
  limits.getLayoutParams().height=104;limits.requestLayout();layout(360,700);AgentChatView.Position expanded=chat.position();assertEquals(before.anchor,expanded.anchor);assertEquals(before.offset,expanded.offset);
  input.getLayoutParams().height=112;input.requestLayout();layout(360,420);assertEquals("draft\nsecond line",input.getText().toString());assertEquals(4,input.getSelectionStart());assertTrue(input.hasFocus());
  chat.restorePosition(new AgentChatView.Position(null,0,0,false));layout(360,420);assertTrue(firstY()>=headerBottom());
 }
}
