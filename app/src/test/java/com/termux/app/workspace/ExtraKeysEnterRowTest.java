package com.termux.app.workspace;
import android.app.Application;import android.view.View;import com.termux.shared.terminal.io.extrakeys.*;import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ExtraKeysEnterRowTest {
 @Test public void enterSitsAbovePageUpAndOriginalRowsRemain()throws Exception{ExtraKeysInfo info=new ExtraKeysInfo("[['ESC','PGUP'],['TAB','PGDN']]","default",ExtraKeysConstants.CONTROL_CHARS_ALIASES);ExtraKeyButton[][] rows=info.getMatrix();assertEquals(3,rows.length);assertEquals("ENTER",rows[0][1].getKey());assertEquals("",rows[0][0].getKey());assertEquals("ESC",rows[1][0].getKey());assertEquals("PGUP",rows[1][1].getKey());assertEquals("PGDN",rows[2][1].getKey());assertEquals(Integer.valueOf(android.view.KeyEvent.KEYCODE_ENTER),ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS.get(rows[0][1].getKey()));}
 @Test public void existingEnterAndLayoutsWithoutPageUpAreNotDuplicated()throws Exception{for(String json:new String[]{"[['ENTER','PGUP']]","[['ESC','TAB']]"})assertEquals(1,new ExtraKeysInfo(json,"default",ExtraKeysConstants.CONTROL_CHARS_ALIASES).getMatrix().length);}
}
