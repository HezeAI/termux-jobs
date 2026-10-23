package com.termux.app.workspace;
import android.app.*;import com.termux.R;import com.termux.app.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowAlertDialog;import java.io.*;import java.lang.reflect.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceSessionLifecycleTest {
 private TermuxActivity activity()throws Exception{TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field f=TermuxActivity.class.getDeclaredField("mTerminalView");f.setAccessible(true);f.set(a,a.findViewById(R.id.terminal_view));return a;}
 private WorkspaceStore store(WorkspaceController c)throws Exception{Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);return (WorkspaceStore)f.get(c);}
 @Test public void startupRetiresOldUnstartedUnsavedRecordWithoutDeletingHistoryOrPresets()throws Exception{
  TermuxActivity a=activity();WorkspaceStore seed=new WorkspaceStore(a,x->fail(x));seed.sessions.clear();
  WorkspaceStore.Session ghost=AgentEnvironmentsTest.profile("codex");ghost.name="Codex";ghost.keepHistory=false;seed.sessions.add(ghost);
  WorkspaceStore.Session saved=ghost.copy();saved.id=java.util.UUID.randomUUID().toString();saved.saved=true;seed.sessions.add(saved);
  WorkspaceStore.Session base=new WorkspaceStore.Session();base.id=WorkspaceStore.BASE_TERMINAL_ID;seed.sessions.add(base);seed.save();seed.awaitWrites();seed.close();
  File history=new File(a.getFilesDir(),"workspace-sessions/"+ghost.id+"/transcript.txt");assertTrue(history.getParentFile().mkdirs());try(FileWriter w=new FileWriter(history)){w.write("keep history");}
  WorkspaceController c=new WorkspaceController(a);try{
   assertEquals(3,store(c).sessions.size());c.refresh();assertEquals("do not infer lifecycle before service binds",3,store(c).sessions.size());
   Field service=TermuxActivity.class.getDeclaredField("mTermuxService");service.setAccessible(true);service.set(a,new TermuxService());c.refresh();
   assertEquals(2,store(c).sessions.size());assertTrue(history.isFile());assertTrue(store(c).sessions.stream().anyMatch(p->p.id.equals(saved.id)));assertTrue(store(c).sessions.stream().anyMatch(WorkspaceStore::isBase));
   // A draft created after startup must not be mistaken for the old orphan.
   WorkspaceStore.Session later=new WorkspaceStore.Session();store(c).sessions.add(later);c.refresh();assertTrue(store(c).sessions.contains(later));
  }finally{c.close();}
 }
 @Test public void createAndLaunchBeforeServiceReadyDoesNotPersistAnInvisibleDraft()throws Exception{
  TermuxActivity a=activity();WorkspaceController c=new WorkspaceController(a);try{
   store(c).sessions.clear();Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(c,null,true);
   AlertDialog form=ShadowAlertDialog.getLatestAlertDialog();form.getButton(AlertDialog.BUTTON_POSITIVE).performClick();store(c).awaitWrites();assertTrue(store(c).sessions.isEmpty());
   WorkspaceStore disk=new WorkspaceStore(a,x->fail(x));try{assertTrue(disk.sessions.isEmpty());}finally{disk.close();}
  }finally{c.close();}
 }
}
