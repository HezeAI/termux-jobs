package com.termux.app.workspace;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.MotionEvent;
import android.widget.TextView;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class WorkspaceBackupProgressTest {
 @Test public void cancelKeepsProgressVisibleUntilWorkerCompletionAndIgnoresLateEvents()throws Exception {
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceBackupProgress p=new WorkspaceBackupProgress(a,()->0L);
  java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
  AlertDialog d=WorkspaceBackupUi.backupBuilder(a).setView(p.view).setNegativeButton("Cancel",(x,w)->fail("default dismiss must be replaced")).create();d.setCancelable(false);d.show();p.bindCancellation(d,calls::incrementAndGet);p.start();
  d.getButton(-2).performClick();assertTrue(d.isShowing());assertTrue(p.isStopping());assertFalse(d.getButton(-2).isEnabled());assertEquals(1,calls.get());assertTrue(p.bar.isIndeterminate());
  String label=p.stage.getText().toString();p.update("Late event",new JSONObject().put("processed",100).put("total",100));assertEquals(label,p.stage.getText().toString());assertTrue(p.bar.isIndeterminate());
  d.getButton(-2).performClick();assertEquals(1,calls.get());
  MotionEvent outside=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,-1000,-1000,0);d.onTouchEvent(outside);outside.recycle();assertTrue(d.isShowing());p.close();d.dismiss();assertFalse(d.isShowing());a.finish();
 }
 @Test public void fileNamesReplaceInOneLineAndKnownTotalFillsBar() throws Exception {
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicLong clock=new AtomicLong();WorkspaceBackupProgress p=new WorkspaceBackupProgress(a,clock::get);
  p.update("Archive",new JSONObject().put("current","first").put("processed",25).put("total",100));
  assertFalse(p.bar.isIndeterminate());assertEquals(25,p.bar.getProgress());assertEquals("first",p.current.getText().toString());
  clock.set(65000);p.update("Archive",new JSONObject().put("current","second").put("processed",100).put("total",100));
  assertEquals(100,p.bar.getProgress());assertEquals("second",p.current.getText().toString());assertEquals(5,p.view.getChildCount());assertEquals(1,p.current.getMaxLines());assertTrue(p.elapsed.getText().toString().endsWith("01:05"));
  p.close();p.update("Late",new JSONObject().put("current","ignored"));assertEquals("second",p.current.getText().toString());a.finish();
 }
 @Test public void unknownTotalsStayIndeterminateAndDatabasePagesAreNotBytes() throws Exception {
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceBackupProgress p=new WorkspaceBackupProgress(a,()->0L);
  p.update("Scan",new JSONObject().put("processed",100));assertTrue(p.bar.isIndeterminate());
  p.update("Database",new JSONObject().put("processed",5).put("total",10).put("unit","pages"));assertFalse(p.bar.isIndeterminate());assertEquals("50%",p.amount.getText().toString());
  assertEquals("1:01:02",WorkspaceBackupProgress.duration(3662000));assertEquals("00:00",WorkspaceBackupProgress.duration(-5));p.close();a.finish();
 }
 @Test public void allBackupDialogShapesIgnoreOutsideTapAndKeepExplicitCancel() {
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();
  for(int shape=0;shape<3;shape++){
   android.app.AlertDialog.Builder builder=WorkspaceBackupUi.backupBuilder(a);boolean[] cancelled={false};
   if(shape==0)builder.setItems(new String[]{"one"},(d,w)->{});
   if(shape==1)builder.setMultiChoiceItems(new String[]{"one"},new boolean[]{false},(d,w,c)->{});
   if(shape==2)builder.setView(new TextView(a));
   AlertDialog d=builder.setNegativeButton("Cancel",(x,w)->cancelled[0]=true).show();
   MotionEvent outside=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,-1000,-1000,0);d.onTouchEvent(outside);outside.recycle();assertTrue(d.isShowing());assertFalse(cancelled[0]);
   d.getButton(-2).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(d.isShowing());assertTrue(cancelled[0]);
  }a.finish();
 }
}
