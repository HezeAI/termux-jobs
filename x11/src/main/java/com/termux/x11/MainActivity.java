package com.termux.x11;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.Keep;

/** Private embedded screen; returning to Termux hides it without touching user sessions. */
@Keep public final class MainActivity extends Activity {
    // Required by the pinned core registration; freeform launching is unused here.
    @Keep private static native void nativeSetLaunchWindowingModeFreeform(Object activityOptions);
    private LorieView display;
    private TextView status;
    private X11Service service;
    private boolean bound,stopped;
    private final Runnable refresh=this::refresh;
    private final ServiceConnection connection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){service=((X11Service.LocalBinder)binder).service();service.addListener(refresh);}
        @Override public void onServiceDisconnected(ComponentName name){service=null;status.setText(tr("X11 остановлен. Вернитесь в терминал и откройте снова.","X11 stopped. Return to the terminal and open it again."));}
    };
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setBackgroundColor(Color.BLACK);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);body.addView(bar);
        TextView title=new TextView(this);title.setText("X11  ▾");title.setPadding(dp(16),0,dp(16),0);title.setOnClickListener(v->showMenu());title.setTextSize(16);title.setTextColor(0xff79dfcd);title.setGravity(Gravity.CENTER);bar.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        button(bar,tr("Клавиатура","Keyboard"),()->{display.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(display,InputMethodManager.SHOW_IMPLICIT);});
        status=new TextView(this);status.setTextSize(12);status.setTextColor(0xffb1c6cc);status.setPadding(dp(12),dp(5),dp(12),dp(5));body.addView(status);
        TextView help=new TextView(this);help.setText(tr("Этап 1: встроенный X11. Цветные панели — настоящее X11-окно. Нажмите на цветные панели и вводите текст: появятся счётчики событий. Ubuntu/XFCE подключим следующим этапом.","Stage 1: embedded X11. The coloured panels are a real X11 window. Tap the coloured panels and type to test the event counters. Ubuntu/XFCE comes next."));help.setTextColor(0xff8a9ea7);help.setTextSize(12);help.setPadding(dp(12),dp(2),dp(12),dp(8));body.addView(help);
        display=new LorieView(this);body.addView(display,new LinearLayout.LayoutParams(-1,0,1));setContentView(body);display.requestFocus();
        try{Intent start=new Intent(this,X11Service.class);startForegroundService(start);bound=bindService(start,connection,Context.BIND_AUTO_CREATE);}catch(Exception e){status.setText(tr("Не удалось запустить X11. Вернитесь в терминал и повторите.","Could not start X11. Return to the terminal and retry."));}
    }
    @Keep public void clientConnectedStateChanged(){runOnUiThread(this::refresh);}
    private void refresh(){
        if(service==null||stopped||isFinishing()||isDestroyed())return;
        status.setText(service.status()+"\n"+tr("Клавиши: ","Keys: ")+service.keys()+tr(" · нажатия: "," · clicks: ")+service.clicks()+tr(" · движения: "," · moves: ")+service.motions());
        if(!display.isConnected())try{ParcelFileDescriptor fd=service.displayConnection();if(fd!=null)display.connectFileDescriptor(fd.detachFd());}catch(Exception e){status.setText(tr("Не удалось подключить экран X11.","Could not connect the X11 display."));}
    }
    private void showMenu(){new AlertDialog.Builder(this).setTitle(tr("Рабочий стол X11","X11 desktop"))
        .setItems(new String[]{tr("Вернуться в терминал","Return to terminal"),tr("Остановить графический рабочий стол","Stop graphical desktop")},(d,w)->{if(w==0)finish();else confirmStop();}).show();}
    private void confirmStop(){new AlertDialog.Builder(this).setTitle(tr("Остановить графический рабочий стол?","Stop graphical desktop?"))
        .setMessage(tr("Будет завершена только проверочная графическая сессия. Обычные терминалы и AI Agents продолжат работу.","Only the graphical test session will stop. Ordinary terminals and AI Agents keep running."))
        .setNegativeButton(tr("Отмена","Cancel"),null).setPositiveButton(tr("Остановить","Stop"),(d,w)->{stopped=true;display.release();finish();if(service!=null)service.requestStop();else stopService(new Intent(this,X11Service.class));}).show();}
    private void button(LinearLayout bar,String label,Runnable action){Button button=new Button(this);button.setAllCaps(false);button.setText(label);button.setTextColor(0xff79dfcd);button.setTextSize(12);button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(dp(8),0,dp(8),0);bar.addView(button,new LinearLayout.LayoutParams(-2,dp(48)));button.setOnClickListener(v->action.run());}
    private String tr(String ru,String en){return X11Text.tr(this,ru,en);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onDestroy(){if(service!=null)service.removeListener(refresh);if(display!=null)display.release();if(bound){unbindService(connection);bound=false;}super.onDestroy();}
}
