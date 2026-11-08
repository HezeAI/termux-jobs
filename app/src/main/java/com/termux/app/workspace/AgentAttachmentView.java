package com.termux.app.workspace;

import android.widget.LinearLayout;

/** Stable attachment shell, including late thumbnail and file-detail updates. */
final class AgentAttachmentView extends LinearLayout {
    final AgentCardSize size;
    AgentAttachmentView(AgentChatView owner){super(owner.getContext());size=new AgentCardSize(this,owner);}
    @Override protected void onMeasure(int width,int height){super.onMeasure(width,height);setMeasuredDimension(getMeasuredWidth(),size.measure(getMeasuredHeight()));}
    @Override protected void onDetachedFromWindow(){size.detached();super.onDetachedFromWindow();}
}
