package com.termux.app.workspace;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** Paint-only activity cue. Idle and active have the same height: never resizes the PTY. */
public final class WorkspaceActivityLine extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix=new Matrix();
    private LinearGradient gradient;
    private boolean working,framePending;
    private final Runnable frame=()->{framePending=false;if(working&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE)invalidate();};
    private void cancelFrame(){removeCallbacks(frame);framePending=false;}
    public WorkspaceActivityLine(Context c,AttributeSet a){super(c,a);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void working(boolean value){if(working==value)return;working=value;if(!value)cancelFrame();setVisibility(value?VISIBLE:INVISIBLE);invalidate();}
    boolean working(){return working;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);gradient=new LinearGradient(0,0,Math.max(1,w*.45f),0,new int[]{0x0079dfcd,0xff79dfcd,0x0079dfcd},null,Shader.TileMode.CLAMP);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(!working||gradient==null)return;
        float width=getWidth(),phase=(android.os.SystemClock.uptimeMillis()%1400)/1400f;
        matrix.setTranslate(-width*.45f+width*1.45f*phase,0);gradient.setLocalMatrix(matrix);paint.setShader(gradient);canvas.drawRect(0,0,width,getHeight(),paint);
        if(!framePending&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE){framePending=true;postDelayed(frame,16);}
    }
    @Override protected void onVisibilityChanged(View v,int visibility){super.onVisibilityChanged(v,visibility);if(visibility!=VISIBLE)cancelFrame();else if(working)invalidate();}
    @Override protected void onDetachedFromWindow(){cancelFrame();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)cancelFrame();else if(working)invalidate();}
}
