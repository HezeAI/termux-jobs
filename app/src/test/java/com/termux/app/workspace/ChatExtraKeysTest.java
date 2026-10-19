package com.termux.app.workspace;
import android.app.Activity;
import android.app.Application;
import android.view.*;
import android.widget.EditText;
import com.termux.view.TerminalView;
import com.termux.shared.terminal.io.extrakeys.ExtraKeysView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ChatExtraKeysTest {
 @Test public void keysEditTextAndNeverEnterBridgePty(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();final int[] delivered={-1,0};EditText field=new EditText(a){@Override public boolean onKeyDown(int code,KeyEvent event){delivered[0]=code;delivered[1]=event.getMetaState();return true;}};a.setContentView(field);field.setText("abcd");field.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(150,View.MeasureSpec.EXACTLY));field.layout(0,0,600,150);field.requestFocus();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();field.setSelection(2);
  TerminalView terminal=new TerminalView(a,null);
  ChatExtraKeys keys=new ChatExtraKeys(terminal,new ExtraKeysView(a,null),()->field);
  keys.onTerminalExtraKeyButtonClick(field,"/",false,false,false,false);assertEquals("ab/cd",field.getText().toString());keys.onTerminalExtraKeyButtonClick(field,"LEFT",false,false,false,false);assertEquals(KeyEvent.KEYCODE_DPAD_LEFT,delivered[0]);keys.onTerminalExtraKeyButtonClick(field,"c",true,false,false,false);assertEquals(KeyEvent.KEYCODE_C,delivered[0]);assertTrue((delivered[1]&KeyEvent.META_CTRL_ON)!=0);assertEquals("ab/cd",field.getText().toString());
 }
 @Test public void disabledComposerCannotBeEdited(){Activity a=Robolectric.buildActivity(Activity.class).setup().get();EditText field=new EditText(a);field.setText("draft");field.setEnabled(false);ChatExtraKeys keys=new ChatExtraKeys(new TerminalView(a,null),new ExtraKeysView(a,null),()->field);keys.onTerminalExtraKeyButtonClick(field,"/",false,false,false,false);assertEquals("draft",field.getText().toString());}
}
