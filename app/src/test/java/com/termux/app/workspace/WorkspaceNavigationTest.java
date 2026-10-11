package com.termux.app.workspace;

import android.app.AlertDialog;
import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Inflate only the UI: never start TermuxService, bootstrap, JNI, or real shells. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class WorkspaceNavigationTest {
    @Test public void navigationEditorFiltersAndPersistentLibrary() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black);
        activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true);
        terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller);
        store.commands.clear(); store.sessions.clear();
        ViewGroup nav = activity.findViewById(R.id.workspace_navigation);
        findText(nav, "Команды").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Команд пока нет"));
        findDescription(activity.findViewById(R.id.workspace_header), "Новая команда").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog editor = ShadowAlertDialog.getLatestAlertDialog(); assertTrue(editor.isShowing());
        List<EditText> fields = new ArrayList<>(); collectEditors(editor.getWindow().getDecorView(), fields);
        assertEquals(3, fields.size());
        fields.get(0).setText("Собрать APK"); fields.get(1).setText("./gradlew assembleDebug");
        fields.get(2).setText("Сборка");
        editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(editor.isShowing()); store.awaitWrites();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        ViewGroup inputRow = activity.findViewById(R.id.workspace_input);
        EditText terminalInput = (EditText) inputRow.getChildAt(0);
        terminalInput.setText("черновик");
        View commandPicker = findDescription(inputRow, "Вставить сохранённую команду");
        commandPicker.performClick(); shadowOf(Looper.getMainLooper()).idle();
        AlertDialog picker = ShadowAlertDialog.getLatestAlertDialog();
        picker.getListView().performItemClick(null, 0, 0);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("./gradlew assembleDebug", terminalInput.getText().toString());
        assertEquals(terminalInput.length(), terminalInput.getSelectionStart());
        assertNull(activity.getCurrentSession()); // Selection never launches a shell or sends input.
        terminalInput.setText("printf 'hello'");
        commandPicker.performClick(); shadowOf(Looper.getMainLooper()).idle();
        picker = ShadowAlertDialog.getLatestAlertDialog();
        picker.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog saveInput = ShadowAlertDialog.getLatestAlertDialog();
        fields.clear(); collectEditors(saveInput.getWindow().getDecorView(), fields);
        assertEquals("printf 'hello'", fields.get(1).getText().toString());
        fields.get(0).setText("Привет");
        saveInput.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle(); store.awaitWrites();
        assertEquals(2, store.commands.size());
        assertEquals("printf 'hello'", store.commands.get(1).command);
        assertEquals("printf 'hello'", terminalInput.getText().toString());
        findText(nav, "Команды").performClick();
        findDescription(activity.findViewById(R.id.workspace_content), "Вставить Собрать APK").performClick();
        assertEquals("./gradlew assembleDebug", terminalInput.getText().toString());
        assertNull(activity.getCurrentSession());
        assertTrue(store.sessions.isEmpty());
        findText(nav, "Команды").performClick();
        findText(activity.findViewById(R.id.workspace_content), "Избранное").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Команд пока нет"));
        findText(activity.findViewById(R.id.workspace_content), "Все команды").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        findText(nav, "Сессии").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Нет активных сессий"));
        assertTrue(controller.onBack()); assertFalse(controller.onBack());
        controller.close();
        WorkspaceStore reopened = new WorkspaceStore(activity, message -> fail(message));
        assertEquals("./gradlew assembleDebug", reopened.commands.get(0).command);
        assertEquals("", reopened.commands.get(0).directory); reopened.close();
    }
    private static View findText(View root, String value) {
        if (root instanceof TextView && ((TextView) root).getText().toString().equals(value)) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View found = findText(((ViewGroup) root).getChildAt(i), value); if (found != null) return found;
        }
        return null;
    }
    private static View findDescription(View root, String value) {
        if (value.equals(root.getContentDescription())) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View found = findDescription(((ViewGroup) root).getChildAt(i), value); if (found != null) return found;
        }
        return null;
    }
    private static void collectEditors(View root, List<EditText> fields) {
        if (root instanceof EditText) fields.add((EditText) root);
        else if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) collectEditors(((ViewGroup) root).getChildAt(i), fields);
    }
}
