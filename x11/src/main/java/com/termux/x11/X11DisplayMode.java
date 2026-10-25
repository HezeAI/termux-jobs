package com.termux.x11;

/** Logical desktop size, independent of IME crop; keep landscape UI near physical resolution. */
final class X11DisplayMode {
    final int width,height;
    private X11DisplayMode(int w,int h){width=w;height=h;}
    static X11DisplayMode resolve(int w,int h,X11ScreenSettings settings){
        return resolve(w,h,settings,w>h);
    }
    static X11DisplayMode resolve(int w,int h,X11ScreenSettings settings,boolean landscape){
        if(settings.automatic())return fit(w,h);
        boolean swap=(settings.width>settings.height)!=landscape;
        int width=swap?settings.height:settings.width,height=swap?settings.width:settings.height;
        return new X11DisplayMode(width-width%8,height);
    }
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
