package com.termux.app.workspace;
import android.app.AlertDialog;
import android.app.Application;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import android.os.Looper;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ClaudeCodeUiTest {
    private View find(View root,String text){if(root.getVisibility()!=View.VISIBLE)return null;if(root instanceof TextView&&((TextView)root).getText().toString().equals(text))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=find(((ViewGroup)root).getChildAt(i),text);if(v!=null)return v;}return null;}
    @Test public void editingClaudeProvidesExactlyThreeChatChoicesAndPersistsMode() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try {
            WorkspaceStore.Session s=new WorkspaceStore.Session();s.claudeAgent=true;s.environment="ubuntu";s.name="Claude Code";s.directory="/root";
            Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);((WorkspaceStore)f.get(c)).sessions.add(s);
            Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(c,s,false);shadowOf(Looper.getMainLooper()).idle();
            AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();View chat=find(editor.getWindow().getDecorView(),WorkspaceText.tr("Чат: ")+WorkspaceText.tr("Новый чат"));assertNotNull(chat);chat.performClick();shadowOf(Looper.getMainLooper()).idle();
            AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,picker.getListView().getCount());assertEquals(WorkspaceText.tr("Выбрать чат"),picker.getListView().getItemAtPosition(1));
            picker.getListView().performItemClick(null,1,1);shadowOf(Looper.getMainLooper()).idle();assertNotNull(find(editor.getWindow().getDecorView(),WorkspaceText.tr("Чат: ")+WorkspaceText.tr("Выбрать чат")));
            View mode=find(editor.getWindow().getDecorView(),(WorkspaceLanguage.code(a).equals("ru")?"Права: ":"Permissions: ")+WorkspaceText.tr("Подтверждать действия"));assertNotNull(mode);mode.performClick();shadowOf(Looper.getMainLooper()).idle();
            picker=ShadowAlertDialog.getLatestAlertDialog();picker.getListView().performItemClick(null,2,2);shadowOf(Looper.getMainLooper()).idle();
            editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(s.claudeAgent);assertEquals("choose",s.claudeChat);assertEquals("plan",s.claudeMode);assertEquals("ubuntu",s.environment);assertEquals("",s.aiAgent);
        } finally {c.close();}
    }
}
