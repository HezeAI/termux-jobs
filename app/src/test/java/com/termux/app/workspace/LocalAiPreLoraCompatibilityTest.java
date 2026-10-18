package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiPreLoraCompatibilityTest {
 @Test public void newerOptionalAdapterFieldDoesNotResetSessionOrSavedChat()throws Exception{
  WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,s->{});
  try{
   store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.name="Existing local chat";p.localAi=true;p.environment="termux";p.localEnvironment=LocalAiEnvironment.UBUNTU;p.localModel="qwen35-2b";p.localChat=java.util.UUID.randomUUID().toString();p.localAccess="write";p.localCheckPaths=false;p.localThreads=6;p.localLocation="ubuntu";store.sessions.add(p);
   JSONObject json=store.encode();json.getJSONArray("sessions").getJSONObject(0).put("localLora","lora-12345678-1234-1234-1234-123456789abc");store.decode(json.toString());
   assertEquals(1,store.sessions.size());WorkspaceStore.Session restored=store.sessions.get(0);assertEquals(p.id,restored.id);assertEquals(p.name,restored.name);assertEquals(p.localChat,restored.localChat);assertEquals(p.localModel,restored.localModel);assertEquals("write",restored.localAccess);assertFalse(restored.localCheckPaths);assertEquals(6,restored.localThreads);assertEquals("ubuntu",restored.localLocation);assertEquals(LocalAiEnvironment.UBUNTU,restored.localEnvironment);
   assertFalse(store.encode().getJSONArray("sessions").getJSONObject(0).has("localLora"));
  }finally{store.close();}
 }
}
