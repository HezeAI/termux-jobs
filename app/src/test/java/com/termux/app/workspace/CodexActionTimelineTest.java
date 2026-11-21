package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.view.MotionEvent;
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
    @Test public void streamingKeepsCardExpansionAndStepIdentityAndOpensOriginalOutput()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=view(a,opened);
        JSONObject one=shell("one","t","pwd","completed"),two=shell("two","t","ls","inProgress");v.rows(Arrays.asList(one,two),false);View first=v.timeline.getChildAt(1);AgentCompactActionView second=(AgentCompactActionView)v.timeline.getChildAt(2);
        two.put("body","ls\nnew native output").put("status","completed").put("exitCode",0).put("durationMs",250);v.rows(Arrays.asList(one,two,shell("three","t","uname -a","inProgress")),false);
        assertSame(first,v.timeline.getChildAt(1));assertSame(second,v.timeline.getChildAt(2));second.item.performClick();assertSame(two,opened.get());assertEquals("two-message",opened.get().getString("message"));assertTrue(opened.get().getString("body").contains("new native output"));assertEquals(View.GONE,second.item.command.getVisibility());assertEquals(View.GONE,second.item.meta.getVisibility());
    }
    @Test public void failuresAndRussianEnglishLabelsUseNativeFacts()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=view(a,new AtomicReference<>());JSONObject failure=shell("one","t","false","completed").put("exitCode",1),denied=shell("two","t","rm file","declined");
        for(String language:new String[]{"ru","en"}){WorkspaceLanguage.select(a,language);v.rows(Arrays.asList(failure,denied),false);AgentCompactActionView first=(AgentCompactActionView)v.timeline.getChildAt(1),second=(AgentCompactActionView)v.timeline.getChildAt(2);assertTrue(first.item.text().toString().contains(language.equals("ru")?"код 1":"exit 1"));assertTrue(second.item.text().toString().contains(language.equals("ru")?"Отклонено":"Declined"));assertEquals(0xffffb08f,first.item.title.getCurrentTextColor());}
    }
    @Test public void eventClockUsesDeviceZoneAndSurvivesRefreshWithoutTimingOtherRows()throws Exception {
        TimeZone previous=TimeZone.getDefault();try{TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"));Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=view(a,opened);JSONObject first=shell("clock","t","pwd","inProgress").put("eventTime",1793611800000L);v.rows(Arrays.asList(first),false);AgentCompactActionView card=(AgentCompactActionView)v.timeline.getChildAt(1);String clock=AgentMessageTime.timelineTime(first.getLong("eventTime"));assertTrue(clock.matches("[0-2][0-9]:[0-5][0-9]"));first.put("status","completed");v.rows(Arrays.asList(first),false);assertSame(card,v.timeline.getChildAt(1));card.performClick();assertEquals(1793611800000L,opened.get().getLong("eventTime"));TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));assertNotEquals(clock,AgentMessageTime.timelineTime(first.getLong("eventTime")));}finally{TimeZone.setDefault(previous);}
    }
    private void layout(AgentChatView view) {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        for (int pass=0;pass<3;pass++) {
            view.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));
            view.layout(0,0,600,700);
        }
        view.scroll.getViewTreeObserver().dispatchOnPreDraw();
    }
    private void readAt(AgentChatView view,int y) {
        long now=android.os.SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,50,100,0);
        MotionEvent cancel=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,50,100,0);
        try {
            view.scroll.dispatchTouchEvent(down);
            view.scroll.scrollTo(0,y);
            view.scroll.dispatchTouchEvent(cancel);
        } finally {
            down.recycle();
            cancel.recycle();
        }
    }
    @Test public void updatesBelowTheViewportDoNotMoveReaderOrReplaceNeighborMessages()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=view(a,new AtomicReference<>());List<JSONObject> rows=new ArrayList<>();for(int i=0;i<25;i++)rows.add(text("message"+i,"text").put("body","message "+i+"\nlonger text\nthird line"));JSONObject command=shell("active","t","pwd","inProgress");rows.add(command);v.rows(rows,false);layout(v);readAt(v,500);int y=v.scroll.getScrollY();assertTrue(y>0);assertFalse(v.position().bottom);View neighbor=v.timeline.getChildAt(3);command.put("body","pwd\nnew output");v.rows(rows,false);layout(v);assertEquals(y,v.scroll.getScrollY());assertSame(neighbor,v.timeline.getChildAt(3));
    }
    @Test public void skillCloudKeepsRealStepsAndStopsAtMessagesOrAnotherSkill()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=view(a,opened);JSONObject read=shell("read","t","cat /root/skills/imagegen/SKILL.md","completed").put("skillContextName","imagegen"),next=shell("next","t","pwd","completed").put("skillContextName","openai-docs");v.rows(Arrays.asList(read,text("answer","text"),next),false);assertTrue(v.timeline.getChildAt(1) instanceof AgentCompactActionView);assertTrue(v.timeline.getChildAt(2) instanceof AgentMarkdownView);assertTrue(v.timeline.getChildAt(3) instanceof AgentCompactActionView);v.timeline.getChildAt(1).performClick();assertSame(read,opened.get());v.timeline.getChildAt(3).performClick();assertSame(next,opened.get());assertEquals("openai-docs",opened.get().getString("skillContextName"));
    }
    @Test public void friendlyNamesDoNotGuessUnknownToolsOrTreatImagesAsSkills()throws Exception{
        JSONObject image=text("image","tool").put("title","image_view");assertEquals("Просмотр изображения",CodexActionLabels.title(image,true));assertEquals("View image",CodexActionLabels.title(image,false));
        JSONObject mcp=text("mcp","tool").put("title","mcpToolCall").put("toolType","mcpToolCall").put("toolName","mcp__image_gen__imagegen");assertEquals("Создание изображения",CodexActionLabels.title(mcp,true));
        JSONObject skill=text("skill","tool").put("title","Skill").put("toolType","skill").put("skillName","my-custom-skill");assertEquals("Подключён навык: my-custom-skill",CodexActionLabels.title(skill,true));assertFalse(CodexActionLabels.title(image,true).contains("навык"));
        JSONObject unknown=text("unknown","tool").put("title","OriginalTool").put("toolType","unknown");assertEquals("OriginalTool",CodexActionLabels.title(unknown,true));
    }
    @Test public void completedCardIsCompactAndUnknownStatusDoesNotFabricateCompletion()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AgentChatView v=view(a,new AtomicReference<>());v.rows(Arrays.asList(shell("one","t","pwd","completed")),false);AgentCompactActionView card=(AgentCompactActionView)v.timeline.getChildAt(1);assertTrue(card.item.text().toString().startsWith("✓"));v.rows(Arrays.asList(shell("one","t","pwd","unknown")),false);assertSame(card,v.timeline.getChildAt(1));assertFalse(card.item.text().toString().startsWith("✓"));assertEquals(View.GONE,card.item.command.getVisibility());
    }
    @Test public void activeTimelineNeverOpensAutomaticallyAndRespectsExplicitChoice()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=view(a,opened);JSONObject running=shell("running","t","build","inProgress").put("outputTail","compiling module");v.rows(Collections.singletonList(running),false);AgentCompactActionView card=(AgentCompactActionView)v.timeline.getChildAt(1);assertNull(opened.get());assertFalse(card.item.text().toString().contains("compiling module"));running.put("body","new output").put("status","completed");v.rows(Collections.singletonList(running),false);assertSame(card,v.timeline.getChildAt(1));assertNull(opened.get());card.item.performClick();assertSame(running,opened.get());
    }
    @Test public void provenUserDeclineUsesNeutralActionPresentationOnly()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();for(String language:new String[]{"ru","en"}){WorkspaceLanguage.select(a,language);AgentChatView v=view(a,new AtomicReference<>());JSONObject denied=shell("denied","turn","synthetic","declined").put("actionOutcome","user_declined");v.rows(Arrays.asList(denied),false);AgentCompactActionView card=(AgentCompactActionView)v.timeline.getChildAt(1);assertEquals(0xfff0f0ee,card.item.title.getCurrentTextColor());assertTrue(card.item.text().toString().contains(language.equals("ru")?"Действие отклонено":"Action declined"));denied.remove("actionOutcome");v.rows(Arrays.asList(denied),false);assertEquals(0xffffb08f,card.item.title.getCurrentTextColor());}
    }

}
