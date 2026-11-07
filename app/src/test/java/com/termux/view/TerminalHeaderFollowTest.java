package com.termux.view;

import android.app.Application;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import com.termux.terminal.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

/** Real parser/buffer/renderer; no PTY, Android session or device graphics. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class TerminalHeaderFollowTest {
    static final int HEADER=60, BOTTOM=100, HEIGHT=475, LINE=20;
    static final class Surface extends Canvas {
        float offset;
        final Deque<Float> saved=new ArrayDeque<>();
        final List<Float> shifts=new ArrayList<>();
        final Map<Float,StringBuilder> text=new TreeMap<>();
        final List<Float> cursors=new ArrayList<>();
        final int cursorColor;
        Surface(int cursorColor){this.cursorColor=cursorColor;}
        @Override public int save(){saved.push(offset);return saved.size();}
        @Override public void restore(){offset=saved.pop();}
        @Override public void translate(float x,float y){offset+=y;shifts.add(y);}
        @Override public void drawTextRun(char[] s,int at,int count,int contextAt,int contextCount,float x,float y,boolean rtl,Paint p){
            text.computeIfAbsent(offset+y,k->new StringBuilder()).append(s,at,count);
        }
        @Override public void drawRect(float l,float t,float r,float b,Paint p){if(p.getColor()==cursorColor)cursors.add(offset+b);}
        boolean headerText(){return text.entrySet().stream().anyMatch(e->e.getKey()>0&&e.getKey()<=HEADER&&!e.getValue().toString().trim().isEmpty());}
    }
    static final class Fixture {
        final TerminalView view=new TerminalView(RuntimeEnvironment.application,null);
        final TerminalEmulator emulator=new TerminalEmulator(new TerminalOutput(){
            public void write(byte[] b,int o,int n){} public void titleChanged(String a,String b){}
            public void onCopyTextToClipboard(String s){} public void onPasteTextFromClipboard(){}
            public void onBell(){} public void onColorsChanged(){}
        },36,15,10,20,2000,null);
        Fixture(){
            TerminalRenderer renderer=new TerminalRenderer(14,Typeface.MONOSPACE);
            ReflectionHelpers.setField(renderer,"mFontLineSpacing",LINE);
            ReflectionHelpers.setField(renderer,"mFontLineSpacingAndAscent",15);
            ReflectionHelpers.setField(renderer,"mFontWidth",10f);
            ReflectionHelpers.setField(view,"mRenderer",renderer);
            view.mEmulator=emulator;view.layout(0,0,360,HEIGHT);
            view.setHeaderTopInset(HEADER);view.setComposerBottomInset(BOTTOM);
            emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR]=0xff12abcd;
        }
        void append(String s){byte[] bytes=s.getBytes(StandardCharsets.UTF_8);emulator.append(bytes,bytes.length);view.onScreenUpdated();}
        Surface draw(){Surface canvas=new Surface(emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR]);view.onDraw(canvas);return canvas;}
        void lines(int count){for(int i=0;i<count;i++)append(String.format(Locale.ROOT,"row%03d\r\n",i));}
        void scroll(int rows){MotionEvent e=MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,80,180,0);view.doScroll(e,rows);e.recycle();}
    }
    @Test public void automaticOutputUsesHeaderHistoryBeforeTheFirstGesture(){
        Fixture f=new Fixture();f.append("FIRST\r\n");Surface initial=f.draw();
        assertFalse(initial.headerText());assertTrue(initial.text.keySet().stream().filter(y->y>0).min(Float::compare).get()>HEADER);
        f.lines(40);assertTrue(f.emulator.getScreen().getActiveTranscriptRows()>0);
        assertEquals(0,f.view.getTopRow());assertTrue("automatic overflow must paint history behind transparent header without a gesture",f.draw().headerText());
        assertEquals(HEADER,f.view.getHeaderTopInset());assertEquals(BOTTOM,f.view.getComposerBottomInset());
        f.scroll(-1);assertTrue(f.draw().headerText());f.scroll(1);
        assertEquals(0,f.view.getTopRow());assertTrue("return to follow must keep the same header area",f.draw().headerText());
    }
    @Test public void followingCursorAndRowsStayStableAcrossTheFirstGesture(){
        Fixture f=new Fixture();f.lines(50);Surface before=f.draw();
        assertFalse(before.cursors.isEmpty());assertTrue(Collections.max(before.cursors)<=HEIGHT-BOTTOM);
        Map<Float,String> body=new TreeMap<>();before.text.forEach((y,s)->{if(y>HEADER)body.put(y,s.toString());});
        f.scroll(-1);assertTrue("cursor below the reserved live viewport must not paint through floating controls",f.draw().cursors.isEmpty());f.scroll(1);Surface after=f.draw();
        for(Map.Entry<Float,String> row:body.entrySet())assertEquals(row.getValue(),after.text.get(row.getKey()).toString());
        assertEquals(before.cursors,after.cursors);assertEquals(HEADER,f.view.getHeaderTopInset());
        f.append("DONE\r\n$ ");assertEquals(0,f.view.getTopRow());assertTrue(f.draw().headerText());
        assertTrue(Collections.max(f.draw().cursors)<=HEIGHT-BOTTOM);
    }
    @Test public void readingAnchorIsPreservedWhenMoreLinesArrive(){
        Fixture f=new Fixture();f.lines(50);f.scroll(-5);
        String held=f.emulator.getScreen().getSelectedText(0,f.view.getTopRow(),35,f.view.getTopRow());
        // Constructor only: never attach/resize or start this synthetic session.
        TerminalSession session=new TerminalSession("unused","unused",new String[0],new String[0],2000,null);
        ReflectionHelpers.setField(session,"mEmulator",f.emulator);f.view.mTermSession=session;
        f.append("additional\r\n");
        assertEquals(held,f.emulator.getScreen().getSelectedText(0,f.view.getTopRow(),35,f.view.getTopRow()));
        assertTrue(f.draw().headerText());
    }
    @Test public void headerHistoryHitTestingMatchesOnlyAlreadyPaintedRows(){
        Fixture f=new Fixture();f.append("FIRST\r\n");
        MotionEvent blank=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,10,2,0);
        assertFalse(f.view.onTouchEvent(blank));blank.recycle();
        f.lines(14);assertEquals(1,f.emulator.getScreen().getActiveTranscriptRows());assertTrue(f.draw().headerText());
        blank=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,10,2,0);assertFalse(f.view.onTouchEvent(blank));blank.recycle();
        MotionEvent text=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,10,HEADER-1,0);
        assertEquals(-1,f.view.getColumnAndRow(text,true)[1]);text.recycle();
        assertEquals("FIRST",f.emulator.getScreen().getSelectedText(0,-1,35,-1));
    }
    @Test public void headerAndCursorRemainConsistentAcrossImeKeysAndRotationGeometry(){
        Fixture f=new Fixture();f.lines(60);
        for(int[] geometry:new int[][]{{360,475,60,100},{360,375,60,100},{360,475,60,160},{700,400,60,100},{360,475,60,100}}){
            int width=geometry[0],height=geometry[1],header=geometry[2],bottom=geometry[3];
            f.view.setHeaderTopInset(header);f.view.setComposerBottomInset(bottom);f.view.layout(0,0,width,height);
            f.emulator.resize(width/10,(height-header-bottom-15)/LINE,10,20);f.view.onScreenUpdated();
            Surface frame=f.draw();assertEquals(0,f.view.getTopRow());assertTrue(frame.headerText());
            assertTrue(Collections.max(frame.cursors)<=height-bottom);assertEquals(header,f.view.getHeaderTopInset());assertEquals(bottom,f.view.getComposerBottomInset());
        }
    }
    @Test public void alternateAndMouseProgramsDoNotAcquireTranscriptUnderHeader(){
        Fixture f=new Fixture();f.lines(40);assertTrue(f.draw().headerText());
        f.append("\u001b[?1049h");assertFalse(f.draw().headerText());f.append("\u001b[?1049l");assertTrue(f.draw().headerText());
        f.append("\u001b[?1000h");assertFalse(f.draw().headerText());f.append("\u001b[?1000l");assertTrue(f.draw().headerText());
    }

}
