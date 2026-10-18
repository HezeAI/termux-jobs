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


 @Test public void codexPreviewParsesUserAndAgentMessagesWithoutExecutingAnything()throws Exception{JSONObject t=new JSONObject("{\"turns\":[{\"items\":[{\"type\":\"userMessage\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]},{\"type\":\"agentMessage\",\"text\":\"world\"}]}]}");String p=WorkspaceChatHistories.codexTranscript(t);assertTrue(p.contains("You: hello"));assertTrue(p.contains("AI: world"));try{WorkspaceChatHistories.codexTranscript(new JSONObject());fail();}catch(IOException expected){}}
 @Test public void codexDeleteUsesExactNativeMethodWithoutArchiveFallback()throws Exception{java.util.List<String> calls=new java.util.ArrayList<>();String id="01900000-0000-7000-8000-000000000001";try{WorkspaceChatHistories.deleteCodex(id,(method,params)->{calls.add(method);assertEquals(id,params.getString("threadId"));throw new IOException("unsupported");});fail();}catch(IOException expected){}assertEquals(java.util.Arrays.asList("thread/delete"),calls);try{WorkspaceChatHistories.deleteCodex("../../auth",(m,p)->{fail();return new JSONObject();});fail();}catch(IllegalArgumentException expected){}}
}
