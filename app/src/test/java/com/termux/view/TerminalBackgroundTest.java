package com.termux.view;

import android.app.Application;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import com.termux.terminal.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Actual shared TerminalView/renderer drawing calls, without a PTY or working session. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class TerminalBackgroundTest {
    static class Surface extends Canvas {
        int color=0xff191a1c, fills;
        final List<Integer> rectangles=new ArrayList<>();
        @Override public void drawColor(int c){color=c;fills++;}
        @Override public void drawColor(int c,PorterDuff.Mode mode){assertEquals(PorterDuff.Mode.SRC,mode);color=c;fills++;}
        @Override public void drawRect(float l,float t,float r,float b,Paint p){rectangles.add(p.getColor());}
    }
    private TerminalEmulator emulator(){
        TerminalOutput output=new TerminalOutput(){public void write(byte[] b,int o,int n){}public void titleChanged(String a,String b){}public void onCopyTextToClipboard(String s){}public void onPasteTextFromClipboard(){}public void onBell(){}public void onColorsChanged(){}};
        TerminalEmulator e=new TerminalEmulator(output,20,4,10,20,2000,null);
        e.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]=Color.BLACK;
        append(e,"\u001b[?25l");return e;
    }
    private void append(TerminalEmulator e,String s){byte[] b=s.getBytes(StandardCharsets.UTF_8);e.append(b,b.length);}
    private TerminalView view(TerminalEmulator e){TerminalView v=new TerminalView(RuntimeEnvironment.application,null);ReflectionHelpers.setField(v,"mEmulator",e);ReflectionHelpers.setField(v,"mRenderer",new TerminalRenderer(14,Typeface.MONOSPACE));return v;}
    @Test public void ordinaryAndAgentTerminalViewsCoverGraphiteHostOnEveryFrame(){
        TerminalView ordinary=view(emulator()),agent=view(emulator());
        for(TerminalView v:new TerminalView[]{ordinary,agent})for(int frame=0;frame<3;frame++){
            Surface surface=new Surface();v.onDraw(surface);
            assertEquals("The terminal must fill its own surface",1,surface.fills);
            assertEquals(Color.BLACK,surface.color);
        }
    }
    @Test public void explicitPaletteAnsiAndReverseVideoKeepTheirMeaning(){
        TerminalEmulator e=emulator();e.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]=0xff123456;
        append(e,"\u001b[41mX\u001b[0m");int[] before=e.mColors.mCurrentColors.clone();TerminalView v=view(e);Surface normal=new Surface();v.onDraw(normal);
        assertEquals(0xff123456,normal.color);assertTrue(normal.rectangles.contains(before[1]));assertArrayEquals(before,e.mColors.mCurrentColors);
        append(e,"\u001b[?5h");Surface reverse=new Surface();v.onDraw(reverse);assertEquals(before[TextStyle.COLOR_INDEX_FOREGROUND],reverse.color);
        append(e,"\u001b[?5l");Surface returned=new Surface();v.onDraw(returned);assertEquals(0xff123456,returned.color);assertArrayEquals(before,e.mColors.mCurrentColors);
    }
    @Test public void unattachedViewStillFillsDefaultBackground(){
        Surface surface=new Surface();view(null).onDraw(surface);assertEquals(1,surface.fills);assertEquals(TerminalColors.COLOR_SCHEME.mDefaultColors[TextStyle.COLOR_INDEX_BACKGROUND],surface.color);
    }
    @Test public void ordinaryTerminalMarginsUseBlackWithoutRecoloringTheScreen(){
        com.termux.app.TermuxActivity a=org.robolectric.Robolectric.buildActivity(com.termux.app.TermuxActivity.class).get();
        a.setTheme(com.termux.R.style.Theme_Termux_Black);a.setContentView(com.termux.R.layout.activity_termux);
        android.view.View surface=a.findViewById(com.termux.R.id.workspace_terminal_surface);
        assertEquals(Color.BLACK,((android.graphics.drawable.ColorDrawable)surface.getBackground()).getColor());
        android.view.View root=a.findViewById(android.R.id.content);root=((android.view.ViewGroup)root).getChildAt(0);
        assertEquals(0xff191a1c,((android.graphics.drawable.ColorDrawable)root.getBackground()).getColor());
    }

}
