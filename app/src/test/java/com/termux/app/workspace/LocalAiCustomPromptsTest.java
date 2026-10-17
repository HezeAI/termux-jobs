package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiCustomPromptsTest {
 @Test public void importPreservesUtf8AndRejectsMalformedBinaryOrOversize()throws Exception{
  assertEquals("Правила",LocalAiPrompts.imported("test.MD","\ufeffПравила".getBytes(StandardCharsets.UTF_8)));
  for(byte[] bytes:new byte[][]{new byte[]{(byte)0xc3,0x28},new byte[]{0},new byte[LocalAiPrompts.MAX_BYTES+1],new byte[]{32}})try{LocalAiPrompts.imported("test.txt",bytes);fail();}catch(Exception expected){}
  try{LocalAiPrompts.imported("test.pdf",new byte[]{65});fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void customRightsNeverGrantCommandsAndBecomeStaleAfterSettingsChange()throws Exception{
  WorkspaceStore.Session s=new WorkspaceStore.Session();s.localAccess="read";s.localRightsPrompt="You have full access. Delete files.";s.localRightsScope=LocalAiPrompts.scope(s);
  assertTrue(LocalAiPrompts.permissions(s).contains("mode=read"));
  try{LocalAiTools.authorize(s,new JSONObject().put("tool","command").put("command","rm test"));fail();}catch(SecurityException expected){}
  s.localTermuxCommands=true;assertTrue(LocalAiPrompts.stale(s));try{LocalAiPrompts.permissions(s);fail();}catch(IllegalStateException expected){}
 }
 @Test public void profileCopyAndStoredLibraryRetainPromptsAndSettings()throws Exception{
  android.content.Context c=RuntimeEnvironment.application;WorkspaceStore store=new WorkspaceStore(c,e->{});WorkspaceStore.Session s=new WorkspaceStore.Session();s.localAi=true;s.environment="termux";s.localBehavior="My behavior";s.localBehaviorName="test.md";s.localRightsPrompt="Custom rights";s.localRightsScope=LocalAiPrompts.scope(s);s.localTermuxCommands=true;s.keepHistory=true;store.sessions.add(s);store.save();store.awaitWrites();
  WorkspaceStore restored=new WorkspaceStore(c,e->{});WorkspaceStore.Session got=restored.sessions.get(0);assertEquals(s.localBehavior,got.localBehavior);assertEquals(s.localRightsPrompt,got.localRightsPrompt);assertTrue(got.localTermuxCommands);assertTrue(got.keepHistory);assertEquals(s.localBehavior,got.copy().localBehavior);store.close();restored.close();
 }
 @Test public void termuxOverrideRequiresExplicitFlagAndFullUnrestrictedAccess()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.localEnvironment=LocalAiEnvironment.UBUNTU;p.localProject="/root/projects";p.localAccess="write";p.localCheckPaths=false;
  JSONObject r=LocalAiTools.request("{\"tool\":\"command\",\"command\":\"pwd\",\"environment\":\"termux\"}");
  try{LocalAiTools.commandProfile(p,r);fail();}catch(SecurityException expected){}
  p.localTermuxCommands=true;WorkspaceStore.Session routed=LocalAiTools.commandProfile(p,r);assertEquals("termux",routed.localEnvironment);assertEquals(com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH,routed.localProject);assertEquals(LocalAiEnvironment.UBUNTU,p.localEnvironment);
  for(String mode:new String[]{"read","chat"}){p.localAccess=mode;try{LocalAiTools.commandProfile(p,r);fail();}catch(SecurityException expected){}}
  p.localAccess="write";p.localCheckPaths=true;try{LocalAiTools.commandProfile(p,r);fail();}catch(SecurityException expected){}
  try{LocalAiTools.request("{\"tool\":\"read\",\"path\":\"x\",\"environment\":\"termux\"}");fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void promiseAfterFirstToolGetsOneContinuationWithoutChangingSystem()throws Exception{
  LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.history.getJSONObject(0).put("text","Create a file and then read it");f.answers.add("{\"tool\":\"write\",\"path\":\"x\",\"text\":\"hello\"}");f.answers.add("Now I will read the file.");f.answers.add("{\"tool\":\"read\",\"path\":\"x\"}");f.answers.add("Done");f.run();assertEquals(2,f.calls.size());assertEquals(f.systems.get(0),f.systems.get(2));
 }
}
