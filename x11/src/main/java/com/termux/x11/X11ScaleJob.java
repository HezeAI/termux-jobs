package com.termux.x11;

import android.content.Context;
import android.os.*;
import java.io.File;
import java.util.concurrent.*;

/** A one-shot, independently owned settings child; never replaces the desktop child. */
final class X11ScaleJob implements AutoCloseable {
    private final Context context;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile Bundle state=new Bundle();
    private volatile DesktopProcess child;
    private volatile boolean closed;
    X11ScaleJob(Context context){this.context=context;}
    Bundle snapshot(){return new Bundle(state);}
    void start(File base,String target,String token,int percent,String request){
        X11ScreenSettings.validateScale(percent);
        if(!request.matches("[a-f0-9-]{36}")||closed||state.getBoolean("pending",false))throw new IllegalStateException("Scale operation unavailable");
        Bundle initial=new Bundle();initial.putString("request",request);initial.putInt("percent",percent);initial.putBoolean("pending",true);state=initial;
        worker.execute(()->{
            DesktopProcess owned=null;boolean ok=false;StringBuilder output=new StringBuilder();
            try{
                if(closed)return;
                owned=DesktopProcess.scale(context,base,target,token,percent);child=owned;
                if(closed)owned.close();
                DesktopProcess running=owned;Runnable timeout=running::close;main.postDelayed(timeout,45000);
                try{owned.read(chunk->{output.append(chunk);if(output.length()>4096)output.delete(0,output.length()-4096);});int exit=owned.waitFor();ok=(exit==0||exit==137)&&output.toString().contains("READY:screen-scale\n")&&output.toString().contains("DESKTOP_CHILD_EXIT=0");}
                finally{main.removeCallbacks(timeout);}
            }catch(Exception ignored){}
            finally{
                if(owned!=null)owned.closeAndWait();child=null;
                Bundle result=new Bundle(initial);result.putBoolean("pending",false);result.putBoolean("success",ok&&!closed);state=result;
            }
        });
    }
    @Override public void close(){closed=true;main.removeCallbacksAndMessages(null);DesktopProcess owned=child;if(owned!=null)owned.close();worker.shutdown();}
}
