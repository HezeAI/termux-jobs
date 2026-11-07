package com.termux.app.workspace;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.termux.R;
/** Presentation only; stop acknowledgement and process ownership remain with WorkspaceDesktop. */
final class DesktopStopAction {
    private final TextView row;
    private boolean pending;
    private Drawable spinner;
    DesktopStopAction(TextView row){this.row=row;row.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){animate(pending);}public void onViewDetachedFromWindow(View v){animate(false);}});}
    void update(boolean waiting,boolean present,boolean allowed,int owners){
        row.setVisibility(waiting||present?View.VISIBLE:View.GONE);row.setEnabled(!waiting&&allowed);row.setAlpha(1);
        row.setTextColor(waiting?WorkspaceDialogs.MUTED:WorkspaceDialogs.TEXT);
        row.setText(waiting?t("Остановка рабочего стола…","Stopping desktop…"):t("Остановить рабочий стол","Stop desktop")+(owners>1?"\n"+t("Общий рабочий стол · сессий: ","Shared desktop · sessions: ")+owners+"\n"+t("Остановка недоступна: есть другие владельцы","Cannot stop while other sessions use it"):""));
        if(spinner==null||pending!=waiting){animate(false);Drawable icon;
            if(waiting){spinner=new ProgressBar(row.getContext(),null,android.R.attr.progressBarStyleSmall).getIndeterminateDrawable().mutate();icon=spinner;}else icon=row.getContext().getDrawable(R.drawable.workspace_desktop).mutate();
            icon.setTint(WorkspaceDialogs.MUTED);int size=WorkspaceDialogs.dp(row.getContext(),24);icon.setBounds(0,0,size,size);row.setCompoundDrawables(icon,null,null,null);
        }
        pending=waiting;animate(waiting&&row.isAttachedToWindow());
    }
    private void animate(boolean value){if(spinner instanceof Animatable){if(value&&android.animation.ValueAnimator.areAnimatorsEnabled()){if(!((Animatable)spinner).isRunning())((Animatable)spinner).start();}else((Animatable)spinner).stop();}}
    private String t(String ru,String en){return AgentInstructionsUi.tr(row.getContext(),ru,en);}
}
