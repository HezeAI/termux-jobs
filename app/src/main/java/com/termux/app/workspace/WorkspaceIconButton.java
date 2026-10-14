package com.termux.app.workspace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

/** Icon-only buttons center the drawable independently of text baselines/padding. */
final class WorkspaceIconButton extends TextView {
    private Drawable icon;
    private int iconSize;
    private boolean badge;
    void setBadge(boolean value){badge=value;invalidate();}
    WorkspaceIconButton(Context context) {super(context);}
    void setIcon(Drawable value,int size){icon=value;iconSize=size;icon.setTint(getCurrentTextColor());setText("");setPadding(0,0,0,0);invalidate();}
    @Override public void setTextColor(int color){super.setTextColor(color);if(icon!=null){icon.setTint(color);invalidate();}}
    @Override protected void onDraw(Canvas canvas){
        if(icon==null){super.onDraw(canvas);return;}
        int size=Math.min(iconSize,Math.min(getWidth(),getHeight()));int x=(getWidth()-size)/2,y=(getHeight()-size)/2;
        icon.setBounds(x,y,x+size,y+size);icon.draw(canvas);
        if(badge){android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setColor(WorkspaceDialogs.ACCENT);float d=getResources().getDisplayMetrics().density;canvas.drawCircle(getWidth()-10*d,10*d,3*d,paint);}
    }
}
