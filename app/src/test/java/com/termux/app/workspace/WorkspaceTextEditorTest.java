package com.termux.app.workspace;

import android.app.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceTextEditorTest {
    private TermuxActivity activity(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);return a;}
    private EditText input(WorkspaceTextEditor e)throws Exception{Field f=WorkspaceTextEditor.class.getDeclaredField("input");f.setAccessible(true);return (EditText)f.get(e);}
    @Test public void markdownMenuPlacesEditorAfterPreviewButApkHasNoEditor()throws Exception{
        TermuxActivity a=activity();WorkspaceFileBrowser browser=new WorkspaceFileBrowser(a,new WorkspaceStore.Session(),"/root",null);Method open=WorkspaceFileBrowser.class.getDeclaredMethod("openFile",ProjectFiles.Entry.class);open.setAccessible(true);
        open.invoke(browser,new ProjectFiles.Entry("/root","README.md",false,10));AlertDialog menu=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Просмотреть",menu.getListView().getItemAtPosition(0));assertEquals("Редактировать",menu.getListView().getItemAtPosition(1));menu.dismiss();
        open.invoke(browser,new ProjectFiles.Entry("/root","app.apk",false,10));menu=ShadowAlertDialog.getLatestAlertDialog();for(int i=0;i<menu.getListView().getCount();i++)assertNotEquals("Редактировать",menu.getListView().getItemAtPosition(i));menu.dismiss();browser.close();
    }
    @Test public void dirtyCancelAsksAndContinueKeepsTextWithoutSaving()throws Exception{
        TermuxActivity a=activity();ProjectFiles files=new ProjectFiles(a,new WorkspaceStore.Session());ExecutorService worker=Executors.newSingleThreadExecutor();int[] saves={0};WorkspaceTextEditor editor=new WorkspaceTextEditor(a,files,worker,new TextFileEdits.Snapshot("/root/README.md","unused","# Title\n"),()->saves[0]++);
        try{editor.show();shadowOf(Looper.getMainLooper()).idle();AlertDialog original=ShadowAlertDialog.getLatestAlertDialog();EditText text=input(editor);assertEquals("# Title\n",text.getText().toString());assertFalse(text.isSaveEnabled());assertEquals(android.view.inputmethod.EditorInfo.IME_ACTION_NONE,text.getImeOptions()&android.view.inputmethod.EditorInfo.IME_MASK_ACTION);
            text.setText("# Changed\nSecond line");original.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();AlertDialog confirm=ShadowAlertDialog.getLatestAlertDialog();assertTrue(original.isShowing());assertNotSame(original,confirm);confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(original.isShowing());assertEquals("# Changed\nSecond line",text.getText().toString());assertEquals(0,saves[0]);
            original.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertFalse(original.isShowing());assertEquals(0,saves[0]);
        }finally{editor.close();files.close();worker.shutdownNow();}
    }
    @Test public void saveFailureKeepsUserTextAndDialogOpen()throws Exception{
        TermuxActivity a=activity();ProjectFiles files=new ProjectFiles(a,new WorkspaceStore.Session());files.close();ExecutorService worker=Executors.newSingleThreadExecutor();WorkspaceTextEditor editor=new WorkspaceTextEditor(a,files,worker,new TextFileEdits.Snapshot("/root/README.md","unused","before"),()->fail("closed transport must not report saved"));
        try{editor.show();shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();input(editor).setText("changed text");dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();worker.submit(()->{}).get(5,TimeUnit.SECONDS);shadowOf(Looper.getMainLooper()).idle();assertTrue(dialog.isShowing());assertEquals("changed text",input(editor).getText().toString());assertTrue(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());}finally{editor.close();worker.shutdownNow();}
    }
}
