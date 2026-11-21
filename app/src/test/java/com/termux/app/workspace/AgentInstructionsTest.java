package com.termux.app.workspace;
import org.junit.Test;import java.io.File;import static org.junit.Assert.*;
public class AgentInstructionsTest {
 @Test public void claudeHelperUsesSelectedEnvironmentWithoutCodexBootstrapOrAuthReads(){String c=AgentInstructions.command(new File("/private/files"),"claude","ubuntu");assertTrue(c.contains("ubuntu -- /bin/bash"));assertTrue(c.contains("agent-instructions-claude.en.md"));assertTrue(c.contains("$HOME/.bashrc"));assertFalse(c.contains("command -v codex"));assertFalse(c.contains("CLAUDE_CONFIG_DIR="));assertFalse(c.contains(".credentials.json"));}
 @Test public void launchInitializesOnlyTheRelevantSharedTemplate(){
  File files=new File("/private/agent's files");
  for(String agent:new String[]{"codex","claude","gemini"}){
   WorkspaceStore.Session profile=new WorkspaceStore.Session();
   profile.environment=agent.equals("codex")?"codex":"ubuntu";
   profile.codexEnvironment="ubuntu-ai";profile.claudeAgent=agent.equals("claude");profile.geminiAgent=agent.equals("gemini");
   profile.geminiAuth=GeminiCli.ANTIGRAVITY;
   String command=AgentInstructions.initialization(files,profile);
   assertTrue(command.contains("\"operation\":\"init\""));
   assertTrue(command.contains("\"agent\":\""+agent+"\""));
   assertTrue(command.contains("python3 -I"));
   assertTrue(command.contains("agent-instructions-"+(agent.equals("codex")?"ubuntu-ai":agent)+".en.md"));
   assertFalse(command.contains("\"project\""));assertFalse(command.contains("LEARNING.md"));assertFalse(command.contains("auth.json"));
   if(profile.geminiAgent){assertTrue(command.contains("\"geminiRuntime\":\"antigravity\""));profile.geminiAuth="google";assertTrue(AgentInstructions.initialization(files,profile).contains("\"geminiRuntime\":\"legacy\""));}
  }
  WorkspaceStore.Session terminal=new WorkspaceStore.Session();
  assertEquals("",AgentInstructions.initialization(files,terminal));
 }
 @Test public void geminiEditorMountContainsOnlyVersionedInstructionAssets(){
  File files=new File("/private/agent's files");
  String command=AgentInstructions.command(files,"gemini","ubuntu-ai","/project with spaces");
  assertTrue(command.contains(SessionShell.quote(AgentInstructions.launchBind(files))));
  assertTrue(command.contains(" --work-dir '/project with spaces'"));
  assertTrue(command.contains("agent-instructions-gemini.en.md"));
  assertFalse(command.contains(".codex-socket:/tmp/termux-jobs-codex"));
 }
 @Test public void helperEntersActualUbuntuAiAndInheritedHomeWithoutInstalling(){String c=AgentInstructions.command(new File("/private app/files"));assertTrue(c.contains("proot-distro login --bind"));assertTrue(c.contains("ubuntu-ai -- /bin/bash"));assertTrue(c.contains("$HOME/.bashrc"));assertFalse(c.contains("CODEX_HOME="));assertFalse(c.contains("apt install"));assertFalse(c.contains("pkill"));assertFalse(c.contains("killall"));assertTrue(c.contains("agent-instructions-ubuntu-ai.en.md"));}
 @Test public void storageSectionRecognitionWorksInBothLanguagesWithoutInventingSections(){assertTrue(AgentInstructions.hasStorageAddition("Prefix\n## File and temporary data storage\r\nRules"));assertTrue(AgentInstructions.hasStorageAddition("## Хранение файлов и временных данных\nПравила"));assertFalse(AgentInstructions.hasStorageAddition("Discussion of File and temporary data storage"));assertEquals("## File and temporary data storage\nRules",AgentInstructions.storageAddition("prefix\n## File and temporary data storage\nRules"));}
 @Test public void legacyCodexRoutesToShownUbuntuOrNativeTermuxWithoutAiRedirect(){
  String ubuntu=AgentInstructions.command(new File("/private/files"),"codex","ubuntu");assertTrue(ubuntu.contains("ubuntu -- /bin/bash"));assertFalse(ubuntu.contains("ubuntu-ai --"));
  String termux=AgentInstructions.command(new File("/private/files"),"codex","termux");assertFalse(termux.contains("proot-distro"));assertTrue(termux.contains("--noprofile --norc -i -c"));assertTrue(termux.contains("agent-instructions.py"));assertFalse(termux.contains("ubuntu-ai --"));
  WorkspaceStore.Session old=new WorkspaceStore.Session();old.environment="codex";old.codexEnvironment="termux";assertEquals("termux",AgentInstructionsUi.runtime(old));assertEquals("ubuntu-ai",AgentInstructionsUi.runtime(null));assertEquals("termux",AgentInstructionsUi.fileProfile("termux").environment);old.environment="ubuntu";old.codexEnvironment="ubuntu";assertEquals("ubuntu",AgentInstructionsUi.runtime(old));
 }

}
