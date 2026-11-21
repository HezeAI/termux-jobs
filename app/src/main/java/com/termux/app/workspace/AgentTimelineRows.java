package com.termux.app.workspace;

import java.util.*;
import org.json.*;

/** Display projection only. Original event keys and bodies remain the detail source. */
final class AgentTimelineRows {
    static boolean web(JSONObject row){
        JSONObject preview=AgentActionPresentation.preview(row);
        return row.optString("toolType").equals("webSearch")||preview!=null&&preview.optString("kind").equals("web");
    }
    static List<JSONObject> project(List<JSONObject> rows){
        Set<String> calls=new HashSet<>();
        for(JSONObject row:rows)if(row.optString("actionRole").equals("call")&&!row.optString("actionId").isEmpty())calls.add(row.optString("actionId"));
        List<JSONObject> result=new ArrayList<>();
        for(JSONObject row:rows){
            if(row.optString("actionRole").equals("result")&&calls.contains(row.optString("actionId")))continue;
            if(row.optString("kind").equals("tool")&&web(row)){
                List<AgentWebActions.Entry> entries=AgentWebActions.entries(row);
                for(int i=0;i<entries.size();i++)try{
                    AgentWebActions.Entry entry=entries.get(i);JSONObject copy=new JSONObject(row.toString());
                    copy.put("detailKey",row.optString("key")).put("webLabel",entry.text).put("webSearch",entry.search);
                    if(i>0)copy.put("key",entry.key(row.optString("key")));
                    result.add(copy);
                }catch(JSONException impossible){throw new IllegalStateException(impossible);}
            }else result.add(row);
        }
        return result;
    }
    static JSONObject detail(JSONObject row){
        if(row.optString("detailKey").isEmpty())return row;
        try{return new JSONObject(row.toString()).put("key",row.optString("detailKey"));}
        catch(JSONException impossible){throw new IllegalStateException(impossible);}
    }
}
