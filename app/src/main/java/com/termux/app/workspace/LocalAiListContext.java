package com.termux.app.workspace;
import org.json.*;
/** Compact presentation for inference only; no filtering, reordering, access or selection changes. */
final class LocalAiListContext {
 static String compact(JSONObject request,String result){
  if(!request.optString("tool").matches("list|search|screenshots"))return result;
  JSONObject raw=LocalAiSearchResult.parse(result);if(raw==null)return result;JSONArray entries=raw.optJSONArray("entries");if(entries==null)return result;
  try{JSONObject out=new JSONObject();for(String k:new String[]{"status","checkedPath","selectedPath","checkedLocations","sort","filter","queryVariants","matched","truncated"})if(raw.has(k))out.put(k,raw.get(k));
   JSONArray rows=new JSONArray();for(int i=0;i<entries.length();i++){JSONObject e=entries.getJSONObject(i);if(!(e.opt("path") instanceof String)||!(e.opt("type") instanceof String))return result;JSONArray row=new JSONArray().put(e.getString("path")).put(e.getString("type")).put(e.opt("size")).put(e.opt("modifiedMillis")).put(e.optString("match"));rows.put(row);}
   out.put("columns",new JSONArray().put("path").put("type").put("sizeBytes").put("modifiedMillis").put("match")).put("rows",rows).put("notice","Metadata only for checked locations; null means unknown. Use read for file contents. No new rights granted; unselected candidates are not approved.");return "exit=0\n"+out;
  }catch(JSONException e){return result;}
 }
 static String preview(JSONObject request,String result){String compact=compact(request,result);if(!compact.equals(result))return compact;return result.length()>8000?result.substring(0,8000)+"\n[Context preview truncated; full result is saved in chat history]":result;}
}
