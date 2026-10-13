package com.termux.app.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

/** Account limits are shared; thread model/effort belong to this dedicated Codex terminal. */
final class CodexStatus {
    String thread = "", model = "", effort = "", state = "", error = "";
    long updated;
    JSONArray models = new JSONArray();
    JSONObject limits = new JSONObject();
    static CodexStatus fromFile(java.io.File file) throws Exception {
        if (!file.isFile()) throw new java.io.IOException("Status helper has not started");
        if (file.length()>65536) throw new java.io.IOException("Invalid status size");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        try (java.io.InputStream input=new java.io.FileInputStream(file)) {
            byte[] buffer=new byte[4096]; int count;
            while((count=input.read(buffer))!=-1) {if(bytes.size()+count>65536)throw new java.io.IOException("Invalid status size");bytes.write(buffer,0,count);}
        }
        JSONObject value=new JSONObject(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
        if(value.optInt("schema")!=1)throw new java.io.IOException("Unsupported status format");
        CodexStatus result=new CodexStatus();result.updated=value.optLong("updated");
        if(System.currentTimeMillis()-result.updated>90000 || result.updated>System.currentTimeMillis()+5000)throw new java.io.IOException("Status helper stopped responding");
        result.thread=value.optString("thread");result.model=value.optString("model");result.effort=value.optString("effort");result.state=value.optString("state");result.error=value.optString("error");
        JSONArray models=value.optJSONArray("models");if(models!=null)result.models=models;
        JSONObject limits=value.optJSONObject("limits");if(limits!=null)result.limits=limits;return result;
    }
    static CodexStatus local(java.io.File files,WorkspaceStore.Session profile) throws Exception {
        java.io.File socket=CodexSessionShell.socket(files,profile);
        java.io.File state=new java.io.File(socket.getAbsolutePath()+".state.json");
        long previous=state.lastModified();
        try(java.io.FileOutputStream request=new java.io.FileOutputStream(socket.getAbsolutePath()+".refresh")) {request.write(1);}
        long until=System.currentTimeMillis()+3000;
        while(state.lastModified()==previous && System.currentTimeMillis()<until)Thread.sleep(50);
        if(!state.isFile()) {CodexStatus pending=new CodexStatus();pending.error="Помощник статуса ещё не запущен. Для сессии, созданной до v0.2.13, откройте новый терминал Codex с тем же ID чата.";return pending;}
        return fromFile(state);
    }
    static CodexStatus read(CodexRpc rpc, String preferredThread) throws Exception {
        CodexStatus result = new CodexStatus();
        try { result.limits = rpc.call("account/rateLimits/read", new JSONObject()).optJSONObject("rateLimits"); } catch (java.io.IOException unavailable) { result.limits = new JSONObject(); }
        if (result.limits == null) result.limits = new JSONObject();
        JSONObject listed = rpc.call("thread/loaded/list", new JSONObject());
        JSONArray ids = listed.optJSONArray("data");
        if (ids != null) for (int i = 0; i < ids.length(); i++) {
            String id = ids.getString(i);
            JSONObject thread = rpc.call("thread/read", new JSONObject().put("threadId", id).put("includeTurns", false)).getJSONObject("thread");
            // Ignore agent descendants. One root chat is owned by this server/TUI.
            if (!thread.isNull("parentThreadId") && !thread.optString("parentThreadId").isEmpty()) continue;
            if (!preferredThread.isEmpty() && !id.equals(preferredThread)) continue;
            if (!result.thread.isEmpty()) { result.thread = ""; result.error = "Несколько чатов: выберите чат в терминале"; break; }
            result.thread = id; result.model = thread.optString("model", ""); result.effort = thread.optString("reasoningEffort", "");
            JSONObject status = thread.optJSONObject("status"); result.state = status == null ? "" : status.optString("type");
        }
        result.updated = System.currentTimeMillis(); return result;
    }
    static String limit(JSONObject limits, String window, String label) {
        JSONObject value = limits == null ? null : limits.optJSONObject(window);
        if (value == null || !value.has("usedPercent")) return label + ": нет данных";
        int remaining = Math.max(0, Math.min(100, (int)Math.round(100 - value.optDouble("usedPercent", 100))));
        return label + ": " + remaining + "%";
    }
    String summary() { return limit(limits, "primary", "5ч") + " · " + limit(limits, "secondary", "7д"); }
}
