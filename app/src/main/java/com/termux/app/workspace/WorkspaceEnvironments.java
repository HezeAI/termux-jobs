package com.termux.app.workspace;

import java.io.File;
import java.util.*;
import com.termux.shared.termux.TermuxConstants;

/** Concrete proot aliases, independent of session name and AI provider. */
final class WorkspaceEnvironments {
    private WorkspaceEnvironments() {}
    static final String OLD_READER_GUARD="printf '%s\\n' 'This environment preset requires a newer Termux Jobs version. Its commands were not run.' >&2; false";
    static final class Choice {
        final String environment,alias,label,home;final boolean installed;
        Choice(String e,String a,String l,String h,boolean i){environment=e;alias=a;label=l;home=h;installed=i;}
    }
    static String alias(WorkspaceStore.Session s){
        if(!s.prootAlias.isEmpty())return s.prootAlias;
        if(s.environment.startsWith("proot:"))return s.environment.substring(6);
        return s.environment.equals("ubuntu")?"ubuntu":"";
    }
    static String label(String alias){switch(alias){case "jobs-x11-ubuntu":return "Ubuntu X11 Test";case "ubuntu-ai":return "Ubuntu AI";case "ubuntu":return "Ubuntu";case "debian":return "Debian";case "alpine":return "Alpine";case "archlinux":return "Arch Linux";case "fedora":return "Fedora";case "opensuse":return "openSUSE";case "void":return "Void Linux";default:return alias;}}
    static File root(File proot,String alias){
        if(!WorkspaceLocalFilePicker.validAlias(alias))return null;
        for(String layout:new String[]{"containers","installed-rootfs"}){
            File parent=new File(proot,layout),container=new File(parent,alias),fs=layout.equals("containers")?new File(container,"rootfs"):container;
            try{if(fs.isDirectory()&&container.getCanonicalFile().equals(new File(parent.getCanonicalFile(),alias))&&fs.getCanonicalFile().equals(new File(fs.getParentFile().getCanonicalFile(),fs.getName())))return fs;}catch(java.io.IOException ignored){}
        }
        return null;
    }
    /** OCI run bypasses an invalid passwd Bash only when no Entrypoint can change our command. */
    static String verb(File proot,String alias){
        File fs=root(proot,alias);if(fs==null||new File(fs,"bin/bash").exists()||!new File(fs,"bin/sh").exists())return "login";
        File manifest=new File(proot,"containers/"+alias+"/manifest.json"),passwd=new File(fs,"etc/passwd");
        try{
            if(!manifest.isFile()||manifest.length()>64*1024||java.nio.file.Files.isSymbolicLink(manifest.toPath())||!passwd.isFile()||passwd.length()>256*1024||java.nio.file.Files.isSymbolicLink(passwd.toPath()))return "login";
            if(!passwd.getCanonicalPath().startsWith(fs.getCanonicalPath()+"/"))return "login";
            boolean badRoot=false;for(String line:java.nio.file.Files.readAllLines(passwd.toPath(),java.nio.charset.StandardCharsets.UTF_8)){String[] fields=line.split(":",-1);if(fields.length>=7&&fields[0].equals("root"))badRoot=fields[6].equals("/bin/bash");}
            if(!badRoot)return "login";
            org.json.JSONObject data=new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(manifest.toPath()),java.nio.charset.StandardCharsets.UTF_8));
            org.json.JSONObject image=data.optJSONObject("image_config"),config=image==null?null:image.optJSONObject("config");if(config==null)return "login";
            for(String key:new String[]{"Entrypoint","Cmd"})if(config.has(key)&&!config.isNull(key)){
                org.json.JSONArray value=config.optJSONArray(key);if(value==null)return "login";
                if(key.equals("Entrypoint")&&value.length()!=0)return "login";
                for(int k=0;k<value.length();k++)if(!(value.get(k) instanceof String))return "login";
            }
            return "run";
        }catch(java.io.IOException|org.json.JSONException error){return "login";}
    }
    static String verb(String alias){return verb(new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"),alias);}
    static String entryCommand(String alias,boolean isolated){String verb=verb(alias);return "proot-distro "+verb+" "+(isolated?"--isolated ":"")+(verb.equals("run")?"--work-dir /root ":"")+SessionShell.quote(alias)+(verb.equals("run")?" -- /bin/sh -i":"");}
    static List<Choice> choices(File home,File proot){
        List<Choice> rows=new ArrayList<>();rows.add(new Choice("termux","","Termux",home.getPath(),true));
        Set<String> names=new LinkedHashSet<>(Arrays.asList("ubuntu","debian","alpine","archlinux","fedora","opensuse","void"));
        for(WorkspaceLocalFilePicker.Location location:WorkspaceLocalFilePicker.locations(home,proot,""))if(location.environment.startsWith("proot:"))names.add(location.environment.substring(6));
        for(String alias:names){File fs=root(proot,alias);rows.add(new Choice(alias.equals("ubuntu")?"ubuntu":"proot:"+alias,alias,label(alias),fs!=null&&new File(fs,"root").isDirectory()?"/root":"/",fs!=null));}
        return rows;
    }
    static List<Choice> choices(){return choices(new File(TermuxConstants.TERMUX_HOME_DIR_PATH),new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"));}
}
