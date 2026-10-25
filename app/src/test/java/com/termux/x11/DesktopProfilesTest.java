package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class DesktopProfilesTest {
 @Test public void plasmaIsArchOnlyAndCannotReuseX11Modes(){
  assertTrue(DesktopProfiles.supported("archlinux","plasma"));
  assertEquals(BuildConfig.WAYLAND_PREVIEW,DesktopProfiles.available("archlinux","plasma"));
  if(DesktopProfiles.waylandEnabled())DesktopProfiles.validate("archlinux","plasma","turnip",false);
  else try{DesktopProfiles.validate("archlinux","plasma","turnip",false);fail("Disabled preview accepted");}catch(IllegalArgumentException expected){}
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void"}){
   try{DesktopProfiles.validate(target,"plasma","turnip",false);fail(target);}catch(IllegalArgumentException expected){}
   DesktopProfiles.validate(target,"xfce","cpu",false);
  }
  for(String mode:new String[]{"cpu","wrapper","bionic-glibc","virgl"})try{DesktopProfiles.validate("archlinux","plasma",mode,false);fail(mode);}catch(IllegalArgumentException expected){}
  try{DesktopProfiles.validate("archlinux","plasma","turnip",true);fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void keyMappingPreservesNavigationAndReturn(){
  assertEquals(28,WaylandKeys.evdev(66));assertEquals(28,WaylandKeys.evdev(160));assertEquals(1,WaylandKeys.evdev(111));
  assertEquals(103,WaylandKeys.evdev(19));assertEquals(108,WaylandKeys.evdev(20));assertEquals(105,WaylandKeys.evdev(21));assertEquals(106,WaylandKeys.evdev(22));
  assertEquals(-1,WaylandKeys.evdev(4));assertEquals(-1,WaylandKeys.evdev(-1));assertEquals(-1,WaylandKeys.evdev(304));
  assertEquals(600,WaylandKeys.unicode(0x410));assertEquals(665,WaylandKeys.unicode(0x451));assertEquals(-1,WaylandKeys.unicode(0x1f600));
 }
}
