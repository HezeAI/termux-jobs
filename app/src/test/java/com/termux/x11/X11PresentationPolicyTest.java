package com.termux.x11;

import org.junit.Test;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import static org.junit.Assert.*;

public class X11PresentationPolicyTest {
    private X11Runtime runtime(String target)throws Exception{
        Constructor<X11Runtime> c=X11Runtime.class.getDeclaredConstructor(File.class,File.class,File.class,File.class,byte[].class,X11Environment.class);c.setAccessible(true);
        return c.newInstance(new File("base"),new File("tmp"),new File("keys"),new File("auth"),new byte[16],X11Environment.forTarget(target));
    }
    @Test public void nonCompositedGpuUsesClippedNativeCopyForEveryTarget()throws Exception{
        for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
            X11Runtime r=runtime(target);
            assertTrue(target,Arrays.asList(r.arguments("virgl")).contains("-disable-gpu-present"));
            assertEquals(target,!target.equals("termux"),Arrays.asList(r.arguments("turnip")).contains("-disable-gpu-present"));
            assertFalse(target,Arrays.asList(r.arguments("cpu")).contains("-disable-gpu-present"));
            assertEquals(":"+X11Environment.forTarget(target).display,r.arguments("virgl")[0]);
            assertTrue(Arrays.asList(r.arguments("virgl")).contains("-auth"));
        }
        assertTrue(Arrays.asList(runtime("termux").arguments("wrapper")).contains("-disable-gpu-present"));
    }
    @Test public void ubuntuBionicBridgeUsesClippedCopyWithoutChangingCpu()throws Exception{
        X11Runtime r=runtime("ubuntu");assertTrue(Arrays.asList(r.arguments("bionic-glibc")).contains("-disable-gpu-present"));assertFalse(Arrays.asList(r.arguments("cpu")).contains("-disable-gpu-present"));
    }
    @Test public void argumentSelectionDoesNotMutateAnotherLaunch()throws Exception{
        X11Runtime r=runtime("termux");String[] before=r.arguments();r.arguments("wrapper");assertArrayEquals(before,r.arguments("cpu"));
    }
}
