package com.termux.app.workspace;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.xmlpull.v1.XmlPullParser;

/** Native package managers; catalogue reads never install or refresh repositories. */
final class EnvironmentPackages {
    enum Kind {APT, APK, DNF, ZYPPER, XBPS, PACMAN}
    // proot exposes Termux bin as a fallback; package commands must stay guest-native.
    static final String GUEST_PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";
    static String nativeCommand(String script){return "( PATH="+SessionShell.quote(GUEST_PATH)+"; export PATH; "+script+"\n)";}
    static String xbps(String script){return "( unset FORCE_IPV6; FORCE_IPV4=1 "+script+" )";}
    static String probe(){return "if command -v apt-get >/dev/null && command -v dpkg-query >/dev/null; then printf APT; elif command -v apk >/dev/null; then printf APK; elif command -v zypper >/dev/null; then printf ZYPPER; elif command -v dnf >/dev/null; then printf DNF; elif command -v xbps-query >/dev/null; then printf XBPS; elif command -v pacman >/dev/null; then printf PACMAN; else exit 2; fi";}
    static Kind kind(byte[] value)throws IOException{try{return Kind.valueOf(new String(value,StandardCharsets.UTF_8).trim());}catch(IllegalArgumentException error){throw new IOException("PACKAGE_MANAGER_NOT_FOUND");}}
    static String query(Kind kind,int tab){
        if(tab<0||tab>2)throw new IllegalArgumentException();
        switch(kind){
            case APT:return PackageCatalog.query(tab);
            case APK:return "LC_ALL=C apk --no-network list "+new String[]{"--installed","","--upgradable"}[tab];
            case DNF:return "LC_ALL=C dnf --quiet --cacheonly repoquery "+new String[]{"--installed","--available --latest-limit=1","--upgrades"}[tab]+" --queryformat '%{name}\t%{evr}\\n'";
            case ZYPPER:return "LC_ALL=C zypper --xmlout --no-refresh "+(tab==0?"--disable-repositories ":"")+(tab==2?"list-updates --type package":"search --details --type package"+(tab==0?" --installed-only":""));
            case XBPS:return xbps("LC_ALL=C "+new String[]{"xbps-query -l","xbps-query -Rs ''","xbps-install -un"}[tab]);
            case PACMAN:return "LC_ALL=C pacman "+new String[]{"-Q","-Sl","-Qu"}[tab];
            default:throw new IllegalArgumentException();
        }
    }
    static boolean validName(String name){return name!=null&&name.length()<=256&&name.matches("[A-Za-z0-9][A-Za-z0-9+._:@-]*");}
    static List<PackageCatalog.Entry> parse(Kind kind,byte[] bytes,int tab)throws IOException{
        if(kind==Kind.APT)return PackageCatalog.parse(bytes,tab);
        if(bytes.length>PackageCatalog.MAX_BYTES)throw new IOException("PACKAGE_LIST_TOO_LARGE");
        TreeMap<String,PackageCatalog.Entry> rows=new TreeMap<>();String input=new String(bytes,StandardCharsets.UTF_8);
        if(kind==Kind.ZYPPER){
            try{XmlPullParser xml=xml(input);for(int event=xml.getEventType();event!=XmlPullParser.END_DOCUMENT;event=xml.next())if(event==XmlPullParser.START_TAG&&(xml.getName().equals("solvable")||xml.getName().equals("update")))add(rows,xml.getAttributeValue(null,"name"),xml.getAttributeValue(null,"edition"));}
            catch(Exception error){throw new IOException("PACKAGE_XML_INVALID",error);}
        }else for(String line:input.split("\n")){
            if(line.trim().isEmpty())continue;String[] f=line.trim().split("\\s+");String name=null,version=null;
            if(kind==Kind.DNF){String[] pair=line.split("\t",-1);if(pair.length!=2)throw new IOException("PACKAGE_METADATA_INVALID");name=pair[0];version=pair[1];}
            if(kind==Kind.PACMAN){if(tab==0&&f.length>=2){name=f[0];version=f[1];}else if(tab==1&&f.length>=3){name=f[1];version=f[2];}else if(tab==2&&f.length>=4&&f[2].equals("->")){name=f[0];version=f[3];}else throw new IOException("PACKAGE_METADATA_INVALID");}
            if(kind==Kind.XBPS&&tab==0&&!f[0].equals("ii"))continue;
            if(kind==Kind.APK||kind==Kind.XBPS){
                String pkg=kind==Kind.APK?f[0]:tab==2?(f.length>1&&f[1].equals("update")?f[0]:null):f.length>1?f[1]:null;
                if(pkg==null)continue;java.util.regex.Matcher m=java.util.regex.Pattern.compile(kind==Kind.XBPS?"^(.+)-([A-Za-z0-9][A-Za-z0-9._+~:]*_[0-9]+)$":"^(.+)-([0-9][A-Za-z0-9._+~:-]*)$").matcher(pkg);
                if(m.matches()){name=m.group(1);version=m.group(2);}else throw new IOException("PACKAGE_METADATA_INVALID");
            }
            if(name!=null)add(rows,name,version);
        }
        return new ArrayList<>(rows.values());
    }
    private static void add(Map<String,PackageCatalog.Entry> rows,String name,String version)throws IOException{
        if(!validName(name)||version==null||version.isEmpty()||version.length()>256||version.contains("\n")||version.contains("\t"))throw new IOException("PACKAGE_METADATA_INVALID");
        rows.putIfAbsent(name,new PackageCatalog.Entry(name,version));
    }
    private static XmlPullParser xml(String value)throws Exception{
        if(value.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))throw new IOException("PACKAGE_XML_DOCTYPE");
        XmlPullParser xml=android.util.Xml.newPullParser();xml.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);xml.setInput(new StringReader(value));return xml;
    }
    static String description(Kind kind,PackageCatalog.Entry entry,boolean installed){
        if(kind==Kind.APT)return PackageCatalog.descriptionQuery(entry,installed);
        if(!validName(entry.name))throw new IllegalArgumentException("INVALID_PACKAGE_NAME");String name=SessionShell.quote(entry.name);
        switch(kind){
            case APK:return "LC_ALL=C apk --no-network info --description -- "+name;
            case DNF:return "LC_ALL=C dnf --cacheonly info -- "+name;
            case ZYPPER:return "LC_ALL=C zypper --xmlout --no-refresh info --type package -- "+name;
            case XBPS:return xbps("LC_ALL=C xbps-query "+(installed?"":"-R ")+"-p short_desc -- "+name);
            case PACMAN:return "LC_ALL=C pacman "+(installed?"-Qi":"-Si")+" -- "+name;
            default:throw new IllegalArgumentException();
        }
    }
    static String parseDescription(Kind kind,byte[] bytes)throws IOException{
        if(kind==Kind.APT)return PackageCatalog.parseDescription(bytes);
        if(bytes.length>256*1024)throw new IOException("DESCRIPTION_TOO_LARGE");String value=new String(bytes,StandardCharsets.UTF_8).trim();
        if(kind!=Kind.ZYPPER)return value;
        try{XmlPullParser xml=xml(value);for(int e=xml.getEventType();e!=XmlPullParser.END_DOCUMENT;e=xml.next())if(e==XmlPullParser.START_TAG&&xml.getName().equals("description"))return xml.nextText().trim();return "";}catch(Exception error){throw new IOException("PACKAGE_XML_INVALID",error);}
    }
    static String operation(Kind kind,String action,String name){
        if(kind==Kind.APT)return PackageCatalog.operation(action,name);
        if(action.equals("refresh"))switch(kind){case APK:return "apk update";case DNF:return "dnf makecache";case ZYPPER:return "zypper refresh";case XBPS:return xbps("xbps-install -S");case PACMAN:return "pacman -Syu";default:throw new IllegalArgumentException();}
        if(!validName(name))throw new IllegalArgumentException("INVALID_PACKAGE_NAME");String pkg=SessionShell.quote(name);boolean install=action.equals("install"),upgrade=action.equals("upgrade"),remove=action.equals("remove");if(!install&&!upgrade&&!remove)throw new IllegalArgumentException();
        switch(kind){
            case APK:return "apk --interactive "+(remove?"del":upgrade?"upgrade":"add")+" -- "+pkg;
            case DNF:return "dnf "+(remove?"remove":upgrade?"upgrade":"install")+" -- "+pkg;
            case ZYPPER:return "zypper "+(remove?"remove":upgrade?"update":"install")+" -- "+pkg;
            case XBPS:return xbps((remove?"xbps-remove":upgrade?"xbps-install -u":"xbps-install")+" -- "+pkg);
            case PACMAN:return "pacman "+(remove?"-R":"-Syu")+" -- "+pkg;
            default:throw new IllegalArgumentException();
        }
    }
}
