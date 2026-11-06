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
    private final LinearLayout fieldLine,toggles;
    private View transcriptHost,status,stop;
    private int terminalAbove;
    private java.util.function.IntConsumer inset=value->{};
    private boolean agent,initialized;
    private final TextView notes,queue,mic,commands,aliases,terminalNotes;
    private final CheckBox interactive,extra;
    private Runnable queueAction=()->{},voiceAction=()->{};
    private java.util.function.IntConsumer library=value->{};
    private java.util.function.Consumer<Boolean> interactiveAction=value->{},extraAction=value->{};
    private boolean updating;
    AgentComposerControls(Context c,LinearLayout row,EditText input,TextView star,TextView send,View attachment){
        context=c;this.row=row;this.input=input;this.star=star;this.send=send;this.attachment=attachment;
        pair=new LinearLayout(c);pair.setOrientation(LinearLayout.VERTICAL);pair.setTag("agent-composer-tools");
        limits=new ComposerLimits(c);
        tools=new ComposerActionLayout(c);pair.addView(tools,new LinearLayout.LayoutParams(-1,-2));
        fieldLine=new LinearLayout(c);fieldLine.setGravity(Gravity.BOTTOM);fieldLine.setBaselineAligned(false);
        toggles=new LinearLayout(c);toggles.setOrientation(LinearLayout.VERTICAL);
        interactive=new CheckBox(c);extra=new CheckBox(c);for(CheckBox b:new CheckBox[]{interactive,extra}){b.setTextSize(11);b.setTextColor(WorkspaceDialogs.MUTED);b.setMinHeight(dp(40));toggles.addView(b);}
        interactive.setOnCheckedChangeListener((b,on)->{if(!updating)interactiveAction.accept(on);});extra.setOnCheckedChangeListener((b,on)->{if(!updating)extraAction.accept(on);});
        notes=action("",()->star.performClick());queue=action("",()->queueAction.run());mic=action("",()->voiceAction.run());
        android.graphics.drawable.Drawable microphone=c.getDrawable(R.drawable.workspace_microphone).mutate();microphone.setTint(WorkspaceDialogs.ACCENT);microphone.setBounds(0,0,dp(22),dp(22));mic.setCompoundDrawables(null,microphone,null,null);mic.setTag("agent-voice-input");
        commands=action("",()->library.accept(0));aliases=action("Aliases",()->library.accept(1));terminalNotes=action("",()->library.accept(2));
        if(attachment instanceof WorkspaceIconButton)((WorkspaceIconButton)attachment).setIcon(c.getDrawable(R.drawable.workspace_plus),dp(22));
        attachment.setMinimumWidth(dp(48));attachment.setMinimumHeight(dp(48));send.setMinWidth(dp(48));send.setMinHeight(dp(48));
        labels();
    }
    private TextView action(String title,Runnable run){TextView v=new TextView(context);v.setText(title);v.setTextSize(11);v.setTextColor(WorkspaceDialogs.TEXT);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(48));v.setMinWidth(dp(48));v.setPadding(dp(6),dp(4),dp(6),dp(4));v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3379dfcd),null,null));v.setOnClickListener(x->run.run());return v;}
    void actions(View status,View stop,Runnable queueAction,Runnable voice,java.util.function.IntConsumer library,java.util.function.Consumer<Boolean> interactiveAction,java.util.function.Consumer<Boolean> extraAction){this.status=status;this.stop=stop;this.queueAction=queueAction;voiceAction=voice;this.library=library;this.interactiveAction=interactiveAction;this.extraAction=extraAction;initialized=false;}
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
            for(View v:new View[]{input,star,send,attachment,pair,fieldLine,toggles,status,stop,limits})remove(v);
            row.removeAllViews();fieldLine.removeAllViews();tools.removeAllViews();row.setOrientation(LinearLayout.VERTICAL);row.setBaselineAligned(false);row.setClickable(true);
            GradientDrawable background=new GradientDrawable();background.setColor(0xff232426);background.setCornerRadius(dp(20));background.setStroke(dp(1),0xff393a3c);row.setBackground(background);
            if(!agent)row.addView(toggles,new LinearLayout.LayoutParams(-1,-2));
            fieldLine.addView(input,new LinearLayout.LayoutParams(0,-2,1));
            if(agent&&stop!=null)fieldLine.addView(stop,new LinearLayout.LayoutParams(dp(48),dp(48)));
            row.addView(fieldLine,new LinearLayout.LayoutParams(-1,-2));
            if(agent){tools.addView(attachment);tools.addView(notes);tools.addView(queue);if(status!=null)tools.addView(status);tools.addView(mic);}
            else{tools.addView(commands);tools.addView(aliases);tools.addView(terminalNotes);}
            tools.addView(send);row.addView(pair,new LinearLayout.LayoutParams(-1,-2));if(agent)row.addView(limits,new LinearLayout.LayoutParams(-1,-2));geometry();
        }
        if(stop!=null)stop.setVisibility(agent?View.VISIBLE:View.GONE);labels();inset.accept(row.getVisibility()==View.VISIBLE?row.getHeight():0);
    }
    void labels(){star.setContentDescription(agent?t("Заметки","Notes"):t("Команды","Commands"));commands.setContentDescription(t("Вставить сохранённую команду","Insert saved command"));interactive.setContentDescription(t("Интерактивный ввод","Interactive input"));extra.setContentDescription(t("Дополнительные клавиши","Extra keys"));notes.setText(t("Заметки","Notes"));queue.setText(t("Очередь","Queue"));commands.setText(t("Команды","Commands"));terminalNotes.setText(t("Заметки","Notes"));interactive.setText(t("Интерактивный ввод","Interactive input"));extra.setText(t("Дополнительные клавиши","Extra keys"));mic.setContentDescription(t("Диктовка в черновик","Dictate into draft"));attachment.setContentDescription(t("Прикрепить файл","Attach file"));send.setContentDescription(agent?t("Отправить сообщение","Send message"):t("Отправить в активный терминал","Send to active terminal"));}
}
