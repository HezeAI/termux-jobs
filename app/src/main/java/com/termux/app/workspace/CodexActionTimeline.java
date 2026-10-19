package com.termux.app.workspace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Presentation-only grouping. Original command rows remain the source for details. */
final class CodexActionTimeline {
    private CodexActionTimeline() {}

    static boolean shell(JSONObject row) {
        return row.optString("kind").equals("tool") && (row.optString("actionType").equals("shell")
            || row.optString("title").startsWith("Shell · "));
    }

    static String status(JSONObject row) {
        String value=row.optString("status");
        if(value.isEmpty())value=row.optString("title").replaceFirst("^Shell · ","");
        switch(value) {
            case "success":case "done":return "completed";
            case "executing":return "running";
            case "error":return "failed";
            case "canceled":return "cancelled";
            default:return value;
        }
    }

    static boolean running(JSONObject row) {
        String value=status(row);
        return value.equals("inProgress")||value.equals("running");
    }

    static boolean failed(JSONObject row) {
        String value=status(row);
        return value.equals("failed")||value.equals("declined")||value.equals("aborted")
            ||value.equals("cancelled")||value.equals("canceled")
            ||row.has("exitCode")&&!row.isNull("exitCode")&&row.optInt("exitCode")!=0;
    }

    static String command(JSONObject row) {
        String command=row.optString("command");
        if(command.isEmpty())command=row.optString("body").split("\n",2)[0];
        return command.length()>240?command.substring(0,240)+"…":command;
    }

    static List<JSONObject> group(List<JSONObject> rows) {return group(rows,false);}

    static List<JSONObject> group(List<JSONObject> rows,boolean allTools) {
        if(allTools)rows=AgentActionMetadata.project(rows);
        List<JSONObject> result=new ArrayList<>();
        for(int i=0;i<rows.size();) {
            JSONObject first=rows.get(i);
            String skill=first.optString("skillContextName");
            if(skill.isEmpty())skill=first.optString("skillName");
            boolean skillGroup=!skill.isEmpty()&&first.optString("kind").equals("tool");
            if(!(allTools?first.optString("kind").equals("tool"):shell(first))&&!skillGroup){result.add(first);i++;continue;}
            JSONArray steps=new JSONArray();String turn=first.optString("turn");
            while(i<rows.size()) {
                JSONObject next=rows.get(i);
                String nextSkill=next.optString("skillContextName");if(nextSkill.isEmpty())nextSkill=next.optString("skillName");
                if(skillGroup?!next.optString("kind").equals("tool")||!skill.equals(nextSkill):!(allTools?next.optString("kind").equals("tool"):shell(next))||!nextSkill.isEmpty())break;
                if(steps.length()>0&&!turn.isEmpty()&&!next.optString("turn").isEmpty()
                    &&!turn.equals(next.optString("turn")))break;
                steps.put(next);i++;
            }
            try {result.add(new JSONObject().put("key","codex-actions:"+first.optString("key"))
                .put("kind","actionGroup").put("skillName",skillGroup?skill:"").put("steps",steps));}
            catch(JSONException impossible){throw new IllegalStateException(impossible);}
        }
        return result;
    }
}
