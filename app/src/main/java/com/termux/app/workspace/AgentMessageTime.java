package com.termux.app.workspace;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** One immutable completion summary, never a timer started by opening history. */
final class AgentMessageTime {
    static String timelineTime(long ms){return new SimpleDateFormat("HH:mm",Locale.ROOT).format(new Date(ms));}
    static String shortTime(Context c,long ms){
        Locale locale=new Locale(WorkspaceLanguage.code(c));
        String pattern=android.text.format.DateFormat.getBestDateTimePattern(locale,android.text.format.DateFormat.is24HourFormat(c)?"Hm":"hma");
        return new SimpleDateFormat(pattern,locale).format(new Date(ms));
    }
    static String full(Context c,long ms){return DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.LONG,new Locale(WorkspaceLanguage.code(c))).format(new Date(ms))+"\n"+TimeZone.getDefault().getID();}
    static String caption(Context c,JSONObject summary){
        String clock=shortTime(c,summary.optLong("completedAt"));
        if(!summary.has("durationMs")||summary.optLong("durationMs",-1)<0)return clock;
        boolean ru=WorkspaceLanguage.code(c).equals("ru");long seconds=Math.round(summary.optLong("durationMs")/1000d),hours=seconds/3600,minutes=(seconds%3600)/60;
        String elapsed=(hours>0?hours+(ru?" ч ":"h "):"")+(minutes>0?minutes+(ru?" мин ":"m "):"")+(seconds%60)+(ru?" с":"s");
        return (ru?"Работал ":"Worked for ")+elapsed+" · "+clock;
    }
    static List<JSONObject> captions(List<JSONObject> rows){
        Map<String,Integer> last=new HashMap<>();
        for(int i=0;i<rows.size();i++){JSONObject r=rows.get(i),summary=r.optJSONObject("requestSummary");if(r.optString("kind").equals("text")&&summary!=null&&summary.optLong("completedAt")>0&&!summary.optString("request").isEmpty())last.put(summary.optString("request"),i);}
        if(last.isEmpty())return rows;
        List<JSONObject> result=new ArrayList<>(rows.size()+last.size());
        for(int i=0;i<rows.size();i++){JSONObject r=rows.get(i);result.add(r);JSONObject summary=r.optJSONObject("requestSummary");Integer end=summary==null?null:last.get(summary.optString("request"));if(end!=null&&end==i)try{result.add(new JSONObject().put("kind","completionTime").put("message",r.optString("message")).put("key","request-summary:"+summary.optString("request")).put("requestSummary",summary));}catch(JSONException impossible){throw new IllegalStateException(impossible);}}
        return result;
    }
}
