/* JNI declarations derived from Termux:X11, GPL-3.0. See third-party/termux-x11. */
package com.termux.x11;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.opengl.GLES20;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.text.InputType;
import androidx.annotation.Keep;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import java.nio.charset.StandardCharsets;

/** Stage-one display, pointer and IME adapter. Clipboard synchronization is disabled. */
@Keep public final class LorieView extends SurfaceView implements SurfaceHolder.Callback {
    @Keep private final MainActivity activity;
    private long pointer;
    private boolean retainedByService;
    private long pointerMoves,pointerHovers,pointerScrolls,buttonDowns,buttonUps,viewportCallbacks;
    String pointerDiagnostic(){return "move="+pointerMoves+" hover="+pointerHovers+" scroll="+pointerScrolls+" button-down="+buttonDowns+" button-up="+buttonUps+" viewport-callback="+viewportCallbacks;}
    private final java.util.Set<Integer> mouseButtons=new java.util.HashSet<>();
    private final X11Touchpad touchpad;
    private Runnable pointerTap=()->{};
    private X11ScreenSettings screenSettings=X11ScreenSettings.defaults();
    private boolean screenLandscape;
    void screenSettings(X11ScreenSettings settings,boolean landscape){if(screenSettings.width==settings.width&&screenSettings.height==settings.height&&screenLandscape==landscape)return;screenSettings=settings;screenLandscape=landscape;resize(getWidth(),getHeight());}
    private int screenWidth=1280,screenHeight=720;
    private int obscuredBottom,lastModeWidth,lastModeHeight;
    private int viewportLeft,viewportTop,viewportWidth=1,viewportHeight=1;
    private float sourceLeft,sourceTop,sourceWidth=1280,sourceHeight=720;
    public LorieView(MainActivity context) { this(context,context); }
    public LorieView(Context context) { this(context,null); }
    private LorieView(Context context,MainActivity callback) {
        super(context);activity=callback;X11Native.load();pointer=nativeInit();
        android.view.ViewConfiguration config=android.view.ViewConfiguration.get(context);
        touchpad=new X11Touchpad(new X11Touchpad.Sink(){
            public void move(float dx,float dy){pointerMoves++;if(pointer!=0)sendMouseEvent(pointer,dx*screenWidth/Math.max(1,getWidth()),dy*screenHeight/Math.max(1,getHeight()),0,false,true);}
            public void button(int button,boolean down){mouseButton(button,down,true);}
            public void tap(){pointerTap.run();}
            public void scroll(float dx,float dy){pointerScrolls++;if(pointer!=0)sendMouseEvent(pointer,dx,dy,4,false,true);}
        },config.getScaledTouchSlop(),android.view.ViewConfiguration.getLongPressTimeout());
        setFocusable(true);setFocusableInTouchMode(true);
        setBackground(new ColorDrawable(Color.TRANSPARENT));getHolder().addCallback(this);
        setFiltering(pointer,GLES20.GL_LINEAR);setClipboardSyncEnabled(pointer,false,false);
    }
    @Override public void surfaceCreated(SurfaceHolder holder) { }
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height) {
        if(pointer==0)return;surfaceChanged(pointer,holder.getSurface());resize(width,height);
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { suspendSurface(); }
    private void resize(int w,int h) {
        if(pointer==0||w<1||h<1)return;
        X11DisplayMode mode=X11DisplayMode.resolve(w,h,screenSettings,screenLandscape);
        screenWidth=mode.width;screenHeight=mode.height;
        setViewport(pointer,0,0,w,h,screenWidth,screenHeight,Math.max(0,Math.min(h-1,obscuredBottom)));
        if(lastModeWidth!=screenWidth||lastModeHeight!=screenHeight){sendWindowChange(pointer,screenWidth,screenHeight,60,"screen");lastModeWidth=screenWidth;lastModeHeight=screenHeight;}
    }
    void obscuredBottom(int height){height=Math.max(0,height);if(obscuredBottom==height)return;obscuredBottom=height;resize(getWidth(),getHeight());}
    public boolean isConnected() { return pointer!=0&&connected(pointer); }
    public void connectFileDescriptor(int fd) {
        if(pointer==0)throw new IllegalStateException("Display was released");
        connect(pointer,fd);lastModeWidth=lastModeHeight=0;resize(getWidth(),getHeight());
    }
    public void release() {
        cancelPointer();
        if(pointer!=0){getHolder().removeCallback(this);connect(pointer,-1);nativeDestroy(pointer);pointer=0;}
    }
    void retainRendererByService(){retainedByService=true;}
    private void mouseButton(int button,boolean down,boolean relative){
        if(down)buttonDowns++;else buttonUps++;
        if(down)mouseButtons.add(button);else mouseButtons.remove(button);
        if(pointer!=0)sendMouseEvent(pointer,0,0,button,down,relative);
    }
    private void cancelPointer(){touchpad.cancel();for(int button:new java.util.ArrayList<>(mouseButtons))mouseButton(button,false,false);}
    void suspendSurface(){cancelPointer();if(pointer!=0)surfaceChanged(pointer,null);}
    @Override protected void onDetachedFromWindow() { if(retainedByService)suspendSurface();else release();super.onDetachedFromWindow(); }
    @Keep private void setRendererViewport(int left,int top,int width,int height,float sx,float sy,float sw,float sh) {
        post(()->{viewportCallbacks++;viewportLeft=left;viewportTop=top;viewportWidth=Math.max(1,width);viewportHeight=Math.max(1,height);sourceLeft=sx;sourceTop=sy;sourceWidth=sw;sourceHeight=sh;});
    }
    @Keep void resetIme() { if(activity==null)return;post(()->{if(pointer!=0)((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(this);}); }
    @Keep void setClipboardText(String text) { }
    @Keep void requestClipboard() { if(pointer!=0)sendClipboardEvent(pointer,new byte[0]); }
    @Keep private void onSyncReply(int serial) { }
    // Embedded rendering receives text/keys over Binder from the Activity-owned IME.
    // It must not open a competing Android input connection on pointer events.
    @Override public boolean onCheckIsTextEditor() { return activity!=null; }
    @Override public InputConnection onCreateInputConnection(EditorInfo info) {
        if(activity==null)return null;
        info.inputType=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        info.imeOptions=EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_ACTION_NONE;
        return new BaseInputConnection(this,false) {
            private String composing="";
            @Override public boolean commitText(CharSequence text,int position) { composing="";if(text!=null)inputText(text.toString());return true; }
            @Override public boolean setComposingText(CharSequence text,int position) { composing=text==null?"":text.toString();return true; }
            @Override public boolean finishComposingText() { String text=composing;composing="";if(!text.isEmpty())inputText(text);return true; }
            @Override public boolean sendKeyEvent(KeyEvent event) { return forwardKey(event); }
            @Override public boolean deleteSurroundingText(int before,int after) {
                for(int i=0;i<Math.min(before,256);i++){key(KeyEvent.KEYCODE_DEL,true);key(KeyEvent.KEYCODE_DEL,false);}
                for(int i=0;i<Math.min(after,256);i++){key(KeyEvent.KEYCODE_FORWARD_DEL,true);key(KeyEvent.KEYCODE_FORWARD_DEL,false);}return true;
            }
        };
    }
    void onPointerTap(Runnable callback){pointerTap=callback;}
    void inputText(String text){if(pointer!=0)X11TextEvents.send(text,part->sendTextEvent(pointer,part.getBytes(StandardCharsets.UTF_8)),code->{key(code,true);key(code,false);});}
    boolean forwardKey(KeyEvent e) {
        if(e.getKeyCode()==KeyEvent.KEYCODE_BACK||e.getKeyCode()==KeyEvent.KEYCODE_VOLUME_UP||e.getKeyCode()==KeyEvent.KEYCODE_VOLUME_DOWN)return false;
        if(e.getAction()!=KeyEvent.ACTION_DOWN&&e.getAction()!=KeyEvent.ACTION_UP)return false;
        return key(e.getKeyCode(),e.getAction()==KeyEvent.ACTION_DOWN);
    }
    private boolean key(int code,boolean down) { return pointer!=0&&code>=0&&code<304&&sendKeyEvent(pointer,0,code,down); }
    @Override public boolean onKeyDown(int code,KeyEvent event) { return forwardKey(event)||super.onKeyDown(code,event); }
    @Override public boolean onKeyUp(int code,KeyEvent event) { return forwardKey(event)||super.onKeyUp(code,event); }
    private float mapX(float x) { return Math.max(0,Math.min(screenWidth-1,sourceLeft+(x-viewportLeft)*sourceWidth/viewportWidth)); }
    private float x(MotionEvent e){return mapX(e.getX());}
    private float mapY(float y) { return Math.max(0,Math.min(screenHeight-1,sourceTop+(y-viewportTop)*sourceHeight/viewportHeight)); }
    private float y(MotionEvent e){return mapY(e.getY());}
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(pointer==0)return false;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN)requestFocus();
        if(e.getToolType(0)==MotionEvent.TOOL_TYPE_FINGER||e.getToolType(0)==MotionEvent.TOOL_TYPE_UNKNOWN)return touchpad.event(e);
        // Mouse/pen retain direct positioning. Native button messages ignore x/y,
        // so position the pointer explicitly before pressing, not only on MOVE.
        int action=e.getActionMasked();float x=x(e),y=y(e);
        if(action==MotionEvent.ACTION_DOWN){touchpad.cancel();sendMouseEvent(pointer,x,y,0,false,false);mouseButton(1,true,false);}
        else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)mouseButton(1,false,false);
        else if(action==MotionEvent.ACTION_MOVE){pointerMoves++;sendMouseEvent(pointer,x,y,0,false,false);}
        return true;
    }
    @Override public boolean onGenericMotionEvent(MotionEvent e) {
        if(pointer==0)return false;
        if(e.getActionMasked()==MotionEvent.ACTION_HOVER_MOVE){pointerHovers++;sendMouseEvent(pointer,x(e),y(e),0,false,false);return true;}
        if(e.getActionMasked()==MotionEvent.ACTION_SCROLL){pointerScrolls++;sendMouseEvent(pointer,-e.getAxisValue(MotionEvent.AXIS_HSCROLL)*100,-e.getAxisValue(MotionEvent.AXIS_VSCROLL)*100,4,false,true);return true;}
        return super.onGenericMotionEvent(e);
    }
    @FastNative private native long nativeInit();
    @FastNative private native void nativeDestroy(long ptr);
    @FastNative private native void surfaceChanged(long ptr, Surface surface);
    @FastNative private native void setFiltering(long ptr, int filtering);
    @FastNative private native void setClipboardSyncEnabled(long ptr, boolean enabled, boolean ignored);
    @FastNative private native void sendClipboardAnnounce(long ptr);
    @FastNative private native void sendClipboardEvent(long ptr, byte[] text);
    @FastNative private native void sendWindowChange(long ptr, int width, int height, int framerate, String name);
    @FastNative private native void setViewport(long ptr, int x, int y, int width, int height, int expectedWidth, int expectedHeight, int hiddenBottom);
    @FastNative private native void setRendererZoom(long ptr, int percent);
    @FastNative private native void setZoomAnchor(long ptr, float sourceX, float sourceY, float fracX, float fracY);
    @FastNative private native void clearZoomAnchor(long ptr);
    @FastNative private native void setFollowCursorPan(long ptr, boolean enabled);
    @FastNative private native long getCursorPosition(long ptr);
    @FastNative private native void sendSync(long ptr, int serial);
    @FastNative private static native void connect(long ptr, int fd);
    @CriticalNative private static native boolean connected(long ptr);
    @FastNative private static native void startLogcat(long ptr, int fd);
    @FastNative private native void sendMouseEvent(long ptr, float x, float y, int whichButton, boolean buttonDown, boolean relative);
    @FastNative private native void sendTouchEvent(long ptr, int action, int id, int x, int y);
    @FastNative private native void sendStylusEvent(long ptr, float x, float y, int pressure, int tiltX, int tiltY, int orientation, int buttons, boolean eraser, boolean mouseMode);
    @FastNative private native void requestStylusEnabled(long ptr, boolean enabled);
    @FastNative private native void sendLockKeysState(long ptr, int state);
    @FastNative private native boolean sendKeyEvent(long ptr, int scanCode, int keyCode, boolean keyDown);
    @FastNative private native void sendTextEvent(long ptr, byte[] text);
    @CriticalNative private static native boolean requestConnection(long ptr);
    @CriticalNative public static native long getLastInputTimestamp();
    @CriticalNative public static native void markUserActivity();
}
