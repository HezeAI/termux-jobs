package com.termux.app.workspace;
import android.app.*;import com.termux.R;import com.termux.app.*;import com.termux.terminal.TerminalSession;import com.termux.shared.shell.TermuxSession;import com.termux.shared.models.ExecutionCommand;import java.lang.reflect.*;import java.util.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.ShadowAlertDialog;import static org.junit.Assert.*;
/** Native finish is intercepted: this fixture cannot signal a process or create a PTY. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows={WorkspaceRestartUiTest.FakeTerminal.class,WorkspaceRestartUiTest.NoClaudeStop.class,WorkspaceRestartUiTest.NoGeminiStop.class,WorkspaceRestartUiTest.NoCodexStop.class,WorkspaceRestartUiTest.NoOpenCodeStop.class})
public class WorkspaceRestartUiTest {
 @Implements(value=TerminalSession.class,isInAndroidSdk=false) public static class FakeTerminal {
  @org.robolectric.annotation.RealObject TerminalSession real;
  @Implementation protected int getPid(){return 12345;}
  @Implementation protected boolean isRunning(){return true;}
  @Implementation protected void finishIfRunning(){finished.add(real.mHandle);}
 }
 @Implements(value=ClaudeCode.class,isInAndroidSdk=false) public static class NoClaudeStop {@Implementation protected static void stop(android.content.Context context,String run,String environment){assertTrue(ClaudeCode.validThread(run));}}
 @Implements(value=GeminiCli.class,isInAndroidSdk=false) public static class NoGeminiStop {@Implementation protected static void stop(android.content.Context context,String run,String environment){assertTrue(GeminiCli.validThread(run));}}
 @Implements(value=CodexRpc.class,isInAndroidSdk=false) public static class NoCodexStop {@Implementation protected static Runnable ownedStop(java.io.File files,WorkspaceStore.Session profile){return ()->{};}}
 @Implements(value=AiAgents.class,isInAndroidSdk=false) public static class NoOpenCodeStop {@Implementation protected static org.json.JSONObject call(android.content.Context context,String connection,WorkspaceStore.Session profile,org.json.JSONObject request)throws Exception{assertEquals("stop",request.getString("op"));return new org.json.JSONObject().put("ok",true);}}
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
 @Test public void savedAndRunningStatesAreIndependentForAllAgentsAndBothLanguages()throws Exception{
  Method menu=WorkspaceController.class.getDeclaredMethod("createSidebar");menu.setAccessible(true);
  for(String language:new String[]{"ru","en"})for(String agent:new String[]{"codex","claude","gemini","opencode"}){
   WorkspaceLanguage.select(a,language);p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";p.directory="/fixture/project";p.saved=true;p.handle=terminal.mHandle;
   WorkspaceSidebar sidebar=(WorkspaceSidebar)menu.invoke(ui);android.widget.LinearLayout frame=sidebar.body.findViewWithTag("sidebar-session-"+p.id);android.widget.TextView label=(android.widget.TextView)((android.view.ViewGroup)frame.getChildAt(0)).getChildAt(0);
   String[] lines=label.getText().toString().split("\n");String badge=language.equals("ru")?"Сохранён":"Saved";assertTrue(lines[0].contains(badge));assertFalse(lines[1].contains(badge));assertTrue(lines[1].contains(p.directory));assertTrue(lines[1].endsWith(language.equals("ru")?"Запущен":"Running"));
   p.handle="missing";sidebar.refreshSessions();assertTrue(label.getText().toString().endsWith(language.equals("ru")?"Не запущен":"Not running"));assertNotNull(frame.getParent());
   p.handle=terminal.mHandle;p.saved=false;sidebar.refreshSessions();assertFalse(label.getText().toString().contains(badge));assertNotNull(frame.getParent());
   p.handle="missing";sidebar.refreshSessions();assertNull(frame.getParent());assertTrue(finished.isEmpty());
  }
 }
 @Test public void unsaveRemovesStoppedRowAfterPersistenceButRetainsRunningRow()throws Exception{
  Method menu=WorkspaceController.class.getDeclaredMethod("createSidebar");menu.setAccessible(true);Method unsave=WorkspaceController.class.getDeclaredMethod("unsaveSession",WorkspaceStore.Session.class);unsave.setAccessible(true);
  for(boolean running:new boolean[]{false,true}){
   p.handle=running?terminal.mHandle:"missing";p.saved=true;p.claudeAgent=true;p.environment="ubuntu";WorkspaceSidebar sidebar=(WorkspaceSidebar)menu.invoke(ui);android.view.View frame=sidebar.body.findViewWithTag("sidebar-session-"+p.id);unsave.invoke(ui,p);
   for(int n=0;n<200;n++){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();if(!running&&frame.getParent()==null)break;Thread.sleep(5);}
   assertFalse(p.saved);assertEquals(running,frame.getParent()!=null);assertTrue(store.sessions.contains(p));assertTrue(finished.isEmpty());
  }
 }
 @Test public void failedSaveBeforeAgentCloseNeverSignalsAndRestoresSavedFlag()throws Exception{
  p.environment="ubuntu";p.claudeAgent=true;p.saved=false;
  Method close=WorkspaceController.class.getDeclaredMethod("closeSession",WorkspaceStore.Session.class);close.setAccessible(true);close.invoke(ui,p);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertFalse(dialog.getButton(-2).getText().toString().contains("preset"));set(store,"writable",false);Method finish=WorkspaceController.class.getDeclaredMethod("finishClose",WorkspaceStore.Session.class,TerminalSession.class,boolean.class);finish.setAccessible(true);finish.invoke(ui,p,terminal,true);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(finished.isEmpty());assertFalse(p.saved);assertSame(p,store.sessions.get(0));assertSame(peer,store.sessions.get(1));
 }
 @Test public void saveAndCloseBindsNativeConversationBeforePersistingAndStopsOnlyOwner()throws Exception{
  for(String agent:new String[]{"codex","claude","gemini","opencode"}){
   finished.clear();((Set<?>)get(ui,"pendingCloses")).clear();p.saved=false;p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";String id=agent.equals("opencode")?"sesFixture123":"12345678-1234-1234-1234-123456789012";
   ui.chatSnapshot=captured->{assertEquals(p.id,captured.id);return new org.json.JSONObject().put("nativeChat",true).put("session",id);};
   Method close=WorkspaceController.class.getDeclaredMethod("closeSession",WorkspaceStore.Session.class);close.setAccessible(true);close.invoke(ui,p);ShadowAlertDialog.getLatestAlertDialog().getButton(-2).performClick();
   for(int n=0;n<200&&finished.isEmpty();n++){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(5);}assertEquals("agent="+agent+" saved="+p.saved+" writable="+store.isWritable()+" toast="+org.robolectric.shadows.ShadowToast.getTextOfLatestToast()+" binding="+get(ui,"saveBinding")+" saving="+get(ui,"closeSaving"),Collections.singletonList(terminal.mHandle),finished);assertTrue(p.saved);assertEquals(id,p.environment.equals("codex")?p.codexThread:p.claudeAgent?p.claudeThread:p.geminiAgent?p.geminiThread:p.aiSavedThread);assertEquals(2,store.sessions.size());assertSame(other,a.getCurrentSession());
  }
 }
 @Test public void headerTitleDoesNotOpenDrawerAndAgentRestartIsOnlyInHeader()throws Exception{
  p.environment="ubuntu";p.claudeAgent=true;a.getTerminalView().mTermSession=terminal;
  Method header=WorkspaceController.class.getDeclaredMethod("renderHeader");header.setAccessible(true);java.lang.reflect.Field screen=WorkspaceController.class.getDeclaredField("screen");screen.setAccessible(true);Object terminalScreen=java.lang.Enum.valueOf((Class)screen.getType(),"TERMINAL");screen.set(ui,terminalScreen);header.invoke(ui);
  android.view.ViewGroup heading=(android.view.ViewGroup)get(ui,"header");android.view.ViewGroup top=(android.view.ViewGroup)heading.getChildAt(0);assertFalse(top.getChildAt(1).hasOnClickListeners());assertNotNull(heading.findViewWithTag("workspace-header-restart"));assertTrue(finished.isEmpty());
 }
 @Test public void libraryWindowsKeepCurrentSessionAndSeparateTheirLists()throws Exception{
  Method open=WorkspaceController.class.getDeclaredMethod("openLibraryWindow",int.class);open.setAccessible(true);Object screen=get(ui,"screen");
  for(int section:new int[]{-1,0,1,2}){
   open.invoke(ui,section);AlertDialog window=ShadowAlertDialog.getLatestAlertDialog();assertTrue(window.isShowing());assertEquals(android.view.Gravity.BOTTOM,window.getWindow().getAttributes().gravity);assertNotNull(window.findViewById(android.R.id.content).findViewWithTag("library-sheet-content"));assertSame(other,a.getCurrentSession());assertSame(screen,get(ui,"screen"));assertTrue(finished.isEmpty());
   android.view.View content=(android.view.View)get(ui,"libraryContent");assertTrue(content.getLayoutParams().height>0);assertTrue(content.getLayoutParams().height<a.getResources().getDisplayMetrics().heightPixels);
   assertEquals(section,((Integer)get(ui,"libraryWindowSection")).intValue());if(section==0){Method close=WorkspaceController.class.getDeclaredMethod("closeLibraryWindow");close.setAccessible(true);close.invoke(ui);assertNull(get(ui,"libraryWindow"));}window.cancel();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertNull(get(ui,"libraryWindow"));assertSame(other,a.getCurrentSession());
  }
 }
 @Test public void sessionSettingsReturnsToSameSidebarWithoutChangingSelectedTerminal()throws Exception{
  WorkspaceLanguage.select(a,"en");Method menu=WorkspaceController.class.getDeclaredMethod("createSidebar");menu.setAccessible(true);WorkspaceSidebar sidebar=(WorkspaceSidebar)menu.invoke(ui);sidebar.show();
  android.widget.LinearLayout frame=sidebar.body.findViewWithTag("sidebar-session-"+p.id);android.widget.LinearLayout actions=(android.widget.LinearLayout)frame.getChildAt(1);((android.view.ViewGroup)frame.getChildAt(0)).getChildAt(0).performClick();
  for(int i=0;i<actions.getChildCount();i++){android.view.View item=actions.getChildAt(i);if(item instanceof android.widget.TextView&&((android.widget.TextView)item).getText().toString().equals("Session settings")){item.performClick();break;}}
  AlertDialog settings=ShadowAlertDialog.getLatestAlertDialog();assertTrue(settings.isShowing());assertTrue(sidebar.dialog.isShowing());assertSame(other,a.getCurrentSession());settings.cancel();assertTrue(sidebar.dialog.isShowing());assertSame(frame,sidebar.body.findViewWithTag("sidebar-session-"+p.id));assertEquals(android.view.View.VISIBLE,actions.getVisibility());assertSame(other,a.getCurrentSession());assertTrue(finished.isEmpty());
 }
 @Test public void cancelAndStaleConfirmationDoNotStopAnyRun()throws Exception{AlertDialog d=ask();d.cancel();assertTrue(finished.isEmpty());d=ask();p.handle="changed";d.getButton(-1).performClick();assertTrue(finished.isEmpty());assertTrue(((Map<?,?>)get(ui,"pendingRestarts")).isEmpty());assertSame(other,a.getCurrentSession());}
 @Test public void confirmationStopsOnlyCapturedTerminalAndRetainsBothRecords()throws Exception{ask().getButton(-1).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(Collections.singletonList(terminal.mHandle),finished);assertSame(p,((Map<?,?>)get(ui,"pendingRestarts")).get(terminal.mHandle));assertEquals(2,store.sessions.size());assertSame(other,a.getCurrentSession());assertSame(peer,store.sessions.get(1));ask().getButton(-1).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,finished.size());}
 @Test public void baseRestartKeepsTheSameProtectedRecord()throws Exception{p.id=WorkspaceStore.BASE_TERMINAL_ID;ask().getButton(-1).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertSame(p,((Map<?,?>)get(ui,"pendingRestarts")).get(terminal.mHandle));assertEquals(2,store.sessions.size());assertSame(p,store.sessions.get(0));assertEquals(Collections.singletonList(terminal.mHandle),finished);}
}
