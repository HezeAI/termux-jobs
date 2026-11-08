package com.termux.app.workspace;
import android.app.*;import android.os.Looper;import android.view.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.json.*;import java.util.*;import java.util.concurrent.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class RtkModeTest {
 Activity a;WorkspaceStore.Session p;RtkMode ui;
 @Before public void setup(){a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");p=new WorkspaceStore.Session();p.environment="ubuntu";p.claudeAgent=true;p.codexEnvironment="ubuntu-ai";}
 @After public void close(){if(ui!=null)ui.close();a.finish();}
 void waitFor(java.util.function.BooleanSupplier condition)throws Exception{for(int i=0;i<100;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(condition.getAsBoolean())return;Thread.sleep(10);}fail("UI operation timed out");}
 JSONObject result(boolean installed,boolean enabled)throws Exception{return new JSONObject().put("ok",true).put("result",new JSONObject().put("installed",installed).put("enabled",enabled).put("ready",true));}
 @Test public void installationUsesOneDialogAndTicksOnlyAfterEnable()throws Exception{
  List<String> ops=Collections.synchronizedList(new ArrayList<>());
  ui=new RtkMode(a,p,(op,token,events)->{ops.add(op);return result(!op.equals("begin"),op.equals("enable"));});ui.open();AlertDialog d=ui.window;ui.onDetachedFromWindow();assertFalse(ui.closed);assertTrue(d.isShowing());waitFor(()->ui.action.getVisibility()==View.VISIBLE);assertFalse(ui.toggle.isChecked());assertEquals("Установить",ui.action.getText().toString());ui.action.performClick();waitFor(()->ui.toggle.isChecked());assertNull(ui.window);assertEquals(Arrays.asList("begin","install","enable"),ops);assertFalse(d.isShowing());
 }
 @Test public void cancellationDoesNotWaitBehindDownloadAndLateCompletionCannotEnable()throws Exception{
  CountDownLatch downloading=new CountDownLatch(1),release=new CountDownLatch(1),cancelled=new CountDownLatch(1);List<String> ops=Collections.synchronizedList(new ArrayList<>());
  ui=new RtkMode(a,p,(op,token,events)->{ops.add(op);if(op.equals("install")){downloading.countDown();release.await(3,TimeUnit.SECONDS);}if(op.equals("cancel"))cancelled.countDown();return result(false,false);});ui.open();waitFor(()->ui.action.getVisibility()==View.VISIBLE);ui.action.performClick();assertTrue(downloading.await(1,TimeUnit.SECONDS));ui.cancel();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(cancelled.await(1,TimeUnit.SECONDS));release.countDown();assertTrue(ui.worker.awaitTermination(1,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();waitFor(()->!ui.busy);assertFalse(ui.toggle.isChecked());assertFalse(ops.contains("enable"));
 }
 @Test public void statsAreBytesAndDoNotClaimSubscriptionSavings()throws Exception{
  ui=new RtkMode(a,p,(op,t,e)->result(true,false));ui.present(new JSONObject().put("statistics",new JSONObject().put("version",2).put("basic",new JSONObject().put("inputBytes",1000).put("outputBytes",600).put("commands",1).put("comparedCommands",1))));assertTrue(ui.statistics.getText().toString().contains("40,0%"));assertTrue(ui.statistics.getText().toString().contains("токенов"));assertEquals(View.GONE,ui.details.getVisibility());ui.detailsToggle.performClick();assertEquals(View.VISIBLE,ui.details.getVisibility());assertTrue(ui.details.getText().toString().contains("байт"));ui.detailsToggle.performClick();assertEquals(View.GONE,ui.details.getVisibility());assertFalse(ui.statistics.getText().toString().contains("подписк"));
 }
 @Test public void dialogButtonsWrapCenterAndGrowForLargeText(){
  android.widget.LinearLayout body=new android.widget.LinearLayout(a);body.setOrientation(1);
  android.widget.TextView button=RtkMode.dialogButton(body,"Отмена");
  int width=WorkspaceDialogs.dp(a,260),height=View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
  body.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),height);
  body.layout(0,0,width,body.getMeasuredHeight());
  assertTrue(button.getWidth()<width);assertEquals((width-button.getWidth())/2,button.getLeft());
  assertTrue(button.getHeight()>=WorkspaceDialogs.dp(a,48));
  assertEquals(WorkspaceDialogs.dp(a,16),button.getPaddingLeft());
  assertEquals(WorkspaceDialogs.dp(a,8),button.getPaddingTop());
  // Explicit multiline input: device font wrapping is checked separately on a phone.
  button.setTextSize(30);button.setText("Включить RTK\nштатный");
  body.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),height);
  body.layout(0,0,width,body.getMeasuredHeight());
  assertTrue(button.getLineCount()>1);assertTrue(button.getWidth()<=width);
  assertNull(button.getEllipsize());
  assertEquals(Integer.MAX_VALUE,button.getMaxLines());
  assertTrue(button.getHeight()>=button.getLayout().getHeight()+button.getCompoundPaddingTop()+button.getCompoundPaddingBottom());
 }

 @Test public void legacyUpdateWarnsBeforeInstallAndDoesNotChangeSelectedMode()throws Exception{
  List<String> ops=Collections.synchronizedList(new ArrayList<>());
  ui=new RtkMode(a,p,(op,t,e)->{ops.add(op);return new JSONObject().put("ok",true).put("result",new JSONObject().put("installed",op.equals("status")).put("legacyInstalled",true).put("activeLegacy",true).put("enabled",true).put("desired",true).put("mode","advanced").put("updateAvailable",!op.equals("status")));});
  ui.present(new JSONObject().put("legacyInstalled",true).put("activeLegacy",true).put("enabled",true).put("desired",true).put("mode","advanced").put("updateAvailable",true));
  assertEquals(View.VISIBLE,ui.update.getVisibility());ui.update.performClick();waitFor(()->ui.action.getVisibility()==View.VISIBLE);
  assertTrue(ui.stage.getText().toString().contains("перезапуск"));assertEquals("Обновить",ui.action.getText().toString());ui.action.performClick();waitFor(()->ui.window==null&&ops.contains("status"));
  assertFalse(ops.contains("enable"));assertTrue(ui.advanced.isChecked());assertFalse(ui.toggle.isChecked());
 }

}
