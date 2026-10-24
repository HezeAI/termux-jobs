package com.termux.app.workspace;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** User-selected documents stream directly into private remote temporaries over SFTP. */
final class WorkspaceServerUploads implements AutoCloseable {
    final ServerTransferPresentation presentation=new ServerTransferPresentation();
    private final TermuxActivity activity;private final Runnable completed;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
    private final WorkspaceLocalFilePicker picker;
    private final List<AlertDialog> dialogs=new ArrayList<>();private final List<Document> documents=new ArrayList<>();
    private ServerProfiles.Profile profile;private String directory;private boolean running,closed;private volatile boolean cancelled;
    private volatile ServerConnection connection;private volatile ServerSftp sftp;private volatile InputStream source;
    private CompletableFuture<String> decision;private AlertDialog progress;private TextView status;private ProgressBar bar;
    private int index,uploaded,skipped,choiceEpoch;private long lastUpdate,startedAt;
    private static final class Document {final WorkspaceStore.Session location;final ProjectFiles.Entry entry;String name;final long size;Document(WorkspaceStore.Session s,ProjectFiles.Entry e){location=s.copy();entry=e;name=e.name;size=e.size;}}
    WorkspaceServerUploads(TermuxActivity a,Runnable done){activity=a;completed=done;picker=new WorkspaceLocalFilePicker(a);}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private void show(AlertDialog d){dialogs.add(d);d.setCanceledOnTouchOutside(false);d.setOnDismissListener(x->dialogs.remove(d));d.show();}
    private void error(String message){if(!closed)show(WorkspaceDialogs.builder(activity).setTitle(t("Отправка файлов","Upload files")).setMessage(message).setPositiveButton("OK",null).create());}
    void disconnect(String id){if(profile!=null&&profile.id.equals(id)){choiceEpoch++;picker.cancelSelection();cancel();for(AlertDialog d:new ArrayList<>(dialogs))if(d!=progress)d.dismiss();}}
    boolean active(){return running;}
    void pick(ServerProfiles.Profile p,String path){
        if(closed||ServerConnection.disconnected(activity,p.id))return;if(active()){presentation.foreground();return;}if(path==null||!path.startsWith("/")){error(t("Сначала откройте каталог сервера.","Open a server directory first."));return;}
        final int epoch=++choiceEpoch;picker.cancelSelection();for(AlertDialog d:new ArrayList<>(dialogs))if(d!=progress)d.dismiss();documents.clear();
        profile=ServerProfiles.connectionSnapshot(p);directory=path;final String serverId=profile.id;
        picker.chooseMultiple(t("Выбрать файлы для загрузки","Choose files to upload"),(location,entries)->{
            if(closed||epoch!=choiceEpoch||ServerConnection.disconnected(activity,serverId))return;
            if(entries.isEmpty()||entries.size()>50){error(t("Выберите от 1 до 50 файлов.","Select 1 to 50 files."));return;}
            List<Document> selected=new ArrayList<>();
            for(ProjectFiles.Entry entry:entries){if(entry.directory||!ServerSftp.validName(entry.name)||entry.size<0||entry.size>ServerSftp.MAX_BYTES){error(t("Недопустимый файл. Лимит — 2 ГБ на файл.","Invalid file. Limit: 2 GB per file."));return;}selected.add(new Document(location,entry));}
            documents.clear();documents.addAll(selected);confirm();
        });
    }
    private void confirm(){final int epoch=choiceEpoch;StringBuilder body=new StringBuilder(profile.name+"\n"+profile.user+"@"+profile.host+":"+profile.port+"\n"+directory+"\n\n");for(Document d:documents)body.append(d.name).append(" · ").append(d.size<0?t("размер неизвестен","unknown size"):ServerFileInfo.size(d.size,WorkspaceLanguage.code(activity).equals("ru"))).append('\n');body.append(t("\nСовпавшие имена будут запрошены отдельно. Передача по SFTP в правах выбранной SSH-учётной записи.","\nConflicting names are reviewed separately. SFTP uses the selected SSH account's permissions."));
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Отправить на сервер?","Upload to server?")).setMessage(body).setPositiveButton(t("Отправить","Upload"),(d,w)->{if(epoch==choiceEpoch)start();}).setNegativeButton(t("Отмена","Cancel"),null).create();show(dialog);dialog.setOnDismissListener(d->{dialogs.remove(dialog);if(choiceEpoch==epoch)choiceEpoch++;});}
    private void start(){if(closed||running||ServerConnection.disconnected(activity,profile.id))return;choiceEpoch++;running=true;cancelled=false;index=uploaded=skipped=0;LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);int pad=WorkspaceDialogs.dp(activity,16);body.setPadding(pad,pad,pad,pad);status=new TextView(activity);status.setTextColor(WorkspaceDialogs.TEXT);body.addView(status);bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);body.addView(bar,new LinearLayout.LayoutParams(-1,-2));progress=WorkspaceDialogs.builder(activity).setTitle(t("Отправка файлов","Uploading files")).setView(body).setNeutralButton(t("В фон","Background"),null).setNegativeButton(t("Остановить","Stop"),null).create();progress.setCancelable(false);show(progress);presentation.begin(profile.id,progress,t("Подключение…","Connecting…"));progress.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->presentation.background());progress.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->cancel());worker.execute(this::transfer);}
    private void cancel(){cancelled=true;if(status!=null)status.setText(t("Остановка и очистка временного файла…","Stopping and cleaning temporary file…"));if(progress!=null)progress.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);CompletableFuture<String> choice=decision;if(choice!=null)choice.completeExceptionally(new IOException("CANCELLED"));ServerSftp channel=sftp;if(channel!=null)channel.close();ServerConnection c=connection;if(c!=null)c.close();InputStream input=source;if(input!=null)try{input.close();}catch(Exception ignored){}}
    private void transfer(){boolean failed=false,cleanupFailed=false;String leftover=null;try{
        connection=new ServerConnection(activity,profile);if(cancelled)throw new IOException("CANCELLED");sftp=connection.sftp();String target=sftp.canonical(directory);
        for(index=0;index<documents.size();index++){if(cancelled)throw new IOException("CANCELLED");Document doc=documents.get(index);ServerSftp.Info expected;
            while(true){expected=sftp.stat(target+(target.equals("/")?"":"/")+doc.name);if(!expected.exists)break;String answer=choose(doc,expected,sftp.canReplace());if(answer.equals("skip")){skipped++;break;}if(answer.equals("replace"))break;doc.name=answer.substring(5);}
            // A skip decision is handled before any local input or remote output is opened.
            if(skipCurrent){skipCurrent=false;continue;}
            final Document current=doc;lastUpdate=0;startedAt=SystemClock.uptimeMillis();postProgress(current,0);ProjectFiles local=new ProjectFiles(activity,doc.location);try{source=local.openUpload(doc.entry);}catch(Exception e){local.close();throw e;}
            ServerDiagnostics.Trace trace=ServerDiagnostics.begin(profile,ServerDiagnostics.Op.UPLOAD);long[] acknowledged={0};Exception failure=null;
            try(InputStream input=source){sftp.upload(input,target,doc.name,expected,bytes->{acknowledged[0]=bytes;if(cancelled)throw new CancellationException();postProgress(current,bytes);});}catch(Exception e){failure=cancelled?new CancellationException():e;throw e;}finally{source=null;trace.finish(trace.elapsed(),0,acknowledged[0],failure);}uploaded++;
        }
    }catch(Exception e){failed=!cancelled;}finally{
        ServerSftp channel=sftp;if(channel!=null)leftover=channel.pendingTemp();if(channel!=null)channel.close();ServerConnection c=connection;if(c!=null)c.close();sftp=null;connection=null;
        if(leftover!=null&&ServerConnection.disconnected(activity,profile.id)){cleanupFailed=true;}else if(leftover!=null){try(ServerConnection cleanup=new ServerConnection(activity,profile);ServerSftp clean=cleanup.sftp()){clean.removeTemporary(leftover);}catch(Exception e){cleanupFailed=true;}}
        final boolean failure=failed,dirty=cleanupFailed;final String remaining=leftover;main.post(()->{running=false;boolean hidden=presentation.finish();if(progress!=null)progress.dismiss();progress=null;if(closed)return;String summary=t("Отправлено: ","Uploaded: ")+uploaded+t(" · Пропущено: "," · Skipped: ")+skipped;if(cancelled)summary+="\n"+t("Передача остановлена. Если остановка совпала с публикацией, проверьте итоговый каталог.","Transfer stopped. If stopped during publication, check the destination directory.");if(failure)summary+="\n"+t("Передача не завершена. Проверьте сеть, права, свободное место и поддержку SFTP. При потере связи в момент публикации файл уже мог быть отправлен — проверьте каталог.","Transfer incomplete. Check network, permissions, free space and SFTP support. If connectivity was lost during publication, the file may already have been uploaded: check the directory.");if(dirty)summary+="\n"+t("Не удалось удалить временный файл: ","Could not remove temporary file: ")+remaining;if(hidden&&!failure&&!dirty&&!cancelled)activity.showToast(t("Загрузка на сервер завершена","Upload complete"),false);else error(summary);completed.run();});
    }}
    private boolean skipCurrent;
    private String choose(Document doc,ServerSftp.Info info,boolean replace)throws Exception{
        CompletableFuture<String> choice=new CompletableFuture<>();decision=choice;main.post(()->{if(closed||cancelled){choice.completeExceptionally(new IOException("CANCELLED"));return;}LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);TextView label=new TextView(activity);label.setTextColor(WorkspaceDialogs.TEXT);label.setText(t("Файл уже существует. Новое имя или замена обычного файла. Замена атомарная, но не блокирует внешних редакторов; владелец нового файла — SSH-пользователь.","File exists. Choose a new name or replace a regular file. Replacement is atomic, but does not lock external editors; the new file belongs to the SSH user."));body.addView(label);EditText name=new EditText(activity);name.setSingleLine(true);name.setText(doc.name);body.addView(name);Button stop=new Button(activity);stop.setText(t("Остановить передачу","Stop upload"));body.addView(stop);
            AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(doc.name).setView(body).setPositiveButton(t("Переименовать","Rename"),null).setNeutralButton(t("Заменить","Replace"),(x,w)->choice.complete("replace")).setNegativeButton(t("Пропустить","Skip"),(x,w)->choice.complete("skip")).create();d.setCancelable(false);show(d);stop.setOnClickListener(v->{d.dismiss();cancel();});d.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(info.regular()&&replace);d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String n=name.getText().toString();if(!ServerSftp.validName(n)||n.equals(doc.name)){name.setError(t("Введите другое имя","Enter a different name"));return;}d.dismiss();choice.complete("name:"+n);});});
        String answer;try{answer=choice.get();}finally{decision=null;}skipCurrent=answer.equals("skip");return answer;
    }
    private void postProgress(Document doc,long bytes){long now=SystemClock.uptimeMillis();if(bytes!=0&&now-lastUpdate<150)return;lastUpdate=now;int number=index+1;main.post(()->{if(closed||cancelled||progress==null)return;status.setText(number+"/"+documents.size()+" · "+doc.name+"\n"+ServerTransferProgress.describe(bytes,doc.size,Math.max(0,now-startedAt),WorkspaceLanguage.code(activity).equals("ru")));bar.setIndeterminate(doc.size<=0);presentation.update(status.getText().toString(),bytes,doc.size<=0?-1:doc.size);if(doc.size>0)bar.setProgress((int)Math.min(1000,bytes*1000/doc.size));});}
    @Override public void close(){if(closed)return;closed=true;presentation.finish();picker.close();cancel();for(AlertDialog d:new ArrayList<>(dialogs))d.dismiss();worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
