package com.termux.app.workspace;

import android.app.Activity;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;

/** Inline inventory status; only the spinner and one status line change during the scan. */
final class WorkspaceBackupScanHeader {
    final LinearLayout view;
    final TextView refresh,status;
    final ProgressBar spinner;
    boolean running,closed,ready=true;
    private WorkspaceBackupEngine engine;
    WorkspaceBackupScanHeader(Activity activity,String title){
        view=new LinearLayout(activity);view.setOrientation(LinearLayout.VERTICAL);int d=WorkspaceDialogs.dp(activity,16);view.setPadding(d,d,d,0);
        LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=new TextView(activity);name.setText(title);name.setTextSize(18);name.setTextColor(WorkspaceDialogs.TEXT);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout control=new FrameLayout(activity);refresh=WorkspaceDialogs.action(activity,WorkspaceBackupUi.text(activity,"Обновить","Refresh"),0);refresh.setGravity(Gravity.CENTER);control.addView(refresh,new FrameLayout.LayoutParams(-1,-1));
        spinner=new ProgressBar(activity);FrameLayout.LayoutParams spin=new FrameLayout.LayoutParams(WorkspaceDialogs.dp(activity,24),WorkspaceDialogs.dp(activity,24),Gravity.CENTER);control.addView(spinner,spin);spinner.setVisibility(View.GONE);spinner.setContentDescription(WorkspaceBackupUi.text(activity,"Сканирование","Scanning"));
        LinearLayout.LayoutParams button=new LinearLayout.LayoutParams(WorkspaceDialogs.dp(activity,100),WorkspaceDialogs.dp(activity,48));button.setMarginStart(WorkspaceDialogs.dp(activity,8));row.addView(control,button);view.addView(row);
        status=new TextView(activity);status.setTextSize(12);status.setTextColor(WorkspaceDialogs.MUTED);status.setSingleLine(true);status.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);status.setPadding(0,WorkspaceDialogs.dp(activity,6),0,WorkspaceDialogs.dp(activity,8));view.addView(status);status.setVisibility(View.GONE);
    }
    void start(WorkspaceBackupEngine owned,String initial){engine=owned;running=true;refresh.setEnabled(false);refresh.setVisibility(View.INVISIBLE);spinner.setVisibility(View.VISIBLE);status.setText(initial);status.setVisibility(View.VISIBLE);}
    void progress(String stage,JSONObject event){if(closed||!running)return;long count=event.optLong("processed");String path=event.optString("current");status.setText(stage+(count>0?" · "+android.text.format.Formatter.formatFileSize(view.getContext(),count):"")+(path.isEmpty()?"":" · "+path));}
    void finish(String error){running=false;engine=null;refresh.setEnabled(true);refresh.setVisibility(View.VISIBLE);spinner.setVisibility(View.GONE);status.setText(error==null?"":error);status.setVisibility(error==null?View.GONE:View.VISIBLE);}
    void close(){closed=true;WorkspaceBackupEngine owned=engine;engine=null;running=false;if(owned!=null)owned.cancel();spinner.setVisibility(View.GONE);}
}
