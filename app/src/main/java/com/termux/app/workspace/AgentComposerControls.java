package com.termux.app.workspace;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Reparents existing actions only at the chat/terminal boundary; never recreates the draft. */
final class AgentComposerControls {
    final LinearLayout row,pair;
    final EditText input;
    final TextView star,send;
    final View attachment;
    private final Drawable starBackground;
    private final Drawable rowBackground;
    private View transcriptHost;
    private int terminalAbove;
    private java.util.function.IntConsumer inset=value->{};
    private final boolean baselineAligned;
    private boolean agent;
    private final Context context;
    AgentComposerControls(Context c,LinearLayout row,EditText input,TextView star,TextView send,View attachment){
        context=c;this.row=row;this.input=input;this.star=star;this.send=send;this.attachment=attachment;starBackground=star.getBackground();rowBackground=row.getBackground();baselineAligned=row.isBaselineAligned();
        pair=new LinearLayout(c);pair.setTag("agent-composer-tools");pair.setOrientation(LinearLayout.HORIZONTAL);pair.setGravity(Gravity.CENTER_VERTICAL);pair.setBackground(shape(0x00000000,0xff365b52));
        labels();
    }
    void overlay(View host,java.util.function.IntConsumer update){
        transcriptHost=host;inset=update;
        if(host.getLayoutParams() instanceof RelativeLayout.LayoutParams)terminalAbove=((RelativeLayout.LayoutParams)host.getLayoutParams()).getRule(RelativeLayout.ABOVE);
        row.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(agent)inset.accept(row.getHeight());});
    }
    private void geometry(){
        if(transcriptHost!=null&&transcriptHost.getLayoutParams() instanceof RelativeLayout.LayoutParams){
            RelativeLayout.LayoutParams p=(RelativeLayout.LayoutParams)transcriptHost.getLayoutParams();
            if(agent){p.removeRule(RelativeLayout.ABOVE);p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);}
            else{p.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);if(terminalAbove!=0)p.addRule(RelativeLayout.ABOVE,terminalAbove);}
            transcriptHost.setLayoutParams(p);
        }
        row.setBackground(agent?null:rowBackground);inset.accept(agent?row.getHeight():0);
    }
    int dp(int value){return WorkspaceDialogs.dp(context,value);}
    String t(String ru,String en){return AgentInstructionsUi.tr(context,ru,en);}
    private GradientDrawable shape(int fill,int border){GradientDrawable bg=new GradientDrawable();bg.setColor(fill);bg.setCornerRadius(dp(12));bg.setStroke(dp(1),border);return bg;}
    private Drawable feedback(){return new RippleDrawable(ColorStateList.valueOf(0x9979dfcd),null,shape(0xffffffff,0xffffffff));}
    private static void remove(View view){if(view.getParent()!=null)((ViewGroup)view.getParent()).removeView(view);}
    void present(boolean nextAgent){
        if(agent!=nextAgent){
            remove(star);remove(send);remove(attachment);remove(pair);pair.removeAllViews();
            agent=nextAgent;
            if(agent){
                star.setBackground(feedback());attachment.setBackground(feedback());pair.addView(star,new LinearLayout.LayoutParams(dp(48),dp(48)));
                TextView slash=new TextView(context);slash.setText("/");slash.setTextColor(WorkspaceDialogs.MUTED);slash.setGravity(Gravity.CENTER);slash.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);slash.setClickable(false);slash.setFocusable(false);pair.addView(slash,new LinearLayout.LayoutParams(dp(12),dp(48)));
                pair.addView(attachment,new LinearLayout.LayoutParams(dp(48),dp(48)));
                LinearLayout.LayoutParams tools=new LinearLayout.LayoutParams(-2,-2);tools.setMarginEnd(dp(6));row.addView(pair,0,tools);
                LinearLayout.LayoutParams actions=new LinearLayout.LayoutParams(dp(48),dp(48));actions.setMarginStart(dp(6));row.addView(send,actions);row.setBaselineAligned(false);row.setGravity(Gravity.BOTTOM);
            }else{
                star.setBackground(starBackground);LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(dp(48),dp(48));left.setMarginEnd(dp(8));row.addView(star,0,left);LinearLayout.LayoutParams actions=new LinearLayout.LayoutParams(dp(48),dp(48));actions.setMarginStart(dp(8));row.addView(send,actions);row.setBaselineAligned(baselineAligned);row.setGravity(Gravity.CENTER_VERTICAL);
            }
            geometry();
        }
        inset.accept(agent?row.getHeight():0);labels();
    }
    void labels(){star.setContentDescription(t("Вставить сохранённую команду","Insert saved command"));attachment.setContentDescription(t("Прикрепить файл","Attach file"));attachment.setTooltipText(t("Прикрепить файл","Attach file"));send.setContentDescription(agent?t("Отправить сообщение","Send message"):t("Отправить в активный терминал","Send to active terminal"));}
}
