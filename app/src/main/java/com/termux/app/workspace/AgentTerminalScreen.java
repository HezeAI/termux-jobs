package com.termux.app.workspace;

import android.app.Activity;
import android.view.*;
import android.widget.*;
import com.termux.R;

/** In-activity terminal presentation; it owns no PTY, process or native history. */
final class AgentTerminalScreen extends LinearLayout {
    private final Activity activity;
    private Runnable dismissed=()->{};
    private boolean closing,finished;
    private boolean terminalPresentation;
    private LinearLayout terminalHeader;private android.widget.FrameLayout terminalHost;private java.util.function.IntConsumer topInset=value->{};
    AgentTerminalScreen(Activity a,String title,String path,Runnable returnToChat){this(a,title,path,AgentStatusUi.tr(a,"Вернуться в чат","Return to chat"),"agent-terminal-screen",returnToChat);}
    AgentTerminalScreen(Activity a,String title,String path,String backLabel,String tag,Runnable returnToChat){super(a);activity=a;setOrientation(VERTICAL);setBackgroundColor(WorkspaceDialogs.BG);setTag(tag);setClickable(true);
        LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(14),dp(14),dp(14),dp(13));
        WorkspaceIconButton menu=new WorkspaceIconButton(a);menu.setIcon(a.getDrawable(R.drawable.workspace_menu),dp(21));menu.setTextColor(WorkspaceDialogs.MUTED);menu.setTag("page-menu");menu.setContentDescription(AgentStatusUi.tr(a,"Открыть меню","Open menu"));menu.setOnClickListener(v->{View headerView=a.findViewById(R.id.workspace_header);View original=headerView==null?null:headerView.findViewWithTag("workspace-menu");if(original!=null)original.performClick();});header.addView(menu,new LayoutParams(dp(44),dp(44)));
        LinearLayout names=new LinearLayout(a);names.setOrientation(VERTICAL);names.setPadding(dp(8),0,dp(8),0);TextView name=new TextView(a);name.setText(title);name.setTextSize(16);name.setTypeface(null,android.graphics.Typeface.BOLD);name.setTextColor(WorkspaceDialogs.TEXT);names.addView(name);TextView folder=new TextView(a);folder.setText(path);folder.setTextSize(10);folder.setTextColor(WorkspaceDialogs.MUTED);names.addView(folder);header.addView(names,new LayoutParams(0,-2,1));
        TextView back=WorkspaceDialogs.action(a,backLabel,0);back.setTag("agent-terminal-return");back.setTextSize(11);back.setMinHeight(dp(44));back.setPadding(dp(8),dp(6),dp(8),dp(6));back.setGravity(Gravity.CENTER);back.setOnClickListener(v->returnToChat.run());header.addView(back,new LayoutParams(dp(120),-2));addView(header,new LayoutParams(-1,-2));
        if("agent-terminal-screen".equals(tag)){terminalPresentation=true;setBackgroundColor(android.graphics.Color.BLACK);header.removeAllViews();View spacer=new View(a);header.addView(spacer,new LayoutParams(0,1,1));WorkspaceIconButton chat=new WorkspaceIconButton(a);chat.setIcon(a.getDrawable(R.drawable.workspace_chat),dp(21));chat.setTextColor(WorkspaceDialogs.TEXT);chat.setTag("agent-terminal-return");chat.setContentDescription(AgentStatusUi.tr(a,"Вернуться в чат","Return to chat"));chat.setBackground(WorkspaceRipple.background(a,WorkspaceDialogs.cardBackground(a),14));chat.setOnClickListener(v->returnToChat.run());header.addView(chat,new LayoutParams(dp(44),dp(44)));}
        if(terminalPresentation){terminalHeader=header;removeView(header);terminalHost=new FrameLayout(a);addView(terminalHost,new LayoutParams(-1,0,1));FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);terminalHost.addView(header,hp);header.setElevation(dp(2));header.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->topInset.accept(b-t));}
        else{View line=new View(a);line.setBackgroundColor(WorkspaceDialogs.LINE);addView(line,new LayoutParams(-1,dp(1)));}
    }
    private int dp(int n){return WorkspaceDialogs.dp(getContext(),n);}
    void navigationEnabled(boolean enabled){if(findViewWithTag("page-menu")!=null)findViewWithTag("page-menu").setEnabled(enabled);findViewWithTag("agent-terminal-return").setEnabled(enabled);}
    void body(View view){if(terminalPresentation){terminalHost.addView(view,0,new FrameLayout.LayoutParams(-1,-1));terminalHeader.bringToFront();}else addView(view,new LayoutParams(-1,0,1));}
    void headerInset(java.util.function.IntConsumer listener){topInset=listener;if(terminalHeader!=null)topInset.accept(terminalHeader.getHeight());}
    void setOnDismissListener(Runnable listener){dismissed=listener;}
    boolean isShowing(){return getParent()!=null&&!closing;}
    void show(){ViewGroup root=activity.findViewById(R.id.activity_termux_root_relative_layout);float elevation=0;for(int i=0;i<root.getChildCount();i++)elevation=Math.max(elevation,root.getChildAt(i).getElevation());setElevation(elevation+dp(1));root.addView(this,new android.widget.RelativeLayout.LayoutParams(-1,-1));bringToFront();if(terminalPresentation&&android.animation.ValueAnimator.areAnimatorsEnabled()){setTranslationX(root.getWidth());animate().translationX(0).setDuration(240).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}}
    void dismiss(){if(closing)return;closing=true;animate().cancel();if(terminalPresentation&&android.animation.ValueAnimator.areAnimatorsEnabled()&&getParent()!=null){animate().translationX(((View)getParent()).getWidth()).setDuration(240).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(this::finishDismiss).start();}else finishDismiss();}
    void dismissImmediately(){animate().cancel();closing=true;finishDismiss();}
    private void finishDismiss(){if(finished)return;finished=true;ViewParent parent=getParent();if(parent instanceof ViewGroup)((ViewGroup)parent).removeView(this);dismissed.run();}
}
