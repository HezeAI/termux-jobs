package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Full server workspace. Monitoring is activity-scoped, no background phone service. */
final class WorkspaceServers implements AutoCloseable {
    interface Terminal {void open(String name,String command);}
    private final TermuxActivity activity;private final Terminal terminal;
    final ServerProfiles store;
    private final ServerWorkQueues queues=new ServerWorkQueues();
    private final ExecutorService worker=queues.foreground;
    private final Set<ServerConnection> auxiliary=ConcurrentHashMap.newKeySet();
    private int manualEpoch;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<String,ServerConnection> connections=new HashMap<>();
    private final Map<String,JSONObject> snapshots=new HashMap<>();
    private final Set<String> connected=new HashSet<>();
    private final Map<String,Boolean> agentPresence=new HashMap<>();
    private final List<WorkspaceFileBrowser> sshBrowsers=new ArrayList<>();
    private final List<ProjectFiles> textFiles=new ArrayList<>();
    private final List<WorkspaceTextEditor> editors=new ArrayList<>();
    private WorkspaceServerPlayer player;private String playerProfile="";
    private WorkspaceServerSchedules scheduleUi;
    private final WorkspaceServerUploads uploads;private final WorkspaceServerDownloads downloads;
    private LinearLayout root;private ServerScrollView scroll;
    private LinearLayout sectionBody,projectBanner;
    private final Map<String,TextView> sectionButtons=new HashMap<>();
    private ServerProfiles.Profile selected;private ServerProfiles.Project project;
    private String tab="overview",directory="~",error="";
    private TextView sampleText,monitorStatus,filePathLabel,fileStatus;private ServerMetricCards metricCards;
    private LinearLayout taskRows,streamPanel,fileRows;private TextView favoriteButton;private String taskFingerprint="";
    private final Map<String,String> streamNames=new HashMap<>();
    private TextView serviceText;
    private JSONObject listing;private boolean foreground,visible,busy,pollBusy,closed;private int generation,dialogs;
    private final List<AlertDialog> ownedDialogs=new ArrayList<>();
    private final Map<String,Long> lastPoll=new HashMap<>();
    private final Map<String,TextView> pollLabels=new HashMap<>(),listMetrics=new HashMap<>();
    private final Map<String,LinearLayout> transferRows=new HashMap<>();
    private Runnable transferListener=()->{};
    void transferListener(Runnable listener){transferListener=listener==null?()->{}:listener;}
    boolean transfersActive(){return downloads.presentation.active()||uploads.presentation.active();}
    private void transferChanged(){
        if(closed)return;
        for(Map.Entry<String,LinearLayout> entry:transferRows.entrySet()){
            boolean upload=entry.getKey().startsWith("up:");String id=entry.getKey().substring(3);ServerTransferPresentation state=upload?uploads.presentation:downloads.presentation;LinearLayout row=entry.getValue();TextView label=(TextView)row.getChildAt(0);android.widget.ProgressBar bar=(android.widget.ProgressBar)row.getChildAt(1);boolean active=state.belongs(id);
            int visibility=active?View.VISIBLE:View.GONE;if(row.getVisibility()!=visibility)row.setVisibility(visibility);
            if(active){String value=(upload?t("Загрузка на сервер","Uploading to server"):t("Скачивание с сервера","Downloading from server"))+"\n"+state.text();if(!value.contentEquals(label.getText()))label.setText(value);bar.setIndeterminate(state.indeterminate());if(!state.indeterminate())bar.setProgress(state.progress());}
        }
        transferListener.run();
    }
    private void transferRows(LinearLayout parent,ServerProfiles.Profile profile){
        for(boolean upload:new boolean[]{true,false}){
            ServerTransferPresentation state=upload?uploads.presentation:downloads.presentation;
            LinearLayout row=column();TextView label=button("",()->{});row.setBackground(label.getBackground());label.setBackground(null);label.setClickable(false);label.setFocusable(false);label.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);label.setMaxLines(3);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setTextSize(12);
            row.addView(label,new LinearLayout.LayoutParams(-1,0,1));
            android.widget.ProgressBar bar=new android.widget.ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.ACCENT));bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.ACCENT));
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(4));bp.setMargins(dp(12),0,dp(12),dp(10));row.addView(bar,bp);
            row.setOnClickListener(v->state.foreground());row.setContentDescription(upload?t("Прогресс загрузки на сервер","Upload progress"):t("Прогресс скачивания с сервера","Download progress"));row.setTag((upload?"up:":"dn:")+profile.id);
            parent.addView(row,new LinearLayout.LayoutParams(-1,dp(96)));transferRows.put((upload?"up:":"dn:")+profile.id,row);row.setVisibility(View.GONE);
        }
        transferChanged();
    }

    boolean pollingEnabled(ServerProfiles.Profile p){return connected.contains(p.id)&&!ServerConnection.disconnected(activity,p.id);}
    long pollingInterval(ServerProfiles.Profile p){return selected==p&&visible?10000:30000;}
    private final Runnable poll=new Runnable(){public void run(){if(closed||!foreground)return;
        if(!busy&&!pollBusy&&dialogs==0){long now=android.os.SystemClock.elapsedRealtime();for(ServerProfiles.Profile p:store.profiles)if(pollingEnabled(p)&&now-lastPoll.getOrDefault(p.id,-30000L)>=pollingInterval(p)){lastPoll.put(p.id,now);refreshMonitor(p);break;}}
        main.postDelayed(this,1000);
    }};
    String pollingLabel(ServerProfiles.Profile p){return pollingEnabled(p)?t("Подключён","Connected"):t("Отключён","Disconnected");}
    private TextView connectionButton(ServerProfiles.Profile p){TextView b=button(pollingEnabled(p)?t("Отключиться","Disconnect"):t("Подключиться","Connect"),()->{if(pollingEnabled(p))disconnectProfile(p);else connect(p);});b.setTag("server-connect-"+p.id);b.setEnabled(!busy||pollingEnabled(p));return b;}
    void disconnectProfile(ServerProfiles.Profile p){
        ServerConnection.disconnectAll(activity,p.id);connected.remove(p.id);snapshots.remove(p.id);lastPoll.remove(p.id);generation++;manualEpoch++;
        closeAuxiliary(p.id);ServerConnection c=connections.remove(p.id);if(c!=null)c.close();uploads.disconnect(p.id);downloads.disconnect(p.id);
        for(WorkspaceFileBrowser browser:new ArrayList<>(sshBrowsers))if(browser.serverProfileId().equals(p.id))browser.close();
        for(WorkspaceTextEditor editor:new ArrayList<>(editors))if(editor.serverProfileId().equals(p.id))editor.close();
        for(ProjectFiles files:new ArrayList<>(textFiles))if(files.profile.id.equals(p.id)){files.close();textFiles.remove(files);}
        if(scheduleUi!=null&&scheduleUi.serverProfileId().equals(p.id)){scheduleUi.close();scheduleUi=null;}
        if(playerProfile.equals(p.id))closePlayer();streamNames.remove(p.id);if(selected==p){listing=null;downloadSelection.clear();deleteSelectionMode=false;if(moveProfile.equals(p.id)){moveSelection.clear();moveProfile="";}}render();
    }
    private void updatePollLabel(ServerProfiles.Profile p,TextView label){boolean active=pollingEnabled(p);label.setText("●  "+pollingLabel(p));android.text.SpannableString value=new android.text.SpannableString(label.getText());value.setSpan(new android.text.style.ForegroundColorSpan(active?0xffe7bc55:WorkspaceDialogs.DANGER),0,1,0);value.setSpan(new android.text.style.RelativeSizeSpan(.65f),0,1,0);label.setText(value);}
    WorkspaceServers(TermuxActivity a,Terminal t){activity=a;terminal=t;store=new ServerProfiles(a);downloads=new WorkspaceServerDownloads(a);uploads=new WorkspaceServerUploads(a,()->{if(!closed&&visible&&selected!=null&&!ServerConnection.disconnected(activity,selected.id)&&tab.equals("files"))loadFiles();});downloads.presentation.listener(this::transferChanged);uploads.presentation.listener(this::transferChanged);}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(activity);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String s,int size,int color){TextView v=new TextView(activity);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));d.setStroke(dp(1),0xff213039);return d;}
    private TextView button(String s,Runnable action){TextView b=text(s,14,WorkspaceDialogs.ACCENT);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setBackground(WorkspaceDialogs.background(activity,false));b.setOnClickListener(v->action.run());return b;}
    private void setLeadingIcon(TextView view,int resource){android.graphics.drawable.Drawable icon=activity.getDrawable(resource).mutate();icon.setTint(WorkspaceDialogs.ACCENT);icon.setBounds(0,0,dp(22),dp(22));view.setCompoundDrawables(icon,null,null,null);view.setCompoundDrawablePadding(dp(8));}
    private TextView iconButton(String label,int resource,Runnable click){WorkspaceIconButton b=new WorkspaceIconButton(activity);b.setTextColor(WorkspaceDialogs.ACCENT);b.setGravity(Gravity.CENTER);b.setMinWidth(dp(48));b.setMinHeight(dp(48));b.setBackground(WorkspaceDialogs.background(activity,false));b.setIcon(activity.getDrawable(resource).mutate(),dp(22));b.setContentDescription(label);b.setOnClickListener(v->click.run());if(android.os.Build.VERSION.SDK_INT>=26)b.setTooltipText(label);return b;}
    private void control(LinearLayout row,View button){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(48),dp(48));if(row.getChildCount()>0)p.setMarginStart(dp(8));row.addView(button,p);}
    private void add(LinearLayout parent,View child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);parent.addView(child,p);}
    private LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(14),dp(14),dp(14),dp(14));c.setBackground(shape(0xff0b1319));add(parent,c);return c;}
    private void title(LinearLayout parent,String s){TextView v=text(s,18,WorkspaceDialogs.TEXT);v.setTypeface(null,Typeface.BOLD);v.setPadding(0,dp(8),0,dp(12));parent.addView(v);}
    private EditText field(LinearLayout parent,String label,String value,boolean multi){parent.addView(text(label,12,WorkspaceDialogs.MUTED));EditText v=new EditText(activity);v.setText(value);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(14);v.setSingleLine(!multi);v.setMinHeight(dp(48));if(multi)v.setMinLines(3);v.setContentDescription(label);add(parent,v);return v;}
    private AlertDialog dialog(String title,View form){AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(title).setView(form).setNegativeButton(t("Отмена","Cancel"),null).create();return d;}
    private void show(AlertDialog d){dialogs++;ownedDialogs.add(d);d.setOnDismissListener(x->{dialogs=Math.max(0,dialogs-1);ownedDialogs.remove(d);});d.show();}
    private void message(String title,String body){AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(title).setMessage(body).setPositiveButton("OK",null).create();show(d);}
    private void confirm(String title,String body,Runnable action){AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(title).setMessage(body).setPositiveButton(t("Да","Yes"),(x,w)->action.run()).setNegativeButton(t("Нет","No"),null).create();show(d);}
    private ServerConnection connection(ServerProfiles.Profile p){ServerConnection c=connections.get(p.id);if(c==null){c=new ServerConnection(activity,p);connections.put(p.id,c);}return c;}
    interface Work {JSONObject run()throws Exception;}
    interface Done {void done(JSONObject value)throws Exception;}
    private void run(Work work,Done done){run(work,done,true);}
    private void run(Work work,Done done,boolean feedback){
        if(closed||busy||!feedback&&pollBusy)return;
        if(feedback){manualEpoch++;busy=true;error="";render();}else pollBusy=true;
        final int g=generation;
        worker.execute(()->{JSONObject result=null;String failure=null;
            try{result=work.run();}catch(Exception e){String code=e.getMessage();failure=code!=null&&code.matches("[A-Z_]{1,80}")?code:"OPERATION_FAILED";}
            final JSONObject value=result;final String problem=failure;
            main.post(()->{if(closed)return;if(feedback)busy=false;else pollBusy=false;
                if(g!=generation){if(feedback)render();return;}
                if(problem!=null){if(feedback){error=explain(problem);render();}else monitorError(problem);return;}
                try{done.done(value);if(feedback)render();}catch(Exception e){if(feedback){error=explain("OPERATION_FAILED");render();}else monitorError("OPERATION_FAILED");}
            });
        });
    }
    /** Explicit file actions update their own region/dialog, never the page. */
    private void runInline(Work work,Done done,java.util.function.Consumer<String> failure){runInline(work,done,failure,()->{});}
    private void runInline(Work work,Done done,java.util.function.Consumer<String> failure,Runnable discarded){
        if(closed||busy)return;manualEpoch++;busy=true;final int g=generation;
        worker.execute(()->{JSONObject result=null;String problem=null;try{result=work.run();}catch(Exception e){String code=e.getMessage();problem=code!=null&&code.matches("[A-Z_]{1,80}")?code:"OPERATION_FAILED";}final JSONObject value=result;final String errorCode=problem;
            main.post(()->{if(closed){discarded.run();return;}busy=false;if(g!=generation){discarded.run();return;}if(errorCode!=null){failure.accept(errorCode);return;}try{done.done(value);}catch(Exception e){failure.accept("OPERATION_FAILED");}});
        });
    }
    private void monitorError(String code){if(visible&&foreground)updateStreamPanel();if(visible&&foreground&&monitorStatus!=null){String value=t("Метрики не обновлены: ","Metrics not updated: ")+code;monitorStatus.setText(value);monitorStatus.setContentDescription(explain(code));monitorStatus.setTextColor(WorkspaceDialogs.DANGER);}}
    private void updateLiveViews(JSONObject data){
        if(!visible||!foreground)return;
        JSONObject metrics=data.optJSONObject("metrics");if(metrics==null)return;
        if(metricCards!=null)metricCards.update(metrics);
        if(sampleText!=null)sampleText.setText(sampleLabel(metrics));
        if(monitorStatus!=null){monitorStatus.setText(t("Автообновление данных от сервера","Auto-update data from server"));monitorStatus.setContentDescription(monitorStatus.getText());monitorStatus.setTextColor(WorkspaceDialogs.MUTED);}
        updateStreamPanel();
        JSONArray tasks=data.optJSONArray("tasks");String fingerprint=tasks==null?"":tasks.toString();
        if(taskRows!=null&&!fingerprint.equals(taskFingerprint)){taskFingerprint=fingerprint;preserveViewport(()->{taskRows.removeAllViews();tasks(taskRows,tasks);});}
    }
    private String sampleLabel(JSONObject metrics){return t("Снимок: ","Snapshot: ")+new java.text.SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date(metrics.optLong("time")*1000));}
    private String explain(String code){
        switch(code){
            case "SSH_FAILED":return t("SSH недоступен. Проверьте сеть, ключ/пароль, права и доверенный ключ хоста. При смене ключа требуется повторная проверка отпечатка.","SSH unavailable. Check network, credentials, permissions and pinned host key. A changed key requires fingerprint verification.");
            case "OPENSSH_REQUIRED":return t("Нужен пакет openssh в Termux. Установите его через менеджер пакетов.","The Termux openssh package is required. Install it using the package manager.");
            case "PYTHON_OR_AGENT_UNAVAILABLE":return t("Нужен Python 3 на сервере или совместимый агент. SSH-терминал остаётся доступен.","Python 3 or a compatible agent is required on the server. The SSH terminal is still available.");
            case "ACTIVE_TASKS":return t("Удаление заблокировано: есть активные или неподконтрольные задачи. Завершите их отдельно; для неподконтрольной задачи проверьте процессы вручную и удалите только её запись.","Removal blocked: active or unmanaged tasks exist. Finish them separately; inspect unmanaged processes manually before forgetting their records.");
            case "SERVICE_PERMISSION_OR_UNAVAILABLE":return t("Недостаточно прав или служба недоступна. Агент не использует sudo и не повышает права.","Insufficient permissions or service unavailable. The agent does not use sudo or elevate privileges.");
            case "PASSWORD_REQUIRED":return t("Введите и сохраните пароль в профиле сервера; он шифруется Android Keystore.","Enter and save the server password; Android Keystore encrypts it.");
            default:return t("Операция не выполнена. Код: ","Operation failed. Code: ")+code;
        }
    }
    void attach(LinearLayout host){root=host;visible=true;render();}
    void visibility(boolean v){visible=v;}
    void resume(){foreground=true;main.removeCallbacks(poll);main.postDelayed(poll,1000);}
    void pause(){foreground=false;main.removeCallbacks(poll);closePlayer();}
    boolean back(){if(selected!=null){closeAuxiliary(selected.id);selected=null;project=null;listing=null;generation++;render();return true;}return false;}
    void info(){message(t("Сервер + Agent","Server + Agent"),t(
        "Устанавливается только с согласия в ~/.local/share/termux-jobs-server, требует Linux, Bash и Python 3.8+. Ведёт задачи и отдаёт файлы. Права равны SSH-пользователю; root означает полный доступ. Для ограничения прав используйте отдельную учётную запись.\n\nАвтозапуска после перезагрузки сервера нет. При подключении агент возобновится. Задачи продолжаются без телефона, но не после перезагрузки сервера; команды должны работать на переднем плане. Журналы до 2 МБ на задачу, последние 40 записей; команды и вывод могут содержать секреты.\n\nВ фоне телефона мониторинг приостановлен. Просмотр видео — через временный SSH-туннель до 30 минут; встроенный плеер освобождается в фоне. Агент удаляется отдельной кнопкой после проверки задач, проекты не удаляются. SSH в создании сессий также сохранён.",
        "Installed with consent under ~/.local/share/termux-jobs-server; requires Linux, Bash and Python 3.8+. Manages tasks and serves files. It has the SSH user's permissions; root means full access. Use a separate account to restrict access.\n\nNo automatic server-boot startup. Connecting restarts the agent. Tasks survive phone disconnection, not server reboot; commands must stay in the foreground. Logs capped at 2 MB/task, latest 40 records. Commands/output may contain secrets.\n\nPhone background monitoring pauses. Video uses a temporary SSH tunnel up to 30 minutes; the built-in player is released in the background. Agent removal is a separate action after checking tasks, without deleting projects. Existing SSH session creation is retained."));}
    void rerender(){render();}
    private void render(){
        if(root==null||closed||!visible)return;int y=scroll==null?0:scroll.getScrollY();sampleText=null;monitorStatus=null;metricCards=null;taskRows=null;streamPanel=null;fileRows=null;filePathLabel=null;fileStatus=null;serviceText=null;sectionBody=null;projectBanner=null;sectionButtons.clear();pollLabels.clear();listMetrics.clear();transferRows.clear();root.removeAllViews();scroll=new ServerScrollView(activity);scroll.setFillViewport(true);LinearLayout body=column();body.setPadding(0,dp(12),0,dp(16));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        if(!store.writable)add(body,text(t("Профили повреждены или имеют неподдерживаемый формат; запись отключена, исходный файл сохранён.","Profiles are damaged or unsupported; writes disabled, original file preserved."),14,WorkspaceDialogs.DANGER));
        LinearLayout heading=row();TextView h=text(selected==null?t("Серверы","Servers"):selected.name,19,WorkspaceDialogs.TEXT);h.setTypeface(null,Typeface.BOLD);heading.addView(h,new LinearLayout.LayoutParams(0,-2,1));h.setMaxLines(2);h.setEllipsize(android.text.TextUtils.TruncateAt.END);if(selected!=null){TextView diagnostics=button(t("Диагностика","Diagnostics"),this::serverDiagnostics);diagnostics.setTextSize(12);diagnostics.setPadding(dp(4),0,dp(4),0);diagnostics.setContentDescription(t("Диагностика сервера","Server diagnostics"));LinearLayout.LayoutParams diagnosticsParams=new LinearLayout.LayoutParams(dp(106),dp(48));diagnosticsParams.setMarginEnd(dp(8));heading.addView(diagnostics,diagnosticsParams);}heading.addView(iconButton(t("Об агенте сервера","About server agent"),R.drawable.workspace_server_info,this::info));if(selected!=null){LinearLayout.LayoutParams menuParams=new LinearLayout.LayoutParams(dp(48),dp(48));menuParams.setMarginStart(dp(8));heading.addView(iconButton(t("Настройки сервера","Server settings"),R.drawable.workspace_more,this::menu),menuParams);}add(body,heading);
        if(busy)add(body,text(t("Подключение / выполнение…","Connecting / working…"),13,WorkspaceDialogs.ACCENT));
        if(!error.isEmpty())add(body,text(error,13,WorkspaceDialogs.DANGER));
        if(selected==null){renderList(body);}else renderServer(body);
        scroll.restoreTo(y);
    }
    private void renderList(LinearLayout body){
        if(store.profiles.isEmpty())add(body,text(t("Добавьте свой сервер. Подключения независимы от обычных SSH-сессий.","Add your server. These connections are independent of existing SSH sessions."),14,WorkspaceDialogs.MUTED));
        if(!store.profiles.isEmpty())add(body,text(t("Автообновление данных от сервера: после подключения каждые 10 с внутри сервера и 30 с в остальных разделах приложения. В фоне приложения приостановлено. «Отключиться» закрывает соединения панели и передачи; сервер, агент, удалённые задачи и отдельные SSH-сессии продолжают работать.","Auto-update data from server: every 10 s inside the server view and 30 s elsewhere after connecting. Paused while the app is in the background. Disconnect closes workspace connections and transfers; the server, agent, remote tasks and independent SSH sessions keep running."),12,WorkspaceDialogs.MUTED));
        for(ServerProfiles.Profile p:store.profiles){LinearLayout c=card(body);title(c,p.name);LinearLayout address=row();address.addView(text(p.user+"@"+p.host+":"+p.port,13,WorkspaceDialogs.MUTED),new LinearLayout.LayoutParams(0,-2,1));TextView toggle=connectionButton(p);LinearLayout.LayoutParams toggleParams=new LinearLayout.LayoutParams(-2,dp(48));toggleParams.setMarginStart(dp(8));address.addView(toggle,toggleParams);add(c,address);TextView status=text("",12,WorkspaceDialogs.MUTED);pollLabels.put(p.id,status);updatePollLabel(p,status);add(c,status);JSONObject snapshot=snapshots.get(p.id);if(snapshot!=null&&snapshot.has("metrics")){TextView metrics=text(metricSummary(snapshot.optJSONObject("metrics")),13,WorkspaceDialogs.ACCENT);listMetrics.put(p.id,metrics);add(c,metrics);}transferRows(c,p);add(c,button(t("Открыть","Open"),()->{if(busy)return;selected=p;directory="~";tab="overview";listing=null;generation++;render();if(p.automatic&&!ServerConnection.disconnected(activity,p.id)&&!pollingEnabled(p))connect();}));c.setOnLongClickListener(v->{if(busy)return true;selected=p;generation++;render();menu();return true;});}
        TextView add=button(t("Добавить сервер","Add server"),()->edit(null));add.setEnabled(store.writable&&!busy);add(body,add);
        add(body,text(t("Профиль хранится на телефоне: это адрес сервера, порт, пользователь и способ входа. Агент, задачи, файлы и программы находятся на сервере и остаются там после удаления профиля из приложения. Профили серверов пока не входят в экспорт библиотеки.","A profile is stored on this phone: the server address, port, user and authentication method. The agent, tasks, files and programs live on the server and remain there when you delete the app profile. Server profiles are not yet included in library exports."),12,WorkspaceDialogs.MUTED));
    }
    private void renderServer(LinearLayout body){
        final ServerProfiles.Profile p=selected;add(body,text(p.user+"@"+p.host+":"+p.port,13,WorkspaceDialogs.MUTED));transferRows(body,p);
        TextView all=button(t("Все серверы","All servers"),()->{if(!busy)back();});setLeadingIcon(all,R.drawable.workspace_server_back);add(body,all);
        LinearLayout actions=row();actions.addView(connectionButton(p),new LinearLayout.LayoutParams(0,-2,1));TextView terminalButton=iconButton(t("Терминал","Terminal"),R.drawable.workspace_terminal,this::openTerminal);LinearLayout.LayoutParams terminalParams=new LinearLayout.LayoutParams(dp(48),dp(48));terminalParams.setMarginStart(dp(8));actions.addView(terminalButton,terminalParams);add(body,actions);
        if(ServerConnection.disconnected(activity,p.id)){add(body,text(t("Отключено. Подключитесь для работы с панелью и автообновления данных от сервера. Удалённые задачи и отдельные SSH-сессии продолжают работать.","Disconnected. Connect to use the workspace and auto-update data from server. Remote tasks and independent SSH sessions keep running."),13,WorkspaceDialogs.MUTED));return;}
        JSONObject data=snapshots.get(p.id);
        if(data==null||!data.has("metrics")){
            add(body,text(t("Подключитесь для проверки Python и агента. Без агента доступны SSH-терминал и файловый менеджер.","Connect to check Python and agent. SSH terminal and file browser work without the agent."),14,WorkspaceDialogs.MUTED));
            add(body,button(t("Проверить состояние агента","Check agent status"),this::agentStateDialog));add(body,button(t("Файлы через SSH","Files over SSH"),this::sshFiles));add(body,button(t("Расписания задач","Task schedules"),()->{if(!busy&&selected!=null)scheduleUi().list();}));return;
        }
        JSONObject metrics=data.optJSONObject("metrics");metricCards=new ServerMetricCards(activity);metricCards.update(metrics);add(body,metricCards);
        sampleText=text(sampleLabel(metrics),12,WorkspaceDialogs.MUTED);add(body,sampleText);
        monitorStatus=text(t("Автообновление данных от сервера","Auto-update data from server"),11,WorkspaceDialogs.MUTED);monitorStatus.setSingleLine(true);monitorStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);add(body,monitorStatus);
        HorizontalScrollView strip=new HorizontalScrollView(activity);strip.setHorizontalScrollBarEnabled(false);LinearLayout choices=row();String[] keys={"overview","tasks","files","services"};String[] labels={t("Проекты","Projects"),t("Задачи","Tasks"),t("Файлы","Files"),t("Службы","Services")};for(int i=0;i<keys.length;i++){final String key=keys[i];TextView b=button(labels[i],()->selectSection(key));sectionButtons.put(key,b);b.setSelected(tab.equals(key));b.setBackground(WorkspaceDialogs.background(activity,tab.equals(key)));b.setSingleLine(true);LinearLayout.LayoutParams option=new LinearLayout.LayoutParams(-2,dp(48));if(i<keys.length-1)option.setMarginEnd(dp(8));choices.addView(b,option);}strip.addView(choices);add(body,strip);
        projectBanner=column();body.addView(projectBanner);sectionBody=column();body.addView(sectionBody);renderSection();
        streamPanel=column();body.addView(streamPanel);updateStreamPanel();
    }
    private void selectSection(String key){
        if(busy||selected==null||key.equals(tab)||!sectionButtons.containsKey(key))return;
        tab=key;listing=null;renderSection();
        if(key.equals("files"))loadFiles();else if(key.equals("services"))services();
    }
    private void preserveViewport(Runnable update){if(scroll==null)update.run();else scroll.preserve(update);}
    private void renderSection(){
        if(sectionBody==null||projectBanner==null||selected==null)return;
        JSONObject data=snapshots.get(selected.id);if(data==null)return;
        int previousHeight=sectionBody.getHeight();
        preserveViewport(()->{
            taskRows=null;fileRows=null;filePathLabel=null;fileStatus=null;serviceText=null;
            sectionBody.setMinimumHeight(tab.equals("files")||tab.equals("services")?previousHeight:0);sectionBody.removeAllViews();projectBanner.removeAllViews();
            for(Map.Entry<String,TextView> entry:sectionButtons.entrySet()){boolean active=tab.equals(entry.getKey());entry.getValue().setSelected(active);entry.getValue().setBackground(WorkspaceDialogs.background(activity,active));}
        if(project!=null){add(projectBanner,text(t("Проект: ","Project: ")+project.name+"\n"+project.path,13,WorkspaceDialogs.MUTED));add(projectBanner,button(t("Снять выбор проекта","Clear project selection"),()->{project=null;directory="~";renderSection();}));}
        if(tab.equals("overview"))projects(sectionBody);else if(tab.equals("tasks")){taskRows=column();add(sectionBody,taskRows);taskFingerprint=data.optJSONArray("tasks")==null?"":data.optJSONArray("tasks").toString();tasks(taskRows,data.optJSONArray("tasks"));}else if(tab.equals("files"))files(sectionBody);else{add(sectionBody,button(t("Обновить службы","Refresh services"),this::services));add(sectionBody,button(t("Журнал / перезапуск службы…","Service log / restart…"),this::serviceDialog));serviceText=text(listing==null?"":listing.optString("text"),12,WorkspaceDialogs.TEXT);add(sectionBody,serviceText);}
        });
    }
    private String state(String value){switch(value){case "queued":return t("В очереди","Queued");case "starting":return t("Запускается","Starting");case "running":return t("Выполняется","Running");case "done":return t("Завершена","Completed");case "failed":return t("Ошибка","Failed");case "cancelled":return t("Остановлена","Stopped");case "unmanaged":return t("Требует ручной проверки","Needs manual inspection");default:return value;}}
    private String metricSummary(JSONObject m){if(m==null)return "";return "CPU "+(m.isNull("cpu")?"—":m.optDouble("cpu")+"%")+"  · RAM "+size(m.optLong("ramUsed"))+" / "+size(m.optLong("ramTotal"))+"\n"+t("Диск ","Disk ")+size(m.optLong("diskUsed"))+" / "+size(m.optLong("diskTotal"));}
    static String size(long n){return ServerFileInfo.size(n,false);}
    private void menu(){
        if(selected==null||busy)return;
        String[] labels={t("Настройки агента","Agent settings"),t("Настройки подключения","Connection settings"),t("Проверить / заменить ключ хоста…","Verify / replace host key…"),t("Отключиться","Disconnect"),t("Удалить профиль из приложения…","Delete profile from app…"),t("Диагностика подключения","Connection diagnostics")};
        AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(selected.name).setItems(labels,(x,w)->{
            if(w==0)agentSettings();if(w==1)edit(selected);if(w==2)scan(true);if(w==5)connectionDiagnostics();
            if(w==3)disconnectProfile(selected);
            if(w==4){ServerProfiles.Profile p=selected;confirm(t("Удалить профиль?","Delete profile?"),t("Агент и задачи на сервере останутся. Удаляется только профиль подключения на телефоне.","Server agent and tasks remain. Only the phone connection profile is removed."),()->{try{store.profiles.remove(p);store.save();closeAuxiliary(p.id);ServerConnection c=connections.remove(p.id);if(c!=null)c.close();connected.remove(p.id);snapshots.remove(p.id);agentPresence.remove(p.id);back();}catch(Exception e){message("Error",explain("STORE_ERROR"));}});}
        }).create();show(d);
    }
    private void serverDiagnostics(){if(selected==null)return;ownedDialogs.removeIf(d->!d.isShowing());AlertDialog dialog=ServerDiagnosticsUi.show(activity,selected,store);ownedDialogs.add(dialog);}
    private void connectionDiagnostics(){
        if(selected==null||busy)return;ServerConnection c=new ServerConnection(activity,selected);auxiliary.add(c);Future<?>[] future={null};TextView status=text(t("Проверка доступности порта → доверия SSH → Python → файла агента…","Checking port reachability → SSH trust → Python → agent file…"),14,WorkspaceDialogs.TEXT);status.setPadding(dp(16),dp(8),dp(16),dp(8));
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Диагностика подключения","Connection diagnostics")).setView(status).setPositiveButton(t("Закрыть","Close"),null).create();show(dialog);
        dialog.setOnDismissListener(d->{dialogs=Math.max(0,dialogs-1);ownedDialogs.remove(dialog);queues.cancelHelper(future[0]);c.close();auxiliary.remove(c);});
        try{future[0]=queues.helper(()->{JSONObject result=null;try{result=c.diagnose();}catch(Exception ignored){}finally{c.close();auxiliary.remove(c);}JSONObject report=result;main.post(()->{if(closed||!dialog.isShowing())return;status.setText(diagnosticReport(report));});});}catch(RejectedExecutionException e){c.close();auxiliary.remove(c);status.setText(t("Все фоновые проверки заняты. Повторите позже.","Background checks are busy. Try again later."));}
    }
    String diagnosticReport(JSONObject report){
        if(report==null)return t("Проверка не завершена.","Check did not complete.");
        if(!report.optBoolean("network"))return t("Порт недоступен. Проверьте адрес, сеть, VPN, firewall и запущен ли SSH-сервер.","Port unreachable. Check address, connectivity, VPN, firewall and whether SSH is running.");
        String text=t("Порт доступен.","Port reachable.");
        if(!report.optBoolean("trusted"))return text+"\n"+t("Ключ сервера ещё не подтверждён. Подключитесь и сверьте отпечаток. Авторизация не проверялась.","Server key not pinned yet. Connect and verify the fingerprint. Authentication was not tested.");
        JSONObject probe=report.optJSONObject("probe");if(probe==null)return text+"\n"+t("SSH / авторизация / Python: проверка не пройдена. Этот ответ не позволяет точно отличить неверный ключ/пароль, изменённый ключ хоста и отсутствие Python. Откройте SSH-терминал для уточнения.","SSH / authentication / Python: check failed. This response cannot distinguish bad credentials, a changed host key or missing Python. Open the SSH terminal for details.");
        return text+"\n"+t("SSH и авторизация: успешно.","SSH and authentication: passed.")+"\nPython: "+probe.optString("python")+" · "+(probe.optBoolean("supported")?"OK":t("Нужен 3.8+","Requires 3.8+"))+"\n"+(probe.optBoolean("agent")?t("Файл агента найден.","Agent file found."):t("Файл агента не найден.","Agent file not found."))+"\n"+t("Агент и задачи не запускались; наличие файла не доказывает его работоспособность.","Agent and tasks were not started; file presence does not prove agent health.");
    }
    String agentLabel(ServerProfiles.Profile profile){Boolean present=knownAgent(profile);return present==null?t("Агент: состояние не проверено","Agent: not checked"):present?t("Агент: установлен · по последней проверке","Agent: installed · last check"):t("Агент: не установлен · по последней проверке","Agent: not installed · last check");}
    private Boolean knownAgent(ServerProfiles.Profile p){
        JSONObject data=snapshots.get(p.id);if(data!=null&&data.optInt("protocol")==1&&data.has("metrics"))return true;
        return agentPresence.get(p.id);
    }
    private void agentSettings(){
        if(selected==null||busy)return;
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Настройки агента","Agent settings")).setItems(new String[]{t("Проверить состояние агента","Check agent status"),t("Удалить агента…","Remove agent…"),t("Компоненты агента","Agent components")},(d,w)->{if(w==0)agentStateDialog();else if(w==1)uninstall();else components();}).create();show(dialog);
    }
    private void agentStateDialog(){
        if(selected==null||busy)return;final ServerProfiles.Profile p=selected;ServerConnection c=connection(p);
        LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(12));
        Boolean cached=knownAgent(p);TextView status=text(Boolean.TRUE.equals(cached)?t("Агент установлен · проверка состояния…","Agent installed · checking status…"):t("Проверка состояния агента…","Checking agent status…"),14,WorkspaceDialogs.TEXT);add(body,status);
        TextView installButton=button(t("Установить Agent…","Install Agent…"),()->{}),removeButton=button(t("Удалить агента…","Remove agent…"),()->{}),retryButton=button(t("Проверить снова","Check again"),()->{});
        for(TextView button:new TextView[]{installButton,removeButton,retryButton}){button.setVisibility(View.GONE);add(body,button);}
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Состояние агента","Agent status")).setView(body).setPositiveButton("OK",null).create();show(dialog);
        installButton.setOnClickListener(v->{dialog.dismiss();if(selected==p)install();});removeButton.setOnClickListener(v->{dialog.dismiss();if(selected==p)uninstall();});retryButton.setOnClickListener(v->{dialog.dismiss();if(selected==p){if(c.trusted())agentStateDialog();else connect();}});
        if(!c.trusted()){status.setText(t("Сначала подключитесь и подтвердите ключ сервера. Состояние агента пока неизвестно.","Connect and verify the server key first. Agent status is unknown."));retryButton.setText(t("Подключить / проверить","Connect / verify"));retryButton.setVisibility(View.VISIBLE);return;}
        checkAgentSettings(c::probe,p,dialog,status,installButton,removeButton,retryButton);
    }
    private void checkAgentSettings(Work probe,ServerProfiles.Profile p,AlertDialog dialog,TextView status,TextView installButton,TextView removeButton,TextView retryButton){
        runInline(probe,result->{
            boolean installed=result.getBoolean("agent");agentPresence.put(p.id,installed);
            if(!installed){snapshots.remove(p.id);connected.remove(p.id);}
            if(!dialog.isShowing()||selected!=p)return;
            status.setText(installed?t("Агент установлен","Agent installed"):t("Агент не установлен","Agent not installed"));
            if(!result.optBoolean("supported",false))status.append("\n"+t("Для работы агента нужен Python 3.8+.","Agent requires Python 3.8+."));
            installButton.setVisibility(!installed&&result.optBoolean("supported",false)?View.VISIBLE:View.GONE);
            removeButton.setVisibility(installed?View.VISIBLE:View.GONE);retryButton.setVisibility(View.GONE);
        },code->{if(dialog.isShowing()&&selected==p){status.setText(t("Не удалось проверить состояние агента. Ошибка подключения не означает, что агент отсутствует.","Could not verify agent status. A connection error does not mean the agent is absent.")+"\n"+explain(code));retryButton.setVisibility(View.VISIBLE);}});
    }
    private void edit(ServerProfiles.Profile old){
        if(busy||!store.writable)return;ServerProfiles.Profile draft=new ServerProfiles.Profile();if(old!=null)draft.id=old.id;
        LinearLayout form=column();form.setPadding(dp(16),0,dp(16),0);EditText name=field(form,t("Название","Name"),old==null?"":old.name,false),host=field(form,t("Адрес без порта","Host without port"),old==null?"":old.host,false),port=field(form,t("Порт","Port"),old==null?"22":""+old.port,false),user=field(form,t("Пользователь","User"),old==null?"":old.user,false);port.setInputType(2);
        Spinner auth=new Spinner(activity);auth.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,new String[]{t("Автоматически / SSH-ключ","Automatic / SSH key"),t("Файл SSH-ключа","SSH key file"),t("Пароль (Keystore)","Password (Keystore)")}));String[] modes={"auto","key","password"};auth.setSelection(old==null?0:Arrays.asList(modes).indexOf(old.auth));add(form,auth);
        LinearLayout keyFields=column(),passwordFields=column();
        EditText identity=field(keyFields,t("Путь приватного ключа","Private key path"),old==null?"":old.identity,false);
        WorkspaceSshKeyPicker keyPicker=new WorkspaceSshKeyPicker(activity);
        keyFields.removeView(identity);LinearLayout identityRow=row();identityRow.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
        control(identityRow,iconButton(t("Выбрать файл ключа: Termux, Ubuntu или память устройства","Choose key file: Termux, Ubuntu or device storage"),R.drawable.workspace_folder,()->keyPicker.choose(identity::setText)));add(keyFields,identityRow);
        EditText passphrase=field(keyFields,t("Passphrase (если требуется)","Passphrase (if required)"),"",false);
        passphrase.setInputType(0x81);passphrase.setSaveEnabled(false);
        passphrase.setHint(t("Пусто — сохранить прежний для того же ключа","Blank keeps existing for the same key"));
        CheckBox clearPassphrase=new CheckBox(activity);clearPassphrase.setText(t("Удалить сохранённый passphrase","Remove saved passphrase"));clearPassphrase.setTextColor(WorkspaceDialogs.TEXT);add(keyFields,clearPassphrase);
        EditText password=field(passwordFields,t("Пароль: пусто — сохранить прежний","Password: blank keeps existing"),"",false);password.setInputType(0x81);password.setSaveEnabled(false);
        if(android.os.Build.VERSION.SDK_INT>=26){passphrase.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);}
        add(form,keyFields);add(form,passwordFields);
        Runnable updateAuth=()->{keyFields.setVisibility(auth.getSelectedItemPosition()==1?View.VISIBLE:View.GONE);passwordFields.setVisibility(auth.getSelectedItemPosition()==2?View.VISIBLE:View.GONE);};
        auth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int position,long id){updateAuth.run();}public void onNothingSelected(AdapterView<?> parent){}});updateAuth.run();
        add(form,text(t("Автоматически: доступные SSH-ключи без запроса пароля. Файл ключа: указанный приватный ключ. Пароль: пароль пользователя сервера. Passphrase расшифровывает защищённый файл ключа.","Automatic: available SSH keys without password prompts. Key file: the specified private key. Password: the server user’s password. A passphrase unlocks an encrypted key file."),12,WorkspaceDialogs.MUTED));
        CheckBox automatic=new CheckBox(activity);automatic.setText(t("Подключаться при открытии сервера","Connect when opening server"));automatic.setTextColor(WorkspaceDialogs.TEXT);automatic.setChecked(old==null||old.automatic);add(form,automatic);
        add(form,text(t("Пароль и passphrase сохраняются только зашифрованными Android Keystore. После переноса данных может потребоваться повторный ввод. Ключ хоста подтверждается отдельно.","Password and passphrase are saved only encrypted with Android Keystore. Moving data may require re-entry. Host key is verified separately."),12,WorkspaceDialogs.MUTED));
        ScrollView viewport=new ScrollView(activity);viewport.addView(form);AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(t("Профиль сервера","Server profile")).setView(viewport).setPositiveButton(t("Сохранить","Save"),null).setNegativeButton(t("Отмена","Cancel"),null).create();show(d);
        d.setOnDismissListener(x->{dialogs=Math.max(0,dialogs-1);ownedDialogs.remove(d);password.setText("");passphrase.setText("");keyPicker.close();});
        d.getButton(-1).setOnClickListener(v->{
            try{draft.name=name.getText().toString().trim();draft.host=host.getText().toString().trim();draft.user=user.getText().toString().trim();draft.port=Integer.parseInt(port.getText().toString());draft.auth=modes[auth.getSelectedItemPosition()];draft.identity=identity.getText().toString().trim();draft.automatic=automatic.isChecked();if(!ServerProfiles.valid(draft))throw new IllegalArgumentException();
                boolean same=old!=null&&old.host.equals(draft.host)&&old.user.equals(draft.user)&&old.port==draft.port;
                if(draft.auth.equals("password")||draft.auth.equals("key")){
                    EditText input=draft.auth.equals("key")?passphrase:password;
                    char[] secret=new char[input.length()];android.text.TextUtils.getChars(input.getText(),0,input.length(),secret,0);
                    try{
                        for(char c:secret)if(c=='\n'||c=='\r'||c=='\0')throw new IllegalArgumentException();if(secret.length>256)throw new IllegalArgumentException();
                        boolean keep=same&&old.auth.equals(draft.auth)&&(!draft.auth.equals("key")||old.identity.equals(draft.identity));
                        draft.encrypted=draft.auth.equals("key")&&clearPassphrase.isChecked()?"":secret.length>0?SshPasswordVault.encrypt(draft.session(),secret):keep?old.encrypted:"";
                        if(draft.auth.equals("password")&&draft.encrypted.isEmpty())throw new IllegalArgumentException();
                    }finally{Arrays.fill(secret,'\0');}
                }
                if(old!=null){draft.diagnosticsPaused=old.diagnosticsPaused;draft.backgroundPolling=old.backgroundPolling;draft.projects.addAll(old.projects);draft.favorites.addAll(old.favorites);draft.favoriteFiles.addAll(old.favoriteFiles);draft.actions.addAll(old.actions);}int index=old==null?store.profiles.size():store.profiles.indexOf(old);if(old==null)store.profiles.add(draft);else store.profiles.set(index,draft);
                try{store.save();}catch(Exception failure){if(old==null)store.profiles.remove(draft);else store.profiles.set(index,old);throw failure;}
                closeAuxiliary(draft.id);ServerConnection c=connections.remove(draft.id);if(c!=null)c.close();if(old!=null&&!same)new File(activity.getFilesDir(),"server-host-"+old.id).delete();snapshots.remove(draft.id);connected.remove(draft.id);agentPresence.remove(draft.id);if(selected==old)selected=draft;generation++;keyPicker.retain(draft.auth.equals("key")?draft.identity:"");password.setText("");passphrase.setText("");d.dismiss();render();
            }catch(Exception e){host.setError(t("Проверьте поля, порт и пароль; профиль не сохранён","Check fields, port and password; profile not saved"));}
        });
    }
    private void connect(){if(selected!=null)connect(selected);}
    private void connect(ServerProfiles.Profile p){
        if(busy||closed)return;ServerConnection.allow(activity,p.id);ServerConnection c=connection(p);
        if(!c.trusted()){selected=p;generation++;render();scan(false);return;}
        run(c::probe,result->{boolean installed=result.getBoolean("agent");agentPresence.put(p.id,installed);
            if(!result.optBoolean("supported",false)){connected.remove(p.id);snapshots.remove(p.id);error=t("Для агента нужен Python 3.8+; SSH-терминал доступен.","Agent requires Python 3.8+; SSH terminal is available.");return;}
            if(installed)run(()->c.rpc(new JSONObject().put("op","status")),r->{if(r.optInt("protocol")!=1){connected.remove(p.id);error=explain("PROTOCOL_MISMATCH");return;}snapshots.put(p.id,r);agentPresence.put(p.id,true);connected.add(p.id);});
            else{connected.remove(p.id);snapshots.remove(p.id);}
        });
    }

    private void scan(boolean replacement){if(selected==null||busy)return;ServerConnection c=connection(selected);final String[][] keys={null};run(()->{keys[0]=c.scan();return new JSONObject();},r->confirm(t("Проверка SSH-сервера","Verify SSH server"),selected.user+"@"+selected.host+"\n\n"+keys[0][1]+t("\nСверьте отпечаток по независимому каналу. Сканирование само по себе не доказывает подлинность сервера. Доверять этому ключу?","\nCompare this fingerprint through an independent channel. Scanning alone does not authenticate a server. Trust this key?")+(replacement?t("\nПрежний доверенный ключ будет заменён.","\nThe previous pinned key will be replaced."):""),()->{try{c.trust(keys[0][0]);connect();}catch(Exception e){message("SSH",explain("TRUST_SAVE_FAILED"));}}));}
    private void closeAuxiliary(String id){for(ServerConnection c:auxiliary)if(c.profile.id.equals(id))c.close();}
    private void refresh(boolean explicit){
        if(selected==null||busy||closed)return;ServerProfiles.Profile p=selected;
        if(explicit){ServerConnection c=connection(p);run(()->c.rpc(new JSONObject().put("op","status")),r->{if(r.optInt("protocol")!=1){connected.remove(p.id);error=explain("PROTOCOL_MISMATCH");return;}snapshots.put(p.id,r);agentPresence.put(p.id,true);connected.add(p.id);});return;}
        refreshMonitor(p);
    }
    private void refreshMonitor(ServerProfiles.Profile p){
        if(pollBusy||closed||busy)return;pollBusy=true;final int g=generation,epoch=manualEpoch;ServerConnection owned=new ServerConnection(activity,p);auxiliary.add(owned);
        try{queues.monitor.execute(()->{JSONObject result=null;String failure=null;try{result=owned.rpc(new JSONObject().put("op","status"));}catch(Exception e){String code=e.getMessage();failure=code!=null&&code.matches("[A-Z_]{1,80}")?code:"OPERATION_FAILED";}finally{owned.close();auxiliary.remove(owned);}JSONObject data=result;String problem=failure;
            main.post(()->applyMonitor(p,g,epoch,data,problem));
        });}catch(RejectedExecutionException e){pollBusy=false;auxiliary.remove(owned);owned.close();}
    }
    void applyMonitor(ServerProfiles.Profile p,int g,int epoch,JSONObject data,String problem){
        pollBusy=false;if(closed||g!=generation||epoch!=manualEpoch||!store.profiles.contains(p)||!pollingEnabled(p))return;
        if(problem!=null){if(selected==p)monitorError(problem);else{TextView label=pollLabels.get(p.id);if(label!=null){updatePollLabel(p,label);label.append(" · "+t("Нет ответа","No response"));}}return;}if(data==null||data.optInt("protocol")!=1){if(selected==p)monitorError("PROTOCOL_MISMATCH");return;}
        snapshots.put(p.id,data);agentPresence.put(p.id,true);connected.add(p.id);if(selected==p)updateLiveViews(data);else{TextView label=pollLabels.get(p.id);if(label!=null)updatePollLabel(p,label);TextView metrics=listMetrics.get(p.id);if(metrics!=null)metrics.setText(metricSummary(data.optJSONObject("metrics")));}
    }
    private void install(){if(selected==null||busy)return;if(Boolean.TRUE.equals(knownAgent(selected))){message(t("Настройки агента","Agent settings"),t("Агент уже установлен. Повторная установка не требуется.","Agent is already installed. Reinstallation is not required."));return;}ServerConnection c=connection(selected);if(!c.trusted()){scan(false);return;}
        confirm(t("Установить Agent?","Install Agent?"),t("На выбранном сервере будет создан ~/.local/share/termux-jobs-server. Нужны Linux, Bash и Python 3.8+; пакеты и службы ОС не устанавливаются. Агент работает с правами SSH-пользователя (root = полный доступ), собирает метрики до 24 ч и хранит команды/журналы задач. Публичный порт не открывается. После перезагрузки стартует при подключении приложения. Удаление — отдельным пунктом; активные задачи его блокируют. Продолжить?","Creates ~/.local/share/termux-jobs-server on the selected server. Requires Linux, Bash, Python 3.8+; installs no OS packages/services. Runs with SSH-user permissions (root = full access), retains up to 24h metrics and task commands/logs. No public port. After server reboot, starts when the app reconnects. Separate removal is blocked by active tasks. Continue?"),()->run(()->{c.install();return new JSONObject();},r->{agentPresence.put(selected.id,true);connected.add(selected.id);refresh(true);}));
    }
    private void uninstall(){if(selected==null||busy)return;if(Boolean.FALSE.equals(knownAgent(selected))){message(t("Настройки агента","Agent settings"),t("Агент не установлен. Удалять нечего.","Agent is not installed. Nothing to remove."));return;}ServerConnection c=connection(selected);confirm(t("Удалить Agent?","Remove Agent?"),t("Будут удалены ТОЛЬКО каталог агента, история состояния, записи команд и журналы задач. Активные или неподконтрольные задачи заблокируют удаление. Просмотр видео остановится. Проекты, видео, SSH и другие службы не удаляются. Расписания Cron/systemd сохраняются и продолжат работать независимо; ими можно управлять в Расписаниях задач. Профиль сервера останется.","Deletes ONLY the agent directory, metric history, task records and logs. Active or unmanaged tasks block removal. Video streaming stops. Projects, videos, SSH and other services remain. Cron/systemd schedules remain active independently; manage them under Task schedules. The server profile is retained."),()->run(()->c.rpc(new JSONObject().put("op","uninstall")),r->{c.stopTunnel();snapshots.remove(selected.id);connected.remove(selected.id);agentPresence.put(selected.id,false);}));}
    private void openTerminal(){if(selected==null||busy)return;ServerConnection c=connection(selected);if(!c.trusted()){scan(false);return;}terminal.open(selected.name,c.terminalCommand(project==null?directory:project.path));}
    private void sshFiles(){if(selected==null||busy)return;WorkspaceFileBrowser browser=new WorkspaceFileBrowser(activity,selected.session(),directory,null);browser.setServerKnownHosts(connection(selected).knownHosts());browser.setOnClosed(()->sshBrowsers.remove(browser));sshBrowsers.add(browser);browser.show();}
    private void projects(LinearLayout body){add(body,button(t("+ Добавить проект","+ Add project"),this::addProject));for(ServerProfiles.Project item:selected.projects){LinearLayout c=card(body);title(c,item.name);add(c,text(item.path,12,WorkspaceDialogs.MUTED));add(c,button(t("Выбрать проект","Select project"),()->{project=item;directory=item.path;renderSection();}));add(c,button(t("Открыть файлы","Open files"),()->{project=item;directory=item.path;tab="files";listing=null;renderSection();loadFiles();}));c.setOnLongClickListener(v->{confirm(t("Убрать проект из списка?","Remove project from list?"),t("Файлы останутся на сервере.","Files remain on the server."),()->{selected.projects.remove(item);if(project==item)project=null;save();renderSection();});return true;});}}
    private void addProject(){LinearLayout f=column();EditText name=field(f,t("Название проекта","Project name"),"",false),path=field(f,t("Каталог на сервере","Server directory"),directory,false);AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(t("Проект","Project")).setView(f).setPositiveButton(t("Сохранить","Save"),null).setNegativeButton(t("Отмена","Cancel"),null).create();show(d);d.getButton(-1).setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()||path.getText().toString().trim().isEmpty())return;final ServerConnection c=connection(selected);final String n=name.getText().toString().trim(),value=path.getText().toString();d.dismiss();run(()->c.rpc(new JSONObject().put("op","list").put("path",value)),r->{ServerProfiles.Project item=new ServerProfiles.Project(n,r.getString("path"));selected.projects.add(item);project=item;directory=item.path;save();});});}
    private void save(){try{store.save();}catch(Exception e){error=explain("STORE_ERROR");}}
    private void tasks(LinearLayout body,JSONArray items){add(body,button(t("Закреплённые команды","Pinned commands"),this::pinnedActions));add(body,button(t("+ Запустить задачу","+ Run task"),this::startTask));add(body,button(t("Расписания задач","Task schedules"),()->{if(!busy&&selected!=null)scheduleUi().list();}));add(body,text(t("Команды выполняются на сервере без телефона. До 2 одновременно, очередь до 10, предел 24 часа. Не запускайте фоновые команды с & / nohup: их потомки не управляются как отдельные задачи.","Commands run on the server without the phone. Up to 2 running, 10 queued, 24-hour limit. Do not use & / nohup: detached descendants are not managed as separate tasks."),12,WorkspaceDialogs.MUTED));if(items==null)return;for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null||project!=null&&!project.path.equals(item.optString("directory")))continue;LinearLayout c=card(body);title(c,item.optString("title"));add(c,text(taskSummary(item)+" · "+item.optString("directory"),12,WorkspaceDialogs.MUTED));add(c,button(t("Журнал / действия","Log / actions"),()->taskLog(item)));}}
    private void pinnedActions(){
        if(selected==null||busy)return;ServerProfiles.Profile p=selected;LinearLayout body=column();ScrollView viewport=new ScrollView(activity);viewport.addView(body);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Закреплённые команды","Pinned commands")).setView(viewport).setPositiveButton(t("Закрыть","Close"),null).create();
        add(body,button(t("Добавить команду","Add command"),()->{dialog.dismiss();editAction(null,null);}));
        add(body,text(t("Нажатие — запуск с подтверждением. Долгий тап — изменить или удалить. Каждое выполнение создаёт отдельную задачу и журнал.","Tap to run with confirmation. Long press to edit or remove. Each run creates its own task and log."),12,WorkspaceDialogs.MUTED));
        for(ServerProfiles.Action action:new ArrayList<>(p.actions)){TextView item=button(action.name+"\n"+action.path,()->{if(selected!=p||busy)return;confirm(t("Запустить на сервере?","Run on server?"),p.user+"@"+p.host+":"+p.port+"\n"+action.path+"\n\n"+action.command+"\n\n"+t("Команда выполняется с правами SSH-пользователя. Проверьте её перед запуском.","Runs with the SSH user's permissions. Review the command before starting."),()->{if(selected!=p)return;dialog.dismiss();ServerConnection c=connection(p);run(()->c.rpc(new JSONObject().put("op","start").put("title",action.name).put("directory",action.path).put("command",action.command)),r->refresh(true));});});
            item.setOnLongClickListener(v->{AlertDialog choices=WorkspaceDialogs.builder(activity).setTitle(action.name).setItems(new String[]{t("Редактировать","Edit"),t("Убрать закрепление","Unpin")},(d,w)->{if(w==0){dialog.dismiss();editAction(action,null);}else confirm(t("Убрать закреплённую команду?","Unpin command?"),t("Уже запущенные задачи продолжат работу.","Already running tasks continue."),()->{int index=p.actions.indexOf(action);if(index<0)return;p.actions.remove(index);try{store.save();dialog.dismiss();}catch(Exception e){p.actions.add(index,action);message("Error",explain("STORE_ERROR"));}});}).create();show(choices);return true;});add(body,item);
        }show(dialog);
    }
    private void editAction(ServerProfiles.Action old,JSONObject task){
        if(selected==null||busy)return;ServerProfiles.Profile p=selected;LinearLayout body=column();
        EditText name=field(body,t("Название","Name"),old!=null?old.name:task==null?"":task.optString("title"),false),path=field(body,t("Каталог на сервере","Server directory"),old!=null?old.path:task==null?directory:task.optString("directory"),false),command=field(body,t("Команда Bash","Bash command"),old!=null?old.command:task==null?"":task.optString("command"),true);
        add(body,text(t("Команда сохраняется локально в профиле сервера. Не включайте пароли и токены в её текст.","The command is stored locally in the server profile. Avoid embedding passwords or tokens."),12,WorkspaceDialogs.MUTED));ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Закрепить команду","Pin command")).setView(scroll).setPositiveButton(t("Сохранить","Save"),null).setNegativeButton(t("Отмена","Cancel"),null).create();show(dialog);
        dialog.getButton(-1).setOnClickListener(v->{String n=name.getText().toString().trim(),cwd=path.getText().toString(),cmd=command.getText().toString();if(n.isEmpty()||n.length()>160||cwd.isEmpty()||cwd.length()>4096||cwd.indexOf('\0')>=0||cmd.trim().isEmpty()||cmd.length()>8192||cmd.indexOf('\0')>=0){command.setError(t("Проверьте название, каталог и команду (до 8192 символов).","Check name, directory and command (up to 8192 characters)."));return;}if(old==null&&p.actions.size()>=50){command.setError(t("До 50 закреплённых команд","Up to 50 pinned commands"));return;}int index=old==null?p.actions.size():p.actions.indexOf(old);if(index<0)return;ServerProfiles.Action next=new ServerProfiles.Action(n,cwd,cmd);if(old==null)p.actions.add(next);else p.actions.set(index,next);try{store.save();dialog.dismiss();}catch(Exception error){if(old==null)p.actions.remove(next);else p.actions.set(index,old);message("Error",explain("STORE_ERROR"));}});
    }
    private WorkspaceServerSchedules scheduleUi(){if(scheduleUi!=null)scheduleUi.close();scheduleUi=new WorkspaceServerSchedules(activity,connection(selected),worker);return scheduleUi;}
    private void startTask(){if(busy)return;LinearLayout f=column();EditText name=field(f,t("Название","Name"),"",false),cwd=field(f,t("Каталог на сервере","Server directory"),project==null?directory:project.path,false),command=field(f,t("Команда Bash","Bash command"),"",true);Spinner mode=new Spinner(activity);mode.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,new String[]{t("Сейчас","Now"),t("По расписанию","Scheduled")}));add(f,mode);ScrollView sv=new ScrollView(activity);sv.addView(f);AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(t("Новая задача","New task")).setView(sv).setPositiveButton(t("Продолжить","Continue"),null).setNegativeButton(t("Отмена","Cancel"),null).create();show(d);d.getButton(-1).setOnClickListener(v->{if(command.getText().toString().trim().isEmpty())return;String cmd=command.getText().toString(),path=cwd.getText().toString(),label=name.getText().toString();d.dismiss();if(mode.getSelectedItemPosition()==1){scheduleUi().create(label,path,cmd);return;}confirm(t("Запустить на сервере?","Run on server?"),selected.user+"@"+selected.host+"\n"+path+"\n\n"+cmd+"\n\n"+t("Команда получит права выбранного пользователя. Ввод с клавиатуры недоступен; для интерактивных задач используйте терминал. Команда и вывод сохраняются в каталоге агента.","Runs with the selected user's permissions. Keyboard input is unavailable; use terminal for interactive work. Command/output are saved in the agent directory."),()->{ServerConnection c=connection(selected);run(()->c.rpc(new JSONObject().put("op","start").put("title",label).put("directory",path).put("command",cmd)),r->refresh(true));});});}
    static boolean finishedTask(String state){return state.equals("done")||state.equals("failed")||state.equals("cancelled");}
    private String taskSummary(JSONObject item){String out=state(item.optString("state"));double started=item.optDouble("started",0),ended=item.optDouble("ended",System.currentTimeMillis()/1000.0);if(started>0)out+=" · "+Math.max(0,(long)(ended-started))+t(" с"," s");if(item.has("exitCode"))out+=" · exit "+item.optInt("exitCode");return out;}
    private void taskLog(JSONObject task){
        if(selected==null||busy)return;ServerConnection c=connection(selected);run(()->c.rpc(new JSONObject().put("op","log").put("id",task.getString("id"))),r->{
            JSONObject[] current={r.getJSONObject("task")};LinearLayout body=column();TextView log=text(r.optString("log"),12,WorkspaceDialogs.TEXT);log.setTypeface(Typeface.MONOSPACE);log.setTextIsSelectable(true);ScrollView viewport=new ScrollView(activity);viewport.addView(log);body.addView(viewport,new LinearLayout.LayoutParams(-1,dp(280)));
            TextView status=text(taskSummary(current[0])+"\n"+t("Последние 32 КБ вывода; полная запись до 2 МБ.","Last 32 KB shown; full log up to 2 MB."),12,WorkspaceDialogs.MUTED);add(body,status);
            add(body,button(t("Скопировать вывод","Copy output"),()->((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("task",log.getText()))));
            AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(current[0].optString("title")).setView(body).setPositiveButton(t("Закрыть","Close"),null).create();
            TextView stop=button(t("Остановить…","Stop…"),()->confirm(t("Остановить задачу?","Stop task?"),current[0].optString("command")+(current[0].optString("command").contains("apt-get")?t("\nОстановка APT может оставить пакеты недонастроенными.","\nStopping APT may leave packages unconfigured."):""),()->run(()->c.rpc(new JSONObject().put("op","cancel").put("id",current[0].getString("id"))),v->refresh(true))));add(body,stop);
            TextView remove=button(t("Убрать из списка…","Remove from list…"),()->{String state=current[0].optString("state");if(!finishedTask(state)&&!state.equals("unmanaged"))return;confirm(t("Удалить запись задачи?","Remove task record?"),state.equals("unmanaged")?t("Процесс неподконтролен. Проверьте его вручную: удаление записи НЕ останавливает процесс.","Process is unmanaged. Inspect it manually: removing its record does NOT stop it."):t("Удаляются только запись и журнал. Файлы и результат команды останутся на сервере.","Only the record and log are removed. Files and command results remain on the server."),()->run(()->c.rpc(new JSONObject().put("op","forget").put("id",current[0].getString("id"))),v->{dialog.dismiss();refresh(true);}));});add(body,remove);
            add(body,button(t("Закрепить команду…","Pin command…"),()->editAction(null,current[0])));
            Runnable actions=()->{String state=current[0].optString("state");stop.setVisibility(state.equals("running")||state.equals("starting")||state.equals("queued")?View.VISIBLE:View.GONE);remove.setVisibility(finishedTask(state)||state.equals("unmanaged")?View.VISIBLE:View.GONE);status.setText(taskSummary(current[0])+"\n"+t("Последние 32 КБ вывода; полная запись до 2 МБ.","Last 32 KB shown; full log up to 2 MB."));};actions.run();show(dialog);
            Runnable watch=new Runnable(){public void run(){if(closed||!dialog.isShowing())return;if(foreground&&!busy&&!pollBusy)WorkspaceServers.this.run(()->c.rpc(new JSONObject().put("op","log").put("id",task.getString("id"))),update->{if(dialog.isShowing()){current[0]=update.getJSONObject("task");updateLog(log,viewport,update.optString("log"));actions.run();}},false);main.postDelayed(this,3000);}};main.postDelayed(watch,3000);
        });
    }
    private void loadFiles(){
        if(busy||selected==null)return;ServerConnection c=connection(selected);String target=directory;
        if(fileStatus!=null){fileStatus.setText(t("Чтение каталога…","Reading directory…"));fileStatus.setTextColor(WorkspaceDialogs.MUTED);}
        runInline(()->c.rpc(new JSONObject().put("op","list").put("path",target)),r->{listing=r;directory=r.getString("path");updateFileViews();},code->{if(listing!=null&&listing.has("path"))directory=listing.optString("path");if(filePathLabel!=null)filePathLabel.setText(directory);if(fileStatus!=null){fileStatus.setText(explain(code));fileStatus.setContentDescription(explain(code));fileStatus.setTextColor(WorkspaceDialogs.DANGER);}});
    }
    private final LinkedHashMap<String,JSONObject> downloadSelection=new LinkedHashMap<>();
    private TextView downloadSelectedButton,deleteSelectedButton,clearFileSelectionButton,selectAllFilesButton;
    private LinearLayout fileSelectionActions;
    private boolean deleteSelectionMode;
    private final List<JSONObject> moveSelection=new ArrayList<>();private String moveProfile="";
    private String selectionProfile="",selectionDirectory="";
    private void updateDownloadSelection(){updateDeleteSelection();boolean selectionActive=!downloadSelection.isEmpty()||!moveSelection.isEmpty();if(fileSelectionActions!=null)fileSelectionActions.setVisibility(selectionActive?View.VISIBLE:View.GONE);if(selectAllFilesButton!=null)selectAllFilesButton.setEnabled(selectionActive&&!busy&&moveSelection.isEmpty()&&listing!=null);if(clearFileSelectionButton!=null){boolean active=!downloadSelection.isEmpty()||!moveSelection.isEmpty();clearFileSelectionButton.setVisibility(active?View.VISIBLE:View.GONE);clearFileSelectionButton.setText("× "+(downloadSelection.size()+moveSelection.size()));}if(downloadSelectedButton==null)return;boolean pending=selected!=null&&moveProfile.equals(selected.id)&&!moveSelection.isEmpty();downloadSelectedButton.setVisibility(View.VISIBLE);downloadSelectedButton.setSelected(pending||!downloadSelection.isEmpty());downloadSelectedButton.setBackground(WorkspaceDialogs.background(activity,pending||!downloadSelection.isEmpty()));downloadSelectedButton.setContentDescription(pending?t("Переместить сюда: ","Move here: ")+moveSelection.size():(downloadSelection.isEmpty()?t("Скачать с сервера / переместить","Download from server / move"):t("Действия с выбранным: ","Selection actions: ")+downloadSelection.size()));if(downloadSelectedButton instanceof WorkspaceIconButton)((WorkspaceIconButton)downloadSelectedButton).setIcon(activity.getDrawable(pending?R.drawable.workspace_move_here:R.drawable.workspace_diag_download).mutate(),dp(22));}
    private void updateDeleteSelection(){
        if(deleteSelectedButton==null)return;
        boolean active=deleteSelectionMode||!downloadSelection.isEmpty();deleteSelectedButton.setSelected(active);deleteSelectedButton.setBackground(WorkspaceDialogs.background(activity,active));
        String label=downloadSelection.isEmpty()?(deleteSelectionMode?t("Отменить выбор для удаления","Cancel delete selection"):t("Выбрать для удаления","Select for deletion")):t("Удалить выбранное: ","Delete selected: ")+downloadSelection.size();
        deleteSelectedButton.setContentDescription(label);deleteSelectedButton.setTooltipText(label);
    }
    private void deleteSelectionAction(){
        if(closed||busy||selected==null||listing==null)return;
        if(downloadSelection.isEmpty()){deleteSelectionMode=!deleteSelectionMode;updateFileViews();return;}
        final ServerProfiles.Profile profile=selected;final String folder=listing.optString("path");final JSONArray entries=new JSONArray();StringBuilder names=new StringBuilder();
        for(JSONObject item:downloadSelection.values())try{JSONObject copy=new JSONObject(item.toString());entries.put(copy);names.append(copy.optString("path")).append(copy.optBoolean("directory")?"/":"").append("\n");}catch(JSONException invalid){return;}
        ScrollView viewport=new ScrollView(activity);TextView body=text(profile.name+"\n\n"+names+t("\nУдаление с сервера безвозвратное. Папки удаляются со всем содержимым. Цели символических ссылок не удаляются. При обрыве уже удалённые объекты не восстанавливаются.","\nDeletion from the server is permanent. Folders include all contents. Symbolic-link targets are not deleted. Interrupted deletion does not restore already removed entries."),14,WorkspaceDialogs.TEXT);body.setTextIsSelectable(true);body.setPadding(dp(16),dp(8),dp(16),dp(8));viewport.addView(body);
        AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(t("Удалить выбранное с сервера?","Delete selection from server?"))
            .setView(viewport).setNegativeButton(t("Нет","No"),null).setPositiveButton(t("Да","Yes"),(d,w)->{
                if(closed||busy||selected!=profile||listing==null||!folder.equals(listing.optString("path"))||ServerConnection.disconnected(activity,profile.id))return;
                if(fileStatus!=null)fileStatus.setText(t("Удаление…","Deleting…"));
                ServerConnection c=connection(profile);
                runInline(()->c.deleteFiles(entries),result->{
                    downloadSelection.clear();deleteSelectionMode=false;
                    if(moveProfile.equals(profile.id)){moveSelection.clear();moveProfile="";}
                    updateDownloadSelection();loadFiles();
                    String code=result.optString("error");String summary=t("Удалено выбранных объектов: ","Deleted selected entries: ")+result.getJSONArray("deleted").length()+" / "+entries.length();
                    if(!code.isEmpty())summary+="\n"+t("Удаление не завершено: ","Deletion incomplete: ")+code+t("\nЧасть содержимого могла быть удалена. Обновите список перед повтором.","\nSome contents may already be deleted. Refresh before retrying.");
                    message(t("Удаление с сервера","Server deletion"),summary);
                },code->{downloadSelection.clear();deleteSelectionMode=false;updateDownloadSelection();loadFiles();message(t("Удаление не подтверждено","Deletion not confirmed"),t("Проверьте список: часть объектов могла быть удалена. Автоматический повтор не выполняется.","Check the listing: some entries may have been deleted. No automatic retry.")+"\n"+code);});
            }).create();show(confirm);
    }
    private void selectedFileActions(){
        if(selected==null||busy)return;
        boolean pending=moveProfile.equals(selected.id)&&!moveSelection.isEmpty();
        if(pending){WorkspaceDialogs.builder(activity).setTitle(t("Перемещение","Move")).setItems(new String[]{t("Переместить сюда","Move here"),t("Отменить перемещение","Cancel move")},(d,w)->{if(w==0){ServerProfiles.Profile p=selected;downloads.move(p,new ArrayList<>(moveSelection),listing==null?directory:listing.optString("path",directory),()->{moveSelection.clear();moveProfile="";downloadSelection.clear();if(selected==p)loadFiles();updateDownloadSelection();});}else{moveSelection.clear();moveProfile="";updateDownloadSelection();}}).show();return;}
        if(downloadSelection.isEmpty()){WorkspaceDialogs.builder(activity).setTitle(t("Файлы сервера","Server files")).setItems(new String[]{t("Скачать с сервера…","Download from server…"),t("Переместить по серверу…","Move within server…")},(d,w)->chooseServerSelection(w==1)).show();return;}
        WorkspaceDialogs.builder(activity).setTitle(t("Выбрано: ","Selected: ")+downloadSelection.size()).setItems(new String[]{t("Скачать…","Download…"),t("Переместить…","Move…"),t("Выбрать всё","Select all"),t("Снять выделение","Clear selection")},(d,w)->{if(w==0)downloads.selected(selected,new ArrayList<>(downloadSelection.values()));else if(w==1){prepareMoveSelection();}else if(w==2){selectAllCurrentFiles();}else{downloadSelection.clear();deleteSelectionMode=false;updateFileViews();}}).show();
    }
    private void selectAllCurrentFiles(){if(busy||selected==null||listing==null||downloadSelection.isEmpty()||!moveSelection.isEmpty())return;JSONArray entries=listing.optJSONArray("entries");boolean limited=false;if(entries!=null)for(int i=0;i<entries.length();i++){JSONObject e=entries.optJSONObject(i);if(e==null||downloadSelection.containsKey(e.optString("path")))continue;if(downloadSelection.size()>=500){limited=true;break;}downloadSelection.put(e.optString("path"),e);}updateFileViews();if(limited)activity.showToast(t("Достигнут лимит: 500 выбранных объектов","Selection limit reached: 500 entries"),false);}
    private void prepareMoveSelection(){moveSelection.clear();moveSelection.addAll(downloadSelection.values());moveProfile=selected.id;downloadSelection.clear();updateFileViews();activity.showToast(t("Откройте папку назначения и нажмите «Переместить сюда»","Open the destination folder and tap Move here"),true);}
    private void chooseServerSelection(boolean move){
        if(selected==null||listing==null)return;JSONArray array=listing.optJSONArray("entries");if(array==null||array.length()==0){message(t("Файлы сервера","Server files"),t("В этой папке нет доступных записей. Откройте другую папку.","No available entries in this folder. Open another directory."));return;}
        ServerProfiles.Profile profile=selected;List<JSONObject> entries=new ArrayList<>();for(int i=0;i<array.length()&&entries.size()<500;i++)if(array.optJSONObject(i)!=null)entries.add(array.optJSONObject(i));String[] names=new String[entries.size()];boolean[] checked=new boolean[entries.size()];for(int i=0;i<names.length;i++){JSONObject e=entries.get(i);names[i]=e.optString("name")+(e.optBoolean("directory")?"/":"");}
        AlertDialog[] holder={null};Runnable enabled=()->{boolean any=false;for(boolean value:checked)any|=value;if(holder[0]!=null)holder[0].getButton(-1).setEnabled(any);};
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(move?t("Что переместить?","What to move?"):t("Что скачать?","What to download?"))
            .setMultiChoiceItems(names,checked,(d,w,value)->{checked[w]=value;enabled.run();}).setPositiveButton(t("Далее","Next"),null).setNegativeButton(t("Назад","Back"),null).setNeutralButton(t("Выбрать всё","Select all"),null).create();holder[0]=dialog;show(dialog);enabled.run();
        dialog.getButton(-3).setOnClickListener(v->{for(int i=0;i<checked.length;i++){checked[i]=true;dialog.getListView().setItemChecked(i,true);}enabled.run();});
        dialog.getButton(-1).setOnClickListener(v->{if(selected!=profile)return;downloadSelection.clear();for(int i=0;i<checked.length;i++)if(checked[i])downloadSelection.put(entries.get(i).optString("path"),entries.get(i));dialog.dismiss();updateFileViews();if(move)prepareMoveSelection();else downloads.selected(profile,new ArrayList<>(downloadSelection.values()));});
    }
    private void toggleDownloadSelection(JSONObject entry){String path=entry.optString("path");if(downloadSelection.containsKey(path))downloadSelection.remove(path);else if(downloadSelection.size()<500)downloadSelection.put(path,entry);else activity.showToast(t("До 500 выбранных записей","Up to 500 selected entries"),false);if(downloadSelection.isEmpty())deleteSelectionMode=false;updateFileViews();}
    private void files(LinearLayout body){filePathLabel=text(directory,13,WorkspaceDialogs.MUTED);add(body,filePathLabel);LinearLayout controls=row();
        control(controls,iconButton(t("На уровень выше","Parent directory"),R.drawable.workspace_server_back,()->{if(busy)return;directory=listing==null?"~":listing.optString("parent","~");loadFiles();}));
        control(controls,iconButton(t("Домашний каталог сервера","Server home directory"),R.drawable.workspace_home,()->{if(busy)return;directory="~";loadFiles();}));
        deleteSelectedButton=iconButton(t("Выбрать для удаления","Select for deletion"),R.drawable.workspace_delete,this::deleteSelectionAction);deleteSelectedButton.setTag("server-delete-selection");control(controls,deleteSelectedButton);
        control(controls,iconButton(t("Путь…","Path…"),R.drawable.workspace_folder,this::pathDialog));
        control(controls,iconButton(t("Обновить","Refresh"),R.drawable.workspace_server_refresh,this::loadFiles));
        favoriteButton=iconButton(t("Избранные папки и файлы","Favorite folders and files"),R.drawable.workspace_star,this::favoriteFolders);control(controls,favoriteButton);
        control(controls,iconButton(t("Поиск по именам","Search names"),R.drawable.workspace_search,this::searchFiles));
        HorizontalScrollView toolbar=new HorizontalScrollView(activity);toolbar.setHorizontalScrollBarEnabled(false);toolbar.addView(controls);add(body,toolbar);
        fileStatus=text("",11,WorkspaceDialogs.MUTED);fileStatus.setSingleLine(true);fileStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);fileStatus.setMinHeight(dp(18));add(body,fileStatus);
        LinearLayout transfers=row();TextView upload=iconButton(t("Загрузить на сервер","Upload to server"),R.drawable.workspace_server_upload,()->{if(!busy&&selected!=null)uploads.pick(selected,listing==null?null:listing.optString("path"));});LinearLayout.LayoutParams uploadParams=new LinearLayout.LayoutParams(0,dp(48),1);uploadParams.setMarginEnd(dp(8));transfers.addView(upload,uploadParams);
        downloadSelectedButton=iconButton(t("Скачать с сервера / переместить","Download from server / move"),R.drawable.workspace_diag_download,this::selectedFileActions);transfers.addView(downloadSelectedButton,new LinearLayout.LayoutParams(0,dp(48),1));clearFileSelectionButton=button("",()->{if(busy)return;downloadSelection.clear();deleteSelectionMode=false;moveSelection.clear();moveProfile="";updateFileViews();});clearFileSelectionButton.setContentDescription(t("Снять всё выделение","Clear selection"));clearFileSelectionButton.setTag("server-clear-selection");add(body,transfers);
        fileSelectionActions=row();fileSelectionActions.setTag("server-selection-actions");LinearLayout.LayoutParams clearParams=new LinearLayout.LayoutParams(-2,dp(48));clearParams.setMarginEnd(dp(8));fileSelectionActions.addView(clearFileSelectionButton,clearParams);
        selectAllFilesButton=iconButton(t("Выбрать всё в открытой папке","Select all in current folder"),R.drawable.workspace_select_all,this::selectAllCurrentFiles);selectAllFilesButton.setTag("server-select-all");fileSelectionActions.addView(selectAllFilesButton,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout.LayoutParams selectionParams=new LinearLayout.LayoutParams(-2,-2);selectionParams.setMarginStart(dp(8));transfers.addView(fileSelectionActions,selectionParams);updateDownloadSelection();
        fileRows=column();body.addView(fileRows);updateFileViews();
        add(body,text(t("До 500 записей. Для остальных откройте вложенную папку или SSH-терминал.","Up to 500 entries. For others use a subfolder or SSH terminal."),12,WorkspaceDialogs.MUTED));
    }
    private void updateFileViews(){
        if(fileRows==null||listing==null||!listing.has("entries"))return;
        preserveViewport(this::replaceFileRows);
    }
    private void replaceFileRows(){
        if(sectionBody!=null)sectionBody.setMinimumHeight(0);
        String owner=selected==null?"":selected.id,currentDirectory=listing.optString("path",directory);if(!selectionProfile.equals(owner)){downloadSelection.clear();deleteSelectionMode=false;moveSelection.clear();moveProfile="";selectionProfile=owner;}selectionDirectory=currentDirectory;updateDownloadSelection();
        if(filePathLabel!=null)filePathLabel.setText(listing.optString("path",directory));if(favoriteButton!=null){boolean starred=selected!=null&&selected.favorites.contains(listing.optString("path"));favoriteButton.setSelected(starred);favoriteButton.setBackground(WorkspaceDialogs.background(activity,starred));}fileRows.removeAllViews();JSONArray entries=listing.optJSONArray("entries");if(entries==null)return;
        for(int i=0;i<entries.length();i++){JSONObject entry=entries.optJSONObject(i);if(entry!=null)add(fileRows,fileRow(entry));}
        if(fileStatus!=null){fileStatus.setText(t("Записей: ","Entries: ")+entries.length());fileStatus.setTextColor(WorkspaceDialogs.MUTED);fileStatus.setContentDescription(fileStatus.getText());}
    }
    private View fileRow(JSONObject entry){
        LinearLayout item=row();boolean checked=downloadSelection.containsKey(entry.optString("path"));item.setBackground(WorkspaceDialogs.background(activity,checked));item.setPadding(dp(14),dp(10),dp(14),dp(10));item.setMinimumHeight(dp(48));
        if(deleteSelectionMode||!downloadSelection.isEmpty()){CheckBox check=new CheckBox(activity);check.setChecked(checked);check.setContentDescription(t("Выбрать ","Select ")+entry.optString("name"));check.setOnClickListener(v->{if(!busy)toggleDownloadSelection(entry);});item.addView(check,new LinearLayout.LayoutParams(dp(44),dp(44)));}
        boolean folder=entry.optBoolean("directory");android.widget.ImageView icon=new android.widget.ImageView(activity);icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);icon.setImageResource(folder?R.drawable.workspace_folder:R.drawable.workspace_server_file);icon.setColorFilter(WorkspaceDialogs.ACCENT);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(22),dp(22));iconParams.setMarginEnd(dp(12));item.addView(icon,iconParams);
        TextView name=text(entry.optString("name"),14,WorkspaceDialogs.TEXT);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);item.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        if(folder){android.widget.ImageView next=new android.widget.ImageView(activity);next.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);next.setImageResource(R.drawable.workspace_next);next.setColorFilter(WorkspaceDialogs.MUTED);item.addView(next,new LinearLayout.LayoutParams(dp(18),dp(18)));}else{TextView bytes=text(ServerFileInfo.size(entry.optLong("size"),WorkspaceLanguage.code(activity).equals("ru")),11,WorkspaceDialogs.MUTED);bytes.setPadding(dp(8),0,0,0);item.addView(bytes);}
        item.setContentDescription(entry.optString("name")+(folder?t(" · Папка"," · Folder"):""));item.setOnLongClickListener(v->{if(!busy)toggleDownloadSelection(entry);return true;});item.setOnClickListener(v->{if(busy)return;if(folder){directory=entry.optString("path");loadFiles();}else fileMenu(entry);});return item;
    }
    private void favoriteFolders(){
        if(selected==null||busy)return;final ServerProfiles.Profile p=selected;String current=listing==null?null:listing.optString("path");
        LinearLayout body=column();ScrollView viewport=new ScrollView(activity);viewport.addView(body);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Избранные папки и файлы","Favorite folders and files")).setView(viewport).setPositiveButton(t("Закрыть","Close"),null).create();
        if(current!=null&&current.startsWith("/")){boolean exists=p.favorites.contains(current);add(body,button(exists?t("Убрать текущую папку из избранного","Remove current folder from favorites"):t("Добавить текущую папку","Add current folder"),()->{
            if(selected!=p)return;try{if(exists)p.favorites.remove(current);else{if(p.favorites.size()>=100)throw new IOException("FAVORITES_LIMIT");p.favorites.add(current);}store.save();dialog.dismiss();updateFileViews();}catch(Exception error){if(exists){if(!p.favorites.contains(current))p.favorites.add(current);}else p.favorites.remove(current);message(t("Ошибка","Error"),t("Не удалось сохранить избранное. Предел: 100 папок.","Could not save favorites. Limit: 100 folders."));}
        }));}
        if(!p.favorites.isEmpty())add(body,text(t("Папки","Folders"),13,WorkspaceDialogs.MUTED));
        for(String path:new ArrayList<>(p.favorites)){TextView item=button(path,()->{dialog.dismiss();if(selected==p&&!busy){directory=path;loadFiles();}});item.setOnLongClickListener(v->{confirm(t("Убрать из избранного?","Remove favorite?"),path,()->{try{p.favorites.remove(path);store.save();dialog.dismiss();if(selected==p)updateFileViews();}catch(Exception e){if(!p.favorites.contains(path))p.favorites.add(path);message("Error",explain("STORE_ERROR"));}});return true;});add(body,item);}
        if(!p.favoriteFiles.isEmpty())add(body,text(t("Файлы","Files"),13,WorkspaceDialogs.MUTED));
        for(String path:new ArrayList<>(p.favoriteFiles)){TextView item=button(path,()->{dialog.dismiss();if(selected==p&&!busy){try{fileMenu(new JSONObject().put("name",new File(path).getName()).put("path",path));}catch(Exception ignored){}}});
            item.setOnLongClickListener(v->{confirm(t("Убрать из избранного?","Remove favorite?"),path,()->{int index=p.favoriteFiles.indexOf(path);if(index<0)return;p.favoriteFiles.remove(index);try{store.save();dialog.dismiss();}catch(Exception e){p.favoriteFiles.add(index,path);message("Error",explain("STORE_ERROR"));}});return true;});add(body,item);}
        if(p.favorites.isEmpty()&&p.favoriteFiles.isEmpty())add(body,text(t("Папки добавляются кнопкой выше, файлы — через меню выбранного файла.","Add folders using the button above; add files through the selected file's menu."),13,WorkspaceDialogs.MUTED));show(dialog);
    }
    private void toggleFileFavorite(JSONObject entry){
        if(selected==null||busy)return;ServerProfiles.Profile p=selected;String path=entry.optString("path");if(!path.startsWith("/")||path.length()>4096||path.indexOf('\0')>=0)return;
        boolean existed=p.favoriteFiles.contains(path);int index=p.favoriteFiles.indexOf(path);
        if(!existed&&p.favoriteFiles.size()>=100){message(t("Избранное","Favorites"),t("До 100 файлов на сервер.","Up to 100 files per server."));return;}
        if(existed)p.favoriteFiles.remove(path);else p.favoriteFiles.add(path);
        try{store.save();activity.showToast(existed?t("Убрано из избранного","Removed from favorites"):t("Добавлено в избранное","Added to favorites"),false);}catch(Exception e){if(existed)p.favoriteFiles.add(index,path);else p.favoriteFiles.remove(path);message("Error",explain("STORE_ERROR"));}
    }

    private void searchFiles(){
        if(selected==null||busy)return;final ServerProfiles.Profile p=selected;final String base=listing==null?directory:listing.optString("path",directory);
        LinearLayout body=column();EditText query=field(body,t("Имя файла или папки","File or folder name"),"",false);
        add(body,text(base+"\n"+t("В выбранном каталоге и подпапках, без перехода по ссылкам. До 20 000 записей / 8 секунд / 500 результатов. Содержимое файлов не читается.","Current directory and subfolders, without following links. Up to 20,000 entries / 8 seconds / 500 results. File contents are not read."),12,WorkspaceDialogs.MUTED));
        TextView state=text("",12,WorkspaceDialogs.MUTED);add(body,state);LinearLayout rows=column();ScrollView viewport=new ScrollView(activity);viewport.addView(rows);body.addView(viewport,new LinearLayout.LayoutParams(-1,dp(280)));
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Поиск на сервере","Search server")).setView(body).setPositiveButton(t("Найти","Search"),null).setNegativeButton(t("Закрыть","Close"),null).create();
        ServerConnection[] owned={null};Future<?>[] future={null};boolean[] running={false};show(dialog);
        dialog.setOnDismissListener(d->{dialogs=Math.max(0,dialogs-1);ownedDialogs.remove(dialog);queues.cancelHelper(future[0]);ServerConnection c=owned[0];owned[0]=null;if(c!=null){c.close();auxiliary.remove(c);}});
        dialog.getButton(-1).setOnClickListener(v->{String value=query.getText().toString().trim();if(running[0]||value.isEmpty())return;if(value.length()>200){query.setError(t("До 200 символов","Up to 200 characters"));return;}
            rows.removeAllViews();running[0]=true;query.setEnabled(false);dialog.getButton(-1).setEnabled(false);dialog.getButton(-2).setText(t("Остановить / закрыть","Stop / close"));state.setText(t("Поиск…","Searching…"));ServerConnection c=new ServerConnection(activity,p);owned[0]=c;auxiliary.add(c);
            try{future[0]=queues.helper(()->{JSONObject result=null;try{result=c.search(base,value);}catch(Exception ignored){}finally{c.close();auxiliary.remove(c);}JSONObject found=result;main.post(()->{if(closed||!dialog.isShowing()||owned[0]!=c)return;owned[0]=null;running[0]=false;query.setEnabled(true);dialog.getButton(-1).setEnabled(true);dialog.getButton(-2).setText(t("Закрыть","Close"));if(found==null){state.setText(t("Поиск не завершён. Проверьте подключение, Python и права на каталог.","Search failed. Check connectivity, Python and directory permissions."));return;}JSONArray entries=found.optJSONArray("entries");state.setText(t("Найдено: ","Found: ")+(entries==null?0:entries.length())+" · "+t("Просмотрено: ","Scanned: ")+found.optInt("scanned")+" · "+t("Пропущено: ","Skipped: ")+found.optInt("skipped")+(found.optBoolean("partial")?"\n"+t("Результат неполный — сузьте каталог или запрос.","Partial results — narrow the directory or query."):""));
                if(entries!=null)for(int i=0;i<entries.length();i++){JSONObject entry=entries.optJSONObject(i);if(entry==null)continue;TextView item=button(entry.optString("path"),()->{if(selected!=p||busy)return;dialog.dismiss();if(entry.optBoolean("directory")){directory=entry.optString("path");loadFiles();}else fileMenu(entry);});add(rows,item);}
            });});}catch(RejectedExecutionException e){c.close();auxiliary.remove(c);owned[0]=null;running[0]=false;query.setEnabled(true);dialog.getButton(-1).setEnabled(true);dialog.getButton(-2).setText(t("Закрыть","Close"));state.setText(t("Все фоновые проверки заняты. Повторите позже.","Background checks are busy. Try again later."));}
        });
    }
    private void updateStreamPanel(){
        if(streamPanel==null||selected==null)return;ServerConnection c=connections.get(selected.id);boolean active=c!=null&&c.tunnelActive();
        if(!active){streamNames.remove(selected.id);if(streamPanel.getChildCount()>0)streamPanel.removeAllViews();streamPanel.setVisibility(View.GONE);return;}
        String tag=selected.id+"\n"+streamNames.getOrDefault(selected.id,"");if(streamPanel.getChildCount()>0&&tag.equals(streamPanel.getTag()))return;streamPanel.removeAllViews();streamPanel.setTag(tag);streamPanel.setVisibility(View.VISIBLE);LinearLayout line=row();line.setBackground(shape(0xff0b1319));line.setPadding(dp(10),dp(4),dp(6),dp(4));
        TextView label=text(t("Просмотр: ","Playing: ")+streamNames.getOrDefault(selected.id,t("Медиа","Media")),12,WorkspaceDialogs.TEXT);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);line.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView stop=button(t("Остановить","Stop"),()->{c.stopTunnel();streamNames.remove(selected.id);streamPanel.removeAllViews();updateStreamPanel();});line.addView(stop);streamPanel.addView(line,new LinearLayout.LayoutParams(-1,-2));
    }
    private void components(){if(busy||selected==null)return;ServerProfiles.Profile p=selected;ServerConnection c=connection(p);run(c::components,status->{LinearLayout body=column();body.setPadding(dp(16),0,dp(16),0);title(body,"FFmpeg / ffprobe");add(body,text("FFmpeg: "+(status.optBoolean("ffmpeg")?t("Установлен","Installed"):t("Не установлен","Not installed"))+"\nffprobe: "+(status.optBoolean("ffprobe")?t("Установлен","Installed"):t("Не установлен","Not installed")),14,WorkspaceDialogs.TEXT));add(body,text(t("ffprobe читает длительность, разрешение и кодеки. FFmpeg также нужен для обработки медиа. Сам просмотр файлов через SSH не требует установки этих пакетов.","ffprobe reads duration, resolution and codecs. FFmpeg also processes media. Streaming files over SSH does not require these packages."),13,WorkspaceDialogs.MUTED));
            TextView install=button(t("Установить FFmpeg на сервер…","Install FFmpeg on server…"),()->installFfmpeg(p,status));install.setEnabled(!status.optBoolean("ffprobe")||!status.optBoolean("ffmpeg"));add(body,install);AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(t("Компоненты агента","Agent components")).setView(body).setPositiveButton(t("Закрыть","Close"),null).create();show(d);install.setOnClickListener(v->{d.dismiss();installFfmpeg(p,status);});});}
    private void installFfmpeg(ServerProfiles.Profile p,JSONObject status){
        if(selected!=p||busy)return;if(!status.optBoolean("agent")){message(t("Нужен Agent","Agent required"),t("Сначала установите агент через меню сервера. Установка пакета будет отдельной задачей с журналом.","Install the agent from the server menu first. Package installation will be a separate logged task."));return;}
        if(!status.optBoolean("apt")){message(t("Установка недоступна","Installation unavailable"),t("Автоустановка пока поддерживает только серверы с APT. На других системах установите FFmpeg вручную через SSH.","Automatic installation currently supports APT servers only. Install FFmpeg manually over SSH on other systems."));return;}
        boolean root=status.optInt("uid",-1)==0;if(!root&&!status.optBoolean("sudo")){message(t("Нужны права","Permissions required"),t("Для APT нужны root или настроенный sudo. Выполните установку вручную в SSH-терминале.","APT needs root or configured sudo. Install manually in the SSH terminal."));return;}
        String command=ServerFileInfo.installCommand(root);confirm(t("Установить FFmpeg на сервер?","Install FFmpeg on server?"),p.user+"@"+p.host+"\n\n"+command+"\n\n"+t("Будут обновлены индексы APT и установлены FFmpeg, ffprobe и зависимости на выбранном сервере. Нужны место, сеть и административные права. sudo выполняется без запроса пароля; если права не настроены, задача завершится ошибкой — используйте SSH-терминал. Пакеты автоматически не удаляются вместе с агентом. Предпочтительно дождаться окончания установки, не прерывая APT.","Updates APT indexes and installs FFmpeg, ffprobe and dependencies on the selected server. Requires disk space, network and administrative permissions. sudo will not prompt; without configured permissions the task fails — use SSH terminal. Packages are not removed with the agent. Prefer waiting for installation to finish without interrupting APT."),()->{ServerConnection c=connection(p);run(()->c.rpc(new JSONObject().put("op","start").put("title","FFmpeg / ffprobe").put("directory",status.optString("home","~")).put("command",command)),r->{project=null;tab="tasks";refresh(true);});});
    }
    private void showProperties(JSONObject entry){
        if(busy||selected==null)return;ServerConnection c=connection(selected);ServerProfiles.Profile p=selected;String path=entry.optString("path");
        TextView info=text(t("Чтение свойств…","Reading properties…"),14,WorkspaceDialogs.TEXT);info.setTextIsSelectable(true);info.setPadding(dp(16),dp(8),dp(16),dp(8));info.setMinHeight(dp(120));ScrollView viewport=new ScrollView(activity);viewport.addView(info);
        AlertDialog.Builder builder=WorkspaceDialogs.builder(activity).setTitle(t("Свойства","Properties")).setView(viewport).setPositiveButton(t("Закрыть","Close"),null);
        if(ServerFileInfo.media(path))builder.setNeutralButton(t("Установить FFmpeg…","Install FFmpeg…"),null);
        AlertDialog dialog=builder.create();show(dialog);if(dialog.getButton(AlertDialog.BUTTON_NEUTRAL)!=null)dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
        runInline(()->c.properties(path),data->{if(!dialog.isShowing())return;info.setText(ServerFileInfo.properties(data,WorkspaceLanguage.code(activity).equals("ru"),TimeZone.getDefault()));JSONObject components=data.optJSONObject("components");if(ServerFileInfo.media(path)&&components!=null&&!components.optBoolean("ffprobe")){TextView install=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);install.setVisibility(View.VISIBLE);install.setOnClickListener(v->{dialog.dismiss();installFfmpeg(p,components);});}},code->{if(dialog.isShowing())info.setText(explain(code));});
    }
    private void pathDialog(){if(busy)return;LinearLayout f=column();EditText input=field(f,t("Каталог на сервере","Server directory"),directory,false);AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(t("Открыть каталог","Open directory")).setView(f).setPositiveButton(t("Открыть","Open"),(x,w)->{directory=input.getText().toString();loadFiles();}).setNegativeButton(t("Отмена","Cancel"),null).create();show(d);}
    private void fileMenu(JSONObject entry){if(busy)return;String path=entry.optString("path");String[] labels={t("Посмотреть","View"),t("Свойства / медиаинформация","Properties / media info"),t("Скачать в Download","Download to Download"),t("Скачать в другую папку…","Download to another folder…"),t("Открыть видео / аудио","Open video / audio"),t("Переименовать…","Rename…"),t("Удалить файл…","Delete file…"),selected.favoriteFiles.contains(path)?t("Убрать из избранного","Remove from favorites"):t("В избранное","Add to favorites")};AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(entry.optString("name")).setItems(labels,(x,w)->{ServerConnection c=connection(selected);if(w==7)toggleFileFavorite(entry);if(w==0)viewText(entry);if(w==1)showProperties(entry);if(w==2)download(entry);if(w==3)downloadTo(entry);if(w==4)stream(entry);if(w==5){LinearLayout f=column();EditText name=field(f,t("Новое имя","New name"),entry.optString("name"),false);AlertDialog rename=WorkspaceDialogs.builder(activity).setTitle(t("Переименовать","Rename")).setView(f).setPositiveButton(t("Сохранить","Save"),(v,which)->run(()->c.rpc(new JSONObject().put("op","rename").put("path",path).put("name",name.getText().toString())),r->loadFiles())).setNegativeButton(t("Отмена","Cancel"),null).create();show(rename);}if(w==6)confirm(t("Удалить файл с сервера?","Delete file from server?"),path,()->run(()->c.rpc(new JSONObject().put("op","delete").put("path",path)),r->loadFiles()));}).create();show(d);}
    private void download(JSONObject entry){if(selected==null||busy)return;downloads.quick(selected,entry);}
    private void downloadTo(JSONObject entry){if(selected==null||busy)return;ServerConnection c=connection(selected);if(!c.trusted()){scan(false);return;}downloads.start(selected,entry);}
    private void closePlayer(){WorkspaceServerPlayer owned=player;player=null;playerProfile="";if(owned!=null)owned.close();}
    private void externalMedia(ServerConnection c,String id,String url,String mime){try{activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url),mime));}catch(ActivityNotFoundException e){c.stopTunnel();streamNames.remove(id);updateStreamPanel();message(t("Нет плеера","No player"),t("Скачайте файл для просмотра.","Download the file to view it."));}}
    private void stream(JSONObject entry){if(selected==null||busy)return;closePlayer();final ServerProfiles.Profile p=selected;ServerConnection c=connection(p);
        runInline(()->{JSONObject r=c.rpc(new JSONObject().put("op","stream").put("path",entry.getString("path")));String token=r.getString("token");if(!token.matches("[a-f0-9]{64}"))throw new IOException("INVALID_TOKEN");int[] port={0};c.openTunnel(r.getInt("port"),port);return new JSONObject().put("url","http://127.0.0.1:"+port[0]+"/"+token).put("mime",r.optString("mime","application/octet-stream"));},r->{if(!foreground||!visible||selected!=p){c.stopTunnel();return;}streamNames.put(p.id,entry.optString("name"));updateStreamPanel();String url=r.getString("url"),mime=r.getString("mime");try{playerProfile=p.id;player=new WorkspaceServerPlayer(activity,entry.optString("name"),url,()->{c.stopTunnel();streamNames.remove(p.id);updateStreamPanel();},()->externalMedia(c,p.id,url,mime),()->{if(selected==p)download(entry);});}catch(RuntimeException e){c.stopTunnel();streamNames.remove(p.id);updateStreamPanel();message(t("Ошибка плеера","Player error"),t("Не удалось открыть плеер. Файл можно скачать.","Could not open player. You can download the file."));}},code->{c.stopTunnel();message(t("Ошибка","Error"),explain(code));},c::stopTunnel);
    }
    private void viewText(JSONObject entry){
        if(selected==null||busy)return;ProjectFiles files=new ProjectFiles(activity,selected.session());files.setServerKnownHosts(connection(selected).knownHosts());textFiles.add(files);
        TextView text=text(t("Чтение файла…","Reading file…"),14,WorkspaceDialogs.TEXT);text.setTypeface(Typeface.MONOSPACE);text.setTextIsSelectable(true);text.setSaveEnabled(false);ScrollView viewport=new ScrollView(activity);viewport.addView(text);viewport.setMinimumHeight(dp(240));
        AlertDialog viewer=WorkspaceDialogs.builder(activity).setTitle(entry.optString("name")).setView(viewport).setPositiveButton(t("Закрыть","Close"),null).setNeutralButton(t("Редактировать","Edit"),null).create();show(viewer);viewer.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        boolean[] editing={false};viewer.setOnDismissListener(d->{dialogs=Math.max(0,dialogs-1);ownedDialogs.remove(viewer);if(!editing[0]){files.close();textFiles.remove(files);}});
        String path=entry.optString("path");java.io.File f=new java.io.File(path);ProjectFiles.Entry file=new ProjectFiles.Entry(f.getParent()==null?"/":f.getParent(),f.getName(),false,entry.optLong("size"));
        runInline(()->{TextFileEdits.Snapshot snapshot=files.editRead(file);return new JSONObject().put("path",snapshot.path).put("revision",snapshot.revision).put("text",snapshot.text);},r->{if(!viewer.isShowing())return;TextFileEdits.Snapshot snapshot=new TextFileEdits.Snapshot(r.getString("path"),r.getString("revision"),r.getString("text"));text.setText(snapshot.text);viewer.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);viewer.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{editing[0]=true;viewer.dismiss();WorkspaceTextEditor[] holder=new WorkspaceTextEditor[1];WorkspaceTextEditor editor=new WorkspaceTextEditor(activity,files,worker,snapshot,()->{if(!closed&&visible&&selected!=null&&!ServerConnection.disconnected(activity,selected.id)&&tab.equals("files"))loadFiles();},()->{files.close();textFiles.remove(files);editors.remove(holder[0]);});holder[0]=editor;editors.add(editor);editor.show();});},code->{if(viewer.isShowing())text.setText(t("Не удалось открыть. Нужен обычный файл UTF-8 до 256 КБ, без символьных и жёстких ссылок.","Unable to open. Requires a regular UTF-8 file up to 256 KB, without symbolic or hard links."));});
    }
    private void services(){if(busy||selected==null)return;ServerConnection c=connection(selected);runInline(()->c.rpc(new JSONObject().put("op","services")),r->{listing=r;if(serviceText!=null){String value=r.optBoolean("available")?r.optString("text"):explain("SERVICE_PERMISSION_OR_UNAVAILABLE");preserveViewport(()->{if(sectionBody!=null)sectionBody.setMinimumHeight(0);if(!value.contentEquals(serviceText.getText()))serviceText.setText(value);});}},code->{if(serviceText!=null)preserveViewport(()->{if(sectionBody!=null)sectionBody.setMinimumHeight(0);serviceText.setText(explain(code));});});}
    static void updateLog(TextView text,ScrollView viewport,String value){
        if(value.contentEquals(text.getText()))return;int y=viewport.getScrollY();String previous=text.getText().toString();
        if(!previous.isEmpty()&&value.startsWith(previous))text.append(value.substring(previous.length()));else text.setText(value);
        viewport.post(()->viewport.scrollTo(0,y));
    }
    private void showServiceLog(ServerConnection c,String name){
        TextView log=text(t("Чтение журнала…","Reading log…"),12,WorkspaceDialogs.TEXT);log.setTypeface(Typeface.MONOSPACE);log.setTextIsSelectable(true);
        ScrollView viewport=new ScrollView(activity);viewport.addView(log);viewport.setMinimumHeight(dp(240));
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(t("Журнал службы","Service log")+" · "+name).setView(viewport).setPositiveButton(t("Закрыть","Close"),null).setNeutralButton(t("Обновить","Refresh"),null).create();show(dialog);
        Runnable update=()->{if(!dialog.isShowing())return;runInline(()->c.rpc(new JSONObject().put("op","service-log").put("name",name)),r->{if(!dialog.isShowing())return;String value=r.optString("text");updateLog(log,viewport,value);},code->{if(dialog.isShowing())android.widget.Toast.makeText(activity,explain(code),android.widget.Toast.LENGTH_SHORT).show();});};
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->update.run());update.run();
    }
    private void serviceDialog(){LinearLayout f=column();EditText name=field(f,t("Имя службы, например turnip-bot.service","Service name, e.g. turnip-bot.service"),"",false);AlertDialog d=WorkspaceDialogs.builder(activity).setTitle(t("Управление службой","Service management")).setView(f).setPositiveButton(t("Журнал","Log"),(x,w)->{ServerConnection c=connection(selected);showServiceLog(c,name.getText().toString().trim());}).setNeutralButton(t("Перезапустить…","Restart…"),(x,w)->confirm(t("Перезапустить службу?","Restart service?"),name.getText().toString()+"\n"+t("Текущая работа службы будет прервана. Без sudo; только доступные SSH-пользователю действия.","Current service work will be interrupted. No sudo; only actions allowed for the SSH user."),()->{ServerConnection c=connection(selected);run(()->c.rpc(new JSONObject().put("op","service-restart").put("name",name.getText().toString().trim())),r->message("OK",t("Запрос выполнен","Request completed")));})).setNegativeButton(t("Отмена","Cancel"),null).create();show(d);}
    public void close(){closed=true;if(scheduleUi!=null)scheduleUi.close();uploads.close();downloads.close();closePlayer();for(WorkspaceTextEditor editor:new ArrayList<>(editors))editor.close();for(ProjectFiles files:textFiles)files.close();editors.clear();textFiles.clear();main.removeCallbacksAndMessages(null);for(AlertDialog d:new ArrayList<>(ownedDialogs))d.dismiss();for(ServerConnection c:connections.values())c.close();for(WorkspaceFileBrowser browser:new ArrayList<>(sshBrowsers))browser.close();sshBrowsers.clear();for(ServerConnection c:auxiliary)c.close();auxiliary.clear();queues.close();connections.clear();root=null;}
}
