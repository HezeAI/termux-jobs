package com.termux.app.workspace;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ServerKeyPickerUiTest {
    private View find(View v,String description){if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),description);if(found!=null)return found;}return null;}
    private Spinner spinner(View v){if(v instanceof Spinner)return(Spinner)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Spinner s=spinner(((ViewGroup)v).getChildAt(i));if(s!=null)return s;}return null;}
    @Test public void keyModeShowsSecretFieldAndPickerIncludesLocalSources()throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"en");
        WorkspaceServers servers=new WorkspaceServers(a,(name,command)->fail("No terminal should be launched"));
        try{
            Method edit=WorkspaceServers.class.getDeclaredMethod("edit",ServerProfiles.Profile.class);edit.setAccessible(true);edit.invoke(servers,new Object[]{null});
            AlertDialog profile=ShadowAlertDialog.getLatestAlertDialog();View body=profile.getWindow().getDecorView();Spinner auth=spinner(body);assertNotNull(auth);
            EditText passphrase=(EditText)find(body,"Passphrase (if required)");assertNotNull(passphrase);assertFalse(passphrase.isSaveEnabled());
            auth.setSelection(1);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(View.VISIBLE,((View)passphrase.getParent()).getVisibility());
            View picker=find(body,"Choose key file: Termux, Ubuntu or device storage");assertNotNull(picker);picker.performClick();
            AlertDialog choices=ShadowAlertDialog.getLatestAlertDialog();ListView list=choices.getListView();assertTrue(list.getAdapter().getCount()>=2);assertEquals("Termux",list.getAdapter().getItem(0));assertEquals("Device storage",list.getAdapter().getItem(list.getAdapter().getCount()-1));
            choices.dismiss();profile.dismiss();
        }finally{servers.close();}
    }
    @Test public void cancelRemovesPendingImportsAndSaveKeepsOnlySelectedCanonicalPath()throws Exception {
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();
        WorkspaceSshKeyPicker picker=new WorkspaceSshKeyPicker(a);
        java.io.File chosen=SshKeyFiles.createPrivate(a.getFilesDir()),unused=SshKeyFiles.createPrivate(a.getFilesDir());
        java.lang.reflect.Field f=WorkspaceSshKeyPicker.class.getDeclaredField("imported");f.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.Set<java.io.File> imports=(java.util.Set<java.io.File>)f.get(picker);imports.add(chosen);imports.add(unused);
        picker.retain(chosen.getParent()+"/./"+chosen.getName());picker.close();assertTrue(chosen.exists());assertFalse(unused.exists());chosen.delete();
        WorkspaceSshKeyPicker cancelled=new WorkspaceSshKeyPicker(a);java.io.File pending=SshKeyFiles.createPrivate(a.getFilesDir());
        @SuppressWarnings("unchecked") java.util.Set<java.io.File> pendingImports=(java.util.Set<java.io.File>)f.get(cancelled);pendingImports.add(pending);
        cancelled.close();assertFalse(pending.exists());
    }

}
