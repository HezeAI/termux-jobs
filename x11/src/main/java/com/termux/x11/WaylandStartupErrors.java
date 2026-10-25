package com.termux.x11;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;

/** First and recent failures of the private KDE launch only, never a terminal transcript. */
public final class WaylandStartupErrors {
    private final StringBuilder pending=new StringBuilder();
    private final ArrayList<String> first=new ArrayList<>();
    private final ArrayDeque<String> recent=new ArrayDeque<>();
    private boolean overflow;
    public synchronized void feed(String text){
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(c=='\n'||c=='\r'){
                if(!overflow)line(pending.toString());
                pending.setLength(0);overflow=false;
            }else if(!overflow){if(pending.length()<8192)pending.append(c);else{pending.setLength(0);overflow=true;}}
        }
    }
    private void line(String text){
        String s=text.toLowerCase(Locale.ROOT);
        // Input-context frames, environment dumps and ordinary program output are excluded.
        if(s.startsWith("jobs_")||s.contains("private key")||s.contains("environment variable"))return;
        boolean failure=s.contains("failed")||s.contains("error")||s.contains("could not")||s.contains("cannot")||s.contains("permission denied")||s.contains("unknown option")||s.contains("no such file")||s.contains("segmentation fault")||s.contains("received signal");
        boolean source=s.contains("kwin")||s.contains("wayland")||s.contains("startplasma")||s.contains("plasma_session")||s.contains("qt.qpa")||s.contains("dbus")||s.contains("d-bus")||s.contains("permission denied")||s.contains("egl")||s.contains("gbm")||s.contains("vulkan")||s.startsWith("(ee)")||s.contains("error while loading shared libraries")||s.contains("symbol lookup error");
        if(!failure||!source)return;
        String safe=X11InstallErrors.redact(text);
        if(safe.isEmpty()||first.contains(safe)||recent.contains(safe))return;
        if(first.size()<8)first.add(safe);
        else{if(recent.size()==8)recent.removeFirst();recent.addLast(safe);}
    }
    public synchronized String snapshot(){
        String text=String.join("\n",first);
        return recent.isEmpty()?text:text+"\n[recent failures]\n"+String.join("\n",recent);
    }
}
