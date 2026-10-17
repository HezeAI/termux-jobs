package com.termux.app.workspace;
import org.json.*;
/** Reserve adaptation never edits the saved transcript or the current user request. */
final class LocalAiReplyBudget {
    static final class Prepared {final JSONArray messages;final int answer,input;Prepared(JSONArray m,int a,int n){messages=m;answer=a;input=n;}}
    static Prepared prepare(JSONArray original,String system,int capacity,int requested,LocalAiContext.Counter counter,LocalAiContext.Notice notice)throws Exception{
        try{JSONArray fitted=LocalAiContext.fit(original,system,capacity,requested,counter,s->{});int input=counter.count(fitted,system);if(!LocalAiContext.same(fitted,original)){notice.state("begin");notice.state("done");}return new Prepared(fitted,requested,input);}
        catch(IllegalStateException error){if(error.getMessage()==null||!error.getMessage().startsWith("CONTEXT_TOO_SMALL"))throw error;}
        JSONArray fitted;
        try{fitted=LocalAiContext.fit(original,system,capacity,128,counter,s->{});}catch(IllegalStateException error){notice.state("failed");throw error;}
        int input=counter.count(fitted,system),answer=Math.min(requested,capacity-64-input);
        if(answer<128){notice.state("failed");throw new IllegalStateException("CONTEXT_TOO_SMALL: current request and instructions leave no usable response space. Saved history is kept.");}
        if(!LocalAiContext.same(fitted,original)){notice.state("begin");notice.state("done");}
        return new Prepared(fitted,answer,input);
    }
    static int media(JSONArray messages,String kind)throws JSONException{int count=0;for(int i=0;i<messages.length();i++){JSONArray media=messages.getJSONObject(i).optJSONArray("media");if(media!=null)for(int j=0;j<media.length();j++){JSONObject item=media.getJSONObject(j);if(kind.equals("audio")?item.optString("kind").equals("audio"):!item.optString("kind").equals("audio"))count++;}}return count;}
}
