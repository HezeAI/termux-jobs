package com.termux.app.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

/** Bounded presentation of supplied events only; no IO, execution or model operations. */
final class AgentTimelineSummary {
    enum Kind { READ, EDIT, COMMAND, OTHER }
    static Kind kind(JSONObject row){
        if(CodexActionTimeline.shell(row)||row.optString("toolType").equals("commandExecution"))return Kind.COMMAND;
        String name=row.optString("toolName");if(name.isEmpty())name=row.optString("toolType");
        if(Arrays.asList("Read","read","read_file","read_many_files").contains(name))return Kind.READ;
        if(row.optString("toolType").equals("fileChange")||Arrays.asList("Write","write","write_file","Edit","edit","MultiEdit","replace","apply_patch").contains(name))return Kind.EDIT;
        if(Arrays.asList("Bash","bash","run_shell_command","exec_command").contains(name))return Kind.COMMAND;
        return Kind.OTHER;
    }
    static String type(JSONObject row,boolean ru){switch(kind(row)){
        case READ:return ru?"Чтение":"Read";
        case EDIT:return ru?"Правка":"Edit";
        case COMMAND:return ru?"Команда":"Command";
        default:return knownOther(row)?CodexActionLabels.title(row,ru):(ru?"Тип не определён":"Type unspecified");
    }}
    static String counts(JSONArray rows,boolean ru){int[] n=new int[4];for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null)n[kind(row).ordinal()]++;}
        return (ru?"Чтение: ":"Reads: ")+n[0]+(ru?" · Правки: ":" · Edits: ")+n[1]+(ru?" · Команды: ":" · Commands: ")+n[2]+(n[3]>0?(ru?" · Другие: ":" · Other: ")+n[3]:"");
    }
    static String path(JSONObject row){String p=row.optString("toolPath");JSONObject preview=AgentActionPresentation.preview(row);if(p.isEmpty()&&preview!=null){
        if(preview.optString("kind").equals("fileTool"))p=preview.optString("path");
        if(preview.optString("kind").equals("image")&&preview.optJSONObject("image")!=null)p=preview.optJSONObject("image").optString("path");
        JSONArray files=preview.optJSONArray("files");if(p.isEmpty()&&files!=null&&files.optJSONObject(0)!=null)p=files.optJSONObject(0).optString("path");
    }return p;}
    static String title(JSONObject row,boolean ru){
        String description=row.optString("actionDescription").trim();if(!description.isEmpty())return shortText(description,120);
        if(kind(row)==Kind.COMMAND)return AgentCommandTitle.title(row,ru);
        if(kind(row)==Kind.READ)return ru?"Чтение файла":"Reading file";
        String p=path(row);if(!p.isEmpty()){String name=p.substring(p.lastIndexOf('/')+1);if(!name.isEmpty()){JSONObject preview=AgentActionPresentation.preview(row);int n=preview==null?0:preview.optInt("count");return shortText(name,200)+(n>1?(ru?" · ещё ":" · more: ")+(n-1):"");}}
        if(kind(row)==Kind.OTHER&&!knownOther(row)){String nativeName=row.optString("toolName");if(nativeName.isEmpty())nativeName=row.optString("title");return shortText(nativeName.isEmpty()?(ru?"Действие":"Action"):nativeName,200);}
        String friendly=CodexActionLabels.title(row,ru);return shortText(friendly.isEmpty()?(ru?"Действие":"Action"):friendly,200);
    }
    static boolean knownOther(JSONObject row){String name=row.optString("toolName");if(name.isEmpty())name=row.optString("toolType");if(name.isEmpty())name=row.optString("title");return Arrays.asList("imageView","image_view","view_image","imageGeneration","image_generation","WebSearch","webSearch","web_search","google_web_search","WebFetch","webfetch","web_fetch","Glob","glob","glob_files","Grep","grep","grep_search","search_file_content","skill").contains(name);}
    static String shortText(String text,int max){String flat=text.replace('\r',' ').replace('\n',' ');int end=Math.min(max,flat.length());if(end>0&&end<flat.length()&&Character.isHighSurrogate(flat.charAt(end-1)))end--;return end<flat.length()?flat.substring(0,end)+"…":flat;}
    static boolean cancelled(JSONObject row){return Arrays.asList("cancelled","canceled").contains(CodexActionTimeline.status(row));}
    static boolean neutral(JSONObject row){return AgentActionPresentation.userDeclined(row)||cancelled(row);}
    static long start(JSONObject row){Object v=row.opt("startedAt");return v instanceof Number&&((Number)v).longValue()>0?((Number)v).longValue():0;}
}
