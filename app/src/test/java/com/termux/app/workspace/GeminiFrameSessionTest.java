package com.termux.app.workspace;

import android.app.Application;
import android.os.*;
import com.termux.terminal.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class GeminiFrameSessionTest {
    private int updates;
    private TerminalSession session;
    private TerminalEmulator emulator;
    private Handler handler;
    private Object queue;
    private static final String BEGIN="\033[?2026h", END="\033[?2026l";
    private void setup(boolean enabled)throws Exception{
        TerminalSessionClient client=(TerminalSessionClient)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{TerminalSessionClient.class},(p,m,a)->{if(m.getName().equals("onTextChanged"))updates++;return m.getName().equals("getTerminalCursorStyle")?Integer.valueOf(0):null;});
        session=new TerminalSession("unused","unused",new String[0],new String[0],2000,client);
        emulator=new TerminalEmulator(session,60,8,10,20,2000,client);
        Field f=TerminalSession.class.getDeclaredField("mEmulator");f.setAccessible(true);f.set(session,emulator);
        f=TerminalSession.class.getDeclaredField("mMainThreadHandler");f.setAccessible(true);handler=(Handler)f.get(session);
        f=TerminalSession.class.getDeclaredField("mProcessToTerminalIOQueue");f.setAccessible(true);queue=f.get(session);
        session.setSynchronizedFramesEnabled(enabled);send("\033[3;1H\033[48;2;42;42;42m> Type your message                         \033[0m");updates=0;
    }
    private void send(String text)throws Exception{
        byte[] b=text.getBytes(StandardCharsets.UTF_8);Method write=queue.getClass().getMethod("write",byte[].class,int.class,int.class);write.setAccessible(true);write.invoke(queue,b,0,b.length);
        handler.handleMessage(Message.obtain(handler,1));
    }
    private int background(){return TextStyle.decodeBackColor(emulator.getScreen().getStyleAt(2,15));}
    @Test public void emulatorAndAnyOtherDrawKeepOldPanelUntilCompleteFrame()throws Exception{
        setup(true);int old=background();send(BEGIN+"\033[3;1H\033[2K");assertEquals(0,updates);assertEquals(old,background());
        send("\033[48;2;42;42;42m> New message                               \033[0m"+END);
        assertEquals(1,updates);assertEquals(old,background());
    }
    @Test public void brokenFrameTimesOutAndDisableFlushesOnce()throws Exception{
        setup(true);int old=background();send(BEGIN+"\033[3;1H\033[2K");
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(201,TimeUnit.MILLISECONDS);
        assertEquals(1,updates);assertNotEquals(old,background());send(BEGIN+"pending");session.setSynchronizedFramesEnabled(false);assertEquals(2,updates);
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(300,TimeUnit.MILLISECONDS);assertEquals(2,updates);
    }
    @Test public void ordinarySessionsKeepImmediateOutputAndResetFlushesPending()throws Exception{
        setup(false);int old=background();send(BEGIN+"\033[3;1H\033[2K");assertEquals(1,updates);assertNotEquals(old,background());
        session.setSynchronizedFramesEnabled(true);send(BEGIN+"held");session.reset();assertEquals(3,updates);
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idleFor(300,TimeUnit.MILLISECONDS);assertEquals(3,updates);
    }
}
