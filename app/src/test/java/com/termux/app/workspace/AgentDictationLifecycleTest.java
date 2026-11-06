package com.termux.app.workspace;

import android.app.Application;
import android.app.Activity;
import android.content.*;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalSession;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentDictationLifecycleTest {
 public static class SpeechActivity extends Activity {
  int launches;Intent intent;boolean missing,denied;
  @Override public void startActivityForResult(Intent i,int request){if(missing)throw new ActivityNotFoundException();if(denied)throw new SecurityException();assertEquals(AgentDictation.REQUEST,request);launches++;intent=i;}
 }
 static void set(Object o,Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void deliver(TermuxActivity a,Intent data)throws Exception{Method m=TermuxActivity.class.getDeclaredMethod("onActivityResult",int.class,int.class,Intent.class);m.setAccessible(true);m.invoke(a,AgentDictation.REQUEST,Activity.RESULT_OK,data);}
 static Intent words(String text){return new Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,new ArrayList<>(Arrays.asList(text)));}
 @Test public void resultBeforeResumeAppendsForAllAgentsWithoutReplacingSelectionOrSending()throws Exception{
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);set(a,TermuxActivity.class,"mTerminalView",a.findViewById(R.id.terminal_view));
  WorkspaceController c=new WorkspaceController(a);set(a,TermuxActivity.class,"mWorkspace",c);
  try{
   WorkspaceStore store=(WorkspaceStore)get(c,"store");AgentDictation voice=(AgentDictation)get(c,"dictation");EditText input=(EditText)get(c,"terminalInput");
   Field screen=WorkspaceController.class.getDeclaredField("screen");screen.setAccessible(true);screen.set(c,Enum.valueOf((Class)screen.getType(),"TERMINAL"));
   for(String agent:new String[]{"codex","claude","gemini","opencode"}){
    WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=agent.equals("codex")?"codex":"ubuntu";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");p.aiAgent=agent.equals("opencode")?"opencode":"";
    if(agent.equals("codex")){java.io.File marker=CodexChatControl.marker(a.getFilesDir(),p);marker.getParentFile().mkdirs();marker.createNewFile();}
    TerminalSession fake=new TerminalSession("unused","unused",new String[0],new String[0],2000,null);p.handle=fake.mHandle;store.sessions.add(p);a.getTerminalView().mTermSession=fake;
    set(c,WorkspaceController.class,"activityStopped",false);set(a,TermuxActivity.class,"mIsVisible",true);input.setText("existing text");input.setSelection(0,input.length());
    voice.start();voice.start();org.robolectric.shadows.ShadowActivity.IntentForResult request=Shadows.shadowOf(a).getNextStartedActivityForResult();assertNotNull(request);assertEquals(AgentDictation.REQUEST,request.requestCode);assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH,request.intent.getAction());assertNull(Shadows.shadowOf(a).getNextStartedActivityForResult());
    set(c,WorkspaceController.class,"activityStopped",true);set(a,TermuxActivity.class,"mIsVisible",false);deliver(a,words("new speech 😀"));
    assertEquals("existing text new speech 😀",input.getText().toString());assertEquals(input.length(),input.getSelectionStart());assertTrue(store.recentCommands.isEmpty());assertNull(Shadows.shadowOf(a).getNextStartedActivityForResult());
    voice.start();Shadows.shadowOf(a).getNextStartedActivityForResult();TerminalSession other=new TerminalSession("unused","unused",new String[0],new String[0],2000,null);a.getTerminalView().mTermSession=other;input.setText("other draft");deliver(a,words("late"));assertEquals("other draft",input.getText().toString());a.getTerminalView().mTermSession=null;
   }
  }finally{c.close();a.finish();}
 }
 @Test public void unavailableDeniedCancelledEmptyAndChangedDraftNeverEraseText(){
  SpeechActivity a=Robolectric.buildActivity(SpeechActivity.class).get();EditText input=new EditText(a);input.setText("keep");String[] owner={"chat"};List<String> messages=new ArrayList<>();AgentDictation voice=new AgentDictation(a,input,()->owner[0],messages::add);
  a.missing=true;voice.start();assertEquals(1,messages.size());assertEquals(0,a.launches);a.missing=false;a.denied=true;voice.start();assertEquals(2,messages.size());a.denied=false;
  voice.start();voice.result(AgentDictation.REQUEST,Activity.RESULT_CANCELED,null);assertEquals("keep",input.getText().toString());
  voice.start();voice.result(AgentDictation.REQUEST,Activity.RESULT_OK,words("  "));assertEquals(3,messages.size());assertEquals("keep",input.getText().toString());
  voice.start();input.setText("edited");voice.result(AgentDictation.REQUEST,Activity.RESULT_OK,words("lost"));assertEquals("edited",input.getText().toString());
  owner[0]=null;voice.start();assertEquals(5,messages.size());assertEquals("edited",input.getText().toString());a.finish();
 }
}
