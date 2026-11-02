package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Paint-only heart. The existing activity-line frame clock owns its animation. */
final class AgentStopButton extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path heart=heartPath();
    private static final double[] BEATS={0,1.3,1.6,3.0,3.3,3.6,5.1};
    private float pulse=1;
    AgentStopButton(Context c){super(c);setClickable(true);setFocusable(true);setEnabled(false);}
    static Path heartPath(){Path p=new Path();p.moveTo(0,.9f);p.cubicTo(-.22f,.7f,-1,.1f,-1,-.38f);p.cubicTo(-1,-.95f,-.33f,-1,0,-.5f);p.cubicTo(.33f,-1,1,-.95f,1,-.38f);p.cubicTo(1,.1f,.22f,.7f,0,.9f);p.close();return p;}
    float circleRadius(){float d=getResources().getDisplayMetrics().density;return Math.max(1,Math.min(Math.min(getWidth(),getHeight())/2f-3*d,18*d));}
    float heartRadius(){return circleRadius()*.68f;}
    static float pulseScale(long nanos,float stronger){
        if(nanos==0)return 1;double t=(nanos%6600000000L)/1e9,beat=0;
        // Deliberate single, double, triple groups with breathing pauses.
        for(double start:BEATS){double x=(t-start)/.23;if(x>=0&&x<=1)beat=Math.max(beat,Math.pow(Math.sin(Math.PI*x),2));}
        double fast=Math.pow(Math.sin((nanos%230000000L)/230000000d*Math.PI),2);
        return (float)(1+.11*beat+stronger*(.10+.12*fast));
    }
    void frame(long nanos,float stronger){float next=isEnabled()?pulseScale(nanos,stronger):1;if(Math.abs(next-pulse)>.0001f){pulse=next;invalidate();}}
    @Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();if(!isEnabled())pulse=1;invalidate();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(Math.min(getWidth(),getHeight())/2f-3*d,18*d);paint.setStyle(Paint.Style.FILL);paint.setColor(isEnabled()?0xffbc3e4d:0xff303a40);canvas.drawCircle(cx,cy,r,paint);
        canvas.save();canvas.translate(cx,cy);float scale=heartRadius()*pulse;canvas.scale(scale,scale);paint.setColor(isEnabled()?Color.WHITE:0xff8c9a9f);canvas.drawPath(heart,paint);canvas.restore();
        if(!isEnabled()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(3*d);paint.setColor(0xff0b1217);canvas.drawLine(cx-r*.72f,cy+r*.72f,cx+r*.72f,cy-r*.72f,paint);paint.setStrokeWidth(1.5f*d);paint.setColor(0xffa3afb4);canvas.drawLine(cx-r*.72f,cy+r*.72f,cx+r*.72f,cy-r*.72f,paint);}
    }
}
