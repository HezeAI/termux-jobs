package com.termux.x11;
import org.junit.Test;
import static org.junit.Assert.*;
public class WaylandStartupErrorsTest {
 @Test public void permissionAndConnectFailuresSurviveFragmentsAndProgress(){
  WaylandStartupErrors log=new WaylandStartupErrors();log.feed("kwin_wayland: /root/private-wrapper: Permiss");log.feed("ion denied\n(EE) could not connect to wayland server\n");
  for(int i=0;i<1000;i++)log.feed("Loaded addon normal-progress\n");
  String out=log.snapshot();assertTrue(out.contains("Permission denied"));assertTrue(out.contains("could not connect to wayland server"));assertFalse(out.contains("private-wrapper"));assertFalse(out.contains("normal-progress"));
 }
 @Test public void secretsFocusFramesAndOrdinaryOutputCannotEnterReport(){
  WaylandStartupErrors log=new WaylandStartupErrors();log.feed("kwin failed to open /sdcard/Download/personal.jpg token=short-secret\nkwin error Authorization: Bearer abc123\nwayland failed at https://alice:password@private.example/path?key=foo\nkwin error 'private title'\nJOBS_X11_FOCUS_V1 secret FocusIn failed wayland\nmy private chat message\n-----BEGIN PRIVATE KEY----- wayland error\n");
  String out=log.snapshot();assertTrue(out.contains("kwin"));
  for(String secret:new String[]{"personal.jpg","short-secret","abc123","alice","password@","private.example","foo","private title","FocusIn","chat message","PRIVATE KEY"})assertFalse(secret,out.contains(secret));
 }
 @Test public void firstFailureIsNotEvictedByLaterFailuresAndStorageIsBounded(){
  WaylandStartupErrors log=new WaylandStartupErrors();log.feed("kwin error original reason\n");for(int i=0;i<100;i++)log.feed("kwin error reason "+i+"\n");String out=log.snapshot();assertTrue(out.contains("original reason"));assertTrue(out.contains("reason 99"));assertFalse(out.contains("reason 50\n"));assertEquals(17,out.split("\n").length);assertTrue(out.length()<16500);
 }
 @Test public void duplicateErrorsAnsiAndOversizedLinesDoNotCrowdOriginalCause(){
  WaylandStartupErrors log=new WaylandStartupErrors();for(int i=0;i<200;i++)log.feed("\u001b[31mkwin error Permission denied\u001b[0m\r");assertEquals("kwin error Permission denied",log.snapshot());
  log.feed("wayland error "+new String(new char[9000]).replace('\0','x')+"token=secret\nkwin failed to initialize\n");assertFalse(log.snapshot().contains("secret"));assertTrue(log.snapshot().endsWith("kwin failed to initialize"));assertTrue(log.snapshot().length()<200);
 }
}
