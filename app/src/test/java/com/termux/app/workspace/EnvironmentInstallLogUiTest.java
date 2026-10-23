package com.termux.app.workspace;

import android.app.*;import android.os.*;import android.view.*;import android.widget.*;
import com.termux.app.TermuxActivity;import org.junit.*;import org.junit.rules.TemporaryFolder;import org.junit.runner.RunWith;
import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.*;
import java.io.*;import java.nio.charset.StandardCharsets;import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class EnvironmentInstallLogUiTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private TermuxActivity activity;private AlertDialog dialog;private String report;
    private View tag(String value){return dialog.getWindow().getDecorView().findViewWithTag(value);}
    @Before public void before(){activity=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceLanguage.select(activity,"en");ShadowEnvironment.setExternalStorageDirectory(temp.getRoot().toPath());report=EnvironmentInstallLog.report("fedora",1,"Failed to install: unexpected end of file");EnvironmentInstallLogUi.show(activity,"Fedora",report);dialog=ShadowAlertDialog.getLatestAlertDialog();}
    @After public void after(){dialog.dismiss();}
    @Test public void downloadRequiresStoragePermissionAndWritesExactSnapshot()throws Exception{
        Shadows.shadowOf(RuntimeEnvironment.application).denyPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        tag("environment-install-log-download").performClick();assertNotNull(Shadows.shadowOf(activity).getLastRequestedPermission());
        File folder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);assertFalse(folder.exists()&&folder.list().length>0);
        Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        activity.onRequestPermissionsResult(WorkspaceStorageAccess.REQUEST,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});
        for(int i=0;i<100;i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();if(((TextView)tag("environment-install-log-status")).getText().toString().startsWith("Saved:"))break;}
        assertTrue(((TextView)tag("environment-install-log-status")).getText().toString().startsWith("Saved:"));File[] saved=folder.listFiles();assertNotNull(saved);assertEquals(1,saved.length);assertEquals(report,new String(java.nio.file.Files.readAllBytes(saved[0].toPath()),StandardCharsets.UTF_8));assertTrue(dialog.isShowing());
    }
    @Test public void failedExportLeavesWindowOpenForRetry()throws Exception{
        Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        File folder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(folder.isDirectory())assertTrue(folder.delete());folder.getParentFile().mkdirs();assertTrue(folder.createNewFile());
        tag("environment-install-log-download").performClick();for(int i=0;i<100;i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();if(((TextView)tag("environment-install-log-status")).getText().toString().startsWith("Could not save"))break;}
        assertTrue(dialog.isShowing());assertTrue(tag("environment-install-log-download").isEnabled());assertTrue(((TextView)tag("environment-install-log-status")).getText().toString().startsWith("Could not save"));
    }
}
