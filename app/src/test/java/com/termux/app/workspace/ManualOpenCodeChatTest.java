package com.termux.app.workspace;

import android.app.AlertDialog;
import android.app.Application;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

/** No service, JNI, native processes or user data. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ManualOpenCodeChatTest {
    private WorkspaceController controller() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));return new WorkspaceController(a);
    }
    private WorkspaceStore store(WorkspaceController c) throws Exception {
        Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);return (WorkspaceStore)f.get(c);
    }
    @Test public void savingLaunchPresetRetainsTheStableHistoryAlias() throws Exception {
        WorkspaceController c=controller();WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.aiAgent="opencode";store(c).sessions.add(s);
        Method save=WorkspaceController.class.getDeclaredMethod("saveSessionProfile",WorkspaceStore.Session.class);save.setAccessible(true);save.invoke(c,s);
        String thread=s.aiThread;AiAgents.prepareLaunch(s);assertEquals(thread,s.aiThread);assertTrue(s.saved);assertEquals("",s.aiSavedThread);assertEquals("new",s.aiChat);
        assertEquals("",store(c).encode().getJSONArray("sessions").getJSONObject(store(c).sessions.indexOf(s)).getString("aiSavedThread"));c.close();
    }
    @Test public void manualSaveChatActionIsAbsentForAllAgents() throws Exception {
        WorkspaceController c=controller();
        try {
            WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";store(c).sessions.add(s);
            Method menu=WorkspaceController.class.getDeclaredMethod("sessionMenu",WorkspaceStore.Session.class,boolean.class);menu.setAccessible(true);
            for(String agent:new String[]{"opencode","codex","claude","gemini"}) {
                s.environment=agent.equals("codex")?"codex":"ubuntu";s.aiAgent=agent.equals("opencode")?agent:"";s.claudeAgent=agent.equals("claude");s.geminiAgent=agent.equals("gemini");menu.invoke(c,s,true);
                AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
                for(int i=0;i<dialog.getListView().getCount();i++)assertNotEquals(WorkspaceText.tr("Сохранить чат сессии"),dialog.getListView().getItemAtPosition(i));
                boolean packages=false;
                for(int i=0;i<dialog.getListView().getCount();i++){String label=dialog.getListView().getItemAtPosition(i).toString();packages|=label.equals("Установка пакетов")||label.equals("Install packages");assertFalse(label.startsWith("Терминал ")||label.startsWith("Terminal "));}
                assertTrue(packages);
                dialog.dismiss();
            }
        } finally {c.close();}
    }
}
