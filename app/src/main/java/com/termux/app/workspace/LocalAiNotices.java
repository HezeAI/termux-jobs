package com.termux.app.workspace;
import org.json.*;
/** One persistent context notice per user turn; excluded from model input. */
final class LocalAiNotices {
    static JSONObject latest(JSONArray history)throws JSONException{
        for(int i=history.length()-1;i>=0;i--){JSONObject m=history.getJSONObject(i);
            if(m.has("contextNotices"))return m;
            if(m.optString("role").equals("user")&&!m.has("toolResult")&&!m.has("contextEvent")&&!m.has("approval")&&!m.has("fileChoice")&&!m.optBoolean("repair"))break;
        }return null;
    }
    static JSONObject put(JSONArray history,String key,String text)throws JSONException{
        JSONObject record=latest(history);if(record==null){record=new JSONObject().put("role","assistant").put("contextEvent",true).put("text","").put("contextNotices",new JSONObject());history.put(record);}
        record.getJSONObject("contextNotices").put(key,text);return record;
    }
    static String details(JSONObject record)throws JSONException{StringBuilder out=new StringBuilder();JSONObject notices=record.getJSONObject("contextNotices");java.util.Iterator<String> keys=notices.keys();while(keys.hasNext()){if(out.length()>0)out.append("\n\n");out.append(notices.getString(keys.next()));}return out.toString();}
}
