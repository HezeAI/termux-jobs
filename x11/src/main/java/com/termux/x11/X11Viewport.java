package com.termux.x11;

/** Unobscured desktop geometry; an IME resize only changes the visible crop. */
final class X11Viewport {
    static final class Size {
        final int width,height,visibleHeight;
        Size(int w,int h,int visible){width=w;height=h;visibleHeight=visible;}
        int hiddenBottom(){return height-visibleHeight;}
    }
    private int viewWidth,windowWidth,windowHeight,top,bottom,fullHeight;
    Size update(int width,int visibleHeight,int winWidth,int winHeight,int relativeTop,int stableBottom,boolean imeVisible){
        width=bound(width);visibleHeight=bound(visibleHeight);
        int estimate=bound(Math.max(visibleHeight,winHeight-relativeTop-stableBottom));
        boolean changed=viewWidth!=width||windowWidth!=winWidth||windowHeight!=winHeight||top!=relativeTop||bottom!=stableBottom;
        // Window bounds can include navigation gaps outside this embedded surface.
        // Only IME may crop the desktop; without it the measured host is exact.
        if(!imeVisible)fullHeight=visibleHeight;
        else if(changed||fullHeight==0)fullHeight=estimate;
        else fullHeight=Math.max(fullHeight,visibleHeight);
        viewWidth=width;windowWidth=winWidth;windowHeight=winHeight;top=relativeTop;bottom=stableBottom;
        return new Size(width,fullHeight,Math.min(visibleHeight,fullHeight));
    }
    static int checkedVisible(int value,int height){if(value<1||value>height)throw new IllegalArgumentException("Invalid visible X11 viewport");return value;}
    static int visibleHeight(int surfaceHeight,int windowBottom,int surfaceTop,int keyboardBottom){
        if(keyboardBottom<=0)return bound(surfaceHeight);
        long available=(long)windowBottom-keyboardBottom-surfaceTop;
        return bound((int)Math.max(1,Math.min(surfaceHeight,available)));
    }
    private static int bound(int value){return Math.max(1,Math.min(8192,value));}
}
