package com.termux.app.workspace;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.*;

/** Shared presentation only. Native catalogs, authorization, limits and policies stay provider-owned. */
final class AgentStatusUi {
    private AgentStatusUi(){}
    static String tr(Context c,String ru,String en){return WorkspaceLanguage.code(c).equals("ru")?ru:en;}
    static TextView chip(Context c,String label,String description,Runnable click){
        TextView v=new TextView(c);v.setText(label);v.setTextSize(12);v.setTextColor(WorkspaceDialogs.ACCENT);
        v.setGravity(Gravity.CENTER);v.setMinHeight(WorkspaceDialogs.dp(c,34));v.setMaxLines(2);v.setEllipsize(TextUtils.TruncateAt.END);
        v.setPadding(WorkspaceDialogs.dp(c,12),WorkspaceDialogs.dp(c,4),WorkspaceDialogs.dp(c,12),WorkspaceDialogs.dp(c,4));
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4479dfcd),WorkspaceDialogs.background(c,false),null));
        v.setContentDescription(description);v.setOnClickListener(view->click.run());return v;
    }
    static void attach(LinearLayout parent,TextView chip){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=lp.bottomMargin=WorkspaceDialogs.dp(parent.getContext(),6);parent.addView(chip,lp);}
    static LinearLayout body(Context c){LinearLayout body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(WorkspaceDialogs.dp(c,12),WorkspaceDialogs.dp(c,4),WorkspaceDialogs.dp(c,12),WorkspaceDialogs.dp(c,12));return body;}
    static TextView row(LinearLayout body,String label,String key,Runnable click){Context c=body.getContext();TextView v=WorkspaceDialogs.action(c,label,0);v.setTextSize(13);v.setGravity(Gravity.CENTER);v.setMinHeight(WorkspaceDialogs.dp(c,48));v.setTag("agent-status-"+key);v.setOnClickListener(view->click.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=WorkspaceDialogs.dp(c,8);body.addView(v,lp);return v;}
    static void available(TextView row,boolean enabled){row.setEnabled(enabled);row.setAlpha(enabled?1f:.45f);}
    static String level(Context c,String value){switch(value){case "default":return tr(c,"По умолчанию CLI","CLI default");case "minimal":return tr(c,"Минимальный","Minimal");case "low":return tr(c,"Низкий","Low");case "medium":return tr(c,"Средний","Medium");case "high":return tr(c,"Высокий","High");case "xhigh":return tr(c,"Очень высокий","Extra high");case "max":return tr(c,"Максимальный","Maximum");default:return tr(c,"Нет данных","Unavailable");}}
    static TextView info(LinearLayout body,String label){TextView v=new TextView(body.getContext());v.setTag("agent-status-info");v.setText(label);v.setTextSize(12);v.setTextColor(WorkspaceDialogs.MUTED);int pad=WorkspaceDialogs.dp(body.getContext(),4);v.setPadding(pad,pad,pad,pad);v.setTextIsSelectable(true);body.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    static ScrollView scroll(LinearLayout body){ScrollView scroll=new ScrollView(body.getContext());scroll.setFillViewport(true);scroll.addView(body);return scroll;}
}
