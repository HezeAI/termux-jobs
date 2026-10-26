package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class X11DisplayModeTest {
    @Test public void portraitKeepsExistingResolution(){X11DisplayMode s=X11DisplayMode.fit(1216,2040);assertEquals(1216,s.width);assertEquals(2040,s.height);}
    @Test public void landscapeUsesTwiceThePreviousLogicalResolutionWithoutStretching(){
        for(int[] input:new int[][]{{2688,1216},{3120,1440},{2400,1080},{1920,1080}}){
            X11DisplayMode s=X11DisplayMode.fit(input[0],input[1]);
            assertTrue(s.width<=2880);assertTrue(s.width<=input[0]);assertEquals(0,s.width%8);
            assertTrue(Math.abs((double)s.width/s.height-(double)input[0]/input[1])<.03);
        }
    }
    @Test public void smallLandscapeNeverUpscalesLogicalResolution(){X11DisplayMode s=X11DisplayMode.fit(800,480);assertEquals(800,s.width);assertEquals(480,s.height);}
    @Test public void extremeSizesStayBounded(){
        X11DisplayMode large=X11DisplayMode.fit(8192,4096);assertEquals(2880,large.width);assertEquals(1440,large.height);
        X11DisplayMode s=X11DisplayMode.fit(1,1);assertEquals(64,s.width);assertEquals(64,s.height);
        for(int[] bad:new int[][]{{0,1},{1,0},{-1,500}})try{X11DisplayMode.fit(bad[0],bad[1]);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void overlayAndResizingKeyboardsHaveSameVisibleCrop(){
        assertEquals(600,X11Viewport.visibleHeight(1216,1216,0,616));
        assertEquals(600,X11Viewport.visibleHeight(600,1216,0,616));
        assertEquals(1216,X11Viewport.visibleHeight(1216,1216,0,0));
        assertEquals(1,X11Viewport.visibleHeight(600,1216,900,616));
        X11Viewport v=new X11Viewport();assertEquals(1216,v.update(2688,1216,2688,1216,0,0,false).height);
        X11Viewport.Size crop=v.update(2688,X11Viewport.visibleHeight(1216,1216,0,616),2688,1216,0,0,true);
        assertEquals(1216,crop.height);assertEquals(600,crop.visibleHeight);
        X11DisplayMode mode=X11DisplayMode.fit(crop.width,crop.height);assertEquals(2688,mode.width);assertEquals(1216,mode.height);
    }
}
