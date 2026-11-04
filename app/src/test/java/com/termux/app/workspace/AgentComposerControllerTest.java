package com.termux.app.workspace;
import android.app.Application;import android.os.Looper;import android.view.*;import com.termux.R;import com.termux.app.TermuxActivity;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.lang.reflect.*;import java.io.File;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentComposerControllerTest {
 Object get(Object o,String key)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);return f.get(o);}
 void set(Object o,String key,Object value)throws Exception{Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);f.set(o,value);}
 void call(Object o,String name)throws Exception{Method m=o.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(o);}
 @Test public void chatBlocksOnlyInteractiveHeaderButtonForAllAgentsAndRestoresTerminal()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);set(a,"mTerminalView",a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);((AgentChatUi)get(c,"agentChatUi")).close();
  Object oldScreen=get(c,"screen");@SuppressWarnings({"unchecked","rawtypes"}) Object terminal=Enum.valueOf((Class)oldScreen.getClass(),"TERMINAL");set(c,"screen",terminal);AgentChatUi ui=null;
  try{for(String agent:new String[]{"codex","claude","gemini","opencode"}){
    if(ui!=null)ui.close();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";File marker=null;if(agent.equals("codex")){marker=CodexChatControl.marker(a.getFilesDir(),p);marker.getParentFile().mkdirs();marker.createNewFile();}
    ui=new AgentChatUi(a,()->p,()->true,(s,q)->new JSONObject().put("ok",true).put("nativeChat",true).put("connected",true).put("session",p.id).put("rows",new JSONArray()).put("forms",new JSONArray()));set(c,"agentChatUi",ui);for(int i=0;i<60&&!ui.showing();i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(ui.showing());call(c,"updateAgentComposer");call(c,"renderHeader");assertEquals(R.id.workspace_tab_scroll,((android.widget.RelativeLayout.LayoutParams)a.findViewById(R.id.drawer_layout).getLayoutParams()).getRule(android.widget.RelativeLayout.BELOW));assertNull(a.findViewById(R.id.workspace_terminal_tools).getBackground());View direct=a.findViewById(R.id.workspace_header).findViewWithTag("workspace-interactive-input");assertFalse(direct.isEnabled());assertEquals(.4f,direct.getAlpha(),0);boolean before=(boolean)get(c,"interactiveInput");direct.performClick();assertEquals(before,get(c,"interactiveInput"));AgentComposerControls controls=(AgentComposerControls)get(c,"composerControls");assertEquals(android.widget.LinearLayout.HORIZONTAL,controls.pair.getOrientation());assertSame(controls.star,controls.pair.getChildAt(0));assertSame(controls.attachment,controls.pair.getChildAt(2));assertNull(a.findViewById(R.id.workspace_terminal_surface).findViewWithTag("agent-voice-input"));assertTrue(ui.terminalAvailable());if(marker!=null)marker.delete();
   }
   ui.close();call(c,"updateAgentComposer");call(c,"renderHeader");assertEquals(R.id.workspace_session_activity,((android.widget.RelativeLayout.LayoutParams)a.findViewById(R.id.drawer_layout).getLayoutParams()).getRule(android.widget.RelativeLayout.BELOW));assertTrue(a.findViewById(R.id.workspace_header).findViewWithTag("workspace-interactive-input").isEnabled());
  }finally{c.close();a.finish();}
 }
}
