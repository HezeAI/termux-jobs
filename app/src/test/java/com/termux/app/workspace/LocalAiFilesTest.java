package com.termux.app.workspace;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class LocalAiFilesTest {
 @Test public void formatUsesHeaderRatherThanRenamedExtension() throws Exception {
  assertEquals("llama",LocalAiFiles.engine("GGUFabcdefghijklm".getBytes(StandardCharsets.US_ASCII),"renamed.litertlm"));
  assertEquals("litert",LocalAiFiles.engine("LITERTLM0123456789".getBytes(StandardCharsets.US_ASCII),"renamed.gguf"));
  try{LocalAiFiles.engine("this is not a model".getBytes(StandardCharsets.US_ASCII),"model.litertlm");fail();}catch(IOException expected){}
 }
 @Test public void idsCannotEscapePrivateDirectory(){for(String id:new String[]{"../auth","/root","a/b","","a.json"})try{LocalAiFiles.safeId(id);fail(id);}catch(IllegalArgumentException expected){}}
 @Test public void cancellationStopsBeforeFurtherWork(){AtomicBoolean stop=new AtomicBoolean(true);try{LocalAiFiles.check(stop);fail();}catch(java.util.concurrent.CancellationException expected){}}
 @Test public void historyRoundTripsUnicodeAndRemainsSeparate() throws Exception {
  LocalAiFiles files=new LocalAiFiles(RuntimeEnvironment.application);String id="test-"+java.util.UUID.randomUUID();JSONArray h=new JSONArray().put(new JSONObject().put("role","user").put("text","Привет\n世界"));files.saveHistory(id,h);assertEquals("Привет\n世界",files.history(id).getJSONObject(0).getString("text"));assertEquals(0,files.history("other-"+id).length());
 }
 @Test public void modelDeletionKeepsChatHistory() throws Exception {
  LocalAiFiles files=new LocalAiFiles(RuntimeEnvironment.application);String id="test-"+java.util.UUID.randomUUID();JSONObject m=new JSONObject().put("id",id).put("engine","llama");try(OutputStream o=new FileOutputStream(files.modelFile(m))){o.write(new byte[]{1,2,3});}files.saveHistory(id,new JSONArray().put("retained"));files.deleteModel(m);assertFalse(files.modelFile(m).exists());assertEquals("retained",files.history(id).getString(0));
 }
 @Test public void localFieldsRoundTripAndDuplicateStartsFresh() throws Exception {
  WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,s->{});store.sessions.clear();WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="termux";s.localAi=true;s.localModel="gemma4-e2b";s.localBackend="gpu";s.localChat="chat-a";s.startup="";store.sessions.add(s);store.decode(store.encode().toString());WorkspaceStore.Session read=store.sessions.get(0);assertTrue(read.localAi);assertEquals("gemma4-e2b",read.localModel);assertEquals("gpu",read.localBackend);assertEquals("chat-a",read.localChat);assertEquals("",read.copy().localChat);assertEquals("",read.startup);store.close();
 }
 @Test public void importUsesUniquePrivateFileAndLeavesOriginalUntouched() throws Exception {
  Context c=RuntimeEnvironment.application;LocalAiFiles files=new LocalAiFiles(c);android.net.Uri uri=android.net.Uri.parse("content://local-test/model");byte[] bytes="GGUF012345678901234567890123456789".getBytes(StandardCharsets.US_ASCII);
  org.robolectric.Shadows.shadowOf(c.getContentResolver()).registerInputStream(uri,new java.io.ByteArrayInputStream(bytes));
  JSONObject m=files.importModel(uri,new AtomicBoolean(),(phase,done,total)->{});assertEquals("llama",m.getString("engine"));assertTrue(m.getBoolean("imported"));assertEquals(bytes.length,files.modelFile(m).length());assertTrue(files.modelFile(m).getCanonicalPath().startsWith(files.models.getCanonicalPath()+"/"));files.deleteModel(m);
 }
 @Test public void cancelledImportLeavesNoFinalModel() throws Exception {
  Context c=RuntimeEnvironment.application;LocalAiFiles files=new LocalAiFiles(c);int before=files.models.list().length;android.net.Uri uri=android.net.Uri.parse("content://local-test/cancel");
  org.robolectric.Shadows.shadowOf(c.getContentResolver()).registerInputStream(uri,new java.io.ByteArrayInputStream("GGUF01234567890123456789".getBytes(StandardCharsets.US_ASCII)));
  try{files.importModel(uri,new AtomicBoolean(true),(phase,done,total)->{});fail();}catch(java.util.concurrent.CancellationException expected){}assertEquals(before,files.models.list().length);
 }
 @Test public void backendSelectionDoesNotPretendCpuBuildHasGpu() throws Exception {
  JSONObject q=new JSONObject().put("engine","llama").put("backend","cpu");LocalAiEngine.validateBackend(q,"auto",RuntimeEnvironment.application);
  try{LocalAiEngine.validateBackend(q,"gpu",RuntimeEnvironment.application);fail();}catch(IllegalArgumentException expected){}
  JSONObject gemma=new JSONObject().put("engine","litert").put("backend","auto");LocalAiEngine.validateBackend(gemma,"gpu",RuntimeEnvironment.application);
  try{LocalAiEngine.validateBackend(gemma,"npu",RuntimeEnvironment.application);fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void npuCompiledModelIsNotAcceptedOnUnverifiedChip() throws Exception {
  JSONObject m=new JSONObject().put("engine","litert").put("backend","npu").put("name","gemma sm8750");
  try{LocalAiEngine.validateBackend(m,"npu",RuntimeEnvironment.application);fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void cancellationIsNotResetWhenGenerationStarts() throws Exception {
  LocalAiEngine engine=new LocalAiEngine();engine.cancel();
  try{engine.generate(new JSONArray(),32,text->{});fail();}catch(java.util.concurrent.CancellationException expected){}finally{engine.close();}
 }
 @Test public void managerImportCopiesStreamWithProgressAndVerifiesLength()throws Exception{
  LocalAiFiles files=new LocalAiFiles(RuntimeEnvironment.application);byte[] data="GGUF012345678901234567890123456789".getBytes(StandardCharsets.US_ASCII);long[] copied={0};JSONObject model=files.importModel("from-ubuntu.gguf",data.length,()->new ByteArrayInputStream(data),new AtomicBoolean(),(phase,done,total)->{copied[0]=done;assertEquals(data.length,total);});assertEquals(data.length,copied[0]);assertEquals("llama",model.getString("engine"));assertEquals(64,model.getString("sha256").length());assertEquals(data.length,files.modelFile(model).length());files.deleteModel(model);
 }
 @Test public void incompleteManagerCopyLeavesNoPartialFile()throws Exception{
  LocalAiFiles files=new LocalAiFiles(RuntimeEnvironment.application);int before=files.models.list().length;try{files.importModel("broken.gguf",100,()->new ByteArrayInputStream("GGUFshort".getBytes(StandardCharsets.US_ASCII)),new AtomicBoolean(),(phase,done,total)->{});fail();}catch(IOException expected){}assertEquals(before,files.models.list().length);
 }
}
