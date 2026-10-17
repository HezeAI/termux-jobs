package com.termux.app.workspace;
import android.app.Application;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;import java.util.concurrent.atomic.AtomicBoolean;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiGemmaRecoveryTest {
 static final String RAW="<|tool_call>call:write{path:<|\"|>Python_Installation_Guide.txt<|\"|>,text:<|\"|># Инструкция\nВАЖНО:";
 LocalAiAgentTest.Fixture fixture(String question)throws Exception{LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localProtocol=LocalAiLiteRt.NATIVE;f.history=new JSONArray().put(new JSONObject().put("role","user").put("text",question));return f;}
 @Test public void ordinaryWriteRequestAndNewCompletionClassification()throws Exception{
  for(String q:new String[]{"напиши инструкцию и положи в загрузки","save this guide","copy this file","move it"})assertTrue(q,LocalAiAgent.actionRequested(new JSONArray().put(new JSONObject().put("role","user").put("text",q))));
  assertTrue(LocalAiLiteRt.leakedCall(RAW));assertEquals("write",LocalAiLiteRt.leakedName(RAW));assertEquals("unparsed_native_tool",LocalAiLiteRt.completion(true,"",RAW));assertEquals("sdk_done",LocalAiLiteRt.completion(false,"",RAW));assertEquals("native_tool",LocalAiLiteRt.completion(true,"{}",RAW));
  for(String prose:new String[]{"Example: "+RAW,"```text\n"+RAW+"\n```","The marker <|tool_call> is literal","<tool_call><function=write>"})assertFalse(LocalAiLiteRt.leakedCall(prose));
 }
 @Test public void leakedOutputIsHiddenAsPendingNotConvertedToRequest(){LocalAiToolText.Preview p=LocalAiToolText.preview(RAW);assertTrue(p.pending);assertEquals("",p.text);assertNull(p.request);assertNull(LocalAiToolText.preview(RAW+"<tool_call|>").request);}
 @Test public void malformedCallRecoversOnlyThroughFreshStructuredResponse()throws Exception{
  LocalAiAgentTest.Fixture f=fixture("напиши инструкцию и положи в загрузки");f.p.localConfirmActions=true;int[] pass={0};
  String result=LocalAiAgent.run(f.p,f.history,f.cancelled,(h,sys,out)->{int n=pass[0]++;if(n==0)return new LocalAiAgent.Generation(RAW,"unparsed_native_tool");if(n==1){assertTrue(h.toString().contains("UNPARSED_GEMMA_TOOL_CALL"));assertTrue(h.toString().contains("COMPLETE"));assertFalse(h.toString().contains("<|tool_call>"));for(int k=0;k<h.length();k++)assertFalse(h.getJSONObject(k).has("toolResult"));assertEquals("напиши инструкцию и положи в загрузки",h.getJSONObject(0).getString("text"));return new LocalAiAgent.Generation("{\"tool\":\"write\",\"path\":\"/sdcard/Download/guide.txt\",\"text\":\"short guide\"}","native_tool");}return new LocalAiAgent.Generation("Done","sdk_done");},r->{f.calls.add(r.toString());return "exit=0\nwritten";},r->{f.approvals++;return true;},f,t->{});
  assertEquals("Done",result);assertEquals(3,pass[0]);assertEquals(1,f.calls.size());assertEquals(1,f.approvals);assertEquals("/sdcard/Download/guide.txt",new JSONObject(f.calls.get(0)).getString("path"));
 }
 @Test public void persistentLeakStopsAfterTwoRetriesWithoutAnyExecution()throws Exception{LocalAiAgentTest.Fixture f=fixture("напиши инструкцию и положи в загрузки");int[] passes={0};String result=LocalAiAgent.run(f.p,f.history,f.cancelled,(h,s,o)->{passes[0]++;return new LocalAiAgent.Generation(RAW,"sdk_done");},r->{fail("raw fragments must never execute");return "";},r->{fail();return false;},f,t->{});assertEquals(3,passes[0]);assertTrue(result.contains("after two retries"));assertFalse(result.contains("<|tool_call>"));}
 @Test public void noToolOrExampleRequestsNeverTriggerRecoveryExecution()throws Exception{
  for(String question:new String[]{"покажи пример вызова write","show an example","что это значит?"}){LocalAiAgentTest.Fixture f=fixture(question);f.answers.add(RAW);String result=f.run();assertEquals(1,f.systems.size());assertTrue(f.calls.isEmpty());assertTrue(result.contains("No action was executed"));}
  LocalAiAgentTest.Fixture f=fixture("напиши файл");f.p.localAccess="chat";f.answers.add(RAW);f.run();assertEquals(1,f.systems.size());assertTrue(f.calls.isEmpty());
 }
 @Test public void recoveryDoesNotBypassReadModeOrApproval()throws Exception{
  LocalAiAgentTest.Fixture f=fixture("напиши файл");f.p.localAccess="read";int[] pass={0};try{LocalAiAgent.run(f.p,f.history,f.cancelled,(h,s,o)->new LocalAiAgent.Generation(pass[0]++==0?RAW:"{\"tool\":\"write\",\"path\":\"/test\",\"text\":\"x\"}",pass[0]==1?"sdk_done":"native_tool"),r->{fail();return "";},r->{fail();return true;},f,t->{});fail();}catch(SecurityException expected){}
 }
 @Test public void cancellationDuringRecoveryDoesNotStartAnotherGeneration()throws Exception{
  LocalAiAgentTest.Fixture f=fixture("напиши файл");int[] pass={0};try{LocalAiAgent.run(f.p,f.history,f.cancelled,(h,s,o)->{pass[0]++;return new LocalAiAgent.Generation(RAW,"sdk_done");},r->{fail();return "";},r->false,new LocalAiAgent.Events(){public void save(){f.cancelled.set(true);}public void phase(String p,String d){}},t->{});fail();}catch(java.util.concurrent.CancellationException expected){}assertEquals(1,pass[0]);
 }
 @Test public void ordinaryTextAndQwenStayOrdinary()throws Exception{
  LocalAiAgentTest.Fixture f=fixture("напиши короткий текст");f.answers.add("Короткий текст.");assertEquals("Короткий текст.",f.run());assertEquals(1,f.systems.size());f=fixture("напиши текст");f.p.localProtocol="qwen-native";f.answers.add(RAW);assertEquals(RAW,f.run());assertEquals(1,f.systems.size());
 }
 @Test public void canonicalJsonWithoutSdkProvenanceCannotExecuteInNativeMode()throws Exception{
  LocalAiAgentTest.Fixture f=fixture("напиши файл");String json="{\"tool\":\"write\",\"path\":\"/test\",\"text\":\"x\"}";f.answers.add(json);f.answers.add(json);f.answers.add(json);assertTrue(f.run().contains("after two retries"));assertTrue(f.calls.isEmpty());
 }
 @Test public void errorFeedbackRecognizesGemmaFunctionForDiagnosisOnly()throws Exception{LocalAiAgentTest.Fixture f=fixture("напиши файл");JSONObject feedback=new JSONObject(LocalAiToolErrors.feedback(f.p,RAW,new IllegalArgumentException("UNPARSED")));assertEquals("write",feedback.getString("toolName"));assertFalse(feedback.getBoolean("executed"));assertTrue(feedback.getString("schema").contains("Function schema"));assertFalse(LocalAiToolErrors.forModel(feedback.toString()).contains("originalCall"));}
}
