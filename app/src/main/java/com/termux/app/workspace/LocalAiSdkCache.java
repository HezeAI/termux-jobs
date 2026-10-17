package com.termux.app.workspace;
import org.json.*;
/** One owned SDK conversation. Exact canonical append only; actual fitted SDK history and feedback are fully budgeted. */
final class LocalAiSdkCache {
 String key="";JSONArray expected=new JSONArray(),runtime=new JSONArray();volatile boolean invalidated;
 void clear(){key="";expected=new JSONArray();runtime=new JSONArray();invalidated=false;}
 static JSONArray source(JSONArray messages)throws JSONException{JSONArray out=new JSONArray(messages.toString());for(int i=0;i<out.length();i++){out.getJSONObject(i).remove("cacheBaseText");out.getJSONObject(i).remove("cacheTransient");}return out;}
 static JSONObject semantic(JSONObject m)throws Exception{
  JSONObject out=new JSONObject().put("role",m.getString("role"));JSONObject call=m.optJSONObject("toolRequest");if(call!=null)out.put("call",LocalAiTurnProgress.call(call));else out.put("text",m.optString("cacheBaseText",m.optString("text")));
  if(m.has("toolResult"))out.put("toolResult",m.get("toolResult"));if(m.has("media"))out.put("media",m.get("media"));if(m.optBoolean("appFeedback"))out.put("appFeedback",true);return out;
 }
 static boolean same(JSONArray a,JSONArray b)throws Exception{if(a.length()!=b.length())return false;for(int i=0;i<a.length();i++)if(!LocalAiContext.same(semantic(a.getJSONObject(i)),semantic(b.getJSONObject(i))))return false;return true;}
 /** Fixed metadata codes only; never return scope IDs, settings or history text. */
 String miss(JSONArray source,String candidate)throws Exception{
  if(invalidated)return "cancelled";if(candidate.isEmpty())return "scope_unavailable";
  if(!key.equals(candidate)){
   if(key.isEmpty())return "no_conversation";
   try{JSONArray old=new JSONArray(key),next=new JSONArray(candidate);String[] codes={"scope_changed","instructions_changed","tools_changed","protocol_changed","reply_limit_changed","sampler_changed","sampler_changed"};for(int i=0;i<codes.length;i++)if(!LocalAiContext.same(old.get(i),next.get(i)))return codes[i];}catch(Exception ignored){}
   return "settings_changed";
  }
  for(int i=0;i<source.length();i++)if(source.getJSONObject(i).has("media"))return "media";
  if(source.length()!=expected.length()+1)return "history_length_changed";
  for(int i=0;i<expected.length();i++)if(!LocalAiContext.same(semantic(source.getJSONObject(i)),semantic(expected.getJSONObject(i))))return "history_changed";
  return "reused";
 }
 boolean appendable(JSONArray source,String candidate)throws Exception{return miss(source,candidate).equals("reused");}
 JSONArray expand(JSONArray source)throws Exception{JSONArray out=new JSONArray(runtime.toString());out.put(new JSONObject(source.getJSONObject(source.length()-1).toString()));return out;}
 boolean runtimePrefix(JSONArray source)throws Exception{if(source.length()!=runtime.length()+1)return false;for(int i=0;i<runtime.length();i++)if(!LocalAiContext.same(source.getJSONObject(i),runtime.getJSONObject(i)))return false;return true;}
 void keep(String candidate,JSONArray source,JSONArray actual,String text,String finish)throws Exception{
  if(candidate.isEmpty()||invalidated||!finish.matches("sdk_done|native_tool")){clear();return;}
  JSONObject answer=new JSONObject().put("role","assistant").put("text",text);if(finish.equals("native_tool"))answer.put("toolRequest",new JSONObject(text));
  key=candidate;expected=new JSONArray(source.toString()).put(new JSONObject(answer.toString()));runtime=new JSONArray(actual.toString()).put(new JSONObject(answer.toString()));
 }
}
