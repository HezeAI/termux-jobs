package com.termux.app.workspace;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.util.Locale;

/** Passive, session-bound native context estimate. No polling, inference or compaction control. */
final class AgentContextMeter extends LinearLayout {
    final TextView usage,note;
    final ProgressBar bar;
    AgentContextMeter(Context context,String agent){
        super(context);setOrientation(VERTICAL);setTag("agent-context-meter");
        TextView title=AgentStatusUi.info(this,t("Рабочий контекст ","Working context · ")+agent);title.setTextColor(WorkspaceDialogs.TEXT);
        usage=AgentStatusUi.info(this,"");usage.setTag("agent-context-usage");
        bar=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgressTintList(ColorStateList.valueOf(WorkspaceDialogs.ACCENT));bar.setTag("agent-context-progress");addView(bar,new LayoutParams(-1,WorkspaceDialogs.dp(context,18)));
        note=AgentStatusUi.info(this,"");note.setTag("agent-context-stale");present(null);
    }
    private String t(String ru,String en){return AgentStatusUi.tr(getContext(),ru,en);}
    static Long count(JSONObject value,String key){Object raw=value==null?null:value.opt(key);if(!(raw instanceof Number))return null;double number=((Number)raw).doubleValue();long whole=((Number)raw).longValue();return Double.isFinite(number)&&number==whole&&whole>=0&&whole<=9007199254740991L?whole:null;}
    private String number(long n){return String.format(Locale.ROOT,"%,d",n).replace(',',' ');}
    void present(JSONObject context){
        JSONObject value=context==null?null:context.optJSONObject("usage");Long used=count(value,"used"),capacity=count(value,"capacity");boolean known=used!=null,window=known&&capacity!=null&&capacity>0;
        usage.setText(!known?t("Нет данных","No data"):window?t("Контекст: ","Context: ")+Math.round(used*100d/capacity)+"% · "+number(used)+" / "+number(capacity)+t(" токенов"," tokens"):t("Оценка: ","Estimate: ")+number(used)+t(" токенов · окно неизвестно"," tokens · window unknown"));
        bar.setVisibility(window?VISIBLE:GONE);if(window){bar.setProgress((int)Math.max(0,Math.min(100,Math.round(used*100d/capacity))));bar.setContentDescription(usage.getText());}
        String model=value==null?"":value.optString("model");note.setText(!known?"":(context.optBoolean("stale",true)?t("Устаревшие данные · ожидается обновление CLI","Stale data · waiting for a CLI update"):t("Последняя оценка CLI","Latest CLI estimate"))+(model.isEmpty()?"":"\n"+t("Модель: ","Model: ")+model));note.setVisibility(known?VISIBLE:GONE);
    }
}
