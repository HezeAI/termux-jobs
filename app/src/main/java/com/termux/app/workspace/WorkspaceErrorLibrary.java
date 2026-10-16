package com.termux.app.workspace;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.URI;
import java.text.Normalizer;
import java.util.*;

/** Offline, curated documentation. Never reads a terminal, runs a command or requests a URL. */
final class WorkspaceErrorLibrary {
    static final String ASSET="workspace-error-library-v1.json";
    static final String[] ENV={"all","termux","ubuntu","debian","arch","alpine","fedora","opensuse","other"};
    static final String[] CATEGORIES={"all","packages","files","launch","network","build","git","environment","python","node"};
    static final String[] CONTEXTS={"all","proot","native","ssh"};
    static final class Entry {
        final JSONObject data;
        final String id,category,context,command,index;
        final Set<String> environments=new HashSet<>();
        Entry(JSONObject item)throws JSONException {
            data=item;id=item.getString("id");category=item.getString("category");context=item.getString("context");command=item.getString("command");
            if(!id.matches("[a-z0-9-]{1,80}")||!Arrays.asList(CATEGORIES).contains(category)||!Arrays.asList(CONTEXTS).contains(context)||command.length()>4096||command.indexOf('\0')>=0)throw new JSONException("INVALID_ENTRY");
            JSONArray env=item.getJSONArray("environments");if(env.length()==0)throw new JSONException("MISSING_ENVIRONMENT");
            for(int i=0;i<env.length();i++){String key=env.getString(i);if(key.equals("all")||!Arrays.asList(ENV).contains(key))throw new JSONException("INVALID_ENVIRONMENT");environments.add(key);}
            StringBuilder searchable=new StringBuilder(id).append(' ');
            for(String key:new String[]{"title","symptom","check","solution","caution"})for(String lang:new String[]{"ru","en"}){String value=item.getJSONObject(key).getString(lang);if(value.trim().isEmpty()||value.length()>6000)throw new JSONException("INVALID_TEXT");searchable.append(value).append(' ');}
            if(!item.getString("reviewed").matches("\\d{4}-\\d{2}-\\d{2}"))throw new JSONException("INVALID_DATE");
            JSONArray sources=item.getJSONArray("sources");if(sources.length()==0||sources.length()>8)throw new JSONException("INVALID_SOURCES");
            for(int i=0;i<sources.length();i++){JSONObject source=sources.getJSONObject(i);if(!safeUrl(source.getString("url"))||!Arrays.asList("official","community","project").contains(source.getString("kind")))throw new JSONException("INVALID_SOURCE");}
            boolean project=false;for(int i=0;i<sources.length();i++)project|=sources.getJSONObject(i).getString("kind").equals("project");
            if(project||item.has("projectStatus")||item.has("projectEvidence")){
                if(!Arrays.asList("resolved","verified-code","open","guidance").contains(item.getString("projectStatus")))throw new JSONException("INVALID_PROJECT_STATUS");
                for(String lang:new String[]{"ru","en"}){String evidence=item.getJSONObject("projectEvidence").getString(lang);if(evidence.trim().isEmpty()||evidence.length()>6000)throw new JSONException("INVALID_PROJECT_EVIDENCE");searchable.append(evidence).append(' ');}
                searchable.append(" Termux Job's опыт разработки project experience ");
            }
            index=normalize(searchable.toString());
        }
        String text(String key,boolean ru){return data.optJSONObject(key).optString(ru?"ru":"en");}
        boolean matches(String query,String env,String cat,String mode){
            if(!env.equals("all")&&!environments.contains(env)||!cat.equals("all")&&!category.equals(cat)||!mode.equals("all")&&!context.equals("all")&&!context.equals(mode))return false;
            for(String token:normalize(query).trim().split("\\s+"))if(!index.contains(token))return false;return true;
        }
    }
    final String version;
    final List<Entry> entries;
    private WorkspaceErrorLibrary(String version,List<Entry> entries){this.version=version;this.entries=Collections.unmodifiableList(entries);}
    static boolean safeUrl(String value){try{URI uri=new URI(value);return value.length()<=2048&&"https".equals(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null&&uri.getPort()==-1;}catch(Exception e){return false;}}
    static String normalize(String value){return Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('ё','е');}
    static WorkspaceErrorLibrary load(Context context)throws IOException,JSONException {
        try(InputStream stream=context.getAssets().open(ASSET)){return parse(stream);}
    }
    static WorkspaceErrorLibrary parse(InputStream stream)throws IOException,JSONException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=stream.read(buffer))!=-1){if(out.size()+n>512*1024)throw new IOException("CATALOG_TOO_LARGE");out.write(buffer,0,n);}
        JSONObject root=new JSONObject(out.toString("UTF-8"));if(root.getInt("schema")!=1)throw new JSONException("UNSUPPORTED_SCHEMA");
        JSONArray rows=root.getJSONArray("entries");if(rows.length()>3000)throw new JSONException("TOO_MANY_ENTRIES");List<Entry> entries=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int i=0;i<rows.length();i++){Entry entry=new Entry(rows.getJSONObject(i));if(!ids.add(entry.id))throw new JSONException("DUPLICATE_ID");entries.add(entry);}
        return new WorkspaceErrorLibrary(root.getString("version"),entries);
    }
    List<Entry> search(String query,String env,String category,String mode){List<Entry> result=new ArrayList<>();for(Entry e:entries)if(e.matches(query,env,category,mode))result.add(e);return result;}
}
