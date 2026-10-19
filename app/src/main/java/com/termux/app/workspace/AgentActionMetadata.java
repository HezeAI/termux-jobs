package com.termux.app.workspace;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Presentation only. Never reads files or executes the command being described. */
final class AgentActionMetadata {
    private AgentActionMetadata() {}
    static String readSkill(JSONObject row) {
        if(!CodexActionTimeline.status(row).equals("completed")||CodexActionTimeline.failed(row))return "";
        String tool=row.optString("toolName"),path=row.optString("toolPath");
        if(!(tool.equals("Read")||tool.equals("read")||tool.equals("read_file")))return "";
        if(!path.startsWith("/")||!path.endsWith("/SKILL.md")||path.contains("/../")||path.contains("/./"))return "";
        String parent=path.substring(0,path.length()-9);int slash=parent.lastIndexOf('/');
        String name=parent.substring(slash+1);
        return name.matches("[\\p{L}\\p{N}_.:-]{1,100}")?name:"";
    }
    static List<JSONObject> project(List<JSONObject> rows) {
        List<JSONObject> result=new ArrayList<>();String skill="",turn="";
        for(JSONObject row:rows) {
            String kind=row.optString("kind"),nextTurn=row.optString("turn");
            if(kind.equals("user")||!nextTurn.isEmpty()&&!turn.isEmpty()&&!turn.equals(nextTurn))skill="";
            if(!nextTurn.isEmpty())turn=nextTurn;
            String read=kind.equals("tool")?readSkill(row):"";
            // An unsuccessful explicit instruction read must not carry the previous skill forward.
            if(kind.equals("tool")&&row.optString("toolPath").endsWith("/SKILL.md"))skill=read;
            if(!skill.isEmpty()&&kind.equals("tool")) {
                JSONObject copy=new JSONObject();
                try {
                    for(Iterator<String> keys=row.keys();keys.hasNext();) {String key=keys.next();copy.put(key,row.get(key));}
                    copy.put("skillContextName",skill);
                    if(!read.isEmpty())copy.put("skillName",skill).put("skillSource","read");
                } catch(JSONException impossible){throw new IllegalStateException(impossible);}
                result.add(copy);
            } else result.add(row);
        }
        return result;
    }
}
