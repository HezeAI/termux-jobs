package com.termux.app.workspace;
import android.app.Application;
import android.app.AlertDialog;
import com.termux.R;
import com.termux.app.TermuxActivity;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ChatAttachmentPickerTest {
 @Test public void storagePermissionIsRequestedOnlyAfterPhoneStorageIsSelected(){
  TermuxActivity a=Robolectric.buildActivity(TermuxActivity.class).get();a.setTheme(R.style.Theme_Termux_Black);java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();WorkspaceLocalFilePicker picker=new WorkspaceLocalFilePicker(a,ready->requests.incrementAndGet());
  try{
   picker.chooseChatAttachment((p,e)->fail());AlertDialog choice=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(2,choice.getListView().getCount());assertEquals(0,requests.get());Shadows.shadowOf(choice).clickOnItem(0);AlertDialog env=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(env.getListView().getCount()>=1);assertEquals(0,requests.get());env.dismiss();
   picker.chooseChatAttachment((p,e)->fail());choice=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(0,requests.get());Shadows.shadowOf(choice).clickOnItem(1);assertEquals(1,requests.get());
  }finally{picker.close();}
 }
}
