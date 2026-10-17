package com.termux.app.workspace;

import org.json.*;

/** Runtime copy only. Never mutates or replaces the saved transcript or system permissions. */
final class LocalAiContext {
    interface Counter {int count(JSONArray messages,String system)throws Exception;}
    interface Notice {void state(String state);}
    // Only prior tool results, never user requests/current-turn results or the saved transcript.
    static String previousResult(String text,String path){
        if(text.length()<=256)return text;
        return text.substring(0,128)+"\n[Earlier tool result excerpt; use history for full original; read for current contents. "+(path.isEmpty()?"":"Source: "+path)+"]\n"+text.substring(text.length()-48);
    }
    static boolean tool(JSONObject m){return m.has("toolResult");}
    static int estimate(JSONArray messages,String system){
        double tokens=weight(system)+32;
        for(int i=0;i<messages.length();i++){JSONObject m=messages.optJSONObject(i);tokens+=weight(m.optString("text"))+20;JSONArray media=m.optJSONArray("media");if(media!=null)tokens+=media.length()*384;}
        return (int)Math.ceil(tokens);
    }
    static JSONArray suffix(JSONArray source,int start){JSONArray out=new JSONArray();for(int i=start;i<source.length();i++)out.put(source.opt(i));return out;}
    static double weight(String text){double result=0;for(int i=0;i<text.length();i++){char c=text.charAt(i);result+=c<128?0.4:1.25;}return result;}
    static JSONArray fit(JSONArray original,String system,int capacity,int reserve,Counter counter,Notice notice)throws Exception{
        JSONArray work=new JSONArray(original.toString());int budget=capacity-reserve-64;if(budget<=0)throw new IllegalArgumentException("Invalid context budget");
        boolean changed=false;
        if(counter.count(work,system)<=budget)return work;
        notice.state("begin");changed=true;
        for(int i=0;i<work.length();i++){JSONObject m=work.getJSONObject(i);if(m.optString("role").equals("assistant")){String answer=LocalAiText.split(m.optString("text")).answer;if(!answer.equals(m.optString("text")))m.put("text",answer.isEmpty()?"[Earlier reasoning omitted]":answer);}}
        java.util.List<Integer> turns=new java.util.ArrayList<>();for(int i=0;i<work.length();i++)if(work.getJSONObject(i).optString("role").equals("user")&&!tool(work.getJSONObject(i)))turns.add(i);
        // Binary search keeps as many recent complete turns as fit, without quadratic tokenization.
        if(counter.count(work,system)>budget&&turns.size()>1){int low=0,high=turns.size()-1;while(low<high){int mid=(low+high)/2;if(counter.count(suffix(work,turns.get(mid)),system)<=budget)high=mid;else low=mid+1;}work=suffix(work,turns.get(low));}
        if(turns.size()==1&&turns.get(0)>0)work=suffix(work,turns.get(0));
        // Current task plus latest tool request/result are protected from wholesale removal.
        while(counter.count(work,system)>budget&&work.length()>3){work.remove(1);}
        // Keep at least the latest image; omit older frames explicitly instead of
        // rejecting every multi-frame request just because the estimate is large.
        while(counter.count(work,system)>budget){
            int last=-1;for(int i=0;i<work.length();i++){JSONArray images=work.getJSONObject(i).optJSONArray("media");if(images!=null&&images.length()>0)last=i;}
            boolean removed=false;
            for(int i=0;i<work.length();i++){JSONObject m=work.getJSONObject(i);JSONArray images=m.optJSONArray("media");if(images==null||images.length()==0)continue;
                if(i!=last||images.length()>1){if(!m.has("contextMediaBase"))m.put("contextMediaBase",m.optString("text"));if(i!=last)m.remove("media");else images.remove(0);int kept=m.optJSONArray("media")==null?0:m.getJSONArray("media").length();m.put("text",m.getString("contextMediaBase")+"\n[Context excerpt: "+kept+" images/frames attached from this message; others omitted. Do not claim to see omitted images.]");removed=true;break;}
            }
            if(!removed)break;
        }
        // Large tool output becomes a clearly marked excerpt; do not cut the user's task.
        while(counter.count(work,system)>budget){
            int largest=-1,length=256;for(int i=0;i<work.length();i++){JSONObject m=work.getJSONObject(i);if(tool(m)&&m.optString("text").length()>length){largest=i;length=m.getString("text").length();}}
            if(largest<0){notice.state("failed");throw new IllegalStateException("CONTEXT_TOO_SMALL: current request and instructions exceed this runtime context. Shorten the current request; saved history is kept.");}
            JSONObject m=work.getJSONObject(largest);String text=m.getString("text");int keep=Math.max(64,(text.length()-100)/2);
            m.put("text",text.substring(0,keep)+"\n[Context excerpt: middle omitted. Full tool result remains in saved history.]\n"+text.substring(text.length()-Math.min(64,text.length())));
        }
        if(changed)notice.state("done");return work;
    }
}
