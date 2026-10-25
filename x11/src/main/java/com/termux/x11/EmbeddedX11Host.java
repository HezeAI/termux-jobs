package com.termux.x11;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.view.*;
import androidx.annotation.RequiresApi;

/** Remote view hierarchy: native renderer stays in :jobs_x11, not in the terminal VM. */
@RequiresApi(30)
final class EmbeddedX11Host {
    private final X11Service service;
    private SurfaceControlViewHost host;
    private LorieView view;
    private IBinder token;
    private String attachment="";
    private int width,height,rendererDisplay=-1;
    private IBinder.DeathRecipient death;
    private final X11ExtraKeys extraKeys=new X11ExtraKeys((code,down)->{if(view!=null)view.forwardKey(new KeyEvent(down?KeyEvent.ACTION_DOWN:KeyEvent.ACTION_UP,code));});
    private final Handler main=new Handler(Looper.getMainLooper());
    EmbeddedX11Host(X11Service service){this.service=service;}
    Bundle attach(Bundle request){
        String id=request.getString("attachment","");IBinder wanted=request.getBinder("token");
        int w=bounded(request.getInt("width")),h=bounded(request.getInt("height")),shown=visible(request,h);
        X11ScreenSettings mode=X11ScreenSettings.mode(request);
        if(id.isEmpty()||id.length()>80||wanted==null)throw new IllegalArgumentException("Invalid X11 host");
        if(host==null||!wanted.equals(token)||!id.equals(attachment)){
            releaseHost();int displayId=request.getInt("display",0);
            Display display=((DisplayManager)service.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(displayId);
            if(display==null)throw new IllegalArgumentException("Display unavailable");
            Context context=service.createDisplayContext(display);
            if(view!=null&&rendererDisplay!=displayId){view.release();view=null;}
            if(view==null){view=new LorieView(context);view.retainRendererByService();view.onPointerTap(service::pointerTapped);view.setZOrderOnTop(true);rendererDisplay=displayId;}
            view.screenSettings(mode,request.getBoolean("logicalLandscape",w>h));
            host=new SurfaceControlViewHost(context,display,wanted);
            // Host SurfaceView stays nonfocusable; the Activity owns the IME endpoint.
            host.setView(view,w,h);width=w;height=h;token=wanted;attachment=id;
            death=()->main.post(()->{if(token==wanted&&attachment.equals(id))releaseHost();});
            try{wanted.linkToDeath(death,0);}catch(RemoteException error){releaseHost();throw new IllegalStateException("X11 host disappeared",error);}
            view.requestFocus();
        }else {view.screenSettings(mode,request.getBoolean("logicalLandscape",w>h));layout(w,h);}
        view.obscuredBottom(h-shown);
        connect();Bundle result=service.snapshot();result.putParcelable("surface",host.getSurfacePackage());return result;
    }
    void resize(Bundle request){if(host!=null&&attachment.equals(request.getString("attachment"))){int w=bounded(request.getInt("width")),h=bounded(request.getInt("height"));int visible=visible(request,h);X11ScreenSettings mode=X11ScreenSettings.mode(request);view.screenSettings(mode,request.getBoolean("logicalLandscape",w>h));layout(w,h);view.obscuredBottom(h-visible);}}
    private void layout(int w,int h){if(width==w&&height==h)return;width=w;height=h;host.relayout(w,h);}
    private static int visible(Bundle q,int h){return X11Viewport.checkedVisible(q.getInt("visibleHeight",h),h);}
    void connect(){if(view!=null&&!view.isConnected()){ParcelFileDescriptor fd=service.displayConnection();if(fd!=null)view.connectFileDescriptor(fd.detachFd());}}
    void input(Bundle request){
        if(host==null||view==null||attachment.isEmpty()||!attachment.equals(request.getString("attachment","")))return;
        if(request.getBoolean("extra",false)){extraKeys.key(request.getInt("key",-1),request.getBoolean("down"));return;}
        String text=request.getString("text");
        if(text!=null){if(text.length()>1024)throw new IllegalArgumentException("X11 text too large");view.inputText(text);}
        else {int code=request.getInt("key",-1);if(code<0||code>=304||code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_VOLUME_DOWN)throw new IllegalArgumentException("Invalid X11 key");view.forwardKey(new KeyEvent(request.getBoolean("down")?KeyEvent.ACTION_DOWN:KeyEvent.ACTION_UP,code));}
    }
    void detach(String id){if(attachment.equals(id))releaseHost();}
    // A tab change replaces the Android host only. Closing/reopening the native
    // socket on every tab races the upstream global connection/buffer cleanup.
    private void releaseHost(){
        if(token!=null&&death!=null)try{token.unlinkToDeath(death,0);}catch(java.util.NoSuchElementException ignored){}
        death=null;extraKeys.release();if(view!=null)view.suspendSurface();
        if(host!=null){host.release();host=null;}
        token=null;attachment="";width=height=0;
    }
    String pointerDiagnostic(){return view==null?"unavailable":view.pointerDiagnostic();}
    boolean rendererConnected(){return view!=null&&view.isConnected();}
    boolean hostAttached(){return host!=null;}
    void release(){releaseHost();if(view!=null){view.release();view=null;}rendererDisplay=-1;}
    private static int bounded(int n){if(n<1||n>8192)throw new IllegalArgumentException("Invalid X11 viewport");return n;}
}
