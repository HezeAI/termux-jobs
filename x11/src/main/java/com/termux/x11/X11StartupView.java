package com.termux.x11;
import android.content.Context;
import android.view.Gravity;
import android.widget.*;
/** Milestones only: no timer, recurring animation or estimated duration. */
public final class X11StartupView extends LinearLayout {
    private final ProgressBar progress;
    private final TextView notice,title,detail,more;
    private final Button cancel;
    private final Button reveal;
    public X11StartupView(Context context,Runnable show){
        super(context);setGravity(Gravity.CENTER);setOrientation(VERTICAL);
        setBackgroundColor(0xff000000);
        for(String label:new String[]{"Termux Job’s","+","X11"}){
            TextView emblem=new TextView(context);android.text.SpannableString script=new android.text.SpannableString(label);script.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),0,script.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);emblem.setText(script);emblem.setTextSize(label.equals("+")?20:28);
            emblem.setTypeface(android.graphics.Typeface.create("cursive",android.graphics.Typeface.ITALIC));
            emblem.setTextColor(0xff79dacb);emblem.setGravity(Gravity.CENTER);emblem.setTag("x11-emblem-"+label);
            addView(emblem,new LayoutParams(dp(240),-2));
        }
        title=new TextView(context);title.setText(X11Text.tr(context,"Запуск рабочего стола","Starting desktop"));title.setTextSize(16);title.setTextColor(0xffe0ebef);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(20),0,dp(14));title.setTag("x11-preparation-title");addView(title);
        progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(false);progress.setMax(4);progress.setProgress(0);progress.setTag("x11-startup-progress");progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff79dacb));
        addView(progress,new LayoutParams(dp(240),dp(8)));
        notice=new TextView(context);notice.setTextSize(13);notice.setTextColor(0xffa6bdc6);notice.setPadding(16,16,16,16);notice.setGravity(Gravity.CENTER);notice.setTag("x11-startup-step");notice.setText(X11Text.tr(context,"Подготовка X11","Preparing X11"));addView(notice);
        detail=new TextView(context);detail.setTextSize(12);detail.setTextColor(0xffa6bdc6);detail.setSingleLine(true);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);detail.setGravity(Gravity.CENTER);detail.setTag("x11-preparation-detail");addView(detail,new LayoutParams(dp(280),-2));
        more=new TextView(context);more.setText(X11Text.tr(context,"Подробнее","Details"));more.setTextColor(0xff79dacb);more.setTextSize(13);more.setGravity(Gravity.CENTER);more.setPadding(dp(16),dp(10),dp(16),dp(10));more.setTag("x11-preparation-details");more.setVisibility(GONE);
        android.graphics.drawable.GradientDrawable morePanel=new android.graphics.drawable.GradientDrawable();morePanel.setColor(0xff0b1319);morePanel.setCornerRadius(dp(12));morePanel.setStroke(dp(1),0xff213039);more.setBackground(morePanel);LayoutParams moreLayout=new LayoutParams(-2,-2);moreLayout.topMargin=dp(8);addView(more,moreLayout);
        cancel=new Button(context);cancel.setAllCaps(false);cancel.setTextSize(13);cancel.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff91a6af,0xffedf5f6}));cancel.setGravity(Gravity.CENTER);cancel.setPadding(dp(13),dp(8),dp(13),dp(8));cancel.setMinWidth(0);cancel.setMinimumWidth(0);cancel.setMinHeight(dp(48));cancel.setMinimumHeight(dp(48));cancel.setStateListAnimator(null);cancel.setElevation(0);cancel.setBackgroundTintList(null);
        android.graphics.drawable.GradientDrawable panel=new android.graphics.drawable.GradientDrawable();panel.setColor(0xff0b1319);panel.setCornerRadius(dp(12));panel.setStroke(dp(1),0xff213039);
        cancel.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x4479dfcd),panel,null));cancel.setVisibility(GONE);cancel.setTag("x11-preparation-cancel");LayoutParams cancelLayout=new LayoutParams(-2,-2);cancelLayout.topMargin=dp(12);addView(cancel,cancelLayout);
        reveal=new Button(context);reveal.setText(X11Text.tr(context,"Показать экран X11","Show X11 screen"));reveal.setVisibility(GONE);reveal.setOnClickListener(v->show.run());addView(reveal);
    }
    public void preparation(String heading,String status,int percent,String line,String action,boolean enabled,Runnable clicked){
        if(!title.getText().toString().equals(heading))title.setText(heading);
        progress.setMax(100);progress.setIndeterminate(percent<0);progress.setVisibility(VISIBLE);if(percent>=0)progress.setProgress(Math.max(0,Math.min(100,percent)));
        if(!notice.getText().toString().equals(status))notice.setText(status);
        if(!detail.getText().toString().equals(line))detail.setText(line);
        reveal.setVisibility(GONE);cancel.setVisibility(VISIBLE);cancel.setText(action);cancel.setEnabled(enabled);cancel.setOnClickListener(v->clicked.run());
    }
    public void details(Runnable open){more.setVisibility(open==null?GONE:VISIBLE);more.setOnClickListener(open==null?null:v->open.run());detail.setOnClickListener(open==null?null:v->open.run());}
    public void preparationIdle(){progress.setVisibility(GONE);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    void update(boolean ready,boolean timedOut,int step){
        int value=ready?4:Math.max(0,Math.min(3,step));
        if(ready||!timedOut){progress.setVisibility(VISIBLE);progress.setProgress(value);reveal.setVisibility(GONE);
            String[] ru={"Подготовка X11","Запуск графического ядра","Запуск XFCE","Ожидание панели и рабочего стола","Готово"};
            String[] en={"Preparing X11","Starting graphics core","Starting XFCE","Waiting for the panel and desktop","Ready"};
            String text=X11Text.tr(getContext(),ru[value],en[value]);if(!notice.getText().toString().equals(text))notice.setText(text);
        }else{progress.setVisibility(GONE);notice.setText(X11Text.tr(getContext(),"Рабочий стол запускается дольше обычного. Можно открыть экран X11.","The desktop is taking longer than usual. You can open the X11 screen."));reveal.setVisibility(VISIBLE);}
    }
    void failed(){progress.setVisibility(GONE);reveal.setVisibility(GONE);notice.setText(X11Text.tr(getContext(),"Не удалось подключить экран X11. Вернитесь в терминал и повторите.","Could not attach X11. Return to terminal and retry."));}
}
