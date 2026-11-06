package com.termux.app.workspace;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.RelativeLayout;
import java.util.function.IntConsumer;

/** Extends only the agent transcript under the header and status controls. */
final class AgentChatHeaderOverlay {
    private final View transcript,header,line,title;
    private final int below,chatBelow;
    private final Drawable background,titleBackground;
    private final float headerElevation,lineElevation,titleElevation;
    private final IntConsumer inset;
    private boolean active;
    private final View.OnLayoutChangeListener layout=(v,l,t,r,b,ol,ot,or,ob)->updateInset();
    AgentChatHeaderOverlay(View transcript,View header,View line,View title,int chatBelow,IntConsumer inset){
        this.transcript=transcript;this.title=title;this.header=header;this.line=line;this.chatBelow=chatBelow;this.inset=inset;
        below=((RelativeLayout.LayoutParams)transcript.getLayoutParams()).getRule(RelativeLayout.BELOW);
        background=header.getBackground();headerElevation=header.getElevation();lineElevation=line.getElevation();
        titleBackground=title==null?null:title.getBackground();titleElevation=title==null?0:title.getElevation();
        if(title!=null)title.addOnLayoutChangeListener(layout);
        header.addOnLayoutChangeListener(layout);line.addOnLayoutChangeListener(layout);
    }
    void present(boolean chat){
        if(active!=chat){
            active=chat;RelativeLayout.LayoutParams p=(RelativeLayout.LayoutParams)transcript.getLayoutParams();
            if(chat){if(title!=null)p.removeRule(RelativeLayout.BELOW);else p.addRule(RelativeLayout.BELOW,chatBelow);}else if(below!=0)p.addRule(RelativeLayout.BELOW,below);else p.removeRule(RelativeLayout.BELOW);
            transcript.setLayoutParams(p);header.setBackground(chat?null:background);
            float top=Math.max(transcript.getElevation(),Math.max(headerElevation,lineElevation))+1;
            if(title!=null){title.setBackground(chat?null:titleBackground);title.setElevation(chat?top:titleElevation);}
            header.setElevation(chat?top:headerElevation);line.setElevation(chat?top:lineElevation);
        }
        updateInset();
    }
    private void updateInset(){inset.accept(active?(title==null||title.getVisibility()==View.GONE?0:title.getHeight())+(header.getVisibility()==View.GONE?0:header.getHeight())+(line.getVisibility()==View.GONE?0:line.getHeight()):0);}
    void close(){present(false);if(title!=null)title.removeOnLayoutChangeListener(layout);header.removeOnLayoutChangeListener(layout);line.removeOnLayoutChangeListener(layout);}
}
