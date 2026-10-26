package com.termux.x11;
import android.app.Application;
import android.content.Context;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class DesktopProcessAssetTest {
 @Test public void sharedVirglLoaderHelperIsPackagedAndExtracted()throws Exception{
  Context c=RuntimeEnvironment.application;File dir=Files.createTempDirectory(c.getCacheDir().toPath(),"virgl-env").toFile();
  Method m=DesktopProcess.class.getDeclaredMethod("asset",Context.class,File.class,String.class);m.setAccessible(true);m.invoke(null,c,dir,"x11-virgl-env.sh");
  String text=new String(Files.readAllBytes(new File(dir,"x11-virgl-env.sh").toPath()),java.nio.charset.StandardCharsets.UTF_8);
  assertTrue(text.contains("jobs_virgl_environment()"));assertTrue(text.contains("GALLIUM_DRIVER=virpipe"));assertTrue(text.contains("LIBGL_KOPPER_DISABLE=true"));
 }
 @Test public void fedoraImageCompatibilityIsPackagedAsSeparateArm64Module()throws Exception{
  Context c=RuntimeEnvironment.application;File dir=Files.createTempDirectory(c.getCacheDir().toPath(),"fedora-image").toFile();
  Method m=DesktopProcess.class.getDeclaredMethod("asset",Context.class,File.class,String.class);m.setAccessible(true);
  m.invoke(null,c,dir,"x11-fedora-glycin.so");byte[] data=Files.readAllBytes(new File(dir,"x11-fedora-glycin.so").toPath());
  assertTrue(data.length>64);assertArrayEquals(new byte[]{127,69,76,70},java.util.Arrays.copyOf(data,4));assertEquals(183,data[18]&255);assertEquals(0,data[19]);
  assertFalse(new File(dir,"x11-alpine-compat.so").exists());
 }
 @Test public void retiredDiagnosticHelpersAreNotPackaged()throws Exception{
  Context c=RuntimeEnvironment.application;
  for(String name:new String[]{"x11-gtk-observe.so","x11-observe.py","libgarcon-1.so.0","x11-garcon-source.zip","x11-touch-style.zip","x11-touch-workspace"}){
   try(InputStream in=c.getAssets().open(name)){fail("Retired asset present: "+name);}catch(IOException expected){}
  }
 }
 @Test public void extractionRefusesScriptLinkWithoutChangingUserFile()throws Exception{
  Context c=RuntimeEnvironment.application;File dir=Files.createTempDirectory(c.getCacheDir().toPath(),"script-link").toFile();File sentinel=new File(dir,"sentinel");Files.write(sentinel.toPath(),new byte[]{7});Files.createSymbolicLink(new File(dir,"x11-session.sh").toPath(),sentinel.toPath());
  Method m=DesktopProcess.class.getDeclaredMethod("asset",Context.class,File.class,String.class);m.setAccessible(true);
  try{m.invoke(null,c,dir,"x11-session.sh");fail();}catch(InvocationTargetException e){assertTrue(e.getCause() instanceof IOException);}
  assertArrayEquals(new byte[]{7},Files.readAllBytes(sentinel.toPath()));
 }
 @Test public void optionalArchivesAreExternalButLoaderAndLicenseRemainPackaged()throws Exception{
  Context c=RuntimeEnvironment.application;
  for(String name:new String[]{"x11-virgl-alpine.zip","x11-style.zip"}){
   try(InputStream in=c.getAssets().open(name)){fail("Optional archive is still embedded: "+name);}catch(IOException expected){}
  }
  try(InputStream in=c.getAssets().open("x11-extras.py")){assertTrue(in.read()!=-1);}
  try(InputStream in=c.getAssets().open("x11-virgl-alpine-LICENSE.txt")){assertTrue(in.read()!=-1);}
 }
}
