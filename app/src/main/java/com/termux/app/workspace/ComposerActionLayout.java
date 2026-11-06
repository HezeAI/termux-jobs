package com.termux.app.workspace;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Wraps at natural widths, keeping the model, microphone and send action together. */
final class ComposerActionLayout extends ViewGroup {
    private View rightStart;
    ComposerActionLayout(Context c){super(c);}
    void rightGroup(View first){rightStart=first;requestLayout();}
    @Override protected void onMeasure(int ws,int hs){
        int width=MeasureSpec.getSize(ws),available=Math.max(0,width-getPaddingLeft()-getPaddingRight());
        for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(v.getVisibility()!=GONE)v.measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));}
        setMeasuredDimension(resolveSize(width,ws),resolveSize(arrange(width,false)+getPaddingBottom(),hs));
    }
    private int arrange(int width,boolean layout){
        int end=width-getPaddingRight(),x=getPaddingLeft(),y=getPaddingTop(),line=0;
        int right=rightStart==null?getChildCount()-1:indexOfChild(rightStart);
        for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(v.getVisibility()==GONE)continue;
            if(i==right){int remaining=0;for(int j=i;j<getChildCount();j++){View child=getChildAt(j);if(child.getVisibility()!=GONE)remaining+=child.getMeasuredWidth();}
                if(x>getPaddingLeft()&&remaining>end-x){y+=line;x=getPaddingLeft();line=0;}
                x=Math.max(x,end-remaining);
            }
            if(x>getPaddingLeft()&&x+v.getMeasuredWidth()>end){y+=line;x=getPaddingLeft();line=0;}
            if(layout)v.layout(x,y,x+v.getMeasuredWidth(),y+v.getMeasuredHeight());
            x+=v.getMeasuredWidth();line=Math.max(line,v.getMeasuredHeight());
        }
        return y+line;
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){arrange(r-l,true);}
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-2,-2);}
}
