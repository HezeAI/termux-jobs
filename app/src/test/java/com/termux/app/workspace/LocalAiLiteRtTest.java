package com.termux.app.workspace;

import android.app.Application;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiLiteRtTest {
    WorkspaceStore.Session profile(String mode){WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess=mode;p.localProject="/root/project";p.localCheckPaths=false;return p;}
    @Test public void nativeSchemaRespectsRealPermissionAndPathSettings()throws Exception{
        for(String mode:new String[]{"chat","read","write"})for(boolean scope:new boolean[]{false,true}){
            WorkspaceStore.Session p=profile(mode);p.localCheckPaths=scope;JSONArray schemas=LocalAiLiteRt.schemas(p);Set<String> names=new HashSet<>();
            for(int i=0;i<schemas.length();i++){JSONObject f=schemas.getJSONObject(i).getJSONObject("function");assertTrue(names.add(f.getString("name")));assertFalse(f.getJSONObject("parameters").getBoolean("additionalProperties"));}
            assertEquals(!mode.equals("chat"),names.contains("listen_audio"));assertEquals(mode.equals("write")&&!scope,names.contains("command"));assertEquals(mode.equals("write"),names.contains("write"));assertFalse(names.contains("chat"));if(mode.equals("chat"))assertEquals(0,schemas.length());
        }
    }
    @Test public void fullNativePromptIncludingSchemasFitsShortFirstTurn()throws Exception{
        for(String mode:new String[]{"chat","read","write"})for(boolean scope:new boolean[]{false,true}){
            WorkspaceStore.Session p=profile(mode);p.localCheckPaths=scope;String system=LocalAiLiteRt.prompt(p,"")+"\n"+LocalAiLiteRt.schemas(p);
            JSONArray h=new JSONArray().put(new JSONObject().put("role","user").put("text","привет"));
            int estimate=LocalAiContext.estimate(h,system);assertTrue(mode+" estimate="+estimate,estimate<4096-768-64);
            LocalAiContext.fit(h,system,4096,768,LocalAiContext::estimate,x->{throw new AssertionError();});
        }
    }
    @Test public void structuredCallsStillRequireExistingValidatorAndAuthorization()throws Exception{
        JSONObject r=LocalAiLiteRt.request("list",Collections.singletonMap("path","/sdcard"));LocalAiTools.authorize(profile("read"),r);
        try{LocalAiTools.authorize(profile("chat"),r);fail();}catch(SecurityException expected){}
        for(String tool:new String[]{"chat","command"})try{JSONObject action=LocalAiLiteRt.request(tool,Collections.singletonMap(tool.equals("command")?"command":"path","x"));LocalAiTools.authorize(profile("read"),action);fail();}catch(SecurityException expected){}
        try{LocalAiLiteRt.request("list",Collections.singletonMap("tool","command"));fail();}catch(IllegalArgumentException expected){}
        Map<String,Object> a=new HashMap<>();a.put("path","/sdcard");a.put("limit",1.0);assertEquals(1,LocalAiLiteRt.request("list",a).getInt("limit"));a.put("limit",1.5);try{LocalAiLiteRt.request("list",a);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void multimodalSupportOnlyForVerifiedPinnedModel()throws Exception{
        JSONObject p=new JSONObject().put("engine","litert").put("backend","npu").put("vision",false).put("sha256",LocalAiNpu.SHA);
        assertTrue(LocalAiMedia.supported(p));assertTrue(LocalAiMedia.audioSupported(p));assertTrue(LocalAiLiteRt.enabled(p));p.put("sha256","wrong");assertFalse(LocalAiMedia.supported(p));assertFalse(LocalAiMedia.audioSupported(p));assertFalse(LocalAiLiteRt.enabled(p));
    }
    @Test public void audioUsesExistingFilePermissionsAndBoundedWaveHeader()throws Exception{
        JSONObject r=LocalAiTools.request("{\"tool\":\"listen_audio\",\"path\":\"/test.wav\"}");LocalAiTools.authorize(profile("read"),r);try{LocalAiTools.authorize(profile("chat"),r);fail();}catch(SecurityException expected){}
        byte[] wave=LocalAiAudio.wav(new byte[320]);assertEquals(364,wave.length);assertEquals("RIFF",new String(wave,0,4,"US-ASCII"));assertEquals("WAVE",new String(wave,8,4,"US-ASCII"));assertEquals(0x80,wave[24]&255);assertEquals(0x3e,wave[25]&255);
        try{LocalAiMedia.checkedAudio(org.robolectric.RuntimeEnvironment.application,"/sdcard/foreign.wav");fail();}catch(java.io.IOException expected){}
    }
    @Test public void liteRtProtocolPersistsWithoutChangingPermissionsAndFiltersCapabilities()throws Exception{
        WorkspaceStore store=new WorkspaceStore(org.robolectric.RuntimeEnvironment.application,s->{});
        try{store.sessions.clear();WorkspaceStore.Session p=profile("read");p.localProtocol="litert-native";store.sessions.add(p);assertTrue(store.encode().toString().contains("\"localLiteRtNative\":true"));assertFalse(store.encode().toString().contains("\"localProtocol\":\"litert-native\""));store.decode(store.encode().toString());assertEquals("litert-native",store.sessions.get(0).copy().localProtocol);assertEquals("read",store.sessions.get(0).localAccess);assertFalse(LocalAiQwenProtocol.enabled(p));}finally{store.close();}
        JSONObject text=new JSONObject().put("engine","litert");assertTrue(LocalAiLiteRt.supported(text));JSONArray schemas=LocalAiLiteRt.schemas(profile("read"),text);for(int i=0;i<schemas.length();i++)assertFalse(schemas.getJSONObject(i).getJSONObject("function").getString("name").matches("view_image|view_video|listen_audio"));
    }

    @Test public void nativeInvalidArgumentsReturnToCoordinatorForRepairWithoutExecution()throws Exception{
        WorkspaceStore.Session p=profile("read");p.localProtocol=LocalAiLiteRt.NATIVE;int[] pass={0},calls={0};
        LocalAiAgent.run(p,new JSONArray().put(new JSONObject().put("role","user").put("text","покажи файлы")),new java.util.concurrent.atomic.AtomicBoolean(),(h,system,out)->{if(pass[0]++==0){Map<String,Object> args=new HashMap<>();args.put("path","/sdcard");args.put("limit",0);return new LocalAiAgent.Generation(LocalAiLiteRt.rawRequest("list",args).toString(),"native_tool");}assertTrue(h.toString().contains("TOOL_VALIDATION_ERROR"));assertTrue(h.toString().contains("native SDK"));return new LocalAiAgent.Generation("Нет выполненных действий","eos");},r->{calls[0]++;return "exit=0";},r->false,new LocalAiSearchTest.Events(),t->{});
        assertEquals(2,pass[0]);assertEquals(0,calls[0]);assertTrue(LocalAiLiteRt.correction("system\nCORRECTION: emit tool JSON").contains("structured SDK function call"));assertEquals("",LocalAiLiteRt.correction("system"));
    }

}
