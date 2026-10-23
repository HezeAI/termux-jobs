package com.termux.app.workspace;

import android.Manifest;
import android.app.AlertDialog;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class LocalFileSourcesTest {
    @Rule public org.junit.rules.TemporaryFolder tmp=new org.junit.rules.TemporaryFolder();
    private TermuxActivity activity(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"en");return a;}
    private void field(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    @Test public void discoversOnlyInstalledRootsInBothLayoutsAndRejectsAliasesAndSymlinks()throws Exception {
        File proot=tmp.newFolder("proot");new File(proot,"installed-rootfs/ubuntu").mkdirs();new File(proot,"containers/debian/rootfs").mkdirs();new File(proot,"installed-rootfs/-invalid").mkdirs();new File(proot,"containers/not-installed").mkdirs();Files.createSymbolicLink(new File(proot,"installed-rootfs/linked").toPath(),new File(proot,"installed-rootfs/ubuntu").toPath());
        List<WorkspaceLocalFilePicker.Location> values=WorkspaceLocalFilePicker.locations(tmp.getRoot(),proot,"Storage");assertEquals(4,values.size());assertEquals("termux",values.get(0).environment);assertEquals("proot:debian",values.get(1).environment);assertEquals("proot:ubuntu",values.get(2).environment);assertTrue(values.get(3).shared);
        assertFalse(WorkspaceLocalFilePicker.validAlias("../escape"));assertFalse(WorkspaceLocalFilePicker.validAlias("--help"));
    }
    @Test public void deniedPermissionDoesNotOpenBrowserAndGrantContinuesExactlyOnce(){
        TermuxActivity a=activity();Shadows.shadowOf(RuntimeEnvironment.application).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);WorkspaceStorageAccess gate=new WorkspaceStorageAccess(a);AtomicInteger calls=new AtomicInteger();gate.request(calls::incrementAndGet);assertEquals(0,calls.get());
        gate.permissionResult(WorkspaceStorageAccess.REQUEST);assertEquals(0,calls.get());AlertDialog denied=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(denied);denied.dismiss();
        gate.request(calls::incrementAndGet);Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);gate.permissionResult(WorkspaceStorageAccess.REQUEST);gate.permissionResult(WorkspaceStorageAccess.REQUEST);assertEquals(1,calls.get());gate.close();
    }
    @Test public void destroyedGateDiscardsPendingContinuation(){TermuxActivity a=activity();Shadows.shadowOf(RuntimeEnvironment.application).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);WorkspaceStorageAccess gate=new WorkspaceStorageAccess(a);AtomicInteger calls=new AtomicInteger();gate.request(calls::incrementAndGet);gate.close();Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);gate.permissionResult(WorkspaceStorageAccess.REQUEST);assertEquals(0,calls.get());}
    @Test public void portableListingPreservesSpacesHiddenFilesAndEmptyFiles()throws Exception{
        File root=tmp.newFolder("files with spaces");Files.write(new File(root,"test ' file.txt").toPath(),new byte[]{1,2,3});Files.write(new File(root,".hidden").toPath(),new byte[0]);new File(root,"folder").mkdir();
        Process p=new ProcessBuilder("sh","-c",ProjectFiles.portableListScript(root.getAbsolutePath())).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int count;while((count=p.getInputStream().read(buffer))!=-1)out.write(buffer,0,count);assertEquals(0,p.waitFor());ProjectFiles.Listing list=ProjectFiles.parse(out.toByteArray());assertEquals(3,list.entries.size());assertEquals("folder",list.entries.get(0).name);assertTrue(list.entries.stream().anyMatch(e->e.name.equals(".hidden")&&e.size==0));assertTrue(list.entries.stream().anyMatch(e->e.name.equals("test ' file.txt")&&e.size==3));
    }
    @Test public void prootCommandValidatesAliasBeforeProcessAndQuotesPaths(){WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="proot:debian";String command=ProjectFiles.command(s,"cat -- '/root/file name'");assertTrue(command.startsWith("proot-distro login 'debian' -- /bin/sh -c "));s.environment="proot:--help";try{ProjectFiles.command(s,"pwd");fail();}catch(IllegalArgumentException expected){}}
    @Test public void multipleSelectionCapsAtFiftyAndSupportsRemovingAndAddingAcrossDirectories()throws Exception{
        TermuxActivity a=activity();WorkspaceFileBrowser browser=new WorkspaceFileBrowser(a,new WorkspaceStore.Session(),"/",null);browser.setFileChoices((source,entries)->fail("Selection must require confirmation"));LinearLayout rows=new LinearLayout(a);ScrollView scroll=new ScrollView(a);scroll.addView(rows);field(browser,"rows",rows);field(browser,"message",new TextView(a));field(browser,"search",new EditText(a));ProjectFiles.Listing listing=new ProjectFiles.Listing();listing.path="/";field(browser,"listing",listing);
        Method choose=WorkspaceFileBrowser.class.getDeclaredMethod("chooseFile",ProjectFiles.Entry.class);choose.setAccessible(true);
        for(int i=0;i<51;i++)choose.invoke(browser,new ProjectFiles.Entry(i%2==0?"/a":"/b","file"+i,false,i));
        Field selected=WorkspaceFileBrowser.class.getDeclaredField("selectedFiles");selected.setAccessible(true);Map<?,?> values=(Map<?,?>)selected.get(browser);assertEquals(50,values.size());assertFalse(values.containsKey("/a/file50"));choose.invoke(browser,new ProjectFiles.Entry("/a","file0",false,0));assertEquals(49,values.size());choose.invoke(browser,new ProjectFiles.Entry("/a","file50",false,50));assertEquals(50,values.size());browser.close();
    }
    @Test public void optionalDesktopDenialCanContinueWithoutOpeningFilePicker(){
        TermuxActivity a=activity();Shadows.shadowOf(RuntimeEnvironment.application).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);WorkspaceStorageAccess gate=new WorkspaceStorageAccess(a);AtomicInteger ready=new AtomicInteger(),cancel=new AtomicInteger();gate.request(ready::incrementAndGet,cancel::incrementAndGet);gate.permissionResult(WorkspaceStorageAccess.REQUEST);AlertDialog denied=ShadowAlertDialog.getLatestAlertDialog();denied.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(0,ready.get());assertEquals(1,cancel.get());gate.permissionResult(WorkspaceStorageAccess.REQUEST);assertEquals(1,cancel.get());gate.close();
    }
    @Test public void optionalCancellationAfterOwnerClosedCannotContinue(){
        TermuxActivity a=activity();Shadows.shadowOf(RuntimeEnvironment.application).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);WorkspaceStorageAccess gate=new WorkspaceStorageAccess(a);AtomicInteger calls=new AtomicInteger();gate.request(calls::incrementAndGet,calls::incrementAndGet);gate.permissionResult(WorkspaceStorageAccess.REQUEST);AlertDialog denied=ShadowAlertDialog.getLatestAlertDialog();gate.close();denied.cancel();assertEquals(0,calls.get());
    }

}
