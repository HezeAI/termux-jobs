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
    View open(String language)throws Exception{WorkspaceLanguage.select(activity,language);WorkspaceLanguage.apply(activity);Method menu=WorkspaceController.class.getDeclaredMethod("globalSettings");menu.setAccessible(true);menu.invoke(controller);idle();return activity.findViewById(R.id.workspace_content).findViewWithTag("workspace-global-settings");}
    TextView text(View v,String wanted){if(v instanceof TextView&&wanted.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++){TextView found=text(((ViewGroup)v).getChildAt(n),wanted);if(found!=null)return found;}return null;}
    String allText(View v){String result=v instanceof TextView?((TextView)v).getText().toString()+"\n":"";if(v instanceof ViewGroup)for(int n=0;n<((ViewGroup)v).getChildCount();n++)result+=allText(((ViewGroup)v).getChildAt(n));return result;}
    void click(View root,String label){View row=text(root,label);assertNotNull(label,row);while(!row.isClickable())row=(View)row.getParent();row.performClick();idle();}
    @Test public void settingsAreScrollContentUnderCommonHeaderWithGroupedCardsInBothLanguages()throws Exception{
        for(String language:new String[]{"ru","en"}){boolean ru=language.equals("ru");AlertDialog before=ShadowAlertDialog.getLatestAlertDialog();View root=open(language);assertNotNull(root);assertSame(before,ShadowAlertDialog.getLatestAlertDialog());assertTrue(root.getParent() instanceof ScrollView);assertNull(text(root,ru?"Закрыть":"Close"));String all=allText(root);String[] names=ru?new String[]{"ВНЕШНИЙ ВИД","АГЕНТЫ И СРЕДА","ДАННЫЕ","Дополнительно","О приложении"}:new String[]{"APPEARANCE","AGENTS AND ENVIRONMENT","DATA","Advanced","About"};int last=-1;for(String name:names){int next=all.indexOf(name);assertTrue(name,next>last);last=next;}TextView agent=text(root,ru?"Агенты и модели":"Agents and models"),instructions=text(root,ru?"Общие инструкции агентов":"Shared agent instructions");assertSame(agent.getParent().getParent().getParent(),instructions.getParent().getParent().getParent());assertNotNull(root.findViewWithTag("session-text-size-preview"));assertNotNull(text(root,ru?"Русский":"English"));assertNotNull(text(root,"50%"));assertNotNull(text(root,"150%"));
            click(root,ru?"Дополнительно":"Advanced");View advanced=activity.findViewById(R.id.workspace_content).findViewWithTag("workspace-advanced-settings");assertNotNull(advanced);assertTrue(allText(advanced).contains(ru?"Недавние команды":"Recent commands"));assertFalse(allText(advanced).contains(ru?"Пресеты":"Presets"));assertTrue(controller.onBack());assertNotNull(activity.findViewById(R.id.workspace_content).findViewWithTag("workspace-global-settings"));assertTrue(controller.onBack());assertEquals(View.GONE,activity.findViewById(R.id.workspace_content).getVisibility());assertNull(activity.getCurrentSession());}
        assertEquals(0,backupInventory);assertEquals(0,updateInventory);
    }
    @Test public void transferredActionsOpenActualWindowsAndReturnWithoutMaintenance()throws Exception{
        for(String language:new String[]{"ru","en"}){View root=open(language);String[] names=language.equals("ru")?new String[]{"Резервное копирование","Проверить обновления окружений","Очистить хранилище","Ошибки и решения"}:new String[]{"Backup and restore","Check environment updates","Clean up storage","Errors and solutions"};for(int n=0;n<names.length;n++){click(root,names[n]);AlertDialog child=latest();assertTrue(child.isShowing());String[][] expected={{"Создать резервную копию","Проверить пакеты всех окружений","Ничего не отмечено заранее.","Ошибки и решения"},{"Create backup","Check packages in all environments","Nothing is preselected.","Errors and solutions"}};assertTrue(allText(child.getWindow().getDecorView()).contains(expected[language.equals("ru")?0:1][n]));child.getButton(-2).performClick();idle();assertFalse(child.isShowing());assertSame(root,activity.findViewById(R.id.workspace_content).findViewWithTag("workspace-global-settings"));assertNull(activity.getCurrentSession());}}
        assertEquals(2,backupInventory);assertEquals(2,updateInventory);
    }
    @Test public void lastAboutEntryKeepsExistingActivityDestination()throws Exception{for(String language:new String[]{"ru","en"}){View root=open(language);click(root,language.equals("ru")?"О приложении":"About");Intent intent=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(intent);assertEquals(SettingsActivity.class.getName(),intent.getComponent().getClassName());assertSame(root,activity.findViewById(R.id.workspace_content).findViewWithTag("workspace-global-settings"));assertNull(activity.getCurrentSession());}}
}
