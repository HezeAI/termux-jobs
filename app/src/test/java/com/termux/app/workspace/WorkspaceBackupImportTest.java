package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceBackupImportTest {
    private Activity activity;private WorkspaceStore store;
    @Before public void before(){activity=Robolectric.buildActivity(Activity.class).setup().get();store=new WorkspaceStore(activity,e->fail(e));store.commands.clear();store.notes.clear();store.sessions.clear();}
    @After public void after(){store.close();activity.finish();}
    private JSONObject report()throws Exception{return new JSONObject().put("workspace",store.encode()).put("components",new JSONArray()).put("candidates",new JSONArray());}
    @Test public void importedPresetsRequireReviewAndNeverCarryRuntimeOrEncryptedPasswords()throws Exception{
        WorkspaceStore.Session original=new WorkspaceStore.Session();original.name="Imported";original.environment="termux";original.startup="echo reviewed";original.saved=true;original.handle="live-source";original.sshEncryptedPassword="unusable-encrypted-value";store.sessions.add(original);JSONObject report=report();store.sessions.clear();
        WorkspaceBackupImport.Choice choice=new WorkspaceBackupImport.Choice();choice.presets=true;WorkspaceBackupImport.Prepared p=WorkspaceBackupImport.prepare(activity,report,new JSONObject(),choice);assertEquals(1,p.presets.size());WorkspaceStore.Session s=p.presets.get(0);assertTrue(s.restoreReview);assertEquals("",s.handle);assertEquals("",s.sshEncryptedPassword);assertNotEquals(original.id,s.id);assertNotEquals(original.codexSocketId,s.codexSocketId);assertEquals(original.startup,s.startup);
        store.sessions.add(s);JSONObject encoded=store.encode();store.decode(encoded.toString());assertTrue(store.sessions.get(0).restoreReview);assertTrue(store.sessions.get(0).copy().restoreReview);
    }
    @Test public void replacementPreservesLiveSessionsAndBaseTerminal()throws Exception{
        WorkspaceStore.Session base=new WorkspaceStore.Session();base.id=WorkspaceStore.BASE_TERMINAL_ID;base.saved=true;WorkspaceStore.Session active=new WorkspaceStore.Session();active.saved=true;active.handle="current-live";WorkspaceStore.Session old=new WorkspaceStore.Session();old.saved=true;
        store.sessions.add(base);store.sessions.add(active);store.sessions.add(old);WorkspaceStore.Command previous=new WorkspaceStore.Command();previous.command="old";store.commands.add(previous);
        WorkspaceBackupImport.Prepared data=new WorkspaceBackupImport.Prepared();WorkspaceStore.Command imported=new WorkspaceStore.Command();imported.command="new";data.commands.add(imported);WorkspaceBackupImport.Choice choice=new WorkspaceBackupImport.Choice();choice.commands=true;choice.presets=true;choice.replace=true;
        WorkspaceBackupImport.apply(activity,store,data,choice);store.awaitWrites();assertEquals(1,store.commands.size());assertEquals("new",store.commands.get(0).command);assertTrue(store.sessions.contains(base));assertTrue(store.sessions.contains(active));assertFalse(store.sessions.contains(old));assertTrue(new java.io.File(activity.getFilesDir(),"workspace-import-recovery").list().length>0);
    }
    @Test public void mergeDeduplicatesButPreservesDifferentCommands()throws Exception{
        WorkspaceStore.Command c=new WorkspaceStore.Command();c.name="Run";c.command="echo old";store.commands.add(c);WorkspaceBackupImport.Prepared data=new WorkspaceBackupImport.Prepared();WorkspaceStore.Command same=new WorkspaceStore.Command();same.name="Run";same.command="echo old";WorkspaceStore.Command other=new WorkspaceStore.Command();other.name="Run";other.command="echo new";data.commands.add(same);data.commands.add(other);
        WorkspaceBackupImport.apply(activity,store,data,new WorkspaceBackupImport.Choice());store.awaitWrites();assertEquals(2,store.commands.size());assertTrue(WorkspaceBackupImport.persisted(store.encode(),data));
    }
    @Test public void invalidLibraryDoesNotClearExistingData()throws Exception{
        WorkspaceStore.Command c=new WorkspaceStore.Command();c.name="Keep";c.command="echo keep";store.commands.add(c);JSONObject r=report();r.getJSONObject("workspace").getJSONArray("commands").getJSONObject(0).put("id","not-uuid");WorkspaceBackupImport.Choice choice=new WorkspaceBackupImport.Choice();choice.commands=true;choice.replace=true;
        try{WorkspaceBackupImport.prepare(activity,r,new JSONObject(),choice);fail();}catch(JSONException expected){}assertEquals("echo keep",store.commands.get(0).command);
    }
    @Test public void aliasesAreExplicitlySelectedAndScriptsRequireRestoredComponent()throws Exception{
        JSONObject r=report();r.getJSONArray("candidates").put(new JSONObject().put("kind","alias").put("name","ll").put("command","ls -lah")).put(new JSONObject().put("kind","script").put("name","build.sh").put("source","home/build.sh").put("root","home").put("component",0));
        WorkspaceBackupImport.Choice c=new WorkspaceBackupImport.Choice();assertEquals(0,WorkspaceBackupImport.prepare(activity,r,new JSONObject(),c).commands.size());c.candidates.add(0);assertEquals("ls -lah",WorkspaceBackupImport.prepare(activity,r,new JSONObject(),c).commands.get(0).command);c.candidates.add(1);try{WorkspaceBackupImport.prepare(activity,r,new JSONObject(),c);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void restoredUbuntuGetsAnExplicitSeparateLaunchPreset()throws Exception{
        JSONObject restored=new JSONObject().put("environments",new JSONArray().put("jobs-abcdef123456"));WorkspaceBackupImport.Prepared p=WorkspaceBackupImport.prepare(activity,report(),restored,new WorkspaceBackupImport.Choice());assertEquals(1,p.presets.size());assertEquals("termux",p.presets.get(0).environment);assertEquals("jobs-abcdef123456",p.presets.get(0).prootAlias);assertTrue(p.presets.get(0).prootIsolated);assertEquals("",p.presets.get(0).startup);assertTrue(p.presets.get(0).restoreReview);
    }

    @Test public void restoredPresetsRebindOnlyTheSelectedSourceInstance()throws Exception{
        WorkspaceStore.Session alpine=new WorkspaceStore.Session();alpine.environment="termux";alpine.prootAlias="alpine";alpine.saved=true;alpine.startup="pwd";store.sessions.add(alpine);
        WorkspaceStore.Session other=new WorkspaceStore.Session();other.environment="termux";other.prootAlias="debian";other.saved=true;store.sessions.add(other);
        WorkspaceStore.Session agent=new WorkspaceStore.Session();agent.environment="codex";agent.saved=true;store.sessions.add(agent);
        WorkspaceBackupImport.Choice choice=new WorkspaceBackupImport.Choice();choice.presets=true;JSONObject restored=new JSONObject().put("environmentBindings",new JSONObject().put("alpine","jobs-abcdef123456").put("ubuntu","jobs-111111111111"));
        WorkspaceBackupImport.Prepared data=WorkspaceBackupImport.prepare(activity,report(),restored,choice);
        assertEquals("jobs-abcdef123456",data.presets.get(0).prootAlias);assertEquals("pwd",data.presets.get(0).startup);assertTrue(data.presets.get(0).restoreReview);
        assertEquals("debian",data.presets.get(1).prootAlias);assertEquals("codex",data.presets.get(2).environment);assertEquals("",data.presets.get(2).prootAlias);
    }
 @Test public void sameNamedAgentPresetsRetainDifferentIdentitiesAndSettings()throws Exception{
  WorkspaceStore.Session existing=new WorkspaceStore.Session();existing.saved=true;existing.environment="ubuntu";existing.name="Agent";existing.aiAgent="opencode";store.sessions.add(existing);
  WorkspaceStore.Session imported=existing.copy();imported.id=java.util.UUID.randomUUID().toString();imported.name=existing.name;imported.saved=true;imported.aiAgent="";imported.geminiAgent=true;
  WorkspaceBackupImport.Prepared data=new WorkspaceBackupImport.Prepared();data.presets.add(imported);
  WorkspaceBackupImport.apply(activity,store,data,new WorkspaceBackupImport.Choice());store.awaitWrites();
  assertEquals(2,store.sessions.size());assertEquals("opencode",store.sessions.get(0).aiAgent);assertFalse(store.sessions.get(0).geminiAgent);assertNotEquals(existing.id,imported.id);assertTrue(store.sessions.get(1).geminiAgent);
  assertTrue(WorkspaceBackupImport.persisted(store.encode(),data));
 }
 @Test public void sameNamedRestoredPresetKeepsHistoryAtItsOwnId()throws Exception{
  WorkspaceStore.Session existing=new WorkspaceStore.Session();existing.saved=true;existing.name="History";store.sessions.add(existing);
  java.io.File restored=new java.io.File(activity.getFilesDir(),"audit-restored");java.io.File source=new java.io.File(restored,"workspace-sessions/"+existing.id+"/history");assertTrue(source.getParentFile().mkdirs());java.nio.file.Files.write(source.toPath(),"from-backup".getBytes("UTF-8"));
  JSONObject report=new JSONObject().put("workspace",store.encode()).put("components",new JSONArray().put(new JSONObject().put("kind","app-data"))).put("candidates",new JSONArray());
  JSONObject restoredPaths=new JSONObject().put("paths",new JSONObject().put("0",restored.getPath()));
  WorkspaceBackupImport.Choice choice=new WorkspaceBackupImport.Choice();choice.presets=true;choice.history=true;
  WorkspaceBackupImport.Prepared data=WorkspaceBackupImport.prepare(activity,report,restoredPaths,choice);
  String newId=data.presets.get(0).id;java.io.File copied=new java.io.File(SessionShell.directory(activity.getFilesDir(),newId),"history");assertTrue(copied.isFile());
  WorkspaceBackupImport.apply(activity,store,data,choice);store.awaitWrites();
  assertNotEquals(existing.id,data.presets.get(0).id);assertEquals(newId,data.presets.get(0).id);assertEquals(2,store.sessions.size());assertTrue(copied.isFile());assertFalse(new java.io.File(SessionShell.directory(activity.getFilesDir(),existing.id),"history").exists());assertTrue(WorkspaceBackupImport.persisted(new JSONObject(store.encode().toString()),data));
 }

 @Test public void persistedRejectsWrongSettingsEvenWhenIdExists()throws Exception{
  WorkspaceStore.Session s=new WorkspaceStore.Session();s.saved=true;s.environment="codex";s.codexAccess="read";WorkspaceBackupImport.Prepared data=new WorkspaceBackupImport.Prepared();data.presets.add(s);store.sessions.add(s);
  JSONObject saved=new JSONObject(store.encode().toString());assertTrue(WorkspaceBackupImport.persisted(saved,data));saved.getJSONArray("sessions").getJSONObject(0).put("codexAccess","full");assertFalse(WorkspaceBackupImport.persisted(saved,data));
 }
}
