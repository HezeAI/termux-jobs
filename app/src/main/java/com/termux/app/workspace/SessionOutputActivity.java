package com.termux.app.workspace;

/** A redraw after viewport interaction must not start the output activity cue. */
final class SessionOutputActivity {
    private long ignoreUntil;
    private final java.util.LinkedHashMap<String,Long> output=new java.util.LinkedHashMap<>();
    void viewport(long now){ignoreUntil=now+1600;output.clear();}
    void output(String handle,long now){
        if(now<ignoreUntil)return;
        output.put(handle,now);
        while(output.size()>64)output.remove(output.keySet().iterator().next());
    }
    boolean active(String handle,long now){Long last=output.get(handle);return last!=null&&now-last<1600;}
}
