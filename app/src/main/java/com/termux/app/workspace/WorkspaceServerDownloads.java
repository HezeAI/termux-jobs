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
    private final WorkspaceLocalFilePicker picker;private volatile LocalDownloadFile destination;
    private boolean running;private AlertDialog progress,result;private long started,lastUpdate;
    WorkspaceServerDownloads(TermuxActivity a){activity=a;picker=new WorkspaceLocalFilePicker(a);}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void start(ServerProfiles.Profile profile,JSONObject entry){start(profile,entry,false);}
    void quick(ServerProfiles.Profile profile,JSONObject entry){start(profile,entry,true);}
    private void start(ServerProfiles.Profile profile,JSONObject entry,boolean quick){
        if(closed||running)return;
        ServerProfiles.Profile snapshot=new ServerProfiles.Profile();snapshot.id=profile.id;snapshot.name=profile.name;snapshot.host=profile.host;snapshot.port=profile.port;snapshot.user=profile.user;snapshot.auth=profile.auth;snapshot.identity=profile.identity;snapshot.encrypted=profile.encrypted;snapshot.diagnosticsPaused=profile.diagnosticsPaused;
        if(entry.optLong("size",0)>ServerSftp.MAX_BYTES){result=WorkspaceDialogs.builder(activity).setMessage(t("Лимит скачивания — 2 ГБ на файл.","Download limit: 2 GB per file.")).setPositiveButton("OK",null).show();return;}
        String raw=entry.optString("name").replaceAll("[^\\p{L}\\p{N}._ -]","_");if(raw.isEmpty())raw="file";String name="download-"+UUID.randomUUID()+"-"+raw;
        if(!ServerSftp.validName(name)){result=WorkspaceDialogs.builder(activity).setMessage(t("Слишком длинное имя файла","File name is too long")).setPositiveButton("OK",null).show();return;}
        if(quick){activity.requestWorkspaceStorageAccess(()->{if(closed)return;WorkspaceStore.Session local=new WorkspaceStore.Session();local.environment="termux";local.name=t("Память устройства","Device storage");confirm(snapshot,entry,local,"/sdcard/Download/Termux-Jobs-Server",name,true);});}
        else picker.chooseDirectory(t("Куда скачать файл?","Choose download destination"),(location,path)->confirm(snapshot,entry,location,path,name,false));
    }
    private void confirm(ServerProfiles.Profile profile,JSONObject entry,WorkspaceStore.Session location,String path,String name,boolean create){
        if(closed)return;
        result=WorkspaceDialogs.builder(activity).setTitle(t("Скачать в выбранную папку?","Download to selected folder?"))
            .setMessage(location.name+"\n"+path+"\n\n"+name+t("\nИмя уникальное: существующие файлы не заменяются. До 2 ГБ / 10 минут, отдельное SSH-соединение; не закрывайте приложение.","\nUnique name: existing files are not replaced. Up to 2 GB / 10 minutes on a separate SSH connection; keep the app open."))
            .setPositiveButton(t("Скачать","Download"),(d,w)->transfer(profile,entry,location,path,name,create)).setNegativeButton(t("Отмена","Cancel"),null).show();
    }
    private void transfer(ServerProfiles.Profile profile,JSONObject entry,WorkspaceStore.Session location,String folder,String name,boolean create){
        if(closed||running)return;running=true;cancelled=false;started=SystemClock.uptimeMillis();lastUpdate=0;
        LinearLayout body=new LinearLayout(activity);body.setOrientation(1);int pad=WorkspaceDialogs.dp(activity,16);body.setPadding(pad,pad,pad,pad);
        TextView status=new TextView(activity);status.setTextColor(WorkspaceDialogs.TEXT);status.setText(entry.optString("name"));body.addView(status);
        ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setIndeterminate(true);body.addView(bar);
        progress=WorkspaceDialogs.builder(activity).setTitle(t("Скачивание файла","Downloading file")).setView(body).setNegativeButton(t("Остановить","Stop"),null).create();progress.setCancelable(false);progress.setCanceledOnTouchOutside(false);progress.show();progress.getButton(-2).setOnClickListener(v->{status.setText(t("Остановка…","Stopping…"));progress.getButton(-2).setEnabled(false);cancel();});
        worker.execute(()->{
            LocalDownloadFile dest=null;boolean success=false;String error="";ServerConnection owned=null;ServerDiagnostics.Trace trace=ServerDiagnostics.begin(profile,ServerDiagnostics.Op.DOWNLOAD);long[] received={0};Exception failure=null;
            try{
                if(cancelled)throw new IOException("CANCELLED");
                if(create)try(ProjectFiles local=new ProjectFiles(activity,location)){local.localOperation("mkdir -p -- "+ProjectFiles.target(folder));}
                dest=new LocalDownloadFile(activity,location,folder,name);destination=dest;if(cancelled){dest.cancel();throw new IOException("CANCELLED");}
                owned=new ServerConnection(activity,profile);connection=owned;if(cancelled)throw new IOException("CANCELLED");
                long total=entry.has("size")?entry.optLong("size",-1):-1;
                owned.download(entry.getString("path"),dest.stream,2L*1024*1024*1024,bytes->{received[0]=bytes;if(cancelled)throw new CancellationException();long now=SystemClock.uptimeMillis();if(now-lastUpdate<150&&bytes!=total)return;lastUpdate=now;String line=ServerTransferProgress.describe(bytes,total,now-started,WorkspaceLanguage.code(activity).equals("ru"));main.post(()->{if(closed||cancelled)return;status.setText(entry.optString("name")+"\n"+line);bar.setIndeterminate(total<=0);if(total>0)bar.setProgress((int)Math.min(1000,bytes*1000.0/total));});});
                if(cancelled)throw new IOException("CANCELLED");dest.finish();success=true;
            }catch(Exception e){failure=cancelled?new CancellationException():e;error=e instanceof IOException&&"DOWNLOAD_PERMISSION".equals(e.getMessage())?"DOWNLOAD_PERMISSION":"DOWNLOAD_FAILED";}
            finally{trace.finish(trace.elapsed(),0,received[0],failure==null&&cancelled?new CancellationException():failure);if(owned!=null)owned.close();connection=null;}
            boolean cleanupFailed=false;if(dest!=null)try{dest.close();}catch(Exception e){cleanupFailed=true;}destination=null;
            final boolean done=success,dirty=cleanupFailed;final String path=dest==null?"":(success?dest.path:dest.temporary),code=error;
            main.post(()->{running=false;if(progress!=null)progress.dismiss();progress=null;if(closed)return;
                String message=done?path:cancelled?t("Передача остановлена.","Transfer stopped."):code.equals("DOWNLOAD_PERMISSION")?t("Нет доступа к выбранной папке.","Cannot access selected directory."):t("Скачивание не завершено. Проверьте сеть, права и свободное место.","Download incomplete. Check connectivity, permissions and free space.");
                if(!done&&!path.isEmpty())message+="\n"+t("При прерывании в момент завершения файл мог сохраниться. Проверьте выбранный каталог.","If interrupted during completion, the file may have been saved. Check the selected directory.");
                if(dirty)message+="\n"+t("Не удалось удалить неполный файл: ","Could not remove partial file: ")+path;
                result=WorkspaceDialogs.builder(activity).setTitle(done?t("Скачано","Downloaded"):t("Скачивание","Download")).setMessage(message).setPositiveButton("OK",null).create();result.setCanceledOnTouchOutside(false);result.show();
            });
        });
    }
    private void cancel(){cancelled=true;LocalDownloadFile local=destination;if(local!=null)local.cancel();ServerConnection owned=connection;if(owned!=null)owned.close();}
    @Override public void close(){if(closed)return;closed=true;picker.close();cancel();if(progress!=null)progress.dismiss();if(result!=null)result.dismiss();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
