package com.termux.app.workspace;

import android.app.Application;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Skip display/loading only; exercise real owner registration and resource shutdown. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,instrumentedPackages={"com.termux.app.workspace.WorkspacePackages","com.termux.app.workspace.WorkspaceFileBrowser"},shadows={WorkspaceBrowserLifecycleTest.PackageUi.class,WorkspaceBrowserLifecycleTest.FileUi.class})
public class WorkspaceBrowserLifecycleTest {
    @Implements(value=WorkspacePackages.class,isInAndroidSdk=false)
    public static class PackageUi {@Implementation protected void show(){}}
    @Implements(value=WorkspaceFileBrowser.class,isInAndroidSdk=false)
    public static class FileUi {@Implementation protected void show(){}}
    private WorkspaceController controller;
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    @Before public void create()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field f=TermuxActivity.class.getDeclaredField("mTerminalView");f.setAccessible(true);f.set(a,a.findViewById(R.id.terminal_view));controller=new WorkspaceController(a);
    }
    @After public void cleanup(){if(controller!=null)controller.close();}
    private Object open(boolean packages)throws Exception{
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment="termux";
        Method m=WorkspaceController.class.getDeclaredMethod(packages?"showPackages":"showFileBrowser",packages?new Class[]{WorkspaceStore.Session.class}:new Class[]{WorkspaceStore.Session.class,String.class,WorkspaceFileBrowser.DirectoryChoice.class});m.setAccessible(true);
        if(packages)m.invoke(controller,profile);else m.invoke(controller,profile,"~",null);
        List<?> owners=(List<?>)field(controller,packages?"packageBrowsers":"fileBrowsers");return owners.get(owners.size()-1);
    }
    @Test public void repeatedCloseUnregistersBrowsersAndClearsCatalogs()throws Exception{
        for(int i=0;i<10;i++)for(boolean packages:new boolean[]{true,false}){
            Object browser=open(packages);List<?> owners=(List<?>)field(controller,packages?"packageBrowsers":"fileBrowsers");assertEquals(1,owners.size());
            if(packages){Field f=WorkspacePackages.class.getDeclaredField("entries");f.setAccessible(true);f.set(browser,PackageCatalog.parse("bash/stable 5.2 arm64\n".getBytes(java.nio.charset.StandardCharsets.UTF_8),1));((WorkspacePackages)browser).close();((WorkspacePackages)browser).close();assertTrue(((List<?>)field(browser,"entries")).isEmpty());}
            else{((WorkspaceFileBrowser)browser).close();((WorkspaceFileBrowser)browser).close();assertNull(field(browser,"listing"));}
            assertTrue(owners.isEmpty());assertTrue(((ExecutorService)field(browser,"worker")).isShutdown());assertNull(field(browser,"onClosed"));
        }
    }
    @Test public void controllerClosesMultipleBrowsersWhileCallbacksRemoveThem()throws Exception{
        List<Object> browsers=new ArrayList<>();for(int i=0;i<3;i++){browsers.add(open(true));browsers.add(open(false));}
        controller.close();assertTrue(((List<?>)field(controller,"packageBrowsers")).isEmpty());assertTrue(((List<?>)field(controller,"fileBrowsers")).isEmpty());for(Object browser:browsers)assertTrue(((ExecutorService)field(browser,"worker")).isShutdown());controller=null;
    }
}
