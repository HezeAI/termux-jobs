package com.termux.app.workspace;

import android.content.Context;
import android.view.View;
import android.widget.*;
import java.util.function.Supplier;

/** Event-driven view of the selected operation; never starts or owns a process. */
final class AgentPreparationCard extends LinearLayout {
    final TextView phase,count,amount;
    final ProgressBar overall;
    private final Supplier<WorkspaceStore.Session> selected;
    private final Runnable changed=this::refresh;
    private boolean observing,closed;
    AgentPreparationCard(Context c,TextView button,Supplier<WorkspaceStore.Session> profile){
        super(c);selected=profile;setOrientation(VERTICAL);setAddStatesFromChildren(true);setTag("agent-preparation-card");
        setBackground(button.getBackground());button.setBackground(null);addView(button);
        phase=text(c,"agent-preparation-card-stage");count=text(c,"agent-preparation-card-count");amount=text(c,"agent-preparation-card-amount");
        phase.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        overall=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);overall.setTag("agent-preparation-card-progress");
        LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(c,8));bar.setMargins(WorkspaceDialogs.dp(c,12),0,WorkspaceDialogs.dp(c,12),WorkspaceDialogs.dp(c,8));addView(overall,bar);
        setOnClickListener(v->button.performClick());refresh();
    }
    private TextView text(Context c,String tag){TextView v=new TextView(c);v.setTag(tag);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(12);int pad=WorkspaceDialogs.dp(c,12);v.setPadding(pad,0,pad,WorkspaceDialogs.dp(c,4));addView(v);return v;}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(!closed&&!observing){observing=true;AgentPreparationOperation.listeners.add(changed);}refresh();}
    @Override protected void onDetachedFromWindow(){stopObserving();super.onDetachedFromWindow();}
    private void stopObserving(){if(observing){AgentPreparationOperation.listeners.remove(changed);observing=false;}}
    void close(){closed=true;stopObserving();}
    void refresh(){
        if(closed)return;
        WorkspaceStore.Session profile=selected.get();
        boolean ssh=profile.environment.equals("ssh");setVisibility(ssh?GONE:VISIBLE);
        AgentPreparationOperation.Job job=AgentPreparation.supports(profile)?AgentPreparationOperation.current(profile):null;
        for(View v:new View[]{phase,count,amount,overall})v.setVisibility(job==null?GONE:VISIBLE);
        if(job==null){phase.setText("");count.setText("");amount.setText("");return;}
        phase.setText(AgentPreparationOperation.stage(job,getContext())+(job.component.isEmpty()?"":" · "+job.component));
        phase.setTextColor(job.stage.equals("failed")?WorkspaceDialogs.DANGER:WorkspaceDialogs.TEXT);
        count.setText(AgentPreparationOperation.overall(job,getContext()));overall.setContentDescription(count.getText());
        AgentPreparationOperation.PlanProgress plan=job.plan;overall.setIndeterminate(plan==null&&job.active);overall.setMax(plan==null?1:plan.total());overall.setProgress(plan==null?0:plan.completed);
        amount.setText(job.total>0?String.format(java.util.Locale.ROOT,AgentPreparation.tr(getContext(),"%d / %d байт · %d%%","%d / %d bytes · %d%%"),job.received,job.total,job.percent):"");amount.setVisibility(job.total>0?VISIBLE:GONE);
    }
}
