package com.termux.app.workspace;
import org.json.*;import org.junit.*;import static org.junit.Assert.*;
import android.app.Application;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiTurnProgressTest {
 JSONArray task(String text)throws Exception{return new JSONArray().put(new JSONObject().put("role","user").put("text",text));}
 JSONObject request(String tool,String value)throws Exception{return new JSONObject().put("tool",tool).put(tool.equals("command")?"command":"path",value);}
 @Test public void identicalCycleWarnsThenStopsBeforeExecutingAgain()throws Exception{LocalAiTurnProgress p=new LocalAiTurnProgress(task("Create, read, delete, verify"));JSONObject[] calls={request("write","/test").put("text","hello"),request("read","/test"),request("command","rm -- /test"),request("list","/")};for(int i=0;i<8;i++){assertEquals(0,p.check(calls[i%4]));p.record(calls[i%4],"exit=0\nunchanged");}assertEquals(1,p.check(calls[0]));assertEquals(2,p.check(calls[0]));assertTrue(p.note().contains("Completed actions: 8"));}
 @Test public void differentFilesChangedResultsAndOrdinaryReadChecksDoNotTrigger()throws Exception{LocalAiTurnProgress p=new LocalAiTurnProgress(task("inspect files"));for(int i=0;i<24;i++){JSONObject r=request("read","/file"+(i%2));assertEquals(0,p.check(r));p.record(r,"exit=0\ntext");}p=new LocalAiTurnProgress(task("develop"));for(int i=0;i<16;i++){JSONObject r=request(i%2==0?"command":"read",i%2==0?"build":"/test");assertEquals(0,p.check(r));p.record(r,"exit=0\nrevision "+i);}}
 @Test public void requestedFiveRepetitionsAreAllowedAndNewTurnStartsFresh()throws Exception{LocalAiTurnProgress p=new LocalAiTurnProgress(task("Повтори тест пять раз"));assertEquals(5,p.requestedRepeats);JSONObject a=request("command","build"),b=request("read","/result");for(int i=0;i<10;i++){JSONObject r=i%2==0?a:b;assertEquals(0,p.check(r));p.record(r,"exit=0\nOK");}assertEquals(1,p.check(a));assertEquals(0,new LocalAiTurnProgress(task("Do it again")).check(a));assertEquals(5,LocalAiTurnProgress.repetitions("Repeat 5 times"));}
 @Test public void keyOrderDoesNotDefeatDetectionAndNativePromptKeepsProgress()throws Exception{JSONObject a=new JSONObject().put("tool","command").put("command","pwd"),b=new JSONObject().put("command","pwd").put("tool","command");assertEquals(LocalAiTurnProgress.call(a),LocalAiTurnProgress.call(b));LocalAiTurnProgress p=new LocalAiTurnProgress(task("run"));p.record(a,"exit=0\n/root");String system="base"+p.note()+"\nCORRECTION: finish";assertTrue(LocalAiLiteRt.progress(system).contains("pwd => ok"));assertFalse(LocalAiLiteRt.progress(system).contains("CORRECTION"));assertEquals("\nCORRECTION: finish",LocalAiLiteRt.correction(system));}
 @Test public void completedProgressSurvivesRemovalOfRawToolHistory()throws Exception{LocalAiTurnProgress p=new LocalAiTurnProgress(task("Create and delete /test"));p.record(request("write","/test").put("text","hello"),"exit=0\nwritten");p.record(request("command","rm /test"),"exit=0\n");JSONArray h=task("Create and delete /test");for(int i=0;i<6;i++)h.put(new JSONObject().put("role","user").put("toolResult","read").put("text",new String(new char[800]).replace('\0','x')));String note=p.note();JSONArray compact=LocalAiContext.fit(h,note,1024,128,LocalAiContext::estimate,s->{});assertTrue(compact.length()<h.length());assertTrue(note.contains("write /test => ok"));assertTrue(note.contains("command rm /test => ok"));}

 @Test public void identicalWritesWarnDespiteChangingDirectoryMetadataAndAllowVerification()throws Exception{
  LocalAiTurnProgress p=new LocalAiTurnProgress(task("Save test and read it"));JSONObject w=request("write","/test").put("text","hello"),list=request("list","/"),read=request("read","/test");
  for(int i=0;i<2;i++){assertEquals(0,p.check(w));p.record(w,"exit=0\nSAVED");p.record(list,"exit=0\nmtime "+i);}
  assertEquals(1,p.check(w));assertEquals(0,p.check(read));p.record(read,"exit=0\nhello");assertEquals(2,p.check(w));
 }
 @Test public void afterDuplicateWriteWarningRepeatedRediscoveryStopsButFreshPathsRemainAllowed()throws Exception{
  LocalAiTurnProgress p=new LocalAiTurnProgress(task("write file"));JSONObject w=request("write","/test").put("text","hello"),list=request("list","/");p.record(w,"exit=0\nSAVED");p.record(w,"exit=0\nSAVED");assertEquals(1,p.check(w));
  for(int i=0;i<2;i++){assertEquals(0,p.check(list));p.record(list,"exit=0\nmtime="+i);}assertEquals(2,p.check(list));assertEquals(0,p.check(request("list","/other")));assertEquals(0,p.check(request("read","/test")));
 }
 @Test public void changedWritesCommandsFailuresAndFreshReadsInvalidateDuplicateAssumption()throws Exception{
  JSONObject w=request("write","/test").put("text","hello");for(String change:new String[]{"different-write","command","failure","changed-read"}){
   LocalAiTurnProgress p=new LocalAiTurnProgress(task("work"));p.record(w,"exit=0\nSAVED");p.record(w,"exit=0\nSAVED");assertEquals(1,p.check(w));
   if(change.equals("different-write"))p.record(request("write","/test").put("text","new"),"exit=0\nSAVED");else if(change.equals("command"))p.record(request("command","rm /test"),"exit=1\nmay have changed state");else if(change.equals("failure"))p.record(w,"exit=1\nfailed");else p.record(request("read","/test"),"exit=0\nexternally changed");
   assertEquals(change,0,p.check(w));
  }
 }
 @Test public void cachedReadAnnotationDoesNotResetWriteGuardAndExplicitRepeatsRemainAllowed()throws Exception{
  LocalAiTurnProgress p=new LocalAiTurnProgress(task("repeat 5 times"));JSONObject w=request("write","/test").put("text","hello");for(int i=0;i<5;i++){assertEquals(0,p.check(w));p.record(w,"exit=0\nSAVED");}assertEquals(1,p.check(w));p.record(request("read","/test"),"exit=0\nhello\n[Cached]",true);assertEquals(2,p.check(w));assertEquals(0,new LocalAiTurnProgress(task("Again")).check(w));
 }
 @Test public void newFilesAndChangedTextNeverHitDuplicateWriteGuard()throws Exception{LocalAiTurnProgress p=new LocalAiTurnProgress(task("write many files"));for(int i=0;i<20;i++){JSONObject w=request("write","/test"+i).put("text","hello");assertEquals(0,p.check(w));p.record(w,"exit=0\nSAVED");}for(int i=0;i<20;i++){JSONObject w=request("write","/test").put("text","revision"+i);assertEquals(0,p.check(w));p.record(w,"exit=0\nSAVED");}}
}
