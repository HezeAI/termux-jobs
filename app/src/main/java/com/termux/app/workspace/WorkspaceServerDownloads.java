package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** One queued batch and an independent owned SFTP connection. Monitoring and terminals are untouched. */
final class WorkspaceServerDownloads implements AutoCloseable {
    final ServerTransferPresentation presentation=new ServerTransferPresentation();
    private final TermuxActivity activity;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile ServerConnection connection;private volatile ServerSftp channel;
    private volatile boolean cancelled,closed;private volatile LocalDownloadFile destination;
    private final WorkspaceLocalFilePicker picker;
    private String ownerProfile="";private int choiceEpoch;
    private boolean running;private AlertDialog progress,result;private long started,lastUpdate;
    WorkspaceServerDownloads(TermuxActivity a){activity=a;picker=new WorkspaceLocalFilePicker(a);}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void start(ServerProfiles.Profile profile,JSONObject entry){choose(profile,Collections.singletonList(entry),false);}
    void quick(ServerProfiles.Profile profile,JSONObject entry){choose(profile,Collections.singletonList(entry),true);}
    void selected(ServerProfiles.Profile profile,List<JSONObject> entries){choose(profile,entries,false);}
    void disconnect(String id){if(ownerProfile.equals(id)){choiceEpoch++;picker.cancelSelection();if(result!=null)result.dismiss();cancel();}}
    private int beginChoice(String id){ownerProfile=id;int epoch=++choiceEpoch;picker.cancelSelection();if(result!=null)result.dismiss();return epoch;}
    private void trackChoice(AlertDialog dialog,int epoch){dialog.setOnDismissListener(d->{if(choiceEpoch==epoch)choiceEpoch++;});}
    private void choose(ServerProfiles.Profile profile,List<JSONObject> entries,boolean quick){
        if(closed||entries.isEmpty()||ServerConnection.disconnected(activity,profile.id))return;if(running){presentation.foreground();return;}
        final int epoch=beginChoice(profile.id);
        ServerProfiles.Profile snapshot=ServerProfiles.connectionSnapshot(profile);
        List<String> paths=new ArrayList<>();for(JSONObject e:entries){if(!e.optBoolean("directory")&&e.optLong("size",0)>ServerSftp.MAX_DOWNLOAD_BYTES){result=WorkspaceDialogs.builder(activity).setMessage(t("Лимит скачивания — 10 ГБ на файл.","Download limit: 10 GB per file.")).setPositiveButton("OK",null).show();return;}paths.add(e.optString("path"));}
        String root="download-"+UUID.randomUUID();
        if(quick)activity.requestWorkspaceStorageAccess(()->{if(closed||epoch!=choiceEpoch)return;WorkspaceStore.Session local=new WorkspaceStore.Session();local.environment="termux";local.name=t("Память устройства","Device storage");confirm(snapshot,paths,local,"/sdcard/Download",root,epoch);});
        else picker.chooseDirectory(t("Куда скачать?","Choose download destination"),(location,path)->confirm(snapshot,paths,location,path,root,epoch));
    }
    private void confirm(ServerProfiles.Profile profile,List<String> paths,WorkspaceStore.Session location,String folder,String root,int epoch){
        if(closed||epoch!=choiceEpoch||ServerConnection.disconnected(activity,profile.id))return;
        result=WorkspaceDialogs.builder(activity).setTitle(t("Скачать выбранное?","Download selection?"))
            .setMessage(location.name+"\n"+folder+"/"+root+"\n\n"+t("Выбрано: ","Selected: ")+paths.size()+t("\nВ отдельную папку, с исходными именами и вложенностью. Существующие файлы не заменяются. Ссылки и специальные файлы пропускаются. До 10 ГБ на файл, 20 000 записей и 256 ГБ суммарно; соединение до 6 часов. Не закрывайте приложение.","\nInto a separate folder with original names and hierarchy. Existing files are not replaced. Links and special files are skipped. Up to 10 GB per file, 20,000 entries and 256 GB total; connection up to 6 hours. Keep the app open."))
            .setPositiveButton(t("Скачать","Download"),(d,w)->transfer(profile,paths,location,folder,root,epoch)).setNegativeButton(t("Отмена","Cancel"),null).show();
        trackChoice(result,epoch);
    }
    private void transfer(ServerProfiles.Profile profile,List<String> paths,WorkspaceStore.Session location,String folder,String root,int epoch){
        if(closed||running||epoch!=choiceEpoch||ServerConnection.disconnected(activity,profile.id))return;choiceEpoch++;ownerProfile=profile.id;running=true;cancelled=false;started=SystemClock.uptimeMillis();lastUpdate=0;
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);int pad=WorkspaceDialogs.dp(activity,16);body.setPadding(pad,pad,pad,pad);
        TextView status=new TextView(activity);status.setTextColor(WorkspaceDialogs.TEXT);status.setText(t("Проверка выбранных файлов…","Checking selected files…"));body.addView(status);
        ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setIndeterminate(true);body.addView(bar);
        progress=WorkspaceDialogs.builder(activity).setTitle(t("Скачивание","Download")).setView(body).setNeutralButton(t("В фон","Background"),null).setNegativeButton(t("Остановить","Stop"),null).create();progress.setCancelable(false);progress.setCanceledOnTouchOutside(false);progress.show();presentation.begin(profile.id,progress,t("Проверка выбранных файлов…","Checking selected files…"));progress.getButton(-3).setOnClickListener(v->presentation.background());progress.getButton(-2).setOnClickListener(v->{status.setText(t("Остановка…","Stopping…"));progress.getButton(-2).setEnabled(false);cancel();});
        worker.execute(()->{
            String base=folder+(folder.equals("/")?"":"/")+root;long[] received={0};int completed=0,skipped=0;boolean success=false;Exception failure=null;
            ServerDiagnostics.Trace trace=ServerDiagnostics.begin(profile,ServerDiagnostics.Op.DOWNLOAD);
            try{
                check();ServerConnection owned=new ServerConnection(activity,profile);connection=owned;check();ServerSftp sftp=owned.sftp(360);channel=sftp;check();
                ServerDownloadPlan plan=ServerDownloadPlan.scan(new ServerDownloadPlan.Source(){public ServerSftp.Info stat(String p)throws Exception{return sftp.stat(p);}public List<ServerSftp.Entry> list(String p)throws Exception{return sftp.list(p);}public String canonical(String p)throws Exception{return sftp.canonical(p);}},paths,(count,path)->{check();update(status,bar,t("Проверка: ","Scanning: ")+count+"\n"+path,0,-1);});skipped=plan.skipped;
                check();try(ProjectFiles local=new ProjectFiles(activity,location)){
                    // Exclusive batch root: no user-supplied existing tree is merged or overwritten.
                    local.localOperation("umask 077; mkdir -- "+ProjectFiles.target(base));
                    for(String dir:plan.directories){check();ServerDownloadPlan.checkedRelative(dir);local.localOperation("mkdir -- "+ProjectFiles.target(base+"/"+dir));}
                }
                int index=0;for(ServerDownloadPlan.Item item:plan.files){check();ServerDownloadPlan.checkedRelative(item.relative);final int current=++index;final long before=received[0];String target=base+"/"+item.relative;int slash=target.lastIndexOf('/');
                    try(LocalDownloadFile local=new LocalDownloadFile(activity,location,target.substring(0,slash),target.substring(slash+1))){destination=local;check();sftp.download(item.remote,local.stream,item.info,bytes->{check();received[0]=before+bytes;String line=ServerTransferProgress.describe(received[0],plan.bytes,SystemClock.uptimeMillis()-started,WorkspaceLanguage.code(activity).equals("ru"));update(status,bar,current+" / "+plan.files.size()+" · "+item.relative+"\n"+line,received[0],plan.bytes);});check();local.finish();completed++;}finally{destination=null;}
                }
                check();success=true;
            }catch(Exception error){failure=cancelled?new CancellationException():error;}
            finally{trace.finish(trace.elapsed(),0,received[0],failure);ServerSftp sftp=channel;channel=null;if(sftp!=null)sftp.close();ServerConnection owned=connection;connection=null;if(owned!=null)owned.close();}
            final boolean done=success;final int count=completed,omitted=skipped;final String error=failure==null?"":failure.getMessage();
            main.post(()->{running=false;boolean hidden=presentation.finish();if(progress!=null)progress.dismiss();progress=null;if(closed)return;String message=(done?t("Скачано и завершено.","Download complete."):cancelled?t("Передача остановлена. Завершённые файлы сохранены.","Transfer stopped. Completed files were kept."):t("Передача не завершена. Завершённые файлы сохранены. Проверьте сеть, права и свободное место.","Download incomplete. Completed files were kept. Check network, permissions and free space."))+"\n"+base+"\n"+t("Готово файлов: ","Completed files: ")+count+"\n"+t("Пропущено ссылок / специальных файлов: ","Skipped links / special files: ")+omitted;
                if(!done&&error!=null)message+="\n"+error.substring(0,Math.min(180,error.length()));result=WorkspaceDialogs.builder(activity).setTitle(t("Скачивание","Download")).setMessage(message).setPositiveButton("OK",null).create();result.setCanceledOnTouchOutside(false);if(hidden&&done)activity.showToast(t("Скачивание завершено","Download complete"),false);else result.show();});
        });
    }
    void move(ServerProfiles.Profile selectedProfile,List<JSONObject> entries,String folder,Runnable finished){
        if(closed||running||entries.isEmpty()||ServerConnection.disconnected(activity,selectedProfile.id))return;
        final ServerProfiles.Profile profile=ServerProfiles.connectionSnapshot(selectedProfile);final int epoch=beginChoice(profile.id);
        final List<String> paths=new ArrayList<>();for(JSONObject entry:entries)paths.add(entry.optString("path"));
        result=WorkspaceDialogs.builder(activity).setTitle(t("Переместить сюда?","Move here?"))
            .setMessage(t("Выбрано: ","Selected: ")+paths.size()+"\n"+folder+t("\nТолько внутри этого сервера, без перезаписи. Между дисками операция может быть недоступна. При ошибке уже перемещённые объекты остаются в папке назначения.","\nWithin this server only, without overwriting. Moving between filesystems may be unavailable. If interrupted, already moved entries remain at the destination."))
            .setNegativeButton(t("Отмена","Cancel"),null).setPositiveButton(t("Переместить","Move"),(dialog,which)->{
                if(closed||running||epoch!=choiceEpoch||ServerConnection.disconnected(activity,profile.id))return;
                choiceEpoch++;ownerProfile=profile.id;running=true;cancelled=false;TextView state=new TextView(activity);state.setText(t("Перемещение…","Moving…"));state.setPadding(24,24,24,24);
                progress=WorkspaceDialogs.builder(activity).setTitle(t("Перемещение","Move")).setView(state).setNegativeButton(t("Остановить","Stop"),(d,w)->cancel()).create();progress.setCancelable(false);progress.setCanceledOnTouchOutside(false);progress.show();
                worker.execute(()->{int moved=0;Exception failure=null;ServerDiagnostics.Trace trace=ServerDiagnostics.begin(profile,ServerDiagnostics.Op.FILE_MOVE);
                    try{check();ServerConnection owned=new ServerConnection(activity,profile);connection=owned;check();try(ServerSftp sftp=owned.sftp()){channel=sftp;String target=sftp.canonical(folder);for(String path:paths){check();ServerSftp.Info info=sftp.stat(path);sftp.move(path,target,info);moved++;}}}catch(Exception e){failure=e;}finally{channel=null;ServerConnection owned=connection;connection=null;if(owned!=null)owned.close();trace.finish(trace.elapsed(),0,0,failure);}
                    final int count=moved;final String error=failure==null?"":failure.getMessage();main.post(()->{running=false;if(progress!=null)progress.dismiss();progress=null;if(closed)return;result=WorkspaceDialogs.builder(activity).setTitle(t("Перемещение","Move")).setMessage(t("Перемещено: ","Moved: ")+count+" / "+paths.size()+"\n"+folder+(error==null||error.isEmpty()?"":"\n"+error)).setPositiveButton("OK",null).show();finished.run();});
                });
            }).show();
        trackChoice(result,epoch);
    }
    private void check(){if(cancelled||closed)throw new CancellationException();}
    private void update(TextView status,ProgressBar bar,String text,long bytes,long total){long now=SystemClock.uptimeMillis();if(now-lastUpdate<150&&bytes!=total)return;lastUpdate=now;main.post(()->{if(closed||cancelled)return;status.setText(text);presentation.update(text,bytes,total);bar.setIndeterminate(total<0);if(total>=0)bar.setProgress(total==0?1000:(int)Math.min(1000,bytes*1000.0/total));});}
    private void cancel(){cancelled=true;LocalDownloadFile local=destination;if(local!=null)local.cancel();ServerConnection owned=connection;if(owned!=null)owned.close();}
    @Override public void close(){if(closed)return;closed=true;presentation.finish();picker.close();cancel();if(progress!=null)progress.dismiss();if(result!=null)result.dismiss();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
