package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.termux.app.TermuxActivity;
import org.json.JSONObject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Gemini-only local polling: updates the chip text, never the terminal view or input. */
final class GeminiLiveUi {
    private final TermuxActivity activity;
    private final Supplier<WorkspaceStore.Session> current;
    private final BooleanSupplier visible;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService reader=Executors.newSingleThreadExecutor(),control=Executors.newSingleThreadExecutor();
    interface ModeApplied {boolean save(WorkspaceStore.Session profile,String run,String before,String mode);}
    private final ModeApplied modeApplied;
    private final GeminiPermissionUi permissions;
    private BooleanSupplier permissionAllowed=()->true;
    void permissionVisibility(BooleanSupplier allowed){permissionAllowed=allowed;permissions.syncVisibility();}
    private static final class PendingMode {
        final WorkspaceStore.Session profile;final String id,run,before,mode;final long created=android.os.SystemClock.uptimeMillis();int unavailable;
        PendingMode(WorkspaceStore.Session p,String id,String mode){this.profile=p;this.id=id;this.mode=mode;run=p.geminiSocketId;before=p.geminiMode;}
    }
    private final java.util.Map<String,PendingMode> pendingModes=new java.util.HashMap<>();
    private final java.util.Set<String> loadingRuns=new java.util.HashSet<>();
    private java.util.function.BiConsumer<WorkspaceStore.Session,String> thinkingApplied=(p,v)->{};
    void thinkingApplied(java.util.function.BiConsumer<WorkspaceStore.Session,String> save){thinkingApplied=save;}
    private boolean closed;
    private String run="";
    private GeminiLiveStatus status;
    private TextView chip;
    private AlertDialog detailsDialog;
    private TextView detailsModel,detailsMode,detailsThinking,detailsInfo;
    private String detailsRun="";
    private Runnable chatPreview=()->{},chatSave=()->{};
    void chatPresentation(Runnable preview,Runnable save){chatPreview=preview;chatSave=save;}
    GeminiLiveUi(TermuxActivity activity,Supplier<WorkspaceStore.Session> current,BooleanSupplier visible){
        this(activity,current,visible,(p,run,before,mode)->true);
    }
    GeminiLiveUi(TermuxActivity activity,Supplier<WorkspaceStore.Session> current,BooleanSupplier visible,ModeApplied modeApplied){
        this.activity=activity;this.current=current;this.visible=visible;this.modeApplied=modeApplied;permissions=new GeminiPermissionUi(activity,current,()->visible.getAsBoolean()&&permissionAllowed.getAsBoolean());handler.post(poll);
    }
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private final Runnable poll=new Runnable(){public void run(){
        if(closed)return;permissions.syncVisibility();WorkspaceStore.Session p=current.get();if(visible.getAsBoolean()&&p!=null)load(p);
        for(PendingMode pending:new java.util.ArrayList<>(pendingModes.values())){if(!pending.run.equals(pending.profile.geminiSocketId))pendingModes.remove(pending.run);else load(pending.profile);}
        handler.postDelayed(this,1500);
    }};
    private boolean owns(String captured){WorkspaceStore.Session p=current.get();return !closed&&p!=null&&captured.equals(p.geminiSocketId);}
    void render(LinearLayout parent,WorkspaceStore.Session p){
        if(!run.equals(p.geminiSocketId)){run=p.geminiSocketId;status=null;}
        chip=AgentStatusUi.chip(activity,"",tr("Статус, модель и права Gemini","Gemini status, model and permissions"),()->details(p));AgentStatusUi.attach(parent,chip);update();load(p);
    }
    private void load(WorkspaceStore.Session profile){
        if(closed)return;String captured=profile.geminiSocketId;if(!loadingRuns.add(captured))return;
        WorkspaceStore.Session snapshot=new WorkspaceStore.Session();snapshot.geminiAgent=true;snapshot.geminiSocketId=captured;
        reader.execute(()->{GeminiLiveStatus value=GeminiLiveStatus.read(activity.getFilesDir(),snapshot);handler.post(()->{
            loadingRuns.remove(captured);if(closed)return;reconcileMode(captured,value);if(!owns(captured))return;status=value;update();
        });});
    }
    private String state(){if(status==null)return tr("подключение…","connecting…");if(!status.available)return tr("нет подключения","disconnected");
        switch(status.state){case "idle":return tr("готов","ready");case "responding":return tr("отвечает","responding");case "waiting_for_confirmation":return tr("ожидает подтверждения","awaiting confirmation");default:return tr("нет подключения","disconnected");}}
    void syncPermissionVisibility(){permissions.syncVisibility();}
    private void update(){WorkspaceStore.Session active=current.get();if(active!=null)permissions.update(active,status);else permissions.syncVisibility();if(chip==null)return;String text="Gemini · "+(status==null||!status.available?tr("нет данных","unavailable"):status.model)+"\n"+state()+(status!=null&&status.available?" · "+GeminiRightsUi.label(status.mode):"")+(status!=null&&status.changeState.equals("queued")?tr(" · смена модели ожидает"," · model change queued"):"")+" ›";
        if(!text.contentEquals(chip.getText()))chip.setText(text);
        if(detailsDialog!=null&&detailsDialog.isShowing()){
            if(!owns(detailsRun)){detailsDialog.dismiss();return;}
            WorkspaceStore.Session profile=current.get();boolean available=status!=null&&status.available;
            detailsModel.setText(tr("Модель: ","Model: ")+(available?status.model:tr("нет данных","unavailable"))+" ›");AgentStatusUi.available(detailsModel,available);
            detailsMode.setText(tr("Режим прав: ","Permissions: ")+(available?GeminiRightsUi.label(status.mode):tr("нет данных","unavailable"))+" ›");AgentStatusUi.available(detailsMode,available&&status.canChangeMode);
            detailsThinking.setText(tr("Рассуждения: ","Reasoning: ")+(available?(status.thinkingEffective?AgentStatusUi.level(activity,status.thinking):tr("По умолчанию CLI","CLI default")):tr("нет данных","unavailable"))+" ›");AgentStatusUi.available(detailsThinking,available&&status.canChangeThinking&&status.state.equals("idle"));
            String latest=detailsSummary()+(available&&!status.canChangeThinking?"\n\n"+tr("Выбор рассуждений для этой модели пока недоступен. Используются настройки CLI.","Reasoning selection is unavailable for this model. CLI settings are used."):"");if(!latest.contentEquals(detailsInfo.getText()))detailsInfo.setText(latest);
        }
    }
    private String detailsMessage(GeminiLiveStatus s){
        String message=s==null||!s.available?tr("Нет подключения к мосту Gemini. Для старой запущенной сессии потребуется новый запуск. Авторизация выполняется в терминале. Несовместимый CLI продолжает работать без карточки.","No Gemini bridge connection. An older running session needs a new launch. Authenticate in the terminal. An unsupported CLI continues without the card."):
            tr("Модель: ","Model: ")+s.model+"\n"+tr("Выбор CLI: ","CLI selection: ")+s.selectedModel+"\n"+tr("Режим прав: ","Permission mode: ")+GeminiRightsUi.label(s.mode)+(s.canChangeMode?" ▾":"")+"\n"+tr("Состояние: ","State: ")+state()+"\n\n"+
            tr("Расход за текущий запуск CLI, включая служебные запросы:","Usage in the current CLI launch, including internal requests:")+"\n"+
            tr("Запросы: ","Requests: ")+s.requests+"\n"+tr("Токены всего (по данным API): ","Total tokens (reported by API): ")+(s.total>0||s.requests==0?Long.toString(s.total):"—")+"\n"+
            tr("Вход / ответ: ","Input / answer: ")+s.input+" / "+s.output+"\n"+
            tr("Из кэша / рассуждения: ","Cached / thinking: ")+s.cache+" / "+s.thoughts+"\n"+tr("Время запуска: ","Launch duration: ")+Math.max(0,(System.currentTimeMillis()-s.startedAt)/1000)+tr(" с\n"," s\n")+tr("Суммарное время API: ","Total API time: ")+(s.apiMs/1000.0)+tr(" с"," s")+"\n\n"+
            tr("Кэш — часть входных токенов. Всего — счётчик API, без повторного сложения частей. Это не общий расход аккаунта и не размер контекста. Остаток квоты недоступен.","Cached tokens are part of the input. Total is the API counter, without adding subsets twice. This is not account-wide usage or context size. Remaining quota is unavailable.");
        if(s!=null&&!s.changeState.isEmpty())message+="\n\n"+tr("Смена модели: ","Model change: ")+s.changeModel+" · "+changeLabel(s.changeState);
        if(s!=null&&!s.modeChangeState.isEmpty())message+="\n\n"+tr("Смена прав: ","Permission change: ")+GeminiRightsUi.label(s.modeChangeMode)+" · "+changeLabel(s.modeChangeState);
        return message;
    }
    private String detailsSummary(){String full=detailsMessage(status);if(status==null||!status.available)return full;String heading=tr("Расход за текущий запуск CLI, включая служебные запросы:","Usage in the current CLI launch, including internal requests:");int at=full.indexOf(heading);return tr("Состояние: ","State: ")+state()+(status.selectedModel.isEmpty()||status.selectedModel.equals(status.model)?"":"\n"+tr("Выбор CLI: ","CLI selection: ")+status.selectedModel)+"\n\n"+(at<0?full:full.substring(at));}
    private void chooseMode(WorkspaceStore.Session profile){
        String captured=profile.geminiSocketId;if(!owns(captured)||status==null||!status.canChangeMode)return;
        String[] labels=new String[3];for(int i=0;i<3;i++)labels[i]=GeminiRightsUi.label(GeminiRightsUi.MODES[i])+"\n"+GeminiRightsUi.description(GeminiRightsUi.MODES[i]);
        String selected=GeminiRightsUi.normalize(status.mode);int checked=java.util.Arrays.asList(GeminiRightsUi.MODES).indexOf(selected);
        java.util.function.IntPredicate enabled=index->index==0||(index==1?status!=null&&status.canEdit:status!=null&&status.canPlan);
        WorkspaceDialogs.builder(activity).setTitle(tr("Режим прав Gemini","Gemini permission mode"))
            .setSingleChoiceItems(WorkspaceDialogs.choiceAdapter(activity,labels,checked,enabled),checked,(d,w)->{if(!enabled.test(w)||!owns(captured))return;d.dismiss();requestMode(profile,GeminiRightsUi.MODES[w]);})
            .setNegativeButton(tr("Назад","Back"),null).show();
    }
    private void requestMode(WorkspaceStore.Session profile,String mode){
        if(!owns(profile.geminiSocketId)||!GeminiRightsUi.valid(mode))return;
        PendingMode request=new PendingMode(profile,UUID.randomUUID().toString(),mode);pendingModes.put(request.run,request);
        control.execute(()->{String error="";try{GeminiLiveControl.request(activity.getFilesDir(),request.run,new JSONObject().put("op","mode").put("mode",mode).put("id",request.id));}catch(Exception failure){error=failureCode(failure);}String reason=error;
            handler.post(()->{if(closed)return;if(!reason.isEmpty()){if(!reason.equals("CONTROL_TIMEOUT")&&!reason.equals("PROTOCOL_FAILED")&&pendingModes.get(request.run)==request)pendingModes.remove(request.run);if(owns(request.run))controlFailure(reason);}load(profile);});
        });
    }
    private void reconcileMode(String captured,GeminiLiveStatus value){
        PendingMode pending=pendingModes.get(captured);if(pending==null)return;
        if(!captured.equals(pending.profile.geminiSocketId)){pendingModes.remove(captured);return;}
        if(!value.error.isEmpty()){if(++pending.unavailable>=10)pendingModes.remove(captured);return;}pending.unavailable=0;
        if(value.state.equals("unsupported")){pendingModes.remove(captured);return;}
        if(!pending.id.equals(value.modeChangeId)||!pending.mode.equals(value.modeChangeMode)){if(value.state.equals("ended")||android.os.SystemClock.uptimeMillis()-pending.created>30000)pendingModes.remove(captured);return;}
        if(value.modeChangeState.equals("queued")){if(value.state.equals("ended"))pendingModes.remove(captured);return;}
        pendingModes.remove(captured);
        if(value.modeChangeState.equals("applied")&&pending.mode.equals(GeminiRightsUi.normalize(value.mode))){
            boolean saved=modeApplied.save(pending.profile,captured,pending.before,pending.mode);
            if(owns(captured))activity.showToast(saved?(pending.profile.saved?tr("Права применены. Настройки сессии и пресета обновлены.","Permissions applied. Session/preset settings updated."):tr("Права применены. Настройки сессии обновлены.","Permissions applied. Session settings updated.")):tr("Права применены, но настройки изменились отдельно или недоступны для записи. Проверьте пресет.","Permissions applied, but settings changed separately or cannot be saved. Check the preset."),true);
        }else if(owns(captured))activity.showToast(tr("Смена прав не подтверждена или отменена в Gemini. Пресет не изменён.","Permission change was not confirmed or was cancelled in Gemini. Preset unchanged."),true);
    }
    private void details(WorkspaceStore.Session profile){
        String captured=profile.geminiSocketId;if(!owns(captured))return;if(detailsDialog!=null)detailsDialog.dismiss();LinearLayout body=AgentStatusUi.body(activity);
        detailsModel=AgentStatusUi.row(body,"","model",()->{if(owns(captured))choose(profile);});detailsMode=AgentStatusUi.row(body,"","permissions",()->{if(owns(captured))chooseMode(profile);});
        detailsThinking=AgentStatusUi.row(body,"","reasoning",()->{if(owns(captured))chooseThinking(profile);});
        detailsInfo=AgentStatusUi.info(body,detailsSummary());detailsInfo.setId(android.R.id.message);
        AgentStatusUi.row(body,tr("Квоты AI Studio","AI Studio quotas"),"quotas",()->openQuotas());
        AgentChatScale.controls(body,profile,()->owns(captured),chatPreview,chatSave);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(tr("Статус Gemini","Gemini status")).setView(AgentStatusUi.scroll(body)).setPositiveButton(tr("Обновить","Refresh"),null).setNegativeButton(tr("Закрыть","Close"),null).create();detailsDialog=dialog;detailsRun=captured;
        dialog.setOnDismissListener(d->{if(detailsDialog==dialog)detailsDialog=null;});dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(owns(captured))load(profile);}));dialog.show();update();
    }
    private void chooseThinking(WorkspaceStore.Session p){if(status==null||!status.available||!status.canChangeThinking||!status.state.equals("idle"))return;String captured=p.geminiSocketId,model=status.model,before=p.geminiThinking;java.util.List<String> values=new java.util.ArrayList<>(status.thinkingOptions);String[] labels=new String[values.size()];for(int i=0;i<labels.length;i++)labels[i]=AgentStatusUi.level(activity,values.get(i))+(status.thinkingModel.startsWith("gemini-2.5-")&&!values.get(i).equals("default")?" · "+(values.get(i).equals("low")?1024:values.get(i).equals("medium")?8192:status.thinkingModel.equals("gemini-2.5-pro")?32768:24576)+tr(" токенов"," tokens"):"");int checked=values.indexOf(status.thinking);WorkspaceDialogs.builder(activity).setTitle(tr("Рассуждения Gemini","Gemini reasoning")).setSingleChoiceItems(labels,checked,(d,w)->{d.dismiss();String level=values.get(w);WorkspaceDialogs.builder(activity).setTitle(tr("Подтвердить выбор","Confirm selection")).setMessage(model+" · "+labels[w]+"\n\n"+tr("Применяется к следующим запросам основного чата через thinkingConfig Gemini CLI. У Gemini 2.5 это бюджет токенов, у поддерживаемых новых моделей — уровень. Расход и время ответа могут измениться. Права не меняются.","Applies to subsequent main-chat requests through Gemini CLI thinkingConfig. Gemini 2.5 uses a token budget; supported newer models use a level. Usage and response time may change. Permissions stay the same.")).setPositiveButton(tr("Применить","Apply"),(confirm,index)->{if(!owns(captured))return;control.execute(()->{boolean ok=false;String error="CONTROL_FAILED";try{JSONObject r=GeminiLiveControl.request(activity.getFilesDir(),captured,new JSONObject().put("op","thinking").put("model",model).put("level",level));JSONObject t=r.optJSONObject("thinking");ok=t!=null&&level.equals(t.optString("value"))&&t.optBoolean("effective");}catch(Exception e){error=failureCode(e);}boolean accepted=ok;String reason=error;handler.post(()->{if(closed||!captured.equals(p.geminiSocketId))return;if(accepted&&before.equals(p.geminiThinking))thinkingApplied.accept(p,level);if(owns(captured)){if(!accepted)controlFailure(reason);load(p);}});});}).setNegativeButton(tr("Назад","Back"),null).show();}).setNegativeButton(tr("Назад","Back"),null).show();}
    private void openQuotas(){
        try{activity.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://aistudio.google.com/rate-limit")));}
        catch(android.content.ActivityNotFoundException noBrowser){WorkspaceDialogs.builder(activity).setTitle(tr("Квоты Gemini","Gemini quotas")).setMessage("https://aistudio.google.com/rate-limit").setPositiveButton(tr("Закрыть","Close"),null).show();}
    }
    private void controlFailure(String code){
        String message;
        switch(code){
            case "THINKING_UNAVAILABLE":case "THINKING_BUSY":message=tr("Выбор рассуждений недоступен: модель сменилась, не поддерживается или чат занят. Обновите карточку после завершения ответа.","Reasoning selection is unavailable: the model changed, is unsupported, or chat is busy. Refresh after the response finishes.");break;
            case "MODE_UNAVAILABLE":message=tr("Gemini пока не разрешает переключение этих прав. Перезапустите сессию после обновления; также проверьте доверие к проекту и доступность планирования.","Gemini cannot switch these permissions yet. Relaunch after updating; check project trust and plan-mode availability.");break;
            case "MODE_BUSY":message=tr("Предыдущая смена прав ещё применяется. Дождитесь результата.","The previous permission change is still being applied. Wait for its result.");break;
            case "CONNECT_FAILED":message=tr("Не удалось подключиться к локальному мосту Gemini. Чат может продолжать работать. Для новой версии моста заново запустите Gemini-сессию.","Could not connect to the local Gemini bridge. The chat can keep working. Relaunch the Gemini session for the new bridge.");break;
            case "CONTROL_TIMEOUT":message=tr("Истекло время ожидания локального моста. Повторите попытку после завершения запуска или текущего действия Gemini.","The local bridge timed out. Retry after Gemini finishes starting or its current operation.");break;
            case "NOT_READY":message=tr("Gemini ещё не готов к управлению. Дождитесь запуска; если в терминале запрошен вход, завершите его.","Gemini is not ready for controls. Wait until startup completes; finish authentication if the terminal requests it.");break;
            case "RESTARTED":case "ENDED":message=tr("Этот запуск Gemini завершён или перезапускается. Дождитесь подключения новой сессии.","This Gemini run has ended or is restarting. Wait for the new session to connect.");break;
            case "EMPTY_CATALOG":message=tr("Gemini не вернул варианты моделей. Чат и авторизация не изменены.","Gemini returned no model options. Chat and authentication are unchanged.");break;
            default:message=tr("Не удалось получить корректный ответ локального моста Gemini. Чат и авторизация не изменены.","Could not read a valid response from the local Gemini bridge. Chat and authentication are unchanged.");
        }
        WorkspaceDialogs.builder(activity).setTitle(tr("Управление Gemini недоступно","Gemini controls unavailable")).setMessage(message+"\n\n"+tr("Код: ","Code: ")+code).setPositiveButton(tr("Закрыть","Close"),null).show();
    }
    private static String failureCode(Exception failure){return failure instanceof GeminiLiveControl.Failure?((GeminiLiveControl.Failure)failure).code:"PROTOCOL_FAILED";}
    private String changeLabel(String value){switch(value){case "applied":return tr("применена CLI","applied by CLI");case "queued":return tr("ожидает завершения ответа","waiting until idle");case "cancelled":return tr("отменена: выбор изменён в CLI","cancelled: selection changed in CLI");default:return tr("ошибка","failed");}}
    private void choose(WorkspaceStore.Session profile){
        String captured=profile.geminiSocketId;if(!owns(captured))return;
        AlertDialog wait=WorkspaceDialogs.builder(activity).setTitle(tr("Модели Gemini","Gemini models")).setMessage(tr("Получение доступного каталога…","Loading model catalog…")).setNegativeButton(tr("Отмена","Cancel"),null).show();
        control.execute(()->{List<String> models=null;String error="EMPTY_CATALOG";try{models=GeminiLiveControl.models(GeminiLiveControl.request(activity.getFilesDir(),captured,new JSONObject().put("op","models")));}catch(Exception failure){error=failureCode(failure);}List<String> options=models;String reason=error;
            handler.post(()->{boolean show=wait.isShowing();wait.dismiss();if(!show||!owns(captured))return;
                if(options==null||options.isEmpty()){controlFailure(reason);return;}
                int selected=status==null?-1:options.indexOf(status.selectedModel);if(selected<0&&status!=null)selected=options.indexOf(status.model);WorkspaceDialogs.builder(activity).setTitle(tr("Модель Gemini","Gemini model")).setSingleChoiceItems(options.toArray(new String[0]),selected,(d,w)->{d.dismiss();confirm(profile,options.get(w));}).setNegativeButton(tr("Назад","Back"),null).show();
            });
        });
    }
    private void confirm(WorkspaceStore.Session profile,String model){
        String captured=profile.geminiSocketId;if(!owns(captured))return;
        WorkspaceDialogs.builder(activity).setTitle(model).setMessage(tr("Сменить модель в текущем чате? История сохраняется. Во время ответа смена ждёт готовности CLI. Каталог не гарантирует доступность модели или бесплатный тариф: сервер проверит это при запросе. Изменение действует до следующего запуска.","Change the model in this chat? History is preserved. While responding, the change waits until idle. The catalog does not guarantee model access or free pricing: the server checks at request time. This change lasts until the next launch."))
            .setPositiveButton(tr("Применить","Apply"),(d,w)->{if(!owns(captured))return;
                control.execute(()->{boolean ok=false;String error="CONTROL_FAILED";try{GeminiLiveControl.request(activity.getFilesDir(),captured,new JSONObject().put("op","model").put("model",model).put("id",UUID.randomUUID().toString()));ok=true;}catch(Exception failure){error=failureCode(failure);}boolean accepted=ok;String reason=error;
                    handler.post(()->{if(!owns(captured))return;if(accepted)activity.showToast(tr("Запрос передан Gemini. Результат — в карточке модели.","Request sent to Gemini. See the model card for its result."),true);else controlFailure(reason);load(profile);});
                });
            }).setNegativeButton(tr("Отмена","Cancel"),null).show();
    }
    void close(){closed=true;permissions.close();pendingModes.clear();if(detailsDialog!=null)detailsDialog.dismiss();handler.removeCallbacksAndMessages(null);reader.shutdownNow();control.shutdownNow();}
}
