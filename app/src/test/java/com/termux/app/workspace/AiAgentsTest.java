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
public class AiAgentsTest {
    private Context context;private WorkspaceStore store;
    @Before public void setup(){context=RuntimeEnvironment.application;new File(context.getFilesDir(),"workspace-v1.json").delete();new File(context.getFilesDir(),"workspace-v1.json.bak").delete();store=new WorkspaceStore(context,e->fail(e));}
    @After public void close(){store.close();}
    @Test public void legacyCodexRoundTripKeepsAuthenticationAndHistoryPath() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="codex";s.codexChat="id";s.codexThread="thread-123";s.codexEnvironment="ubuntu";s.directory="/root/project";
        String original=CodexSessionShell.invocation(s,"/tmp/control.sock");store.sessions.add(s);store.decode(store.encode().toString());WorkspaceStore.Session next=store.sessions.get(0);
        assertEquals(original,CodexSessionShell.invocation(next,"/tmp/control.sock"));assertEquals("",next.aiAgent);assertEquals(8,store.encode().getInt("version"));
        JSONObject old=store.encode();JSONObject row=old.getJSONArray("sessions").getJSONObject(0);for(String name:new String[]{"aiAgent","aiConnection","aiModel","aiMode","aiChat","aiThread","aiVariant"})row.remove(name);store.decode(old.toString());assertEquals(original,CodexSessionShell.invocation(store.sessions.get(0),"/tmp/control.sock"));
    }
    @Test public void openCodeProfilesPreserveThreadAndSeparateConnections() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.aiAgent="opencode";s.aiModel="google/model-name";s.aiVariant="high";s.aiChat="resume";s.aiThread="sesPreserved";s.saved=true;store.sessions.add(s);store.decode(store.encode().toString());WorkspaceStore.Session next=store.sessions.get(0);
        assertEquals("sesPreserved",next.aiThread);assertEquals("high",next.aiVariant);assertEquals("resume",next.aiChat);assertEquals("ubuntu",next.environment);
        WorkspaceStore.Session duplicate=next.copy();assertEquals(next.aiConnection,duplicate.aiConnection);assertNotEquals(next.aiThread,duplicate.aiThread);assertEquals("new",duplicate.aiChat);
        assertEquals("ubuntu",ProjectFiles.environment(next));assertFalse(store.encode().toString().contains("apiKey"));
    }
    @Test public void invalidAiProfileDoesNotReplaceLibrary() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.aiAgent="opencode";store.sessions.add(s);JSONObject data=store.encode();data.getJSONArray("sessions").getJSONObject(0).put("aiThread","ses; touch /bad");
        try{store.decode(data.toString());fail("Unsafe ID accepted");}catch(JSONException expected){}assertEquals(s,store.sessions.get(0));
        data.getJSONArray("sessions").getJSONObject(0).put("aiThread",s.aiThread).put("aiAgent","unknown");try{store.decode(data.toString());fail("Unknown agent accepted");}catch(JSONException expected){}assertEquals(s,store.sessions.get(0));
    }
    @Test public void generatedShellUsesOwnBridgeAndUbuntuMount() throws Exception {
        AiAgents.prepare(context);WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.aiAgent="opencode";s.aiChat="resume";s.aiThread="sesMine";s.directory="/root/project";
        File rc=SessionShell.prepare(context.getFilesDir(),s);String text=new String(java.nio.file.Files.readAllBytes(rc.toPath()),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("/tmp/termux-jobs-ai"));String command=AiAgents.command(context.getFilesDir(),s);assertTrue(command.contains("sesMine"));assertTrue(command.contains("bootstrap-v1.sh"));assertTrue(new File(AiAgents.directory(context.getFilesDir()),"bootstrap-v1.sh").isFile());assertFalse(command.contains("--auto"));assertFalse(command.contains("API_KEY"));assertFalse(command.contains("codex"));assertTrue(new File(AiAgents.directory(context.getFilesDir()),"bridge-v1.js").isFile());
    }
    @Test public void openCodeModelAcknowledgementUpdatesVisibleLabelAndProfileTogether() throws Exception {
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.aiModel="opencode/ling-3.1-flash-free";android.widget.TextView label=new android.widget.TextView(context);label.setText("old model");int[] saves={0};
        JSONObject actual=new JSONObject().put("providerID","opencode").put("id","exo-free").put("variant","high");
        assertTrue(AiAgentsUi.showConfirmedModel(profile,actual,label,()->saves[0]++));assertEquals("opencode / exo-free · high",label.getText().toString());assertEquals("opencode/exo-free",profile.aiModel);assertEquals("high",profile.aiVariant);assertEquals(1,saves[0]);
        assertFalse(AiAgentsUi.showConfirmedModel(profile,new JSONObject(),label,()->saves[0]++));assertEquals("opencode/exo-free",profile.aiModel);assertEquals("opencode / exo-free · high",label.getText().toString());assertEquals(1,saves[0]);
        actual.put("id","ling-3.1-flash-free").put("variant","");assertTrue(AiAgentsUi.showConfirmedModel(profile,actual,label,()->saves[0]++));assertEquals("opencode / ling-3.1-flash-free",label.getText().toString());assertEquals("",profile.aiVariant);
    }
    @Test public void sessionCardUsesNativeModelOrSelectedPresetInsteadOfConnectionName()throws Exception{
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.aiModel="openrouter/vendor/chosen-model";profile.aiVariant="high";
        assertEquals("vendor/chosen-model · high",AiAgentsUi.sessionModelLabel(profile,null));
        JSONObject actual=new JSONObject().put("providerID","opencode").put("id","actual-model");assertEquals("actual-model",AiAgentsUi.sessionModelLabel(profile,actual));assertEquals("vendor/chosen-model · high",AiAgentsUi.sessionModelLabel(profile,new JSONObject()));assertEquals("openrouter/vendor/chosen-model",profile.aiModel);
    }
    @Test public void accessPersistsAndOldAiProfilesDefaultToConfirmations() throws Exception {
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment="ubuntu";profile.aiAgent="opencode";profile.aiAccess="read";store.sessions.add(profile);store.decode(store.encode().toString());assertEquals("read",store.sessions.get(0).aiAccess);assertEquals("read",store.sessions.get(0).copy().aiAccess);
        JSONObject data=store.encode();data.getJSONArray("sessions").getJSONObject(0).remove("aiAccess");store.decode(data.toString());assertEquals("ask",store.sessions.get(0).aiAccess);
        data.getJSONArray("sessions").getJSONObject(0).put("aiAccess","unknown");try{store.decode(data.toString());fail("Unknown access accepted");}catch(JSONException expected){}assertEquals("ask",store.sessions.get(0).aiAccess);
    }
    @Test public void modelAndThreadInputsRejectShellSyntax() {
        assertTrue(AiAgents.validThread("ses_ec9d35a1dffeNXH7FG1i812tSY"));assertTrue(AiAgents.validThread("sesPreserved"));assertTrue(AiAgents.validModel("google/gemini-model"));assertTrue(AiAgents.validModel("openrouter/vendor/model"));assertTrue(AiAgents.validModel(""));assertFalse(AiAgents.validModel("google/$(touch bad)"));assertFalse(AiAgents.validThread("ses\nother"));
    }
    @Test public void repeatedLaunchesKeepTheSameHistoryAndCopiesStartIndependentChats() {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.aiAgent="opencode";
        String original=s.aiThread;AiAgents.prepareLaunch(s);assertEquals(original,s.aiThread);
        AiAgents.prepareLaunch(s);assertEquals(original,s.aiThread);
        WorkspaceStore.Session copy=s.copy();assertNotEquals(s.aiThread,copy.aiThread);assertEquals("new",copy.aiChat);
        s.aiChat="resume";s.aiThread="sesExplicit";AiAgents.prepareLaunch(s);AiAgents.prepareLaunch(s);
        assertEquals("sesExplicit",s.aiThread);assertEquals("resume",s.aiChat);
        assertTrue(AiAgents.command(context.getFilesDir(),s).contains("sesExplicit"));
    }
    @Test public void manuallySavedChatSurvivesReloadAndLaunchButDuplicatesHaveNoBookmark() throws Exception {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="ubuntu";s.aiAgent="opencode";s.aiSavedThread="sesSaved";s.saved=true;store.sessions.add(s);
        store.decode(store.encode().toString());s=store.sessions.get(0);AiAgents.prepareLaunch(s);
        assertEquals("sesSaved",s.aiThread);assertEquals("resume",s.aiChat);assertTrue(AiAgents.command(context.getFilesDir(),s).contains("sesSaved"));
        assertEquals("",s.copy().aiSavedThread);assertEquals("new",s.copy().aiChat);
        JSONObject data=store.encode();data.getJSONArray("sessions").getJSONObject(0).remove("aiSavedThread");store.decode(data.toString());assertEquals("",store.sessions.get(0).aiSavedThread);assertEquals("resume",store.sessions.get(0).aiChat);assertEquals("sesSaved",store.sessions.get(0).aiThread);
        data=store.encode();data.getJSONArray("sessions").getJSONObject(0).put("aiSavedThread","ses;unsafe");
        try{store.decode(data.toString());fail("Unsafe bookmark accepted");}catch(JSONException expected){}
        assertEquals("",store.sessions.get(0).aiSavedThread);
    }
    @Test public void everyOpenCodeLaunchUsesAnIndependentControlSocketWithoutChangingHistory(){
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.aiAgent="opencode";p.aiSavedThread="sesSaved";String connection=p.aiConnection;
        AiAgents.prepareLaunch(p);File first=AiAgents.socket(context.getFilesDir(),p);AiAgents.prepareLaunch(p);File second=AiAgents.socket(context.getFilesDir(),p);
        assertNotEquals("a delayed control operation must not address a later run",first,second);assertEquals(connection,p.aiConnection);assertEquals("sesSaved",p.aiThread);assertEquals("resume",p.aiChat);
    }

    @Test public void capturedOpenCodeTargetCannotFollowAMutableProfileIntoItsNextRun(){
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.aiAgent="opencode";p.handle="first-terminal";AiAgents.prepareLaunch(p);
        WorkspaceStore.Session target=AiAgents.snapshot(p);File old=AiAgents.socket(context.getFilesDir(),target);String connection=p.aiConnection;
        AiAgents.prepareLaunch(p);p.handle="second-terminal";p.codexEnvironment="ubuntu-ai";
        assertEquals(old,AiAgents.socket(context.getFilesDir(),target));assertNotEquals(old,AiAgents.socket(context.getFilesDir(),p));assertEquals("first-terminal",target.handle);assertEquals("ubuntu",target.codexEnvironment);assertEquals(connection,target.aiConnection);assertEquals("",p.copy().aiSocketId);
    }
    @Test public void openCodeRunIdentityRoundTripsAndLegacyProfilesKeepTheirOldSocketUntilLaunch()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.aiAgent="opencode";File legacy=AiAgents.socket(context.getFilesDir(),p);AiAgents.prepareLaunch(p);String run=p.aiSocketId;store.sessions.add(p);store.decode(store.encode().toString());assertEquals(run,store.sessions.get(0).aiSocketId);assertNotEquals(legacy,AiAgents.socket(context.getFilesDir(),store.sessions.get(0)));
        JSONObject data=store.encode();data.getJSONArray("sessions").getJSONObject(0).remove("aiSocketId");store.decode(data.toString());assertEquals("",store.sessions.get(0).aiSocketId);assertEquals(legacy,AiAgents.socket(context.getFilesDir(),store.sessions.get(0)));
        for(Object bad:new Object[]{"../foreign",123,JSONObject.NULL}){JSONObject invalid=new JSONObject(data.toString());invalid.getJSONArray("sessions").getJSONObject(0).put("aiSocketId",bad);try{store.decode(invalid.toString());fail("Invalid run accepted");}catch(JSONException expected){}assertEquals("",store.sessions.get(0).aiSocketId);}
    }

}
