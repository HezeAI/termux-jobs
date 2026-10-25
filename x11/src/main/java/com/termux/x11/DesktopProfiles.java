package com.termux.x11;
/** A profile never selects another rootfs, display owner or terminal session. */
public final class DesktopProfiles {
    private DesktopProfiles() {}
    public static boolean waylandEnabled(){return BuildConfig.WAYLAND_PREVIEW;}
    public static boolean supported(String target,String profile){return "xfce".equals(profile)||"archlinux".equals(target)&&"plasma".equals(profile);}
    public static boolean available(String target,String profile){return supported(target,profile)&&(!"plasma".equals(profile)||waylandEnabled());}
    public static void validate(String target,String profile,String renderer,boolean style){
        if(!available(target,profile))throw new IllegalArgumentException("Desktop profile unavailable in this build");
        if("plasma".equals(profile)&&(!"turnip".equals(renderer)||style))throw new IllegalArgumentException("Arch Wayland requires its own Turnip renderer and KDE style");
    }
}
