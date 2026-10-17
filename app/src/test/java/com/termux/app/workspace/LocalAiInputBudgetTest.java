package com.termux.app.workspace;
import org.json.*;import org.junit.*;import static org.junit.Assert.*;
import android.app.Application;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiInputBudgetTest {
 JSONObject m(String role,String text)throws Exception{return new JSONObject().put("role",role).put("text",text);}
 @Test public void attributionCountsCurrentUserNotToolResultAndKeepsInputUnchanged()throws Exception{
 JSONArray messages=new JSONArray().put(m("user","old")).put(m("assistant","reply")).put(m("user","hi")).put(m("user","secret file").put("toolResult","read"));String original=messages.toString();
 LocalAiContext.Counter counter=(a,s)->{int n=10+s.length();for(int i=0;i<a.length();i++)n+=a.getJSONObject(i).getString("text").length();return n;};JSONObject v=LocalAiInputBudget.measure(messages,"sys",counter,true);assertEquals(3,v.getInt("system"));assertEquals(8,v.getInt("history"));assertEquals(11,v.getInt("tools"));assertEquals(2,v.getInt("current"));assertEquals(10,v.getInt("format"));assertEquals(34,v.getInt("total"));assertEquals(original,messages.toString());assertFalse(v.toString().contains("secret"));assertTrue(LocalAiInputBudget.report(v,false).contains("exact model tokenizer"));
 }
 @Test public void breakdownSurvivesPerformanceUpdatesAndIsIncludedInCompletedPass()throws Exception{LocalAiTiming t=new LocalAiTiming(()->10L);t.begin(new WorkspaceStore.Session(),"llama");t.phase("context_preparation");JSONObject v=LocalAiInputBudget.measure(new JSONArray().put(m("user","hi")),"s",LocalAiContext::estimate,false);t.performance(v.toString());t.phase("postprocessing");t.finish("finished");assertTrue(t.report(false).contains("Input composition"));assertTrue(t.report(false).contains("estimate; LiteRT"));assertTrue(t.report(true).contains("Состав входа"));t.begin(new WorkspaceStore.Session(),"llama");assertFalse(t.report(false).contains("Input composition"));}
 @Test public void measurementFailureIsNonFatalAndDoesNotExposeErrorText()throws Exception{JSONObject v=LocalAiInputBudget.safeMeasure(new JSONArray(),"secret",(a,s)->{throw new IllegalStateException("private path");},true);assertTrue(v.getBoolean("unavailable"));assertFalse(v.toString().contains("private"));try{LocalAiInputBudget.safeMeasure(new JSONArray(),"",(a,s)->{throw new java.util.concurrent.CancellationException();},true);fail();}catch(java.util.concurrent.CancellationException expected){}}
}
