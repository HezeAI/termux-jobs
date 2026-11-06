package com.termux.app.workspace;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Text presentation preference; does not affect X11 density or remote screen settings. */
public final class SessionTextSize {
    private SessionTextSize(){}
    public static int percent(Context c){return AgentChatScale.clamp(c.getSharedPreferences("workspace-appearance",0).getInt("session-text-percent",100));}
    public static int terminalPixels(Context c,int base){return Math.max(1,Math.round(base*percent(c)/100f));}
    static int agentPercent(Context c,int profile){return AgentChatScale.clamp(Math.round(profile*percent(c)/100f));}
    static void control(LinearLayout parent,Runnable changed){
        Context c=parent.getContext();TextView label=AgentStatusUi.info(parent,"");label.setTag("session-text-size-label");
        Runnable update=()->label.setText(AgentStatusUi.tr(c,"Размер основного текста сессий (100% - Default)","Session main text size (100% - Default)")+": "+percent(c)+"%");update.run();
        SeekBar slider=new SeekBar(c);slider.setMax(100);slider.setProgress(percent(c)-50);slider.setContentDescription(label.getText());slider.setTag("session-text-size");parent.addView(slider,new LinearLayout.LayoutParams(-1,-2));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int n,boolean user){if(!user)return;c.getSharedPreferences("workspace-appearance",0).edit().putInt("session-text-percent",AgentChatScale.clamp(n+50)).apply();update.run();changed.run();}});
    }
}
