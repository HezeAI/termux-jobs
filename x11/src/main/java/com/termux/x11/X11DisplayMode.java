package com.termux.x11;

/** Logical desktop size, independent of IME crop; keep landscape UI near physical resolution. */
final class X11DisplayMode {
    final int width,height;
    private X11DisplayMode(int w,int h){width=w;height=h;}
    static X11DisplayMode fit(int w,int h){
        if(w<1||h<1)throw new IllegalArgumentException("Invalid X11 display size");
        double scale=1;
        if(w>h){
            scale=Math.max(scale,w/2880.0);
        }
        int width=Math.max(64,Math.min(4096,(int)(w/scale)));
        int height=Math.max(64,Math.min(4096,(int)Math.round(h/scale)));
        return new X11DisplayMode(width-width%8,height);
    }
}
