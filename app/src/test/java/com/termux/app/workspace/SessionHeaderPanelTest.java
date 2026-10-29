package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import com.termux.app.TermuxActivity;import com.termux.R;import com.termux.terminal.TerminalSession;import java.lang.reflect.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class SessionHeaderPanelTest {
 private TermuxActivity a;private WorkspaceController controller;
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));controller=new WorkspaceController(a);}
 @After public void close(){controller.close();}
 private Object field(String key)throws Exception{Field f=WorkspaceController.class.getDeclaredField(key);f.setAccessible(true);return f.get(controller);}
 private void render()throws Exception{Method m=WorkspaceController.class.getDeclaredMethod("renderTabs");m.setAccessible(true);m.invoke(controller);}
 private FrameLayout panel(){return a.findViewById(R.id.workspace_tabs).findViewWithTag("session-current-panel");}
 @Test public void selectorAndActionsShareOneFullWidthCardWithoutAnActiveSession()throws Exception{
  render();ViewGroup tabs=a.findViewById(R.id.workspace_tabs);assertEquals(1,tabs.getChildCount());assertSame(panel(),tabs.getChildAt(0));assertEquals(-1,panel().getLayoutParams().width);assertNotNull(panel().findViewWithTag("session-current-selector"));assertNotNull(panel().findViewWithTag("session-current-packages"));assertNotNull(panel().findViewWithTag("session-current-files"));assertNull(panel().findViewWithTag("session-current-settings"));
 }
 @Test public void activeSettingsPackagesFilesAndStartupCancelStayInsideCard()throws Exception{
  TerminalSession session=new TerminalSession("/bin/bash","/tmp",new String[0],new String[0],null,null);session.mSessionName="Ubuntu";a.getTerminalView().mTermSession=session;
  WorkspaceStore store=(WorkspaceStore)field("store");store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.name="Ubuntu";p.handle=session.mHandle;p.environment="ubuntu";p.directory="/tmp";p.saved=true;p.startup="echo ready";store.sessions.add(p);render();
  for(String tag:new String[]{"session-current-settings","session-current-packages","session-current-files","cancel-startup"}){View v=panel().findViewWithTag(tag);assertNotNull(tag,v);assertTrue(tag,v.isClickable());assertNotNull(v.getBackground());assertSame(panel().findViewWithTag("session-current-actions"),v.getParent());}
  assertTrue(panel().findViewWithTag("session-current-settings").performClick());assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());assertSame(session,a.getCurrentSession());
 }
}
