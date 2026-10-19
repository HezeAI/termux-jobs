package com.termux.app.workspace;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import com.termux.terminal.*;
import com.termux.view.TerminalView;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class GeminiScrollTest {
    private TerminalEmulator emulator;
    private TerminalSession session;
    private TerminalView view(boolean preserve)throws Exception{
        TerminalOutput out=new TerminalOutput(){public void write(byte[] b,int o,int n){}public void titleChanged(String a,String b){}public void onCopyTextToClipboard(String s){}public void onPasteTextFromClipboard(){}public void onBell(){}public void onColorsChanged(){}};
        emulator=new TerminalEmulator(out,20,4,10,20,2000,null);session=new TerminalSession("unused","unused",new String[0],new String[0],2000,null);session.setPreserveTranscriptScroll(preserve);
        TerminalView v=new TerminalView(RuntimeEnvironment.application,null);v.mTermSession=session;Field f=TerminalView.class.getDeclaredField("mEmulator");f.setAccessible(true);f.set(v,emulator);
        for(int i=0;i<16;i++)append("line "+i+"\r\n");v.onScreenUpdated();return v;
    }
    private void append(String text){byte[] b=text.getBytes(StandardCharsets.UTF_8);emulator.append(b,b.length);}
    private String row(TerminalView v){return emulator.getScreen().getSelectedText(0,v.getTopRow(),19,v.getTopRow());}
    @Test public void incomingGeminiOutputKeepsTheSameTranscriptTextVisible()throws Exception{
        TerminalView v=view(true);v.setTopRow(-5);String before=row(v);append("new one\r\nnew two\r\n");v.onScreenUpdated();assertEquals(-7,v.getTopRow());assertEquals(before,row(v));
        v.onScreenUpdated();assertEquals(-7,v.getTopRow());v.setTopRow(0);append("follow again\r\n");v.onScreenUpdated();assertEquals(0,v.getTopRow());
    }
    @Test public void otherSessionsKeepTheirPreviousFollowOutputBehavior()throws Exception{
        TerminalView v=view(false);v.setTopRow(-5);append("new\r\n");v.onScreenUpdated();assertEquals(0,v.getTopRow());assertFalse(new TerminalSession("unused","unused",new String[0],new String[0],2000,null).preservesTranscriptScroll());
    }
    @Test public void alternateBufferAndSessionSwitchDoNotInheritGeminiAnchoring()throws Exception{
        TerminalView v=view(true);append("\u001b[?1049h");v.setTopRow(-5);v.onScreenUpdated();assertEquals(0,v.getTopRow());assertTrue(session.preservesTranscriptScroll());
        TerminalView other=view(false);other.setTopRow(-4);append("other\r\n");other.onScreenUpdated();assertEquals(0,other.getTopRow());
    }
}
