package com.termux.app.workspace;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
/** Content-sized message surface, bounded by the available chat width on every measure. */
final class AgentBubble extends LinearLayout {
    AgentBubble(Context context){super(context);setOrientation(VERTICAL);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int mode=View.MeasureSpec.getMode(widthSpec),width=View.MeasureSpec.getSize(widthSpec);
        super.onMeasure(mode==View.MeasureSpec.UNSPECIFIED?widthSpec:View.MeasureSpec.makeMeasureSpec(Math.max(1,(int)Math.floor(width*.85f)),View.MeasureSpec.AT_MOST),heightSpec);
    }
}
