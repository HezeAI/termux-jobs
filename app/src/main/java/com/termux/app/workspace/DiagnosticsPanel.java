package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import java.io.*;

/** Shared stable dialog; refreshing it never starts a network request. */
final class DiagnosticsPanel {
    interface Source {
        default void refresh(){}
        default boolean hasX11(){return false;}default String x11(){return "";}default void prepareX11(Runnable done){done.run();}
        default boolean hasJournalControl(){return true;}
        default int capacity(){return WorkspaceDiagnostics.LIMIT;}
        default String capacitySummary(){return "";}
        default void prepareExport(Runnable action){action.run();}
        default String actionLabel(){return "";}default void action(){}default String pauseLabel(){return "";}default boolean canPause(){return true;}default String privacy(){return "";}
        String title();String summary();String events();String report();String id();int bytes();int dropped();boolean paused();void paused(boolean value)throws Exception;
        default boolean hasMode(){return false;}default boolean extended(){return false;}default void extended(boolean value){}
    }
    static String tr(TermuxActivity a,String ru,String en){return WorkspaceLanguage.code(a).equals("ru")?ru:en;}
    private static int dp(TermuxActivity a,int n){return WorkspaceDialogs.dp(a,n);}
    private static TextView label(TermuxActivity a,String text,int size){TextView v=new TextView(a);v.setText(text);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(size);return v;}
    private static TextView button(TermuxActivity a,String text,Runnable click){TextView v=label(a,text,13);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(a,48));v.setPadding(dp(a,8),dp(a,4),dp(a,8),dp(a,4));v.setBackground(WorkspaceDialogs.background(a,false));v.setTextColor(WorkspaceDialogs.ACCENT);v.setOnClickListener(w->click.run());return v;}
    private static void selected(TermuxActivity a,TextView button,boolean value){if(button.getTag()==null||button.isSelected()!=value){button.setTag(Boolean.TRUE);button.setSelected(value);button.setBackground(WorkspaceDialogs.background(a,value));}}
    static String clipboardText(String report){return report.length()<=96000?report:report.substring(0,1200)+"\n[Clipboard excerpt; save full report to Download]\n"+report.substring(report.length()-94000);}
    static AlertDialog show(TermuxActivity a,Source source){
        Handler main=new Handler(Looper.getMainLooper());boolean[] closed={false},saving={false};int[] tab={0};
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(a,12),0,dp(a,12),dp(a,8));
        LinearLayout tabs=new LinearLayout(a);TextView overview=button(a,tr(a,"Сводка","Overview"),()->{}),events=button(a,tr(a,"События","Events"),()->{});tabs.addView(overview,new LinearLayout.LayoutParams(0,dp(a,44),1));LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(a,44),1);right.setMarginStart(dp(a,8));tabs.addView(events,right);body.addView(tabs);
        if(!source.actionLabel().isEmpty())body.addView(button(a,source.actionLabel(),source::action));
        LinearLayout modes=new LinearLayout(a);TextView normal=button(a,tr(a,"Обычный","Normal"),()->{}),detail=button(a,tr(a,"Подробный","Detailed"),()->{}),x11=button(a,"X11",()->{});normal.setTag("diagnostics-normal");detail.setTag("diagnostics-detailed");x11.setTag("diagnostics-x11");
        LinearLayout modePair=new LinearLayout(a);modePair.addView(normal,new LinearLayout.LayoutParams(0,dp(a,44),1));modePair.addView(detail,new LinearLayout.LayoutParams(0,dp(a,44),1));modes.addView(modePair,new LinearLayout.LayoutParams(0,dp(a,44),1));
        if(source.hasX11()){normal.setTextSize(11);detail.setTextSize(11);normal.setPadding(0,0,0,0);detail.setPadding(0,0,0,0);LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(0,dp(a,44),1);xp.setMarginStart(dp(a,8));modes.addView(x11,xp);}if(source.hasMode())body.addView(modes);
        ScrollView scroll=new ScrollView(a);LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(a,10),0,dp(a,10));TextView summary=label(a,"",14);summary.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12));summary.setBackground(WorkspaceDialogs.background(a,false));content.addView(summary);TextView lines=label(a,"",12);lines.setTextIsSelectable(true);lines.setPadding(dp(a,6),dp(a,12),dp(a,6),dp(a,12));content.addView(lines);scroll.addView(content);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView capacity=label(a,"",11);body.addView(capacity);ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(source.capacity());body.addView(progress,new LinearLayout.LayoutParams(-1,dp(a,6)));
        TextView status=label(a,"",11);body.addView(status);
        TextView pause=button(a,"",()->{}),copy=button(a,tr(a,"Скопировать отчёт","Copy report"),()->{}),path=button(a,tr(a,"Скопировать путь","Copy path"),()->{}),download=button(a,tr(a,"В Download","To Download"),()->{});
        pause.setVisibility(source.hasJournalControl()?View.VISIBLE:View.GONE);download.setTag("diagnostics-download");path.setTag("diagnostics-copy-path");copy.setTag("diagnostics-copy");
        TextView[] iconButtons={pause,copy,path,download};int[] icons={R.drawable.workspace_diag_pause,R.drawable.workspace_copy,R.drawable.workspace_folder,R.drawable.workspace_diag_download};for(int index=0;index<icons.length;index++){android.graphics.drawable.Drawable icon=a.getDrawable(icons[index]).mutate();icon.setTint(WorkspaceDialogs.ACCENT);icon.setBounds(0,0,dp(a,18),dp(a,18));iconButtons[index].setCompoundDrawables(icon,null,null,null);iconButtons[index].setCompoundDrawablePadding(dp(a,6));}
        for(TextView[] pair:new TextView[][]{{pause,copy},{path,download}}){LinearLayout row=new LinearLayout(a);LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.topMargin=dp(a,6);body.addView(row,rowParams);row.addView(pair[0],new LinearLayout.LayoutParams(0,dp(a,48),1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(a,48),1);p.setMarginStart(dp(a,8));row.addView(pair[1],p);}
        TextView privacy=label(a,source.privacy().isEmpty()?tr(a,"Без команд, переписки, адресов, путей и секретов. Журнал в памяти; экспорт сохраняет отчёт.","No commands, chats, addresses, paths or secrets. In-memory journal; export saves a report."):source.privacy(),11);privacy.setTextColor(WorkspaceDialogs.MUTED);privacy.setPadding(0,dp(a,6),0,0);body.addView(privacy);
        FrameLayout holder=new FrameLayout(a);holder.addView(body,new FrameLayout.LayoutParams(-1,(int)(a.getResources().getDisplayMetrics().heightPixels*.67)));
        AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(source.title()).setView(holder).setPositiveButton(WorkspaceText.tr("Закрыть"),null).create();dialog.setCanceledOnTouchOutside(false);
        Runnable refresh=()->{if(closed[0])return;try{
            source.refresh();String info=tab[0]==2?source.x11():source.summary();if(!info.contentEquals(summary.getText()))summary.setText(info);summary.setVisibility(tab[0]!=1?View.VISIBLE:View.GONE);lines.setVisibility(tab[0]==2?View.GONE:View.VISIBLE);normal.setEnabled(tab[0]!=2);detail.setEnabled(tab[0]!=2);normal.setAlpha(tab[0]==2?.4f:1f);detail.setAlpha(tab[0]==2?.4f:1f);
            String text=source.events();if(tab[0]==0){String[] entries=text.split("\n");StringBuilder recent=new StringBuilder();for(int i=0;i<Math.min(8,entries.length);i++)recent.append(entries[i]).append('\n');text=recent.toString();}if(!text.contentEquals(lines.getText())){int y=scroll.getScrollY();lines.setText(text);scroll.post(()->{if(!closed[0])scroll.scrollTo(0,y);});}
            pause.setEnabled(source.canPause());pause.setText(source.pauseLabel().isEmpty()?tr(a,source.paused()?"Возобновить журнал":"Остановить журнал",source.paused()?"Resume journal":"Stop journal"):source.pauseLabel());if(!java.util.Objects.equals(pause.getTag(),source.paused())){pause.setTag(source.paused());android.graphics.drawable.Drawable icon=a.getDrawable(source.paused()?R.drawable.workspace_play:R.drawable.workspace_diag_pause).mutate();icon.setTint(WorkspaceDialogs.ACCENT);icon.setBounds(0,0,dp(a,18),dp(a,18));pause.setCompoundDrawables(icon,null,null,null);}capacity.setText(source.capacitySummary().isEmpty()?ServerFileInfo.size(source.bytes(),WorkspaceLanguage.code(a).equals("ru"))+" / 2 MiB · "+tr(a,"удалено старых: ","old entries dropped: ")+source.dropped():source.capacitySummary());progress.setProgress(source.bytes());
            selected(a,x11,tab[0]==2);selected(a,overview,tab[0]==0);selected(a,events,tab[0]==1);selected(a,normal,!source.extended());selected(a,detail,source.extended());
        }catch(Exception e){status.setText(tr(a,"Не удалось обновить диагностику","Could not refresh diagnostics"));}};
        x11.setOnClickListener(v->{tab[0]=2;refresh.run();source.prepareX11(()->{if(!closed[0])refresh.run();});});
        overview.setOnClickListener(v->{tab[0]=0;refresh.run();});events.setOnClickListener(v->{tab[0]=1;refresh.run();});normal.setOnClickListener(v->{if(tab[0]==2)return;source.extended(false);refresh.run();});detail.setOnClickListener(v->{if(tab[0]==2)return;source.extended(true);refresh.run();status.setText(tr(a,"Собираются новые события; прошлые не восстанавливаются. Нативный verbose — только при запуске.","New events only; earlier events cannot be recovered. Native verbose requires launch flags."));});
        pause.setOnClickListener(v->{try{source.paused(!source.paused());status.setText("");refresh.run();}catch(Exception error){status.setText(tr(a,"Не удалось сохранить выбор; состояние не изменилось","Could not save choice; state unchanged"));}});
        copy.setOnClickListener(v->{try{String full=source.report();((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("diagnostics",clipboardText(full)));status.setText(full.length()>96000?tr(a,"Скопирована выдержка. Полный отчёт — в Download.","Excerpt copied. Save full report to Download."):tr(a,"Отчёт скопирован","Report copied"));}catch(Exception e){status.setText(tr(a,"Не удалось скопировать; сохраните в Download","Could not copy; save to Download"));}});
        java.util.function.Consumer<Boolean> export=toDownload->{if(saving[0])return;saving[0]=true;String report=source.report();WorkspaceDiagnostics.save(a.getFilesDir(),source.id(),report,file->{File dest=null;String saved=file.getAbsolutePath();Exception failure=null;try{if(toDownload){File folder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException();dest=File.createTempFile("Termux-Jobs-diagnostics-",".txt",folder);try(InputStream in=new FileInputStream(file);OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}saved=dest.getAbsolutePath();}}catch(Exception e){failure=e;if(dest!=null)dest.delete();}String result=saved;Exception error=failure;main.post(()->{saving[0]=false;if(closed[0])return;if(error!=null){status.setText(tr(a,"Download недоступен; проверьте разрешение хранения","Download unavailable; check storage permission"));return;}if(!toDownload)((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("diagnostics",result));status.setText(toDownload?tr(a,"Сохранено: ","Saved: ")+result:tr(a,"Путь скопирован","Path copied"));});},error->main.post(()->{saving[0]=false;if(!closed[0])status.setText(tr(a,"Не удалось сохранить отчёт","Could not save report"));}));};
        path.setOnClickListener(v->export.accept(false));download.setOnClickListener(v->source.prepareExport(()->{if(!closed[0])export.accept(true);}));
        Runnable tick=new Runnable(){public void run(){if(closed[0])return;if(a.isFinishing()||a.isDestroyed()||!dialog.isShowing()){dialog.dismiss();return;}refresh.run();main.postDelayed(this,2000);}};dialog.setOnDismissListener(v->{closed[0]=true;main.removeCallbacksAndMessages(null);});dialog.setOnShowListener(v->tick.run());refresh.run();dialog.show();return dialog;
    }
}
