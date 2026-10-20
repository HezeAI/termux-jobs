package com.termux.app.workspace;

import android.app.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class StorageDeletionConsentTest {
    @Test public void deletionCannotStartUntilFreshAcknowledgementAndCannotDismissOutside(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(com.termux.R.style.Theme_Termux_Black);int[] count={0};AlertDialog dialog=StorageDeletionConsent.show(activity,"Delete","Selected files",()->count[0]++);
        assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
        CheckBox checkbox=find(dialog.getWindow().getDecorView());assertNotNull(checkbox);checkbox.setChecked(true);assertTrue(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());checkbox.setChecked(false);assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
        dialog.onTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_OUTSIDE,-1,-1,0));assertTrue(dialog.isShowing());dialog.onBackPressed();assertTrue(dialog.isShowing());dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,count[0]);
        dialog=StorageDeletionConsent.show(activity,"Delete","Selected",()->count[0]++);assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());find(dialog.getWindow().getDecorView()).setChecked(true);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertEquals(1,count[0]);
    }
    private CheckBox find(android.view.View view){if(view instanceof CheckBox)return (CheckBox)view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){CheckBox found=find(group.getChildAt(i));if(found!=null)return found;}}return null;}
    @Test public void cleanupScopeTracksConcreteRunningEnvironmentAndBaseNeedsSeparateIdleCheck(){
        WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,message->{});store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="termux";p.prootAlias="alpine";p.handle="live";store.sessions.add(p);
        assertTrue(WorkspaceStorageCleanup.scopeInUse(store,"alpine",java.util.Set.of("live")));assertFalse(WorkspaceStorageCleanup.scopeInUse(store,"debian",java.util.Set.of("live")));assertFalse(WorkspaceStorageCleanup.scopeInUse(store,"alpine",java.util.Set.of()));
        p.environment="codex";p.prootAlias="";p.codexEnvironment="ubuntu";assertTrue(WorkspaceStorageCleanup.scopeInUse(store,"ubuntu",java.util.Set.of("live")));
        p.id=WorkspaceStore.BASE_TERMINAL_ID;p.environment="termux";assertFalse(WorkspaceStorageCleanup.scopeInUse(store,"termux",java.util.Set.of("live")));
        assertTrue(WorkspaceStorageCleanup.idleBaseShell("123 (bash) S 1 123 123 0 123"));assertFalse(WorkspaceStorageCleanup.idleBaseShell("123 (bash) S 1 123 123 0 456"));assertFalse(WorkspaceStorageCleanup.idleBaseShell("bad"));
    }
    @Test public void libraryCleanupPreservesUnselectedNotesSessionsAndRejectsStaleLists()throws Exception{
        WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,m->{});store.commands.clear();store.notes.clear();store.sessions.clear();store.recentCommands.clear();
        WorkspaceStore.Command a=new WorkspaceStore.Command(),b=new WorkspaceStore.Command();a.name="a";b.name="b";store.commands.add(a);store.commands.add(b);WorkspaceStore.Note note=new WorkspaceStore.Note();store.notes.add(note);
        String expected=store.encode().getJSONArray("commands").toString();a.command="changed";
        assertFalse(WorkspaceStorageCleanup.clearLibrarySelection(store,"commands",expected,java.util.Set.of(a.id)));assertEquals(2,store.commands.size());
        expected=store.encode().getJSONArray("commands").toString();assertTrue(WorkspaceStorageCleanup.clearLibrarySelection(store,"commands",expected,java.util.Set.of(a.id)));assertEquals(b,store.commands.get(0));assertEquals(note,store.notes.get(0));
        WorkspaceStore.Session base=new WorkspaceStore.Session(),saved=new WorkspaceStore.Session(),temporary=new WorkspaceStore.Session();base.id=WorkspaceStore.BASE_TERMINAL_ID;base.saved=true;saved.saved=true;store.sessions.add(base);store.sessions.add(saved);store.sessions.add(temporary);
        expected=store.encode().getJSONArray("sessions").toString();assertTrue(WorkspaceStorageCleanup.clearLibrarySelection(store,"sessions",expected,java.util.Set.of(base.id,saved.id,temporary.id)));assertEquals(2,store.sessions.size());assertTrue(store.sessions.contains(base));assertTrue(store.sessions.contains(temporary));store.awaitWrites();store.close();
    }
    @Test public void recentCleanupUsesUnchangedSnapshotAndOnlySelectedIndices()throws Exception{
        WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,m->{});store.recentCommands.clear();store.recentCommands.add(new WorkspaceStore.RecentCommand("a",1));store.recentCommands.add(new WorkspaceStore.RecentCommand("b",2));
        String expected=store.encode().getJSONArray("recentCommands").toString();assertTrue(WorkspaceStorageCleanup.clearLibrarySelection(store,"recentCommands",expected,java.util.Set.of("0")));assertEquals("b",store.recentCommands.get(0).command);assertFalse(WorkspaceStorageCleanup.clearLibrarySelection(store,"recentCommands",expected,java.util.Set.of("0")));store.awaitWrites();store.close();
    }
    @Test public void wholeEnvironmentRemovalIsAvailableFromSystemContentsButNeedsConsent()throws Exception{
        com.termux.app.TermuxActivity activity=Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();activity.setTheme(com.termux.R.style.Theme_Termux_Black);WorkspaceStore store=new WorkspaceStore(activity,m->{});
        WorkspaceStorageCleanup cleanup=new WorkspaceStorageCleanup(activity,store,s->false,p->false,()->{},()->{},()->{});
        org.json.JSONObject section=new org.json.JSONObject().put("kind","system").put("scope","alpine").put("path","/fixture/rootfs").put("entries",new org.json.JSONArray());
        org.json.JSONObject group=new org.json.JSONObject().put("id","fixture").put("kind","environment").put("scope","alpine").put("name","alpine").put("bytes",12).put("selectable",true);
        org.json.JSONObject inventory=new org.json.JSONObject().put("plan","fixture").put("groups",new org.json.JSONArray().put(group));
        java.lang.reflect.Method method=WorkspaceStorageCleanup.class.getDeclaredMethod("component",org.json.JSONObject.class,String.class,boolean.class,org.json.JSONObject.class);method.setAccessible(true);
        for(String language:new String[]{"ru","en"}){
            WorkspaceLanguage.select(activity,language);method.invoke(cleanup,section,"Alpine",false,inventory);AlertDialog contents=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            TextView action=findText(contents.getWindow().getDecorView(),language.equals("ru")?"Удалить это окружение целиком":"Delete this entire environment");assertNotNull(action);action.performClick();AlertDialog consent=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotSame(contents,consent);assertFalse(consent.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());assertNotNull(find(consent.getWindow().getDecorView()));consent.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();contents.dismiss();
        }
        store.close();
    }
    private TextView findText(android.view.View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return (TextView)view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView found=findText(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
}
