package com.termux.app.workspace;

import org.json.JSONObject;import org.junit.Test;import static org.junit.Assert.*;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class) @org.robolectric.annotation.Config(manifest=org.robolectric.annotation.Config.NONE)
public class AgentModelConnectionTest {
 @Test public void noInventedLimitAndLocalizedNativeStates()throws Exception{
  JSONObject event=new JSONObject().put("state","retrying").put("attempt",2);
  assertEquals("Переподключение… 2 ▾",AgentModelConnection.label(event,true));assertEquals("Reconnecting… 2 ▾",AgentModelConnection.label(event,false));
  event.put("limit",5);assertEquals("Переподключение… 2/5 ▾",AgentModelConnection.label(event,true));event.remove("attempt");assertEquals("Reconnecting… ▾",AgentModelConnection.label(event,false));
  event.put("state","failed");assertTrue(AgentModelConnection.label(event,true).contains("ошибка CLI"));assertTrue(AgentModelConnection.details(event,false).contains("do not confirm response recovery"));assertEquals("",AgentModelConnection.label(null,true));
 }
}
