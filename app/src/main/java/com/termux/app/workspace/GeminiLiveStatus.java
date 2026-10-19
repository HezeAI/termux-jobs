package com.termux.app.workspace;

import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;

/** Sanitized, local scalar snapshot from one owned Gemini run. Never reads its transcript/auth. */
final class GeminiLiveStatus {
    String model="",selectedModel="",mode="",auth="",state="starting",error="";
    String changeId="",changeModel="",changeState="";
    boolean available,canChangeMode,canPlan,canEdit;
    String thinking="",thinkingModel="";boolean canChangeThinking,thinkingEffective;java.util.List<String> thinkingOptions=new java.util.ArrayList<>();
    String permissionId="";
    String modeChangeId="",modeChangeMode="",modeChangeState="";
    long updated,startedAt,requests,input,output,cache,thoughts,total,apiMs;
    static GeminiLiveStatus parse(JSONObject v,String run,long now) throws Exception {
        if(v.getInt("schema")!=1||!run.equals(v.getString("run")))throw new IOException("RUN_MISMATCH");
        GeminiLiveStatus s=new GeminiLiveStatus();s.updated=v.getLong("updated");s.startedAt=v.getLong("startedAt");
        if(s.updated>now+5000||now-s.updated>15000)throw new IOException("STALE_STATUS");
        s.model=model(v.optString("model"));s.selectedModel=model(v.optString("selectedModel"));
        s.mode=one(v.optString("mode"),"default|autoEdit|auto_edit|plan|yolo");s.auth=one(v.optString("auth"),"gemini-api-key|oauth-personal|vertex-ai|gateway|compute-default-credentials");
        s.state=one(v.optString("state"),"starting|idle|responding|waiting_for_confirmation|unsupported|ended");
        s.available=v.optBoolean("available")&&!s.state.equals("unsupported")&&!s.state.equals("ended");
        s.requests=count(v,"requests");s.input=count(v,"input");s.output=count(v,"output");s.cache=count(v,"cache");s.thoughts=count(v,"thoughts");s.total=count(v,"total");s.apiMs=count(v,"apiMs");
        JSONObject change=v.optJSONObject("change");if(change!=null){s.changeId=one(change.optString("id"),"[-a-zA-Z0-9]{1,64}");s.changeModel=model(change.optString("model"));s.changeState=one(change.optString("state"),"queued|applied|failed|cancelled");}
        JSONObject thinking=v.optJSONObject("thinking");if(thinking!=null){s.thinking=one(thinking.optString("value"),"default|minimal|low|medium|high");s.thinkingModel=model(thinking.optString("model"));s.canChangeThinking=thinking.optBoolean("available");s.thinkingEffective=thinking.optBoolean("effective");org.json.JSONArray options=thinking.optJSONArray("options");if(options!=null)for(int i=0;i<Math.min(options.length(),5);i++){String option=one(options.optString(i),"default|minimal|low|medium|high");if(!option.isEmpty()&&!s.thinkingOptions.contains(option))s.thinkingOptions.add(option);}}
        s.permissionId=GeminiCli.validThread(v.optString("permissionId"))?v.optString("permissionId"):"";
        s.canChangeMode=v.optBoolean("canChangeMode");s.canPlan=v.optBoolean("canPlan");s.canEdit=v.optBoolean("canEdit");
        JSONObject m=v.optJSONObject("modeChange");if(m!=null){s.modeChangeId=one(m.optString("id"),"[-a-zA-Z0-9]{1,64}");s.modeChangeMode=one(m.optString("mode"),"default|auto_edit|plan");s.modeChangeState=one(m.optString("state"),"queued|applied|failed|cancelled");}
        return s;
    }
    private static String one(String value,String pattern){return value.matches(pattern)?value:"";}
    private static String model(String value){return GeminiLiveControl.validModel(value)?value:"";}
    private static long count(JSONObject o,String key){long n=o.optLong(key);return n>=0&&n<=9007199254740991L?n:0;}
    static GeminiLiveStatus read(File files,WorkspaceStore.Session s){
        try{
            if(!s.geminiAgent||!GeminiCli.validThread(s.geminiSocketId))throw new IOException("INVALID_RUN");
            File f=new File(GeminiCli.directory(files),s.geminiSocketId+".status.json");
            if(Files.isSymbolicLink(f.toPath())||!Files.isRegularFile(f.toPath(),LinkOption.NOFOLLOW_LINKS)||f.length()>16384)throw new IOException("NO_STATUS");
            return parse(new JSONObject(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8)),s.geminiSocketId,System.currentTimeMillis());
        }catch(Exception e){GeminiLiveStatus out=new GeminiLiveStatus();out.error="UNAVAILABLE";return out;}
    }
}
