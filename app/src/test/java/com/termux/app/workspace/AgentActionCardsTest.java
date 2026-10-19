package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.TextView;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentActionCardsTest {
    JSONObject tool(String key,String name,String status)throws Exception{return new JSONObject().put("key",key).put("kind","tool").put("title",name).put("toolName",name).put("status",status).put("body","original detail").put("message","native-message").put("part",key);}
    JSONObject barrier(String kind)throws Exception{return new JSONObject().put("key",kind).put("kind",kind).put("body",kind);}
    @Test public void allAgentsUseStableExpandableCardsWithOriginalDetailKeys()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");
        for(String name:new String[]{"OpenCode","Gemini","Claude"}) {
            AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView view=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});view.actionCards(true);view.assistantName(name);a.setContentView(view);
            JSONObject first=tool("read","read_file","success"),second=tool("search","grep","executing");view.rows(Arrays.asList(first,second),false);
            CodexActionsView card=(CodexActionsView)view.timeline.getChildAt(1);card.heading.performClick();View firstView=card.steps.getChildAt(0);assertEquals(2,card.steps.getChildCount());assertTrue(card.heading.getText().toString().contains("В работе"));
            second.put("status","error");view.rows(Arrays.asList(first,second),false);assertSame(card,view.timeline.getChildAt(1));assertSame(firstView,card.steps.getChildAt(0));assertEquals(View.VISIBLE,card.steps.getVisibility());assertTrue(card.summary.getText().toString().contains("Одно действие"));card.steps.getChildAt(1).performClick();assertSame(second,opened.get());assertEquals("search",opened.get().getString("part"));
            WorkspaceLanguage.select(a,"en");view.rows(Arrays.asList(first,second),false);assertTrue(((TextView)card.steps.getChildAt(0)).getText().toString().contains("Read files"));WorkspaceLanguage.select(a,"ru");
        }
    }
    @Test public void receiptsThoughtsAnswersAndUserMessagesKeepTheirPositions()throws Exception {
        for(String kind:new String[]{"receipt","reasoning","text","user","error"}) {
            JSONObject stop=barrier(kind);List<JSONObject> rows=CodexActionTimeline.group(Arrays.asList(tool("a","Read","completed"),stop,tool("b","Bash","completed")),true);assertEquals(3,rows.size());assertSame(stop,rows.get(1));
        }
        JSONObject unknown=tool("x","my_custom_tool","");assertFalse(CodexActionTimeline.running(unknown));assertFalse(CodexActionTimeline.failed(unknown));assertEquals("my_custom_tool",CodexActionLabels.title(unknown,true));
        assertEquals("completed",CodexActionTimeline.status(tool("a","Read","success")));assertTrue(CodexActionTimeline.failed(tool("b","Read","error")));
    }
    @Test public void successfulExplicitSkillReadGroupsWithoutChangingNativeDetails()throws Exception {
        JSONObject read=tool("read","Read","completed").put("toolPath","/project/skills/custom/SKILL.md"),write=tool("write","Write","completed");
        List<JSONObject> rows=CodexActionTimeline.group(Arrays.asList(read,write,barrier("user"),tool("later","Read","completed")),true);
        assertEquals("custom",rows.get(0).getString("skillName"));assertEquals(2,rows.get(0).getJSONArray("steps").length());assertFalse(read.has("skillName"));assertFalse(write.has("skillContextName"));assertEquals("",rows.get(2).getString("skillName"));assertEquals("native-message",rows.get(0).getJSONArray("steps").getJSONObject(0).getString("message"));
        read.put("status","failed");rows=CodexActionTimeline.group(Arrays.asList(read,write),true);assertEquals("",rows.get(0).getString("skillName"));
        read.put("status","completed").put("toolPath","relative/SKILL.md");assertEquals("",AgentActionMetadata.readSkill(read));read.put("toolPath","/project/a/../custom/SKILL.md");assertEquals("",AgentActionMetadata.readSkill(read));
    }
    @Test public void pendingAndUnknownAreNotDisplayedAsCompleted()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});view.actionCards(true);a.setContentView(view);
        view.rows(Arrays.asList(tool("a","Read","awaiting_approval"),tool("b","Read","")),false);CodexActionsView card=(CodexActionsView)view.timeline.getChildAt(1);card.heading.performClick();assertTrue(((TextView)card.steps.getChildAt(0)).getText().toString().contains("Ожидает разрешения"));assertTrue(((TextView)card.steps.getChildAt(1)).getText().toString().contains("Статус неизвестен"));assertFalse(((TextView)card.steps.getChildAt(1)).getText().toString().contains("✓"));
    }

    @Test public void automaticBottomScrollDoesNotStealComposerFocusSelectionOrComposition()throws Exception {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();
        android.widget.LinearLayout outer=new android.widget.LinearLayout(a);outer.setOrientation(android.widget.LinearLayout.VERTICAL);
        AgentChatView view=new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});view.actionCards(true);
        android.widget.EditText composer=new android.widget.EditText(a);outer.addView(view,new android.widget.LinearLayout.LayoutParams(-1,0,1));outer.addView(composer);a.setContentView(outer);
        JSONObject answer=barrier("text").put("body","streaming answer");view.rows(Arrays.asList(tool("a","Read","running"),answer),false);
        outer.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));outer.layout(0,0,600,800);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        composer.setText("unsent draft");composer.setSelection(3,7);composer.requestFocus();android.view.inputmethod.BaseInputConnection connection=new android.view.inputmethod.BaseInputConnection(composer,true){@Override public android.text.Editable getEditable(){return composer.getText();}};connection.setComposingRegion(0,3);
        for(int i=0;i<3;i++) {
            answer.put("body","streaming answer "+i);view.rows(Arrays.asList(tool("a","Read",i==2?"completed":"running"),answer),false);view.bottomAfterLayout();
            outer.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));outer.layout(0,0,600,800);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue("streaming update must keep composer focus",composer.hasFocus());assertEquals("unsent draft",composer.getText().toString());assertEquals(3,composer.getSelectionStart());assertEquals(7,composer.getSelectionEnd());assertEquals(0,android.view.inputmethod.BaseInputConnection.getComposingSpanStart(composer.getText()));assertEquals(3,android.view.inputmethod.BaseInputConnection.getComposingSpanEnd(composer.getText()));
        }
    }
}
