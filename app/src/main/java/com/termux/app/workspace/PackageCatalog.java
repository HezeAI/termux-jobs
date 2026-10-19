package com.termux.app.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** APT metadata only; mutations are explicitly handed to a new interactive terminal. */
final class PackageCatalog {
    static final int MAX_BYTES=16*1024*1024;
    static final class Entry {
        final String name,version;final boolean downgrade;
        Entry(String name,String version){this(name,version,false);}
        Entry(String name,String version,boolean downgrade){this.name=name;this.version=version;this.downgrade=downgrade;}
    }
    static boolean validName(String name){return name!=null&&name.matches("[a-z0-9][a-z0-9+.-]*(?::[a-z0-9][a-z0-9-]*)?");}
    static String query(int tab){
        if(tab==0)return "LC_ALL=C dpkg-query -W -f='${db:Status-Abbrev}\\t${binary:Package}\\t${Version}\\n'";
        if(tab!=1&&tab!=2)throw new IllegalArgumentException();
        return "LC_ALL=C apt -o Dir::Cache::pkgcache='' -o Dir::Cache::srcpkgcache='' list"+(tab==2?" --upgradable":"");
    }
    static List<Entry> parse(byte[] data,int tab)throws IOException{
        if(data.length>MAX_BYTES)throw new IOException(WorkspaceText.tr("Каталог пакетов слишком большой или повреждён"));
        TreeMap<String,Entry> result=new TreeMap<>();
        for(String line:new String(data,StandardCharsets.UTF_8).split("\n")){
            String name,version;
            if(tab==0){String[] f=line.split("\t",-1);if(f.length!=3||!f[0].startsWith("ii"))continue;name=f[1];version=f[2];}
            else {int slash=line.indexOf('/');if(slash<1)continue;name=line.substring(0,slash);String[] f=line.substring(slash+1).trim().split("\\s+");if(f.length<3)throw new IOException(WorkspaceText.tr("Каталог пакетов слишком большой или повреждён"));version=f[1];}
            if(!validName(name)||version.length()>256||version.isEmpty())throw new IOException(WorkspaceText.tr("Каталог пакетов слишком большой или повреждён"));
            result.put(name,new Entry(name,version));
        }
        return new ArrayList<>(result.values());
    }
    static String operation(String action,String name){
        if(action.equals("refresh"))return "apt-get update";
        if(!validName(name))throw new IllegalArgumentException(WorkspaceText.tr("Некорректное имя пакета"));
        switch(action){case "install":return "apt-get install -- "+SessionShell.quote(name);case "upgrade":return "apt-get install --only-upgrade -- "+SessionShell.quote(name);case "remove":return "apt-get remove -- "+SessionShell.quote(name);default:throw new IllegalArgumentException();}
    }
    static boolean validVersion(String version){return version!=null&&version.length()<=256&&version.matches("[0-9][A-Za-z0-9.+:~\\-]*");}
    static String versionOperation(Entry entry){
        if(!validName(entry.name)||!validVersion(entry.version))throw new IllegalArgumentException(WorkspaceText.tr("Некорректная версия пакета"));
        return "apt-get install -- "+SessionShell.quote(entry.name+"="+entry.version);
    }
    static String versionsQuery(String name){
        if(!validName(name))throw new IllegalArgumentException(WorkspaceText.tr("Некорректное имя пакета"));
        return "set -o pipefail; metadata=$(LC_ALL=C dpkg-query -W -f='${db:Status-Abbrev}|${Version}' -- "+SessionShell.quote(name)+" 2>/dev/null) || metadata=''; installed=''; case \"$metadata\" in ii*) installed=\"${metadata#*|}\";; esac; "
            +"LC_ALL=C apt-cache -o Dir::Cache::pkgcache='' -o Dir::Cache::srcpkgcache='' madison "+SessionShell.quote(name)
            +" | while IFS='|' read -r package version repository; do version=\"${version//[[:space:]]/}\"; [ -n \"$version\" ] || continue; lower=0; if [ -n \"$installed\" ] && dpkg --compare-versions \"$version\" lt \"$installed\"; then lower=1; fi; printf '%s\\t%s\\n' \"$version\" \"$lower\"; done";
    }
    static List<Entry> parseVersions(String name,byte[] bytes)throws IOException{
        if(!validName(name)||bytes.length>1024*1024)throw new IOException(WorkspaceText.tr("Некорректная версия пакета"));
        LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
        for(String line:new String(bytes,StandardCharsets.UTF_8).split("\n")){
            if(line.isEmpty())continue;String[] fields=line.split("\t",-1);
            if(fields.length!=2||!validVersion(fields[0])||!fields[1].matches("[01]"))throw new IOException(WorkspaceText.tr("Некорректная версия пакета"));
            entries.put(fields[0],new Entry(name,fields[0],fields[1].equals("1")));
            if(entries.size()>1000)throw new IOException(WorkspaceText.tr("Каталог пакетов слишком большой или повреждён"));
        }
        return new ArrayList<>(entries.values());
    }
    static String descriptionQuery(Entry entry,boolean installed){
        if(!validName(entry.name)||!validVersion(entry.version))throw new IllegalArgumentException();
        if(installed)return "LC_ALL=C dpkg-query -W -f='Description: ${Description}\\n' -- "+SessionShell.quote(entry.name);
        return "LC_ALL=C apt-cache -o Dir::Cache::pkgcache='' -o Dir::Cache::srcpkgcache='' show --no-all-versions "+SessionShell.quote(entry.name+"="+entry.version);
    }
    static String parseDescription(byte[] bytes)throws IOException{
        if(bytes.length>256*1024)throw new IOException(WorkspaceText.tr("Описание недоступно. Обновите каталог или проверьте выбранное окружение."));
        StringBuilder description=new StringBuilder();boolean active=false;
        for(String line:new String(bytes,StandardCharsets.UTF_8).split("\n",-1)){
            if(active){if(line.startsWith(" ")){String value=line.substring(1);description.append('\n').append(value.equals(".")?"":value);continue;}break;}
            if(line.startsWith("Description: ")||line.startsWith("Description-en: ")){active=true;description.append(line.substring(line.indexOf(':')+1).trim());}
        }
        return description.toString().trim();
    }
    static boolean matchesTarget(WorkspaceStore.Session profile,String heldHandle,String actualHandle,String environment){
        return heldHandle!=null&&!heldHandle.isEmpty()&&heldHandle.equals(actualHandle)&&ProjectFiles.environment(profile).equals(environment);
    }
    static WorkspaceStore.Session operationProfile(WorkspaceStore.Session source,String command){
        String env=ProjectFiles.environment(source);
        if(!env.equals("termux")&&!env.equals("ubuntu")&&!env.startsWith("proot:"))throw new IllegalArgumentException();
        WorkspaceStore.Session target=new WorkspaceStore.Session();target.name=WorkspaceText.tr("Пакеты · ")+ProjectFiles.environmentLabel(source);target.environment=env.startsWith("proot:")?"termux":env;target.prootAlias=env.startsWith("proot:")?env.substring(6):"";target.prootIsolated=source.prootIsolated;target.directory="~";target.startup=command;
        if(!target.prootAlias.isEmpty()){
            if(!WorkspaceLocalFilePicker.validAlias(target.prootAlias))throw new IllegalArgumentException("INVALID_ENVIRONMENT");
            target.startup="if [ \"${TERMUX_JOBS_ENVIRONMENT_ALIAS:-}\" = "+SessionShell.quote(target.prootAlias)+" ]; then\n"+EnvironmentPackages.nativeCommand(command)+"\nelse printf '%s\\n' 'Wrong environment. Open a new session in the selected environment.' >&2; false; fi";
        }
        return target;
    }
}
