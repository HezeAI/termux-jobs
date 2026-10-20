package com.termux.app.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

/** Account limits are shared; thread model/effort belong to this dedicated Codex terminal. */
final class CodexStatus {
    String cwd = "", title = "", planType = "", accountType = "";
    String thread = "", model = "", effort = "", state = "", error = "";
    long updated;boolean modelPending;Boolean materialized;
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
        if(value.opt("materialized") instanceof Boolean)result.materialized=value.getBoolean("materialized");
        result.cwd=value.optString("cwd");result.title=value.optString("title");result.thread=value.optString("thread");result.model=value.optString("model");result.effort=value.optString("effort");result.state=value.optString("state");result.error=value.optString("error");
        JSONArray models=value.optJSONArray("models");if(models!=null)result.models=models;
        JSONObject limits=value.optJSONObject("limits");if(limits!=null)result.limits=limits;result.planType=plan(value.optString("planType",result.limits.optString("planType")));result.accountType=accountType(value.optString("accountType"));return result;
    }
    static CodexStatus local(java.io.File files,WorkspaceStore.Session profile) throws Exception {
        java.io.File socket=CodexSessionShell.socket(files,profile);
        java.io.File state=new java.io.File(socket.getAbsolutePath()+".state.json");
        long previous=state.lastModified();
        try(java.io.FileOutputStream request=new java.io.FileOutputStream(socket.getAbsolutePath()+".refresh")) {request.write(1);}
        long until=System.currentTimeMillis()+3000;
        while(state.lastModified()==previous && System.currentTimeMillis()<until)Thread.sleep(50);
        if(!state.isFile()) {CodexStatus pending=new CodexStatus();pending.error=WorkspaceText.tr("Помощник статуса ещё не запущен. Для сессии, созданной до v0.2.13, откройте новый терминал Codex с тем же ID чата.");return pending;}
        CodexStatus result=fromFile(state);
        if(CodexChatControl.enabled(files,profile))try{JSONObject choice=CodexChatControl.request(files,profile,new JSONObject().put("op","status"));if(choice.optString("thread").equals(result.thread)&&!result.thread.isEmpty()){result.model=choice.optString("model",result.model);result.effort=choice.optString("effort",result.effort);result.modelPending=choice.optBoolean("pending");}}catch(Exception ignored){}
        return result;
    }
    static CodexStatus read(CodexRpc rpc, String preferredThread) throws Exception {
        CodexStatus result = new CodexStatus();
        try { result.limits = rpc.call("account/rateLimits/read", new JSONObject()).optJSONObject("rateLimits"); } catch (java.io.IOException unavailable) { result.limits = new JSONObject(); }
        if (result.limits == null) result.limits = new JSONObject();
        result.planType=plan(result.limits.optString("planType"));
        try {JSONObject account=rpc.call("account/read",new JSONObject().put("refreshToken",false)).optJSONObject("account");if(account!=null){result.accountType=accountType(account.optString("type"));if(result.accountType.equals("chatgpt"))result.planType=plan(account.optString("planType",result.planType));}}catch(java.io.IOException unavailable){}
        JSONObject listed = rpc.call("thread/loaded/list", new JSONObject());
        JSONArray ids = listed.optJSONArray("data");
        if (ids != null) for (int i = 0; i < ids.length(); i++) {
            String id = ids.getString(i);
            JSONObject thread = rpc.call("thread/read", new JSONObject().put("threadId", id).put("includeTurns", false)).getJSONObject("thread");
            // Ignore agent descendants. One root chat is owned by this server/TUI.
            if (!thread.isNull("parentThreadId") && !thread.optString("parentThreadId").isEmpty()) continue;
            if (!preferredThread.isEmpty() && !id.equals(preferredThread)) continue;
            if (!result.thread.isEmpty()) { result.thread = ""; result.error = WorkspaceText.tr("Несколько чатов: выберите чат в терминале"); break; }
            result.thread = id; result.model = thread.optString("model", ""); result.effort = thread.optString("reasoningEffort", "");
            JSONObject status = thread.optJSONObject("status"); result.state = status == null ? "" : status.optString("type");
        }
        result.updated = System.currentTimeMillis(); return result;
    }
    private static String tr(String ru,String en){return WorkspaceText.tr("нет данных").equals("нет данных")?ru:en;}
    static String plan(String value){return value!=null&&value.matches("[a-z][a-z0-9_-]{0,47}")?value:"";}
    private static String accountType(String value){return value.equals("chatgpt")||value.equals("apiKey")?value:"";}
    String planLabel(){
        if(accountType.equals("apiKey"))return tr("Вход: API-ключ","Sign-in: API key");
        String name;
        switch(planType){case "free":name="Free";break;case "go":name="Go";break;case "plus":name="Plus";break;case "pro":name="Pro";break;case "team":name="Team";break;case "business":name="Business";break;case "enterprise":name="Enterprise";break;case "edu":name="Edu";break;case "":case "unknown":case "none":return tr("Подписка: нет данных","Subscription: unavailable");default:name=planType;}
        return tr("Подписка: ChatGPT ","Subscription: ChatGPT ")+name;
    }
    java.util.List<String> windows(){java.util.List<String> out=new java.util.ArrayList<>();for(String key:new String[]{"primary","secondary"})if(limits.optJSONObject(key)!=null)out.add(key);return out;}
    static Integer remaining(JSONObject value){
        Object used=value==null?null:value.opt("usedPercent");if(!(used instanceof Number))return null;
        double percent=((Number)used).doubleValue();if(!Double.isFinite(percent)||percent<0)return null;
        return Math.max(0,Math.min(100,(int)Math.round(100-percent)));
    }
    static String windowLabel(JSONObject limits,String key,boolean compact){
        JSONObject value=limits==null?null:limits.optJSONObject(key);Object durationValue=value==null?null:value.opt("windowDurationMins");long minutes=durationValue instanceof Number?((Number)durationValue).longValue():0;if(durationValue instanceof Number&&((Number)durationValue).doubleValue()!=minutes)minutes=0;
        if(minutes==300)return compact?WorkspaceText.tr("5ч"):WorkspaceText.tr("Пятичасовой лимит");
        if(minutes==10080)return compact?WorkspaceText.tr("7д"):WorkspaceText.tr("Недельный лимит");
        if(minutes>0){String duration=minutes%1440==0?(minutes/1440)+tr("д","d"):minutes%60==0?(minutes/60)+tr("ч","h"):minutes+tr("мин","min");return compact?duration:tr("Лимит за ","Limit over ")+duration;}
        return compact?tr("Лимит ","Limit ")+(key.equals("primary")?"1":"2"):tr("Лимит · период не указан","Limit · period unspecified");
    }
    static String limit(JSONObject limits, String window, String label) {
        Integer remaining=remaining(limits==null?null:limits.optJSONObject(window));
        return label+(remaining==null?WorkspaceText.tr(": нет данных"):": "+remaining+"%");
    }
    String summary() {
        java.util.List<String> values=new java.util.ArrayList<>();for(String key:windows())values.add(limit(limits,key,windowLabel(limits,key,true)));
        return values.isEmpty()?tr("Лимиты: нет данных","Limits: unavailable"):android.text.TextUtils.join(" · ",values);
    }
}
