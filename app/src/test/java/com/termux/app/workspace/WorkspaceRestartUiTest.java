package com.termux.app.workspace;
import android.app.*;import com.termux.R;import com.termux.app.*;import com.termux.terminal.TerminalSession;import com.termux.shared.shell.TermuxSession;import com.termux.shared.models.ExecutionCommand;import java.lang.reflect.*;import java.util.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.ShadowAlertDialog;import static org.junit.Assert.*;
/** Native finish is intercepted: this fixture cannot signal a process or create a PTY. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=WorkspaceRestartUiTest.FakeTerminal.class)
public class WorkspaceRestartUiTest {
 @Implements(value=TerminalSession.class,isInAndroidSdk=false) public static class FakeTerminal {
  @org.robolectric.annotation.RealObject TerminalSession real;
  @Implementation protected int getPid(){return 12345;}
  @Implementation protected boolean isRunning(){return true;}
  @Implementation protected void finishIfRunning(){finished.add(real.mHandle);}
 }
 static List<String> finished=new ArrayList<>();TermuxActivity a;WorkspaceController ui;WorkspaceStore store;WorkspaceStore.Session p,peer;TerminalSession terminal,other;
 static void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 @Before public void setup()throws Exception{
  finished.clear();a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);set(a,"mTerminalView",a.findViewById(R.id.terminal_view));ui=new WorkspaceController(a);store=(WorkspaceStore)get(ui,"store");store.sessions.clear();TermuxService service=new TermuxService();set(a,"mTermuxService",service);
  Constructor<TermuxSession> c=TermuxSession.class.getDeclaredConstructor(TerminalSession.class,ExecutionCommand.class,TermuxSession.TermuxSessionClient.class,boolean.class);c.setAccessible(true);
  terminal=new TerminalSession("fixture","fixture",new String[0],new String[0],null,null);other=new TerminalSession("fixture","fixture",new String[0],new String[0],null,null);
  for(TerminalSession t:new TerminalSession[]{terminal,other}){service.getTermuxSessions().add(c.newInstance(t,new ExecutionCommand(),null,false));WorkspaceStore.Session s=new WorkspaceStore.Session();s.saved=true;s.handle=t.mHandle;store.sessions.add(s);}p=store.sessions.get(0);peer=store.sessions.get(1);a.getTerminalView().mTermSession=other;
 }
 @After public void close(){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();if(d!=null)d.dismiss();ui.close();}
 AlertDialog ask()throws Exception{Method m=WorkspaceController.class.getDeclaredMethod("restartSession",WorkspaceStore.Session.class);m.setAccessible(true);m.invoke(ui,p);return ShadowAlertDialog.getLatestAlertDialog();}
 @Test public void sidebarDisclosureTracksSelectedTerminalForAllAgentsWithoutActions()throws Exception{
  java.lang.reflect.Method menu=WorkspaceController.class.getDeclaredMethod("createSidebar");menu.setAccessible(true);
  for(String agent:new String[]{"terminal","codex","claude","gemini","opencode"}){
   p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";a.getTerminalView().mTermSession=terminal;
   WorkspaceSidebar sidebar=(WorkspaceSidebar)menu.invoke(ui);android.widget.LinearLayout frame=sidebar.body.findViewWithTag("sidebar-session-"+p.id),otherFrame=sidebar.body.findViewWithTag("sidebar-session-"+peer.id);
   assertEquals(android.view.View.VISIBLE,frame.getChildAt(1).getVisibility());assertEquals(android.view.View.GONE,otherFrame.getChildAt(1).getVisibility());assertSame(terminal,a.getCurrentSession());assertEquals(2,store.sessions.size());assertTrue(finished.isEmpty());
   a.getTerminalView().mTermSession=other;WorkspaceSidebar next=(WorkspaceSidebar)menu.invoke(ui);frame=next.body.findViewWithTag("sidebar-session-"+p.id);otherFrame=next.body.findViewWithTag("sidebar-session-"+peer.id);assertEquals(android.view.View.GONE,frame.getChildAt(1).getVisibility());assertEquals(android.view.View.VISIBLE,otherFrame.getChildAt(1).getVisibility());assertSame(other,a.getCurrentSession());assertTrue(finished.isEmpty());
  }
 }
 @Test public void cancelAndStaleConfirmationDoNotStopAnyRun()throws Exception{AlertDialog d=ask();d.cancel();assertTrue(finished.isEmpty());d=ask();p.handle="changed";d.getButton(-1).performClick();assertTrue(finished.isEmpty());assertTrue(((Map<?,?>)get(ui,"pendingRestarts")).isEmpty());assertSame(other,a.getCurrentSession());}
 @Test public void confirmationStopsOnlyCapturedTerminalAndRetainsBothRecords()throws Exception{ask().getButton(-1).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(Collections.singletonList(terminal.mHandle),finished);assertSame(p,((Map<?,?>)get(ui,"pendingRestarts")).get(terminal.mHandle));assertEquals(2,store.sessions.size());assertSame(other,a.getCurrentSession());assertSame(peer,store.sessions.get(1));ask().getButton(-1).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,finished.size());}
 @Test public void baseRestartKeepsTheSameProtectedRecord()throws Exception{p.id=WorkspaceStore.BASE_TERMINAL_ID;ask().getButton(-1).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertSame(p,((Map<?,?>)get(ui,"pendingRestarts")).get(terminal.mHandle));assertEquals(2,store.sessions.size());assertSame(p,store.sessions.get(0));assertEquals(Collections.singletonList(terminal.mHandle),finished);}
}
