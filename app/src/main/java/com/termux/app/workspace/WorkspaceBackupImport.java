package com.termux.app.workspace;

import android.app.Activity;
import org.json.*;
import java.io.File;

import java.util.*;

/** Validates complete library before mutation; imports only chosen data, never live handles. */
final class WorkspaceBackupImport {
    static final class Choice {boolean commands,notes,presets,history,settings,replace;Set<Integer> candidates=new HashSet<>();}
    static final class Prepared {
        final List<WorkspaceStore.Command> commands=new ArrayList<>();final List<WorkspaceStore.Note> notes=new ArrayList<>();final List<WorkspaceStore.Session> presets=new ArrayList<>();
    }
    static Prepared prepare(Activity a,JSONObject report,JSONObject restored,Choice choice)throws Exception{
        Prepared out=new Prepared();JSONObject library=report.optJSONObject("workspace");
        if(library!=null){WorkspaceStore incoming=new WorkspaceStore(a,e->{});try{incoming.decode(library.toString());
            if(choice.commands)for(WorkspaceStore.Command c:incoming.commands){c.id=UUID.randomUUID().toString();out.commands.add(c);}
            if(choice.notes)for(WorkspaceStore.Note n:incoming.notes){n.id=UUID.randomUUID().toString();out.notes.add(n);}
            if(choice.presets)for(WorkspaceStore.Session s:incoming.sessions){if(WorkspaceStore.isBase(s)||!s.saved)continue;String oldId=s.id;s.id=UUID.randomUUID().toString();s.restoreReview=true;s.handle="";s.sshEncryptedPassword="";s.geminiEncryptedKey="";s.codexSocketId=UUID.randomUUID().toString();s.claudeSocketId=UUID.randomUUID().toString();s.geminiSocketId=UUID.randomUUID().toString();s.aiConnection=UUID.randomUUID().toString();s.aiSocketId="";s.saved=true;rebind(s,restored);if(choice.history)history(a,report,restored,oldId,s.id);out.presets.add(s);}
        }finally{incoming.close();}}
        JSONArray candidates=report.optJSONArray("candidates");JSONObject paths=restored.optJSONObject("paths");
        if(candidates!=null)for(Integer index:choice.candidates){JSONObject c=candidates.getJSONObject(index);WorkspaceStore.Command command=new WorkspaceStore.Command();command.name=c.getString("name");command.category="Imported";
            if(c.getString("kind").equals("alias"))command.command=c.getString("command");else{
                String component=Integer.toString(c.getInt("component")),target=paths==null?null:paths.optString(component,null);if(target==null)throw new IllegalArgumentException("SCRIPT_COMPONENT_NOT_RESTORED");
                String root=c.optString("root"),source=c.getString("source");String relative=root.isEmpty()?source:source.substring(root.length()+1);
                if(target.startsWith("proot-distro login "))command.command=target+" -- /bin/bash "+SessionShell.quote("/"+relative);
                else{File file=new File(target,relative);if(!file.getCanonicalPath().startsWith(new File(target).getCanonicalPath()+"/")||WorkspaceBackupEngine.symlink(file)||!file.isFile())throw new IllegalArgumentException("SCRIPT_COMPONENT_NOT_RESTORED");command.command="bash "+SessionShell.quote(file.getAbsolutePath());command.directory=target;}
            }out.commands.add(command);
        }
        JSONArray envs=restored.optJSONArray("environments");if(envs!=null)for(int i=0;i<envs.length();i++){String alias=envs.getString(i);if(!alias.matches("jobs-[a-f0-9]{12}"))throw new IllegalArgumentException("Invalid environment");WorkspaceStore.Session s=new WorkspaceStore.Session();s.name="Linux · "+alias;s.environment="termux";s.prootAlias=alias;s.prootIsolated=true;s.directory="/";s.startup="";s.saved=true;s.restoreReview=true;out.presets.add(s);}
        return out;
    }
    private static void rebind(WorkspaceStore.Session s,JSONObject restored)throws JSONException{
        // Restored agents keep their existing, explicit review path; do not migrate the shared Ubuntu.
        if(s.environment.equals("codex")||s.geminiAgent||s.claudeAgent||!s.aiAgent.isEmpty())return;
        String source=WorkspaceEnvironments.alias(s);JSONObject bindings=restored.optJSONObject("environmentBindings");
        if(source.isEmpty()||bindings==null||bindings.isNull(source)||!bindings.has(source))return;
        String target=bindings.getString(source);if(!target.matches("jobs-[a-f0-9]{12}"))throw new JSONException("Invalid restored environment binding");
        s.environment="termux";s.prootAlias=target;s.prootIsolated=true;
    }
    private static void history(Activity a,JSONObject report,JSONObject restored,String oldId,String newId)throws Exception{
        JSONArray components=report.getJSONArray("components");JSONObject paths=restored.optJSONObject("paths");if(paths==null)throw new IllegalArgumentException("APP_DATA_REQUIRED");
        for(int i=0;i<components.length();i++)if(components.getJSONObject(i).optString("kind").equals("app-data")&&paths.has(Integer.toString(i))){
            File root=new File(paths.getString(Integer.toString(i)));File file=new File(root,"workspace-sessions/"+oldId+"/history");
            if(!file.getCanonicalPath().startsWith(root.getCanonicalPath()+"/")||WorkspaceBackupEngine.symlink(file))throw new IllegalArgumentException("UNSAFE_HISTORY");
            if(file.isFile()){if(file.length()>64*1024*1024)throw new IllegalArgumentException("HISTORY_TOO_LARGE");File dir=SessionShell.directory(a.getFilesDir(),newId);if(!dir.mkdirs())throw new IllegalArgumentException("HISTORY_DIRECTORY");try(java.io.InputStream in=new java.io.FileInputStream(file);java.io.OutputStream out=new java.io.FileOutputStream(new File(dir,"history"))){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);}return;}
            return;
        }throw new IllegalArgumentException("APP_DATA_REQUIRED");
    }
    static void apply(Activity activity,WorkspaceStore store,Prepared data,Choice choice)throws Exception{
        if(!store.isWritable())throw new IllegalStateException("LIBRARY_READ_ONLY");
        File recovery=new File(activity.getFilesDir(),"workspace-import-recovery");if(!recovery.isDirectory()&&!recovery.mkdirs())throw new java.io.IOException("Recovery directory");
        File snapshot=new File(recovery,"before-"+UUID.randomUUID()+".json");try(java.io.OutputStream out=new java.io.FileOutputStream(snapshot)){out.write(store.encode().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        if(choice.replace){if(choice.commands)store.commands.clear();if(choice.notes)store.notes.clear();if(choice.presets)store.sessions.removeIf(s->s.saved&&s.handle.isEmpty()&&!WorkspaceStore.isBase(s));}
        for(WorkspaceStore.Command c:data.commands){WorkspaceStore.Command existing=store.commands.stream().filter(x->x.name.equals(c.name)&&x.command.equals(c.command)&&x.directory.equals(c.directory)).findFirst().orElse(null);if(existing==null)store.commands.add(c);else c.id=existing.id;}
        for(WorkspaceStore.Note n:data.notes){WorkspaceStore.Note existing=store.notes.stream().filter(x->x.name.equals(n.name)&&x.text.equals(n.text)).findFirst().orElse(null);if(existing==null)store.notes.add(n);else n.id=existing.id;}
        // Each restored preset retains its freshly allocated ID and history directory.
        // Names are not identities: even otherwise identical copies can own different histories.
        for(WorkspaceStore.Session s:data.presets)store.sessions.add(s);
        store.save();
    }
    static boolean persisted(JSONObject saved,Prepared data){
        for(String kind:new String[]{"commands","notes","sessions"}){
            Set<String> ids=new HashSet<>();JSONArray array=saved.optJSONArray(kind);if(array==null)return false;for(int i=0;i<array.length();i++)ids.add(array.optJSONObject(i).optString("id"));
            if(kind.equals("commands"))for(WorkspaceStore.Command c:data.commands){if(!ids.contains(c.id))return false;}
            if(kind.equals("notes"))for(WorkspaceStore.Note n:data.notes){if(!ids.contains(n.id))return false;}
            if(kind.equals("sessions"))for(WorkspaceStore.Session s:data.presets){
                JSONObject actual=null;for(int i=0;i<array.length();i++){JSONObject item=array.optJSONObject(i);if(item!=null&&s.id.equals(item.optString("id"))){actual=item;break;}}
                try{if(actual==null||!sameJson(actual,WorkspaceStore.sessionJson(s)))return false;}catch(JSONException error){return false;}
            }
        }return true;
    }
    private static boolean sameJson(Object left,Object right)throws JSONException{
        if(left instanceof JSONObject&&right instanceof JSONObject){JSONObject a=(JSONObject)left,b=(JSONObject)right;if(a.length()!=b.length())return false;Iterator<String> keys=a.keys();while(keys.hasNext()){String key=keys.next();if(!b.has(key)||!sameJson(a.get(key),b.get(key)))return false;}return true;}
        if(left instanceof JSONArray&&right instanceof JSONArray){JSONArray a=(JSONArray)left,b=(JSONArray)right;if(a.length()!=b.length())return false;for(int i=0;i<a.length();i++)if(!sameJson(a.get(i),b.get(i)))return false;return true;}
        if(left instanceof Number&&right instanceof Number)return new java.math.BigDecimal(left.toString()).compareTo(new java.math.BigDecimal(right.toString()))==0;
        return java.util.Objects.equals(left,right);
    }
    static JSONObject settings(Activity a)throws JSONException{
        JSONObject out=new JSONObject();for(String name:new String[]{"workspace-language","workspace-input","com.termux_preferences"}){JSONObject values=new JSONObject();for(Map.Entry<String,?> entry:a.getSharedPreferences(name,0).getAll().entrySet()){Object v=entry.getValue();if(v instanceof String||v instanceof Boolean||v instanceof Integer||v instanceof Long||v instanceof Float)values.put(entry.getKey(),v);}out.put(name,values);}return out;
    }
    static void settings(Activity a,JSONObject value)throws JSONException{
        if(value==null)return;
        for(String name:new String[]{"workspace-language","workspace-input","com.termux_preferences"}){JSONObject data=value.optJSONObject(name);if(data==null)continue;android.content.SharedPreferences.Editor edit=a.getSharedPreferences(name,0).edit();Iterator<String> keys=data.keys();while(keys.hasNext()){String key=keys.next();if(key.equals("current_session")||key.equals("jobs_log_off_default_v1"))continue;Object v=data.get(key);if(v instanceof Boolean)edit.putBoolean(key,(Boolean)v);else if(v instanceof Integer)edit.putInt(key,(Integer)v);else if(v instanceof Long)edit.putLong(key,(Long)v);else if(v instanceof Number)edit.putFloat(key,((Number)v).floatValue());else if(v instanceof String&&((String)v).length()<64*1024)edit.putString(key,(String)v);}edit.apply();}
    }
}
