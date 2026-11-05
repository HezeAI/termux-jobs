package com.termux.app.workspace;
import android.app.*;import android.os.Looper;import android.view.View;import android.widget.*;
import com.termux.R;import com.termux.app.TermuxActivity;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class CodexContextUiTest {
 TermuxActivity activity;CodexContextUi ui;WorkspaceStore.Session profile;JSONObject current;int starts;String sentOwner,sentThread;boolean reject,disconnect;
 @Before public void before()throws Exception{activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"ru");profile=new WorkspaceStore.Session();profile.environment="codex";profile.codexSocketId="run-a";profile.codexThread="chat-a";profile.id="owner-a";current=value("chat-a",true);ui=new CodexContextUi(activity,profile,this::request);}
 @After public void after(){ui.close();activity.finish();}
 JSONObject request(WorkspaceStore.Session owner,JSONObject q)throws Exception{
  if(disconnect)throw new java.io.IOException("Synthetic disconnect");
  if(q.getString("op").equals("chat-compact")){starts++;sentOwner=owner.id+":"+owner.codexSocketId;sentThread=q.getString("session");current=value("chat-a",true);current.getJSONObject("context").put("canCompact",false).put("compaction",new JSONObject().put("phase",reject?"failed":"running").put("error",reject?"Synthetic rejection":""));return new JSONObject().put("ok",!reject).put("accepted",!reject).put("error",reject?"Synthetic rejection":"");}return current;
 }
 JSONObject value(String thread,boolean ready)throws Exception{return new JSONObject().put("ok",true).put("thread",thread).put("connected",ready).put("busy",false).put("context",new JSONObject().put("compactSupported",true).put("canCompact",ready).put("stale",false).put("usage",new JSONObject().put("used",128000).put("capacity",256000)));}
 void idle()throws Exception{for(int n=0;n<200;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!ui.loading&&!ui.submitting)return;Thread.sleep(10);}fail("Context control timed out");}
 AlertDialog confirm(){ui.compact.performClick();return org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();}
 @Test public void unknownIsNotZeroAndFreshCachedTokensAreNotDiscounted()throws Exception{
  assertEquals("Нет данных",ui.usage.getText().toString());assertEquals(View.GONE,ui.bar.getVisibility());assertFalse(ui.compact.isEnabled());
  ui.apply(current);assertTrue(ui.usage.getText().toString().contains("50%"));assertTrue(ui.usage.getText().toString().contains("128 000 / 256 000"));assertEquals(50,ui.bar.getProgress());assertTrue(ui.compact.isEnabled());
  current.getJSONObject("context").put("stale",true);ui.apply(current);assertTrue(ui.note.getText().toString().contains("Устаревшие"));
  current.getJSONObject("context").put("usage",JSONObject.NULL);ui.apply(current);assertEquals("Нет данных",ui.usage.getText().toString());assertEquals(View.GONE,ui.bar.getVisibility());
 }
 @Test public void cancelWritesNothingAndConfirmationCannotRetargetOrRepeat()throws Exception{
  ui.apply(current);AlertDialog dialog=confirm();assertTrue(((TextView)dialog.findViewById(android.R.id.message)).getText().toString().contains("часть деталей может не попасть"));dialog.getButton(-2).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,starts);assertTrue(ui.compact.isEnabled());
  dialog=confirm();profile.id="other-owner";profile.codexSocketId="other-run";profile.codexThread="other-chat";dialog.getButton(-1).performClick();dialog.getButton(-1).performClick();idle();assertEquals(1,starts);assertEquals("owner-a:run-a",sentOwner);assertEquals("chat-a",sentThread);assertEquals("Сокращение…",ui.compact.getText().toString());assertFalse(ui.compact.isEnabled());
  ui.apply(value("foreign",true));assertFalse(ui.compact.isEnabled());assertEquals("Нет данных",ui.usage.getText().toString());
 }
 @Test public void onlyNativeCompletionUpdatesOperationAndNewUsageWithoutArtificialZero()throws Exception{
  ui.apply(current);confirm().getButton(-1).performClick();idle();assertTrue(ui.result.getText().toString().contains("Сокращение"));assertTrue(ui.usage.getText().toString().contains("128 000"));
  current=value("chat-a",true);current.getJSONObject("context").put("compaction",new JSONObject().put("phase","completed")).put("stale",true);ui.apply(current);assertTrue(ui.compact.isEnabled());assertTrue(ui.result.getText().toString().contains("сокращён"));assertTrue(ui.note.getText().toString().contains("Устаревшие"));
  current.getJSONObject("context").getJSONObject("usage").put("used",32000);current.getJSONObject("context").put("stale",false);ui.apply(current);assertEquals(13,ui.bar.getProgress());assertFalse(ui.note.getText().toString().contains("Устаревшие"));
  current.getJSONObject("context").put("usage",JSONObject.NULL);ui.apply(current);assertEquals("Нет данных",ui.usage.getText().toString());current.getJSONObject("context").put("usage",new JSONObject().put("used",50000).put("capacity",100000));ui.apply(current);assertEquals(50,ui.bar.getProgress());
 }
 @Test public void failureDisconnectReopenBusyAndEnglishRemainExplicit()throws Exception{
  WorkspaceLanguage.select(activity,"en");ui.apply(current);reject=true;confirm().getButton(-1).performClick();idle();assertTrue(ui.result.getText().toString().contains("Synthetic rejection"));
  current=value("chat-a",true);ui.apply(current);assertTrue(ui.compact.isEnabled());current.put("busy",true);ui.apply(current);assertFalse(ui.compact.isEnabled());current.put("busy",false);ui.apply(current);
  disconnect=true;ui.refresh();idle();assertFalse(ui.compact.isEnabled());assertTrue(ui.result.getText().toString().contains("unavailable"));assertTrue(ui.note.getText().toString().contains("Stale"));
  ui.close();ui=new CodexContextUi(activity,profile,this::request);JSONObject context=current.getJSONObject("context");context.put("canCompact",false).put("compaction",new JSONObject().put("phase","running"));ui.apply(current);assertEquals("Compacting…",ui.compact.getText().toString());assertFalse(ui.compact.isEnabled());assertEquals(1,starts);
  context.put("compaction",new JSONObject().put("phase","unknown").put("error","Synthetic disconnect"));ui.apply(current);assertFalse(ui.compact.isEnabled());assertTrue(ui.result.getText().toString().contains("unconfirmed"));
 }
 @Test public void closingConfirmationDoesNotStartOrCancelAnOperation()throws Exception{ui.apply(current);AlertDialog dialog=confirm();ui.close();dialog.getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,starts);}
 @Test public void detachedStatusStopsPollingWithoutSendingCancellation()throws Exception{ui.apply(current);confirm().getButton(-1).performClick();idle();assertEquals(1,starts);assertTrue(current.getJSONObject("context").getJSONObject("compaction").getString("phase").equals("running"));ui.onDetachedFromWindow();assertTrue(ui.closed);assertTrue(ui.worker.isShutdown());assertEquals(1,starts);}
}
