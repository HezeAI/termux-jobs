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
    final TextView heading,summary;
    final LinearLayout steps;
    private final AgentChatView owner;
    private final Consumer<JSONObject> open;
    private final Map<String,TextView> entries=new HashMap<>();
    private final Map<String,JSONObject> data=new HashMap<>();
    private boolean expanded,userToggled,sawRunning;
    private String skillName="";
    private int count,active,errors,failures,declined,interrupted;
    private boolean onlyShell;

    CodexActionsView(AgentChatView owner,Consumer<JSONObject> open) {
        super(owner.getContext());this.owner=owner;this.open=open;
        setOrientation(VERTICAL);setPadding(owner.dp(4),owner.dp(3),owner.dp(4),owner.dp(4));
        GradientDrawable background=new GradientDrawable();background.setColor(0xff0b1319);
        background.setCornerRadius(owner.dp(12));background.setStroke(owner.dp(1),0xff28433d);setBackground(background);
        heading=owner.label("",13);heading.setMinHeight(owner.dp(36));heading.setPadding(owner.dp(10),owner.dp(5),owner.dp(10),owner.dp(5));heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);heading.setOnClickListener(v->{userToggled=true;expanded=!expanded;refreshHeading();});addView(heading,new LayoutParams(-1,-2));
        summary=owner.label("",12);summary.setTextColor(0xff91a6af);summary.setMaxLines(2);summary.setPadding(owner.dp(10),0,owner.dp(10),owner.dp(4));summary.setOnClickListener(v->heading.performClick());addView(summary,new LayoutParams(-1,-2));
        steps=new LinearLayout(getContext());steps.setOrientation(VERTICAL);steps.setVisibility(GONE);addView(steps,new LayoutParams(-1,-2));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(0,owner.dp(4),0,owner.dp(6));setLayoutParams(layout);
    }

    void update(JSONObject group) {
        JSONArray rows=group.optJSONArray("steps");if(rows==null)return;skillName=group.optString("skillName");
        count=rows.length();onlyShell=true;active=0;errors=0;failures=0;declined=0;interrupted=0;String current="";Set<String> present=new HashSet<>();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;onlyShell&=CodexActionTimeline.shell(row);
            if(CodexActionTimeline.running(row)){active++;if(current.isEmpty())current=caption(row);}
            if(CodexActionTimeline.failed(row)){errors++;String status=CodexActionTimeline.status(row);if(status.equals("declined"))declined++;else if(status.equals("aborted")||status.equals("cancelled")||status.equals("canceled"))interrupted++;else failures++;}
            String key=row.optString("key");present.add(key);data.put(key,row);
            TextView item=entries.get(key);
            if(item==null){item=owner.label("",12);item.setMinHeight(owner.dp(48));item.setMaxLines(4);entries.put(key,item);
                android.graphics.drawable.StateListDrawable outline=new android.graphics.drawable.StateListDrawable();
                for(boolean pressed:new boolean[]{true,false}){GradientDrawable bg=new GradientDrawable();bg.setColor(pressed?0xff17312c:0xff0d171b);bg.setCornerRadius(owner.dp(8));bg.setStroke(owner.dp(1),0xff28433d);outline.addState(pressed?new int[]{android.R.attr.state_pressed}:new int[]{},bg);}item.setBackground(outline);
                LinearLayout.LayoutParams itemLayout=new LinearLayout.LayoutParams(-1,-2);itemLayout.setMargins(owner.dp(6),owner.dp(3),owner.dp(6),owner.dp(3));item.setLayoutParams(itemLayout);
                final String id=key;item.setOnClickListener(v->{JSONObject original=data.get(id);if(original!=null)open.accept(original);});}
            String state=state(row),command=caption(row);boolean liveOutput=CodexActionTimeline.running(row)&&!row.optString("outputTail").isEmpty();String shownCommand=liveOutput?compactLine(item,command):command;
            String caption=(CodexActionTimeline.failed(row)?"! ":CodexActionTimeline.running(row)?"● ":CodexActionTimeline.status(row).equals("completed")?"✓ ":"○ ")+shownCommand+"\n    "+state;
            if(liveOutput)caption+="\n    ↳ "+compactLine(item,row.optString("outputTail"));
            if(!item.getText().toString().equals(caption))item.setText(caption);
            item.setTextColor(CodexActionTimeline.failed(row)?0xffffb08f:0xffd5e4e0);
            item.setContentDescription(command+". "+state+". "+owner.tr("Открыть действие и подробности","Open action and details"));
            if(item.getParent()==null)steps.addView(item,i);else if(steps.indexOfChild(item)!=i){steps.removeView(item);steps.addView(item,i);}
        }
        for(String key:new HashSet<>(entries.keySet()))if(!present.contains(key)){steps.removeView(entries.remove(key));data.remove(key);}
        if(active>0&&!sawRunning){sawRunning=true;if(!userToggled)expanded=true;}
        String errorText=failures==1?owner.tr(onlyShell?"Одна команда завершилась с ошибкой":"Одно действие завершилось с ошибкой",onlyShell?"One command failed":"One action failed"):failures>1?owner.tr(onlyShell?"Команд с ошибкой: ":"Действий с ошибкой: ",onlyShell?"Failed commands: ":"Failed actions: ")+failures:"";
        if(declined>0)errorText+=(errorText.isEmpty()?"":" · ")+owner.tr("Отклонено: ","Declined: ")+declined;
        if(interrupted>0)errorText+=(errorText.isEmpty()?"":" · ")+owner.tr("Прервано: ","Interrupted: ")+interrupted;
        String caption=active>0?owner.tr("Сейчас: ","Current: ")+current+(errorText.isEmpty()?"":" · "+errorText):errorText;
        if(!summary.getText().toString().equals(caption))summary.setText(caption);
        summary.setVisibility(caption.isEmpty()?GONE:VISIBLE);
        refreshHeading();
    }

    private String caption(JSONObject row){
        if(CodexActionTimeline.shell(row)&&!row.optString("skillSource").equals("read"))return CodexActionTimeline.command(row);
        return CodexActionLabels.title(row,WorkspaceLanguage.code(getContext()).equals("ru"));
    }

    private String compactLine(TextView item,String value){
        int width=owner.getWidth()>0?owner.getWidth():getResources().getDisplayMetrics().widthPixels;
        float available=Math.max(owner.dp(80),width-owner.dp(96))*100f/owner.scalePercent();
        return android.text.TextUtils.ellipsize(value.replace('\n',' ').replace('\r',' '),item.getPaint(),available,android.text.TextUtils.TruncateAt.MIDDLE).toString();
    }

    private String state(JSONObject row) {return AgentActionPresentation.state(row,WorkspaceLanguage.code(getContext()).equals("ru"));}

    private String actionWord(){
        String ru=count%100>=11&&count%100<=14?" действий":count%10==1?" действие":count%10>=2&&count%10<=4?" действия":" действий";
        return owner.tr(ru,count==1?" action":" actions");
    }

    private void refreshHeading() {
        String caption=(!skillName.isEmpty()?owner.tr("Навык: ","Skill: ")+skillName:active>0?owner.tr("В работе","Running"):owner.tr("Хронология","Timeline"))+" · "+count+actionWord()+(expanded?" ▴":" ▾");
        if(!heading.getText().toString().equals(caption))heading.setText(caption);
        LinearLayout.LayoutParams layout=(LinearLayout.LayoutParams)getLayoutParams();int width=!skillName.isEmpty()&&!expanded?LayoutParams.WRAP_CONTENT:LayoutParams.MATCH_PARENT;if(layout.width!=width){layout.width=width;setLayoutParams(layout);}
        heading.setTextColor(errors>0?0xffffb08f:0xff79dfcd);steps.setVisibility(expanded?VISIBLE:GONE);
        heading.setContentDescription(caption+". "+owner.tr(expanded?"Свернуть":"Развернуть",expanded?"Collapse":"Expand"));
    }
}
