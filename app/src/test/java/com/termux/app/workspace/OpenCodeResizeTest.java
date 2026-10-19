package com.termux.app.workspace;

import android.app.Application;
import android.app.Activity;
import android.widget.FrameLayout;
import org.robolectric.android.controller.ActivityController;
import android.os.Looper;
import com.termux.terminal.*;
import com.termux.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows=OpenCodeResizeTest.NativePty.class)
public class OpenCodeResizeTest {
    @Implements(className="com.termux.terminal.JNI",isInAndroidSdk=false)
    public static class NativePty {
        static final List<int[]> writes=new ArrayList<>();static int creates;
        @Implementation public static void __staticInitializer__(){}
        @Implementation public static void setPtyWindowSize(int fd,int rows,int columns,int width,int height){writes.add(new int[]{fd,rows,columns});}
        @Implementation public static int createSubprocess(String cmd,String cwd,String[] args,String[] env,int[] pid,int rows,int cols,int width,int height){creates++;throw new NoProcessStarted();}
    }
    static class NoProcessStarted extends RuntimeException {}
    private ActivityController<Activity> activity;
    private TerminalView view;
    private TerminalSession session;
    private TerminalEmulator emulator;
    private int ascent;
    @Before public void reset(){NativePty.writes.clear();NativePty.creates=0;}
    @After public void cleanup(){if(view!=null)view.setWorkspaceTerminalVisible(false);if(activity!=null)activity.pause().stop().destroy();}
    private Object proxy(Class<?> cls){return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{cls},(p,m,a)->{
        Class<?> type=m.getReturnType();if(type==boolean.class)return false;if(type==int.class||type==Integer.class)return 0;if(type==float.class)return 1f;return null;});}
    private void field(Object obj,String name,Object value)throws Exception{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);f.set(obj,value);}
    private TerminalSession existing(boolean stable,int fd)throws Exception{
        TerminalSession s=new TerminalSession("unused","unused",new String[0],new String[0],2000,(TerminalSessionClient)proxy(TerminalSessionClient.class));
        s.setStableResizeEnabled(stable);s.setPreserveTranscriptScroll(stable);
        TerminalEmulator e=new TerminalEmulator(s,80,35,10,20,2000,null);field(s,"mEmulator",e);field(s,"mTerminalFileDescriptor",fd);return s;
    }
    private void setup(boolean stable)throws Exception{
        activity=Robolectric.buildActivity(Activity.class).setup().visible();
        view=new TerminalView(activity.get(),null);view.setTextSize(16);field(view.mRenderer,"mFontWidth",8f);field(view.mRenderer,"mFontLineSpacing",16);field(view.mRenderer,"mFontLineSpacingAndAscent",3);view.setTerminalViewClient((TerminalViewClient)proxy(TerminalViewClient.class));
        Field f=TerminalRenderer.class.getDeclaredField("mFontLineSpacingAndAscent");f.setAccessible(true);ascent=(int)f.get(view.mRenderer);
        FrameLayout host=new FrameLayout(activity.get());activity.get().setContentView(host);host.addView(view,new FrameLayout.LayoutParams(1,1));Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Robolectric 4.4 attaches this bare Activity but leaves its window GONE.
        // Supply the visibility that Android dispatches for the foreground window.
        Field attach=android.view.View.class.getDeclaredField("mAttachInfo");attach.setAccessible(true);
        Object info=attach.get(view);Field visibility=info.getClass().getDeclaredField("mWindowVisibility");visibility.setAccessible(true);visibility.setInt(info,android.view.View.VISIBLE);
        session=existing(stable,11);emulator=session.getEmulator();view.mTermSession=session;view.mEmulator=emulator;geometry(80,35);assertEquals(0,NativePty.writes.size());assertTrue("attached",view.isAttachedToWindow());assertEquals("window visibility",android.view.View.VISIBLE,view.getWindowVisibility());assertEquals("stable flag",stable,session.usesStableResize());assertEquals("rows",35,(view.getHeight()-ascent)/16);
    }
    private void geometry(int cols,int rows){int width=(int)Math.ceil(cols*view.mRenderer.getFontWidth()),height=rows*view.mRenderer.getFontLineSpacing()+ascent;view.setLayoutParams(new FrameLayout.LayoutParams(width,height));view.layout(0,0,width,height);}
    private void tick(long n){Shadows.shadowOf(Looper.getMainLooper()).idleFor(n,TimeUnit.MILLISECONDS);}
    private void append(String text){byte[] b=text.getBytes(StandardCharsets.UTF_8);emulator.append(b,b.length);}
    private String top(){return emulator.getScreen().getSelectedText(0,view.getTopRow(),79,view.getTopRow());}
    private void seed(){for(int i=0;i<120;i++)append("line "+i+"\r\n");view.onScreenUpdated();}

    @Test public void hiddenGeometryAndReturnToSameSizeDoNotSendPtyResize()throws Exception{
        setup(true);view.setWorkspaceTerminalVisible(false);geometry(80,42);tick(500);assertEquals(35,emulator.mRows);assertTrue(NativePty.writes.isEmpty());
        geometry(80,35);view.setWorkspaceTerminalVisible(true);tick(500);assertTrue(NativePty.writes.isEmpty());
    }
    @Test public void keyboardBurstAppliesOnlyFinalGeometryAndUnchangedTargetDoesNotRepeat()throws Exception{
        setup(true);for(int rows:new int[]{32,28,23,19}){geometry(80,rows);tick(30);}assertTrue(NativePty.writes.isEmpty());tick(65);assertTrue(NativePty.writes.isEmpty());tick(1);
        assertEquals(1,NativePty.writes.size());assertEquals(19,emulator.mRows);view.updateSize();tick(500);assertEquals(1,NativePty.writes.size());
    }
    @Test public void continuousLayoutChangesHaveBoundedWait()throws Exception{
        setup(true);for(int i=0;i<8;i++){geometry(80,34-i);tick(40);}assertEquals(1,NativePty.writes.size());assertEquals(27,emulator.mRows);
    }
    @Test public void hideCancelsPendingResizeAndResumeUsesCurrentGeometry()throws Exception{
        setup(true);geometry(80,20);tick(20);view.setWorkspaceTerminalVisible(false);tick(500);assertTrue(NativePty.writes.isEmpty());geometry(80,29);view.setWorkspaceTerminalVisible(true);tick(96);assertEquals(29,emulator.mRows);assertEquals(1,NativePty.writes.size());
    }
    @Test public void heightOnlyResizeKeepsSameTranscriptTextForShrinkAndExpand()throws Exception{
        setup(true);seed();view.setTopRow(-12);String old=top();geometry(80,19);tick(96);assertEquals(old,top());assertEquals(-28,view.getTopRow());geometry(80,35);tick(96);assertEquals(old,top());assertEquals(-12,view.getTopRow());
    }
    @Test public void blankRowsAndBottomFollowingUseActualHistoryShift()throws Exception{
        setup(true);seed();append("\033[1;1H\033[Jtop\033[1;1H");view.onScreenUpdated();view.setTopRow(-8);String old=top();geometry(80,19);tick(96);assertEquals(-8,view.getTopRow());assertEquals(old,top());
        view.setTopRow(0);geometry(80,35);tick(96);assertEquals(0,view.getTopRow());
    }
    @Test public void existingSameSizeSessionAttachmentDoesNotSendSignalAndOldJobCannotTouchNewSession()throws Exception{
        setup(true);TerminalSession other=existing(true,22);view.attachSession(other);tick(200);assertTrue(NativePty.writes.isEmpty());
        geometry(80,19);TerminalSession third=existing(true,33);view.attachSession(third);tick(500);assertEquals(1,NativePty.writes.size());assertEquals(33,NativePty.writes.get(0)[0]);assertEquals(35,session.getEmulator().mRows);assertEquals(35,other.getEmulator().mRows);assertSame(third,view.mTermSession);
    }
    @Test public void windowDetachAndClosedSessionCancelPendingGeometry()throws Exception{
        setup(true);geometry(80,19);Method detach=TerminalView.class.getDeclaredMethod("onDetachedFromWindow");detach.setAccessible(true);detach.invoke(view);tick(500);assertTrue(NativePty.writes.isEmpty());geometry(80,20);tick(500);assertTrue(NativePty.writes.isEmpty());
        Method attach=TerminalView.class.getDeclaredMethod("onAttachedToWindow");attach.setAccessible(true);attach.invoke(view);field(session,"mShellPid",-1);tick(500);assertTrue(NativePty.writes.isEmpty());
    }
    @Test public void ordinaryAndGeminiSessionsRetainImmediateResizeAndOldScrollRule()throws Exception{
        setup(false);seed();view.setTopRow(-8);view.setWorkspaceTerminalVisible(false);geometry(80,19);assertEquals(1,NativePty.writes.size());assertEquals(19,emulator.mRows);assertEquals(0,view.getTopRow());tick(500);assertEquals(1,NativePty.writes.size());
    }
    @Test public void changedColumnsAndAlternateScreenUseExistingResetPolicy()throws Exception{
        setup(true);seed();view.setTopRow(-8);geometry(70,35);tick(96);assertEquals(0,view.getTopRow());append("\033[?1049h");geometry(70,19);tick(96);assertEquals(0,view.getTopRow());assertTrue(emulator.isAlternateBufferActive());
    }
    @Test public void pendingResizeDisplaysSameBottomTextAndMapsCoordinatesToVisibleRows()throws Exception{
        setup(true);seed();geometry(80,19);assertEquals(0,view.getTopRow());assertEquals(16,view.getVisibleTopRow());assertTrue(NativePty.writes.isEmpty());
        android.view.MotionEvent move=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_MOVE,1,ascent+8,0);
        assertEquals(16,view.getColumnAndRow(move,true)[1]);assertEquals(0,view.getColumnAndRow(move,false)[1]);move.recycle();
        String first=emulator.getScreen().getSelectedText(0,view.getVisibleTopRow(),79,view.getVisibleTopRow());
        view.draw(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(640,400,android.graphics.Bitmap.Config.ARGB_8888)));
        tick(96);assertEquals(0,view.getVisibleTopRow());assertEquals(first,top());
        geometry(80,35);assertEquals(-16,view.getVisibleTopRow());String expanded=emulator.getScreen().getSelectedText(0,-16,79,-16);
        view.draw(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(640,600,android.graphics.Bitmap.Config.ARGB_8888)));
        tick(96);assertEquals(expanded,top());
    }
    @Test public void touchAppliesGeometryBeforeSelectionOrNativePointerCoordinates()throws Exception{
        setup(true);seed();geometry(80,19);assertEquals(16,view.getVisibleTopRow());
        android.view.MotionEvent down=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,1,ascent+8,0);view.onTouchEvent(down);down.recycle();
        assertEquals(1,NativePty.writes.size());assertEquals(19,emulator.mRows);assertEquals(0,view.getVisibleTopRow());tick(500);assertEquals(1,NativePty.writes.size());
    }
    @Test public void transcriptReceiptTapDoesNotAlsoSendNativeMouseClick()throws Exception{
        setup(true);field(session,"mShellPid",1);append("\033[?1000h");assertTrue(emulator.isMouseTrackingActive());
        java.util.concurrent.atomic.AtomicInteger taps=new java.util.concurrent.atomic.AtomicInteger();view.setTranscriptTapListener(line->{taps.incrementAndGet();return true;});
        long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,8,24,0),up=android.view.MotionEvent.obtain(now,now+20,1,8,24,0);view.onTouchEvent(down);view.onTouchEvent(up);down.recycle();up.recycle();
        Field f=TerminalSession.class.getDeclaredField("mTerminalToProcessIOQueue");f.setAccessible(true);Object queue=f.get(session);Method read=queue.getClass().getDeclaredMethod("read",byte[].class,boolean.class);read.setAccessible(true);assertEquals(1,taps.get());assertEquals(0,(int)read.invoke(queue,new byte[100],false));
        view.setTranscriptTapListener(null);now+=1000;down=android.view.MotionEvent.obtain(now,now,0,8,24,0);up=android.view.MotionEvent.obtain(now,now+20,1,8,24,0);view.onTouchEvent(down);view.onTouchEvent(up);down.recycle();up.recycle();assertTrue((int)read.invoke(queue,new byte[100],false)>0);
    }
    @Test public void firstPtyInitializationIsNotDelayed()throws Exception{
        setup(true);TerminalSession fresh=new TerminalSession("unused","unused",new String[0],new String[0],2000,(TerminalSessionClient)proxy(TerminalSessionClient.class));fresh.setStableResizeEnabled(true);
        try{view.attachSession(fresh);fail("Expected test JNI boundary");}catch(NoProcessStarted expected){}assertEquals(1,NativePty.creates);assertTrue(NativePty.writes.isEmpty());
    }
}
