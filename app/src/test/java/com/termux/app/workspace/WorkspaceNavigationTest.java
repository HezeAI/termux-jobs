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
import com.termux.shared.shell.TermuxSession;
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
    private static void call(WorkspaceController c,String method,Class<?>[] types,Object... args)throws Exception{java.lang.reflect.Method m=WorkspaceController.class.getDeclaredMethod(method,types);m.setAccessible(true);m.invoke(c,args);}
    private static void navigate(WorkspaceController c,String section)throws Exception{
        if(section.equals("Команды/Заметки")){call(c,"openLibrary",new Class[]{int.class},0);return;}
        Class<?> screen=Class.forName("com.termux.app.workspace.WorkspaceController$Screen");call(c,"show",new Class[]{screen,boolean.class},Enum.valueOf((Class)screen,section.equals("Терминал")?"TERMINAL":"HOME"),false);
    }
    private static EditText composer(TermuxActivity a){return editorIn(a.findViewById(R.id.workspace_input));}
    private static EditText editorIn(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText e=editorIn(((ViewGroup)v).getChildAt(i));if(e!=null)return e;}return null;}
    private static void picker(WorkspaceController c,int section)throws Exception{call(c,"chooseInputCommand",new Class[]{int.class},section);shadowOf(Looper.getMainLooper()).idle();}
    private static void sessionMenu(WorkspaceController c,WorkspaceStore.Session p)throws Exception{call(c,"sessionMenu",new Class[]{WorkspaceStore.Session.class,boolean.class},p,true);shadowOf(Looper.getMainLooper()).idle();}
    @org.junit.Before public void useRussianUi(){WorkspaceLanguage.select(org.robolectric.RuntimeEnvironment.application,"ru");WorkspaceText.resources=WorkspaceLanguage.wrap(org.robolectric.RuntimeEnvironment.application).getResources();}
    private static void openLibrary(WorkspaceController c,int section)throws Exception {java.lang.reflect.Method m=WorkspaceController.class.getDeclaredMethod("openLibrary",int.class);m.setAccessible(true);m.invoke(c,section);}
    @Test public void separateLibrariesKeepTheirOwnSearchAndStableResultViewport()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try{Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore store=(WorkspaceStore)f.get(c);store.commands.clear();store.notes.clear();store.aliases.clear();
            for(String name:new String[]{"Alpha","Beta"}){WorkspaceStore.Command cmd=new WorkspaceStore.Command();cmd.name=name+" command";cmd.command="echo "+name;store.commands.add(cmd);WorkspaceStore.Note note=new WorkspaceStore.Note();note.name=name+" note";note.text=name;store.notes.add(note);WorkspaceStore.Alias alias=new WorkspaceStore.Alias();alias.name=name+" alias";alias.command=name;store.aliases.add(alias);}
            String[] kinds={"command","alias","note"};
            for(int section=0;section<3;section++){call(c,"openLibrary",new Class[]{int.class},section);ViewGroup content=a.findViewById(R.id.workspace_content);View viewport=content.getChildAt(0);EditText search=(EditText)findDescription(a.findViewById(R.id.workspace_header),"Поиск в библиотеке");search.requestFocus();
                for(String query:new String[]{"Alpha","missing",""}){search.setText(query);assertSame(viewport,content.getChildAt(0));assertTrue(search.hasFocus());assertEquals(query,search.getText().toString());for(int other=0;other<3;other++){assertEquals(other==section&&!query.equals("missing"),findText(content,"Alpha "+kinds[other])!=null);assertEquals(other==section&&query.isEmpty(),findText(content,"Beta "+kinds[other])!=null);}}
                search.setText(kinds[section]);
            }
            for(int section=0;section<3;section++){call(c,"openLibrary",new Class[]{int.class},section);assertEquals(kinds[section],((EditText)findDescription(a.findViewById(R.id.workspace_header),"Поиск в библиотеке")).getText().toString());}assertNull(a.getCurrentSession());
        }finally{c.close();}
    }
    @Test public void categorySuggestionsAllowExistingAndNewValuesInBothEditors() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController(activity);Field sf=WorkspaceController.class.getDeclaredField("store");sf.setAccessible(true);WorkspaceStore store=(WorkspaceStore)sf.get(controller);store.commands.clear();store.notes.clear();
        WorkspaceStore.Command command=new WorkspaceStore.Command();command.name="Existing";command.command="echo test";command.category="Build";store.commands.add(command);WorkspaceStore.Note note=new WorkspaceStore.Note();note.name="Existing note";note.category="Docs";store.notes.add(note);
        for(boolean notes:new boolean[]{false,true}){
            java.lang.reflect.Method edit=WorkspaceController.class.getDeclaredMethod(notes?"editNote":"editCommand",notes?WorkspaceStore.Note.class:WorkspaceStore.Command.class);edit.setAccessible(true);edit.invoke(controller,new Object[]{null});shadowOf(Looper.getMainLooper()).idle();
            AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();List<EditText> fields=new ArrayList<>();collectEditors(dialog.getWindow().getDecorView(),fields);assertEquals(3,fields.size());assertTrue(fields.get(2) instanceof android.widget.AutoCompleteTextView);
            android.widget.AutoCompleteTextView category=(android.widget.AutoCompleteTextView)fields.get(2);boolean found=false;for(int i=0;i<category.getAdapter().getCount();i++)if(category.getAdapter().getItem(i).equals(notes?"Docs":"Build"))found=true;assertTrue(found);
            category.setText("unmatched text",false);findDescription(dialog.getWindow().getDecorView(),"Выбрать категорию").performClick();shadowOf(Looper.getMainLooper()).idle();
            AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();assertEquals(1,picker.getListView().getCount());assertEquals(notes?"Docs":"Build",picker.getListView().getItemAtPosition(0));picker.getListView().performItemClick(null,0,0);shadowOf(Looper.getMainLooper()).idle();assertEquals(notes?"Docs":"Build",category.getText().toString());
            fields.get(0).setText(notes?"New note":"New command");fields.get(1).setText("echo test");category.setText("New category",false);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();store.awaitWrites();assertEquals("New category",notes?store.notes.get(1).category:store.commands.get(1).category);
        }controller.close();
    }

    @Test public void terminalComposerTogglesReflectEnabledStateAcrossViews() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try {navigate(c,"Терминал");View input=a.findViewById(R.id.workspace_input);View direct=findDescription(input,"Интерактивный ввод"),keys=findDescription(input,"Дополнительные клавиши");assertNotNull(direct);assertNotNull(keys);assertNull(findDescription(a.findViewById(R.id.workspace_header),"Интерактивный ввод"));boolean before=((android.widget.Switch)direct).isChecked();direct.performClick();assertEquals(!before,((android.widget.Switch)direct).isChecked());navigate(c,"Команды/Заметки");navigate(c,"Терминал");assertEquals(!before,((android.widget.Switch)findDescription(input,"Интерактивный ввод")).isChecked());}finally{c.close();}
    }

    @Test public void commonSettingsIncludeLanguageOnAllThreeTabs() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceLanguage.select(a,"ru");WorkspaceController c=new WorkspaceController(a);
        try{for(String label:new String[]{"Главная","Команды/Заметки","Терминал"}){
            navigate(c,label);View header=a.findViewById(R.id.workspace_header);assertNull(findDescription(header,"Язык / Language"));
            call(c,"globalSettings",new Class[]{});shadowOf(Looper.getMainLooper()).idle();View page=a.findViewById(R.id.workspace_content);for(String item:new String[]{"Язык интерфейса","Резервное копирование"})assertNotNull(findText(page,item));assertNull(findText(page,"Дополнительно"));assertNull(findText(page,"О приложении"));assertNotNull(page.findViewWithTag("automatic-app-updates"));assertTrue(c.onBack());
        }}finally{c.close();}
    }

    @Test public void packagesShortcutReplacesHeldMenuEntryIncludingBase() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(activity,activity.findViewById(R.id.terminal_view));
        WorkspaceController controller=new WorkspaceController(activity);
        java.lang.reflect.Method menu=WorkspaceController.class.getDeclaredMethod("sessionMenu",WorkspaceStore.Session.class,boolean.class);menu.setAccessible(true);
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.name="Held Ubuntu";profile.environment="ubuntu";profile.saved=true;
        menu.invoke(controller,profile,true);AlertDialog held=ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("Held Ubuntu",shadowOf(held).getTitle());boolean packages=false;for(int i=0;i<held.getListView().getCount();i++)if(held.getListView().getItemAtPosition(i).equals("Пакеты"))packages=true;assertFalse(packages);held.dismiss();
        menu.invoke(controller,profile,false);AlertDialog home=ShadowAlertDialog.getLatestAlertDialog();for(int i=0;i<home.getListView().getCount();i++)assertNotEquals("Пакеты",home.getListView().getItemAtPosition(i));home.dismiss();
        profile.id=WorkspaceStore.BASE_TERMINAL_ID;menu.invoke(controller,profile,true);AlertDialog base=ShadowAlertDialog.getLatestAlertDialog();assertTrue(base.getButton(AlertDialog.BUTTON_NEUTRAL)==null || base.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility()==View.GONE);base.dismiss();controller.close();
    }

    @Test public void categoryFiltersAreExclusiveAndFolderPickerUsesVector() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminal = TermuxActivity.class.getDeclaredField("mTerminalView"); terminal.setAccessible(true);
        terminal.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field sf = WorkspaceController.class.getDeclaredField("store"); sf.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) sf.get(controller);
        WorkspaceStore.Command command = new WorkspaceStore.Command(); command.name="Test"; command.category="AI"; store.commands.add(command);
        WorkspaceStore.Note note = new WorkspaceStore.Note(); note.name="Test note"; note.category="AI"; store.notes.add(note);
        navigate(controller,"Команды/Заметки");
        for (String prefix : new String[]{"command", "note"}) {
            Field category = WorkspaceController.class.getDeclaredField(prefix+"Category"); category.setAccessible(true); category.set(controller,"AI");
            Field favorite = WorkspaceController.class.getDeclaredField(prefix.equals("note")?"noteFavorites":"favorites"); favorite.setAccessible(true); favorite.setBoolean(controller,true);
        }
        java.lang.reflect.Method render = WorkspaceController.class.getDeclaredMethod("renderContent"); render.setAccessible(true); render.invoke(controller);
        View root = activity.findViewById(R.id.workspace_content);
        assertFalse(findText(root,"Все команды").isSelected());assertNull(findText(root,"Все заметки"));
        findText(root,"Все команды").performClick(); root=activity.findViewById(R.id.workspace_content);
        assertTrue(findText(root,"Все команды").isSelected());
        assertFalse(findDescription(root,"Фильтр категорий команд").isSelected());
        call(controller,"openLibrary",new Class[]{int.class},2);root=activity.findViewById(R.id.workspace_content);assertFalse(findText(root,"Все заметки").isSelected());
        findText(root,"Все заметки").performClick(); root=activity.findViewById(R.id.workspace_content);
        assertTrue(findText(root,"Все заметки").isSelected()); assertFalse(findDescription(root,"Фильтр категорий заметок").isSelected());
        java.lang.reflect.Method edit = WorkspaceController.class.getDeclaredMethod("editSession", WorkspaceStore.Session.class); edit.setAccessible(true); edit.invoke(controller,new Object[]{null});
        shadowOf(Looper.getMainLooper()).idle();
        View picker=findDescription(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView(),"Выбрать рабочий каталог");
        Field icon=WorkspaceIconButton.class.getDeclaredField("icon"); icon.setAccessible(true); assertNotNull(icon.get(picker));
        assertEquals("",((TextView)picker).getText().toString());
    }

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
        assertNull(nav);assertNotNull(activity.findViewById(R.id.workspace_header).findViewWithTag("workspace-menu"));
        assertEquals(View.GONE,activity.findViewById(R.id.workspace_content).getVisibility());
        assertNull(findText(activity.findViewById(R.id.workspace_content), "Новая сессия"));
        navigate(controller,"Команды/Заметки");
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Команд пока нет"));
        findDescription(activity.findViewById(R.id.workspace_content), "Новая команда").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog editor = ShadowAlertDialog.getLatestAlertDialog(); assertTrue(editor.isShowing());
        List<EditText> fields = new ArrayList<>(); collectEditors(editor.getWindow().getDecorView(), fields);
        assertEquals(3, fields.size());
        fields.get(0).setText("Собрать APK"); fields.get(1).setText("./gradlew assembleDebug");
        fields.get(2).setText("Сборка");
        editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        store.awaitWrites(); shadowOf(Looper.getMainLooper()).idle(); assertFalse(editor.isShowing());
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        EditText commandSearch = (EditText) findDescription(activity.findViewById(R.id.workspace_header), "Поиск в библиотеке");
        assertNotNull(commandSearch);
        assertNull(findText(activity.findViewById(R.id.workspace_content), "Все"));
        commandSearch.setText("не найдено");
        assertNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        commandSearch.setText("APK");
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        assertSame(commandSearch, findDescription(activity.findViewById(R.id.workspace_header), "Поиск в библиотеке"));
        assertEquals("APK", commandSearch.getText().toString());
        commandSearch.setText("gradlew");
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        commandSearch.setText("");
        ViewGroup inputRow = activity.findViewById(R.id.workspace_input);
        EditText terminalInput = composer(activity);
        assertNotNull(findDescription(inputRow,"Вставить сохранённую команду"));
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
        assertEquals("черновик", terminalInput.getText().toString());
        AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(findText(preview.getWindow().getDecorView(),"./gradlew assembleDebug"));
        preview.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();
        assertEquals("./gradlew assembleDebug", terminalInput.getText().toString());
        assertEquals(terminalInput.length(), terminalInput.getSelectionStart());
        assertNull(activity.getCurrentSession()); // Selection never launches a shell or sends input.
        assertNull(findDescription(activity.findViewById(R.id.workspace_header), "Меню терминала"));
        findDescription(activity.findViewById(R.id.workspace_input),"Интерактивный ввод").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(activity.getTerminalView().onCheckIsTextEditor());
        assertTrue(activity.getSharedPreferences("workspace-input", android.content.Context.MODE_PRIVATE)
            .getBoolean("interactive-input", false));
        findDescription(activity.findViewById(R.id.workspace_input),"Интерактивный ввод").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertFalse(activity.getTerminalView().onCheckIsTextEditor());
        assertNull(activity.getTerminalView().onCreateInputConnection(new android.view.inputmethod.EditorInfo()));
        terminalInput.setText("printf 'hello'");
        commandPicker.performClick(); shadowOf(Looper.getMainLooper()).idle();
        picker = ShadowAlertDialog.getLatestAlertDialog();
        assertFalse(picker.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility()==View.VISIBLE);picker.dismiss();
        call(controller,"editCommand",new Class[]{WorkspaceStore.Command.class},new Object[]{null});shadowOf(Looper.getMainLooper()).idle();
        AlertDialog saveInput = ShadowAlertDialog.getLatestAlertDialog();
        fields.clear(); collectEditors(saveInput.getWindow().getDecorView(), fields);
        assertEquals("printf 'hello'",terminalInput.getText().toString());fields.get(1).setText("printf 'hello'");
        fields.get(0).setText("Привет");
        saveInput.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle(); store.awaitWrites();
        assertEquals(2, store.commands.size());
        assertEquals("printf 'hello'", store.commands.get(1).command);
        assertEquals("printf 'hello'", terminalInput.getText().toString());
        navigate(controller,"Команды/Заметки");
        findDescription(activity.findViewById(R.id.workspace_content), "Фильтр категорий команд").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog categories = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(findText(categories.getWindow().getDecorView(),"Все категории"));
        categories.getListView().performItemClick(null,1,1);
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        assertNull(findText(activity.findViewById(R.id.workspace_content), "Привет"));
        findDescription(activity.findViewById(R.id.workspace_content), "Фильтр категорий команд").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,0,0);
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Привет"));
        assertNull(findDescription(activity.findViewById(R.id.workspace_content), "Вставить Собрать APK"));
        ((View) findText(activity.findViewById(R.id.workspace_content), "Собрать APK").getParent().getParent()).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        findText(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView(), "Вставить в терминал").performClick();
        assertEquals("./gradlew assembleDebug", terminalInput.getText().toString());
        navigate(controller,"Команды/Заметки");
        assertNull(findDescription(activity.findViewById(R.id.workspace_content),"Действия с командой Собрать APK"));
        findDescription(activity.findViewById(R.id.workspace_content),"Команда Собрать APK").performClick();shadowOf(Looper.getMainLooper()).idle();
        AlertDialog commandActions=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(findText(commandActions.getWindow().getDecorView(),"Редактировать"));assertNotNull(findText(commandActions.getWindow().getDecorView(),"Удалить"));
        findText(commandActions.getWindow().getDecorView(),"В избранное").performClick();assertTrue(store.commands.get(0).favorite);
        findDescription(activity.findViewById(R.id.workspace_content),"Команда Собрать APK").performClick();shadowOf(Looper.getMainLooper()).idle();commandActions=ShadowAlertDialog.getLatestAlertDialog();
        findText(commandActions.getWindow().getDecorView(),"Убрать из избранного").performClick();
        assertFalse(store.commands.get(0).favorite);
        assertEquals(2, store.commands.size());
        assertNull(activity.getCurrentSession());
        assertTrue(store.sessions.isEmpty());
        openLibrary(controller,1);
        findText(activity.findViewById(R.id.workspace_content), "Все Alias").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Aliases пока нет"));
        openLibrary(controller,0);
        findText(activity.findViewById(R.id.workspace_content), "Все команды").performClick();
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Собрать APK"));
        assertTrue(controller.onBack()); assertFalse(controller.onBack());
        store.recordCommand("echo recent");
        navigate(controller,"Главная");
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "echo recent"));
        EditText journalSearch = (EditText) findDescription(activity.findViewById(R.id.workspace_content), "Поиск по журналу");
        journalSearch.setText("not present");
        assertNull(findText(activity.findViewById(R.id.workspace_content), "echo recent"));
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "Ничего не найдено"));
        journalSearch.setText("ECHO");
        assertNotNull(findText(activity.findViewById(R.id.workspace_content), "echo recent"));
        journalSearch.setText("");
        assertNull(findDescription(activity.findViewById(R.id.workspace_content), "Действия с записью echo recent"));
        findText(activity.findViewById(R.id.workspace_content), "echo recent").performClick();
        android.app.Dialog sheet = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNull(findText(sheet.getWindow().getDecorView(), "Отмена"));
        findText(sheet.getWindow().getDecorView(), "Скопировать").performClick();
        assertEquals("echo recent", ((android.content.ClipboardManager) activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE))
            .getPrimaryClip().getItemAt(0).getText().toString());
        assertNull(findDescription(activity.findViewById(R.id.workspace_content), "Действия с записью echo recent"));
        findText(activity.findViewById(R.id.workspace_content), "echo recent").performClick();
        sheet = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        findText(sheet.getWindow().getDecorView(), "Сохранить в команды").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog fromJournal = ShadowAlertDialog.getLatestAlertDialog();
        fields.clear(); collectEditors(fromJournal.getWindow().getDecorView(), fields);
        assertEquals("echo recent", fields.get(1).getText().toString());
        fromJournal.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle();
        findText(activity.findViewById(R.id.workspace_content), "echo recent").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        findText(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView(), "Вставить в терминал").performClick();
        assertEquals("echo recent", terminalInput.getText().toString());
        assertEquals(1, store.recentCommands.size()); // Insertion is not a new sent command.
        navigate(controller,"Главная");
        findText(activity.findViewById(R.id.workspace_content), "Очистить").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle(); store.awaitWrites();
        assertTrue(store.recentCommands.isEmpty()); assertEquals(2, store.commands.size());
        assertNull(findText(activity.findViewById(R.id.workspace_content), "Пресеты запуска"));
        java.lang.reflect.Method saveSession = WorkspaceController.class.getDeclaredMethod("editSession", WorkspaceStore.Session.class, boolean.class);
        saveSession.setAccessible(true); saveSession.invoke(controller, null, false);
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog sessionEditor = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("Создать пресет", shadowOf(sessionEditor).getTitle());
        fields.clear(); collectEditors(sessionEditor.getWindow().getDecorView(), fields);
        findDescription(sessionEditor.getWindow().getDecorView(), "Среда запуска").performClick();
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog environmentChooser = ShadowAlertDialog.getLatestAlertDialog();
        environmentChooser.getListView().performItemClick(null, 0, 0);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH, fields.get(1).getText().toString());
        fields.get(0).setText("Codex / Ubuntu"); fields.get(2).setText("codex resume");
        findDescription(sessionEditor.getWindow().getDecorView(), "Добавить команду запуска").performClick();
        findDescription(sessionEditor.getWindow().getDecorView(), "Добавить команду запуска").performClick();
        fields.clear(); collectEditors(sessionEditor.getWindow().getDecorView(), fields);
        assertEquals(5, fields.size()); fields.get(3).setText("echo second"); fields.get(4).setText("echo third");
        assertEquals(View.GONE, findDescription(sessionEditor.getWindow().getDecorView(), "Добавить команду запуска").getVisibility());
        sessionEditor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle(); store.awaitWrites();
        shadowOf(Looper.getMainLooper()).idle(); // Process the acknowledged save callback before asserting the UI state.
        assertEquals(1, store.sessions.size()); assertTrue(store.sessions.get(0).saved); assertNull(activity.getCurrentSession());
        assertNull(findText(activity.findViewById(R.id.workspace_content), "Codex / Ubuntu"));
        assertEquals("termux", store.sessions.get(0).environment);
        assertEquals(3, store.sessions.get(0).startupCommands().size());
        assertEquals(java.util.Arrays.asList("codex resume","echo second","echo third"),store.sessions.get(0).startupCommands());
        for (int i = 0; i < 20; i++) {
            store.recordCommand("long journal command " + i);
            WorkspaceStore.Session extra = new WorkspaceStore.Session(); extra.name = "Saved " + i; extra.saved = true; store.sessions.add(extra);
        }
        navigate(controller,"Главная");
        ViewGroup homeContent = activity.findViewById(R.id.workspace_content);
        assertTrue(homeContent.getChildAt(0) instanceof AdaptiveWorkspaceLayout);
        homeContent.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
        homeContent.layout(0, 0, 1080, 800);
        android.widget.ScrollView journalPanel = (android.widget.ScrollView) findDescription(homeContent, "Журнал последних 50 команд");
        android.widget.ScrollView sessionsPanel = (android.widget.ScrollView) findDescription(homeContent, "Список пресетов запуска");
        assertTrue(journalPanel.getHeight() > 0); assertNull(sessionsPanel);
        assertTrue(journalPanel.getChildAt(0).getHeight() > journalPanel.getHeight());
        journalPanel.scrollTo(0, 100); assertEquals(100, journalPanel.getScrollY());
        java.lang.reflect.Method showSidebar=WorkspaceController.class.getDeclaredMethod("showSidebar");showSidebar.setAccessible(true);showSidebar.invoke(controller);
        android.app.Dialog drawer=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(findDescription(drawer.getWindow().getDecorView(),"Меню: Codex / Ubuntu"));assertNotNull(findDescription(drawer.getWindow().getDecorView(),"Меню: Saved 19"));drawer.dismiss();
        controller.close();
        WorkspaceStore reopened = new WorkspaceStore(activity, message -> fail(message));
        assertEquals("./gradlew assembleDebug", reopened.commands.get(0).command);
        assertEquals("", reopened.commands.get(0).directory); reopened.close();
    }
    @Test public void terminalTitleUpdatesKeepNavigationAndSearchViewsStable() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black);
        activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        Field visible = TermuxActivity.class.getDeclaredField("mIsVisible");
        visible.setAccessible(true); visible.setBoolean(activity, true);
        WorkspaceController controller = new WorkspaceController(activity);
        Field workspace = TermuxActivity.class.getDeclaredField("mWorkspace");
        workspace.setAccessible(true); workspace.set(activity, controller);
        com.termux.app.terminal.TermuxTerminalSessionClient client =
            new com.termux.app.terminal.TermuxTerminalSessionClient(activity);
        ViewGroup header = activity.findViewById(R.id.workspace_header);
        ViewGroup content = activity.findViewById(R.id.workspace_content);
        View originalHome = content.getChildAt(0);
        View originalNavigation = activity.findViewById(R.id.workspace_header).findViewWithTag("workspace-menu");
        for (int i = 0; i < 1000; i++) client.onTitleChanged(null);
        assertSame(originalHome, content.getChildAt(0));
        assertSame(originalNavigation, activity.findViewById(R.id.workspace_header).findViewWithTag("workspace-menu"));
        navigate(controller,"Команды/Заметки");
        EditText search = (EditText) findDescription(header, "Поиск в библиотеке");
        search.setText("codex"); search.requestFocus();
        View originalCommands = content.getChildAt(0);
        for (int i = 0; i < 1000; i++) client.onTitleChanged(null);
        assertSame(search, findDescription(header, "Поиск в библиотеке"));
        assertEquals("codex", search.getText().toString());
        assertSame(originalCommands, content.getChildAt(0));
        assertTrue(search.hasFocus());
        navigate(controller,"Главная");
        assertNotNull(findText(content, "Недавние команды"));
        controller.close();
    }
    @Test public void ordinaryTerminalNeedsExplicitSettingsSave() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field storeField = WorkspaceController.class.getDeclaredField("store"); storeField.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) storeField.get(controller); store.sessions.clear();
        com.termux.terminal.TerminalSession terminal = new com.termux.terminal.TerminalSession("/bin/bash", "/tmp", new String[0], new String[0], null, null);
        terminal.mSessionName = "Temporary Bash";
        com.termux.shared.models.ExecutionCommand command = new com.termux.shared.models.ExecutionCommand();
        command.workingDirectory = "/tmp";
        java.lang.reflect.Constructor<com.termux.shared.shell.TermuxSession> constructor =
            com.termux.shared.shell.TermuxSession.class.getDeclaredConstructor(com.termux.terminal.TerminalSession.class,
                com.termux.shared.models.ExecutionCommand.class, com.termux.shared.shell.TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        com.termux.app.TermuxService service = new com.termux.app.TermuxService();
        service.getTermuxSessions().add(constructor.newInstance(terminal, command, null, false));
        Field serviceField = TermuxActivity.class.getDeclaredField("mTermuxService");
        serviceField.setAccessible(true); serviceField.set(activity, service);
        activity.getTerminalView().mTermSession=terminal;
        controller.refresh(); controller.refresh();
        assertEquals(1, store.sessions.size()); assertFalse(store.sessions.get(0).saved);
        ViewGroup content = activity.findViewById(R.id.workspace_content);
        assertNull(findText(content, "Temporary Bash"));
        assertNull(findText(content, "Пресетов запуска нет"));
        navigate(controller,"Терминал");
        sessionMenu(controller,store.sessions.get(0));
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog menu = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(5, menu.getListView().getAdapter().getCount());
        for(int n=0;n<menu.getListView().getCount();n++)assertNotEquals("Диагностика сессии",menu.getListView().getItemAtPosition(n));
        assertEquals("Добавить в пресеты запуска", menu.getListView().getAdapter().getItem(0));
        assertEquals("Перезапустить", menu.getListView().getAdapter().getItem(1));
        assertEquals("Настройки", menu.getListView().getAdapter().getItem(2));
        assertEquals("Закрыть сессию", menu.getListView().getAdapter().getItem(3));
        menu.getListView().performItemClick(null, 0, 0);
        store.awaitWrites(); assertTrue(store.sessions.get(0).saved); assertEquals(1, store.sessions.size());
        sessionMenu(controller,store.sessions.get(0)); shadowOf(Looper.getMainLooper()).idle();
        menu = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(4, menu.getListView().getAdapter().getCount());
        for(int n=0;n<menu.getListView().getCount();n++)assertNotEquals("Диагностика сессии",menu.getListView().getItemAtPosition(n));
        assertEquals("Перезапустить", menu.getListView().getAdapter().getItem(0));
        assertEquals("Настройки", menu.getListView().getAdapter().getItem(1));
        assertEquals("Закрыть сессию", menu.getListView().getAdapter().getItem(2));
        menu.getListView().performItemClick(null, 0, 0);
        shadowOf(Looper.getMainLooper()).idle();
        AlertDialog confirmation = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(findText(confirmation.getWindow().getDecorView(), "Перезапустить эту сессию? Текущая команда будет остановлена. Рабочий стол и другие сессии продолжат работу."));
        confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        sessionMenu(controller,store.sessions.get(0)); shadowOf(Looper.getMainLooper()).idle();
        menu = ShadowAlertDialog.getLatestAlertDialog(); menu.getListView().performItemClick(null, 2, 2);
        shadowOf(Looper.getMainLooper()).idle(); confirmation = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(findText(confirmation.getWindow().getDecorView(), "Процесс будет остановлен, вкладка терминала закрыта. Сохранённые настройки и история останутся."));
        assertEquals("Закрыть сессию", confirmation.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertFalse(confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).getVisibility() == View.VISIBLE);
        confirmation.dismiss();
        assertEquals(1, service.getTermuxSessionsSize());
        navigate(controller,"Главная");
        assertNull(findText(content, "Temporary Bash")); // Journal no longer contains presets.
        assertTrue(store.sessions.get(0).saved);
        java.lang.reflect.Method sidebarMethod=WorkspaceController.class.getDeclaredMethod("showSidebar");sidebarMethod.setAccessible(true);sidebarMethod.invoke(controller);
        shadowOf(Looper.getMainLooper()).idle();
        android.app.Dialog sidebarDialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(findDescription(sidebarDialog.getWindow().getDecorView(),"Меню: Temporary Bash"));
        sidebarDialog.dismiss();
        controller.close();
        WorkspaceStore reopened = new WorkspaceStore(activity, message -> fail(message));
        assertEquals(1, reopened.sessions.size()); assertTrue(reopened.sessions.get(0).saved); reopened.close();
    }
    @Test public void newSessionCancelsFromLeftWhileSavedProfilesRemainInSidebar() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller); store.sessions.clear();
        WorkspaceStore.Session saved = new WorkspaceStore.Session(); saved.name = "Saved Ubuntu"; saved.saved = true;
        WorkspaceStore.Session temporary = new WorkspaceStore.Session(); temporary.name = "Temporary";
        store.sessions.add(saved); store.sessions.add(temporary);
        navigate(controller,"Терминал");
        call(controller,"showSidebar",new Class[0]);Field side=WorkspaceController.class.getDeclaredField("sidebar");side.setAccessible(true);WorkspaceSidebar drawer=(WorkspaceSidebar)side.get(controller);
        assertNotNull(drawer.body.findViewWithTag("sidebar-session-"+saved.id));assertNull(drawer.body.findViewWithTag("sidebar-session-"+temporary.id));findText(drawer.body,"Создать сессию").performClick();
        shadowOf(Looper.getMainLooper()).idle(); AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("Создать сессию", shadowOf(dialog).getTitle());
        assertEquals("Отмена", dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());
        assertEquals(View.GONE,dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getVisibility());
        assertEquals("Создать и открыть",dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick(); shadowOf(Looper.getMainLooper()).idle();
        assertFalse(dialog.isShowing());assertSame(dialog,ShadowAlertDialog.getLatestAlertDialog());
        assertEquals(2,store.sessions.size());assertSame(saved,store.sessions.get(0));assertTrue(saved.saved);assertNull(activity.getCurrentSession());controller.close();
    }
    @Test public void closingRemovesOnlyUnsavedProfileAndItsOwnHistoryAfterExit() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller); store.sessions.clear();
        WorkspaceStore.Session saved = new WorkspaceStore.Session(); saved.saved = true; saved.handle = "saved-live";
        WorkspaceStore.Session temporary = new WorkspaceStore.Session(); temporary.handle = "temporary-live";
        store.sessions.add(saved); store.sessions.add(temporary);
        java.io.File kept = SessionShell.directory(activity.getFilesDir(), saved.id); kept.mkdirs();
        java.io.File removed = SessionShell.directory(activity.getFilesDir(), temporary.id); removed.mkdirs();
        java.nio.file.Files.write(new java.io.File(kept, "history").toPath(), "saved command".getBytes());
        java.nio.file.Files.write(new java.io.File(removed, "history").toPath(), "temporary command".getBytes());
        java.io.File codex = new java.io.File(activity.getFilesDir(), "codex-history-test");
        java.nio.file.Files.write(codex.toPath(), "independent Codex history".getBytes());
        java.lang.reflect.Method retire = WorkspaceController.class.getDeclaredMethod("retireProfile", WorkspaceStore.Session.class);
        retire.setAccessible(true); retire.invoke(controller, temporary); retire.invoke(controller, saved);
        Field executor = WorkspaceController.class.getDeclaredField("reader"); executor.setAccessible(true);
        ((java.util.concurrent.ExecutorService) executor.get(controller)).submit(() -> {}).get(); store.awaitWrites();
        assertEquals(1, store.sessions.size()); assertSame(saved, store.sessions.get(0)); assertEquals("", saved.handle);
        assertFalse(removed.exists()); assertTrue(new java.io.File(kept, "history").isFile()); assertTrue(codex.isFile());
        controller.close(); WorkspaceStore reopened = new WorkspaceStore(activity, message -> fail(message));
        assertEquals(1, reopened.sessions.size()); assertTrue(reopened.sessions.get(0).saved); reopened.close();
    }
    @Test public void separateLibrariesScrollAndKeepIndependentFavoriteFilters()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);Field field=WorkspaceController.class.getDeclaredField("store");field.setAccessible(true);WorkspaceStore store=(WorkspaceStore)field.get(c);store.commands.clear();store.notes.clear();
        try{for(int i=0;i<30;i++){WorkspaceStore.Command command=new WorkspaceStore.Command();command.name="Command "+i;command.command="command body";command.favorite=i==0;store.commands.add(command);WorkspaceStore.Note note=new WorkspaceStore.Note();note.name="Note "+i;note.text="note body";note.favorite=i==1;store.notes.add(note);}
            for(int section:new int[]{0,2}){call(c,"openLibrary",new Class[]{int.class},section);ViewGroup content=a.findViewById(R.id.workspace_content);content.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));content.layout(0,0,320,400);android.widget.ScrollView scroll=(android.widget.ScrollView)findDescription(content,section==0?"Список сохранённых команд":"Список сохранённых заметок");assertTrue(scroll.getChildAt(0).getHeight()>scroll.getHeight());scroll.scrollTo(0,100);assertEquals(100,scroll.getScrollY());assertNull(findDescription(content,section==0?"Список сохранённых заметок":"Список сохранённых команд"));findText(content,"Избранное").performClick();assertNotNull(findText(content,section==0?"Command 0":"Note 1"));assertNull(findText(content,section==0?"Command 1":"Note 0"));}
            call(c,"openLibrary",new Class[]{int.class},0);View content=a.findViewById(R.id.workspace_content);assertNotNull(findText(content,"Command 0"));assertNull(findText(content,"Command 1"));
        }finally{c.close();}
    }
    @Test @Config(qualifiers = "w320dp-h640dp") public void narrowLibraryKeepsFiltersAndAddButtonsInsideWindow() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        android.content.res.Configuration configuration = new android.content.res.Configuration(activity.getResources().getConfiguration());
        configuration.fontScale = 1.6f;
        android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        metrics.scaledDensity = metrics.density * configuration.fontScale;
        activity.getResources().updateConfiguration(configuration, metrics);
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        navigate(controller,"Команды/Заметки");
        ViewGroup content = activity.findViewById(R.id.workspace_content);
        int width = Math.round(320 * metrics.density), height = Math.round(240 * metrics.density);
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, width, height);
        for (String type : new String[]{"команд", "заметок"}) {
            call(controller,"openLibrary",new Class[]{int.class},type.equals("заметок")?2:0);content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));content.layout(0,0,width,height);
            ViewGroup row = (ViewGroup) findDescription(content, "Фильтры " + type);
            ViewGroup filters = type.equals("заметок")?(ViewGroup)row.getChildAt(0):row;
            View add = row.getChildAt(row.getChildCount()-1);
            assertTrue(add.getRight() <= row.getWidth());
            if(filters!=row)assertTrue(filters.getRight() <= add.getLeft());
            for (int i = 0; i < filters.getChildCount()-(filters==row?1:0); i++) {
                TextView filter = (TextView) filters.getChildAt(i);
                assertTrue(filter.getWidth() > 0);
                assertTrue(filter.getRight() <= filters.getWidth());
                assertNotNull(filter.getLayout());
                for (int line = 0; line < filter.getLayout().getLineCount(); line++)
                    assertEquals(0, filter.getLayout().getEllipsisCount(line));
            }
        }
        controller.close();
    }
    @Test public void passwordEditorShowsMaskedInputAndNeverWritesOneShotPassword() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller); store.sessions.clear();
        java.lang.reflect.Method edit = WorkspaceController.class.getDeclaredMethod("editSession", WorkspaceStore.Session.class, boolean.class);
        edit.setAccessible(true); edit.invoke(controller, null, false); shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        findDescription(dialog.getWindow().getDecorView(), "Среда запуска").performClick(); shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null, 1, 1); shadowOf(Looper.getMainLooper()).idle();
        findDescription(dialog.getWindow().getDecorView(), "Авторизация SSH").performClick(); shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null, 2, 2); shadowOf(Looper.getMainLooper()).idle();
        List<EditText> fields = new ArrayList<>(); collectEditors(dialog.getWindow().getDecorView(), fields);
        fields.get(0).setText("Password server"); fields.get(2).setText("example.invalid");
        EditText password = (EditText) findDescription(dialog.getWindow().getDecorView(), "Пароль SSH");
        assertFalse(password.isSaveEnabled()); assertNotNull(password.getTransformationMethod());
        password.setText("dummy-TEST-private"); findText(dialog.getWindow().getDecorView(), "Показать пароль").performClick();
        assertNull(password.getTransformationMethod());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); store.awaitWrites();
        assertEquals("password", store.sessions.get(0).sshAuth); assertEquals("", store.sessions.get(0).sshEncryptedPassword);
        assertFalse(store.encode().toString().contains("dummy-TEST-private")); assertEquals(0, password.length());
        controller.close();
    }
    @Test public void sshSessionEditorValidatesPortAndPersistsConnectionOnlyWhenChosen() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller); store.sessions.clear();
        java.lang.reflect.Method edit = WorkspaceController.class.getDeclaredMethod("editSession", WorkspaceStore.Session.class, boolean.class);
        edit.setAccessible(true); edit.invoke(controller, null, false); shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog(); List<EditText> fields = new ArrayList<>();
        collectEditors(dialog.getWindow().getDecorView(), fields); assertEquals(3, fields.size());
        findDescription(dialog.getWindow().getDecorView(), "Среда запуска").performClick(); shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null, 1, 1);
        shadowOf(Looper.getMainLooper()).idle(); fields.clear(); collectEditors(dialog.getWindow().getDecorView(), fields);
        assertEquals(6, fields.size()); assertEquals("~", fields.get(1).getText().toString());
        fields.get(0).setText("Server"); fields.get(2).setText("example.invalid"); fields.get(3).setText("deploy");
        fields.get(4).setText("70000"); fields.get(5).setText("pwd");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); assertTrue(store.sessions.isEmpty()); assertNotNull(fields.get(4).getError());
        fields.get(4).setText("2222"); dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); store.awaitWrites();
        assertEquals(1, store.sessions.size()); WorkspaceStore.Session ssh = store.sessions.get(0);
        assertEquals("ssh", ssh.environment); assertEquals("example.invalid", ssh.sshHost);
        assertEquals("deploy", ssh.sshUser); assertEquals(2222, ssh.sshPort); assertTrue(ssh.sshKeepOpen);
        assertEquals("pwd", ssh.startup); assertTrue(ssh.saved); assertNull(activity.getCurrentSession());
        controller.close();
    }
    @Test public void noteEditorOpenEditFavoriteAndDeletePreserveCommands() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller); store.notes.clear(); store.commands.clear();
        store.commands.add(new WorkspaceStore.Command());
        call(controller,"openLibrary",new Class[]{int.class},2);
        ViewGroup content = activity.findViewById(R.id.workspace_content);
        findDescription(content, "Новая заметка").performClick(); shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog(); List<EditText> fields = new ArrayList<>();
        collectEditors(dialog.getWindow().getDecorView(), fields);
        fields.get(0).setText("Build tips"); fields.get(1).setText("First line\nSecond line"); fields.get(2).setText("Build");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); store.awaitWrites(); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, store.notes.size()); assertEquals("First line\nSecond line", store.notes.get(0).text);
        findDescription(content, "Заметка Build tips").performClick(); shadowOf(Looper.getMainLooper()).idle();
        dialog = ShadowAlertDialog.getLatestAlertDialog(); assertNotNull(findText(dialog.getWindow().getDecorView(), "First line\nSecond line"));
        dialog.dismiss(); findDescription(content, "Заметка Build tips").performClick();
        shadowOf(Looper.getMainLooper()).idle(); dialog = ShadowAlertDialog.getLatestAlertDialog(); findText(dialog.getWindow().getDecorView(),"В избранное").performClick();
        assertTrue(store.notes.get(0).favorite);
        findDescription(content, "Заметка Build tips").performClick(); shadowOf(Looper.getMainLooper()).idle();
        findText(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView(),"Редактировать").performClick(); shadowOf(Looper.getMainLooper()).idle();
        dialog = ShadowAlertDialog.getLatestAlertDialog(); fields.clear(); collectEditors(dialog.getWindow().getDecorView(), fields);
        fields.get(1).setText("Edited\nText"); dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        store.awaitWrites(); assertEquals("Edited\nText", store.notes.get(0).text);
        findDescription(content, "Заметка Build tips").performClick(); shadowOf(Looper.getMainLooper()).idle();
        findText(ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView(),"Удалить").performClick(); shadowOf(Looper.getMainLooper()).idle();
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick(); shadowOf(Looper.getMainLooper()).idle(); store.awaitWrites();
        assertTrue(store.notes.isEmpty()); assertEquals(1, store.commands.size()); controller.close();
    }
    @Test public void starPickerSearchFiltersNameContentAndCategoryWithoutSendingOrTakingFocus() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView");
        terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        Field field = WorkspaceController.class.getDeclaredField("store"); field.setAccessible(true);
        WorkspaceStore store = (WorkspaceStore) field.get(controller); store.commands.clear(); store.notes.clear(); store.recentCommands.clear();
        WorkspaceStore.Command alpha = new WorkspaceStore.Command(); alpha.name = "Alpha"; alpha.command = "echo alpha"; alpha.category = "Build";
        WorkspaceStore.Command beta = new WorkspaceStore.Command(); beta.name = "Beta"; beta.command = "first\nsecond"; beta.category = "Custom"; beta.favorite = true;
        store.commands.add(alpha); store.commands.add(beta);
        WorkspaceStore.Note note = new WorkspaceStore.Note(); note.name = "Ubuntu guide"; note.text = "line one\nline two"; note.category = "Docs"; note.favorite = true;
        WorkspaceStore.Note other = new WorkspaceStore.Note(); other.name = "Other"; other.text = "another text";
        store.notes.add(other); store.notes.add(note);
        ViewGroup input = activity.findViewById(R.id.workspace_input); EditText composer = composer(activity); composer.setText("draft");
        findDescription(input, "Вставить сохранённую команду").performClick(); shadowOf(Looper.getMainLooper()).idle();
        AlertDialog picker = ShadowAlertDialog.getLatestAlertDialog();
        EditText search = (EditText) findDescription(picker.getWindow().getDecorView(), "Поиск в меню звёздочки");
        assertNotNull(search); assertFalse(search.hasFocus());
        assertEquals(0,picker.getWindow().getAttributes().flags & android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        search.performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(search.hasFocus());
        assertTrue(shadowOf((android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).isSoftInputVisible());
        assertSame(beta, picker.getListView().getAdapter().getItem(0));
        search.setText("ALPHA"); assertEquals(1, picker.getListView().getAdapter().getCount()); assertSame(alpha, picker.getListView().getAdapter().getItem(0));
        search.setText("second"); assertSame(beta, picker.getListView().getAdapter().getItem(0));
        search.setText("Build"); assertSame(alpha, picker.getListView().getAdapter().getItem(0));
        search.setText("not found"); assertEquals(0, picker.getListView().getAdapter().getCount());
        assertEquals(View.VISIBLE, findText(picker.getWindow().getDecorView(), "Ничего не найдено").getVisibility());
        assertEquals("draft", composer.getText().toString()); search.setText("Custom");
        picker.getListView().performItemClick(null,0,0);shadowOf(Looper.getMainLooper()).idle();AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(findText(preview.getWindow().getDecorView(),beta.command));assertEquals("draft",composer.getText().toString());preview.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(beta.command,composer.getText().toString());
        picker(controller,2);picker=ShadowAlertDialog.getLatestAlertDialog();search=(EditText)findDescription(picker.getWindow().getDecorView(),"Поиск в меню звёздочки");assertEquals("Найти заметку",search.getHint().toString());assertEquals("",search.getText().toString());assertSame(note,picker.getListView().getAdapter().getItem(0));
        for(String query:new String[]{"UBUNTU","line two","Docs"}){search.setText(query);assertEquals(1,picker.getListView().getAdapter().getCount());assertSame(note,picker.getListView().getAdapter().getItem(0));}
        search.setText("missing");assertEquals(0,picker.getListView().getAdapter().getCount());search.setText("Docs");picker.getListView().performItemClick(null,0,0);shadowOf(Looper.getMainLooper()).idle();preview=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(findText(preview.getWindow().getDecorView(),note.text));assertEquals(beta.command,composer.getText().toString());preview.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(note.text,composer.getText().toString());assertTrue(store.recentCommands.isEmpty());assertNull(activity.getCurrentSession());
        store.notes.clear();picker(controller,2);picker=ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,picker.getListView().getAdapter().getCount());assertEquals(View.VISIBLE,findText(picker.getWindow().getDecorView(),"Сохраните заметку в библиотеке.").getVisibility());picker.dismiss();
        controller.close();
    }
    @Test public void starPreviewBackRetainsTabQueryAndDraft()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);android.content.res.Resources previous=WorkspaceText.resources;
        try{
            Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore store=(WorkspaceStore)f.get(c);store.commands.clear();store.notes.clear();
            WorkspaceStore.Command command=new WorkspaceStore.Command();command.name="needle command";command.command="echo hello";store.commands.add(command);
            WorkspaceStore.Note note=new WorkspaceStore.Note();note.name="needle note";note.text="note text";store.notes.add(note);
            EditText composer=composer(a);composer.setText("draft");
            for(boolean ru:new boolean[]{true,false})for(boolean notes:new boolean[]{false,true}){
                WorkspaceLanguage.select(a,ru?"ru":"en");WorkspaceText.resources=WorkspaceLanguage.wrap(a).getResources();picker(c,notes?2:0);AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();
                List<EditText> fields=new ArrayList<>();collectEditors(picker.getWindow().getDecorView(),fields);EditText search=fields.get(0);search.setText("needle");Object selected=picker.getListView().getAdapter().getItem(0);
                picker.getListView().performItemClick(null,0,0);shadowOf(Looper.getMainLooper()).idle();AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();assertEquals(ru?"Назад":"Back",preview.getButton(-2).getText().toString());preview.getButton(-2).performClick();shadowOf(Looper.getMainLooper()).idle();
                assertTrue(picker.isShowing());assertEquals("needle",search.getText().toString());assertSame(selected,picker.getListView().getAdapter().getItem(0));assertEquals("draft",composer.getText().toString());
                picker.getListView().performItemClick(null,0,0);shadowOf(Looper.getMainLooper()).idle();ShadowAlertDialog.getLatestAlertDialog().cancel();shadowOf(Looper.getMainLooper()).idle();assertTrue(picker.isShowing());assertSame(selected,picker.getListView().getAdapter().getItem(0));picker.dismiss();
            }
        }finally{WorkspaceLanguage.select(a,"ru");WorkspaceText.resources=previous;c.close();}
    }
    @Test public void libraryPreviewAllowsEditingAndConfirmedDeletionWithoutSubmitting()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);WorkspaceLanguage.select(a,"ru");Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try{
            Field field=WorkspaceController.class.getDeclaredField("store");field.setAccessible(true);WorkspaceStore store=(WorkspaceStore)field.get(c);store.commands.clear();store.notes.clear();store.recentCommands.clear();
            WorkspaceStore.Command command=new WorkspaceStore.Command();command.name="Command";command.command="echo original";store.commands.add(command);WorkspaceStore.Note note=new WorkspaceStore.Note();note.name="Note";note.text="original note";store.notes.add(note);
            EditText composer=composer(a);composer.setText("unsent draft");
            for(boolean notes:new boolean[]{false,true}){
                String full=String.join("\n",java.util.Collections.nCopies(100,"long line"))+"\nlast line";if(notes)note.text=full;else command.command=full;
                java.lang.reflect.Method open=WorkspaceController.class.getDeclaredMethod(notes?"openNote":"openCommand",notes?WorkspaceStore.Note.class:WorkspaceStore.Command.class);open.setAccessible(true);open.invoke(c,notes?note:command);shadowOf(Looper.getMainLooper()).idle();AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(findText(preview.getWindow().getDecorView(),full));assertEquals("unsent draft",composer.getText().toString());assertNotNull(findText(preview.getWindow().getDecorView(),"В избранное"));
                findText(preview.getWindow().getDecorView(),"Редактировать").performClick();shadowOf(Looper.getMainLooper()).idle();AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();List<EditText> fields=new ArrayList<>();collectEditors(editor.getWindow().getDecorView(),fields);fields.get(1).setText("edited\ntext");editor.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();store.awaitWrites();assertEquals("edited\ntext",notes?note.text:command.command);assertEquals("unsent draft",composer.getText().toString());
                for(boolean accept:new boolean[]{false,true}){
                    open.invoke(c,notes?note:command);shadowOf(Looper.getMainLooper()).idle();preview=ShadowAlertDialog.getLatestAlertDialog();findText(preview.getWindow().getDecorView(),"Удалить").performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(1,notes?store.notes.size():store.commands.size());AlertDialog confirmation=ShadowAlertDialog.getLatestAlertDialog();confirmation.getButton(accept?-1:-2).performClick();shadowOf(Looper.getMainLooper()).idle();store.awaitWrites();assertEquals(accept?0:1,notes?store.notes.size():store.commands.size());
                }
                assertEquals("unsent draft",composer.getText().toString());assertTrue(store.recentCommands.isEmpty());assertNull(a.getCurrentSession());
            }
        }finally{c.close();}
    }
    @Test public void switchingTerminalRestoresSeparateDraftsAndSelection() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field field=TermuxActivity.class.getDeclaredField("mTerminalView");field.setAccessible(true);field.set(activity,activity.findViewById(R.id.terminal_view));
        WorkspaceController controller=new WorkspaceController(activity);
        EditText input=composer(activity);
        // Constructor only, no attachSession/updateSize/JNI/service or shell process.
        com.termux.terminal.TerminalSession a=new com.termux.terminal.TerminalSession("unused","unused",new String[0],new String[0],2000,null);
        com.termux.terminal.TerminalSession b=new com.termux.terminal.TerminalSession("unused","unused",new String[0],new String[0],2000,null);
        activity.getTerminalView().mTermSession=a;controller.refresh();input.setText("unsent in A");input.setSelection(2,5);
        controller.refresh();assertEquals("unsent in A",input.getText().toString());call(controller,"setInteractiveInput",new Class[]{boolean.class},true);assertTrue(activity.getTerminalView().isSoftKeyboardInputEnabled());
        activity.getTerminalView().mTermSession=b;controller.refresh();assertEquals("",input.getText().toString());
        input.setText("unsent in B");input.setSelection(4);call(controller,"setInteractiveInput",new Class[]{boolean.class},false);controller.refresh();assertEquals("unsent in B",input.getText().toString());assertFalse(activity.getTerminalView().isSoftKeyboardInputEnabled());
        activity.getTerminalView().mTermSession=a;controller.refresh();assertEquals("unsent in A",input.getText().toString());
        assertEquals(2,input.getSelectionStart());assertEquals(5,input.getSelectionEnd());assertTrue(activity.getTerminalView().isSoftKeyboardInputEnabled());
        input.setText("");
        activity.getTerminalView().mTermSession=b;controller.refresh();assertEquals("unsent in B",input.getText().toString());assertEquals(4,input.getSelectionStart());assertFalse(activity.getTerminalView().isSoftKeyboardInputEnabled());
        activity.getTerminalView().mTermSession=a;controller.refresh();assertEquals("",input.getText().toString());
        controller.close();
    }
    @Test public void modelClickOpensBehaviorThenConfirmationWithoutApplyingEarly() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field field=TermuxActivity.class.getDeclaredField("mTerminalView");field.setAccessible(true);field.set(activity,activity.findViewById(R.id.terminal_view));
        WorkspaceController controller=new WorkspaceController(activity);WorkspaceStore.Session profile=new WorkspaceStore.Session();
        CodexStatus status=new CodexStatus();status.model="gpt-6.1-sol";status.effort="low";
        org.json.JSONObject model=new org.json.JSONObject().put("model","gpt-6.1-sol").put("displayName","GPT-6.1-Sol").put("defaultReasoningEffort","low")
            .put("supportedReasoningEfforts",new org.json.JSONArray().put(new org.json.JSONObject().put("reasoningEffort","low")).put(new org.json.JSONObject().put("reasoningEffort","high")));
        status.models.put(model);
        java.lang.reflect.Method choose=WorkspaceController.class.getDeclaredMethod("chooseCodexModel",WorkspaceStore.Session.class,CodexStatus.class);choose.setAccessible(true);choose.invoke(controller,profile,status);
        AlertDialog models=ShadowAlertDialog.getLatestAlertDialog();assertTrue(models.isShowing());assertEquals(View.GONE,models.getButton(AlertDialog.BUTTON_POSITIVE).getVisibility());
        models.getListView().performItemClick(null,0,0);shadowOf(Looper.getMainLooper()).idle();
        AlertDialog behavior=ShadowAlertDialog.getLatestAlertDialog();assertFalse(models.isShowing());assertTrue(behavior.isShowing());assertEquals(View.GONE,behavior.getButton(AlertDialog.BUTTON_POSITIVE).getVisibility());
        behavior.getListView().performItemClick(null,1,1);shadowOf(Looper.getMainLooper()).idle();
        AlertDialog confirmation=ShadowAlertDialog.getLatestAlertDialog();assertFalse(behavior.isShowing());assertTrue(confirmation.isShowing());
        String message=((TextView)confirmation.findViewById(android.R.id.message)).getText().toString();assertTrue(message.contains("GPT-6.1-Sol"));assertTrue(message.contains("Высокий"));assertEquals("Применить",confirmation.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        Field generation=WorkspaceController.class.getDeclaredField("codexApplyGeneration");generation.setAccessible(true);assertEquals(0,generation.getInt(controller));
        confirmation.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,generation.getInt(controller));assertNull(activity.getCurrentSession());controller.close();
    }
    @Test public void codexEditorHasAccessModesAndChatChoicesWithoutStartingProcesses() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get(); activity.setTheme(R.style.Theme_Termux_Black); activity.setContentView(R.layout.activity_termux);
        Field terminalView = TermuxActivity.class.getDeclaredField("mTerminalView"); terminalView.setAccessible(true); terminalView.set(activity, activity.findViewById(R.id.terminal_view));
        WorkspaceController controller = new WorkspaceController(activity);
        java.lang.reflect.Method edit = WorkspaceController.class.getDeclaredMethod("editSession", WorkspaceStore.Session.class, boolean.class); edit.setAccessible(true); edit.invoke(controller, null, false);
        AlertDialog form = ShadowAlertDialog.getLatestAlertDialog(); View decor = form.getWindow().getDecorView();
        findDescription(decor, "Среда запуска").performClick(); AlertDialog environments = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(3,environments.getListView().getCount()); assertEquals(WorkspaceLanguage.code(activity).equals("ru")?"Окружения Termux":"Termux environments",environments.getListView().getItemAtPosition(0)); assertEquals("AI Agents", environments.getListView().getAdapter().getItem(2)); environments.getListView().performItemClick(null,2,2);
        assertNull(findText(decor,"Среда Codex: Ubuntu")); assertNull(findDescription(decor,"Среда Codex"));
        assertEquals(View.GONE, findDescription(decor,"Добавить команду запуска").getVisibility());
        findDescription(decor,"Доступ Codex").performClick(); AlertDialog access = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(2,access.getListView().getAdapter().getCount()); access.getListView().performItemClick(null,0,0);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(WorkspaceLanguage.code(activity).equals("ru")?"Права: Чтение":"Permissions: Read", ((TextView)findDescription(decor,"Доступ Codex")).getText().toString());
        assertNotNull(findDescription(decor,"Codex project write scope"));
        assertNotNull(decor.findViewWithTag("codex-command-confirmations"));assertEquals(WorkspaceLanguage.code(activity).equals("ru")?"Подтверждать недоверенные команды":"Confirm untrusted commands",((TextView)decor.findViewWithTag("codex-command-confirmations")).getText().toString());
        findDescription(decor,"Чат Codex").performClick(); AlertDialog chat=ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,chat.getListView().getAdapter().getCount());chat.getListView().performItemClick(null,0,0);
        assertNotNull(findDescription(decor,"Выбрать рабочий каталог"));
        shadowOf(Looper.getMainLooper()).idle();
        form.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Field storeField = WorkspaceController.class.getDeclaredField("store"); storeField.setAccessible(true); WorkspaceStore store = (WorkspaceStore)storeField.get(controller);
        assertEquals(1,store.sessions.size()); WorkspaceStore.Session profile = store.sessions.get(0);
        assertEquals("codex",profile.environment); assertEquals("read",profile.codexAccess); assertEquals("new",profile.codexChat); assertEquals("",profile.codexThread); assertEquals("/root",profile.directory); assertEquals("ubuntu-ai",profile.codexEnvironment); assertTrue(profile.saved);
        assertNull(activity.getCurrentSession()); assertNull(activity.findViewById(R.id.workspace_codex_status));
        store.awaitWrites(); controller.close();
    }
    @Test public void openCodeEditorStoresIndependentUbuntuProfileWithoutLegacyStartupCommands() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field f=TermuxActivity.class.getDeclaredField("mTerminalView");f.setAccessible(true);f.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController(activity);
        java.lang.reflect.Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(controller,null,false);
        AlertDialog form=ShadowAlertDialog.getLatestAlertDialog();View decor=form.getWindow().getDecorView();findDescription(decor,"Среда запуска").performClick();ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,2,2);
        findText(decor,"Агент: Codex").performClick();ShadowAlertDialog.getLatestAlertDialog().getListView().performItemClick(null,1,1);
        shadowOf(Looper.getMainLooper()).idle();assertNotNull(findText(decor,"Агент: OpenCode"));assertNotNull(findText(decor,"Подключение: Основное подключение"));assertEquals(View.GONE,findDescription(decor,"Добавить команду запуска").getVisibility());
        form.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();Field sf=WorkspaceController.class.getDeclaredField("store");sf.setAccessible(true);WorkspaceStore store=(WorkspaceStore)sf.get(controller);assertEquals(1,store.sessions.size());WorkspaceStore.Session profile=store.sessions.get(0);
        assertEquals("ubuntu",profile.environment);assertEquals("opencode",profile.aiAgent);assertEquals("new",profile.aiChat);assertTrue(profile.startupCommands().isEmpty());assertEquals("/root",profile.directory);assertNull(activity.getCurrentSession());store.awaitWrites();controller.close();
    }
    @Test public void sidebarKeepsSessionActionsAndTitleDoesNotOpenTerminal() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try{Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore store=(WorkspaceStore)f.get(c);store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.name="Synthetic saved Ubuntu";p.environment="ubuntu";p.saved=true;store.sessions.add(p);call(c,"showSidebar",new Class[]{});f=WorkspaceController.class.getDeclaredField("sidebar");f.setAccessible(true);WorkspaceSidebar menu=(WorkspaceSidebar)f.get(c);ViewGroup frame=menu.body.findViewWithTag("sidebar-session-"+p.id);ViewGroup actions=(ViewGroup)frame.getChildAt(1);assertEquals(View.GONE,actions.getVisibility());((ViewGroup)frame.getChildAt(0)).getChildAt(0).performClick();assertEquals(View.VISIBLE,actions.getVisibility());assertNull(a.getCurrentSession());
        String[] expected={"Открыть терминал","Настройки сессии","Настройки рабочего стола","Программы","Файлы окружения","Пакеты окружения","Документация окружения"};assertEquals(expected.length,actions.getChildCount());for(int i=0;i<expected.length;i++)assertEquals(expected[i],((TextView)actions.getChildAt(i)).getText().toString());assertNull(actions.findViewWithTag("sidebar-desktop-stop-"+p.id));assertNotNull(findText(menu.body,"Создать сессию"));assertNotNull(findText(menu.body,"Создать чат"));assertNotNull(findText(menu.body,"Общие настройки"));actions.getChildAt(1).performClick();shadowOf(Looper.getMainLooper()).idle();AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();assertTrue(editor.isShowing());assertNotNull(findText(editor.getWindow().getDecorView(),p.name));assertNull(a.getCurrentSession());editor.getButton(-2).performClick();shadowOf(Looper.getMainLooper()).idle();assertSame(p,store.sessions.get(0));assertTrue(p.saved);
        }finally{c.close();}
    }
    @Test public void baseTerminalMenuCannotCloseDeleteOrChangeItsProfile() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field f=TermuxActivity.class.getDeclaredField("mTerminalView");f.setAccessible(true);f.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController(activity);
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.id=WorkspaceStore.BASE_TERMINAL_ID;profile.name="Terminal";
        java.lang.reflect.Method menu=WorkspaceController.class.getDeclaredMethod("sessionMenu",WorkspaceStore.Session.class,boolean.class);menu.setAccessible(true);menu.invoke(controller,profile,true);
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Установка пакетов",dialog.getListView().getItemAtPosition(0));assertEquals("Перезапустить",dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertNull(findText(dialog.getWindow().getDecorView(),"Закрыть сессию"));assertNull(findText(dialog.getWindow().getDecorView(),"Настройки"));dialog.dismiss();controller.close();
    }
    @Test public void savingBusyNewCodexDoesNotCreateFalseResumeAssociation() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field view=TermuxActivity.class.getDeclaredField("mTerminalView");view.setAccessible(true);view.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController(activity);
        com.termux.terminal.TerminalSession terminal=new com.termux.terminal.TerminalSession("unused","unused",new String[0],new String[0],2000,null);
        java.lang.reflect.Constructor<TermuxSession> constructor=TermuxSession.class.getDeclaredConstructor(com.termux.terminal.TerminalSession.class,com.termux.shared.models.ExecutionCommand.class,TermuxSession.TermuxSessionClient.class,boolean.class);constructor.setAccessible(true);
        com.termux.app.TermuxService service=new com.termux.app.TermuxService();service.getTermuxSessions().add(constructor.newInstance(terminal,new com.termux.shared.models.ExecutionCommand(),null,false));
        Field serviceField=TermuxActivity.class.getDeclaredField("mTermuxService");serviceField.setAccessible(true);serviceField.set(activity,service);
        Field storeField=WorkspaceController.class.getDeclaredField("store");storeField.setAccessible(true);WorkspaceStore store=(WorkspaceStore)storeField.get(controller);
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment="codex";profile.handle=terminal.mHandle;store.sessions.add(profile);
        controller.chatSnapshot=captured->null;
        java.lang.reflect.Method save=WorkspaceController.class.getDeclaredMethod("saveSessionProfile",WorkspaceStore.Session.class);save.setAccessible(true);save.invoke(controller,profile);
        Field pending=WorkspaceController.class.getDeclaredField("saveBinding");pending.setAccessible(true);
        for(int i=0;i<100&&!((java.util.Set<?>)pending.get(controller)).isEmpty();i++){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}
        assertFalse(profile.saved);assertEquals("new",profile.codexChat);assertEquals("",profile.codexThread);
        controller.chatSnapshot=captured->new org.json.JSONObject().put("nativeChat",true).put("session","verified-thread");save.invoke(controller,profile);
        for(int i=0;i<100&&!profile.saved;i++){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}assertTrue(profile.saved);assertEquals("verified-thread",profile.codexThread);
        store.awaitWrites();controller.close();
    }
    @Test public void differentChatDirectoryRequiresExplicitConfirmation() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field field=TermuxActivity.class.getDeclaredField("mTerminalView");field.setAccessible(true);field.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController controller=new WorkspaceController(activity);
        int[] accepted={0};java.lang.reflect.Method confirm=WorkspaceController.class.getDeclaredMethod("confirmCodexDirectory",String.class,String.class,String.class,Runnable.class);confirm.setAccessible(true);
        confirm.invoke(controller,"ubuntu","/root/termux-fork","/root",(Runnable)()->accepted[0]++);
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,accepted[0]);String message=((TextView)dialog.findViewById(android.R.id.message)).getText().toString();assertTrue(message.contains("/root/termux-fork"));assertTrue(message.contains("История останется прежней"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(0,accepted[0]);
        confirm.invoke(controller,"ubuntu","/root/termux-fork","/root",(Runnable)()->accepted[0]++);ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals(1,accepted[0]);
        confirm.invoke(controller,"ubuntu","/root/termux-fork/","/root/termux-fork",(Runnable)()->accepted[0]++);assertEquals(2,accepted[0]);controller.close();
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
        if (root.getVisibility() != View.VISIBLE) return;
        if (root instanceof EditText) fields.add((EditText) root);
        else if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) collectEditors(((ViewGroup) root).getChildAt(i), fields);
    }


    @Test public void openSessionRowsKeepSizingAndHighlightOnlyTheSelectedTerminal()throws Exception{
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field view=TermuxActivity.class.getDeclaredField("mTerminalView");view.setAccessible(true);view.set(activity,activity.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(activity);
        try {
            com.termux.terminal.TerminalSession first=new com.termux.terminal.TerminalSession("unused","unused",new String[0],new String[0],2000,null);
            com.termux.terminal.TerminalSession second=new com.termux.terminal.TerminalSession("unused","unused",new String[0],new String[0],2001,null);
            first.mSessionName="First Bash";second.mSessionName="Second Bash";
            java.lang.reflect.Constructor<TermuxSession> ctor=TermuxSession.class.getDeclaredConstructor(com.termux.terminal.TerminalSession.class,com.termux.shared.models.ExecutionCommand.class,TermuxSession.TermuxSessionClient.class,boolean.class);ctor.setAccessible(true);
            com.termux.app.TermuxService service=new com.termux.app.TermuxService();
            service.getTermuxSessions().add(ctor.newInstance(first,new com.termux.shared.models.ExecutionCommand(),null,false));service.getTermuxSessions().add(ctor.newInstance(second,new com.termux.shared.models.ExecutionCommand(),null,false));
            Field sf=TermuxActivity.class.getDeclaredField("mTermuxService");sf.setAccessible(true);sf.set(activity,service);
            Field storeField=WorkspaceController.class.getDeclaredField("store");storeField.setAccessible(true);WorkspaceStore stored=(WorkspaceStore)storeField.get(c);
            for(com.termux.terminal.TerminalSession terminal:new com.termux.terminal.TerminalSession[]{first,second}){WorkspaceStore.Session p=new WorkspaceStore.Session();p.name=terminal.mSessionName;p.handle=terminal.mHandle;p.environment="termux";stored.sessions.add(p);}
            java.lang.reflect.Method stack=WorkspaceController.class.getDeclaredMethod("showSessionStack",View.class);stack.setAccessible(true);View anchor=activity.findViewById(R.id.workspace_tabs);
            for(com.termux.terminal.TerminalSession selected:new com.termux.terminal.TerminalSession[]{first,second}) {
                activity.getTerminalView().mTermSession=selected;
                android.widget.PopupWindow popup=(android.widget.PopupWindow)stack.invoke(c,anchor);View root=popup.getContentView();TextView create=root.findViewWithTag("session-stack-create");
                assertFalse(create.isSelected());
                for(com.termux.terminal.TerminalSession terminal:new com.termux.terminal.TerminalSession[]{first,second}) {
                    TextView row=root.findViewWithTag("session-stack-"+terminal.mHandle);assertNotNull(row);assertEquals(terminal==selected,row.isSelected());assertNotNull(row.getCompoundDrawables()[0]);
                    assertEquals(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,row.getLayoutParams().height);assertEquals(create.getLayoutParams().height,row.getLayoutParams().height);assertEquals(14f,row.getTextSize()/activity.getResources().getDisplayMetrics().scaledDensity,.01f);assertFalse(row.isLongClickable());assertNotNull(root.findViewWithTag("session-stack-settings-"+terminal.mHandle));
                }
                assertEquals(Math.min(Math.round(340*activity.getResources().getDisplayMetrics().density),activity.getResources().getDisplayMetrics().widthPixels-Math.round(24*activity.getResources().getDisplayMetrics().density)),popup.getWidth());
                com.termux.terminal.TerminalSession other=selected==first?second:first;root.findViewWithTag("session-stack-settings-"+other.mHandle).performClick();assertSame(selected,activity.getCurrentSession());assertFalse(popup.isShowing());AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();assertTrue(menu.isShowing());assertEquals(other.mSessionName,shadowOf(menu).getTitle().toString());menu.dismiss();
            }
        } finally { c.close(); }
    }
    @Test public void legacyTermuxCodexEditorKeepsEnvironmentAndPathsWithoutSelector() throws Exception {
        TermuxActivity activity=Robolectric.buildActivity(TermuxActivity.class).get();activity.setTheme(R.style.Theme_Termux_Black);activity.setContentView(R.layout.activity_termux);
        Field terminal=TermuxActivity.class.getDeclaredField("mTerminalView");terminal.setAccessible(true);terminal.set(activity,activity.findViewById(R.id.terminal_view));
        WorkspaceController controller=new WorkspaceController(activity);
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.name="Legacy Codex";profile.environment="codex";profile.codexEnvironment="termux";profile.directory=com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH;profile.saved=true;
        Field field=WorkspaceController.class.getDeclaredField("store");field.setAccessible(true);((WorkspaceStore)field.get(controller)).sessions.add(profile);
        java.lang.reflect.Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(controller,profile,false);
        AlertDialog form=ShadowAlertDialog.getLatestAlertDialog();assertNull(findDescription(form.getWindow().getDecorView(),"Среда Codex"));
        shadowOf(Looper.getMainLooper()).idle();form.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ((WorkspaceStore)field.get(controller)).awaitWrites();shadowOf(Looper.getMainLooper()).idle();assertFalse(form.isShowing());assertEquals("termux",profile.codexEnvironment);assertEquals(com.termux.shared.termux.TermuxConstants.TERMUX_HOME_DIR_PATH,profile.directory);assertEquals("new",profile.codexChat);controller.close();
    }

}
