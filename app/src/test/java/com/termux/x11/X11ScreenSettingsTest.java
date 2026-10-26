package com.termux.x11;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class X11ScreenSettingsTest {
 private final Context c=RuntimeEnvironment.application;
 @Before public void clear(){c.getSharedPreferences("workspace-x11",0).edit().clear().commit();}
 @Test public void customResolutionKeepsFullHostHeightWhileImeCropsPortrait(){
  for(int scale:new int[]{75,100,125}){
   X11ScreenSettings settings=new X11ScreenSettings(1300,600,0,0,scale);X11Viewport viewport=new X11Viewport();
   for(int visible:new int[]{2040,1800,1000,1800,2040}){
    X11Viewport.Size size=settings.viewport(viewport,1216,visible,1216,2688,600,48,true);
    assertEquals(1216,size.width);assertEquals(2040,size.height);assertEquals(visible,size.visibleHeight);
    X11DisplayMode logical=X11DisplayMode.resolve(size.width,size.height,settings,false);
    assertEquals(600,logical.width);assertEquals(1300,logical.height);
   }
  }
 }
 @Test public void customModeOpenedWithImeVisibleUsesTheSameHostPolicyAsAutoAndRotates(){
  for(X11ScreenSettings settings:new X11ScreenSettings[]{X11ScreenSettings.defaults(),new X11ScreenSettings(1920,1080,0,0,75)}){
   X11Viewport viewport=new X11Viewport();X11Viewport.Size size=settings.viewport(viewport,1216,1000,1216,2688,600,48,true);
   assertEquals(2040,size.height);assertEquals(1000,size.visibleHeight);
   size=settings.viewport(viewport,2688,500,2688,1216,0,0,true);assertEquals(1216,size.height);assertEquals(500,size.visibleHeight);
   size=settings.viewport(viewport,1216,2040,1216,2688,600,48,true);assertEquals(2040,size.height);assertEquals(2040,size.visibleHeight);
  }
 }
 @Test public void manualModeRotatesWithPhoneButNotWithKeyboard(){
  X11ScreenSettings s=new X11ScreenSettings(1300,600,24,12);
  for(int[] size:new int[][]{{2448,1080,1},{1080,1800,0},{2448,500,1},{1080,500,0}}){
   boolean landscape=size[2]==1;X11DisplayMode m=X11DisplayMode.resolve(size[0],size[1],s,landscape);
   assertEquals(landscape?1296:600,m.width);assertEquals(landscape?600:1300,m.height);
   X11ScreenSettings.Frame f=s.frame(size[0],size[1],2);assertTrue(f.left>=48);assertTrue(f.top>=24);assertTrue(f.width+2*f.left<=size[0]);assertTrue(f.height+2*f.top<=size[1]);assertEquals(size[0]-96,f.width);assertEquals(size[1]-48,f.height);
  }
 }
 @Test public void manualResolutionAndInterfaceScaleFillPortraitAndLandscapeWithoutExtraBorders(){
  for(int scale:new int[]{50,75,100,125,150})for(int[] view:new int[][]{{926,1460},{2448,1080},{1080,500}}){
   X11ScreenSettings settings=new X11ScreenSettings(1300,600,0,0,scale);X11ScreenSettings.Frame f=settings.frame(view[0],view[1],3);
   assertEquals(0,f.left);assertEquals(0,f.top);assertEquals(view[0],f.width);assertEquals(view[1],f.height);
  }
 }
 @Test public void onlyUserSelectedMarginsReduceTheFrameAtAnyResolution(){
  for(int[] mode:new int[][]{{0,0},{1300,600},{1920,1080}}){
   X11ScreenSettings.Frame f=new X11ScreenSettings(mode[0],mode[1],16,8,75).frame(926,1460,2);
   assertEquals(32,f.left);assertEquals(16,f.top);assertEquals(862,f.width);assertEquals(1428,f.height);
  }
 }
 @Test public void scalePersistsPerEnvironmentAndOldSettingsDefaultTo100(){
  new X11ScreenSettings(1300,600,0,0,75).save(c,"termux");assertEquals(75,X11ScreenSettings.load(c,"termux").scalePercent);assertEquals(100,X11ScreenSettings.load(c,"ubuntu").scalePercent);
  for(int invalid:new int[]{0,49,151,Integer.MAX_VALUE})try{new X11ScreenSettings(0,0,0,0,invalid);fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void defaultsKeepExistingAutoModeWithoutMargins(){
  X11ScreenSettings s=X11ScreenSettings.defaults();X11ScreenSettings.Frame f=s.frame(2448,1080,3);assertEquals(0,f.left);assertEquals(0,f.top);assertEquals(2448,f.width);assertEquals(1080,f.height);
  assertEquals(X11DisplayMode.fit(2448,1080).width,X11DisplayMode.resolve(2448,1080,s).width);
 }
 @Test public void environmentSettingsAreIndependentAndAutoPreservesMargins(){
  new X11ScreenSettings(1300,600,16,8).save(c,"ubuntu");new X11ScreenSettings(0,0,32,0).save(c,"debian");
  assertEquals(1300,X11ScreenSettings.load(c,"ubuntu").width);assertTrue(X11ScreenSettings.load(c,"debian").automatic());assertEquals(32,X11ScreenSettings.load(c,"debian").horizontalDp);assertEquals(0,X11ScreenSettings.load(c,"termux").horizontalDp);
 }
 @Test public void badStoredValuesFallBackWithoutTouchingOtherSettings(){
  c.getSharedPreferences("workspace-x11",0).edit().putString("screen:ubuntu:width","bad").putBoolean("termux-style:ubuntu",true).commit();assertTrue(X11ScreenSettings.load(c,"ubuntu").automatic());assertTrue(c.getSharedPreferences("workspace-x11",0).getBoolean("termux-style:ubuntu",false));
 }
 @Test public void invalidModeAndMarginRequestsFailBeforeNativeAllocation(){
  for(int[] v:new int[][]{{0,600,0,0},{320,0,0,0},{319,600,0,0},{1300,199,0,0},{4097,600,0,0},{4096,4096,0,0},{1300,600,-1,0},{1300,600,0,97}})try{new X11ScreenSettings(v[0],v[1],v[2],v[3]);fail();}catch(IllegalArgumentException expected){}
  Bundle q=new Bundle();q.putInt("logicalWidth",Integer.MAX_VALUE);q.putInt("logicalHeight",600);try{X11ScreenSettings.mode(q);fail();}catch(IllegalArgumentException expected){}
  q.clear();assertTrue(X11ScreenSettings.mode(q).automatic());
 }
 @Test public void tinyWindowsKeepAtLeastHalfTheSpaceAndNeverCollapse(){
  X11ScreenSettings s=new X11ScreenSettings(0,0,96,96);X11ScreenSettings.Frame f=s.frame(100,60,4);assertEquals(50,f.width);assertEquals(30,f.height);f=s.frame(1,1,4);assertEquals(1,f.width);assertEquals(1,f.height);
 }
}
