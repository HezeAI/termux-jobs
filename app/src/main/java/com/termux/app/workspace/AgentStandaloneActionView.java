package com.termux.app.workspace;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
/** Separate website/subagent activity in the chat; details never rerun the tool. */
final class AgentStandaloneActionView extends LinearLayout {
 private final AgentChatView owner;
 final TextView heading,state;
 private JSONObject row=new JSONObject();
 AgentStandaloneActionView(AgentChatView owner){
  super(owner.getContext());this.owner=owner;setOrientation(VERTICAL);setMinimumHeight(owner.dp(48));setBackground(WorkspaceRipple.background(getContext(),null,12));
  heading=owner.label("",13);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setMinHeight(owner.dp(40));addView(heading,new LayoutParams(-1,-2));
  state=owner.label("",11);state.setTextColor(WorkspaceDialogs.MUTED);addView(state,new LayoutParams(-1,-2));setOnClickListener(v->owner.openAction(row));
 }
 void update(JSONObject next){row=next;boolean agent=AgentSubagentPresentation.matches(row);JSONObject preview=AgentActionPresentation.preview(row);
  String title=agent?AgentSubagentPresentation.title(row,WorkspaceLanguage.code(getContext()).equals("ru")):owner.tr("Просмотр сайта","View website");
  if(!agent&&preview!=null&&preview.optString("operation").equals("search"))title=owner.tr("Поиск в интернете","Search the web");
  if(row.optString("toolType").equals("mcpToolCall"))title=AgentTimelineSummary.title(row,WorkspaceLanguage.code(getContext()).equals("ru"));
  boolean compact=row.optString("toolType").equals("contextCompaction");
  if(compact)title=owner.tr("Сжатие контекста","Context compaction")+" • "+(CodexActionTimeline.running(row)?owner.tr("выполняется","in progress"):row.optString("status").equals("completed")?owner.tr("выполнено","completed"):owner.tr("не завершено","not completed"));
  boolean completed=CodexActionTimeline.status(row).equals("completed")&&!CodexActionTimeline.failed(row);
  state.setVisibility(compact||completed?GONE:VISIBLE);
  android.graphics.drawable.Drawable icon=getContext().getDrawable(compact?com.termux.R.drawable.workspace_compact:row.optString("toolType").equals("mcpToolCall")?com.termux.R.drawable.workspace_mcp:agent?com.termux.R.drawable.workspace_ai_agent:com.termux.R.drawable.workspace_globe).mutate();icon.setTint(WorkspaceDialogs.MUTED);icon.setBounds(0,0,owner.dp(20),owner.dp(20));heading.setCompoundDrawablesRelative(icon,null,null,null);heading.setCompoundDrawablePadding(owner.dp(8));
  heading.setText(title+" ›");state.setText(AgentActionPresentation.state(row,WorkspaceLanguage.code(getContext()).equals("ru")));setContentDescription(state.getVisibility()==GONE?title:title+". "+state.getText());
 }
}
