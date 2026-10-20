package com.termux.app.workspace;

import org.json.JSONObject;
import java.util.*;

/** Bounded presentation cache only. Native history, inference and live permissions are not cached here. */
final class AgentChatCache {
    static final int MAX_ENTRIES=8, MAX_ENTRY_CHARS=512*1024, MAX_TOTAL_CHARS=2*1024*1024;
    static final class State {
        final LinkedHashMap<String,JSONObject> rows=new LinkedHashMap<>();
        String older="",model="",session="",notice="";
        boolean trimmed,historyExhausted,nativeChat,resolved,busy,directShell,needsTerminal,connected,failed;
        AgentChatView.Position position;
        private int chars;
    }
    private final LinkedHashMap<String,State> entries=new LinkedHashMap<>();
    private int chars;
    void put(String owner,State state){
        if(owner.isEmpty())return;take(owner);
        List<Map.Entry<String,JSONObject>> list=new ArrayList<>(state.rows.entrySet());state.rows.clear();int size=0;
        LinkedHashMap<String,JSONObject> reverse=new LinkedHashMap<>();
        for(int i=list.size()-1;i>=0;i--){Map.Entry<String,JSONObject> row=list.get(i);int n=row.getKey().length()+row.getValue().toString().length();if(size+n>MAX_ENTRY_CHARS)break;reverse.put(row.getKey(),row.getValue());size+=n;}
        List<String> keys=new ArrayList<>(reverse.keySet());for(int i=keys.size()-1;i>=0;i--){String key=keys.get(i);state.rows.put(key,reverse.get(key));}
        state.trimmed=state.rows.size()<list.size();
        state.chars=size+state.older.length()+state.model.length()+state.session.length()+state.notice.length();
        if(state.chars>MAX_TOTAL_CHARS)return;entries.put(owner,state);chars+=state.chars;
        while(entries.size()>MAX_ENTRIES||chars>MAX_TOTAL_CHARS)take(entries.keySet().iterator().next());
    }
    State take(String owner){State s=entries.remove(owner);if(s!=null)chars-=s.chars;return s;}
    void clear(){entries.clear();chars=0;}
    int size(){return entries.size();}
    int characters(){return chars;}
}
