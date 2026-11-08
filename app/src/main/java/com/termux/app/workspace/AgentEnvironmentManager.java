package com.termux.app.workspace;

import android.app.*;import android.widget.*;
import com.termux.app.TermuxActivity;import com.termux.shared.termux.TermuxConstants;import com.termux.shared.shell.TermuxShellUtils;
import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;import java.util.function.*;

/** User-triggered rootfs operations. No live-session switching or shell injection. */
final class AgentEnvironmentManager implements AutoCloseable {
    static final Set<String> operating=ConcurrentHashMap.newKeySet();
    private final TermuxActivity activity;private final WorkspaceStore store;private final Predicate<String> live;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private boolean closed;
    private AlertDialog progress;private volatile File cancelFile;
    AgentEnvironmentManager(TermuxActivity a,WorkspaceStore s,Predicate<String> running){activity=a;store=s;live=running;}
    String t(String r,String e){return WorkspaceLanguage.code(activity).equals("ru")?r:e;}
    static boolean busy(WorkspaceStore.Session p){return operating.contains(AgentEnvironments.scope(p));}
    List<String> references(String alias){List<String> names=new ArrayList<>();for(WorkspaceStore.Session p:store.sessions)if((p.saved||!p.handle.isEmpty())&&alias.equals(AgentEnvironments.scope(p)))names.add(p.name);return names;}
    private boolean running(String alias){return live.test(alias);}
    private void message(String value){if(!closed)WorkspaceDialogs.builder(activity).setMessage(value).setPositiveButton(t("Назад","Back"),null).show();}
    Consumer<Consumer<Boolean>> prepareTools;
    void choose(String current,Consumer<String> selected){
        WorkspaceDialogs.builder(activity).setTitle(t("Окружение агента","Agent environment")).setSingleChoiceItems(new String[]{"Ubuntu",t("Ubuntu AI · для агентов","Ubuntu AI · for agents")},current.equals("ubuntu-ai")?1:0,(d,w)->{
            String alias=w==0?"ubuntu":"ubuntu-ai";d.dismiss();
            selected.accept(alias);
        }).setNegativeButton(t("Назад","Back"),null).show();
    }
    void remove(WorkspaceEnvironments.Choice choice,Runnable refresh){
        String alias=choice.alias;if(alias.isEmpty()||!WorkspaceLocalFilePicker.validAlias(alias)||!choice.installed)return;
        if(running(alias)){message(environmentError(alias,"ENVIRONMENT_BUSY"));return;}
        List<String> refs=references(alias);if(!refs.isEmpty()){message(t("Окружение используется сессиями или пресетами. Сначала переключите их на другую среду либо удалите пресеты:\n• ","The environment is referenced by sessions or presets. Switch them to another environment or remove those presets first:\n• ")+String.join("\n• ",refs));return;}
        run("plan",alias,"",true,plan->{
            if(!alias.equals(plan.optString("alias"))||!("containers/"+alias).equals(plan.optString("container"))){message(error("SOURCE_CHANGED"));return;}
            AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(t("Удалить ","Delete ")+choice.label+"?")
                .setMessage(t("Навсегда удалятся все файлы, проекты, пакеты, ключи и настройки этого окружения.\n","Permanently delete all files, projects, packages, keys and settings in this environment.\n")+choice.alias+" · "+size(plan.optLong("bytes"))+scanWarning(plan)+t("\nАрхивы резервных копий и общая память телефона сохраняются. Произвольные команды со вписанным вручную именем системы автоматически не распознаются. Отменить начавшееся удаление нельзя.","\nBackup archives and phone shared storage are retained. Arbitrary commands containing a manually typed system name are not detected automatically. Deletion cannot be cancelled once started."))
                .setNegativeButton(t("Назад","Back"),null).setPositiveButton(t("Удалить навсегда","Delete permanently"),(d,w)->{if(running(alias)||!references(alias).isEmpty()){message(environmentError(alias,"ENVIRONMENT_BUSY"));return;}run("remove",alias,plan.optString("token"),false,result->refresh.run());}).create();confirm.setCanceledOnTouchOutside(false);confirm.show();
        });
    }
    String environmentError(String alias,String code){return error(code)+"\n"+t("Окружение: ","Environment: ")+WorkspaceEnvironments.label(alias)+" ("+alias+")\ncontainers/"+alias;}
    String scanWarning(JSONObject plan){
        JSONArray paths=plan.optJSONArray("unscanned");if(paths==null||paths.length()==0)return "";
        return t("\nСодержимое служебного каталога run/systemd/dissect-root с правами 000 не просмотрено и не учтено в размере. Он будет удалён только если пуст. Если он не пуст, удаление окружения остановится без изменения прав.","\nContents of the service directory run/systemd/dissect-root (mode 000) were not scanned or included in the size. It will only be removed if empty. If not empty, environment deletion will stop without changing permissions.");
    }
    private String size(long b){return android.text.format.Formatter.formatShortFileSize(activity,b);}
    private void problem(String code,Runnable retry){
        if((code.equals("ENVIRONMENT_TOOLS_REQUIRED")||code.equals("NATIVE_LOCKS_REQUIRED"))&&prepareTools!=null){WorkspaceDialogs.builder(activity).setMessage(error(code)).setNegativeButton(t("Назад","Back"),null).setPositiveButton(t("Подготовить автоматически","Prepare automatically"),(d,w)->prepareTools.accept(ok->{if(ok&&!closed)retry.run();})).show();}else message(error(code));
    }
    private void run(String op,String alias,String token,boolean cancellable,Consumer<JSONObject> done){
        if(closed)return;
        if(op.equals("remove")&&(running(alias)||!references(alias).isEmpty())){message(environmentError(alias,"ENVIRONMENT_BUSY"));return;}
        if(!new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"python3").canExecute()||!new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH,"proot-distro").canExecute()){problem("ENVIRONMENT_TOOLS_REQUIRED",()->run(op,alias,token,cancellable,done));return;}
        Set<String> scopes=new HashSet<>();scopes.add(alias);
        synchronized(operating){for(String s:scopes)if(operating.contains(s)){message(environmentError(alias,"ENVIRONMENT_BUSY"));return;}operating.addAll(scopes);}
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(24,16,24,16);ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setIndeterminate(true);body.addView(bar);TextView status=new TextView(activity);status.setTextColor(WorkspaceDialogs.TEXT);status.setText(t("Проверка…","Checking…"));body.addView(status);
        AlertDialog wait=WorkspaceDialogs.builder(activity).setTitle(op.equals("remove")?t("Удаление окружения","Deleting environment"):op.equals("plan")?t("Сканирование окружения","Scanning environment"):t("Операция с окружением","Environment operation")).setView(body).setNegativeButton(cancellable?t("Отмена","Cancel"):t("Назад","Back"),null).create();progress=wait;wait.setCancelable(false);wait.setCanceledOnTouchOutside(false);wait.show();wait.getButton(-2).setEnabled(cancellable);
        final java.util.concurrent.atomic.AtomicBoolean stop=new java.util.concurrent.atomic.AtomicBoolean();wait.getButton(-2).setOnClickListener(v->{stop.set(true);File f=cancelFile;if(f!=null)try{f.createNewFile();}catch(IOException ignored){}wait.getButton(-2).setEnabled(false);status.setText(t("Отмена проверки…","Cancelling the check…"));});
        worker.execute(()->{JSONObject result=null;String failure="OPERATION_FAILED",location="";File marker=null;Process child=null;
            try{
                File dir=new File(activity.getFilesDir(),".agent-environment-operations");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException();
                if(!dir.getCanonicalFile().equals(new File(activity.getFilesDir().getCanonicalFile(),".agent-environment-operations")))throw new IOException();
                protect(dir);
                File script=new File(dir,"runtime.py"),temp=File.createTempFile("runtime-",".tmp",dir);
                try{try(InputStream in=activity.getAssets().open("agent-environment.py");OutputStream out=new FileOutputStream(temp)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}protect(temp);if(!temp.renameTo(script))throw new IOException();}finally{temp.delete();}
                marker=new File(dir,"cancel-"+UUID.randomUUID());cancelFile=marker;if(stop.get()||closed)marker.createNewFile();
                ProcessBuilder pb=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/python3",script.getAbsolutePath(),op,alias,token,marker.getAbsolutePath());pb.environment().clear();for(String v:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=v.indexOf('=');if(eq>0)pb.environment().put(v.substring(0,eq),v.substring(eq+1));}pb.environment().put("PREFIX",TermuxConstants.TERMUX_PREFIX_DIR_PATH);pb.environment().put("PYTHONDONTWRITEBYTECODE","1");pb.redirectError(new File("/dev/null"));Process p=pb.start();child=p;p.getOutputStream().close();
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){if(line.length()>8192)throw new IOException();JSONObject event=new JSONObject(line);String kind=event.optString("event");if(kind.equals("progress")){activity.runOnUiThread(()->{if(closed||progress!=wait||!wait.isShowing())return;long total=event.optLong("total"),value=event.optLong("done");bar.setIndeterminate(total<=0);if(total>0){bar.setMax(1000);bar.setProgress((int)Math.min(1000,value*1000/total));}String phase=event.optString("phase");status.setText((phase.equals("copy")?t("Копирование","Copying"):phase.equals("verify")?t("Проверка файлов","Verifying files"):phase.equals("publish")?t("Завершение","Finishing"):t("Сканирование","Scanning"))+"\n"+size(value)+(total>0?" / "+size(total):""));});}else if(kind.equals("error")){failure=event.optString("code","OPERATION_FAILED")+(event.has("errno")?"_ERRNO_"+event.optInt("errno"):"");location=scanDetails(event);}else if(kind.equals("plan")||kind.equals("done"))result=event;}}
                if(p.waitFor()!=0)result=null;
            }catch(Exception ignored){}finally{if(child!=null&&child.isAlive()){if(marker!=null)try{marker.createNewFile();}catch(IOException ignored){}try{child.waitFor();}catch(InterruptedException e){Thread.currentThread().interrupt();}}if(marker!=null)marker.delete();cancelFile=null;synchronized(operating){operating.removeAll(scopes);}}
            JSONObject answer=result;String reason=failure,detail=location;activity.runOnUiThread(()->{if(progress==wait)progress=null;wait.dismiss();if(closed)return;if(stop.get()&&op.equals("plan")){message(error("CANCELLED"));return;}if(answer==null){if(reason.equals("ENVIRONMENT_TOOLS_REQUIRED")||reason.equals("NATIVE_LOCKS_REQUIRED"))problem(reason,()->run(op,alias,token,cancellable,done));else message(environmentError(alias,reason)+detail);}else done.accept(answer);});
        });
    }
    private static void protect(File f)throws IOException{if(!f.setReadable(false,false)||!f.setWritable(false,false)||!f.setExecutable(false,false)||!f.setReadable(true,true)||!f.setWritable(true,true)||f.isDirectory()&&!f.setExecutable(true,true))throw new IOException("Private permissions unavailable");}
    String scanDetails(JSONObject event){
        if(!event.optString("code").equals("SCAN_ACCESS_DENIED")||!event.has("scanPath"))return "";
        String path=event.optString("scanPath"),op=event.optString("scanOperation");
        if(path.startsWith("/")||Arrays.asList(path.split("/",-1)).contains("..")||!Arrays.asList("list","stat","open-directory").contains(op))return "";
        StringBuilder safe=new StringBuilder();for(int i=0;i<Math.min(path.length(),2048);i++){char c=path.charAt(i);if(Character.isISOControl(c)||(c>=0x202a&&c<=0x202e)||(c>=0x2066&&c<=0x2069))safe.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else safe.append(c);}if(path.length()>2048)safe.append("…");
        String action=op.equals("list")?t("чтение списка каталога","listing directory"):op.equals("stat")?t("чтение атрибутов","reading metadata"):t("открытие каталога","opening directory");
        return "\n\n"+t("Операция: ","Operation: ")+action+"\n"+t("Путь внутри исходного контейнера: ","Path inside source container: ")+(path.isEmpty()?t("корень контейнера","container root"):safe.toString())+"\n"+t("Версия проверки: ","Diagnostic version: ")+com.termux.BuildConfig.VERSION_NAME;
    }
    String error(String code){
        if(code.contains("_ERRNO_")){String[] parts=code.split("_ERRNO_",2);return error(parts[0])+"\n"+t("Системный код: ","System code: ")+parts[1];}
        switch(code){case "ENVIRONMENT_TOOLS_REQUIRED":return t("В Termux отсутствует Python или proot-distro. Можно подготовить необходимые компоненты автоматически.","Python or proot-distro is missing in Termux. Required components can be prepared automatically.");
        case "STORAGE_ACCESS_DENIED":return t("Android отказал в доступе к пути хранилища proot-distro. Это ошибка доступа, а не признак отсутствия пакетов. Код: STORAGE_ACCESS_DENIED", "Android denied access to the proot-distro storage path. This is an access failure, not evidence of missing packages. Code: STORAGE_ACCESS_DENIED");
        case "PROCESS_CHECK_DENIED":return t("Android не разрешил проверить процессы. Без проверки занятости операция не запускается. Код: PROCESS_CHECK_DENIED", "Android denied process inspection. The operation cannot start without the busy check. Code: PROCESS_CHECK_DENIED");
        case "ENVIRONMENT_PATH_MISSING":return t("Не найден путь исходного окружения в containers либо обязательный файл системы. Нужна проверка структуры среды. Код: ENVIRONMENT_PATH_MISSING", "The source containers environment path or a required system file is missing. Check the environment layout. Code: ENVIRONMENT_PATH_MISSING");
        case "LOCKS_ACCESS_DENIED":return t("Нет доступа к каталогу или файлу блокировки proot-distro. Файлы выбранного окружения ещё не сканировались. Код: LOCKS_ACCESS_DENIED","Access to the proot-distro lock directory or file was denied. Files of the selected environment have not been scanned. Code: LOCKS_ACCESS_DENIED");
        case "SCAN_ACCESS_DENIED":return t("Отказ в доступе при проверке выбранного окружения. Проверка не завершена; удаление не началось. Код: SCAN_ACCESS_DENIED","Access denied while checking the selected environment. The check is incomplete; deletion has not started. Code: SCAN_ACCESS_DENIED");
        case "ENVIRONMENT_ACCESS_DENIED":return t("Нет доступа к структуре каталогов окружения. Код: ENVIRONMENT_ACCESS_DENIED","Access to the environment directory structure was denied. Code: ENVIRONMENT_ACCESS_DENIED");

        case "SEALED_DIRECTORY_NOT_EMPTY":return t("Служебный каталог run/systemd/dissect-root не пуст. Удаление остановлено; окружение возвращено на исходное место. Права не изменены.","The service directory run/systemd/dissect-root is not empty. Deletion stopped; the environment was restored to its original location. Permissions were not changed.");
        case "FILES_ACCESS_DENIED":return t("Нет доступа к одному из файлов или каталогов окружения. Установка пакетов не предлагается, так как причина в доступе. Код: FILES_ACCESS_DENIED", "An environment file or directory is inaccessible. Package installation is not suggested for an access failure. Code: FILES_ACCESS_DENIED");
        case "FILESYSTEM_ERROR":return t("Файловая операция завершилась ошибкой; системный код приведён ниже. Код: FILESYSTEM_ERROR", "Filesystem operation failed; see the system code below. Code: FILESYSTEM_ERROR");case "CANCELLED":return t("Проверка отменена. Окружение сохранено.","Check cancelled. The environment is retained.");case "ENVIRONMENT_BUSY":return t("Среда занята. Закройте её сессии, рабочий стол и пакетные операции, затем повторите. Процессы автоматически не останавливаются.","Environment busy. Close its sessions, desktop and package operations, then retry. Processes are not stopped automatically.");case "SOURCE_CHANGED":return t("Файлы среды изменились. Повторите сканирование при закрытых сессиях.","Environment files changed. Scan again with sessions closed.");case "NATIVE_LOCKS_REQUIRED":return t("Нужен proot-distro 5 с блокировками и хранилищем containers. Обновите пакет proot-distro в Termux и повторите. Старые среды не изменены.","Requires proot-distro 5 with locks and containers storage. Update proot-distro in Termux and retry. Existing systems are unchanged.");case "DELETE_INCOMPLETE":return t("Удаление завершилось ошибкой и могло удалить часть файлов. Остаток находится в скрытом служебном каталоге .jobs-delete-* внутри хранилища proot-distro. Другие окружения не удалялись.","Deletion failed and may have removed some files. The remainder is in a hidden .jobs-delete-* service directory in proot-distro storage. Other systems were not removed.");case "UNSUPPORTED_SPECIAL_FILE":return t("В среде есть специальный файл, который нельзя безопасно скопировать. Операция остановлена; среда сохранена.","The system contains a special file which cannot be safely copied. Operation stopped; the environment is retained.");default:return t("Операция не завершена. Проверьте фактическое состояние окружения. Код: ","Operation incomplete. Check the actual environment state. Code: ")+code;}}
    public void close(){closed=true;File f=cancelFile;if(f!=null)try{f.createNewFile();}catch(IOException ignored){}if(progress!=null)progress.dismiss();worker.shutdown();}
}
