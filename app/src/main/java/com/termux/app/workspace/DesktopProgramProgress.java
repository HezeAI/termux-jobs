package com.termux.app.workspace;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import java.util.Locale;

/** Updates only the selected program card; closing its UI never owns/cancels the job. */
final class DesktopProgramProgress {
    final LinearLayout view;
    final ProgressBar bar;
    final TextView remaining,file;
    DesktopProgramProgress(Context context){
        view=new LinearLayout(context);view.setOrientation(LinearLayout.VERTICAL);view.setVisibility(View.GONE);
        view.setTag("desktop-program-progress");
        bar=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);
        view.addView(bar,new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(context,10)));
        remaining=AgentStatusUi.info(view,"");remaining.setTextSize(11);remaining.setTag("desktop-program-remaining");
        file=AgentStatusUi.info(view,"");file.setTextSize(11);file.setSingleLine(true);file.setEllipsize(TextUtils.TruncateAt.MARQUEE);file.setMarqueeRepeatLimit(-1);file.setTag("desktop-program-file");
    }
    void update(Bundle state,boolean visible){
        view.setVisibility(visible?View.VISIBLE:View.GONE);file.setSelected(visible);if(!visible)return;
        int percent=state.getInt("percent",-1);bar.setIndeterminate(percent<0);if(percent>=0)bar.setProgress(Math.min(100,Math.max(0,percent)));
        Context c=view.getContext();boolean ru=WorkspaceLanguage.code(c).equals("ru");
        long eta=state.getLong("eta",-1),sample=state.getLong("sampleTime",0),left=eta-Math.max(0,(SystemClock.elapsedRealtime()-sample)/1000);
        String text=(ru?"Осталось: ":"Remaining: ")+(eta<0||left<=0?(ru?"рассчитывается":"calculating"):"≈ "+(left>=60?left/60+(ru?" мин ":" min "):"")+left%60+(ru?" с":" s"));
        long done=state.getLong("downloadBytes",-1),total=state.getLong("downloadTotal",-1);
        String stage=state.getString("stage","");
        if(!state.getString("item","").isEmpty())text=(stage.equals("installing")?(ru?"Установка пакетов":"Installing packages"):(ru?"Загрузка пакетов":"Downloading packages"))+"\n"+text;
        if((stage.equals("downloading")||stage.equals("updating"))&&done>=0&&total>0&&done<=total)
            text+=" · "+bytes(done)+" / "+bytes(total);
        set(remaining,text);set(file,state.getString("item","").isEmpty()?lastLine(state.getString("detail","")):state.getString("item",""));
    }
    static String lastLine(String detail){
        String[] lines=detail.replaceAll("\u001B\\[[0-9;?]*[ -/]*[@-~]","").split("[\\r\\n]");
        for(int i=lines.length-1;i>=0;i--){String line=lines[i].trim();if(line.startsWith("pmstatus:")||line.startsWith("dlstatus:")){String[] parts=line.split(":",4);if(parts.length==4&&!parts[3].trim().isEmpty())return parts[3].trim();continue;}
            if(!line.isEmpty()&&!spinner(line)&&!line.startsWith("JOBS_")&&!line.matches("^\\[.*\\] Bytes:.*")&&!line.startsWith("PROGRAM:")&&!line.startsWith("READY:"))return line;}
        return "";
    }
    private static boolean spinner(String line){return line.chars().allMatch(c->c=='.'||c=='\\'||c=='|'||c=='/'||c=='-'||c=='='||c=='['||c==']'||c=='—'||c=='−'||c=='–'||Character.isWhitespace(c));}
    private static String bytes(long n){double v=n;String[] units={"B","kB","MB","GB","TB"};int i=0;while(v>=1000&&i<4){v/=1000;i++;}return String.format(Locale.ROOT,i==0?"%.0f %s":"%.1f %s",v,units[i]);}
    private static void set(TextView v,String s){if(!s.contentEquals(v.getText()))v.setText(s);}
}
