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
}
