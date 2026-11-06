package com.termux.app.workspace;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.*;
import org.json.JSONObject;

/** Compact projection of known native windows; never fetches data or predicts a reset. */
final class ComposerLimits extends LinearLayout {
    final TextView label;
    final TextView[] captions=new TextView[2];
    final ProgressBar[] bars=new ProgressBar[2];
    ComposerLimits(Context c){super(c);setOrientation(VERTICAL);setTag("composer-limits");label=new TextView(c);label.setTextSize(10);label.setTextColor(WorkspaceDialogs.MUTED);addView(label);
        LinearLayout windows=new LinearLayout(c);addView(windows,new LayoutParams(-1,-2));
        for(int i=0;i<2;i++){LinearLayout window=new LinearLayout(c);window.setOrientation(VERTICAL);window.setPadding(4,0,4,0);windows.addView(window,new LayoutParams(0,-2,1));captions[i]=new TextView(c);captions[i].setTextSize(10);captions[i].setTextColor(WorkspaceDialogs.MUTED);window.addView(captions[i]);bars[i]=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);bars[i].setMax(100);bars[i].setProgressTintList(ColorStateList.valueOf(WorkspaceDialogs.ACCENT));window.addView(bars[i],new LayoutParams(-1,WorkspaceDialogs.dp(c,6)));}
        present(null,null,false,false);
    }
    private String t(String ru,String en){return AgentStatusUi.tr(getContext(),ru,en);}
    void codex(CodexStatus status,long now){JSONObject first=null,second=null;
        if(status!=null&&status.error.isEmpty()&&!status.accountType.equals("apiKey"))for(String key:status.windows()){JSONObject window=status.limits.optJSONObject(key);if(window!=null){Object duration=window.opt("windowDurationMins");if(duration instanceof Number&&((Number)duration).doubleValue()==300)first=window;else if(duration instanceof Number&&((Number)duration).doubleValue()==10080)second=window;}}
        boolean stale=status==null||status.updated<=0||status.updated>now||now-status.updated>90000;
        present(first,second,stale||expired(first,now,1000),stale||expired(second,now,1000));
    }
    void claude(JSONObject subscription,long now){boolean trusted=subscription!=null&&subscription.optBoolean("available")&&subscription.optString("source").equals("native-get-usage");JSONObject first=trusted?subscription.optJSONObject("fiveHour"):null,second=trusted?subscription.optJSONObject("sevenDay"):null;Long time=AgentContextMeter.count(subscription,"updatedAt");boolean stale=time==null||time<=0||time>now||now-time>90000||subscription.optBoolean("stale");present(first,second,stale||expired(first,now,1),stale||expired(second,now,1));}
    private boolean expired(JSONObject window,long now,int factor){Long time=AgentContextMeter.count(window,"resetsAt");return time!=null&&time>0&&time<=now/factor;}
    void unavailable(){label.setText(t("Лимиты: Нет данных","Limits: No data"));for(int i=0;i<2;i++){captions[i].setVisibility(GONE);bars[i].setVisibility(GONE);}}
    void present(JSONObject first,JSONObject second,boolean staleFirst,boolean staleSecond){
        label.setText(t("Лимиты: остаток","Limits: remaining"));JSONObject[] windows={first,second};boolean[] stale={staleFirst,staleSecond};
        for(int i=0;i<2;i++){captions[i].setVisibility(VISIBLE);Object raw=windows[i]==null?null:windows[i].opt("usedPercent");double used=raw instanceof Number?((Number)raw).doubleValue():Double.NaN;boolean known=Double.isFinite(used)&&used>=0&&used<=100;String caption=(i==0?t("5ч","5h"):t("7д","7d"))+" · "+(known?Math.round(100-used)+"%"+(stale[i]?t(" · устарело"," · stale"):""):t("Нет данных","No data"));if(!caption.contentEquals(captions[i].getText()))captions[i].setText(caption);bars[i].setVisibility(known?VISIBLE:GONE);if(known)bars[i].setProgress((int)Math.round(100-used));bars[i].setContentDescription(caption);}
    }
}
