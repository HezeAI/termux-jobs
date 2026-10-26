package com.termux.app.workspace;

import android.app.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** UI-only: saving to a real installed rootfs is never invoked by these tests. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceAliasesUiTest {
    private TermuxActivity a;private WorkspaceController c;private WorkspaceStore s;
    @Before public void setup()throws Exception{
        a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);WorkspaceLanguage.select(a,"ru");WorkspaceText.resources=WorkspaceLanguage.wrap(a).getResources();
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));c=new WorkspaceController(a);Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);s=(WorkspaceStore)f.get(c);s.aliases.clear();s.commands.clear();s.notes.clear();s.recentCommands.clear();
    }
    @After public void close(){c.close();}
    private Object call(String name,Class<?>[] types,Object... args)throws Exception{Method m=WorkspaceController.class.getDeclaredMethod(name,types);m.setAccessible(true);Object r=m.invoke(c,args);shadowOf(Looper.getMainLooper()).idle();return r;}
    private static TextView text(View root,String value){if(root instanceof TextView&&((TextView)root).getText().toString().equals(value))return (TextView)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){TextView found=text(((ViewGroup)root).getChildAt(i),value);if(found!=null)return found;}return null;}
    private static EditText input(View root,String description){if(root instanceof EditText&&description.contentEquals(root.getContentDescription()))return (EditText)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){EditText found=input(((ViewGroup)root).getChildAt(i),description);if(found!=null)return found;}return null;}
    private WorkspaceStore.Alias alias(){WorkspaceStore.Alias v=new WorkspaceStore.Alias();v.name="ll";v.command="ls -lah\nprintf 'full preview'";v.category="Files";v.environment="fedora";s.aliases.add(v);return v;}
    @Test public void creationMenuHasOutlinedChoicesAndAliasEditorExplainsPersistence()throws Exception{
        call("newLibraryEntry",new Class<?>[]{});AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(text(picker.getWindow().getDecorView(),"Новая команда"));text(picker.getWindow().getDecorView(),"Новый alias").performClick();shadowOf(Looper.getMainLooper()).idle();
        AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();View root=editor.getWindow().getDecorView();assertNotNull(text(root,"Окружение"));assertNotNull(text(root,"Категория"));assertNotNull(input(root,"Имя alias"));assertNotNull(text(root,"Будет автоматически доступен в новых терминальных сессиях выбранного окружения."));
        input(root,"Имя alias").setText("bad name");editor.getButton(-1).performClick();assertTrue(editor.isShowing());assertNotNull(input(root,"Имя alias").getError());assertTrue(s.aliases.isEmpty());
    }
    @Test public void libraryFiltersAndCategoriesSeparateCommandsAliasesAndNotes()throws Exception{
        alias();WorkspaceStore.Command cmd=new WorkspaceStore.Command();cmd.name="ordinary";cmd.command="pwd";cmd.category="Build";s.commands.add(cmd);
        call("show",new Class<?>[]{Class.forName("com.termux.app.workspace.WorkspaceController$Screen"),boolean.class},Enum.valueOf((Class)Class.forName("com.termux.app.workspace.WorkspaceController$Screen"),"COMMANDS"),true);
        View root=a.findViewById(R.id.workspace_content);assertNotNull(text(root,"ordinary"));text(root,"Все Alias").performClick();root=a.findViewById(R.id.workspace_content);assertNotNull(text(root,"ll"));assertNull(text(root,"ordinary"));
        call("chooseLibraryCategory",new Class<?>[]{});AlertDialog categories=ShadowAlertDialog.getLatestAlertDialog();View menu=categories.getWindow().getDecorView();assertNotNull(text(menu,"Категории команд"));assertNotNull(text(menu,"Категории Alias"));assertNotNull(text(menu,"Files"));text(menu,"Категории команд").performClick();assertNotNull(text(menu,"Build"));assertNull(text(menu,"Files"));
    }
    @Test public void termuxRestartHintIsOnlyVisibleForTermuxAndDeleteUsesSameScope()throws Exception{
        String hint="Перезапустите вкладку для добавления Alias";
        WorkspaceStore.Alias value=alias();value.environment="termux";call("editAlias",new Class<?>[]{WorkspaceStore.Alias.class},value);
        AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();assertEquals(View.VISIBLE,text(editor.getWindow().getDecorView(),hint).getVisibility());editor.dismiss();
        value.environment="debian";call("editAlias",new Class<?>[]{WorkspaceStore.Alias.class},value);editor=ShadowAlertDialog.getLatestAlertDialog();assertEquals(View.GONE,text(editor.getWindow().getDecorView(),hint).getVisibility());
        text(editor.getWindow().getDecorView(),"Debian ▾").performClick();AlertDialog chooser=ShadowAlertDialog.getLatestAlertDialog();chooser.getListView().performItemClick(null,0,0);assertEquals(View.VISIBLE,text(editor.getWindow().getDecorView(),hint).getVisibility());editor.dismiss();
        value.environment="termux";call("openAlias",new Class<?>[]{WorkspaceStore.Alias.class,Runnable.class},value,null);AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();text(preview.getWindow().getDecorView(),"Удалить").performClick();shadowOf(Looper.getMainLooper()).idle();AlertDialog confirmation=ShadowAlertDialog.getLatestAlertDialog();TextView message=confirmation.findViewById(android.R.id.message);assertTrue(message.getText().toString().contains("Перезапустите вкладку после удаления Alias"));confirmation.getButton(-2).performClick();assertEquals(1,s.aliases.size());
    }
    @Test public void deleteConfirmationExplainsLoadedShellStateAndCancelKeepsAlias()throws Exception{
        WorkspaceStore.Alias value=alias();call("openAlias",new Class<?>[]{WorkspaceStore.Alias.class,Runnable.class},value,null);AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();text(preview.getWindow().getDecorView(),"Удалить").performClick();shadowOf(Looper.getMainLooper()).idle();AlertDialog confirm=ShadowAlertDialog.getLatestAlertDialog();TextView message=confirm.findViewById(android.R.id.message);assertTrue(message.getText().toString().contains("закройте эту сессию и откройте её заново"));confirm.getButton(-2).performClick();assertEquals(1,s.aliases.size());assertSame(value,s.aliases.get(0));
    }
    @Test public void favoritesAreIndependentVirtualCategoriesAndAliasToggleOnlyChangesMetadata()throws Exception{
        WorkspaceStore.Alias value=alias();WorkspaceStore.Command command=new WorkspaceStore.Command();command.name="favorite command";command.command="pwd";command.favorite=true;s.commands.add(command);
        call("openAlias",new Class<?>[]{WorkspaceStore.Alias.class,Runnable.class},value,null);AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();text(preview.getWindow().getDecorView(),"В избранное").performClick();assertTrue(value.favorite);
        call("chooseLibraryCategory",new Class<?>[]{});AlertDialog categories=ShadowAlertDialog.getLatestAlertDialog();text(categories.getWindow().getDecorView(),"Категории Alias").performClick();text(categories.getWindow().getDecorView(),"Избранное").performClick();
        Field af=WorkspaceController.class.getDeclaredField("aliasFavorites"),cf=WorkspaceController.class.getDeclaredField("favorites");af.setAccessible(true);cf.setAccessible(true);assertTrue(af.getBoolean(c));assertFalse(cf.getBoolean(c));
        call("chooseLibraryCategory",new Class<?>[]{});categories=ShadowAlertDialog.getLatestAlertDialog();text(categories.getWindow().getDecorView(),"Категории команд").performClick();text(categories.getWindow().getDecorView(),"Избранное").performClick();assertTrue(cf.getBoolean(c));assertTrue(af.getBoolean(c));
    }
    @Test public void starHasThreeTabsAliasSearchAndFullPreviewWithoutSending()throws Exception{
        WorkspaceStore.Alias value=alias();call("chooseInputCommand",new Class<?>[]{});AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();View root=picker.getWindow().getDecorView();assertNotNull(text(root,"Команды"));assertNotNull(text(root,"Заметки"));text(root,"Alias").performClick();assertEquals(1,picker.getListView().getCount());assertSame(value,picker.getListView().getItemAtPosition(0));
        EditText search=input(root,"Поиск в меню звёздочки");search.setText("missing");assertEquals(0,picker.getListView().getCount());search.setText("fedora");assertEquals(1,picker.getListView().getCount());picker.getListView().performItemClick(null,0,0);shadowOf(Looper.getMainLooper()).idle();AlertDialog preview=ShadowAlertDialog.getLatestAlertDialog();View body=preview.getWindow().getDecorView();assertNotNull(text(body,value.command));assertNotNull(text(body,"Вставить в сессию"));assertNotNull(text(body,"Редактировать"));assertNotNull(text(body,"Удалить"));assertNull(a.getCurrentSession());assertTrue(s.recentCommands.isEmpty());preview.getButton(-1).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(picker.isShowing());assertEquals("fedora",search.getText().toString());
    }
}
