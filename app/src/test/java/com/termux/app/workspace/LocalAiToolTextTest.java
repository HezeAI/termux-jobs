package com.termux.app.workspace;
import android.app.Application;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiToolTextTest {
    static final String LIST="{\"tool\":\"list\",\"path\":\"/sdcard\",\"filter\":\"image\",\"sort\":\"modified_desc\",\"limit\":10}";
    @Test public void greetingAndPoliteScreenshotRequestsAreFoundButQuestionsAndNegationsAreNot()throws Exception{
        for(String text:new String[]{"привет посмотри ласт скрин","Привет! Посмотри, пожалуйста, последние 3 скрина","можешь посмотреть последний скрин?","hello, please show latest 3 screenshots","посмотри ласт скрин и скажи что не так","please show the last screenshot","look at the latest screenshot","could you please show me the last screenshot?","check last screenshot","screenshot","скрин","show screenshot"}){JSONObject r=LocalAiAgent.screenshotRequest(new JSONArray().put(new JSONObject().put("role","user").put("text",text)));assertNotNull(text,r);assertEquals(text.contains("3")?3:1,r.getInt("limit"));}
        for(String text:new String[]{"привет, как найти последний скрин?","привет не открывай последний скрин","посмотри Screenshot121.jpg","посмотри последний скрин /other/name.jpg"})assertNull(text,LocalAiAgent.screenshotRequest(new JSONArray().put(new JSONObject().put("role","user").put("text",text))));
    }
    @Test public void mixedProseBecomesPreviewButNeverExecutableRequest()throws Exception{String raw="Давайте посмотрим:\n"+LIST;LocalAiToolText.Preview p=LocalAiToolText.preview(raw);assertEquals("Давайте посмотрим:",p.text);assertEquals("/sdcard",p.request.getString("path"));assertNull(LocalAiTools.request(raw));assertEquals(raw,"Давайте посмотрим:\n"+LIST);}
    @Test public void pureToolJsonAndCodeFenceUseSameCard(){for(String raw:new String[]{LIST,"```json\n"+LIST+"\n```","{\"path\":\"/sdcard\",\"tool\":\"list\"}"}){LocalAiToolText.Preview p=LocalAiToolText.preview(raw);assertEquals("",p.text);assertNotNull(p.request);assertFalse(p.pending);}}
    @Test public void streamedJsonIsHiddenUntilCompleteAndUserDataIsNotARequest(){for(String raw:new String[]{"{","{\"to","{\"tool\":\"li","checking\n{\"tool\":\"list\",\"path\":\"/sd"}){LocalAiToolText.Preview p=LocalAiToolText.preview(raw);assertTrue(raw,p.pending);assertFalse(p.text.contains("{\"tool"));assertNull(p.request);}assertFalse(LocalAiToolText.preview("void main() {").pending);assertEquals("Data {\"other\":1}",LocalAiToolText.preview("Data {\"other\":1}").text);}
    @Test public void escapedBracesInsideArgumentsDoNotBreakPreview()throws Exception{String json=new JSONObject().put("tool","write").put("path","note.txt").put("text","quoted \" } { literal").toString();LocalAiToolText.Preview p=LocalAiToolText.preview("Explanation\n"+json+"\nAfter");assertNotNull(p.request);assertEquals("quoted \" } { literal",p.request.getString("text"));assertEquals("Explanation\n\nAfter",p.text);assertNull(LocalAiTools.request("Explanation\n"+json));}
    @Test public void mixedRequestIsRepairedOnceBeforeNormalAuthorizedExecution()throws Exception{LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.answers.add("Давайте посмотрим:\n"+LIST);f.answers.add(LIST);f.answers.add("Found");assertEquals("Found",f.run());assertEquals(1,f.calls.size());assertTrue(f.systems.get(1).contains("Embedded JSON was NOT executed"));assertTrue(f.history.getJSONObject(1).getBoolean("repair"));}
    @Test public void examplesInQuestionNeverBecomeExecutableOrClaimSuccess()throws Exception{
        for(String prompt:new String[]{"Что означает такой формат?","покажи пример JSON","show an example"}){LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.history=new JSONArray().put(new JSONObject().put("role","user").put("text",prompt));f.answers.add("Example:\n"+LIST);f.run();assertTrue(f.calls.isEmpty());assertEquals(1,f.systems.size());}
    }
    @Test public void mixedWriteCannotBypassReadPermissionOnRepair()throws Exception{LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localAccess="read";String write="{\"tool\":\"write\",\"path\":\"note.txt\",\"text\":\"hi\"}";f.answers.add("Do it:\n"+write);f.answers.add(write);try{f.run();fail();}catch(SecurityException expected){}assertTrue(f.calls.isEmpty());}
}
