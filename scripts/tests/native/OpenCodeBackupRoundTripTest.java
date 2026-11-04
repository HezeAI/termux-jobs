package com.termux.app.workspace;

import android.app.Activity;
import android.util.AtomicFile;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Real CLI -> backup helper -> Java profile importer -> persisted profile -> real CLI. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=android.app.Application.class)
public class OpenCodeBackupRoundTripTest {
 public static class TestActivity extends Activity {static File files;@Override public File getFilesDir(){return files;}}
 private JSONObject request(int port,String route,JSONObject body)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:"+port+route).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(90000);c.setDoOutput(true);c.setRequestMethod("POST");try{try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}InputStream in=c.getResponseCode()<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);JSONObject r=new JSONObject(out.toString("UTF-8"));assertFalse(r.optString("error"),r.has("error"));return r;}finally{c.disconnect();}
 }
 @Test public void restoredShellReceiptDoesNotExecuteOrReuseOnce()throws Exception{check("shell");}
 @Test public void restoredEditReceiptDoesNotExecuteOrReuseOnce()throws Exception{check("write");}
 private void check(String kind)throws Exception{
  String repo=System.getenv("TERMUX_FORK_SOURCE_ROOT"),guard=System.getenv("TERMUX_FORK_AUDIT_NET_GUARD");assertNotNull(repo);assertNotNull(guard);
  File root=new File(System.getProperty("java.io.tmpdir"),"ob"+UUID.randomUUID().toString().replace("-","").substring(0,10));Files.createDirectory(root.toPath(),java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
  ProcessBuilder pb=new ProcessBuilder(guard,new File(root,"network.jsonl").getAbsolutePath(),"--","node",new File(repo,"scripts/tests/fixtures/opencode-backup-roundtrip.cjs").getAbsolutePath(),root.getAbsolutePath(),repo,kind);String path=System.getenv("PATH");pb.environment().clear();pb.environment().put("PATH",path);pb.environment().put("HOME",new File(root,"unconfigured-home").getAbsolutePath());pb.environment().put("TERMUX_FORK_TEST_UNIX_ROOT",root.getAbsolutePath());pb.redirectError(new File(root,"fixture.stderr"));Process process=pb.start();BufferedReader stdout=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8));int port=-1;Activity activity=null;WorkspaceStore store=null;
  try{
   ExecutorService reader=Executors.newSingleThreadExecutor();String ready;try{ready=reader.submit(stdout::readLine).get(20,TimeUnit.SECONDS);}finally{reader.shutdown();}assertNotNull(ready);assertTrue(ready,ready.startsWith("READY "));port=new JSONObject(ready.substring(6)).getInt("port");
   WorkspaceStore.Session source=WorkspaceStore.newProfile();source.name="Synthetic OpenCode backup";source.saved=true;source.environment="ubuntu";source.aiAgent="opencode";source.aiModel="audit/audit";source.directory=new File(root,"project").getAbsolutePath();source.aiSocketId=UUID.randomUUID().toString();String connection=source.aiConnection;
   request(port,"/seed",new JSONObject().put("profile",WorkspaceStore.sessionJson(source)).put("command",AiAgents.command(new File(root,"source"),source)));
   source.aiSavedThread=source.aiThread;source.aiChat="resume";JSONObject library=new JSONObject().put("version",8).put("commands",new JSONArray()).put("notes",new JSONArray()).put("sessions",new JSONArray().put(WorkspaceStore.sessionJson(source)));
   JSONObject report=request(port,"/export",new JSONObject().put("workspace",library)).getJSONObject("report");JSONObject restored=request(port,"/restore",new JSONObject());
   TestActivity.files=new File(root,"target");activity=Robolectric.buildActivity(TestActivity.class).setup().get();store=new WorkspaceStore(activity,x->{throw new AssertionError(x);},new AtomicFile(new File(TestActivity.files,"workspace.json")));
   WorkspaceBackupImport.Choice choice=new WorkspaceBackupImport.Choice();choice.presets=true;WorkspaceBackupImport.Prepared prepared=WorkspaceBackupImport.prepare(activity,report,restored,choice);WorkspaceStore.Session imported=prepared.presets.get(0);String id=imported.id;assertNotEquals(source.id,id);assertNotEquals(connection,imported.aiConnection);assertEquals(source.aiAccess,imported.aiAccess);assertTrue(imported.restoreReview);WorkspaceBackupImport.apply(activity,store,prepared,choice);store.awaitWrites();store.close();store=new WorkspaceStore(activity,x->{throw new AssertionError(x);},new AtomicFile(new File(TestActivity.files,"workspace.json")));WorkspaceStore.Session saved=store.sessions.stream().filter(s->s.id.equals(id)).findFirst().get();assertEquals(imported.aiConnection,saved.aiConnection);
   // Explicit review is retained; no session launches merely by restoring it.
   saved.restoreReview=false;AiAgents.prepareLaunch(saved);String command=AiAgents.command(TestActivity.files,saved);
   JSONObject result=request(port,"/continue",new JSONObject().put("profile",WorkspaceStore.sessionJson(saved)).put("originalConnection",connection).put("command",command));assertTrue(result.getBoolean("continuedResponse"));assertTrue(result.getBoolean("oldHistoryReachedProvider"));assertTrue(result.getBoolean("priorOnceNotReused"));assertTrue(result.getBoolean("receiptsVisibleWithoutExecution"));
  }finally{if(store!=null)store.close();if(activity!=null)activity.finish();if(port>0)try{request(port,"/shutdown",new JSONObject());}catch(Exception ignored){}if(!process.waitFor(15,TimeUnit.SECONDS)){process.destroy();assertTrue(process.waitFor(15,TimeUnit.SECONDS));}stdout.close();}
 }
}
