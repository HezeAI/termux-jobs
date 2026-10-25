package com.termux.app.workspace;
import android.app.*;import android.os.*;import android.widget.*;import com.termux.app.TermuxActivity;import com.termux.x11.X11StartupView;import com.termux.R;import java.lang.reflect.*;import java.util.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDesktopPreparationTest {
 private TermuxActivity a;private WorkspaceDesktop ui;private WorkspaceStore.Session p;private X11StartupView view;private boolean[] visible={true};
 private Object get(String n)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField(n);f.setAccessible(true);return f.get(ui);}
 private void set(String n,Object v)throws Exception{Field f=WorkspaceDesktop.class.getDeclaredField(n);f.setAccessible(true);f.set(ui,v);}
 private void invoke(String n)throws Exception{Method m=WorkspaceDesktop.class.getDeclaredMethod(n);m.setAccessible(true);m.invoke(ui);}
 private void accept(Bundle b)throws Exception{Method m=WorkspaceDesktop.class.getDeclaredMethod("accept",Bundle.class);m.setAccessible(true);m.invoke(ui,b);}
 private Bundle state(String phase){Bundle b=new Bundle();b.putString("target","debian");b.putString("phase",phase);return b;}
 @Before public void setup()throws Exception{a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);p=new WorkspaceStore.Session();p.id="owner";p.handle="handle";p.environment="proot:debian";ui=new WorkspaceDesktop(a,()->p,()->visible[0],new TextView(a),()->{},()->{});view=new X11StartupView(a,()->{});set("preparation",view);set("preparationProfile",p);set("launchAfterCheck",p);((Set<String>)get("graphical")).add(p.id);}
 @After public void close(){ui.close();}
 @Test public void installerDiagnosticExportKeepsFailureWithoutExportingRawDetail()throws Exception{
  Bundle b=state("error");b.putString("componentOperation","install");b.putString("componentResult","failed");b.putString("installStage","downloading");b.putInt("componentChildExit",19);b.putInt("componentGuardianExit",137);b.putString("componentDiagnostic","PACKAGE_SIGNATURE_DOWNLOAD_FAILED\nNETWORK_TIMEOUT");b.putString("componentErrors","ERROR: failed to download signature from [url]: Not Found");b.putString("detail","https://user:private-password@host/path?token=private-token");accept(b);
  String report=ui.sessionDiagnostic(p);for(String text:new String[]{"Component operation: install","Component result: failed","Component stage: downloading","Component child exit: 19","Component guardian exit: 137","PACKAGE_SIGNATURE_DOWNLOAD_FAILED","NETWORK_TIMEOUT","signature from [url]: Not Found"})assertTrue(text,report.contains(text));
  assertFalse(report.contains("private-password"));assertFalse(report.contains("private-token"));assertFalse(report.contains("https://"));
 }
 @Test public void fullInstallerFailureIsScrollableSelectableAndCopiedOnlyOnTap()throws Exception{
  String text="download progress\nerror: failed retrieving file 'libcups.pkg' from mirror.example : Operation too slow\nerror: failed to commit transaction (download library error)";
  Bundle b=state("error");b.putString("detail",text);accept(b);invoke("preparationUpdate");
  TextView details=view.findViewWithTag("x11-preparation-details");assertEquals(android.view.View.VISIBLE,details.getVisibility());assertNull(ShadowAlertDialog.getLatestAlertDialog());details.performClick();
  AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();TextView full=d.getWindow().getDecorView().findViewWithTag("x11-install-full-log");assertEquals(text,full.getText().toString());assertTrue(full.isTextSelectable());assertTrue(full.getMaxLines()>1);
  d.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();assertTrue(d.isShowing());android.content.ClipboardManager clipboard=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);assertEquals(text,clipboard.getPrimaryClip().getItemAt(0).getText().toString());d.dismiss();
  accept(state("checking"));invoke("preparationUpdate");assertEquals(android.view.View.GONE,details.getVisibility());
 }
 @Test public void checkingAndInstallingUpdateInlineWithoutOpeningLogDialog()throws Exception{
  Bundle b=state("checking");accept(b);assertNull(get("progress"));ProgressBar bar=view.findViewWithTag("x11-startup-progress");assertTrue(bar.isIndeterminate());
  b=state("installing");b.putString("status","Packages");b.putString("detail","old line\nnew package\n");b.putInt("installPercent",42);b.putString("installStage","downloading");b.putLong("downloadBytes",42000000);b.putLong("downloadTotal",100000000);accept(b);
  assertNull(get("progress"));assertSame(view,get("preparation"));assertFalse(bar.isIndeterminate());assertEquals(42,bar.getProgress());assertEquals("new package",((TextView)view.findViewWithTag("x11-preparation-detail")).getText().toString());assertTrue(((TextView)view.findViewWithTag("x11-startup-step")).getText().toString().contains("42% (42.0 MB / 100.0 MB)"));
 }
 @Test public void cancellationRetainsProgressAndLocksRendererUntilCleanupCompletes()throws Exception{
  set("remote",new Binder());set("preparationCancelling",true);Bundle b=state("cancelling");b.putString("componentOperation","install");b.putString("status","Stopping installer");b.putInt("installPercent",42);accept(b);
  assertSame(p,get("preparationProfile"));assertTrue((Boolean)get("preparationCancelling"));assertFalse(((Button)view.findViewWithTag("x11-preparation-cancel")).isEnabled());assertEquals(42,((ProgressBar)view.findViewWithTag("x11-startup-progress")).getProgress());
  Method busy=WorkspaceDesktop.class.getDeclaredMethod("rendererBackendBusy");busy.setAccessible(true);assertTrue((Boolean)busy.invoke(ui));accept(state("cancelled"));assertFalse((Boolean)get("preparationCancelling"));assertTrue(((Button)view.findViewWithTag("x11-preparation-cancel")).isEnabled());
 }
 @Test public void alreadyRunningDesktopReattachesWithoutWaitingForInstalled()throws Exception{
  org.robolectric.util.ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",30);try{
   java.util.concurrent.CountDownLatch seen=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<String> operation=new java.util.concurrent.atomic.AtomicReference<>();
   Binder binder=new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){data.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);operation.set(data.readBundle(getClass().getClassLoader()).getString("op"));reply.writeNoException();reply.writeBundle(state("running"));seen.countDown();return true;}};
   set("remote",binder);set("launchCheckAcknowledged",true);Bundle b=state("running");b.putBoolean("started",true);accept(b);assertTrue(seen.await(3,java.util.concurrent.TimeUnit.SECONDS));assertEquals("launch",operation.get());assertNull(get("launchAfterCheck"));assertNull(get("preparationProfile"));assertTrue(((Set<?>)get("graphical")).contains(p.id));
  }finally{org.robolectric.util.ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",28);}
 }
 @Test public void cancelDuringCheckRaceStopsRunningDesktopInsteadOfCancellingAbsentCheck()throws Exception{
  visible[0]=false;org.robolectric.util.ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",30);try{
   java.util.concurrent.CountDownLatch seen=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<String> operation=new java.util.concurrent.atomic.AtomicReference<>();
   Binder binder=new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){data.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);operation.set(data.readBundle(getClass().getClassLoader()).getString("op"));reply.writeNoException();reply.writeBundle(state("stopping"));seen.countDown();return true;}};
   set("remote",binder);Bundle b=state("running");b.putBoolean("started",true);set("state",b);invoke("preparationAction");assertTrue(seen.await(3,java.util.concurrent.TimeUnit.SECONDS));assertEquals("stop",operation.get());assertNull(get("launchAfterCheck"));
  }finally{org.robolectric.util.ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",28);}
 }
 @Test public void missingNeedsAcknowledgementAndNoReturnsToTerminal()throws Exception{
  Bundle b=state("missing");accept(b);assertFalse((Boolean)get("preparationNeedsInstall"));set("launchCheckAcknowledged",true);accept(b);
  AlertDialog prompt=ShadowAlertDialog.getLatestAlertDialog();assertTrue(prompt.isShowing());assertTrue((Boolean)get("preparationNeedsInstall"));prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(get("preparationProfile"));assertFalse(((Set<?>)get("graphical")).contains(p.id));assertNull(get("launchAfterCheck"));
 }
 @Test public void hiddenMissingResultWaitsForOriginBeforeShowingPrompt()throws Exception{
  visible[0]=false;set("launchCheckAcknowledged",true);accept(state("missing"));assertTrue((Boolean)get("preparationNeedsInstall"));assertNull(get("preparationPrompt"));assertSame(p,get("preparationProfile"));visible[0]=true;Method m=WorkspaceDesktop.class.getDeclaredMethod("confirmInstall",WorkspaceStore.Session.class);m.setAccessible(true);m.invoke(ui,p);assertNotNull(get("preparationPrompt"));
 }
 @Test public void errorsRemainInlineAndBackClearsPendingLaunch()throws Exception{
  set("launchCheckAcknowledged",true);Bundle b=state("error");b.putString("status","Failed to install");accept(b);invoke("preparationUpdate");assertNull(get("progress"));Button back=view.findViewWithTag("x11-preparation-cancel");assertTrue(back.isEnabled());back.performClick();assertNull(get("preparationProfile"));assertNull(get("launchAfterCheck"));
 }
 @Test public void cancellingBeforeBindingCannotLeaveQueuedCheckOrLaunch()throws Exception{
  set("pending","check");set("pendingProfile",p);invoke("preparationAction");assertEquals("",get("pending"));assertNull(get("pendingProfile"));assertNull(get("launchAfterCheck"));assertNull(get("preparationProfile"));
 }
 @Test public void otherEnvironmentSnapshotCannotUpdateThisPreparation()throws Exception{
  Bundle b=state("installing");b.putString("target","alpine");b.putInt("installPercent",90);set("state",b);invoke("preparationUpdate");assertTrue(((ProgressBar)view.findViewWithTag("x11-startup-progress")).isIndeterminate());assertSame(p,get("preparationProfile"));
 }
 @Test public void launchAcknowledgementHandsOffWithoutReopeningInstaller()throws Exception{
  set("preparationLaunching",true);set("launchAfterCheck",null);accept(state("preparing"));assertNull(get("preparationProfile"));assertNull(get("progress"));assertTrue(((Set<?>)get("graphical")).contains(p.id));
 }
 @Test public void reopeningHiddenPreparationDoesNotQueueAnotherCheck()throws Exception{
  ((Set<?>)get("graphical")).clear();set("pending","unchanged");Method m=WorkspaceDesktop.class.getDeclaredMethod("menuAction",WorkspaceStore.Session.class,int.class);m.setAccessible(true);m.invoke(ui,p,0);assertTrue(((Set<?>)get("graphical")).contains(p.id));assertEquals("unchanged",get("pending"));assertSame(p,get("preparationProfile"));
 }

 @Test public void rendererOnInstalledDesktopChangesOnlyModeWithoutLaunching()throws Exception{
  set("remote",new Binder());set("preparationRenderer","cpu");set("launchCheckAcknowledged",true);set("pending","unchanged");accept(state("installed"));
  assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"debian"));assertNull(get("preparationProfile"));assertEquals("unchanged",get("pending"));assertFalse(((Bundle)get("state")).getBoolean("started"));
 }
 @Test public void rendererChoiceSurvivesDesktopInstallConfirmation()throws Exception{
  set("preparationRenderer","turnip");set("launchCheckAcknowledged",true);accept(state("missing"));AlertDialog prompt=ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("turnip",get("preparationRenderer"));assertTrue((Boolean)get("rendererLaunchAfterInstall"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"debian"));
 }
 @Test public void gpuCancellationDoesNotSendBackendDesktopCommands()throws Exception{
  int[] cancelled={0};set("preparationGpu",true);set("rendererCancel",(Runnable)()->cancelled[0]++);set("pending","unchanged");invoke("preparationAction");assertEquals(1,cancelled[0]);assertTrue((Boolean)get("preparationCancelling"));assertEquals("unchanged",get("pending"));
 }
 @Test public void declinedGpuComponentsCannotLaunchAfterDesktopInstallation()throws Exception{
  set("remote",new Binder());set("preparationRenderer","turnip");set("rendererLaunchAfterInstall",true);set("launchCheckAcknowledged",true);set("pending","unchanged");accept(state("installed"));AlertDialog prompt=ShadowAlertDialog.getLatestAlertDialog();prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue((Boolean)get("rendererPreparationFailed"));assertFalse((Boolean)get("preparationLaunching"));assertEquals("unchanged",get("pending"));
 }
 @Test public void firstRendererInstallLaunchesOnlyAfterPreparationCompletes()throws Exception{
  org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION.class,"SDK_INT",30);try{
  visible[0]=false;java.util.concurrent.CountDownLatch seen=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<Bundle> request=new java.util.concurrent.atomic.AtomicReference<>();
  Binder fake=new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){data.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);Bundle q=data.readBundle(getClass().getClassLoader());request.set(q);Bundle result=state("preparing");result.putBoolean("started",true);reply.writeNoException();reply.writeBundle(result);seen.countDown();return true;}};
  set("remote",fake);set("preparationRenderer","cpu");set("rendererLaunchAfterInstall",true);set("launchCheckAcknowledged",true);accept(state("installed"));assertTrue(seen.await(3,java.util.concurrent.TimeUnit.SECONDS));assertEquals("launch",request.get().getString("op"));assertEquals("debian",request.get().getString("target"));assertEquals("cpu",request.get().getString("renderer"));assertEquals("owner:handle",request.get().getString("owner"));Shadows.shadowOf(Looper.getMainLooper()).idle();
  }finally{org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION.class,"SDK_INT",28);}
 }

 @Test public void missingGpuConfirmationWaitsUntilItsOriginIsVisible()throws Exception{
  visible[0]=false;set("remote",new Binder());set("preparationRenderer","turnip");set("launchCheckAcknowledged",true);accept(state("installed"));assertTrue((Boolean)get("rendererReadyForPreparation"));assertFalse((Boolean)get("preparationGpu"));assertFalse((Boolean)get("preparationLaunching"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"debian"));
 }

 @Test public void authoritativelyVerifiedGpuDoesNotLaunchOrDownload()throws Exception{
  java.io.File root=new java.io.File(new java.io.File(a.getFilesDir(),"workspace-x11-gpu"),WorkspaceDesktopGpu.directory("debian"));assertTrue(root.mkdirs()||root.isDirectory());java.io.File marker=new java.io.File(root,"ready.json");java.nio.file.Files.write(marker.toPath(),"{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  try{set("remote",new Binder());set("preparationRenderer","turnip");set("launchCheckAcknowledged",true);set("pending","unchanged");Bundle checked=state("installed");checked.putString("renderer","turnip");checked.putBoolean("rendererComponents",true);accept(checked);assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"debian"));assertNull(get("preparationProfile"));assertEquals("unchanged",get("pending"));}finally{marker.delete();}
 }
 @Test public void rendererControlsRemainBusyDuringOwnPreparationButBackendCanBeIdle()throws Exception{
  set("remote",new Binder());set("state",state("installed"));set("preparationGpu",true);Method uiBusy=WorkspaceDesktop.class.getDeclaredMethod("rendererBusy"),backend=WorkspaceDesktop.class.getDeclaredMethod("rendererBackendBusy");uiBusy.setAccessible(true);backend.setAccessible(true);assertTrue((Boolean)uiBusy.invoke(ui));assertFalse((Boolean)backend.invoke(ui));
 }

 @Test public void selectGpuBeforeDesktopInstallationOnlySavesChoice()throws Exception{
  visible[0]=false;set("remote",new Binder());set("state",state("missing"));set("preparationProfile",null);set("launchAfterCheck",null);((Set<?>)get("graphical")).clear();set("pending","unchanged");
  Method choose=WorkspaceDesktop.class.getDeclaredMethod("selectRenderer",WorkspaceStore.Session.class,String.class);choose.setAccessible(true);choose.invoke(ui,p,"turnip");
  assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"debian"));assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"alpine"));assertFalse(WorkspaceDesktopGpu.installed(a,"debian"));assertNull(get("preparationProfile"));assertNull(get("launchAfterCheck"));assertFalse(((Set<?>)get("graphical")).contains(p.id));assertEquals("unchanged",get("pending"));assertFalse((Boolean)get("preparationGpu"));
 }
 @Test public void selectingCpuOnInstalledDesktopDoesNotCheckInstallOrLaunch()throws Exception{
  visible[0]=false;set("remote",new Binder());set("state",state("installed"));set("preparationProfile",null);set("launchAfterCheck",null);set("pending","unchanged");a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:debian","turnip").commit();
  Method choose=WorkspaceDesktop.class.getDeclaredMethod("selectRenderer",WorkspaceStore.Session.class,String.class);choose.setAccessible(true);choose.invoke(ui,p,"cpu");assertEquals("cpu",WorkspaceDesktopGpu.mode(a,"debian"));assertNull(get("preparationProfile"));assertNull(get("launchAfterCheck"));assertEquals("unchanged",get("pending"));
 }
 @Test public void switchingToDesktopPreparesPreviouslySelectedMissingGpu()throws Exception{
  Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);org.robolectric.util.ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",30);try{
   visible[0]=false;set("preparationProfile",null);set("launchAfterCheck",null);((Set<?>)get("graphical")).clear();a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:debian","turnip").commit();
   java.util.concurrent.CountDownLatch seen=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<Bundle> request=new java.util.concurrent.atomic.AtomicReference<>();
   Binder fake=new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags){data.enforceInterface(com.termux.x11.X11Remote.DESCRIPTOR);request.set(data.readBundle(getClass().getClassLoader()));reply.writeNoException();reply.writeBundle(state("checking"));seen.countDown();return true;}};
   set("remote",fake);Method action=WorkspaceDesktop.class.getDeclaredMethod("menuAction",WorkspaceStore.Session.class,int.class);action.setAccessible(true);action.invoke(ui,p,0);assertTrue(seen.await(3,java.util.concurrent.TimeUnit.SECONDS));assertEquals("check",request.get().getString("op"));assertEquals("turnip",get("preparationRenderer"));assertTrue((Boolean)get("rendererLaunchAfterInstall"));Shadows.shadowOf(Looper.getMainLooper()).idle();
   set("launchCheckAcknowledged",true);accept(state("installed"));assertTrue((Boolean)get("rendererReadyForPreparation"));assertFalse((Boolean)get("preparationLaunching"));assertFalse(WorkspaceDesktopGpu.installed(a,"debian"));
   org.robolectric.util.ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",28);visible[0]=true;Method prepare=WorkspaceDesktop.class.getDeclaredMethod("prepareSelectedRenderer",WorkspaceStore.Session.class);prepare.setAccessible(true);prepare.invoke(ui,p);AlertDialog prompt=ShadowAlertDialog.getLatestAlertDialog();assertTrue(prompt.isShowing());assertEquals("turnip",get("preparationRenderer"));prompt.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse((Boolean)get("preparationLaunching"));assertTrue((Boolean)get("rendererPreparationFailed"));assertEquals("turnip",WorkspaceDesktopGpu.mode(a,"debian"));
  }finally{org.robolectric.util.ReflectionHelpers.setStaticField(Build.VERSION.class,"SDK_INT",28);}
 }

 @Test public void failedInstallShowsPackageCauseWithoutOfferingInstallAgain()throws Exception{
  set("launchCheckAcknowledged",true);set("preparationRenderer","turnip");Bundle b=state("error");b.putString("status","Desktop installation failed");b.putString("detail","Repositories loaded\nNo match for argument: xorg-x11-utils\nDESKTOP_CHILD_EXIT=1\n");accept(b);invoke("preparationUpdate");
  assertFalse((Boolean)get("preparationNeedsInstall"));assertNull(get("preparationPrompt"));assertNull(get("launchAfterCheck"));assertFalse((Boolean)get("preparationLaunching"));assertEquals("No match for argument: xorg-x11-utils",((TextView)view.findViewWithTag("x11-preparation-detail")).getText().toString());
  ((Button)view.findViewWithTag("x11-preparation-cancel")).performClick();assertNull(get("preparationProfile"));
 }

}
