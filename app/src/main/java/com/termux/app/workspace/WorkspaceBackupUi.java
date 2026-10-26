package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Environment;
import android.view.View;
import android.widget.*;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/** Deliberate backup/restore flows. No operation runs merely from opening settings. */
final class WorkspaceBackupUi {
    private int compressionLevel=3,compressionThreads=1;private String compressionEngine="gzip";private boolean installingPigz;
    private final TermuxActivity activity;private final WorkspaceStore store;private final Runnable changed;private final Consumer<String> packageInstall;
    private WorkspaceBackupUi(TermuxActivity a,WorkspaceStore s,Runnable r,Consumer<String> install){activity=a;store=s;changed=r;packageInstall=install;}
    static String text(Activity a,String ru,String en){return WorkspaceLanguage.code(a).equals("ru")?ru:en;}
    private String t(String ru,String en){return text(activity,ru,en);}
    static String label(Activity a){return text(a,"Резервное копирование","Backup and restore");}
    static void show(TermuxActivity a,WorkspaceStore s,Runnable changed,Consumer<String> install){new WorkspaceBackupUi(a,s,changed,install).menu();}
    static void showRestored(TermuxActivity a,WorkspaceStore s,Runnable changed){new WorkspaceBackupUi(a,s,changed,command->{}).restoredEnvironments();}
    static WorkspaceDialogs.Builder backupBuilder(Activity a){return WorkspaceDialogs.builder(a).blockOutsideDismiss();}
    private void backupCreated(JSONObject result){message(t("Бэкап создан и проверен","Backup created and verified")+"\n"+result.optString("path"));}
    private void message(String text){if(!activity.isDestroyed())backupBuilder(activity).setTitle(label(activity)).setMessage(text).setPositiveButton("OK",null).show();}
    private LinearLayout column(){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);int d=WorkspaceDialogs.dp(activity,16);v.setPadding(d,d,d,d);return v;}
    private void info(LinearLayout form,String value){TextView v=new TextView(activity);v.setText(value);v.setTextColor(WorkspaceDialogs.TEXT);v.setTextSize(14);v.setPadding(0,8,0,12);form.addView(v);}
    private CheckBox check(LinearLayout form,String value,boolean selected){CheckBox b=new CheckBox(activity);b.setText(value);b.setTextColor(WorkspaceDialogs.TEXT);b.setChecked(selected);form.addView(b);return b;}
    private EditText field(LinearLayout form,String value,String hint){EditText e=new EditText(activity);e.setSingleLine();e.setText(value);e.setHint(hint);e.setTextColor(WorkspaceDialogs.TEXT);e.setSelectAllOnFocus(true);form.addView(e);return e;}
    private void action(LinearLayout form,String title,Runnable click){TextView b=WorkspaceDialogs.action(activity,title,0);b.setOnClickListener(v->click.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=WorkspaceDialogs.dp(activity,8);form.addView(b,p);}
    private ScrollView scroll(LinearLayout form){ScrollView s=new ScrollView(activity);s.setFillViewport(false);s.addView(form);return s;}
    private String compressionName(){return compressionEngine+" · "+compressionThreads+t(compressionThreads==1?" поток":" потока",compressionThreads==1?" thread":" threads");}
    private String levelName(){return compressionLevel==1?t("Быстрое","Fast"):compressionLevel==9?t("Максимальное","Maximum"):t("Сбалансированное","Balanced");}
    private JSONObject compressedRequest(JSONObject request)throws JSONException{return request.put("compressionLevel",compressionLevel).put("compressionEngine",compressionEngine).put("compressionThreads",compressionThreads);}
    private boolean pigzAvailable(){return new File(WorkspaceBackupEngine.prefix(),"bin/pigz").canExecute();}
    private void compressionControl(LinearLayout form){
        info(form,t("Способ сжатия · .tar.gz","Compression method · .tar.gz"));
        TextView control=WorkspaceDialogs.action(activity,compressionName(),0);form.addView(control);
        control.setOnClickListener(v->backupBuilder(activity).setTitle(t("Способ сжатия","Compression method")).setItems(new String[]{t("gzip — 1 поток","gzip — 1 thread"),t("pigz — 2 потока","pigz — 2 threads"),t("pigz — 4 потока","pigz — 4 threads")},(d,w)->{
            if(w==0){compressionEngine="gzip";compressionThreads=1;control.setText(compressionName());}
            else if(pigzAvailable()){compressionEngine="pigz";compressionThreads=w==1?2:4;control.setText(compressionName());}
            else offerPigz(control,w==1?2:4);
        }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        info(form,t("Уровень сжатия","Compression level"));
        TextView level=WorkspaceDialogs.action(activity,levelName(),0);form.addView(level);
        level.setOnClickListener(v->backupBuilder(activity).setTitle(t("Уровень сжатия","Compression level")).setItems(new String[]{t("Быстрое · уровень 1, архив больше","Fast · level 1, larger archive"),t("Сбалансированное · уровень 3","Balanced · level 3"),t("Максимальное · уровень 9, медленнее","Maximum · level 9, slower")},(d,w)->{compressionLevel=new int[]{1,3,9}[w];level.setText(levelName());}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show());
        info(form,t("Все варианты создают совместимый .tar.gz. Уровень 3 выбран по умолчанию. Ускорение зависит от накопителя и состава файлов. Полная Ubuntu на старом proot-distro использует штатное сжатие независимо от выбора.","All options create compatible .tar.gz archives. Level 3 is the default. Speed depends on storage and file mix. Full Ubuntu backups on legacy proot-distro use native compression regardless of this selection."));
    }
    private void offerPigz(TextView control,int threads){
        if(installingPigz)return;
        backupBuilder(activity).setTitle(t("Установить pigz?","Install pigz?")).setMessage(t("Пакет pigz отсутствует. Установить его в общую среду Termux в фоне? Нужен интернет; APT может добавить зависимости. После успешной установки будет выбран pigz. Сжатие gzip доступно без установки.","pigz is missing. Install it into the shared Termux environment in the background? Internet is required; APT may add dependencies. Successful installation selects pigz. gzip works without installation.")).setPositiveButton(t("Установить","Install"),(d,w)->{
            WorkspaceBackupEngine engine=new WorkspaceBackupEngine();installingPigz=true;control.setEnabled(false);control.setText(t("Установка pigz…","Installing pigz…"));
            boolean started=engine.start(activity,request("install-pigz"),new WorkspaceBackupEngine.Callback(){
                public void progress(JSONObject event){}
                public void done(JSONObject result){installingPigz=false;control.setEnabled(true);if(pigzAvailable()){compressionEngine="pigz";compressionThreads=threads;control.setText(compressionName());}else{control.setText(compressionName());message(errorText("PIGZ_INSTALL_FAILED"));}}
                public void error(String code){installingPigz=false;control.setEnabled(true);control.setText(compressionName());message(errorText(code));}
            });
            if(!started){installingPigz=false;control.setEnabled(true);control.setText(compressionName());message(t("Другая операция ещё выполняется. Дождитесь её завершения и повторите установку.","Another operation is running. Wait for it to finish and retry installation."));}
        }).setNegativeButton(t("Не сейчас","Not now"),null).show();
    }
    private void menu(){
        LinearLayout form=column();
        action(form,t("Создать резервную копию","Create backup"),this::inventory);
        action(form,t("Проверить резервную копию","Verify backup"),()->chooseFile(false));
        action(form,t("Восстановить из файла","Restore from file"),()->chooseFile(true));
        action(form,t("Импорт/Экспорт Alias","Import/export aliases"),this::aliasTransferMenu);
        backupBuilder(activity).setTitle(label(activity)).setView(scroll(form)).setNegativeButton(WorkspaceText.tr("Закрыть"),null).show();
    }
    private void aliasTransferMenu(){
        LinearLayout form=column();
        action(form,t("Импорт из shell-файла","Import from shell file"),()->aliasSourceFile(true));
        action(form,t("Импорт из JSON/бэкапа","Import from JSON/backup"),()->aliasSourceFile(false));
        action(form,t("Экспорт в shell-файл","Export to shell file"),()->aliasExport(true));
        action(form,t("Экспорт в JSON","Export to JSON"),()->aliasExport(false));
        backupBuilder(activity).setTitle(t("Импорт/Экспорт Alias","Import/export aliases")).setView(scroll(form)).setNegativeButton(WorkspaceText.tr("Назад"),null).show();
    }
    private void aliasExport(boolean shell){
        List<WorkspaceStore.Alias> copy=new ArrayList<>();for(WorkspaceStore.Alias a:store.aliases)copy.add(a.copy());
        if(!shell){aliasPicker(copy,null,false);return;}
        List<String> environments=new ArrayList<>();for(String environment:WorkspaceAliases.ENVIRONMENTS)if(copy.stream().anyMatch(a->a.environment.equals(environment)))environments.add(environment);
        if(environments.isEmpty()){message(t("Сохранённых Alias нет.","No saved aliases found."));return;}
        String[] labels=new String[environments.size()];for(int i=0;i<labels.length;i++)labels[i]=WorkspaceAliases.label(environments.get(i));
        backupBuilder(activity).setTitle(t("Окружение для экспорта","Export environment")).setItems(labels,(d,w)->{List<WorkspaceStore.Alias> selected=new ArrayList<>();for(WorkspaceStore.Alias a:copy)if(a.environment.equals(environments.get(w)))selected.add(a);aliasPicker(selected,null,true);}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    private void aliasSourceFile(boolean shell){
        LinearLayout form=column();info(form,shell?t("Выберите .bashrc, .bash_aliases, .zshrc или другой shell-файл. Будут прочитаны только простые определения alias без выполнения файла. Динамические определения пропускаются. Затем выберите нужные Alias для импорта.","Choose .bashrc, .bash_aliases, .zshrc or another shell file. Only literal alias definitions are read, without executing the file. Dynamic definitions are skipped. Then select aliases to import."):t("Выберите JSON экспорта Alias, workspace-v1.json или manifest.json резервной копии. Файлы окружений не сканируются, команды не выполняются.","Choose an alias-export JSON, workspace-v1.json or backup manifest.json. Environment files are not scanned and commands are not executed."));
        EditText path=field(form,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),t("Путь к файлу","File path"));
        action(form,t("Выбрать файл","Choose file"),()->browse(new File(path.getText().toString()),false,path::setText));
        String[] environment={"termux"};if(shell){info(form,t("Окружение для импорта","Import environment"));TextView target=WorkspaceDialogs.action(activity,"Termux ▾",0);form.addView(target);target.setOnClickListener(v->{List<WorkspaceEnvironments.Choice> choices=WorkspaceAliases.installed(WorkspaceEnvironments.choices());String[] labels=new String[choices.size()];int selected=0;for(int i=0;i<labels.length;i++){WorkspaceEnvironments.Choice c=choices.get(i);labels[i]=c.label;if((c.alias.isEmpty()?"termux":c.alias).equals(environment[0]))selected=i;}backupBuilder(activity).setTitle(t("Окружение","Environment")).setSingleChoiceItems(labels,selected,(d,w)->{WorkspaceEnvironments.Choice c=choices.get(w);environment[0]=c.alias.isEmpty()?"termux":c.alias;target.setText(c.label+" ▾");d.dismiss();}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();});}
        backupBuilder(activity).setTitle(shell?t("Импорт из shell-файла","Import from shell file"):t("Импорт из JSON/бэкапа","Import from JSON/backup")).setView(scroll(form)).setPositiveButton(t("Проверить","Verify"),(d,w)->{try{JSONObject req=request(shell?"alias-inspect-shell":"inspect").put("path",path.getText().toString().trim());if(shell)req.put("environment",environment[0]);run(req,this::aliasImportReport);}catch(Exception e){message(errorText("INVALID_PATH"));}}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
    private void aliasImportReport(JSONObject report){try{aliasPicker(WorkspaceAliasImport.read(activity,report.optJSONObject("workspace")),report);}catch(Exception e){message(t("Не удалось прочитать Alias. Нужен совместимый экспорт или бэкап приложения.","Could not read aliases. Choose a compatible export or app backup."));}}
    private void aliasPicker(List<WorkspaceStore.Alias> aliases,JSONObject source){
        aliasPicker(aliases,source,false);
    }
    private void aliasPicker(List<WorkspaceStore.Alias> aliases,JSONObject source,boolean shell){
        boolean importing=source!=null;if(aliases.isEmpty()){message(importing&&source.optBoolean("aliasShell")?t("Простые определения Alias не найдены. Пропущено неподдерживаемых определений: ","No literal alias definitions found. Unsupported definitions skipped: ")+source.optInt("skipped"):t("Сохранённых Alias нет.","No saved aliases found."));return;}
        LinearLayout form=column();info(form,importing?t("Выберите Alias для импорта в их окружения. Отсутствующее окружение сначала установите. Существующие записи с тем же именем и окружением заменяются только после подтверждения. Импортируйте только доверенные команды.","Select aliases to import into their environments. Install missing environments first. Matching names in the same environment require replacement confirmation. Import only trusted commands."):shell?t("Выберите Alias для экспорта в shell-файл. Команды могут содержать личные данные — проверьте перед передачей файла.","Select aliases to export to a shell file. Commands may contain private data; review them before sharing."):t("Выберите Alias для экспорта. Сохраняются только выбранные определения, окружения, категории и избранное. Команды могут содержать личные данные — проверьте перед передачей файла.","Select aliases to export. Only selected definitions, environments, categories and favorites are saved. Commands may contain private data; review them before sharing."));
        if(importing&&source.optBoolean("aliasShell"))info(form,t("Пропущено неподдерживаемых определений: ","Unsupported definitions skipped: ")+source.optInt("skipped"));
        if(shell)info(form,t("Shell-файл содержит только определения одного окружения. Его можно подключить из .bashrc или .zshrc. Категории и избранное сохраняются только в JSON.","A shell file contains definitions for one environment. It can be sourced from .bashrc or .zshrc. Categories and favorites are retained only in JSON."));
        Set<String> installed=new HashSet<>();if(importing)for(WorkspaceEnvironments.Choice c:WorkspaceAliases.installed(WorkspaceEnvironments.choices()))installed.add(c.alias.isEmpty()?"termux":c.alias);
        List<CheckBox> checks=new ArrayList<>();for(WorkspaceStore.Alias a:aliases){
            boolean available=!importing||installed.contains(a.environment);
            CheckBox box=check(form,a.name+" · "+WorkspaceAliases.label(a.environment)+" · "+(a.category.trim().isEmpty()?WorkspaceText.tr("Общее"):a.category)+(available?"":t(" · окружение не установлено"," · environment not installed")),!importing);box.setEnabled(available);checks.add(box);
            TextView preview=WorkspaceDialogs.action(activity,t("Предпросмотр: ","Preview: ")+a.name,0);form.addView(preview);preview.setOnClickListener(v->{LinearLayout detail=column();info(detail,WorkspaceAliases.label(a.environment));info(detail,a.command);backupBuilder(activity).setTitle(a.name).setView(scroll(detail)).setPositiveButton(WorkspaceText.tr("Закрыть"),null).show();});
        }
        EditText destination=importing?null:field(form,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),t("Каталог экспорта","Export directory"));
        if(!importing)action(form,t("Выбрать каталог","Choose directory"),()->browse(new File(destination.getText().toString()),true,destination::setText));
        AlertDialog dialog=backupBuilder(activity).setTitle(importing?t("Импорт Alias","Import aliases"):t("Экспорт Alias","Export aliases")).setView(scroll(form)).setPositiveButton(importing?t("Импортировать","Import"):t("Экспортировать","Export"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{
            List<WorkspaceStore.Alias> selected=new ArrayList<>();Set<String> ids=new HashSet<>();for(int i=0;i<aliases.size();i++)if(checks.get(i).isEnabled()&&checks.get(i).isChecked()){selected.add(aliases.get(i));ids.add(aliases.get(i).id);}if(selected.isEmpty()){message(t("Выберите хотя бы один Alias.","Select at least one alias."));return;}
            if(!importing){JSONObject req=request("alias-export").put("aliasFormat",shell?"shell":"json").put("workspace",WorkspaceAliasImport.export(selected)).put("destination",destination.getText().toString().trim());run(req,r->{dialog.dismiss();message(t("Alias экспортированы: ","Aliases exported: ")+r.optInt("count")+"\n"+r.optString("path"));});return;}
            WorkspaceAliasImport.Plan plan=WorkspaceAliasImport.plan(aliases,ids,store.aliases,WorkspaceText.tr("Общее"));
            StringBuilder summary=new StringBuilder(t("Импортировать выбранные Alias: ","Import selected aliases: ")+plan.after.size());
            if(plan.replacements()>0){summary.append(t("\nБудут заменены:\n","\nWill replace:\n"));for(int i=0;i<plan.before.size();i++)if(plan.before.get(i)!=null){WorkspaceStore.Alias a=plan.before.get(i);summary.append(a.name).append(" · ").append(WorkspaceAliases.label(a.environment)).append('\n');}}
            summary.append(t("\nКоманды не будут выполнены. ","\nCommands will not be executed. ")).append(aliasReloadHint(plan));
            backupBuilder(activity).setTitle(t("Подтвердить импорт Alias","Confirm alias import")).setMessage(summary).setPositiveButton(t("Импортировать","Import"),(q,w)->{
                try{JSONObject req=request(source.optBoolean("aliasShell")?"alias-inspect-shell":"inspect").put("path",source.getString("path"));if(source.optBoolean("aliasShell"))req.put("environment",source.getString("environment"));run(req,fresh->{if(!fresh.optJSONObject("fingerprints").toString().equals(source.optJSONObject("fingerprints").toString())){message(errorText("BACKUP_CHANGED_RECHECK"));return;}applyAliases(plan,dialog);});}catch(Exception e){message(errorText("IMPORT_FAILED"));}
            }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
        }catch(Exception e){message(errorText("IMPORT_FAILED"));}}));dialog.show();
    }
    private void applyAliases(WorkspaceAliasImport.Plan plan,AlertDialog picker){
        if(!store.isWritable()||!WorkspaceAliases.BUSY.compareAndSet(false,true)){message(t("Дождитесь окончания изменений Alias и повторите импорт.","Wait for pending alias changes, then retry import."));return;}
        try{WorkspaceAliasImport.verify(plan,store.aliases);}catch(Exception e){WorkspaceAliases.BUSY.set(false);message(errorText("ALIAS_LIBRARY_CHANGED"));return;}
        AlertDialog wait=backupBuilder(activity).setTitle(t("Импорт Alias","Import aliases")).setMessage(t("Сохранение в окружения…","Saving into environments…")).create();wait.setCancelable(false);wait.show();
        new Thread(()->{
            try{
                // Private recovery metadata is written before any managed shell definition changes.
                File recovery=new File(activity.getFilesDir(),"workspace-import-recovery");if(!recovery.isDirectory()&&!recovery.mkdirs())throw new java.io.IOException("PRIVATE_STORAGE");
                JSONObject snapshot=WorkspaceAliasImport.export(plan.before.stream().filter(Objects::nonNull).collect(java.util.stream.Collectors.toList()));
                try(java.io.OutputStream out=new java.io.FileOutputStream(new File(recovery,"aliases-before-"+UUID.randomUUID()+".json"))){out.write(snapshot.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                WorkspaceAliasImport.install(plan,(a,delete)->WorkspaceAliases.apply(activity,a,delete,""));
            }catch(Exception e){activity.runOnUiThread(()->{WorkspaceAliases.BUSY.set(false);wait.dismiss();message(t("Импорт Alias не завершён. Код: ","Alias import did not complete. Code: ")+aliasError(e));});return;}
            activity.runOnUiThread(()->{
                WorkspaceStore target=store.isWritable()?store:new WorkspaceStore(activity,error->{});boolean separate=target!=store;
                try{WorkspaceAliasImport.commit(plan,target);if(!activity.isDestroyed()){picker.dismiss();changed.run();}}
                catch(Exception e){if(separate)target.close();WorkspaceAliases.BUSY.set(false);wait.dismiss();message(t("Определения записаны, но библиотека не обновлена. Сохранён снимок восстановления. Код: ","Definitions were written, but the library was not updated. A recovery snapshot was retained. Code: ")+aliasError(e));return;}
                new Thread(()->{Exception failure=null;try{target.awaitWrites();JSONObject saved=new JSONObject(new String(readFile(new File(activity.getFilesDir(),"workspace-v1.json")),java.nio.charset.StandardCharsets.UTF_8));if(!WorkspaceAliasImport.persisted(plan,saved))throw new java.io.IOException("LIBRARY_WRITE_FAILED");}catch(Exception e){failure=e;}finally{if(separate)target.close();}final Exception result=failure;activity.runOnUiThread(()->{WorkspaceAliases.BUSY.set(false);wait.dismiss();message(result==null?t("Alias импортированы. ","Aliases imported. ")+aliasReloadHint(plan):t("Не удалось подтвердить сохранение библиотеки. Определения могли быть записаны. Снимок восстановления сохранён. Код: ","Library persistence could not be confirmed. Definitions may have been written. Recovery snapshot retained. Code: ")+aliasError(result));});},"jobs-alias-import-save").start();
            });
        },"jobs-alias-import").start();
    }
    private static String aliasError(Exception error){String code=error.getMessage();return code!=null&&code.matches("[A-Z_]{1,80}")?code:"ALIAS_IMPORT_FAILED";}
    private String aliasReloadHint(WorkspaceAliasImport.Plan plan){boolean termux=false,guest=false;for(WorkspaceStore.Alias a:plan.after)if(a.environment.equals("termux"))termux=true;else guest=true;return (termux?t("Для Termux перезапустите вкладку. ","Restart the Termux tab. "):"")+(guest?t("Для остальных выбранных окружений откройте новую терминальную сессию.","Open a new terminal session for the other selected environments."):"");}
    static boolean restoredProfileInUse(WorkspaceStore store,String alias,Set<String> runningHandles){
        for(WorkspaceStore.Session profile:store.sessions)if(runningHandles.contains(profile.handle)&&WorkspaceEnvironments.alias(profile).equals(alias))return true;
        return false;
    }
    static void removeRestoredPresets(WorkspaceStore store,String alias){
        if(!alias.matches("jobs-[a-f0-9]{12}"))throw new IllegalArgumentException("Unconfirmed alias");
        store.sessions.removeIf(s->WorkspaceEnvironments.alias(s).equals(alias));
    }
    private boolean restoredInUse(String alias){
        if(activity.getTermuxService()==null)return true;
        Set<String> handles=new HashSet<>();
        for(com.termux.shared.shell.TermuxSession item:activity.getTermuxService().getTermuxSessions()){
            com.termux.terminal.TerminalSession terminal=item.getTerminalSession();if(terminal.isRunning())handles.add(terminal.mHandle);
        }
        return restoredProfileInUse(store,alias,handles);
    }
    private void restoredEnvironments(){run(request("restored-inventory"),result->{
        LinearLayout form=column();info(form,t("Удаляется восстановленная Linux-система и связанные пресеты. Архивы в Download, папки отчётов и исходные окружения сохраняются. Закройте сессии этой копии перед удалением.","Deletes the restored Linux system and linked presets. Download archives, report folders and original environments are retained. Close this copy's sessions before deletion."));
        JSONArray entries=result.optJSONArray("environments");
        if(entries==null||entries.length()==0)info(form,t("Восстановленных окружений не найдено.","No restored environments found."));
        AlertDialog list=backupBuilder(activity).setTitle(t("Удаление восстановленных окружений","Delete restored environments")).setView(scroll(form)).setNegativeButton(t("Назад","Back"),null).create();
        if(entries!=null)for(int i=0;i<entries.length();i++){
            JSONObject entry=entries.optJSONObject(i);String alias=entry.optString("alias");boolean confirmed=entry.optBoolean("confirmed");
            String title=(entry.optString("source").isEmpty()?alias:WorkspaceEnvironments.label(entry.optString("source"))+" · "+alias)+"\n"+(confirmed?size(entry.optLong("bytes")):t("Происхождение не подтверждено — удаление недоступно","Origin unconfirmed — deletion unavailable"))+(entry.optBoolean("incomplete")?"\n"+t("Незавершённое восстановление","Incomplete restore"):"");
            TextView row=WorkspaceDialogs.action(activity,title,0);row.setEnabled(confirmed);if(!confirmed)row.setAlpha(.55f);form.addView(row);
            row.setOnClickListener(v->{if(restoredInUse(alias)){message(t("Окружение используется. Сначала закройте его сессии. Принудительная остановка не выполняется.","Environment is in use. Close its sessions first. No sessions are forcibly stopped."));return;}
                StorageDeletionConsent.show(activity,t("Удалить окружение?","Delete environment?"),alias+"\n"+t("Все файлы и установленные пакеты этой восстановленной копии будут удалены без возможности отмены, вместе со связанными пресетами. Исходная система и архив бэкапа сохранятся.","All files and installed packages in this restored copy will be permanently deleted, together with linked presets. The original system and backup archive are retained."),()->{
                        if(restoredInUse(alias)){message(t("Окружение используется. Закройте его сессии.","Environment is in use. Close its sessions."));return;}
                        list.dismiss();try{run(request("remove-restored").put("alias",alias).put("identity",entry.optString("identity")),removed->{
                            if(!alias.equals(removed.optString("removed")))return;
                            removeRestoredPresets(store,alias);store.save();changed.run();
                            message(t("Восстановленное окружение удалено. Архив бэкапа сохранён.","Restored environment deleted. Backup archive retained."));
                        });}catch(Exception error){message(errorText("RESTORED_REMOVE_FAILED"));}
                    });
            });
        }
        list.show();
    });}
    private void run(JSONObject request,Consumer<JSONObject> success){
        WorkspaceBackupEngine engine=new WorkspaceBackupEngine();WorkspaceBackupProgress progress=new WorkspaceBackupProgress(activity);
        boolean removal="remove-restored".equals(request.optString("operation"));
        AlertDialog.Builder builder=backupBuilder(activity).setTitle(label(activity)).setView(progress.view);
        if(!removal)builder.setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->engine.cancel());
        AlertDialog dialog=builder.create();dialog.setCancelable(false);
        dialog.setOnDismissListener(d->progress.close());
        boolean started=engine.start(activity,request,new WorkspaceBackupEngine.Callback(){
            public void progress(JSONObject event){if(dialog.isShowing())progress.update(stageName(event.optString("stage")),event);}
            public void done(JSONObject result){progress.close();dialog.dismiss();try{if("restore".equals(request.optString("operation"))||removal)BackupInventoryCache.SHARED.clear();success.accept(result);}catch(Exception e){message(t("Операция не применена полностью. Проверьте результат и сохранённые исходные данные.","Operation was not fully applied. Inspect the result; original files were retained."));}}
            public void error(String code){progress.close();dialog.dismiss();message(errorText(code));}
        });if(started){dialog.show();progress.bindCancellation(dialog,engine::cancel);progress.start();}else{progress.close();message(t("Другая операция резервного копирования ещё выполняется.","Another backup operation is still running."));}
    }

    private String stageName(String stage){switch(stage){case "INSTALL_PIGZ":return t("Установка pigz","Installing pigz");case "DATABASE":return t("Копирование базы данных","Backing up database");case "CHECKSUM":return t("Проверка SHA-256","Checking SHA-256");case "SCAN":return t("Проверка структуры архива","Checking archive structure");case "VERIFY_COMPRESSION":return t("Проверка целостности сжатия","Verifying compressed stream");case "ARCHIVE":return t("Создание архива","Creating archive");case "PROOT_DISTRO":return t("Работа proot-distro","Running proot-distro");case "RESTORE_FILES":return t("Извлечение в новую папку","Extracting to a new folder");case "PREPARE_UBUNTU":return t("Подготовка отдельной Ubuntu","Preparing separate Ubuntu");default:return t("Подготовка и оценка размера","Preparing and estimating size");}}
    private String errorText(String code){switch(code){case "RESTORED_NOT_CONFIRMED":return t("Происхождение копии не подтверждено. Удаление заблокировано.","Copy origin is unconfirmed. Deletion blocked.");case "RESTORED_CHANGED_RECHECK":return t("Окружение изменилось. Откройте список удаления заново.","Environment changed. Reopen the deletion list.");case "RESTORED_LOCKS_REQUIRED":return t("Для безопасного удаления нужен современный proot-distro с блокировкой используемых окружений. Обновите proot-distro.","Safe deletion requires modern proot-distro with active-environment locking. Update proot-distro.");case "RESTORED_REMOVE_FAILED":return t("proot-distro не завершил удаление. Копия может использоваться другим процессом или быть удалена частично. Закройте её сессии и перечитайте список. Исходные окружения не затрагивались.","proot-distro did not complete deletion. The copy may be in use by another process or partially deleted. Close its sessions and reread the list. Original environments were not targeted.");case "PIGZ_INSTALL_FAILED":return t("Не удалось установить pigz. Проверьте интернет, репозитории и завершение других операций APT. gzip остаётся доступен. При необходимости выполните вручную: pkg install pigz.","Could not install pigz. Check internet, repositories and other APT operations. gzip remains available. If needed, run manually: pkg install pigz.");case "PIGZ_REQUIRED":return t("pigz больше недоступен. Выберите gzip или установите pigz заново.","pigz is no longer available. Select gzip or install pigz again.");case "SOURCE_UNREADABLE":case "SOURCE_CHANGED_RETRY":return t("Выбранные исходные файлы недоступны или изменились во время копирования. Копия не завершена; текущие окружения не заменялись. Повторите проверку состава после завершения задач, меняющих файлы.","Selected source files cannot be read or changed while copying. Backup is incomplete; existing environments were not replaced. Recheck the selection after file-writing jobs finish.");case "PROOT_LINK_UNSAFE":return t("Служебная ссылка proot повреждена или ведёт за пределы разрешённого хранилища. Копирование остановлено без изменения источника.","A proot internal link is broken or escapes the permitted backing store. Copying stopped without changing the source.");case "PYTHON_REQUIRED":return t("Нужен Python в Termux. Установите вручную: pkg install python. Данные не изменены.","Python is required in Termux. Install manually: pkg install python. Your data was not changed.");case "INSUFFICIENT_SPACE":return t("Недостаточно свободного места. Для восстановления нужен запас на проверенную копию и извлечение.","Not enough free space. Restore needs room for a verified copy and extraction.");case "CANCELLED":return t("Операция отменена. Незавершённая копия помечена INCOMPLETE, если каталог уже был создан. Не используйте её как готовый бэкап.","Cancelled. A created partial backup/restore folder is marked INCOMPLETE. Do not treat it as complete.");case "ARCH_OR_PROOT_UNCONFIRMED":return t("Архитектура не подтверждена/несовместима или нужен современный proot-distro. Восстановление Linux-окружения заблокировано.","Architecture is unconfirmed/incompatible or modern proot-distro is required. Linux environment restore blocked.");case "UNRECOGNIZED_ARCHIVE":return t("Формат архива не распознан. Поддерживаются manifest.json нашего бэкапа, workspace-v1.json и распознаваемые TAR-архивы HOME/PREFIX/proot-distro.","Archive format not recognized. Supported: our manifest.json, workspace-v1.json and recognized HOME/PREFIX/proot-distro TAR layouts.");case "UNSAFE_ARCHIVE_PATH":case "WRITE_THROUGH_SYMLINK":case "UNSAFE_LINK":case "DUPLICATE_ARCHIVE_PATH":return t("Архив содержит опасные или конфликтующие пути/ссылки. Восстановление заблокировано.","Archive contains unsafe or conflicting paths/links. Restore blocked.");case "CHECKSUM_MISMATCH":case "BACKUP_CHANGED_RECHECK":return t("Контрольная сумма не совпадает или бэкап изменился. Выберите и проверьте его заново.","Checksum mismatch or backup changed. Select and verify it again.");default:return t("Операция остановлена. Текущие окружения не заменялись. Возможна нехватка прав, повреждённый/неподдерживаемый архив или ошибка инструмента. Код: ","Operation stopped. Existing environments were not replaced. Check permissions, archive format/integrity and required tools. Code: ")+code;}}
    private JSONObject request(String operation){try{return WorkspaceBackupEngine.base(activity,operation);}catch(Exception e){throw new IllegalStateException(e);}}
    private String inventoryKey(boolean selective){return (selective?"selection:":"full:")+activity.getFilesDir().getAbsolutePath()+":"+WorkspaceBackupEngine.prefix();}
    private void inventory(){backupBuilder(activity).setTitle(t("Режим резервного копирования","Backup mode")).setItems(new String[]{t("Полная копия окружения","Full environment backup"),t("По выбору","Custom selection")},(d,w)->{
        BackupInventoryCache.Entry entry=BackupInventoryCache.SHARED.get(inventoryKey(w==1));
        if(w==0)createForm(entry==null?null:entry.inventory);
        else try{selectiveForm(entry==null?null:entry.inventory,entry==null?new JSONObject():new JSONObject(entry.folders.toString()),new LinkedHashMap<>(),new JSONObject(),entry==null,null);}catch(Exception e){message(errorText("INVENTORY_FAILED"));}
    }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();}
    private void inlineInventory(AlertDialog dialog,WorkspaceBackupScanHeader header,LinearLayout form,JSONObject req,Consumer<JSONObject> success){
        if(header.running||header.closed||installingPigz)return;
        WorkspaceBackupEngine engine=new WorkspaceBackupEngine();Map<View,Boolean> enabled=new IdentityHashMap<>();rememberEnabled(form,enabled);for(View v:enabled.keySet())v.setEnabled(false);
        Button positive=dialog.getButton(-1);boolean wasEnabled=positive!=null&&positive.isEnabled();if(positive!=null)positive.setEnabled(false);
        header.start(engine,t("Сканирование…","Scanning…"));
        Runnable restore=()->{if(!header.ready)return;for(Map.Entry<View,Boolean> e:enabled.entrySet())e.getKey().setEnabled(e.getValue());if(positive!=null)positive.setEnabled(wasEnabled);};
        boolean started=engine.start(activity,req,new WorkspaceBackupEngine.Callback(){
            public void progress(JSONObject event){if(!header.closed&&dialog.isShowing())header.progress(stageName(event.optString("stage")),event);}
            public void done(JSONObject result){if(header.closed||!dialog.isShowing())return;header.ready=true;header.finish(null);restore.run();try{success.accept(result);}catch(Exception e){header.finish(errorText("INVENTORY_FAILED"));}}
            public void error(String code){if(header.closed||!dialog.isShowing())return;header.finish(errorText(code));restore.run();}
        });
        if(!started){header.finish(t("Другая операция резервного копирования ещё выполняется.","Another backup operation is still running."));restore.run();}
    }
    private void rememberEnabled(View view,Map<View,Boolean> enabled){enabled.put(view,view.isEnabled());if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)rememberEnabled(group.getChildAt(i),enabled);}}
    static void bindConsent(AlertDialog dialog,CheckBox agreement){
        Button create=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        create.setEnabled(agreement.isChecked());create.setAlpha(agreement.isChecked()?1f:0.4f);
        agreement.setOnCheckedChangeListener((button,checked)->{create.setEnabled(checked);create.setAlpha(checked?1f:0.4f);});
    }
    private void createForm(JSONObject inventory){createForm(inventory,new JSONObject());}
    private void createForm(JSONObject supplied,JSONObject state){try{
        boolean initial=supplied==null;
        JSONObject inventory=initial?new JSONObject().put("environments",new JSONArray()).put("ubuntuBytes",new JSONObject()):supplied;
        WorkspaceBackupScanHeader scan=new WorkspaceBackupScanHeader(activity,t("Создать резервную копию","Create backup"));scan.ready=!initial;
        LinearLayout form=column();info(form,t("Архивы могут содержать SSH-ключи, токены и авторизацию. Завершите изменяющие файлы задачи перед копированием. Процессы не останавливаются автоматически; копия работающих баз не гарантированно согласована.","Archives may contain SSH keys, tokens and authorization. Finish file-writing tasks first. Processes are not stopped automatically; live database copies may be inconsistent."));
        CheckBox library=check(form,t("Команды, Alias, заметки, пресеты, настройки и файлы истории сессий","Commands, aliases, notes, presets, settings and shell history files"),state.optBoolean("library",true));
        if(!store.isWritable()){library.setChecked(false);library.setEnabled(false);info(form,t("Библиотека недоступна для чтения/записи. Её исходный файл нужно сохранить отдельно.","Library is unavailable. Preserve its original file separately."));}
        CheckBox home=check(form,"Termux HOME · "+size(inventory.optLong("homeBytes")),state.optBoolean("home")),prefix=check(form,"Termux PREFIX · "+size(inventory.optLong("prefixBytes")),state.optBoolean("prefix"));
        info(form,t("PREFIX сохраняется без rootfs/cache proot-distro и временных файлов. Linux-окружения выбираются отдельно. Внешние файлы по символьным ссылкам и общая память Android не копируются.","PREFIX excludes proot-distro storage/cache and temporary files. Select Linux environments separately. External symlink targets and Android shared storage are not copied."));
        JSONArray environments=inventory.getJSONArray("environments");List<CheckBox> envChecks=new ArrayList<>();for(int i=0;i<environments.length();i++){String name=environments.getJSONObject(i).getString("name");envChecks.add(check(form,"Linux / proot · "+name+" · "+size(inventory.getJSONObject("ubuntuBytes").optLong(name)),state.optJSONObject("envs")!=null&&state.optJSONObject("envs").optBoolean(name)));}
        compressionControl(form);
        info(form,t("Папка создания резервной копии","Backup destination folder"));
        EditText destination=field(form,state.optString("destination",Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),t("Путь к папке","Folder path"));action(form,t("Выбрать каталог","Choose directory"),()->browse(new File(destination.getText().toString()),true,p->destination.setText(p)));
        CheckBox confirmed=check(form,t("Я завершил важные задачи и понимаю, что архив содержит секреты","I finished important jobs and understand the archive contains secrets"),false);
        AlertDialog dialog=backupBuilder(activity).setCustomTitle(scan.view).setView(scroll(form)).setPositiveButton(t("Создать","Create"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{
            if(installingPigz){message(t("Дождитесь окончания установки pigz.","Wait for pigz installation to finish."));return;}if(!confirmed.isChecked()){confirmed.setError(t("Нужно подтверждение","Confirmation required"));return;}JSONArray selected=new JSONArray(),envs=new JSONArray();if(library.isChecked())selected.put("library");if(home.isChecked())selected.put("home");if(prefix.isChecked())selected.put("prefix");for(int i=0;i<envChecks.size();i++)if(envChecks.get(i).isChecked())envs.put(environments.getJSONObject(i).getString("name"));if(selected.length()+envs.length()==0)return;
            JSONObject req=compressedRequest(request("create")).put("destination",destination.getText().toString().trim()).put("components",selected).put("environments",envs).put("workspace",store.encode()).put("settings",WorkspaceBackupImport.settings(activity));dialog.dismiss();run(req,this::backupCreated);
        }catch(Exception e){message(t("Не удалось подготовить бэкап.","Could not prepare backup."));}}));dialog.setOnDismissListener(d->scan.close());dialog.show();bindConsent(dialog,confirmed);
        Runnable refresh=()->{try{
            JSONObject draft=new JSONObject().put("library",library.isChecked()).put("home",home.isChecked()).put("prefix",prefix.isChecked()).put("destination",destination.getText().toString());JSONObject names=new JSONObject();for(int i=0;i<envChecks.size();i++)names.put(environments.getJSONObject(i).getString("name"),envChecks.get(i).isChecked());draft.put("envs",names);
            inlineInventory(dialog,scan,form,request("inventory"),result->{BackupInventoryCache.SHARED.put(inventoryKey(false),result,new JSONObject());dialog.dismiss();createForm(result,draft);});
        }catch(Exception e){scan.finish(errorText("INVENTORY_FAILED"));}};
        scan.refresh.setOnClickListener(v->refresh.run());if(initial)refresh.run();
    }catch(Exception e){message(errorText("INVENTORY_FAILED"));}}
    private String scopeName(String id){return id.equals("termux")?"Termux":id.startsWith("ubuntu:")?"Linux · "+id.substring(7):id;}
    private String itemName(String id){
        if(id.startsWith("project:"))return t("Проект · ","Project · ")+id.substring(8);
        if(id.startsWith("folder:"))return t("Папка · ","Folder · ")+id.substring(7);
        switch(id){case "codex":return t("Codex · программа, авторизация, настройки и история","Codex · CLI, authorization, settings and history");case "agents":return t("Другие AI-агенты · найденные файлы и настройки","Other AI agents · detected files and settings");case "ssh":return t("SSH и Git · ключи и настройки","SSH and Git · keys and settings");case "shell":return t("Оболочка и пользовательские скрипты","Shell and user scripts");case "builds":return t("Инструменты, кеши и результаты сборок","Tools, caches and build outputs");case "other":return t("Остальные пользовательские файлы","Other user files");case "system":return t("Система и установленные зависимости","System and installed dependencies");default:return id;}
    }
    private void selectiveInventory(JSONObject folders,Map<String,Set<String>> retained,JSONObject cached,JSONObject state){selectiveForm(cached,folders,retained,state,true,cached);}
    private void selectiveForm(JSONObject supplied,JSONObject folders,Map<String,Set<String>> retained,JSONObject state,boolean scanOnOpen,JSONObject cached){try{
        boolean initial=supplied==null;
        JSONObject inventory=initial?new JSONObject().put("scopes",new JSONArray()):supplied;
        WorkspaceBackupScanHeader scan=new WorkspaceBackupScanHeader(activity,t("По выбору","Custom selection"));scan.ready=!initial;
        final EditText[] destinationField={null};
        LinearLayout form=column();info(form,t("Выбранные данные. Полная копия остаётся отдельным режимом. Размеры до сжатия; каждая найденная папка учитывается один раз. Внешние цели ссылок не копируются. Архив содержит секреты.","Selected data. Full backup remains a separate mode. Sizes are before compression; detected folders are counted once. External symlink targets are not copied. Archives contain secrets."));
        CheckBox library=check(form,t("Библиотека, настройки и история терминала","Library, settings and terminal history")+" · "+size(inventory.optLong("libraryBytes")),store.isWritable()&&state.optBoolean("library",true));library.setEnabled(store.isWritable());
        JSONArray scopes=inventory.getJSONArray("scopes");Map<String,Map<String,CheckBox>> checks=new LinkedHashMap<>();
        final AlertDialog[] holder={null};
        for(int i=0;i<scopes.length();i++){
            JSONObject scope=scopes.getJSONObject(i);String sid=scope.getString("id");
            LinearLayout section=column();section.setBackground(WorkspaceDialogs.background(activity,false));
            LinearLayout.LayoutParams sectionParams=new LinearLayout.LayoutParams(-1,-2);sectionParams.bottomMargin=WorkspaceDialogs.dp(activity,10);form.addView(section,sectionParams);
            LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);
            String title=sid.equals("termux")?"Termux":"Ubuntu · "+sid.substring(7);
            TextView heading=WorkspaceDialogs.action(activity,title+" ▾",0);section.addView(heading);section.addView(body);
            heading.setOnClickListener(v->{boolean open=body.getVisibility()==View.VISIBLE;body.setVisibility(open?View.GONE:View.VISIBLE);heading.setText(title+(open?" ▸":" ▾"));});
            Map<String,CheckBox> rows=new LinkedHashMap<>();checks.put(sid,rows);JSONArray items=scope.getJSONArray("items");
            for(int j=0;j<items.length();j++){
                JSONObject item=items.getJSONObject(j);String id=item.getString("id");
                if(item.optLong("files")==0&&item.optInt("unreadable")==0)continue;
                boolean readable=item.optInt("unreadable")==0;CheckBox b=check(body,itemName(id)+" · "+(readable?size(item.optLong("bytes")):t("размер неполный · недоступные файлы","incomplete size · unreadable files")),readable&&retained.getOrDefault(sid,Collections.emptySet()).contains(id));b.setEnabled(readable);rows.put(id,b);
            }
            JSONArray issues=scope.optJSONArray("issues");if(issues!=null&&issues.length()>0){info(body,t("Некоторые пути прочитать нельзя. Эти компоненты отключены; остальные можно выбрать. Права исходных файлов не меняются.","Some paths cannot be read. Affected components are disabled; other items remain selectable. Source permissions are not changed."));for(int j=0;j<Math.min(8,issues.length());j++){JSONObject issue=issues.optJSONObject(j);info(body,issue.optString("path")+" · "+t("код ОС ","OS error ")+issue.optInt("errno"));}}
            action(body,t("Добавить папку…","Add folder…"),()->browse(new File(scope.optString("root"),scope.optString("home")),true,p->{try{
                File root=new File(scope.getString("root")).getCanonicalFile(),chosen=new File(p).getCanonicalFile();String base=root.getPath()+"/",user=base+scope.getString("home")+"/";
                if(!chosen.getPath().startsWith(user)){message(t("Выберите подпапку пользовательского HOME этого окружения.","Choose a subdirectory of this environment's user HOME."));return;}
                String relative=chosen.getPath().substring(base.length());JSONArray added=folders.optJSONArray(sid);if(added==null){added=new JSONArray();folders.put(sid,added);}boolean exists=false;for(int k=0;k<added.length();k++)if(relative.equals(added.optString(k)))exists=true;if(!exists)added.put(relative);
                Map<String,Set<String>> keep=selectedItems(checks);keep.computeIfAbsent(sid,key->new HashSet<>()).add("folder:"+relative);
                JSONObject draft=new JSONObject().put("library",library.isChecked()).put("destination",destinationField[0].getText().toString());holder[0].dismiss();selectiveInventory(folders,keep,inventory,draft);
            }catch(Exception e){message(errorText("INVALID_SELECTION_PATH"));}}));
        }
        compressionControl(form);
        info(form,t("Папка создания резервной копии","Backup destination folder"));
        EditText destination=field(form,state.optString("destination",Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),t("Путь к папке","Folder path"));action(form,t("Выбрать каталог","Choose directory"),()->browse(new File(destination.getText().toString()),true,destination::setText));
        destinationField[0]=destination;
        info(form,t("История чатов не заменяет файлы проектов. Работающие процессы и Android Keystore не переносятся. Для других AI-агентов точный состав и зависимости требуют отдельной проверки.","Chat history does not include project files. Live processes and Android Keystore cannot be transferred. Other AI agents need individual file/dependency review."));
        AlertDialog dialog=backupBuilder(activity).setCustomTitle(scan.view).setView(scroll(form)).setPositiveButton(t("Проверить состав","Check selection"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();holder[0]=dialog;
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{
            if(installingPigz){message(t("Дождитесь окончания установки pigz.","Wait for pigz installation to finish."));return;}JSONArray selections=new JSONArray();long total=library.isChecked()?inventory.optLong("libraryBytes"):0;
            for(int i=0;i<scopes.length();i++){JSONObject scope=scopes.getJSONObject(i);String sid=scope.getString("id");JSONArray ids=new JSONArray();Map<String,CheckBox> rows=checks.get(sid);JSONArray items=scope.getJSONArray("items");for(int j=0;j<items.length();j++){JSONObject item=items.getJSONObject(j);CheckBox b=rows.get(item.getString("id"));if(b!=null&&b.isChecked()){ids.put(item.getString("id"));total+=item.optLong("bytes");}}if(ids.length()>0)selections.put(new JSONObject().put("scope",sid).put("items",ids).put("folders",folders.optJSONArray(sid)==null?new JSONArray():folders.getJSONArray(sid)).put("runtime",scope.optJSONObject("runtime")));}
            if(selections.length()==0&&!library.isChecked())return;
            JSONObject req=compressedRequest(request("create")).put("destination",destination.getText().toString().trim()).put("components",library.isChecked()?new JSONArray().put("library"):new JSONArray()).put("environments",new JSONArray()).put("selections",selections).put("workspace",store.encode()).put("settings",WorkspaceBackupImport.settings(activity));
            selectionConfirmation(req,scopes,checks,dialog,total);
        }catch(Exception e){message(errorText("INVALID_SELECTION"));}}));dialog.setOnDismissListener(d->scan.close());dialog.show();
        Runnable refresh=()->{try{
            Map<String,Set<String>> keep=scanOnOpen?retained:selectedItems(checks);JSONObject draft=new JSONObject().put("library",library.isChecked()).put("destination",destination.getText().toString());JSONObject req=request("selection-inventory").put("folders",folders);
            if(scanOnOpen&&cached!=null&&cached.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<4*1024*1024)req.put("inventoryCache",cached);
            inlineInventory(dialog,scan,form,req,result->{BackupInventoryCache.SHARED.put(inventoryKey(true),result,folders);dialog.dismiss();selectiveForm(result,folders,keep,draft,false,null);});
        }catch(Exception e){scan.finish(errorText("INVENTORY_FAILED"));}};
        scan.refresh.setOnClickListener(v->{selectiveRefresh(dialog,scan,form,folders,checks,library,destination);});if(scanOnOpen||initial)refresh.run();
    }catch(Exception e){message(errorText("INVENTORY_FAILED"));}}
    private void selectiveRefresh(AlertDialog dialog,WorkspaceBackupScanHeader scan,LinearLayout form,JSONObject folders,Map<String,Map<String,CheckBox>> checks,CheckBox library,EditText destination){try{
        Map<String,Set<String>> keep=selectedItems(checks);JSONObject draft=new JSONObject().put("library",library.isChecked()).put("destination",destination.getText().toString());
        inlineInventory(dialog,scan,form,request("selection-inventory").put("folders",folders),result->{BackupInventoryCache.SHARED.put(inventoryKey(true),result,folders);dialog.dismiss();selectiveForm(result,folders,keep,draft,false,null);});
    }catch(Exception e){scan.finish(errorText("INVENTORY_FAILED"));}}
    private Map<String,Set<String>> selectedItems(Map<String,Map<String,CheckBox>> checks){Map<String,Set<String>> result=new LinkedHashMap<>();for(Map.Entry<String,Map<String,CheckBox>> scope:checks.entrySet()){Set<String> ids=new HashSet<>();for(Map.Entry<String,CheckBox> row:scope.getValue().entrySet())if(row.getValue().isChecked())ids.add(row.getKey());result.put(scope.getKey(),ids);}return result;}
    private void selectionConfirmation(JSONObject req,JSONArray scopes,Map<String,Map<String,CheckBox>> checks,AlertDialog parent,long total)throws JSONException{
        LinearLayout form=column();info(form,t("Выбрано до сжатия: ","Selected before compression: ")+size(total));JSONArray plans=req.getJSONArray("selections");final AlertDialog[] reviewHolder={null};
        for(int i=0;i<plans.length();i++){
            JSONObject plan=plans.getJSONObject(i);String sid=plan.getString("scope");Set<String> ids=new HashSet<>();JSONArray selected=plan.getJSONArray("items");for(int j=0;j<selected.length();j++)ids.add(selected.getString(j));
            info(form,sid.equals("termux")?"Termux":"Ubuntu · "+sid.substring(7));
            for(String id:ids)info(form,"✓ "+itemName(id));
            if(!ids.contains("system")){
                info(form,t("Для запуска потребуется готовое окружение. Система и её зависимости не включены: добавьте их сейчас или установите в целевом окружении. Данные будут извлечены отдельно, без автоматической замены файлов.","A prepared environment is required. The system/dependencies are not included: add them now or install in the target environment. Data will be staged separately, without automatic file replacement."));
                CheckBox system=checks.get(sid).get("system");if(system!=null&&system.isEnabled())action(form,t("Добавить систему и зависимости","Add system and dependencies"),()->{system.setChecked(true);if(reviewHolder[0]!=null)reviewHolder[0].dismiss();});
            }
            if(ids.contains("codex")){
                boolean node=plan.optJSONObject("runtime")!=null&&plan.optJSONObject("runtime").optBoolean("node");
                info(form,node&&ids.contains("system")?t("Node.js найден в выбранной системе. Переносимость CLI проверяется в целевом окружении.","Node.js found in the selected system. CLI portability is checked in the target environment."):t("Codex требует совместимый Node.js. Если он не включён в систему, установите его в целевом окружении. Авторизация может потребовать повторного входа.","Codex requires compatible Node.js. If not included in the system, install it in the target environment. Authorization may require signing in again."));
                if(ids.stream().noneMatch(id->id.startsWith("project:")||id.startsWith("folder:")))info(form,t("История сохранится; проекты не выбраны. Добавьте нужные папки, чтобы продолжить работу с файлами.","History is included; projects are not selected. Add their folders to continue working with files."));
            }
            if(ids.contains("ssh"))info(form,t("Для использования ключей нужны Git и SSH-клиент. ","Using the keys requires Git and an SSH client. ")+(ids.contains("system")&&plan.optJSONObject("runtime")!=null&&plan.optJSONObject("runtime").optBoolean("git")&&plan.optJSONObject("runtime").optBoolean("ssh")?t("Оба найдены в выбранной системе; доступ к серверу проверяется отдельно.","Both found in the selected system; server access must be checked separately."):t("Если их нет в целевом окружении: Termux — pkg install git openssh; Ubuntu — apt install git openssh-client. Сейчас команды не выполняются.","If absent in the target environment: Termux — pkg install git openssh; Ubuntu — apt install git openssh-client. No commands run now.")));
            if(ids.contains("shell"))info(form,t("Зависимости пользовательских скриптов не определяются автоматически; проверьте интерпретаторы и нужные команды.","User-script dependencies are not detected automatically; review interpreters and required commands."));
            if(ids.contains("agents"))info(form,t("Для других агентов зависимости индивидуальны. Наличие файлов не подтверждает готовность к запуску.","Other agents have individual dependencies. File presence does not establish launch readiness."));
        }
        info(form,t("Завершите задачи, меняющие файлы. Процессы не останавливаются. Базы Codex SQLite копируются согласованно по отдельности; весь бэкап не является атомарным снимком.","Finish file-writing jobs. Processes are not stopped. Codex SQLite databases are backed up consistently individually; the complete backup is not an atomic snapshot."));
        CheckBox agree=check(form,t("Я завершил важные задачи, понимаю, что архив может содержать секреты, и подтверждаю состав","I finished important jobs, understand the archive may contain secrets and confirm the selection"),false);
        AlertDialog review=backupBuilder(activity).setTitle(t("Проверка выбранного","Review selection")).setView(scroll(form)).setPositiveButton(t("Создать копию","Create backup"),null).setNegativeButton(t("Изменить выбор","Change selection"),null).create();
        reviewHolder[0]=review;review.setOnShowListener(d->review.getButton(-1).setOnClickListener(v->{if(!agree.isChecked()){agree.setError(t("Нужно подтверждение","Confirmation required"));return;}try{
            // Rebuild from live checkboxes, including system selections added in this review.
            JSONArray updated=new JSONArray();Map<String,Set<String>> now=selectedItems(checks);for(int i=0;i<plans.length();i++){JSONObject plan=plans.getJSONObject(i);JSONArray ids=new JSONArray();for(String id:now.get(plan.getString("scope")))ids.put(id);plan.put("items",ids);updated.put(plan);}req.put("selections",updated);
            review.dismiss();parent.dismiss();run(req,this::backupCreated);
        }catch(Exception e){message(errorText("INVALID_SELECTION"));}}));review.show();bindConsent(review,agree);
    }
    private void requirements(LinearLayout form,JSONObject component){
        JSONObject selection=component.optJSONObject("selection"),req=component.optJSONObject("requirements");if(selection==null||req==null)return;
        JSONArray savedItems=selection.optJSONArray("items");if(savedItems!=null)for(int i=0;i<savedItems.length();i++)info(form,"✓ "+itemName(savedItems.optString(i)));
        info(form,req.optBoolean("architectureMatches")?t("Архитектура источника совпадает. Это не проверка запуска CLI.","Source architecture matches. This does not verify CLI launch."):t("Архитектура отличается или неизвестна: файлы можно извлечь отдельно, но бинарники могут потребовать переустановки.","Source architecture differs or is unknown: data can be staged, but binaries may need reinstalling."));
        if(req.optBoolean("stagingOnly"))info(form,t("Только отдельное извлечение. Текущие файлы не заменятся; окружение и зависимости нужно подготовить перед ручным переносом.","Staging only. Existing files are not replaced; prepare the environment and dependencies before manually activating files."));
        JSONArray candidates=req.optJSONArray("candidates");if(selection.optBoolean("system")&&selection.optString("environment").equals("ubuntu"))info(form,t("Система включена и будет восстановлена под новым именем. Наличие текущей системы не требуется. Версии runtime и работоспособность CLI проверьте после восстановления.","The system is included and will be restored under a new name. An existing system is not required. Verify runtime versions and CLI operation after restore."));
        else if(candidates==null||candidates.length()==0)info(form,t("Подходящее окружение в целевом окружении не найдено. Установите его либо восстановите систему из полного бэкапа.","No matching environment found in the target environment. Install it or restore the system from a full backup."));
        else for(int i=0;i<candidates.length();i++){JSONObject c=candidates.optJSONObject(i);info(form,t("Обнаружено: ","Detected: ")+scopeName(c.optString("scope"))+(req.optBoolean("needsNode")?c.optBoolean("node")?t(" · Node.js найден"," · Node.js found"):t(" · Node.js не найден; установите перед запуском"," · Node.js not found; install before launching"):""));}
        if(selection.optBoolean("historyOnlyWithoutProjects"))info(form,t("Проекты не выбраны: история чатов не содержит их файлы.","Projects were not selected: chat history does not contain their files."));
        if(req.optBoolean("needsSshGit")){
            info(form,t("Ключам нужны Git и SSH-клиент. Если отсутствуют: pkg install git openssh в Termux; apt install git openssh-client в Ubuntu. Сейчас установка не выполняется.","Keys require Git and an SSH client. If absent: pkg install git openssh in Termux; apt install git openssh-client in Ubuntu. Nothing is installed now."));
            if(candidates!=null)for(int i=0;i<candidates.length();i++){JSONObject c=candidates.optJSONObject(i);info(form,scopeName(c.optString("scope"))+" · Git: "+(c.optBoolean("git")?t("найден","found"):t("не найден","not found"))+" · SSH: "+(c.optBoolean("ssh")?t("найден","found"):t("не найден","not found")));}
        }
        if(req.optBoolean("scriptsRequireReview"))info(form,t("Зависимости пользовательских скриптов требуют ручной проверки.","User-script dependencies require manual review."));
        if(req.optBoolean("agentsRequireReview"))info(form,t("Проверьте зависимости и вход каждого AI-агента отдельно.","Check each AI agent's dependencies and sign-in separately."));
        if(req.optBoolean("needsNode"))info(form,t("Подготовка вручную: Termux — pkg install nodejs; Ubuntu — proot-distro install ubuntu, затем apt update && apt install nodejs npm внутри Ubuntu. Доступная версия Node.js должна соответствовать требованиям сохранённого CLI. Это подсказки; команды сейчас не выполняются.","Manual preparation: Termux — pkg install nodejs; Ubuntu — proot-distro install ubuntu, then apt update && apt install nodejs npm inside Ubuntu. The Node.js version must meet the saved CLI requirements. These are hints; no commands are run now."));
    }
    private String size(long value){return android.text.format.Formatter.formatFileSize(activity,value);}
    private void chooseFile(boolean restore){LinearLayout form=column();info(form,t("Выберите manifest.json из каталога бэкапа, workspace-v1.json или TAR-архив. Проверка ничего не распаковывает и не запускает.","Choose manifest.json from a backup directory, workspace-v1.json or a TAR archive. Inspection never extracts or executes archive contents."));EditText file=field(form,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),t("Путь к файлу","File path"));action(form,t("Выбрать файл","Choose file"),()->browse(new File(file.getText().toString()),false,file::setText));backupBuilder(activity).setTitle(restore?t("Восстановить из файла","Restore from file"):t("Проверить резервную копию","Verify backup")).setView(scroll(form)).setPositiveButton(t("Проверить","Verify"),(d,w)->{try{run(request("inspect").put("path",file.getText().toString().trim()),r->report(r,restore));}catch(Exception e){message(errorText("INVALID_PATH"));}}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();}
    private void browse(File start,boolean folders,Consumer<String> selected){
        File folder=start.isDirectory()?start:start.getParentFile();if(folder==null||!folder.isDirectory())folder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] list=folder.listFiles(f->!WorkspaceBackupEngine.symlink(f)&&(f.isDirectory()||!folders&&f.isFile()));if(list==null){message(t("Нет доступа к папке. Проверьте разрешение доступа к файлам Android или введите доступный путь вручную.","Cannot access this directory. Check Android storage permission or enter an accessible path."));return;}Arrays.sort(list,Comparator.comparing((File f)->!f.isDirectory()).thenComparing(File::getName));if(list.length>3000){message(t("Слишком много файлов; введите вложенный путь.","Too many files; enter a nested path."));return;}List<String> rows=new ArrayList<>();rows.add("../");for(File f:list)rows.add((f.isDirectory()?"▸ ":"")+f.getName());File current=folder;
        AlertDialog.Builder b=backupBuilder(activity).setTitle(current.getAbsolutePath()).setItems(rows.toArray(new String[0]),(d,w)->{if(w==0){File parent=current.getParentFile();if(parent!=null)browse(parent,folders,selected);}else if(list[w-1].isDirectory())browse(list[w-1],folders,selected);else selected.accept(list[w-1].getAbsolutePath());}).setNegativeButton(WorkspaceText.tr("Отмена"),null);if(folders)b.setPositiveButton(t("Выбрать эту папку","Select this directory"),(d,w)->selected.accept(current.getAbsolutePath()));b.show();
    }
    private void report(JSONObject report,boolean restore){try{
        LinearLayout form=column();info(form,t("Проверка завершена. SHA-256 не подтверждает подлинность чужого бэкапа. Не восстанавливайте недоверенные данные. Совместимость работающих баз и внешних путей автоматически не гарантируется.","Inspection complete. SHA-256 does not authenticate a foreign backup. Do not restore untrusted data. Live database consistency and external paths are not guaranteed."));
        JSONObject source=report.optJSONObject("source");if(source!=null&&source.length()>0)info(form,t("Источник: ","Source: ")+source.optString("packageId","?")+" · "+source.optString("arch","?")+" · "+source.optString("appVersion","?")+"\n"+t("Пути из бэкапа: ","Backup paths: ")+source.optString("home","?")+" / "+source.optString("prefix","?"));else info(form,t("Исходные версии приложения и package ID неизвестны. Совместимость путей не подтверждена.","Source app version and package ID are unknown. Path compatibility is unconfirmed."));
        JSONArray components=report.getJSONArray("components");for(int i=0;i<components.length();i++){JSONObject c=components.getJSONObject(i);info(form,(i+1)+". "+kindName(c.optString("kind"))+" · "+c.optString("format")+" · "+c.optString("arch")+"\n"+status(c)+" · "+size(c.optLong("unpacked")));requirements(form,c);}
        JSONObject library=report.optJSONObject("workspace");if(library!=null)info(form,t("Библиотека: команды / Alias / заметки / пресеты: ","Library: commands / aliases / notes / presets: ")+count(library,"commands")+" / "+count(library,"aliases")+" / "+count(library,"notes")+" / "+count(library,"sessions"));info(form,t("Свободно: ","Free: ")+size(report.optLong("free")));
        action(form,t("Пакеты · сравнить версии","Packages · compare versions"),()->packages(report));
        action(form,t("Выбрать компоненты восстановления","Choose restore components"),()->restoreForm(report));
        backupBuilder(activity).setTitle(t("Проверка бэкапа","Backup inspection")).setView(scroll(form)).setPositiveButton(WorkspaceText.tr("Закрыть"),null).show();
    }catch(Exception e){message(errorText("REPORT_FAILED"));}}
    private static int count(JSONObject v,String key){JSONArray a=v.optJSONArray(key);return a==null?0:a.length();}
    private String kindName(String kind){switch(kind){case "home":return "Termux HOME";case "prefix":return "Termux PREFIX";case "ubuntu":return "Linux / proot";case "app-data":return t("Файлы истории терминала","Terminal history files");case "packages":return t("Список пакетов Termux","Termux package list");case "data":return t("Выбранные данные окружения","Selected environment data");case "library":return t("Библиотека приложения","App library");default:return kind;}}
    private String status(JSONObject c){if(c.optString("status").equals("blocked"))return t("ЗАБЛОКИРОВАНО: архитектура или proot-distro не подтверждены","BLOCKED: architecture or proot-distro unconfirmed");if(c.optString("kind").equals("data"))return t("Отдельное извлечение; зависимости и ручная активация","Staging only; dependencies and manual activation required");if(c.optString("kind").equals("prefix"))return t("Только извлечение отдельно; текущий PREFIX не заменяется","Staging only; current PREFIX is not replaced");if(c.optString("kind").equals("ubuntu"))return t("Отдельная среда с новым именем","Separate environment with a fresh name");return t("Перенос в новую папку; пути требуют проверки","Stage in a new directory; review paths");}
    private void restoreForm(JSONObject report){try{
        LinearLayout form=column();info(form,t("Файлы извлекаются только в новую подпапку выбранного каталога HOME. Текущие HOME/PREFIX/Linux-окружения не заменяются. Для восстановленной Linux-системы создаются минимальные настройки запуска: переменные и команды из OCI-манифеста не переносятся. Файлы системы и настройки пользователя сохраняются. Пресеты после импорта требуют проверки путей и среды перед запуском. Сохранённые пароли SSH Android Keystore перенести нельзя.","Files are staged only in a new subdirectory under HOME. Current HOME/PREFIX/Linux environments are not replaced. Restored Linux systems get minimal launch metadata; OCI manifest environment variables and commands are not imported. Rootfs files and user settings are preserved. Imported presets require path/environment review before launch. Android Keystore remembered SSH passwords cannot be transferred."));
        JSONArray components=report.getJSONArray("components");List<CheckBox> selected=new ArrayList<>();for(int i=0;i<components.length();i++){JSONObject c=components.getJSONObject(i);CheckBox b=check(form,kindName(c.optString("kind"))+" · "+status(c),false);b.setEnabled(!c.optString("status").equals("blocked"));selected.add(b);requirements(form,c);}
        WorkspaceBackupImport.Choice choice=new WorkspaceBackupImport.Choice();JSONObject library=report.optJSONObject("workspace");CheckBox commands=check(form,t("Импортировать команды","Import commands"),false),notes=check(form,t("Импортировать заметки","Import notes"),false),presets=check(form,t("Импортировать пресеты запуска","Import launch presets"),false),history=check(form,t("Перенести файлы истории импортируемых пресетов (нужен app-data)","Copy history files for imported presets (requires app-data)"),false),settings=check(form,t("Импортировать настройки приложения","Import app settings"),false),replace=check(form,t("Заменить выбранные библиотеки вместо объединения","Replace selected libraries instead of merging"),false);
        commands.setEnabled(library!=null);notes.setEnabled(library!=null);presets.setEnabled(library!=null);history.setEnabled(library!=null);settings.setEnabled(report.optJSONObject("settings")!=null);
        action(form,t("Импорт Alias","Import aliases"),()->aliasImportReport(report));
        JSONArray candidates=report.optJSONArray("candidates");Set<Integer> chosen=new HashSet<>();action(form,t("Aliases и скрипты → добавить в команды","Aliases and scripts → add to commands"),()->candidatePicker(candidates,chosen));
        EditText dest=field(form,WorkspaceBackupEngine.home(),t("Родительский каталог внутри HOME","Parent directory inside HOME"));action(form,t("Выбрать каталог","Choose directory"),()->browse(new File(dest.getText().toString()),true,dest::setText));
        CheckBox agree=check(form,t("Я доверяю бэкапу и подтверждаю выбранный план","I trust this backup and confirm the selected plan"),false);
        AlertDialog dialog=backupBuilder(activity).setTitle(t("План восстановления","Restore plan")).setView(scroll(form)).setPositiveButton(t("Подтвердить восстановление","Confirm restore"),null).setNegativeButton(WorkspaceText.tr("Отмена"),null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{
            if(!agree.isChecked()){agree.setError(t("Нужно подтверждение","Confirmation required"));return;}JSONArray indices=new JSONArray();for(int i=0;i<selected.size();i++)if(selected.get(i).isChecked())indices.put(i);choice.commands=commands.isChecked();choice.notes=notes.isChecked();choice.presets=presets.isChecked();choice.history=history.isChecked();choice.settings=settings.isChecked();choice.replace=replace.isChecked();choice.candidates=chosen;
            for(Integer index:chosen){JSONObject c=candidates.getJSONObject(index);if(c.getString("kind").equals("script")&&!selected.get(c.getInt("component")).isChecked()){message(t("Для выбранного скрипта сначала выберите восстановление его HOME/Ubuntu.","Select the HOME/Ubuntu component containing each chosen script."));return;}}
            if(indices.length()==0&&!choice.commands&&!choice.notes&&!choice.presets&&!choice.settings&&chosen.isEmpty())return;
            JSONObject req=request("inspect").put("path",report.getString("path"));dialog.dismiss();run(req,fresh->{try{if(!fresh.getJSONObject("fingerprints").toString().equals(report.getJSONObject("fingerprints").toString())){message(errorText("BACKUP_CHANGED_RECHECK"));return;}if(indices.length()==0)applyImport(fresh,new JSONObject(),choice);else run(request("restore").put("path",report.getString("path")).put("fingerprints",report.getJSONObject("fingerprints")).put("selected",indices).put("destination",dest.getText().toString().trim()),r->applyImport(fresh,r,choice));}catch(Exception e){message(errorText("IMPORT_FAILED"));}});
        }catch(Exception e){message(errorText("IMPORT_FAILED"));}}));dialog.show();
    }catch(Exception e){message(errorText("IMPORT_FAILED"));}}
    private void candidatePicker(JSONArray candidates,Set<Integer> selected){if(candidates==null||candidates.length()==0){message(t("Простые aliases и shell-скрипты не найдены.","No simple aliases or shell scripts found."));return;}String[] labels=new String[candidates.length()];boolean[] values=new boolean[labels.length];for(int i=0;i<labels.length;i++){JSONObject c=candidates.optJSONObject(i);labels[i]=c.optString("kind")+" · "+c.optString("name")+"\n"+c.optString("source");values[i]=selected.contains(i);}backupBuilder(activity).setTitle(t("Добавить в команды","Add to commands")).setMultiChoiceItems(labels,values,(d,w,checked)->{if(checked)selected.add(w);else selected.remove(w);}).setPositiveButton("OK",null).show();}
    private void applyImport(JSONObject report,JSONObject restored,WorkspaceBackupImport.Choice choice){
        AlertDialog wait=backupBuilder(activity).setTitle(label(activity)).setMessage(t("Проверка и импорт библиотеки…","Validating and importing library…")).create();wait.setCancelable(false);wait.show();
        new Thread(()->{try{WorkspaceBackupImport.Prepared prepared=WorkspaceBackupImport.prepare(activity,report,restored,choice);activity.runOnUiThread(()->{try{
            if(activity.isDestroyed())return;WorkspaceBackupImport.apply(activity,store,prepared,choice);if(choice.settings){WorkspaceBackupImport.settings(activity,report.optJSONObject("settings"));WorkspaceLanguage.apply(activity);}BackupInventoryCache.SHARED.clear();changed.run();
            new Thread(()->{try{store.awaitWrites();String saved=new String(readFile(new File(activity.getFilesDir(),"workspace-v1.json")),java.nio.charset.StandardCharsets.UTF_8);if(!WorkspaceBackupImport.persisted(new JSONObject(saved),prepared))throw new java.io.IOException();activity.runOnUiThread(()->{wait.dismiss();message(t("Выбранные компоненты восстановлены. Ничего из архива не запускалось. Проверьте пути, настройки и авторизацию. Для части настроек нужно заново открыть приложение.\n","Selected components restored. No archive content was executed. Review paths, settings and authorization. Some settings require reopening the app.\n")+restored.optString("path",t("Импортирована библиотека","Library imported"))+"\n"+t("Отдельные Linux-окружения добавлены как пресеты запуска. Исходные библиотеки перед импортом сохранены в приватном каталоге приложения.","Separate Linux environments were added as launch presets. Original libraries were saved in the app's private directory before import."));});}catch(Exception e){activity.runOnUiThread(()->{wait.dismiss();message(errorText("LIBRARY_WRITE_FAILED"));});}},"jobs-backup-save").start();
        }catch(Exception e){wait.dismiss();message(errorText("IMPORT_FAILED"));}});}catch(Exception e){activity.runOnUiThread(()->{wait.dismiss();message(t("Файлы могли быть извлечены, но импорт библиотеки не завершён. Проверьте каталог результата.","Files may have been staged, but library import did not complete. Inspect the result directory."));});}},"jobs-backup-import").start();
    }
    private byte[] readFile(File file)throws java.io.IOException{try(java.io.InputStream in=new java.io.FileInputStream(file);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1){if(out.size()+n>16*1024*1024)throw new java.io.IOException();out.write(data,0,n);}return out.toByteArray();}}
    private void packages(JSONObject report){JSONObject source=report.optJSONObject("packages");if(source==null||source.length()==0){message(t("Список пакетов Termux в бэкапе не найден. Пакеты отдельной Ubuntu восстанавливаются с её системой и с текущим Termux не смешиваются.","No Termux package list found. Separate Ubuntu packages are restored with its rootfs, not mixed into current Termux."));return;}try{run(request("packages").put("packages",source),r->{JSONArray rows=r.optJSONArray("packages");if(rows==null||rows.length()==0){message(t("Различий в известных версиях не найдено.","No differences in known package versions."));return;}packageSelection(rows);});}catch(Exception e){message(errorText("PACKAGE_LIST_FAILED"));}}
    private void packageSelection(JSONArray rows){Set<Integer> selected=new HashSet<>();LinearLayout form=column();info(form,t("Сравнение относится к пакетам Termux. Выбирайте только доверенные пакеты. APT может изменить зависимости; план показывается до установки. Текущие репозитории должны содержать выбранные версии.","Comparison covers Termux packages. Select trusted packages only. APT may change dependencies; review its plan before installing. Selected versions must exist in current repositories."));List<CheckBox> checks=new ArrayList<>();for(int i=0;i<rows.length();i++){JSONObject p=rows.optJSONObject(i);CheckBox b=check(form,p.optString("name")+"\n"+t("Сейчас: ","Current: ")+(p.optString("current").isEmpty()?t("не установлен","not installed"):p.optString("current"))+" → "+t("Бэкап: ","Backup: ")+p.optString("backup"),false);b.setEnabled(p.optBoolean("selectable"));checks.add(b);}action(form,t("Выбрать все доступные различия","Select all available differences"),()->{if(checks.stream().filter(View::isEnabled).count()>200){message(t("За один план можно выбрать до 200 пакетов. Выберите их вручную несколькими группами.","Select up to 200 packages per plan. Process larger lists in batches."));return;}for(CheckBox b:checks)if(b.isEnabled())b.setChecked(true);});action(form,t("Не заменять ничего","Do not replace anything"),()->{for(CheckBox b:checks)b.setChecked(false);});backupBuilder(activity).setTitle(t("Пакеты · различия","Packages · differences")).setView(scroll(form)).setPositiveButton(t("Получить план APT","Get APT plan"),(d,w)->{try{JSONObject packages=new JSONObject();for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked()){JSONObject p=rows.getJSONObject(i);packages.put(p.getString("name"),p.getString("backup"));}if(packages.length()==0)return;run(request("apt-plan").put("packages",packages),plan->{LinearLayout body=column();info(body,plan.optString("plan"));info(body,t("Установка затронет общую среду Termux. Завершите важные задачи. Откроется отдельный терминал; APT дополнительно запросит подтверждение. Старый план может измениться — проверьте его ещё раз в терминале.","Installation changes the shared Termux environment. Finish important jobs. A separate terminal opens; APT asks for confirmation again. Its plan may have changed; review it again in the terminal."));AlertDialog.Builder prompt=backupBuilder(activity).setTitle(t("План APT","APT plan")).setView(scroll(body)).setNegativeButton(WorkspaceText.tr("Отмена"),null);if(plan.optBoolean("ok"))prompt.setPositiveButton(t("Подтвердить установку","Confirm installation"),(x,y)->packageInstall.accept(plan.optString("command")));prompt.show();});}catch(Exception e){message(errorText("APT_PLAN_FAILED"));}}).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();}
}
