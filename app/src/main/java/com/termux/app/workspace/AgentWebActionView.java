package com.termux.app.workspace;

import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import org.json.JSONObject;

/** One compact query/address. Every subrow opens the unchanged parent event. */
final class AgentWebActionView extends LinearLayout {
 private final AgentChatView owner;
 final AgentActionTextView heading;
 private JSONObject row=new JSONObject();
 private String title="";
 private boolean active=true;
 AgentWebActionView(AgentChatView owner){
  super(owner.getContext());this.owner=owner;setOrientation(VERTICAL);setMinimumHeight(owner.dp(48));setBackground(WorkspaceRipple.background(getContext(),null,12));setFocusable(true);
  heading=new AgentActionTextView(getContext());heading.setTextSize(13);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setMinHeight(owner.dp(44));heading.setSingleLine(false);heading.setMaxLines(4);heading.setEllipsize(TextUtils.TruncateAt.END);heading.setPadding(0,owner.dp(6),0,owner.dp(6));addView(heading,new LayoutParams(-1,-2));setOnClickListener(v->owner.openAction(row));
 }
 void update(JSONObject next,String text,boolean search){
  row=next;boolean ru=WorkspaceLanguage.code(getContext()).equals("ru");String state=AgentActionPresentation.state(row,ru);
  title=text.isEmpty()?owner.tr("Действие в интернете · подробности","Web action · details"):text;
  boolean failed=CodexActionTimeline.failed(row);if(failed)title=state+" · "+title;
  heading.setTextColor(failed?0xffffb08f:0xfff0f0ee);
  android.graphics.drawable.Drawable icon=getContext().getDrawable(search?com.termux.R.drawable.workspace_search:com.termux.R.drawable.workspace_globe).mutate();icon.setTint(failed?0xffffb08f:WorkspaceDialogs.MUTED);icon.setBounds(0,0,owner.dp(20),owner.dp(20));heading.setCompoundDrawablesRelative(icon,null,null,null);heading.setCompoundDrawablePadding(owner.dp(8));
  setContentDescription(title+". "+state+". "+owner.tr("Открыть действие и подробности","Open action and details"));refreshRunning();
 }
 void screenActive(boolean value){active=value;refreshRunning();}
 private void refreshRunning(){heading.present(title,active&&isShown()&&CodexActionTimeline.running(row));}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refreshRunning();}
 @Override protected void onVisibilityChanged(android.view.View view,int visibility){super.onVisibilityChanged(view,visibility);if(heading!=null)refreshRunning();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(heading!=null)heading.present(title,visibility==VISIBLE&&active&&isShown()&&CodexActionTimeline.running(row));}
 @Override protected void onDetachedFromWindow(){heading.present(title,false);super.onDetachedFromWindow();}
}
