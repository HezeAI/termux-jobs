package com.termux.app.workspace;

import android.app.Activity;
import android.view.*;
import android.widget.*;
import com.termux.R;

/** In-activity terminal presentation; it owns no PTY, process or native history. */
final class AgentTerminalScreen extends LinearLayout {
    private final Activity activity;
    private Runnable dismissed=()->{};
    private boolean closing;
    AgentTerminalScreen(Activity a,String title,String path,Runnable returnToChat){super(a);activity=a;setOrientation(VERTICAL);setBackgroundColor(WorkspaceDialogs.BG);setTag("agent-terminal-screen");setClickable(true);
        LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(14),dp(14),dp(14),dp(13));
        WorkspaceIconButton menu=new WorkspaceIconButton(a);menu.setIcon(a.getDrawable(R.drawable.workspace_menu),dp(21));menu.setTextColor(WorkspaceDialogs.MUTED);menu.setContentDescription(AgentStatusUi.tr(a,"Открыть меню","Open menu"));menu.setOnClickListener(v->{View headerView=a.findViewById(R.id.workspace_header);View original=headerView==null?null:headerView.findViewWithTag("workspace-menu");if(original!=null)original.performClick();});header.addView(menu,new LayoutParams(dp(44),dp(44)));
        LinearLayout names=new LinearLayout(a);names.setOrientation(VERTICAL);names.setPadding(dp(8),0,dp(8),0);TextView name=new TextView(a);name.setText(title);name.setTextSize(16);name.setTypeface(null,android.graphics.Typeface.BOLD);name.setTextColor(WorkspaceDialogs.TEXT);names.addView(name);TextView folder=new TextView(a);folder.setText(path);folder.setTextSize(10);folder.setTextColor(WorkspaceDialogs.MUTED);names.addView(folder);header.addView(names,new LayoutParams(0,-2,1));
        TextView back=WorkspaceDialogs.action(a,AgentStatusUi.tr(a,"Вернуться в чат","Return to chat"),0);back.setTag("agent-terminal-return");back.setTextSize(11);back.setMinHeight(dp(44));back.setPadding(dp(8),dp(6),dp(8),dp(6));back.setGravity(Gravity.CENTER);back.setOnClickListener(v->returnToChat.run());header.addView(back,new LayoutParams(dp(120),-2));addView(header,new LayoutParams(-1,-2));
        View line=new View(a);line.setBackgroundColor(WorkspaceDialogs.LINE);addView(line,new LayoutParams(-1,dp(1)));
    }
    private int dp(int n){return WorkspaceDialogs.dp(getContext(),n);}
    void body(View view){addView(view,new LayoutParams(-1,0,1));}
    void setOnDismissListener(Runnable listener){dismissed=listener;}
    boolean isShowing(){return getParent()!=null&&!closing;}
    void show(){ViewGroup root=activity.findViewById(R.id.activity_termux_root_relative_layout);root.addView(this,new android.widget.RelativeLayout.LayoutParams(-1,-1));bringToFront();}
    void dismiss(){if(closing)return;closing=true;ViewParent parent=getParent();if(parent instanceof ViewGroup)((ViewGroup)parent).removeView(this);dismissed.run();}
}
