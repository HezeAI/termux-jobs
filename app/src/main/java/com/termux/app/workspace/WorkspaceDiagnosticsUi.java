package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.graphics.Typeface;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalSession;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/** Read-only view; collection is independent from this dialog's lifecycle. */
final class WorkspaceDiagnosticsUi {
    private WorkspaceDiagnosticsUi() {}
    static void show(TermuxActivity activity,WorkspaceStore.Session profile,Supplier<TerminalSession> terminal){
        Handler handler=new Handler(Looper.getMainLooper());
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(24,12,24,12);
        TextView notice=new TextView(activity);notice.setText(WorkspaceText.tr("Журнал обновляется автоматически и собирается при закрытом окне. До 2 МБ на сессию. Отчёт содержит события и коды ошибок, без текста диалога и секретов."));body.addView(notice);
        Button pause=new Button(activity);body.addView(pause);
        Button copy=new Button(activity);copy.setText(WorkspaceText.tr("Скопировать путь к отчёту"));body.addView(copy);
        Button download=new Button(activity);download.setText(WorkspaceText.tr("Сохранить в Download"));body.addView(download);
        TextView status=new TextView(activity);body.addView(status);
        ScrollView scroll=new ScrollView(activity);scroll.setMinimumHeight((int)(activity.getResources().getDisplayMetrics().heightPixels*.35));TextView output=new TextView(activity);output.setTypeface(Typeface.MONOSPACE);output.setTextSize(11);output.setTextIsSelectable(true);scroll.addView(output);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        int height=(int)(activity.getResources().getDisplayMetrics().heightPixels*.65);body.setMinimumHeight(height);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Диагностика сессии"))
            .setView(body).setPositiveButton(WorkspaceText.tr("Закрыть"),null).create();
        boolean[] dismissed={false},busy={false};
        Runnable refresh=()->{
            pause.setText(WorkspaceText.tr(WorkspaceDiagnostics.paused(profile.id)?"Продолжить журнал":"Остановить журнал"));
            TerminalSession live=terminal.get();if(live!=null)WorkspaceDiagnostics.onOutput(live);
            String report=WorkspaceDiagnostics.report(profile,live).replaceFirst("Generated: [^\n]*\n", "");
            // Bound UI layout work even when the full saved report is much larger.
            String visible=report.length()>65536?report.substring(0,Math.min(1600,report.length()))+"\n[…]\n"+report.substring(report.length()-62000):report;
            if(!visible.contentEquals(output.getText()))output.setText(visible);
        };
        Runnable poll=new Runnable(){public void run(){if(dismissed[0])return;refresh.run();handler.postDelayed(this,2000);}};
        pause.setOnClickListener(v->{WorkspaceDiagnostics.pause(profile.id,!WorkspaceDiagnostics.paused(profile.id));refresh.run();});
        dialog.setOnShowListener(v->{
            copy.setOnClickListener(w->{
                if(busy[0])return;busy[0]=true;
                WorkspaceDiagnostics.save(activity.getFilesDir(),profile.id,WorkspaceDiagnostics.report(profile,terminal.get()),file->activity.runOnUiThread(()->{busy[0]=false;if(dismissed[0])return;((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("diagnostics",file.getAbsolutePath()));status.setText(WorkspaceText.tr("Путь скопирован"));}),error->activity.runOnUiThread(()->{busy[0]=false;if(!dismissed[0])status.setText(WorkspaceText.tr("Не удалось сохранить отчёт"));}));
            });
            download.setOnClickListener(w->{
                if(busy[0])return;busy[0]=true;
                WorkspaceDiagnostics.save(activity.getFilesDir(),profile.id,WorkspaceDiagnostics.report(profile,terminal.get()),file->{
                    File dest=null;
                    try{
                        File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Download unavailable");
                        dest=File.createTempFile("Termux-Jobs-diagnostics-",".txt",dir);
                        try(InputStream in=new FileInputStream(file);OutputStream out=new FileOutputStream(dest)){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
                        final File saved=dest;
                        activity.runOnUiThread(()->{busy[0]=false;if(!dismissed[0])status.setText(WorkspaceText.tr("Сохранено: ")+saved.getAbsolutePath());});
                    }catch(Exception error){if(dest!=null)dest.delete();activity.runOnUiThread(()->{busy[0]=false;if(!dismissed[0])status.setText(WorkspaceText.tr("Download недоступен; проверьте разрешение хранения"));});}
                },error->activity.runOnUiThread(()->{busy[0]=false;if(!dismissed[0])status.setText(WorkspaceText.tr("Не удалось сохранить отчёт"));}));
            });
            poll.run();
        });
        dialog.setOnDismissListener(v->{dismissed[0]=true;handler.removeCallbacks(poll);});dialog.show();
    }
}
