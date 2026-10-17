package com.termux.app.workspace;

import org.json.*;
import java.util.regex.*;

/** Diagnostic evidence only. Never turns malformed model text into an action. */
final class LocalAiToolErrors {
 static String feedback(WorkspaceStore.Session p,String raw,IllegalArgumentException error)throws Exception{
  Matcher m=Pattern.compile("<function=([a-z_]+)>|\"tool\"\\s*:\\s*\"([a-z_]+)\"").matcher(raw);String name=m.find()?(m.group(1)!=null?m.group(1):m.group(2)):"";
  if(p.localProtocol.equals(LocalAiLiteRt.NATIVE)&&LocalAiLiteRt.leakedCall(raw))name=LocalAiLiteRt.leakedName(raw);
  JSONArray tools=p.localProtocol.equals(LocalAiLiteRt.NATIVE)?LocalAiLiteRt.schemas(p):LocalAiQwenProtocol.tools(p),names=new JSONArray();JSONObject selected=null;
  for(int i=0;i<tools.length();i++){JSONObject f=tools.getJSONObject(i).getJSONObject("function");names.put(f.getString("name"));if(f.getString("name").equals(name))selected=f;}
  boolean nativeFormat=LocalAiQwenProtocol.enabled(p);
  String schema=p.localProtocol.equals(LocalAiLiteRt.NATIVE)?"Use exactly one native SDK structured function call with corrected arguments. No tool JSON or Qwen tags. If the previous native call was unparsed, regenerate a concise COMPLETE call; do not continue its raw fragment or claim it ran. For a document use write; discover an unknown destination with locations instead of guessing a path. A leaked call does not prove a token limit. ":nativeFormat?"Emit exactly ONE complete <tool_call><function=NAME><parameter=NAME>value</parameter></function></tool_call> block. No surrounding prose, fences or multiple calls. Boolean true/false, decimal integers, variants JSON array.":"Emit exactly one complete tool JSON with string tool field. No surrounding prose. ";
  schema+=" Selected tool: "+name+". "+(selected==null?"Available tools: "+names:"Function schema: "+selected)+". Preserve task/path. Do not invent tools, duplicate parameters, change permissions or claim a rejected call executed. Correct only this call.";
  return new JSONObject().put("status","TOOL_VALIDATION_ERROR").put("executed",false).put("toolName",name).put("error",error.getMessage()==null?"Invalid arguments":error.getMessage()).put("originalCall",raw.length()>131072?raw.substring(0,131072):raw).put("originalTruncated",raw.length()>131072).put("schema",schema).toString();
 }
 static String forModel(String feedback)throws Exception{JSONObject r=new JSONObject(feedback);r.remove("originalCall");r.remove("originalTruncated");return r.toString();}
}
