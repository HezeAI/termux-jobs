package com.termux.app.workspace;

import android.Manifest;
import android.app.AlertDialog;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ServerDownloadUiTest {
    private TermuxActivity activity(){TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"en");return a;}
    private ServerProfiles.Profile profile(){ServerProfiles.Profile p=new ServerProfiles.Profile();p.name="test";p.host="example.invalid";p.user="user";return p;}
    @Test public void customDownloadOpensEnvironmentChooserWithoutNetwork()throws Exception{
        TermuxActivity a=activity();WorkspaceServerDownloads download=new WorkspaceServerDownloads(a);try{download.start(profile(),new JSONObject().put("name","file.txt").put("path","/srv/file.txt").put("size",1));AlertDialog chooser=ShadowAlertDialog.getLatestAlertDialog();ListView list=chooser.getListView();assertNotNull(list);assertEquals("Termux",list.getAdapter().getItem(0));assertEquals("Device storage",list.getAdapter().getItem(list.getAdapter().getCount()-1));assertNull(Shadows.shadowOf(a).getNextStartedActivityForResult());}finally{download.close();}
    }
    @Test public void quickDownloadKeepsDownloadFolderAndRequiresConfirmation()throws Exception{
        TermuxActivity a=activity();Shadows.shadowOf(RuntimeEnvironment.application).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);WorkspaceServerDownloads download=new WorkspaceServerDownloads(a);try{download.quick(profile(),new JSONObject().put("name","тест.txt").put("path","/srv/test").put("size",1));AlertDialog confirm=ShadowAlertDialog.getLatestAlertDialog();TextView message=(TextView)confirm.findViewById(android.R.id.message);assertNotNull(message);assertTrue(message.getText().toString().contains("/sdcard/Download/download-"));assertTrue(message.getText().toString().contains("Selected: 1"));assertNotNull(confirm.getButton(AlertDialog.BUTTON_POSITIVE));}finally{download.close();}
    }
}
