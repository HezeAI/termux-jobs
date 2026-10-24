package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.shell.TermuxShellUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Explicit local environment checks/setup, independent of all AI transports. */
final class EnvironmentSetup implements AutoCloseable {
    static final class Result {
        final String report;final int code;
        Result(String text,int status){report=text;code=status;}
        boolean ready(){return code==0&&report.contains("CHECK:done")&&report.contains("READY:environment")&&report.contains("READY:shell")&&!report.contains("MISSING:");}
    }
    private final TermuxActivity activity;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean closed,running;
    private AlertDialog progress;
    interface InstallationRunner {Result run(String alias,Consumer<String> update,File cancel)throws Exception;}
    private final InstallationRunner installationRunner;
    private final java.util.function.Supplier<List<WorkspaceEnvironments.Choice>> inventory;
    EnvironmentSetup(TermuxActivity a){this(a,WorkspaceEnvironments::choices);}
    EnvironmentSetup(TermuxActivity a,java.util.function.Supplier<List<WorkspaceEnvironments.Choice>> choices){this(a,choices,null);}
    EnvironmentSetup(TermuxActivity a,java.util.function.Supplier<List<WorkspaceEnvironments.Choice>> choices,InstallationRunner runner){activity=a;inventory=choices;installationRunner=runner;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    java.util.function.BiConsumer<WorkspaceEnvironments.Choice,Runnable> removal;
    private static final class InstallJob {
        final String alias;final File cancel;
        final EnvironmentInstallProgress progress=new EnvironmentInstallProgress();
        boolean active=true,stopping,success,resuming;String report="",log="";
        InstallJob(String alias,File cancel){this.alias=alias;this.cancel=cancel;}
    }
    private final Map<String,InstallJob> installations=new HashMap<>();
    private final Map<String,List<Runnable>> cardUpdates=new HashMap<>();
    private boolean resumable(String alias){if(!WorkspaceLocalFilePicker.validAlias(alias))return false;android.content.SharedPreferences p=activity.getSharedPreferences("environment-install-attempts",0);return p.getBoolean("pending."+alias,false)||(!p.getBoolean("completed."+alias,false)&&new File(activity.getFilesDir(),".environment-control/downloads/"+alias+"/state.json").isFile());}
    private void recordAttempt(String alias,boolean complete){activity.getSharedPreferences("environment-install-attempts",0).edit().putBoolean("pending."+alias,!complete).putBoolean("completed."+alias,complete).apply();if(complete)try{File base=new File(activity.getFilesDir().getCanonicalFile(),".environment-control");if(!base.getCanonicalPath().equals(base.getAbsolutePath()))return;File state=new File(base,"downloads/"+alias+"/state.json");if(state.getCanonicalPath().equals(state.getAbsolutePath()))state.delete();}catch(IOException ignored){}}
    private boolean installed(WorkspaceEnvironments.Choice choice){InstallJob job=installations.get(choice.alias);return job==null?choice.installed&&!resumable(choice.alias):job.success;}
    private void refreshCard(String alias){for(Runnable update:new ArrayList<>(cardUpdates.getOrDefault(alias,Collections.emptyList())))update.run();}
    private void stopInstall(InstallJob job){
        if(!job.active||job.stopping)return;
        try{if(!job.cancel.createNewFile()&&!job.cancel.isFile())throw new IOException();job.stopping=true;refreshCard(job.alias);}
        catch(IOException error){activity.showToast(t("Не удалось запросить остановку","Could not request stop"),true);}
    }
    private String storeInstallLog(String alias,Result answer){
        String log=answer.ready()?"":EnvironmentInstallLog.report(alias,answer.code,answer.report);
        try{if(answer.ready())EnvironmentInstallLog.clear(activity.getFilesDir(),alias);else EnvironmentInstallLog.save(activity.getFilesDir(),alias,log);}catch(IOException ignored){/* The current report remains available in memory. */}
        return log;
    }
    private void startInstall(WorkspaceEnvironments.Choice choice){
        if(closed)return;
        if(running){activity.showToast(t("Дождитесь текущей операции","Wait for the current operation"),true);return;}
        File directory=new File(activity.getFilesDir(),".environment-control");
        if(!directory.isDirectory()&&!directory.mkdirs()){activity.showToast(t("Нет доступа к папке установки","Cannot access installation directory"),true);return;}
        File jobDirectory;
        try{jobDirectory=executionDirectory(directory);}catch(IOException error){activity.showToast(t("Нет доступа к папке установки","Cannot access installation directory"),true);return;}
        InstallJob job=new InstallJob(choice.alias,new File(jobDirectory,"cancel-"+UUID.randomUUID()));job.resuming=resumable(choice.alias);recordAttempt(choice.alias,false);installations.put(choice.alias,job);running=true;refreshCard(choice.alias);
        worker.execute(()->{
            Result result;
            try{Consumer<String> update=report->main.post(()->{job.report=report;job.progress.update(report,android.os.SystemClock.elapsedRealtime());if(!closed)refreshCard(choice.alias);});result=installationRunner==null?execute(choice.alias,true,update,job):installationRunner.run(choice.alias,update,job.cancel);}
            catch(Exception error){result=new Result("ERROR:SETUP_FAILED",1);}
            finally{cleanExecution(job.cancel.getParentFile(),job.cancel);}
            Result answer=result;String log=storeInstallLog(choice.alias,answer);
            main.post(()->{job.active=false;job.cancel.delete();job.success=answer.ready();recordAttempt(choice.alias,job.success);job.report=EnvironmentInstallLog.clean(answer.report);job.log=log;running=false;if(!closed)refreshCard(choice.alias);});
        });
    }
    private void offerInstall(WorkspaceEnvironments.Choice choice){
        InstallJob job=installations.get(choice.alias);if(job!=null&&job.active){stopInstall(job);return;}
        WorkspaceDialogs.builder(activity).setTitle((resumable(choice.alias)?t("Продолжить установку ","Resume installing "):t("Установить ","Install "))+choice.label+"?")
            .setMessage(t("Система будет скачана и установлена как отдельное окружение внутри Termux Job’s. Другое приложение Termux устанавливать не нужно. Нужны интернет и свободное место. Установку выполняет штатный proot-distro. Он повторно использует целиком скачанные и проверенные слои; оборванный слой может скачиваться заново. Если proot-distro отсутствует, установим его. Уже установленные системы не заменяются. Поддержка зависит от версии proot-distro и архитектуры; недоступный образ будет отклонён.","Download and install a separate environment inside Termux Job’s. No separate Termux app is needed. Internet and free storage are required. Installation uses the native proot-distro CLI. It can reuse complete verified layers; an interrupted layer may download again. Missing proot-distro will be installed. Existing systems are never replaced. Availability depends on proot-distro and architecture; unavailable images are rejected.")+(choice.alias.equals("archlinux")?t("\nНа ARM64 устанавливается Arch Linux ARM из архива Termux, с проверкой SHA-256. Это отдельный проект от Arch Linux для x86-64.","\nOn ARM64, install Arch Linux ARM from a Termux archive with SHA-256 verification. It is a separate project from x86-64 Arch Linux."):""))
            .setPositiveButton(resumable(choice.alias)?t("Продолжить","Resume"):t("Установить","Install"),(d,w)->startInstall(choice)).setNegativeButton(t("Назад","Back"),null).show();
    }
    private void showInstallLog(WorkspaceEnvironments.Choice choice){
        InstallJob job=installations.get(choice.alias);if(closed||job!=null&&job.active)return;
        if(job!=null&&!job.log.isEmpty()){EnvironmentInstallLogUi.show(activity,choice.label,job.log);return;}
        worker.execute(()->{String report;try{report=EnvironmentInstallLog.read(activity.getFilesDir(),choice.alias);}catch(IOException e){report="";}String snapshot=report;main.post(()->{if(closed)return;if(snapshot.isEmpty())activity.showToast(t("Журнал недоступен","Log unavailable"),true);else EnvironmentInstallLogUi.show(activity,choice.label,snapshot);});});
    }
    void choose(Consumer<WorkspaceEnvironments.Choice> selected){
        List<WorkspaceEnvironments.Choice> choices=new ArrayList<>(inventory.get());
        Map<String,List<Runnable>> bindings=new HashMap<>();
        boolean[] chooserOpen={true};
        ArrayAdapter<WorkspaceEnvironments.Choice> rows=new ArrayAdapter<WorkspaceEnvironments.Choice>(activity,0,choices){
            @Override public View getView(int position,View recycled,ViewGroup parent){
                WorkspaceEnvironments.Choice choice=getItem(position);int gap=WorkspaceDialogs.dp(activity,8);
                FrameLayout outer=new FrameLayout(activity);outer.setPadding(gap*2,gap/2,gap*2,gap/2);
                LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(gap*2,gap,gap,gap);card.setBackground(WorkspaceDialogs.background(activity,false));
                LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);
                TextView label=new TextView(activity);label.setText(choice.label);label.setTextSize(15);label.setTextColor(WorkspaceDialogs.TEXT);label.setMaxLines(2);
                LinearLayout labels=new LinearLayout(activity);labels.setOrientation(LinearLayout.VERTICAL);labels.addView(label);
                if(choice.alias.equals("ubuntu-ai")){TextView subtitle=new TextView(activity);subtitle.setText(t("Используется для AI-агентов","Used for AI agents"));subtitle.setTextSize(12);subtitle.setTextColor(WorkspaceDialogs.MUTED);labels.addView(subtitle);}
                row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
                TextView more=WorkspaceDialogs.action(activity,t("Подробнее","Details"),0);more.setTextSize(12);more.setGravity(Gravity.CENTER);more.setMinHeight(WorkspaceDialogs.dp(activity,44));more.setPadding(gap/2,gap,gap/2,gap);more.setTag("environment-install-details-"+choice.alias);
                row.addView(more,new LinearLayout.LayoutParams(-2,-2));more.setOnClickListener(v->showInstallLog(choice));
                TextView badge=WorkspaceDialogs.action(activity,"",0);badge.setTextSize(12);badge.setGravity(Gravity.CENTER);badge.setMinHeight(WorkspaceDialogs.dp(activity,44));badge.setPadding(gap,gap,gap,gap);badge.setTag("environment-install-"+choice.alias);
                LinearLayout.LayoutParams badgeParams=new LinearLayout.LayoutParams(-2,-2);badgeParams.leftMargin=gap;row.addView(badge,badgeParams);badge.setOnClickListener(v->offerInstall(choice));
                WorkspaceIconButton trash=new WorkspaceIconButton(activity);trash.setIcon(activity.getDrawable(com.termux.R.drawable.workspace_delete).mutate(),WorkspaceDialogs.dp(activity,20));trash.setTextColor(0xffff8888);trash.setFocusable(false);trash.setFocusableInTouchMode(false);trash.setContentDescription(t("Удалить ","Delete ")+choice.label);
                row.addView(trash,new LinearLayout.LayoutParams(gap*6,gap*6));
                trash.setOnClickListener(v->{if(removal!=null)removal.accept(choice,()->{installations.remove(choice.alias);choices.clear();choices.addAll(inventory.get());notifyDataSetChanged();});});
                card.addView(row);LinearLayout detail=new LinearLayout(activity);detail.setOrientation(LinearLayout.VERTICAL);detail.setTag("environment-progress-"+choice.alias);
                ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);detail.addView(bar,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(activity,10)));
                TextView remaining=new TextView(activity);remaining.setTextColor(WorkspaceDialogs.MUTED);remaining.setTextSize(12);detail.addView(remaining);
                TextView line=new TextView(activity);line.setTextSize(11);line.setTextColor(WorkspaceDialogs.MUTED);line.setSingleLine(true);line.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);line.setMarqueeRepeatLimit(-1);detail.addView(line);card.addView(detail);
                Runnable update=()->{
                    InstallJob job=installations.get(choice.alias);boolean active=job!=null&&job.active,ready=installed(choice);
                    more.setVisibility(!active&&!ready&&(job!=null&&!job.log.isEmpty()||EnvironmentInstallLog.exists(activity.getFilesDir(),choice.alias))?View.VISIBLE:View.GONE);
                    badge.setText(active?(job.stopping?t("Остановка…","Stopping…"):t("Стоп","Stop")):ready?t("Установлено","Installed"):resumable(choice.alias)?t("Продолжить","Resume"):t("Установить","Install"));badge.setEnabled(!ready&&(!active||!job.stopping));badge.setTextColor(ready?WorkspaceDialogs.MUTED:WorkspaceDialogs.ACCENT);badge.setAlpha(ready?.6f:1f);
                    trash.setVisibility((ready||choice.installed)&&!active&&!choice.alias.isEmpty()&&removal!=null?View.VISIBLE:View.GONE);
                    detail.setVisibility(job!=null&&!job.success?View.VISIBLE:View.GONE);bar.setVisibility(active?View.VISIBLE:View.GONE);line.setSelected(active);
                    if(job!=null){int percent=job.progress.percent;bar.setIndeterminate(percent<0);if(percent>=0)bar.setProgress(percent);
                        remaining.setText(active?(job.stopping?t("Остановка установщика…","Stopping installer…"):t("Осталось на этапе: ","Remaining in this phase: ")+(job.progress.eta<0?t("рассчитывается","calculating"):"≈ "+job.progress.eta+t(" с"," s"))):job.report.contains("INSTALL_CANCELLED")?t("Установка остановлена","Installation stopped"):t("Установка не завершена","Installation did not finish"));
                        String failure=EnvironmentInstallProgress.failureLine(job.report);
                        if(failure.equals("ERROR:INCOMPLETE_ROOTFS_REQUIRES_REMOVAL"))failure=t("Осталась незавершённая распаковка. Удалите это неполное окружение перед продолжением; скачанные данные сохранятся.","An incomplete extraction remains. Remove this incomplete environment before resuming; downloaded data will be retained.");
                        else if(failure.equals("MISSING:shell"))failure=t("Папка системы найдена, но её оболочка отсутствует. Возможно, установка была прервана.","System directory found, but its shell is missing. Installation may have been interrupted.");
                        else if(failure.equals("MISSING:environment"))failure=t("Папка установленной системы не найдена.","Installed system directory not found.");
                        else if(failure.equals("MISSING:proot-distro"))failure=t("Не найден proot-distro.","proot-distro not found.");
                        line.setText(active?job.progress.line:failure.isEmpty()?t("Причина не указана установщиком","Installer did not report the cause"):failure);}
                };
                // ListView also asks for unattached rows during measurement. Those must
                // never steal the subscription from a card actually visible on screen.
                outer.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
                    @Override public void onViewAttachedToWindow(View view){
                        if(closed||!chooserOpen[0])return;
                        List<Runnable> local=bindings.computeIfAbsent(choice.alias,k->new ArrayList<>());
                        List<Runnable> global=cardUpdates.computeIfAbsent(choice.alias,k->new ArrayList<>());
                        if(!local.contains(update))local.add(update);if(!global.contains(update))global.add(update);update.run();
                    }
                    @Override public void onViewDetachedFromWindow(View view){
                        List<Runnable> local=bindings.get(choice.alias);if(local!=null)local.remove(update);
                        List<Runnable> global=cardUpdates.get(choice.alias);if(global!=null){global.remove(update);if(global.isEmpty())cardUpdates.remove(choice.alias);}
                    }
                });
                update.run();
                outer.addView(card,new FrameLayout.LayoutParams(-1,-2));return outer;
            }
        };
        AlertDialog chooser=WorkspaceDialogs.builder(activity).setTitle(t("Окружение","Environment")).setAdapter(rows,null).setNegativeButton(t("Назад","Back"),null).create();
        chooser.setOnDismissListener(d->{chooserOpen[0]=false;for(Map.Entry<String,List<Runnable>> entry:bindings.entrySet()){List<Runnable> updates=cardUpdates.get(entry.getKey());if(updates!=null){updates.removeAll(entry.getValue());if(updates.isEmpty())cardUpdates.remove(entry.getKey());}}});
        chooser.show();chooser.getListView().setOnItemClickListener((parent,view,index,id)->{WorkspaceEnvironments.Choice choice=choices.get(index);InstallJob job=installations.get(choice.alias);if(job!=null&&job.active)return;if(installed(choice)){chooser.dismiss();selected.accept(new WorkspaceEnvironments.Choice(choice.environment,choice.alias,choice.label,choice.home,true));}else offerInstall(choice);});
    }
    void prepareMigrationTools(Consumer<Boolean> done){WorkspaceDialogs.builder(activity).setTitle(t("Подготовить миграцию?","Prepare migration?"))
        .setMessage(t("Проверим Python и proot-distro с поддержкой блокировок в Termux. Установим недостающие пакеты после подтверждения. При старой версии proot-distro потребуется доступная в репозитории новая версия. Ubuntu не копируется, сессии не останавливаются.","Check Python and proot-distro locking support in Termux. Install missing packages after confirmation. An outdated proot-distro requires a newer repository version. Ubuntu is not copied and sessions are not stopped."))
        .setNegativeButton(t("Назад","Back"),null).setPositiveButton(t("Подготовить","Prepare"),(d,w)->run("agent-migration-tools",true,r->done.accept(r.ready()))).show();}
    void installAgentUbuntu(Consumer<Boolean> completed){WorkspaceDialogs.builder(activity).setTitle(t("Установить Ubuntu AI?","Install Ubuntu AI?" )).setMessage(t("Будет скачана отдельная чистая Ubuntu с именем ubuntu-ai. Нужны интернет и свободное место. Агенты установят свои компоненты при первом запуске после подтверждения. Другие системы не заменяются.","Download a separate clean Ubuntu named ubuntu-ai. Internet and free storage are required. Agents install their components on first launch after confirmation. Other systems are never replaced.")).setNegativeButton(t("Назад","Back"),null).setPositiveButton(t("Установить","Install"),(d,w)->run("ubuntu-ai",true,r->completed.accept(r.ready()))).show();}
    void check(String environment,Consumer<Result> completed){String alias=environment.equals("termux")?"termux":environment.equals("ubuntu")?"ubuntu":environment.startsWith("proot:")?environment.substring(6):"";if(!alias.isEmpty())run(alias,false,completed);}
    private void run(String alias,boolean install,Consumer<Result> completed){
        if(closed||running)return;running=true;
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);int pad=Math.round(16*activity.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
        ProgressBar spinner=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);spinner.setIndeterminate(true);box.addView(spinner);
        TextView text=new TextView(activity);text.setTextColor(0xffd5e4e0);text.setTextSize(13);text.setText(t("Проверка компонентов…","Checking components…"));box.addView(text);
        AlertDialog wait=WorkspaceDialogs.builder(activity).setTitle((install?t("Установка · ","Installing · "):t("Проверка среды запуска · ","Launch environment check · "))+(alias.equals("agent-migration-tools")?t("Компоненты миграции","Migration components"):WorkspaceEnvironments.label(alias))).setView(box).setPositiveButton(t("Назад","Back"),null).create();progress=wait;wait.setCancelable(false);wait.setCanceledOnTouchOutside(false);wait.show();wait.getButton(-1).setEnabled(false);
        worker.execute(()->{Result result;try{result=execute(alias,install,line->main.post(()->{if(!closed&&progress==wait&&wait.isShowing())text.setText(line);}));}catch(Exception failure){result=new Result("ERROR:SETUP_FAILED",1);}Result answer=result;
            boolean environmentInstall=install&&!alias.equals("agent-migration-tools");if(environmentInstall)storeInstallLog(alias,answer);
            main.post(()->{running=false;if(environmentInstall){installations.remove(alias);recordAttempt(alias,answer.ready());if(!closed)refreshCard(alias);}if(closed)return;spinner.setVisibility(View.GONE);text.setText(answer.ready()?t("Окружение и оболочка найдены. Это не проверка всех пакетов или доступа к моделям.","Environment and shell found. This does not verify every package or model access."):t("Проверка или установка не завершена. Существующие системы не заменялись.\n","Check or installation did not finish. Existing systems were not replaced.\n")+answer.report.substring(Math.max(0,answer.report.length()-12000)));wait.getButton(-1).setEnabled(true);completed.accept(answer);});});
    }
    private Result execute(String alias,boolean install,Consumer<String> update)throws Exception{return execute(alias,install,update,null);}
    private Result execute(String alias,boolean install,Consumer<String> update,InstallJob job)throws Exception{
        if(!WorkspaceLocalFilePicker.validAlias(alias))throw new IOException("INVALID_ALIAS");
        File directory=new File(activity.getFilesDir(),".environment-control");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("SETUP_DIRECTORY");
        File control=directory;
        directory=job==null?executionDirectory(control):job.cancel.getParentFile();
        try{
        File script=new File(directory,"runtime.sh");try(InputStream in=activity.getAssets().open("environment-runtime.sh");OutputStream out=new FileOutputStream(script)){byte[] bytes=new byte[8192];int count;while((count=in.read(bytes))!=-1)out.write(bytes,0,count);}
        if(!script.setReadable(false,false)||!script.setWritable(false,false)||!script.setReadable(true,true)||!script.setWritable(true,true))throw new IOException("SETUP_PERMISSIONS");
        ProcessBuilder builder;
        if(job!=null){
            File supervisor=new File(directory,"environment-install.py");try(InputStream in=activity.getAssets().open("environment-install.py");OutputStream out=new FileOutputStream(supervisor)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            if(!supervisor.setReadable(false,false)||!supervisor.setWritable(false,false)||!supervisor.setReadable(true,true)||!supervisor.setWritable(true,true))throw new IOException("SETUP_PERMISSIONS");
            String bootstrap="if [ ! -x \"$PREFIX/bin/python3\" ];then printf 'STAGE:install-python\\n'; pkg install -y python || exit $?;fi;exec \"$PREFIX/bin/python3\" -B \"$1\" \"$2\" \"$3\" \"$4\"";
            builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c",bootstrap,"environment-install",supervisor.getAbsolutePath(),script.getAbsolutePath(),alias,job.cancel.getAbsolutePath());
        }else builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc",script.getAbsolutePath(),install?"install":"check",alias);
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}builder.environment().put("PREFIX",TermuxConstants.TERMUX_PREFIX_DIR_PATH);builder.environment().put("PD_FORCE_NO_COLORS","1");builder.environment().put("JOBS_ENVIRONMENT_DOWNLOAD_DIR",control.getCanonicalPath());builder.environment().put("JOBS_ENVIRONMENT_RETRY",job!=null&&job.resuming?"1":"0");builder.redirectErrorStream(true);
        Process process=builder.start();return collectProcess(process,update,job==null?null:job.cancel);
        }finally{if(job==null)cleanExecution(directory,null);}
    }
    static File executionDirectory(File control)throws IOException{
        return java.nio.file.Files.createTempDirectory(control.toPath(),"job-").toFile();
    }
    static void cleanExecution(File directory,File cancel){
        // Only fixed files from this invocation; never recurse into downloads/rootfs.
        if(cancel!=null)cancel.delete();
        new File(directory,"runtime.sh").delete();new File(directory,"environment-install.py").delete();directory.delete();
    }
    static Result collectProcess(Process process,Consumer<String> update,File cancel)throws Exception{
        boolean exited=false,interrupted=false;
        try{
            process.getOutputStream().close();
            EnvironmentInstallLog.Capture report=new EnvironmentInstallLog.Capture();char[] bytes=new char[1024];long last=0;
            try(Reader reader=new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8)){
                int count;while((count=reader.read(bytes))!=-1){report.append(bytes,count);long now=android.os.SystemClock.elapsedRealtime();if(now-last>200){last=now;update.accept(EnvironmentInstallLog.clean(report.recent()));}}
            }
            update.accept(EnvironmentInstallLog.clean(report.recent()));
            int code=process.waitFor();exited=true;
            return new Result(EnvironmentInstallLog.clean(report.snapshot()),code);
        }catch(InterruptedException error){interrupted=true;throw error;}
        finally{
            if(!exited){
                // Only this job's private marker requests group cleanup. Never signal
                // a PID taken from output, another session or a package-manager lock.
                if(cancel!=null)try{cancel.createNewFile();}catch(IOException ignored){}
                try{process.getInputStream().close();}catch(Exception ignored){}
                try{process.getErrorStream().close();}catch(Exception ignored){}
                try{process.getOutputStream().close();}catch(Exception ignored){}
                // Retain the operation slot until the supervisor (or prerequisite
                // preparation) really exits, even if output/callbacks/waits failed.
                while(true){try{process.waitFor();break;}catch(InterruptedException error){interrupted=true;}}
            }
            if(interrupted)Thread.currentThread().interrupt();
        }
    }
    public void close(){closed=true;cardUpdates.clear();if(progress!=null)progress.dismiss();worker.shutdown();}
}
