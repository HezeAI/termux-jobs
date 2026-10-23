package com.termux.x11;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class X11ProgramOutputTest {
 @Test public void onlyCompleteSuccessfulResultsAreTrustedAndUnknownIdsAreIgnored(){
  X11ProgramInstaller.ProgramOutput p=new X11ProgramInstaller.ProgramOutput();
  p.feed("PROGRAM:injected:installed\n"+new String(new char[2000]).replace('\0','x')+"\n");
  for(String id:DesktopPrograms.IDS)p.feed("PROGRAM:"+id+":installed\n");assertFalse(p.success());
  p.feed("READY:pro");p.feed("grams\nDESKTOP_CHILD_EXIT=7\n");assertFalse(p.success());p.feed("DESKTOP_CHILD_EXIT=0\n");assertTrue(p.success());assertEquals(DesktopPrograms.IDS.length,p.items().size());
  p.items().clear();assertEquals(DesktopPrograms.IDS.length,p.items().size());
 }
 @Test public void unknownDnsStateInvalidatesEarlierKnownValue(){X11ProgramInstaller.ProgramOutput p=new X11ProgramInstaller.ProgramOutput();p.feed("FIREFOX_DNS:on\n");assertTrue(p.dnsSeen);assertTrue(p.dnsKnown);assertTrue(p.dnsEnabled);p.feed("FIREFOX_DNS:unknown\n");assertFalse(p.dnsKnown);p.feed("FIREFOX_DNS:off\n");assertTrue(p.dnsKnown);assertFalse(p.dnsEnabled);}
 @Test public void rejectedProgramsDoNotStartAnExecutorJob(){
  X11ProgramInstaller i=new X11ProgramInstaller(org.robolectric.RuntimeEnvironment.application);
  try{for(String id:new String[]{"../pdf","pdf;touch /tmp/no","pkg:unknown-app",""})try{i.start("install","jobs-x11-ubuntu",id);fail(id);}catch(IllegalArgumentException expected){}
   assertFalse(i.busy());assertEquals(0,i.snapshot().getInt("job",0));assertEquals("",i.snapshot().getString("phase",""));
  }finally{i.close();}
 }
}
