package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiGpuErrorsTest {
 @Test public void computeDefaultsCopiesAndPersistsWithoutChangingCpuSettings()throws Exception{
  WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,s->{});try{store.sessions.clear();WorkspaceStore.Session s=new WorkspaceStore.Session();assertEquals("cpu",s.localCompute);s.localCompute="gpu";s.localThreads=8;s.localCpu="compatible";s.localLocation="ubuntu";store.sessions.add(s);store.decode(store.encode().toString());WorkspaceStore.Session r=store.sessions.get(0).copy();assertEquals("gpu",r.localCompute);assertEquals(8,r.localThreads);assertEquals("compatible",r.localCpu);assertEquals("ubuntu",r.localLocation);}finally{store.close();}
 }
 @Test public void computeModeRejectsUnknownValuesAndUnsupportedModels()throws Exception{
  for(String s:new String[]{"", "auto", "npu", "GPU"})try{LocalAiEngine.validateCompute(s);fail();}catch(IllegalArgumentException expected){}
  assertTrue(LocalAiEngine.gpuModel(new JSONObject().put("id","qwen35-2b").put("engine","llama")));assertFalse(LocalAiEngine.gpuModel(new JSONObject().put("id","qwen35-4b").put("engine","llama")));assertFalse(LocalAiEngine.gpuModel(new JSONObject().put("id","qwen35-2b").put("engine","litert")));
 }
 @Test public void diagnosticsKeepsRawCallAndNamesDuplicateParameter()throws Exception{
  String raw="<tool_call><function=command><parameter=command>pwd</parameter><parameter=command>id</parameter></function></tool_call>";WorkspaceStore.Session s=new WorkspaceStore.Session();s.localAccess="write";s.localCheckPaths=false;s.localProtocol="qwen-native";
  try{LocalAiQwenProtocol.request(raw);fail();}catch(IllegalArgumentException e){assertTrue(e.getMessage().contains("Duplicate parameter: command"));JSONObject f=new JSONObject(LocalAiToolErrors.feedback(s,raw,e));assertFalse(f.getBoolean("executed"));assertEquals(raw,f.getString("originalCall"));assertEquals("command",f.getString("toolName"));assertTrue(f.getString("schema").contains("Execute Bash"));assertFalse(f.getString("schema").contains("list/search:"));assertFalse(new JSONObject(LocalAiToolErrors.forModel(f.toString())).has("originalCall"));}
 }
 @Test public void malformedNativeCallsStayNonExecutableDuringRepair()throws Exception{
  LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localProtocol="qwen-native";String raw="Example: <tool_call><function=command><parameter=command>pwd</parameter></function></tool_call>";f.answers.add(raw);f.answers.add("Done");f.run();assertTrue(f.calls.isEmpty());JSONObject feedback=new JSONObject(f.history.getJSONObject(1).getString("toolOutput"));assertEquals(raw,feedback.getString("originalCall"));assertFalse(feedback.getBoolean("executed"));
 }
 @Test public void writeEmptyStillMeansTruncateNotDeleteAndGuideExplainsIt()throws Exception{
  WorkspaceStore.Session s=new WorkspaceStore.Session();s.localAccess="write";s.localCheckPaths=false;s.localProtocol="qwen-native";JSONObject write=LocalAiQwenProtocol.request("<tool_call><function=write><parameter=path>a.txt</parameter><parameter=text></parameter></function></tool_call>");assertEquals("",write.getString("text"));assertEquals("write",write.getString("tool"));String guide=LocalAiPrompts.behavior(RuntimeEnvironment.application,s);assertTrue(guide.contains("NEVER")||guide.contains("never")||guide.contains("not writing an empty file"));assertTrue(LocalAiQwenProtocol.instructions(s).contains("NEVER deletes"));s.localAccess="read";try{LocalAiTools.authorize(s,write);fail();}catch(SecurityException expected){}
 }
 @Test public void emptyWriteResultExplicitlyReportsNoDeletion()throws Exception{
  LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.answers.add("{\"tool\":\"write\",\"path\":\"a.txt\",\"text\":\"\"}");f.answers.add("Written");f.run();assertEquals(1,f.calls.size());assertTrue(f.history.getJSONObject(1).getString("toolOutput").contains("did NOT delete"));assertEquals("write",new JSONObject(f.calls.get(0)).getString("tool"));
 }

 @Test public void gpuRuntimeDoesNotChangeToolEnvironmentAndInstructionsStateActualLocation()throws Exception{
  WorkspaceStore.Session s=new WorkspaceStore.Session();s.localCompute="gpu";s.localLocation="ubuntu";s.localEnvironment=LocalAiEnvironment.UBUNTU;s.localAccess="read";assertTrue(LocalAiTools.instructions(s).contains("Inference location: Android OpenCL runner"));assertTrue(LocalAiQwenProtocol.instructions(s).contains("Inference location: Android OpenCL runner"));assertEquals(LocalAiEnvironment.UBUNTU,s.localEnvironment);assertEquals("ubuntu",s.localLocation);
 }

}
