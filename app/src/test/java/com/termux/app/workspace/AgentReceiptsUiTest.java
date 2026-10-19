package com.termux.app.workspace;
import android.app.Application;
import android.os.Looper;
import android.widget.FrameLayout;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentReceiptsUiTest {
 private TermuxActivity activity(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);FrameLayout host=new FrameLayout(a);host.setId(R.id.workspace_terminal_surface);a.setContentView(host);return a;}
 private void drain()throws Exception{Thread.sleep(30);Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private JSONObject result()throws Exception{return new JSONObject().put("ok",true).put("receipt",new JSONObject().put("token","abcdef12").put("title","Shell").put("body","synthetic command").put("outcome","proceed_once"));}
 @Test public void tapsRouteOnlyToTheSelectedAgentAndOpenNativeReceiptDetails()throws Exception{
  for(boolean gemini:new boolean[]{true,false}){TermuxActivity a=activity();WorkspaceStore.Session s=new WorkspaceStore.Session();s.geminiAgent=gemini;s.aiAgent=gemini?"":"opencode";AtomicInteger calls=new AtomicInteger();
   AgentReceiptsUi ui=new AgentReceiptsUi(a,()->s,()->true,(p,q)->{if(q.optString("op").equals("permission"))return new JSONObject().put("permission",JSONObject.NULL);calls.incrementAndGet();assertEquals(s.id,p.id);return result();});
   try{android.app.AlertDialog previous=ShadowAlertDialog.getLatestAlertDialog();assertFalse(ui.tap("ordinary terminal output"));assertFalse(ui.tap(gemini?"[O:abcdef12]":"[G:abcdef12]"));assertTrue(ui.tap("ⓘ Allowed once ["+(gemini?"G":"O")+":abcdef12]"));
    for(int i=0;i<40&&(calls.get()==0||ShadowAlertDialog.getLatestAlertDialog()==previous||!ShadowAlertDialog.getLatestAlertDialog().isShowing());i++)drain();assertEquals(1,calls.get());assertNotNull(ShadowAlertDialog.getLatestAlertDialog());assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
   }finally{ui.close();}
  }
 }
 @Test public void lateReceiptCannotOpenAfterLeavingAndReturningToSameChat()throws Exception{
  TermuxActivity a=activity();WorkspaceStore.Session s=new WorkspaceStore.Session();s.geminiAgent=true;AtomicBoolean visible=new AtomicBoolean(true);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  AgentReceiptsUi ui=new AgentReceiptsUi(a,()->s,visible::get,(p,q)->{entered.countDown();release.await(2,TimeUnit.SECONDS);return result();});
  try{assertTrue(ui.tap("[G:abcdef12]"));assertTrue(entered.await(2,TimeUnit.SECONDS));visible.set(false);ui.syncVisibility();visible.set(true);ui.syncVisibility();release.countDown();for(int i=0;i<8;i++)drain();assertNull(ShadowAlertDialog.getLatestAlertDialog());}finally{release.countDown();ui.close();}
 }
}
