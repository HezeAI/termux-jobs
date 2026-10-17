package com.termux.app.workspace;
import org.json.*;

/** Only native tool output supplies selectable paths. A selection never grants action permission. */
final class LocalAiSearchResult {
    static JSONObject parse(String result){try{return result.startsWith("exit=0\n")?new JSONObject(result.substring(7)):null;}catch(Exception ignored){return null;}}
    static JSONArray candidates(JSONObject request,String result){
        JSONArray out=new JSONArray();boolean search=request.optString("tool").equals("search"),namedList=request.optString("tool").equals("list")&&!request.optString("query").trim().isEmpty();if(!search&&!namedList||search&&!request.has("query")&&request.optString("path").isEmpty())return out;
        JSONObject data=parse(result);if(data==null)return out;if(request.has("query")&&request.optString("query").trim().isEmpty()&&(request.optJSONArray("variants")==null||request.optJSONArray("variants").length()==0))return out;JSONArray entries=data.optJSONArray("entries");if(entries==null)return out;
        for(int i=0;i<entries.length()&&out.length()<200;i++){JSONObject f=entries.optJSONObject(i);if(f!=null&&f.optString("type").equals("file")&&f.optString("path").startsWith("/")&&f.optString("path").indexOf(0)<0)out.put(f);}
        return out;
    }
    static boolean needsChoice(JSONArray files){if(files.length()>1)return true;return files.length()==1&&files.optJSONObject(0).optString("match").equals("name_variant");}
    static String selected(JSONArray candidates,String path)throws Exception{
        for(int i=0;i<candidates.length();i++){JSONObject f=candidates.getJSONObject(i);if(f.getString("path").equals(path))return "exit=0\n"+new JSONObject().put("status","USER_SELECTED_FILE").put("selectedPath",path).put("entries",new JSONArray().put(f)).put("notice","User selected this path for the original task. Selection does not approve installation, execution, edits or broader access. Other candidates were not selected.");}
        throw new SecurityException("Selected file is not a search candidate");
    }
}
