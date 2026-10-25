package com.termux.x11;
import android.app.Application;
import android.os.Bundle;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WaylandSessionEndTest {
 private X11Service service;
 @Before public void setup(){service=Robolectric.buildService(X11Service.ArchLinux.class).get();ReflectionHelpers.setField(service,"desktop","plasma");}
 @After public void close(){service.onDestroy();}
 private void ended()throws Exception{java.lang.reflect.Method m=X11Service.class.getDeclaredMethod("desktopEnded",int.class);m.setAccessible(true);m.invoke(service,137);}
 @Test public void zeroExitWithoutReadinessIsAStartupFailureAndReportRetainsCause()throws Exception{
  X11StartupLog log=ReflectionHelpers.getField(service,"startupLog");log.feed("DESKTOP_CHILD_EXIT=0\n");WaylandStartupErrors errors=ReflectionHelpers.getField(service,"waylandStartupErrors");errors.feed("kwin failed to open /private/lib: Permission denied\n");ended();Bundle state=service.snapshot();assertEquals("error",state.getString("phase"));assertEquals("wayland",state.getString("backend"));assertTrue(state.getString("startupDiagnostic").contains("WAYLAND_SESSION_ENDED_BEFORE_READY"));assertTrue(state.getString("waylandStartupErrors").contains("Permission denied"));assertFalse(state.getString("waylandStartupErrors").contains("/private"));assertEquals(0,state.getInt("desktopChildExit"));assertEquals(137,state.getInt("desktopGuardianExit"));
 }
 @Test public void previouslyReadyPlasmaHasNormalWaylandEndStatus()throws Exception{
  ReflectionHelpers.setField(service,"desktopReady",true);ended();Bundle b=service.snapshot();assertEquals("ended",b.getString("phase"));assertTrue(b.getString("status").contains("Wayland"));assertFalse(b.getString("status").contains("X11"));
 }
 @Test public void x11DoesNotExportWaylandFailures()throws Exception{
  ReflectionHelpers.setField(service,"desktop","xfce");WaylandStartupErrors errors=ReflectionHelpers.getField(service,"waylandStartupErrors");errors.feed("kwin failed fixture\n");ended();Bundle b=service.snapshot();assertEquals("ended",b.getString("phase"));assertEquals("",b.getString("waylandStartupErrors"));
 }
}
