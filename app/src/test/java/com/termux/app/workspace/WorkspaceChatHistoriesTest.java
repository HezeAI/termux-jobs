package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceChatHistoriesTest {
 @Test public void localDeletionRemovesOnlyExactChatAndAtomicCompanions()throws Exception{
  LocalAiFiles f=new LocalAiFiles(RuntimeEnvironment.application);String a="chat-a",b="chat-b";JSONArray h=new JSONArray().put(new JSONObject().put("role","user").put("text","hello"));f.saveHistory(a,h);f.saveHistory(b,h);LocalAiFiles.atomic(new File(f.chats,a+".json.bak"),"[]");LocalAiFiles.atomic(new File(f.chats,a+".json.new"),"[]");f.deleteHistory(a);assertFalse(new File(f.chats,a+".json").exists());assertFalse(new File(f.chats,a+".json.bak").exists());assertFalse(new File(f.chats,a+".json.new").exists());assertEquals(1,f.history(b).length());
  try{f.deleteHistory("../auth");fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void localPreviewUsesActualTranscriptNotInstructionMetadata()throws Exception{JSONArray h=new JSONArray().put(new JSONObject().put("role","user").put("text","First question")).put(new JSONObject().put("role","assistant").put("text","Answer"));assertEquals("First question",WorkspaceChatHistories.localTitle(h,"id"));String p=WorkspaceChatHistories.transcript(h);assertTrue(p.contains("user: First question"));assertTrue(p.contains("assistant: Answer"));}
 @Test public void codexPreviewParsesUserAndAgentMessagesWithoutExecutingAnything()throws Exception{JSONObject t=new JSONObject("{\"turns\":[{\"items\":[{\"type\":\"userMessage\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]},{\"type\":\"agentMessage\",\"text\":\"world\"}]}]}");String p=WorkspaceChatHistories.codexTranscript(t);assertTrue(p.contains("You: hello"));assertTrue(p.contains("AI: world"));try{WorkspaceChatHistories.codexTranscript(new JSONObject());fail();}catch(IOException expected){}}
 @Test public void codexDeleteUsesExactNativeMethodWithoutArchiveFallback()throws Exception{java.util.List<String> calls=new java.util.ArrayList<>();String id="01900000-0000-7000-8000-000000000001";try{WorkspaceChatHistories.deleteCodex(id,(method,params)->{calls.add(method);assertEquals(id,params.getString("threadId"));throw new IOException("unsupported");});fail();}catch(IOException expected){}assertEquals(java.util.Arrays.asList("thread/delete"),calls);try{WorkspaceChatHistories.deleteCodex("../../auth",(m,p)->{fail();return new JSONObject();});fail();}catch(IllegalArgumentException expected){}}
}
