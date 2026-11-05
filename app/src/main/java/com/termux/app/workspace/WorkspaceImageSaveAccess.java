package com.termux.app.workspace;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/** Permission continuation only for saving a new image; never requests all-files access. */
public final class WorkspaceImageSaveAccess {
    public static final int REQUEST=0x5352;
    private final Activity activity;
    private Runnable pending,declined;
    private boolean closed;
    public WorkspaceImageSaveAccess(Activity activity){this.activity=activity;}
    private boolean granted(){return Build.VERSION.SDK_INT>=29||activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;}
    public boolean request(Runnable ready,Runnable cancelled){
        if(closed||pending!=null)return false;
        if(granted()){ready.run();return true;}
        pending=ready;declined=cancelled;
        try{activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST);}
        catch(RuntimeException unavailable){pending=null;declined=null;if(cancelled!=null)cancelled.run();}
        return true;
    }
    public boolean permissionResult(int request){
        if(request!=REQUEST)return false;
        if(closed)return true;
        Runnable ready=pending,cancelled=declined;pending=null;declined=null;
        if(ready!=null){if(granted())ready.run();else if(cancelled!=null)cancelled.run();}
        return true;
    }
    public void close(){closed=true;pending=null;declined=null;}
}
