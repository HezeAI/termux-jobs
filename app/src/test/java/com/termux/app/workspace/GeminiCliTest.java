package com.termux.app.workspace;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.json.*;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class GeminiCliTest {
    private Context context;private WorkspaceStore store;
    @Before public void setup(){context=RuntimeEnvironment.application;new File(context.getFilesDir(),"workspace-v1.json").delete();new File(context.getFilesDir(),"workspace-v1.json.bak").delete();store=new WorkspaceStore(context,e->fail(e));}
    @After public void close(){store.close();}
    private WorkspaceStore.Session profile(){WorkspaceStore.Session s=new WorkspaceStore.Session();s.geminiAgent=true;s.environment="ubuntu";s.directory="/root/project";return s;}
    @Test public void settingsAndSelectedHistorySurviveReloadWhileDuplicatesStartNew() throws Exception {
        WorkspaceStore.Session s=profile();s.geminiAuth="google";s.geminiChat="id";s.geminiModel="flash";s.geminiMode="auto_edit";store.sessions.add(s);
        store.decode(store.encode().toString());WorkspaceStore.Session loaded=store.sessions.get(0);
        assertTrue(loaded.geminiAgent);assertEquals("google",loaded.geminiAuth);assertEquals(s.geminiThread,loaded.geminiThread);assertEquals("id",loaded.geminiChat);assertEquals("auto_edit",loaded.geminiMode);
        WorkspaceStore.Session copy=loaded.copy();assertTrue(copy.geminiAgent);assertEquals("google",copy.geminiAuth);assertEquals("flash",copy.geminiModel);assertEquals("new",copy.geminiChat);assertNotEquals(s.geminiThread,copy.geminiThread);assertNotEquals(s.geminiSocketId,copy.geminiSocketId);
        assertEquals(8,store.encode().getInt("version"));assertEquals("",loaded.aiAgent);
    }
    @Test public void invalidProfileKeepsOriginalLibrary() throws Exception {
        WorkspaceStore.Session s=profile();store.sessions.add(s);JSONObject data=store.encode();JSONObject row=data.getJSONArray("sessions").getJSONObject(0);
        for(String[] bad:new String[][]{{"geminiAuth","unknown"},{"geminiThread","x; touch /bad"},{"geminiMode","bypassPermissions"},{"geminiModel","$(bad)"},{"geminiChat","last"},{"environment","termux"},{"aiAgent","opencode"}}) {
            JSONObject attempt=new JSONObject(data.toString());attempt.getJSONArray("sessions").getJSONObject(0).put(bad[0],bad[1]);
            try{store.decode(attempt.toString());fail("Invalid profile accepted: "+bad[0]);}catch(JSONException expected){}assertSame(s,store.sessions.get(0));
        }
    }
    @Test public void bootstrapAndControlsAreSeparateFromOtherAgents() throws Exception {
        WorkspaceStore.Session s=profile();GeminiCli.prepare(context);File rc=SessionShell.prepare(context.getFilesDir(),s);
        String text=new String(java.nio.file.Files.readAllBytes(rc.toPath()),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("/tmp/termux-jobs-gemini"));assertTrue(text.contains("--work-dir '/root/project'"));assertFalse(text.contains("termux-jobs-claude"));assertFalse(text.contains("termux-jobs-ai"));assertFalse(text.contains("termux-jobs-codex"));
        String command=GeminiCli.command(context.getFilesDir(),s);assertTrue(command.contains(s.geminiSocketId));assertTrue(command.contains(s.geminiThread));assertFalse(command.contains(".codex"));assertFalse(command.contains("CLAUDE"));assertFalse(command.contains("gemini-code"));assertFalse(command.contains("API_KEY"));
        assertTrue(new File(GeminiCli.directory(context.getFilesDir()),"gemini-control-v2.js").isFile());
    }
    @Test public void olderGeminiProfilesKeepTheNativeSignInChoice() throws Exception {
        WorkspaceStore.Session s=profile();store.sessions.add(s);JSONObject data=store.encode();data.getJSONArray("sessions").getJSONObject(0).remove("geminiAuth");store.decode(data.toString());assertEquals("auto",store.sessions.get(0).geminiAuth);
    }
    @Test public void legacyProfilesDoNotAcquireGeminiAndKeepCommands() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.startup="echo unchanged";store.sessions.add(s);
        JSONObject data=store.encode();JSONObject row=data.getJSONArray("sessions").getJSONObject(0);for(String key:new String[]{"geminiAgent","geminiModel","geminiMode","geminiChat","geminiThread","geminiSocketId"})row.remove(key);
        store.decode(data.toString());s=store.sessions.get(0);assertFalse(s.geminiAgent);assertEquals("echo unchanged",s.startup);
        File rc=SessionShell.prepare(context.getFilesDir(),s);assertFalse(new String(java.nio.file.Files.readAllBytes(rc.toPath())).contains("termux-jobs-gemini"));
    }
    @Test public void vertexLaunchIsScopedAndDefaultAvoidsUnavailableGoogle() throws Exception {
        WorkspaceStore.Session s=profile();assertEquals("api",s.geminiAuth);s.geminiAuth="vertex";s.geminiCloudProject="test-cloud-project";s.geminiCloudLocation="global";assertTrue(GeminiCli.valid(s));String command=GeminiCli.command(context.getFilesDir(),s);assertTrue(command.contains("JOBS_GEMINI_VERTEX_PROJECT='test-cloud-project'"));assertTrue(command.contains("JOBS_GEMINI_VERTEX_LOCATION='global'"));assertTrue(command.contains("'vertex'"));assertFalse(command.contains("GOOGLE_API_KEY"));WorkspaceStore.Session copy=s.copy();assertEquals(s.geminiCloudProject,copy.geminiCloudProject);assertEquals(s.geminiCloudLocation,copy.geminiCloudLocation);s.geminiAuth="api";assertFalse(GeminiCli.command(context.getFilesDir(),s).contains("JOBS_GEMINI_VERTEX_PROJECT"));s.geminiCloudProject="x; touch /bad";assertFalse(GeminiCli.valid(s));assertFalse(GeminiCli.validCloudLocation("us-central1\nexport x=1"));assertFalse(GeminiCli.validCloudProject("/root/project"));assertTrue(GeminiCli.validCloudProject(""));assertTrue(GeminiCli.validCloudLocation(""));
    }

}
