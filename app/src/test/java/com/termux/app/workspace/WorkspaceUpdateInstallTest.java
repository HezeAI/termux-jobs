package com.termux.app.workspace;

import android.app.*;
import android.content.Intent;
import android.content.pm.*;
import android.os.Looper;
import androidx.core.content.FileProvider;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class WorkspaceUpdateInstallTest {
    private Activity a; private File file; private WorkspaceUpdates.Release release;
    private org.robolectric.shadows.ShadowPackageManager pm;
    private AtomicBoolean busy;
    @Before public void setup()throws Exception{
        java.lang.reflect.Field cache=FileProvider.class.getDeclaredField("sCache");cache.setAccessible(true);((Map<?,?>)cache.get(null)).clear();
        java.lang.reflect.Field flag=WorkspaceUpdates.class.getDeclaredField("BUSY");flag.setAccessible(true);busy=(AtomicBoolean)flag.get(null);assertFalse(busy.get());
        a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"en");pm=Shadows.shadowOf(a.getPackageManager());
        File dir=new File(a.getCacheDir(),"workspace-updates");dir.mkdirs();file=File.createTempFile("update-test-",".apk",dir);
        try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(file))){zip.putNextEntry(new ZipEntry("lib/arm64-v8a/libtermux.so"));zip.write(7);zip.closeEntry();}
        long code=com.termux.BuildConfig.VERSION_CODE+1;
        String hash=WorkspaceUpdates.hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())));
        JSONObject manifest=new JSONObject().put("schema",1).put("versionCode",code).put("versionName","9.9.9").put("packageId","com.termux").put("minSdk",24)
            .put("apk",new JSONObject().put("abi","arm64-v8a").put("url",WorkspaceUpdates.DOWNLOAD+"v9.9.9/Termux-Jobs-v9.9.9-arm64.apk").put("sha256",hash).put("size",file.length()))
            .put("changes",new JSONObject().put("en",new org.json.JSONArray().put("fixture")));
        release=new WorkspaceUpdates.Release(manifest,"v9.9.9",false);
        PackageInfo installed=pm.getInternalMutablePackageInfo(a.getPackageName());installed.setLongVersionCode(code-1);installed.signatures=pkg(code-1).signatures;
        pm.setPackageArchiveInfo(file.getPath(),pkg(code));
    }
    private PackageInfo pkg(long code){
        PackageInfo p=new PackageInfo();p.packageName=a.getPackageName();p.versionName="9.9.9";p.setLongVersionCode(code);
        p.signatures=new Signature[]{new Signature("aabb")};p.applicationInfo=new ApplicationInfo();p.applicationInfo.packageName=p.packageName;p.applicationInfo.minSdkVersion=24;return p;
    }
    private void drain()throws Exception{
        long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while(busy.get()&&System.nanoTime()<until){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse("worker did not complete",busy.get());
    }
    @After public void cleanup()throws Exception{drain();if(file!=null)file.delete();if(a!=null)a.finish();}
    @Test public void exactBytesAndPackageAreRecheckedBeforeReadOnlyInstallerIntent()throws Exception{
        WorkspaceUpdates.verify(a,file,release);
        WorkspaceUpdates.prepareInstall(a,file,release);drain();Intent intent=Shadows.shadowOf(a).getNextStartedActivity();assertNotNull("installer missing; dialog="+org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().findViewById(android.R.id.message),intent);
        assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals("content",intent.getData().getScheme());assertEquals("com.termux.workspace.files",intent.getData().getAuthority());
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,intent.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));assertNotNull(intent.getClipData());
    }
    @Test public void changedBytesCannotLaunchInstallerEvenWithMatchingMockedMetadata()throws Exception{
        byte[] bytes=Files.readAllBytes(file.toPath());bytes[bytes.length-1]^=1;Files.write(file.toPath(),bytes);
        WorkspaceUpdates.prepareInstall(a,file,release);drain();assertNull(Shadows.shadowOf(a).getNextStartedActivity());
        assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());
    }
    @Test public void versionThatBecameOldWhileDialogWasOpenIsRejected()throws Exception{
        pm.getInternalMutablePackageInfo(a.getPackageName()).setLongVersionCode(release.code);WorkspaceUpdates.prepareInstall(a,file,release);drain();assertNull(Shadows.shadowOf(a).getNextStartedActivity());
    }
    @Test public void finishingScreenNeverLaunchesLateInstaller()throws Exception{
        WorkspaceUpdates.prepareInstall(a,file,release);a.finish();drain();assertNull(Shadows.shadowOf(a).getNextStartedActivity());
    }
}
