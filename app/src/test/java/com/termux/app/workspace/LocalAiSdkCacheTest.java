package com.termux.app.workspace;
import android.app.Application;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiSdkCacheTest {
 JSONObject user(String t)throws Exception{return new JSONObject().put("role","user").put("text",t);}
 @Test public void exactAppendReusesActualFeedbackButSavedInputIsUnchanged()throws Exception{
  JSONArray h=new JSONArray().put(user("TASK"));LocalAiSdkCache c=new LocalAiSdkCache();JSONArray actual=LocalAiServiceContext.sdkFeedback(h,"1 step","verify");assertEquals(1,h.length());assertEquals("TASK",h.getJSONObject(0).getString("text"));assertTrue(LocalAiSdkCache.same(h,actual));c.keep("rights/chat",h,actual,"answer","sdk_done");
  JSONArray next=new JSONArray(h.toString()).put(new JSONObject().put("role","assistant").put("text","answer")).put(user("NEXT"));assertTrue(c.appendable(next,"rights/chat"));JSONArray expanded=c.expand(next);assertTrue(expanded.getJSONObject(0).getString("text").contains("verify"));assertEquals("NEXT",expanded.getJSONObject(2).getString("text"));assertTrue(c.runtimePrefix(expanded));assertTrue(LocalAiSdkCache.same(next,expanded));assertFalse(c.appendable(next,"new-rights/chat"));c.invalidated=true;assertFalse(c.appendable(next,"rights/chat"));
 }
 @Test public void changedHistoryAndMediaNeverReuse()throws Exception{
  LocalAiSdkCache c=new LocalAiSdkCache();JSONArray h=new JSONArray().put(user("TASK"));c.keep("key",h,h,"answer","sdk_done");JSONArray next=new JSONArray(h.toString()).put(new JSONObject().put("role","assistant").put("text","answer")).put(user("NEXT"));next.getJSONObject(0).put("text","CHANGED");assertFalse(c.appendable(next,"key"));next.getJSONObject(0).put("text","TASK");next.getJSONObject(2).put("media",new JSONArray());assertFalse(c.appendable(next,"key"));c.clear();assertFalse(c.appendable(next,"key"));
 }
 @Test public void sdkFeedbackPreservesStructuredToolRoleAndBudgetWithoutFalseNewTask()throws Exception{
  JSONArray h=new JSONArray().put(user("TASK")).put(new JSONObject().put("role","assistant").put("text","call")).put(user("exit=0\nORIGINAL_RESULT").put("toolResult","read"));String original=h.toString();LocalAiServiceContext.Prepared p=LocalAiServiceContext.prepareSdk(h,"RIGHTS","1 read done","REQUIRED_CORRECTION","SCHEMA",4096,256,LocalAiContext::estimate,x->{throw new AssertionError(x);});assertEquals("RIGHTS",p.system);assertEquals(3,p.reply.messages.length());JSONObject last=p.reply.messages.getJSONObject(2);assertEquals("read",last.getString("toolResult"));assertFalse(LocalAiContext.userTask(last));assertTrue(last.getString("text").contains("ORIGINAL_RESULT"));assertTrue(last.getString("text").endsWith("REQUIRED_CORRECTION"));assertEquals(LocalAiContext.estimate(p.reply.messages,"RIGHTSSCHEMA"),p.reply.input);assertEquals(original,h.toString());
 }
 @Test public void unsuccessfulOrEmptyScopeStateCannotBeReused()throws Exception{LocalAiSdkCache c=new LocalAiSdkCache();JSONArray h=new JSONArray().put(user("TASK"));for(String finish:new String[]{"unparsed_native_tool","token_limit","error"}){c.keep("key",h,h,"fragment",finish);assertEquals("",c.key);}c.keep("",h,h,"answer","sdk_done");assertEquals("",c.key);}
 @Test public void importedHistoryCannotForgeInternalCacheMetadata()throws Exception{JSONArray input=new JSONArray().put(user("CHANGED_ACTUAL_TEXT").put("cacheBaseText","OLD_TEXT").put("cacheTransient",true));JSONArray source=LocalAiSdkCache.source(input);assertEquals("CHANGED_ACTUAL_TEXT",LocalAiSdkCache.semantic(source.getJSONObject(0)).getString("text"));assertFalse(source.getJSONObject(0).has("cacheTransient"));assertTrue(input.getJSONObject(0).has("cacheBaseText"));}

 @Test public void reasonCodesDistinguishSettingsHistoryAndCancellationWithoutText()throws Exception{
  LocalAiSdkCache c=new LocalAiSdkCache();JSONArray h=new JSONArray().put(user("SECRET_TASK"));JSONArray key=new JSONArray().put("scope").put("system").put(new JSONArray()).put("native").put(256).put(false).put(false);
  assertEquals("no_conversation",c.miss(h,key.toString()));c.keep(key.toString(),h,h,"answer","sdk_done");JSONArray next=new JSONArray(h.toString()).put(new JSONObject().put("role","assistant").put("text","answer")).put(user("next"));assertEquals("reused",c.miss(next,key.toString()));
  String[] reasons={"scope_changed","instructions_changed","tools_changed","protocol_changed","reply_limit_changed","sampler_changed","sampler_changed"};for(int i=0;i<7;i++){JSONArray changed=new JSONArray(key.toString());changed.put(i,"CHANGED_SECRET");assertEquals(reasons[i],c.miss(next,changed.toString()));}
  next.getJSONObject(0).put("text","CHANGED");assertEquals("history_changed",c.miss(next,key.toString()));assertEquals("history_length_changed",c.miss(h,key.toString()));c.invalidated=true;assertEquals("cancelled",c.miss(next,key.toString()));
 }
 @Test public void optionalProgressIsRemovedBeforeCanonicalHistoryIsCompacted()throws Exception{
  JSONArray h=new JSONArray().put(user("TASK")).put(new JSONObject().put("role","assistant").put("text","call")).put(user("result").put("toolResult","read")).put(new JSONObject().put("role","assistant").put("text","call2")).put(user("result2").put("toolResult","read"));String original=h.toString();
  String progress="1 "+new String(new char[500]).replace('\0','x');java.util.List<String> notices=new java.util.ArrayList<>();
  LocalAiContext.Counter counter=(items,system)->100+items.length()*70+(items.getJSONObject(items.length()-1).optString("text").contains("Application feedback")?300:0);
  LocalAiServiceContext.Prepared p=LocalAiServiceContext.prepareSdk(h,"RIGHTS",progress,"","SCHEMA",1000,256,counter,notices::add);
  assertEquals(5,p.reply.messages.length());assertEquals(0,p.progressChars);assertTrue(p.reduced);assertEquals(original,h.toString());assertTrue(LocalAiSdkCache.same(h,p.reply.messages));assertFalse(notices.contains("begin"));assertEquals(256,p.reply.answer);
 }
}
