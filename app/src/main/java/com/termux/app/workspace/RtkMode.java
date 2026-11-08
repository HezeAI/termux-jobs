package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** One installation per Ubuntu AI; state and measured bytes live in that environment per profile. */
final class RtkMode extends LinearLayout {
    final android.app.Activity activity;final WorkspaceStore.Session profile;
    interface Backend {JSONObject request(String operation,String attempt,java.util.function.Consumer<JSONObject> events)throws Exception;}
    final Backend backend;
    final CheckBox toggle,advanced;final TextView statistics,description,stateLabel,detailsToggle,details,update;final boolean deferred;String selectedMode="off",targetMode="basic";final java.util.Map<String,String> choices=new ConcurrentHashMap<>();final java.util.Set<String> accepted=ConcurrentHashMap.newKeySet();
    final Handler handler=new Handler(Looper.getMainLooper());
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    AlertDialog window;TextView stage,action;ProgressBar progress;
    String attempt="";boolean closed,enabled,busy,success,updateOnly;long generation;
    RtkMode(android.app.Activity a,WorkspaceStore.Session p){this(a,p,null);}
    RtkMode(android.app.Activity a,WorkspaceStore.Session p,Backend backend){this(a,p,backend,false);}
    RtkMode(android.app.Activity a,WorkspaceStore.Session p,Backend backend,boolean deferred){super(a);this.deferred=deferred;activity=a;profile=p;this.backend=backend==null?this::request:backend;setOrientation(VERTICAL);setTag("rtk-mode");
        LinearLayout modes=new LinearLayout(a);modes.setOrientation(HORIZONTAL);addView(modes,new LayoutParams(-1,-2));advanced=new CheckBox(a);advanced.setText(t("RTK штатный","RTK native"));advanced.setTextColor(WorkspaceDialogs.TEXT);advanced.setTag("rtk-advanced-checkbox");modes.addView(advanced,new LayoutParams(0,-2,1));
        toggle=new CheckBox(a);toggle.setText("RTK Lite");toggle.setTextColor(WorkspaceDialogs.TEXT);toggle.setTag("rtk-mode-checkbox");modes.addView(toggle,new LayoutParams(0,-2,1));stateLabel=AgentStatusUi.info(this,"");stateLabel.setTag("rtk-mode-state");
        statistics=AgentStatusUi.info(this,t("Сокращение вывода: пока нет данных","Output reduction: no data yet"));statistics.setTag("rtk-mode-statistics");
        description=AgentStatusUi.info(this,t("Сокращает вывод команд перед передачей агенту. Оценка относится к выводу команд, а не ко всему контексту или лимиту подписки.","Reduces command output before sending it to the agent. The estimate concerns command output, not the entire context or subscription quota."));
        detailsToggle=AgentStatusUi.info(this,t("Подробности ▾","Details ▾"));detailsToggle.setMinHeight(WorkspaceDialogs.dp(a,48));detailsToggle.setGravity(Gravity.CENTER_VERTICAL);
        details=AgentStatusUi.info(this,"");details.setVisibility(GONE);
        detailsToggle.setOnClickListener(v->{boolean show=details.getVisibility()!=VISIBLE;details.setVisibility(show?VISIBLE:GONE);detailsToggle.setText(show?t("Подробности ▴","Details ▴"):t("Подробности ▾","Details ▾"));});

        update=dialogButton(this,t("Обновить RTK","Update RTK"));update.setVisibility(GONE);update.setOnClickListener(v->{open("basic");updateOnly=true;});
        String unavailable=unavailable(p);if(!unavailable.isEmpty()){toggle.setEnabled(false);advanced.setEnabled(false);AgentStatusUi.info(this,unavailable);}
        toggle.setOnClickListener(v->choose("basic"));advanced.setOnClickListener(v->choose("advanced"));
    }
    void choose(String mode){toggle.setChecked(enabled&&selectedMode.equals("basic"));advanced.setChecked(enabled&&selectedMode.equals("advanced"));if(busy)return;if(enabled&&selectedMode.equals(mode))perform("disable",this::refresh);else open(mode);}
    static String environment(WorkspaceStore.Session p,String run){return "ubuntu-ai".equals(p.codexEnvironment)?"; export JOBS_RTK_PROFILE="+SessionShell.quote(p.id)+" JOBS_RTK_RUN="+SessionShell.quote(run)+"; export PATH=\"$HOME/.local/share/termux-jobs/components/rtk/1.1.0-jobs.1/chat-bin:$HOME/.local/share/termux-jobs/components/rtk/1.1.0/chat-bin:$PATH\"":"";}
    String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    String unavailable(WorkspaceStore.Session p){if(!p.codexEnvironment.equals("ubuntu-ai"))return t("RTK поддерживается только в Ubuntu AI выбранного агента.","RTK is supported only in the selected agent's Ubuntu AI environment.");if(!p.environment.equals("codex")&&!p.claudeAgent)return t("Для этого агента подключение RTK с сохранением штатных разрешений пока не проверено.","RTK integration preserving native permissions has not yet been verified for this agent.");return "";}
    String run(){return profile.claudeAgent?profile.claudeSocketId:profile.codexSocketId;}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refresh();}
    @Override protected void onDetachedFromWindow(){if(window==null||!window.isShowing()||activity.isFinishing()||activity.isDestroyed())close();super.onDetachedFromWindow();}
    void refresh(){if(closed||!unavailable(profile).isEmpty())return;perform("status",()->{});}
    String asset(String name)throws IOException{try(InputStream in=activity.getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
    JSONObject request(String operation,String token,java.util.function.Consumer<JSONObject> events)throws Exception{
        JSONObject q=new JSONObject().put("op",operation).put("profile",profile.id).put("run",run()).put("attempt",token).put("_source",asset("rtk-mode.py")).put("mode",choices.getOrDefault(token,"basic")).put("confirmed",accepted.contains(token)).put("deferred",deferred);
        JSONObject pin=new JSONObject(asset("rtk-component.json"));
        JSONObject envelope=new JSONObject().put("pin",pin).put("request",q);
        String bootstrap="import json,sys; e=json.load(sys.stdin); g={'__name__':'rtk_bridge','PIN':e['pin']}; exec(compile(e['request']['_source'],'rtk-mode.py','exec'),g); g['main'](e['request'])";
        final JSONObject[] result={null};ByteArrayOutputStream line=new ByteArrayOutputStream();
        OutputStream out=new OutputStream(){public void write(int b)throws IOException{if(b==10){try{JSONObject event=new JSONObject(line.toString("UTF-8"));if(event.has("ok"))result[0]=event;else events.accept(event);}catch(JSONException e){throw new IOException("PROTOCOL_ERROR");}line.reset();}else{if(line.size()>16384)throw new IOException("PROTOCOL_ERROR");line.write(b);}}};
        try(ProjectFiles files=new ProjectFiles(activity,profile)){files.run("exec python3 -I -c "+SessionShell.quote(bootstrap),out,2*1024*1024,envelope.toString().getBytes(StandardCharsets.UTF_8),600);}
        if(result[0]==null)throw new IOException("PROTOCOL_ERROR");return result[0];
    }
    void present(JSONObject s){if(s==null)return;selectedMode=s.optString("mode",s.optBoolean("desired",s.optBoolean("enabled"))?"basic":"off");enabled=deferred?s.optBoolean("desired"):s.optBoolean("enabled");toggle.setChecked(enabled&&selectedMode.equals("basic"));advanced.setChecked(enabled&&selectedMode.equals("advanced"));
        stateLabel.setText(s.optBoolean("desired")?(deferred?t("Применится при запуске чата","Applies when the chat starts"):s.optBoolean("enabled")?t("Подключено","Connected"):t("Подключение не подтверждено","Connection not confirmed")+(s.optString("error").isEmpty()?"":": "+s.optString("error"))):t("Выключено","Off"));
        update.setVisibility(s.optBoolean("updateAvailable")?VISIBLE:GONE);
        if(s.optBoolean("updateAvailable"))stateLabel.append(t(" · Доступна ревизия RTK jobs.1"," · RTK revision jobs.1 is available"));
        if(s.optBoolean("activeLegacy"))stateLabel.append(t(" · Работает предыдущая сборка RTK. Обновление применится после запуска чата."," · The previous RTK build is running. The update applies after starting the chat."));
        String stats=RtkStatistics.text(s.optJSONObject("statistics"),WorkspaceLanguage.code(activity).equals("ru"));
        if(!deferred&&s.optBoolean("desired")&&!s.optBoolean("statisticsReady"))stats+="\n\n"+t("Для учёта получения результатов нужен следующий запуск чата. Рабочая сессия не перезапускается автоматически.","Result-delivery accounting needs the next chat start. The working session is not restarted automatically.");
        stats+="\n\n"+t("Байты текста результатов инструментов. Служебная обвязка CLI и повторная передача истории не учитываются.","Tool-result text bytes. CLI framing and repeated history transmission are not counted.");
        stats+="\n\n"+t("Токены в краткой строке — оценка: 1 токен ≈ 4 байта UTF-8.","Summary tokens are estimated: 1 token ≈ 4 UTF-8 bytes.");
        stats+="\n\n"+t("RTK Lite — наша интеграция с ограниченным проверенным набором команд: Git, поиск, чтение файлов, некоторые тесты. Для Gradle — сокращённый просмотр stdout с итогом сборки. Составные и неподдерживаемые команды проходят обычно; вывод ошибок не сокращается. RTK штатный — штатная интеграция RTK с более широким охватом.","RTK Lite is our integration with a limited, verified command set: Git, search, file reads and selected tests. Gradle uses a stdout preview with the build ending. Compound and unsupported commands run normally; diagnostics are not compressed. RTK native uses RTK’s native integration with broader coverage.");
        statistics.setText(RtkStatistics.summary(s.optJSONObject("statistics"),selectedMode,WorkspaceLanguage.code(activity).equals("ru")));
        details.setText(stats);}


    void perform(String op,Runnable after){if(closed)return;busy=true;final long epoch=generation;final String token=attempt;
        worker.execute(()->{JSONObject result;try{result=backend.request(op,token,event->handler.post(()->{if(!closed&&epoch==generation)stage(event);}));}catch(Exception e){result=new JSONObject();try{result.put("ok",false).put("error",e.getMessage()==null?"RTK_FAILED":e.getMessage());}catch(JSONException ignored){}}
            JSONObject response=result;handler.post(()->{if(closed||epoch!=generation)return;busy=false;if(!response.optBoolean("ok")){if(window!=null)error(response.optString("error"));else statistics.setText(t("Не удалось проверить подключение RTK. Откройте окно повторно.","Could not verify RTK. Reopen this window."));return;}present(response.optJSONObject("result"));last=response.optJSONObject("result");after.run();});});
    }
    JSONObject last;
    void open(){open("basic");}
    void open(String mode){if(window!=null)return;targetMode=mode;success=false;updateOnly=false;attempt=UUID.randomUUID().toString();choices.put(attempt,mode);generation++;
        LinearLayout body=AgentStatusUi.body(activity);TextView title=AgentStatusUi.info(body,mode.equals("advanced")?t("RTK штатный","RTK native"):"RTK Lite");title.setGravity(Gravity.CENTER);title.setTextSize(18);
        stage=AgentStatusUi.info(body,"");stage.setGravity(Gravity.CENTER);progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);body.addView(progress,new LayoutParams(-1,WorkspaceDialogs.dp(activity,12)));
        action=dialogButton(body,"");action.setVisibility(GONE);
        TextView cancel=dialogButton(body,t("Отмена","Cancel"));cancel.setOnClickListener(v->cancel());
        window=WorkspaceDialogs.builder(activity).setView(AgentStatusUi.scroll(body)).create();window.setCanceledOnTouchOutside(false);window.setOnCancelListener(d->cancel());window.setOnDismissListener(d->{window=null;if(!success)cancelAttempt();if(!isAttachedToWindow())close();});window.show();window.getWindow().setGravity(Gravity.CENTER);window.getWindow().setLayout(Math.min(WorkspaceDialogs.dp(activity,340),activity.getResources().getDisplayMetrics().widthPixels-WorkspaceDialogs.dp(activity,32)),-2);
        if(mode.equals("advanced")){progress.setVisibility(GONE);stage.setText(profile.claudeAgent?t("Некоторые запреты и правила подтверждения Bash-команд могут не срабатывать после преобразования в RTK. Это не означает отключение всех файловых ограничений.","Some Bash command denials and approval rules may not apply after RTK rewriting. This does not disable all file restrictions."):t("Некоторые запреты по префиксу исходной команды могут не срабатывать после преобразования в RTK. В проверенном режиме untrusted подтверждения сохраняются.","Some original-command prefix denials may not apply after RTK rewriting. Approvals remain in the tested untrusted mode."));button(t("Включить RTK штатный","Enable RTK native"),()->{accepted.add(attempt);check();});}else check();
    }
    String installDescription(){try{JSONObject pin=new JSONObject(asset("rtk-component.json"));return (last!=null&&last.optBoolean("legacyInstalled")?t("Обновление RTK. Для применения в работающем чате потребуется его перезапуск. Автоматического перезапуска не будет. Настройки, статистика и recall сохраняются.\n\n","RTK update. Applying it in a running chat requires restarting that chat. It will not restart automatically. Settings, statistics and recall are retained.\n\n"):"")+t("Требуется RTK ","RTK required: ")+pin.optString("version")+t(" OSS. Размер загрузки: "," OSS. Download size: ")+pin.optLong("size")+t(" байт."," bytes.");}catch(Exception e){return t("Требуется установка компонента RTK.","RTK component installation is required.");}}
    void check(){stage.setText(t("Проверка наличия RTK…","Checking RTK…"));progress.setVisibility(VISIBLE);progress.setIndeterminate(true);action.setVisibility(GONE);perform("begin",()->{if(last!=null&&last.optBoolean("installed"))connect();else{stage.setText(installDescription());progress.setVisibility(GONE);button(last!=null&&last.optBoolean("legacyInstalled")?t("Обновить","Update"):t("Установить","Install"),()->{action.setVisibility(GONE);perform("install",this::connect);});}});}
    static TextView dialogButton(LinearLayout body,String title){
        android.content.Context context=body.getContext();
        TextView button=WorkspaceDialogs.action(context,title,0);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);button.setMinimumWidth(0);
        button.setMinHeight(WorkspaceDialogs.dp(context,48));
        button.setMinimumHeight(WorkspaceDialogs.dp(context,48));
        button.setPadding(WorkspaceDialogs.dp(context,16),WorkspaceDialogs.dp(context,8),
                WorkspaceDialogs.dp(context,16),WorkspaceDialogs.dp(context,8));
        button.setSingleLine(false);button.setMaxLines(Integer.MAX_VALUE);button.setEllipsize(null);
        LayoutParams params=new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        params.gravity=Gravity.CENTER_HORIZONTAL;body.addView(button,params);
        return button;
    }
    void button(String title,Runnable click){action.setText(title);action.setVisibility(VISIBLE);action.setOnClickListener(v->{if(!busy)click.run();});}
    void stage(JSONObject e){if(window==null)return;String value=e.optString("stage");progress.setVisibility(VISIBLE);if(value.equals("download")){long total=e.optLong("total"),bytes=e.optLong("bytes");int percent=total>0?(int)Math.min(100,bytes*100/total):0;progress.setIndeterminate(total<=0);progress.setMax(100);progress.setProgress(percent);stage.setText(t("Загрузка RTK — ","Downloading RTK — ")+percent+"%");}else{progress.setIndeterminate(true);stage.setText(t("Проверка и установка RTK…","Verifying and installing RTK…"));}}
    void connect(){if(updateOnly){success=true;refresh();if(window!=null)window.dismiss();return;}stage.setText(deferred?t("Сохранение выбора RTK…","Saving RTK selection…"):t("Подключение RTK к чату…","Connecting RTK to this chat…"));progress.setVisibility(VISIBLE);progress.setIndeterminate(true);action.setVisibility(GONE);perform("enable",()->{success=true;if(window!=null)window.dismiss();});}
    void error(String code){progress.setVisibility(GONE);if(code.equals("RESTART_REQUIRED")){stage.setText(t("Для подключения нужен перезапуск только этого чата. Выполняемая задача будет остановлена. История сохранится. Режим ещё не включён.","Connecting requires restarting only this chat. Its running task will stop. History is retained. RTK is not enabled yet."));button(t("Перезапустить этот чат","Restart this chat"),()->{action.setVisibility(GONE);if(activity instanceof TermuxActivity){((TermuxActivity)activity).restartWorkspaceAgentForRtk(profile.id);waitReady(0);}else error("RESTART_UNAVAILABLE");});return;}
        String reason=code.equals("CHECKSUM_MISMATCH")||code.equals("SIZE_MISMATCH")?t("Файл не прошёл проверку размера или SHA-256.","The file failed size or SHA-256 verification."):code.equals("INSTALL_BUSY")?t("RTK устанавливается другим чатом этого окружения.","Another chat is installing RTK in this environment."):code.equals("UNSUPPORTED_ENVIRONMENT")?t("Нужно окружение Ubuntu AI ARM64.","Ubuntu AI ARM64 is required."):code.equals("NETWORK_ERROR")?t("Ошибка сети или доступа к файлу.","Network or file access error."):code.equals("INSTALLATION_DAMAGED")?t("Установленная копия повреждена. Автоматическая замена не выполнена.","The installed copy is damaged. It was not replaced automatically."):t("Подключение не подтверждено: ","Connection was not confirmed: ")+code;
        stage.setText(reason);button(t("Повторить","Retry"),this::check);
    }
    void waitReady(int count){if(closed||window==null)return;if(count>=60){error("HOOK_NOT_CONFIRMED");return;}stage.setText(t("Ожидание подключения после перезапуска…","Waiting for connection after restart…"));progress.setVisibility(VISIBLE);progress.setIndeterminate(true);perform("status",()->{if(last!=null&&last.optBoolean("ready"))connect();else handler.postDelayed(()->waitReady(count+1),1000);});}
    void cancelAttempt(){generation++;busy=false;String token=attempt;attempt="";if(token.isEmpty())return;new Thread(()->{try{backend.request("cancel",token,e->{});}catch(Exception ignored){}},"rtk-cancel").start();}
    void cancel(){success=false;cancelAttempt();if(window!=null)window.dismiss();toggle.setChecked(enabled&&selectedMode.equals("basic"));advanced.setChecked(enabled&&selectedMode.equals("advanced"));}
    void close(){if(closed)return;if(window!=null)cancel();closed=true;handler.removeCallbacksAndMessages(null);worker.shutdown();}
}
