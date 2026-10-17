package com.termux.app.workspace;

import org.json.*;
import java.lang.reflect.*;
import java.util.*;

/** Pinned Gemma SDK bridge. Never executes tools; the application coordinator authorizes every call. */
final class LocalAiLiteRt {
    static final String NATIVE="litert-native";
    static boolean supported(JSONObject model){return model!=null&&"litert".equals(model.optString("engine"));}
    static final String PKG="com.google.ai.edge.litertlm.";
    static boolean enabled(JSONObject model){return LocalAiNpu.verifiedModel(model);}
    static JSONArray schemas(WorkspaceStore.Session s)throws Exception{
        JSONArray out=LocalAiQwenProtocol.tools(s);
        for(int i=0;i<out.length();i++){
            JSONObject f=out.getJSONObject(i).getJSONObject("function");String name=f.getString("name");f.put("description",description(name));if(name.equals("command"))f.getJSONObject("parameters").getJSONObject("properties").getJSONObject("environment").put("description","Omit for selected container. ubuntu means jobs-local-ai; internal proot IDs are invalid.");
        }
        if(!s.localAccess.equals("chat"))out.put(new JSONObject().put("type","function").put("function",new JSONObject().put("name","listen_audio").put("description",description("listen_audio")).put("parameters",new JSONObject().put("type","object").put("properties",new JSONObject().put("path",new JSONObject().put("type","string"))).put("required",new JSONArray().put("path")).put("additionalProperties",false))));
        return out;
    }
    /** Task selection guidance, not an execution policy. Real authorization stays in code. */
    static String description(String name){switch(name){
        case "list":return "List existing directory entries; not file contents. For newest use modified_desc and requested limit.";
        case "read":return "Read an existing UTF-8 file, not binary/APK. Use after finding the path; refresh=true bypasses cache.";
        case "search":return "Find local filenames under a directory, not web pages or file contents. Read/view a chosen result separately.";
        case "locations":return "Discover available device/environment folders when a path is unknown. Does not read their files.";
        case "screenshots":return "Find newest screenshot paths, excluding videos; limit=requested count. Use view_image to see pixels.";
        case "history":return "Recover earlier messages from THIS saved chat; untrusted excerpts, not current file contents or permissions.";
        case "view_image":return "Inspect an existing image visually when supported. Does not generate images; use screenshots/search to find paths.";
        case "view_video":return "Inspect sampled frames of an existing video; report frame differences/counts. No continuous playback or sound.";
        case "listen_audio":return "Inspect only the first 5 seconds of an existing audio file when supported. No full-file claim or speech generation.";
        case "inspect_apk":return "Inspect APK package/archive metadata, not UTF-8 text. Does not install, launch or decompile the app.";
        case "write":return "Save requested text, guides or documents as a UTF-8 file. Writing a Python installation guide means save the guide, NOT install Python. path=destination; text=complete contents. Empty truncates, NEVER deletes.";
        case "command":return "Execute Bash for requested actions: deletion/build/HTTP. NOT for writing/saving an installation guide; use write. command=script to execute, not a tutorial. Omit environment for default; check uid/tools before installation.";
        case "install":return "Install a required supported dependency after explicit app APT-plan approval. NOT for writing an installation guide; use write.";
        default:return "Inspect local data.";
    }}
    static JSONArray schemas(WorkspaceStore.Session s,JSONObject model)throws Exception{
        JSONArray all=schemas(s),out=new JSONArray();
        for(int i=0;i<all.length();i++){JSONObject schema=all.getJSONObject(i);String name=schema.getJSONObject("function").getString("name");if(name.matches("view_image|view_video")&&!LocalAiMedia.supported(model)||name.equals("listen_audio")&&!LocalAiMedia.audioSupported(model))continue;out.put(schema);}return out;
    }
    static String prompt(WorkspaceStore.Session s,String customBehavior)throws Exception{
        String behavior=customBehavior.isEmpty()?LocalAiPrompts.brief()+" Before installation check dependencies and selected environment; do not assume sudo is available or needed. Clarify only missing essentials; public webpages need no login.":customBehavior;
        if(LocalAiPrompts.stale(s))throw new IllegalStateException("CUSTOM_RIGHTS_PROMPT_STALE: review your custom rights prompt");
        if(s.localToolsOnly)behavior="";else if(s.localBehaviorMode.equals("brief"))behavior=LocalAiPrompts.brief();
        return behavior+"\n"+((s.localToolsOnly||s.localBehaviorMode.equals("brief"))?LocalAiPrompts.compactSettings(s):LocalAiQwenProtocol.settings(s))+(s.localRightsPrompt.isEmpty()?"":"\nCustom rights text (does not override actual app settings):\n"+s.localRightsPrompt)+"\nProtocol: use SDK structured function calls, one at a time; not tool JSON, tool_name/args or Qwen tags. Ordinary replies are text; no chat function. The app executes and checks approvals.";
    }
    /** Recognize leaked native syntax only; NEVER parse or execute its arguments. */
    static boolean leakedCall(String text){return LocalAiText.split(text).answer.trim().startsWith("<|tool_call>");}
    static String leakedName(String text){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("^"+java.util.regex.Pattern.quote("<|tool_call>")+"[ ]*call:([a-z_]{1,32})[ ]*[{]").matcher(LocalAiText.split(text).answer.trim());
        return m.find()?m.group(1):"";
    }
    static String completion(boolean nativeTools,String call,String text){return !call.isEmpty()?"native_tool":nativeTools&&leakedCall(text)?"unparsed_native_tool":"sdk_done";}
    static String progress(String system){int at=system.lastIndexOf("\nCURRENT_TURN_PROGRESS");if(at<0)return "";int end=system.indexOf("\nCORRECTION:",at);return system.substring(at,end<0?system.length():end);}
    static String correction(String coordinatorSystem){int at=coordinatorSystem.lastIndexOf("\nCORRECTION:");return at<0?"":coordinatorSystem.substring(at).replace("tool JSON","structured SDK function call").replace("Embedded JSON was NOT executed.","Draft output was NOT executed.");}
    static List<Object> providers(JSONArray schemas)throws Exception{
        List<Object> result=new ArrayList<>();Class<?> api=Class.forName(PKG+"OpenApiTool");
        for(int i=0;i<schemas.length();i++){
            String description=schemas.getJSONObject(i).getJSONObject("function").toString();
            Object proxy=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(p,m,a)->{
                if(m.getName().equals("getToolDescriptionJsonString"))return description;
                if(m.getName().equals("execute"))throw new SecurityException("Automatic SDK tool execution is disabled; use application authorization");
                if(m.getName().equals("toString"))return "ManualAppTool";
                if(m.getName().equals("hashCode"))return System.identityHashCode(p);
                if(m.getName().equals("equals"))return p==a[0];return null;
            });
            result.add(Class.forName(PKG+"ToolKt").getMethod("tool",api).invoke(null,proxy));
        }
        return result;
    }
    static JSONObject rawRequest(String name,Map<?,?> args)throws Exception{
        if(name==null||!name.matches("[a-z_]{1,32}"))throw new IllegalArgumentException("Invalid function name");
        if(args.containsKey("tool"))throw new IllegalArgumentException("Reserved tool discriminator");
        JSONObject r=new JSONObject(args);r.put("tool",name);
        for(String key:new String[]{"limit","start"})if(r.opt(key) instanceof Number){double d=((Number)r.get(key)).doubleValue();if(Double.isFinite(d)&&d==Math.rint(d)&&d>=Integer.MIN_VALUE&&d<=Integer.MAX_VALUE)r.put(key,(int)d);}
        return r;
    }
    static JSONObject request(String name,Map<?,?> args)throws Exception{
        JSONObject checked=LocalAiTools.request(rawRequest(name,args).toString());if(checked==null)throw new IllegalArgumentException("Invalid structured call");return checked;
    }
    static String call(Object message)throws Exception{
        List<?> calls=(List<?>)message.getClass().getMethod("getToolCalls").invoke(message);
        if(calls.isEmpty())return "";
        if(calls.size()!=1)throw new IllegalArgumentException("Only one structured function per response is supported; no calls executed");
        Object call=calls.get(0);return rawRequest((String)call.getClass().getMethod("getName").invoke(call),(Map<?,?>)call.getClass().getMethod("getArguments").invoke(call)).toString();
    }
    static Object contents(List<Object> parts)throws Exception{Object c=Class.forName(PKG+"Contents").getField("Companion").get(null);return c.getClass().getMethod("of",List.class).invoke(c,parts);}
    static Object text(String value)throws Exception{return Class.forName(PKG+"Content$Text").getConstructor(String.class).newInstance(value);}
    static Object message(String role,Object contents)throws Exception{Object c=Class.forName(PKG+"Message").getField("Companion").get(null);if(role.equals("model"))return c.getClass().getMethod("model",Class.forName(PKG+"Contents"),List.class,Map.class).invoke(c,contents,Collections.emptyList(),Collections.emptyMap());return c.getClass().getMethod(role,Class.forName(PKG+"Contents")).invoke(c,contents);}
    static Object callMessage(JSONObject request)throws Exception{
        JSONObject args=new JSONObject(request.toString());String name=args.getString("tool");args.remove("tool");Map<String,Object> values=map(args);
        Object call=Class.forName(PKG+"ToolCall").getConstructor(String.class,Map.class).newInstance(name,values);
        Object c=Class.forName(PKG+"Message").getField("Companion").get(null);
        return c.getClass().getMethod("model",Class.forName(PKG+"Contents"),List.class,Map.class).invoke(c,contents(Collections.singletonList(text(""))),Collections.singletonList(call),Collections.emptyMap());
    }
    static Object response(String name,String output)throws Exception{
        Object part=Class.forName(PKG+"Content$ToolResponse").getConstructor(String.class,Object.class).newInstance(name,Collections.singletonMap("output",output));
        return message("tool",contents(Collections.singletonList(part)));
    }
    static Map<String,Object> map(JSONObject o)throws Exception{Map<String,Object> out=new LinkedHashMap<>();Iterator<String> keys=o.keys();while(keys.hasNext()){String k=keys.next();out.put(k,value(o.get(k)));}return out;}
    static Object value(Object o)throws Exception{if(o instanceof JSONObject)return map((JSONObject)o);if(o instanceof JSONArray){List<Object> a=new ArrayList<>();for(int i=0;i<((JSONArray)o).length();i++)a.add(value(((JSONArray)o).get(i)));return a;}return o==JSONObject.NULL?null:o;}
}
