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
    private final java.util.function.Supplier<List<WorkspaceEnvironments.Choice>> inventory;
    EnvironmentSetup(TermuxActivity a){this(a,WorkspaceEnvironments::choices);}
    EnvironmentSetup(TermuxActivity a,java.util.function.Supplier<List<WorkspaceEnvironments.Choice>> choices){activity=a;inventory=choices;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void choose(Consumer<WorkspaceEnvironments.Choice> selected){
        List<WorkspaceEnvironments.Choice> choices=inventory.get();
        ArrayAdapter<WorkspaceEnvironments.Choice> rows=new ArrayAdapter<WorkspaceEnvironments.Choice>(activity,0,choices){
            @Override public View getView(int position,View recycled,ViewGroup parent){
                WorkspaceEnvironments.Choice choice=getItem(position);
                int gap=WorkspaceDialogs.dp(activity,8);
                FrameLayout outer=new FrameLayout(activity);outer.setPadding(gap*2,gap/2,gap*2,gap/2);
                LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(gap*2,gap,gap,gap);row.setBackground(WorkspaceDialogs.background(activity,false));
                TextView label=new TextView(activity);label.setText(choice.label);label.setTextSize(15);label.setTextColor(WorkspaceDialogs.TEXT);label.setMaxLines(2);
                row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
                TextView badge=WorkspaceDialogs.action(activity,choice.installed?t("Установлено","Installed"):t("Установить","Install"),0);
                badge.setTextSize(12);badge.setGravity(Gravity.CENTER);badge.setMinHeight(WorkspaceDialogs.dp(activity,44));badge.setPadding(gap,gap,gap,gap);
                badge.setTextColor(choice.installed?WorkspaceDialogs.MUTED:WorkspaceDialogs.ACCENT);badge.setEnabled(!choice.installed);badge.setAlpha(choice.installed?.6f:1f);
                LinearLayout.LayoutParams badgeParams=new LinearLayout.LayoutParams(-2,-2);badgeParams.leftMargin=gap;row.addView(badge,badgeParams);
                outer.addView(row,new FrameLayout.LayoutParams(-1,-2));return outer;
            }
        };
        WorkspaceDialogs.builder(activity).setTitle(t("Окружение","Environment")).setAdapter(rows,(dialog,index)->{
            WorkspaceEnvironments.Choice choice=choices.get(index);
            if(choice.installed){selected.accept(choice);return;}
            WorkspaceDialogs.builder(activity).setTitle(t("Установить ","Install ")+choice.label+"?")
                .setMessage(t("Система будет скачана и установлена отдельно в Termux. Нужны интернет и свободное место. Если proot-distro отсутствует, установим и его. Уже установленные системы не заменяются. Поддержка зависит от версии proot-distro и архитектуры; недоступный образ будет отклонён.","Download and install a separate system in Termux. Internet and free storage are required. Missing proot-distro will also be installed. Existing systems are never replaced. Availability depends on proot-distro and architecture; unavailable images are rejected.")+(choice.alias.equals("archlinux")?t("\nНа ARM64 устанавливается Arch Linux ARM из архива Termux, с проверкой SHA-256. Это отдельный проект от Arch Linux для x86-64.","\nOn ARM64, install Arch Linux ARM from a Termux archive with SHA-256 verification. It is a separate project from x86-64 Arch Linux."):""))
                .setPositiveButton(t("Установить","Install"),(d,w)->run(choice.alias,true,result->{if(result.ready())selected.accept(new WorkspaceEnvironments.Choice(choice.environment,choice.alias,choice.label,"/root",true));}))
                .setNegativeButton(t("Назад","Back"),null).show();
        }).setNegativeButton(t("Назад","Back"),null).show();
    }
    void check(String environment,Consumer<Result> completed){String alias=environment.equals("termux")?"termux":environment.equals("ubuntu")?"ubuntu":environment.startsWith("proot:")?environment.substring(6):"";if(!alias.isEmpty())run(alias,false,completed);}
    private void run(String alias,boolean install,Consumer<Result> completed){
        if(closed||running)return;running=true;
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);int pad=Math.round(16*activity.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
        ProgressBar spinner=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);spinner.setIndeterminate(true);box.addView(spinner);
        TextView text=new TextView(activity);text.setTextColor(0xffd5e4e0);text.setTextSize(13);text.setText(t("Проверка компонентов…","Checking components…"));box.addView(text);
        AlertDialog wait=WorkspaceDialogs.builder(activity).setTitle((install?t("Установка · ","Installing · "):t("Проверка среды запуска · ","Launch environment check · "))+WorkspaceEnvironments.label(alias)).setView(box).setPositiveButton(t("Назад","Back"),null).create();progress=wait;wait.setCancelable(false);wait.setCanceledOnTouchOutside(false);wait.show();wait.getButton(-1).setEnabled(false);
        worker.execute(()->{Result result;try{result=execute(alias,install,line->main.post(()->{if(!closed&&progress==wait&&wait.isShowing())text.setText(line);}));}catch(Exception failure){result=new Result("ERROR:SETUP_FAILED",1);}Result answer=result;main.post(()->{running=false;if(closed)return;spinner.setVisibility(View.GONE);text.setText(answer.ready()?t("Окружение и оболочка найдены. Это не проверка всех пакетов или доступа к моделям.","Environment and shell found. This does not verify every package or model access."):t("Проверка или установка не завершена. Существующие системы не заменялись.\n","Check or installation did not finish. Existing systems were not replaced.\n")+answer.report);wait.getButton(-1).setEnabled(true);completed.accept(answer);});});
    }
    private Result execute(String alias,boolean install,Consumer<String> update)throws Exception{
        if(!WorkspaceLocalFilePicker.validAlias(alias))throw new IOException("INVALID_ALIAS");
        File directory=new File(activity.getFilesDir(),".environment-control");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("SETUP_DIRECTORY");
        File script=new File(directory,"runtime.sh");try(InputStream in=activity.getAssets().open("environment-runtime.sh");OutputStream out=new FileOutputStream(script)){byte[] bytes=new byte[8192];int count;while((count=in.read(bytes))!=-1)out.write(bytes,0,count);}
        if(!script.setReadable(false,false)||!script.setWritable(false,false)||!script.setReadable(true,true)||!script.setWritable(true,true))throw new IOException("SETUP_PERMISSIONS");
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc",script.getAbsolutePath(),install?"install":"check",alias);builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}builder.environment().put("PREFIX",TermuxConstants.TERMUX_PREFIX_DIR_PATH);builder.environment().put("PD_FORCE_NO_COLORS","1");builder.environment().put("JOBS_ENVIRONMENT_DOWNLOAD_DIR",directory.getAbsolutePath());builder.redirectErrorStream(true);
        Process process=builder.start();process.getOutputStream().close();StringBuilder report=new StringBuilder();char[] bytes=new char[1024];long last=0;
        try(Reader reader=new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8)){int count;while((count=reader.read(bytes))!=-1){report.append(bytes,0,count);if(report.length()>12000)report.delete(0,report.length()-12000);long now=android.os.SystemClock.elapsedRealtime();if(now-last>200){last=now;update.accept(report.toString().replaceAll("\\x1B\\[[0-?]*[ -/]*[@-~]","").replace('\r','\n'));}}}
        return new Result(report.toString().replaceAll("\\x1B\\[[0-?]*[ -/]*[@-~]",""),process.waitFor());
    }
    public void close(){closed=true;if(progress!=null)progress.dismiss();worker.shutdown();}
}
