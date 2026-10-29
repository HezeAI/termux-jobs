package com.termux.app.workspace;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;
import com.termux.app.TermuxActivity;import com.termux.R;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.io.*;import java.nio.file.*;import java.util.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class EnvironmentUpdatesTest {
 Activity activity;
 void field(String name,Object value)throws Exception{java.lang.reflect.Field f=EnvironmentUpdateJobs.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
 @Before public void setup()throws Exception{activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"ru");field("active",true);field("state",new JSONObject().put("operation","upgrade").put("target","debian").put("phase","copy"));}
 @After public void cleanup()throws Exception{field("active",false);field("cancelRequested",false);field("cancel",null);activity.finish();}
 List<TextView> texts(View root){List<TextView> list=new ArrayList<>();if(root instanceof TextView)list.add((TextView)root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)list.addAll(texts(((ViewGroup)root).getChildAt(i)));return list;}
 TextView find(View root,String value){for(TextView t:texts(root))if(t.getText().toString().startsWith(value))return t;throw new AssertionError(value);}
 JSONObject inventory()throws Exception{JSONObject i=new JSONObject().put("native",true);JSONArray rows=new JSONArray();for(String t:Arrays.asList("ubuntu","debian","kali","alpine","fedora","opensuse","void","archlinux","manjaro"))rows.put(new JSONObject().put("target",t).put("installed",true).put("system",new JSONObject().put("PRETTY_NAME",t)));return i.put("targets",rows);}
 @Test public void nineTargetsAreShownAndMutationsDisabledWhileJobRuns()throws Exception{
  LinearLayout rows=WorkspaceEnvironmentUpdates.column(activity);WorkspaceEnvironmentUpdates.render(activity,rows,inventory(),new JSONObject(),true);
  assertEquals(14,rows.getChildCount());assertFalse(find(rows,"Debian").isEnabled());assertFalse(find(rows,"Проверить пакеты").isEnabled());assertFalse(find(rows,"Новые официальные").isEnabled());assertTrue(find(rows,"Termux и Ubuntu AI").getText().toString().contains("прежний"));assertTrue(EnvironmentUpdateJobs.mutating("debian"));assertFalse(EnvironmentUpdateJobs.mutating("ubuntu-ai"));
 }
 @Test public void closingSettingsDoesNotCancelOrStopOwnedUpdate()throws Exception{
  WorkspaceEnvironmentUpdates.show(activity);AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(d.isShowing());d.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(EnvironmentUpdateJobs.active());assertFalse(EnvironmentUpdateJobs.state().optBoolean("stopping"));
 }
 @Test public void stopBeforeWorkerStartsPersistsRequestWithoutAnySignal()throws Exception{
  EnvironmentUpdateJobs.cancel();assertTrue(EnvironmentUpdateJobs.state().optBoolean("stopping"));assertTrue(EnvironmentUpdateJobs.active());java.lang.reflect.Field f=EnvironmentUpdateJobs.class.getDeclaredField("cancelRequested");f.setAccessible(true);assertEquals(true,f.get(null));
 }
 @Test public void rollbackConfirmationDefaultsToHomePreservationAndExplainsOtherFiles()throws Exception{
  WorkspaceEnvironmentUpdates.confirm(activity,"Debian","restore",EnvironmentUpdateJobs.request("rollback","debian"),q->q.put("snapshot",UUID.randomUUID().toString()),true);
  AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();CheckBox box=(CheckBox)find(d.getWindow().getDecorView(),"Сохранить текущие");assertTrue(box.isChecked());assertNotNull(find(d.getWindow().getDecorView(),"Файлы вне этих"));d.dismiss();assertTrue(EnvironmentUpdateJobs.active());
 }
 @Test public void imageAndPackageActionsRemainSeparateAndCandidateSolverLimitIsVisible()throws Exception{
  JSONObject row=new JSONObject().put("target","debian").put("installed",true).put("plan",new JSONObject().put("id",UUID.randomUUID().toString()).put("candidates",new JSONArray().put(new JSONObject().put("name","demo").put("installed","1").put("version","2"))));
  WorkspaceEnvironmentUpdates.details(activity,row,new JSONObject().put("release","rootfs-arm64-20261028").put("sha256",String.join("",Collections.nCopies(64,"a"))));AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View root=d.getWindow().getDecorView();
  assertNotNull(find(root,"Проверить обновления пакетов"));assertNotNull(find(root,"Вернуть закреплённый"));assertNotNull(find(root,"Установить этот базовый"));
  find(root,"Посмотреть список").performClick();AlertDialog list=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(find(list.getWindow().getDecorView(),"Кандидаты").getText().toString().contains("удаления"));list.dismiss();d.dismiss();
 }
 @Test public void internalAliasesAreHiddenOnlyWithValidOwnedJournal()throws Exception{
  Path fixture=Files.createTempDirectory(activity.getFilesDir().toPath(),"update-choices");File home=fixture.resolve("home").toFile();assertTrue(home.mkdir());File proot=fixture.resolve("usr/var/lib/proot-distro").toFile();assertTrue(proot.mkdirs());String id=UUID.randomUUID().toString(),alias="jobs-update-"+id;
  assertTrue(new File(proot,"containers/"+alias+"/rootfs").mkdirs());assertFalse(WorkspaceEnvironments.internalAlias(proot,alias));assertEquals(3,WorkspaceLocalFilePicker.locations(home,proot,"").size());
  Path journal=fixture.resolve("workspace-environment-updates/jobs/"+id+"/state.json");Files.createDirectories(journal.getParent());Files.write(journal,new JSONObject().put("owner","termux-jobs-environment-updates").put("id",id).put("temporary",alias).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  assertTrue(WorkspaceEnvironments.internalAlias(proot,alias));assertEquals(2,WorkspaceLocalFilePicker.locations(home,proot,"").size());
  Path filesAlias=fixture.resolveSibling("files-alias-"+id);Files.createSymbolicLink(filesAlias,fixture);File aliasedProot=filesAlias.resolve("usr/var/lib/proot-distro").toFile();assertTrue(WorkspaceEnvironments.internalAlias(aliasedProot,alias));assertEquals(2,WorkspaceLocalFilePicker.locations(home,aliasedProot,"").size());
 }
 @Test public void staleStopRequestCannotCancelNextJobOrCreateItsMarker()throws Exception{
  String current=UUID.randomUUID().toString();field("state",new JSONObject().put("id",current));File marker=new File(activity.getFilesDir(),"cancel-current-update");field("cancel",marker);
  EnvironmentUpdateJobs.cancel(UUID.randomUUID().toString());assertFalse(marker.exists());assertFalse(EnvironmentUpdateJobs.state().optBoolean("stopping"));
  EnvironmentUpdateJobs.cancel(current);assertTrue(marker.exists());assertTrue(EnvironmentUpdateJobs.state().optBoolean("stopping"));assertTrue(marker.delete());
 }
 @Test public void officialReleasesCompareNumericBranchesWithoutTreatingPointFixesAsMigration()throws Exception{
  JSONObject old=new JSONObject().put("ID","ubuntu").put("VERSION_ID","26.04");JSONObject upstream=new JSONObject().put("kind","release").put("series","26.10");
  assertTrue(WorkspaceEnvironmentUpdates.releaseStatus(activity,"ubuntu",old,upstream).contains("более новый"));
  old.put("VERSION_ID","26.10.1");assertTrue(WorkspaceEnvironmentUpdates.releaseStatus(activity,"ubuntu",old,upstream).contains("совпадает"));
  old.put("VERSION_ID","27.04");assertTrue(WorkspaceEnvironmentUpdates.releaseStatus(activity,"ubuntu",old,upstream).contains("новее проверенного"));
  old.put("VERSION_ID","999999999999999999999999");assertTrue(WorkspaceEnvironmentUpdates.releaseStatus(activity,"ubuntu",old,upstream).contains("недоступно"));
  upstream.put("kind","rolling");assertTrue(WorkspaceEnvironmentUpdates.releaseStatus(activity,"kali",null,upstream).startsWith("Rolling"));
  WorkspaceLanguage.select(activity,"en");upstream.put("kind","release");old.put("VERSION_ID","26.04");assertTrue(WorkspaceEnvironmentUpdates.releaseStatus(activity,"ubuntu",old,upstream).contains("newer official release"));
 }
 @Test public void officialDiscoveryShowsPerSourceErrorsAndNoInstallAction()throws Exception{
  JSONObject ubuntu=new JSONObject().put("kind","release").put("version","26.10").put("series","26.10").put("source","https://releases.ubuntu.com/").put("image","docker.io/library/ubuntu:26.10");
  JSONObject result=new JSONObject().put("checkedAt",1).put("targets",new JSONObject().put("ubuntu",ubuntu).put("opensuse",new JSONObject().put("error","OFFICIAL_NETWORK")));
  LinearLayout rows=WorkspaceEnvironmentUpdates.column(activity);WorkspaceEnvironmentUpdates.officialRows(activity,rows,inventory(),result,false);
  assertNotNull(find(rows,"Официальный образ: 26.10"));assertNotNull(find(rows,"Не удалось подключиться"));assertNotNull(find(rows,"Открыть официальный источник"));
  for(TextView t:texts(rows))assertFalse(t.getText().toString().startsWith("Установить"));
  assertFalse(EnvironmentUpdateJobs.mutating("ubuntu"));
 }
 @Test public void closingOfficialWindowPreservesTheJobAndCachedResults()throws Exception{
  JSONObject cached=new JSONObject().put("checkedAt",1).put("targets",new JSONObject());field("officialImages",cached);field("state",new JSONObject().put("operation","official-images").put("target","debian").put("phase","checking-official-images"));
  WorkspaceEnvironmentUpdates.official(activity);AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(find(d.getWindow().getDecorView(),"Остановить").isShown());d.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(EnvironmentUpdateJobs.active());assertFalse(EnvironmentUpdateJobs.state().optBoolean("stopping"));assertEquals(1,EnvironmentUpdateJobs.officialImages().optLong("checkedAt"));
 }

}
