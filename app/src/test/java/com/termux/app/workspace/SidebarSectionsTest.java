package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class SidebarSectionsTest {
 Activity a;
 @Before public void setup(){a=Robolectric.buildActivity(Activity.class).setup().get();a.getSharedPreferences("sidebar-sections",0).edit().clear().commit();}
 @After public void finish(){a.finish();}
 WorkspaceSidebar menu(String active){WorkspaceSidebar m=new WorkspaceSidebar(a);for(String key:new String[]{"terminals","agents","servers","app"}){m.section(key,key.equals("terminals")?"Терминальные сессии":key.equals("agents")?"Чаты агентов":key.equals("servers")?"Серверы":"Приложение",0);if(key.equals("terminals")||key.equals("agents"))m.session(key,key,"running",false,true,key.equals(active),()->true,()->fail(),()->fail(),()->fail());else m.action("Existing action",0,()->{});}return m;}
 View head(WorkspaceSidebar m,String key){return m.body.findViewWithTag("sidebar-section-"+key);}
 boolean open(WorkspaceSidebar m,String key){return ((TextView)head(m,key)).getText().toString().endsWith("▾");}
 @Test public void manualCollapsePersistsButActiveOverrideDoesNotOverwriteChoice(){
  WorkspaceSidebar first=menu("agents");head(first,"agents").performClick();head(first,"servers").performClick();assertFalse(open(first,"agents"));first.refreshSessions();assertFalse(open(first,"agents"));first.dispose();a.finish();a=Robolectric.buildActivity(Activity.class).setup().get();
  WorkspaceSidebar second=menu("agents");assertTrue(open(second,"agents"));assertFalse(open(second,"servers"));assertFalse(a.getSharedPreferences("sidebar-sections",0).getBoolean("agents",true));second.dispose();
  WorkspaceSidebar third=menu("terminals");assertFalse(open(third,"agents"));assertTrue(open(third,"terminals"));third.dispose();
  WorkspaceSidebar fourth=menu("");assertFalse(open(fourth,"agents"));assertFalse(open(fourth,"servers"));assertNotNull(fourth.body.findViewWithTag("sidebar-card-app"));fourth.dispose();
 }
 @Test public void narrowLargeTextKeepsHeadersInsideCards(){
  android.content.res.Configuration c=a.getResources().getConfiguration();c.fontScale=1.5f;a.getResources().updateConfiguration(c,a.getResources().getDisplayMetrics());WorkspaceSidebar m=menu("terminals");m.body.measure(View.MeasureSpec.makeMeasureSpec(260,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));m.body.layout(0,0,260,m.body.getMeasuredHeight());for(String key:new String[]{"terminals","agents","servers","app"}){View h=head(m,key);assertTrue(h.getMeasuredWidth()<=260);assertTrue(h.getMeasuredHeight()>0);}m.dispose();
 }
}
