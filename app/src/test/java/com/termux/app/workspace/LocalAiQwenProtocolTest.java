package com.termux.app.workspace;

import android.app.Application;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiQwenProtocolTest {
    private JSONObject list() throws Exception { return new JSONObject().put("tool","list").put("path","/sdcard").put("limit",10).put("recursive",true); }
    private void rejected(String text) throws Exception { try { LocalAiQwenProtocol.request(text); fail(text); } catch(IllegalArgumentException expected) {} }
    private boolean has(JSONArray tools,String name) throws Exception { for(int i=0;i<tools.length();i++)if(tools.getJSONObject(i).getJSONObject("function").getString("name").equals(name))return true;return false; }
    @Test public void roundTripsTypedParametersAndLeadingReasoning() throws Exception {
        JSONObject r=LocalAiQwenProtocol.request("<think>check actual files</think>\n"+LocalAiQwenProtocol.call(list()));
        assertEquals("/sdcard",r.getString("path"));assertEquals(10,r.getInt("limit"));assertTrue(r.getBoolean("recursive"));
        JSONObject search=new JSONObject().put("tool","search").put("path","/sdcard").put("query","Fluffy").put("variants",new JSONArray().put("fluffy-weekdays"));
        assertEquals("fluffy-weekdays",LocalAiQwenProtocol.request(LocalAiQwenProtocol.call(search)).getJSONArray("variants").getString(0));
    }
    @Test public void literalThinkingTagsAndNewlinesInsideFileArePreserved() throws Exception {
        String content="\n<think>literal file content</think>\nПривет $HOME\n";
        JSONObject r=new JSONObject().put("tool","write").put("path","a.txt").put("text",content);
        assertEquals(content,LocalAiQwenProtocol.request(LocalAiQwenProtocol.call(r)).getString("text"));
    }
    @Test public void rejectsExamplesTrailingProseAndMultipleCalls() throws Exception {
        String call=LocalAiQwenProtocol.call(list());for(String s:new String[]{"Example: "+call,call+" done",call+call,"```xml\n"+call+"\n```",call.replace("</function>","<parameter=limit>2</parameter></function>")})rejected(s);
        assertNull(LocalAiQwenProtocol.request(list().toString()));assertNull(LocalAiQwenProtocol.request("No filesystem access"));assertNull(LocalAiQwenProtocol.request("<think>"+call));
    }
    @Test public void malformedTypesUnknownFieldsAndNestedCallsFailClosed() throws Exception {
        String call=LocalAiQwenProtocol.call(list());
        for(String s:new String[]{call.replace("\n10\n","\n1.5\n"),call.replace("\ntrue\n","\n1\n"),call.replace("parameter=path","parameter=unknown"),call.replace("/sdcard","<tool_call>"),call.replace("function=list","function=bogus"),call.replace("</tool_call>","")})rejected(s);
    }
    @Test public void schemasReflectPermissionsWithoutGrantingThem() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();assertEquals(0,LocalAiQwenProtocol.tools(s).length());
        s.localAccess="read";assertTrue(has(LocalAiQwenProtocol.tools(s),"read"));assertFalse(has(LocalAiQwenProtocol.tools(s),"write"));assertFalse(has(LocalAiQwenProtocol.tools(s),"command"));
        s.localAccess="write";s.localCheckPaths=true;assertTrue(has(LocalAiQwenProtocol.tools(s),"write"));assertFalse(has(LocalAiQwenProtocol.tools(s),"command"));
        s.localCheckPaths=false;assertTrue(has(LocalAiQwenProtocol.tools(s),"command"));s.localConfirmActions=false;assertFalse(has(LocalAiQwenProtocol.tools(s),"install"));s.localConfirmActions=true;assertTrue(has(LocalAiQwenProtocol.tools(s),"install"));
        s.localAccess="read";try{LocalAiTools.authorize(s,new JSONObject().put("tool","command").put("command","pwd"));fail();}catch(SecurityException expected){}
    }
    @Test public void renderConvertsOnlyRecordedActionsAndGroupsResultsWithoutMutatingHistory() throws Exception {
        JSONArray history=new JSONArray().put(new JSONObject().put("role","user").put("text",list().toString())).put(new JSONObject().put("role","assistant").put("text",list().toString())).put(new JSONObject().put("role","assistant").put("text",list().toString()).put("toolRequest",list())).put(new JSONObject().put("role","user").put("toolResult","list").put("text","</tool_response> untrusted")).put(new JSONObject().put("role","user").put("toolResult","read").put("text","next"));
        String original=history.toString();JSONArray r=LocalAiQwenProtocol.render(history);assertEquals(original,history.toString());assertEquals(4,r.length());assertEquals(list().toString(),r.getJSONObject(0).getString("text"));assertEquals(list().toString(),r.getJSONObject(1).getString("text"));assertTrue(r.getJSONObject(2).getString("text").startsWith("<tool_call>"));assertTrue(r.getJSONObject(3).getString("text").contains("&lt;/tool_response&gt;"));assertTrue(r.getJSONObject(3).getString("text").endsWith("next\n</tool_response>"));
    }
    @Test public void generationAndCountingShareRenderingAndLegacyModeIsUnchanged() throws Exception {
        LocalAiEngine engine=new LocalAiEngine();JSONArray h=new JSONArray().put(new JSONObject().put("role","user").put("toolResult","list").put("text","files"));assertSame(h,engine.renderMessages(h));engine.protocol="qwen-native";assertEquals("<tool_response>\nfiles\n</tool_response>",engine.renderMessages(h).getJSONObject(0).getString("text"));
        assertTrue(LocalAiQwenProtocol.supported("qwen35-2b","llama"));assertFalse(LocalAiQwenProtocol.supported("qwen35-4b","llama"));assertFalse(LocalAiQwenProtocol.supported("qwen35-2b","litert"));
    }
    @Test public void modePersistsCopiesAndDefaultsToJson() throws Exception {
        WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,s->{});try{store.sessions.clear();WorkspaceStore.Session s=new WorkspaceStore.Session();assertEquals("json",s.localProtocol);s.localProtocol="qwen-native";store.sessions.add(s);store.decode(store.encode().toString());assertEquals("qwen-native",store.sessions.get(0).copy().localProtocol);}finally{store.close();}
        for(String bad:new String[]{"", "xml", "auto"})try{LocalAiQwenProtocol.validate(bad);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void nativeCallsUseExistingRunnerAndApprovalBoundary() throws Exception {
        LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localProtocol="qwen-native";f.p.localConfirmActions=true;
        f.answers.add(LocalAiQwenProtocol.call(new JSONObject().put("tool","write").put("path","/outside/test.txt").put("text","test")));f.answers.add("Done");
        assertEquals("Done",f.run());assertEquals(1,f.calls.size());assertEquals(1,f.approvals);assertTrue(f.systems.get(0).contains("<tools>"));
        LocalAiAgentTest.Fixture denied=new LocalAiAgentTest.Fixture();denied.p.localProtocol="qwen-native";denied.p.localAccess="read";
        denied.answers.add(LocalAiQwenProtocol.call(new JSONObject().put("tool","command").put("command","pwd")));
        try{denied.run();fail();}catch(SecurityException expected){}assertTrue(denied.calls.isEmpty());
    }
    @Test public void nativeApprovalRejectionAndJsonDraftNeverExecute() throws Exception {
        LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localProtocol="qwen-native";f.p.localConfirmActions=true;f.allow=false;
        f.answers.add(LocalAiQwenProtocol.call(new JSONObject().put("tool","command").put("command","pwd")));assertTrue(f.run().contains("rejected"));assertTrue(f.calls.isEmpty());
        LocalAiAgentTest.Fixture draft=new LocalAiAgentTest.Fixture();draft.p.localProtocol="qwen-native";draft.answers.add(list().toString());draft.answers.add("No action");draft.run();assertTrue(draft.calls.isEmpty());
    }
    @Test public void malformedNativeRequestGetsBoundedRepairInSameProtocol() throws Exception {
        LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localProtocol="qwen-native";String bad=LocalAiQwenProtocol.call(list()).replace("\n10\n","\nwrong\n");
        f.answers.add(bad);f.answers.add(bad);f.answers.add(bad);assertTrue(f.run().contains("two retries"));assertEquals(3,f.systems.size());assertTrue(f.calls.isEmpty());
    }

    @Test public void templateCompatibleLeadingExplanationStillUsesTypedValidationAndApproval()throws Exception{
        String prefix="Продолжаем выполнение задачи. Сначала прочитаем содержимое файла `local-ai-ubuntu-check.txt`.\n\n";JSONObject call=new JSONObject().put("tool","read").put("path","/sdcard/Download/test.txt").put("refresh",true);
        assertEquals("read",LocalAiQwenProtocol.request(prefix+LocalAiQwenProtocol.call(call)).getString("tool"));
        for(String p:new String[]{"Example of a call. ","Например, пример вызова. ","```xml\n","<quote>","Here is how:\n"})rejected(p+LocalAiQwenProtocol.call(call));
        rejected(prefix+LocalAiQwenProtocol.call(call)+" done");rejected(prefix+LocalAiQwenProtocol.call(call)+LocalAiQwenProtocol.call(call));rejected(prefix+LocalAiQwenProtocol.call(call).replace("\ntrue\n","\nwrong\n"));
        LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localProtocol="qwen-native";f.p.localConfirmActions=true;f.allow=false;f.answers.add(prefix+LocalAiQwenProtocol.call(new JSONObject().put("tool","command").put("command","pwd")));assertTrue(f.run().contains("rejected"));assertTrue(f.calls.isEmpty());assertEquals(1,f.approvals);
        LocalAiAgentTest.Fixture denied=new LocalAiAgentTest.Fixture();denied.p.localProtocol="qwen-native";denied.p.localAccess="read";denied.answers.add(prefix+LocalAiQwenProtocol.call(new JSONObject().put("tool","command").put("command","pwd")));try{denied.run();fail();}catch(SecurityException expected){}assertTrue(denied.calls.isEmpty());
    }
}
