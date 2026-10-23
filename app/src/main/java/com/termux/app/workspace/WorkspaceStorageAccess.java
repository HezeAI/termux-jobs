package com.termux.app.workspace;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;

/** Activity-owned continuation; permission denial never opens an empty shared-storage browser. */
public final class WorkspaceStorageAccess {
    public static final int REQUEST=0x5351;
    private final Activity activity;private Runnable pending, declined;private boolean closed;
    public WorkspaceStorageAccess(Activity activity){this.activity=activity;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    boolean granted(){return activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED||Build.VERSION.SDK_INT>=30&&Environment.isExternalStorageManager();}
    public void request(Runnable ready){request(ready,null);}
    public void request(Runnable ready,Runnable cancelled){
        if(closed||pending!=null)return;
        if(granted()){ready.run();return;}pending=ready;declined=cancelled;
        activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST);
    }
    public boolean permissionResult(int request){if(request!=REQUEST)return false;complete();return true;}
    public boolean activityResult(int request){if(request!=REQUEST)return false;complete();return true;}
    private void complete(){
        if(closed)return;Runnable ready=pending,cancelled=declined;pending=null;declined=null;if(ready==null)return;
        if(granted()){ready.run();return;}
        WorkspaceDialogs.builder(activity).setTitle(t("Нет доступа к памяти устройства","Device storage access denied"))
            .setMessage(t("Разрешите доступ к файлам в настройках Android. Без него содержимое общей памяти недоступно. Приватные папки других приложений могут оставаться закрытыми.","Allow file access in Android settings. Shared storage cannot be browsed without it. Other apps’ private folders may remain restricted."))
            .setPositiveButton(t("Открыть настройки","Open settings"),(d,w)->{
                if(closed)return;pending=ready;declined=cancelled;
                Intent intent=new Intent(Build.VERSION.SDK_INT>=30?Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION:Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName()));
                try{activity.startActivityForResult(intent,REQUEST);}catch(ActivityNotFoundException e){pending=null;declined=null;if(cancelled!=null&&!closed)cancelled.run();}
            }).setNegativeButton(t("Отмена","Cancel"),(d,w)->{if(cancelled!=null&&!closed)cancelled.run();}).setOnCancelListener(d->{if(cancelled!=null&&!closed)cancelled.run();}).show();
    }
    public void close(){closed=true;pending=null;declined=null;}
}
