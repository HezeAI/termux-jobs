package com.termux.x11;

import android.view.KeyEvent;
import java.util.LinkedHashSet;
import java.util.Set;

/** Bounded physical-key state, separate from IME text and terminal input. */
public final class X11ExtraKeys {
    public interface Sink { void key(int code, boolean down); }
    private final Sink sink;
    private final Set<Integer> held=new LinkedHashSet<>();
    public X11ExtraKeys(Sink sink){this.sink=sink;}
    public static boolean allowed(int code){
        switch(code){
            case KeyEvent.KEYCODE_ESCAPE: case KeyEvent.KEYCODE_TAB:
            case KeyEvent.KEYCODE_CTRL_LEFT: case KeyEvent.KEYCODE_ALT_LEFT: case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MOVE_HOME: case KeyEvent.KEYCODE_MOVE_END:
            case KeyEvent.KEYCODE_PAGE_UP: case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_FORWARD_DEL: case KeyEvent.KEYCODE_ENTER: return true;
            default:return false;
        }
    }
    public void key(int code,boolean down){
        if(!allowed(code))return;
        if(down?held.add(code):held.remove(code))sink.key(code,down);
    }
    public void release(){
        Integer[] keys=held.toArray(new Integer[0]);
        for(int i=keys.length-1;i>=0;i--)key(keys[i],false);
    }
}
