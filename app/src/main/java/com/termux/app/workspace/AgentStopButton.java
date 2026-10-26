package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A scoped request interrupt control. Its idle state is visibly crossed out. */
final class AgentStopButton extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path hand=new Path();
    AgentStopButton(Context c){super(c);setClickable(true);setFocusable(true);setEnabled(false);
        hand.moveTo(7,14);hand.lineTo(7,6);hand.quadTo(7,4,8.5f,4);hand.quadTo(10,4,10,6);hand.lineTo(10,11);hand.lineTo(10,3);hand.quadTo(10,1,11.5f,1);hand.quadTo(13,1,13,3);hand.lineTo(13,11);hand.lineTo(13,4);hand.quadTo(13,2,14.5f,2);hand.quadTo(16,2,16,4);hand.lineTo(16,12);hand.lineTo(16,7);hand.quadTo(16,5,17.5f,5);hand.quadTo(19,5,19,7);hand.lineTo(19,15);hand.cubicTo(19,21,16,24,11,24);hand.cubicTo(7,24,5,21,3,18);hand.lineTo(1,14);hand.quadTo(0,12,1.5f,11.5f);hand.quadTo(3,11,4,13);hand.lineTo(7,17);hand.close();
    }
    @Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(Math.min(getWidth(),getHeight())/2f-3*d,18*d);paint.setStyle(Paint.Style.FILL);paint.setColor(isEnabled()?0xffbc3e4d:0xff303a40);canvas.drawCircle(cx,cy,r,paint);
        canvas.save();float scale=r/16;canvas.translate(cx-10*scale,cy-12*scale);canvas.scale(scale,scale);paint.setColor(isEnabled()?Color.WHITE:0xff8c9a9f);canvas.drawPath(hand,paint);canvas.restore();
        if(!isEnabled()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(3*d);paint.setColor(0xff0b1217);canvas.drawLine(cx-r*.72f,cy+r*.72f,cx+r*.72f,cy-r*.72f,paint);paint.setStrokeWidth(1.5f*d);paint.setColor(0xffa3afb4);canvas.drawLine(cx-r*.72f,cy+r*.72f,cx+r*.72f,cy-r*.72f,paint);}
    }
}
