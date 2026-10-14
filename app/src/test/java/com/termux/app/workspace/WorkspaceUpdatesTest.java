package com.termux.app.workspace;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.security.MessageDigest;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class WorkspaceUpdatesTest {
    private JSONObject manifest()throws Exception{
        return new JSONObject("{\"schema\":1,\"versionCode\":1056,\"versionName\":\"1.0.19\",\"packageId\":\"com.termux\",\"minSdk\":24,\"apk\":{\"abi\":\"arm64-v8a\",\"url\":\"https://github.com/HezeAI/termux-jobs/releases/download/v1.0.19/Termux-Jobs-v1.0.19-arm64.apk\",\"sha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"size\":36000000},\"changes\":{\"ru\":[\"Обновления\"],\"en\":[\"Updates\"]}}");
    }
    @Test public void releaseUsesVersionCodeAndLocalizedNotes()throws Exception{
        WorkspaceUpdates.Release ru=new WorkspaceUpdates.Release(manifest(),"v1.0.19",true),en=new WorkspaceUpdates.Release(manifest(),"v1.0.19",false);
        assertEquals(1056,ru.code);assertTrue(ru.notes.contains("Обновления"));assertTrue(en.notes.contains("Updates"));
    }
    private void rejected(JSONObject m)throws Exception{try{new WorkspaceUpdates.Release(m,"v1.0.19",false);fail("Accepted unsafe manifest");}catch(java.io.IOException expected){}}
    @Test public void rejectWrongPackageTagAbiHashAndOversizedDownload()throws Exception{
        rejected(manifest().put("packageId","other.app"));rejected(manifest().put("versionName","1.0.20"));
        JSONObject m=manifest();m.getJSONObject("apk").put("abi","x86");rejected(m);
        m=manifest();m.getJSONObject("apk").put("sha256","bad");rejected(m);
        m=manifest();m.getJSONObject("apk").put("size",Long.MAX_VALUE);rejected(m);
        m=manifest();m.getJSONObject("apk").put("url","https://example.com/update.apk");rejected(m);
    }
    @Test public void assetsAreBoundToRepositoryTagAndExactName(){
        String base=WorkspaceUpdates.DOWNLOAD+"v1.0.19/";assertTrue(WorkspaceUpdates.validAsset(base+"update.json","v1.0.19","update.json"));
        assertFalse(WorkspaceUpdates.validAsset(base+"update.json?x=1","v1.0.19","update.json"));assertFalse(WorkspaceUpdates.validAsset(base+"update.json","../v1.0.19","update.json"));assertFalse(WorkspaceUpdates.validAsset(base.replace("https:","http:")+"update.json","v1.0.19","update.json"));
    }
    @Test public void signerSetMustMatchExactly(){
        Signature a=new Signature("aabb"),b=new Signature("ccdd");assertTrue(WorkspaceUpdates.sameSignatures(new Signature[]{a},new Signature[]{a}));
        assertFalse(WorkspaceUpdates.sameSignatures(new Signature[]{a},new Signature[]{b}));assertFalse(WorkspaceUpdates.sameSignatures(null,new Signature[]{a}));assertFalse(WorkspaceUpdates.sameSignatures(new Signature[0],new Signature[0]));assertFalse(WorkspaceUpdates.sameSignatures(new Signature[]{a},new Signature[]{a,b}));
    }
    private PackageInfo pkg(int code){PackageInfo p=new PackageInfo();p.packageName="com.termux";p.versionName="1.0.19";p.setLongVersionCode(code);p.signatures=new Signature[]{new Signature("aabb")};p.applicationInfo=new ApplicationInfo();p.applicationInfo.minSdkVersion=24;return p;}
    private void blocked(PackageInfo installed,PackageInfo apk)throws Exception{try{WorkspaceUpdates.validatePackage("com.termux",installed,apk,new WorkspaceUpdates.Release(manifest(),"v1.0.19",false),28);fail("Accepted unsafe APK");}catch(java.io.IOException expected){}}
    @Test public void apkMustMatchManifestInstalledPackageSignerAndUpgradeVersion()throws Exception{
        PackageInfo installed=pkg(1055),apk=pkg(1056);WorkspaceUpdates.validatePackage("com.termux",installed,apk,new WorkspaceUpdates.Release(manifest(),"v1.0.19",false),28);
        blocked(pkg(1056),apk);blocked(pkg(1057),apk);blocked(installed,null);
        apk=pkg(1056);apk.packageName="other.app";blocked(installed,apk);
        apk=pkg(1056);apk.signatures=new Signature[]{new Signature("ccdd")};blocked(installed,apk);
        apk=pkg(1056);apk.applicationInfo.minSdkVersion=29;blocked(installed,apk);
        apk=pkg(1056);apk.versionName="1.0.20";blocked(installed,apk);
    }
    @Test public void appLogDefaultMigrationIsOneTimeAndPreservesLaterChoices(){
        android.content.Context context=org.robolectric.RuntimeEnvironment.application;
        com.termux.shared.settings.preferences.TermuxAppSharedPreferences prefs=com.termux.shared.settings.preferences.TermuxAppSharedPreferences.build(context);
        context.getSharedPreferences("com.termux_preferences",0).edit().remove("jobs_log_off_default_v1").commit();
        prefs.setLogLevel(null,1);prefs.migrateForkLogDefault();assertEquals(0,prefs.getLogLevel());
        prefs.setLogLevel(null,1);prefs.migrateForkLogDefault();assertEquals(1,prefs.getLogLevel());
        assertEquals(0,com.termux.shared.logger.Logger.DEFAULT_LOG_LEVEL);
    }
    @Test public void automaticChecksRespectTenHoursAndClockRollback(){long now=100000000L;assertTrue(WorkspaceUpdates.due(now,0));assertFalse(WorkspaceUpdates.due(now,now));assertFalse(WorkspaceUpdates.due(now,now-WorkspaceUpdates.CHECK_INTERVAL+1));assertTrue(WorkspaceUpdates.due(now,now-WorkspaceUpdates.CHECK_INTERVAL));assertTrue(WorkspaceUpdates.due(now,now+1));}
    @Test public void sha256IsLowercaseHex()throws Exception{assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",WorkspaceUpdates.hex(MessageDigest.getInstance("SHA-256").digest("abc".getBytes("UTF-8"))));}
}
