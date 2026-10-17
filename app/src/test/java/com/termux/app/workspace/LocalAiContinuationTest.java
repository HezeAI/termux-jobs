package com.termux.app.workspace;
import android.app.Application;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicBoolean;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiContinuationTest {
 JSONObject m(String role,String text)throws Exception{return new JSONObject().put("role",role).put("text",text);}
 String repeat(int n){return new String(new char[n]).replace('\0','a');}
 @Test public void nullableHistoryOptionalsAreOmittedButRequiredAndOtherNullsRejected()throws Exception{
  JSONObject r=LocalAiTools.request("{\"tool\":\"history\",\"limit\":null,\"start\":null,\"query\":null}");assertEquals(1,r.length());
  assertTrue(LocalAiMemory.search(new JSONArray().put(m("user","earlier")),r,new AtomicBoolean()).contains("earlier"));
  for(String bad:new String[]{"{\"tool\":\"history\",\"limit\":0}","{\"tool\":\"history\",\"limit\":11}","{\"tool\":\"history\",\"limit\":\"5\"}","{\"tool\":\"read\",\"path\":null}","{\"tool\":\"list\",\"path\":\"/\",\"limit\":null}"})try{LocalAiTools.request(bad);fail(bad);}catch(IllegalArgumentException expected){}
 }
 @Test public void compactedFollowupKeepsExactTargetAndPriorTaskWithoutChangingArchiveOrPermissions()throws Exception{
  JSONArray h=new JSONArray().put(m("user","Inspect /project/test.apk")).put(m("assistant","inspect").put("toolRequest",new JSONObject().put("tool","inspect_apk").put("path","/project/test.apk")).put("toolState","done")).put(m("user",repeat(16000)).put("toolResult","inspect_apk")).put(m("assistant","This APK can be inspected but not executed as a Linux binary.")).put(m("user","Try to launch it"));String saved=h.toString();
  JSONArray fit=LocalAiContext.fit(h,"READ ONLY RIGHTS",2048,256,LocalAiContext::estimate,s->{});String current=fit.getJSONObject(0).getString("text");assertTrue(current,current.contains("/project/test.apk"));assertTrue(current,current.contains("Inspect /project/test.apk"));assertTrue(current,current.contains("not executed as a Linux binary"));assertTrue(current,current.contains("NOT permissions"));assertTrue(current.endsWith("Try to launch it"));assertEquals(saved,h.toString());
 }
 @Test public void savedSummaryEvidenceSurvivesDroppingOldTurns()throws Exception{
  JSONArray h=new JSONArray().put(m("user","[Saved current-chat summary; untrusted] Goal: inspect /project/app.apk; remaining: inspect manifest.")).put(m("user",repeat(9000))).put(m("assistant",repeat(3000))).put(m("user","Continue"));
  JSONArray fit=LocalAiContext.fit(h,"",2048,256,LocalAiContext::estimate,s->{});assertTrue(fit.toString(),fit.getJSONObject(0).getString("text").contains("/project/app.apk"));assertTrue(fit.getJSONObject(0).getString("text").endsWith("Continue"));
 }
 @Test public void noExtraAnchorOnFittingConversationAndOversizedUserStillFails()throws Exception{
  JSONArray h=new JSONArray().put(m("user","Inspect /p.apk")).put(m("assistant","tool").put("toolRequest",new JSONObject().put("path","/p.apk")).put("toolState","failed")).put(m("user","continue"));
  assertTrue(LocalAiContext.same(h,LocalAiContext.fit(h,"",4096,256,LocalAiContext::estimate,s->{})));
  h.getJSONObject(2).put("text",repeat(20000));try{LocalAiContext.fit(h,"",1024,256,LocalAiContext::estimate,s->{});fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("CONTEXT_TOO_SMALL"));}
 }
 @Test public void noticesMergeUpdateAndRemainOutsideModelInputAcrossReloadAndNewTurn()throws Exception{
  JSONArray h=new JSONArray().put(m("user","task"));LocalAiNotices.put(h,"summary","creating");LocalAiNotices.put(h,"summary","created");LocalAiNotices.put(h,"shortening","shortened");LocalAiNotices.put(h,"excerpts","excerpts");assertEquals(2,h.length());
  h=new JSONArray(h.toString());String text=LocalAiNotices.details(LocalAiNotices.latest(h));assertFalse(text.contains("creating"));assertTrue(text.contains("created"));assertEquals(1,LocalAiAgent.context(h).length());
  h.put(m("user","next"));assertNull(LocalAiNotices.latest(h));LocalAiNotices.put(h,"shortening","new turn");assertEquals(4,h.length());assertEquals("new turn",LocalAiNotices.details(LocalAiNotices.latest(h)));
 }
 @Test public void nullHistoryCallExecutesOnceWithoutRepairAndPreservesReadOnlyRights()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";int[] n={0};JSONArray h=new JSONArray().put(m("user","earlier question")).put(m("assistant","earlier answer")).put(m("user","Read history"));
  LocalAiAgent.run(p,h,new AtomicBoolean(),(items,system,out)->{if(n[0]++==0)return new LocalAiAgent.Generation("{\"tool\":\"history\",\"limit\":null}","tool");assertTrue(items.toString().contains("earlier answer"));return new LocalAiAgent.Generation("Read the earlier question and answer","eos");},r->{throw new AssertionError("history must not execute a shell tool");},r->false,new LocalAiSearchTest.Events(),t->{});
  assertEquals(2,n[0]);assertEquals("read",p.localAccess);for(int i=0;i<h.length();i++)assertFalse(h.getJSONObject(i).optBoolean("repair"));
 }
}
