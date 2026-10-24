package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.Locale;
import java.util.function.LongSupplier;

/** Bounded, replace-in-place progress UI. The worker owns all cancellation and I/O. */
final class WorkspaceBackupProgress {
    final LinearLayout view;
    final ProgressBar bar;
    final TextView stage,current,amount,elapsed;
    private final Activity activity;
    private final LongSupplier clock;
    private final long started;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean closed,stopping;
    private final Runnable tick=new Runnable(){public void run(){
        if(closed)return;
        if(activity.isFinishing()||activity.isDestroyed()){close();return;}
        updateElapsed();handler.postDelayed(this,1000);
    }};
    WorkspaceBackupProgress(Activity a){this(a,SystemClock::elapsedRealtime);}
    WorkspaceBackupProgress(Activity a,LongSupplier time){
        activity=a;clock=time;started=time.getAsLong();view=new LinearLayout(a);view.setOrientation(LinearLayout.VERTICAL);
        int pad=WorkspaceDialogs.dp(a,20);view.setPadding(pad,pad,pad,pad);
        stage=line(15,WorkspaceDialogs.TEXT);bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setIndeterminate(true);
        ColorStateList tint=ColorStateList.valueOf(WorkspaceDialogs.ACCENT);bar.setProgressTintList(tint);bar.setIndeterminateTintList(tint);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(a,6));params.topMargin=WorkspaceDialogs.dp(a,14);params.bottomMargin=WorkspaceDialogs.dp(a,14);view.addView(bar,params);
        current=line(12,WorkspaceDialogs.MUTED);current.setSingleLine(true);current.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        amount=line(13,WorkspaceDialogs.TEXT);elapsed=line(13,WorkspaceDialogs.MUTED);
        stage.setText(WorkspaceBackupUi.text(a,"Подготовка…","Preparing…"));current.setText(" ");updateElapsed();
    }
    private TextView line(int size,int color){TextView v=new TextView(activity);v.setTextSize(size);v.setTextColor(color);view.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    void start(){if(!closed){handler.removeCallbacks(tick);tick.run();}}
    void close(){closed=true;handler.removeCallbacks(tick);}
    boolean isStopping(){return stopping;}
    /** Bind after show: cancellation requests worker shutdown, it does not dismiss completion UI. */
    void bindCancellation(AlertDialog dialog,Runnable cancel){
        android.widget.Button button=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if(button==null)return;
        button.setOnClickListener(v->{
            if(stopping||closed)return;
            stopping=true;button.setEnabled(false);
            button.setText(WorkspaceBackupUi.text(activity,"Отменяется…","Cancelling…"));
            stage.setText(WorkspaceBackupUi.text(activity,"Остановка операции…","Stopping operation…"));
            current.setText(" ");amount.setText(WorkspaceBackupUi.text(activity,"Дождитесь завершения процесса","Waiting for the process to exit"));bar.setIndeterminate(true);
            cancel.run();
        });
    }
    void update(String label,JSONObject event){
        if(closed||stopping)return;stage.setText(label);
        String filename=event.optString("current","");current.setText(filename.isEmpty()?" ":filename);
        long done=Math.max(0,event.optLong("processed")),total=Math.max(0,event.optLong("total"));
        if(total>0){
            int percent=(int)(Math.min(done,total)*100d/total);bar.setIndeterminate(false);bar.setProgress(percent);
            amount.setText(percent+"%"+(event.optString("unit","bytes").equals("bytes")?" · "+android.text.format.Formatter.formatFileSize(activity,done)+" / "+android.text.format.Formatter.formatFileSize(activity,total):""));
        }else{
            bar.setIndeterminate(true);amount.setText(done>0?android.text.format.Formatter.formatFileSize(activity,done):WorkspaceBackupUi.text(activity,"Выполняется…","Working…"));
        }
        updateElapsed();
    }
    void updateElapsed(){elapsed.setText(WorkspaceBackupUi.text(activity,"Прошло: ","Elapsed: ")+duration(Math.max(0,clock.getAsLong()-started)));}
    static String duration(long milliseconds){long seconds=Math.max(0,milliseconds)/1000,hours=seconds/3600;return hours>0?String.format(Locale.ROOT,"%d:%02d:%02d",hours,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
}
