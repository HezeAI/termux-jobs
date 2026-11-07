package com.termux.app.workspace;
import android.content.Context;import android.graphics.Canvas;import android.graphics.Paint;import android.view.View;import android.view.ViewGroup;import android.widget.LinearLayout;
/** The owner-local branch never extends into the next session. Decorative only. */
final class SidebarTreeActions extends LinearLayout {
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG);
    SidebarTreeActions(Context c){super(c);setOrientation(VERTICAL);setWillNotDraw(false);line.setColor(0xff748e81);line.setStrokeWidth(Math.max(1,WorkspaceDialogs.dp(c,2)));setPadding(WorkspaceDialogs.dp(c,10),0,0,WorkspaceDialogs.dp(c,8));}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float x=line.getStrokeWidth()/2;canvas.drawLine(x,0,x,getHeight(),line);branches(canvas,this,0);}
    private void branches(Canvas canvas,ViewGroup group,int offset){
        for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);if(child.getVisibility()!=VISIBLE)continue;int top=offset+child.getTop();
            if(child.hasOnClickListeners()){float y=top+child.getHeight()/2f;canvas.drawLine(line.getStrokeWidth()/2,y,WorkspaceDialogs.dp(getContext(),7),y,line);}
            else if(child instanceof ViewGroup)branches(canvas,(ViewGroup)child,top);
        }
    }
}
