package com.termux.app.workspace;

import android.app.*;import android.content.Context;import android.os.Looper;import android.view.*;import android.widget.*;
import com.termux.app.TermuxActivity;import com.termux.R;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import java.time.Duration;import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows=EnvironmentUpdateMenuTest.JobsShadow.class)
public class EnvironmentUpdateMenuTest {
 @Implements(value=EnvironmentUpdateJobs.class,isInAndroidSdk=false)
 public static class JobsShadow {
  static JSONObject requested;
  @Implementation protected static boolean start(Context c,JSONObject q){requested=q;return true;}
 }
 Activity activity;
 void field(String name,Object value)throws Exception{java.lang.reflect.Field f=EnvironmentUpdateJobs.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
 @Before public void setup()throws Exception{
  activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"ru");JobsShadow.requested=null;
  field("active",false);field("state",new JSONObject());field("inventory",new JSONObject());field("officialImages",new JSONObject());field("revision",0L);
 }
 @After public void cleanup()throws Exception{field("active",false);activity.finish();}
 TextView find(View v,String prefix){if(v instanceof TextView&&((TextView)v).getText().toString().startsWith(prefix))return (TextView)v;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){TextView found=find(((ViewGroup)v).getChildAt(n),prefix);if(found!=null)return found;}return null;}
 JSONObject row(boolean installed)throws Exception{return new JSONObject().put("target","debian").put("installed",installed).put("system",new JSONObject().put("ID","debian").put("VERSION_ID","13").put("PRETTY_NAME","Debian 13"));}
 AlertDialog show(boolean installed)throws Exception{WorkspaceEnvironmentUpdates.details(activity,row(installed),null);return org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();}
 @Test public void packageCheckDoesNotDismissSelectedMenuAndResultUpdatesInPlace()throws Exception{
  AlertDialog d=show(true);find(d.getWindow().getDecorView(),"Проверить обновления пакетов").performClick();assertTrue(d.isShowing());assertSame(d,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());assertEquals("check",JobsShadow.requested.getString("operation"));assertEquals("debian",JobsShadow.requested.getString("target"));
  JSONObject updated=row(true).put("plan",new JSONObject().put("id","00000000-0000-0000-0000-000000000001").put("candidates",new JSONArray().put(new JSONObject().put("name","demo").put("version","2"))));field("inventory",new JSONObject().put("targets",new JSONArray().put(updated)));field("revision",1L);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(310));
  assertTrue(d.isShowing());assertNotNull(find(d.getWindow().getDecorView(),"Посмотреть список · 1"));d.dismiss();
 }
 @Test public void officialCheckIsScopedToSelectedEnvironmentWithoutNavigatingAway()throws Exception{
  AlertDialog d=show(true);find(d.getWindow().getDecorView(),"Проверить официальный образ").performClick();assertTrue(d.isShowing());assertEquals("official-images",JobsShadow.requested.getString("operation"));assertEquals("debian",JobsShadow.requested.getString("target"));d.dismiss();
 }
 @Test public void absentEnvironmentCanCheckAndInstallOfficialImageWithConfirmation()throws Exception{
  field("officialImages",new JSONObject().put("targets",new JSONObject().put("debian",new JSONObject().put("version","13.7").put("series","13").put("kind","release").put("image","docker.io/library/debian:stable").put("plan","00000000-0000-0000-0000-000000000001").put("source","https://www.debian.org/releases/stable/"))));
  AlertDialog selected=show(false);View root=selected.getWindow().getDecorView();assertNull(find(root,"Проверить обновления пакетов"));find(root,"Установить официальный образ").performClick();AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(selected,confirm);assertTrue(selected.isShowing());assertNotNull(find(confirm.getWindow().getDecorView(),"Будет установлена выбранная официальная база"));assertNull(find(confirm.getWindow().getDecorView(),"Сохранить текущие /root"));confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNotNull(JobsShadow.requested);assertEquals("official-install",JobsShadow.requested.getString("operation"));assertTrue(selected.isShowing());selected.dismiss();
 }
 @Test public void officialUpdateConfirmationExplainsApplicationReplacementAndOffersHomes()throws Exception{
  field("officialImages",new JSONObject().put("targets",new JSONObject().put("debian",new JSONObject().put("version","13.7").put("series","13").put("kind","release").put("plan","00000000-0000-0000-0000-000000000001"))));
  AlertDialog d=show(true);find(d.getWindow().getDecorView(),"Обновить из официального образа").performClick();AlertDialog confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(find(confirm.getWindow().getDecorView(),"Сохранить текущие /root и /home"));assertTrue(find(confirm.getWindow().getDecorView(),"Будет установлена выбранная официальная база").getText().toString().contains("установить заново"));confirm.dismiss();d.dismiss();
 }
}
