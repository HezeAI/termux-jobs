package com.termux.app.workspace;

import android.app.*;
import android.view.View;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class DevkitTimelineTest {
    JSONObject tool(String id,String type)throws Exception{return new JSONObject().put("key",id).put("kind","tool").put("toolType",type).put("status","completed").put("body","{}").put("message",id+"-message").put("part","event");}
    AgentChatView chat(Activity a,AtomicReference<JSONObject> opened){AgentChatView v=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});v.actionCards(true);a.setContentView(v);return v;}
    @Test public void webQueriesSubagentsAndCommandsKeepChronologyAndOriginalDetails()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView v=chat(a,opened);
        JSONObject command=tool("cmd","commandExecution").put("command","/bin/bash -lc 'pwd'");
        JSONObject web=tool("web","webSearch").put("actionPreview",new JSONObject().put("kind","web").put("operation","search").put("queries",new JSONArray().put("first query").put("second query")));
        JSONObject agent=tool("agent","collabAgentToolCall").put("toolName","spawnAgent").put("body",new JSONObject().put("senderThreadId","parent").put("receiverThreadIds",new JSONArray().put("child")).put("prompt","Review files").toString());
        v.rows(Arrays.asList(command,web,agent),false);
        assertTrue(v.timeline.getChildAt(1) instanceof AgentCompactActionView);assertTrue(v.timeline.getChildAt(2) instanceof AgentWebActionView);assertTrue(v.timeline.getChildAt(3) instanceof AgentWebActionView);assertTrue(v.timeline.getChildAt(4) instanceof AgentStandaloneActionView);
        AgentCompactActionView compact=(AgentCompactActionView)v.timeline.getChildAt(1);assertTrue(compact.item.title.getText().toString().contains("bash -lc"));compact.performClick();assertEquals("/bin/bash -lc 'pwd'",opened.get().getString("command"));
        v.timeline.getChildAt(3).performClick();assertEquals("web",opened.get().getString("key"));assertEquals("web-message",opened.get().getString("message"));assertEquals(web.getString("body"),opened.get().getString("body"));
        AgentStandaloneActionView standalone=(AgentStandaloneActionView)v.timeline.getChildAt(4);assertTrue(standalone.heading.getText().toString().contains("Запуск субагента"));standalone.performClick();assertSame(agent,opened.get());
        v.rows(Arrays.asList(command,web,agent),false);assertSame(compact,v.timeline.getChildAt(1));assertSame(standalone,v.timeline.getChildAt(4));
    }
    @Test public void subagentDetailsUseHumanTextAndSuppressMisleadingMissingPlaceholders()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");AgentChatView v=chat(a,new AtomicReference<>());
        JSONObject history=new JSONObject().put("relationVerified",true).put("complete",true).put("state",new JSONObject().put("type","notLoaded")).put("messages",new JSONArray().put(new JSONObject().put("id","answer").put("role","agentMessage").put("text","Actual child response")));
        JSONObject event=tool("done","subAgentActivity").put("toolName","completed").put("body",new JSONObject().put("agentThreadId","child").put("agentHistory",new JSONObject().put("child",history)).toString());
        AgentActionDetailsView detail=new AgentActionDetailsView(v,event,(j,i)->{},j->{});String text=detail.body.getText().toString();
        assertTrue(text.contains("Actual child response"));assertFalse(text.contains("Текст сообщения недоступен"));assertFalse(text.contains("Состояние при чтении"));assertFalse(text.startsWith("{"));
        detail.nativeText(event.getString("body"),true);
        JSONObject live=new JSONObject(event.getString("body")).put("prompt","New live task");detail.update(new JSONObject(event.toString()).put("body",live.toString()));assertTrue(detail.body.getText().toString().contains("New live task"));assertTrue(detail.body.getText().toString().contains("Actual child response"));
        AgentActionDetailsView failed=new AgentActionDetailsView(v,new JSONObject(event.toString()).put("body","{"),(j,i)->{},j->{});failed.historyFailure();assertTrue(failed.body.getText().toString().contains("Не удалось загрузить историю"));assertFalse(failed.body.getText().toString().contains("Загрузка подробностей"));
        history.remove("messages");history.put("unavailable",true);detail.update(new JSONObject(event.toString()).put("body",new JSONObject().put("agentThreadId","child").put("agentHistory",new JSONObject().put("child",history)).toString()));assertTrue(detail.body.getText().toString().contains("Не удалось загрузить историю субагента"));
    }
    @Test public void capturedSubagentDetailsDistinguishMissingNativeHistoryFromCapturedMessages()throws Exception{
        JSONObject message=new JSONObject().put("id","response").put("role","agentMessage").put("text","Captured response");
        JSONObject history=new JSONObject().put("relationVerified",true).put("complete",false).put("messages",new JSONArray().put(message));
        JSONObject participant=new JSONObject().put("id","repo-reader").put("type","explorer").put("role","review");
        JSONObject event=tool("subagent","collabAgentToolCall").put("toolName","spawnAgent").put("body",
            new JSONObject().put("sourceHistoryAvailable",false)
                .put("agentParticipants",new JSONArray().put(participant))
                .put("agentHistory",new JSONObject().put("repo-reader",history)).toString());
        String english=AgentSubagentPresentation.details(event,false);
        assertTrue(english.contains("Name: repo-reader\nType: explorer\nRole: review"));
        assertTrue(english.contains("Captured response"));
        assertTrue(english.contains("The subagent history is incomplete."));
        assertTrue(english.contains("Full subagent history is unavailable."));
        String russian=AgentSubagentPresentation.details(event,true);
        assertTrue(russian.contains("Имя: repo-reader\nТип: explorer\nРоль: review"));
        assertTrue(russian.contains("Получена не вся история субагента."));
        assertTrue(russian.contains("Полная история субагента недоступна."));
    }
    @Test public void compactCommandsNeverRewriteStoredArguments()throws Exception{
        assertEquals("bash -lc 'echo /bin/bash'",AgentCompactActionView.commandCaption("/bin/bash -lc 'echo /bin/bash'"));
        for(String command:new String[]{"/tmp/tool arg","$SHELL -c pwd","/bin/bash; rm -f x","'/bin/bash'bad -c pwd"})assertEquals(command,AgentCompactActionView.commandCaption(command));
    }
}
