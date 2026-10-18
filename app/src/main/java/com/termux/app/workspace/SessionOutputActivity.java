package com.termux.app.workspace;

/** A redraw after viewport interaction must not start the output activity cue. */
final class SessionOutputActivity {
    private long ignoreUntil;
    private final java.util.LinkedHashMap<String,Long> resizeIgnore=new java.util.LinkedHashMap<>();
    private final java.util.LinkedHashMap<String,Long> output=new java.util.LinkedHashMap<>();
    void viewport(long now){ignoreUntil=now+1600;output.clear();resizeIgnore.clear();}
    // A keyboard resize or scroll must not erase ongoing work or extend its lifetime.
    void resize(String handle,long now){
        if(active(handle,now))return;
        output.remove(handle);resizeIgnore.put(handle,now+1600);
        while(resizeIgnore.size()>64)resizeIgnore.remove(resizeIgnore.keySet().iterator().next());
    }
    void output(String handle,long now){
        if(now<ignoreUntil||now<resizeIgnore.getOrDefault(handle,0L))return;
        resizeIgnore.remove(handle);
        output.put(handle,now);
        while(output.size()>64)output.remove(output.keySet().iterator().next());
    }
    boolean active(String handle,long now){Long last=output.get(handle);return last!=null&&now-last<1600;}
}
