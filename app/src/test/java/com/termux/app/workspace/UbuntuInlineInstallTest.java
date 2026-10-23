package com.termux.app.workspace;
import android.app.*;import android.os.Looper;import android.view.*;import android.widget.*;import com.termux.app.TermuxActivity;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import java.util.*;import java.util.concurrent.*;import java.io.File;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class UbuntuInlineInstallTest {
 private EnvironmentSetup setup;private CountDownLatch started,release,finished;private volatile File cancel;private volatile String alias;private volatile int runs;private volatile java.util.function.Consumer<String> updateProgress;
 private void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private View row(AlertDialog d,int i){return d.getListView().getAdapter().getView(i,null,d.getListView());}
 private TextView badge(View r,String alias){return r.findViewWithTag("environment-install-"+alias);}
 @Before public void before(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();WorkspaceLanguage.select(a,"en");started=new CountDownLatch(1);release=new CountDownLatch(1);finished=new CountDownLatch(1);setup=new EnvironmentSetup(a,()->Arrays.asList(new WorkspaceEnvironments.Choice("ubuntu","ubuntu","Ubuntu","/root",false),new WorkspaceEnvironments.Choice("proot:ubuntu-ai","ubuntu-ai","Ubuntu AI","/root",false)),(target,update,marker)->{runs++;alias=target;cancel=marker;updateProgress=update;update.accept("Downloading 40%");started.countDown();release.await(5,TimeUnit.SECONDS);boolean stopped=marker.exists();finished.countDown();return new EnvironmentSetup.Result(stopped?"ERROR:INSTALL_CANCELLED":"READY:environment\nREADY:shell\nCHECK:done",stopped?130:0);});}
 @After public void after(){release.countDown();setup.close();}
 private AlertDialog begin()throws Exception{setup.choose(c->fail("installation must not select session"));AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();badge(row(d,0),"ubuntu").performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();idle();assertTrue(started.await(3,TimeUnit.SECONDS));idle();assertEquals("ubuntu",alias);return d;}
 private void complete()throws Exception{release.countDown();assertTrue(finished.await(3,TimeUnit.SECONDS));for(int i=0;i<30;i++){Thread.sleep(10);idle();}}
 @Test public void ordinaryUbuntuProgressAndSuccessStayInTheirOwnCardAfterReopening()throws Exception{
  AlertDialog first=begin();View ubuntu=row(first,0),ai=row(first,1);assertEquals("Stop",badge(ubuntu,"ubuntu").getText().toString());assertEquals("Install",badge(ai,"ubuntu-ai").getText().toString());ViewGroup progress=ubuntu.findViewWithTag("environment-progress-ubuntu");assertEquals(View.VISIBLE,progress.getVisibility());ProgressBar bar=(ProgressBar)progress.getChildAt(0);assertFalse(bar.isIndeterminate());assertEquals(40,bar.getProgress());assertEquals(View.GONE,ai.findViewWithTag("environment-progress-ubuntu-ai").getVisibility());
  first.dismiss();assertFalse(cancel.exists());setup.choose(c->{});AlertDialog second=ShadowAlertDialog.getLatestAlertDialog();View current=row(second,0);assertEquals("Stop",badge(current,"ubuntu").getText().toString());complete();current=row(second,0);assertEquals("Installed",badge(current,"ubuntu").getText().toString());assertEquals(View.GONE,current.findViewWithTag("environment-progress-ubuntu").getVisibility());assertEquals("Install",badge(row(second,1),"ubuntu-ai").getText().toString());second.dismiss();
 }
 @Test public void ordinaryUbuntuStopDoesNotMarkEitherUbuntuInstalled()throws Exception{
  AlertDialog d=begin();View current=row(d,0);badge(current,"ubuntu").performClick();current=row(d,0);assertTrue(cancel.exists());assertEquals("Stopping…",badge(current,"ubuntu").getText().toString());assertFalse(badge(current,"ubuntu").isEnabled());assertEquals("Stopping installer…",((TextView)((ViewGroup)current.findViewWithTag("environment-progress-ubuntu")).getChildAt(1)).getText().toString());complete();current=row(d,0);assertEquals("Resume",badge(current,"ubuntu").getText().toString());assertTrue(badge(current,"ubuntu").isEnabled());assertEquals("Install",badge(row(d,1),"ubuntu-ai").getText().toString());d.dismiss();
 }
 @Test public void attachedVisibleUbuntuCardUpdatesWithoutReopening()throws Exception{
  AlertDialog d=begin();ListView list=d.getListView();list.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));list.layout(0,0,500,900);View actual=list.getChildAt(0);assertNotNull(actual);TextView status=badge(actual,"ubuntu");assertEquals("Stop",status.getText().toString());list.getAdapter().getView(0,null,list);updateProgress.accept("Downloading 65%");idle();ViewGroup measured=list.getChildAt(0).findViewWithTag("environment-progress-ubuntu");assertEquals(65,((ProgressBar)measured.getChildAt(0)).getProgress());complete();assertEquals("Installed",badge(list.getChildAt(0),"ubuntu").getText().toString());d.dismiss();
 }

 @Test public void closingChooserRemovesVisibleSubscriptionsButKeepsInstallRunning()throws Exception{
  AlertDialog d=begin();ListView list=d.getListView();list.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));list.layout(0,0,500,900);
  java.lang.reflect.Field field=EnvironmentSetup.class.getDeclaredField("cardUpdates");field.setAccessible(true);Map<?,?> subscriptions=(Map<?,?>)field.get(setup);assertTrue(subscriptions.containsKey("ubuntu"));d.dismiss();idle();assertTrue(subscriptions.isEmpty());assertFalse(cancel.exists());complete();
 }

 @Test public void cancelledInstallerCanRestartOnlyAfterItsWorkerFinishes()throws Exception{
  AlertDialog d=begin();View current=row(d,0);badge(current,"ubuntu").performClick();assertEquals(1,runs);assertFalse(badge(row(d,0),"ubuntu").isEnabled());complete();
  badge(row(d,0),"ubuntu").performClick();ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();
  for(int i=0;i<40;i++){Thread.sleep(10);idle();}
  assertEquals(2,runs);assertEquals("Installed",badge(row(d,0),"ubuntu").getText().toString());d.dismiss();
 }

}
