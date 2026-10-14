package com.termux.app.workspace;

import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class WorkspaceUpdateProgressTest {
    @Test public void checkingDownloadAndVerificationHaveDistinctProgressStates(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();
        WorkspaceUpdates.UpdateProgress view=new WorkspaceUpdates.UpdateProgress(a);
        assertTrue(view.bar.isIndeterminate());String checking=view.status.getText().toString();
        view.downloaded(500,1000);assertFalse(view.bar.isIndeterminate());assertEquals(50,view.bar.getProgress());assertTrue(view.status.getText().toString().startsWith("50%"));
        view.downloaded(1000,1000);assertEquals(100,view.bar.getProgress());
        view.verifying();assertTrue(view.bar.isIndeterminate());assertNotEquals(checking,view.status.getText().toString());a.finish();
    }
    @Test public void progressHandlesUnknownSizeAndClampsOnlyUiValues(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();
        WorkspaceUpdates.UpdateProgress view=new WorkspaceUpdates.UpdateProgress(a);
        view.downloaded(0,0);assertTrue(view.bar.isIndeterminate());
        view.downloaded(-10,1000);assertEquals(0,view.bar.getProgress());
        view.downloaded(2000,1000);assertEquals(100,view.bar.getProgress());a.finish();
    }
}
