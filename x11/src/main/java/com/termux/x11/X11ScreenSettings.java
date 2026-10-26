package com.termux.x11;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Per-environment X11 presentation. No rootfs, renderer or global Android display changes. */
public final class X11ScreenSettings {
    public final int width,height,horizontalDp,verticalDp,scalePercent;
    public X11ScreenSettings(int width,int height,int horizontalDp,int verticalDp){this(width,height,horizontalDp,verticalDp,100);}
    public X11ScreenSettings(int width,int height,int horizontalDp,int verticalDp,int scalePercent){
        if(!((width==0&&height==0)||(width>=320&&width<=4096&&height>=200&&height<=4096&&(long)width*height<=8388608)))
            throw new IllegalArgumentException("Resolution: 320–4096 × 200–4096, at most 8 megapixels");
        if(horizontalDp<0||horizontalDp>96||verticalDp<0||verticalDp>96)
            throw new IllegalArgumentException("Edge margins: 0–96 dp");
        validateScale(scalePercent);this.scalePercent=scalePercent;
        this.width=width;this.height=height;this.horizontalDp=horizontalDp;this.verticalDp=verticalDp;
    }
    public static void validateScale(int scale){if(scale<50||scale>150)throw new IllegalArgumentException("Interface scale: 50–150%");}
    public boolean automatic(){return width==0;}
    /** Both logical modes keep a stable Android host; IME only crops visibility. */
    X11Viewport.Size viewport(X11Viewport viewport,int width,int visibleHeight,int windowWidth,int windowHeight,int top,int bottom,boolean imeVisible){
        return viewport.update(width,visibleHeight,windowWidth,windowHeight,top,bottom,imeVisible);
    }
    public int effectiveWidth(){return width-width%8;}
    public static X11ScreenSettings defaults(){return new X11ScreenSettings(0,0,0,0);}
    private static String key(String target){X11Environment.forTarget(target);return "screen:"+target+":";}
    public static X11ScreenSettings load(Context context,String target){
        String k=key(target);SharedPreferences p=context.getSharedPreferences("workspace-x11",0);
        try{return new X11ScreenSettings(p.getInt(k+"width",0),p.getInt(k+"height",0),p.getInt(k+"horizontal",0),p.getInt(k+"vertical",0),p.getInt(k+"scale",100));}
        catch(IllegalArgumentException|ClassCastException invalid){return defaults();}
    }
    public void save(Context context,String target){
        String k=key(target);context.getSharedPreferences("workspace-x11",0).edit().putInt(k+"width",width).putInt(k+"height",height)
            .putInt(k+"horizontal",horizontalDp).putInt(k+"vertical",verticalDp).putInt(k+"scale",scalePercent).apply();
    }
    void putMode(Bundle q,boolean landscape){q.putInt("logicalWidth",width);q.putInt("logicalHeight",height);q.putBoolean("logicalLandscape",landscape);}
    static X11ScreenSettings mode(Bundle q){return new X11ScreenSettings(q.getInt("logicalWidth",0),q.getInt("logicalHeight",0),0,0);}
    /** Fill the Android host; only explicitly selected safe margins reserve space. */
    public Frame frame(int w,int h,float density){
        w=Math.max(1,w);h=Math.max(1,h);
        int x=Math.min(w/4,Math.round(horizontalDp*density)),y=Math.min(h/4,Math.round(verticalDp*density));
        return new Frame(x,y,w-2*x,h-2*y);
    }
    public static final class Frame {
        public final int left,top,width,height;
        Frame(int l,int t,int w,int h){left=l;top=t;width=w;height=h;}
    }
}
