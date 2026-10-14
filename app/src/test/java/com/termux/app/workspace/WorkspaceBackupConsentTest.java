package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.CheckBox;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class WorkspaceBackupConsentTest {
    @Test public void createRequiresConsentAndUncheckingDisablesItAgain() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        CheckBox consent=new CheckBox(activity);
        AlertDialog dialog=new AlertDialog.Builder(activity).setView(consent).setPositiveButton("Create",null).create();
        dialog.show();WorkspaceBackupUi.bindConsent(dialog,consent);
        assertFalse(dialog.getButton(-1).isEnabled());
        assertEquals(0.4f,dialog.getButton(-1).getAlpha(),0.01f);
        consent.setChecked(true);assertTrue(dialog.getButton(-1).isEnabled());
        assertEquals(1f,dialog.getButton(-1).getAlpha(),0.01f);
        consent.setChecked(false);assertFalse(dialog.getButton(-1).isEnabled());
        dialog.dismiss();activity.finish();
    }
    @Test public void aNewReviewDoesNotInheritConsentFromThePreviousDialog() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        for(int i=0;i<2;i++){
            CheckBox consent=new CheckBox(activity);
            AlertDialog dialog=new AlertDialog.Builder(activity).setView(consent).setPositiveButton("Create",null).create();
            dialog.show();WorkspaceBackupUi.bindConsent(dialog,consent);
            assertFalse(dialog.getButton(-1).isEnabled());consent.setChecked(true);
            assertTrue(dialog.getButton(-1).isEnabled());dialog.dismiss();
        }
        activity.finish();
    }
}
