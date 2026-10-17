package com.termux.app.workspace;
import android.app.Application;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiServiceContextTest {
 JSONArray request(String text)throws Exception{return new JSONArray().put(new JSONObject().put("role","user").put("text",text));}
 int count(JSONArray a,String system){int n=system.length();for(int i=0;i<a.length();i++)n+=a.optJSONObject(i).optString("text").length();return n;}
 @Test public void serviceProgressShrinksWithoutEditingRightsSchemasCorrectionOrCurrentRequest()throws Exception{
  String base="RIGHTS: read only; CURRENT_TURN_PROGRESS marker inside user instructions stays.",schema="\nSCHEMA_MUST_STAY",correction="\nCORRECTION: do not repeat completed writes; verify or stop.";StringBuilder progress=new StringBuilder("\nCURRENT_TURN_PROGRESS\n");for(int i=0;i<30;i++)progress.append(i).append(" write /test").append(new String(new char[90]).replace('\0','x')).append(" => ok\n");JSONArray h=request("my exact current task");String original=h.toString();java.util.List<String> notices=new java.util.ArrayList<>();
  LocalAiServiceContext.Prepared r=LocalAiServiceContext.prepare(h,base,progress.toString(),correction,schema,1024,256,(a,s)->{assertTrue(s.endsWith(schema));return count(a,s);},notices::add);
  assertTrue(r.reduced);assertTrue(r.system.startsWith(base));assertTrue(r.system.endsWith(correction));assertTrue(r.system.contains("excerpt"));assertEquals(256,r.reply.answer);assertEquals("my exact current task",r.reply.messages.getJSONObject(0).getString("text"));assertEquals(original,h.toString());assertTrue(notices.contains("service_reduced"));assertFalse(notices.contains("failed"));
 }
 @Test public void shorterProgressIsPreferredOverShrinkingAnswerReserve()throws Exception{LocalAiServiceContext.Prepared r=LocalAiServiceContext.prepare(request("task"),"BASE","LONG_PROGRESS\n1 write /test => ok","\nCORRECTION: finish","SCHEMA",1024,256,(a,s)->s.contains("LONG_PROGRESS")?720:500,x->{});assertTrue(r.reduced);assertEquals(256,r.reply.answer);}
 @Test public void noServiceMetadataKeepsExistingReserveAdaptation()throws Exception{LocalAiServiceContext.Prepared r=LocalAiServiceContext.prepare(request("task"),"BASE","","","SCHEMA",1024,256,(a,s)->720,x->{});assertFalse(r.reduced);assertEquals("BASE",r.system);assertEquals(240,r.reply.answer);}
 @Test public void impossibleCurrentRequestAndRequiredCorrectionAreNotCut()throws Exception{
  for(boolean oversized:new boolean[]{true,false}){JSONArray h=request(oversized?new String(new char[2000]).replace('\0','x'):"task");String before=h.toString();String correction=oversized?"warning":new String(new char[2000]).replace('\0','c');try{LocalAiServiceContext.prepare(h,"RIGHTS","progress",correction,"SCHEMA",1024,256,this::count,x->{});fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("CONTEXT_TOO_SMALL"));assertEquals(before,h.toString());}}
 }
 @Test public void applicationFeedbackIsNotMistakenForANewUserTask()throws Exception{
  JSONArray h=request("ORIGINAL_USER_REQUEST").put(new JSONObject().put("role","assistant").put("text",new String(new char[2000]).replace('\0','x'))).put(new JSONObject().put("role","assistant").put("text","recent" )).put(new JSONObject().put("role","user").put("appFeedback",true).put("text","APPLICATION_FEEDBACK"));String before=h.toString();JSONArray fitted=LocalAiContext.fit(h,"",512,128,(a,s)->{int n=0;for(int i=0;i<a.length();i++)n+=a.optJSONObject(i).optString("text").length();return n;},x->{});
  // The oldest assistant output can be removed, while the actual user task remains.
  assertEquals("ORIGINAL_USER_REQUEST",fitted.getJSONObject(0).getString("text"));assertEquals(0,LocalAiSummary.lastUser(h));assertEquals(before,h.toString());
 }

 @Test public void llamaKeepsSystemStableAndAppendsCountedTransientFeedback()throws Exception{
  JSONArray h=request("EXACT_USER_TASK");String original=h.toString();java.util.List<String> notices=new java.util.ArrayList<>();
  LocalAiServiceContext.Prepared a=LocalAiServiceContext.prepare(h,"RIGHTS_AND_SCHEMA","\n1 write done","\nCORRECTION: verify","",4096,256,this::count,notices::add,true);
  LocalAiServiceContext.Prepared b=LocalAiServiceContext.prepare(h,"RIGHTS_AND_SCHEMA","\n1 write done\n2 read done","","",4096,256,this::count,notices::add,true);
  assertEquals("RIGHTS_AND_SCHEMA",a.system);assertEquals(a.system,b.system);assertEquals(2,a.reply.messages.length());JSONObject feedback=a.reply.messages.getJSONObject(1);assertTrue(feedback.getBoolean("appFeedback"));assertFalse(LocalAiContext.userTask(feedback));assertTrue(feedback.getString("text").contains("CORRECTION: verify"));assertEquals("EXACT_USER_TASK",a.reply.messages.getJSONObject(0).getString("text"));assertEquals(count(a.reply.messages,a.system),a.reply.input);assertEquals(original,h.toString());assertTrue(notices.isEmpty());assertEquals("1 write done".length()+1,a.progressChars);
  LocalAiServiceContext.Prepared changed=LocalAiServiceContext.prepare(h,"NEW_READ_ONLY_RIGHTS","","","",4096,256,this::count,x->{},true);assertNotEquals(a.system,changed.system);assertEquals(1,changed.reply.messages.length());
 }
 @Test public void llamaProtectsRequiredFeedbackEvenWhenHistoryIsFitted()throws Exception{
  JSONArray h=request("OLDER_TASK").put(new JSONObject().put("role","assistant").put("text",new String(new char[2000]).replace('\0','x'))).put(new JSONObject().put("role","user").put("text","CURRENT_EXACT_TASK"));String original=h.toString();
  LocalAiServiceContext.Prepared a=LocalAiServiceContext.prepare(h,"RIGHTS","1 completed","CORRECTION_MUST_STAY","",512,128,this::count,x->{},true);
  assertEquals("RIGHTS",a.system);assertEquals("CURRENT_EXACT_TASK",a.reply.messages.getJSONObject(0).getString("text"));assertTrue(a.reply.messages.getJSONObject(a.reply.messages.length()-1).getString("text").contains("CORRECTION_MUST_STAY"));assertEquals(original,h.toString());assertTrue(a.reply.input<=512-128-64);
  try{LocalAiServiceContext.prepare(request("CURRENT_EXACT_TASK"),"RIGHTS","",new String(new char[2000]).replace('\0','c'),"",512,128,this::count,x->{},true);fail();}catch(IllegalStateException expected){assertTrue(LocalAiServiceContext.tooSmall(expected));}
 }
 @Test public void llamaProgressReductionDoesNotChangeSystemOrLoseExactBudget()throws Exception{
  JSONArray h=request("TASK");String progress="1 "+new String(new char[2000]).replace('\0','x');java.util.List<String> notices=new java.util.ArrayList<>();LocalAiServiceContext.Prepared a=LocalAiServiceContext.prepare(h,"RIGHTS",progress,"VERIFY","",1024,256,this::count,notices::add,true);assertTrue(a.reduced);assertEquals("RIGHTS",a.system);assertEquals(256,a.reply.answer);assertEquals(count(a.reply.messages,a.system),a.reply.input);assertTrue(a.reply.messages.getJSONObject(a.reply.messages.length()-1).getString("text").endsWith("VERIFY"));assertTrue(notices.contains("service_reduced"));assertFalse(notices.contains("begin"));
 }
 @Test public void llamaNativeRenderingKeepsToolResultBeforeAppFeedback()throws Exception{
  JSONArray h=request("TASK").put(new JSONObject().put("role","assistant").put("toolRequest",new JSONObject().put("tool","read").put("path","/test")).put("text","call")).put(new JSONObject().put("role","user").put("toolResult","read").put("text","exit=0\nvalue"));
  LocalAiServiceContext.Prepared a=LocalAiServiceContext.prepare(h,"RIGHTS","1 read done","","",4096,256,this::count,x->{},true);JSONArray rendered=LocalAiQwenProtocol.render(a.reply.messages);assertEquals(4,rendered.length());assertTrue(rendered.getJSONObject(2).getString("text").contains("tool_response"));assertTrue(rendered.getJSONObject(3).getString("text").contains("1 read done"));assertEquals("user",rendered.getJSONObject(3).getString("role"));assertEquals(3,h.length());
 }
}
