package com.termux.app.workspace;
import android.app.*;import android.os.Looper;import android.view.*;import android.widget.*;import com.termux.R;import com.termux.app.TermuxActivity;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentChatCatalogUiTest {
 TermuxActivity a;AgentChatCatalogUi picker;AtomicInteger calls=new AtomicInteger(),closes=new AtomicInteger();List<String> selected=new ArrayList<>();
 @Before public void setup(){a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"en");}
 @After public void end(){if(picker!=null)picker.dialog.dismiss();a.finish();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 void idle()throws Exception{for(int n=0;n<100;n++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(!picker.busy)return;Thread.sleep(5);}fail("catalog timeout");}
 JSONObject page(String cursor,String... ids)throws Exception{JSONArray rows=new JSONArray();for(String id:ids)rows.put(new JSONObject().put("id",id).put("name","same title").put("directory","/fixture"));return new JSONObject().put("sessions",rows).put("cursor",cursor);}
 @Test public void failureRetryPagingAndSelectionRetainIdsWithoutDuplicates()throws Exception{
  picker=new AgentChatCatalogUi(a,new AgentChatCatalogUi.Backend(){public JSONObject page(String cursor)throws Exception{int n=calls.incrementAndGet();if(n==1)throw new Exception("fixture");return n==2?AgentChatCatalogUiTest.this.page("next","one","two"):AgentChatCatalogUiTest.this.page("","two","three");}public void close(){closes.incrementAndGet();}},id->id.equals("two"),row->selected.add(row.optString("id")));
  picker.show("Claude");idle();assertTrue(picker.status.getText().toString().startsWith("Could not load"));assertEquals("Retry",picker.more.getText().toString());assertTrue(selected.isEmpty());picker.more.performClick();idle();assertEquals(2,picker.entries.size());assertFalse(picker.rows.findViewWithTag("chat-catalog-two").isEnabled());picker.more.performClick();idle();assertEquals(3,picker.entries.size());assertEquals(View.GONE,picker.more.getVisibility());picker.rows.findViewWithTag("chat-catalog-three").performClick();assertEquals(Collections.singletonList("three"),selected);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,closes.get());
 }
 @Test public void cancellationRejectsLatePageAndNeverCallsChoice()throws Exception{
  CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
  picker=new AgentChatCatalogUi(a,new AgentChatCatalogUi.Backend(){public JSONObject page(String cursor)throws Exception{started.countDown();release.await();return AgentChatCatalogUiTest.this.page("","late");}public void close(){closes.incrementAndGet();release.countDown();}},id->false,row->selected.add(row.optString("id")));
  picker.show("Gemini");assertTrue(started.await(2,TimeUnit.SECONDS));picker.dialog.cancel();Thread.sleep(30);Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(selected.isEmpty());assertTrue(picker.entries.isEmpty());assertEquals(1,closes.get());
 }
 @Test public void partialEmptyPageIsNotNoSavedChats()throws Exception{
  picker=new AgentChatCatalogUi(a,new AgentChatCatalogUi.Backend(){public JSONObject page(String c)throws Exception{return AgentChatCatalogUiTest.this.page(c.isEmpty()?"next":"");}public void close(){}},id->false,row->fail());picker.show("Gemini");idle();assertEquals(View.VISIBLE,picker.more.getVisibility());assertTrue(picker.status.getText().toString().contains("this page"));picker.more.performClick();idle();assertEquals("No saved chats",picker.status.getText().toString());
 }
}
