package com.termux.x11;
import android.app.Application;
import android.os.Bundle;
import java.lang.reflect.*;
import java.io.File;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WaylandReleaseGateTest {
 @Test public void disabledPreviewRejectsServiceAndProcessBeforePreparingFiles()throws Exception{
  Assume.assumeFalse(DesktopProfiles.waylandEnabled());
  X11Service service=Robolectric.buildService(X11Service.ArchLinux.class).get();
  try{
   Method command=X11Service.class.getDeclaredMethod("command",Bundle.class);command.setAccessible(true);
   for(String operation:new String[]{"check","install","launch"}){
    Bundle q=X11Remote.command(operation);q.putString("target","archlinux");q.putString("desktop","plasma");q.putString("renderer","turnip");
    try{command.invoke(service,q);fail(operation);}catch(InvocationTargetException e){assertTrue(e.getCause().toString(),e.getCause() instanceof IllegalArgumentException);}
   }
   File absent=new File(RuntimeEnvironment.application.getFilesDir(),"disabled-wayland-fixture");assertFalse(absent.exists());
   try{WaylandProcess.start(RuntimeEnvironment.application,absent,"check",null,null);fail();}catch(IllegalStateException expected){}
   try{WaylandRuntime.prepare(RuntimeEnvironment.application,absent);fail();}catch(java.io.IOException expected){}
   assertFalse(absent.exists());assertFalse(service.snapshot().getBoolean("started"));
  }finally{service.onDestroy();}
 }
}
