package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.JSONObject;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Expanded-status content only. Captures one run and thread; never targets the selected chat. */
final class CodexContextUi extends LinearLayout {
    final TermuxActivity activity;
    final WorkspaceStore.Session owner;
    final AgentChatUi.Transport transport;
    final TextView usage,note,result,compact;
    final ProgressBar bar;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final Handler handler=new Handler(Looper.getMainLooper());
    private JSONObject status=new JSONObject();
    private String thread="";
    boolean closed,loading,submitting;
    AlertDialog confirmation;
    private final Runnable poll=new Runnable(){public void run(){refresh();if(!closed)handler.postDelayed(this,1500);}};
    CodexContextUi(TermuxActivity activity,WorkspaceStore.Session profile){this(activity,profile,(p,q)->CodexChatControl.request(activity.getFilesDir(),p,q));}
    CodexContextUi(TermuxActivity activity,WorkspaceStore.Session profile,AgentChatUi.Transport transport){
        super(activity);this.activity=activity;this.owner=profile.copy();this.owner.id=profile.id;this.owner.codexSocketId=profile.codexSocketId;this.owner.handle=profile.handle;this.owner.name=profile.name;if(profile.codexChat.equals("id")&&CodexSessionShell.validThread(profile.codexThread))thread=profile.codexThread;this.transport=transport;setOrientation(VERTICAL);setTag("codex-context-panel");
        TextView title=label(t("Рабочий контекст Codex","Codex working context"));title.setTextColor(WorkspaceDialogs.TEXT);addView(title);
        usage=label("");usage.setTag("codex-context-usage");addView(usage);
        bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgressTintList(ColorStateList.valueOf(WorkspaceDialogs.ACCENT));bar.setTag("codex-context-progress");addView(bar,new LayoutParams(-1,WorkspaceDialogs.dp(activity,18)));
        note=label("");note.setTag("codex-context-stale");addView(note);
        compact=WorkspaceDialogs.action(activity,t("Сократить контекст","Compact context"),0);compact.setTag("codex-context-compact");compact.setOnClickListener(v->confirm());addView(compact,new LayoutParams(-1,-2));
        result=label("");result.setTag("codex-context-result");addView(result);render();
    }
    private String t(String ru,String en){return AgentStatusUi.tr(activity,ru,en);}
    private TextView label(String value){TextView view=new TextView(activity);view.setText(value);view.setTextSize(13);view.setTextColor(WorkspaceDialogs.MUTED);view.setPadding(0,WorkspaceDialogs.dp(activity,4),0,WorkspaceDialogs.dp(activity,4));return view;}
    @Override protected void onDetachedFromWindow(){close();super.onDetachedFromWindow();}
    void start(){if(!closed)handler.post(poll);}
    void close(){if(closed)return;closed=true;handler.removeCallbacks(poll);if(confirmation!=null)confirmation.dismiss();worker.shutdownNow();}
    static Long count(JSONObject object,String name){Object value=object==null?null:object.opt(name);if(!(value instanceof Number))return null;double number=((Number)value).doubleValue();long whole=((Number)value).longValue();return Double.isFinite(number)&&number==whole&&whole>=0&&whole<=9007199254740991L?whole:null;}
    private String number(long value){return String.format(Locale.ROOT,"%,d",value).replace(',',' ');}
    void apply(JSONObject value){
        String returned=value.optString("thread");
        if(returned.isEmpty()){status=new JSONObject();render();result.setText(t("Контекст не передан этим помощником CLI.","This CLI helper did not report context data."));return;}
        if(!CodexSessionShell.validThread(returned)||!thread.isEmpty()&&!thread.equals(returned)){status=new JSONObject();result.setText(t("Чат изменился. Откройте его статус заново.","The chat changed. Reopen its status."));render();return;}
        thread=returned;status=value;AgentOfflineData.remember(activity,owner,"context",value.optJSONObject("context"),System.currentTimeMillis());render();
    }
    private void render(){
        JSONObject context=status.optJSONObject("context"),tokens=context==null?null:context.optJSONObject("usage");Long used=count(tokens,"used"),capacity=count(tokens,"capacity");boolean known=used!=null&&capacity!=null&&capacity>0;
        usage.setText(known?t("Контекст: ","Context: ")+Math.round(used*100d/capacity)+"% · "+number(used)+" / "+number(capacity)+t(" токенов"," tokens"):t("Нет данных","No data"));
        bar.setVisibility(known?VISIBLE:GONE);if(known){bar.setProgress((int)Math.max(0,Math.min(100,Math.round(used*100d/capacity))));bar.setContentDescription(usage.getText());}
        boolean stale=context!=null&&context.optBoolean("stale",true);note.setText(known?(stale?t("Устаревшие данные · ожидается обновление CLI","Stale data · waiting for a CLI update"):t("Последняя оценка CLI","Latest CLI estimate")):"");note.setVisibility(known?VISIBLE:GONE);
        JSONObject operation=context==null?null:context.optJSONObject("compaction");String phase=operation==null?"":operation.optString("phase");boolean pending=phase.equals("starting")||phase.equals("running"),unknown=phase.equals("unknown");
        compact.setText(submitting||pending||unknown?t("Сокращение…","Compacting…"):t("Сократить контекст","Compact context"));
        AgentStatusUi.available(compact,!closed&&!submitting&&confirmation==null&&status.optBoolean("connected")&&!status.optBoolean("busy")&&context!=null&&context.optBoolean("compactSupported")&&context.optBoolean("canCompact")&&!unknown);
        if(pending||submitting)result.setText(t("Сокращение…","Compacting…"));
        else if(unknown)result.setText(t("Завершение сокращения не подтверждено. ","Compaction completion is unconfirmed. ")+operation.optString("error"));
        else if(phase.equals("completed"))result.setText(t("Контекст сокращён штатным Codex.","Context compacted by Codex."));
        else if(phase.equals("failed"))result.setText(t("Не удалось сократить контекст: ","Could not compact context: ")+operation.optString("error"));
        else if(status.optBoolean("connected"))result.setText(context==null||!context.optBoolean("compactSupported")?t("Поддержка сокращения этим CLI не подтверждена.","Compaction support is not confirmed for this CLI."):"");
    }
    void refresh(){if(closed||loading||submitting)return;loading=true;worker.execute(()->{
        JSONObject value=null;Exception failure=null;try{value=transport.request(owner,AgentChatUi.op("status"));}catch(Exception e){failure=e;}JSONObject response=value;Exception error=failure;
        activity.runOnUiThread(()->{loading=false;if(closed)return;if(error==null)apply(response);else unavailable();});
    });}
    private void unavailable(){try{status.put("connected",false);JSONObject context=status.optJSONObject("context");if(context!=null)context.put("stale",true);}catch(Exception ignored){}render();result.setText(t("Связь с Codex недоступна. Состояние операции не подтверждено.","Codex connection unavailable. Operation state is unconfirmed."));}
    private void confirm(){
        if(!compact.isEnabled()||closed||confirmation!=null||thread.isEmpty())return;final String target=thread;
        confirmation=WorkspaceDialogs.builder(activity).setTitle(t("Сократить контекст","Compact context")).setMessage(t("Сократить рабочий контекст? История чата сохранится, но часть деталей может не попасть в краткое содержание","Compact the working context? Chat history will be kept, but some details may be omitted from the summary"))
            .setPositiveButton(t("Сократить","Compact"),(d,w)->submit(target)).setNegativeButton(t("Отмена","Cancel"),null).create();confirmation.setOnDismissListener(d->{confirmation=null;render();});render();confirmation.show();
    }
    private void submit(String target){
        if(closed||submitting||!target.equals(thread))return;submitting=true;render();worker.execute(()->{
            JSONObject response=null,next=null;Exception failure=null;try{JSONObject command=AgentChatUi.op("chat-compact");command.put("session",target);response=transport.request(owner,command);next=transport.request(owner,AgentChatUi.op("status"));}catch(Exception e){failure=e;}
            JSONObject action=response,value=next;Exception error=failure;activity.runOnUiThread(()->{submitting=false;if(closed)return;if(value!=null)apply(value);else unavailable();if(error==null&&action!=null&&!action.optBoolean("ok"))result.setText(t("Сокращение не подтверждено: ","Compaction was not confirmed: ")+action.optString("error"));});
        });
    }
}
