package com.termux.app.workspace;

import android.widget.LinearLayout;
import org.json.JSONObject;

/** One live action in chronological order; its original event remains the detail source. */
final class AgentCompactActionView extends LinearLayout {
    private final AgentChatView owner;
    final CodexActionsView.ActionRow item;
    private JSONObject row=new JSONObject();
    private String title="";
    private boolean active=true;

    AgentCompactActionView(AgentChatView owner){
        super(owner.getContext());this.owner=owner;setOrientation(VERTICAL);
        item=new CodexActionsView.ActionRow(owner);item.setMinimumHeight(owner.dp(48));
        item.setBackground(WorkspaceRipple.background(getContext(),null,12));
        item.setFocusable(true);item.setOnClickListener(v->owner.openAction(row));
        addView(item,new LayoutParams(-1,-2));
        setOnClickListener(v->owner.openAction(row));
    }

    void update(JSONObject next){
        row=next;boolean ru=WorkspaceLanguage.code(getContext()).equals("ru");
        String caption=AgentTimelineSummary.title(row,ru);
        if(AgentTimelineSummary.kind(row)==AgentTimelineSummary.Kind.COMMAND){
            String command=row.optString("command");
            if(!command.isEmpty())caption=AgentTimelineSummary.shortText(commandCaption(command),300);
        }else if(AgentTimelineSummary.kind(row)==AgentTimelineSummary.Kind.READ){
            String path=AgentTimelineSummary.path(row);
            if(!path.isEmpty())caption+=" "+new java.io.File(path).getName();
        }
        String state=AgentActionPresentation.state(row,ru);
        String status=CodexActionTimeline.status(row);
        if(CodexActionTimeline.running(row))title=owner.tr("Выполняется: ","Running: ")+caption;
        else if(CodexActionTimeline.failed(row)||!status.equals("completed"))title=state.isEmpty()?caption:state+" · "+caption;
        else title="✓ "+caption;
        item.update(title,"",state,CodexActionTimeline.failed(row)&&!AgentTimelineSummary.neutral(row)?0xffffb08f:0xfff0f0ee);
        item.icon(row);refreshRunning();
        String description=caption+". "+state+". "+owner.tr("Открыть действие и подробности","Open action and details");
        setContentDescription(description);item.setContentDescription(description);
    }

    /** Display only: leave arguments and the event used by details/history untouched. */
    static String commandCaption(String command){
        int start=0;
        while(start<command.length()&&Character.isWhitespace(command.charAt(start)))start++;
        if(start==command.length())return command;
        int end=start;String executable;
        char quote=command.charAt(start);
        if(quote=='\''||quote=='"'){
            end=command.indexOf(quote,start+1);
            if(end<0)return command;
            executable=command.substring(start+1,end++);
            if(end<command.length()&&!Character.isWhitespace(command.charAt(end)))return command;
        }else{
            while(end<command.length()&&!Character.isWhitespace(command.charAt(end)))end++;
            executable=command.substring(start,end);
            for(char c:new char[]{'\'', '"', ';', '&', '|', '<', '>', '(', ')', '*', '?', '[', ']'})if(executable.indexOf(c)>=0)return command;
        }
        // Do not interpret expansions, escapes, concatenated tokens or arbitrary executables.
        if(!executable.startsWith("/")||executable.indexOf('\\')>=0||executable.indexOf('$')>=0||executable.indexOf('`')>=0)return command;
        String shell=executable.substring(executable.lastIndexOf('/')+1);
        if(!shell.equals("bash")&&!shell.equals("sh")&&!shell.equals("dash")&&!shell.equals("zsh")&&!shell.equals("ksh")&&!shell.equals("ash")&&!shell.equals("fish"))return command;
        return command.substring(0,start)+shell+command.substring(end);
    }

    void screenActive(boolean value){active=value;refreshRunning();}
    private void refreshRunning(){((AgentActionTextView)item.title).present(title,active&&CodexActionTimeline.running(row));}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refreshRunning();}
    @Override protected void onDetachedFromWindow(){
        ((AgentActionTextView)item.title).present(title,false);
        super.onDetachedFromWindow();
    }
}
