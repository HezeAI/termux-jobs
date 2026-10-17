package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiCompletionTest {
 JSONObject call(String name)throws Exception{return new JSONObject().put("tool",name).put("path","/sdcard/Download/test.txt").put("text","Test");}
 @Test public void foldersAndPastHistoryAreNotCurrentWriteReadEvidence()throws Exception{
  LocalAiCompletion c=new LocalAiCompletion();c.record(call("locations"),"exit=0\nfolders");String m=c.missing("Файл успешно сохранён и прочитан.","Сохрани /sdcard/Download/test.txt и прочитай обратно",false);assertEquals("write,read",m);
  c.record(call("write"),"exit=0\nSAVED");assertEquals("read",c.missing("Файл сохранён и прочитан.","write/read file",false));c.record(call("read"),"exit=0\nTest");assertEquals("",c.missing("Файл сохранён и прочитан.","write/read file",false));
 }
 @Test public void failedResultsAndEmptyWriteNeverProveDeletion()throws Exception{
  LocalAiCompletion c=new LocalAiCompletion();c.record(call("read"),"Tool failed: denied");assertEquals("read",c.missing("I have read the file.","Read /a.txt",false));c.record(call("write"),"exit=0\n");assertEquals("delete",c.missing("Файл удалён.","Удали /a.txt",false));
 }
 @Test public void questionsExamplesNegationAndTextOnlyWritingDoNotRequireFileTools()throws Exception{
  LocalAiCompletion c=new LocalAiCompletion();for(String answer:new String[]{"Файл не сохранён.","I have not read the file.","Теперь я прочитаю файл.","Use write to save a file.","Если файл создан, прочитайте его.","Нужна проверка файла перед удалением.","Для создания файла нужен доступ."})assertEquals(answer,"",c.missing(answer,"Read /a.txt",false));assertEquals("",c.missing("I saved the file.","show an example",true));assertEquals("",c.missing("Я создал рассказ.","Напиши рассказ",false));assertEquals("",c.missing("Read the earlier question and answer","Read history",false));assertEquals("",c.missing("Файл создан.","Прочитай файл и скажи, существует ли он",false));
 }
 @Test public void genericCompletionChecksExplicitFileTaskAndOpaqueCommandIsNotMisclassified()throws Exception{
  LocalAiCompletion c=new LocalAiCompletion();assertEquals("write,read",c.missing("Done.","Write /a.txt and read it",false));c.record(call("command"),"exit=0\n");assertEquals("",c.missing("I saved the file.","write /a.txt",false));
 }
 LocalAiAgentTest.Fixture fixture()throws Exception{LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.history=new JSONArray().put(new JSONObject().put("role","assistant").put("text","Saved earlier").put("toolState","done").put("toolRequest",call("write"))).put(new JSONObject().put("role","user").put("text","Сохрани /sdcard/Download/test.txt и прочитай обратно"));return f;}
 @Test public void oneReminderLetsModelChooseActualWriteReadInBothNativeProtocols()throws Exception{
  for(boolean qwen:new boolean[]{false,true}){LocalAiAgentTest.Fixture f=fixture();f.p.localProtocol=qwen?"qwen-native":"litert-native";String[] stages={"Файл сохранён и прочитан.",qwen?LocalAiQwenProtocol.call(call("write")):call("write").toString(),qwen?LocalAiQwenProtocol.call(new JSONObject().put("tool","read").put("path","/sdcard/Download/test.txt")):new JSONObject().put("tool","read").put("path","/sdcard/Download/test.txt").toString(),"Файл сохранён и прочитан."};int[] i={0};String answer=LocalAiAgent.run(f.p,f.history,f.cancelled,(h,s,o)->{int n=i[0]++;if(n==1)assertTrue(s.contains("COMPLETION_EVIDENCE_MISSING"));return new LocalAiAgent.Generation(stages[n],!qwen&&(n==1||n==2)?"native_tool":"eos");},r->{f.calls.add(r.toString());return "exit=0\nTest";},r->true,f,t->{});assertEquals("Файл сохранён и прочитан.",answer);assertEquals(4,i[0]);assertEquals(2,f.calls.size());assertTrue(f.history.getJSONObject(2).getBoolean("repair"));}
 }
 @Test public void repeatedUnsupportedClaimStopsWithoutInventingActions()throws Exception{
  LocalAiAgentTest.Fixture f=fixture();f.answers.add("Файл сохранён и прочитан.");f.answers.add("Файл сохранён и прочитан.");assertTrue(f.run().contains("Completion not confirmed"));assertEquals(2,f.systems.size());assertTrue(f.calls.isEmpty());
 }
 @Test public void onlyLocationsDoesNotMakeWriteReadClaimTrue()throws Exception{
  LocalAiAgentTest.Fixture f=fixture();f.answers.add("{\"tool\":\"locations\"}");f.answers.add("Файл сохранён и прочитан.");f.answers.add("Файл сохранён и прочитан.");assertTrue(f.run().contains("Completion not confirmed"));assertEquals(1,f.calls.size());assertEquals("locations",new JSONObject(f.calls.get(0)).getString("tool"));
 }
 @Test public void reminderCannotGrantWriteToReadOnlyOrSkipApprovalOrCancellation()throws Exception{
  LocalAiAgentTest.Fixture f=fixture();f.p.localAccess="read";f.answers.add("Файл сохранён.");f.answers.add(call("write").toString());try{f.run();fail();}catch(SecurityException expected){}assertTrue(f.calls.isEmpty());
  f=fixture();f.p.localConfirmActions=true;f.allow=false;f.answers.add("Файл сохранён.");f.answers.add(call("write").toString());assertTrue(f.run().contains("rejected"));assertEquals(1,f.approvals);assertTrue(f.calls.isEmpty());
  LocalAiAgentTest.Fixture c=fixture();int[] n={0};try{LocalAiAgent.run(c.p,c.history,c.cancelled,(h,s,o)->{if(n[0]++==1)c.cancelled.set(true);return new LocalAiAgent.Generation("Файл сохранён.","eos");},r->{fail();return "";},r->true,c,t->{});fail();}catch(java.util.concurrent.CancellationException expected){}assertEquals(2,n[0]);
 }
 @Test public void chatOnlyNeverRequestsForbiddenActionAndDenialsMayFinish()throws Exception{
  LocalAiAgentTest.Fixture f=fixture();f.p.localAccess="chat";f.answers.add("Файл сохранён.");assertTrue(f.run().contains("Completion not confirmed"));assertEquals(1,f.systems.size());assertTrue(f.calls.isEmpty());
  f=fixture();f.answers.add("Не могу сохранить файл: доступ ограничен.");f.answers.add("Сохранение не выполнено.");assertTrue(f.run().contains("не выполнено"));assertTrue(f.calls.isEmpty());
 }
}
