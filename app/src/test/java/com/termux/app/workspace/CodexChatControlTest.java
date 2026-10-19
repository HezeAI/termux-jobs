package com.termux.app.workspace;

import android.app.Application;
import android.os.Looper;
import android.widget.FrameLayout;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class CodexChatControlTest {
    private WorkspaceStore.Session profile(){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexSocketId="fixture-run";p.codexEnvironment="ubuntu";return p;}
    @Test public void newRunMarkerDoesNotAdoptOldOrOtherSessions()throws Exception{
        android.content.Context c=RuntimeEnvironment.application;WorkspaceStore.Session p=profile();CodexChatControl.marker(c.getFilesDir(),p).delete();assertFalse(CodexChatControl.enabled(c.getFilesDir(),p));CodexChatControl.prepare(c,p);assertTrue(CodexChatControl.enabled(c.getFilesDir(),p));
        WorkspaceStore.Session other=profile();other.codexSocketId="different-run";assertFalse(CodexChatControl.enabled(c.getFilesDir(),other));assertFalse(CodexChatControl.enabled(c.getFilesDir(),p.copy()));
        p.codexEnvironment="termux";assertFalse(CodexChatControl.enabled(c.getFilesDir(),p));p.codexEnvironment="ubuntu";p.environment="ubuntu";assertFalse(CodexChatControl.enabled(c.getFilesDir(),p));
    }
    @Test public void privateNewLauncherUsesExistingServerAndDoesNotStartSecondTui()throws Exception{
        android.content.Context c=RuntimeEnvironment.application;WorkspaceStore.Session p=profile();CodexChatControl.prepare(c,p);String command=CodexChatControl.command(c.getFilesDir(),p,"/tmp/socket");assertTrue(command.contains("codex-chat-v1.cjs"));assertTrue(command.contains("'full' 'new'"));assertTrue(command.contains("'never'"));assertFalse(command.contains("codex --remote"));
        File dir=CodexSessionShell.socketDirectory(c.getFilesDir());assertTrue(new File(dir,"codex-wire-v0.2.22.js").isFile());assertTrue(new String(Files.readAllBytes(new File(dir,"codex-chat-v1.cjs").toPath())).contains("turn/steer"));
        p.codexChat="id";p.codexThread="known-history";p.codexSourceDirectory="/root/original";String resume=CodexChatControl.command(c.getFilesDir(),p,"/tmp/socket");assertTrue(resume.contains("'id' 'known-history'"));assertTrue(resume.contains("'/root/original'"));
    }
    private TermuxActivity activity()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);FrameLayout root=new FrameLayout(a);root.setId(R.id.workspace_terminal_surface);TerminalView tv=new TerminalView(a,null);root.addView(tv);Field field=TermuxActivity.class.getDeclaredField("mTerminalView");field.setAccessible(true);field.set(a,tv);a.setContentView(root);return a;
    }
    @Test public void codexChatUsesNativeThreadAndSendAckWithoutWritingTerminal()throws Exception{
        TermuxActivity a=activity();WorkspaceStore.Session p=profile();CodexChatControl.prepare(a,p);AtomicInteger sent=new AtomicInteger(),accepted=new AtomicInteger();
        AgentChatUi ui=new AgentChatUi(a,()->p,()->true,(captured,q)->{assertEquals(p.codexSocketId,captured.codexSocketId);if(q.optString("op").equals("chat-send")){assertEquals("native-thread",q.optString("session"));assertEquals("message",q.optString("kind"));assertEquals("Hello Codex",q.optString("text"));sent.incrementAndGet();return new JSONObject().put("ok",true).put("accepted",true);}return new JSONObject().put("ok",true).put("nativeChat",true).put("connected",true).put("session","native-thread").put("rows",new JSONArray()).put("knownMessages",new JSONArray());});
        try{for(int i=0;i<50&&!ui.showing();i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(ui.showing());assertEquals("Codex",ui.name());assertTrue(ui.terminalAvailable());ui.send("Hello Codex",accepted::incrementAndGet);for(int i=0;i<50&&accepted.get()==0;i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertEquals(1,sent.get());assertEquals(1,accepted.get());}finally{ui.close();}
    }
    private android.widget.TextView text(android.view.View node,String label){
        if(node instanceof android.widget.TextView&&((android.widget.TextView)node).getText().toString().equals(label))return (android.widget.TextView)node;
        if(node instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)node;for(int i=0;i<group.getChildCount();i++){android.widget.TextView found=text(group.getChildAt(i),label);if(found!=null)return found;}}return null;
    }
    @Test public void minimizingDoesNotExitButExplicitReturnDoes()throws Exception{
        TermuxActivity a=activity();WorkspaceStore.Session p=profile();CodexChatControl.prepare(a,p);AtomicInteger hidden=new AtomicInteger(),exited=new AtomicInteger();
        AgentChatUi ui=new AgentChatUi(a,()->p,()->true,(captured,q)->{if(q.optString("op").equals("terminal-close"))hidden.incrementAndGet();if(q.optString("op").equals("terminal-exit"))exited.incrementAndGet();return new JSONObject().put("ok",true).put("nativeChat",true).put("connected",true).put("session","native-thread").put("rows",new JSONArray()).put("knownMessages",new JSONArray());});
        try{for(int i=0;i<40&&!ui.showing();i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}ui.terminal();Shadows.shadowOf(Looper.getMainLooper()).idle();android.app.AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(d.isShowing());d.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick();for(int i=0;i<40&&hidden.get()==0;i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertEquals(1,hidden.get());assertEquals(0,exited.get());
            ui.terminal();Shadows.shadowOf(Looper.getMainLooper()).idle();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.widget.TextView back=text(d.getWindow().getDecorView(),"Вернуться в чат Codex");if(back==null)back=text(d.getWindow().getDecorView(),"Return to Codex chat");assertNotNull(back);back.performClick();for(int i=0;i<40&&d.isShowing();i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertEquals(1,exited.get());assertFalse(d.isShowing());
        }finally{ui.close();}
    }

}
