package com.termux.app.workspace;
import android.app.*;import android.content.*;import android.os.*;import com.termux.R;import com.termux.app.TermuxActivity;
/** Foreground lifecycle for the owned update job only, never TermuxService. */
public final class EnvironmentUpdateService extends Service {
 private static final int ID=0x4a4f4255;private static final String CHANNEL="environment-updates";
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable refresh=new Runnable(){public void run(){if(EnvironmentUpdateJobs.stopServiceIfIdle(EnvironmentUpdateService.this))return;startForeground(ID,notification());handler.postDelayed(this,1000);}};
 @Override public void onCreate(){super.onCreate();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"Environment updates",NotificationManager.IMPORTANCE_LOW));}
 @Override public int onStartCommand(Intent intent,int flags,int startId){if(intent!=null&&"cancel".equals(intent.getAction()))EnvironmentUpdateJobs.cancel(intent.getStringExtra("job"));if(EnvironmentUpdateJobs.stopServiceIfIdle(this))return START_NOT_STICKY;startForeground(ID,notification());handler.removeCallbacks(refresh);handler.postDelayed(refresh,1000);return START_NOT_STICKY;}
 private Notification notification(){boolean ru=WorkspaceLanguage.code(this).equals("ru");org.json.JSONObject state=EnvironmentUpdateJobs.state();String target=WorkspaceEnvironments.label(state.optString("target"));Intent open=new Intent(this,TermuxActivity.class);PendingIntent show=PendingIntent.getActivity(this,81,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Intent stop=new Intent(this,EnvironmentUpdateService.class).setAction("cancel").setData(android.net.Uri.parse("termux-jobs-update://stop/"+state.optString("id"))).putExtra("job",state.optString("id"));PendingIntent cancel=PendingIntent.getService(this,82,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=new Notification.Builder(this,CHANNEL);return b.setSmallIcon(R.drawable.ic_service_notification).setContentTitle(ru?"Обновление окружения":"Environment update").setContentText(target+" · "+WorkspaceEnvironmentUpdates.phase(this,state.optString("phase"))).setOngoing(true).setContentIntent(show).addAction(new Notification.Action.Builder(null,ru?"Остановить":"Stop",cancel).build()).build();}
 @Override public void onDestroy(){handler.removeCallbacks(refresh);super.onDestroy();}
 @Override public IBinder onBind(Intent intent){return null;}
}
