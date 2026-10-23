package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.json.JSONObject;
import org.json.JSONArray;

/** Termux-only Wrapper selection/import; no guest rootfs or global driver settings. */
final class WorkspaceDesktopWrapper implements AutoCloseable {
    private final TermuxActivity activity;
    private final WorkspaceLocalFilePicker picker;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed,busy;
    private Runnable cancel;
    private AlertDialog window;
    WorkspaceDesktopWrapper(TermuxActivity activity){this.activity=activity;picker=new WorkspaceLocalFilePicker(activity);}
    static boolean supported(String target){return "termux".equals(target);}
    static File base(android.content.Context context){return new File(context.getFilesDir(),"workspace-x11-wrapper");}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    boolean busy(){return busy;}
    static JSONObject metadata(android.content.Context context){
        try{
            File root=base(context),file=new File(root,"current.json");
            if(Files.isSymbolicLink(root.toPath())||Files.isSymbolicLink(file.toPath())||!file.isFile()||file.length()>32768)return null;
            JSONObject value=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
            if(!value.optString("id").matches("[a-f0-9]{64}")||!value.optString("library").matches("lib[A-Za-z0-9_.+-]{1,100}\\.so"))return null;
            File parent=new File(root,"drivers"),driver=new File(parent,value.getString("id")),library=new File(driver,value.getString("library"));
            if(Files.isSymbolicLink(parent.toPath())||Files.isSymbolicLink(driver.toPath())||Files.isSymbolicLink(library.toPath())||!library.isFile())return null;
            return value;
        }catch(Exception error){return null;}
    }
    static boolean installed(android.content.Context context){
        File runtime=new File(base(context),"runtime"),ready=new File(runtime,"ready.json");
        return metadata(context)!=null&&!Files.isSymbolicLink(runtime.toPath())&&!Files.isSymbolicLink(ready.toPath())&&ready.isFile();
    }
    static String driverLabel(android.content.Context context){JSONObject value=metadata(context);return value==null?"":value.optString("name","Turnip Bionic");}
    void showDrivers(BooleanSupplier unavailable,Runnable updated){libraryJob("catalog","-",unavailable,updated);}
    private void driverChoices(JSONArray drivers,BooleanSupplier unavailable,Runnable updated){
        if(closed)return;if(unavailable.getAsBoolean()){updated.run();return;}
        if(drivers.length()==0){message(tr("Нет загруженных драйверов. Добавьте ZIP кнопкой папки справа от рендера.","No imported drivers. Add a ZIP using the folder button next to the renderer."),updated);return;}
        String[] labels=new String[drivers.length()],ids=new String[drivers.length()];boolean[] enabled=new boolean[drivers.length()];
        JSONObject current=metadata(activity);String selected=current==null?"":current.optString("id");int checked=-1;
        for(int i=0;i<labels.length;i++){
            JSONObject value=drivers.optJSONObject(i);if(value==null){labels[i]="?";ids[i]="";continue;}
            ids[i]=value.optString("id");labels[i]=value.optString("name","Turnip Bionic");
            enabled[i]=ids[i].matches("[a-f0-9]{64}")&&value.optInt("minApi",0)<=Build.VERSION.SDK_INT;
            if(selected.equals(ids[i]))checked=i;
        }
        for(int i=0;i<labels.length;i++)for(int j=i+1;j<labels.length;j++)if(labels[i].equals(labels[j])){
            String name=labels[i];for(int k=i;k<labels.length;k++)if(labels[k].equals(name))labels[k]+=" · "+ids[k].substring(0,Math.min(8,ids[k].length()));break;
        }
        window=WorkspaceDialogs.builder(activity).setTitle(tr("Драйверы Wrapper","Wrapper drivers"))
            .setAdapter(WorkspaceDialogs.choiceAdapter(activity,labels,checked,i->enabled[i]),(d,i)->{
                if(!enabled[i]||closed)return;if(unavailable.getAsBoolean()||selected.equals(ids[i])){updated.run();return;}
                libraryJob("select",ids[i],unavailable,updated);
            }).setNegativeButton(tr("Назад","Back"),(d,w)->updated.run()).create();window.show();
    }
    private void libraryJob(String operation,String identity,BooleanSupplier unavailable,Runnable updated){
        if(closed||busy||unavailable.getAsBoolean())return;
        busy=true;AtomicBoolean cancelled=new AtomicBoolean();AtomicReference<File> cancellation=new AtomicReference<>();
        window=WorkspaceDialogs.builder(activity).setTitle(tr("Драйверы Wrapper","Wrapper drivers"))
            .setMessage(operation.equals("catalog")?tr("Чтение списка драйверов…","Reading driver library…"):tr("Проверка и выбор драйвера…","Verifying and selecting driver…"))
            .setNegativeButton(tr("Отмена","Cancel"),null).create();window.setCancelable(false);window.setCanceledOnTouchOutside(false);window.show();AlertDialog progress=window;
        cancel=()->{cancelled.set(true);File marker=cancellation.get();if(marker!=null)try{marker.createNewFile();}catch(IOException ignored){}progress.getButton(-2).setEnabled(false);};progress.getButton(-2).setOnClickListener(v->cancel.run());
        worker.execute(()->{
            File job=null;Result result=new Result(false,"ERROR:WRAPPER_LIBRARY_FAILED");
            try{job=jobDirectory();File marker=new File(job,"cancel");cancellation.set(marker);if(cancelled.get())marker.createNewFile();result=run(job,operation,identity,marker,line->{});}catch(Exception ignored){}finally{cleanup(job);}
            Result answer=result;main.post(()->{
                busy=false;cancel=null;progress.dismiss();if(closed)return;
                if(cancelled.get()){updated.run();return;}
                if(!answer.success){message(answer.report.contains("WRAPPER_BUSY")?tr("Сначала остановите рабочий стол Termux. Выбор драйвера сохранён.","Stop the Termux desktop first. The selected driver is preserved."):tr("Не удалось прочитать или выбрать драйвер. Предыдущий выбор сохранён.\n","Could not read or select driver. The previous selection is preserved.\n")+answer.report,updated);return;}
                if(operation.equals("select")){updated.run();return;}
                try{for(String line:answer.report.split("\n"))if(line.startsWith("WRAPPER_DRIVERS:")){driverChoices(new JSONArray(line.substring("WRAPPER_DRIVERS:".length())),unavailable,updated);return;}throw new IOException("Missing library");}
                catch(Exception ignored){message(tr("Не удалось прочитать список драйверов.","Could not read the driver library."),updated);}
            });
        });
    }
    void choose(BooleanSupplier unavailable,Runnable updated){
        if(closed||busy||unavailable.getAsBoolean())return;
        picker.choosePhoneFile((source,entry)->{
            if(closed||busy||unavailable.getAsBoolean())return;
            if(!entry.name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")||entry.size>64L*1024*1024){message(tr("Выберите ZIP драйвера Turnip Bionic до 64 МБ с meta.json и библиотекой ARM64.","Choose a Turnip Bionic ZIP up to 64 MB with meta.json and an ARM64 library."),updated);return;}
            window=WorkspaceDialogs.builder(activity).setTitle(tr("Загрузить драйвер Bionic?","Import Bionic driver?"))
                .setMessage(tr("Только для рабочего стола Termux. ZIP будет скопирован и проверен в приватной папке приложения. Драйвер — исполняемый код с доступом к данным приложения: выбирайте только доверенный источник. Проверка формата и SHA-256 не подтверждает безопасность или совместимость. После импорта драйвер добавится в список и будет выбран; остальные загруженные драйверы и оригинал в памяти телефона сохранятся.","For the Termux desktop only. The ZIP will be copied and checked in private app storage. A driver is executable code with access to app data: use a trusted source. Format and SHA-256 checks do not establish safety or compatibility. The imported driver is added to the library and selected; other imported drivers and the original phone file are preserved."))
                .setPositiveButton(tr("Загрузить","Import"),(d,w)->{if(!closed&&!unavailable.getAsBoolean())importDriver(source,entry,unavailable,updated);})
                .setNegativeButton(tr("Назад","Back"),(d,w)->updated.run()).create();window.show();
        });
    }
    private void importDriver(WorkspaceStore.Session source,ProjectFiles.Entry entry,BooleanSupplier unavailable,Runnable updated){
        busy=true;AtomicBoolean cancelled=new AtomicBoolean();
        LinearLayout body=AgentStatusUi.body(activity);ProgressBar bar=new ProgressBar(activity);body.addView(bar);
        TextView text=AgentStatusUi.info(body,tr("Копирование и проверка драйвера…","Copying and checking driver…"));
        window=WorkspaceDialogs.builder(activity).setTitle("Wrapper · Turnip Bionic").setView(body).setNegativeButton(tr("Отмена","Cancel"),null).create();window.setCancelable(false);window.setCanceledOnTouchOutside(false);window.show();
        AlertDialog progress=window;AtomicReference<File> cancellation=new AtomicReference<>();
        cancel=()->{cancelled.set(true);File marker=cancellation.get();if(marker!=null)try{marker.createNewFile();}catch(IOException ignored){}text.setText(tr("Отмена…","Cancelling…"));progress.getButton(-2).setEnabled(false);};
        progress.getButton(-2).setOnClickListener(v->cancel.run());
        worker.execute(()->{
            boolean success=false;String report="ERROR:WRAPPER_IMPORT_FAILED";File job=null;
            try{
                job=jobDirectory();File archive=new File(job,"driver.zip"),marker=new File(job,"cancel");cancellation.set(marker);
                if(cancelled.get())marker.createNewFile();
                try(ProjectFiles files=new ProjectFiles(activity,source)){files.export(entry,archive,64L*1024*1024);}
                if(!cancelled.get()){
                    Result result=run(job,"import",archive.getPath(),marker,chunk->{});success=result.success&&result.report.contains("READY:wrapper-driver");report=result.report;
                }
            }catch(Exception ignored){}finally{cleanup(job);}
            boolean ok=success;String detail=report;main.post(()->{
                busy=false;cancel=null;progress.dismiss();if(closed)return;
                if(ok||cancelled.get()){updated.run();return;}
                message(detail.contains("WRAPPER_BUSY")?tr("Остановите рабочий стол Termux перед заменой драйвера. Предыдущий выбор сохранён.","Stop the Termux desktop before replacing its driver. The previous selection is preserved."):tr("Не удалось импортировать драйвер. Нужен ZIP Bionic ARM64 с meta.json, подходящий версии Android. Предыдущий драйвер сохранён.\n","Could not import driver. Choose a Bionic ARM64 ZIP with meta.json matching this Android version. The previous driver is preserved.\n")+detail,updated);
            });
        });
    }
    void prepare(BooleanSupplier unavailable,Runnable updated,WorkspaceDesktopGpu.Inline inline){
        if(closed||busy||unavailable.getAsBoolean()){finish(inline,false,"ERROR:GPU_BUSY",updated);return;}
        if(metadata(activity)==null){finish(inline,false,tr("Выберите ZIP Turnip Bionic кнопкой папки рядом с рендером.","Choose a Turnip Bionic ZIP using the folder button next to the renderer."),updated);return;}
        if(installed(activity)){updated.run();return;}
        window=WorkspaceDialogs.builder(activity).setTitle(tr("Подготовить Wrapper?","Prepare Wrapper?"))
            .setMessage(tr("Будет загружено около 3,5 МБ: Wrapper Lee Gao и Vulkan Loader для Termux. Компоненты хранятся отдельно, системные драйверы не заменяются. Выбранный Bionic-драйвер будет выполняться только в процессе рабочего стола Termux. Режим экспериментальный.","Downloads about 3.5 MB: Lee Gao’s Wrapper and the Termux Vulkan Loader. Components are stored separately; system drivers are not replaced. The selected Bionic driver will run in the Termux desktop process tree only. Experimental mode."))
            .setNegativeButton(tr("Назад","Back"),(d,w)->finish(inline,false,"ERROR:GPU_CANCELLED",updated))
            .setPositiveButton(tr("Подготовить","Prepare"),(d,w)->install(unavailable,updated,inline)).create();window.setCancelable(false);window.setCanceledOnTouchOutside(false);window.show();
    }
    private void install(BooleanSupplier unavailable,Runnable updated,WorkspaceDesktopGpu.Inline inline){
        if(closed||busy||unavailable.getAsBoolean()){finish(inline,false,"ERROR:GPU_BUSY",updated);return;}
        if(inline==null){message(tr("Запустите рабочий стол для подготовки Wrapper.","Start the desktop to prepare Wrapper."),updated);return;}
        busy=true;AtomicBoolean cancelled=new AtomicBoolean();AtomicReference<File> cancellation=new AtomicReference<>();
        cancel=()->{cancelled.set(true);File marker=cancellation.get();if(marker!=null)try{marker.createNewFile();}catch(IOException ignored){}inline.progress(-1,tr("Остановка загрузки…","Stopping download…"));};inline.started(cancel);
        worker.execute(()->{
            File job=null;boolean success=false;String report="ERROR:WRAPPER_SETUP_FAILED";
            try{
                job=jobDirectory();File marker=new File(job,"cancel");cancellation.set(marker);if(cancelled.get())marker.createNewFile();
                long began=SystemClock.elapsedRealtime();long[] last={0};
                Result result=run(job,"install","-",marker,line->{
                    if(!line.startsWith("GPU_PROGRESS:"))return;
                    try{String[] parts=line.split(":");long done=Long.parseLong(parts[1]),total=Long.parseLong(parts[2]),now=SystemClock.elapsedRealtime();if(now-last[0]<200&&done<total)return;last[0]=now;
                        int percent=(int)Math.min(100,done*100/Math.max(1,total));long eta=done==0?-1:(now-began)*(total-done)/done/1000;
                        String text=String.format(java.util.Locale.ROOT,"%.1f / %.1f MB",done/1e6,total/1e6)+(eta<0?"":" · ≈ "+eta+tr(" с"," s"));main.post(()->{if(!closed&&!cancelled.get())inline.progress(percent,text);});
                    }catch(Exception ignored){}
                });success=result.success&&result.report.contains("READY:wrapper");report=result.report;
            }catch(Exception ignored){}finally{cleanup(job);}
            boolean ok=success&&!cancelled.get();String detail=cancelled.get()?"ERROR:GPU_CANCELLED":report;main.post(()->{busy=false;cancel=null;if(!closed)finish(inline,ok,detail,updated);});
        });
    }
    private void message(String text,Runnable updated){if(closed)return;window=WorkspaceDialogs.builder(activity).setMessage(text).setPositiveButton(tr("Назад","Back"),(d,w)->updated.run()).create();window.show();}
    private static void finish(WorkspaceDesktopGpu.Inline inline,boolean success,String report,Runnable updated){if(inline!=null)inline.finished(success,report);updated.run();}
    private File jobDirectory()throws Exception{
        File root=base(activity);if(Files.isSymbolicLink(root.toPath())||(!root.isDirectory()&&!root.mkdirs()))throw new IOException("Wrapper directory");return Files.createTempDirectory(root.toPath(),".job-").toFile();
    }
    private static final class Result{final boolean success;final String report;Result(boolean ok,String text){success=ok;report=text;}}
    private Result run(File job,String operation,String source,File cancel,Consumer<String> output)throws Exception{
        for(String name:new String[]{"x11-wrapper.py","x11-gpu.py"})try(InputStream in=activity.getAssets().open(name);OutputStream out=new FileOutputStream(new File(job,name))){byte[] data=new byte[8192];int count;while((count=in.read(data))!=-1)out.write(data,0,count);}
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/python3","-B",new File(job,"x11-wrapper.py").getPath(),operation,"termux",base(activity).getPath(),source,Integer.toString(Build.VERSION.SDK_INT),cancel.getPath());
        builder.directory(job);builder.environment().clear();for(String value:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=value.indexOf('=');if(eq>0)builder.environment().put(value.substring(0,eq),value.substring(eq+1));}builder.environment().put("PREFIX",TermuxConstants.TERMUX_PREFIX_DIR_PATH);builder.redirectErrorStream(true);
        java.lang.Process process=builder.start();process.getOutputStream().close();StringBuilder tail=new StringBuilder();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){tail.append(line).append('\n');if(tail.length()>262144)tail.delete(0,tail.length()-262144);output.accept(line);}}
        return new Result(process.waitFor()==0,tail.toString());
    }
    private static void cleanup(File job){if(job==null)return;for(String name:new String[]{"driver.zip","cancel","x11-wrapper.py","x11-gpu.py"})new File(job,name).delete();job.delete();}
    @Override public void close(){closed=true;picker.close();if(cancel!=null)cancel.run();if(window!=null)window.dismiss();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
