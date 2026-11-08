package com.termux.app.workspace;

import org.json.JSONObject;

/** Captured run identity and native-chat binding; no process or history mutation here. */
final class WorkspaceRestart {
    private WorkspaceRestart(){}
    static WorkspaceStore.Session snapshot(WorkspaceStore.Session p){
        WorkspaceStore.Session s=AiAgents.snapshot(p);
        s.codexSocketId=p.codexSocketId;s.claudeSocketId=p.claudeSocketId;s.geminiSocketId=p.geminiSocketId;
        s.claudeChat=p.claudeChat;s.claudeThread=p.claudeThread;s.geminiChat=p.geminiChat;s.geminiThread=p.geminiThread;
        return s;
    }
    static boolean sameRun(WorkspaceStore.Session p,WorkspaceStore.Session s){
        return p.id.equals(s.id)&&p.handle.equals(s.handle)&&p.environment.equals(s.environment)
            &&p.codexEnvironment.equals(s.codexEnvironment)&&p.aiConnection.equals(s.aiConnection)&&p.aiAgent.equals(s.aiAgent)&&p.claudeAgent==s.claudeAgent&&p.geminiAgent==s.geminiAgent
            &&p.codexSocketId.equals(s.codexSocketId)&&p.claudeSocketId.equals(s.claudeSocketId)
            &&p.geminiSocketId.equals(s.geminiSocketId)&&p.aiSocketId.equals(s.aiSocketId);
    }
    static boolean bindRecorded(WorkspaceStore.Session p){
        // Codex assigns a new conversation remotely. A previous field value in
        // "new" mode is not evidence that this run belongs to that conversation.
        if(p.environment.equals("codex")&&!p.codexChat.equals("id"))return false;
        String id=p.claudeAgent?p.claudeThread:p.geminiAgent?p.geminiThread:p.aiAgent.equals("opencode")?(!p.aiSavedThread.isEmpty()?p.aiSavedThread:p.aiThread):p.codexThread;
        try{return bind(p,new JSONObject().put("nativeChat",true).put("session",id));}catch(org.json.JSONException impossible){return false;}
    }
    static boolean bind(WorkspaceStore.Session p,JSONObject reply){
        if(reply==null||!reply.optBoolean("nativeChat"))return false;
        String id=reply.optString("session","");
        if(p.claudeAgent&&ClaudeCode.validThread(id)){p.claudeChat="id";p.claudeThread=id;return true;}
        if(p.geminiAgent&&GeminiCli.validThread(id)){p.geminiChat="id";p.geminiThread=id;return true;}
        if(p.aiAgent.equals("opencode")&&AiAgents.validThread(id)){p.aiChat="resume";p.aiThread=id;p.aiSavedThread=id;return true;}
        if(p.environment.equals("codex")&&CodexSessionShell.validThread(id)){p.codexChat="id";p.codexThread=id;return true;}
        return false;
    }
}
