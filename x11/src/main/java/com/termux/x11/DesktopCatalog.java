package com.termux.x11;

import android.os.Bundle;
import org.json.JSONObject;
import java.util.*;

/** Bounded metadata only. Only entries from a completed OS-catalog job may be installed. */
final class DesktopCatalog {
    static final int PAGE=30,MAX=100000;
    static boolean packageName(String value){return value!=null&&value.matches("[A-Za-z0-9][A-Za-z0-9+_.-]{0,127}");}
    static final class Entry {
        final String pkg,name,summary,search;final boolean installed,available,snap;
        Entry(String pkg,String name,String summary,boolean installed,boolean available,boolean snap){this.pkg=pkg;this.name=name;this.summary=summary;this.installed=installed;this.available=available;this.snap=snap;search=(name+" "+pkg+" "+summary).toLowerCase(Locale.ROOT);}
        Bundle bundle(){Bundle b=new Bundle();b.putString("package",pkg);b.putString("name",name);b.putString("summary",summary);b.putBoolean("installed",installed);b.putBoolean("available",available);b.putBoolean("snap",snap);return b;}
    }
    final List<Entry> entries;
    DesktopCatalog(List<Entry> entries){this.entries=Collections.unmodifiableList(new ArrayList<>(entries));}
    boolean containsInstalled(String pkg){for(Entry e:entries)if(e.pkg.equals(pkg))return e.installed;return false;}
    boolean allows(String pkg){for(Entry e:entries)if(e.pkg.equals(pkg))return e.available;return false;}
    DesktopCatalog installed(String pkg){List<Entry> next=new ArrayList<>();for(Entry e:entries)next.add(e.pkg.equals(pkg)?new Entry(e.pkg,e.name,e.summary,true,e.available,e.snap):e);return new DesktopCatalog(next);}
    Bundle page(String query,int offset){
        if(query==null||query.length()>200||offset<0||offset>MAX)throw new IllegalArgumentException("Invalid catalog page");
        String[] words=query.trim().toLowerCase(Locale.ROOT).split("\\s+");List<Entry> matches=new ArrayList<>();
        outer:for(Entry e:entries){for(String word:words)if(!e.search.contains(word))continue outer;matches.add(e);}
        ArrayList<Bundle> rows=new ArrayList<>();for(int i=offset;i<Math.min(matches.size(),offset+PAGE);i++)rows.add(matches.get(i).bundle());
        Bundle b=new Bundle();b.putString("query",query);b.putInt("offset",offset);b.putInt("total",matches.size());b.putParcelableArrayList("rows",rows);return b;
    }
    static final class Parser {
        private final boolean allowEmpty;
        Parser(){this(false);}
        Parser(boolean allowEmpty){this.allowEmpty=allowEmpty;}
        private final StringBuilder line=new StringBuilder();private boolean overflow,invalid,ready,exited;boolean missingTools,empty;
        private final Map<String,Entry> entries=new LinkedHashMap<>();
        void feed(String chunk){for(int i=0;i<chunk.length();i++){char c=chunk.charAt(i);if(c=='\n'||c=='\r'){if(!overflow)parse(line.toString());line.setLength(0);overflow=false;}else if(!overflow){if(line.length()<8192)line.append(c);else{line.setLength(0);overflow=true;invalid=true;}}}}
        private void parse(String value){
            if(value.equals("ERROR:CATALOG_TOOLS_REQUIRED"))missingTools=true;
            if(value.equals("ERROR:CATALOG_METADATA_EMPTY"))empty=true;
            if(value.equals("READY:catalog"))ready=true;if(value.equals("DESKTOP_CHILD_EXIT=0"))exited=true;
            if(!value.startsWith("CATALOG:"))return;
            try{JSONObject j=new JSONObject(value.substring(8));String pkg=j.getString("package"),name=j.getString("name"),summary=j.getString("summary");
                if(!packageName(pkg)||name.isEmpty()||name.length()>128||summary.length()>500||entries.containsKey(pkg)||entries.size()>=MAX)throw new IllegalArgumentException();
                entries.put(pkg,new Entry(pkg,name,summary,j.getBoolean("installed"),j.getBoolean("available"),j.optBoolean("snap",false)));
            }catch(Exception e){invalid=true;}
        }
        DesktopCatalog result(){if(invalid||!ready||!exited||(!allowEmpty&&entries.isEmpty()))return null;return new DesktopCatalog(new ArrayList<>(entries.values()));}
    }
}
