package com.termux.app.workspace;

import android.animation.ValueAnimator;
import android.icu.text.BreakIterator;
import android.os.SystemClock;
import android.text.SpannedString;
import android.view.View;
import android.widget.TextView;

/** Transient presentation of one growing block. No history writes, model calls or Markdown parsing. */
final class AgentTextReveal {
    private final TextView view;
    private final java.util.function.Consumer<CharSequence> draw;
    private CharSequence target="";
    private String plain="";
    private int offset;
    private BreakIterator characters;
    private long last;
    private double budget;
    private long batchStarted;
    private int batchStart,batchLength;
    private boolean scheduled;
    Runnable beforeChange=()->{};
    AgentTextReveal(TextView view,java.util.function.Consumer<CharSequence> draw){this.view=view;this.draw=draw;}
    static double delay(int pending){return pending<=5?12:Math.max(.05,12.0/(1+(pending-5)/12.0));}
    boolean pending(){return offset<target.length();}
    void update(CharSequence next,boolean animate){
        String text=next==null?"":next.toString();boolean append=text.startsWith(plain);
        if(!pending()&&text.length()>offset){batchStarted=SystemClock.uptimeMillis();batchStart=offset;batchLength=text.length()-offset;}
        target=next instanceof android.text.Spanned?new SpannedString(next):text;plain=text;
        if(!animate||!append||!ValueAnimator.areAnimatorsEnabled()){showAll();return;}
        offset=Math.min(offset,target.length());characters=BreakIterator.getCharacterInstance();characters.setText(plain);
        if(!view.isAttachedToWindow()){draw.accept(target.subSequence(0,offset));return;}
        if(!view.isShown()){finish();return;}
        attached();if(!pending())draw.accept(target);
    }
    void attached(){if(pending()&&!scheduled&&view.isAttachedToWindow()&&view.isShown()){last=SystemClock.uptimeMillis();budget=0;schedule();}}
    private void schedule(){scheduled=true;view.postOnAnimation(tick);}
    private final Runnable tick=()->step();
    private void step(){
        scheduled=false;if(!pending())return;
        if(!view.isShown()||!ValueAnimator.areAnimatorsEnabled()){finish();return;}
        long now=SystemClock.uptimeMillis();budget+=Math.min(64,Math.max(0,now-last));last=now;
        int start=offset;
        // Advance in grapheme order, then paint once per display frame. Large backlogs accelerate.
        int minimumDuration=Math.min(420,Math.max(120,batchLength*8));
        int frameLimit=batchStart+(int)Math.min(batchLength,(long)batchLength*Math.max(0,now-batchStarted)/minimumDuration);
        while(pending()&&(offset<frameLimit||offset>=batchStart+batchLength)){
            double delay=delay(target.length()-offset);if(budget<delay)break;budget-=delay;
            int next=characters.following(offset);offset=next==BreakIterator.DONE?target.length():next;
        }
        if(offset!=start){beforeChange.run();draw.accept(target.subSequence(0,offset));}
        if(pending())schedule();
    }
    void finish(){if(pending())showAll();}
    private void showAll(){view.removeCallbacks(tick);scheduled=false;offset=target.length();draw.accept(target);}
}
