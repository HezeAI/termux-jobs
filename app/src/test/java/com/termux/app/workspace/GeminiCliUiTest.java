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
public class GeminiCliUiTest {
    private View find(View root,String text){if(root.getVisibility()!=View.VISIBLE)return null;if(root instanceof TextView&&((TextView)root).getText().toString().equals(text))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=find(((ViewGroup)root).getChildAt(i),text);if(v!=null)return v;}return null;}
    @Test public void editingGeminiProvidesExactlyThreeChatChoicesAndPersistsMode() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try {
            WorkspaceStore.Session s=new WorkspaceStore.Session();s.geminiAgent=true;s.environment="ubuntu";s.name="Gemini CLI";s.directory="/root";
            Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);((WorkspaceStore)f.get(c)).sessions.add(s);
            Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(c,s,false);shadowOf(Looper.getMainLooper()).idle();
            AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();View chat=find(editor.getWindow().getDecorView(),WorkspaceText.tr("Чат: ")+WorkspaceText.tr("Новый чат"));assertNotNull(chat);chat.performClick();shadowOf(Looper.getMainLooper()).idle();
            AlertDialog picker=ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,picker.getListView().getCount());assertEquals(WorkspaceText.tr("Выбрать чат"),picker.getListView().getItemAtPosition(1));
            picker.getListView().performItemClick(null,1,1);shadowOf(Looper.getMainLooper()).idle();assertNotNull(find(editor.getWindow().getDecorView(),WorkspaceText.tr("Чат: ")+WorkspaceText.tr("Выбрать чат")));
            View mode=find(editor.getWindow().getDecorView(),(WorkspaceLanguage.code(a).equals("ru")?"Режим прав: ":"Permission mode: ")+WorkspaceText.tr("Подтверждать действия"));assertNotNull(mode);mode.performClick();shadowOf(Looper.getMainLooper()).idle();
            picker=ShadowAlertDialog.getLatestAlertDialog();picker.getListView().performItemClick(null,2,2);shadowOf(Looper.getMainLooper()).idle();
            editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(s.geminiAgent);assertEquals("choose",s.geminiChat);assertEquals("plan",s.geminiMode);assertEquals("ubuntu",s.environment);assertEquals("",s.aiAgent);
        } finally {c.close();}
    }
    @Test public void googleIsDisabledAndVertexReplacesTerminalChoice() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);String[] chosen={"api"};GeminiAuthUi.choose(a,"api",v->chosen[0]=v);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();android.widget.ListAdapter adapter=dialog.getListView().getAdapter();assertEquals(3,adapter.getCount());assertFalse(adapter.isEnabled(0));assertTrue(adapter.isEnabled(1));assertTrue(adapter.isEnabled(2));View google=adapter.getView(0,null,dialog.getListView());assertFalse(google.isEnabled());assertTrue(google.getAlpha()<1);dialog.getListView().performItemClick(null,0,0);assertEquals("api",chosen[0]);assertTrue(dialog.isShowing());dialog.getListView().performItemClick(null,2,2);assertEquals("vertex",chosen[0]);assertFalse(dialog.isShowing());
    }
    @Test public void vertexCloudFieldsAreConditionalAndPersistWithoutChangingWorkingFolderOrChat() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        try {WorkspaceStore.Session s=new WorkspaceStore.Session();s.geminiAgent=true;s.environment="ubuntu";s.name="Gemini cloud";s.directory="/root/project";s.geminiAuth="api";s.geminiChat="id";String thread=s.geminiThread;Field f=WorkspaceController.class.getDeclaredField("store");f.setAccessible(true);WorkspaceStore store=(WorkspaceStore)f.get(c);store.sessions.add(s);Method edit=WorkspaceController.class.getDeclaredMethod("editSession",WorkspaceStore.Session.class,boolean.class);edit.setAccessible(true);edit.invoke(c,s,false);shadowOf(Looper.getMainLooper()).idle();AlertDialog editor=ShadowAlertDialog.getLatestAlertDialog();View root=editor.getWindow().getDecorView();android.widget.EditText project=root.findViewWithTag("gemini-cloud-project"),location=root.findViewWithTag("gemini-cloud-location");assertEquals(View.GONE,((View)project.getParent()).getVisibility());android.widget.EditText apiKey=root.findViewWithTag("gemini-api-key");assertNotNull(apiKey);assertEquals(View.VISIBLE,((View)apiKey.getParent()).getVisibility());assertFalse(apiKey.isSaveEnabled());assertTrue((apiKey.getInputType()&android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)!=0);View auth=find(root,WorkspaceText.tr("Вход Gemini: ")+GeminiAuthUi.label(a,"api"));assertNotNull(auth);auth.performClick();AlertDialog choices=ShadowAlertDialog.getLatestAlertDialog();choices.getListView().performItemClick(null,2,2);shadowOf(Looper.getMainLooper()).idle();assertEquals(View.VISIBLE,((View)project.getParent()).getVisibility());assertEquals(View.GONE,((View)apiKey.getParent()).getVisibility());project.setText("/root/project");editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertTrue(editor.isShowing());assertNotNull(project.getError());assertEquals("api",s.geminiAuth);project.setText("test-cloud-project");location.setText("us-central1");editor.getButton(AlertDialog.BUTTON_POSITIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertEquals("vertex",s.geminiAuth);assertEquals("test-cloud-project",s.geminiCloudProject);assertEquals("us-central1",s.geminiCloudLocation);assertEquals("/root/project",s.directory);assertEquals(thread,s.geminiThread);store.decode(store.encode().toString());WorkspaceStore.Session restored=store.sessions.get(0);assertEquals("vertex",restored.geminiAuth);assertEquals("test-cloud-project",restored.geminiCloudProject);assertEquals("us-central1",restored.geminiCloudLocation);
        } finally {c.close();}
    }

}
