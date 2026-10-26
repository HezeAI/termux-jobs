package com.termux.app.workspace;

import android.app.Activity;
import java.io.IOException;
import java.util.*;
import org.json.*;

/** Explicit managed-library transfer. Never discovers or evaluates shell definitions. */
final class WorkspaceAliasImport {
    static List<WorkspaceStore.Alias> read(Activity activity,JSONObject library)throws Exception {
        if(library==null)throw new IOException("ALIAS_LIBRARY_MISSING");
        WorkspaceStore incoming=new WorkspaceStore(activity,error->{});
        try {
            incoming.decode(library.toString());
            List<WorkspaceStore.Alias> out=new ArrayList<>();
            for(WorkspaceStore.Alias alias:incoming.aliases)out.add(alias.copy());
            return out;
        } finally {incoming.close();}
    }
    static JSONObject export(List<WorkspaceStore.Alias> selected)throws JSONException {
        JSONArray aliases=new JSONArray();Set<String> names=new HashSet<>(),ids=new HashSet<>();
        for(WorkspaceStore.Alias a:selected){
            if(!WorkspaceAliases.valid(a)||!names.add(a.environment+":"+a.name)||!ids.add(a.id))throw new JSONException("ALIAS_INVALID_DEFINITION");
            aliases.put(new JSONObject().put("id",a.id).put("name",a.name).put("command",a.command).put("environment",a.environment).put("category",a.category).put("favorite",a.favorite));
        }
        return new JSONObject().put("version",8).put("commands",new JSONArray()).put("notes",new JSONArray()).put("sessions",new JSONArray()).put("recentCommands",new JSONArray()).put("aliases",aliases);
    }
    static final class Plan {
        final List<WorkspaceStore.Alias> before=new ArrayList<>(),after=new ArrayList<>();
        int replacements(){int count=0;for(WorkspaceStore.Alias a:before)if(a!=null)count++;return count;}
    }
    static Plan plan(List<WorkspaceStore.Alias> incoming,Set<String> selected,List<WorkspaceStore.Alias> existing,String general)throws IOException {
        Plan out=new Plan();Set<String> found=new HashSet<>(),keys=new HashSet<>(),ids=new HashSet<>();
        for(WorkspaceStore.Alias a:existing)ids.add(a.id);
        for(WorkspaceStore.Alias source:incoming){
            if(!WorkspaceAliases.valid(source)||!keys.add(source.environment+":"+source.name))throw new IOException("ALIAS_INVALID_DEFINITION");
            if(!selected.contains(source.id))continue;
            if(!found.add(source.id))throw new IOException("ALIAS_INVALID_DEFINITION");
            WorkspaceStore.Alias previous=null;for(WorkspaceStore.Alias a:existing)if(a.environment.equals(source.environment)&&a.name.equals(source.name)){previous=a.copy();break;}
            WorkspaceStore.Alias next=source.copy();
            if(previous!=null)next.id=previous.id;else do{next.id=UUID.randomUUID().toString();}while(!ids.add(next.id));
            if(next.category.trim().isEmpty())next.category=general;
            out.before.add(previous);out.after.add(next);
        }
        if(found.size()!=selected.size()||found.isEmpty())throw new IOException("ALIAS_INVALID_SELECTION");
        return out;
    }
    static void verify(Plan plan,List<WorkspaceStore.Alias> current)throws IOException {
        for(int i=0;i<plan.after.size();i++){
            WorkspaceStore.Alias after=plan.after.get(i),before=plan.before.get(i),found=null;
            for(WorkspaceStore.Alias a:current)if(a.environment.equals(after.environment)&&a.name.equals(after.name)||a.id.equals(after.id)){found=a;break;}
            if(!same(before,found))throw new IOException("ALIAS_LIBRARY_CHANGED");
        }
    }
    private static boolean same(WorkspaceStore.Alias a,WorkspaceStore.Alias b){return a==null?b==null:b!=null&&a.id.equals(b.id)&&a.name.equals(b.name)&&a.command.equals(b.command)&&a.environment.equals(b.environment)&&a.category.equals(b.category)&&a.favorite==b.favorite;}
    static void install(Plan plan,WorkspaceAliases.Installer installer)throws Exception {
        int done=0;
        try {for(;done<plan.after.size();done++)WorkspaceAliases.change(plan.before.get(done),plan.after.get(done),null,installer);}
        catch(Exception failure){
            boolean failed=false;for(int i=done-1;i>=0;i--)try{WorkspaceAliases.change(plan.after.get(i),plan.before.get(i),null,installer);}catch(Exception rollback){failed=true;}
            if(failed)throw new IOException("ALIAS_ROLLBACK_FAILED",failure);
            throw failure;
        }
    }
    static void commit(Plan plan,WorkspaceStore store)throws IOException {
        if(!store.isWritable())throw new IOException("LIBRARY_READ_ONLY");verify(plan,store.aliases);
        for(WorkspaceStore.Alias a:plan.after){store.aliases.removeIf(old->old.id.equals(a.id));store.aliases.add(a.copy());}store.save();
    }
    static boolean persisted(Plan plan,JSONObject saved)throws JSONException {
        JSONArray aliases=saved.optJSONArray("aliases");if(aliases==null)return false;
        for(WorkspaceStore.Alias a:plan.after){boolean found=false;
            for(int i=0;i<aliases.length();i++){JSONObject b=aliases.getJSONObject(i);if(a.id.equals(b.optString("id"))){found=a.name.equals(b.optString("name"))&&a.command.equals(b.optString("command"))&&a.environment.equals(b.optString("environment"))&&a.category.equals(b.optString("category"))&&a.favorite==b.optBoolean("favorite");break;}}
            if(!found)return false;
        }return true;
    }
}
