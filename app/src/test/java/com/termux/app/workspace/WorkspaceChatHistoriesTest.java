package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceChatHistoriesTest {


 @Test public void codexPreviewParsesUserAndAgentMessagesWithoutExecutingAnything()throws Exception{JSONObject t=new JSONObject("{\"turns\":[{\"items\":[{\"type\":\"userMessage\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]},{\"type\":\"agentMessage\",\"text\":\"world\"}]}]}");String p=WorkspaceChatHistories.codexTranscript(t);assertTrue(p.contains("You: hello"));assertTrue(p.contains("AI: world"));try{WorkspaceChatHistories.codexTranscript(new JSONObject());fail();}catch(IOException expected){}}
 @Test public void codexDeleteUsesExactNativeMethodWithoutArchiveFallback()throws Exception{java.util.List<String> calls=new java.util.ArrayList<>();String id="01900000-0000-7000-8000-000000000001";try{WorkspaceChatHistories.deleteCodex(id,(method,params)->{calls.add(method);assertEquals(id,params.getString("threadId"));throw new IOException("unsupported");});fail();}catch(IOException expected){}assertEquals(java.util.Arrays.asList("thread/delete"),calls);try{WorkspaceChatHistories.deleteCodex("../../auth",(m,p)->{fail();return new JSONObject();});fail();}catch(IllegalArgumentException expected){}}
 @Test public void cleanupHistoryDialogsRequireExplicitDismissalAndFreshConsent(){
  android.app.Activity a=Robolectric.buildActivity(android.app.Activity.class).setup().get();a.setTheme(com.termux.R.style.Theme_Termux_Black);WorkspaceStore store=new WorkspaceStore(a,m->{});
  WorkspaceChatHistories browser=new WorkspaceChatHistories(a,store,r->{},e->false,()->{},true);browser.show();browser.dialog.onBackPressed();assertTrue(browser.dialog.isShowing());browser.dialog.onTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_OUTSIDE,-1,-1,0));assertTrue(browser.dialog.isShowing());browser.busy=false;
  WorkspaceChatHistories.Entry entry=new WorkspaceChatHistories.Entry("codex","ubuntu","01900000-0000-7000-8000-000000000001","test","/root",1);browser.confirmDelete(entry);android.app.AlertDialog consent=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertFalse(consent.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled());consent.onBackPressed();assertTrue(consent.isShowing());consent.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();browser.dialog.dismiss();store.close();
 }
}
