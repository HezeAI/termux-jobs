package com.termux.app.workspace;

import android.app.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Synthetic inventory/workers. No package manager, provider or real guest is invoked. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentPreparationMatrixTest {
    String[] variants={"codex","claude","gemini-api","gemini-vertex","opencode"};
    WorkspaceStore.Session profile(String variant){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=variant.equals("codex")?"codex":"ubuntu";p.codexEnvironment="ubuntu-ai";p.claudeAgent=variant.equals("claude");p.geminiAgent=variant.startsWith("gemini");p.geminiAuth=variant.endsWith("vertex")?"vertex":"api";if(variant.equals("opencode"))p.aiAgent="opencode";return p;}
    AgentPreparation.Plan plan(String report){return AgentPreparation.Plan.parse(report+"\nCHECK:done\n",0);}
    void idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
    void finish(AgentPreparationOperation.Job j)throws Exception {for(int n=0;j.active&&n<300;n++){idle();Thread.sleep(10);}assertFalse(j.active);idle();}
    @Test public void everyAgentAndGeminiModeRetainsRealPhaseThroughMissingReadyFailureAndRetry(){
        for(String variant:variants)for(String scenario:new String[]{"no-proot-no-ubuntu","missing-agent-components","ready","download-error","package-error"}){
            WorkspaceStore.Session p=profile(variant);AtomicInteger installs=new AtomicInteger();
            AgentPreparation.Plan inventory=plan(scenario.equals("no-proot-no-ubuntu")?"MISSING:proot-distro\nMISSING:ubuntu":scenario.equals("ready")?"READY:ubuntu":"READY:ubuntu\nMISSING:node");
            AgentPreparationOperation.Worker worker=(c,s,o)->{
                assertEquals("ubuntu-ai",AgentEnvironments.alias(s));assertEquals(p.geminiAuth,s.geminiAuth);
                if(scenario.equals("no-proot-no-ubuntu")||scenario.equals("package-error")){
                    o.accept("PREPARATION:preparing-termux-packages");o.accept("COMPONENT:proot-distro");
                    if(scenario.equals("package-error")){o.accept("PREPARATION:failed-ubuntu");throw new AgentPreparationOperation.PreparationFailure("PREPARATION_EXIT_FAILED",71);}
                    installs.incrementAndGet();o.accept("STAGE:download-pinned-ubuntu-ai");o.accept("IMAGE_PROGRESS: 55% (20/100 bytes)");o.accept("STAGE:extracting-environment");
                }
                if(scenario.equals("download-error")){o.accept("STAGE:connecting-image-download");o.accept("ERROR:IMAGE_DOWNLOAD_NETWORK");o.accept("PREPARATION:failed-ubuntu");throw new AgentPreparationOperation.PreparationFailure("PREPARATION_EXIT_FAILED",33);}
                if(!scenario.equals("ready"))o.accept("PREPARATION:installing-components");o.accept("PREPARATION:verifying-components");return plan("READY:ubuntu");
            };
            AgentPreparationOperation.Job j=new AgentPreparationOperation.Job(RuntimeEnvironment.application,p,(c,s)->inventory,worker);j.run();
            assertEquals(scenario.equals("no-proot-no-ubuntu")?1:0,installs.get());
            if(scenario.endsWith("error")){
                assertEquals("failed",j.stage);assertEquals(scenario.equals("package-error")?"preparing-termux-packages":"STAGE:connecting-image-download",j.failedStage);
                AgentPreparationOperation.Job retry=new AgentPreparationOperation.Job(RuntimeEnvironment.application,p,(c,s)->inventory,(c,s,o)->{o.accept("PREPARATION:verifying-components");return plan("READY:ubuntu");});retry.run();assertEquals("complete",retry.stage);
            }else assertEquals("complete",j.stage);
            assertEquals(-1,j.percent);
        }
    }
    @Test public void checkAndFirstLaunchEntryKeepOneOperationAcrossCloseAndReopen()throws Exception{
        for(String variant:variants){
            AgentPreparationOperation.jobs.clear();Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"en");WorkspaceStore.Session p=profile(variant);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger launched=new AtomicInteger();
            AgentPreparation.Plan missing=plan("MISSING:proot-distro\nMISSING:ubuntu");
            AgentPreparationOperation.Job j=AgentPreparationOperation.start(a,p,(c,s)->missing,(c,s,o)->{o.accept("PREPARATION:preparing-termux-packages");entered.countDown();assertTrue(release.await(20,TimeUnit.SECONDS));return plan("READY:ubuntu");});assertTrue(entered.await(3,TimeUnit.SECONDS));
            try{
                AgentPreparationOperation.show(a,p,ignored->launched.incrementAndGet());AlertDialog check=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();idle();assertTrue(AgentPreparationOperation.stage(j,a).contains("Termux packages"));check.dismiss();idle();assertTrue(j.active);assertTrue(j.observers.isEmpty());
                AgentPreparation.beforeLaunch(a,p,(c,s)->missing,ignored->launched.incrementAndGet());
                AlertDialog consent=null;for(int n=0;n<200;n++){idle();consent=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();if(consent.getButton(-1)!=null&&consent.getButton(-1).getText().toString().equals("Prepare and launch"))break;Thread.sleep(5);}
                assertNotNull(consent);assertEquals("Prepare and launch",consent.getButton(-1).getText().toString());consent.getButton(-1).performClick();idle();
                AlertDialog preparation=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertSame(j,AgentPreparationOperation.jobs.get(AgentPreparationOperation.key(p)));assertTrue(preparation.isShowing());assertEquals(0,launched.get());assertFalse(preparation.getButton(-1).isEnabled());
                preparation.dismiss();idle();assertTrue(j.active);AgentPreparationOperation.show(a,p,ignored->launched.incrementAndGet());AlertDialog reopened=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertSame(j,AgentPreparationOperation.jobs.get(AgentPreparationOperation.key(p)));
                release.countDown();finish(j);assertEquals(1,launched.get());reopened.dismiss();
            }finally{release.countDown();finish(j);a.finish();}
        }
    }
    @Test public void allFiveVariantsSerializeWithDistinctApiAndVertexJobs()throws Exception{
        AgentPreparationOperation.jobs.clear();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger active=new AtomicInteger(),maximum=new AtomicInteger(),calls=new AtomicInteger();java.util.List<AgentPreparationOperation.Job> jobs=new java.util.ArrayList<>();
        try{
            for(String variant:variants){WorkspaceStore.Session p=profile(variant);AgentPreparationOperation.Worker w=(c,s,o)->{int current=active.incrementAndGet();maximum.accumulateAndGet(current,Math::max);try{calls.incrementAndGet();entered.countDown();assertTrue(release.await(20,TimeUnit.SECONDS));return plan("READY:ubuntu");}finally{active.decrementAndGet();}};
                AgentPreparationOperation.Job j=AgentPreparationOperation.start(RuntimeEnvironment.application,p,(c,s)->plan("READY:ubuntu"),w);jobs.add(j);assertSame(j,AgentPreparationOperation.start(RuntimeEnvironment.application,p,(c,s)->plan("READY:ubuntu"),w));
            }
            assertTrue(entered.await(3,TimeUnit.SECONDS));assertEquals(1,calls.get());assertNotSame(jobs.get(2),jobs.get(3));release.countDown();for(AgentPreparationOperation.Job j:jobs)finish(j);assertEquals(5,calls.get());assertEquals(1,maximum.get());
        }finally{release.countDown();for(AgentPreparationOperation.Job j:jobs)finish(j);}
    }

    @Test public void readyFirstLaunchDoesNotStartPreparationAndGeneratedShellKeepsAgentAuthRoute()throws Exception{
        for(String variant:variants){AgentPreparationOperation.jobs.clear();Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceStore.Session p=profile(variant);AtomicInteger launched=new AtomicInteger();
            try{AgentPreparation.beforeLaunch(a,p,(c,s)->plan("READY:ubuntu"),ignored->launched.incrementAndGet());for(int n=0;n<200&&launched.get()==0;n++){idle();Thread.sleep(5);}assertEquals(1,launched.get());assertTrue(AgentPreparationOperation.jobs.isEmpty());
                p.id="11111111-1111-4111-8111-111111111111";p.codexSocketId="fixture";p.agentSetupAllowed=true;
                java.io.File files=java.nio.file.Files.createTempDirectory("prep-route").toFile();java.io.File host=SessionShell.prepare(files,p);
                String text=new String(java.nio.file.Files.readAllBytes(host.toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(text.contains("jobs_agent_host_prepare"));assertTrue(text.contains("JOBS_AGENT_UBUNTU='ubuntu-ai'"));
                String guest=new String(java.nio.file.Files.readAllBytes(new java.io.File(SessionShell.directory(files,p.id),"ubuntu-bashrc").toPath()),java.nio.charset.StandardCharsets.UTF_8);String command=SessionShell.quote("jobs_agent_prepare '"+AgentPreparation.agent(p)+"' '"+p.geminiAuth+"'");assertTrue(variant,guest.contains(command.substring(1,command.length()-1)));assertEquals(variant.equals("gemini-vertex"),guest.contains("vertex-bootstrap.sh"));
                assertEquals(0,new ProcessBuilder("/bin/bash","-n",host.getAbsolutePath()).start().waitFor());
            }finally{a.finish();}
        }
    }
}
