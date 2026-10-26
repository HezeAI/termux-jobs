package com.termux.app.workspace;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Alias definitions are written as data to one explicit installed environment. */
final class WorkspaceAliases {
    static final java.util.concurrent.atomic.AtomicBoolean BUSY=new java.util.concurrent.atomic.AtomicBoolean();
    static final List<String> ENVIRONMENTS=Collections.unmodifiableList(Arrays.asList("termux","ubuntu","debian","alpine","archlinux","fedora","opensuse","void"));
    static boolean validName(String name){return name!=null&&name.matches("[A-Za-z_][A-Za-z0-9_-]{0,63}")&&!Arrays.asList("if","then","else","elif","fi","do","done","for","while","until","case","esac","in","function","select","time","coproc").contains(name);}
    static boolean valid(WorkspaceStore.Alias a){
        if(a==null||!validName(a.name)||!ENVIRONMENTS.contains(a.environment)||a.command==null||a.command.trim().isEmpty()||a.command.length()>16384||a.command.indexOf('\0')>=0||a.command.indexOf('\r')>=0)return false;
        return a.id!=null&&a.id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }
    static WorkspaceStore.Session profile(String environment){
        if(!ENVIRONMENTS.contains(environment))throw new IllegalArgumentException("ALIAS_INVALID_ENVIRONMENT");
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="termux";
        if(!environment.equals("termux"))s.prootAlias=environment;
        return s;
    }
    static List<WorkspaceEnvironments.Choice> installed(List<WorkspaceEnvironments.Choice> all){
        List<WorkspaceEnvironments.Choice> out=new ArrayList<>();
        for(WorkspaceEnvironments.Choice c:all)if(c.installed&&ENVIRONMENTS.contains(c.alias.isEmpty()?"termux":c.alias))out.add(c);
        return out;
    }
    static String hook(){return "if [ ! -L \"$HOME/.termux-jobs-aliases\" ] && [ -f \"$HOME/.termux-jobs-aliases/_loader.sh\" ] && [ ! -L \"$HOME/.termux-jobs-aliases/_loader.sh\" ]; then\n"
        +"for _termux_jobs_alias in \"$HOME\"/.termux-jobs-aliases/*.sh; do\n"
        +"if [ -f \"$_termux_jobs_alias\" ] && [ ! -L \"$_termux_jobs_alias\" ]; then . \"$_termux_jobs_alias\"; fi\ndone\n\\unset _termux_jobs_alias\nfi\n";}
    static String script(String helper,WorkspaceStore.Alias a,boolean delete,String replace){
        if(!valid(a)||replace==null||!replace.isEmpty()&&!replace.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("ALIAS_INVALID_DEFINITION");
        return "set -- "+SessionShell.quote(delete?"delete":"save")+" "+SessionShell.quote(a.id)+" "+SessionShell.quote(a.name)+" "+SessionShell.quote(a.command)+" "+SessionShell.quote(replace)+"\n"+helper;
    }
    static void apply(android.app.Activity activity,WorkspaceStore.Alias a,boolean delete,String replace)throws Exception{
        apply(activity,a,delete,replace,WorkspaceEnvironments.choices());
    }
    static void apply(android.app.Activity activity,WorkspaceStore.Alias a,boolean delete,String replace,List<WorkspaceEnvironments.Choice> choices)throws Exception{
        if(!valid(a))throw new IOException("ALIAS_INVALID_DEFINITION");
        // Recheck at execution time: a removed rootfs must not silently fall back to Termux.
        boolean found=false;for(WorkspaceEnvironments.Choice c:installed(choices))if((c.alias.isEmpty()?"termux":c.alias).equals(a.environment))found=true;
        if(!found){if(delete)return;throw new IOException("ALIAS_ENVIRONMENT_NOT_INSTALLED");}
        String helper;try(InputStream in=activity.getAssets().open("workspace-aliases.sh");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);helper=new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
        try(ProjectFiles files=new ProjectFiles(activity,profile(a.environment))){
            String answer=files.aliasOperation(script(helper,a,delete,replace));if(!answer.trim().equals("ALIAS_OK"))throw new IOException("ALIAS_SAVE_FAILED");
        }
    }
    interface Installer {void apply(WorkspaceStore.Alias alias,boolean delete)throws Exception;}
    private interface Undo {void run()throws Exception;}
    static void change(WorkspaceStore.Alias before,WorkspaceStore.Alias after,WorkspaceStore.Alias collision,Installer installer)throws Exception{
        List<Undo> undo=new ArrayList<>();
        try{
            if(after==null){installer.apply(before,true);return;}
            installer.apply(after,false);
            if(before!=null&&before.environment.equals(after.environment))undo.add(()->installer.apply(before,false));else undo.add(()->installer.apply(after,true));
            if(collision!=null){installer.apply(collision,true);undo.add(()->installer.apply(collision,false));}
            if(before!=null&&!before.environment.equals(after.environment)){installer.apply(before,true);undo.add(()->installer.apply(before,false));}
        }catch(Exception failure){
            boolean failed=false;for(int i=undo.size()-1;i>=0;i--)try{undo.get(i).run();}catch(Exception rollback){failed=true;}
            if(failed)throw new IOException("ALIAS_ROLLBACK_FAILED: "+failure.getMessage(),failure);
            throw failure;
        }
    }
    static String label(String environment){return environment.equals("termux")?"Termux":WorkspaceEnvironments.label(environment);}
}
