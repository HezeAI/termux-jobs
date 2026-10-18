package com.termux.app.workspace;
import android.app.Activity;
import android.view.View;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class WorkspaceBackupScanHeaderTest {
    @Test public void spinnerReplacesRefreshAndProgressStaysOneLine()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceBackupScanHeader h=new WorkspaceBackupScanHeader(a,"Backup");
        assertEquals(View.VISIBLE,h.refresh.getVisibility());h.start(null,"Scanning");assertFalse(h.refresh.isEnabled());assertEquals(View.INVISIBLE,h.refresh.getVisibility());assertEquals(View.VISIBLE,h.spinner.getVisibility());
        View line=h.status;for(int i=0;i<30;i++)h.progress("Scanning",new JSONObject().put("current","file"+i).put("processed",i));assertSame(line,h.status);assertEquals(1,h.status.getMaxLines());assertTrue(h.status.getText().toString().endsWith("file29"));assertEquals(2,h.view.getChildCount());
        h.finish(null);assertTrue(h.refresh.isEnabled());assertEquals(View.VISIBLE,h.refresh.getVisibility());assertEquals(View.GONE,h.spinner.getVisibility());assertEquals(View.GONE,h.status.getVisibility());h.close();a.finish();
    }
    @Test public void errorsAllowRetryAndClosedHeaderIgnoresLateProgress()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceBackupScanHeader h=new WorkspaceBackupScanHeader(a,"Backup");h.start(null,"Scanning");h.finish("Error");assertEquals("Error",h.status.getText().toString());assertTrue(h.refresh.isEnabled());
        h.start(null,"Retry");h.close();h.progress("Late",new JSONObject().put("current","ignored"));assertEquals("Retry",h.status.getText().toString());assertFalse(h.running);assertEquals(View.GONE,h.spinner.getVisibility());a.finish();
    }
    @Test public void closingScanCancelsOnlyItsOwnedWorker()throws Exception{
        class FakeProcess extends Process {
            int destroyed;
            public java.io.OutputStream getOutputStream(){return new java.io.ByteArrayOutputStream();}
            public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(new byte[0]);}
            public java.io.InputStream getErrorStream(){return getInputStream();}
            public int waitFor(){return 0;}public int exitValue(){return 0;}public void destroy(){destroyed++;}
        }
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceBackupEngine engine=new WorkspaceBackupEngine();FakeProcess owned=new FakeProcess(),other=new FakeProcess();
        java.lang.reflect.Field process=WorkspaceBackupEngine.class.getDeclaredField("process");process.setAccessible(true);process.set(engine,owned);
        WorkspaceBackupScanHeader h=new WorkspaceBackupScanHeader(a,"Backup");h.start(engine,"Scanning");h.close();h.close();assertEquals(1,owned.destroyed);assertEquals(0,other.destroyed);a.finish();
    }
}
