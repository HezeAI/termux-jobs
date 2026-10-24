package com.termux.app.workspace;
import android.app.*;import android.widget.*;import com.termux.R;import com.termux.app.TermuxActivity;
import java.util.*;import java.util.concurrent.*;import java.lang.reflect.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class) @LooperMode(LooperMode.Mode.PAUSED)
public class WorkspaceFileChoiceOwnershipTest {
 static class Queue extends AbstractExecutorService {final List<Runnable> jobs=new ArrayList<>();boolean closed;public void execute(Runnable r){if(closed)throw new RejectedExecutionException();jobs.add(r);}public void shutdown(){closed=true;}public List<Runnable> shutdownNow(){closed=true;return new ArrayList<>(jobs);}public boolean isShutdown(){return closed;}public boolean isTerminated(){return closed;}public boolean awaitTermination(long n,TimeUnit u){return closed;}}
 static class Files implements WorkspaceFileBrowser.TransferBackend {int transfers;boolean fail;boolean lastMove;String destination;List<ProjectFiles.Entry> selection;public boolean ready(List<ProjectFiles.Entry> e,String f,boolean m,boolean r){return true;}public void run(List<ProjectFiles.Entry> e,String f,boolean m,boolean r)throws Exception{transfers++;lastMove=m;destination=f;selection=new ArrayList<>(e);if(fail)throw new ProjectFiles.TransferFailure(0,new java.io.IOException("fixture-disconnected"));}public ProjectFiles.Listing list(String p){ProjectFiles.Listing l=new ProjectFiles.Listing();l.path=p;return l;}}
 WorkspaceFileBrowser browser;Queue queue;Files files;TermuxActivity activity;
 static Field field(String name)throws Exception{Field f=WorkspaceFileBrowser.class.getDeclaredField(name);f.setAccessible(true);return f;}
 Object get(String n)throws Exception{return field(n).get(browser);}void set(String n,Object o)throws Exception{field(n).set(browser,o);}
 void call(String method)throws Exception{Method m=WorkspaceFileBrowser.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(browser);}
 void idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
 void click(AlertDialog d){d.getButton(-1).performClick();idle();}
 @Before public void setup()throws Exception{
  activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(activity,"en");WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";
  browser=new WorkspaceFileBrowser(activity,p,"/fixture",null);((ExecutorService)get("worker")).shutdown();queue=new Queue();set("worker",queue);files=new Files();set("transferBackend",files);set("busy",true);browser.show();idle();set("busy",false);
  ProjectFiles.Listing l=new ProjectFiles.Listing();l.path="/fixture";l.entries.add(new ProjectFiles.Entry("/fixture","file.txt",false,1));set("listing",l);call("render");((LinearLayout)get("rows")).getChildAt(0).performLongClick();
 }
 @After public void cleanup(){browser.close();idle();}
 AlertDialog menu()throws Exception{call("transferMenu");return ShadowAlertDialog.getLatestAlertDialog();}
 AlertDialog confirm()throws Exception{click(menu());assertEquals(1,queue.jobs.size());queue.jobs.remove(0).run();idle();return ShadowAlertDialog.getLatestAlertDialog();}
 @Test public void dismissedMenuCannotQueuePreflight()throws Exception{AlertDialog old=menu();old.dismiss();idle();click(old);assertTrue(queue.jobs.isEmpty());}
 @Test public void replacedMenuCannotQueuePreflight()throws Exception{AlertDialog old=menu();AlertDialog current=menu();click(old);assertTrue(queue.jobs.isEmpty());click(current);assertEquals(1,queue.jobs.size());}
 @Test public void clearedSelectionInvalidatesOldMenu()throws Exception{AlertDialog old=menu();((android.view.View)get("clearTransferButton")).performClick();click(old);assertTrue(queue.jobs.isEmpty());}
 @Test public void dismissedConfirmationCannotQueueTransfer()throws Exception{AlertDialog old=confirm();old.dismiss();idle();click(old);assertTrue(queue.jobs.isEmpty());assertEquals(0,files.transfers);}
 @Test public void acceptedConfirmationRunsOnceAndUsesCapturedSelection()throws Exception{AlertDialog current=confirm();click(current);assertEquals(1,queue.jobs.size());queue.jobs.remove(0).run();idle();assertEquals(1,files.transfers);click(current);assertTrue(queue.jobs.isEmpty());}
 @Test public void moveKeepsItsCapturedDestinationAndEntries()throws Exception{
  AlertDialog mode=menu();mode.getButton(-3).performClick();idle();queue.jobs.remove(0).run();idle();AlertDialog confirm=ShadowAlertDialog.getLatestAlertDialog();click(confirm);queue.jobs.remove(0).run();idle();
  assertEquals(1,files.transfers);assertTrue(files.lastMove);assertEquals("/fixture",files.destination);assertEquals("/fixture/file.txt",files.selection.get(0).path);
 }
 @Test public void closeDuringPreflightRejectsItsLateConfirmation()throws Exception{
  click(menu());Runnable inflight=queue.jobs.remove(0);browser.close();idle();inflight.run();idle();assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());assertEquals(0,files.transfers);
 }

 @Test public void unconfirmedOperationExplainsUncertaintyAndDoesNotRequeue()throws Exception{
  files.fail=true;AlertDialog current=confirm();click(current);assertEquals(1,queue.jobs.size());queue.jobs.remove(0).run();idle();
  String status=((TextView)get("message")).getText().toString();assertTrue(status.contains("completion is unconfirmed"));assertTrue(status.contains("Acknowledged entries: 0 / 1"));assertTrue(status.contains("may still be running"));assertTrue(status.contains("no automatic retry"));assertFalse(status.contains("Completed entries"));assertEquals(1,files.transfers);assertTrue(queue.jobs.isEmpty());assertFalse((Boolean)get("busy"));
  click(current);assertTrue(queue.jobs.isEmpty());assertEquals(1,files.transfers);
 }

}
