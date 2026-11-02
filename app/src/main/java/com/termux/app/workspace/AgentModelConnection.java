package com.termux.app.workspace;

import org.json.JSONObject;

/** Presentation of a reported native event, never an inferred network health probe. */
final class AgentModelConnection {
    static String label(JSONObject event,boolean ru){
        if(event==null)return "";
        if(event.optString("state").equals("unknown"))return ru?"Нет связи с CLI · состояние ответа неизвестно ▾":"CLI disconnected · response state unknown ▾";
        if(event.optString("state").equals("failed"))return ru?"Ответ прерван · ошибка CLI ▾":"Response interrupted · CLI error ▾";
        if(!event.optString("state").equals("retrying"))return "";
        long n=event.optLong("attempt",0),limit=event.optLong("limit",0);
        return (ru?"Переподключение…":"Reconnecting…")+(n>0?" "+n+(limit>=n?"/"+limit:""):"")+" ▾";
    }
    static String details(JSONObject event,boolean ru){
        return (ru?"Это состояние связи с моделью, сообщённое CLI. Готовность локального CLI и приём уточнения не подтверждают восстановление ответа.\n\n":"This is the model connection state reported by the CLI. Local CLI readiness and steering admission do not confirm response recovery.\n\n")+
            event.optString("reason")+"\n"+event.optString("details")+"\n\n"+(ru?"Запрос: ":"Request: ")+event.optString("turn");
    }
}
