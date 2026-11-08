package com.termux.app.workspace;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Layout height only: never owns the arrival's translation, scale, alpha or clock. */
final class AgentCardSize {
    private final View view;
    private final AgentChatView owner;
    boolean enabled;
    private int displayed=-1,target=-1;
    private ValueAnimator animation;
    AgentCardSize(View view,AgentChatView owner){this.view=view;this.owner=owner;}
    void seed(int height){if(displayed<0){displayed=Math.max(0,height);enabled=true;}}
    int measure(int natural){
        if(displayed<0||!enabled||!view.isShown()||!ValueAnimator.areAnimatorsEnabled()){
            stop();displayed=target=natural;
        }else if(natural!=target){
            target=natural;stop();
            ValueAnimator next=ValueAnimator.ofInt(displayed,natural);animation=next;
            next.setDuration(240);next.setInterpolator(new DecelerateInterpolator());
            next.addUpdateListener(frame->{owner.preserveTimelinePosition();displayed=(Integer)frame.getAnimatedValue();view.requestLayout();});
            next.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(animation==a)animation=null;}});next.start();
        }
        return displayed;
    }
    private void stop(){if(animation!=null){animation.cancel();animation=null;}}
    void detached(){stop();displayed=target=-1;}
}
