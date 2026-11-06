package com.termux.app.workspace;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import com.termux.R;

/** Native composer shell; the original EditText and action handlers keep their identity. */
final class AgentComposerControls {
    final LinearLayout row,pair;
    final EditText input;
    final TextView star,send;
    final View attachment;
    private final Context context;
    final ComposerLimits limits;
    private final ComposerActionLayout tools;
    private final LinearLayout fieldLine;
    private final ComposerActionLayout toggles;
    final LinearLayout card;
    private View transcriptHost,status,stop;
    private int terminalAbove;
    private java.util.function.IntConsumer inset=value->{};
    private boolean agent,initialized,stopAvailable;
    private final TextView notes,queue,mic,commands,aliases,terminalNotes;
    private final Switch interactive,extra;
    private Runnable queueAction=()->{},voiceAction=()->{};
    private java.util.function.IntConsumer library=value->{};
    private java.util.function.Consumer<Boolean> interactiveAction=value->{},extraAction=value->{};
    private boolean updating;
    AgentComposerControls(Context c,LinearLayout row,EditText input,TextView star,TextView send,View attachment){
        context=c;this.row=row;this.input=input;this.star=star;this.send=send;this.attachment=attachment;
        pair=new LinearLayout(c);pair.setOrientation(LinearLayout.VERTICAL);pair.setTag("agent-composer-tools");
        card=new LinearLayout(c);card.setTag("composer-card");card.setOrientation(LinearLayout.VERTICAL);card.setClickable(true);card.setPadding(dp(11),dp(5),dp(11),dp(9));
        limits=new ComposerLimits(c);
        tools=new ComposerActionLayout(c);pair.addView(tools,new LinearLayout.LayoutParams(-1,-2));
        fieldLine=new LinearLayout(c);fieldLine.setGravity(Gravity.TOP);fieldLine.setBaselineAligned(false);
        toggles=new ComposerActionLayout(c);toggles.setTag("terminal-inline-controls");
        interactive=new Switch(c);extra=new Switch(c);for(Switch b:new Switch[]{interactive,extra}){b.setTextSize(11);b.setTextColor(WorkspaceDialogs.TEXT);b.setMinHeight(dp(44));b.setSwitchMinWidth(dp(30));b.setSwitchPadding(dp(7));b.setPadding(0,0,dp(12),0);b.setThumbTintList(ColorStateList.valueOf(WorkspaceDialogs.ACCENT));b.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff777872,WorkspaceDialogs.LINE}));toggles.addView(b);}
        interactive.setOnCheckedChangeListener((b,on)->{if(!updating)interactiveAction.accept(on);});extra.setOnCheckedChangeListener((b,on)->{if(!updating)extraAction.accept(on);});
        notes=icon(R.drawable.workspace_note,18.5f,()->star.performClick());queue=action("",()->queueAction.run());mic=icon(R.drawable.workspace_microphone,18,()->voiceAction.run());
        mic.setTag("agent-voice-input");notes.setTag("agent-notes-input");
        commands=action("",()->library.accept(0));aliases=action("Aliases",()->library.accept(1));terminalNotes=action("",()->library.accept(2));
        if(attachment instanceof WorkspaceIconButton)((WorkspaceIconButton)attachment).setIcon(c.getDrawable(R.drawable.workspace_plus),Math.round(18.5f*c.getResources().getDisplayMetrics().density));
        attachment.setBackground(WorkspaceRipple.background(c,null,10));if(attachment instanceof TextView)((TextView)attachment).setTextColor(WorkspaceDialogs.TEXT);
        attachment.setMinimumWidth(dp(44));attachment.setMinimumHeight(dp(44));send.setMinWidth(dp(44));send.setMinHeight(dp(44));
        input.setBackgroundColor(android.graphics.Color.TRANSPARENT);input.setTextColor(WorkspaceDialogs.TEXT);input.setHintTextColor(WorkspaceDialogs.MUTED);input.setPadding(dp(5),dp(4),dp(5),dp(8));input.setTextSize(13);input.setMinHeight(dp(40));
        GradientDrawable circle=new GradientDrawable();circle.setColor(WorkspaceDialogs.ACCENT);circle.setShape(GradientDrawable.OVAL);send.setBackground(new InsetDrawable(WorkspaceRipple.background(c,circle,22),dp(5)));send.setTextColor(WorkspaceDialogs.BG);if(send instanceof WorkspaceIconButton)((WorkspaceIconButton)send).setIcon(c.getDrawable(R.drawable.workspace_up),dp(17));else send.setText("↑");send.setTextSize(22);send.setGravity(Gravity.CENTER);send.setPadding(0,0,0,0);
        labels();
    }
    private TextView action(String title,Runnable run){TextView v=new TextView(context);v.setText(title);v.setTextSize(11);v.setTextColor(WorkspaceDialogs.TEXT);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(44));v.setMinWidth(dp(44));v.setPadding(dp(6),dp(4),dp(6),dp(4));v.setBackground(WorkspaceRipple.background(context,null,10));v.setOnClickListener(x->run.run());return v;}
    private TextView icon(int drawable,float size,Runnable run){
        WorkspaceIconButton v=new WorkspaceIconButton(context);v.setTextColor(WorkspaceDialogs.TEXT);v.setMinHeight(dp(44));v.setMinWidth(dp(44));v.setBackground(WorkspaceRipple.background(context,null,10));v.setIcon(context.getDrawable(drawable).mutate(),Math.round(size*context.getResources().getDisplayMetrics().density));v.setOnClickListener(x->run.run());return v;
    }
    void actions(View status,View stop,Runnable queueAction,Runnable voice,java.util.function.IntConsumer library,java.util.function.Consumer<Boolean> interactiveAction,java.util.function.Consumer<Boolean> extraAction){this.status=status;if(status!=null)status.setTag("composer-model-status");this.stop=stop;this.queueAction=queueAction;voiceAction=voice;this.library=library;this.interactiveAction=interactiveAction;this.extraAction=extraAction;initialized=false;}
    void terminalModes(boolean direct,boolean keys){updating=true;interactive.setChecked(direct);extra.setChecked(keys);updating=false;}
    void overlay(View host,java.util.function.IntConsumer update){transcriptHost=host;inset=update;if(host.getLayoutParams() instanceof RelativeLayout.LayoutParams)terminalAbove=((RelativeLayout.LayoutParams)host.getLayoutParams()).getRule(RelativeLayout.ABOVE);row.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->inset.accept(row.getVisibility()==View.VISIBLE?row.getHeight():0));}
    private void geometry(){
        if(transcriptHost!=null&&transcriptHost.getLayoutParams() instanceof RelativeLayout.LayoutParams){RelativeLayout.LayoutParams p=(RelativeLayout.LayoutParams)transcriptHost.getLayoutParams();p.removeRule(RelativeLayout.ABOVE);p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);transcriptHost.setLayoutParams(p);}
        inset.accept(row.getVisibility()==View.VISIBLE?row.getHeight():0);
    }
    int dp(int value){return WorkspaceDialogs.dp(context,value);}
    private String t(String ru,String en){return AgentInstructionsUi.tr(context,ru,en);}
    private static void remove(View view){if(view!=null&&view.getParent()!=null)((ViewGroup)view.getParent()).removeView(view);}
    void present(boolean nextAgent){
        if(!initialized||agent!=nextAgent){
            initialized=true;agent=nextAgent;
            for(View v:new View[]{input,star,send,attachment,pair,fieldLine,toggles,card,status,stop,limits})remove(v);
            row.removeAllViews();card.removeAllViews();fieldLine.removeAllViews();tools.removeAllViews();row.setOrientation(LinearLayout.VERTICAL);row.setBaselineAligned(false);row.setClickable(true);
            GradientDrawable background=new GradientDrawable();background.setColor(0xff232426);background.setCornerRadius(dp(24));background.setStroke(dp(1),0xff393a3c);card.setBackground(background);row.setBackgroundColor(android.graphics.Color.TRANSPARENT);row.setPadding(dp(12),0,dp(12),dp(10));
            if(!agent)row.addView(toggles,new LinearLayout.LayoutParams(-1,-2));
            row.addView(card,new LinearLayout.LayoutParams(-1,-2));
            fieldLine.addView(input,new LinearLayout.LayoutParams(0,-2,1));
            if(agent&&stop!=null)fieldLine.addView(stop,new LinearLayout.LayoutParams(dp(44),dp(44)));
            card.addView(fieldLine,new LinearLayout.LayoutParams(-1,-2));
            if(agent){tools.addView(attachment,new ViewGroup.LayoutParams(dp(44),dp(44)));tools.addView(notes,new ViewGroup.LayoutParams(dp(44),dp(44)));if(status!=null)tools.addView(status);tools.addView(mic,new ViewGroup.LayoutParams(dp(44),dp(44)));}
            else{tools.addView(commands);tools.addView(separator());tools.addView(aliases);tools.addView(separator());tools.addView(terminalNotes);}
            tools.addView(send,new ViewGroup.LayoutParams(dp(44),dp(44)));tools.rightGroup(agent?(status!=null?status:mic):send);card.setPadding(dp(agent?10:11),dp(agent?6:5),dp(agent?10:11),dp(agent?4:9));card.addView(pair,new LinearLayout.LayoutParams(-1,-2));if(agent)card.addView(limits,new LinearLayout.LayoutParams(-1,-2));geometry();
        }
        updateStop();labels();inset.accept(row.getVisibility()==View.VISIBLE?row.getHeight():0);
    }
    void stopAvailable(boolean available){stopAvailable=available;updateStop();}
    private void updateStop(){if(stop!=null){stop.setVisibility(agent?(stopAvailable?View.VISIBLE:View.INVISIBLE):View.GONE);stop.setEnabled(agent&&stopAvailable);}}
    private View separator(){TextView v=new TextView(context);v.setText("/");v.setTextSize(12);v.setTextColor(WorkspaceDialogs.MUTED);v.setPadding(dp(8),0,dp(8),0);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(44));return v;}
    void labels(){star.setContentDescription(agent?t("Заметки","Notes"):t("Команды","Commands"));commands.setContentDescription(t("Вставить сохранённую команду","Insert saved command"));interactive.setContentDescription(t("Интерактивный ввод","Interactive input"));extra.setContentDescription(t("Дополнительные клавиши","Extra keys"));notes.setContentDescription(t("Заметки","Notes"));queue.setText(t("Очередь","Queue"));commands.setText(t("Команды","Commands"));terminalNotes.setText(t("Заметки","Notes"));interactive.setText(t("Интерактивный ввод","Interactive input"));extra.setText(t("Дополнительные клавиши","Extra keys"));mic.setContentDescription(t("Диктовка в черновик","Dictate into draft"));attachment.setContentDescription(t("Прикрепить файл","Attach file"));send.setContentDescription(agent?t("Отправить сообщение","Send message"):t("Отправить в активный терминал","Send to active terminal"));}
}
