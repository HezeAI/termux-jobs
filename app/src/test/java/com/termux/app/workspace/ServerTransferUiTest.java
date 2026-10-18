package com.termux.app.workspace;

import android.app.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.lang.reflect.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ServerTransferUiTest {
    @Test public void progressRowsAreConditionalIndependentAndDoNotRecreateTheCard()throws Exception{
        TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);WorkspaceLanguage.select(a,"en");WorkspaceServers servers=new WorkspaceServers(a,(n,c)->{});AlertDialog up=null,down=null;
        try{
            ServerProfiles.Profile p=new ServerProfiles.Profile();LinearLayout card=new LinearLayout(a);card.setOrientation(1);Method rows=WorkspaceServers.class.getDeclaredMethod("transferRows",LinearLayout.class,ServerProfiles.Profile.class);rows.setAccessible(true);rows.invoke(servers,card,p);
            LinearLayout upload=(LinearLayout)card.getChildAt(0),download=(LinearLayout)card.getChildAt(1);assertEquals(View.GONE,upload.getVisibility());assertEquals(View.GONE,download.getVisibility());
            Field f=WorkspaceServers.class.getDeclaredField("uploads");f.setAccessible(true);WorkspaceServerUploads uploader=(WorkspaceServerUploads)f.get(servers);f=WorkspaceServers.class.getDeclaredField("downloads");f.setAccessible(true);WorkspaceServerDownloads downloader=(WorkspaceServerDownloads)f.get(servers);
            Activity dialogHost=Robolectric.buildActivity(Activity.class).setup().get();up=new AlertDialog.Builder(dialogHost).create();down=new AlertDialog.Builder(dialogHost).create();up.show();down.show();
            uploader.presentation.begin(p.id,up,"one.txt · 50%");downloader.presentation.begin(p.id,down,"two.txt · 20%");assertTrue(servers.transfersActive());assertEquals(View.VISIBLE,upload.getVisibility());assertEquals(View.VISIBLE,download.getVisibility());
            uploader.presentation.background();downloader.presentation.background();uploader.presentation.update("one.txt · 60%",60,100);assertSame(upload,card.getChildAt(0));assertSame(download,card.getChildAt(1));assertTrue(((TextView)upload.getChildAt(0)).getText().toString().contains("60%"));assertTrue(((TextView)download.getChildAt(0)).getText().toString().contains("20%"));assertEquals(600,((ProgressBar)upload.getChildAt(1)).getProgress());assertFalse(((ProgressBar)upload.getChildAt(1)).isIndeterminate());assertTrue(((ProgressBar)download.getChildAt(1)).isIndeterminate());upload.performClick();assertTrue(up.isShowing());
            uploader.presentation.finish();assertEquals(View.GONE,upload.getVisibility());assertEquals(View.VISIBLE,download.getVisibility());downloader.presentation.finish();assertEquals(View.GONE,download.getVisibility());assertFalse(servers.transfersActive());
        }finally{if(up!=null)up.dismiss();if(down!=null)down.dismiss();servers.close();}
    }
}
