package com.termux.x11;
import android.app.Application;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class X11ObservationControlTest {
 private static final String TOKEN="a11a11a1-1111-4111-8111-111111111111";
 @Test public void writesOnlyFixedDataAndRefusesLinksOrInvalidIdentity()throws Exception{
  File dir=Files.createTempDirectory(RuntimeEnvironment.application.getFilesDir().toPath(),"obs-").toFile();
  X11ObservationControl.write(dir,TOKEN,1,true);File out=new File(dir,"x11-observation-control");assertEquals(TOKEN+" 1 on\n",new String(Files.readAllBytes(out.toPath()),java.nio.charset.StandardCharsets.US_ASCII));
  X11ObservationControl.write(dir,TOKEN,2,false);assertTrue(new String(Files.readAllBytes(out.toPath())).endsWith("2 off\n"));
  out.delete();File outside=new File(dir,"untouched");Files.write(outside.toPath(),new byte[]{7});Files.createSymbolicLink(out.toPath(),outside.toPath());
  try{X11ObservationControl.write(dir,TOKEN,3,true);fail();}catch(IOException expected){}assertArrayEquals(new byte[]{7},Files.readAllBytes(outside.toPath()));
  try{X11ObservationControl.write(dir,"../other",0,true);fail();}catch(IOException expected){}
 }
 @Test public void fragmentedAcknowledgementsRequireSessionSerialAndKnownState(){
  List<String> states=new ArrayList<>();X11ObservationControl log=new X11ObservationControl(TOKEN,(serial,state)->states.add(serial+":"+state));
  log.feed("JOBS_X11_OBSERV");log.feed("ATION "+TOKEN+" 1 active\nJOBS_X11_OBSERVATION wrong 2 stopped\n");
  log.feed("JOBS_X11_OBSERVATION "+TOKEN+" 2 stopped\nJOBS_X11_OBSERVATION "+TOKEN+" 9999999999 active\n");
  assertEquals(Arrays.asList("1:active","2:stopped"),states);
 }
 @Test public void captureKeepsCurrentSnapshotSeparateFromEventTailAndRejectsOtherSession(){
  X11CaptureLog log=new X11CaptureLog(TOKEN);log.feed("JOBS_X11_CAPTURE_BEGIN wrong 1\nJOBS_X11_OBSERVE ignored\n");assertEquals("",log.events());
  log.feed("JOBS_X11_CAPTURE_BEGIN "+TOKEN+" 1\nJOBS_X11_BACKDROP_CURRENT_BEGIN\nJOBS_X11_BACKDROP_CURRENT style=5\nJOBS_X11_BACKDROP_CURRENT_END\n");
  for(int i=0;i<2000;i++)log.feed("JOBS_X11_OBSERVE many-events\n");assertTrue(log.events().length()<=32768);assertTrue(log.backdrop().contains("style=5"));
  log.feed("JOBS_X11_CAPTURE_BEGIN "+TOKEN+" 2\n");assertFalse(log.events().contains("many-events"));assertEquals("",log.backdrop());
 }
 @org.junit.Test public void gtkCountersCannotLeakFromPreviousCapture(){
  String token="a11a11a1-1111-4111-8111-111111111111";X11CaptureLog log=new X11CaptureLog(token);
  log.feed("JOBS_X11_CAPTURE_BEGIN "+token+" 3\nJOBS_X11_GTK source=panel capture=3 phase=sample draw=10\n");
  org.junit.Assert.assertTrue(log.events().contains("draw=10"));
  log.feed("JOBS_X11_CAPTURE_BEGIN "+token+" 5\nJOBS_X11_GTK source=panel capture=3 phase=finished draw=99\nJOBS_X11_GTK source=desktop capture=5 phase=sample reload=2\n");
  org.junit.Assert.assertFalse(log.events().contains("draw=99"));org.junit.Assert.assertFalse(log.events().contains("draw=10"));org.junit.Assert.assertTrue(log.events().contains("reload=2"));
 }

}
