package com.termux.app.workspace;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import android.app.Application;import org.json.JSONObject;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceRestartTest {
 @Test public void unknownEndedChatCannotSilentlyBecomeANewConversation(){for(String agent:new String[]{"codex","claude","gemini","opencode"}){WorkspaceStore.Session p=AgentEnvironmentsTest.profile(agent);p.codexThread="";p.claudeThread="";p.geminiThread="";p.aiThread="";assertFalse(agent,WorkspaceRestart.bindRecorded(p));}}
 @Test public void onlyConfirmedNativeIdentityCanBindEachAgentAndSnapshotKeepsItsRun()throws Exception{
  for(String agent:new String[]{"codex","claude","gemini","opencode"}){
   WorkspaceStore.Session p=AgentEnvironmentsTest.profile(agent);p.handle="owner-run";p.codexSocketId="codex-run";p.claudeSocketId="claude-run";p.geminiSocketId="gemini-run";p.aiSocketId="open-run";String uuid="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",id=agent.equals("opencode")?"ses_123abc":uuid;
   WorkspaceStore.Session copy=WorkspaceRestart.snapshot(p);assertTrue(WorkspaceRestart.sameRun(p,copy));assertFalse(WorkspaceRestart.bind(p,new JSONObject().put("session",id)));assertFalse(WorkspaceRestart.bind(p,new JSONObject().put("nativeChat",true).put("session","")));
   assertTrue(agent,WorkspaceRestart.bind(p,new JSONObject().put("nativeChat",true).put("session",id)));assertEquals("owner-run",p.handle);assertEquals(copy.id,p.id);
   assertEquals(id,agent.equals("codex")?p.codexThread:agent.equals("claude")?p.claudeThread:agent.equals("gemini")?p.geminiThread:p.aiSavedThread);
   assertTrue(agent,WorkspaceRestart.bindRecorded(p));
   p.handle="new-run";assertFalse(WorkspaceRestart.sameRun(p,copy));
  }
 }
}
