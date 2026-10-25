package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class DesktopProfilesTest {
 @Test public void onlyXfceIsAvailableInEverySupportedEnvironment(){
  for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
   for(String renderer:new String[]{"cpu","turnip","virgl","wrapper","bionic-glibc"})DesktopProfiles.validate(target,"xfce",renderer,false);
   for(String retired:new String[]{"plasma","wayland","lxqt",""}){
    assertFalse(DesktopProfiles.available(target,retired));
    try{DesktopProfiles.validate(target,retired,"turnip",false);fail(target);}catch(IllegalArgumentException expected){}
   }
  }
 }
}
