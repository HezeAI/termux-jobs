package com.termux.app.workspace;

import java.util.*;
import org.json.*;

/** Explicit web metadata only: references and opaque search results are never resolved as URLs. */
final class AgentWebActions {
 static final class Entry {
  final String text;final boolean search;
  Entry(String text,boolean search){this.text=text;this.search=search;}
  String key(String parent){try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder(64);for(byte value:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return "web:"+parent.length()+":"+parent+":"+(search?"q:":"u:")+hex;}catch(java.security.NoSuchAlgorithmException unavailable){throw new IllegalStateException(unavailable);}}
 }
 static String string(JSONObject object,String key){Object value=object.opt(key);return value instanceof String?(String)value:"";}
 static void add(List<Entry> entries,Set<String> seen,String value,boolean search){if(!value.trim().isEmpty()&&(search||explicitUrl(value))&&seen.add((search?"q:":"u:")+value))entries.add(new Entry(value,search));}
 // A compact label may retain a long URL even when the existing browser-link policy limits navigation.
 static boolean explicitUrl(String value){
  for(int i=0;i<value.length();i++)if(Character.isWhitespace(value.charAt(i))||Character.isISOControl(value.charAt(i)))return false;
  try{android.net.Uri uri=android.net.Uri.parse(value);return ("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))&&uri.getHost()!=null&&uri.getUserInfo()==null;}catch(Exception invalid){return false;}
 }
 static void array(List<Entry> entries,Set<String> seen,JSONObject object,String key,boolean search){JSONArray values=object.optJSONArray(key);if(values!=null)for(int i=0;i<values.length();i++)if(values.opt(i) instanceof String)add(entries,seen,(String)values.opt(i),search);}
 static List<Entry> entries(JSONObject row){
  List<Entry> entries=new ArrayList<>();Set<String> seen=new HashSet<>();JSONObject preview=row.optJSONObject("actionPreview");
  if(preview!=null&&preview.optString("kind").equals("web")){
   String operation=string(preview,"operation");boolean search=operation.equals("search");
   if(search){add(entries,seen,string(preview,"query"),true);array(entries,seen,preview,"queries",true);}
   else if(operation.equals("open")||operation.equals("find")){add(entries,seen,string(preview,"url"),false);array(entries,seen,preview,"urls",false);}
  }
  try{JSONObject body=new JSONObject(row.optString("body"));JSONObject action=body.optJSONObject("action");String type=action==null?"":string(action,"type");
   if(type.equals("search")||type.isEmpty()){
    if(action!=null){add(entries,seen,string(action,"query"),true);array(entries,seen,action,"queries",true);}
    if(entries.isEmpty())add(entries,seen,string(body,"query"),true);
   }else if(type.equals("openPage")||type.equals("findInPage")||type.equals("open_page")||type.equals("find_in_page"))add(entries,seen,string(action,"url"),false);
  }catch(JSONException absent){}
  if(entries.isEmpty())entries.add(new Entry("",false));return entries;
 }
}
