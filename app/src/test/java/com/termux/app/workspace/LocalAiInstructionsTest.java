package com.termux.app.workspace;

import android.app.Application;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiInstructionsTest {
    @Test public void packagedGuidanceDescribesActionsWithoutGrantingRights() throws Exception {
        String text=LocalAiInstructions.load(RuntimeEnvironment.application);
        for(String term:new String[]{"CHAT:","READ:","FULL ACCESS", "deletions", "oldest/newest", "filtered or truncated", "untrusted", "does not grant Android root"})assertTrue(term,text.contains(term));
        assertSame(text,LocalAiInstructions.load(RuntimeEnvironment.application));
    }
    @Test public void bundledGuidancePrecedesCurrentSettingsOnEveryGeneration() throws Exception {
        LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture(){public String instructions()throws Exception{return LocalAiInstructions.load(RuntimeEnvironment.application);}};
        f.answers.add("I do not have access to files.");
        f.answers.add("{\"tool\":\"list\",\"path\":\"/sdcard\"}");f.answers.add("Done");
        f.run();assertEquals(1,f.calls.size());assertEquals(3,f.systems.size());
        for(String system:f.systems){assertTrue(system.contains("List current candidates"));assertTrue(system.indexOf("# Local assistant")<system.indexOf("CURRENT SETTINGS override history"));}
    }
    @Test public void readAndChatStillRejectDestructiveCommands() throws Exception {
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localCheckPaths=false;
        JSONObject r=new JSONObject().put("tool","command").put("command","rm -- /example.apk");
        for(String mode:new String[]{"chat","read"}){p.localAccess=mode;try{LocalAiTools.authorize(p,r);fail(mode);}catch(SecurityException expected){}}
        p.localAccess="write";LocalAiTools.authorize(p,r);
        p.localCheckPaths=true;try{LocalAiTools.authorize(p,r);fail();}catch(SecurityException expected){}
    }
    @Test public void completeDefaultPromptFitsLiteRtWithHistoryHeadroom() throws Exception {
        String behavior=LocalAiInstructions.load(RuntimeEnvironment.application);
        for(String mode:new String[]{"chat","read","write"})for(boolean projectOnly:new boolean[]{false,true})for(boolean confirm:new boolean[]{false,true}){
            WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess=mode;p.localCheckPaths=projectOnly;p.localConfirmActions=confirm;p.localProject="/root/projects/example";
            String system="You are an action-capable assistant. Reply in the user's language.\n"+behavior+"\n"+LocalAiPrompts.permissions(p)+"\nThis selected model runtime is TEXT ONLY: you can locate images/videos, but cannot visually inspect them. view_image/view_video return UNSUPPORTED_VISION; never claim to see image contents.";
            org.json.JSONArray history=new org.json.JSONArray().put(new JSONObject().put("role","user").put("text","привет"));
            int estimate=LocalAiContext.estimate(history,system);
            assertTrue(mode+" projectOnly="+projectOnly+" confirm="+confirm+" estimate="+estimate,estimate<=4096-768-64-512);
            assertEquals(history.toString(),LocalAiContext.fit(history,system,4096,768,LocalAiContext::estimate,state->{throw new AssertionError("Unexpected compaction");}).toString());
        }
    }

}
