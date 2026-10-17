package com.termux.app.workspace;
import android.app.*;
import android.widget.*;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LocalAiPromptUiTest {
 Activity activity;LocalAiUi ui;LocalAiUi.Selection selection;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();ui=new LocalAiUi(activity);selection=new LocalAiUi.Selection(new WorkspaceStore.Session());}
 @After public void close(){ui.close();}
 static CheckBox checkbox(View root){if(root instanceof CheckBox)return (CheckBox)root;if(root instanceof ViewGroup){ViewGroup v=(ViewGroup)root;for(int i=0;i<v.getChildCount();i++){CheckBox c=checkbox(v.getChildAt(i));if(c!=null)return c;}}return null;}
 @Test public void customRightsSaveIsDisabledUntilWarningAcknowledged(){ui.editRights(selection,"Custom instructions",()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(d.getButton(-1).isEnabled());checkbox(d.getWindow().getDecorView()).setChecked(true);assertTrue(d.getButton(-1).isEnabled());d.getButton(-1).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("Custom instructions",selection.rightsPrompt);assertEquals(LocalAiPrompts.scope(selection.profile()),selection.rightsScope);assertEquals("chat",selection.access);}
 @Test public void restoreAutomaticDoesNotChangeActualRights(){selection.access="read";selection.rightsPrompt="I am root";selection.rightsScope=LocalAiPrompts.scope(selection.profile());ui.rightsPrompt(selection,()->{});AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();d.getButton(-3).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("",selection.rightsPrompt);assertEquals("read",selection.access);}
 @Test public void settingsPlacesBehaviorImmediatelyAfterPermissions(){LinearLayout form=ui.settings(selection,()->{});View behavior=form.findViewWithTag("local-behavior");int index=form.indexOfChild(behavior);assertTrue(index>0);assertTrue(((TextView)form.getChildAt(index-1)).getText().toString().matches("(?s)(Права|Permissions):.*"));}
}
