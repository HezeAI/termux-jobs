package com.termux.app.workspace;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Locale;

/** Environment-scoped history, with optional project filtering on the client. */
final class CodexChatCatalog {
    static final class Entry {
        final String id,name,cwd;final long updated;
        Entry(String id,String name,String cwd,long updated){this.id=id;this.name=name;this.cwd=cwd;this.updated=updated;}
    }
    static JSONObject params(String cursor) throws Exception {
        JSONObject result=new JSONObject().put("limit",50).put("sortKey","updated_at").put("archived",false).put("modelProviders",new JSONArray());
        if(cursor!=null&&!cursor.isEmpty())result.put("cursor",cursor);return result;
    }
    static Entry parse(JSONObject chat) {
        String id=chat.optString("id");if(!CodexSessionShell.validThread(id))return null;
        String name=chat.optString("name","");if(name.isEmpty()||name.equals("null"))name=chat.optString("preview",WorkspaceText.tr("Чат Codex"));
        if(name.length()>160)name=name.substring(0,160)+"…";
        return new Entry(id,name,chat.optString("cwd",""),chat.optLong("updatedAt"));
    }
    static boolean visible(Entry chat,String environment,String directory,boolean projectOnly,String query) {
        if(projectOnly&&(chat.cwd.isEmpty()||!CodexProjects.matches(environment,directory,chat.cwd)))return false;
        return (chat.name+" "+chat.cwd).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }
}
