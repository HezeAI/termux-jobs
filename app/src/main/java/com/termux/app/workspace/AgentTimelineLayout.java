package com.termux.app.workspace;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/** Reorder a retained row without dispatching detach/attach or resetting its animations. */
final class AgentTimelineLayout extends LinearLayout {
    AgentTimelineLayout(Context context){super(context);setOrientation(VERTICAL);}
    void move(View child,int index){
        if(indexOfChild(child)==index)return;
        detachViewFromParent(child);
        attachViewToParent(child,index,child.getLayoutParams());
        requestLayout();invalidate();
    }
}
