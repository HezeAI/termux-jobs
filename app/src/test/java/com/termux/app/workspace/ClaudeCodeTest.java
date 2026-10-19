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
public class ClaudeCodeTest {
    private Context context;private WorkspaceStore store;
    @Before public void setup(){context=RuntimeEnvironment.application;new File(context.getFilesDir(),"workspace-v1.json").delete();new File(context.getFilesDir(),"workspace-v1.json.bak").delete();store=new WorkspaceStore(context,e->fail(e));}
    @After public void close(){store.close();}
    private WorkspaceStore.Session profile(){WorkspaceStore.Session s=new WorkspaceStore.Session();s.claudeAgent=true;s.environment="ubuntu";s.directory="/root/project";return s;}
    @Test public void settingsAndSelectedHistorySurviveReloadWhileDuplicatesStartNew() throws Exception {
        WorkspaceStore.Session s=profile();s.claudeChat="id";s.claudeModel="sonnet";s.claudeMode="acceptEdits";s.claudeEffort="high";store.sessions.add(s);
        store.decode(store.encode().toString());WorkspaceStore.Session loaded=store.sessions.get(0);
        assertTrue(loaded.claudeAgent);assertEquals(s.claudeThread,loaded.claudeThread);assertEquals("id",loaded.claudeChat);assertEquals("acceptEdits",loaded.claudeMode);assertEquals("high",loaded.claudeEffort);
        WorkspaceStore.Session copy=loaded.copy();assertTrue(copy.claudeAgent);assertEquals("sonnet",copy.claudeModel);assertEquals("new",copy.claudeChat);assertNotEquals(s.claudeThread,copy.claudeThread);assertNotEquals(s.claudeSocketId,copy.claudeSocketId);
        assertEquals(8,store.encode().getInt("version"));assertEquals("",loaded.aiAgent);
    }
    @Test public void invalidProfileKeepsOriginalLibrary() throws Exception {
        WorkspaceStore.Session s=profile();store.sessions.add(s);JSONObject data=store.encode();JSONObject row=data.getJSONArray("sessions").getJSONObject(0);
        for(String[] bad:new String[][]{{"claudeThread","x; touch /bad"},{"claudeMode","bypassPermissions"},{"claudeModel","$(bad)"},{"claudeChat","last"},{"environment","termux"},{"aiAgent","opencode"}}) {
            JSONObject attempt=new JSONObject(data.toString());attempt.getJSONArray("sessions").getJSONObject(0).put(bad[0],bad[1]);
            try{store.decode(attempt.toString());fail("Invalid profile accepted: "+bad[0]);}catch(JSONException expected){}assertSame(s,store.sessions.get(0));
        }
    }
    @Test public void bootstrapAndControlsAreSeparateFromOtherAgents() throws Exception {
        WorkspaceStore.Session s=profile();ClaudeCode.prepare(context);File rc=SessionShell.prepare(context.getFilesDir(),s);
        String text=new String(java.nio.file.Files.readAllBytes(rc.toPath()),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("/tmp/termux-jobs-claude"));assertTrue(text.contains("--work-dir '/root/project'"));assertFalse(text.contains("termux-jobs-ai"));assertFalse(text.contains("termux-jobs-codex"));
        String command=ClaudeCode.command(context.getFilesDir(),s);assertTrue(command.contains("claude-control-v2.js"));assertTrue(command.contains(s.claudeSocketId));assertTrue(command.contains(s.claudeThread));assertFalse(command.contains(".codex"));assertFalse(command.contains("API_KEY"));
        assertTrue(new File(ClaudeCode.directory(context.getFilesDir()),"claude-control.js").isFile());for(String file:new String[]{"claude-control-v2.js","claude-chat.cjs","claude-native-hook.cjs","agent-receipts.cjs"})assertTrue(file,new File(ClaudeCode.directory(context.getFilesDir()),file).isFile());
    }
    @Test public void legacyProfilesDoNotAcquireClaudeAndKeepCommands() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.startup="echo unchanged";store.sessions.add(s);
        JSONObject data=store.encode();JSONObject row=data.getJSONArray("sessions").getJSONObject(0);for(String key:new String[]{"claudeAgent","claudeModel","claudeMode","claudeChat","claudeThread","claudeSocketId"})row.remove(key);
        store.decode(data.toString());s=store.sessions.get(0);assertFalse(s.claudeAgent);assertEquals("echo unchanged",s.startup);
        File rc=SessionShell.prepare(context.getFilesDir(),s);assertFalse(new String(java.nio.file.Files.readAllBytes(rc.toPath())).contains("termux-jobs-claude"));
    }
}
