package com.termux.app.workspace;
import android.app.Application;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiPromptOptimizationTest {
 String canonical(Object value)throws Exception{
  if(value instanceof JSONObject){JSONObject o=(JSONObject)value;java.util.TreeSet<String> keys=new java.util.TreeSet<>();java.util.Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();if(!k.equals("description"))keys.add(k);}StringBuilder b=new StringBuilder("{");for(String key:keys){if(b.length()>1)b.append(',');b.append(JSONObject.quote(key)).append(':').append(canonical(o.get(key)));}return b.append('}').toString();}
  if(value instanceof JSONArray){StringBuilder b=new StringBuilder("[");JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++){if(i>0)b.append(',');b.append(canonical(a.get(i)));}return b.append(']').toString();}return value instanceof String?JSONObject.quote((String)value):String.valueOf(value);
 }
 String digest(Object value)throws Exception{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return b.toString();}
 @Test public void onlyDescriptionsChangeAcrossAllBaselineToolSchemaConfigurations()throws Exception{
  java.io.InputStream in=getClass().getResourceAsStream("/local-ai-schema-v158-hashes.json");assertNotNull(in);JSONObject baseline=new JSONObject(LocalAiFiles.read(in,65536));int n=0;
  for(String mode:new String[]{"chat","read","write"})for(boolean scope:new boolean[]{false,true})for(boolean confirm:new boolean[]{false,true})for(boolean extra:new boolean[]{false,true}){
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess=mode;p.localCheckPaths=scope;p.localConfirmActions=confirm;p.localTermuxCommands=extra;p.localEnvironment=LocalAiEnvironment.UBUNTU;String key=mode+"|"+scope+"|"+confirm+"|"+extra;JSONObject expected=baseline.getJSONObject(key);
   assertEquals(key+" qwen",expected.getString("qwen"),digest(LocalAiQwenProtocol.tools(p)));assertEquals(key+" litert",expected.getString("litert"),digest(LocalAiLiteRt.schemas(p)));n++;
  }assertEquals(24,n);
 }
 @Test public void defaultsAndBriefGuideDeliverablesWithoutChangingCustomOrToolsOnly()throws Exception{
  for(String protocol:new String[]{"json","qwen-native","litert-native"})for(String mode:new String[]{"standard","brief"}){
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.localProtocol=protocol;p.localBehaviorMode=mode;
   String behavior=protocol.equals("litert-native")?LocalAiLiteRt.prompt(p,""):LocalAiPrompts.behavior(RuntimeEnvironment.application,p);assertTrue(behavior.contains(LocalAiPrompts.intent()));
   p.localToolsOnly=true;assertEquals("",LocalAiPrompts.behavior(RuntimeEnvironment.application,p));assertFalse(LocalAiLiteRt.prompt(p,"").contains(LocalAiPrompts.intent()));
   p.localToolsOnly=false;p.localBehaviorMode="custom";p.localBehavior="MY EXACT USER BEHAVIOR";assertEquals(p.localBehavior,LocalAiPrompts.behavior(RuntimeEnvironment.application,p));assertFalse(LocalAiLiteRt.prompt(p,p.localBehavior).contains(LocalAiPrompts.intent()));
  }
 }
 @Test public void activeSettingsRemainExplicitWithoutGrantingCommandsToReadOrProjectOnly()throws Exception{
  for(String mode:new String[]{"chat","read","write"})for(boolean scope:new boolean[]{false,true}){
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess=mode;p.localCheckPaths=scope;p.localConfirmActions=true;p.localProject="/root/project";
   String text=LocalAiPrompts.compactSettings(p);assertTrue(text.contains("mode="+mode));assertTrue(text.contains("projectOnly="+scope));assertTrue(text.contains(p.localProject));assertTrue(text.contains("confirm=true"));assertTrue(text.contains("untrusted"));assertTrue(text.contains("not Android root"));
   JSONObject command=new JSONObject().put("tool","command").put("command","true");if(!mode.equals("write")||scope)try{LocalAiTools.authorize(p,command);fail();}catch(SecurityException expected){}else LocalAiTools.authorize(p,command);
  }
 }
 @Test public void toolDescriptionsReachBothNativeSchemaSetsIncludingToolsOnly()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="write";p.localCheckPaths=false;p.localConfirmActions=true;p.localToolsOnly=true;
  java.util.Set<String> names=new java.util.HashSet<>();for(JSONArray schemas:new JSONArray[]{LocalAiQwenProtocol.tools(p),LocalAiLiteRt.schemas(p)})for(int i=0;i<schemas.length();i++){
   JSONObject f=schemas.getJSONObject(i).getJSONObject("function");String name=f.getString("name");names.add(name);assertEquals(LocalAiLiteRt.description(name),f.getString("description"));assertTrue(f.getString("description").length()<260);
   if(name.equals("command"))assertTrue(f.getString("description").contains("NOT for writing/saving an installation guide; use write"));
   if(name.equals("write")){assertTrue(f.getString("description").contains("NOT install Python"));assertTrue(f.getJSONObject("parameters").getJSONObject("properties").getJSONObject("text").getString("description").contains("SAVE"));}
  }assertEquals(13,names.size());assertEquals("",LocalAiPrompts.behavior(RuntimeEnvironment.application,p));
 }
}
