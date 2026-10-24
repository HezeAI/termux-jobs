package com.termux.app.workspace;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class WorkspacePersistenceTest {
 private Context context;private File file;private List<String> errors;
 @Before public void setup(){context=RuntimeEnvironment.application;file=new File(context.getFilesDir(),"workspace-v1.json");file.delete();new File(file+".bak").delete();errors=new ArrayList<>();}
 static class GateFile extends AtomicFile {
  volatile boolean gate,broken;final AtomicInteger calls=new AtomicInteger();final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),second=new CountDownLatch(1),firstDone=new CountDownLatch(1),secondDone=new CountDownLatch(1);final ThreadLocal<Integer> order=new ThreadLocal<>();
  GateFile(File file){super(file);}
  @Override public FileOutputStream startWrite()throws IOException{
   if(gate){int n=calls.incrementAndGet();order.set(n);if(n==1){entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IOException("fixture timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}}else second.countDown();}
   FileOutputStream out=super.startWrite();return broken?new PartialOutput(out):out;
  }
  @Override public void finishWrite(FileOutputStream out){super.finishWrite(out);Integer n=order.get();if(n!=null){if(n==1)firstDone.countDown();if(n==2)secondDone.countDown();}order.remove();}
 }
 static class PartialOutput extends FileOutputStream {
  final FileOutputStream original;
  PartialOutput(FileOutputStream out)throws IOException{super(out.getFD());original=out;}
  @Override public void write(byte[] bytes)throws IOException{original.write(bytes,0,Math.min(7,bytes.length));throw new IOException("fixture disk full");}
  @Override public void close()throws IOException{original.close();}
 }
 @Test public void recreationUsesPendingSnapshotAndSerializesOldAndNewWriters()throws Exception{
  GateFile storage=new GateFile(file);WorkspaceStore first=new WorkspaceStore(context,errors::add,storage),second=null,read=null;
  try{
   WorkspaceStore.Command c=new WorkspaceStore.Command();c.name="initial";first.commands.add(c);first.save();first.awaitWrites();
   storage.gate=true;c.name="pending";first.save();assertTrue(storage.entered.await(3,TimeUnit.SECONDS));first.close();
   second=new WorkspaceStore(context,errors::add,storage);String seen=second.commands.get(0).name;second.commands.get(0).name="newest";second.save();boolean overlap=storage.second.await(300,TimeUnit.MILLISECONDS);
   storage.release.countDown();assertTrue(storage.firstDone.await(3,TimeUnit.SECONDS));assertTrue(storage.secondDone.await(3,TimeUnit.SECONDS));second.awaitWrites();second.close();
   read=new WorkspaceStore(context,errors::add);assertFalse("AtomicFile must not have concurrent writers across recreation",overlap);assertEquals("pending",seen);assertEquals("newest",read.commands.get(0).name);
  }finally{storage.release.countDown();first.close();if(second!=null)second.close();if(read!=null)read.close();}
 }
 @Test public void closedStoreRejectsLateSaveWithoutCrashingOrOverwriting()throws Exception{
  WorkspaceStore store=new WorkspaceStore(context,errors::add);WorkspaceStore.Command c=new WorkspaceStore.Command();c.name="keep";store.commands.add(c);store.save();store.awaitWrites();store.close();byte[] before=Files.readAllBytes(file.toPath());
  c.name="stale callback";store.save();assertFalse(store.isWritable());assertArrayEquals(before,Files.readAllBytes(file.toPath()));
 }
 @Test public void completionAlreadyQueuedDoesNotInvokeClosedActivityCallback()throws Exception{
  WorkspaceStore store=new WorkspaceStore(context,errors::add);AtomicInteger callbacks=new AtomicInteger();store.save(callbacks::incrementAndGet);store.awaitWrites();store.close();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,callbacks.get());assertTrue(file.isFile());
 }
 @Test public void partialWriteFailureRetainsCommittedDataAndAllowsRetry()throws Exception{
  GateFile storage=new GateFile(file);WorkspaceStore store=new WorkspaceStore(context,errors::add,storage);try{
   WorkspaceStore.Command c=new WorkspaceStore.Command();c.name="committed";store.commands.add(c);store.save();store.awaitWrites();byte[] good=Files.readAllBytes(file.toPath());
   storage.broken=true;c.name="unfinished";store.save();store.awaitWrites();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(1,errors.size());assertArrayEquals(good,Files.readAllBytes(file.toPath()));
   storage.broken=false;c.name="retry";store.save();store.awaitWrites();WorkspaceStore check=new WorkspaceStore(context,errors::add);try{assertEquals("retry",check.commands.get(0).name);}finally{check.close();}
  }finally{store.close();}
 }
 @Test public void duplicateIdsCannotOverwriteAValidCommittedSnapshot()throws Exception{
  WorkspaceStore store=new WorkspaceStore(context,errors::add);try{
   store.commands.add(new WorkspaceStore.Command());store.sessions.add(new WorkspaceStore.Session());store.notes.add(new WorkspaceStore.Note());store.save();store.awaitWrites();byte[] good=Files.readAllBytes(file.toPath());
   int n=0;for(List list:new List[]{store.commands,store.sessions,store.notes}){list.add(list.get(0));store.save();store.awaitWrites();assertEquals(++n,errors.size());assertArrayEquals(good,Files.readAllBytes(file.toPath()));list.remove(list.size()-1);}
  }finally{store.close();}
 }
 @Test public void sharedWriterIsReleasedOnlyAfterLastOwnerAndPendingWrite()throws Exception{
  GateFile storage=new GateFile(file);WorkspacePersistence first=new WorkspacePersistence(storage,()->fail("fixture write failed")),second=new WorkspacePersistence(storage,()->fail("fixture write failed"));
  try{storage.gate=true;AtomicInteger finished=new AtomicInteger();first.write("fixture".getBytes(StandardCharsets.UTF_8),finished::incrementAndGet);assertTrue(storage.entered.await(3,TimeUnit.SECONDS));first.close();first.close();
   java.lang.reflect.Field registry=WorkspacePersistence.class.getDeclaredField("FILES");registry.setAccessible(true);Map entries=(Map)registry.get(null);String key=file.getCanonicalPath();synchronized(entries){assertTrue(entries.containsKey(key));}
   second.close();synchronized(entries){assertTrue(entries.containsKey(key));}storage.release.countDown();first.awaitWrites();assertEquals(1,finished.get());synchronized(entries){assertFalse(entries.containsKey(key));}
  }finally{storage.release.countDown();first.close();second.close();}
 }
 @Test public void writeFailureAfterRecreationWarnsCurrentOwnerAndRetainsCommittedFile()throws Exception{
  GateFile storage=new GateFile(file);List<String> currentErrors=new ArrayList<>();WorkspaceStore first=new WorkspaceStore(context,errors::add,storage),second=null;
  try{WorkspaceStore.Command c=new WorkspaceStore.Command();c.name="committed";first.commands.add(c);first.save();first.awaitWrites();byte[] good=Files.readAllBytes(file.toPath());
   storage.gate=true;c.name="pending";first.save();assertTrue(storage.entered.await(3,TimeUnit.SECONDS));first.close();second=new WorkspaceStore(context,currentErrors::add,storage);assertEquals("pending",second.commands.get(0).name);
   storage.broken=true;storage.release.countDown();first.awaitWrites();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0,errors.size());assertEquals(1,currentErrors.size());assertArrayEquals(good,Files.readAllBytes(file.toPath()));
  }finally{storage.release.countDown();first.close();if(second!=null)second.close();}
 }
 private void duplicate(String key)throws Exception{
  WorkspaceStore seed=new WorkspaceStore(context,errors::add);JSONObject data;try{seed.commands.add(new WorkspaceStore.Command());seed.notes.add(new WorkspaceStore.Note());seed.sessions.add(new WorkspaceStore.Session());data=seed.encode();}finally{seed.close();}
  JSONArray items=data.getJSONArray(key);items.put(new JSONObject(items.getJSONObject(0).toString()));byte[] bad=data.toString().getBytes(StandardCharsets.UTF_8);Files.write(file.toPath(),bad);
  WorkspaceStore store=new WorkspaceStore(context,errors::add);try{assertFalse("duplicate "+key+" IDs must not be admitted",store.isWritable());store.save();store.awaitWrites();assertArrayEquals(bad,Files.readAllBytes(file.toPath()));}finally{store.close();}
 }
 @Test public void duplicateSessionIdsLeaveLibraryReadOnlyAndIntact()throws Exception{duplicate("sessions");}
 @Test public void duplicateCommandIdsLeaveLibraryReadOnlyAndIntact()throws Exception{duplicate("commands");}
 @Test public void duplicateNoteIdsLeaveLibraryReadOnlyAndIntact()throws Exception{duplicate("notes");}
}
