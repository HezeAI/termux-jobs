package com.termux.x11;
import android.app.Application;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class DesktopCatalogTest {
 @Test public void alpineNamesKeepTheirCaseAndUnderscoresWithoutAcceptingOptionsOrShell(){
  DesktopCatalog.Parser parser=new DesktopCatalog.Parser();
  for(String name:new String[]{"libSvtAv1Enc","ssl_client","R"}){assertTrue(DesktopPrograms.requestId("pkg:"+name));parser.feed(entry(name,name,true));}
  parser.feed("READY:catalog\nDESKTOP_CHILD_EXIT=0\n");DesktopCatalog catalog=parser.result();assertNotNull(catalog);
  assertTrue(catalog.allows("libSvtAv1Enc"));assertFalse(catalog.allows("libsvtav1enc"));assertEquals(1,catalog.page("SSL_CLIENT",0).getInt("total"));
  for(String bad:new String[]{"--help","../file","x;true","file name","/path","_option"})assertFalse(DesktopCatalog.packageName(bad));
 }

 private String entry(String pkg,String name,boolean available){return "CATALOG:{\"package\":\""+pkg+"\",\"name\":\""+name+"\",\"summary\":\"Drawing pictures\",\"installed\":false,\"available\":"+available+",\"snap\":false}\n";}
 @Test public void cacheRequiresSuccessfulCompletionAndOnlyApprovesAvailablePackages(){
  DesktopCatalog.Parser p=new DesktopCatalog.Parser();p.feed(entry("inkscape","Inkscape",true));assertNull(p.result());p.feed(entry("snap-browser","Snap Browser",false)+"READY:catalog\n");assertNull(p.result());p.feed("DESKTOP_CHILD_EXIT=0\n");DesktopCatalog c=p.result();assertNotNull(c);assertTrue(c.allows("inkscape"));assertFalse(c.allows("snap-browser"));assertFalse(c.allows("--option"));assertFalse(c.allows("inkscape;touch"));
  Bundle page=c.page("drawing inkscape",0);assertEquals(1,page.getInt("total"));assertEquals(1,page.getParcelableArrayList("rows").size());assertTrue(c.installed("inkscape").page("inkscape",0).<Bundle>getParcelableArrayList("rows").get(0).getBoolean("installed"));
 }
 @Test public void deletionRequiresVerifiedInstalledCacheAndCannotTargetCoreOrUnlistedPackages(){
  X11ProgramInstaller installer=new X11ProgramInstaller(org.robolectric.RuntimeEnvironment.application);
  try{for(String id:new String[]{"pkg:mousepad","pkg:thunar","pkg:python3","editor","pkg:mousepad;true"})try{installer.start("remove","jobs-x11-ubuntu",id);fail("Unverified removal accepted: "+id);}catch(IllegalArgumentException expected){}
   try{installer.page("ubuntu","",0,true);fail();}catch(IllegalStateException expected){}
  }finally{installer.close();}
 }
 @Test public void emptyInstalledListIsValidButEmptyDownloadCatalogIsNot(){
  DesktopCatalog.Parser installed=new DesktopCatalog.Parser(true),catalog=new DesktopCatalog.Parser();installed.feed("READY:catalog\nDESKTOP_CHILD_EXIT=0\n");catalog.feed("READY:catalog\nDESKTOP_CHILD_EXIT=0\n");assertNotNull(installed.result());assertEquals(0,installed.result().page("",0).getInt("total"));assertNull(catalog.result());
  assertFalse(DesktopPrograms.removable("thunar"));assertFalse(DesktopPrograms.removable("xfce4-panel"));assertFalse(DesktopPrograms.removable("fcitx5-frontend-gtk3"));assertFalse(DesktopPrograms.removable("fonts-dejavu-core"));assertFalse(DesktopPrograms.removable("python3"));assertFalse(DesktopPrograms.removable("gir1.2-appstream-1.0"));assertFalse(DesktopPrograms.removable("--bad"));assertTrue(DesktopPrograms.removable("mousepad"));assertTrue(DesktopPrograms.removable("libreoffice-writer"));
 }
 @Test public void invalidDuplicateAndOversizedDataFailClosed(){
  for(String input:new String[]{entry("../pkg","Bad",true),entry("valid-pkg","Name",true)+entry("valid-pkg","Again",true),"CATALOG:"+new String(new char[9000]).replace('\0','x')+"\n"}){
   DesktopCatalog.Parser p=new DesktopCatalog.Parser();p.feed(input+"READY:catalog\nDESKTOP_CHILD_EXIT=0\n");assertNull(p.result());
  }
 }
 @Test public void pagesAreBoundedSearchIsCaseInsensitiveAndCannotBeAShellCommand(){
  DesktopCatalog.Parser p=new DesktopCatalog.Parser();for(int i=0;i<70;i++)p.feed(entry("app-"+i,"Программа "+i,true));p.feed("READY:catalog\nDESKTOP_CHILD_EXIT=0\n");DesktopCatalog c=p.result();assertEquals(30,c.page("ПРОГРАММА",0).getParcelableArrayList("rows").size());assertEquals(10,c.page("",60).getParcelableArrayList("rows").size());assertEquals(0,c.page("; touch /tmp/injected",0).getInt("total"));
  for(int offset:new int[]{-1,DesktopCatalog.MAX+1})try{c.page("",offset);fail();}catch(IllegalArgumentException expected){}
 }
}
