package com.termux.app.workspace;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class AgentPreparationOperationTest {
    WorkspaceStore.Session profile(String agent){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=agent.equals("codex")?"codex":"ubuntu";p.codexEnvironment="ubuntu-ai";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.equals("gemini");if(agent.equals("opencode"))p.aiAgent="opencode";return p;}
    AgentPreparation.Plan ready(){return AgentPreparation.Plan.parse("READY:ubuntu\nCHECK:done\n",0);}
    void finish(AgentPreparationOperation.Job j)throws Exception {for(int n=0;j.active&&n<100;n++)Thread.sleep(20);assertFalse(j.active);}
    @Test public void sameAgentReusesActiveJobAndOtherAgentIsSeparate()throws Exception{
        AgentPreparationOperation.jobs.clear();AtomicInteger calls=new AtomicInteger();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        AgentPreparationOperation.Worker worker=(c,p,o)->{calls.incrementAndGet();entered.countDown();assertTrue(release.await(3,TimeUnit.SECONDS));return ready();};
        WorkspaceStore.Session p=profile("claude");AgentPreparation.Runner check=(c,s)->ready();
        AgentPreparationOperation.Job first=AgentPreparationOperation.start(RuntimeEnvironment.application,p,check,worker);assertTrue(entered.await(2,TimeUnit.SECONDS));
        assertSame(first,AgentPreparationOperation.start(RuntimeEnvironment.application,p,check,worker));
        AgentPreparationOperation.Job second=AgentPreparationOperation.start(RuntimeEnvironment.application,profile("gemini"),check,(c,s,o)->ready());assertNotSame(first,second);assertEquals(1,calls.get());
        release.countDown();finish(first);finish(second);assertTrue(first.result.ready());
    }
    @Test public void brokenExistingEnvironmentNeverReachesInstallerAndRetryIsExplicit()throws Exception{
        AgentPreparationOperation.jobs.clear();AtomicInteger calls=new AtomicInteger();WorkspaceStore.Session p=profile("codex");
        AgentPreparationOperation.Job failed=AgentPreparationOperation.start(RuntimeEnvironment.application,p,(c,s)->AgentPreparation.Plan.parse("",1),(c,s,o)->{calls.incrementAndGet();return ready();});finish(failed);assertEquals(0,calls.get());assertEquals("failed",failed.stage);
        AgentPreparationOperation.Job retry=AgentPreparationOperation.start(RuntimeEnvironment.application,p,(c,s)->ready(),(c,s,o)->ready());finish(retry);assertNotSame(failed,retry);assertTrue(retry.result.ready());
    }
    @Test public void progressIsMeasuredAndUnknownOutputIsDiscarded(){
        AgentPreparationOperation.Job j=new AgentPreparationOperation.Job(RuntimeEnvironment.application,profile("opencode"),(c,s)->ready(),(c,s,o)->ready());
        j.output("IMAGE_PROGRESS: 25% (100/400 bytes)");assertEquals(25,j.percent);assertEquals(100,j.received);assertEquals(400,j.total);
        j.output("PREPARATION:installing-components");assertEquals(-1,j.percent);assertEquals(0,j.total);
        j.output("Authorization: Bearer SYNTHETIC_TEST_SECRET");
        j.output("PREPARATION:SYNTHETIC_TEST_SECRET");j.output("STAGE:SYNTHETIC_TEST_SECRET");
        j.output("ERROR:SYNTHETIC_TEST_SECRET");j.output("MISSING:SYNTHETIC_TEST_SECRET");
        assertEquals("installing-components",j.stage);assertEquals("",j.component);assertEquals("",j.error);
        j.output("IMAGE_PROGRESS: 50% (100/99999999999999999999999999999 bytes)");assertEquals(-1,j.percent);
        j.output("READY:node");assertEquals("node",j.component);
    }

    @Test public void exceptionMessagesAreNeverExposedAndNoLogIsCreated()throws Exception{
        java.io.File root=RuntimeEnvironment.application.getFilesDir();
        java.io.File old=new java.io.File(AgentPreparation.directory(root),"operations/ubuntu-ai-codex.log");old.getParentFile().mkdirs();
        java.nio.file.Files.write(old.toPath(),"EXISTING_FIXTURE_LOG".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        AgentPreparationOperation.Job j=new AgentPreparationOperation.Job(RuntimeEnvironment.application,profile("codex"),(c,s)->ready(),(c,s,o)->{o.accept("SECRET_FIXTURE\nprivate output");throw new java.io.IOException("SECRET_FIXTURE");});j.run();
        assertEquals("failed",j.stage);assertEquals("PREPARATION_FAILED",j.error);
        assertFalse(AgentPreparationOperation.stage(j,RuntimeEnvironment.application).contains("SECRET_FIXTURE"));
        assertEquals("EXISTING_FIXTURE_LOG",new String(java.nio.file.Files.readAllBytes(old.toPath()),java.nio.charset.StandardCharsets.UTF_8));
        AgentPreparationOperation.Job cancelled=new AgentPreparationOperation.Job(RuntimeEnvironment.application,profile("claude"),(c,s)->ready(),(c,s,o)->{throw new AgentPreparationOperation.PreparationFailure("INSTALL_CANCELLED",130);});cancelled.run();
        assertEquals(Integer.valueOf(130),cancelled.exitCode);assertEquals("INSTALL_CANCELLED",cancelled.error);
        AgentPreparationOperation.Job retry=new AgentPreparationOperation.Job(RuntimeEnvironment.application,profile("claude"),(c,s)->ready(),(c,s,o)->ready());retry.run();assertEquals("complete",retry.stage);
        assertFalse(new java.io.File(old.getParentFile(),"ubuntu-ai-claude.log").exists());
    }
    @Test public void allFourAgentsShareUbuntuButKeepIndividualJobKeys(){
        java.util.Set<String> keys=new java.util.HashSet<>();for(String a:new String[]{"codex","claude","gemini","opencode"}){WorkspaceStore.Session p=profile(a);assertTrue(AgentPreparation.supports(p));assertEquals("ubuntu-ai",AgentEnvironments.alias(p));keys.add(AgentPreparationOperation.key(p));}assertEquals(4,keys.size());
    }
    @Test public void failedWindowShowsSafeErrorAndEnablesRetry()throws Exception{
        AgentPreparationOperation.jobs.clear();android.app.Activity a=org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup().get();WorkspaceStore.Session p=profile("codex");
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        AgentPreparationOperation.Job job=AgentPreparationOperation.start(a,p,(c,s)->ready(),(c,s,o)->{entered.countDown();release.await(3,TimeUnit.SECONDS);throw new java.io.IOException("PRIVATE_FIXTURE_OUTPUT");});assertTrue(entered.await(2,TimeUnit.SECONDS));
        AgentPreparationOperation.show(a,p,null);android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertFalse(dialog.getButton(-1).isEnabled());release.countDown();finish(job);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertTrue(dialog.isShowing());assertTrue(dialog.getButton(-1).isEnabled());assertEquals(android.view.View.VISIBLE,dialog.getButton(-1).getVisibility());
        assertFalse(AgentPreparationOperation.stage(job,a).contains("PRIVATE_FIXTURE_OUTPUT"));dialog.dismiss();a.finish();
    }
    @Test public void closingAndReopeningWindowKeepsOperationWithoutLaunchingChat()throws Exception{
        AgentPreparationOperation.jobs.clear();android.app.Activity a=org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup().get();WorkspaceStore.Session p=profile("claude");
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        AgentPreparationOperation.Job job=AgentPreparationOperation.start(a,p,(c,s)->ready(),(c,s,o)->{entered.countDown();assertTrue(release.await(30,TimeUnit.SECONDS));return ready();});assertTrue(entered.await(3,TimeUnit.SECONDS));
        AgentPreparationOperation.show(a,p,null);android.app.AlertDialog first=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(first.isShowing());assertFalse(first.getButton(-1).isEnabled());first.dismiss();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue("closing must not cancel preparation",job.active);assertTrue("dismiss releases the window observer",job.observers.isEmpty());
        AgentPreparationOperation.show(a,p,null);android.app.AlertDialog second=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(first,second);assertSame(job,AgentPreparationOperation.jobs.get(AgentPreparationOperation.key(p)));assertEquals(1,job.observers.size());
        release.countDown();finish(job);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(job.result.ready());second.dismiss();a.finish();
    }
}
