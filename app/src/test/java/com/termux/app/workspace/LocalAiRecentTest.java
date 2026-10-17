package com.termux.app.workspace;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiRecentTest {
    static JSONArray h(String text)throws Exception{return new JSONArray().put(new JSONObject().put("role","user").put("text",text));}
    @Test public void understandsShortRussianEnglishTypesAndFolders()throws Exception{
        LocalAiRecent r=LocalAiRecent.parse(h("хорошо посмотри последний тхт в довнолоад"));assertNotNull(r);assertEquals("txt",r.extension);assertTrue(r.read);assertEquals("Download",r.folder);
        assertEquals("Documents",LocalAiRecent.parse(h("please read the latest markdown file in documents")).folder);
        assertEquals("apk",LocalAiRecent.parse(h("find latest apk in downloads")).extension);
        assertFalse(LocalAiRecent.parse(h("найди последний txt в загрузках")).read);
        assertEquals("image",LocalAiRecent.parse(h("посмотри последнее фото в картинках")).filter);
    }
    @Test public void doesNotRewriteQuestionsNegationsNamedPathsOrCompoundTasks()throws Exception{
        for(String text:new String[]{"как найти последний txt в загрузках?","не открывай последний txt в загрузках","прочитай /root/last.txt","прочитай последний txt в загрузках и удали его","найди Factorio.apk","найди последний txt в неизвестной папке"})assertNull(text,LocalAiRecent.parse(h(text)));
    }
    @Test public void selectsActualAvailableAliasButDoesNotUseWrongFolder()throws Exception{
        LocalAiRecent r=LocalAiRecent.parse(h("read latest txt in downloads"));JSONObject list=r.listing("exit=0\nAVAILABLE\t/sdcard\nNOT_FOUND\t/sdcard/Download\nAVAILABLE\t/storage/emulated/0/Downloads\n");assertEquals("/storage/emulated/0/Downloads",list.getString("path"));assertEquals(1,list.getInt("limit"));assertEquals("modified_desc",list.getString("sort"));assertEquals("txt",list.getString("extension"));assertNotNull(LocalAiTools.request(list.toString()));
        assertEquals("/sdcard/Download",r.listing("exit=0\nAVAILABLE\t/sdcard\n").getString("path"));
    }
    @Test public void exactExtensionFiltersBeforeNewestLimitAndExcludesDirectories()throws Exception{
        JSONObject request=new JSONObject().put("tool","list").put("path","/sdcard/Download").put("extension","txt").put("limit",1);
        String raw="900\0009\000f\000/sdcard/Download/new.md\000800\0000\000d\000/sdcard/Download/folder.txt\00030\0003\000f\000/sdcard/Download/last.TXT\00010\0001\000f\000/sdcard/Download/old.txt\000";
        JSONObject result=new JSONObject(LocalAiDirectory.listing(raw.getBytes(StandardCharsets.UTF_8),request));assertEquals(2,result.getInt("matched"));assertEquals("/sdcard/Download/last.TXT",result.getJSONArray("entries").getJSONObject(0).getString("path"));assertEquals("/sdcard/Download",result.getString("checkedPath"));
    }
    @Test public void schemaRejectsExtensionInjection()throws Exception{
        for(String ext:new String[]{"../txt","txt;id","",".txt"})try{LocalAiTools.request(new JSONObject().put("tool","list").put("path",".").put("extension",ext).toString());fail(ext);}catch(IllegalArgumentException expected){}
    }
    static String found()throws Exception{return "exit=0\n"+new JSONObject().put("entries",new JSONArray().put(new JSONObject().put("type","file").put("path","/sdcard/Download/new.txt")));}
    @Test public void coordinatorDiscoversListsAndReadsBeforeModelAnswer()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";p.localCheckPaths=false;int[] runs={0},generations={0};
        LocalAiAgent.run(p,h("прочитай последний тхт в довнолоад"),new AtomicBoolean(),(messages,system,out)->{generations[0]++;assertTrue(messages.getJSONObject(messages.length()-1).getString("text").contains("actual text"));return new LocalAiAgent.Generation("done","eos");},r->{switch(runs[0]++){case 0:assertEquals("locations",r.getString("tool"));return "exit=0\nAVAILABLE\t/sdcard/Download\n";case 1:assertEquals("/sdcard/Download",r.getString("path"));return found();case 2:assertEquals("read",r.getString("tool"));assertEquals("/sdcard/Download/new.txt",r.getString("path"));return "exit=0\nactual text";default:throw new AssertionError();}},r->{throw new AssertionError();},new LocalAiSearchTest.Events(),t->{});
        assertEquals(3,runs[0]);assertEquals(0,generations[0]);
    }
    @Test public void emptyOrDeniedResultsNeverTriggerReadAndBinaryIsNotAutoRead()throws Exception{
        LocalAiRecent r=LocalAiRecent.parse(h("read latest txt in downloads"));assertNull(r.reading("exit=2\nACCESS_DENIED"));assertNull(r.reading("exit=0\n{\"entries\":[]}"));assertNull(LocalAiRecent.parse(h("open latest apk in downloads")).reading(found()));
    }
    @Test public void generatedDiscoveryStillUsesSelectedEnvironmentAndScopeGuards()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";p.localProject="/root/project";p.localCheckPaths=true;
        JSONObject list=LocalAiRecent.parse(h("read latest txt in downloads")).listing("exit=0\nOUTSIDE_PROJECT\t/sdcard/Download\n");
        assertTrue(LocalAiTools.script(p,list).contains("OUTSIDE_PROJECT"));assertEquals("/sdcard/Download",list.getString("path"));
        JSONObject read=new JSONObject().put("tool","read").put("path","/sdcard/Download/a.txt");assertTrue(LocalAiTools.script(p,read).contains("/proc/$$/fd/3"));
        p.localAccess="chat";try{LocalAiTools.authorize(p,list);fail();}catch(SecurityException expected){}
    }
    @Test public void chatModeDoesNotDiscoverOrRead()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="chat";
        LocalAiAgent.run(p,h("read latest txt in downloads"),new AtomicBoolean(),(messages,system,out)->new LocalAiAgent.Generation("Chat only","eos"),r->{throw new AssertionError();},r->false,new LocalAiSearchTest.Events(),t->{});
    }
    @Test public void missingFolderIsRecognizedAndClarifiedWithoutModelOrProbe()throws Exception{
        for(String text:new String[]{"покажи ласт тхт","посмотри ласт тхт на телефоне","show latest apk on my phone"}){LocalAiRecent recent=LocalAiRecent.parse(h(text));assertNotNull(text,recent);assertEquals("",recent.folder);WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";String answer=LocalAiAgent.run(p,h(text),new AtomicBoolean(),(a,s,o)->{throw new AssertionError();},r->{throw new AssertionError();},r->false,new LocalAiSearchTest.Events(),t->{});assertTrue(answer.contains("Which folder"));}
    }
}
