package com.termux.app.workspace;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/** Keeps both terminal switches in one row using their actual scaled measurements. */
final class TerminalModeRow extends ViewGroup {
    private String full="",compact="";
    TerminalModeRow(Context context){super(context);}
    void labels(String full,String compact){this.full=full;this.compact=compact;if(getChildCount()==2&&((Switch)getChildAt(1)).getText().length()==0)((Switch)getChildAt(1)).setText(full);requestLayout();}
    private void text(Switch target,String value){if(!value.contentEquals(target.getText()))target.setText(value);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        if(getChildCount()!=2){super.onMeasure(widthSpec,heightSpec);return;}
        Switch first=(Switch)getChildAt(0),second=(Switch)getChildAt(1);
        int unlimited=MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
        text(second,full);first.measure(unlimited,unlimited);second.measure(unlimited,unlimited);
        int padding=getPaddingLeft()+getPaddingRight();
        int width=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?first.getMeasuredWidth()+second.getMeasuredWidth()+padding:MeasureSpec.getSize(widthSpec);
        int available=Math.max(0,width-padding);
        if(first.getMeasuredWidth()+second.getMeasuredWidth()>available){text(second,compact);second.measure(unlimited,unlimited);}
        int left=first.getMeasuredWidth(),right=second.getMeasuredWidth();
        if(left+right>available){
            // At extreme accessibility scales labels may wrap inside their own
            // buttons. Neither switch moves to another row or loses its touch area.
            int firstBase=first.getCompoundPaddingLeft()+first.getCompoundPaddingRight();
            int secondBase=second.getCompoundPaddingLeft()+second.getCompoundPaddingRight();
            int textSpace=Math.max(0,available-firstBase-secondBase);
            int firstText=Math.max(1,left-firstBase),secondText=Math.max(1,right-secondBase);
            left=firstBase+(int)((long)textSpace*firstText/(firstText+secondText));right=available-left;
        }
        first.measure(MeasureSpec.makeMeasureSpec(left,MeasureSpec.EXACTLY),unlimited);
        second.measure(MeasureSpec.makeMeasureSpec(right,MeasureSpec.EXACTLY),unlimited);
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(Math.max(first.getMeasuredHeight(),second.getMeasuredHeight())+getPaddingTop()+getPaddingBottom(),heightSpec));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        if(getChildCount()!=2)return;
        View first=getChildAt(0),second=getChildAt(1);
        int height=getHeight()-getPaddingTop()-getPaddingBottom();
        int y=getPaddingTop()+(height-first.getMeasuredHeight())/2;
        first.layout(getPaddingLeft(),y,getPaddingLeft()+first.getMeasuredWidth(),y+first.getMeasuredHeight());
        y=getPaddingTop()+(height-second.getMeasuredHeight())/2;
        int end=getWidth()-getPaddingRight();second.layout(end-second.getMeasuredWidth(),y,end,y+second.getMeasuredHeight());
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);}
}
