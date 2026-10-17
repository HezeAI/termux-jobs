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
    private List<PackageCatalog.Entry> entries=Collections.emptyList();private int tab,limit=100,generation;private volatile boolean closed;private boolean busy;
    WorkspacePackages(TermuxActivity activity,WorkspaceStore.Session profile,Launch launch){this.activity=activity;this.files=new ProjectFiles(activity,profile);this.launch=launch;}
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
        body.addView(text(WorkspaceText.tr("Список из локального каталога APT. Для свежих данных обновите каталог."),12));
        LinearLayout controls=new LinearLayout(activity);controls.addView(compactAction(activity,WorkspaceText.tr("Перечитать список"),this::load),compactParams());controls.addView(compactAction(activity,WorkspaceText.tr("Обновить каталог"),()->confirm("refresh",null)),compactParams());body.addView(controls);
        status=text("",12);body.addView(status);rows=column();ScrollView scroll=new ScrollView(activity);scroll.addView(rows);
        int height=Math.min(dp(330),Math.max(dp(120),(int)(activity.getResources().getDisplayMetrics().heightPixels*0.32)));body.addView(scroll,new LinearLayout.LayoutParams(-1,height));
        dialog=WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Пакеты · ")+ProjectFiles.environmentLabel(files.profile)).setView(body).setNegativeButton(WorkspaceText.tr("Закрыть"),null).create();dialog.setOnDismissListener(d->close());dialog.show();load();
    }
    private void renderTabs(){tabs.removeAllViews();String[] names={WorkspaceText.tr("Установленные"),WorkspaceText.tr("Доступные"),WorkspaceText.tr("Обновления")};for(int i=0;i<3;i++){final int next=i;TextView t=compactAction(activity,names[i],()->{if(tab==next)return;tab=next;limit=100;entries=Collections.emptyList();renderTabs();load();});t.setSelected(tab==i);t.setBackground(WorkspaceDialogs.background(activity,tab==i));tabs.addView(t,compactParams());}}
    private void load(){if(closed)return;int request=++generation,selected=tab;busy=true;entries=Collections.emptyList();render();status.setText(WorkspaceText.tr("Загрузка пакетов…"));worker.submit(()->{if(closed)return;List<PackageCatalog.Entry> result=null;String error=null;try{result=PackageCatalog.parse(files.packageQuery(selected),selected);}catch(Exception e){error=e.getMessage();}List<PackageCatalog.Entry> loaded=result;String message=error;activity.runOnUiThread(()->{if(closed||generation!=request)return;busy=false;if(message!=null){status.setText(message);return;}entries=loaded;status.setText(Integer.toString(entries.size()));render();});});}
    private void render(){if(rows==null)return;rows.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);int shown=0,total=0;for(PackageCatalog.Entry e:entries){if(!e.name.toLowerCase(Locale.ROOT).contains(query))continue;total++;if(shown++>=limit)continue;TextView row=action(e.name+"\n"+WorkspaceText.tr("Версия: ")+e.version,()->details(e));rows.addView(row,new LinearLayout.LayoutParams(-1,-2));}if(total>limit)rows.addView(action(WorkspaceText.tr("Показать ещё"),()->{limit+=100;render();}));else if(total==0&&!busy)rows.addView(text(WorkspaceText.tr("Пакеты не найдены"),14));}
    private void details(PackageCatalog.Entry entry){
        if(closed)return;final int selected=tab;LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(8));
        body.addView(text(WorkspaceText.tr("Описание"),12));TextView description=text(WorkspaceText.tr("Загрузка описания…"),14);description.setMaxLines(3);description.setEllipsize(android.text.TextUtils.TruncateAt.END);body.addView(description);
        String[] full={""};TextView more=action(WorkspaceText.tr("Подробнее"),()->fullDescription(entry,full[0]));more.setVisibility(android.view.View.GONE);body.addView(more);
        AlertDialog card=WorkspaceDialogs.builder(activity).setTitle(entry.name+" · "+entry.version).setView(body).setNegativeButton(WorkspaceText.tr("Закрыть"),null).create();
        body.addView(action(selected==1?WorkspaceText.tr("Установить"):WorkspaceText.tr("Обновить пакет"),()->{card.dismiss();confirm(selected==1?"install":"upgrade",entry);}));
        body.addView(action(WorkspaceText.tr("Выбрать версию"),()->{card.dismiss();chooseVersion(entry);}));
        if(selected==0)body.addView(action(WorkspaceText.tr("Удалить пакет"),()->{card.dismiss();confirm("remove",entry);}));card.show();
        worker.submit(()->{if(closed)return;String result="";try{result=PackageCatalog.parseDescription(files.packageDescription(entry,selected==0));}catch(Exception ignored){}String value=result;
            activity.runOnUiThread(()->{if(closed||!card.isShowing())return;full[0]=value;description.setText(value.isEmpty()?WorkspaceText.tr("Описание недоступно. Обновите каталог или проверьте выбранное окружение."):value);more.setVisibility(value.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);});
        });
    }
    private void fullDescription(PackageCatalog.Entry entry,String value){
        if(closed||value.isEmpty())return;LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(8));body.addView(text(WorkspaceText.tr("Описание из репозитория"),12));
        TextView full=text(value,14);full.setTextIsSelectable(true);full.setPadding(0,dp(10),0,dp(10));ScrollView scroll=new ScrollView(activity);scroll.addView(full);body.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(360),activity.getResources().getDisplayMetrics().heightPixels/3)));
        body.addView(text(WorkspaceText.tr("Язык описания зависит от репозитория."),12));WorkspaceDialogs.builder(activity).setTitle(entry.name+" · "+WorkspaceText.tr("Описание")).setView(body).setNegativeButton(WorkspaceText.tr("Закрыть"),null).show();
    }
    private void chooseVersion(PackageCatalog.Entry entry){
        if(closed)return;AlertDialog progress=WorkspaceDialogs.builder(activity).setTitle(entry.name).setMessage(WorkspaceText.tr("Загрузка версий…")).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();progress.show();
        worker.submit(()->{if(closed)return;List<PackageCatalog.Entry> versions=null;String failure=null;try{versions=PackageCatalog.parseVersions(entry.name,files.packageVersions(entry.name));}catch(Exception e){failure=e.getMessage();}List<PackageCatalog.Entry> result=versions;String error=failure;
            activity.runOnUiThread(()->{if(closed||!progress.isShowing())return;progress.dismiss();if(error!=null){activity.showToast(error,true);return;}if(result.isEmpty()){activity.showToast(WorkspaceText.tr("В подключённых репозиториях нет доступных версий. Обновите каталог."),true);return;}
                String[] labels=new String[result.size()];for(int i=0;i<labels.length;i++)labels[i]=result.get(i).version;
                WorkspaceDialogs.builder(activity).setTitle(entry.name+" · "+WorkspaceText.tr("Выбрать версию")).setItems(labels,(d,index)->confirm("version",result.get(index))).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
            });
        });
    }
    private void confirm(String action,PackageCatalog.Entry entry){
        if(closed)return;String command=action.equals("version")?PackageCatalog.versionOperation(entry):PackageCatalog.operation(action,entry==null?null:entry.name);
        LinearLayout body=column();body.setPadding(dp(16),0,dp(16),dp(8));
        body.addView(text(command,14));
        if(action.equals("version")&&entry.downgrade){TextView warning=text(WorkspaceText.tr("Выбрана более старая версия. Понижение может потребовать изменения зависимостей; проверьте план APT перед подтверждением."),13);warning.setTextColor(WorkspaceDialogs.DANGER);body.addView(warning);}
        body.addView(text(WorkspaceText.tr("В выбранной сессии должен быть свободный Bash без незавершённого ввода. Если выполняется команда или работает Codex, откройте в новой сессии того же окружения. Пользователь сам проверяет, свободна ли оболочка."),13));
        RadioGroup choices=new RadioGroup(activity);
        RadioButton selected=new RadioButton(activity);selected.setId(android.view.View.generateViewId());selected.setText(WorkspaceText.tr("В выбранной сессии"));selected.setTextColor(WorkspaceDialogs.TEXT);choices.addView(selected);
        RadioButton separate=new RadioButton(activity);separate.setId(android.view.View.generateViewId());separate.setText(WorkspaceText.tr("В новой сессии того же окружения"));separate.setTextColor(WorkspaceDialogs.TEXT);choices.addView(separate);
        choices.check(selected.getId());body.addView(choices);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Пакеты · ")+ProjectFiles.environmentLabel(files.profile)).setView(scroll)
            .setPositiveButton(WorkspaceText.tr("Продолжить"),(d,w)->{if(closed)return;boolean current=choices.getCheckedRadioButtonId()==selected.getId();WorkspaceStore.Session operation=PackageCatalog.operationProfile(files.profile,command);close();launch.run(operation,current);})
            .setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    public void close(){if(closed)return;closed=true;++generation;if(dialog!=null&&dialog.isShowing())dialog.dismiss();try{files.close();}finally{worker.shutdownNow();entries=Collections.emptyList();Runnable callback=onClosed;onClosed=null;if(callback!=null)callback.run();}}
}
