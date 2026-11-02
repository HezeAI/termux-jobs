package com.termux.app.workspace;

import android.app.*;import android.os.Looper;import android.view.*;import android.widget.*;import com.termux.R;import com.termux.app.TermuxActivity;import com.termux.view.TerminalView;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;

/** Reduced-height layout fixtures model the available IME area, not Android keyboard behavior. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentChatNarrowLayoutTest {
 void layout(View view,int width,int height){for(int i=0;i<3;i++){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);Shadows.shadowOf(Looper.getMainLooper()).idle();}}
 android.graphics.Rect rect(ViewGroup parent,View child){android.graphics.Rect r=new android.graphics.Rect();child.getDrawingRect(r);parent.offsetDescendantRectToMyCoords(child,r);return r;}
 @Test public void questionsReconnectAndStopFitNarrowScaledViewportInBothLanguages()throws Exception{
  for(String language:new String[]{"ru","en"})for(int scale:new int[]{100,150}){
   TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,language);android.content.res.Resources resources=a.getResources();android.content.res.Configuration config=new android.content.res.Configuration(resources.getConfiguration());config.fontScale=1.5f;resources.updateConfiguration(config,resources.getDisplayMetrics());resources.getDisplayMetrics().scaledDensity=resources.getDisplayMetrics().density*1.5f;
   FrameLayout host=new FrameLayout(a);host.setId(R.id.workspace_terminal_surface);TerminalView terminal=new TerminalView(a,null);host.addView(terminal);java.lang.reflect.Field field=TermuxActivity.class.getDeclaredField("mTerminalView");field.setAccessible(true);field.set(a,terminal);a.setContentView(host);
   WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexEnvironment="ubuntu-ai";p.chatScale=scale;java.io.File marker=CodexChatControl.marker(a.getFilesDir(),p);marker.getParentFile().mkdirs();marker.createNewFile();
   String question=language.equals("ru")?"Какой вариант настройки предпочесть для этого проекта?":"Which configuration should be used for this project?";
   JSONObject form=new JSONObject().put("id","narrow").put("nonce","nonce").put("fields",new JSONArray().put(new JSONObject().put("key","choice").put("type","string").put("title",question).put("custom",true).put("options",new JSONArray().put(new JSONObject().put("label",language.equals("ru")?"Первый вариант с подробным пояснением":"First option with a detailed explanation").put("value","first")))));
   AgentChatUi ui=new AgentChatUi(a,()->p,()->true,(profile,q)->new JSONObject().put("ok",true).put("nativeChat",true).put("connected",true).put("busy",true).put("session","narrow-session").put("stopToken","narrow-turn").put("rows",new JSONArray()).put("forms",new JSONArray().put(form)).put("modelConnection",new JSONObject().put("state","retrying").put("attempt",2).put("limit",5).put("turn","narrow-turn")));
   try{
    for(int i=0;i<40&&!ui.showing();i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(ui.showing());AgentChatView chat=(AgentChatView)host.getChildAt(host.getChildCount()-1);int width=chat.dp(280),height=chat.dp(360);layout(chat,width,height);
    View card=chat.findViewWithTag("codex-question-narrow");assertNotNull(card);assertTrue(card.getWidth()<=chat.scroll.getWidth());EditText answer=card.findViewWithTag("question-custom-choice");answer.setText("Custom answer stays");answer.requestFocus();View send=card.findViewWithTag("question-send");assertTrue(send.isEnabled());assertTrue(send.getWidth()<=card.getWidth());
    chat.scroll.scrollTo(0,chat.timeline.getHeight());layout(chat,width,height);android.graphics.Rect button=rect(chat,send);assertTrue("reply can be reached in scroll viewport: "+button,button.bottom<=chat.scroll.getBottom());assertTrue(button.top>=chat.scroll.getTop());assertEquals("Custom answer stays",answer.getText().toString());assertTrue(chat.status.getText().toString().contains("2/5"));assertTrue(chat.status.getLineCount()<=2);
    ui.stopCurrent();AlertDialog stop=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(stop.isShowing());View decor=stop.getWindow().getDecorView();layout(decor,width,height);View yes=stop.getButton(AlertDialog.BUTTON_NEGATIVE),no=stop.getButton(AlertDialog.BUTTON_POSITIVE);android.graphics.Rect y=rect((ViewGroup)decor,yes),n=rect((ViewGroup)decor,no);assertTrue("yes fits "+y,y.top>=0&&y.bottom<=height);assertTrue("no fits "+n,n.top>=0&&n.bottom<=height);assertTrue("Yes left / No right",y.right<=n.left);stop.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertTrue(ui.working());
   }finally{ui.close();marker.delete();a.finish();}
  }
 }
}
