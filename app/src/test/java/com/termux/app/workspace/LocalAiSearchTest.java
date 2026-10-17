package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiSearchTest {
    static JSONObject search()throws Exception{return LocalAiTools.request("{\"tool\":\"search\",\"path\":\"/sdcard/Download\",\"query\":\"факторио\",\"filter\":\"apk\"}");}
    static JSONArray files()throws Exception{return new JSONArray().put(new JSONObject().put("name","Factorio_v2_mod.apk").put("path","/sdcard/Download/Factorio_v2_mod.apk").put("type","file").put("size",12).put("modifiedMillis",1000)).put(new JSONObject().put("name","Factorio.apk").put("path","/sdcard/Download/Factorio.apk").put("type","file").put("size",24).put("modifiedMillis",2000));}
    @Test public void namesMatchCaseSuffixAndRussianLatinSpellingWithoutRewritingPaths()throws Exception{
        assertEquals("name_variant",LocalAiNames.match("Factorio_v2_mod.apk",search()));assertEquals("name_variant",LocalAiNames.match("Telegram.apk",new JSONObject().put("tool","search").put("query","телеграмм")));assertEquals("name",LocalAiNames.match("FACTORIO_mod.apk",new JSONObject().put("tool","search").put("query","factorio")));assertEquals("",LocalAiNames.match("Other.apk",search()));assertEquals("",LocalAiNames.match("Factorio.apk",new JSONObject().put("tool","screenshots").put("query","факторио")));
    }
    @Test public void knownAlternateNamesAreExplicitAndSchemaValidated()throws Exception{
        JSONObject r=LocalAiTools.request("{\"tool\":\"search\",\"path\":\".\",\"query\":\"заметки\",\"variants\":[\"notes\"]}");assertEquals("name",LocalAiNames.match("notes-release.txt",r));for(String wrong:new String[]{"[3]","[\"\"]","\"notes\""})try{LocalAiTools.request("{\"tool\":\"search\",\"path\":\".\",\"variants\":"+wrong+"}");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void choicesAreOnlyFilesAndSelectedPathMustBeExactCandidate()throws Exception{
        JSONArray source=files();source.put(new JSONObject().put("type","directory").put("path","/sdcard/Download/folder"));source.put(new JSONObject().put("type","file").put("path","relative"));String result="exit=0\n"+new JSONObject().put("entries",source);JSONArray candidates=LocalAiSearchResult.candidates(search(),result);assertEquals(2,candidates.length());assertTrue(LocalAiSearchResult.needsChoice(candidates));String selected=LocalAiSearchResult.selected(candidates,candidates.getJSONObject(1).getString("path"));assertTrue(selected.contains("USER_SELECTED_FILE"));assertEquals(1,LocalAiSearchResult.parse(selected).getJSONArray("entries").length());try{LocalAiSearchResult.selected(candidates,"/outside/forged");fail();}catch(SecurityException expected){}assertEquals(0,LocalAiSearchResult.candidates(search(),"exit=2\nACCESS_DENIED").length());
    }
    @Test public void evenOneApproximateMatchNeedsChoiceButSingleExactAndScreenshotDoNot()throws Exception{
        JSONArray one=new JSONArray().put(files().getJSONObject(0));assertFalse(LocalAiSearchResult.needsChoice(one));one.getJSONObject(0).put("match","name_variant");assertTrue(LocalAiSearchResult.needsChoice(one));assertEquals(0,LocalAiSearchResult.candidates(new JSONObject().put("tool","screenshots"),"exit=0\n"+new JSONObject().put("entries",files())).length());
    }
    static final class Events implements LocalAiAgent.Events {
        String selected;boolean cancel;int choices;public void save(){}public void phase(String p,String d){}public String tr(String ru,String en){return en;}public String chooseFile(JSONObject request,JSONArray candidates){choices++;return cancel?null:selected;}
    }
    @Test public void coordinatorWaitsForUserSelectionAndOnlySelectedFileReachesNextGeneration()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";p.localCheckPaths=false;JSONArray history=new JSONArray().put(new JSONObject().put("role","user").put("text","найди апк факторио"));Events events=new Events();events.selected=files().getJSONObject(1).getString("path");int[] generations={0},runs={0};String answer=LocalAiAgent.run(p,history,new AtomicBoolean(),(messages,system,out)->{if(generations[0]++==0)return new LocalAiAgent.Generation(search().toString(),"eos");String selected=messages.getJSONObject(messages.length()-1).getString("text");assertTrue(selected.contains("USER_SELECTED_FILE"));assertTrue(selected.contains("Factorio.apk"));assertFalse(selected.contains("Factorio_v2_mod.apk"));return new LocalAiAgent.Generation("Selected","eos");},r->{runs[0]++;return "exit=0\n"+new JSONObject().put("entries",files());},r->true,events,t->{});assertEquals("Selected",answer);assertEquals(1,events.choices);assertEquals(1,runs[0]);assertEquals(2,generations[0]);
    }
    @Test public void cancellingChoiceDoesNotContinueModelOrExecuteAnyFileAction()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";JSONArray history=new JSONArray().put(new JSONObject().put("role","user").put("text","find files"));Events events=new Events();events.cancel=true;int[] generations={0},runs={0};String answer=LocalAiAgent.run(p,history,new AtomicBoolean(),(messages,system,out)->{generations[0]++;return new LocalAiAgent.Generation(search().toString(),"eos");},r->{runs[0]++;return "exit=0\n"+new JSONObject().put("entries",files());},r->true,events,t->{});assertTrue(answer.contains("cancelled"));assertEquals(1,generations[0]);assertEquals(1,runs[0]);
    }
    @Test public void chooserUiEventsAreNotReplayedAsModelInstructions()throws Exception{JSONArray h=new JSONArray().put(new JSONObject().put("role","assistant").put("text","choice UI").put("fileChoice",files()));assertEquals(0,LocalAiAgent.context(h).length());}
}
