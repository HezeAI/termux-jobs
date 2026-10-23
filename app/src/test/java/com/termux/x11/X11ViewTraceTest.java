package com.termux.x11;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class X11ViewTraceTest {
 @Test public void boundsRecentEventsButRetainsCounts(){X11ViewTrace t=new X11ViewTrace();for(int i=0;i<100;i++)t.event("geometry","1200x1947 visible=1944");String r=t.report();assertTrue(r.contains("geometry=100"));assertEquals(25,r.split("\n").length);assertFalse(r.contains("authority"));}
}
