package com.termux.app.workspace;

import android.app.*;
import android.content.*;
import android.content.res.Resources;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.activities.SettingsActivity;
import java.lang.reflect.*;
import java.util.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

/** Exercise actual UI entry points while preventing maintenance workers from running. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows={WorkspaceSettingsMenuTest.BackupShadow.class,WorkspaceSettingsMenuTest.UpdatesShadow.class})
public class WorkspaceSettingsMenuTest {
    static int backupInventory,updateInventory;
    @Implements(value=WorkspaceBackupEngine.class,isInAndroidSdk=false)
    public static class BackupShadow {
        @Implementation protected boolean start(Activity activity,JSONObject request,WorkspaceBackupEngine.Callback callback){
            assertEquals("Only read-only inventory may be requested from a menu", "cleanup-inventory",request.optString("operation"));backupInventory++;return false;
        }
    }
    @Implements(value=EnvironmentUpdateJobs.class,isInAndroidSdk=false)
    public static class UpdatesShadow {
        @Implementation protected static boolean start(Context context,JSONObject request){
            assertEquals("Only read-only inventory may be requested from a menu","inventory",request.optString("operation"));updateInventory++;return false;
        }
    }
    TermuxActivity activity;WorkspaceController controller;Resources previous,previousEnglish;
    final List<AlertDialog> dialogs=new ArrayList<>();
    @Before public void setUp()throws Exception{
        backupInventory=updateInventory=0;previous=WorkspaceText.resources;previousEnglish=WorkspaceText.englishResources;
        activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field terminal=TermuxActivity.class.getDeclaredField("mTerminalView");terminal.setAccessible(true);terminal.set(activity,activity.findViewById(R.id.terminal_view));
        WorkspaceLanguage.select(activity,"ru");WorkspaceLanguage.apply(activity);controller=new WorkspaceController(activity);
    }
    @After public void tearDown(){for(int n=dialogs.size()-1;n>=0;n--)dialogs.get(n).dismiss();idle();controller.close();activity.finish();WorkspaceText.resources=previous;WorkspaceText.englishResources=previousEnglish;}
    void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
    AlertDialog latest(){idle();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();if(!dialogs.contains(d))dialogs.add(d);return d;}
    AlertDialog open(String language)throws Exception{
        WorkspaceLanguage.select(activity,language);WorkspaceLanguage.apply(activity);Method menu=WorkspaceController.class.getDeclaredMethod("globalSettings");menu.setAccessible(true);menu.invoke(controller);return latest();
    }
    String firstText(View v){if(v instanceof TextView)return ((TextView)v).getText().toString();if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){String text=firstText(((ViewGroup)v).getChildAt(n));if(!text.isEmpty())return text;}return "";}
    String allText(View v){String result=v instanceof TextView?((TextView)v).getText().toString()+"\n":"";if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++)result+=allText(((ViewGroup)v).getChildAt(n));return result;}
    List<String> rows(AlertDialog d,String tag){ViewGroup form=d.getWindow().getDecorView().findViewWithTag(tag);assertNotNull(form);List<String> labels=new ArrayList<>();for(int n=0;n<form.getChildCount();n++)labels.add(firstText(form.getChildAt(n)));return labels;}
    View row(AlertDialog d,String tag,int index){ViewGroup form=d.getWindow().getDecorView().findViewWithTag(tag);return form.getChildAt(index);}
    @Test public void rootAndAdvancedOrderNoDuplicatesInBothLanguages()throws Exception{
        for(String language:new String[]{"ru","en"}){
            boolean ru=language.equals("ru");AlertDialog root=open(language);
            List<String> expected=ru?Arrays.asList("Язык / Language","Проверить обновления","Инструкции агентов","Истории чатов","Дополнительно","О приложении"):Arrays.asList("Language / Язык","Check for updates","Agent instructions","Chat histories","Advanced","About");
            assertEquals(expected,rows(root,"workspace-global-settings"));row(root,"workspace-global-settings",4).performClick();AlertDialog advanced=latest();
            assertEquals(ru?Arrays.asList("Резервное копирование","Проверить обновления окружений","Очистить хранилище","Ошибки и решения"):Arrays.asList("Backup and restore","Check environment updates","Clean up storage","Errors and solutions"),rows(advanced,"workspace-advanced-settings"));
            assertEquals(ru?"Назад":"Back",advanced.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());assertTrue(root.isShowing());advanced.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();idle();assertFalse(advanced.isShowing());assertTrue(root.isShowing());root.dismiss();
        }
        assertEquals(0,backupInventory);assertEquals(0,updateInventory);
    }
    @Test public void transferredActionsOpenActualWindowsAndReturnWithoutMaintenance()throws Exception{
        for(String language:new String[]{"ru","en"}){
            AlertDialog root=open(language);row(root,"workspace-global-settings",4).performClick();AlertDialog advanced=latest();
            for(int index=0;index<4;index++){
                row(advanced,"workspace-advanced-settings",index).performClick();AlertDialog child=latest();assertNotSame(advanced,child);assertTrue(child.isShowing());assertTrue(advanced.isShowing());assertTrue(root.isShowing());
                String[][] expected={{"Создать резервную копию","Проверить пакеты всех окружений","Ничего не отмечено заранее.","Ошибки и решения"},{"Create backup","Check packages in all environments","Nothing is preselected.","Errors and solutions"}};
                assertTrue("Wrong child window",allText(child.getWindow().getDecorView()).contains(expected[language.equals("ru")?0:1][index]));
                assertNotNull(child.getButton(AlertDialog.BUTTON_NEGATIVE));child.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();idle();assertFalse(child.isShowing());assertTrue(advanced.isShowing());
                assertNull(activity.getCurrentSession());
            }
            advanced.dismiss();root.dismiss();
        }
        assertEquals(2,backupInventory);assertEquals(2,updateInventory);
    }
    @Test public void backCancelAndRepeatedOpenPreserveOneParentMenu()throws Exception{
        AlertDialog root=open("ru");row(root,"workspace-global-settings",4).performClick();AlertDialog advanced=latest();row(root,"workspace-global-settings",4).performClick();assertSame(advanced,latest());
        advanced.onBackPressed();idle();assertFalse(advanced.isShowing());assertTrue(root.isShowing());
        row(root,"workspace-global-settings",4).performClick();advanced=latest();advanced.cancel();idle();assertTrue(root.isShowing());assertFalse(advanced.isShowing());
        row(root,"workspace-global-settings",4).performClick();advanced=latest();root.dismiss();idle();assertFalse(root.isShowing());assertFalse(advanced.isShowing());
    }
    @Test public void lastAboutEntryKeepsExistingActivityDestination()throws Exception{
        for(String language:new String[]{"ru","en"}){
            AlertDialog root=open(language);row(root,"workspace-global-settings",5).performClick();idle();Intent intent=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(intent);assertEquals(SettingsActivity.class.getName(),intent.getComponent().getClassName());assertFalse(root.isShowing());assertNull(activity.getCurrentSession());
        }
    }
}
