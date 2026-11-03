package com.termux.app.workspace;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Shared library-style dialog: fixed header/footer, exactly one vertical viewport. */
final class AgentManagerLayout extends LinearLayout {
    final LinearLayout header,content;
    final ScrollView scroll;
    final Footer footer;
    AgentManagerLayout(Context c){
        super(c);setOrientation(VERTICAL);int p=WorkspaceDialogs.dp(c,10);setPadding(p,0,p,p);
        header=column(c);header.setTag("agent-manager-header");addView(header,new LayoutParams(-1,-2));
        content=column(c);scroll=new ScrollView(c);scroll.setTag("agent-manager-scroll");scroll.setFillViewport(true);scroll.addView(content,new ScrollView.LayoutParams(-1,-2));addView(scroll,new LayoutParams(-1,0,1));
        footer=new Footer(c);footer.setTag("agent-manager-footer");LayoutParams params=new LayoutParams(-1,-2);params.topMargin=WorkspaceDialogs.dp(c,6);addView(footer,params);
    }
    private static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(VERTICAL);return v;}
    void actions(View... buttons){footer.removeAllViews();for(View button:buttons){if(button==null)continue;if(button.getParent() instanceof ViewGroup)((ViewGroup)button.getParent()).removeView(button);if(button instanceof TextView)((TextView)button).setMinHeight(WorkspaceDialogs.dp(getContext(),48));else button.setMinimumHeight(WorkspaceDialogs.dp(getContext(),48));int x=WorkspaceDialogs.dp(getContext(),12),y=WorkspaceDialogs.dp(getContext(),8);button.setPadding(x,y,x,y);footer.addView(button,new ViewGroup.LayoutParams(-2,-2));}}
    @Override protected void onMeasure(int w,int h){
        if(MeasureSpec.getMode(h)==MeasureSpec.AT_MOST){
            int width=MeasureSpec.getSize(w)-getPaddingLeft()-getPaddingRight();
            int exactWidth=MeasureSpec.makeMeasureSpec(Math.max(1,width),MeasureSpec.EXACTLY),natural=MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
            header.measure(exactWidth,natural);footer.measure(exactWidth,natural);content.measure(exactWidth,natural);
            int desired=getPaddingTop()+getPaddingBottom()+header.getMeasuredHeight()+footer.getMeasuredHeight()+content.getMeasuredHeight()+WorkspaceDialogs.dp(getContext(),6);
            h=MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(h),desired),MeasureSpec.EXACTLY);
        }
        super.onMeasure(w,h);
    }
    static TextView status(Context c){return new androidx.appcompat.widget.AppCompatTextView(c){
        private boolean reveal;
        @Override protected void onTextChanged(CharSequence text,int start,int before,int count){super.onTextChanged(text,start,before,count);reveal=text!=null&&text.length()>0;setVisibility(reveal?VISIBLE:GONE);setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);if(reveal){reveal=false;post(()->{if(isShown()&&length()>0)requestRectangleOnScreen(new android.graphics.Rect(0,0,getWidth(),getHeight()),true);});}}
    };}
    static void keyboard(android.app.Dialog dialog){Window w=dialog.getWindow();if(w!=null){w.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);}}
    /** Wrap whole labels onto a second row instead of clipping or shrinking text. */
    static final class Footer extends ViewGroup {
        private final List<android.graphics.Rect> cells=new ArrayList<>();
        Footer(Context c){super(c);}
        @Override public void onViewAdded(View child){super.onViewAdded(child);cells.add(new android.graphics.Rect());}
        @Override public void onViewRemoved(View child){super.onViewRemoved(child);if(!cells.isEmpty())cells.remove(cells.size()-1);}
        @Override protected void onMeasure(int w,int h){
            int width=MeasureSpec.getSize(w),gap=WorkspaceDialogs.dp(getContext(),6),x=0,y=0,rowHeight=0;
            for(int i=0;i<getChildCount();i++){View v=getChildAt(i);if(v.getVisibility()==GONE){cells.get(i).setEmpty();continue;}v.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int cw=v.getMeasuredWidth(),ch=v.getMeasuredHeight();if(x>0&&x+cw>width){y+=rowHeight+gap;x=0;rowHeight=0;}cells.get(i).set(x,y,x+cw,y+ch);x+=cw+gap;rowHeight=Math.max(rowHeight,ch);}
            setMeasuredDimension(resolveSize(width,w),resolveSize(y+rowHeight,h));
        }
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(int i=0;i<getChildCount();i++){android.graphics.Rect cell=cells.get(i);getChildAt(i).layout(cell.left,cell.top,cell.right,cell.bottom);}}
    }
}
