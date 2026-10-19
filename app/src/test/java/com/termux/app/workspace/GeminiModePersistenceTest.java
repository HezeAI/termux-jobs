package com.termux.app.workspace;

import android.app.Application;
import android.app.AlertDialog;
import android.widget.TextView;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.json.JSONObject;
import java.lang.reflect.*;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class GeminiModePersistenceTest {
    @Test public void presetAndUnsavedSessionUpdateOnlyForTheirOwnConfirmedRun()throws Exception{
        File f=new File(RuntimeEnvironment.application.getFilesDir(),"workspace-v1.json");f.delete();new File(f+".bak").delete();WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,e->fail(e));
        try{WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=true;p.environment="ubuntu";p.saved=true;store.sessions.add(p);WorkspaceStore.Session other=p.copy();store.sessions.add(other);
            assertFalse(store.applyGeminiMode(p,"wrong-run","default","auto_edit"));assertEquals("default",p.geminiMode);
            assertTrue(store.applyGeminiMode(p,p.geminiSocketId,"default","auto_edit"));assertEquals("auto_edit",p.geminiMode);assertEquals("default",other.geminiMode);assertTrue(p.saved);
            assertFalse(store.applyGeminiMode(p,p.geminiSocketId,"default","plan"));assertFalse(store.applyGeminiMode(p,p.geminiSocketId,"auto_edit","yolo"));
            assertTrue(store.applyGeminiMode(other,other.geminiSocketId,"default","plan"));assertFalse(other.saved);store.awaitWrites();
            WorkspaceStore reloaded=new WorkspaceStore(RuntimeEnvironment.application,e->fail(e));try{assertEquals("auto_edit",reloaded.sessions.get(0).geminiMode);assertEquals("plan",reloaded.sessions.get(1).geminiMode);}finally{reloaded.close();}
        }finally{store.close();}
    }
    @Test public void rightsRowOpensThreeChoicesAndBackReturnsToSameDetails()throws Exception{
        com.termux.app.TermuxActivity a=Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();a.setTheme(com.termux.R.style.Theme_Termux_Black);WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=true;
        GeminiLiveUi ui=new GeminiLiveUi(a,()->p,()->false);try{
            Field chip=GeminiLiveUi.class.getDeclaredField("chip");chip.setAccessible(true);chip.set(ui,new TextView(a));
            JSONObject value=new JSONObject().put("schema",1).put("run",p.geminiSocketId).put("updated",System.currentTimeMillis()).put("startedAt",System.currentTimeMillis()).put("available",true).put("state","idle").put("mode","default").put("canChangeMode",true).put("canPlan",true).put("canEdit",true);
            Field status=GeminiLiveUi.class.getDeclaredField("status");status.setAccessible(true);status.set(ui,GeminiLiveStatus.parse(value,p.geminiSocketId,System.currentTimeMillis()));
            Method details=GeminiLiveUi.class.getDeclaredMethod("details",WorkspaceStore.Session.class);details.setAccessible(true);details.invoke(ui,p);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();AlertDialog original=ShadowAlertDialog.getLatestAlertDialog();TextView rights=original.getWindow().getDecorView().findViewWithTag("agent-status-permissions");assertNotNull(rights);assertTrue(rights.getText().toString().contains(GeminiRightsUi.label("default")));assertTrue(rights.isEnabled());rights.performClick();AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(original,picker);assertEquals(3,picker.getListView().getCount());picker.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(original.isShowing());assertFalse(picker.isShowing());assertEquals("default",p.geminiMode);
        }finally{ui.close();}
    }
    @Test public void onlyMatchingAppliedAcknowledgementPersistsEvenWhenAnotherTabIsSelected()throws Exception{
        com.termux.app.TermuxActivity a=Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();a.setTheme(com.termux.R.style.Theme_Termux_Black);
        WorkspaceStore.Session selected=new WorkspaceStore.Session(),target=new WorkspaceStore.Session();target.geminiAgent=true;selected.geminiAgent=true;int[] writes={0};
        GeminiLiveUi ui=new GeminiLiveUi(a,()->selected,()->false,(p,run,before,mode)->{assertSame(target,p);assertEquals(target.geminiSocketId,run);assertEquals("default",before);p.geminiMode=mode;writes[0]++;return true;});
        try{
            Class<?> type=Class.forName("com.termux.app.workspace.GeminiLiveUi$PendingMode");Constructor<?> ctor=type.getDeclaredConstructor(WorkspaceStore.Session.class,String.class,String.class);ctor.setAccessible(true);
            Field field=GeminiLiveUi.class.getDeclaredField("pendingModes");field.setAccessible(true);java.util.Map map=(java.util.Map)field.get(ui);map.put(target.geminiSocketId,ctor.newInstance(target,"request","auto_edit"));
            Method reconcile=GeminiLiveUi.class.getDeclaredMethod("reconcileMode",String.class,GeminiLiveStatus.class);reconcile.setAccessible(true);
            GeminiLiveStatus status=new GeminiLiveStatus();status.mode="autoEdit";status.state="idle";status.modeChangeId="wrong-request";status.modeChangeMode="auto_edit";status.modeChangeState="applied";
            reconcile.invoke(ui,target.geminiSocketId,status);assertEquals(0,writes[0]);status.modeChangeId="request";status.modeChangeState="queued";reconcile.invoke(ui,target.geminiSocketId,status);assertEquals(0,writes[0]);
            status.modeChangeState="applied";reconcile.invoke(ui,target.geminiSocketId,status);assertEquals(1,writes[0]);assertEquals("auto_edit",target.geminiMode);assertEquals("default",selected.geminiMode);assertTrue(map.isEmpty());
            reconcile.invoke(ui,target.geminiSocketId,status);assertEquals(1,writes[0]);
        }finally{ui.close();}
    }

}
