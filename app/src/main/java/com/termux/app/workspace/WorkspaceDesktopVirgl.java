package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.x11.VirglInstaller;
import com.termux.x11.X11InstallProgress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/** Renderer setup uses a pipe-owned process group; cancellation never adopts host PIDs. */
final class WorkspaceDesktopVirgl {
    private static String t(TermuxActivity a,String ru,String en){return WorkspaceLanguage.code(a).equals("ru")?ru:en;}
    static void prepare(TermuxActivity a,String target,BooleanSupplier busy,Runnable updated,WorkspaceDesktopGpu.Inline inline,boolean verified){
        if(!VirglInstaller.supported(target)){if(inline!=null)inline.finished(false,"ERROR:VIRGL_TARGET");updated.run();return;}
        if(verified&&VirglInstaller.installed(a,target)){a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,"virgl").apply();updated.run();return;}
        AlertDialog confirm=WorkspaceDialogs.builder(a).setTitle(t(a,"Установить VirGL?","Install VirGL?"))
            .setMessage(t(a,"Экспериментальный рендер через ANGLE на стороне Android. В Termux будут установлены virglrenderer-android, angle-android и Mesa из его репозиториев. В Linux-окружениях библиотеки Mesa устанавливаются только внутри выбранной системы. В Arch их установка также обновит пакеты этого Arch (pacman -Syu). Системные пакеты Android не меняются. Для каждого рабочего стола запускается собственный сервер с отдельным сокетом; он и его дочерние процессы останавливаются вместе со столом. Alpine скачивает отдельный клиент Mesa с проверкой SHA-256 и сохраняет его для работы без интернета, без замены системной Mesa. Termux, Debian, Fedora, Void и Arch используют ANGLE Vulkan. Совместимость и скорость зависят от устройства.","Experimental rendering through ANGLE on Android. Installs virglrenderer-android, angle-android and Mesa from Termux repositories. Linux environments also require Mesa libraries installed only inside the selected system. On Arch their installation also upgrades this Arch’s packages (pacman -Syu). Android system packages are unchanged. Each desktop owns its own server and socket; it and its child processes stop with the desktop. Alpine downloads a separate SHA-256-verified Mesa client and caches it for offline reuse without replacing system Mesa. Termux, Debian, Fedora, Void and Arch use ANGLE Vulkan. Compatibility and speed depend on the device."))
            .setNegativeButton(t(a,"Назад","Back"),(d,w)->{if(inline!=null)inline.finished(false,"ERROR:GPU_CANCELLED");updated.run();})
            .setPositiveButton(t(a,"Установить","Install"),(d,w)->{if(busy.getAsBoolean()){if(inline!=null)inline.finished(false,"ERROR:GPU_BUSY");updated.run();return;}install(a,target,busy,updated,inline);}).create();
        confirm.setCancelable(false);confirm.setCanceledOnTouchOutside(false);confirm.show();
    }
    private static void install(TermuxActivity a,String target,BooleanSupplier busy,Runnable updated,WorkspaceDesktopGpu.Inline inline){
        Handler main=new Handler(Looper.getMainLooper());AtomicBoolean cancelled=new AtomicBoolean();AtomicReference<VirglInstaller> owned=new AtomicReference<>();
        LinearLayout body=AgentStatusUi.body(a);ProgressBar bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setIndeterminate(true);bar.setMax(100);body.addView(bar);TextView detail=AgentStatusUi.info(body,t(a,"Подготовка компонентов VirGL…","Preparing VirGL components…"));
        AlertDialog wait=WorkspaceDialogs.builder(a).setTitle("VirGL").setView(body).setNegativeButton(t(a,"Отмена","Cancel"),null).setPositiveButton(t(a,"Назад","Back"),null).create();wait.setCancelable(false);wait.setCanceledOnTouchOutside(false);
        Runnable stop=()->{cancelled.set(true);VirglInstaller job=owned.get();if(job!=null)job.close();if(inline!=null)inline.progress(-1,t(a,"Остановка установщика…","Stopping installer…"));else{detail.setText(t(a,"Остановка установщика…","Stopping installer…"));wait.getButton(-2).setEnabled(false);}};
        if(inline!=null)inline.started(stop);else{wait.show();wait.getButton(-1).setEnabled(false);wait.getButton(-2).setOnClickListener(v->stop.run());}
        ExecutorService worker=Executors.newSingleThreadExecutor();worker.execute(()->{
            boolean success=false;StringBuilder report=new StringBuilder();VirglInstaller job=null;
            try{
                if(cancelled.get())throw new java.io.IOException("Cancelled");
                job=VirglInstaller.start(a.getApplicationContext(),target);owned.set(job);if(cancelled.get())job.close();
                X11InstallProgress progress=new X11InstallProgress();StringBuilder tail=new StringBuilder();AtomicBoolean ready=new AtomicBoolean(),exited=new AtomicBoolean();long[] last={0};
                job.read(chunk->{
                    report.append(chunk);if(report.length()>8192)report.delete(0,report.length()-8192);
                    tail.append(chunk);int end;while((end=tail.indexOf("\n"))>=0){String line=tail.substring(0,end).trim();tail.delete(0,end+1);if(line.equals("READY:virgl"))ready.set(true);if(line.equals("DESKTOP_CHILD_EXIT=0"))exited.set(true);}if(tail.length()>8192)tail.delete(0,tail.length()-8192);
                    long now=SystemClock.elapsedRealtime();progress.feed(chunk,now);if(now-last[0]<200)return;last[0]=now;
                    int percent=progress.percent();long eta=progress.etaSeconds(now);String text=progress.currentItem();if(text.isEmpty())text=t(a,"Установка компонентов VirGL…","Installing VirGL components…");
                    if(eta>=0)text+="\n"+t(a,"Осталось: ≈ ","Remaining: ≈ ")+eta+t(a," с"," s");
                    if(progress.totalBytes()>0)text+="\n"+String.format(java.util.Locale.ROOT,"%.1f / %.1f MB",progress.downloadedBytes()/1000000.0,progress.totalBytes()/1000000.0);
                    String status=text;main.post(()->{if(cancelled.get()||a.isFinishing()||a.isDestroyed())return;if(inline!=null)inline.progress(percent,status);else{bar.setIndeterminate(percent<0);if(percent>=0)bar.setProgress(percent);detail.setText(status);}});
                });
                int exit=job.waitFor();success=(exit==0||exit==137)&&ready.get()&&exited.get()&&!cancelled.get();if(success)job.markReady();
            }catch(Exception error){success=false;report.append("\nERROR:VIRGL_SETUP_FAILED ").append(error.getClass().getSimpleName());}
            finally{owned.set(null);if(job!=null){job.close();try{job.waitFor();job.cleanup();}catch(InterruptedException error){Thread.currentThread().interrupt();}}}
            boolean result=success;String diagnostic=cancelled.get()?"ERROR:GPU_CANCELLED":report.toString();main.post(()->{
                if(a.isFinishing()||a.isDestroyed())return;
                boolean applied=result&&!busy.getAsBoolean();if(applied)a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,"virgl").apply();
                if(inline!=null){inline.finished(applied,diagnostic);updated.run();}
                else{bar.setVisibility(android.view.View.GONE);wait.getButton(-2).setVisibility(android.view.View.GONE);detail.setText(applied?t(a,"VirGL установлен и выбран.","VirGL installed and selected."):diagnostic);wait.getButton(-1).setEnabled(true);wait.getButton(-1).setOnClickListener(v->{wait.dismiss();updated.run();});}
            });worker.shutdown();
        });
    }
}
