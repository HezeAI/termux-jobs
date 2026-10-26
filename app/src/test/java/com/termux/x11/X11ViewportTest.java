package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class X11ViewportTest {
 @Test public void closedKeyboardUsesActualSurfaceWithoutPhantomCursorPan(){
  X11Viewport v=new X11Viewport();
  X11Viewport.Size s=v.update(1216,1950,1216,2688,600,48,false);
  assertEquals(1950,s.height);assertEquals(0,s.hiddenBottom());
  s=v.update(1216,1000,1216,2688,600,48,true);
  assertEquals(1950,s.height);assertEquals(950,s.hiddenBottom());
  s=v.update(1216,1950,1216,2688,600,48,false);
  assertEquals(1950,s.height);assertEquals(0,s.hiddenBottom());
  s=v.update(2688,1000,2688,1216,0,0,false);
  assertEquals(1000,s.height);assertEquals(0,s.hiddenBottom());
  s=v.update(1216,1800,1216,2688,600,48,false);
  assertEquals(1800,s.height);assertEquals(0,s.hiddenBottom());
 }
 @Test public void openingAndClosingImeKeepDesktopModeWhileVisibleCropChanges(){
  X11Viewport v=new X11Viewport();
  for(int visible:new int[]{2040,1900,1500,1150,1500,1900,2040}){
   X11Viewport.Size s=v.update(1216,visible,1216,2688,600,48,true);
   assertEquals(1216,s.width);assertEquals(2040,s.height);assertEquals(visible,s.visibleHeight);assertEquals(2040-visible,s.hiddenBottom());
  }
 }
 @Test public void launchingWithKeyboardAlreadyVisibleStillUsesFullDesktop(){X11Viewport v=new X11Viewport();X11Viewport.Size s=v.update(1216,1000,1216,2688,600,48,true);assertEquals(2040,s.height);assertEquals(1040,s.hiddenBottom());assertEquals(2040,v.update(1216,2040,1216,2688,600,48,true).height);}
 @Test public void rotationReplacesGeometryInsteadOfReusingPortraitMaximum(){X11Viewport v=new X11Viewport();v.update(1216,2040,1216,2688,600,48,true);X11Viewport.Size s=v.update(2688,500,2688,1216,300,0,true);assertEquals(916,s.height);assertEquals(416,s.hiddenBottom());}
 @Test public void splitScreenCanBecomeSmallerAndThenGrow(){X11Viewport v=new X11Viewport();v.update(1216,2040,1216,2688,600,48,true);assertEquals(1152,v.update(1216,1152,1216,1800,600,48,true).height);assertEquals(2040,v.update(1216,2040,1216,2688,600,48,true).height);}
 @Test public void headerAndStableSystemInsetChangesAreRealGeometryChanges(){X11Viewport v=new X11Viewport();v.update(1216,2040,1216,2688,600,48,true);assertEquals(1940,v.update(1216,1000,1216,2688,700,48,true).height);assertEquals(1920,v.update(1216,1000,1216,2688,700,68,true).height);}
 @Test public void edgeToEdgeObservedHeightIsNotLostWhileImeAnimates(){X11Viewport v=new X11Viewport();assertEquals(2088,v.update(1216,2088,1216,2688,600,48,true).height);assertEquals(2088,v.update(1216,1000,1216,2688,600,48,true).height);}
 @Test public void floatingKeyboardWithNoResizeDoesNotChangeCrop(){X11Viewport v=new X11Viewport();for(int i=0;i<3;i++){X11Viewport.Size s=v.update(1216,2040,1216,2688,600,48,true);assertEquals(0,s.hiddenBottom());}}
 @Test public void malformedAndExtremeGeometryIsBounded(){X11Viewport v=new X11Viewport();X11Viewport.Size s=v.update(100000,100000,100000,100000,0,0,true);assertEquals(8192,s.width);assertEquals(8192,s.height);assertEquals(8192,s.visibleHeight);assertEquals(0,s.hiddenBottom());s=v.update(0,0,0,0,100,0,true);assertEquals(1,s.width);assertEquals(1,s.height);assertEquals(0,s.hiddenBottom());}
 @Test public void invalidVisibleCropIsRejectedBeforeNativeOrHostMutation(){for(int bad:new int[]{-1,0,2041,Integer.MAX_VALUE})try{X11Viewport.checkedVisible(bad,2040);fail();}catch(IllegalArgumentException expected){}assertEquals(1,X11Viewport.checkedVisible(1,2040));assertEquals(2040,X11Viewport.checkedVisible(2040,2040));}
}
