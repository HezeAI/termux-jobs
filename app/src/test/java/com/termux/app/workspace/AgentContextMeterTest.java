package com.termux.app.workspace;
import android.app.*;import android.view.View;import android.widget.*;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.lang.reflect.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentContextMeterTest {
 com.termux.app.TermuxActivity a;
 @Before public void before(){a=Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();a.setTheme(com.termux.R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"ru");}
 @After public void after(){a.finish();}
 JSONObject context(long used,Object capacity,boolean stale,String model)throws Exception{return new JSONObject().put("session","synthetic").put("stale",stale).put("usage",new JSONObject().put("used",used).put("capacity",capacity).put("model",model));}
 static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static void call(Object o,String n)throws Exception{Method m=o.getClass().getDeclaredMethod(n);m.setAccessible(true);m.invoke(o);}
 static void menu(Object o,String n,WorkspaceStore.Session p)throws Exception{Method m=o.getClass().getDeclaredMethod(n,WorkspaceStore.Session.class);m.setAccessible(true);m.invoke(o,p);}
 @Test public void unknownEstimateWindowAndCachedInputStayDistinct()throws Exception{
  AgentContextMeter meter=new AgentContextMeter(a,"Claude");assertEquals("Нет данных",meter.usage.getText().toString());assertEquals(View.GONE,meter.bar.getVisibility());
  meter.present(context(333,JSONObject.NULL,false,"actual-model"));assertTrue(meter.usage.getText().toString().contains("333"));assertFalse(meter.usage.getText().toString().contains("%"));assertEquals(View.GONE,meter.bar.getVisibility());
  meter.present(context(128000,256000,false,"model-a"));assertTrue(meter.usage.getText().toString().contains("50%"));assertTrue(meter.usage.getText().toString().contains("128 000 / 256 000"));assertEquals(50,meter.bar.getProgress());
  meter.present(context(128000,256000,true,"model-a"));assertTrue(meter.note.getText().toString().contains("Устаревшие"));meter.present(context(32000,128000,false,"model-b"));assertEquals(25,meter.bar.getProgress());assertTrue(meter.note.getText().toString().contains("model-b"));assertFalse(meter.note.getText().toString().contains("Устаревшие"));
  meter.present(context(0,1000,false,"m"));assertTrue(meter.usage.getText().toString().contains("0%"));meter.present(new JSONObject());assertEquals("Нет данных",meter.usage.getText().toString());
 }
 @Test public void englishAndInvalidCountsNeverInventWindowOrZero()throws Exception{
  WorkspaceLanguage.select(a,"en");AgentContextMeter meter=new AgentContextMeter(a,"Gemini");assertEquals("No data",meter.usage.getText().toString());meter.present(context(333,JSONObject.NULL,true,"gemini-routed"));assertTrue(meter.usage.getText().toString().contains("Estimate"));assertTrue(meter.note.getText().toString().contains("Stale"));
  for(Object bad:new Object[]{"123",-1,1.5,JSONObject.NULL}){JSONObject c=context(1,1000,false,"m");c.getJSONObject("usage").put("used",bad);meter.present(c);assertEquals("No data",meter.usage.getText().toString());assertEquals(View.GONE,meter.bar.getVisibility());}
 }
 @Test public void claudeActualExpandedMenuAppendsMeterAndDoesNotAlterChip()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.claudeAgent=true;final WorkspaceStore.Session[] selected={p};ClaudeLiveUi ui=new ClaudeLiveUi(a,()->selected[0],()->false,(x,y,z,w)->{});
  try{TextView chip=new TextView(a);set(ui,"chip",chip);set(ui,"run",p.claudeSocketId);JSONObject status=new JSONObject().put("ready",true).put("state","idle").put("model","sonnet").put("mode","manual").put("context",context(170,1000,false,"claude-native"));set(ui,"status",status);call(ui,"update");String collapsed=chip.getText().toString();menu(ui,"menu",p);AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();AgentContextMeter meter=dialog.getWindow().getDecorView().findViewWithTag("agent-context-meter");assertNotNull(meter);assertTrue(meter.usage.getText().toString().contains("17%"));assertEquals(collapsed,chip.getText().toString());LinearLayout parent=(LinearLayout)meter.getParent();View rtk=parent.findViewWithTag("rtk-mode");assertNotNull(rtk);assertEquals(parent.indexOfChild(meter)+1,parent.indexOfChild(rtk));assertSame(rtk,parent.getChildAt(parent.getChildCount()-1));assertEquals(4,meter.getChildCount());
   status.put("context",context(50,500,false,"changed"));call(ui,"update");assertEquals(10,meter.bar.getProgress());status.put("context",JSONObject.NULL);call(ui,"update");assertEquals("Нет данных",meter.usage.getText().toString());
   status.put("context",context(70,1000,true,"reopened"));call(ui,"update");assertTrue(meter.note.getText().toString().contains("Устаревшие"));selected[0]=new WorkspaceStore.Session();menu(ui,"menu",p);assertSame(dialog,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());ui.render(new LinearLayout(a),selected[0]);assertFalse(dialog.isShowing());menu(ui,"menu",selected[0]);AgentContextMeter other=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView().findViewWithTag("agent-context-meter");assertEquals("Нет данных",other.usage.getText().toString());
  }finally{ui.close();}
 }
 @Test public void geminiActualExpandedMenuUsesActualModelEstimateAndNewSession()throws Exception{
  WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=true;final WorkspaceStore.Session[] selected={p};GeminiLiveUi ui=new GeminiLiveUi(a,()->selected[0],()->false);
  try{TextView chip=new TextView(a);set(ui,"chip",chip);set(ui,"run",p.geminiSocketId);GeminiLiveStatus status=new GeminiLiveStatus();status.available=true;status.state="idle";status.model="auto";status.context=context(333,JSONObject.NULL,false,"gemini-actual");set(ui,"status",status);call(ui,"update");String collapsed=chip.getText().toString();menu(ui,"details",p);AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();AgentContextMeter meter=dialog.getWindow().getDecorView().findViewWithTag("agent-context-meter");assertNotNull(meter);assertTrue(meter.usage.getText().toString().contains("333"));assertTrue(meter.note.getText().toString().contains("gemini-actual"));assertEquals(View.GONE,meter.bar.getVisibility());assertEquals(collapsed,chip.getText().toString());LinearLayout parent=(LinearLayout)meter.getParent();View rtk=parent.findViewWithTag("rtk-mode");assertNotNull(rtk);assertEquals(parent.indexOfChild(meter)+1,parent.indexOfChild(rtk));assertSame(rtk,parent.getChildAt(parent.getChildCount()-1));assertEquals(4,meter.getChildCount());
   status.context=context(100,1000,false,"known-native");call(ui,"update");assertEquals(10,meter.bar.getProgress());selected[0]=new WorkspaceStore.Session();call(ui,"update");assertFalse(dialog.isShowing());
   selected[0]=p;status.context=context(100,1000,true,"known-native");menu(ui,"details",p);meter=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView().findViewWithTag("agent-context-meter");assertTrue(meter.note.getText().toString().contains("Устаревшие"));
  }finally{ui.close();}
 }
}
