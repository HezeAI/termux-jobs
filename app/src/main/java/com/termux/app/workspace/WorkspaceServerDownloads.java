package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.JSONObject;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.*;

/** Independent owned SSH channel: cancelling a download cannot close monitoring or terminals. */
final class WorkspaceServerDownloads implements AutoCloseable {
    private final TermuxActivity activity;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile ServerConnection connection;private volatile boolean cancelled,closed;
    private boolean running;private AlertDialog progress,result;private long started,lastUpdate;
    WorkspaceServerDownloads(TermuxActivity a){activity=a;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void start(ServerProfiles.Profile profile,JSONObject entry){
        if(closed||running)return;running=true;cancelled=false;started=SystemClock.uptimeMillis();lastUpdate=0;
        LinearLayout body=new LinearLayout(activity);body.setOrientation(1);int pad=WorkspaceDialogs.dp(activity,16);body.setPadding(pad,pad,pad,pad);
        TextView status=new TextView(activity);status.setTextColor(WorkspaceDialogs.TEXT);status.setText(entry.optString("name"));body.addView(status);
        ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setIndeterminate(true);body.addView(bar);
        progress=WorkspaceDialogs.builder(activity).setTitle(t("Скачивание файла","Downloading file")).setView(body).setNegativeButton(t("Остановить","Stop"),null).create();progress.setCancelable(false);progress.setCanceledOnTouchOutside(false);progress.show();progress.getButton(-2).setOnClickListener(v->{status.setText(t("Остановка…","Stopping…"));progress.getButton(-2).setEnabled(false);cancel();});
        worker.execute(()->{
            File dest=null;boolean success=false;String error="";ServerConnection owned=null;ServerDiagnostics.Trace trace=ServerDiagnostics.begin(profile,ServerDiagnostics.Op.DOWNLOAD);long[] received={0};Exception failure=null;
            try{
                if(cancelled)throw new IOException("CANCELLED");
                File folder=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Termux-Jobs-Server");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("DOWNLOAD_PERMISSION");
                String name=entry.optString("name").replaceAll("[^\\p{L}\\p{N}._ -]","_");if(name.isEmpty())name="file";
                dest=new File(folder,UUID.randomUUID().toString()+"-"+name);if(!dest.createNewFile())throw new IOException("DOWNLOAD_FAILED");
                owned=new ServerConnection(activity,profile);connection=owned;if(cancelled)throw new IOException("CANCELLED");
                long total=entry.has("size")?entry.optLong("size",-1):-1;
                owned.download(entry.getString("path"),dest,2L*1024*1024*1024,bytes->{received[0]=bytes;if(cancelled)throw new CancellationException();long now=SystemClock.uptimeMillis();if(now-lastUpdate<150&&bytes!=total)return;lastUpdate=now;String line=ServerTransferProgress.describe(bytes,total,now-started,WorkspaceLanguage.code(activity).equals("ru"));main.post(()->{if(closed||cancelled)return;status.setText(entry.optString("name")+"\n"+line);bar.setIndeterminate(total<=0);if(total>0)bar.setProgress((int)Math.min(1000,bytes*1000.0/total));});});
                success=!cancelled;
            }catch(Exception e){failure=cancelled?new CancellationException():e;error=e instanceof IOException&&"DOWNLOAD_PERMISSION".equals(e.getMessage())?"DOWNLOAD_PERMISSION":"DOWNLOAD_FAILED";}
            finally{trace.finish(trace.elapsed(),0,received[0],failure==null&&cancelled?new CancellationException():failure);if(owned!=null)owned.close();connection=null;}
            boolean cleanupFailed=false;if(!success&&dest!=null&&dest.exists())cleanupFailed=!dest.delete();
            final boolean done=success,dirty=cleanupFailed;final String path=dest==null?"":dest.getAbsolutePath(),code=error;
            main.post(()->{running=false;if(progress!=null)progress.dismiss();progress=null;if(closed)return;
                String message=done?path:cancelled?t("Передача остановлена.","Transfer stopped."):code.equals("DOWNLOAD_PERMISSION")?t("Нет доступа к папке Download.","Cannot access Download directory."):t("Скачивание не завершено. Проверьте сеть, права и свободное место.","Download incomplete. Check connectivity, permissions and free space.");
                if(dirty)message+="\n"+t("Не удалось удалить неполный файл: ","Could not remove partial file: ")+path;
                result=WorkspaceDialogs.builder(activity).setTitle(done?t("Скачано","Downloaded"):t("Скачивание","Download")).setMessage(message).setPositiveButton("OK",null).create();result.setCanceledOnTouchOutside(false);result.show();
            });
        });
    }
    private void cancel(){cancelled=true;ServerConnection owned=connection;if(owned!=null)owned.close();}
    @Override public void close(){if(closed)return;closed=true;cancel();if(progress!=null)progress.dismiss();if(result!=null)result.dismiss();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
