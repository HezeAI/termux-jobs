package com.termux.app.workspace;
import android.app.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentPermissionsUiTest {
    TermuxActivity a;WorkspaceController c;WorkspaceStore store;
    @Before public void setup()throws Exception{a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field f=TermuxActivity.class.getDeclaredField("mTerminalView");f.setAccessible(true);f.set(a,a.findViewById(R.id.terminal_view));c=new WorkspaceController(a);f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);store=(WorkspaceStore)f.get(c);}
    @After public void cleanup(){c.close();}
    static View described(View v,String description){if(v.getVisibility()!=View.VISIBLE)return null;if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=described(((ViewGroup)v).getChildAt(i),description);if(found!=null)return found;}return null;}
    AlertDialog edit(WorkspaceStore.Session p)throws Exception{store.sessions.add(p);Method method=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);method.setAccessible(true);method.invoke(c,p,false);shadowOf(android.os.Looper.getMainLooper()).idle();return ShadowAlertDialog.getLatestAlertDialog();}
    @Test public void openingAndSavingLegacyCodexSettingsDoesNotChangeAccess()throws Exception{
        for(String access:new String[]{"full","project","read"}){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexAccess=access;p.name="Preserved";p.directory="/root/project";String before=CodexSessionShell.invocation(p,"/fixture");AlertDialog dialog=edit(p);View root=dialog.getWindow().getDecorView();CheckBox scope=(CheckBox)described(root,"Codex project write scope"),confirm=(CheckBox)described(root,"Codex action confirmations");assertNotNull(scope);assertNotNull(confirm);assertEquals(access.equals("project"),scope.isChecked());assertEquals(!access.equals("read"),scope.isEnabled());assertEquals(!access.equals("full"),confirm.isChecked());dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(before,CodexSessionShell.invocation(p,"/fixture"));assertEquals(access,p.codexAccess);assertEquals("Preserved",p.name);}
    }
    @Test public void openCodeScopeChangesOnlyAfterExplicitSaveAndPreservesAccess()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.aiAgent="opencode";p.environment="ubuntu";p.directory="/root/project";p.aiAccess="full";AlertDialog dialog=edit(p);CheckBox scope=(CheckBox)described(dialog.getWindow().getDecorView(),"OpenCode project scope");assertNotNull(scope);scope.performClick();assertFalse(p.aiProjectOnly);assertEquals("full",p.aiAccess);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(p.aiProjectOnly);assertEquals("full",p.aiAccess);assertEquals("ubuntu",p.environment);
    }
}
