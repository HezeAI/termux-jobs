package com.termux.app.workspace;

import android.app.Application;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspacePackageHistoryTest {
    private File files;private String key;
    @Before public void setup()throws Exception{files=Files.createTempDirectory("package-history-").toFile();key=WorkspacePackageHistory.key("proot:alpine","/fixture/root",1,2);}
    private List<PackageCatalog.Entry> rows(String... pairs){List<PackageCatalog.Entry> rows=new ArrayList<>();for(int i=0;i<pairs.length;i+=2)rows.add(new PackageCatalog.Entry(pairs[i],pairs[i+1]));return rows;}
    @Test public void firstSnapshotIsBaselineAndChangesAreScopedAndPersistent()throws Exception{
        WorkspacePackageHistory.observe(files,key,rows("bash","1","tree","1"),100);assertEquals(0,WorkspacePackageHistory.read(files,key).length());
        WorkspacePackageHistory.observe(files,key,rows("bash","2","curl","3"),200);JSONArray changes=WorkspacePackageHistory.read(files,key);assertEquals(3,changes.length());
        Map<String,String> actions=new HashMap<>();for(int i=0;i<changes.length();i++)actions.put(changes.getJSONObject(i).getString("name"),changes.getJSONObject(i).getString("action"));
        assertEquals("changed",actions.get("bash"));assertEquals("installed",actions.get("curl"));assertEquals("removed",actions.get("tree"));
        WorkspacePackageHistory.observe(files,key,rows("bash","2","curl","3"),300);assertEquals(3,WorkspacePackageHistory.read(files,key).length());
        String another=WorkspacePackageHistory.key("proot:debian","/fixture/root",1,2);assertEquals(0,WorkspacePackageHistory.read(files,another).length());
        assertNotEquals(key,WorkspacePackageHistory.key("proot:alpine","/fixture/root",1,3));
    }
    @Test public void operationReceiptRecordsFailureAndDeduplicatesRetries()throws Exception{
        WorkspacePackageHistory.observe(files,key,rows("tree","1"),100);
        JSONObject meta=new JSONObject().put("id","one").put("name","tree").put("operation","install").put("version","2");
        WorkspacePackageHistory.operation(files,key,meta,2,150);WorkspacePackageHistory.operation(files,key,meta,2,160);
        JSONArray rows=WorkspacePackageHistory.read(files,key);assertEquals(1,rows.length());assertEquals("failure",rows.getJSONObject(0).getString("phase"));assertEquals(2,rows.getJSONObject(0).getInt("exit"));
        WorkspacePackageHistory.observe(files,key,rows("tree","1"),200);assertEquals(1,WorkspacePackageHistory.read(files,key).length());
    }
    @Test public void successAndObservedVersionChangeBecomeOneEntry()throws Exception{
        WorkspacePackageHistory.observe(files,key,rows("tree","1"),100);
        JSONObject meta=new JSONObject().put("id","one").put("name","tree").put("operation","version").put("version","2");WorkspacePackageHistory.operation(files,key,meta,0,150);
        WorkspacePackageHistory.observe(files,key,rows("tree","2"),200);JSONArray events=WorkspacePackageHistory.read(files,key);assertEquals(1,events.length());assertTrue(events.getJSONObject(0).getBoolean("reconciled"));assertEquals("1",events.getJSONObject(0).getString("before"));assertEquals("2",events.getJSONObject(0).getString("after"));
    }
    @Test public void realCommandExitIsPreservedAndCollectedWithoutTerminalOutputCapture()throws Exception{
        WorkspacePackageHistory.observe(files,key,rows(),100);
        String command=PackageOperationJournal.prepare(files,key,"session-1",false,new PackageOperationJournal.Intent("install","tree","2"),"printf visible; (exit 7)");
        Process process=new ProcessBuilder("/bin/sh","-c",command).start();String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);assertEquals(7,process.waitFor());assertEquals("visible",output);
        PackageOperationJournal.collect(files,null);JSONArray events=WorkspacePackageHistory.read(files,key);assertEquals(1,events.length());assertEquals(7,events.getJSONObject(0).getInt("exit"));assertEquals("failure",events.getJSONObject(0).getString("phase"));
        PackageOperationJournal.collect(files,null);assertEquals(1,WorkspacePackageHistory.read(files,key).length());assertEquals(0,PackageOperationJournal.pending(files,key).length());
    }
    @Test public void pendingAndClosedWithoutResultAreNotInventedFailures()throws Exception{
        PackageOperationJournal.prepare(files,key,"session-2",true,new PackageOperationJournal.Intent("remove","tree","1"),"false");
        assertEquals("pending",PackageOperationJournal.pending(files,key).getJSONObject(0).getString("phase"));assertEquals(0,WorkspacePackageHistory.read(files,key).length());
        PackageOperationJournal.collect(files,"session-2");JSONObject result=WorkspacePackageHistory.read(files,key).getJSONObject(0);assertEquals("unknown",result.getString("phase"));assertEquals(-1,result.getInt("exit"));
    }
    @Test public void resultWriterNeverOverwritesALinkOrExistingFile()throws Exception{
        Path protectedFile=files.toPath().resolve("auth.json");Files.write(protectedFile,"secret".getBytes(StandardCharsets.UTF_8));Path link=files.toPath().resolve("result");Files.createSymbolicLink(link,protectedFile);
        Process process=new ProcessBuilder("/bin/sh","-c",PackageOperationJournal.wrap("true",link.toString())).start();assertEquals(0,process.waitFor());assertEquals("secret",new String(Files.readAllBytes(protectedFile),StandardCharsets.UTF_8));
        process=new ProcessBuilder("/bin/sh","-c",PackageOperationJournal.wrap("false",protectedFile.toString())).start();assertEquals(1,process.waitFor());assertEquals("secret",new String(Files.readAllBytes(protectedFile),StandardCharsets.UTF_8));
    }
    @Test public void linkedHistoryAndUnsafeKeyAreRejected()throws Exception{
        File external=Files.createTempDirectory("history-external-").toFile();Files.createSymbolicLink(files.toPath().resolve("workspace-package-history"),external.toPath());
        try{WorkspacePackageHistory.read(files,key);fail();}catch(IOException expected){}
        try{WorkspacePackageHistory.read(files,"../auth");fail();}catch(IOException expected){}
    }
    @Test public void historyLimitAndMergeOrderAreBounded()throws Exception{
        WorkspacePackageHistory.observe(files,key,rows(),0);
        for(int i=0;i<305;i++)WorkspacePackageHistory.operation(files,key,new JSONObject().put("id","id"+i).put("name","tree").put("operation","install").put("version","1"),0,i);
        JSONArray done=WorkspacePackageHistory.read(files,key);assertEquals(300,done.length());
        JSONArray pending=new JSONArray().put(new JSONObject().put("observed",999).put("name","pending"));JSONArray merged=WorkspacePackageHistory.merge(done,pending);assertEquals(300,merged.length());assertEquals("pending",merged.getJSONObject(299).getString("name"));
    }
}
