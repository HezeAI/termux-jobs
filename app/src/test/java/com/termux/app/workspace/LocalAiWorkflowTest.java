package com.termux.app.workspace;

import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Adapter/routing checks; not evidence that real models select these calls correctly. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiWorkflowTest {
 WorkspaceStore.Session profile(){WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="write";p.localCheckPaths=false;p.localConfirmActions=false;p.localEnvironment=LocalAiEnvironment.UBUNTU;p.localProject="/root/projects/demo";return p;}
 JSONObject roundTrip(JSONObject call,boolean sdk)throws Exception{
  if(!sdk)return LocalAiQwenProtocol.request(LocalAiQwenProtocol.call(call));
  JSONObject args=new JSONObject(call.toString());String name=args.getString("tool");args.remove("tool");return LocalAiLiteRt.request(name,LocalAiLiteRt.map(args));
 }
 @Test public void screenshotAndApkRequestsKeepExactPathsAcrossBothProtocols()throws Exception{
  for(boolean sdk:new boolean[]{false,true})for(JSONObject call:new JSONObject[]{new JSONObject().put("tool","screenshots").put("limit",1),new JSONObject().put("tool","inspect_apk").put("path","/sdcard/Download/Demo App-v2.apk")}){
   JSONObject actual=roundTrip(call,sdk);LocalAiTools.authorize(profile(),actual);assertEquals(call.length(),actual.length());java.util.Iterator<String> keys=call.keys();while(keys.hasNext()){String key=keys.next();assertEquals(key,call.get(key),actual.get(key));}
  }
 }
 @Test public void shellBuildAndDependencyChecksRouteOnlyToDedicatedUbuntu()throws Exception{
  for(boolean sdk:new boolean[]{false,true})for(String script:new String[]{"command -v aapt; command -v apksigner","./gradlew assembleDebug","apt-get update && apt-get install -y unzip"}){
   WorkspaceStore.Session p=profile();JSONObject call=roundTrip(new JSONObject().put("tool","command").put("command",script),sdk);WorkspaceStore.Session routed=LocalAiTools.commandProfile(p,call);assertEquals(LocalAiEnvironment.UBUNTU,routed.localEnvironment);assertEquals(p.localProject,routed.localProject);
   WorkspaceStore.Session host=new WorkspaceStore.Session();host.environment=LocalAiEnvironment.backend(routed.localEnvironment);String command=ProjectFiles.command(host,LocalAiTools.script(routed,call));assertTrue(command.contains("jobs-local-ai"));assertTrue(command.contains("--isolated"));assertFalse(command.startsWith("proot-distro login ubuntu "));
   call.put("environment","termux");try{LocalAiTools.commandProfile(p,call);fail();}catch(SecurityException expected){}assertEquals(LocalAiEnvironment.UBUNTU,p.localEnvironment);
  }
 }
 @Test public void readAndProjectModesNeverGainShellFromWorkflowOrLora()throws Exception{
  for(boolean sdk:new boolean[]{false,true}){WorkspaceStore.Session p=profile();p.localLora="lora-12345678-1234-1234-1234-123456789abc";JSONObject call=roundTrip(new JSONObject().put("tool","command").put("command","true"),sdk);
   p.localAccess="read";try{LocalAiTools.authorize(p,call);fail();}catch(SecurityException expected){}p.localAccess="write";p.localCheckPaths=true;try{LocalAiTools.authorize(p,call);fail();}catch(SecurityException expected){}
   p.localAccess="read";JSONObject install=roundTrip(new JSONObject().put("tool","install").put("package","unzip"),sdk);try{LocalAiTools.authorize(p,install);fail();}catch(SecurityException expected){}p.localConfirmActions=true;LocalAiTools.authorize(p,install);assertTrue(LocalAiPackages.script(install,true).contains("unzip"));
  }
 }
}
