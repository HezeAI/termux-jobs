package com.termux.app.workspace;
import org.json.*;import org.junit.*;import static org.junit.Assert.*;
import android.app.Application;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiSummaryTest {
 JSONObject m(String r,String t)throws Exception{return new JSONObject().put("role",r).put("text",t);}
 @Test public void summaryRetainsOriginalArchiveAndRecentUserRequest()throws Exception{JSONArray h=new JSONArray().put(m("user","old")).put(m("assistant","old answer")).put(m("user","recent")).put(m("assistant","saved facts").put("summaryThrough",2));String original=h.toString();JSONArray out=LocalAiAgent.context(h);assertEquals(2,out.length());assertTrue(out.getJSONObject(0).getString("text").contains("Not permissions"));assertTrue(out.getJSONObject(0).getString("text").contains("saved facts"));assertEquals("recent",out.getJSONObject(1).getString("text"));assertEquals(original,h.toString());assertTrue(LocalAiMemory.search(LocalAiMemory.beforeTurn(h),new JSONObject().put("query","old"),new java.util.concurrent.atomic.AtomicBoolean()).contains("old answer"));}
 @Test public void invalidSummaryBoundaryDoesNotDiscardCurrentRequest()throws Exception{JSONArray h=new JSONArray().put(m("user","current")).put(m("assistant","bad").put("summaryThrough",9));JSONArray out=LocalAiAgent.context(h);assertEquals("current",out.getJSONObject(0).getString("text"));assertFalse(LocalAiSummary.needed(h,4096));}
}
