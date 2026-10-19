package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.TextView;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class CodexActionTimelineTest {
    private JSONObject shell(String key,String turn,String command,String status)throws Exception{return new JSONObject().put("key",key).put("kind","tool").put("title","Shell · "+status).put("body",command+"\noriginal output").put("message",key+"-message").put("part","command").put("actionType","shell").put("turn",turn).put("command",command).put("status",status);}
    private JSONObject text(String key,String kind)throws Exception{return new JSONObject().put("key",key).put("kind",kind).put("body",key);}
    private AgentChatView view(Activity a,AtomicReference<JSONObject> opened){AgentChatView v=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});v.codexActions(true);v.assistantName("Codex");a.setContentView(v);return v;}

    @Test public void onlyConsecutiveNativeShellsInTheSameTurnAreGrouped()throws Exception{
        JSONObject first=shell("one","turn-a","printf a && printf b","completed"),second=shell("two","turn-a","pwd","inProgress");
        List<JSONObject> grouped=CodexActionTimeline.group(Arrays.asList(first,second,text("answer","text"),shell("three","turn-b","ls","completed")));
        assertEquals(3,grouped.size());assertEquals(2,grouped.get(0).getJSONArray("steps").length());assertSame(first,grouped.get(0).getJSONArray("steps").getJSONObject(0));assertEquals("text",grouped.get(1).getString("kind"));
        List<JSONObject> turns=CodexActionTimeline.group(Arrays.asList(first,shell("next","turn-b","pwd","completed")));assertEquals(2,turns.size());
        for(String kind:new String[]{"receipt","reasoning","user","error"}){JSONObject barrier=text("keep",kind);List<JSONObject> rows=CodexActionTimeline.group(Arrays.asList(first,barrier,second));assertEquals(3,rows.size());assertSame(barrier,rows.get(1));}
        assertEquals("printf a && printf b",CodexActionTimeline.command(first)); // A compound command remains one step.
    }
    @Test public void legacyRowsGroupWithoutInventingDurationOrAffectingOtherAgents()throws Exception{
        JSONObject old=new JSONObject().put("key","old").put("kind","tool").put("title","Shell · completed").put("body","ls /sdcard/Download\noutput");
        assertTrue(CodexActionTimeline.shell(old));assertEquals("ls /sdcard/Download",CodexActionTimeline.command(old));assertFalse(old.has("durationMs"));
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView normal=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});normal.rows(Arrays.asList(old,shell("two","t","pwd","completed")),false);assertTrue(normal.timeline.getChildAt(1) instanceof TextView);assertTrue(normal.timeline.getChildAt(2) instanceof TextView);
    }
    @Test public void streamingKeepsCardExpansionAndStepIdentityAndOpensOriginalOutput()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=view(a,opened);
        JSONObject one=shell("one","t","pwd","completed"),two=shell("two","t","ls","inProgress");v.rows(Arrays.asList(one,two),false);CodexActionsView card=(CodexActionsView)v.timeline.getChildAt(1);assertEquals(View.GONE,card.steps.getVisibility());assertTrue(card.summary.getText().toString().contains("ls"));card.heading.performClick();View first=card.steps.getChildAt(0);assertEquals(View.VISIBLE,card.steps.getVisibility());
        two.put("body","ls\nnew native output").put("status","completed").put("exitCode",0).put("durationMs",250);JSONObject third=shell("three","t","uname -a","inProgress");v.rows(Arrays.asList(one,two,third),false);assertSame(card,v.timeline.getChildAt(1));assertSame(first,card.steps.getChildAt(0));assertEquals(View.VISIBLE,card.steps.getVisibility());assertEquals(3,card.steps.getChildCount());assertEquals(3,v.timeline.getChildCount());card.steps.getChildAt(1).performClick();assertSame(two,opened.get());assertEquals("two-message",opened.get().getString("message"));assertEquals("command",opened.get().getString("part"));assertTrue(opened.get().getString("body").contains("new native output"));assertTrue(((TextView)card.steps.getChildAt(1)).getText().toString().contains("250 мс"));
    }
    @Test public void failuresAndRussianEnglishLabelsUseNativeFacts()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=view(a,opened);JSONObject failure=shell("one","t","false","completed").put("exitCode",1),denied=shell("two","t","rm file","declined");
        WorkspaceLanguage.select(a,"ru");v.rows(Arrays.asList(failure,denied),false);CodexActionsView card=(CodexActionsView)v.timeline.getChildAt(1);card.heading.performClick();assertTrue(card.heading.getText().toString().contains("Хронология"));assertTrue(card.summary.getText().toString().contains("Одна команда завершилась с ошибкой"));assertTrue(((TextView)card.steps.getChildAt(0)).getText().toString().contains("код 1"));assertTrue(((TextView)card.steps.getChildAt(1)).getText().toString().contains("Отклонено"));assertFalse(((TextView)card.steps.getChildAt(0)).getText().toString().contains("мс"));
        WorkspaceLanguage.select(a,"en");failure.put("body","false\nupdated");v.rows(Arrays.asList(failure,denied),false);assertSame(card,v.timeline.getChildAt(1));assertTrue(card.summary.getText().toString().contains("One command failed"));assertTrue(((TextView)card.steps.getChildAt(0)).getText().toString().contains("exit 1"));assertTrue(((TextView)card.steps.getChildAt(1)).getText().toString().contains("Declined"));
    }
    private void layout(AgentChatView v){for(int i=0;i<3;i++){v.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));v.layout(0,0,600,700);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}}
    @Test public void updatesBelowTheViewportDoNotMoveReaderOrReplaceNeighborMessages()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=view(a,new AtomicReference<>());List<JSONObject> rows=new ArrayList<>();for(int i=0;i<25;i++)rows.add(text("message"+i,"text").put("body","message "+i+"\nlonger text\nthird line"));JSONObject command=shell("active","t","pwd","inProgress");rows.add(command);v.rows(rows,false);layout(v);v.scroll.scrollTo(0,500);int y=v.scroll.getScrollY();View neighbor=v.timeline.getChildAt(3);command.put("body","pwd\nnew output");v.rows(rows,false);layout(v);assertEquals(y,v.scroll.getScrollY());assertSame(neighbor,v.timeline.getChildAt(3));
    }
    @Test public void skillCloudKeepsRealStepsAndStopsAtMessagesOrAnotherSkill()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=view(a,opened);
        JSONObject read=shell("read","t","cat /root/skills/imagegen/SKILL.md","completed").put("skillName","imagegen").put("skillSource","read").put("skillContextName","imagegen");
        JSONObject image=new JSONObject().put("key","image").put("kind","tool").put("message","image-msg").put("part","event").put("title","imageView").put("toolType","imageView").put("status","completed").put("turn","t").put("skillContextName","imagegen").put("body","original image event");
        JSONObject next=shell("next","t","pwd","completed").put("skillContextName","openai-docs");v.rows(Arrays.asList(read,image,next),false);CodexActionsView cloud=(CodexActionsView)v.timeline.getChildAt(1);assertTrue(cloud.heading.getText().toString().contains("Навык: imagegen"));assertEquals(View.GONE,cloud.summary.getVisibility());cloud.heading.performClick();assertEquals(2,cloud.steps.getChildCount());assertTrue(((TextView)cloud.steps.getChildAt(1)).getText().toString().contains("Просмотр изображения"));cloud.steps.getChildAt(1).performClick();assertSame(image,opened.get());assertEquals(4,v.timeline.getChildCount());
        List<JSONObject> split=CodexActionTimeline.group(Arrays.asList(read,text("answer","text"),image));assertEquals(3,split.size());
    }
    @Test public void friendlyNamesDoNotGuessUnknownToolsOrTreatImagesAsSkills()throws Exception{
        JSONObject image=text("image","tool").put("title","image_view");assertEquals("Просмотр изображения",CodexActionLabels.title(image,true));assertEquals("View image",CodexActionLabels.title(image,false));
        JSONObject mcp=text("mcp","tool").put("title","mcpToolCall").put("toolType","mcpToolCall").put("toolName","mcp__image_gen__imagegen");assertEquals("Создание изображения",CodexActionLabels.title(mcp,true));
        JSONObject skill=text("skill","tool").put("title","Skill").put("toolType","skill").put("skillName","my-custom-skill");assertEquals("Подключён навык: my-custom-skill",CodexActionLabels.title(skill,true));assertFalse(CodexActionLabels.title(image,true).contains("навык"));
        JSONObject unknown=text("unknown","tool").put("title","OriginalTool").put("toolType","unknown");assertEquals("OriginalTool",CodexActionLabels.title(unknown,true));
    }
    @Test public void completedCardIsCompactAndUnknownStatusDoesNotFabricateCompletion()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=view(a,new AtomicReference<>());v.rows(Arrays.asList(shell("one","t","pwd","completed")),false);layout(v);CodexActionsView card=(CodexActionsView)v.timeline.getChildAt(1);assertEquals(View.GONE,card.summary.getVisibility());assertTrue(card.getMeasuredHeight()<=v.dp(65));
        JSONObject unknown=shell("one","t","pwd","unknown");v.rows(Arrays.asList(unknown),false);card.heading.performClick();assertFalse(((TextView)card.steps.getChildAt(0)).getText().toString().startsWith("✓"));
    }

}
