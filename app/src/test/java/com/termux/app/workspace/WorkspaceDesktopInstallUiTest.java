package com.termux.app.workspace;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopInstallUiTest {
 @Test public void zypperCardKeepsCurrentPackageWhileRawOutputSpins(){
  DesktopProgramProgress card=new DesktopProgramProgress(ui.dialog.getContext());Bundle state=new Bundle();state.putString("item","libpulse0 (1/5)");state.putString("detail","Retrieving: libpulse0\n.\r|\r/\r");
  card.update(state,true);assertEquals("libpulse0 (1/5)",card.file.getText().toString());CharSequence text=card.file.getText();card.update(state,true);assertSame(text,card.file.getText());
  assertEquals("Retrieving: libpulse0",DesktopProgramProgress.lastLine("Retrieving: libpulse0\n.\r|\r/\r\\\r[..]\r"));
 }
 @Test public void inlineRemainingAndMeasuredVolumeShareOneLineAndUnknownBytesStayHidden(){
  DesktopProgramProgress card=new DesktopProgramProgress(ui.dialog.getContext());Bundle b=new Bundle();b.putString("stage","downloading");b.putLong("eta",30);b.putLong("sampleTime",SystemClock.elapsedRealtime());b.putLong("downloadBytes",18000000);b.putLong("downloadTotal",64000000);card.update(b,true);
  String text=card.remaining.getText().toString();assertTrue(text.contains(" · 18.0 MB / 64.0 MB"));assertFalse(text.contains("\n"));
  CharSequence before=card.remaining.getText();card.update(b,true);assertSame(before,card.remaining.getText());
  b.putLong("downloadTotal",-1);card.update(b,true);assertFalse(card.remaining.getText().toString().contains("MB"));
  b.putLong("downloadTotal",64000000);b.putLong("downloadBytes",65000000);card.update(b,true);assertFalse(card.remaining.getText().toString().contains("MB"));
 }
 private DesktopInstallUi ui;private ScrollView scroll;private TextView details;
 @Before public void setup(){Activity a=Robolectric.buildActivity(Activity.class).setup().get();a.setTheme(R.style.Theme_Termux_Black);ui=new DesktopInstallUi(a,"Install",()->{});scroll=ui.dialog.getWindow().getDecorView().findViewWithTag("desktop-install-log");details=ui.dialog.getWindow().getDecorView().findViewWithTag("desktop-install-detail");}
 @After public void close(){ui.close();}
 private void update(String log){ui.update("Installing",log,30,-1,SystemClock.elapsedRealtime(),"installing");int w=View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),h=View.MeasureSpec.makeMeasureSpec(180,View.MeasureSpec.EXACTLY);scroll.measure(w,h);scroll.layout(0,0,400,180);scroll.getViewTreeObserver().dispatchOnPreDraw();}
 private String lines(int count){StringBuilder s=new StringBuilder();for(int i=0;i<count;i++)s.append("Package ").append(i).append('\n');return s.toString();}
 @Test public void newOutputFollowsTailButPreservesManuallyScrolledPosition(){update(lines(70));assertTrue(details.getHeight()>scroll.getHeight());assertEquals("first output",details.getHeight()-scroll.getHeight(),scroll.getScrollY());update(lines(90));assertEquals("growing output",details.getHeight()-scroll.getHeight(),scroll.getScrollY());scroll.scrollTo(0,40);update(lines(110));assertEquals(40,scroll.getScrollY());scroll.fullScroll(View.FOCUS_DOWN);update(lines(130));assertEquals("resume following",details.getHeight()-scroll.getHeight(),scroll.getScrollY());}
 @Test public void identicalPollingOutputDoesNotReplaceSelectableText(){String log=lines(70);update(log);CharSequence text=details.getText();scroll.scrollTo(0,40);update(log);assertSame(text,details.getText());assertEquals(40,scroll.getScrollY());}
 @Test public void outsideTapCannotDismissInstallation(){MotionEvent e=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,-100,-100,0);try{ui.dialog.onTouchEvent(e);assertTrue(ui.dialog.isShowing());}finally{e.recycle();}}
 @Test public void measuredVolumeFollowsPercentageAndIsHiddenOutsideDownloads(){
  TextView status=ui.dialog.getWindow().getDecorView().findViewWithTag("desktop-install-status");
  ui.update("Install","",45,-1,SystemClock.elapsedRealtime(),"downloading",180000000,400000000);
  assertTrue(status.getText().toString().contains("45% (180 "));assertTrue(status.getText().toString().contains(" / 400 "));
  ui.update("Install","",60,-1,SystemClock.elapsedRealtime(),"installing",180000000,400000000);assertFalse(status.getText().toString().contains("("));
  ui.update("Install","",20,-1,SystemClock.elapsedRealtime(),"downloading",0,-1);assertFalse(status.getText().toString().contains("("));
 }
}
