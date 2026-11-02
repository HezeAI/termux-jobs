package com.termux.app.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

/** Presentation only: never turns local CLI readiness into model connectivity. */
final class AgentIndicatorState {
    enum State { IDLE, WORKING, WAITING, RECONNECTING, ERROR }
    static State from(JSONObject snapshot,boolean localFailure){
        if(localFailure)return State.ERROR;
        if(snapshot==null)return State.IDLE;
        JSONObject connection=snapshot.optJSONObject("modelConnection");
        String model=connection==null?"":connection.optString("state");
        if(model.equals("retrying"))return State.RECONNECTING;
        if(model.equals("failed")||model.equals("unknown")||snapshot.optBoolean("agentError"))return State.ERROR;
        JSONArray forms=snapshot.optJSONArray("forms");
        if(snapshot.optJSONObject("permission")!=null||forms!=null&&forms.length()>0||snapshot.optBoolean("needsTerminal")||snapshot.optBoolean("authRequired"))return State.WAITING;
        return snapshot.optBoolean("busy")?State.WORKING:State.IDLE;
    }
    static String label(State state,boolean ru){
        switch(state){
            case WORKING:return ru?"Выполняет запрос":"Working";
            case WAITING:return ru?"Ожидает ответа или действия пользователя":"Waiting for your answer or action";
            case RECONNECTING:return ru?"Переподключение к модели":"Reconnecting to the model";
            case ERROR:return ru?"Ошибка; подробности в чате":"Error; see chat for details";
            default:return ru?"Нет активного запроса":"No active request";
        }
    }
}
