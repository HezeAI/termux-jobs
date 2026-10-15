package com.termux.app.workspace;

import android.content.Context;
import com.termux.terminal.TerminalSession;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class WorkspaceDiagnosticsTest {
    @Test public void startupCancelOnlyAppearsForSupportedCustomSteps(){
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";assertFalse(SessionShell.hasCancellableStartup(s));s.startup="echo first";assertTrue(SessionShell.hasCancellableStartup(s));s.saved=true;assertTrue(SessionShell.hasCancellableStartup(s));s.geminiAgent=true;assertFalse(SessionShell.hasCancellableStartup(s));s.geminiAgent=false;s.environment="ssh";assertFalse(SessionShell.hasCancellableStartup(s));s.environment="termux";assertTrue(SessionShell.hasCancellableStartup(s));
    }
    @Test public void reportsNeverCopyArbitraryTerminalPayloads(){
        String raw="Authorization: Bearer PRIVATE_TOKEN\nAPI_KEY=AIzaPRIVATE\nhttps://host/path?code=PRIVATE_CODE\n-----BEGIN PRIVATE KEY-----\nsecret\nError HTTP 429 RESOURCE_EXHAUSTED: quota exceeded";
        String result=WorkspaceDiagnostics.classify(raw);
        assertTrue(result.contains("HTTP_429"));assertTrue(result.contains("RESOURCE_EXHAUSTED"));
        assertFalse(result.contains("PRIVATE"));assertFalse(result.contains("AIza"));assertFalse(result.contains("https"));assertFalse(result.contains("secret"));
        assertEquals("",WorkspaceDiagnostics.classify("normal model answer and private user prompt"));
    }
    @Test public void collectionExistsWithoutDialogAndPauseDoesNotStopSession(){
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.extendedDebug=true;
        TerminalSession terminal=new TerminalSession("/bin/bash","/root",new String[0],new String[0],100,null);s.handle=terminal.mHandle;
        WorkspaceDiagnostics.register(s,false);assertTrue(WorkspaceDiagnostics.report(s,terminal).contains("Extended active at launch: false"));WorkspaceDiagnostics.register(s,false);assertTrue(WorkspaceDiagnostics.report(s,terminal).contains("Extended active at launch: false"));
        WorkspaceDiagnostics.register(s,true);WorkspaceDiagnostics.finished(terminal);
        assertTrue(WorkspaceDiagnostics.report(s,terminal).contains("PROCESS_EXIT"));
        WorkspaceDiagnostics.pause(s.id,true);String before=WorkspaceDiagnostics.report(s,terminal);WorkspaceDiagnostics.finished(terminal);
        String after=WorkspaceDiagnostics.report(s,terminal);assertEquals(before.substring(before.indexOf("Session:")),after.substring(after.indexOf("Session:")));
        WorkspaceDiagnostics.pause(s.id,false);WorkspaceDiagnostics.finished(terminal);
        assertTrue(WorkspaceDiagnostics.report(s,terminal).contains("COLLECTION_RESUMED"));
        s.extendedDebug=false;WorkspaceDiagnostics.register(s,false);assertTrue(WorkspaceDiagnostics.report(s,terminal).contains("Extended active at launch: true"));
    }
    @Test public void debugFlagSurvivesPresetReloadCopyAndDefaultsOff() throws Exception {
        Context context=RuntimeEnvironment.application;WorkspaceStore store=new WorkspaceStore(context,e->fail(e));
        try{WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.extendedDebug=true;store.sessions.clear();store.sessions.add(s);
            JSONObject data=store.encode();store.decode(data.toString());assertTrue(store.sessions.get(0).extendedDebug);assertTrue(store.sessions.get(0).copy().extendedDebug);
            data.getJSONArray("sessions").getJSONObject(0).remove("extendedDebug");store.decode(data.toString());assertFalse(store.sessions.get(0).extendedDebug);
            s.geminiAgent=true;s.extendedDebug=true;assertTrue(GeminiCli.command(context.getFilesDir(),s).contains("JOBS_GEMINI_DEBUG=1"));s.extendedDebug=false;assertTrue(GeminiCli.command(context.getFilesDir(),s).contains("JOBS_GEMINI_DEBUG=0"));
        }finally{store.close();}
    }
    @Test public void reportCanBeSavedWithoutAWindowAndReadBack() throws Exception {
        Context c=RuntimeEnvironment.application;WorkspaceStore.Session s=new WorkspaceStore.Session();WorkspaceDiagnostics.register(s,true);
        String report=WorkspaceDiagnostics.report(s,null);CountDownLatch done=new CountDownLatch(1);File[] saved={null};Exception[] errors={null};
        WorkspaceDiagnostics.save(c.getFilesDir(),s.id,report,f->{saved[0]=f;done.countDown();},e->{errors[0]=e;done.countDown();});
        assertTrue(done.await(5,TimeUnit.SECONDS));assertNull(errors[0]);assertNotNull(saved[0]);assertEquals(report,new String(Files.readAllBytes(saved[0].toPath()),java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test public void ringRemovesOldEntriesAtTwoMiB() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();WorkspaceDiagnostics.register(s,true);
        java.lang.reflect.Field map=WorkspaceDiagnostics.class.getDeclaredField("RUNS");map.setAccessible(true);Object run=((java.util.Map<?,?>)map.get(null)).get(s.id);
        java.lang.reflect.Method append=WorkspaceDiagnostics.class.getDeclaredMethod("append",run.getClass(),String.class);append.setAccessible(true);
        String text=new String(new char[50000]).replace('\0','x');for(int i=0;i<100;i++)append.invoke(null,run,"event"+i+text);
        String report=WorkspaceDiagnostics.report(s,null);assertFalse(report.contains("event0x"));assertTrue(report.contains("event99x"));assertTrue(report.length()<WorkspaceDiagnostics.LIMIT+2500);
    }

    @Test public void runtimeDetailSwitchDoesNotChangeLaunchFlagsOrGrantPermissions(){WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.geminiAgent=true;s.extendedDebug=false;WorkspaceDiagnostics.register(s,true);WorkspaceDiagnostics.extended(s.id,true);assertTrue(WorkspaceDiagnostics.extended(s.id));assertFalse(s.extendedDebug);assertTrue(WorkspaceDiagnostics.report(s,null).contains("Extended active at launch: false"));assertTrue(WorkspaceDiagnostics.report(s,null).contains("DETAILS_ENABLED"));WorkspaceDiagnostics.extended(s.id,false);assertFalse(WorkspaceDiagnostics.extended(s.id));}
    @Test public void reportShowsActualAppAndFriendlyEventsRetainUncertainty(){WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="termux";WorkspaceDiagnostics.register(s,true);String report=WorkspaceDiagnostics.report(s,null);assertTrue(report.contains("App: "+com.termux.BuildConfig.VERSION_NAME));assertFalse(report.contains("diagnostics v1.0.18"));String friendly=WorkspaceDiagnostics.friendly("2026-10-16 03:00:00.000 +0300 TERMINAL_SIGNAL HTTP_429 (observed text; not proof of a failed request)",true);assertTrue(friendly.contains("не подтверждение"));assertTrue(friendly.contains("HTTP_429"));assertFalse(WorkspaceDiagnostics.friendly("2026-10-16 03:00:00.000 +0300 START environment=termux",true).contains("вход выполнен"));}
}
