package com.termux.x11;
import android.app.Application;import android.os.Bundle;import java.io.File;import java.util.*;import org.junit.*;import org.junit.rules.TemporaryFolder;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class KaliProgramsTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder(new File(System.getProperty("java.io.tmpdir")));
 @Test public void reviewedToolsBelongOnlyToKali(){
  assertEquals(16,DesktopPrograms.ids("kali").length);assertEquals(9,DesktopPrograms.recommended("kali").length);for(String tool:DesktopPrograms.KALI_TOOLS)assertFalse(Arrays.asList(DesktopPrograms.recommended("kali")).contains(tool));assertEquals("wireshark",DesktopPrograms.mainPackage("kali-wireshark","kali"));
  for(String id:DesktopPrograms.KALI_TOOLS){assertTrue(DesktopPrograms.requestId(id,"kali"));assertFalse(DesktopPrograms.requestId(id,"debian"));assertEquals("",DesktopPrograms.mainPackage(id,"ubuntu"));assertFalse(DesktopPrograms.title(id,true).isEmpty());assertFalse(DesktopPrograms.description(id,false).isEmpty());}
  for(String target:new String[]{"termux","ubuntu","debian","manjaro","archlinux"})assertEquals(12,DesktopPrograms.ids(target).length);
  String[] ids=DesktopPrograms.ids("kali");ids[0]="bad";assertEquals("files",DesktopPrograms.ids("kali")[0]);
 }
 @Test public void partialBaseListCannotFinishKaliCheckAndUnknownIdsDoNotCount(){
  X11ProgramInstaller.ProgramOutput out=new X11ProgramInstaller.ProgramOutput("kali");for(String id:DesktopPrograms.IDS)out.feed("PROGRAM:"+id+":installed\n");out.feed("PROGRAM:unreviewed:installed\nREADY:programs\nDESKTOP_CHILD_EXIT=0\n");assertFalse(out.success());
  for(String id:DesktopPrograms.KALI_TOOLS)out.feed("PROGRAM:"+id+":missing\n");assertTrue(out.success());assertFalse(out.items().getBoolean("kali-nmap"));assertEquals(16,out.items().size());
 }
 @Test public void cachesAndResumeKeepKaliToolsSeparateFromOtherEnvironments()throws Exception{
  DesktopCatalogCache cache=new DesktopCatalogCache(tmp.newFolder());Bundle items=new Bundle();for(String id:DesktopPrograms.ids("kali"))items.putBoolean(id,true);cache.saveItems("kali",items);assertEquals(16,cache.loadItems("kali").size());assertNull(cache.loadItems("debian"));
  DesktopProgramResume resume=new DesktopProgramResume(tmp.newFolder());resume.put("kali","kali-nmap",true);assertTrue(resume.read("kali").getBoolean("kali-nmap"));assertTrue(resume.read("debian").isEmpty());
  try{resume.put("debian","kali-nmap",true);fail();}catch(IllegalArgumentException expected){}
 }
}
