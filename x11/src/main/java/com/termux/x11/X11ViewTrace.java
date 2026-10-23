package com.termux.x11;

import android.os.SystemClock;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

/** View-lifetime metadata only; records callbacks without posts, timers or invalidations. */
final class X11ViewTrace {
    private final long started=SystemClock.uptimeMillis();
    private final ArrayDeque<String> recent=new ArrayDeque<>();
    private final Map<String,Integer> totals=new LinkedHashMap<>();
    void event(String kind,String value){
        totals.put(kind,totals.getOrDefault(kind,0)+1);
        if(recent.size()==24)recent.removeFirst();
        recent.addLast((SystemClock.uptimeMillis()-started)+"ms "+kind+" "+value);
    }
    String report(){StringBuilder s=new StringBuilder("Android view counters: ").append(totals);for(String line:recent)s.append('\n').append(line);return s.toString();}
}
