package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;
import com.termux.R;import com.termux.app.TermuxActivity;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;import org.json.*;import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentConnectionUiTest {
 private TermuxActivity a;private WorkspaceController controller;
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);WorkspaceLanguage.select(a,"en");Field field=TermuxActivity.class.getDeclaredField("mTerminalView");field.setAccessible(true);field.set(a,a.findViewById(R.id.terminal_view));controller=new WorkspaceController(a);}
 @After public void close(){controller.close();}
 private AlertDialog open(WorkspaceStore.Session p,boolean launch)throws Exception{Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(controller,p,launch);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();return ShadowAlertDialog.getLatestAlertDialog();}
 @Test public void fieldIsAboveDebugForEachUbuntuAgentAndHiddenForOrdinaryShell()throws Exception{
  for(String agent:new String[]{"codex","gemini","claude","opencode","shell"}){
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.name=agent;p.environment=agent.equals("codex")?"codex":"ubuntu";p.codexEnvironment="ubuntu-ai";p.geminiAgent=agent.equals("gemini");p.claudeAgent=agent.equals("claude");if(agent.equals("opencode"))p.aiAgent=agent;
   AlertDialog dialog=open(p,false);View row=dialog.getWindow().getDecorView().findViewWithTag("agent-connection-fields");EditText input=dialog.getWindow().getDecorView().findViewWithTag("agent-connection-command");assertNotNull(input);View loginHint=dialog.getWindow().getDecorView().findViewWithTag("codex-login-hint");assertNotNull(loginHint);assertEquals(agent,agent.equals("codex")?View.VISIBLE:View.GONE,loginHint.getVisibility());assertEquals(agent,agent.equals("shell")?View.GONE:View.VISIBLE,row.getVisibility());
   ViewGroup form=(ViewGroup)row.getParent();int debug=form.indexOfChild(dialog.getWindow().getDecorView().findViewWithTag("agent-extended-debug"));
   assertTrue(debug>form.indexOfChild(row));dialog.dismiss();
  }
 }
 @Test public void existingSessionAndPresetSaveConnectionWithoutStartingAProcess()throws Exception{
  for(boolean preset:new boolean[]{false,true}){
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.name="Agent";p.environment="codex";p.codexEnvironment="ubuntu-ai";p.directory="/root";p.saved=preset;
   AlertDialog dialog=open(p,!preset);EditText input=dialog.getWindow().getDecorView().findViewWithTag("agent-connection-command");input.setText("/root/private/launcher");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertEquals("/root/private/launcher",p.agentConnectionCommand);assertNull(a.getCurrentSession());assertFalse(dialog.isShowing());
  }
 }
 @Test public void optionalFieldRoundTripsAndOldProfilesDefaultToNormal()throws Exception{
  WorkspaceStore store=new WorkspaceStore(a,message->{});try{
   store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.name="Codex";p.environment="codex";p.codexEnvironment="ubuntu-ai";p.agentConnectionCommand="/root/launcher";p.codexChat="id";p.codexThread="same-chat";p.directory="/root";store.sessions.add(p);
   JSONObject data=store.encode();store.decode(data.toString());p=store.sessions.get(0);assertEquals("/root/launcher",p.agentConnectionCommand);assertEquals("same-chat",p.codexThread);assertEquals("/root/launcher",p.copy().agentConnectionCommand);
   data.getJSONArray("sessions").getJSONObject(0).remove("agentConnectionCommand");store.decode(data.toString());assertEquals("",store.sessions.get(0).agentConnectionCommand);
   for(Object bad:new Object[]{3,JSONObject.NULL,"relative","/root/a\nother"}){
    data.getJSONArray("sessions").getJSONObject(0).put("agentConnectionCommand",bad);try{store.decode(data.toString());fail(String.valueOf(bad));}catch(JSONException expected){}
   }
  }finally{store.close();}
 }
}
