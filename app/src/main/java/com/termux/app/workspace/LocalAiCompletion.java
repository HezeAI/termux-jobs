package com.termux.app.workspace;

import org.json.*;
import java.util.*;

/** Conservative wording/evidence check for one turn, not a semantic verifier or executor. */
final class LocalAiCompletion {
    private final Set<String> successful=new HashSet<>();
    void record(JSONObject request,String result){if(result.startsWith("exit=0\n"))successful.add(request.optString("tool"));}
    private static String text(String value){return LocalAiText.split(value).answer.toLowerCase(Locale.ROOT);}
    private static boolean word(String value,String pattern){return value.matches("(?isu).*\\b(?:"+pattern+")\\b.*");}
    static String task(JSONArray history){for(int i=history.length()-1;i>=0;i--){JSONObject m=history.optJSONObject(i);if(m!=null&&m.optString("role").equals("user")&&!m.has("toolResult")&&!m.optBoolean("appFeedback"))return m.optString("text");}return "";}
    String missing(String answer,String task,boolean examples){
        if(examples)return "";String a=text(answer),request=text(task);boolean fileScope=word(request+" "+a,"файл[а-яё]*|file|files|документ[а-яё]*|document")||request.contains("/");Set<String> claims=new LinkedHashSet<>();
        // Ignore negative, hypothetical and instructional sentences. This is deliberately
        // narrower than arbitrary natural-language entailment; no hidden model calls.
        for(String sentence:a.split("[!?\\n]+|(?<=[a-zа-яё])[.]\\s+")){
            sentence=sentence.trim().replaceFirst("[.]+$", "");
            if(word(sentence,"не|нет|not|never|cannot|can't|если|if|можно|would|could|will|будет|буду|нужно|надо|should|must"))continue;
            if(word(sentence,"сохран[её]н(?:а|о|ы)?|записан(?:а|о|ы)?|создан(?:а|о|ы)?|сохранил[аи]?|записал[аи]?|создал[аи]?"))claims.add("write");
            if(word(sentence,"прочитан(?:а|о|ы)?|прочитал[аи]?"))claims.add("read");
            if(word(sentence,"удал[её]н(?:а|о|ы)?|удалил[аи]?"))claims.add("delete");
            if(word(sentence,"установлен(?:а|о|ы)?|установил[аи]?"))claims.add("install");
            if(sentence.matches("(?s).*(?:\\bi (?:have |already )?(?:successfully )?|\\b(?:file|document|contents?)\\b.{0,100}\\b(?:was |were |has been |is |successfully )?)(?:saved|written|created)\\b.*"))claims.add("write");
            if(sentence.matches("(?s).*\\bi (?:have |already )?(?:successfully )?read\\b.*")||sentence.matches("(?s).*\\b(?:file|document|contents?)\\b.{0,100}\\b(?:was |has been |successfully )read\\b.*"))claims.add("read");
            if(sentence.matches("(?s).*\\b(?:i (?:have )?|file.{0,100})(?:deleted|removed)\\b.*"))claims.add("delete");
            if(sentence.matches("(?s).*\\b(?:i (?:have )?|package.{0,100})(?:installed)\\b.*"))claims.add("install");
            if(word(sentence,"задача выполнена|всё выполнено|все выполнено|task completed|all done")||sentence.trim().matches("(?:done|готово|выполнено)")){
                if(word(request,"запиши|сохрани|создай|write|save|create"))claims.add("write");
                if(word(request,"прочитай|прочитать|read"))claims.add("read");
                if(word(request,"удали|delete|remove"))claims.add("delete");
                if(word(request,"установи|install"))claims.add("install");
            }
        }
        // Check only explicitly requested operation kinds. A statement about an
        // existing file is not by itself a claim of a new mutation this turn.
        Set<String> requested=new HashSet<>();
        if(word(request,"запиши|сохрани|создай|write|save|create"))requested.add("write");
        if(word(request,"прочитай|прочитать|read"))requested.add("read");
        if(word(request,"удали|delete|remove"))requested.add("delete");
        if(word(request,"установи|install"))requested.add("install");
        claims.retainAll(requested);
        if(!fileScope){claims.remove("write");claims.remove("read");claims.remove("delete");}
        // A successful arbitrary shell command may itself perform these operations.
        // Do not misclassify it without interpreting its semantics (not done here).
        if(successful.contains("command"))return "";
        claims.removeIf(kind->successful.contains(kind.equals("delete")?"command":kind));
        return String.join(",",claims);
    }
    static String correction(String missing){return "\nCOMPLETION_EVIDENCE_MISSING: No successful current-turn tool result supports these claimed operations: "+missing+". Earlier turns and file existence are not evidence of this turn's actions. If the user requested the operation and current permissions allow it, issue the proper tool call and check the result. Otherwise accurately report what was and was not done. Do not repeat completed mutations, invent commands, expand the task, change permissions or claim success without evidence.";}
}
