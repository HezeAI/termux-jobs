package com.termux.x11;
/** Each environment retains its existing XFCE/X11 desktop and renderer. */
public final class DesktopProfiles {
    private DesktopProfiles() {}
    public static boolean supported(String target,String profile){return "xfce".equals(profile);}
    public static boolean available(String target,String profile){return supported(target,profile);}
    public static void validate(String target,String profile,String renderer,boolean style){
        if(!supported(target,profile))throw new IllegalArgumentException("Desktop profile unavailable");
    }
}
