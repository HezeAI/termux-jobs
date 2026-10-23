package com.termux.app.workspace;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.io.*;import java.nio.file.Files;import org.json.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=android.app.Application.class)
public class AgentEnvironmentsTest {
 @Test public void newEditorsAlwaysUseAiWhileExistingAndLegacySelectionsStayUnchanged(){assertEquals("ubuntu-ai",AgentEnvironments.editorUbuntu(null));WorkspaceStore.Session p=new WorkspaceStore.Session();p.codexEnvironment="ubuntu";assertEquals("ubuntu",AgentEnvironments.editorUbuntu(p));p.codexEnvironment="termux";assertEquals("termux",AgentEnvironments.editorUbuntu(p));p.codexEnvironment="ubuntu-ai";assertEquals("ubuntu-ai",AgentEnvironments.editorUbuntu(p));}
 @Test public void oldProfilesStayOnUbuntuAndAllAgentsCanSelectTheirOwnUbuntu()throws Exception {
  for(String agent:new String[]{"codex","gemini","claude","opencode"}){
   WorkspaceStore.Session p=profile(agent);p.id="11111111-1111-4111-8111-111111111111";p.codexSocketId="fixture";assertEquals("ubuntu",AgentEnvironments.scope(p));p.codexEnvironment="ubuntu-ai";
   assertTrue(AgentPreparation.supports(p));assertEquals("ubuntu-ai",AgentEnvironments.scope(p));assertEquals("proot:ubuntu-ai",ProjectFiles.environment(p));assertEquals("ubuntu-ai",p.copy().codexEnvironment);
   File files=Files.createTempDirectory("agent-ai-shell").toFile();File rc=SessionShell.prepare(files,p);String script=new String(Files.readAllBytes(rc.toPath()),"UTF-8");
   assertTrue(script,script.contains("'ubuntu-ai' --"));assertTrue(script,script.contains("JOBS_AGENT_UBUNTU='ubuntu-ai'"));
   assertFalse(script,script.contains(" ubuntu --"));
   String query=ProjectFiles.command(p,"pwd");assertTrue(query,query.contains("'ubuntu-ai'"));assertFalse(query,query.contains("login ubuntu "));
   WorkspaceStore.Session packages=PackageCatalog.operationProfile(p,"apt-get update");assertEquals("ubuntu-ai",packages.prootAlias);assertEquals("proot:ubuntu-ai",ProjectFiles.environment(packages));
  }
 }
 static WorkspaceStore.Session profile(String agent){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=agent.equals("codex")?"codex":"ubuntu";p.directory="/root/project";p.geminiAgent=agent.equals("gemini");p.claudeAgent=agent.equals("claude");if(agent.equals("opencode"))p.aiAgent="opencode";return p;}
 @Test public void codexHistoryControlAndProjectPathsUseSelectedUbuntu()throws Exception {
  WorkspaceStore.Session p=profile("codex");p.codexEnvironment="ubuntu-ai";
  assertTrue(CodexSessionShell.historyCommand(p).contains("'ubuntu-ai'"));assertTrue(CodexSessionShell.proxyCommand(new File("/tmp/fixture"),p).contains("'ubuntu-ai'"));
  assertEquals("/root/work",CodexProjects.directory("ubuntu-ai","~/work"));
  p.codexEnvironment="termux";assertFalse(AgentPreparation.supports(p));assertFalse(CodexSessionShell.historyCommand(p).contains("proot-distro"));
 }
 @Test public void storeRoundTripsBothUbuntuNamespacesWithoutChangingOtherData()throws Exception {
  android.content.Context c=RuntimeEnvironment.application;WorkspaceStore s=new WorkspaceStore(c,x->{});s.sessions.clear();
  for(String agent:new String[]{"codex","gemini","claude","opencode"}){WorkspaceStore.Session p=profile(agent);p.codexEnvironment="ubuntu-ai";s.sessions.add(p);}
  String encoded=s.encode().toString();s.decode(encoded);assertEquals(4,s.sessions.size());for(WorkspaceStore.Session p:s.sessions)assertEquals("ubuntu-ai",p.codexEnvironment);
  JSONObject bad=new JSONObject(encoded);bad.getJSONArray("sessions").getJSONObject(0).put("codexEnvironment","../escape");try{s.decode(bad.toString());fail();}catch(JSONException expected){}assertEquals(4,s.sessions.size());s.close();
 }
 @Test public void globalAccountRequestsCanUseAiAfterOrdinaryUbuntuWasRemoved()throws Exception {
  File root=Files.createTempDirectory("agent-preferred").toFile();assertEquals("ubuntu-ai",AgentEnvironments.defaultUbuntu(root));
  File normal=new File(root,"containers/ubuntu/rootfs");assertTrue(normal.mkdirs());assertEquals("ubuntu",AgentEnvironments.defaultUbuntu(root));
  File ai=new File(root,"containers/ubuntu-ai/rootfs");assertTrue(ai.mkdirs());assertEquals("ubuntu-ai",AgentEnvironments.defaultUbuntu(root));assertTrue(normal.delete());assertEquals("ubuntu-ai",AgentEnvironments.defaultUbuntu(root));
 }
 @Test public void operationGateSeparatesPlainUbuntuAiAndOtherDistros(){
  WorkspaceStore.Session p=profile("gemini");p.codexEnvironment="ubuntu-ai";AgentEnvironmentManager.operating.add("ubuntu-ai");try{assertTrue(AgentEnvironmentManager.busy(p));p.codexEnvironment="ubuntu";assertFalse(AgentEnvironmentManager.busy(p));WorkspaceStore.Session normal=new WorkspaceStore.Session();normal.environment="termux";normal.prootAlias="ubuntu-ai";assertTrue(AgentEnvironmentManager.busy(normal));}finally{AgentEnvironmentManager.operating.clear();}
 }
}
