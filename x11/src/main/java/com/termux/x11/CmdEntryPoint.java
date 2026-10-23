package com.termux.x11;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Keep;
import dalvik.annotation.optimization.CriticalNative;

/** JNI ABI retained from pinned Termux:X11; no exported receiver or shell loader. */
@Keep public final class CmdEntryPoint {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ready;
    CmdEntryPoint(Runnable ready) { this.ready=ready; }
    @Keep private void sendBroadcast() { handler.post(ready); }
    @Keep private void sendBroadcastDelayed() { handler.post(ready); }
    @Keep private void setDocumentTag(String tag) { }
    public native boolean start(String[] args);
    public native ParcelFileDescriptor getXConnection();
    public native ParcelFileDescriptor getLogcatOutput();
    public native void reportFatalError(String message);
    @CriticalNative private static native boolean connected();
}
