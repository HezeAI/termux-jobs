package com.termux.app.workspace;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Wraps composer controls without shrinking their touch targets or clipping large text. */
final class ComposerActionLayout extends ViewGroup {
    ComposerActionLayout(Context c){super(c);}
    @Override protected void onMeasure(int ws,int hs){
        int width=MeasureSpec.getSize(ws),available=Math.max(0,width-getPaddingLeft()-getPaddingRight());
        int x=0,y=0,line=0;
        for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(v.getVisibility()==GONE)continue;
            v.measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
            if(x>0&&x+v.getMeasuredWidth()>available){y+=line;x=0;line=0;}
            x+=v.getMeasuredWidth();line=Math.max(line,v.getMeasuredHeight());
        }
        setMeasuredDimension(resolveSize(width,ws),resolveSize(y+line+getPaddingTop()+getPaddingBottom(),hs));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int width=r-l-getPaddingRight(),x=getPaddingLeft(),y=getPaddingTop(),line=0;
        for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(v.getVisibility()==GONE)continue;
            if(x>getPaddingLeft()&&x+v.getMeasuredWidth()>width){y+=line;x=getPaddingLeft();line=0;}
            int left=i==getChildCount()-1?Math.max(x,width-v.getMeasuredWidth()):x;
            v.layout(left,y,left+v.getMeasuredWidth(),y+v.getMeasuredHeight());x+=v.getMeasuredWidth();line=Math.max(line,v.getMeasuredHeight());
        }
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-2,-2);}
}
