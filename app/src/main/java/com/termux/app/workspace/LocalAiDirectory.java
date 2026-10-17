package com.termux.app.workspace;

import org.json.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Metadata, not guessed filenames. All paths come from the selected environment. */
final class LocalAiDirectory {
    static final int MAX_SCAN=2*1024*1024;
    static String checked(JSONObject r,String key,String fallback,String... values){String value=r.optString(key,fallback);for(String item:values)if(item.equals(value))return value;throw new IllegalArgumentException("Invalid "+key);}
    static boolean matches(String name,String filter){String n=name.toLowerCase(Locale.ROOT);if(filter.equals("all"))return true;if(filter.equals("text"))return ProjectFiles.mime(n).equals("text/plain")||n.matches(".*\\.(csv|ini|conf|cfg|sql|kt|kts|tsx|jsx|srt|vtt)$");if(filter.equals("image"))return n.matches(".*\\.(png|jpe?g|webp|gif|bmp|heic|heif)$");if(filter.equals("video"))return n.matches(".*\\.(mp4|mkv|webm|mov|m4v|3gp|avi)$");return n.endsWith(".apk");}
    static String listing(byte[] bytes,JSONObject request)throws Exception{
        String[] f=new String(bytes,StandardCharsets.UTF_8).split("\u0000",-1);if((f.length-1)%4!=0)throw new IOException("INVALID_LISTING: incomplete file metadata");List<JSONObject> all=new ArrayList<>();String filter=checked(request,"filter","all","all","text","image","video","apk");String sort=checked(request,"sort","modified_desc","modified_desc","name");
        for(int i=0;i+3<f.length;i+=4){String path=f[i+3];int at=path.lastIndexOf('/');String name=path.substring(at+1);if(request.has("extension")&&(!f[i+2].equals("f")||!name.toLowerCase(Locale.ROOT).endsWith("."+request.getString("extension").toLowerCase(Locale.ROOT))))continue;if(!matches(name,filter)||!filter.equals("all")&&!f[i+2].equals("f"))continue;String match=LocalAiNames.match(name,request);if(match.isEmpty())continue;double seconds=Double.parseDouble(f[i]);all.add(new JSONObject().put("name",name).put("match",match).put("path",path).put("modifiedMillis",(long)(seconds*1000)).put("size",Long.parseLong(f[i+1])).put("type",f[i+2].equals("d")?"directory":f[i+2].equals("f")?"file":"symlink"));}
        all.sort((a,b)->{if(sort.equals("modified_desc")){int result=Long.compare(b.optLong("modifiedMillis"),a.optLong("modifiedMillis"));if(result!=0)return result;}return a.optString("path").compareToIgnoreCase(b.optString("path"));});int limit=request.optInt("limit",50);if(limit<1||limit>200)throw new IllegalArgumentException("limit must be 1..200");JSONArray out=new JSONArray();for(int i=0;i<Math.min(limit,all.size());i++)out.put(all.get(i));return new JSONObject().put("checkedPath",request.optString("path")).put("notice","Only this directory was checked (and descendants only if recursive). No matches here does not mean the file is absent elsewhere. Entries contain metadata; use read for text contents.").put("sort",sort).put("filter",filter).put("queryVariants",new JSONArray(LocalAiNames.variants(request))).put("matchingNotice","Name variants are spelling/transliteration hints, not guaranteed translations. Approximate or multiple file matches require user selection.").put("matched",all.size()).put("truncated",all.size()>limit).put("entries",out).toString();
    }
    static List<String> knownPaths(WorkspaceStore.Session s){
        LinkedHashSet<String> paths=new LinkedHashSet<>();paths.add(s.localProject);
        for(String base: new String[]{"/sdcard","/storage/emulated/0","/storage/self/primary",com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH+"/storage/shared"}){
            paths.add(base);for(String child:new String[]{"Download","Downloads","Pictures/Screenshots","DCIM/Screenshots","Screenshots","Pictures/ScreenShots","DCIM/ScreenShots","Pictures","DCIM","Documents","Movies","Music"})paths.add(base+"/"+child);
        }
        paths.add(com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH);paths.add("/root");paths.add("/home");return new ArrayList<>(paths);
    }
    static List<String> screenshotPaths(WorkspaceStore.Session s){
        List<String> out=new ArrayList<>();for(String path:knownPaths(s))if(path.toLowerCase(Locale.ROOT).endsWith("/screenshots"))out.add(path);return out;
    }
    static String screenshotScript(WorkspaceStore.Session s,String pre,JSONObject request){
        List<String> paths=request.has("path")?Collections.singletonList(request.optString("path")):screenshotPaths(s);
        StringBuilder script=new StringBuilder(pre);
        for(String path:paths){
            if(path.isEmpty()||path.indexOf('\0')>=0||path.contains("\n")||path.contains("\r"))throw new IllegalArgumentException("Invalid path");
            script.append("candidate=").append(SessionShell.quote(path)).append("; target=$(realpath -m -- \"$candidate\") || fail PATH_RESOLUTION_FAILED; ");
            // Scope is checked before probing anything. find -P never follows nested symlinks.
            if(s.localCheckPaths)script.append("case \"$target\" in \"$root\"|\"$root\"/*) allowed=true;; *) allowed=false;; esac; ");else script.append("allowed=true; ");
            script.append("status=AVAILABLE; if [ \"$allowed\" = false ]; then status=OUTSIDE_PROJECT; else probe=\"$target\"; while [ ! -e \"$probe\" ] && [ \"$probe\" != / ]; do probe=$(dirname -- \"$probe\"); done; if [ ! -x \"$probe\" ]; then status=ACCESS_DENIED; elif [ ! -d \"$target\" ]; then status=NOT_FOUND; elif [ ! -r \"$target\" ] || [ ! -x \"$target\" ]; then status=ACCESS_DENIED; fi; fi; printf '0\\0' ; printf '0\\0%s\\0%s\\0' \"$status\" \"$candidate\"; if [ \"$status\" = AVAILABLE ]; then find -P \"$target\" -mindepth 1 -maxdepth 2 -type f \\( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' -o -iname '*.gif' -o -iname '*.bmp' -o -iname '*.heic' -o -iname '*.heif' \\) -printf '%T@\\0%s\\0f:%D:%i\\0%p\\0' || fail SCREENSHOT_SCAN_FAILED; fi; ");
        }
        return script.toString();
    }
    static String screenshots(byte[] bytes,JSONObject request)throws Exception{
        String[] fields=new String(bytes,StandardCharsets.UTF_8).split("\u0000",-1);if((fields.length-1)%4!=0)throw new IOException("INVALID_LISTING: incomplete screenshot metadata");
        ByteArrayOutputStream metadata=new ByteArrayOutputStream();JSONArray checked=new JSONArray();Set<String> seen=new HashSet<>();boolean available=false;
        for(int i=0;i+3<fields.length;i+=4){
            if(!fields[i+2].equals("f")&&!fields[i+2].startsWith("f:")){String status=fields[i+2];available|=status.equals("AVAILABLE");checked.put(new JSONObject().put("path",fields[i+3]).put("status",status));continue;}
            String identity=fields[i+2].startsWith("f:")?fields[i+2]:fields[i+3];
            if(seen.add(identity))for(int j=0;j<4;j++){metadata.write((j==2?"f":fields[i+j]).getBytes(StandardCharsets.UTF_8));metadata.write(0);}
        }
        JSONObject options=new JSONObject(request.toString()).put("filter","image").put("sort","modified_desc");JSONObject result=new JSONObject(listing(metadata.toByteArray(),options));
        return result.put("checkedLocations",checked).put("status",result.getInt("matched")>0?"FOUND":available?"NO_MATCHES_IN_CHECKED_LOCATIONS":"NO_ACCESSIBLE_SCREENSHOT_DIRECTORY").put("notice","Only checked locations were scanned. Missing folders, denied access and project restrictions are listed separately. Images were located, not visually inspected.").toString();
    }
    static String locationsScript(WorkspaceStore.Session s,String pre){
        List<String> paths=knownPaths(s);StringBuilder script=new StringBuilder(pre);
        for(String path:new LinkedHashSet<>(paths)){script.append("candidate=").append(SessionShell.quote(path)).append("; target=$(realpath -m -- \"$candidate\") || exit 2; ");if(s.localCheckPaths)script.append("case \"$target\" in \"$root\"|\"$root\"/*) ;; *) printf 'OUTSIDE_PROJECT\\t%s\\n' \"$candidate\"; continue_location=true;; esac; if [ \"${continue_location:-false}\" = true ]; then unset continue_location; else ");script.append("if [ -d \"$target\" ]; then if [ -r \"$target\" ] && [ -x \"$target\" ]; then printf 'AVAILABLE\\t%s\\n' \"$target\"; else printf 'ACCESS_DENIED\\t%s\\n' \"$candidate\"; fi; else printf 'NOT_FOUND\\t%s\\n' \"$candidate\"; fi; ");if(s.localCheckPaths)script.append("fi; ");}return script.toString();
    }
}
