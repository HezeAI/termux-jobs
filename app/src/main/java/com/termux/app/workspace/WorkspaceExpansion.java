package com.termux.app.workspace;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

/** In-place disclosure: reversal starts at the last rendered height. */
final class WorkspaceExpansion {
    private final View body;
    private ValueAnimator animation;
    private boolean open;
    WorkspaceExpansion(View body) {
        this.body=body;
        body.setVisibility(View.GONE);
        body.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){}
            public void onViewDetachedFromWindow(View v){cancel();finish();}
        });
    }
    boolean isOpen(){return open;}
    void setOpen(boolean value){
        if(open==value)return;
        int from=body.getVisibility()==View.GONE?0:animation==null?body.getHeight():Math.max(0,body.getLayoutParams().height);
        cancel();open=value;
        if(!ValueAnimator.areAnimatorsEnabled()||!body.isAttachedToWindow()||!body.isLaidOut()&&((View)body.getParent()).getWidth()==0){finish();return;}
        View parent=(View)body.getParent();
        int width=Math.max(0,parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight());
        body.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        int target=value?body.getMeasuredHeight():0;
        body.setVisibility(View.VISIBLE);height(from);
        ValueAnimator next=ValueAnimator.ofInt(from,target);animation=next;
        next.setDuration(260);next.setInterpolator(new AccelerateDecelerateInterpolator());
        next.addUpdateListener(a->height((Integer)a.getAnimatedValue()));
        next.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(animation==a){animation=null;finish();}}});
        next.start();
    }
    private void height(int px){ViewGroup.LayoutParams lp=body.getLayoutParams();lp.height=px;body.setLayoutParams(lp);}
    private void cancel(){ValueAnimator old=animation;animation=null;if(old!=null)old.cancel();}
    private void finish(){body.setVisibility(open?View.VISIBLE:View.GONE);height(open?ViewGroup.LayoutParams.WRAP_CONTENT:0);}
}
