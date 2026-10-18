package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.view.View;

/** Fixed-size canvas crossfade: never changes header geometry or terminal content. */
final class WorkspaceBackgroundActivity extends View {
    private final Paint dots=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private boolean active,available;
    private String event="";private long eventAt;
    WorkspaceBackgroundActivity(Context c){super(c);text.setColor(0xff79dfcd);text.setTextSize(11*c.getResources().getDisplayMetrics().scaledDensity);setClickable(true);setFocusable(true);}
    void state(boolean available,boolean active){this.available=available;this.active=active;setVisibility(available?VISIBLE:INVISIBLE);invalidate();}
    void event(String value){event=value;eventAt=android.os.SystemClock.uptimeMillis();invalidate();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(!available)return;long now=android.os.SystemClock.uptimeMillis(),age=now-eventAt;float alpha=event.isEmpty()||age>=2400?0:age<250?age/250f:age>2000?(2400-age)/400f:1;
        float density=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f;
        if(alpha>0){text.setAlpha((int)(255*alpha));CharSequence value=TextUtils.ellipsize(event,text,Math.max(1,getWidth()-24*density),TextUtils.TruncateAt.END);canvas.drawText(value.toString(),cx-text.measureText(value.toString())/2,cy-(text.ascent()+text.descent())/2,text);}
        if(alpha<1){float angle=active?(now%1200)*360f/1200:0;for(int i=0;i<8;i++){double a=Math.toRadians(angle+i*45);dots.setColor(active?0xff79dfcd:0xff58716d);dots.setAlpha((int)((40+i*28)*(1-alpha)));canvas.drawCircle(cx+(float)Math.cos(a)*7*density,cy+(float)Math.sin(a)*7*density,1.7f*density,dots);}}
        if((active||alpha>0||!event.isEmpty()&&age<2400)&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE)postInvalidateOnAnimation();
    }
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)invalidate();}
}
