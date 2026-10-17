package com.termux.app.workspace;

import java.util.*;
import java.text.Normalizer;
import org.json.*;

/** Filename hints only: no invented translations, shell interpolation or path rewriting. */
final class LocalAiNames {
    static List<String> variants(JSONObject request){
        LinkedHashSet<String> out=new LinkedHashSet<>();String q=request.optString("query","").trim();if(!q.isEmpty())out.add(q);
        JSONArray extra=request.optJSONArray("variants");if(extra!=null)for(int i=0;i<extra.length();i++){String v=extra.optString(i,"").trim();if(!v.isEmpty())out.add(v);}
        return new ArrayList<>(out);
    }
    static String phonetic(String value){
        String letters="абвгдеёжзийклмнопрстуфхцчшщъыьэюя";String[] latin={"a","b","v","g","d","e","e","zh","z","i","i","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch","","y","","e","yu","ya"};
        String text=Normalizer.normalize(value.toLowerCase(Locale.ROOT),Normalizer.Form.NFKD).replaceAll("\\p{M}","");StringBuilder out=new StringBuilder();for(int i=0;i<text.length();i++){char c=text.charAt(i);int at=letters.indexOf(c);out.append(at<0?String.valueOf(c):latin[at]);}
        return out.toString().replace("ph","f").replace("ck","k").replace('c','k').replace('q','k').replace('y','i').replaceAll("[^a-z0-9]","").replaceAll("([a-z])\\1+","$1");
    }
    static String match(String name,JSONObject request){
        List<String> variants=variants(request);if(variants.isEmpty())return "name";
        String lower=name.toLowerCase(Locale.ROOT);for(String query:variants)if(lower.contains(query.toLowerCase(Locale.ROOT)))return "name";
        if(request.optString("tool").matches("search|list")){String n=phonetic(name);for(String query:variants){String q=phonetic(query);if(!q.isEmpty()&&n.contains(q))return "name_variant";}}
        return "";
    }
}
