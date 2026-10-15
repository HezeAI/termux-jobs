package com.termux.app.workspace;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import androidx.core.widget.TextViewCompat;
import com.termux.R;
import org.json.JSONObject;
import java.util.Locale;

/** Three stable views: refreshing data changes only labels and progress values. */
final class ServerMetricCards extends LinearLayout {
    private final TextView[] values=new TextView[3];private final ProgressBar[] bars=new ProgressBar[3];
    private final boolean ru;private final String[] labels;
    ServerMetricCards(Context context){super(context);setOrientation(HORIZONTAL);ru=WorkspaceLanguage.code(context).equals("ru");labels=new String[]{"CPU","RAM",ru?"Диск":"Disk"};int[] icons={R.drawable.workspace_server_cpu,R.drawable.workspace_server_ram,R.drawable.workspace_server_disk};
        for(int i=0;i<3;i++){
            LinearLayout card=new LinearLayout(context);card.setOrientation(VERTICAL);card.setPadding(dp(10),dp(12),dp(10),dp(12));card.setBackground(WorkspaceDialogs.background(context,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i>0)p.setMarginStart(dp(8));addView(card,p);
            LinearLayout heading=new LinearLayout(context);heading.setGravity(Gravity.CENTER_VERTICAL);ImageView icon=new ImageView(context);icon.setImageResource(icons[i]);icon.setColorFilter(WorkspaceDialogs.MUTED);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(18),dp(18));ip.setMarginEnd(dp(6));heading.addView(icon,ip);
            TextView label=new TextView(context);label.setText(labels[i]);label.setTextSize(13);label.setTextColor(WorkspaceDialogs.MUTED);label.setSingleLine(true);TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label,10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);heading.addView(label,new LinearLayout.LayoutParams(0,dp(24),1));card.addView(heading);
            TextView value=new TextView(context);value.setTextSize(14);value.setTypeface(null,Typeface.BOLD);value.setTextColor(WorkspaceDialogs.TEXT);value.setMaxLines(2);value.setGravity(Gravity.CENTER_VERTICAL);TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(value,10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);card.addView(value,new LinearLayout.LayoutParams(-1,dp(42)));values[i]=value;
            ProgressBar bar=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setIndeterminate(false);bar.setProgressTintList(ColorStateList.valueOf(WorkspaceDialogs.ACCENT));bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xff29343b));card.addView(bar,new LinearLayout.LayoutParams(-1,dp(6)));bars[i]=bar;
        }update(null);
    }
    private int dp(int n){return WorkspaceDialogs.dp(getContext(),n);}
    static int progress(double percent){return Double.isFinite(percent)?(int)Math.round(Math.max(0,Math.min(100,percent))*10):0;}
    static String usage(long used,long total,boolean ru){if(used<0||total<=0)return "—";double unit=1;String label=ru?"Б":"B";if(total>=1073741824L){unit=1073741824.0;label=ru?"ГБ":"GB";}else if(total>=1048576){unit=1048576.0;label=ru?"МБ":"MB";}else if(total>=1024){unit=1024.0;label=ru?"КБ":"KB";}return String.format(ru?new Locale("ru"):Locale.US,"%.1f / %.1f %s",used/unit,total/unit,label);}
    void update(JSONObject metrics){
        double cpu=metrics==null?Double.NaN:metrics.optDouble("cpu",Double.NaN);boolean validCpu=Double.isFinite(cpu)&&cpu>=0;
        set(0,validCpu?String.format(ru?new Locale("ru"):Locale.US,"%.0f%%",cpu):"—",validCpu?progress(cpu):0);
        for(int i=1;i<3;i++){String prefix=i==1?"ram":"disk";long total=metrics==null?-1:metrics.optLong(prefix+"Total",-1),used=metrics==null?-1:metrics.optLong(prefix+"Used",-1);set(i,usage(used,total,ru),used>=0&&total>0?progress(100.0*used/total):0);}
    }
    private void set(int index,String value,int progress){TextView view=values[index];if(!value.contentEquals(view.getText())){view.setText(value);getChildAt(index).setContentDescription(labels[index]+" · "+value);}if(bars[index].getProgress()!=progress)bars[index].setProgress(progress,false);}
}
