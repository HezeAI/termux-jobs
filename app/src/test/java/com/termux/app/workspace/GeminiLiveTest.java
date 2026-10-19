package com.termux.app.workspace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class GeminiLiveTest {
    private JSONObject data() throws Exception{return new JSONObject().put("schema",1).put("run","run").put("updated",10000).put("startedAt",9000).put("available",true).put("state","idle").put("model","gemini-test").put("selectedModel","flash").put("requests",3).put("input",9);}
    @Test public void statusIsRunScopedFreshAndSanitized() throws Exception {
        GeminiLiveStatus s=GeminiLiveStatus.parse(data().put("output",-4).put("mode","default"),"run",11000);assertTrue(s.available);assertEquals("gemini-test",s.model);assertEquals(9,s.input);assertEquals(0,s.output);
        for(JSONObject bad:new JSONObject[]{data().put("run","another"),data().put("updated",1),data().put("updated",30000),data().put("schema",2)})try{GeminiLiveStatus.parse(bad,"run",20000);fail();}catch(Exception expected){}
        s=GeminiLiveStatus.parse(data().put("model","x;touch /bad").put("state","ended").put("mode","unknown"),"run",11000);assertFalse(s.available);assertEquals("",s.model);assertEquals("",s.mode);
    }
    @Test public void catalogRejectsExecutableNamesAndDeduplicates() throws Exception {
        JSONArray a=new JSONArray().put(new JSONObject().put("id","gemini-good")).put(new JSONObject().put("id","gemini-good")).put(new JSONObject().put("id","$(bad)"));
        assertEquals(Arrays.asList("gemini-good"),GeminiLiveControl.models(new JSONObject().put("models",a)));
        assertFalse(GeminiLiveControl.validModel("gemini-a\n/bad"));assertFalse(GeminiLiveControl.validModel(null));
    }
    static class Target implements GeminiInputDispatch.Target {
        boolean current=true;int pasted,entered,cancelled;String text;
        public boolean current(){return current;}public void paste(String t){text=t;pasted++;}public void enter(){entered++;}public void cancelled(String t){cancelled++;}
    }
    @Test public void geminiPasteHasOneDelayedReturnAndRejectsDoubleSend(){
        GeminiInputDispatch dispatch=new GeminiInputDispatch();Target t=new Target();List<Runnable> tasks=new ArrayList<>();
        assertTrue(dispatch.send("first\nsecond",t,(r,delay)->{assertEquals(160,delay);tasks.add(r);}));assertEquals(1,t.pasted);assertEquals(0,t.entered);
        assertFalse(dispatch.send("duplicate",t,(r,d)->fail()));tasks.get(0).run();assertEquals(1,t.entered);assertFalse(dispatch.pending());assertEquals("first\nsecond",t.text);
    }
    @Test public void noReturnAfterSwitchClosedRunOrCancellation(){
        GeminiInputDispatch dispatch=new GeminiInputDispatch();Target t=new Target();List<Runnable> tasks=new ArrayList<>();
        dispatch.send("draft",t,(r,d)->tasks.add(r));t.current=false;tasks.remove(0).run();assertEquals(0,t.entered);assertEquals(1,t.cancelled);
        t.current=true;dispatch.send("draft",t,(r,d)->tasks.add(r));dispatch.cancel();tasks.remove(0).run();assertEquals(0,t.entered);
        assertTrue(dispatch.send("new",t,(r,d)->tasks.add(r)));tasks.remove(0).run();assertEquals(1,t.entered);
    }
    @Test public void androidSocketDescriptorExistsBeforeSettingTimeout() throws Exception {
        java.util.List<String> calls=new ArrayList<>();
        android.net.LocalSocket socket=new android.net.LocalSocket(){
            boolean created;
            @Override public void connect(android.net.LocalSocketAddress address){created=true;calls.add("connect");assertEquals(android.net.LocalSocketAddress.Namespace.FILESYSTEM,address.getNamespace());}
            @Override public void setSoTimeout(int timeout) throws java.io.IOException {if(!created)throw new java.io.IOException("socket not created");assertEquals(15000,timeout);calls.add("timeout");}
        };
        GeminiLiveControl.connect(socket,"/private/run.sock");assertEquals(Arrays.asList("connect","timeout"),calls);socket.close();
    }
    @Test public void totalTokensComeFromApiAndDoNotAddCachedTokensAgain() throws Exception {
        GeminiLiveStatus value=GeminiLiveStatus.parse(data().put("total",17).put("cache",4).put("thoughts",3),"run",11000);
        assertEquals(17,value.total);assertEquals(9,value.input);assertEquals(4,value.cache);assertEquals(3,value.thoughts);
    }
    @Test public void openDetailsFollowCurrentStatusInsteadOfFreezingOnResponding() throws Exception {
        com.termux.app.TermuxActivity activity=org.robolectric.Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();activity.setTheme(com.termux.R.style.Theme_Termux_Black);
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.geminiAgent=true;
        GeminiLiveUi ui=new GeminiLiveUi(activity,()->profile,()->false);
        java.lang.reflect.Field chip=GeminiLiveUi.class.getDeclaredField("chip");chip.setAccessible(true);chip.set(ui,new android.widget.TextView(activity));
        java.lang.reflect.Field status=GeminiLiveUi.class.getDeclaredField("status");status.setAccessible(true);status.set(ui,GeminiLiveStatus.parse(data().put("state","responding"),"run",11000));
        java.lang.reflect.Method details=GeminiLiveUi.class.getDeclaredMethod("details",WorkspaceStore.Session.class);details.setAccessible(true);details.invoke(ui,profile);
        android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();String initial=((android.widget.TextView)dialog.getWindow().getDecorView().findViewWithTag("agent-status-info")).getText().toString();assertTrue(initial.contains("отвечает")||initial.contains("responding"));
        status.set(ui,GeminiLiveStatus.parse(data().put("state","idle").put("total",42),"run",11000));
        java.lang.reflect.Method update=GeminiLiveUi.class.getDeclaredMethod("update");update.setAccessible(true);update.invoke(ui);
        String message=((android.widget.TextView)dialog.getWindow().getDecorView().findViewWithTag("agent-status-info")).getText().toString();assertTrue(message.contains("готов")||message.contains("ready"));assertTrue(message.contains("42"));assertTrue(dialog.isShowing());ui.close();
    }
    @Test public void connectionErrorDoesNotClaimTheUserIsLoggedOut() throws Exception {
        com.termux.app.TermuxActivity activity=org.robolectric.Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();activity.setTheme(com.termux.R.style.Theme_Termux_Black);
        GeminiLiveUi ui=new GeminiLiveUi(activity,()->null,()->false);java.lang.reflect.Method error=GeminiLiveUi.class.getDeclaredMethod("controlFailure",String.class);error.setAccessible(true);error.invoke(ui,"CONNECT_FAILED");
        android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();String message=((android.widget.TextView)dialog.findViewById(android.R.id.message)).getText().toString();assertTrue(message.contains("CONNECT_FAILED"));assertFalse(message.contains("Проверьте вход"));assertFalse(message.contains("Check Gemini authentication"));dialog.dismiss();ui.close();
    }

}
