package com.termux.app.workspace;

import android.app.Dialog;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import com.termux.app.TermuxActivity;

/** Created only on explicit playback. Owns no service, cache, terminal or global window state. */
final class WorkspaceServerPlayer implements AutoCloseable {
    private final Dialog dialog;private final Runnable stopped;private final PlayerView view;
    private ExoPlayer player;private boolean closed,handedOff;
    static boolean validUrl(String url){try{Uri u=Uri.parse(url);return "http".equals(u.getScheme())&&"127.0.0.1".equals(u.getHost())&&u.getPort()>0&&u.getPort()<=65535&&u.getUserInfo()==null&&u.getQuery()==null&&u.getFragment()==null&&u.getPath()!=null&&u.getPath().matches("/[a-f0-9]{64}");}catch(Exception e){return false;}}
    @androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
    WorkspaceServerPlayer(TermuxActivity activity,String name,String url,Runnable stopped,Runnable external,Runnable download){
        if(!validUrl(url))throw new IllegalArgumentException("INVALID_MEDIA_URL");this.stopped=stopped;
        boolean ru=WorkspaceLanguage.code(activity).equals("ru");
        dialog=new Dialog(activity,android.R.style.Theme_Material_NoActionBar);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);
        TextView title=new TextView(activity);title.setText(name);title.setTextColor(WorkspaceDialogs.TEXT);title.setTextSize(15);title.setPadding(16,12,16,12);root.addView(title);
        view=new PlayerView(activity);root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        TextView error=new TextView(activity);error.setTextSize(12);error.setTextIsSelectable(true);error.setPadding(12,8,12,8);error.setTextColor(WorkspaceDialogs.DANGER);error.setVisibility(View.GONE);root.addView(error);
        LinearLayout actions=new LinearLayout(activity);root.addView(actions);
        Button exit=new Button(activity);exit.setText(ru?"Закрыть":"Close");actions.addView(exit,new LinearLayout.LayoutParams(0,-2,1));exit.setOnClickListener(v->close());
        Button other=new Button(activity);other.setText(ru?"Внешний плеер":"External player");actions.addView(other,new LinearLayout.LayoutParams(0,-2,1));other.setOnClickListener(v->{handedOff=true;close();external.run();});
        Button save=new Button(activity);save.setText(ru?"Скачать":"Download");actions.addView(save,new LinearLayout.LayoutParams(0,-2,1));save.setOnClickListener(v->{close();download.run();});
        dialog.setContentView(root);dialog.setCanceledOnTouchOutside(false);dialog.setOnDismissListener(d->close());
        try{
            // No cross-protocol redirects or software decoder extensions. All networking is loopback SSH.
            DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setAllowCrossProtocolRedirects(false);
            player=new ExoPlayer.Builder(activity).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).build();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
            player.setHandleAudioBecomingNoisy(true);view.setPlayer(player);view.setKeepScreenOn(true);
            player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){error.setText(ServerMediaDiagnostics.describe(e,ru));error.setVisibility(View.VISIBLE);view.setKeepScreenOn(false);}});
            dialog.show();dialog.getWindow().setLayout(-1,-1);player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();
        }catch(RuntimeException e){close();throw e;}
    }
    @Override public void close(){if(closed)return;closed=true;view.setPlayer(null);view.setKeepScreenOn(false);if(player!=null){player.release();player=null;}dialog.dismiss();if(!handedOff)stopped.run();}
}
