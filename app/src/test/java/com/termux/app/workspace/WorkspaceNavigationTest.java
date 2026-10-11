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
        activity.getSharedPreferences("workspace-input", android.content.Context.MODE_PRIVATE).edit().clear().commit();
        WorkspaceController controller = new WorkspaceController(activity);
        assertFalse(activity.getTerminalView().onCheckIsTextEditor());
        assertNull(activity.getTerminalView().onCreateInputConnection(new android.view.inputmethod.EditorInfo()));
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller);
        store.commands.clear(); store.sessions.clear();
        ViewGroup nav = activity.findViewById(R.id.workspace_navigation);
        assertEquals(3, nav.getChildCount()); assertNull(findText(nav, "Сессии"));
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Недавние команды"));
        assertNull(findText(activity.findViewById(R.id.workspace_content), "Новая сессия"));
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
        terminalInput.setText("первая строка"); terminalInput.setSelection(terminalInput.length());
        android.view.inputmethod.EditorInfo inputInfo = new android.view.inputmethod.EditorInfo();
        android.view.inputmethod.InputConnection inputConnection = terminalInput.onCreateInputConnection(inputInfo);
        assertNotNull(inputConnection);
        assertEquals(android.view.inputmethod.EditorInfo.IME_ACTION_NONE,
            inputInfo.imeOptions & android.view.inputmethod.EditorInfo.IME_MASK_ACTION);
        inputConnection.commitText("\n", 1);
        assertEquals("первая строка\n", terminalInput.getText().toString());
        assertTrue(store.recentCommands.isEmpty());
        terminalInput.setText("первая строка\nвторая строка");
        assertEquals("первая строка\nвторая строка", terminalInput.getText().toString());
        terminalInput.setText("черновик");
        View commandPicker = findDescription(inputRow, "Вставить сохранённую команду");
        commandPicker.performClick(); shadowOf(Looper.getMainLooper()).idle();
        AlertDialog picker = ShadowAlertDialog.getLatestAlertDialog();
        picker.getListView().performItemClick(null, 0, 0);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("./gradlew assembleDebug", terminalInput.getText().toString());
        assertEquals(terminalInput.length(), terminalInput.getSelectionStart());
        assertNull(activity.getCurrentSession()); // Selection never launches a shell or sends input.
        findDescription(activity.findViewById(R.id.workspace_header), "Меню терминала").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null, 6, 6);
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(activity.getTerminalView().onCheckIsTextEditor());
        assertTrue(activity.getSharedPreferences("workspace-input", android.content.Context.MODE_PRIVATE)
            .getBoolean("interactive-input", false));
        findDescription(activity.findViewById(R.id.workspace_header), "Меню терминала").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null, 6, 6);
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(activity.getTerminalView().onCheckIsTextEditor());
        assertNull(activity.getTerminalView().onCreateInputConnection(new android.view.inputmethod.EditorInfo()));
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
        findText(nav, "Команды").performClick();
        findDescription(activity.findViewById(R.id.workspace_content), "Действия с командой Собрать APK").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog commandActions = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("Скопировать команду", commandActions.getListView().getAdapter().getItem(2));
        commandActions.getListView().performItemClick(null, 2, 2);
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        assertEquals("./gradlew assembleDebug", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
        assertEquals(2, store.commands.size());
        assertNull(activity.getCurrentSession());
        assertTrue(store.sessions.isEmpty());
        findText(nav, "Команды").performClick();
        findText(activity.findViewById(R.id.workspace_content), "Избранное").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Команд пока нет"));
        findText(activity.findViewById(R.id.workspace_content), "Все команды").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        assertTrue(controller.onBack()); assertFalse(controller.onBack());
        store.recordCommand("echo recent");
        findText(nav, "Главная").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "echo recent"));
        EditText journalSearch = (EditText) findDescription(activity.findViewById(R.id.workspace_content), "Поиск по журналу");
        journalSearch.setText("not present");
        assertNull(findText(activity.findViewById(R.id.workspace_content), "echo recent"));
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Ничего не найдено"));
        journalSearch.setText("ECHO");
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "echo recent"));
        journalSearch.setText("");
        findDescription(activity.findViewById(R.id.workspace_content), "Действия с записью echo recent").performClick();
        android.app.Dialog sheet = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        findText(sheet.getWindow().getDecorView(), "Скопировать").performClick();
        assertEquals("echo recent", ((android.content.ClipboardManager) activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE))
            .getPrimaryClip().getItemAt(0).getText().toString());
        findDescription(activity.findViewById(R.id.workspace_content), "Действия с записью echo recent").performClick();
        sheet = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        findText(sheet.getWindow().getDecorView(), "Сохранить в команды").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog fromJournal = ShadowAlertDialog.getLatestAlertDialog();
        fields.clear(); collectEditors(fromJournal.getWindow().getDecorView(), fields);
        assertEquals("echo recent", fields.get(1).getText().toString());
        fromJournal.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle();
        findText(activity.findViewById(R.id.workspace_content), "echo recent").performClick();
        assertEquals("echo recent", terminalInput.getText().toString());
        assertEquals(1, store.recentCommands.size()); // Insertion is not a new sent command.
        findText(nav, "Главная").performClick();
        findText(activity.findViewById(R.id.workspace_content), "Очистить").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle(); store.awaitWrites();
        assertTrue(store.recentCommands.isEmpty()); assertEquals(2, store.commands.size());
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Сохранённые сессии"));
        java.lang.reflect.Method saveSession = WorkspaceController.class.getDeclaredMethod("editSession", WorkspaceStore.Session.class, boolean.class);
        saveSession.setAccessible(true); saveSession.invoke(controller, null, false);
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog sessionEditor = ShadowAlertDialog.getLatestAlertDialog();
        fields.clear(); collectEditors(sessionEditor.getWindow().getDecorView(), fields);
        findDescription(sessionEditor.getWindow().getDecorView(), "Среда запуска").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog environmentChooser = ShadowAlertDialog.getLatestAlertDialog();
        environmentChooser.getListView().performItemClick(null, 2, 2);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("/root", fields.get(1).getText().toString());
        fields.get(0).setText("Codex / Ubuntu"); fields.get(2).setText("codex resume");
        findDescription(sessionEditor.getWindow().getDecorView(), "Добавить команду запуска").performClick();
        findDescription(sessionEditor.getWindow().getDecorView(), "Добавить команду запуска").performClick();
        fields.clear(); collectEditors(sessionEditor.getWindow().getDecorView(), fields);
        assertEquals(5, fields.size()); fields.get(3).setText("echo second"); fields.get(4).setText("echo third");
        assertEquals(View.GONE, findDescription(sessionEditor.getWindow().getDecorView(), "Добавить команду запуска").getVisibility());
        sessionEditor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle(); store.awaitWrites();
        assertEquals(1, store.sessions.size()); assertNull(activity.getCurrentSession());
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Codex / Ubuntu"));
        assertEquals("ubuntu", store.sessions.get(0).environment);
        assertEquals(3, store.sessions.get(0).startupCommands().size());
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "codex resume → echo second → echo third"));
        for (int i = 0; i < 20; i++) {
            store.recordCommand("long journal command " + i);
            WorkspaceStore.Session extra = new WorkspaceStore.Session(); extra.name = "Saved " + i; store.sessions.add(extra);
        }
        findText(nav, "Главная").performClick();
        ViewGroup homeContent = activity.findViewById(R.id.workspace_content);
        assertTrue(homeContent.getChildAt(0) instanceof android.widget.LinearLayout);
        homeContent.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY));
        homeContent.layout(0, 0, 1080, 1600);
        android.widget.ScrollView journalPanel = (android.widget.ScrollView) findDescription(homeContent, "Журнал последних 25 команд");
        android.widget.ScrollView sessionsPanel = (android.widget.ScrollView) findDescription(homeContent, "Список сохранённых сессий");
        assertTrue(journalPanel.getHeight() > 0); assertTrue(sessionsPanel.getHeight() > 0);
        assertTrue(journalPanel.getChildAt(0).getHeight() > journalPanel.getHeight());
        assertTrue(sessionsPanel.getChildAt(0).getHeight() > sessionsPanel.getHeight());
        journalPanel.scrollTo(0, 100); assertEquals(100, journalPanel.getScrollY()); assertEquals(0, sessionsPanel.getScrollY());
        sessionsPanel.scrollTo(0, 100); assertEquals(100, sessionsPanel.getScrollY()); assertEquals(100, journalPanel.getScrollY());
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
