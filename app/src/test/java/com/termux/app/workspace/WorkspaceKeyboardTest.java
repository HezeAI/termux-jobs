package com.termux.app.workspace;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.TermuxTerminalViewClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class WorkspaceKeyboardTest {
    @Test public void inheritedDisableFlagCannotBlockWorkspaceFieldsOrReturnFromBackground() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        WorkspaceController c=new WorkspaceController(a);Field workspace=TermuxActivity.class.getDeclaredField("mWorkspace");workspace.setAccessible(true);workspace.set(a,c);
        assertEnabled(a);assertFalse(a.getTerminalView().isSoftKeyboardInputEnabled());
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        new TermuxTerminalViewClient(a,null).setSoftKeyboardState(true,false);assertEnabled(a);
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        EditText input=(EditText)((ViewGroup)a.findViewById(R.id.workspace_input)).getChildAt(1);input.performClick();shadowOf(Looper.getMainLooper()).idle();assertEnabled(a);assertTrue(input.getShowSoftInputOnFocus());
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);c.toggleKeyboard();shadowOf(Looper.getMainLooper()).idle();assertEnabled(a);c.close();
    }
    @Test public void enablingInteractiveInputDoesNotRestoreGlobalKeyboardBlock() throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);a.setContentView(R.layout.activity_termux);
        Field tv=TermuxActivity.class.getDeclaredField("mTerminalView");tv.setAccessible(true);tv.set(a,a.findViewById(R.id.terminal_view));WorkspaceController c=new WorkspaceController(a);
        java.lang.reflect.Method set=WorkspaceController.class.getDeclaredMethod("setInteractiveInput",boolean.class);set.setAccessible(true);
        for(boolean interactive:new boolean[]{true,false}){a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);set.invoke(c,interactive);assertEnabled(a);assertEquals(interactive,a.getTerminalView().isSoftKeyboardInputEnabled());}
        c.close();
    }
    private static void assertEnabled(TermuxActivity a){assertEquals(0,a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,a.getWindow().getAttributes().softInputMode&WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);}
}
