package com.termux.app.workspace;
import android.app.Application;import org.json.*;import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class RtkStatisticsTest {
 @Test public void headWindowUnknownBaseAndRecallRemainDistinct()throws Exception{
  JSONObject basic=new JSONObject().put("commands",1).put("comparedCommands",1).put("noReductionCommands",1).put("inputBytes",450).put("outputBytes",450).put("recallBytes",1000).put("recallCommands",1);
  JSONObject advanced=new JSONObject().put("commands",1).put("unknownBaselineCommands",1).put("knownOutputBytes",123);
  String text=RtkStatistics.text(new JSONObject().put("version",2).put("basic",basic).put("advanced",advanced),true);
  assertTrue(text.contains("450 → 450 байт · 0.0%"));assertTrue(text.contains("через recall: 1000 байт"));assertTrue(text.contains("база неизвестна"));assertTrue(text.contains("Получено всего: 123 байт"));assertFalse(text.contains("0 → 123"));assertTrue(text.contains("без сокращения: 1"));
 }
 @Test public void legacyAndStartDateAreHiddenWithoutChangingStoredData()throws Exception{
  JSONObject s=new JSONObject().put("version",2).put("basic",new JSONObject().put("commands",1).put("unconfirmedCommands",1)).put("legacy",new JSONObject().put("advanced",new JSONObject().put("commands",2).put("input",9000).put("output",3000)));
  s.put("startedAt",1794100000000L);String original=s.toString();String ru=RtkStatistics.text(s,true),en=RtkStatistics.text(s,false);assertFalse(ru.contains("Прежняя оценка"));assertFalse(ru.contains("9000"));assertFalse(ru.contains("Новый расчёт с"));assertEquals(original,s.toString());assertTrue(ru.contains("не подтверждено: 1"));assertFalse(ru.contains("66.7%"));assertFalse(en.contains("Previous estimate"));assertFalse(en.contains("New measurements since"));assertTrue(en.contains("baseline unknown"));
 }
 @Test public void summaryUsesSelectedModeAndNeverLegacyOrPartialBaseline()throws Exception{
  JSONObject basic=new JSONObject().put("commands",1).put("comparedCommands",1).put("inputBytes",48000).put("outputBytes",18000);
  JSONObject nativeMode=new JSONObject().put("commands",1).put("unknownBaselineCommands",1).put("knownOutputBytes",18000);
  JSONObject stats=new JSONObject().put("version",2).put("basic",basic).put("advanced",nativeMode);
  String original=stats.toString();String known=RtkStatistics.summary(stats,"basic",true);
  assertTrue(known.contains("62,5%"));assertTrue(known.contains("≈12"));
  String unknown=RtkStatistics.summary(stats,"advanced",true);assertTrue(unknown.startsWith("Получено:"));assertTrue(unknown.contains("сокращение неизвестно"));assertFalse(unknown.contains("→"));assertEquals(original,stats.toString());
  basic.put("unknownBaselineCommands",1);assertTrue(RtkStatistics.summary(stats,"basic",true).contains("62,5%"));assertTrue(RtkStatistics.summary(stats,"basic",true).contains("измеренная часть"));
  nativeMode.put("knownOutputBytes",0).put("unknownOutputCommands",1);assertTrue(RtkStatistics.summary(stats,"advanced",true).contains("объём неизвестен"));
 }

}
