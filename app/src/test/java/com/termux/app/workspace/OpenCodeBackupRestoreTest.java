package com.termux.app.workspace;

import android.app.Activity;
import android.util.AtomicFile;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=android.app.Application.class)
public class OpenCodeBackupRestoreTest {
    Activity activity;File base,stage,original;WorkspaceStore store;JSONObject report,restored;WorkspaceBackupImport.Choice choice;String old="11111111-1111-4111-8111-111111111111";
    @Before public void setup()throws Exception{
        activity=Robolectric.buildActivity(Activity.class).setup().get();base=Files.createTempDirectory("ocbackup").toFile();stage=new File(base,"restored/opencode-0/"+old);assertTrue(new File(stage,"config/opencode").mkdirs());Files.write(new File(stage,"config/opencode/opencode.json").toPath(),"SYNTHETIC_RESTORED_SECRET".getBytes(StandardCharsets.UTF_8));
        File connections=new File(activity.getFilesDir(),".ai-control/connections");connections.mkdirs();original=new File(connections,old);new File(original,"config").mkdirs();Files.write(new File(original,"config/original.txt").toPath(),"SYNTHETIC_ORIGINAL".getBytes(StandardCharsets.UTF_8));
        WorkspaceStore.Session profile=WorkspaceStore.newProfile();profile.saved=true;profile.environment="ubuntu";profile.aiAgent="opencode";profile.aiConnection=old;profile.name="Synthetic restored OpenCode";
        report=new JSONObject().put("workspace",new JSONObject().put("version",8).put("commands",new JSONArray()).put("notes",new JSONArray()).put("sessions",new JSONArray().put(WorkspaceStore.sessionJson(profile))));
        restored=new JSONObject().put("path",new File(base,"restored").getAbsolutePath()).put("opencodeConnections",new JSONObject().put(old,stage.getAbsolutePath()));choice=new WorkspaceBackupImport.Choice();choice.presets=true;
        store=new WorkspaceStore(activity,x->{throw new AssertionError(x);},new AtomicFile(new File(base,"workspace.json")));
    }
    @After public void end(){store.close();activity.finish();}
    void originalUnchanged()throws Exception{assertEquals("SYNTHETIC_ORIGINAL",new String(Files.readAllBytes(new File(original,"config/original.txt").toPath()),StandardCharsets.UTF_8));}
    @Test public void importPublishesFreshConnectionAndPersistsItsProfile()throws Exception{
        WorkspaceBackupImport.Prepared data=WorkspaceBackupImport.prepare(activity,report,restored,choice);WorkspaceStore.Session p=data.presets.get(0);String id=p.id,connection=p.aiConnection;assertNotEquals(old,connection);File target=new File(activity.getFilesDir(),".ai-control/connections/"+connection);assertFalse(target.exists());originalUnchanged();
        WorkspaceBackupImport.apply(activity,store,data,choice);store.awaitWrites();assertEquals("SYNTHETIC_RESTORED_SECRET",new String(Files.readAllBytes(new File(target,"config/opencode/opencode.json").toPath()),StandardCharsets.UTF_8));assertTrue(p.restoreReview);assertEquals("",p.handle);assertTrue(AiAgents.command(activity.getFilesDir(),p).contains(connection));
        WorkspaceStore read=new WorkspaceStore(activity,x->{throw new AssertionError(x);},new AtomicFile(new File(base,"workspace.json")));try{assertEquals(connection,read.sessions.stream().filter(s->s.id.equals(id)).findFirst().get().aiConnection);}finally{read.close();}originalUnchanged();
    }
    @Test public void sharedConnectionIsClonedOnceAndRepeatImportGetsNewIdentity()throws Exception{
        JSONArray list=report.getJSONObject("workspace").getJSONArray("sessions");JSONObject second=new JSONObject(list.getJSONObject(0).toString());second.put("id",java.util.UUID.randomUUID().toString());list.put(second);
        WorkspaceBackupImport.Prepared first=WorkspaceBackupImport.prepare(activity,report,restored,choice);assertEquals(first.presets.get(0).aiConnection,first.presets.get(1).aiConnection);String id=first.presets.get(0).aiConnection;WorkspaceBackupImport.apply(activity,store,first,choice);
        WorkspaceBackupImport.Prepared next=WorkspaceBackupImport.prepare(activity,report,restored,choice);try{assertNotEquals(id,next.presets.get(0).aiConnection);}finally{next.close();}assertTrue(new File(activity.getFilesDir(),".ai-control/connections/"+id).isDirectory());originalUnchanged();
    }
    @Test public void cancellingPreparedImportRemovesOnlyOwnedTemporaryCopy()throws Exception{
        WorkspaceBackupImport.Prepared data=WorkspaceBackupImport.prepare(activity,report,restored,choice);String id=data.presets.get(0).aiConnection;data.close();assertFalse(new File(activity.getFilesDir(),".ai-control/connections/"+id).exists());assertFalse(new File(activity.getFilesDir(),".ai-control/connections/.backup-"+id).exists());originalUnchanged();
    }
    @Test public void oldArchiveWithoutConnectionComponentRemainsImportable()throws Exception{
        WorkspaceBackupImport.Prepared data=WorkspaceBackupImport.prepare(activity,report,new JSONObject(),choice);try{assertNotEquals(old,data.presets.get(0).aiConnection);assertTrue(data.presets.get(0).restoreReview);}finally{data.close();}originalUnchanged();
    }
    @Test public void externalSourceOrSymlinkIsRejectedWithoutChangingOriginal()throws Exception{
        restored.getJSONObject("opencodeConnections").put(old,original.getAbsolutePath());try{WorkspaceBackupImport.prepare(activity,report,restored,choice);fail("External source accepted");}catch(java.io.IOException expected){}originalUnchanged();
        restored.getJSONObject("opencodeConnections").put(old,stage.getAbsolutePath());File outside=new File(base,"outside.txt");Files.write(outside.toPath(),"SYNTHETIC_OUTSIDE".getBytes(StandardCharsets.UTF_8));Files.createSymbolicLink(new File(stage,"config/leak").toPath(),outside.toPath());try{WorkspaceBackupImport.prepare(activity,report,restored,choice);fail("Symlink accepted");}catch(java.io.IOException expected){}originalUnchanged();
    }
    @Test public void destinationCollisionPreservesItAndRetryUsesFreshIdentity()throws Exception{
        WorkspaceBackupImport.Prepared data=WorkspaceBackupImport.prepare(activity,report,restored,choice);String id=data.presets.get(0).aiConnection;File target=new File(activity.getFilesDir(),".ai-control/connections/"+id);assertTrue(target.mkdir());File marker=new File(target,"unrelated.txt");Files.write(marker.toPath(),"SYNTHETIC_UNRELATED".getBytes(StandardCharsets.UTF_8));
        try{WorkspaceBackupImport.apply(activity,store,data,choice);fail("Collision accepted");}catch(java.io.IOException expected){}assertEquals("SYNTHETIC_UNRELATED",new String(Files.readAllBytes(marker.toPath()),StandardCharsets.UTF_8));originalUnchanged();
        WorkspaceBackupImport.Prepared next=WorkspaceBackupImport.prepare(activity,report,restored,choice);assertNotEquals(id,next.presets.get(0).aiConnection);WorkspaceBackupImport.apply(activity,store,next,choice);originalUnchanged();
    }

}
