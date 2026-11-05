package com.termux.app.workspace;
import android.app.Application;import android.view.View;import com.termux.app.TermuxActivity;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ClaudeSubscriptionMeterTest {
 TermuxActivity a;long now=1793880000000L;
 @Before public void setup(){a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(com.termux.R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"ru");}
 @After public void close(){a.finish();}
 JSONObject data()throws Exception{return new JSONObject().put("source","native-get-usage").put("available",true).put("updatedAt",now).put("plan","").put("fiveHour",new JSONObject().put("usedPercent",25).put("resetsAt",now+10000)).put("sevenDay",new JSONObject().put("usedPercent",80).put("resetsAt",now+50000));}
 @Test public void nativePercentMeansUsedAndUnknownPlanDoesNotHideWindows()throws Exception{
  ClaudeSubscriptionMeter m=new ClaudeSubscriptionMeter(a);m.show(data(),false,now);assertEquals("5 часов: 75% осталось",m.usage[0].getText().toString());assertEquals("Неделя: 20% осталось",m.usage[1].getText().toString());assertEquals(75,m.bars[0].getProgress());assertEquals(20,m.bars[1].getProgress());assertFalse(m.reset[0].getText().toString().contains("Устаревшие"));assertTrue(m.updated.getText().toString().contains("2026"));
 }
 @Test public void missingInvalidAndApiDataNeverBecomeZeroOrFull()throws Exception{
  ClaudeSubscriptionMeter m=new ClaudeSubscriptionMeter(a);for(JSONObject d:new JSONObject[]{null,new JSONObject(),data().put("available",false),data().put("source","api-headers"),data().put("fiveHour",JSONObject.NULL).put("sevenDay",JSONObject.NULL)}){m.show(d,false,now);for(int i=0;i<2;i++){assertTrue(m.usage[i].getText().toString().contains("Нет данных"));assertEquals(View.GONE,m.bars[i].getVisibility());}}
  for(Object invalid:new Object[]{-1,101,"25",JSONObject.NULL}){JSONObject d=data();d.getJSONObject("fiveHour").put("usedPercent",invalid);m.show(d,false,now);assertEquals(View.GONE,m.bars[0].getVisibility());assertEquals(View.VISIBLE,m.bars[1].getVisibility());}
  JSONObject d=data();d.getJSONObject("fiveHour").put("usedPercent",0);d.getJSONObject("sevenDay").put("usedPercent",100);m.show(d,false,now);assertEquals(100,m.bars[0].getProgress());assertEquals(0,m.bars[1].getProgress());
 }
 @Test public void resetPassageAndStaleReadNeverInventRecovery()throws Exception{
  ClaudeSubscriptionMeter m=new ClaudeSubscriptionMeter(a);JSONObject d=data();m.show(d,false,now+10001);assertEquals(75,m.bars[0].getProgress());assertTrue(m.reset[0].getText().toString().contains("Устаревшие"));assertFalse(m.reset[1].getText().toString().contains("Устаревшие"));
  m.show(d,false,now+90001);assertTrue(m.reset[1].getText().toString().contains("Устаревшие"));m.show(d,true,now);assertTrue(m.reset[0].getText().toString().contains("Устаревшие"));
  m.present(d);m.disconnected();assertEquals(75,m.bars[0].getProgress());assertTrue(m.reset[0].getText().toString().contains("Устаревшие"));m.present(null);assertEquals(View.GONE,m.bars[0].getVisibility());
  d=data().put("updatedAt",now+20000);d.getJSONObject("fiveHour").put("usedPercent",40).put("resetsAt",now+90000);m.show(d,false,now+20000);assertEquals(60,m.bars[0].getProgress());assertFalse(m.reset[0].getText().toString().contains("Устаревшие"));
 }
 @Test public void englishMissingResetAndReopenedViewKeepNativeMeaning()throws Exception{
  WorkspaceLanguage.select(a,"en");JSONObject d=data();d.getJSONObject("fiveHour").remove("resetsAt");ClaudeSubscriptionMeter first=new ClaudeSubscriptionMeter(a),reopened=new ClaudeSubscriptionMeter(a);first.show(d,false,now);reopened.show(d,false,now);assertEquals("5 hours: 75% remaining",first.usage[0].getText().toString());assertEquals("Reset: No data",first.reset[0].getText().toString());assertEquals(first.usage[1].getText().toString(),reopened.usage[1].getText().toString());
 }
}
