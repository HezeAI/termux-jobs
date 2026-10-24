package com.termux.app.workspace;

/** Field focus and the manual button share one Activity-owned keyboard endpoint. */
final class DesktopKeyboardState {
    boolean enabled;
    boolean automatic=true;
    private boolean seenVisible,automaticHide,suppressed;
    private long focusSerial=-1,tapSerial=-1;
    boolean toggle(){enabled=!enabled;suppressed=!enabled;automaticHide=false;seenVisible=false;return enabled;}
    /** A real field transition can open the keyboard without first pressing the button. */
    Boolean focus(long serial,boolean text){
        if(serial==focusSerial)return null;focusSerial=serial;if(serial==0||!automatic)return null;
        if(text){if(suppressed)return null;enabled=true;automaticHide=false;return true;}
        suppressed=false;if(!enabled)return null;automaticHide=true;return false;
    }
    /** Reopening the same focused field after Back requires a fresh pointer tap. */
    Boolean tap(long serial,boolean text){
        if(serial==tapSerial)return null;tapSerial=serial;if(serial==0||!text||!automatic)return null;
        suppressed=false;enabled=true;automaticHide=false;return true;
    }
    boolean visibility(Boolean visible){
        if(visible==null)return false; // Host reattachment has no window insets yet.
        if(visible){seenVisible=true;return false;}
        if(!seenVisible)return false;
        seenVisible=false;
        if(automaticHide){automaticHide=false;return false;}
        boolean changed=enabled;enabled=false;suppressed=true;return changed;
    }
    void reset(){enabled=seenVisible=automaticHide=suppressed=false;focusSerial=tapSerial=-1;}
}
