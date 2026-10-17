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
}
