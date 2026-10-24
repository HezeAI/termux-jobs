package com.termux.app.workspace;

import android.app.*;
import android.content.*;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.settings.properties.SharedProperties;
import com.termux.shared.settings.properties.TermuxSharedProperties;
import com.termux.shared.termux.TermuxConstants;
import java.util.Locale;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ServiceLocaleContractTest {
    @Test public void protocolValuesAndCtrlIStayStableInTurkish(){
        Locale previous=Locale.getDefault();try{
            Locale.setDefault(new Locale("tr","TR"));
            assertEquals("file",SharedProperties.toLowerCase("FILE"));
            assertEquals(Integer.valueOf('i'),TermuxSharedProperties.getCodePointForSessionShortcuts("shortcut.create-session","CTRL+I"));
            assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts("shortcut.create-session","missing-plus"));
        }finally{Locale.setDefault(previous);}
    }
    @Test public void notificationActionsAreImmutableAndKeepExplicitTargets()throws Exception{
        // get() attaches a private context without executing service.onCreate or starting terminals.
        TermuxService service=Robolectric.buildService(TermuxService.class).get();
        java.lang.reflect.Method method=TermuxService.class.getDeclaredMethod("buildNotification");method.setAccessible(true);
        Notification n=(Notification)method.invoke(service);assertNotNull(n);
        org.robolectric.shadows.ShadowPendingIntent content=Shadows.shadowOf(n.contentIntent);
        assertEquals(PendingIntent.FLAG_IMMUTABLE,content.getFlags()&PendingIntent.FLAG_IMMUTABLE);
        assertEquals(TermuxActivity.class.getName(),content.getSavedIntent().getComponent().getClassName());
        assertEquals(2,n.actions.length);
        for(Notification.Action action:n.actions){
            org.robolectric.shadows.ShadowPendingIntent pending=Shadows.shadowOf(action.actionIntent);
            assertEquals(PendingIntent.FLAG_IMMUTABLE,pending.getFlags()&PendingIntent.FLAG_IMMUTABLE);
            Intent saved=pending.getSavedIntent();assertEquals(TermuxService.class.getName(),saved.getComponent().getClassName());
            assertTrue(saved.getAction().equals(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_STOP_SERVICE)||saved.getAction().equals(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.ACTION_WAKE_LOCK));
        }
    }
}
