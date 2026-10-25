package com.termux.x11;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.view.*;
import androidx.annotation.RequiresApi;
import com.droiddeck.launcher.wayland.WaylandCompositor;
import java.io.*;
import java.nio.file.*;

/** Private Android surface; one native compositor per owning Arch service VM. */
@RequiresApi(30)
final class EmbeddedWaylandHost {
    private final X11Service service;
    private final WaylandRuntime runtime;
    private final Handler main=new Handler(Looper.getMainLooper());
    private SurfaceControlViewHost host;
    private SurfaceView view;
    private IBinder token;
    private IBinder.DeathRecipient death;
    private String attachment="";
    private boolean nativeStarted,visible,closed,readyNotified;
    private boolean nativeTextInput;
    private int width,height,lastWidth,lastHeight;
    private long nextFrame,startedAt;
    private final X11Touchpad touchpad;
    private final X11ExtraKeys extraKeys=new X11ExtraKeys(this::key);
    private final Runnable onReady;
    private final java.util.function.Consumer<String> onError;
    private final Runnable health=new Runnable(){public void run(){
        if(closed||!nativeStarted)return;
        int state=WaylandCompositor.nativeStartupState();
        if(state<0||state==3||!readyNotified&&SystemClock.elapsedRealtime()-startedAt>30000){onError.accept("Wayland renderer startup failed ("+state+")");return;}
        if(state==2&&!readyNotified){readyNotified=true;onReady.run();}
        main.postDelayed(this,readyNotified?1000:50);
    }};
    private final Choreographer.FrameCallback frame=new Choreographer.FrameCallback(){public void doFrame(long nanos){if(!visible||closed)return;if(nextFrame==0||nanos-nextFrame>1000000000L)nextFrame=nanos;if(nanos>=nextFrame){WaylandCompositor.nativeVsync(nanos);do{nextFrame+=16666667;}while(nextFrame<=nanos);}Choreographer.getInstance().postFrameCallback(this);}};
    EmbeddedWaylandHost(X11Service service,WaylandRuntime runtime,Runnable onReady,java.util.function.Consumer<String> onError){
        this.service=service;this.runtime=runtime;this.onReady=onReady;this.onError=onError;
        ViewConfiguration c=ViewConfiguration.get(service);
        touchpad=new X11Touchpad(new X11Touchpad.Sink(){
            public void move(float dx,float dy){WaylandCompositor.sendPointerDelta(dx,dy);}
            public void button(int button,boolean down){WaylandCompositor.nativeSendSceneInput(3,button==3?273:272,down?1:0);}
            public void scroll(float dx,float dy){WaylandCompositor.nativeSendSceneInput(11,Math.round(dy),Math.round(dx));}
            public void tap(){service.pointerTapped();}
        },c.getScaledTouchSlop(),ViewConfiguration.getLongPressTimeout());
        WaylandCompositor.setTextInputListener((enabled,program,x,y,w,h)->main.post(()->{if(!closed)nativeTextInput=enabled;}));
    }
    Bundle attach(Bundle q){
        String id=q.getString("attachment","");IBinder wanted=q.getBinder("token");int w=bounded(q.getInt("width")),h=bounded(q.getInt("height"));
        if(id.isEmpty()||id.length()>80||wanted==null)throw new IllegalArgumentException("Invalid Wayland host");
        if(host==null||!wanted.equals(token)||!id.equals(attachment)){
            detachHost();Display d=((DisplayManager)service.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(q.getInt("display",0));if(d==null)throw new IllegalArgumentException("Display unavailable");
            Context context=service.createDisplayContext(d);host=new SurfaceControlViewHost(context,d,wanted);
            view=new SurfaceView(context){
                @Override public boolean onTouchEvent(MotionEvent e){
                    if(!nativeStarted)return false;
                    if(e.getToolType(0)==MotionEvent.TOOL_TYPE_FINGER||e.getToolType(0)==MotionEvent.TOOL_TYPE_UNKNOWN)return touchpad.event(e);
                    int action=e.getActionMasked();int x=Math.round(e.getX()*1920/Math.max(1,getWidth())),y=Math.round(e.getY()*1080/Math.max(1,getHeight()));
                    WaylandCompositor.nativeSendPointer(1,x,y);
                    if(action==MotionEvent.ACTION_DOWN)WaylandCompositor.nativeSendSceneInput(3,272,1);
                    if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)WaylandCompositor.nativeSendSceneInput(3,272,0);
                    return true;
                }
                @Override public boolean onGenericMotionEvent(MotionEvent e){
                    if(!nativeStarted)return false;
                    if(e.getActionMasked()==MotionEvent.ACTION_HOVER_MOVE){WaylandCompositor.nativeSendPointer(1,Math.round(e.getX()*1920/Math.max(1,getWidth())),Math.round(e.getY()*1080/Math.max(1,getHeight())));return true;}
                    if(e.getActionMasked()==MotionEvent.ACTION_SCROLL){WaylandCompositor.nativeSendSceneInput(11,Math.round(-e.getAxisValue(MotionEvent.AXIS_VSCROLL)*120),Math.round(-e.getAxisValue(MotionEvent.AXIS_HSCROLL)*120));return true;}return super.onGenericMotionEvent(e);
                }
            };
            view.setFocusable(false);view.setZOrderOnTop(true);
            final SurfaceView ownedView=view;
            view.getHolder().addCallback(new SurfaceHolder.Callback(){
                public void surfaceCreated(SurfaceHolder holder){if(view!=ownedView)return;bindSurface(holder.getSurface());}
                public void surfaceChanged(SurfaceHolder holder,int format,int w,int h){if(view!=ownedView)return;resizeNative(w,h);bindSurface(holder.getSurface());}
                public void surfaceDestroyed(SurfaceHolder holder){if(view==ownedView)suspend();}
            });
            token=wanted;attachment=id;width=w;height=h;
            death=()->main.post(()->{if(token==wanted&&attachment.equals(id))detachHost();});
            try{wanted.linkToDeath(death,0);}catch(RemoteException e){detachHost();throw new IllegalStateException("Wayland host disappeared",e);}
            host.setView(view,w,h);
        }else layout(w,h);
        Bundle b=service.snapshot();b.putParcelable("surface",host.getSurfacePackage());return b;
    }
    private void bindSurface(Surface surface){
        if(closed||!surface.isValid())return;
        try{
            resizeNative(Math.max(1,width),Math.max(1,height));
            if(!nativeStarted){
                WaylandCompositor.nativeSetZeroCopy(false);WaylandCompositor.nativeSetOutputRefreshRate(60);WaylandCompositor.nativeSetOutputSize(lastWidth,lastHeight);
                WaylandCompositor.nativeStartWithSurface(surface,runtime.runtime.getPath(),runtime.driver.getPath()+"/",runtime.library,runtime.nativeDirectory);nativeStarted=true;startedAt=SystemClock.elapsedRealtime();main.post(health);
            }else WaylandCompositor.nativeSetSurface(surface);
            if(!visible){visible=true;nextFrame=0;Choreographer.getInstance().postFrameCallback(frame);}
        }catch(Throwable error){onError.accept("Wayland surface initialization failed");}
    }
    private void resizeNative(int w,int h){
        try{
            X11DisplayMode mode=X11DisplayMode.fit(w,h);if(lastWidth==mode.width&&lastHeight==mode.height)return;lastWidth=mode.width;lastHeight=mode.height;File stage=new File(runtime.runtime,".output-size");
            try(OutputStream out=new FileOutputStream(stage)){out.write((mode.width+" "+mode.height+"\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
            Files.move(stage.toPath(),new File(runtime.runtime,"output-size").toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            if(nativeStarted)WaylandCompositor.nativeResizeOutput(mode.width,mode.height);
        }catch(Exception error){onError.accept("Wayland output configuration failed");}
    }
    void resize(Bundle q){if(host!=null&&attachment.equals(q.getString("attachment")))layout(bounded(q.getInt("width")),bounded(q.getInt("height")));}
    private void layout(int w,int h){if(width==w&&height==h)return;width=w;height=h;host.relayout(w,h);resizeNative(w,h);}
    void input(Bundle q){
        if(host==null||!nativeStarted||!attachment.equals(q.getString("attachment","")))return;
        if(q.getBoolean("extra",false)){extraKeys.key(q.getInt("key",-1),q.getBoolean("down"));return;}
        String text=q.getString("text");
        if(text==null){key(q.getInt("key",-1),q.getBoolean("down"));return;}
        if(text.length()>1024)throw new IllegalArgumentException("Wayland text too large");
        if(nativeTextInput){WaylandCompositor.textInputCommit(text);return;}
        for(int pos=0;pos<text.length();){
            int cp=text.codePointAt(pos);pos+=Character.charCount(cp);int mapped=WaylandKeys.unicode(cp);
            if(mapped>=0){WaylandCompositor.nativeSendKey(mapped,1);WaylandCompositor.nativeSendKey(mapped,0);continue;}
            KeyEvent[] events=KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(Character.toChars(cp));
            if(events!=null)for(KeyEvent event:events)key(event.getKeyCode(),event.getAction()==KeyEvent.ACTION_DOWN);
        }
    }
    private void key(int code,boolean down){int evdev=WaylandKeys.evdev(code);if(evdev>=0&&nativeStarted)WaylandCompositor.nativeSendKey(evdev,down?1:0);}
    void detach(String id){if(attachment.equals(id))detachHost();}
    private void suspend(){visible=false;Choreographer.getInstance().removeFrameCallback(frame);touchpad.cancel();extraKeys.release();if(nativeStarted){WaylandCompositor.nativeSendSceneInput(3,272,0);WaylandCompositor.nativeSendSceneInput(3,273,0);WaylandCompositor.nativeSetSurface(null);}}
    private void detachHost(){
        if(token!=null&&death!=null)try{token.unlinkToDeath(death,0);}catch(java.util.NoSuchElementException ignored){}
        suspend();if(host!=null)host.release();host=null;view=null;token=null;death=null;attachment="";width=height=0;
    }
    boolean rendererConnected(){return readyNotified&&!closed;}
    boolean hostAttached(){return host!=null;}
    void release(){if(closed)return;detachHost();closed=true;main.removeCallbacks(health);WaylandCompositor.setTextInputListener(null);}
    private static int bounded(int n){if(n<1||n>8192)throw new IllegalArgumentException("Invalid Wayland viewport");return n;}
}
