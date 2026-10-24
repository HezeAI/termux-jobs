package com.termux.x11;
import android.view.KeyEvent;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class X11ExtraKeysTest {
    @Test public void duplicateDownAndUnmatchedUpAreIgnored(){
        List<String> sent=new ArrayList<>();X11ExtraKeys keys=new X11ExtraKeys((c,d)->sent.add(c+":"+d));
        keys.key(KeyEvent.KEYCODE_ESCAPE,false);keys.key(KeyEvent.KEYCODE_ESCAPE,true);keys.key(KeyEvent.KEYCODE_ESCAPE,true);keys.key(KeyEvent.KEYCODE_ESCAPE,false);keys.key(KeyEvent.KEYCODE_ESCAPE,false);
        assertEquals(Arrays.asList("111:true","111:false"),sent);
    }
    @Test public void releaseClearsAllHeldKeysOnceInReverseOrder(){
        List<String> sent=new ArrayList<>();X11ExtraKeys keys=new X11ExtraKeys((c,d)->sent.add(c+":"+d));
        keys.key(KeyEvent.KEYCODE_CTRL_LEFT,true);keys.key(KeyEvent.KEYCODE_DPAD_UP,true);keys.release();keys.release();
        assertEquals(Arrays.asList("113:true","19:true","19:false","113:false"),sent);
    }
    @Test public void invalidAndAndroidNavigationKeysCannotEnterExtraKeyChannel(){
        List<String> sent=new ArrayList<>();X11ExtraKeys keys=new X11ExtraKeys((c,d)->sent.add(c+":"+d));
        for(int c:new int[]{-1,304,Integer.MAX_VALUE,KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_HOME,KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_A})keys.key(c,true);
        keys.release();assertTrue(sent.isEmpty());
    }
    @Test public void stateBelongsToOneHostAndDoesNotLeakIntoReplacement(){
        List<String> old=new ArrayList<>(),next=new ArrayList<>();X11ExtraKeys a=new X11ExtraKeys((c,d)->old.add(c+":"+d)),b=new X11ExtraKeys((c,d)->next.add(c+":"+d));
        a.key(KeyEvent.KEYCODE_ALT_LEFT,true);a.release();b.key(KeyEvent.KEYCODE_ENTER,true);b.key(KeyEvent.KEYCODE_ENTER,false);
        assertEquals(Arrays.asList("57:true","57:false"),old);assertEquals(Arrays.asList("66:true","66:false"),next);
    }
}
