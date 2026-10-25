package com.termux.x11;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.regex.Pattern;

/** Bounded, redacted transfer failures only; not a terminal transcript. */
public final class X11InstallErrors {
    private static final int LINE_LIMIT=8192, MAX_ERRORS=12;
    private static final Pattern ANSI=Pattern.compile("\\x1b(?:\\[[0-?]*[ -/]*[@-~]|\\][^\\x07\\x1b]*(?:\\x07|\\x1b\\\\))");
    private static final Pattern URL=Pattern.compile("(?i)\\b[a-z][a-z0-9+.-]*://[^\\s'\"`<>]+|\\bwww\\.[^\\s'\"`<>]+");
    private static final Pattern PRIVATE_VALUE=Pattern.compile("(?i)\\b(?:authorization|proxy-authorization|cookie|set-cookie|password|passwd|passphrase|token|access[_-]?token|refresh[_-]?token|api[_-]?key|secret|credential|client[_-]?secret)\\b\\s*[:= ]\\s*.*");
    private static final Pattern QUOTED=Pattern.compile("`[^'`]*['`]|'[^']*'|\"[^\"]*\"");
    private static final Pattern PATH=Pattern.compile("(?:[A-Za-z]:[\\\\/]|(?<![A-Za-z0-9])(?:~?/|\\.\\.?/))[^\\s<>]+|\\b[A-Za-z0-9_.-]+/[A-Za-z0-9_./-]+");
    private static final Pattern ADDRESS=Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+|\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b|\\b(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\\b|(?i)(?:[0-9a-f]{0,4}:){2,}[0-9a-f:]*");
    private static final Pattern OPAQUE=Pattern.compile("[A-Za-z0-9_+=.-]{32,}|\\b(?:gh[p]_|github[_]pat[_]|sk-)[A-Za-z0-9_-]+|[?&][^\\s]+|\\b[A-Za-z_][A-Za-z0-9_-]*=[^\\s]+");
    private final StringBuilder pending=new StringBuilder();
    private final ArrayDeque<String> errors=new ArrayDeque<>();
    private boolean overflow;

    public synchronized void feed(String chunk){
        for(int i=0;i<chunk.length();i++){
            char c=chunk.charAt(i);
            if(c=='\n'||c=='\r'){
                if(overflow){if(relevant(pending.toString()))add("TRANSFER_ERROR: line exceeded diagnostic limit; text omitted");}
                else line(pending.toString());
                pending.setLength(0);overflow=false;
            }else if(pending.length()<LINE_LIMIT)pending.append(c);else overflow=true;
        }
    }
    public synchronized void finish(){feed("\n");}
    private static boolean relevant(String text){
        String s=text.toLowerCase(Locale.ROOT);
        return ((s.contains("download")||s.contains("fetch")||s.contains("retriev"))&&(s.contains("failed")||s.contains("error")||s.contains("unable")||s.contains("cannot")))||s.contains("curl error")||s.startsWith("curl:");
    }
    private void line(String raw){
        String text=ANSI.matcher(raw).replaceAll("").replaceAll("[\\p{Cc}\\p{Cf}]", " ").trim();
        if(!relevant(text))return;
        add(redact(text));
    }
    /** Shared redaction for bounded diagnostic failures, never general program output. */
    static String redact(String text){
        text=ANSI.matcher(text).replaceAll("").replaceAll("[\\p{Cc}\\p{Cf}]", " ").trim();
        text=URL.matcher(text).replaceAll("[url]");
        text=PRIVATE_VALUE.matcher(text).replaceAll("[credential redacted]");
        text=QUOTED.matcher(text).replaceAll("[value]");
        // Preserve HTTP protocol versions; they are not filesystem paths.
        text=text.replaceAll("(?i)HTTP/([0-9.]+)","HTTP-$1");
        text=PATH.matcher(text).replaceAll("[path]");
        text=ADDRESS.matcher(text).replaceAll("[address]");
        text=OPAQUE.matcher(text).replaceAll("[value]");
        text=text.replaceAll("\\s+"," ").trim();
        if(text.length()>1024)text=text.substring(0,480)+" [middle omitted] "+text.substring(text.length()-480);
        return text;
    }
    private void add(String text){if(errors.contains(text))return;if(errors.size()==MAX_ERRORS)errors.removeFirst();errors.addLast(text);}
    public synchronized String snapshot(){return String.join("\n",errors);}
}
