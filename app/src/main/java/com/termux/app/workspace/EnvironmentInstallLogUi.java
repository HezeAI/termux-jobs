package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** A static snapshot: no polling, installer restart or terminal focus changes. */
final class EnvironmentInstallLogUi {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static String t(TermuxActivity a,String ru,String en){return WorkspaceLanguage.code(a).equals("ru")?ru:en;}
    static void show(TermuxActivity a,String label,String report){
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);int gap=WorkspaceDialogs.dp(a,12);body.setPadding(gap,0,gap,gap);
        ScrollView scroll=new ScrollView(a);TextView log=new TextView(a);log.setTextColor(WorkspaceDialogs.TEXT);log.setTextSize(12);log.setTypeface(android.graphics.Typeface.MONOSPACE);log.setTextIsSelectable(true);log.setText(report);log.setTag("environment-install-log-text");scroll.addView(log);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView info=new TextView(a);info.setTextSize(11);info.setTextColor(WorkspaceDialogs.MUTED);info.setText(t(a,"Журнал последней попытки. Адреса URL и известные секреты скрыты автоматически; проверьте текст перед отправкой.","Last attempt log. URLs and known secrets are automatically redacted; review before sharing."));body.addView(info);
        TextView status=new TextView(a);status.setTextSize(12);status.setTextColor(WorkspaceDialogs.ACCENT);status.setTag("environment-install-log-status");body.addView(status);
        FrameLayout holder=new FrameLayout(a);holder.addView(body,new FrameLayout.LayoutParams(-1,(int)(a.getResources().getDisplayMetrics().heightPixels*.60)));
        AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(t(a,"Журнал установки · ","Installation log · ")+label).setView(holder)
            .setNegativeButton(t(a,"Назад","Back"),null).setPositiveButton(t(a,"В Download","To Download"),null).create();
        dialog.setCanceledOnTouchOutside(false);dialog.show();boolean[] saving={false};Handler main=new Handler(Looper.getMainLooper());
        dialog.getButton(-1).setTag("environment-install-log-download");
        dialog.getButton(-1).setOnClickListener(v->a.requestWorkspaceStorageAccess(()->{
            if(!dialog.isShowing()||saving[0])return;saving[0]=true;dialog.getButton(-1).setEnabled(false);status.setText(t(a,"Сохранение…","Saving…"));
            IO.execute(()->{File out=null;String result;try{
                File folder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException();
                out=File.createTempFile("Termux-Jobs-environment-install-",".txt",folder);
                try(OutputStream stream=new FileOutputStream(out)){stream.write(report.getBytes(StandardCharsets.UTF_8));}
                result=t(a,"Сохранено: ","Saved: ")+out.getAbsolutePath();
            }catch(Exception e){if(out!=null)out.delete();result=t(a,"Не удалось сохранить в Download. Проверьте доступ и свободное место.","Could not save to Download. Check storage access and free space.");}
            String message=result;main.post(()->{saving[0]=false;if(dialog.isShowing()){status.setText(message);dialog.getButton(-1).setEnabled(true);}});});
        },()->{if(dialog.isShowing())status.setText(t(a,"Для сохранения нужен доступ к памяти устройства.","Device storage access is required to save."));}));
    }
}
