package com.termux.x11;
import android.os.*;import java.lang.reflect.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
public class KaliManjaroServiceTest {
 @Test public void newServicesRejectCrossEnvironmentAndUnsupportedRenderRequestsBeforeMutation()throws Exception{
  for(Class<? extends X11Service> type:new Class[]{X11Service.Kali.class,X11Service.Manjaro.class}){
   X11Service s=Robolectric.buildService(type).get();try{
    Field own=X11Service.class.getDeclaredField("ownProcess");own.setAccessible(true);own.set(s,true);String target=type==X11Service.Kali.class?"kali":"manjaro";assertEquals(target,X11Environment.context(s).target);
    Method method=X11Service.class.getDeclaredMethod("command",Bundle.class);method.setAccessible(true);
    for(String other:new String[]{"ubuntu","debian",target.equals("kali")?"manjaro":"kali"}){Bundle b=X11Remote.command("state");b.putString("target",other);try{method.invoke(s,b);fail(other);}catch(InvocationTargetException e){assertTrue(e.getCause() instanceof IllegalArgumentException);}}
    Bundle b=X11Remote.command("state");b.putString("target",target);assertFalse(((Bundle)method.invoke(s,b)).getBoolean("started"));
    if(target.equals("manjaro"))for(String mode:new String[]{"turnip","bionic-glibc","wrapper"}){b=X11Remote.command("check");b.putString("target",target);b.putString("renderer",mode);try{method.invoke(s,b);fail(mode);}catch(InvocationTargetException e){assertTrue(e.getCause() instanceof IllegalArgumentException);}assertEquals("",s.snapshot().getString("target"));}
   }finally{Field f=X11Service.class.getDeclaredField("worker");f.setAccessible(true);((java.util.concurrent.ExecutorService)f.get(s)).shutdown();}
  }
 }
}
