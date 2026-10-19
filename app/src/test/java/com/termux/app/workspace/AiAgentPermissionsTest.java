package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AiAgentPermissionsTest {
    WorkspaceStore store;
    @Before public void setup(){new File(RuntimeEnvironment.application.getFilesDir(),"workspace-v1.json").delete();new File(RuntimeEnvironment.application.getFilesDir(),"workspace-v1.json.bak").delete();store=new WorkspaceStore(RuntimeEnvironment.application,e->fail(e));}
    @After public void close(){store.close();}
    @Test public void oldProfilesKeepExactAccessAndApprovalPoliciesOnLoad()throws Exception{
        for(String access:new String[]{"full","project","read"}){store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexAccess=access;p.codexChat="id";p.codexThread="history-kept";p.codexSourceDirectory="/old/project";store.sessions.add(p);JSONObject data=store.encode();JSONObject row=data.getJSONArray("sessions").getJSONObject(0);row.remove("codexConfirmActions");row.remove("aiProjectOnly");store.decode(data.toString());p=store.sessions.get(0);assertEquals(access,p.codexAccess);assertEquals(access.equals("full")?"never":"on-request",CodexSessionShell.approval(p));assertNull(p.codexConfirmActions);assertFalse(p.aiProjectOnly);assertEquals("history-kept",p.codexThread);assertEquals("/old/project",p.codexSourceDirectory);}
    }
    @Test public void independentChoicesPersistAndCopyWithoutLosingOtherSettings()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexAccess="project";p.codexConfirmActions=false;p.aiProjectOnly=true;p.name="Preserved";p.directory="/project";p.saved=true;store.sessions.add(p);store.decode(store.encode().toString());p=store.sessions.get(0);assertEquals("project",p.codexAccess);assertEquals("never",CodexSessionShell.approval(p));assertTrue(p.aiProjectOnly);assertEquals("Preserved",p.name);assertEquals("/project",p.directory);assertTrue(p.saved);WorkspaceStore.Session copy=p.copy();assertEquals(Boolean.FALSE,copy.codexConfirmActions);assertTrue(copy.aiProjectOnly);assertEquals("project",copy.codexAccess);
    }
    @Test public void permissionsFieldsRejectStringsRatherThanSilentlyChangingPolicy()throws Exception{
        store.sessions.add(new WorkspaceStore.Session());JSONObject original=store.encode();for(String key:new String[]{"codexConfirmActions","aiProjectOnly"}){JSONObject bad=new JSONObject(original.toString());bad.getJSONArray("sessions").getJSONObject(0).put(key,"false");try{store.decode(bad.toString());fail();}catch(JSONException expected){}}
    }
    @Test public void codexSandboxAndApprovalsAreIndependentForNewAndResumedChats()throws Exception{
        for(String access:new String[]{"full","project","read"})for(boolean confirm:new boolean[]{false,true}){WorkspaceStore.Session p=new WorkspaceStore.Session();p.codexAccess=access;p.codexConfirmActions=confirm;String expected=confirm?"on-request":"never";String command=CodexSessionShell.invocation(p,"/tmp/fixture");assertTrue(command.contains("--sandbox "+CodexSessionShell.sandbox(access)));assertTrue(command.contains("--ask-for-approval "+expected));p.codexChat="id";p.codexThread="thread-fixture";String resume=CodexSessionShell.resumePreflight(p,"/tmp/fixture");assertTrue(resume.contains("'"+access+"' 'id' 'thread-fixture'"));assertTrue(resume.contains("'"+expected+"'"));assertFalse(command.contains("||"));}
    }
    @Test public void openCodeScopeIsExplicitInLaunchAndLegacyDefaultsToUnrestricted(){WorkspaceStore.Session p=new WorkspaceStore.Session();p.aiAgent="opencode";p.environment="ubuntu";String before=AiAgents.command(RuntimeEnvironment.application.getFilesDir(),p);assertTrue(before.contains("'ask' 'unrestricted'"));p.aiProjectOnly=true;assertTrue(AiAgents.command(RuntimeEnvironment.application.getFilesDir(),p).contains("'ask' 'project'"));assertFalse(before.contains("codex"));}
    @Test public void openCodeProjectPolicyDeniesShellExternalPluginsAndDelegation()throws Exception{
        File asset=new File("src/main/assets/ai-agents-control.js").getAbsoluteFile();String script="const assert=require('node:assert/strict');const {accessRules}=require(process.argv[1]);const effect=(r,a)=>r.filter(x=>x.action==='*'||x.action===a).at(-1)?.effect;for(const mode of ['full','ask','read']){const r=accessRules(mode,'project');for(const action of ['shell','execute','subagent','skill','plugin_mcp','unknown','external_directory'])assert.equal(effect(r,action),'deny');for(const action of ['read','glob','grep','question'])assert.equal(effect(r,action),'allow');assert.equal(effect(r,'edit'),mode==='full'?'allow':mode==='ask'?'ask':'deny');}assert.deepEqual(accessRules('full'),[{action:'*',resource:'*',effect:'allow'}]);assert.equal(effect(accessRules('ask'),'shell'),'ask');assert.equal(effect(accessRules('read'),'shell'),'deny');assert.throws(()=>accessRules('full','invalid'));";
        Process p=new ProcessBuilder("node","-e",script,asset.toString()).redirectErrorStream(true).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=p.getInputStream().read(b))!=-1)out.write(b,0,n);assertEquals(out.toString("UTF-8"),0,p.waitFor());
    }
    @Test public void newCodexHelperPreservesPreviouslyRunningHelperFile()throws Exception{
        File directory=CodexSessionShell.socketDirectory(RuntimeEnvironment.application.getFilesDir());directory.mkdirs();File previous=new File(directory,"bridge-v0.2.19.js");java.nio.file.Files.write(previous.toPath(),"previous process helper".getBytes(StandardCharsets.UTF_8));File v20=new File(directory,"bridge-v0.2.20.js");java.nio.file.Files.write(v20.toPath(),"active v20 helper".getBytes(StandardCharsets.UTF_8));CodexSessionShell.prepareBridge(RuntimeEnvironment.application);assertEquals("active v20 helper",new String(java.nio.file.Files.readAllBytes(v20.toPath()),StandardCharsets.UTF_8));assertEquals("previous process helper",new String(java.nio.file.Files.readAllBytes(previous.toPath()),StandardCharsets.UTF_8));File current=new File(directory,"bridge-v0.2.21.js");assertTrue(current.isFile());assertTrue(new String(java.nio.file.Files.readAllBytes(current.toPath()),StandardCharsets.UTF_8).contains("requestedApproval"));
    }

    @Test public void openCodeStartsStructuredChatWithoutInstallingCustomMini(){WorkspaceStore.Session p=new WorkspaceStore.Session();p.aiAgent="opencode";p.environment="ubuntu";String command=AiAgents.command(RuntimeEnvironment.application.getFilesDir(),p);assertTrue(command.contains(" run-chat "));assertFalse(command.contains("opencode-mini"));assertFalse(command.contains("--replay"));}

}
