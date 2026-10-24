package com.termux.app.workspace;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Separate versioned store; never migrates or rewrites existing SSH sessions/library. */
final class ServerProfiles {
    static final class Profile {
        String id=UUID.randomUUID().toString(),name="",host="",user="",identity="",auth="auto",encrypted="";
        int port=22; boolean automatic=true;volatile boolean diagnosticsPaused; boolean backgroundPolling; final List<Project> projects=new ArrayList<>(); final List<String> favorites=new ArrayList<>(),favoriteFiles=new ArrayList<>(); final List<Action> actions=new ArrayList<>();
        WorkspaceStore.Session session(){WorkspaceStore.Session s=new WorkspaceStore.Session();s.id=id;s.name=name;s.environment="ssh";s.sshHost=host;s.sshUser=user;s.sshPort=port;s.sshAuth=auth;s.sshIdentity=identity;s.sshEncryptedPassword=encrypted;s.directory="~";return s;}
        JSONObject json() throws JSONException {if(favorites.size()>100||favoriteFiles.size()>100||actions.size()>50)throw new JSONException("STORE_LIMIT");for(String path:favorites)if(!path.startsWith("/")||path.length()>4096||path.indexOf('\0')>=0)throw new JSONException("INVALID_FAVORITE");for(String path:favoriteFiles)if(!path.startsWith("/")||path.length()>4096||path.indexOf('\0')>=0)throw new JSONException("INVALID_FAVORITE");for(Action action:actions)if(action.name.trim().isEmpty()||action.name.length()>160||action.path.isEmpty()||action.path.length()>4096||action.path.indexOf('\0')>=0||action.command.trim().isEmpty()||action.command.length()>8192||action.command.indexOf('\0')>=0)throw new JSONException("INVALID_ACTION");JSONArray files=new JSONArray();for(String path:favoriteFiles)files.put(path);JSONArray stars=new JSONArray();for(String path:favorites)stars.put(path);JSONArray acts=new JSONArray();for(Action action:actions)acts.put(new JSONObject().put("name",action.name).put("path",action.path).put("command",action.command));JSONArray a=new JSONArray();for(Project p:projects)a.put(new JSONObject().put("name",p.name).put("path",p.path));return new JSONObject().put("id",id).put("name",name).put("host",host).put("user",user).put("port",port).put("identity",identity).put("auth",auth).put("encrypted",encrypted).put("automatic",automatic).put("diagnosticsPaused",diagnosticsPaused).put("backgroundPolling",backgroundPolling).put("projects",a).put("favorites",stars).put("favoriteFiles",files).put("actions",acts);}
    }
    static final class Action {final String name,path,command;Action(String n,String p,String c){name=n;path=p;command=c;}}
    static final class Project {final String name,path;Project(String n,String p){name=n;path=p;}}
    final List<Profile> profiles=new ArrayList<>();
    /** Freeze connection settings for one explicit operation; do not copy editable UI collections. */
    static Profile connectionSnapshot(Profile source){Profile p=new Profile();p.id=source.id;p.name=source.name;p.host=source.host;p.port=source.port;p.user=source.user;p.auth=source.auth;p.identity=source.identity;p.encrypted=source.encrypted;p.diagnosticsPaused=source.diagnosticsPaused;return p;}
    private final AtomicFile file;
    boolean writable=true;
    ServerProfiles(Context c){file=new AtomicFile(new File(c.getFilesDir(),"workspace-servers-v1.json"));load();}
    static boolean valid(Profile p){return p.id.matches("[a-f0-9-]{36}")&&!p.name.trim().isEmpty()&&p.name.length()<=100&&SshSessionShell.validHost(p.host)&&SshSessionShell.validUser(p.user)&&SshSessionShell.validPort(p.port)&&Arrays.asList("auto","key","password").contains(p.auth)&&(!p.auth.equals("key")||SshSessionShell.validIdentity(p.identity));}
    private void load(){
        try(InputStream in=file.openRead()){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1024*1024)throw new IOException("STORE_LIMIT");out.write(b,0,n);}
            JSONObject root=new JSONObject(out.toString("UTF-8"));if(root.getInt("version")!=1)throw new IOException("STORE_VERSION");JSONArray items=root.getJSONArray("servers");
            Set<String> ids=new HashSet<>();if(items.length()>30)throw new IOException("STORE_LIMIT");
            for(int i=0;i<items.length();i++){JSONObject o=items.getJSONObject(i);Profile p=new Profile();p.id=o.getString("id");p.name=o.getString("name");p.host=o.getString("host");p.user=o.getString("user");p.port=o.getInt("port");p.identity=o.optString("identity");p.auth=o.optString("auth","auto");p.encrypted=o.optString("encrypted");p.automatic=o.optBoolean("automatic",true);p.diagnosticsPaused=o.optBoolean("diagnosticsPaused",false);p.backgroundPolling=o.optBoolean("backgroundPolling",false);if(!valid(p)||!ids.add(p.id))throw new IOException("INVALID_PROFILE");JSONArray ps=o.optJSONArray("projects");if(ps!=null)for(int j=0;j<Math.min(ps.length(),100);j++){JSONObject v=ps.getJSONObject(j);p.projects.add(new Project(v.getString("name"),v.getString("path")));}JSONArray favorites=o.optJSONArray("favorites");if(favorites!=null)for(int j=0;j<Math.min(favorites.length(),100);j++){String path=favorites.getString(j);if(path.startsWith("/")&&path.length()<=4096&&path.indexOf('\0')<0&&!p.favorites.contains(path))p.favorites.add(path);}
            JSONArray favoriteFiles=o.optJSONArray("favoriteFiles");if(favoriteFiles!=null)for(int j=0;j<Math.min(favoriteFiles.length(),100);j++){String path=favoriteFiles.getString(j);if(path.startsWith("/")&&path.length()<=4096&&path.indexOf('\0')<0&&!p.favoriteFiles.contains(path))p.favoriteFiles.add(path);}
            JSONArray actions=o.optJSONArray("actions");if(actions!=null)for(int j=0;j<Math.min(actions.length(),50);j++){JSONObject action=actions.getJSONObject(j);String actionName=action.getString("name"),path=action.getString("path"),cmd=action.getString("command");if(!actionName.trim().isEmpty()&&actionName.length()<=160&&!path.isEmpty()&&path.length()<=4096&&path.indexOf('\0')<0&&!cmd.trim().isEmpty()&&cmd.length()<=8192&&cmd.indexOf('\0')<0)p.actions.add(new Action(actionName,path,cmd));}profiles.add(p);}
        }catch(FileNotFoundException ignored){}catch(Exception e){profiles.clear();writable=false;}
    }
    synchronized void save() throws Exception{
        if(!writable)throw new IOException("STORE_READ_ONLY");if(profiles.size()>30)throw new IOException("STORE_LIMIT");JSONArray a=new JSONArray();for(Profile p:profiles){if(!valid(p))throw new IOException("INVALID_PROFILE");a.put(p.json());}
        byte[] data=new JSONObject().put("version",1).put("servers",a).toString().getBytes(StandardCharsets.UTF_8);if(data.length>1024*1024)throw new IOException("STORE_LIMIT");
        FileOutputStream out=null;try{out=file.startWrite();out.write(data);file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
}
