package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.*;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.R;
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

/** Separate, target-scoped Bionic driver libraries; never replaces system drivers. */
final class WorkspaceDesktopWrapper implements AutoCloseable {
    static final String DEFAULT_DRIVER_ID="ce35642c5d93ecdfa08b730f80fd738ad71b473cdd7aa3dc58c37d2e6b1e984f";
    static final String DEFAULT_DRIVER_NAME="a6xx-a7xx-a8xx";
    private final TermuxActivity activity;
    private final String target;
    private final WorkspaceLocalFilePicker picker;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed,busy;
    private Runnable cancel;
    private AlertDialog window;
    WorkspaceDesktopWrapper(TermuxActivity activity){this(activity,"termux");}
    WorkspaceDesktopWrapper(TermuxActivity activity,String target){if(!supported(target)&&!bridgeSupported(target))throw new IllegalArgumentException("Bionic target");this.activity=activity;this.target=target;picker=new WorkspaceLocalFilePicker(activity);}
    static boolean bridgeSupported(String target){return "ubuntu".equals(target)||"debian".equals(target)||"fedora".equals(target)||"archlinux".equals(target)||"opensuse".equals(target)||"void".equals(target);}
    private boolean bridge(){return bridgeSupported(target);}
    static boolean supported(String target){return "termux".equals(target);}
    static File base(android.content.Context context){return base(context,"termux");}
    static File base(android.content.Context context,String target){return new File(context.getFilesDir(),bridgeSupported(target)?("workspace-x11-bionic-glibc"+("ubuntu".equals(target)?"":"-"+target)):"workspace-x11-wrapper");}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    boolean busy(){return busy;}
    static JSONObject metadata(android.content.Context context){return metadata(context,"termux");}
    static JSONObject metadata(android.content.Context context,String target){
        try{
            File root=base(context,target),file=new File(root,"current.json");
            if(Files.isSymbolicLink(root.toPath())||Files.isSymbolicLink(file.toPath())||!file.isFile()||file.length()>32768)return null;
            JSONObject value=new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
            if(!value.optString("id").matches("[a-f0-9]{64}")||!value.optString("library").matches("[A-Za-z0-9_][A-Za-z0-9_.+-]{0,127}\\.so"))return null;
            File parent=new File(root,"drivers"),driver=new File(parent,value.getString("id")),library=new File(driver,value.getString("library"));
            if(Files.isSymbolicLink(parent.toPath())||Files.isSymbolicLink(driver.toPath())||Files.isSymbolicLink(library.toPath())||!library.isFile())return null;
            return value;
        }catch(Exception error){return null;}
    }
    static boolean builtinSelected(android.content.Context context,String target){return metadata(context,target)==null||context.getSharedPreferences("workspace-x11",0).getBoolean("builtin-driver:"+target,false);}
    static void chooseBuiltin(android.content.Context context,String target){context.getSharedPreferences("workspace-x11",0).edit().putBoolean("builtin-driver:"+target,true).apply();}
    static void chooseImported(android.content.Context context,String target){context.getSharedPreferences("workspace-x11",0).edit().putBoolean("builtin-driver:"+target,false).apply();}
    static boolean installed(android.content.Context context){return installed(context,"termux");}
    static boolean installed(android.content.Context context,String target){
        File runtime=new File(base(context,target),"runtime"),ready=new File(runtime,"ready.json");
        JSONObject selected=metadata(context,target);
        return selected!=null&&(!builtinSelected(context,target)||DEFAULT_DRIVER_ID.equals(selected.optString("id")))&&!Files.isSymbolicLink(runtime.toPath())&&!Files.isSymbolicLink(ready.toPath())&&ready.isFile();
    }
    static String driverLabel(android.content.Context context){return driverLabel(context,"termux");}
    static String driverLabel(android.content.Context context,String target){if(builtinSelected(context,target))return DEFAULT_DRIVER_NAME;JSONObject value=metadata(context,target);return value==null?"":value.optString("name","Turnip Bionic");}
    void showDrivers(BooleanSupplier unavailable,Runnable updated){
        if(closed||busy||unavailable.getAsBoolean())return;
        // A fresh installation can choose the virtual preset before host Python exists.
        if(!new File(base(activity,target),"drivers").exists()&&metadata(activity,target)==null){driverChoices(new JSONArray(),unavailable,updated);return;}
        libraryJob("catalog","-",unavailable,updated);
    }
    private void driverChoices(JSONArray drivers,BooleanSupplier unavailable,Runnable updated){
        if(closed)return;if(unavailable.getAsBoolean()){updated.run();return;}
        JSONArray imported=new JSONArray();for(int i=0;i<drivers.length();i++){JSONObject value=drivers.optJSONObject(i);if(value!=null&&!DEFAULT_DRIVER_ID.equals(value.optString("id")))imported.put(value);}
        String[] labels=new String[imported.length()+1],ids=new String[labels.length];boolean[] enabled=new boolean[labels.length];
        JSONObject current=metadata(activity,target);String selected=current==null?"":current.optString("id");int checked=builtinSelected(activity,target)?0:-1;
        labels[0]=DEFAULT_DRIVER_NAME;ids[0]=DEFAULT_DRIVER_ID;enabled[0]=Build.VERSION.SDK_INT>=28;
        for(int i=1;i<labels.length;i++){
            JSONObject value=imported.optJSONObject(i-1);ids[i]=value.optString("id");labels[i]=value.optString("name","Turnip Bionic");
            enabled[i]=ids[i].matches("[a-f0-9]{64}")&&value.optInt("minApi",0)<=Build.VERSION.SDK_INT;
            if(!builtinSelected(activity,target)&&selected.equals(ids[i]))checked=i;
        }
        for(int i=0;i<labels.length;i++)for(int j=i+1;j<labels.length;j++)if(labels[i].equals(labels[j])){
            String name=labels[i];for(int k=i;k<labels.length;k++)if(labels[k].equals(name))labels[k]+=" · "+ids[k].substring(0,Math.min(8,ids[k].length()));break;
        }
        final int selectedIndex=checked;
        BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return labels.length;}
            public Object getItem(int position){return labels[position];}
            public long getItemId(int position){return position;}
            public View getView(int position,View recycled,ViewGroup parent){
                LinearLayout outer=new LinearLayout(activity);outer.setGravity(Gravity.CENTER_VERTICAL);outer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                outer.setPadding(WorkspaceDialogs.dp(activity,16),WorkspaceDialogs.dp(activity,4),WorkspaceDialogs.dp(activity,16),WorkspaceDialogs.dp(activity,4));
                LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(WorkspaceDialogs.background(activity,position==selectedIndex));
                TextView label=WorkspaceDialogs.action(activity,labels[position],0);label.setBackground(null);label.setAlpha(enabled[position]?1f:.4f);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
                if(position>0&&ids[position].matches("[a-f0-9]{64}")){
                    ImageButton trash=new ImageButton(activity);trash.setImageResource(R.drawable.workspace_delete);trash.setImageTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.DANGER));trash.setScaleType(ImageView.ScaleType.CENTER_INSIDE);trash.setPadding(WorkspaceDialogs.dp(activity,10),WorkspaceDialogs.dp(activity,10),WorkspaceDialogs.dp(activity,10),WorkspaceDialogs.dp(activity,10));trash.setBackground(WorkspaceDialogs.background(activity,false));trash.setFocusable(false);trash.setTag("wrapper-remove-"+ids[position]);trash.setContentDescription(tr("Удалить ","Remove ")+labels[position]);trash.setOnClickListener(v->{if(closed||busy||unavailable.getAsBoolean())return;window.dismiss();confirmRemove(ids[position],labels[position],unavailable,updated);});row.addView(trash,new LinearLayout.LayoutParams(WorkspaceDialogs.dp(activity,44),WorkspaceDialogs.dp(activity,44)));
                }
                outer.addView(row,new LinearLayout.LayoutParams(-1,-2));return outer;
            }
        };
        window=WorkspaceDialogs.builder(activity).setTitle(tr("Драйверы Wrapper","Wrapper drivers"))
            .setAdapter(adapter,(d,i)->{
                if(!enabled[i]||closed)return;if(unavailable.getAsBoolean()){updated.run();return;}
                if(i==0){chooseBuiltin(activity,target);updated.run();return;}
                if(selected.equals(ids[i])){chooseImported(activity,target);updated.run();return;}
                libraryJob("select",ids[i],unavailable,updated);
            }).setNegativeButton(tr("Назад","Back"),(d,w)->updated.run()).create();window.show();
    }
    private void confirmRemove(String identity,String label,BooleanSupplier unavailable,Runnable updated){
        window=WorkspaceDialogs.builder(activity).setTitle(tr("Удалить драйвер?","Remove driver?"))
            .setMessage(label+"\n\n"+tr("Удалится только копия из выбранного окружения. Оригинальный ZIP в памяти телефона сохранится. Если удалён выбранный драйвер, будет выбран a6xx-a7xx-a8xx; загрузка произойдёт при следующем запуске стола.","Removes only this environment’s copy. The original phone ZIP is preserved. Removing the selected driver selects a6xx-a7xx-a8xx; it downloads at the next desktop launch."))
            .setPositiveButton(tr("Удалить","Remove"),(d,w)->libraryJob("remove",identity,unavailable,updated))
            .setNegativeButton(tr("Отмена","Cancel"),(d,w)->showDrivers(unavailable,updated)).create();window.show();
    }
    private void libraryJob(String operation,String identity,BooleanSupplier unavailable,Runnable updated){
        if(closed||busy||unavailable.getAsBoolean())return;
        busy=true;AtomicBoolean cancelled=new AtomicBoolean();AtomicReference<File> cancellation=new AtomicReference<>();
        window=WorkspaceDialogs.builder(activity).setTitle(tr("Драйверы Wrapper","Wrapper drivers"))
            .setMessage(operation.equals("catalog")?tr("Чтение списка драйверов…","Reading driver library…"):operation.equals("remove")?tr("Удаление драйвера…","Removing driver…"):tr("Проверка и выбор драйвера…","Verifying and selecting driver…"))
            .setNegativeButton(tr("Отмена","Cancel"),null).create();window.setCancelable(false);window.setCanceledOnTouchOutside(false);window.show();AlertDialog progress=window;
        cancel=()->{cancelled.set(true);File marker=cancellation.get();if(marker!=null)try{marker.createNewFile();}catch(IOException ignored){}progress.getButton(-2).setEnabled(false);};progress.getButton(-2).setOnClickListener(v->cancel.run());
        worker.execute(()->{
            File job=null;Result result=new Result(false,"ERROR:WRAPPER_LIBRARY_FAILED");
            try{job=jobDirectory();File marker=new File(job,"cancel");cancellation.set(marker);if(cancelled.get())marker.createNewFile();result=run(job,operation,identity,marker,line->{});}catch(Exception ignored){}finally{cleanup(job);}
            Result answer=result;main.post(()->{
                busy=false;cancel=null;progress.dismiss();if(closed)return;
                if(operation.equals("remove")&&answer.success&&metadata(activity,target)==null)chooseBuiltin(activity,target);
                if(cancelled.get()){updated.run();return;}
                if(!answer.success){message(answer.report.contains("WRAPPER_BUSY")?tr("Сначала остановите рабочий стол выбранного окружения. Выбор драйвера сохранён.","Stop the selected environment’s desktop first. The selected driver is preserved."):(operation.equals("remove")?tr("Не удалось удалить драйвер. Подробности:\n","Could not remove the driver. Details:\n"):tr("Не удалось прочитать или выбрать драйвер. Предыдущий выбор сохранён.\n","Could not read or select driver. The previous selection is preserved.\n"))+answer.report,updated);return;}
                if(operation.equals("select")){chooseImported(activity,target);updated.run();return;}
                if(operation.equals("remove")){showDrivers(unavailable,updated);return;}
                try{for(String line:answer.report.split("\n"))if(line.startsWith("WRAPPER_DRIVERS:")){driverChoices(new JSONArray(line.substring("WRAPPER_DRIVERS:".length())),unavailable,updated);return;}throw new IOException("Missing library");}
                catch(Exception ignored){message(tr("Не удалось прочитать список драйверов.","Could not read the driver library."),updated);}
            });
        });
    }
    void choose(BooleanSupplier unavailable,Runnable updated){
        if(closed||busy||unavailable.getAsBoolean())return;
        picker.choosePhoneFile((source,entry)->{
            if(closed||busy||unavailable.getAsBoolean())return;
            if(!entry.name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")||entry.size>64L*1024*1024){message(tr("Выберите ZIP драйвера Turnip Bionic до 64 МБ с meta.json и библиотекой ARM64.","Choose a Bionic ZIP up to 64 MB with meta.json and an ARM64 library."),updated);return;}
            window=WorkspaceDialogs.builder(activity).setTitle(tr("Загрузить драйвер Bionic?","Import Bionic driver?"))
                .setMessage((bridge()?tr("Для выбранного glibc-окружения, через отдельный мост. Совместимость ZIP с конкретным телефоном не гарантируется.\n","For the selected glibc environment, through its separate bridge. ZIP/device compatibility is not guaranteed.\n"):"")+tr("Драйвер рабочего стола. ZIP будет скопирован и проверен в приватной папке приложения. Драйвер — исполняемый код с доступом к данным приложения: выбирайте только доверенный источник. Проверка формата и SHA-256 не подтверждает безопасность или совместимость. После импорта драйвер добавится в список и будет выбран; остальные загруженные драйверы и оригинал в памяти телефона сохранятся.","Desktop driver. The ZIP will be copied and checked in private app storage. A driver is executable code with access to app data: use a trusted source. Format and SHA-256 checks do not establish safety or compatibility. The imported driver is added to the library and selected; other imported drivers and the original phone file are preserved."))
                .setPositiveButton(tr("Загрузить","Import"),(d,w)->{if(!closed&&!unavailable.getAsBoolean())importDriver(source,entry,unavailable,updated);})
                .setNegativeButton(tr("Назад","Back"),(d,w)->updated.run()).create();window.show();
        });
    }
    private void importDriver(WorkspaceStore.Session source,ProjectFiles.Entry entry,BooleanSupplier unavailable,Runnable updated){
        busy=true;AtomicBoolean cancelled=new AtomicBoolean();
        LinearLayout body=AgentStatusUi.body(activity);ProgressBar bar=new ProgressBar(activity);body.addView(bar);
        TextView text=AgentStatusUi.info(body,tr("Копирование и проверка драйвера…","Copying and checking driver…"));
        window=WorkspaceDialogs.builder(activity).setTitle("Wrapper · Bionic").setView(body).setNegativeButton(tr("Отмена","Cancel"),null).create();window.setCancelable(false);window.setCanceledOnTouchOutside(false);window.show();
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
                if(ok){chooseImported(activity,target);updated.run();return;}
                if(cancelled.get()){updated.run();return;}
                message(detail.contains("WRAPPER_BUSY")?tr("Остановите выбранный рабочий стол перед заменой драйвера. Предыдущий выбор сохранён.","Stop the selected desktop before replacing its driver. The previous selection is preserved."):tr("Не удалось импортировать драйвер. Нужен ZIP Bionic ARM64 с meta.json, подходящий версии Android. Предыдущий драйвер сохранён.\n","Could not import driver. Choose a Bionic ARM64 ZIP with meta.json matching this Android version. The previous driver is preserved.\n")+detail,updated);
            });
        });
    }
    void prepare(BooleanSupplier unavailable,Runnable updated,WorkspaceDesktopGpu.Inline inline){
        if(closed||busy||unavailable.getAsBoolean()){finish(inline,false,"ERROR:GPU_BUSY",updated);return;}
        if(!builtinSelected(activity,target)&&metadata(activity,target)==null){finish(inline,false,tr("Выберите ZIP Bionic кнопкой папки рядом с рендером.","Choose a Bionic ZIP using the folder button next to the renderer."),updated);return;}
        if(builtinSelected(activity,target)){install(unavailable,updated,inline);return;}
        if(bridge()){install(unavailable,updated,inline);return;}
        if(installed(activity,target)){updated.run();return;}
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
                Result result=run(job,builtinSelected(activity,target)?"install-default":"install","-",marker,line->{
                    if(!line.startsWith("GPU_PROGRESS:"))return;
                    try{String[] parts=line.split(":");long done=Long.parseLong(parts[1]),total=Long.parseLong(parts[2]),now=SystemClock.elapsedRealtime();if(now-last[0]<200&&done<total)return;last[0]=now;
                        int percent=(int)Math.min(100,done*100/Math.max(1,total));long eta=done==0?-1:(now-began)*(total-done)/done/1000;
                        String text=String.format(java.util.Locale.ROOT,"%.1f / %.1f MB",done/1e6,total/1e6)+(eta<0?"":" · ≈ "+eta+tr(" с"," s"));main.post(()->{if(!closed&&!cancelled.get())inline.progress(percent,text);});
                    }catch(Exception ignored){}
                });success=result.success&&result.report.contains("READY:wrapper");report=result.report;
            }catch(Exception ignored){}finally{cleanup(job);}
            boolean ok=success&&!cancelled.get();String detail=cancelled.get()?"ERROR:GPU_CANCELLED":ok?report:WorkspaceDesktopGpu.failure(activity,report);main.post(()->{busy=false;cancel=null;if(!closed)finish(inline,ok,detail,updated);});
        });
    }
    private void message(String text,Runnable updated){if(closed)return;window=WorkspaceDialogs.builder(activity).setMessage(text).setPositiveButton(tr("Назад","Back"),(d,w)->updated.run()).create();window.show();}
    private static void finish(WorkspaceDesktopGpu.Inline inline,boolean success,String report,Runnable updated){if(inline!=null)inline.finished(success,report);updated.run();}
    private File jobDirectory()throws Exception{
        File root=base(activity,target);if(Files.isSymbolicLink(root.toPath())||(!root.isDirectory()&&!root.mkdirs()))throw new IOException("Wrapper directory");return Files.createTempDirectory(root.toPath(),".job-").toFile();
    }
    private static final class Result{final boolean success;final String report;Result(boolean ok,String text){success=ok;report=text;}}
    private Result run(File job,String operation,String source,File cancel,Consumer<String> output)throws Exception{
        for(String name:bridge()?new String[]{"x11-wrapper.py","x11-bionic-glibc.py","x11-gpu.py","x11-extras.py","x11-default-driver.py"}:new String[]{"x11-wrapper.py","x11-gpu.py","x11-extras.py","x11-default-driver.py"})try(InputStream in=activity.getAssets().open(name);OutputStream out=new FileOutputStream(new File(job,name))){byte[] data=new byte[8192];int count;while((count=in.read(data))!=-1)out.write(data,0,count);}
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/python3","-B",new File(job,operation.equals("install-default")?"x11-default-driver.py":bridge()?"x11-bionic-glibc.py":"x11-wrapper.py").getPath(),operation,target,base(activity,target).getPath(),source,Integer.toString(Build.VERSION.SDK_INT),cancel.getPath());
        builder.directory(job);builder.environment().clear();for(String value:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=value.indexOf('=');if(eq>0)builder.environment().put(value.substring(0,eq),value.substring(eq+1));}builder.environment().put("PREFIX",TermuxConstants.TERMUX_PREFIX_DIR_PATH);builder.redirectErrorStream(true);
        java.lang.Process process=builder.start();process.getOutputStream().close();StringBuilder tail=new StringBuilder();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){tail.append(line).append('\n');if(tail.length()>262144)tail.delete(0,tail.length()-262144);output.accept(line);}}
        return new Result(process.waitFor()==0,tail.toString());
    }
    private static void cleanup(File job){if(job==null)return;for(String name:new String[]{"driver.zip","cancel","x11-wrapper.py","x11-gpu.py","x11-bionic-glibc.py","x11-bionic-glibc.zip","x11-extras.py","x11-default-driver.py"})new File(job,name).delete();job.delete();}
    @Override public void close(){closed=true;picker.close();if(cancel!=null)cancel.run();if(window!=null)window.dismiss();worker.shutdown();main.removeCallbacksAndMessages(null);}
}
