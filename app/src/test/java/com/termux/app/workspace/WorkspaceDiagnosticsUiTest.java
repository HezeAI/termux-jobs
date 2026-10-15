package com.termux.app.workspace;
import android.app.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceDiagnosticsUiTest {
    private View find(View root,String text){if(root instanceof TextView&&((TextView)root).getText().toString().equals(text))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View child=find(((ViewGroup)root).getChildAt(i),text);if(child!=null)return child;}return null;}
    @Test public void editorsOfferFlagAndDialogPauseSurvivesWindowClose() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"ru");a.setContentView(R.layout.activity_termux);
        Field view=TermuxActivity.class.getDeclaredField("mTerminalView");view.setAccessible(true);view.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try{
            Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);
            for(boolean launch:new boolean[]{false,true}){edit.invoke(c,null,launch);shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View check=find(dialog.getWindow().getDecorView(),WorkspaceText.tr("Расширенный дебаг-журнал"));assertTrue(check instanceof CheckBox);assertFalse(((CheckBox)check).isChecked());dialog.dismiss();}
            WorkspaceStore.Session steps=new WorkspaceStore.Session();steps.environment="ubuntu";steps.startup="echo first";steps.startup2="echo second";SessionShell.prepare(a.getFilesDir(),steps);
            Method cancel=WorkspaceController.class.getDeclaredMethod("confirmCancelStartup",WorkspaceStore.Session.class);cancel.setAccessible(true);cancel.invoke(c,steps);shadowOf(Looper.getMainLooper()).idle();AlertDialog confirmation=ShadowAlertDialog.getLatestAlertDialog();
            java.io.File marker=new java.io.File(SessionShell.directory(a.getFilesDir(),steps.id),"cancel-startup");assertFalse(marker.exists());confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(marker.exists());
            cancel.invoke(c,steps);shadowOf(Looper.getMainLooper()).idle();confirmation=ShadowAlertDialog.getLatestAlertDialog();confirmation.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(marker.isFile());
            WorkspaceStore.Session s=new WorkspaceStore.Session();WorkspaceDiagnostics.register(s,true);
            WorkspaceDiagnosticsUi.show(a,s,()->null);shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(find(dialog.getWindow().getDecorView(),WorkspaceText.tr("Остановить журнал")).performClick());assertTrue(WorkspaceDiagnostics.paused(s.id));dialog.dismiss();
            WorkspaceDiagnosticsUi.show(a,s,()->null);shadowOf(Looper.getMainLooper()).idle();dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(find(dialog.getWindow().getDecorView(),"Возобновить журнал"));assertNotNull(find(dialog.getWindow().getDecorView(),"Скопировать путь"));assertNotNull(find(dialog.getWindow().getDecorView(),"В Download"));dialog.dismiss();
        }finally{c.close();}
    }

    @Test public void runtimeModesWorkForTermuxUbuntuSshAndAiWithoutChangingLaunchConfiguration(){
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"ru");
        for(String environment:new String[]{"termux","ubuntu","ssh","codex"}){WorkspaceStore.Session session=new WorkspaceStore.Session();session.environment=environment;session.extendedDebug=false;WorkspaceDiagnostics.register(session,true);WorkspaceDiagnosticsUi.show(a,session,()->null);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(find(d.getWindow().getDecorView(),"Подробный"));find(d.getWindow().getDecorView(),"Подробный").performClick();assertTrue(WorkspaceDiagnostics.extended(session.id));assertFalse(session.extendedDebug);find(d.getWindow().getDecorView(),"Обычный").performClick();assertFalse(WorkspaceDiagnostics.extended(session.id));d.dismiss();}
        WorkspaceLanguage.select(a,"en");WorkspaceStore.Session session=new WorkspaceStore.Session();WorkspaceDiagnostics.register(session,true);WorkspaceDiagnosticsUi.show(a,session,()->null);AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(find(d.getWindow().getDecorView(),"Detailed"));assertNotNull(find(d.getWindow().getDecorView(),"Stop journal"));d.dismiss();
    }
}
