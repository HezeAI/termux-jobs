package com.termux.app.workspace;

import android.app.*;
import android.view.View;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentPreparationProgressTest {
    WorkspaceStore.Session profile(String agent){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment=agent.equals("codex")?"codex":"ubuntu";p.codexEnvironment="ubuntu-ai";p.claudeAgent=agent.equals("claude");p.geminiAgent=agent.startsWith("gemini");p.geminiAuth=agent.endsWith("vertex")?"vertex":"api";if(agent.equals("opencode"))p.aiAgent="opencode";return p;}
    AgentPreparation.Plan inventory(String values){return AgentPreparation.Plan.parse(values+"\nCHECK:done\n",0);}
    void idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
    void finish(AgentPreparationOperation.Job job)throws Exception{for(int n=0;job.active&&n<300;n++){idle();Thread.sleep(10);}assertFalse(job.active);idle();}
    @Test public void plansExcludeKnownReadyWorkAndOnlySuccessfulFinalVerificationFillsBar(){
        for(String a:new String[]{"codex","claude","gemini-api","gemini-vertex","opencode"}){
            AgentPreparation.Plan clean=inventory("MISSING:ubuntu\nMISSING:proot-distro"),partial=inventory("READY:ubuntu\nREADY:proot-distro\nMISSING:node"),ready=inventory("READY:ubuntu\nREADY:proot-distro\nREADY:node");
            assertEquals(6,new AgentPreparationOperation.PlanProgress(clean).total());assertEquals(3,new AgentPreparationOperation.PlanProgress(partial).total());assertEquals(2,new AgentPreparationOperation.PlanProgress(ready).total());
            AgentPreparationOperation.Job j=new AgentPreparationOperation.Job(RuntimeEnvironment.application,profile(a),(c,p)->clean,(c,p,o)->{o.accept("PREPARATION:preparing-termux-packages");o.accept("IMAGE_PROGRESS: 99% (20/100 bytes)");o.accept("STAGE:extracting-environment");o.accept("PREPARATION:installing-components");o.accept("PREPARATION:checking-cli");o.accept("PREPARATION:ready");o.accept("PREPARATION:verifying-components");return inventory("MISSING:node");});j.run();assertEquals("failed",j.stage);assertEquals(5,j.plan.completed);assertEquals(6,j.plan.total());
            AgentPreparationOperation.Job retry=new AgentPreparationOperation.Job(RuntimeEnvironment.application,profile(a),(c,p)->partial,(c,p,o)->{o.accept("PREPARATION:installing-components");o.accept("PREPARATION:verifying-components");return ready;});retry.run();assertEquals("complete",retry.stage);assertEquals(3,retry.plan.completed);
        }
    }
    @Test public void dialogAndSelectedCardObserveOneJobAcrossCloseReopenQueueAndCompletion()throws Exception{
        for(String agent:new String[]{"codex","claude","gemini-api","gemini-vertex","opencode"}){
            AgentPreparationOperation.jobs.clear();Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"en");WorkspaceStore.Session original=profile(agent);WorkspaceStore.Session[] selected={original};CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();int listeners=AgentPreparationOperation.listeners.size();
            AgentPreparationOperation.Job job=AgentPreparationOperation.start(a,original,(c,p)->inventory("MISSING:ubuntu\nMISSING:proot-distro"),(c,p,o)->{calls.incrementAndGet();o.accept("STAGE:connecting-image-download");o.accept("IMAGE_PROGRESS: 99% (20/100 bytes)");entered.countDown();assertTrue(release.await(20,TimeUnit.SECONDS));o.accept("PREPARATION:verifying-components");return inventory("READY:ubuntu");});assertTrue(entered.await(3,TimeUnit.SECONDS));
            TextView button=new TextView(a);button.setText("Launch environment check");button.setOnClickListener(v->AgentPreparationOperation.show(a,selected[0],null));AgentPreparationCard card=new AgentPreparationCard(a,button,()->selected[0]);a.setContentView(card);idle();
            AgentPreparationOperation.Job next=null;
            try{
                assertTrue(card.count.getText().toString().contains("2 of 6"));assertTrue(card.amount.getText().toString().contains("20 / 100"));assertEquals(2,card.overall.getProgress());button.performClick();idle();AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();ProgressBar total=dialog.getWindow().getDecorView().findViewWithTag("preparation-overall-progress");ProgressBar current=dialog.getWindow().getDecorView().findViewWithTag("preparation-stage-progress");assertFalse(total.isIndeterminate());assertEquals(2,total.getProgress());assertEquals(6,total.getMax());assertFalse(current.isIndeterminate());assertEquals(20,current.getProgress());dialog.dismiss();idle();assertTrue(job.active);
                job.output("STAGE:extracting-environment");idle();assertTrue(card.phase.getText().toString().contains("Extracting"));assertEquals(3,card.overall.getProgress());button.performClick();idle();dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();current=dialog.getWindow().getDecorView().findViewWithTag("preparation-stage-progress");assertTrue(current.isIndeterminate());dialog.dismiss();
                selected[0]=profile(agent.equals("opencode")?"codex":"opencode");card.refresh();assertEquals(View.GONE,card.phase.getVisibility());next=AgentPreparationOperation.start(a,selected[0],(c,p)->inventory("READY:ubuntu"),(c,p,o)->inventory("READY:ubuntu"));idle();assertEquals("queued",next.stage);assertTrue(card.phase.getText().toString().contains("Waiting"));assertTrue(card.overall.isIndeterminate());
                selected[0]=original;card.refresh();assertTrue(card.phase.getText().toString().contains("Extracting"));release.countDown();finish(job);finish(next);assertEquals(6,card.overall.getProgress());assertTrue(card.phase.getText().toString().contains("ready"));
                button.performClick();idle();dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertSame(job,AgentPreparationOperation.current(original));assertEquals(1,calls.get());assertTrue(dialog.isShowing());dialog.dismiss();
            }finally{release.countDown();finish(job);if(next!=null)finish(next);card.close();a.finish();idle();assertEquals(listeners,AgentPreparationOperation.listeners.size());}
        }
    }
    @Test public void failedCardKeepsSafeErrorAndPartialOverallProgress(){Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"en");WorkspaceStore.Session p=profile("claude");AgentPreparationOperation.Job j=new AgentPreparationOperation.Job(a,p,(c,s)->inventory("MISSING:ubuntu"),(c,s,o)->{o.accept("STAGE:connecting-image-download");o.accept("ERROR:IMAGE_DOWNLOAD_NETWORK");throw new java.io.IOException("SYNTHETIC_SECRET");});j.run();AgentPreparationOperation.jobs.put(AgentPreparationOperation.key(p),j);AgentPreparationCard card=new AgentPreparationCard(a,new TextView(a),()->p);a.setContentView(card);idle();assertFalse(card.phase.getText().toString().contains("SYNTHETIC_SECRET"));assertTrue(card.phase.getText().toString().contains("Image download failed"));assertTrue(card.overall.getProgress()<card.overall.getMax());assertFalse(card.overall.isIndeterminate());card.close();a.finish();}
}
