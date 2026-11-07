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
        Context c=parent.getContext();LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);int pad=WorkspaceDialogs.dp(c,14);box.setPadding(pad,pad,pad,pad);parent.addView(box,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout heading=new LinearLayout(c);heading.setGravity(android.view.Gravity.CENTER_VERTICAL);TextView label=WorkspaceSettingsPage.text(c,AgentStatusUi.tr(c,"Масштаб текста чатов","Chat text scale"),12,WorkspaceDialogs.TEXT);label.setTag("session-text-size-label");heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView value=WorkspaceSettingsPage.text(c,"",12,WorkspaceDialogs.ACCENT);value.setTag("session-text-size-value");heading.addView(value);box.addView(heading);
        SeekBar slider=new SeekBar(c);slider.setMax(100);slider.setProgress(percent(c)-50);slider.setTag("session-text-size");slider.setProgressTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.ACCENT));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.ACCENT));slider.setMinimumHeight(WorkspaceDialogs.dp(c,48));box.addView(slider,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout ticks=new LinearLayout(c);for(int n:new int[]{50,100,150}){TextView tick=WorkspaceSettingsPage.text(c,n+"%",10,WorkspaceDialogs.MUTED);tick.setGravity(n==50?android.view.Gravity.LEFT:n==150?android.view.Gravity.RIGHT:android.view.Gravity.CENTER);ticks.addView(tick,new LinearLayout.LayoutParams(0,-2,1));}box.addView(ticks);
        TextView description=WorkspaceSettingsPage.text(c,AgentStatusUi.tr(c,"Размер основного текста сессий (100% - Default)\n\nПрименяется ко всем агентским чатам и терминальным сессиям. Графический рабочий стол X11 не меняется.","Session main text size (100% - Default)\n\nApplies to all agent chats and terminal sessions. The X11 desktop is unchanged."),12.8f,WorkspaceDialogs.MUTED);description.setPadding(0,pad,0,pad);box.addView(description);
        LinearLayout preview=new LinearLayout(c);preview.setOrientation(LinearLayout.VERTICAL);preview.setTag("session-text-size-preview");preview.setPadding(pad,pad,pad,pad);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(WorkspaceDialogs.BG);bg.setCornerRadius(WorkspaceDialogs.dp(c,10));bg.setStroke(WorkspaceDialogs.dp(c,1),WorkspaceDialogs.LINE);preview.setBackground(bg);box.addView(preview,new LinearLayout.LayoutParams(-1,-2));preview.addView(WorkspaceSettingsPage.text(c,AgentStatusUi.tr(c,"Предпросмотр","Preview"),9,WorkspaceDialogs.MUTED));TextView prose=WorkspaceSettingsPage.text(c,AgentStatusUi.tr(c,"Текст ответа агента и поле ввода","Agent response and input text"),13,WorkspaceDialogs.TEXT),command=WorkspaceSettingsPage.text(c,AgentStatusUi.tr(c,"$ Команда в терминале","$ Terminal command"),12,WorkspaceDialogs.TEXT);prose.setPadding(0,WorkspaceDialogs.dp(c,9),0,WorkspaceDialogs.dp(c,9));command.setTypeface(android.graphics.Typeface.MONOSPACE);preview.addView(prose);preview.addView(command);
        Runnable update=()->{value.setText(percent(c)+"%");slider.setContentDescription(label.getText()+": "+value.getText());prose.setTextSize(13*percent(c)/100f);command.setTextSize(12*percent(c)/100f);};update.run();
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int n,boolean user){if(!user)return;c.getSharedPreferences("workspace-appearance",0).edit().putInt("session-text-percent",AgentChatScale.clamp(n+50)).apply();update.run();changed.run();}});

    }
}
