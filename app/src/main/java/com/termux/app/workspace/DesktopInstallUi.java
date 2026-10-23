package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.Context;
import android.os.SystemClock;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;

/** Shared desktop/program installer UI. Outside taps and Back never cancel owned jobs. */
final class DesktopInstallUi {
    final AlertDialog dialog;
    private final Context context;
    private final ProgressBar bar;
    private final TextView status,remaining,details;
    private final LogScroll scroll;
    private int logRevision;
    private ViewTreeObserver logObserver;
    private ViewTreeObserver.OnPreDrawListener logLayout;
    private boolean reading;
    /** TextView updates must not move the viewport to its old selection/cursor. */
    private final class LogScroll extends ScrollView {
        boolean updating;
        LogScroll(Context c){super(c);}
        @Override public boolean dispatchTouchEvent(MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){reading=true;logRevision++;clearLogLayout();updating=false;}
            else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL)reading=false;
            return super.dispatchTouchEvent(e);
        }
        @Override public void requestChildFocus(View child,View focused){if(!updating)super.requestChildFocus(child,focused);}
        @Override public boolean requestChildRectangleOnScreen(View child,Rect rectangle,boolean immediate){return !updating&&super.requestChildRectangleOnScreen(child,rectangle,immediate);}
    }
    DesktopInstallUi(Context c,String title,Runnable cancel){
        context=c;LinearLayout body=AgentStatusUi.body(c);
        bar=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setIndeterminate(true);bar.setTag("desktop-install-progress");body.addView(bar,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(c,12)));
        status=AgentStatusUi.info(body,"");status.setTag("desktop-install-status");remaining=AgentStatusUi.info(body,"");remaining.setTag("desktop-install-remaining");
        details=new TextView(c);details.setTextSize(12);details.setTextColor(WorkspaceDialogs.MUTED);details.setTextIsSelectable(true);
        scroll=new LogScroll(c);scroll.setTag("desktop-install-log");details.setTag("desktop-install-detail");scroll.addView(details);
        body.addView(scroll,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(c,180)));
        dialog=WorkspaceDialogs.builder(c).setTitle(title).setView(body).setNegativeButton(tr("Отмена","Cancel"),null).create();
        dialog.setCanceledOnTouchOutside(false);dialog.setCancelable(false);dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->{v.setEnabled(false);((TextView)v).setText(tr("Отменяется…","Cancelling…"));cancel.run();});
    }
    void update(String text,String detail,int percent,long eta,long sampleTime,String stage){
        update(text,detail,percent,eta,sampleTime,stage,-1,-1);
    }
    void update(String text,String detail,int percent,long eta,long sampleTime,String stage,long downloaded,long total){
        bar.setIndeterminate(percent<0);if(percent>=0)bar.setProgress(Math.max(0,Math.min(100,percent)));
        String volume=(stage.equals("downloading")||stage.equals("updating"))&&downloaded>=0&&total>0&&downloaded<=total?" ("+bytes(downloaded)+" / "+bytes(total)+")":"";
        status.setText(text+(percent>=0?" · "+percent+"%":"")+volume);updateLog(detail);
        long age=Math.max(0,(SystemClock.elapsedRealtime()-sampleTime)/1000);long left=eta-age;
        String prefix=tr("Осталось на этапе: ","Remaining in this stage: ");
        String stageName=stage.equals("updating")?tr("Списки пакетов","Package lists"):stage.equals("downloading")?tr("Загрузка пакетов","Downloading packages"):stage.equals("installing")?tr("Установка пакетов","Installing packages"):tr("Подготовка","Preparing");
        remaining.setText(stageName+"\n"+prefix+(eta<0||left<=0?tr("рассчитывается","calculating"):"≈ "+duration(left)));
    }
    private String bytes(long n){
        String[] units={tr("Б","B"),tr("КБ","kB"),tr("МБ","MB"),tr("ГБ","GB"),tr("ТБ","TB")};double value=n;int unit=0;
        while(value>=1000&&unit<units.length-1){value/=1000;unit++;}
        return String.format(java.util.Locale.ROOT,value>=100||unit==0?"%.0f %s":"%.1f %s",value,units[unit]);
    }
    private void updateLog(String detail){
        if(detail.contentEquals(details.getText()))return;
        int oldY=scroll.getScrollY();
        boolean tail=!reading&&oldY>=Math.max(0,details.getHeight()-scroll.getHeight())-WorkspaceDialogs.dp(context,24);
        int revision=++logRevision;clearLogLayout();scroll.updating=true;details.setText(detail);
        // A posted runnable may run before Android measures the new log height.
        // Restore/follow only after layout, before this frame is drawn.
        logObserver=scroll.getViewTreeObserver();
        logLayout=()->{clearLogLayout();if(revision==logRevision){scroll.scrollTo(0,tail?Math.max(0,details.getHeight()-scroll.getHeight()):oldY);scroll.updating=false;}return true;};
        logObserver.addOnPreDrawListener(logLayout);
    }
    private void clearLogLayout(){if(logObserver!=null&&logObserver.isAlive()&&logLayout!=null)logObserver.removeOnPreDrawListener(logLayout);logObserver=null;logLayout=null;}
    private String duration(long seconds){return seconds>=3600?(seconds/3600)+tr(" ч "," h ")+((seconds%3600)/60)+tr(" мин"," min"):seconds>=60?(seconds/60)+tr(" мин "," min ")+(seconds%60)+tr(" с"," s"):seconds+tr(" с"," s");}
    void close(){logRevision++;clearLogLayout();scroll.updating=false;dialog.dismiss();}
    private String tr(String ru,String en){return AgentStatusUi.tr(context,ru,en);}
}
