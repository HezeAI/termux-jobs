package com.termux.app.workspace;
import android.app.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.json.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentOfflineDataTest {
 @Test public void perChatPersistenceKeepsOriginalTimeAndRejectsUnknownReplacement()throws Exception{
  Application a=RuntimeEnvironment.application;WorkspaceStore.Session p=new WorkspaceStore.Session(),other=new WorkspaceStore.Session();
  JSONObject data=new JSONObject("{\"usage\":{\"used\":100,\"capacity\":1000}}");AgentOfflineData.remember(a,p,"context",data,12345);
  AgentOfflineData.remember(a,p,"context",data,20000);assertEquals(12345,AgentOfflineData.read(a,p,"context").getLong("time"));
  AgentOfflineData.remember(a,p,"context",new JSONObject("{\"stale\":true}"),30000);assertEquals(100,AgentOfflineData.read(a,p,"context").getJSONObject("data").getJSONObject("usage").getInt("used"));
  assertEquals(0,AgentOfflineData.read(a,other,"context").length());p.codexThread="another";assertEquals(0,AgentOfflineData.read(a,p,"context").length());
 }
}
