package com.termux.app.workspace;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Clockwise paint-only overlay; never requests layout or refreshes parent content. */
final class WorkspaceTransferBorder extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline=new Path(),segment=new Path();
    private final PathMeasure measure=new PathMeasure();
    private boolean active,framePending;
    private final Runnable frame=()->{framePending=false;if(active&&isVisible())invalidateSelf();};
    private final float inset,radius;
    WorkspaceTransferBorder(float density){inset=density;radius=12*density;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xff79dfcd);}
    void active(boolean value){if(active==value)return;active=value;if(!value){unscheduleSelf(frame);framePending=false;}invalidateSelf();}
    boolean active(){return active;}
    @Override protected void onBoundsChange(Rect b){outline.reset();outline.addRoundRect(new RectF(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset),radius,radius,Path.Direction.CW);measure.setPath(outline,true);}
    @Override public void draw(Canvas c){if(!active)return;float length=measure.getLength();if(length<=0)return;float start=length*(android.os.SystemClock.uptimeMillis()%2000)/2000f,end=start+length*.22f;segment.reset();measure.getSegment(start,Math.min(end,length),segment,true);if(end>length)measure.getSegment(0,end-length,segment,true);c.drawPath(segment,paint);if(isVisible()&&!framePending&&getCallback()!=null){framePending=true;scheduleSelf(frame,android.os.SystemClock.uptimeMillis()+16);}}
    @Override public boolean setVisible(boolean visible,boolean restart){if(!visible){unscheduleSelf(frame);framePending=false;}return super.setVisible(visible,restart);}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
