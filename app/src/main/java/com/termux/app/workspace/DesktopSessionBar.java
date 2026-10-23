package com.termux.app.workspace;

import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.*;

/** Two independent actions; animation invalidates only the programs button drawable. */
final class DesktopSessionBar extends LinearLayout {
    final TextView desktop,programs;
    final WorkspaceTransferBorder border;
    DesktopSessionBar(Context context,String label,Runnable switchDesktop,Runnable openPrograms){
        super(context);setOrientation(HORIZONTAL);
        desktop=AgentStatusUi.chip(context,label,AgentStatusUi.tr(context,"Переключение терминала и рабочего стола","Switch terminal and desktop"),switchDesktop);desktop.setTag("session-x11-desktop");
        programs=AgentStatusUi.chip(context,AgentStatusUi.tr(context,"Программы","Programs"),AgentStatusUi.tr(context,"Программы рабочего стола","Desktop programs"),openPrograms);programs.setTag("session-x11-programs");
        LayoutParams left=new LayoutParams(0,-2,1),right=new LayoutParams(0,-2,1);right.leftMargin=WorkspaceDialogs.dp(context,6);
        addView(desktop,left);addView(programs,right);
        border=new WorkspaceTransferBorder(context.getResources().getDisplayMetrics().density);
        programs.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{programs.getBackground(),border}));
    }
    void working(boolean active){border.active(active);}
    @Override protected void onDetachedFromWindow(){border.active(false);super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(border!=null&&visibility!=VISIBLE)border.active(false);}
}
