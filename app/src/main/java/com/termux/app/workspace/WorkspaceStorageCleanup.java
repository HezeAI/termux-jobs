package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.Environment;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import org.json.*;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;

/** Complete component overview; explicit guarded deletion, no implicit app reset. */
final class WorkspaceStorageCleanup {
    private final TermuxActivity activity;
    private final Predicate<String> inUse;
    private final Runnable restored,changed,histories;
    private final WorkspaceStore store;
    private final Predicate<WorkspaceStore.Session> live;
    private String archiveRoot=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    WorkspaceStorageCleanup(TermuxActivity activity,WorkspaceStore store,Predicate<String> inUse,Predicate<WorkspaceStore.Session> live,Runnable restored,Runnable histories,Runnable changed){this.activity=activity;this.store=store;this.inUse=inUse;this.live=live;this.restored=restored;this.histories=histories;this.changed=changed;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    static String label(android.app.Activity a){return WorkspaceLanguage.code(a).equals("ru")?"Очистить хранилище":"Clean up storage";}
    static String scope(WorkspaceStore.Session profile){String env=ProjectFiles.environment(profile);return env.startsWith("proot:")?env.substring(6):env.equals("ubuntu")?"ubuntu":env.equals("termux")?"termux":"";}
    static boolean scopeInUse(WorkspaceStore store,String scope,Set<String> runningHandles){
        if(scope.isEmpty())return false;
        for(WorkspaceStore.Session profile:store.sessions)if(runningHandles.contains(profile.handle)&&scope.equals(scope(profile))&&!WorkspaceStore.isBase(profile))return true;
        return false;
    }
    static boolean idleBaseShell(String stat){
        try{int end=stat.lastIndexOf(')');String[] fields=stat.substring(end+1).trim().split("\\s+");int pgrp=Integer.parseInt(fields[2]),foreground=Integer.parseInt(fields[5]);return end>0&&pgrp>0&&pgrp==foreground;}catch(Exception invalid){return false;}
    }
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);int p=WorkspaceDialogs.dp(activity,16);v.setPadding(p,0,p,p);return v;}
    private TextView info(LinearLayout body,String value){TextView v=new TextView(activity);v.setText(value);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(13);body.addView(v);return v;}
    private void action(LinearLayout body,String value,Runnable click){TextView v=WorkspaceDialogs.action(activity,value,0);v.setOnClickListener(w->click.run());body.addView(v);}
    private ScrollView scroll(LinearLayout body){ScrollView s=new ScrollView(activity);s.addView(body);return s;}
    private String size(long bytes){return android.text.format.Formatter.formatFileSize(activity,Math.max(0,bytes));}
    private JSONObject request(String operation){try{return WorkspaceBackupEngine.base(activity,operation).put("archiveRoot",archiveRoot).put("overview",operation.equals("cleanup-inventory"));}catch(Exception error){throw new IllegalStateException(error);}}
    void show(){scan();}
    private void message(String value){if(!activity.isDestroyed())WorkspaceDialogs.builder(activity).blockOutsideDismiss().setTitle(label(activity)).setMessage(value).setPositiveButton("OK",null).show();}
    private String error(String code){
        switch(code){
            case "CLEANUP_ENVIRONMENT_BUSY":return t("Окружение или менеджер пакетов используется. Закройте связанные сессии и повторите анализ.","The environment or package manager is in use. Close related sessions and rescan.");
            case "CLEANUP_CONSENT_REQUIRED":return t("Подтвердите, что данные могут быть потеряны навсегда.","Acknowledge that data may be permanently lost.");
            case "CLEANUP_PLAN_EXPIRED":case "CLEANUP_CHANGED_RESCAN":return t("Данные изменились или анализ устарел. Выполните анализ заново.","Data changed or the scan expired. Rescan before deleting.");
            case "CLEANUP_ENVIRONMENT_REMOVE_FAILED":return t("Окружение не удалено полностью. Оно может использоваться другой сессией. Закройте её и обновите список; принудительная остановка не выполняется.","Environment removal did not complete. Another session may be using it. Close it and refresh; no sessions are forcibly stopped.");
            case "CLEANUP_OVERLAPPING_SELECTION":return t("Выбрано окружение целиком и данные внутри него. Снимите отдельные пункты внутри этой среды.","A whole environment and data inside it are both selected. Deselect the nested items.");
            case "RESTORED_LOCKS_REQUIRED":return t("Для очистки окружения нужен proot-distro с поддерживаемой блокировкой сессий.","Environment cleanup requires proot-distro with supported session locking.");
            case "CLEANUP_TOO_MANY_FILES":return t("Список слишком большой для этой операции. Выберите меньше пунктов или более узкую папку.","The list is too large for this operation. Select fewer items or a narrower folder.");
            case "PYTHON_REQUIRED":return t("Для анализа нужен Python в Termux: pkg install python.","Scanning requires Python in Termux: pkg install python.");
            case "CANCELLED":return t("Анализ отменён. Выбранные данные не удалялись.","Scan cancelled. Selected data was not deleted.");
            default:return t("Операция не завершена. Выполните анализ заново; часть уже удалённых файлов восстановить автоматически нельзя.","Operation did not complete. Rescan; files already deleted cannot be restored automatically.");
        }
    }
    private void worker(JSONObject request,boolean deleting,java.util.function.Consumer<JSONObject> done){
        WorkspaceBackupEngine engine=new WorkspaceBackupEngine();WorkspaceBackupProgress progress=new WorkspaceBackupProgress(activity);
        AlertDialog.Builder builder=WorkspaceDialogs.builder(activity).blockOutsideDismiss().setTitle(label(activity)).setView(progress.view);
        if(!request.optBoolean("nativeRemoval"))builder.setNegativeButton(t("Отмена","Cancel"),(d,w)->engine.cancel());
        AlertDialog dialog=builder.create();dialog.setCancelable(false);dialog.setOnDismissListener(d->progress.close());
        boolean started=engine.start(activity,request,new WorkspaceBackupEngine.Callback(){
            public void progress(JSONObject event){if(!dialog.isShowing()||progress.isStopping())return;progress.update(t(deleting?"Удаление выбранных файлов":"Анализ хранилища",deleting?"Deleting selected files":"Scanning storage"),event);if(event.optLong("total")==0&&event.optString("unit").equals("files"))progress.amount.setText(event.optLong("processed")+t(" файлов"," files"));}
            public void done(JSONObject result){progress.close();dialog.dismiss();done.accept(result);}
            public void error(String code){progress.close();dialog.dismiss();message(deleting&&code.equals("CANCELLED")?t("Очистка остановлена. Часть файлов могла быть уже удалена. Выполните анализ заново.","Cleanup stopped. Some files may already have been deleted. Rescan storage."):WorkspaceStorageCleanup.this.error(code));}
        });
        if(started){dialog.show();progress.bindCancellation(dialog,engine::cancel);progress.start();}else{progress.close();message(t("Дождитесь завершения другой операции бэкапа или очистки.","Wait for the other backup or cleanup operation to finish."));}
    }
    private void scan(){try{selection(new JSONObject().put("groups",new JSONArray()).put("overview",new JSONArray()),true);}catch(JSONException impossible){throw new IllegalStateException(impossible);}}
    private void chooseFolder(){
        WorkspaceLocalFilePicker picker=new WorkspaceLocalFilePicker(activity);
        picker.chooseDirectory(t("Папка с резервными копиями","Backup folder"),(profile,path)->{
            try{
                String env=ProjectFiles.environment(profile);File physical;
                if(env.startsWith("proot:")){
                    File root=WorkspaceEnvironments.root(new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"),env.substring(6));
                    if(root==null)throw new java.io.IOException();
                    physical=new File(root,path.startsWith("/")?path.substring(1):path).getCanonicalFile();
                    if(!physical.getPath().equals(root.getCanonicalPath())&&!physical.getPath().startsWith(root.getCanonicalPath()+"/"))throw new java.io.IOException();
                }else physical=new File(path).getCanonicalFile();
                archiveRoot=physical.getPath();picker.close();scan();
            }catch(Exception error){picker.close();message(t("Выбранная папка недоступна.","Selected folder is unavailable."));}
        });
    }
    private String kind(JSONObject group){
        switch(group.optString("kind")){
            case "environment":return t("Окружение целиком · ","Whole environment · ")+WorkspaceEnvironments.label(group.optString("scope"));
            case "app-history":return t("Истории оболочек · ","Shell histories · ")+group.optString("name");
            case "home-data":return "HOME · "+WorkspaceEnvironments.label(group.optString("scope").split("/")[0])+" · "+group.optString("name");
            case "cache":return t("Кэш пакетов · ","Package cache · ")+WorkspaceEnvironments.label(group.optString("scope"));
            case "reports":return t("Сохранённые отчёты диагностики","Saved diagnostic reports");
            case "partial":return t("Незавершённый бэкап · ","Incomplete backup · ")+group.optString("name");
            default:return t("Архив бэкапа · ","Backup archive · ")+group.optString("name");
        }
    }
    private void selection(JSONObject result){selection(result,false);}
    private void selection(JSONObject result,boolean scanOnOpen){
        boolean overview=result.has("overview");
        WorkspaceBackupScanHeader header=new WorkspaceBackupScanHeader(activity,label(activity));header.ready=!scanOnOpen;
        if(!overview)header.refresh.setVisibility(android.view.View.GONE);
        LinearLayout body=column();
        info(body,t("Выберите компоненты для удаления или откройте их состав. Ничего не отмечено заранее. Закройте сессии выбранной среды. PREFIX самого Termux доступен для просмотра; это runtime приложения.","Select components to delete or open their contents. Nothing is preselected. Close sessions in the selected environment. Termux PREFIX is available for inspection; it is the app runtime."));
        if(overview){overviewRows(body,result);info(body,t("Кэши, отчёты, архивы и окружения","Caches, reports, archives and environments"));info(body,t("Папка с бэкапами: ","Backup folder: ")+archiveRoot);}
        JSONArray groups=result.optJSONArray("groups");List<JSONObject> values=new ArrayList<>();List<CheckBox> checks=new ArrayList<>();
        TextView total=info(body,t("Выбрано: ","Selected: ")+size(0));
        AlertDialog dialog=WorkspaceDialogs.builder(activity).blockOutsideDismiss().setCustomTitle(header.view).setView(scroll(body)).setPositiveButton(t("Продолжить","Continue"),null).setNegativeButton(t("Отмена","Cancel"),null).create();
        Runnable update=()->{long bytes=0;int count=0;for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked()){bytes+=values.get(i).optLong("bytes");count++;}total.setText(t("Выбрано: ","Selected: ")+size(bytes));if(dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(header.ready&&!header.running&&count>0);};
        if(groups!=null)for(int i=0;i<groups.length();i++){
            JSONObject group=groups.optJSONObject(i);if(group==null)continue;
            boolean environment=group.optString("kind").equals("environment");
            CheckBox box=new CheckBox(activity);box.setTextColor(WorkspaceDialogs.TEXT);
            box.setText(kind(group)+"\n"+size(group.optLong("bytes"))+(environment?t(" · включая HOME, программы и настройки"," · including HOME, programs and settings"):" · "+group.optInt("count")+t(" файлов"," files"))+"\n"+group.optString("path")+(!group.optBoolean("selectable",true)?"\n"+t("Нужен proot-distro с блокировкой используемых сред","Requires proot-distro with active-environment locking"):""));
            box.setEnabled(group.optBoolean("selectable",true));body.addView(box);values.add(group);checks.add(box);box.setOnCheckedChangeListener((v,checked)->update.run());
        }
        if(values.isEmpty()&&!scanOnOpen)info(body,t("Компонентов для удаления не найдено.","No deletable components found."));
        if(overview){
            if(result.optBoolean("archiveUnavailable"))action(body,t("Разрешить доступ к памяти и повторить","Allow storage access and retry"),()->activity.requestWorkspaceStorageAccess(()->{dialog.dismiss();scan();}));
            action(body,t("Выбрать другую папку с бэкапами","Choose another backup folder"),()->{dialog.dismiss();chooseFolder();});
        }
        dialog.setCancelable(false);dialog.setOnDismissListener(d->header.close());dialog.show();update.run();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{List<JSONObject> selected=new ArrayList<>();for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked())selected.add(values.get(i));confirmGroups(result,selected,dialog);});
        header.refresh.setOnClickListener(v->inlineScan(dialog,header,body));if(scanOnOpen)inlineScan(dialog,header,body);
    }
    private void inlineScan(AlertDialog dialog,WorkspaceBackupScanHeader header,LinearLayout body){
        if(header.closed||header.running)return;
        WorkspaceBackupEngine engine=new WorkspaceBackupEngine();Map<android.view.View,Boolean> enabled=new IdentityHashMap<>();rememberEnabled(body,enabled);
        for(android.view.View view:enabled.keySet())view.setEnabled(false);
        Button positive=dialog.getButton(AlertDialog.BUTTON_POSITIVE);boolean wasEnabled=positive.isEnabled();positive.setEnabled(false);
        header.start(engine,t("Сканирование…","Scanning…"));
        Runnable restore=()->{for(Map.Entry<android.view.View,Boolean> e:enabled.entrySet())e.getKey().setEnabled(e.getValue());positive.setEnabled(header.ready&&wasEnabled);};
        boolean started=engine.start(activity,request("cleanup-inventory"),new WorkspaceBackupEngine.Callback(){
            public void progress(JSONObject event){if(!header.closed&&dialog.isShowing()){
                // The shared header expects byte counts. For file counts keep only the path.
                try{JSONObject line=new JSONObject(event.toString());if(line.optString("unit").equals("files"))line.put("processed",0);header.progress(t("Сканирование","Scanning"),line);}catch(JSONException ignored){}
            }}
            public void done(JSONObject result){if(header.closed||!dialog.isShowing())return;header.ready=true;header.finish(null);dialog.dismiss();selection(result);}
            public void error(String code){if(header.closed||!dialog.isShowing())return;header.finish(WorkspaceStorageCleanup.this.error(code));restore.run();}
        });
        if(!started){header.finish(t("Другая операция ещё выполняется. Повторите позже.","Another operation is running. Try again later."));restore.run();}
    }
    private void rememberEnabled(android.view.View view,Map<android.view.View,Boolean> enabled){enabled.put(view,view.isEnabled());if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)rememberEnabled(group.getChildAt(i),enabled);}}
    private void confirmGroups(JSONObject result,List<JSONObject> selected,AlertDialog dialog){
        if(selected.isEmpty()||blocked(selected))return;
        Set<String> environments=new HashSet<>();for(JSONObject g:selected)if(g.optString("kind").equals("environment"))environments.add(g.optString("scope"));
        for(JSONObject g:selected)if(!g.optString("kind").equals("environment")&&environments.contains(g.optString("scope").split("/")[0])){message(error("CLEANUP_OVERLAPPING_SELECTION"));return;}
        StringBuilder summary=new StringBuilder();long bytes=0;for(JSONObject g:selected){summary.append("• ").append(kind(g)).append(" · ").append(size(g.optLong("bytes"))).append('\n');bytes+=g.optLong("bytes");}
        summary.append('\n').append(t("Выбрано к удалению: ","Selected for deletion: ")).append(size(bytes)).append('\n').append(t("Удаление без корзины. Выбранные окружения удаляются целиком, включая HOME, установленные программы, проекты и авторизацию. Пресеты запуска и архивы бэкапов вне выбранных папок сохраняются. Отдельные файлы HOME также могут содержать авторизацию. При ошибке возможен частичный результат.","Deletion bypasses trash. Selected environments are removed in full, including HOME, installed programs, projects and authentication. Launch presets and backup archives outside selected folders are retained. Selected HOME files may also contain authentication. A failure can leave a partial result."));
        if(!environments.isEmpty())summary.append("\n\n").append(t("После начала штатного удаления окружений отмена недоступна. Дождитесь результата.","Once native environment removal starts, cancellation is unavailable. Wait for the result."));
        else summary.append("\n").append(t("Отмена прекращает дальнейшее удаление, но не возвращает удалённое.","Cancel stops further deletion but cannot restore removed files."));
        StorageDeletionConsent.show(activity,label(activity),summary.toString(),()->{
            if(blocked(selected))return;
            try{
                JSONArray ids=new JSONArray();for(JSONObject g:selected)ids.put(g.getString("id"));
                JSONObject req=request("cleanup-execute").put("plan",result.getString("plan")).put("selected",ids).put("acknowledged",true).put("nativeRemoval",!environments.isEmpty());
                dialog.dismiss();worker(req,true,r->{
                    BackupInventoryCache.SHARED.clear();
                    JSONArray removed=r.optJSONArray("removedEnvironments");String text=t("Удалено отдельных файлов: ","Individual files deleted: ")+r.optInt("files")+"\n"+t("Объём отдельных файлов: ","Individual file sizes: ")+size(r.optLong("bytes"));
                    if(removed!=null&&removed.length()>0){text+="\n"+t("Удалены окружения: ","Environments removed: ");for(int i=0;i<removed.length();i++)text+=(i==0?"":", ")+WorkspaceEnvironments.label(removed.optString(i));changed.run();}
                    JSONArray errors=r.optJSONArray("errors");if(errors!=null&&errors.length()>0){text+="\n\n"+t("Часть данных не удалена:\n","Some data was not deleted:\n");for(int i=0;i<errors.length();i++)text+=error(errors.optJSONObject(i).optString("code"))+"\n";}
                    message(text);
                });
            }catch(Exception invalid){message(error("CLEANUP_PLAN_INVALID"));}
        });
    }

    private void overviewRows(LinearLayout body,JSONObject result){
        info(body,t("Данные приложения","App data"));
        libraryRow(body,"commands",t("Команды","Commands"));libraryRow(body,"notes",t("Заметки","Notes"));
        libraryRow(body,"sessions",t("Пресеты запуска","Launch presets"));libraryRow(body,"recentCommands",t("Недавние команды","Recent commands"));
        action(body,t("Истории чатов Codex · просмотр и удаление","Codex chat histories · view and delete"),histories);
        JSONArray sections=result.optJSONArray("overview");if(sections==null)return;
        info(body,t("Termux и Linux-окружения","Termux and Linux environments"));
        for(int i=0;i<sections.length();i++){
            JSONObject section=sections.optJSONObject(i);if(section==null)continue;
            String scope=section.optString("scope"),name=WorkspaceEnvironments.label(scope.split("/")[0]);
            boolean history=section.optString("kind").equals("history"),home=section.optString("kind").equals("home");
            String title=history?t("Истории оболочек приложения","App shell histories")+" · "+size(section.optLong("bytes")):name+" · "+(home?(scope.endsWith("/home")?"/home":"HOME"):(scope.equals("termux")?"PREFIX":"/ · "+t("Система","System")))+" · "+size(section.optLong("bytes"));
            action(body,title,()->component(section,title,home||history,result));
        }
    }
    private void component(JSONObject section,String title,boolean removable,JSONObject inventory){
        LinearLayout body=column();info(body,section.optString("path"));
        if(section.optString("kind").equals("history"))info(body,t("Это сохранённый вывод оболочек приложения. Файлы выбираются по ID сессии; собственные разговоры CLI-агентов находятся в HOME и здесь не удаляются.","These are app-side shell records. Folders are identified by session ID; native CLI conversations are stored in HOME and are not deleted here."));
        info(body,t(removable?"Выберите папки и файлы. Настройки, авторизация и проекты внутри выбранного будут удалены. Ссылки удаляются сами, без удаления их целей. Размер приблизительный.":"Состав системы доступен для просмотра. Целиком удалять PREFIX или системные файлы из работающего приложения нельзя: это повредит запуск и пакетный менеджер. Пакеты удаляйте через «Установка пакетов». Отдельное окружение можно удалить целиком кнопкой ниже после закрытия его сессий.",removable?"Select folders and files. Settings, authentication and projects inside the selection will be deleted. Links themselves are removed, not their targets. Sizes are approximate.":"System contents are available for inspection. Removing PREFIX or system files from the running app would break startup and the package manager. Remove packages through Package installation. An entire environment can be removed with the button below after closing its sessions."));
        JSONArray entries=section.optJSONArray("entries");List<JSONObject> values=new ArrayList<>();List<CheckBox> checks=new ArrayList<>();
        if(entries!=null)for(int i=0;i<entries.length();i++){
            JSONObject e=entries.optJSONObject(i);if(e==null)continue;
            String text=e.optString("name")+" · "+size(e.optLong("bytes"))+" · "+e.optLong("count")+t(" файлов"," files")+(e.optLong("unreadable")>0?t(" · часть недоступна"," · partially unreadable"):"");
            if(!removable){info(body,text);continue;}
            CheckBox check=new CheckBox(activity);check.setTextColor(WorkspaceDialogs.TEXT);check.setText(text);check.setEnabled(e.optJSONArray("identity")!=null&&e.optLong("unreadable")==0);body.addView(check);values.add(e);checks.add(check);
        }
        if(entries==null||entries.length()==0)info(body,t("Пусто","Empty"));
        AlertDialog.Builder builder=WorkspaceDialogs.builder(activity).blockOutsideDismiss().setTitle(title).setView(scroll(body)).setNegativeButton(t("Отмена","Cancel"),null);
        if(removable)builder.setPositiveButton(t("Продолжить","Continue"),null);
        AlertDialog dialog=builder.create();dialog.setCancelable(false);dialog.show();
        if(!removable){
            JSONArray groups=inventory.optJSONArray("groups");if(groups!=null)for(int i=0;i<groups.length();i++){
                JSONObject group=groups.optJSONObject(i);if(group!=null&&group.optString("kind").equals("environment")&&group.optString("scope").equals(section.optString("scope"))){
                    if(group.optBoolean("selectable",true))action(body,t("Удалить это окружение целиком","Delete this entire environment"),()->confirmGroups(inventory,Collections.singletonList(group),dialog));
                    else info(body,error("RESTORED_LOCKS_REQUIRED"));break;
                }
            }
            return;
        }
        Button button=dialog.getButton(AlertDialog.BUTTON_POSITIVE);button.setEnabled(false);
        for(CheckBox c:checks)c.setOnCheckedChangeListener((v,checked)->{boolean selected=false;for(CheckBox item:checks)selected|=item.isChecked();button.setEnabled(selected);});
        button.setOnClickListener(v->{
            String scope=section.optString("scope");for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked()&&scope.equals("app-history")&&inUse.test("history:"+values.get(i).optString("name"))){message(t("Сначала закройте выбранную сессию.","Close the selected session first."));return;}if(!scope.equals("app-history")&&inUse.test(scope.split("/")[0])){message(t("Сначала закройте связанные сессии.","Close related sessions first."));return;}
            JSONArray chosen=new JSONArray();for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked())chosen.put(values.get(i));if(chosen.length()==0)return;
            try{JSONObject req=request("cleanup-prepare-home").put("scope",scope).put("entries",chosen);dialog.dismiss();worker(req,false,this::selection);}catch(JSONException invalid){message(error("CLEANUP_PLAN_INVALID"));}
        });
    }
    private void libraryRow(LinearLayout body,String key,String title){
        try{
            JSONArray all=store.encode().getJSONArray(key);int count=0;long bytes=0;
            for(int i=0;i<all.length();i++){JSONObject e=all.optJSONObject(i);if(key.equals("sessions")&&(e==null||!e.optBoolean("saved")||WorkspaceStore.BASE_TERMINAL_ID.equals(e.optString("id"))))continue;count++;bytes+=all.get(i).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;}
            action(body,title+" · "+count+" · "+size(bytes),()->library(key,title));
        }catch(JSONException invalid){info(body,title+" · "+t("Недоступно","Unavailable"));}
    }
    static boolean clearLibrarySelection(WorkspaceStore store,String key,String expected,Set<String> selected){return clearLibrarySelection(store,key,expected,selected,null);}
    static boolean clearLibrarySelection(WorkspaceStore store,String key,String expected,Set<String> selected,Runnable completed){
        if(!store.isWritable()||selected.isEmpty())return false;
        try{if(!store.encode().getJSONArray(key).toString().equals(expected))return false;}catch(JSONException invalid){return false;}
        boolean found=false;
        if(key.equals("commands"))for(WorkspaceStore.Command c:store.commands)found|=selected.contains(c.id);
        if(key.equals("notes"))for(WorkspaceStore.Note n:store.notes)found|=selected.contains(n.id);
        if(key.equals("sessions"))for(WorkspaceStore.Session p:store.sessions)found|=p.saved&&!WorkspaceStore.isBase(p)&&selected.contains(p.id);
        if(key.equals("recentCommands"))for(int i=0;i<store.recentCommands.size();i++)found|=selected.contains(Integer.toString(i));
        if(!found)return false;
        switch(key){
            case "commands":store.commands.removeIf(e->selected.contains(e.id));break;
            case "notes":store.notes.removeIf(e->selected.contains(e.id));break;
            case "sessions":store.sessions.removeIf(e->e.saved&&!WorkspaceStore.isBase(e)&&selected.contains(e.id));break;
            case "recentCommands":for(int i=store.recentCommands.size()-1;i>=0;i--)if(selected.contains(Integer.toString(i)))store.recentCommands.remove(i);break;
            default:return false;
        }
        store.save(completed);return true;
    }
    private void library(String key,String title){
        try{
            JSONArray all=store.encode().getJSONArray(key);String expected=all.toString();LinearLayout body=column();List<CheckBox> checks=new ArrayList<>();List<String> ids=new ArrayList<>();
            info(body,t("Выберите записи. Будут удалены только записи библиотеки; файлы проектов и собственные чаты CLI сохраняются.","Select entries. Only library entries are removed; project files and native CLI conversations are retained."));
            for(int i=0;i<all.length();i++){
                JSONObject e=all.optJSONObject(i);String id=key.equals("recentCommands")?Integer.toString(i):e==null?"":e.optString("id");
                if(key.equals("sessions")&&(e==null||!e.optBoolean("saved")||WorkspaceStore.BASE_TERMINAL_ID.equals(id)))continue;
                CheckBox check=new CheckBox(activity);check.setTextColor(WorkspaceDialogs.TEXT);String preview=e==null?all.optString(i):e.optString("name");if(e!=null){String content=e.optString("command",e.optString("text"));if(!content.isEmpty())preview+=(preview.isEmpty()?"":"\n")+(content.length()>512?content.substring(0,512)+"…":content);}check.setText(preview);body.addView(check);checks.add(check);ids.add(id);
            }
            AlertDialog dialog=WorkspaceDialogs.builder(activity).blockOutsideDismiss().setTitle(title).setView(scroll(body)).setNegativeButton(t("Отмена","Cancel"),null).setPositiveButton(t("Продолжить","Continue"),null).create();dialog.setCancelable(false);dialog.show();Button button=dialog.getButton(AlertDialog.BUTTON_POSITIVE);button.setEnabled(false);
            for(CheckBox c:checks)c.setOnCheckedChangeListener((v,b)->{boolean any=false;for(CheckBox item:checks)any|=item.isChecked();button.setEnabled(any);});
            button.setOnClickListener(v->{Set<String> selected=new HashSet<>();for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked())selected.add(ids.get(i));if(selected.isEmpty())return;
                Runnable remove=()->{if(key.equals("sessions"))for(WorkspaceStore.Session p:store.sessions)if(selected.contains(p.id)&&live.test(p)){message(t("Сначала закройте выбранные сессии.","Close the selected sessions first."));return;}
                    if(!clearLibrarySelection(store,key,expected,selected,()->{if(activity.isDestroyed())return;changed.run();message(t("Выбранные записи удалены.","Selected entries deleted."));})){message(t("Список изменился или недоступен. Откройте его заново.","The list changed or is unavailable. Reopen it."));return;}dialog.dismiss();};
                StorageDeletionConsent.show(activity,title,t("Будут удалены выбранные записи: ","Selected entries to delete: ")+selected.size(),remove);
            });
        }catch(JSONException invalid){message(error("CLEANUP_PLAN_INVALID"));}
    }

    private boolean blocked(List<JSONObject> groups){for(JSONObject g:groups){JSONArray busy=g.optJSONArray("busyScopes");if(busy!=null)for(int i=0;i<busy.length();i++)if(inUse.test(busy.optString(i))){message(t("Сначала закройте выбранные сессии.","Close the selected sessions first."));return true;}String scope=g.optString("scope").split("/")[0];if(!scope.isEmpty()&&inUse.test(scope)){message(t("Сначала закройте сессии окружения: ","Close sessions in this environment first: ")+WorkspaceEnvironments.label(scope));return true;}}return false;}
}
