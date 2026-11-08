package com.termux.app.workspace;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;
/** A decorative thumb driven only by real gesture/inertial scroll frames. */
class AgentManualScroll extends ScrollView {
    private final Paint thumb=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean dispatching,dragging,flinging,computing;
    private float downY,opacity;
    private final int slop;
    private android.animation.ValueAnimator fade;
    AgentManualScroll(Context c){super(c);slop=ViewConfiguration.get(c).getScaledTouchSlop();setVerticalScrollBarEnabled(false);setHorizontalScrollBarEnabled(false);thumb.setColor(0xffa5a5a2);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){downY=e.getY();dragging=false;flinging=false;}
        if(action==MotionEvent.ACTION_MOVE&&Math.abs(e.getY()-downY)>slop)dragging=true;
        dispatching=true;try{return super.dispatchTouchEvent(e);}finally{dispatching=false;if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)dragging=false;}
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){boolean prior=dragging;dispatching=true;dragging=e.getActionMasked()==MotionEvent.ACTION_SCROLL;try{return super.dispatchGenericMotionEvent(e);}finally{dispatching=false;dragging=prior;}}
    @Override public void fling(int velocity){flinging=dispatching&&dragging;super.fling(velocity);}
    @Override public void computeScroll(){computing=true;try{super.computeScroll();}finally{computing=false;}}
    @Override protected void onScrollChanged(int l,int t,int oldl,int oldt){super.onScrollChanged(l,t,oldl,oldt);if(t!=oldt&&(dispatching&&dragging||computing&&flinging))manualMovement();invalidate();}
    void manualMovement(){if(fade!=null){fade.cancel();fade=null;}opacity=1;removeCallbacks(hide);postDelayed(hide,300);invalidate();}
    private final Runnable hide=()->{flinging=false;if(!android.animation.ValueAnimator.areAnimatorsEnabled()){opacity=0;invalidate();return;}android.animation.ValueAnimator a=android.animation.ValueAnimator.ofFloat(opacity,0);fade=a;a.setDuration(220);a.addUpdateListener(v->{opacity=(float)v.getAnimatedValue();invalidate();});a.start();};
    boolean manualScrolling(){return dragging||flinging;}
    float indicatorOpacity(){return opacity;}
    void hideIndicator(){removeCallbacks(hide);if(fade!=null){fade.cancel();fade=null;}opacity=0;dragging=flinging=false;invalidate();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)hideIndicator();}
    @Override protected void onVisibilityChanged(android.view.View v,int visibility){super.onVisibilityChanged(v,visibility);if(!isShown())hideIndicator();}
    @Override protected void onDetachedFromWindow(){hideIndicator();super.onDetachedFromWindow();}
    @Override public void onDrawForeground(Canvas canvas){super.onDrawForeground(canvas);if(opacity<=0)return;int range=computeVerticalScrollRange(),extent=computeVerticalScrollExtent();if(range<=extent)return;float density=getResources().getDisplayMetrics().density,track=getHeight()-8*density;float height=Math.max(24*density,track*extent/range),offset=Math.max(0,Math.min(1,computeVerticalScrollOffset()/(float)(range-extent)));float y=getScrollY()+4*density+(track-height)*offset;thumb.setAlpha(Math.round(opacity*170));canvas.drawRoundRect(getScrollX()+getWidth()-5*density,y,getScrollX()+getWidth()-2*density,y+height,2*density,2*density,thumb);}
}
