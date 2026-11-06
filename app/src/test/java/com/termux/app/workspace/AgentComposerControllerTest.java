package com.termux.app.workspace;
import android.app.Application;import android.os.Looper;import android.view.*;import com.termux.R;import com.termux.app.TermuxActivity;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.lang.reflect.*;import java.io.File;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentComposerControllerTest {
 Object get(Object o,String key)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);return f.get(o);}
 void set(Object o,String key,Object value)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);f.set(o,value);}
 void call(Object o,String name)throws Exception{Method m=o.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(o);}
 @Test public void fourAgentComposersKeepVoiceAndAttachmentSeparateFromTerminalModes()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);set(a,"mTerminalView",a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);((AgentChatUi)get(c,"agentChatUi")).close();
  Object oldScreen=get(c,"screen");@SuppressWarnings({"unchecked","rawtypes"}) Object terminal=Enum.valueOf((Class)oldScreen.getClass(),"TERMINAL");set(c,"screen",terminal);AgentChatUi ui=null;
  try{for(String agent:new String[]{"codex","claude","gemini","opencode"}){
    if(ui!=null)ui.close();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";File marker=null;if(agent.equals("codex")){marker=CodexChatControl.marker(a.getFilesDir(),p);marker.getParentFile().mkdirs();marker.createNewFile();}
    ui=new AgentChatUi(a,()->p,()->true,(s,q)->new JSONObject().put("ok",true).put("nativeChat",true).put("connected",true).put("session",p.id).put("rows",new JSONArray()).put("forms",new JSONArray()));set(c,"agentChatUi",ui);for(int i=0;i<60&&!ui.showing();i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(ui.showing());call(c,"updateAgentComposer");call(c,"renderHeader");assertEquals(0,((android.widget.RelativeLayout.LayoutParams)a.findViewById(R.id.drawer_layout).getLayoutParams()).getRule(android.widget.RelativeLayout.BELOW));assertNull(a.findViewById(R.id.workspace_terminal_tools).getBackground());assertNull(a.findViewById(R.id.workspace_header).findViewWithTag("workspace-interactive-input"));AgentComposerControls controls=(AgentComposerControls)get(c,"composerControls");assertEquals(android.widget.LinearLayout.VERTICAL,controls.pair.getOrientation());assertNotNull(controls.attachment.getParent());assertNotNull(controls.row.findViewWithTag("agent-voice-input"));assertNotNull(controls.row.findViewWithTag("agent-stop"));assertTrue(ui.terminalAvailable());WorkspaceStore store=(WorkspaceStore)get(c,"store");com.termux.terminal.TerminalSession fake=new com.termux.terminal.TerminalSession("unused","unused",new String[0],new String[0],2000,null);p.handle=fake.mHandle;store.sessions.add(p);a.getTerminalView().mTermSession=fake;verifyFloatingHeader(a,c,p,fake);set(ui,"codexAuthRequired",true);call(c,"renderHeader");android.widget.TextView notice=a.findViewById(R.id.workspace_header).findViewWithTag("agent-terminal-attention");assertNotNull(notice);assertTrue(notice.getText().toString().contains("authorize")||notice.getText().toString().contains("авторизовать"));set(ui,"codexAuthRequired",false);set(ui,"requiresTerminal",true);call(c,"renderHeader");notice=a.findViewById(R.id.workspace_header).findViewWithTag("agent-terminal-attention");assertNotNull(notice);assertFalse(notice.getText().toString().contains("authorize")||notice.getText().toString().contains("авторизовать"));set(ui,"requiresTerminal",false);call(c,"renderHeader");assertNull(a.findViewById(R.id.workspace_header).findViewWithTag("agent-terminal-attention"));a.getTerminalView().mTermSession=null;
    ui.terminal();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(ui.terminalVisible());a.getTerminalView().setComposerBottomInset(147);Method viewport=WorkspaceController.class.getDeclaredMethod("composerViewport",int.class);viewport.setAccessible(true);viewport.invoke(c,0);assertEquals(147,a.getTerminalView().getComposerBottomInset());ui.terminalBack();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(ui.terminalVisible());if(marker!=null)marker.delete();
   }
   ui.close();call(c,"updateAgentComposer");call(c,"renderHeader");assertEquals(R.id.workspace_session_activity,((android.widget.RelativeLayout.LayoutParams)a.findViewById(R.id.drawer_layout).getLayoutParams()).getRule(android.widget.RelativeLayout.BELOW));AgentComposerControls terminalControls=(AgentComposerControls)get(c,"composerControls");assertNull(terminalControls.attachment.getParent());assertNull(terminalControls.row.findViewWithTag("agent-voice-input"));
  }finally{c.close();a.finish();}
 }
 void verifyFloatingHeader(TermuxActivity a,WorkspaceController c,WorkspaceStore.Session p,com.termux.terminal.TerminalSession fake)throws Exception{
  p.name="Long artificial chat title with Unicode — тест "+p.id;p.directory="/synthetic/long/project/path/with spaces/"+p.id;
  for(String locale:new String[]{"ru","en"})for(float font:new float[]{1f,1.5f}){
   android.content.res.Configuration config=new android.content.res.Configuration(a.getResources().getConfiguration());config.fontScale=font;config.setLocale(new java.util.Locale(locale));a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());WorkspaceLanguage.select(a,locale);
   call(c,"renderHeader");android.widget.LinearLayout header=a.findViewById(R.id.workspace_header);
   android.widget.LinearLayout titles=header.findViewWithTag("agent-header-title");assertNotNull(titles);
   assertEquals(locale.equals("ru")?"Чат":"Chat",((android.widget.TextView)titles.getChildAt(0)).getText().toString());
   assertFalse(header.isClickable());assertFalse(titles.isClickable());
   for(String tag:new String[]{"agent-header-name","agent-header-path"}){android.widget.TextView text=header.findViewWithTag(tag);assertEquals(1,text.getMaxLines());assertNotNull(text.getEllipsize());assertFalse(text.isClickable());}
   for(int width:new int[]{320,412,700}){
    int px=Math.round(width*a.getResources().getDisplayMetrics().density);header.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));header.layout(0,0,px,header.getMeasuredHeight());
    android.widget.LinearLayout row=(android.widget.LinearLayout)header.getChildAt(0);View menu=row.findViewWithTag("workspace-menu"),terminal=row.findViewWithTag("workspace-header-terminal"),restart=row.findViewWithTag("workspace-header-restart");
    assertNotNull(restart);assertTrue(titles.getWidth()>0);assertTrue(menu.getRight()<titles.getLeft());assertTrue(titles.getRight()<terminal.getLeft());assertTrue(terminal.getRight()<restart.getLeft());assertTrue(restart.getRight()<=row.getWidth());
    for(View button:new View[]{menu,terminal,restart}){assertNotNull(button.getBackground());assertTrue(button.isClickable());assertTrue(button.getHeight()>=Math.round(44*a.getResources().getDisplayMetrics().density));}
   }
  }
  java.lang.reflect.Field pid=com.termux.terminal.TerminalSession.class.getDeclaredField("mShellPid");pid.setAccessible(true);pid.setInt(fake,-1);call(c,"renderHeader");assertNull(a.findViewById(R.id.workspace_header).findViewWithTag("workspace-header-restart"));pid.setInt(fake,0);
 }

}
