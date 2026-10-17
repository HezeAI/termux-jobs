package com.termux.app.workspace;

import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.app.Application;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiScreenshotsTest {
    static JSONObject request(String text)throws Exception{return LocalAiTools.request(text);}
    @Test public void schemaAndPermissionsAreSharedAcrossLocalModels()throws Exception{
        JSONObject r=request("{\"tool\":\"screenshots\",\"limit\":3}");
        for(String model:new String[]{"qwen35-2b","gemma4-e2b","gemma4-e4b","import-custom"}){WorkspaceStore.Session p=new WorkspaceStore.Session();p.localModel=model;p.localAccess="read";LocalAiTools.authorize(p,r);assertTrue(LocalAiTools.instructions(p).contains("screenshots"));}
        try{LocalAiTools.authorize("chat",r);fail();}catch(SecurityException expected){}
        for(String bad:new String[]{"{\"tool\":\"screenshots\",\"limit\":201}","{\"tool\":\"screenshots\",\"recursive\":true}","{\"tool\":\"screenshots\",\"path\":5}"})try{request(bad);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void mixedImagesUseModificationTimeAndExcludeVideosAndNestedSymlinks()throws Exception{
        Path dir=Files.createTempDirectory(new File("build").toPath(),"screenshots-");try{
            for(String name:new String[]{"old.png","new.JPG","middle.webp","movie.mp4","note.txt"})Files.write(dir.resolve(name),new byte[]{1});
            Files.setLastModifiedTime(dir.resolve("old.png"),java.nio.file.attribute.FileTime.fromMillis(100000));Files.setLastModifiedTime(dir.resolve("middle.webp"),java.nio.file.attribute.FileTime.fromMillis(200000));Files.setLastModifiedTime(dir.resolve("new.JPG"),java.nio.file.attribute.FileTime.fromMillis(300000));
            Files.createSymbolicLink(dir.resolve("link.jpg"),dir.resolve("new.JPG").toAbsolutePath());
            WorkspaceStore.Session p=LocalAiDiscoveryTest.profile(dir);JSONObject r=request("{\"tool\":\"screenshots\",\"path\":\".\",\"limit\":2}");
            JSONObject out=new JSONObject(LocalAiDirectory.screenshots(LocalAiDiscoveryTest.shell(LocalAiTools.script(p,r),0),r));
            assertEquals("FOUND",out.getString("status"));assertEquals(3,out.getInt("matched"));assertTrue(out.getBoolean("truncated"));assertEquals("new.JPG",out.getJSONArray("entries").getJSONObject(0).getString("name"));assertEquals("middle.webp",out.getJSONArray("entries").getJSONObject(1).getString("name"));
        }finally{LocalAiDiscoveryTest.clean(dir);}
    }
    @Test public void screenshotDiscoveryChecksScopeBeforeProbingOtherPhonePaths()throws Exception{
        Path dir=Files.createTempDirectory(new File("build").toPath(),"scope-");try{WorkspaceStore.Session p=LocalAiDiscoveryTest.profile(dir);JSONObject r=request("{\"tool\":\"screenshots\"}");JSONObject out=new JSONObject(LocalAiDirectory.screenshots(LocalAiDiscoveryTest.shell(LocalAiTools.script(p,r),0),r));assertEquals("NO_ACCESSIBLE_SCREENSHOT_DIRECTORY",out.getString("status"));JSONArray locations=out.getJSONArray("checkedLocations");assertTrue(locations.length()>5);for(int i=0;i<locations.length();i++)assertEquals("OUTSIDE_PROJECT",locations.getJSONObject(i).getString("status"));}finally{LocalAiDiscoveryTest.clean(dir);}
    }
    @Test public void alternateStoragePathsAndFilenameCaseAreNotDeviceSpecific(){
        WorkspaceStore.Session p=new WorkspaceStore.Session();assertTrue(LocalAiDirectory.screenshotPaths(p).contains("/storage/self/primary/DCIM/Screenshots"));assertTrue(LocalAiDirectory.screenshotPaths(p).contains("/storage/emulated/0/Pictures/ScreenShots"));assertTrue(LocalAiDirectory.knownPaths(p).contains("/sdcard/Downloads"));
    }
    @Test public void missingDeniedAndEmptyRemainDifferentAndDuplicateAliasesDoNotDuplicateFiles()throws Exception{
        String records="0\u00000\u0000NOT_FOUND\u0000/missing\u00000\u00000\u0000ACCESS_DENIED\u0000/denied\u00000\u00000\u0000AVAILABLE\u0000/readable\u0000";
        JSONObject empty=new JSONObject(LocalAiDirectory.screenshots(records.getBytes(StandardCharsets.UTF_8),new JSONObject()));assertEquals("NO_MATCHES_IN_CHECKED_LOCATIONS",empty.getString("status"));assertEquals("ACCESS_DENIED",empty.getJSONArray("checkedLocations").getJSONObject(1).getString("status"));
        String image="10\u00001\u0000f\u0000/readable/Screenshot121.JPG\u0000";JSONObject out=new JSONObject(LocalAiDirectory.screenshots((records+image+image).getBytes(StandardCharsets.UTF_8),new JSONObject().put("query","Screenshot121")));assertEquals(1,out.getInt("matched"));
    }
    @Test public void explicitMissingDirectoryDoesNotInventEmptySuccess()throws Exception{
        Path dir=Files.createTempDirectory(new File("build").toPath(),"missing-");try{JSONObject r=request("{\"tool\":\"screenshots\",\"path\":\"missing\"}");JSONObject out=new JSONObject(LocalAiDirectory.screenshots(LocalAiDiscoveryTest.shell(LocalAiTools.script(LocalAiDiscoveryTest.profile(dir),r),0),r));assertEquals("NOT_FOUND",out.getJSONArray("checkedLocations").getJSONObject(0).getString("status"));assertEquals("NO_ACCESSIBLE_SCREENSHOT_DIRECTORY",out.getString("status"));}finally{LocalAiDiscoveryTest.clean(dir);}
    }
    @Test public void shortRequestsDiscoverBeforeModelGuessesButQuestionsAndExactNamesDoNot()throws Exception{
        for(String text:new String[]{"3 последних скрина","посмотри 3 последних скриншота","ласт скрин","show latest 3 screenshots","три последних скрина"}){JSONObject out=LocalAiAgent.screenshotRequest(new JSONArray().put(new JSONObject().put("role","user").put("text",text)));assertNotNull(text,out);assertEquals(text.contains("3")||text.contains("три")?3:1,out.getInt("limit"));}
        for(String text:new String[]{"как найти последний скрин?","прочитай Screenshot121.jpg","посмотри /other/image.jpg"})assertNull(LocalAiAgent.screenshotRequest(new JSONArray().put(new JSONObject().put("role","user").put("text",text))));
    }
    @Test public void aliasesWithSameDeviceAndInodeYieldOneImage()throws Exception{
        String records="10\u00001\u0000f:99:123\u0000/sdcard/Pictures/Screenshots/a.jpg\u000010\u00001\u0000f:99:123\u0000/storage/emulated/0/Pictures/Screenshots/a.jpg\u0000";
        JSONObject result=new JSONObject(LocalAiDirectory.screenshots(records.getBytes(StandardCharsets.UTF_8),new JSONObject()));assertEquals(1,result.getInt("matched"));
    }
    @Test public void directDiscoveryUsesNormalToolHistoryAndDoesNotRequireModelGuess()throws Exception{
        LocalAiAgentTest.Fixture f=new LocalAiAgentTest.Fixture();f.p.localAccess="read";f.history=new JSONArray().put(new JSONObject().put("role","user").put("text","3 последних скрина"));f.answers.add("The returned paths identify three screenshots.");f.run();assertEquals(1,f.calls.size());assertEquals("screenshots",new JSONObject(f.calls.get(0)).getString("tool"));assertEquals(3,new JSONObject(f.calls.get(0)).getInt("limit"));assertEquals("done",f.history.getJSONObject(1).getString("toolState"));assertEquals(0,f.systems.size());assertEquals(0,f.approvals);
    }

    @Test public void phoneScreenshotQuestionUsesRealDiscoveryButGeneralQuestionsDoNot()throws Exception{
        for(String text:new String[]{"а скриншоты ты видишь в телефоне последние?","can you see the latest screenshot on my phone?","посмотри ласт скрин на телефоне"})assertNotNull(text,LocalAiAgent.screenshotRequest(new JSONArray().put(new JSONObject().put("role","user").put("text",text))));
        for(String text:new String[]{"ты умеешь видеть скриншоты?","не показывай последний скрин на телефоне","как найти последний скрин на телефоне?"})assertNull(text,LocalAiAgent.screenshotRequest(new JSONArray().put(new JSONObject().put("role","user").put("text",text))));
    }
    @Test public void textOnlyDiscoveryReturnsActualPathsWithoutClaimingVisionOrGenerating()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";String answer=LocalAiAgent.run(p,new JSONArray().put(new JSONObject().put("role","user").put("text","а скриншоты ты видишь в телефоне последние?")),new java.util.concurrent.atomic.AtomicBoolean(),(a,s,o)->{throw new AssertionError("No text model inference needed");},r->{assertEquals("screenshots",r.getString("tool"));return "exit=0\n"+new JSONObject().put("entries",new JSONArray().put(new JSONObject().put("path","/storage/emulated/0/DCIM/Screenshots/latest.JPG")));},r->false,new LocalAiSearchTest.Events(),t->{});
        assertTrue(answer.contains("/storage/emulated/0/DCIM/Screenshots/latest.JPG"));assertTrue(answer.contains("text-only"));assertFalse(answer.contains("Switch to"));
    }
    @Test public void visionCapableSessionCanContinueWithItsModel()throws Exception{
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.localAccess="read";int[] n={0};LocalAiAgent.run(p,new JSONArray().put(new JSONObject().put("role","user").put("text","last screenshot")),new java.util.concurrent.atomic.AtomicBoolean(),(a,s,o)->{n[0]++;return new LocalAiAgent.Generation("Located; request image tool next.","eos");},r->"exit=0\n{\"entries\":[]}",r->false,new LocalAiAgent.Events(){public void save(){}public void phase(String phase,String detail){}public boolean supportsVision(){return true;}},t->{});assertEquals(1,n[0]);
    }
}
