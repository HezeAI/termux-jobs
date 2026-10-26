package com.termux.app.workspace;
import android.app.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import java.util.function.Consumer;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceBackupFormTest {
    TermuxActivity a;WorkspaceBackupUi ui;
    @Before public void setup()throws Exception{
        a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"en");
        Constructor<WorkspaceBackupUi> c=WorkspaceBackupUi.class.getDeclaredConstructor(TermuxActivity.class,WorkspaceStore.class,Runnable.class,Consumer.class);c.setAccessible(true);ui=c.newInstance(a,new WorkspaceStore(a,x->{}),(Runnable)()->{},(Consumer<String>)x->{});BackupInventoryCache.SHARED.clear();
    }
    @After public void cleanup(){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();if(d!=null)d.dismiss();BackupInventoryCache.SHARED.clear();a.finish();}
    Object invoke(String name,Class<?>[] types,Object... args)throws Exception{Method m=WorkspaceBackupUi.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(ui,args);}
    static TextView find(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView x=find(((ViewGroup)v).getChildAt(i),text);if(x!=null)return x;}return null;}
    JSONObject inventory(boolean selective)throws Exception{return selective?new JSONObject().put("scopes",new JSONArray()):new JSONObject().put("environments",new JSONArray()).put("ubuntuBytes",new JSONObject()).put("homeBytes",123);}
    AlertDialog openCached(boolean selective)throws Exception{
        String key=(String)invoke("inventoryKey",new Class[]{boolean.class},selective);BackupInventoryCache.SHARED.put(key,inventory(selective),new JSONObject());invoke("inventory",new Class[]{});
        AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();int which=selective?1:0;menu.getListView().performItemClick(new View(a),which,which);
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();return ShadowAlertDialog.getLatestAlertDialog();
    }
    @Test public void bothCachedModesOpenImmediatelyWithRefreshOutsideScrolledBody()throws Exception{
        for(boolean selective:new boolean[]{false,true}){
            AlertDialog d=openCached(selective);TextView refresh=find(d.getWindow().getDecorView(),"Refresh");assertNotNull(refresh);assertEquals(View.VISIBLE,refresh.getVisibility());assertTrue(refresh.isEnabled());
            for(android.view.ViewParent parent=refresh.getParent();parent!=null;parent=parent.getParent())assertFalse(parent instanceof ScrollView);
            assertNotNull(find(d.getWindow().getDecorView(),"Compression method · .tar.gz"));assertNotNull(find(d.getWindow().getDecorView(),"Compression level"));d.dismiss();
        }
    }
    @Test public void compressionMethodAndLevelHaveIndependentMenus()throws Exception{
        AlertDialog d=openCached(false);View root=d.getWindow().getDecorView();TextView method=find(root,"gzip · 1 thread"),level=find(root,"Balanced");assertNotNull(method);assertNotNull(level);
        method.performClick();AlertDialog choices=ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,choices.getListView().getAdapter().getCount());assertEquals("pigz — 2 threads",choices.getListView().getAdapter().getItem(1));assertEquals("pigz — 4 threads",choices.getListView().getAdapter().getItem(2));choices.dismiss();
        level.performClick();choices=ShadowAlertDialog.getLatestAlertDialog();choices.getListView().performItemClick(new View(a),0,0);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        JSONObject req=(JSONObject)invoke("compressedRequest",new Class[]{JSONObject.class},new JSONObject());assertEquals("gzip",req.getString("compressionEngine"));assertEquals(1,req.getInt("compressionThreads"));assertEquals(1,req.getInt("compressionLevel"));assertEquals("Fast",level.getText().toString());d.dismiss();
    }
    @Test public void aliasImportPreviewsFullCommandAndConfirmsReplacementWithoutExecutingIt()throws Exception{
        WorkspaceStore.Alias alias=new WorkspaceStore.Alias();alias.environment="termux";alias.name="ll";alias.command="printf 'never executed'";
        Field f=WorkspaceBackupUi.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore store=(WorkspaceStore)f.get(ui);store.aliases.clear();WorkspaceStore.Alias existing=alias.copy();existing.command="old";store.aliases.add(existing);
        invoke("aliasPicker",new Class[]{java.util.List.class,JSONObject.class},java.util.Collections.singletonList(alias),new JSONObject());org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();View root=picker.getWindow().getDecorView();
        find(root,"Preview: ll").performClick();AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(find(preview.getWindow().getDecorView(),alias.command));preview.getButton(-1).performClick();
        CheckBox choice=(CheckBox)find(root,"ll · Termux · "+WorkspaceText.tr("Общее"));assertNotNull(choice);assertTrue(choice.isEnabled());assertFalse(choice.isChecked());choice.setChecked(true);picker.getButton(-1).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();AlertDialog confirm=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Confirm alias import",org.robolectric.Shadows.shadowOf(confirm).getTitle());String message=((TextView)confirm.findViewById(android.R.id.message)).getText().toString();assertTrue(message.contains("Will replace:"));assertTrue(message.contains("ll · Termux"));assertTrue(message.contains("Restart the Termux tab"));confirm.getButton(-2).performClick();assertEquals("old",existing.command);assertEquals(1,store.aliases.size());assertTrue(picker.isShowing());assertFalse(WorkspaceAliases.BUSY.get());store.close();
    }
    @Test public void aliasExportHasSeparateSelectionAndDoesNotIncludeUnselectedRecordsOnOpen()throws Exception{
        WorkspaceStore.Alias alias=new WorkspaceStore.Alias();alias.name="files";alias.command="ls -lah";
        invoke("aliasPicker",new Class[]{java.util.List.class,JSONObject.class},java.util.Collections.singletonList(alias),null);AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();View root=picker.getWindow().getDecorView();assertEquals("Export aliases",org.robolectric.Shadows.shadowOf(picker).getTitle());assertNotNull(find(root,"Preview: files"));assertNotNull(find(root,"Choose directory"));assertEquals("Export",picker.getButton(-1).getText().toString());picker.getButton(-2).performClick();assertFalse(WorkspaceAliases.BUSY.get());
    }
    @Test public void unifiedAliasTransferHasFourExplicitFormatsAndManualImportChoosesEnvironment()throws Exception{
        invoke("aliasTransferMenu",new Class[]{});AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();View root=menu.getWindow().getDecorView();for(String title:new String[]{"Import from shell file","Import from JSON/backup","Export to shell file","Export to JSON"})assertNotNull(find(root,title));
        find(root,"Import from shell file").performClick();AlertDialog manual=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(find(manual.getWindow().getDecorView(),"Termux ▾"));assertNotNull(find(manual.getWindow().getDecorView(),"Import environment"));manual.dismiss();
        find(root,"Import from JSON/backup").performClick();AlertDialog json=ShadowAlertDialog.getLatestAlertDialog();assertNull(find(json.getWindow().getDecorView(),"Import environment"));json.dismiss();
    }
    @Test public void shellExportSelectsOnlyOneEnvironmentBeforeChoosingRecords()throws Exception{
        Field f=WorkspaceBackupUi.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore store=(WorkspaceStore)f.get(ui);store.aliases.clear();for(String environment:new String[]{"termux","ubuntu"}){WorkspaceStore.Alias alias=new WorkspaceStore.Alias();alias.name="files";alias.command="ls -lah";alias.environment=environment;store.aliases.add(alias);}
        invoke("aliasExport",new Class[]{boolean.class},true);AlertDialog environments=ShadowAlertDialog.getLatestAlertDialog();assertEquals(2,environments.getListView().getCount());environments.getListView().performItemClick(null,0,0);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();View root=picker.getWindow().getDecorView();assertNotNull(find(root,"files · Termux · "+WorkspaceText.tr("Общее")));assertNull(find(root,"files · Ubuntu · "+WorkspaceText.tr("Общее")));picker.dismiss();store.close();
    }
}
