package com.termux.x11;

/** Loaded only by the private X11 activity/service process, never by terminal UI. */
final class X11Native {
    private static boolean loaded;
    static synchronized void load() {
        if (!loaded) { System.loadLibrary("Xlorie"); loaded=true; }
    }
}
