package com.termux.app.workspace;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;

/** Native subscription windows only; passive UI, no model requests and no reset prediction. */
final class ClaudeSubscriptionMeter extends LinearLayout {
    final SubscriptionUsageLink subscription;
    final TextView updated;
    final TextView[] usage=new TextView[2],reset=new TextView[2];
    final ProgressBar[] bars=new ProgressBar[2];
    private JSONObject last;
    ClaudeSubscriptionMeter(Context context){
        super(context);setOrientation(VERTICAL);setTag("claude-subscription-meter");
        subscription=SubscriptionUsageLink.add(this,"",true);
        for(int i=0;i<2;i++){
            usage[i]=AgentStatusUi.info(this,"");usage[i].setTag("claude-limit-"+i);usage[i].setTextColor(WorkspaceDialogs.TEXT);
            bars[i]=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);bars[i].setMax(100);bars[i].setProgressTintList(ColorStateList.valueOf(WorkspaceDialogs.ACCENT));addView(bars[i],new LayoutParams(-1,WorkspaceDialogs.dp(context,18)));
            reset[i]=AgentStatusUi.info(this,"");
        }
        updated=AgentStatusUi.info(this,"");present(null);
    }
    private String t(String ru,String en){return AgentStatusUi.tr(getContext(),ru,en);}
    void disconnected(){show(last,true,System.currentTimeMillis());}
    void present(JSONObject value){last=value;show(value,false,System.currentTimeMillis());}
    void show(JSONObject value,boolean disconnected,long now){
        String plan=value==null?"":value.optString("plan");
        subscription.subscription(t("Подписка: ","Subscription: ")+(java.util.Arrays.asList("pro","max","ultra","team","enterprise").contains(plan)?"Claude "+Character.toUpperCase(plan.charAt(0))+plan.substring(1):t("нет данных","unavailable")));
        Long time=AgentContextMeter.count(value,"updatedAt");
        boolean trusted=value!=null&&value.optString("source").equals("native-get-usage")&&value.optBoolean("available");
        boolean stale=disconnected||value==null||value.optBoolean("stale")||time==null||time>now||now-time>90000;
        for(int i=0;i<2;i++){
            JSONObject window=trusted?value.optJSONObject(i==0?"fiveHour":"sevenDay"):null;
            Object raw=window==null?null:window.opt("usedPercent");double used=raw instanceof Number?((Number)raw).doubleValue():Double.NaN;
            boolean known=Double.isFinite(used)&&used>=0&&used<=100;
            Long when=AgentContextMeter.count(window,"resetsAt");boolean expired=when!=null&&when>0&&now>=when;
            String label=i==0?t("5 часов","5 hours"):t("Неделя","Week");
            usage[i].setText(label+": "+(known?Math.round(100-used)+t("% осталось","% remaining"):t("Нет данных","No data")));
            bars[i].setVisibility(known?VISIBLE:GONE);if(known){bars[i].setProgress((int)Math.round(100-used));bars[i].setContentDescription(usage[i].getText());}
            String date=when!=null&&when>0?android.text.format.DateFormat.format("dd.MM.yyyy HH:mm:ss",when).toString():t("Нет данных","No data");
            reset[i].setText(t("Сброс: ","Reset: ")+date+(known&&(stale||expired)?"\n"+t("Устаревшие данные · ожидается обновление CLI","Stale data · waiting for a CLI update"):""));
        }
        updated.setText(t("Обновлено: ","Updated: ")+(time==null||time<=0?t("Нет данных","No data"):android.text.format.DateFormat.format("dd.MM.yyyy HH:mm:ss",time).toString()));
    }
}
