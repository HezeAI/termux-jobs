package com.termux.app.workspace;

import android.content.Context;
import org.json.*;
import java.util.*;

/** Passive per-chat projections. Never starts a CLI or reads account credentials. */
final class AgentOfflineData {
    static String identity(WorkspaceStore.Session p) {
        return p.id+":"+p.codexEnvironment+":"+(p.claudeAgent?p.claudeThread:p.geminiAgent?p.geminiThread:p.aiAgent.equals("opencode")?p.aiThread:p.codexThread);
    }
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("agent-offline-data",Context.MODE_PRIVATE);}
    static JSONObject read(Context c,WorkspaceStore.Session p,String kind){try{return new JSONObject(prefs(c).getString(identity(p)+":"+kind,"{}"));}catch(JSONException e){return new JSONObject();}}
    static void remember(Context c,WorkspaceStore.Session p,String kind,JSONObject data,long time){
        if(data==null||data.length()==0||time<=0)return;
        if(kind.equals("context")&&(data.optJSONObject("usage")==null||AgentContextMeter.count(data.optJSONObject("usage"),"used")==null))return;
        if(kind.equals("limits")&&data.has("available")&&!data.optBoolean("available"))return;
        try{JSONObject old=read(c,p,kind);if(data.toString().equals(old.optJSONObject("data")==null?"":old.optJSONObject("data").toString()))return;
            prefs(c).edit().putString(identity(p)+":"+kind,new JSONObject().put("time",time).put("data",data).toString()).apply();
        }catch(JSONException ignored){}
    }
    static void files(Context c,WorkspaceStore.Session p,List<JSONObject> rows,boolean complete){
        try{JSONArray all=new JSONArray();for(ChatFileReferences.Entry e:ChatFileReferences.collect(rows)){all.put(new JSONObject().put("key",e.key).put("message",e.message).put("kind",e.sender.equals("user")?"user":"text").put("messageSender",e.sender).put("messageTime",e.time).put("attachment",e.file));}
            if(all.toString().length()>1024*1024)return;
            remember(c,p,"files",new JSONObject().put("rows",all).put("complete",complete),System.currentTimeMillis());
        }catch(JSONException ignored){}
    }
    static void showLimits(com.termux.app.TermuxActivity a,WorkspaceStore.Session p){
        android.widget.LinearLayout body=AgentStatusUi.body(a);
        for(String kind:new String[]{"limits","context"}){
            JSONObject saved=read(a,p,kind),data=saved.optJSONObject("data");
            AgentStatusUi.info(body,AgentStatusUi.tr(a,kind.equals("limits")?"Лимиты":"Контекст",kind.equals("limits")?"Limits":"Context"));
            if(data==null){AgentStatusUi.info(body,AgentStatusUi.tr(a,"Нет данных","No data"));continue;}
            AgentStatusUi.info(body,AgentStatusUi.tr(a,"Последние известные данные · ","Last known data · ")+android.text.format.DateFormat.format("dd.MM.yyyy HH:mm",saved.optLong("time")));
            if(kind.equals("context")){try{data.put("stale",true);}catch(JSONException ignored){}AgentContextMeter meter=new AgentContextMeter(a,AgentPreparation.agent(p));meter.present(data);body.addView(meter);}
            else{JSONObject windows=data.optJSONObject("windows");if(windows==null)windows=data;boolean any=false;Iterator<String> keys=windows.keys();while(keys.hasNext()){String key=keys.next();JSONObject window=windows.optJSONObject(key);if(window==null||!(window.opt("usedPercent") instanceof Number))continue;any=true;double used=window.optDouble("usedPercent");String caption=key.equals("fiveHour")?AgentStatusUi.tr(a,"5 часов","5 hours"):key.equals("sevenDay")?AgentStatusUi.tr(a,"7 дней","7 days"):window.optInt("windowDurationMins")==300?AgentStatusUi.tr(a,"5 часов","5 hours"):window.optInt("windowDurationMins")==10080?AgentStatusUi.tr(a,"7 дней","7 days"):key;AgentStatusUi.info(body,caption+": "+String.format(Locale.ROOT,"%.1f%%",used)+AgentStatusUi.tr(a," использовано"," used"));}if(!any)AgentStatusUi.info(body,AgentStatusUi.tr(a,"Нет данных","No data"));}
        }
        android.widget.TextView compact=AgentStatusUi.row(body,AgentStatusUi.tr(a,"Сократить контекст","Compact context"),"offline-compact",()->{});compact.setEnabled(false);
        AgentStatusUi.info(body,AgentStatusUi.tr(a,"Доступно после запуска чата","Available after starting the chat"));
        RtkMode rtk=new RtkMode(a,p,null,true);body.addView(rtk);
        android.app.AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(AgentStatusUi.tr(a,"Лимиты и контекст","Limits and context")).setView(AgentStatusUi.scroll(body)).setNegativeButton(AgentStatusUi.tr(a,"Закрыть","Close"),null).create();dialog.setOnDismissListener(d->rtk.close());dialog.show();
    }
}
