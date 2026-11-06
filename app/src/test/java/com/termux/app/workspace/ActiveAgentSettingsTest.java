package com.termux.app.workspace;

import android.app.AlertDialog;
import android.widget.EditText;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.terminal.TerminalSession;
import com.termux.shared.models.ExecutionCommand;
import com.termux.shared.shell.TermuxSession;
import java.lang.reflect.*;
import java.util.Map;
import org.junit.Test;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

/** Real editor/save/status path, synthetic running terminal with no OS process or signals. */
public class ActiveAgentSettingsTest extends AgentSettingsPresentationTest {
    static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static void set(Object o,String n,Object value)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,value);}
    @Test public void liveCodexFormUsesNextSettingsButStatusMustNotClaimTheyAreActive()throws Exception{
        WorkspaceStore.Session p=profile("codex");p.codexAccess="full";p.codexConfirmActions=false;
        TerminalSession terminal=new TerminalSession("/fixture/no-executable","/fixture",new String[0],new String[0],null,null);
        p.handle=terminal.mHandle;WorkspaceStore store=(WorkspaceStore)get(controller,"store");store.sessions.clear();store.sessions.add(p);
        Constructor<TermuxSession> c=TermuxSession.class.getDeclaredConstructor(TerminalSession.class,ExecutionCommand.class,TermuxSession.TermuxSessionClient.class,boolean.class);c.setAccessible(true);
        TermuxService service=new TermuxService();service.getTermuxSessions().add(c.newInstance(terminal,new ExecutionCommand(),null,false));set(activity,"mTermuxService",service);
        CodexStatus active=new CodexStatus();active.cwd="/root/project";active.thread="fixture-thread";active.model="fixture";((Map<String,CodexStatus>)get(controller,"codexStatuses")).put(p.id,active);
        open(p,false);((EditText)tag("session-working-directory")).setText("/root/next");assertNull(findCheck(editor.getWindow().getDecorView(),"Codex project write scope"));p.codexAccess="project";
        editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();store.awaitWrites();idle();
        assertEquals("/root/next",p.directory);assertEquals("project",p.codexAccess);assertEquals("/root/project",active.cwd);assertTrue(terminal.isRunning());
        Method show=WorkspaceController.class.getDeclaredMethod("showCodexStatus",WorkspaceStore.Session.class);show.setAccessible(true);show.invoke(controller,p);idle();editor=ShadowAlertDialog.getLatestAlertDialog();
        String text=visible(editor.getWindow().getDecorView());
        assertFalse(text.contains("Действующий доступ"));assertFalse(text.contains("Следующий запуск"));
        String limitsBefore=active.limits.toString();editor.getWindow().getDecorView().findViewWithTag("subscription-usage-link").performClick();
        android.content.Intent browser=org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity().getParcelableExtra(android.content.Intent.EXTRA_INTENT);
        assertEquals(SubscriptionUsageLink.CODEX,browser.getDataString());assertTrue(editor.isShowing());assertTrue(terminal.isRunning());assertEquals(limitsBefore,active.limits.toString());
        assertTrue(text.contains("Доступность и условия сброса указаны на сайте"));
    }
}
