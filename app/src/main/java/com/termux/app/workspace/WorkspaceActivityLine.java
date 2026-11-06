package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.Choreographer;

/** Paint-only activity cue. Idle and active have the same height: never resizes the PTY. */
public final class WorkspaceActivityLine extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix=new Matrix();
    private LinearGradient gradient;
    private boolean working,framePending,legacyAnimations=true;
    private final FrameCadence cadence=new FrameCadence();
    private long animationTimeNanos;
    private View stop;
    private java.util.function.LongConsumer agentFrame=value->{};
    void agentFrame(java.util.function.LongConsumer listener){agentFrame=listener;}
    public void stopTarget(View target){if(stop==target)return;resetIndicator();stop=target;clearRoute();invalidate();}
    private void resetIndicator(){agentFrame.accept(0);if(stop instanceof AgentStopButton)((AgentStopButton)stop).frame(0,0);}
    private void clearRoute(){resetIndicator();}
    boolean routing(){return false;}
    private boolean drawRoute(){
        if(!(stop instanceof AgentStopButton))return false;
        AgentStopButton indicator=(AgentStopButton)stop;
        indicator.frame(animationsAllowed()?animationTimeNanos:0,0);
        return true; // Agent motion is entirely inside the button. No border/line paint.
    }
    /** Display-frame driven, with an average 60 Hz ceiling on faster panels. */
    static final class FrameCadence {
        static final long PERIOD=16666667L;
        private long next;
        boolean due(long time){
            if(next==0||time-next>PERIOD*4){next=time+PERIOD;return true;}
            if(time+PERIOD/20<next)return false;
            next+=PERIOD;return true;
        }
        void reset(){next=0;}
    }
    private boolean animationsAllowed(){return android.os.Build.VERSION.SDK_INT<26?legacyAnimations:android.animation.ValueAnimator.areAnimatorsEnabled();}
    private boolean canAnimate(){return animationsAllowed()&&working&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE;}
    private final Choreographer.FrameCallback frame=time->{
        framePending=false;if(!canAnimate()){resetIndicator();animationTimeNanos=0;invalidate();return;}
        if(cadence.due(time)){animationTimeNanos=time;if(stop instanceof AgentStopButton)((AgentStopButton)stop).frame(time,0);if(stop instanceof AgentStopButton)agentFrame.accept(time);invalidate();}
        scheduleFrame();
    };
    private void scheduleFrame(){if(!framePending&&canAnimate()){framePending=true;Choreographer.getInstance().postFrameCallback(frame);}}
    private void cancelFrame(){if(framePending)Choreographer.getInstance().removeFrameCallback(frame);framePending=false;cadence.reset();animationTimeNanos=0;resetIndicator();}
    public WorkspaceActivityLine(Context c,AttributeSet a){super(c,a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void working(boolean value){if(working==value)return;if(android.os.Build.VERSION.SDK_INT<26)legacyAnimations=android.provider.Settings.Global.getFloat(getContext().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;working=value;if(!value){cancelFrame();clearRoute();}setVisibility(value?VISIBLE:INVISIBLE);invalidate();}
    boolean working(){return working;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);gradient=new LinearGradient(0,0,Math.max(1,w*.45f),0,new int[]{0x00ecece5,0xffecece5,0x00ecece5},null,Shader.TileMode.CLAMP);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(!working||gradient==null)return;
        if(drawRoute()){scheduleFrame();return;}
        float width=getWidth(),phase=animationsAllowed()?((animationTimeNanos==0?System.nanoTime():animationTimeNanos)%1400000000L)/1400000000f:.5f;
        matrix.setTranslate(-width*.45f+width*1.45f*phase,0);gradient.setLocalMatrix(matrix);paint.setShader(gradient);canvas.drawRect(0,0,width,getHeight(),paint);
        scheduleFrame();
    }
    @Override protected void onVisibilityChanged(View v,int visibility){super.onVisibilityChanged(v,visibility);if(visibility!=VISIBLE){cancelFrame();clearRoute();}else if(working)invalidate();}
    @Override protected void onDetachedFromWindow(){cancelFrame();clearRoute();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE){cancelFrame();clearRoute();}else if(working)invalidate();}
}
