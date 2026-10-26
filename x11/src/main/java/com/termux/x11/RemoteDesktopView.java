package com.termux.x11;

import android.content.Context;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.RequiresApi;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Compositor-only host; never loads libXlorie or starts a Linux process. */
@RequiresApi(30)
public final class RemoteDesktopView extends FrameLayout implements SurfaceHolder.Callback {
    private final SurfaceView surface;
    private final X11StartupView loading;
    private final X11InputView ime;
    private boolean keyboardWanted,keyboardConfigured,startupReady,coreReady,revealed;
    private final ExecutorService calls=Executors.newSingleThreadExecutor();
    private String attachment=UUID.randomUUID().toString();
    private final Consumer<Bundle> changed;
    private IBinder remote;
    private boolean closed,attached,attaching,attachFailed;
    private int generation,keyboardGeneration,shownGeneration=-1;
    private String imeDiagnostic="";
    private final X11ViewTrace trace=new X11ViewTrace();
    private final X11Viewport viewport=new X11Viewport();
    private int sentWidth,sentHeight,sentVisible;
    private boolean sentLandscape;
    private X11ScreenSettings screenSettings=X11ScreenSettings.defaults();
    public void screenSettings(X11ScreenSettings settings){
        if(screenSettings.width==settings.width&&screenSettings.height==settings.height&&screenSettings.horizontalDp==settings.horizontalDp&&screenSettings.verticalDp==settings.verticalDp)return;
        screenSettings=settings;sentWidth=sentHeight=sentVisible=0;requestLayout();
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        X11ScreenSettings.Frame f=screenSettings.frame(MeasureSpec.getSize(widthSpec),MeasureSpec.getSize(heightSpec),getResources().getDisplayMetrics().density);
        LayoutParams lp=(LayoutParams)surface.getLayoutParams();lp.width=f.width;lp.height=f.height;lp.leftMargin=f.left;lp.topMargin=f.top;
        super.onMeasure(widthSpec,heightSpec);
    }
    private boolean presentationLandscape(){return getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;}
    private final X11ExtraKeys extraKeys=new X11ExtraKeys((code,down)->{
        Bundle q=X11Remote.command("input");q.putBoolean("extra",true);q.putInt("key",code);q.putBoolean("down",down);q.putString("attachment",attachment);send(q);
    });
    public void extraKey(int code,boolean down){if(!closed&&attached&&(!down||inputReady()))extraKeys.key(code,down);}
    public void releaseExtraKeys(){extraKeys.release();}
    private Runnable extraKeysReset=()->{};
    public void onExtraKeysReset(Runnable callback){extraKeysReset=callback;}
    public boolean inputReady(){return attached&&!closed&&(startupReady||revealed);}
    public Boolean imeVisible(){WindowInsets insets=getRootWindowInsets();return insets==null?null:insets.isVisible(WindowInsets.Type.ime());}
    public String keyboardDiagnostic(){WindowInsets insets=getRootWindowInsets();return imeDiagnostic+"\ncurrent windowFocus="+hasWindowFocus()+" editorFocus="+ime.hasFocus()+" editorWindowFocus="+ime.hasWindowFocus()+" keyboardWanted="+keyboardWanted+" IME-visible="+(insets==null?"unknown":insets.isVisible(WindowInsets.Type.ime()))+"\nViewport="+sentWidth+"x"+sentHeight+" visibleHeight="+sentVisible+"\n"+trace.report();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(trace!=null)trace.event("window-focus",String.valueOf(focus));}
    public RemoteDesktopView(Context context,Consumer<Bundle> changed){
        super(context);this.changed=changed;setBackgroundColor(0xff000000);
        surface=new SurfaceView(context);surface.setZOrderOnTop(true);surface.setFocusable(false);surface.setAlpha(0f);surface.setVisibility(VISIBLE);surface.getHolder().addCallback(this);addView(surface,new LayoutParams(-1,-1));
        loading=new X11StartupView(context,()->{revealed=true;showScreen();attach();});addView(loading,new LayoutParams(-1,-1));
        ime=new X11InputView(context,new X11InputView.Sink(){
            public void text(String text){
                // Stay below the private Binder request limit, preserving surrogate pairs.
                for(int start=0;start<text.length();){int end=Math.min(text.length(),start+1024);if(end<text.length()&&Character.isHighSurrogate(text.charAt(end-1)))end--;Bundle q=X11Remote.command("input");q.putString("text",text.substring(start,end));input(q);start=end;}
            }
            public void key(int code,boolean down){Bundle q=X11Remote.command("input");q.putInt("key",code);q.putBoolean("down",down);input(q);}
        });ime.onWindowFocused(()->{if(keyboardWanted)requestIme(keyboardGeneration);});addView(ime,new LayoutParams(1,1));
    }
    public void connect(IBinder binder){remote=binder;if(surface.getHolder().getSurface().isValid())attach();}
    public void startup(boolean ready,boolean timedOut){startup(ready,timedOut,ready?4:0);}
    public void startup(boolean ready,boolean timedOut,int step){
        if(closed||attachFailed)return;startupReady=ready;coreReady=ready||step>=2;loading.update(ready,timedOut,step);
        if(ready||revealed)showScreen();if(coreReady&&!attached)attach();
    }
    private void showScreen(){surface.setVisibility(VISIBLE);surface.setAlpha(1f);if(attached){loading.setVisibility(GONE);changed.accept(new Bundle());}}
    private void input(Bundle q){if(!closed&&attached&&keyboardWanted){q.putString("attachment",attachment);send(q);}}
    private void attach(){
        IBinder binder=remote;IBinder token=surface.getHostToken();if(closed||(!coreReady&&!revealed)||attaching||binder==null||token==null||getDisplay()==null||surface.getWidth()<1||surface.getHeight()<1)return;
        trace.event("attach","start");attaching=true;int epoch=generation;
        Bundle q=X11Remote.command("attach");q.putBinder("token",token);q.putString("attachment",attachment);q.putInt("display",getDisplay().getDisplayId());putGeometry(q);
        calls.execute(()->{try{Bundle result=X11Remote.call(binder,q);SurfaceControlViewHost.SurfacePackage pack=result.getParcelable("surface");post(()->{
            if(closed||binder!=remote||epoch!=generation){if(pack!=null)pack.release();return;}
            attaching=false;if(pack!=null){surface.setChildSurfacePackage(pack);attached=true;trace.event("attach","ready");if(startupReady||revealed)showScreen();resize();if(keyboardWanted)keyboard(true);}changed.accept(result);
        });}catch(Exception e){post(()->{if(!closed&&epoch==generation){attaching=false;attachFailed=true;surface.setVisibility(INVISIBLE);loading.setVisibility(VISIBLE);loading.failed();}});}});
    }
    @Override public void surfaceCreated(SurfaceHolder holder){trace.event("surface-created","");attach();}
    @Override public void surfaceChanged(SurfaceHolder holder,int f,int w,int h){if(!attached)attach();else resize();}
    private X11Viewport.Size geometry(){
        WindowMetrics metrics=((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getCurrentWindowMetrics();
        android.graphics.Rect bounds=metrics.getBounds();int[] position=new int[2];surface.getLocationOnScreen(position);
        WindowInsets insets=getRootWindowInsets();if(insets==null)insets=metrics.getWindowInsets();
        android.graphics.Insets bars=insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
        // Transient navigation bars shown with IME must not shrink an immersive
        // desktop mode. In the embedded app window keep the existing stable inset.
        boolean edgeToEdge=(getWindowSystemUiVisibility()&View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)!=0;
        int stableBottom=edgeToEdge?0:bars.bottom;
        int keyboardBottom=insets.getInsets(WindowInsets.Type.ime()).bottom;
        int visible=X11Viewport.visibleHeight(surface.getHeight(),bounds.bottom,position[1],keyboardBottom);
        // Custom logical resolution still needs the full, unobscured host size.
        // An IME resize changes the visible crop, never the desktop's transform.
        int bottomGap=Math.max(0,getHeight()-surface.getBottom());
        return screenSettings.viewport(viewport,surface.getWidth(),visible,bounds.width(),bounds.height(),position[1]-bounds.top,stableBottom+bottomGap,keyboardBottom>0);
    }
    private void putGeometry(Bundle q){X11Viewport.Size size=geometry();q.putInt("width",size.width);q.putInt("height",size.height);q.putInt("visibleHeight",size.visibleHeight);screenSettings.putMode(q,presentationLandscape());}
    private void resize(){
        if(!attached||closed||surface.getWidth()<1||surface.getHeight()<1)return;
        X11Viewport.Size size=geometry();if(sentWidth==size.width&&sentHeight==size.height&&sentVisible==size.visibleHeight&&sentLandscape==presentationLandscape())return;
        trace.event("geometry",size.width+"x"+size.height+" visible="+size.visibleHeight);sentWidth=size.width;sentHeight=size.height;sentVisible=size.visibleHeight;sentLandscape=presentationLandscape();
        Bundle q=X11Remote.command("resize");q.putString("attachment",attachment);q.putInt("width",size.width);q.putInt("height",size.height);q.putInt("visibleHeight",size.visibleHeight);screenSettings.putMode(q,presentationLandscape());send(q);
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){super.onLayout(changed,l,t,r,b);resize();}
    @Override public WindowInsets onApplyWindowInsets(WindowInsets insets){post(this::resize);return super.onApplyWindowInsets(insets);}
    @Override public void surfaceDestroyed(SurfaceHolder holder){trace.event("surface-destroyed","");detach();}
    public void keyboard(boolean show){
        if(keyboardConfigured&&!X11ImeFocus.needsRequest(show,keyboardWanted,ime.hasFocus(),ime.hasWindowFocus()))return;
        trace.event("keyboard-request",String.valueOf(show));keyboardConfigured=true;
        int request=++keyboardGeneration;
        if(show)ime.restart();ime.active(show);keyboardWanted=show;
        android.view.inputmethod.InputMethodManager manager=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if(show){
            if(ime.hasWindowFocus())ime.requestFocus();else X11ImeFocus.take(surface,ime);
            imeDiagnostic="request="+request+" viewFocus="+ime.hasFocus()+" windowFocus="+ime.hasWindowFocus();
            requestIme(request);
        }else {manager.hideSoftInputFromWindow(ime.getWindowToken(),0);if(getWindowInsetsController()!=null)getWindowInsetsController().hide(WindowInsets.Type.ime());}
    }
    private void requestIme(int request){
        ime.post(()->{
            if(closed||request!=keyboardGeneration||!keyboardWanted||shownGeneration==request)return;
            imeDiagnostic="request="+request+" viewFocus="+ime.hasFocus()+" windowFocus="+ime.hasWindowFocus();
            // The window-manager focus notification is asynchronous. A local View
            // focus alone cannot authorize IME; onWindowFocused retries this request.
            if(!ime.hasFocus()||!ime.hasWindowFocus())return;
            shownGeneration=request;
            android.view.inputmethod.InputMethodManager manager=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.restartInput(ime);
            boolean accepted=manager.showSoftInput(ime,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            imeDiagnostic+=" showAccepted="+accepted;
            if(ime.getWindowInsetsController()!=null)ime.getWindowInsetsController().show(WindowInsets.Type.ime());
        });
    }
    private void send(Bundle q){IBinder binder=remote;if(closed||binder==null)return;calls.execute(()->{try{X11Remote.call(binder,q);}catch(Exception ignored){}});}
    private void detach(){extraKeys.release();trace.event("detach","");keyboardConfigured=false;sentWidth=sentHeight=sentVisible=0;String old=attachment;attachment=UUID.randomUUID().toString();generation++;attaching=false;Bundle q=X11Remote.command("detach");q.putString("attachment",old);send(q);attached=false;extraKeysReset.run();}
    public void close(){if(closed)return;if(keyboardWanted)keyboard(false);ime.active(false);detach();closed=true;surface.getHolder().removeCallback(this);calls.shutdown();}
}
