package com.termux.x11;

import android.view.SurfaceView;
import android.view.View;

/** A real SurfaceView focus loss returns the embedded window's focus to its host. */
final class X11ImeFocus {
    /** Repeated Fcitx notifications are not new IME requests. Lost host focus still needs recovery. */
    static boolean needsRequest(boolean show,boolean wanted,boolean viewFocus,boolean windowFocus){
        return show!=wanted||(show&&(!viewFocus||!windowFocus));
    }
    static boolean take(SurfaceView surface,View editor){
        // A touch can focus the embedded input window without changing the host
        // View's focus bit. Drive an actual gain/loss pair through public View APIs.
        surface.setFocusableInTouchMode(true);
        surface.requestFocus();
        boolean focused=editor.requestFocus();
        surface.setFocusable(false);
        surface.setFocusableInTouchMode(false);
        return focused;
    }
}
