package com.termux.app.workspace;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

/** Keeps the server viewport stable during a local content replacement, not during user navigation. */
final class ServerScrollView extends ScrollView {
    private boolean preserving;
    private int savedY;
    ServerScrollView(Context context){super(context);}
    void preserve(Runnable update){
        if(!preserving)savedY=getScrollY();
        preserving=true;
        try{update.run();}finally{requestLayout();}
    }
    void restoreTo(int y){savedY=Math.max(0,y);preserving=true;requestLayout();}
    @Override public void requestChildFocus(View child,View focused){
        super.requestChildFocus(child,focused);
    }
    @Override public void scrollTo(int x,int y){if(!preserving)super.scrollTo(x,y);}
    @Override public boolean requestChildRectangleOnScreen(View child,Rect rect,boolean immediate){
        return !preserving&&super.requestChildRectangleOnScreen(child,rect,immediate);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        boolean restore=preserving;int y=savedY;
        super.onLayout(changed,l,t,r,b);
        if(restore){super.scrollTo(0,y);preserving=false;}
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)preserving=false;
        return super.onInterceptTouchEvent(event);
    }
    @Override public boolean onGenericMotionEvent(MotionEvent event){preserving=false;return super.onGenericMotionEvent(event);}
}
