package com.termux.app.workspace;

import org.json.*;
import java.util.*;

/** Opt-in Qwen3.5 text/tool protocol adapter. Does not grant permissions or evaluate arbitrary Jinja. */
final class LocalAiQwenProtocol {
    static final String JSON="json",QWEN="qwen-native";
    static void validate(String value){if(!JSON.equals(value)&&!QWEN.equals(value)&&!LocalAiLiteRt.NATIVE.equals(value))throw new IllegalArgumentException("Invalid model communication format");}
    static boolean supported(String model,String engine){return "qwen35-2b".equals(model)&&"llama".equals(engine);}
    static boolean enabled(WorkspaceStore.Session s){validate(s.localProtocol);return QWEN.equals(s.localProtocol);}
    static String settings(WorkspaceStore.Session s){return LocalAiPrompts.compactSettings(s);}
    static JSONObject field(String type)throws JSONException{return new JSONObject().put("type",type);}
    static JSONObject enumeration(String... values)throws JSONException{return field("string").put("enum",new JSONArray(Arrays.asList(values)));}
    static JSONArray tools(WorkspaceStore.Session s)throws Exception{
        JSONArray out=new JSONArray();if(s.localAccess.equals("chat"))return out;
        for(String name:new String[]{"list","read","search","locations","screenshots","history","view_image","view_video","inspect_apk","write","command","install"}){
            if(name.matches("write|command")&&!s.localAccess.equals("write"))continue;
            if(name.equals("command")&&s.localCheckPaths||name.equals("install")&&!s.localConfirmActions)continue;
            JSONObject properties=new JSONObject();JSONArray required=new JSONArray();
            if(name.equals("command")){properties.put("command",field("string"));required.put("command");properties.put("environment",s.localEnvironment.equals("termux")?enumeration("termux"):s.localTermuxCommands?enumeration("ubuntu","termux"):enumeration("ubuntu"));}
            else if(name.equals("install")){properties.put("package",enumeration("coreutils","findutils","file","unzip","zip","binutils","aapt","apksigner","python","python3","python3-pip","python3-venv","git","make","cmake","clang","build-essential","pkg-config","curl","wget"));required.put("package");}
            else if(!name.matches("locations|screenshots|history")){properties.put("path",field("string"));required.put("path");}
            if(name.equals("write")){properties.put("text",field("string"));required.put("text");}
            if(name.equals("read"))properties.put("refresh",field("boolean"));
            if(name.matches("list|search")){properties.put("filter",enumeration("all","text","image","video","apk")).put("extension",field("string").put("pattern","^[A-Za-z0-9]{1,12}$")).put("sort",enumeration("modified_desc","name")).put("recursive",field("boolean")).put("query",field("string")).put("limit",field("integer").put("minimum",1).put("maximum",200));}
            if(name.equals("search"))properties.put("variants",field("array").put("items",field("string")).put("maxItems",8));
            if(name.matches("screenshots|history"))properties.put("query",field("string")).put("limit",field("integer").put("minimum",1).put("maximum",name.equals("history")?10:200));
            if(name.equals("screenshots"))properties.put("path",field("string"));
            if(name.equals("history"))properties.put("start",field("integer").put("minimum",0));
            if(name.equals("write"))properties.getJSONObject("text").put("description","Complete UTF-8 document contents to SAVE; not commands to execute.");
            if(name.equals("command"))properties.getJSONObject("command").put("description","Bash script to EXECUTE. To save instructions/documentation instead, use write.");
            String description=LocalAiLiteRt.description(name);
            out.put(new JSONObject().put("type","function").put("function",new JSONObject().put("name",name).put("description",description).put("parameters",new JSONObject().put("type","object").put("properties",properties).put("required",required).put("additionalProperties",false))));
        }return out;
    }
    static String instructions(WorkspaceStore.Session s)throws Exception{
        JSONArray tools=tools(s);StringBuilder out=new StringBuilder();
        if(tools.length()>0){out.append("# Tools\n<tools>");for(int i=0;i<tools.length();i++)out.append('\n').append(tools.getJSONObject(i));out.append("\n</tools>\n\nFunction call format (one call; optional brief explanation before it):\n\n<tool_call>\n<function=example_function_name>\n<parameter=example_parameter_1>\nvalue_1\n</parameter>\n</function>\n</tool_call>\n\n");}
        return out.append((s.localToolsOnly||s.localBehaviorMode.equals("brief"))?LocalAiPrompts.compactSettings(s):settings(s)).append("\nProtocol: Qwen native tools; overrides old JSON examples. Emit one complete tool_call/function block; optional brief explanation before it, never after it; no JSON call envelope. Boolean=true/false; integers decimal; variants=JSON array. Normal answers are text, never tool-call examples. Continue after tool_response; never claim unexecuted success. Prefer starting directly with <tool_call>; keep lengthy explanations for the final answer after results.\n").toString();
    }
    static String call(JSONObject request)throws Exception{
        validated(request);StringBuilder out=new StringBuilder("<tool_call>\n<function=").append(request.getString("tool")).append(">\n");
        Iterator<String> keys=request.keys();while(keys.hasNext()){String key=keys.next();if(key.equals("tool"))continue;Object value=request.get(key);out.append("<parameter=").append(key).append(">\n").append(value).append("\n</parameter>\n");}return out.append("</function>\n</tool_call>").toString();
    }
    static JSONObject request(String raw)throws Exception{try{return parse(raw);}catch(JSONException e){throw new IllegalArgumentException("Invalid native parameter encoding",e);}}
    private static JSONObject parse(String raw)throws Exception{
        String answer=raw.trim();if(answer.startsWith("<think>")){int end=answer.indexOf("</think>");if(end<0)return null;answer=answer.substring(end+8).trim();}if(!answer.contains("<tool_call>"))return null;
        // The installed Qwen template explicitly permits natural-language reasoning BEFORE a call.
        // Accept only a short plain prefix and a single terminal block; never unwrap examples/fences.
        int start=answer.indexOf("<tool_call>");String prefix=answer.substring(0,start).trim();
        if(!prefix.isEmpty()){
            if(prefix.length()>4096||(prefix.contains("```")||prefix.contains("~~~"))||prefix.contains("<")||prefix.contains(">")||prefix.endsWith(":")||prefix.matches("(?isu).*\\b(example|examples|пример|примеры|образец)\\b.*"))throw new IllegalArgumentException("Quoted/example native calls are not executable");
            answer=answer.substring(start);
        }
        if(raw.length()>131072||!answer.startsWith("<tool_call>")||!answer.endsWith("</tool_call>"))throw new IllegalArgumentException("Native call must be one complete block without prose or code fences; expected <tool_call> ... </tool_call>");
        Cursor c=new Cursor(answer);c.expect("<tool_call>");c.space();c.expect("<function=");String name=c.until(">");if(!name.matches("[a-z_]{1,32}"))throw new IllegalArgumentException("Invalid function name");JSONObject out=new JSONObject().put("tool",name);c.space();Set<String> seen=new HashSet<>();
        while(c.starts("<parameter=")){
            c.expect("<parameter=");String key=c.until(">");if(!key.matches("[a-zA-Z_]{1,32}")||key.equals("tool"))throw new IllegalArgumentException("Invalid parameter name: "+key+" in function "+name);if(!seen.add(key))throw new IllegalArgumentException("Duplicate parameter: "+key+" in function "+name);String value=c.until("</parameter>");if(value.startsWith("\r\n"))value=value.substring(2);else if(value.startsWith("\n"))value=value.substring(1);if(value.endsWith("\r\n"))value=value.substring(0,value.length()-2);else if(value.endsWith("\n"))value=value.substring(0,value.length()-1);
            if(value.contains("<tool_call>")||value.contains("</tool_call>")||value.contains("<function=")||value.contains("</function>")||value.contains("<parameter="))throw new IllegalArgumentException("Nested protocol tags are not allowed in arguments");
            if(key.equals("limit")||key.equals("start")){if(!value.trim().matches("[0-9]{1,9}"))throw new IllegalArgumentException("Integer parameter required: "+key);out.put(key,Integer.parseInt(value.trim()));}
            else if(key.equals("recursive")||key.equals("refresh")){if(!value.trim().matches("true|false"))throw new IllegalArgumentException("Boolean parameter required: "+key);out.put(key,Boolean.parseBoolean(value.trim()));}
            else if(key.equals("variants"))out.put(key,new JSONArray(value));else out.put(key,value);c.space();
        }
        c.expect("</function>");c.space();c.expect("</tool_call>");c.space();if(c.at!=answer.length())throw new IllegalArgumentException("Multiple/trailing native calls are disabled");return validated(out);
    }
    static JSONObject validated(JSONObject request)throws Exception{return LocalAiTools.request(request.toString().replace("<","\\u003c").replace(">","\\u003e"));}
    static final class Cursor{final String s;int at;Cursor(String s){this.s=s;}boolean starts(String token){return s.startsWith(token,at);}void expect(String token){if(!starts(token))throw new IllegalArgumentException("Invalid/incomplete native tool call");at+=token.length();}String until(String token){int end=s.indexOf(token,at);if(end<0)throw new IllegalArgumentException("Incomplete native tool call");String v=s.substring(at,end);at=end+token.length();return v;}void space(){while(at<s.length()&&Character.isWhitespace(s.charAt(at)))at++;}}
    static JSONArray render(JSONArray input)throws Exception{
        JSONArray out=new JSONArray();boolean lastTool=false;for(int i=0;i<input.length();i++){JSONObject m=input.getJSONObject(i);String role=m.getString("role"),text=m.getString("text");boolean tool=m.has("toolResult");
            if(tool){text="<tool_response>\n"+text.replace("<tool_response>","&lt;tool_response&gt;").replace("</tool_response>","&lt;/tool_response&gt;")+"\n</tool_response>";if(lastTool){JSONObject prev=out.getJSONObject(out.length()-1);prev.put("text",prev.getString("text")+"\n"+text);continue;}role="user";}
            else if(role.equals("assistant")){JSONObject r=m.optJSONObject("toolRequest");if(r!=null&&!r.optString("tool").matches("validation|incomplete"))text=call(r);else text=LocalAiText.split(text).answer;}
            JSONObject copy=new JSONObject(m.toString()).put("role",role).put("text",text);out.put(copy);lastTool=tool;
        }return out;
    }
}
