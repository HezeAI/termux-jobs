package com.termux.app.workspace;

import android.app.AlertDialog;
import android.text.*;
import androidx.core.widget.TextViewCompat;
import android.view.Gravity;
import android.widget.*;
import com.termux.app.TermuxActivity;
import java.util.*;
import java.util.concurrent.*;

/** Session-targeted catalogue; PTY operations require an explicit destination confirmation. */
final class WorkspacePackages implements AutoCloseable {
    interface Launch {void run(WorkspaceStore.Session profile,boolean selectedSession);}
    private final TermuxActivity activity;private final ProjectFiles files;private final Launch launch;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Runnable onClosed;
    void setOnClosed(Runnable callback){onClosed=callback;}
    private AlertDialog dialog;private LinearLayout rows,tabs;private TextView status;private EditText search;
    private EnvironmentPackages.Kind backend;
    private TextView catalogHint,refreshButton;
    private volatile boolean historyUnavailable;
    private List<PackageCatalog.Entry> entries=Collections.emptyList();private int tab,limit=100,generation;private volatile boolean closed;private boolean busy;
    WorkspacePackages(TermuxActivity activity,WorkspaceStore.Session profile,Launch launch){this.activity=activity;this.files=new ProjectFiles(activity,profile);this.launch=launch;String environment=ProjectFiles.environment(profile);if(environment.equals("termux")||environment.equals("ubuntu"))backend=EnvironmentPackages.Kind.APT;}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private int dp(int n){return WorkspaceDialogs.dp(activity,n);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(String value,int size){TextView t=new TextView(activity);t.setText(value);t.setTextColor(WorkspaceDialogs.TEXT);t.setTextSize(size);return t;}
    private TextView action(String label,Runnable run){TextView t=WorkspaceDialogs.action(activity,label,0);t.setOnClickListener(v->run.run());return t;}
    static TextView compactAction(android.content.Context context,String label,Runnable run){
        TextView view=WorkspaceDialogs.action(context,label,0);view.setGravity(Gravity.CENTER);view.setSingleLine(true);
        view.setPadding(WorkspaceDialogs.dp(context,6),WorkspaceDialogs.dp(context,8),WorkspaceDialogs.dp(context,6),WorkspaceDialogs.dp(context,8));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(view,10,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        view.setOnClickListener(v->run.run());return view;
    }
    private LinearLayout.LayoutParams compactParams(){return new LinearLayout.LayoutParams(0,dp(56),1);}
    void show(){
        LinearLayout body=column();body.setPadding(dp(12),0,dp(12),dp(8));
        body.addView(text(WorkspaceText.tr("Сессия: ")+files.profile.name,12));
        body.addView(text(WorkspaceText.tr("Установленные пакеты сохраняются в выбранном окружении и доступны всем его сессиям. Закрытие сессии без сохранения не удаляет пакеты."),12));
        tabs=new LinearLayout(activity);tabs.setGravity(Gravity.CENTER_VERTICAL);body.addView(tabs);renderTabs();
        search=new EditText(activity);search.setSingleLine(true);search.setTextColor(WorkspaceDialogs.TEXT);search.setHintTextColor(WorkspaceDialogs.MUTED);search.setHint(WorkspaceText.tr("Поиск пакетов"));body.addView(search);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){limit=100;render();}public void afterTextChanged(Editable e){}});
        catalogHint=text(WorkspaceLanguage.code(activity).equals("ru")?"Пакеты выбранного окружения. Для свежих данных обновите каталог.":"Packages in the selected environment. Refresh the catalogue for current metadata.",12);body.addView(catalogHint);
        LinearLayout controls=new LinearLayout(activity);controls.addView(compactAction(activity,WorkspaceText.tr("Перечитать список"),this::load),compactParams());refreshButton=compactAction(activity,WorkspaceText.tr("Обновить каталог"),()->confirm("refresh",null));controls.addView(refreshButton,compactParams());body.addView(controls);
        status=text("",12);body.addView(status);rows=column();ScrollView scroll=new ScrollView(activity);scroll.addView(rows);
        int height=Math.min(dp(330),Math.max(dp(120),(int)(activity.getResources().getDisplayMetrics().heightPixels*0.32)));body.addView(scroll,new LinearLayout.LayoutParams(-1,height));
        dialog=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Пакеты · ")+ProjectFiles.environmentLabel(files.profile)).setView(body).setNeutralButton(tr("История","History"),(d,w)->{}).setNegativeButton(WorkspaceText.tr("Закрыть"),null).create();dialog.setOnDismissListener(d->close());dialog.show();dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->showHistory());load();
    }
    private void renderTabs(){if(dialog!=null&&dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(tab==0?android.view.View.VISIBLE:android.view.View.GONE);tabs.removeAllViews();String[] names={WorkspaceText.tr("Установленные"),WorkspaceText.tr("Доступные"),WorkspaceText.tr("Обновления")};for(int i=0;i<3;i++){final int next=i;TextView t=compactAction(activity,names[i],()->{if(tab==next)return;tab=next;limit=100;entries=Collections.emptyList();renderTabs();load();});t.setSelected(tab==i);t.setBackground(WorkspaceDialogs.background(activity,tab==i));tabs.addView(t,compactParams());}}
    private void load(){if(closed)return;int request=++generation,selected=tab;busy=true;entries=Collections.emptyList();render();status.setText(WorkspaceText.tr("Загрузка пакетов…"));worker.submit(()->{if(closed)return;List<PackageCatalog.Entry> result=null;String error=null;try{if(backend==null)backend=files.packageBackend();result=EnvironmentPackages.parse(backend,files.packageQuery(backend,selected),selected);if(selected==0)recordHistory(result);}catch(Exception e){error=e.getMessage();}List<PackageCatalog.Entry> loaded=result;String message=error;activity.runOnUiThread(()->{if(closed||generation!=request)return;busy=false;if(backend!=null){catalogHint.setText(backend.name()+" · "+ProjectFiles.environmentLabel(files.profile)+"\n"+tr("Чтение локального каталога. Если он пуст или устарел, обновите его явно.","Reads local metadata. Refresh it explicitly if empty or outdated."));refreshButton.setText(backend==EnvironmentPackages.Kind.PACMAN?tr("Обновить систему","Upgrade system"):WorkspaceText.tr("Обновить каталог"));}if(message!=null){status.setText(message);return;}entries=loaded;status.setText(Integer.toString(entries.size()));render();});});}
    private void recordHistory(List<PackageCatalog.Entry> installed){
        try{PackageOperationJournal.collect(activity.getFilesDir(),null);String key=WorkspacePackageHistory.key(files.profile);if(PackageOperationJournal.pending(activity.getFilesDir(),key).length()==0)WorkspacePackageHistory.observe(activity.getFilesDir(),key,installed,System.currentTimeMillis());historyUnavailable=false;}catch(Exception error){historyUnavailable=true;}
    }
    private void showHistory(){
        if(closed||tab!=0)return;
        AlertDialog progress=WorkspaceDialogs.builder(activity).setTitle(tr("История пакетов","Package history")).setMessage(tr("Загрузка истории…","Loading history…")).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();progress.show();
        worker.submit(()->{
            org.json.JSONArray records=null;
            try{if(!historyUnavailable){PackageOperationJournal.collect(activity.getFilesDir(),null);String key=WorkspacePackageHistory.key(files.profile);records=WorkspacePackageHistory.read(activity.getFilesDir(),key);org.json.JSONArray pending=PackageOperationJournal.pending(activity.getFilesDir(),key);records=WorkspacePackageHistory.merge(records,pending);}}catch(Exception error){}
            org.json.JSONArray result=records;
            activity.runOnUiThread(()->{
                if(closed||!progress.isShowing())return;progress.dismiss();LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(12));
                body.addView(text(tr("Общая история сессий и пресетов этого окружения. Действия из нашего менеджера записываются с результатом. Прочие изменения обнаруживаются при чтении установленного списка. Дата — время получения результата или обнаружения. Последние 300 записей.","Shared by sessions and presets in this environment. Actions from this manager include their outcome. Other changes are detected when the installed list is read. Dates show result collection or detection time. Latest 300 records."),12));
                if(result==null)body.addView(text(tr("Не удалось прочитать историю. Перечитайте установленный список и повторите.","Could not read history. Reload the installed list and retry."),14));
                else if(result.length()==0)body.addView(text(tr("Изменений пока нет. Начальный список сохранён; следующие установки, удаления и смены версий появятся после перечитывания списка.","No changes yet. The initial list is saved; later installs, removals and version changes appear after reloading the list."),14));
                else for(int i=result.length()-1;i>=0;i--){
                    org.json.JSONObject row=result.optJSONObject(i);if(row==null)continue;String action=row.optString("action");String label=action.equals("installed")?tr("Установлен","Installed"):action.equals("removed")?tr("Удалён","Removed"):tr("Версия изменена","Version changed");
                    String versions=action.equals("installed")?row.optString("after"):action.equals("removed")?row.optString("before"):row.optString("before")+" → "+row.optString("after");
                    if(action.equals("operation")){
                        String op=row.optString("operation"),phase=row.optString("phase");String noun=op.equals("remove")?tr("Удаление","Removal"):op.equals("upgrade")?tr("Обновление","Upgrade"):op.equals("version")?tr("Установка версии","Version installation"):tr("Установка","Installation");
                        label=noun+" · "+(phase.equals("success")?tr("завершено","completed"):phase.equals("cancelled")?tr("отменено","cancelled"):phase.equals("failure")?tr("ошибка или отмена","failed or declined"):tr("результат не получен","result not received"));
                        if(phase.equals("failure"))label+=" · "+tr("код ","code ")+row.optInt("exit");
                        versions=row.optBoolean("reconciled")?row.optString("before")+" → "+row.optString("after"):row.optString("after").isEmpty()?"":tr("Версия в запросе: ","Requested/catalogue version: ")+row.optString("after");
                    }
                    String date=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(row.optLong("observed")));
                    TextView line=text(row.optString("name")+" · "+label+"\n"+versions+"\n"+date,14);line.setPadding(0,dp(10),0,dp(10));line.setTextIsSelectable(true);body.addView(line);
                }
                ScrollView scroll=new ScrollView(activity);scroll.addView(body);LinearLayout frame=column();frame.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(420),activity.getResources().getDisplayMetrics().heightPixels/2)));
                WorkspaceDialogs.builder(activity).setTitle(tr("История пакетов · ","Package history · ")+ProjectFiles.environmentLabel(files.profile)).setView(frame).setNegativeButton(tr("Назад","Back"),null).show();
            });
        });
    }
    private void render(){if(rows==null)return;rows.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);int shown=0,total=0;for(PackageCatalog.Entry e:entries){if(!e.name.toLowerCase(Locale.ROOT).contains(query))continue;total++;if(shown++>=limit)continue;TextView row=action(e.name+"\n"+WorkspaceText.tr("Версия: ")+e.version,()->details(e));rows.addView(row,new LinearLayout.LayoutParams(-1,-2));}if(total>limit)rows.addView(action(WorkspaceText.tr("Показать ещё"),()->{limit+=100;render();}));else if(total==0&&!busy)rows.addView(text(WorkspaceText.tr("Пакеты не найдены"),14));}
    private void details(PackageCatalog.Entry entry){
        if(closed)return;final int selected=tab;LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(8));
        body.addView(text(WorkspaceText.tr("Описание"),12));TextView description=text(WorkspaceText.tr("Загрузка описания…"),14);description.setMaxLines(3);description.setEllipsize(android.text.TextUtils.TruncateAt.END);body.addView(description);
        String[] full={""};TextView more=action(WorkspaceText.tr("Подробнее"),()->fullDescription(entry,full[0]));more.setVisibility(android.view.View.GONE);body.addView(more);
        AlertDialog card=WorkspaceDialogs.builder(activity).setTitle(entry.name+" · "+entry.version).setView(body).setNegativeButton(WorkspaceText.tr("Закрыть"),null).create();
        body.addView(action(selected==1?WorkspaceText.tr("Установить"):WorkspaceText.tr("Обновить пакет"),()->{card.dismiss();confirm(selected==1?"install":"upgrade",entry);}));
        if(backend==EnvironmentPackages.Kind.APT||EnvironmentPackageVersions.supported(backend))body.addView(action(WorkspaceText.tr("Выбрать версию"),()->{card.dismiss();chooseVersion(entry);}));else body.addView(text(tr("Выбор отдельных версий для этого менеджера пока недоступен. Менеджер покажет план установки в терминале.","Version selection is not available for this manager yet. The package manager shows its installation plan in the terminal."),12));
        if(selected==0)body.addView(action(WorkspaceText.tr("Удалить пакет"),()->{card.dismiss();confirm("remove",entry);}));card.show();
        worker.submit(()->{if(closed)return;String result="";try{result=EnvironmentPackages.parseDescription(backend,files.packageDescription(backend,entry,selected==0));}catch(Exception ignored){}String value=result;
            activity.runOnUiThread(()->{if(closed||!card.isShowing())return;full[0]=value;description.setText(value.isEmpty()?WorkspaceText.tr("Описание недоступно. Обновите каталог или проверьте выбранное окружение."):value);more.setVisibility(value.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);});
        });
    }
    private void fullDescription(PackageCatalog.Entry entry,String value){
        if(closed||value.isEmpty())return;LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(8));body.addView(text(WorkspaceText.tr("Описание из репозитория"),12));
        TextView full=text(value,14);full.setTextIsSelectable(true);full.setPadding(0,dp(10),0,dp(10));ScrollView scroll=new ScrollView(activity);scroll.addView(full);body.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(360),activity.getResources().getDisplayMetrics().heightPixels/3)));
        body.addView(text(WorkspaceText.tr("Язык описания зависит от репозитория."),12));WorkspaceDialogs.builder(activity).setTitle(entry.name+" · "+WorkspaceText.tr("Описание")).setView(body).setNegativeButton(WorkspaceText.tr("Закрыть"),null).show();
    }
    private void chooseVersion(PackageCatalog.Entry entry){
        if(backend!=EnvironmentPackages.Kind.APT){chooseNativeVersion(entry);return;}
        if(closed)return;AlertDialog progress=WorkspaceDialogs.builder(activity).setTitle(entry.name).setMessage(WorkspaceText.tr("Загрузка версий…")).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();progress.show();
        worker.submit(()->{if(closed)return;List<PackageCatalog.Entry> versions=null;String failure=null;try{versions=PackageCatalog.parseVersions(entry.name,files.packageVersions(entry.name));}catch(Exception e){failure=e.getMessage();}List<PackageCatalog.Entry> result=versions;String error=failure;
            activity.runOnUiThread(()->{if(closed||!progress.isShowing())return;progress.dismiss();if(error!=null){activity.showToast(error,true);return;}if(result.isEmpty()){activity.showToast(WorkspaceText.tr("В подключённых репозиториях нет доступных версий. Обновите каталог."),true);return;}
                String[] labels=new String[result.size()];for(int i=0;i<labels.length;i++)labels[i]=result.get(i).version;
                WorkspaceDialogs.builder(activity).setTitle(entry.name+" · "+WorkspaceText.tr("Выбрать версию")).setItems(labels,(d,index)->confirm("version",result.get(index))).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
            });
        });
    }
    private void chooseNativeVersion(PackageCatalog.Entry entry){
        if(closed||!EnvironmentPackageVersions.supported(backend))return;
        final EnvironmentPackages.Kind kind=backend;
        AlertDialog progress=WorkspaceDialogs.builder(activity).setTitle(entry.name).setMessage(WorkspaceText.tr("Загрузка версий…")).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();progress.show();
        worker.submit(()->{
            List<EnvironmentPackageVersions.Version> versions=null;String failure=null;
            try{if(closed)return;versions=EnvironmentPackageVersions.parse(kind,entry.name,files.packageVersions(kind,entry.name));}catch(Exception error){failure=error.getMessage();}
            List<EnvironmentPackageVersions.Version> result=versions;String error=failure;
            activity.runOnUiThread(()->{
                if(closed||!progress.isShowing())return;progress.dismiss();
                if(error!=null){activity.showToast(tr("Не удалось прочитать версии. Обновите каталог выбранного окружения и повторите.","Could not read versions. Refresh the selected environment's catalogue and retry."),true);return;}
                if(result.isEmpty()){activity.showToast(WorkspaceText.tr("В подключённых репозиториях нет доступных версий. Обновите каталог."),true);return;}
                String[] labels=new String[result.size()];for(int i=0;i<labels.length;i++)labels[i]=result.get(i).label();
                String title=entry.name+" · "+WorkspaceText.tr("Выбрать версию")+(result.size()==1?"\n"+tr("Других версий в подключённых репозиториях нет","No other versions in enabled repositories"):"");
                WorkspaceDialogs.builder(activity).setTitle(title).setItems(labels,(d,index)->confirm("version",entry,result.get(index))).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
            });
        });
    }
    private void confirm(String action,PackageCatalog.Entry entry){
        confirm(action,entry,null);
    }
    private void confirm(String action,PackageCatalog.Entry entry,EnvironmentPackageVersions.Version selectedVersion){
        if(closed||backend==null)return;String command=selectedVersion!=null?EnvironmentPackageVersions.operation(backend,selectedVersion):action.equals("version")?PackageCatalog.versionOperation(entry):EnvironmentPackages.operation(backend,action,entry==null?null:entry.name);
        LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(8));
        body.addView(text(command,14));
        if(selectedVersion!=null){
            body.addView(text(selectedVersion.label(),13));
            TextView warning=text(tr("Выбрана точная версия. Она может быть старее установленной; зависимости могут измениться. Менеджер покажет план в терминале и запросит подтверждение. Если версия исчезла из репозитория, установка завершится ошибкой.","An exact version is selected. It may be older than the installed one and dependencies may change. The manager will show its plan in the terminal and ask for confirmation. Installation will fail if the version is no longer in the repository."),13);warning.setTextColor(WorkspaceDialogs.DANGER);body.addView(warning);
            if(backend==EnvironmentPackages.Kind.APK)body.addView(text(tr("Alpine сохранит ограничение на эту версию в world. Последующие обновления пакета могут потребовать снять это ограничение.","Alpine records this exact-version constraint in world. Future package upgrades may require removing the constraint."),13));
        }
        if(backend==EnvironmentPackages.Kind.PACMAN&&!action.equals("remove"))body.addView(text(WorkspaceLanguage.code(activity).equals("ru")?"Pacman выполнит полное обновление окружения вместе с выбранным действием. Частичные обновления Arch не поддерживает; проверьте план перед подтверждением.":"Pacman performs a full environment upgrade with this action. Arch does not support partial upgrades; review the plan before confirming.",13));
        if(action.equals("version")&&entry.downgrade){TextView warning=text(WorkspaceText.tr("Выбрана более старая версия. Понижение может потребовать изменения зависимостей; проверьте план APT перед подтверждением."),13);warning.setTextColor(WorkspaceDialogs.DANGER);body.addView(warning);}
        body.addView(text(tr("В выбранной сессии должна быть свободная оболочка без незавершённого ввода. Если работает агент или выполняется команда, выберите новую сессию того же окружения. Пользователь сам проверяет, свободна ли оболочка.","The selected session must have an idle shell with no unfinished input. If an agent or command is running, choose a new session in the same environment. Check that the shell is idle yourself."),13));
        RadioGroup choices=new RadioGroup(activity);
        RadioButton selected=new RadioButton(activity);selected.setId(android.view.View.generateViewId());selected.setText(WorkspaceText.tr("В выбранной сессии"));selected.setTextColor(WorkspaceDialogs.TEXT);choices.addView(selected);
        RadioButton separate=new RadioButton(activity);separate.setId(android.view.View.generateViewId());separate.setText(WorkspaceText.tr("В новой сессии того же окружения"));separate.setTextColor(WorkspaceDialogs.TEXT);choices.addView(separate);
        choices.check(selected.getId());body.addView(choices);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Пакеты · ")+ProjectFiles.environmentLabel(files.profile)).setView(scroll)
            .setPositiveButton(WorkspaceText.tr("Продолжить"),(d,w)->{if(closed)return;boolean current=choices.getCheckedRadioButtonId()==selected.getId();WorkspaceStore.Session operation=PackageCatalog.operationProfile(files.profile,command);if(entry!=null&&!action.equals("refresh"))operation.packageOperation=new PackageOperationJournal.Intent(action,entry.name,selectedVersion==null?entry.version:selectedVersion.version);close();launch.run(operation,current);})
            .setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    public void close(){if(closed)return;closed=true;++generation;if(dialog!=null&&dialog.isShowing())dialog.dismiss();try{files.close();}finally{worker.shutdownNow();entries=Collections.emptyList();Runnable callback=onClosed;onClosed=null;if(callback!=null)callback.run();}}
}
