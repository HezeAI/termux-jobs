package com.termux.app.workspace;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.robolectric.Shadows.shadowOf;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=android.app.Application.class)
public class AgentPreparationTest {
    @Test public void inventoryRequiresCompletionAndSuccessfulExit() {
        assertTrue(AgentPreparation.Plan.parse("READY:node\nCHECK:done\n",0).ready());
        assertFalse(AgentPreparation.Plan.parse("READY:node\n",0).ready());
        assertFalse(AgentPreparation.Plan.parse("CHECK:done\n",1).ready());
        AgentPreparation.Plan missing=AgentPreparation.Plan.parse("READY:node\nMISSING:node\nCHECK:done\n",0);
        assertFalse(missing.ready());assertTrue(missing.missing.contains("node"));
    }
    @Test public void parserDiscardsUnknownOutputAndBoundsInput() {
        AgentPreparation.Plan p=AgentPreparation.Plan.parse("secret fixture\nMISSING:unknown-secret\nAUTH:needed\nREADY:gcloud\r\nCHECK:done\n",0);
        assertTrue(p.ready());assertTrue(p.authNeeded);assertEquals(1,p.ready.size());assertTrue(p.missing.isEmpty());assertEquals("",p.error);
        assertFalse(AgentPreparation.Plan.parse(new String(new char[32769]),0).ready());
    }
    @Test public void consentIsNotInheritedAndOrdinarySessionsStayUnchanged() {
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.agentSetupAllowed=true;
        assertFalse(p.copy().agentSetupAllowed);assertFalse(AgentPreparation.supports(p));
        assertEquals("printf ordinary",AgentPreparation.wrap(p,"printf ordinary"));
        p.environment="codex";p.codexEnvironment="termux";assertFalse(AgentPreparation.supports(p));
        assertEquals("codex",AgentPreparation.wrap(p,"codex"));
        p.codexEnvironment="ubuntu";assertTrue(AgentPreparation.supports(p));
        assertTrue(AgentPreparation.wrap(p,"codex").contains("JOBS_AGENT_SETUP_ALLOWED=1"));
        p.agentSetupAllowed=false;assertTrue(AgentPreparation.wrap(p,"codex").contains("JOBS_AGENT_SETUP_ALLOWED=0"));
    }
    @Test public void vertexMetadataChangesInvalidateInventoryAndAreShellQuoted() {
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.geminiAgent=true;p.geminiAuth="vertex";
        String key=AgentPreparation.key(p);p.geminiCloudProject="cloud-project";assertNotEquals(key,AgentPreparation.key(p));
        String wrapped=AgentPreparation.wrap(p,"run-gemini");assertTrue(wrapped.contains("vertex-bootstrap.sh"));assertTrue(wrapped.contains("JOBS_GEMINI_VERTEX_PROJECT='cloud-project'"));
        p.geminiAuth="api";assertFalse(AgentPreparation.wrap(p,"run-gemini").contains("vertex-bootstrap.sh"));
    }
    @Test public void cancellingInventorySuppressesLateCallback() throws Exception {
        android.app.Activity a=Robolectric.buildActivity(android.app.Activity.class).setup().get();
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="ubuntu";p.geminiAgent=true;
        CountDownLatch release=new CountDownLatch(1),finished=new CountDownLatch(1);boolean[] called={false};
        AgentPreparation.check(a,p,(context,profile)->{release.await(3,TimeUnit.SECONDS);finished.countDown();return AgentPreparation.Plan.parse("CHECK:done\n",0);},result->called[0]=true);
        android.app.AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());dialog.cancel();release.countDown();assertTrue(finished.await(3,TimeUnit.SECONDS));
        // Ensure the background task has posted its result before draining the UI queue.
        for(int i=0;i<20;i++){Thread.sleep(10);shadowOf(android.os.Looper.getMainLooper()).idle();}
        assertFalse(called[0]);assertFalse(dialog.isShowing());a.finish();
    }
}
