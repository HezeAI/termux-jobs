package com.termux.app.workspace;

import android.app.*;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceAliasImportTest {
    private Activity activity;private WorkspaceStore store;
    @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();store=new WorkspaceStore(activity,error->fail(error));store.aliases.clear();store.commands.clear();store.notes.clear();store.sessions.clear();}
    @After public void close(){store.close();activity.finish();}
    private WorkspaceStore.Alias alias(String environment,String name,String body){WorkspaceStore.Alias a=new WorkspaceStore.Alias();a.environment=environment;a.name=name;a.command=body;return a;}
    @Test public void exportContainsOnlySelectedAliasesAndRoundTripsEnvironmentCategoryFavorites()throws Exception{
        List<WorkspaceStore.Alias> selected=new ArrayList<>();for(String environment:WorkspaceAliases.ENVIRONMENTS){WorkspaceStore.Alias a=alias(environment,"files","ls -lah");a.category="Files";a.favorite=true;selected.add(a);}
        JSONObject file=WorkspaceAliasImport.export(selected);assertEquals(6,file.length());for(String key:new String[]{"sessions","commands","notes","recentCommands"})assertEquals(0,file.getJSONArray(key).length());
        List<WorkspaceStore.Alias> imported=WorkspaceAliasImport.read(activity,file);assertEquals(8,imported.size());for(int i=0;i<8;i++){assertEquals(selected.get(i).id,imported.get(i).id);assertEquals(selected.get(i).environment,imported.get(i).environment);assertEquals("Files",imported.get(i).category);assertTrue(imported.get(i).favorite);}
        assertTrue(store.aliases.isEmpty());
    }
    @Test public void selectionNormalizesEmptyCategoryPreservesBindingAndDoesNotMutateSource()throws Exception{
        WorkspaceStore.Alias chosen=alias("debian","ll","ls -la"),skipped=alias("ubuntu","ll","ls -l");chosen.category="  ";
        WorkspaceAliasImport.Plan plan=WorkspaceAliasImport.plan(Arrays.asList(chosen,skipped),Collections.singleton(chosen.id),store.aliases,"Общее");assertEquals(1,plan.after.size());assertEquals("debian",plan.after.get(0).environment);assertEquals("Общее",plan.after.get(0).category);assertEquals("  ",chosen.category);assertNotEquals(chosen.id,plan.after.get(0).id);assertEquals(0,plan.replacements());
    }
    @Test public void replacementIsScopedToNameAndEnvironmentAndUnrelatedLibrarySurvives()throws Exception{
        WorkspaceStore.Alias old=alias("ubuntu","ll","old"),other=alias("debian","ll","keep"),incoming=alias("ubuntu","ll","new");store.aliases.addAll(Arrays.asList(old,other));WorkspaceStore.Command cmd=new WorkspaceStore.Command();cmd.command="pwd";store.commands.add(cmd);
        WorkspaceAliasImport.Plan plan=WorkspaceAliasImport.plan(Collections.singletonList(incoming),Collections.singleton(incoming.id),store.aliases,"General");assertEquals(1,plan.replacements());assertEquals(old.id,plan.after.get(0).id);assertEquals("old",old.command);
        List<String> writes=new ArrayList<>();WorkspaceAliasImport.install(plan,(a,delete)->writes.add(a.environment+":"+a.command+":"+delete));assertEquals(Collections.singletonList("ubuntu:new:false"),writes);
        WorkspaceAliasImport.commit(plan,store);store.awaitWrites();assertEquals(2,store.aliases.size());assertTrue(store.aliases.contains(other));assertSame(cmd,store.commands.get(0));assertTrue(WorkspaceAliasImport.persisted(plan,store.encode()));JSONObject wrong=new JSONObject(store.encode().toString());for(int i=0;i<wrong.getJSONArray("aliases").length();i++)if(old.id.equals(wrong.getJSONArray("aliases").getJSONObject(i).getString("id")))wrong.getJSONArray("aliases").getJSONObject(i).put("command","wrong");assertFalse(WorkspaceAliasImport.persisted(plan,wrong));
    }
    @Test public void failedBatchRestoresCompletedWritesAndRetainsMetadata()throws Exception{
        WorkspaceStore.Alias old=alias("ubuntu","ll","old");store.aliases.add(old);WorkspaceStore.Alias a=alias("ubuntu","ll","new"),b=alias("debian","ll","new2");WorkspaceAliasImport.Plan plan=WorkspaceAliasImport.plan(Arrays.asList(a,b),new HashSet<>(Arrays.asList(a.id,b.id)),store.aliases,"General");
        List<String> calls=new ArrayList<>();try{WorkspaceAliasImport.install(plan,(value,delete)->{calls.add(value.environment+":"+value.command);if(value.environment.equals("debian"))throw new java.io.IOException("ALIAS_ENVIRONMENT_NOT_INSTALLED");});fail();}catch(java.io.IOException expected){assertEquals("ALIAS_ENVIRONMENT_NOT_INSTALLED",expected.getMessage());}
        assertEquals(Arrays.asList("ubuntu:new","debian:new2","ubuntu:old"),calls);assertEquals(1,store.aliases.size());assertSame(old,store.aliases.get(0));
    }
    @Test public void rollbackFailureIsExplicitAndStalePlansAreRejected()throws Exception{
        WorkspaceStore.Alias a=alias("ubuntu","a","one"),b=alias("debian","b","two");WorkspaceAliasImport.Plan plan=WorkspaceAliasImport.plan(Arrays.asList(a,b),new HashSet<>(Arrays.asList(a.id,b.id)),store.aliases,"General");
        try{WorkspaceAliasImport.install(plan,(value,delete)->{if(delete||value.environment.equals("debian"))throw new java.io.IOException("FAIL");});fail();}catch(java.io.IOException expected){assertEquals("ALIAS_ROLLBACK_FAILED",expected.getMessage());}
        store.aliases.add(alias("ubuntu","a","concurrent"));try{WorkspaceAliasImport.verify(plan,store.aliases);fail();}catch(java.io.IOException expected){assertEquals("ALIAS_LIBRARY_CHANGED",expected.getMessage());}
    }
    @Test public void invalidAiBindingDuplicatesAndUnknownSelectionRejectWithoutWrites()throws Exception{
        WorkspaceStore.Alias a=alias("ubuntu-ai","ll","ls");try{WorkspaceAliasImport.export(Collections.singletonList(a));fail();}catch(JSONException expected){}
        a.environment="termux";WorkspaceStore.Alias b=a.copy();b.id=UUID.randomUUID().toString();try{WorkspaceAliasImport.plan(Arrays.asList(a,b),Collections.singleton(a.id),store.aliases,"General");fail();}catch(java.io.IOException expected){}
        try{WorkspaceAliasImport.plan(Collections.singletonList(a),Collections.singleton("missing"),store.aliases,"General");fail();}catch(java.io.IOException expected){}
        assertTrue(store.aliases.isEmpty());JSONObject old=store.encode();old.remove("aliases");assertTrue(WorkspaceAliasImport.read(activity,old).isEmpty());
    }
}
