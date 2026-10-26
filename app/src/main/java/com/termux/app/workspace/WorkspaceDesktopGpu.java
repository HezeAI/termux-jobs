package com.termux.app.workspace;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.*;
import com.termux.app.TermuxActivity;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.*;

/** Optional shared driver cache. Selection is per environment, never per terminal profile. */
final class WorkspaceDesktopGpu {
    static String directory(String target){return target.equals("ubuntu")?"ubuntu-26.04-mesa26.2.5":target.equals("termux")?"termux-mesa26.2.5":target.equals("debian")?"debian-13-mesa26.2.5":target.equals("alpine")?"alpine-3.24-mesa26.2.5":target.equals("fedora")?"fedora-44-mesa26.2.5":target.equals("opensuse")?"opensuse-16-mesa26.2.5":target.equals("void")?"void-mesa26.2.5":target.equals("archlinux")?"archlinux-mesa26.2.5":"";}
    static boolean supported(String target){return !directory(target).isEmpty();}
    static String mode(android.content.Context c,String target){String value=c.getSharedPreferences("workspace-x11",0).getString("renderer:"+target,"cpu");return value.equals("bionic-glibc")&&WorkspaceDesktopWrapper.bridgeSupported(target)?"bionic-glibc":value.equals("wrapper")&&WorkspaceDesktopWrapper.supported(target)?"wrapper":value.equals("turnip")?"turnip":value.equals("virgl")&&com.termux.x11.VirglInstaller.supported(target)?"virgl":"cpu";}
    private static String t(TermuxActivity a,String ru,String en){return WorkspaceLanguage.code(a).equals("ru")?ru:en;}
    static String failure(TermuxActivity a,String raw){
        String code="GPU_SETUP_FAILED";
        for(String line:raw.split("\\r?\\n"))if(line.matches("ERROR:[A-Z0-9_]{1,100}"))code=line.substring(6);
        if(code.startsWith("GPU_DOWNLOAD_HTTP_"))return t(a,"Источник компонента недоступен: HTTP ","Component source unavailable: HTTP ")+code.substring(18)+t(a,". Повторите позже или проверьте обновление приложения.",". Retry later or check for an app update.");
        if(code.equals("GPU_DOWNLOAD_NETWORK"))return t(a,"Не удалось скачать компонент. Проверьте интернет и повторите.","Could not download the component. Check the connection and retry.");
        if(code.contains("CHECKSUM"))return t(a,"Компонент не прошёл проверку целостности. Повторите загрузку.","Component integrity check failed. Retry the download.");
        return t(a,"Подготовка не завершена. Код: ","Setup incomplete. Code: ")+code;
    }
    static boolean installed(TermuxActivity a,String target){return supported(target)&&new File(new File(new File(a.getFilesDir(),"workspace-x11-gpu"),directory(target)),"ready.json").isFile();}
    static boolean installed(TermuxActivity a,String target,String choice){return choice.equals("bionic-glibc")&&WorkspaceDesktopWrapper.bridgeSupported(target)&&WorkspaceDesktopWrapper.installed(a,target)||choice.equals("wrapper")&&WorkspaceDesktopWrapper.supported(target)&&WorkspaceDesktopWrapper.installed(a)||choice.equals("cpu")||choice.equals("virgl")&&com.termux.x11.VirglInstaller.installed(a,target)||choice.equals("turnip")&&installed(a,target);}
    static String turnipLabel(String target){return "Turnip + Zink (Mesa "+"26.2.5"+")";}
    static String label(TermuxActivity a,String target){String mode=mode(a,target);return mode.equals("bionic-glibc")?"Bionic for glibc (libhybris + Jobs bridge)":mode.equals("wrapper")?"Wrapper · Bionic (AdrenoTools)":mode.equals("virgl")?"VirGL (GPU)":mode.equals("turnip")?turnipLabel(target):t(a,"Программный (CPU)","Software (CPU)");}
    interface Inline {
        void started(Runnable cancel);
        void progress(int percent,String text);
        void finished(boolean success,String report);
    }
    static void choose(TermuxActivity a,String target,BooleanSupplier busy,Runnable updated){
        select(a,target,busy,choice->{if(choice.equals("wrapper")||choice.equals("bionic-glibc")){a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,choice).apply();updated.run();}else prepare(a,target,choice,busy,updated,null);},updated);
    }
    static void select(TermuxActivity a,String target,BooleanSupplier busy,Consumer<String> selected,Runnable back){
        if(busy.getAsBoolean())return;
        boolean bionic=WorkspaceDesktopWrapper.supported(target)||WorkspaceDesktopWrapper.bridgeSupported(target);
        String[] labels=bionic?new String[]{t(a,"Программный (CPU)","Software (CPU)"),turnipLabel(target),"VirGL (GPU)",(WorkspaceDesktopWrapper.bridgeSupported(target)?"Bionic for glibc (libhybris + Jobs bridge)":"Wrapper · Bionic (AdrenoTools)")}:new String[]{t(a,"Программный (CPU)","Software (CPU)"),turnipLabel(target),"VirGL (GPU)"};
        WorkspaceDialogs.builder(a).setTitle(t(a,"Рендеринг · ","Rendering · ")+WorkspaceEnvironments.label(target))
            .setAdapter(WorkspaceDialogs.choiceAdapter(a,labels,(mode(a,target).equals("wrapper")||mode(a,target).equals("bionic-glibc"))?3:mode(a,target).equals("turnip")?1:mode(a,target).equals("virgl")?2:0,i->i==0||(i==1?supported(target):i==2?com.termux.x11.VirglInstaller.supported(target):bionic)),(d,index)->{if(!busy.getAsBoolean()&&(index==0||(index==1?supported(target):index==2?com.termux.x11.VirglInstaller.supported(target):bionic)))selected.accept(index==0?"cpu":index==1?"turnip":index==2?"virgl":WorkspaceDesktopWrapper.bridgeSupported(target)?"bionic-glibc":"wrapper");})
            .setNegativeButton(t(a,"Назад","Back"),(d,w)->back.run()).show();
    }
    static void prepare(TermuxActivity a,String target,String choice,BooleanSupplier busy,Runnable updated,Inline inline){
        prepare(a,target,choice,busy,updated,inline,false);
    }
    static void prepare(TermuxActivity a,String target,String choice,BooleanSupplier busy,Runnable updated,Inline inline,boolean verified){
        if(!choice.equals("cpu")&&!choice.equals("turnip")&&!choice.equals("virgl"))throw new IllegalArgumentException("Invalid renderer");
        if(busy.getAsBoolean()){if(inline!=null)inline.finished(false,"ERROR:GPU_BUSY");updated.run();return;}
        if(choice.equals("virgl")){WorkspaceDesktopVirgl.prepare(a,target,busy,updated,inline,verified);return;}
        if(choice.equals("cpu")||verified&&installed(a,target)){
            a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,choice).apply();updated.run();return;
        }
        if(!supported(target)){if(inline!=null)inline.finished(false,"ERROR:GPU_TARGET_UNSUPPORTED");updated.run();return;}
        AlertDialog confirm=WorkspaceDialogs.builder(a).setTitle(t(a,"Установить Turnip + Zink?","Install Turnip + Zink?"))
                    .setMessage(t(a,"Экспериментальный режим для Adreno 7xx, 830 и 840. Совместимость конкретного устройства проверяется при запуске. Компоненты скачиваются с проверкой SHA-256 в общую папку приложения; системная Mesa не заменяется. Повреждённая копия компонентов приложения будет восстановлена. Для Termux нужно около 1,8 МБ загрузки, для Ubuntu 26.04, Debian 13, Alpine 3.24 Fedora 44, openSUSE Leap 16.0 и Void Linux — около 8–10 МБ. Arch Linux — около 10 МБ, нужны mesa, vulkan-icd-loader и llvm22-libs. Нужны библиотеки Vulkan и EGL: libvulkan1/libegl1 в Ubuntu и Debian, vulkan-loader/mesa-egl в Alpine, vulkan-loader/libglvnd-egl в Fedora, libvulkan1/Mesa-libEGL1/Mesa-dri в openSUSE, vulkan-loader/libglvnd/libllvm18 в Void. Настройка меняется только при остановленном рабочем столе. Остальные сессии не переключаются.","Experimental mode for Adreno 7xx, 830 and 840. Actual device compatibility must be tested. Components are downloaded with SHA-256 verification into shared app storage; system Mesa is not replaced. Damaged application-owned components will be repaired. Termux download is about 1.8 MB; Ubuntu 26.04, Debian 13, Alpine 3.24 Fedora 44, openSUSE Leap 16.0 and Void Linux about 8–10 MB. Arch Linux download is about 10 MB and requires mesa, vulkan-icd-loader and llvm22-libs. Vulkan/EGL libraries are required: libvulkan1/libegl1 on Ubuntu and Debian, vulkan-loader/mesa-egl on Alpine, vulkan-loader/libglvnd-egl on Fedora, libvulkan1/Mesa-libEGL1/Mesa-dri on openSUSE, vulkan-loader/libglvnd/libllvm18 on Void. Change modes only with the desktop stopped. Other sessions are not switched."))
                    .setNegativeButton(t(a,"Назад","Back"),(x,w)->{if(inline!=null)inline.finished(false,"ERROR:GPU_CANCELLED");updated.run();})
                    .setPositiveButton(t(a,"Установить","Install"),(x,w)->{if(!busy.getAsBoolean())install(a,target,busy,updated,inline);else {if(inline!=null)inline.finished(false,"ERROR:GPU_BUSY");updated.run();}}).create();
        confirm.setCancelable(false);confirm.setCanceledOnTouchOutside(false);confirm.show();
    }
    private static void install(TermuxActivity a,String target,BooleanSupplier busy,Runnable updated,Inline inline){
        LinearLayout body=AgentStatusUi.body(a);ProgressBar bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setIndeterminate(true);body.addView(bar);
        TextView detail=AgentStatusUi.info(body,t(a,"Проверка совместимости…","Checking compatibility…"));
        File cancel=new File(new File(a.getFilesDir(),".gpu-control"),"cancel-"+java.util.UUID.randomUUID());java.util.concurrent.atomic.AtomicBoolean cancelRequested=new java.util.concurrent.atomic.AtomicBoolean();
        AlertDialog wait=WorkspaceDialogs.builder(a).setTitle("Turnip + Zink").setView(body).setNegativeButton(t(a,"Отмена","Cancel"),null).setPositiveButton(t(a,"Назад","Back"),null).create();wait.setCancelable(false);wait.setCanceledOnTouchOutside(false);
        Runnable stop=()->{cancelRequested.set(true);try{if(cancel.getParentFile().isDirectory())cancel.createNewFile();}catch(IOException ignored){}if(inline==null)wait.getButton(-2).setEnabled(false);else inline.progress(-1,t(a,"Остановка загрузки…","Stopping download…"));detail.setText(t(a,"Остановка загрузки…","Stopping download…"));};
        if(inline==null){wait.show();wait.getButton(-1).setEnabled(false);wait.getButton(-2).setOnClickListener(v->stop.run());}else inline.started(stop);
        Handler main=new Handler(Looper.getMainLooper());ExecutorService worker=Executors.newSingleThreadExecutor();worker.execute(()->{
            boolean ready=false;String result="";
            try{
                File control=new File(a.getFilesDir(),".gpu-control");if(java.nio.file.Files.isSymbolicLink(control.toPath()))throw new IOException("GPU_CONTROL_LINK");if(!control.isDirectory()&&!control.mkdirs())throw new IOException("GPU_CONTROL_DIRECTORY");
                if(cancelRequested.get())cancel.createNewFile();
                File script=new File(control,"x11-gpu.py");if(java.nio.file.Files.isSymbolicLink(script.toPath()))throw new IOException("GPU_CONTROL_LINK");try(InputStream in=a.getAssets().open("x11-gpu.py");OutputStream out=new FileOutputStream(script)){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);}
                if(!script.setReadable(false,false)||!script.setWritable(false,false)||!script.setReadable(true,true)||!script.setWritable(true,true))throw new IOException("GPU_CONTROL_PERMISSIONS");
                String bin=TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
                ProcessBuilder builder=new ProcessBuilder(bin+"/python3","-B",script.getPath(),"repair",target,new File(a.getFilesDir(),"workspace-x11-gpu").getPath(),cancel.getPath());builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
                for(String item:TermuxShellUtils.buildEnvironment(a,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int i=item.indexOf('=');if(i>0)builder.environment().put(item.substring(0,i),item.substring(i+1));}builder.environment().put("PREFIX",TermuxConstants.TERMUX_PREFIX_DIR_PATH);builder.redirectErrorStream(true);
                Process process=builder.start();process.getOutputStream().close();StringBuilder report=new StringBuilder();long started=SystemClock.elapsedRealtime(),last=0;
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){report.append(line).append('\n');if(report.length()>8000)report.delete(0,report.length()-8000);
                    if(line.startsWith("GPU_PROGRESS:")){String[] parts=line.split(":");long done=Long.parseLong(parts[1]),total=Long.parseLong(parts[2]);long now=SystemClock.elapsedRealtime();if(now-last<200&&done<total)continue;last=now;int percent=(int)Math.min(100,done*100/Math.max(1,total));long seconds=done>0?(now-started)*(total-done)/done/1000:-1;String text=t(a,"Загружено: ","Downloaded: ")+done/1000000.0+" / "+total/1000000.0+" MB\n"+t(a,"Осталось на загрузку: ","Download remaining: ")+(seconds<0?t(a,"рассчитывается","calculating"):"≈ "+seconds+t(a," с"," s"));main.post(()->{if(inline!=null)inline.progress(percent,text);else if(wait.isShowing()){bar.setIndeterminate(false);bar.setProgress(percent);detail.setText(text);}});
                    }else if(line.equals("GPU_STAGE:extract"))main.post(()->{if(inline!=null)inline.progress(-1,t(a,"Проверка и распаковка компонентов…","Verifying and extracting components…"));else if(wait.isShowing()){bar.setIndeterminate(true);detail.setText(t(a,"Проверка и распаковка компонентов…","Verifying and extracting components…"));}});
                }}
                ready=process.waitFor()==0&&report.toString().contains("READY:gpu");result=report.toString();
            }catch(Exception error){result="ERROR:GPU_SETUP_FAILED";}
            cancel.delete();boolean success=ready&&!cancelRequested.get();String report=cancelRequested.get()?"ERROR:GPU_CANCELLED":success?result:failure(a,result);main.post(()->{if(a.isFinishing()||a.isDestroyed())return;if(inline!=null){boolean applied=success&&!busy.getAsBoolean();if(applied)a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,"turnip").apply();inline.finished(applied,report);updated.run();return;}bar.setVisibility(android.view.View.GONE);wait.getButton(-2).setVisibility(android.view.View.GONE);if(success){if(!busy.getAsBoolean())a.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target,"turnip").apply();detail.setText(busy.getAsBoolean()?t(a,"Компоненты установлены. Остановите рабочий стол и выберите Turnip + Zink.","Components installed. Stop the desktop and select Turnip + Zink."):t(a,"Компоненты установлены. Выбран Turnip + Zink. Проверьте рабочий стол и glxinfo -B; программный режим остаётся доступным.","Components installed. Turnip + Zink selected. Test the desktop and glxinfo -B; software mode remains available."));}else detail.setText(t(a,"Не удалось подготовить GPU. Выбор рендеринга не изменён.\n","Could not prepare GPU. Rendering selection is unchanged.\n")+report);wait.getButton(-1).setEnabled(true);wait.getButton(-1).setOnClickListener(v->{wait.dismiss();updated.run();});});worker.shutdown();
        });
    }
}
