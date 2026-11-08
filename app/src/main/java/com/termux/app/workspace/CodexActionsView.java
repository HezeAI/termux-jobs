package com.termux.app.workspace;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.function.Consumer;

/** One stable, expandable card for a consecutive run of structured agent actions. */
final class CodexActionsView extends LinearLayout {
    final TextView heading,summary,time,currentAction,outcomes;
    final LinearLayout steps;
    final LinearLayout summaryCard;
    private final AgentChatView owner;
    private final Consumer<JSONObject> open;
    private final Map<String,TextView> entries=new HashMap<>();
    private final Map<String,JSONObject> data=new HashMap<>();
    private boolean expanded;
    private String skillName="";
    private int count,active,errors,failures,declined,interrupted,cancelled;
    private boolean onlyShell;

    CodexActionsView(AgentChatView owner,Consumer<JSONObject> open) {
        super(owner.getContext());this.owner=owner;this.open=open;size=new AgentCardSize(this,owner);
        setOrientation(VERTICAL);
        summaryCard=new LinearLayout(getContext());summaryCard.setOrientation(VERTICAL);summaryCard.setPadding(owner.dp(4),owner.dp(3),owner.dp(4),owner.dp(4));summaryCard.setTag("timeline-summary-hit");summaryCard.setBackground(WorkspaceRipple.background(getContext(),null,12));summaryCard.setOnClickListener(v->{expanded=!expanded;owner.rememberActions(data.values(),expanded);refreshHeading();});addView(summaryCard,new LayoutParams(-1,-2));
        GradientDrawable background=new GradientDrawable();background.setColor(0xff232426);
        background.setCornerRadius(owner.dp(12));background.setStroke(owner.dp(1),0xff393a3c);setBackground(background);
        heading=owner.label("",13);heading.setMinHeight(owner.dp(36));heading.setPadding(owner.dp(10),owner.dp(5),owner.dp(10),owner.dp(5));heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);heading.setClickable(false);
        LinearLayout header=new LinearLayout(getContext());header.setOrientation(HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(heading,new LayoutParams(0,-2,1));
        time=owner.label("",12);time.setTag("action-group-time");time.setTextColor(0xffa5a5a2);time.setSingleLine(true);time.setPadding(0,owner.dp(5),owner.dp(10),owner.dp(5));time.setClickable(false);header.addView(time,new LayoutParams(-2,-2));summaryCard.addView(header,new LayoutParams(-1,-2));
        summary=owner.label("",12);summary.setTextColor(0xffa5a5a2);summary.setSingleLine(false);summary.setPadding(owner.dp(10),0,owner.dp(10),owner.dp(4));summary.setClickable(false);summaryCard.addView(summary,new LayoutParams(-1,-2));
        currentAction=owner.label("",12);currentAction.setTag("timeline-current");currentAction.setTextColor(0xffa5a5a2);currentAction.setPadding(owner.dp(10),0,owner.dp(10),owner.dp(4));currentAction.setClickable(false);summaryCard.addView(currentAction,new LayoutParams(-1,-2));
        outcomes=owner.label("",12);outcomes.setTag("timeline-outcomes");outcomes.setPadding(owner.dp(10),0,owner.dp(10),owner.dp(4));outcomes.setClickable(false);summaryCard.addView(outcomes,new LayoutParams(-1,-2));
        steps=new LinearLayout(getContext());steps.setOrientation(VERTICAL);steps.setPadding(owner.dp(4),0,owner.dp(4),owner.dp(4));steps.setVisibility(GONE);addView(steps,new LayoutParams(-1,-2));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(0,owner.dp(4),0,owner.dp(6));setLayoutParams(layout);
    }

    boolean sharesActions(JSONObject group){JSONArray next=group.optJSONArray("steps");if(next==null)return false;Set<String> ids=new HashSet<>();for(JSONObject row:data.values())ids.add(owner.choiceKey(row));for(int i=0;i<next.length();i++)if(next.optJSONObject(i)!=null&&ids.contains(owner.choiceKey(next.optJSONObject(i))))return true;return false;}
    void seedCardHeight(int height){size.seed(height);}
    boolean smoothResize;
    private final AgentCardSize size;
    @Override protected void onMeasure(int width,int height){
        super.onMeasure(width,height);size.enabled=smoothResize;setMeasuredDimension(getMeasuredWidth(),size.measure(getMeasuredHeight()));
    }
    @Override protected void onDetachedFromWindow(){size.detached();super.onDetachedFromWindow();}

    void update(JSONObject group) {
        JSONArray rows=group.optJSONArray("steps");if(rows==null)return;skillName=group.optString("skillName");
        long eventTime=group.optLong("eventTime");time.setVisibility(eventTime>0?VISIBLE:GONE);time.setText(eventTime>0?AgentMessageTime.timelineTime(eventTime):"");time.setContentDescription(eventTime>0?owner.tr("Время события: ","Event time: ")+AgentMessageTime.full(getContext(),eventTime):"");
        Boolean choice=owner.actionChoice(rows);if(choice!=null)expanded=choice;
        count=rows.length();onlyShell=true;active=0;errors=0;failures=0;declined=0;interrupted=0;cancelled=0;String current="";Set<String> present=new HashSet<>();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;onlyShell&=AgentTimelineSummary.kind(row)==AgentTimelineSummary.Kind.COMMAND;
            if(CodexActionTimeline.running(row)||java.util.Arrays.asList("awaiting_approval","pending","scheduled").contains(CodexActionTimeline.status(row))){active++;if(current.isEmpty())current=caption(row)+" · "+state(row);}
            if(CodexActionTimeline.failed(row)){errors++;String status=CodexActionTimeline.status(row);if(status.equals("declined"))declined++;else if(AgentTimelineSummary.cancelled(row))cancelled++;else if(status.equals("aborted"))interrupted++;else failures++;}
            String key=row.optString("key");present.add(key);data.put(key,row);
            TextView item=entries.get(key);
            if(item==null){item=owner.label("",12);item.setMinHeight(owner.dp(48));item.setSingleLine(false);item.setMaxLines(Integer.MAX_VALUE);item.setEllipsize(null);item.setFocusable(true);entries.put(key,item);
                android.graphics.drawable.StateListDrawable outline=new android.graphics.drawable.StateListDrawable();
                for(boolean pressed:new boolean[]{true,false}){GradientDrawable bg=new GradientDrawable();bg.setColor(pressed?WorkspaceDialogs.TINT:0xff232426);bg.setCornerRadius(owner.dp(8));bg.setStroke(owner.dp(1),0xff393a3c);outline.addState(pressed?new int[]{android.R.attr.state_pressed}:new int[]{},bg);}item.setBackground(outline);
                LinearLayout.LayoutParams itemLayout=new LinearLayout.LayoutParams(-1,-2);itemLayout.setMargins(owner.dp(6),owner.dp(3),owner.dp(6),owner.dp(3));item.setLayoutParams(itemLayout);
                final String id=key;item.setOnClickListener(v->{JSONObject original=data.get(id);if(original!=null)open.accept(original);});}
            String state=state(row),command=caption(row);String type=AgentTimelineSummary.type(row,WorkspaceLanguage.code(getContext()).equals("ru"));
            String marker=AgentTimelineSummary.neutral(row)?"○ ":CodexActionTimeline.failed(row)?"! ":CodexActionTimeline.running(row)?"● ":CodexActionTimeline.status(row).equals("completed")?"✓ ":"○ ";
            String caption=marker+command+"\n"+state+"\n"+type;
            long start=AgentTimelineSummary.start(row);if(start>0)caption+="\n"+owner.tr("Начало: ","Started: ")+AgentMessageTime.timelineTime(start);
            if(!item.getText().toString().equals(caption)){android.text.SpannableString shown=new android.text.SpannableString(caption);int kindAt=caption.indexOf("\n"+type)+1;shown.setSpan(new android.text.style.ForegroundColorSpan(0xffa5a5a2),kindAt,kindAt+type.length(),0);item.setText(shown);}
            item.setTextColor(CodexActionTimeline.failed(row)&&!AgentTimelineSummary.neutral(row)?0xffffb08f:0xfff0f0ee);
            item.setContentDescription(command+". "+state+". "+owner.tr("Открыть действие и подробности","Open action and details"));
            if(item.getParent()==null)steps.addView(item,i);else if(steps.indexOfChild(item)!=i){steps.removeView(item);steps.addView(item,i);}
        }
        for(String key:new HashSet<>(entries.keySet()))if(!present.contains(key)){steps.removeView(entries.remove(key));data.remove(key);}
        String errorText=failures==1?owner.tr(onlyShell?"Одна команда завершилась с ошибкой":"Одно действие завершилось с ошибкой",onlyShell?"One command failed":"One action failed"):failures>1?owner.tr(onlyShell?"Команд с ошибкой: ":"Действий с ошибкой: ",onlyShell?"Failed commands: ":"Failed actions: ")+failures:"";
        if(declined>0)errorText+=(errorText.isEmpty()?"":" · ")+owner.tr("Отклонено: ","Declined: ")+declined;
        if(cancelled>0)errorText+=(errorText.isEmpty()?"":" · ")+owner.tr("Отменено: ","Cancelled: ")+cancelled;
        if(interrupted>0)errorText+=(errorText.isEmpty()?"":" · ")+owner.tr("Прервано: ","Interrupted: ")+interrupted;
        String counts=AgentTimelineSummary.counts(rows,WorkspaceLanguage.code(getContext()).equals("ru"));if(!summary.getText().toString().equals(counts))summary.setText(counts);summary.setVisibility(VISIBLE);
        String now=active>0?owner.tr("Сейчас: ","Current: ")+current:"";if(!currentAction.getText().toString().equals(now))currentAction.setText(now);currentAction.setVisibility(now.isEmpty()?GONE:VISIBLE);
        if(!outcomes.getText().toString().equals(errorText))outcomes.setText(errorText);outcomes.setTextColor(failures>0||interrupted>0?0xffffb08f:0xffa5a5a2);outcomes.setVisibility(errorText.isEmpty()?GONE:VISIBLE);
        refreshHeading();
    }

    int stepTop(String key){TextView entry=entries.get(key);return steps.getVisibility()!=VISIBLE||entry==null?-1:steps.getTop()+entry.getTop();}
    String readingStep(int y){if(steps.getVisibility()!=VISIBLE||y<steps.getTop())return null;for(int i=0;i<steps.getChildCount();i++){TextView item=(TextView)steps.getChildAt(i);if(steps.getTop()+item.getBottom()>y)for(Map.Entry<String,TextView> e:entries.entrySet())if(e.getValue()==item)return e.getKey();}return null;}

    private String caption(JSONObject row){return AgentTimelineSummary.title(row,WorkspaceLanguage.code(getContext()).equals("ru"));}

    private String state(JSONObject row) {return AgentActionPresentation.state(row,WorkspaceLanguage.code(getContext()).equals("ru"));}

    private String actionWord(){
        String ru=count%100>=11&&count%100<=14?" действий":count%10==1?" действие":count%10>=2&&count%10<=4?" действия":" действий";
        return owner.tr(ru,count==1?" action":" actions");
    }

    private void refreshHeading() {
        String caption=(!skillName.isEmpty()?owner.tr("Навык: ","Skill: ")+skillName:active>0?owner.tr("В работе","Running"):owner.tr("Хронология","Timeline"))+" · "+count+actionWord()+(expanded?" ▴":" ▾");
        if(!heading.getText().toString().equals(caption))heading.setText(caption);
        LinearLayout.LayoutParams layout=(LinearLayout.LayoutParams)getLayoutParams();int width=!skillName.isEmpty()&&!expanded&&time.getVisibility()==GONE?LayoutParams.WRAP_CONTENT:LayoutParams.MATCH_PARENT;if(layout.width!=width){layout.width=width;setLayoutParams(layout);}
        heading.setTextColor(failures>0?0xffffb08f:0xffecece5);steps.setVisibility(expanded?VISIBLE:GONE);
        summaryCard.setContentDescription(caption+". "+summary.getText()+". "+currentAction.getText()+". "+outcomes.getText()+". "+owner.tr(expanded?"Свернуть":"Развернуть",expanded?"Collapse":"Expand"));
    }
}
