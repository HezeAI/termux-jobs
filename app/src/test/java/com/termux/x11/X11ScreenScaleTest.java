package com.termux.x11;
import android.app.Application;import android.os.Bundle;import java.lang.reflect.*;import java.util.concurrent.ExecutorService;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class X11ScreenScaleTest {
 private X11Service service;private Method command;
 private Field field(String key)throws Exception{Field f=X11Service.class.getDeclaredField(key);f.setAccessible(true);return f;}
 @Before public void setup()throws Exception{service=Robolectric.buildService(X11Service.class).get();command=X11Service.class.getDeclaredMethod("command",Bundle.class);command.setAccessible(true);field("target").set(service,"ubuntu");}
 @After public void close()throws Exception{((ExecutorService)field("worker").get(service)).shutdown();((X11ScaleJob)field("scaleJob").get(service)).close();}
 private void refuse(Bundle b,Class<?> type)throws Exception{try{command.invoke(service,b);fail();}catch(InvocationTargetException e){assertTrue(e.getCause().toString(),type.isInstance(e.getCause()));}assertFalse(((X11ScaleJob)field("scaleJob").get(service)).snapshot().getBoolean("pending",false));}
 private Bundle request(String target,int percent){Bundle b=X11Remote.command("screen-scale");b.putString("target",target);b.putInt("interfaceScale",percent);b.putString("request","11111111-1111-4111-8111-111111111111");return b;}
 @Test public void invalidScaleCannotChangeStateOrStartAChild()throws Exception{refuse(request("ubuntu",49),IllegalArgumentException.class);assertEquals(100,field("interfaceScale").get(service));}
 @Test public void wrongEnvironmentAndUnreadyDesktopAreRejected()throws Exception{refuse(request("debian",75),IllegalStateException.class);refuse(request("ubuntu",75),IllegalStateException.class);assertFalse((Boolean)field("started").get(service));}
 @Test public void waylandDoesNotUseX11ScaleJob()throws Exception{field("desktop").set(service,"plasma");refuse(request("ubuntu",75),IllegalStateException.class);}
}
