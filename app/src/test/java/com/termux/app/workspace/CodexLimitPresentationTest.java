package com.termux.app.workspace;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class CodexLimitPresentationTest {
    private JSONObject window(long minutes,int used)throws Exception{return new JSONObject().put("windowDurationMins",minutes).put("usedPercent",used).put("resetsAt",1792888860L);}
    @Test public void proWithOnlyWeeklyPrimaryNeverShowsFiveHoursOrInventsSecondary()throws Exception{
        CodexStatus s=new CodexStatus();s.planType="pro";s.accountType="chatgpt";s.limits.put("primary",window(10080,14));
        assertEquals(Arrays.asList("primary"),s.windows());assertEquals("7д: 86%",s.summary());assertEquals("Недельный лимит",CodexStatus.windowLabel(s.limits,"primary",false));assertEquals("Подписка: ChatGPT Pro",s.planLabel());
    }
    @Test public void windowDurationWinsOverSlotAndSubscription()throws Exception{
        CodexStatus s=new CodexStatus();s.planType="plus";s.limits.put("primary",window(10080,40)).put("secondary",window(300,20));
        assertEquals("7д: 60% · 5ч: 80%",s.summary());assertEquals("Пятичасовой лимит",CodexStatus.windowLabel(s.limits,"secondary",false));
        s.limits.put("primary",window(60,5));assertEquals("1ч",CodexStatus.windowLabel(s.limits,"primary",true));
    }
    @Test public void missingPeriodIsNotGuessedFromResetDateOrPosition()throws Exception{
        CodexStatus s=new CodexStatus();s.limits.put("primary",new JSONObject().put("usedPercent",14).put("resetsAt",1792888860L));
        assertEquals("Лимит 1: 86%",s.summary());assertEquals("Лимит · период не указан",CodexStatus.windowLabel(s.limits,"primary",false));
        s.limits.put("primary",window(0,0));assertFalse(s.summary().contains("5ч"));
    }
    @Test public void missingOrInvalidUsageNeverBecomesAnEmptyQuota()throws Exception{
        CodexStatus s=new CodexStatus();assertEquals("Лимиты: нет данных",s.summary());
        assertNull(CodexStatus.remaining(new JSONObject().put("usedPercent","NaN")));assertNull(CodexStatus.remaining(new JSONObject().put("usedPercent",-2)));
        assertEquals(Integer.valueOf(0),CodexStatus.remaining(window(10080,105)));
    }
    @Test public void localSnapshotKeepsSafePlanWithoutReadingAuthFiles()throws Exception{
        File f=File.createTempFile("codex-plan-",".json");JSONObject v=new JSONObject().put("schema",1).put("updated",System.currentTimeMillis()).put("planType","pro").put("accountType","chatgpt").put("limits",new JSONObject().put("primary",window(10080,14)));
        Files.write(f.toPath(),v.toString().getBytes(StandardCharsets.UTF_8));CodexStatus s=CodexStatus.fromFile(f);assertEquals("pro",s.planType);assertEquals("chatgpt",s.accountType);assertEquals("7д: 86%",s.summary());
        v.put("planType","user@example.com").put("accountType","unknown");Files.write(f.toPath(),v.toString().getBytes(StandardCharsets.UTF_8));assertEquals("Подписка: нет данных",CodexStatus.fromFile(f).planLabel());
    }
    @Test public void apiKeyAndUnknownPlansDoNotInventPaidSubscriptions(){
        CodexStatus s=new CodexStatus();s.accountType="apiKey";s.planType="pro";assertEquals("Вход: API-ключ",s.planLabel());
        s.accountType="";s.planType="";assertEquals("Подписка: нет данных",s.planLabel());s.planType="pro_future";assertTrue(s.planLabel().endsWith("pro_future"));
    }
}
