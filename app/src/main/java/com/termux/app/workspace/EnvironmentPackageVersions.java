package com.termux.app.workspace;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.xmlpull.v1.XmlPullParser;

/** Available versions from enabled, cached repositories; never refreshes or installs. */
final class EnvironmentPackageVersions {
    static final int MAX_BYTES=1024*1024;
    static final class Version {
        final String name,version,architecture,repository;
        Version(String name,String version,String architecture,String repository){
            this.name=name;this.version=version;this.architecture=architecture;this.repository=repository;
        }
        String label(){return version+(architecture.isEmpty()?"":" · "+architecture)+"\n"+repository;}
    }
    static boolean supported(EnvironmentPackages.Kind kind){
        return kind==EnvironmentPackages.Kind.APK||kind==EnvironmentPackages.Kind.DNF||kind==EnvironmentPackages.Kind.ZYPPER;
    }
    private static boolean validVersion(String value){return value!=null&&value.length()<=256&&value.matches("[A-Za-z0-9][A-Za-z0-9.+_:~%\\-]*");}
    private static boolean validRepository(String value){return value!=null&&value.length()<=200&&value.matches("[A-Za-z0-9_][A-Za-z0-9_.:+\\-]*");}
    private static boolean validZypperRepository(String value){return value!=null&&value.length()<=200&&value.matches("[\\p{L}\\p{N}_][\\p{L}\\p{N}_.:+() \\-]*");}
    private static boolean validArch(String value){return value!=null&&value.length()<=64&&value.matches("[A-Za-z0-9][A-Za-z0-9_\\-]*")&&!value.equals("src")&&!value.equals("nosrc");}
    static String query(EnvironmentPackages.Kind kind,String name){
        if(!supported(kind)||!EnvironmentPackages.validName(name))throw new IllegalArgumentException("INVALID_PACKAGE_NAME");
        String pkg=SessionShell.quote(name);
        switch(kind){
            case APK:return "LC_ALL=C apk --no-network policy -- "+pkg;
            case DNF:return "LC_ALL=C dnf --quiet --cacheonly repoquery --available --queryformat '%{name}\t%{evr}\t%{arch}\t%{repoid}\\n' -- "+pkg;
            case ZYPPER:return "LC_ALL=C zypper --xmlout --no-refresh search --details --type package --match-exact -- "+pkg;
            default:throw new IllegalArgumentException();
        }
    }
    static List<Version> parse(EnvironmentPackages.Kind kind,String name,byte[] bytes)throws IOException{
        if(!supported(kind)||!EnvironmentPackages.validName(name)||bytes.length>MAX_BYTES)throw invalid();
        LinkedHashMap<String,Version> rows=new LinkedHashMap<>();
        String text=new String(bytes,StandardCharsets.UTF_8);
        if(kind==EnvironmentPackages.Kind.ZYPPER){
            if(text.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))throw invalid();
            try{
                XmlPullParser xml=android.util.Xml.newPullParser();xml.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);xml.setInput(new java.io.StringReader(text));
                for(int event=xml.getEventType();event!=XmlPullParser.END_DOCUMENT;event=xml.next()){
                    if(event!=XmlPullParser.START_TAG||!xml.getName().equals("solvable"))continue;
                    String actual=xml.getAttributeValue(null,"name");if(!name.equals(actual))continue;
                    String repo=xml.getAttributeValue(null,"repository"),arch=xml.getAttributeValue(null,"arch");
                    if("(System Packages)".equals(repo)||"@System".equals(repo)||"src".equals(arch)||"nosrc".equals(arch))continue;
                    add(rows,kind,new Version(actual,xml.getAttributeValue(null,"edition"),arch,repo));
                }
            }catch(Exception error){throw new IOException("PACKAGE_VERSIONS_INVALID",error);}
        }else if(kind==EnvironmentPackages.Kind.DNF){
            for(String line:text.split("\n")){
                if(line.isEmpty())continue;String[] f=line.split("\t",-1);
                if(f.length!=4)throw invalid();
                if(!name.equals(f[0])||f[2].equals("src")||f[2].equals("nosrc"))continue;
                add(rows,kind,new Version(f[0],f[1],f[2],f[3]));
            }
        }else{
            String version=null;boolean target=false;
            for(String line:text.split("\n")){
                if(line.trim().isEmpty())continue;
                if(!line.startsWith(" ")){target=line.equals(name+" policy:");version=null;continue;}
                if(!target)continue;
                if(line.startsWith("  ")&&!line.startsWith("    ")&&line.endsWith(":")){
                    version=line.trim();version=version.substring(0,version.length()-1);
                    if(!validVersion(version))throw invalid();
                }else if(line.startsWith("    ")){
                    String repo=line.trim();
                    // An installed-only version cannot be downloaded from a repository.
                    if(repo.equals("lib/apk/db/installed")||repo.equals("/lib/apk/db/installed")||repo.equals("usr/lib/apk/db/installed")||repo.equals("/usr/lib/apk/db/installed"))continue;
                    if(version==null)throw invalid();
                    add(rows,kind,new Version(name,version,"",safeRepositoryUrl(repo)));
                }else throw invalid();
            }
        }
        return new ArrayList<>(rows.values());
    }
    private static String safeRepositoryUrl(String value)throws IOException{
        try{
            URI uri=new URI(value);
            if(value.length()>2048||!("https".equals(uri.getScheme())||"http".equals(uri.getScheme()))||uri.getHost()==null||uri.getRawPath().contains("\n"))throw invalid();
            // Credentials and query strings must never appear in the version picker.
            return new URI(uri.getScheme(),null,uri.getHost(),uri.getPort(),uri.getPath(),null,null).toASCIIString();
        }catch(java.net.URISyntaxException error){throw invalid();}
    }
    private static void add(Map<String,Version> rows,EnvironmentPackages.Kind kind,Version value)throws IOException{
        if(!EnvironmentPackages.validName(value.name)||!validVersion(value.version))throw invalid();
        if(kind==EnvironmentPackages.Kind.APK){if(value.repository==null||!value.architecture.isEmpty())throw invalid();}
        else if(!validArch(value.architecture)||!(kind==EnvironmentPackages.Kind.ZYPPER?validZypperRepository(value.repository):validRepository(value.repository)))throw invalid();
        rows.putIfAbsent(value.version+"\t"+value.architecture+"\t"+value.repository,value);
        if(rows.size()>1000)throw invalid();
    }
    static String operation(EnvironmentPackages.Kind kind,Version value){
        try{if(!supported(kind))throw invalid();add(new HashMap<>(),kind,value);}catch(IOException error){throw new IllegalArgumentException("INVALID_PACKAGE_VERSION",error);}
        switch(kind){
            case APK:return "apk --interactive add -- "+SessionShell.quote(value.name+"="+value.version);
            case DNF:return "dnf install --from-repo="+SessionShell.quote(value.repository)+" -- "+SessionShell.quote(value.name+"-"+value.version+"."+value.architecture);
            case ZYPPER:return "zypper install --oldpackage --from "+SessionShell.quote(value.repository)+" -- "+SessionShell.quote(value.name+"."+value.architecture+"="+value.version);
            default:throw new IllegalArgumentException();
        }
    }
    private static IOException invalid(){return new IOException("PACKAGE_VERSIONS_INVALID");}
}
