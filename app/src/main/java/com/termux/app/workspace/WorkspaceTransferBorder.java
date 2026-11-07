package com.termux.app.workspace;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Clockwise paint-only overlay; never requests layout or refreshes parent content. */
final class WorkspaceTransferBorder extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline=new Path(),segment=new Path();
    private final PathMeasure measure=new PathMeasure();
    private boolean active,framePending;
    private java.lang.ref.WeakReference<android.view.View> host;
    private android.animation.ValueAnimator clock;
    private float phase;
    void follow(android.view.View view){
        host=new java.lang.ref.WeakReference<>(view);
        view.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(android.view.View v){syncClock();}public void onViewDetachedFromWindow(android.view.View v){stopClock();}});
        syncClock();
    }
    private void stopClock(){if(clock!=null){clock.cancel();clock=null;}phase=0;}
    private void syncClock(){
        if(host==null)return;
        android.view.View view=host.get();
        if(!active||!isVisible()||view==null||!view.isAttachedToWindow()||!view.isShown()||!android.animation.ValueAnimator.areAnimatorsEnabled()){stopClock();return;}
        if(clock!=null)return;
        clock=android.animation.ValueAnimator.ofFloat(0,1);clock.setDuration(2000);clock.setRepeatCount(android.animation.ValueAnimator.INFINITE);clock.setInterpolator(new android.view.animation.LinearInterpolator());
        clock.addUpdateListener(frame->{android.view.View current=host.get();if(!active||current==null||!current.isAttachedToWindow()||!current.isShown()||!android.animation.ValueAnimator.areAnimatorsEnabled()){stopClock();return;}phase=(float)frame.getAnimatedValue();current.invalidate();});clock.start();
    }
    private final Runnable frame=()->{framePending=false;if(active&&isVisible())invalidateSelf();};
    private final float inset,radius;private final boolean circular;
    WorkspaceTransferBorder(float density){this(density,false);}
    WorkspaceTransferBorder(float density,boolean circular){this.circular=circular;inset=density;radius=12*density;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xffecece5);}
    void active(boolean value){if(active==value){syncClock();return;}active=value;if(!value){unscheduleSelf(frame);framePending=false;}syncClock();invalidateSelf();}
    boolean active(){return active;}
    @Override protected void onBoundsChange(Rect b){outline.reset();if(circular)outline.addOval(new RectF(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset),Path.Direction.CW);else outline.addRoundRect(new RectF(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset),radius,radius,Path.Direction.CW);measure.setPath(outline,true);}
    @Override public void draw(Canvas c){if(!active)return;float length=measure.getLength();if(length<=0)return;boolean animate=android.animation.ValueAnimator.areAnimatorsEnabled();float start=animate?length*(host==null?(android.os.SystemClock.uptimeMillis()%2000)/2000f:phase):0,end=start+length*.22f;segment.reset();measure.getSegment(start,Math.min(end,length),segment,true);if(end>length)measure.getSegment(0,end-length,segment,true);c.drawPath(segment,paint);if(host==null&&animate&&isVisible()&&!framePending&&getCallback()!=null){framePending=true;scheduleSelf(frame,android.os.SystemClock.uptimeMillis()+16);}}
    @Override public boolean setVisible(boolean visible,boolean restart){if(!visible){unscheduleSelf(frame);framePending=false;stopClock();}boolean changed=super.setVisible(visible,restart);syncClock();return changed;}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
