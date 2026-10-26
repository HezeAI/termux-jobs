package com.termux.x11;

import android.app.*;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.*;
import android.system.Os;
import android.system.OsConstants;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import com.termux.shared.termux.TermuxConstants;

/** Own process, own private runtime and foreground lifetime. Never kills terminal processes. */
public class X11Service extends Service {
    public static final class Ubuntu extends X11Service {}
    public static final class TestUbuntu extends X11Service {}
    public static final class Debian extends X11Service {}
    public static final class Alpine extends X11Service {}
    public static final class Fedora extends X11Service {}
    public static final class OpenSuse extends X11Service {}
    public static final class VoidLinux extends X11Service {}
    public static final class ArchLinux extends X11Service {}
    private X11Environment environment(){return X11Environment.context(this);}
    static final String STOP="com.termux.x11.STOP_PRIVATE_TEST";
    private static final int NOTIFICATION=7981;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final java.util.Set<String> desktopUsers=new java.util.LinkedHashSet<>();
    private String desktopOwner="",pointerBaseline="unavailable";
    private String observationToken="",observationError="",observationLog="",currentBackdrop="";
    private boolean observationActive,observationPending;
    private int observationSerial;
    private final List<Runnable> listeners=new ArrayList<>();
    private final Binder binder=new LocalBinder();
    private final Runnable notify=()->{for(Runnable r:new ArrayList<>(listeners))r.run();};
    private boolean ownProcess,started,ready,preparingExtras;
    private volatile boolean stopping,retiring;
    private volatile String status="";
    private volatile long keys,clicks,motions;
    private X11Runtime runtime;
    private CmdEntryPoint entry;
    private FileDescriptor discoveryGuard;
    private volatile LocalSocket testSocket;
    private EmbeddedX11Host embedded;
    private String desktop="xfce";
    private boolean garconFilter=false;
    private String componentOperation="";
    private String target="", phase="idle", detail="",desktopMetadata="";
    private int guardianExitCode=-1;
    private volatile X11StartupLog startupLog=new X11StartupLog();
    private volatile X11StartupLog componentLog=new X11StartupLog();
    private volatile X11InstallErrors componentErrors=new X11InstallErrors();
    private int componentGuardianExit=-1;private String componentResult="not-started";
    private volatile String previousStartup="";
    private String focusedContext="";
    private boolean textFocus,desktopReady,desktopStartupTimedOut;
    private int startupStep;
    private long focusSerial,fieldTapSerial;
    private final Runnable fieldTap=()->{if(!stopping&&ready&&textFocus&&!focusedContext.isEmpty())fieldTapSerial++;};
    void pointerTapped(){main.removeCallbacks(fieldTap);main.postDelayed(fieldTap,200);}
    private boolean componentsReady,rendererComponents;
    private final java.util.Set<String> verifiedTargets=new java.util.HashSet<>();
    private volatile DesktopProcess child;
    private X11ProgramInstaller programs;
    private int installPercent=-1;private long installEta=-1,installSampleTime,installDownloaded=-1,installTotal=-1;private String installStage="preparing",installItem="";
    private volatile int generation;
    public final class LocalBinder extends Binder {
        X11Service service(){return X11Service.this;}
        @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
            if(code!=X11Remote.CALL)return super.onTransact(code,data,reply,flags);
            if(data.dataSize()>16384)throw new IllegalArgumentException("Desktop request too large");
            data.enforceInterface(X11Remote.DESCRIPTOR);
            if(Binder.getCallingUid()!=android.os.Process.myUid())throw new SecurityException("Private desktop only");
            Bundle request=data.readBundle(X11Service.class.getClassLoader());
            if(request==null||data.dataAvail()!=0)throw new IllegalArgumentException("Invalid desktop request");
            FutureTask<Bundle> task=new FutureTask<>(()->command(request));
            if(Looper.myLooper()==main.getLooper())task.run();else main.post(task);
            try{Bundle result=task.get(5,TimeUnit.SECONDS);reply.writeNoException();reply.writeBundle(result);}
            catch(Exception error){task.cancel(false);throw new RemoteException("Desktop request failed: "+safe(error));}
            return true;
        }
    }
    Bundle snapshot(){Bundle scaling=scaleJob.snapshot();if(scaling.getBoolean("success",false))interfaceScale=scaling.getInt("percent",interfaceScale);Bundle b=new Bundle();b.putBundle("screenScale",scaling);b.putString("backend","x11");b.putString("status",status);b.putString("target",target);b.putString("desktop",desktop);b.putString("renderer",renderer);b.putBoolean("termuxStyle",termuxStyle);b.putBoolean("preparingExtras",preparingExtras);b.putInt("interfaceScale",interfaceScale);b.putString("componentOperation",componentOperation);b.putString("componentResult",componentResult);b.putString("componentErrors",componentErrors.snapshot());b.putString("componentDiagnostic",componentLog.snapshot());b.putInt("componentChildExit",componentLog.childExit());b.putInt("componentGuardianExit",componentGuardianExit);b.putString("startupDiagnostic",startupLog.snapshot());b.putInt("desktopChildExit",startupLog.childExit());b.putInt("desktopGuardianExit",guardianExitCode);b.putLong("startupElapsedMs",startupLog.elapsedMs());b.putString("previousStartupDiagnostic",previousStartup);b.putBoolean("garconFilter",garconFilter);b.putString("desktopOwner",desktopOwner);b.putStringArrayList("desktopOwners",new java.util.ArrayList<>(desktopUsers));b.putString("pointerDiagnostic",Build.VERSION.SDK_INT<30||embedded==null?"unavailable":embedded.pointerDiagnostic());b.putString("pointerBaseline",pointerBaseline);b.putString("phase",phase);b.putBoolean("rendererConnected",Build.VERSION.SDK_INT>=30&&(embedded!=null&&embedded.rendererConnected()));b.putBoolean("hostAttached",Build.VERSION.SDK_INT>=30&&(embedded!=null&&embedded.hostAttached()));b.putString("detail",detail);b.putString("desktopMetadata",desktopMetadata);b.putBoolean("observationReady",!observationToken.isEmpty()&&ready&&!stopping&&phase.equals("running"));b.putBoolean("observationActive",observationActive);b.putBoolean("observationPending",observationPending);b.putString("observationError",observationError);b.putString("observationLog",observationLog);b.putString("currentBackdrop",currentBackdrop);b.putInt("observationSerial",observationSerial);b.putBoolean("components",componentsReady);b.putBoolean("rendererComponents",rendererComponents);b.putBoolean("ready",ready&&!stopping&&!retiring&&!phase.equals("stopping"));b.putBoolean("started",started&&!stopping&&!retiring&&!phase.equals("stopping"));b.putInt("installPercent",installPercent);b.putLong("downloadBytes",installDownloaded);b.putLong("downloadTotal",installTotal);b.putLong("installEta",installEta);b.putLong("installSampleTime",installSampleTime);b.putString("installStage",installStage);b.putString("installItem",installItem);if(programs!=null)b.putBundle("programs",programs.snapshot());b.putInt("startupStep",startupStep);b.putBoolean("desktopReady",desktopReady);b.putBoolean("desktopStartupTimedOut",desktopStartupTimedOut);b.putBoolean("textFocus",textFocus);b.putLong("focusSerial",focusSerial);b.putLong("fieldTapSerial",fieldTapSerial);return b;}
    private String renderer="cpu";
    private boolean termuxStyle;
    private int interfaceScale=100;
    private final X11ScaleJob scaleJob=new X11ScaleJob(this);
    private volatile String desktopToken="";
    private Bundle command(Bundle q){
        String op=q.getString("op","");
        if(ownProcess&&q.containsKey("target")&&!environment().target.equals(q.getString("target")))throw new IllegalArgumentException("Wrong X11 service for environment");
        if(op.equals("state"))return snapshot();
        // A native server has no restart API. Release its dedicated VM before any
        // new rootfs/operation can reuse the display or its control directory.
        if((retiring||stopping||phase.equals("stopping"))&&!op.equals("stop")&&!op.equals("session-close")&&!op.equals("detach"))throw new IllegalStateException("X11 is stopping; retry after it has stopped");
        if(phase.equals("cancelling")&&!op.equals("stop")&&!op.equals("cancel")&&!op.equals("session-close")&&!op.equals("detach"))throw new IllegalStateException("X11 installer is stopping; wait for cleanup");
        if(op.equals("session-close")){
            String owner=q.getString("owner","");
            if(started&&target.equals(q.getString("target",""))&&!stopping&&!phase.equals("stopping")){
                boolean matched=desktopUsers.remove(owner)||desktopUsers.isEmpty()&&!owner.isEmpty()&&desktopOwner.equals(owner);
                if(matched&&desktopUsers.isEmpty()){phase="stopping";main.postDelayed(this::requestStop,150);}
                else if(matched&&desktopOwner.equals(owner))desktopOwner=desktopUsers.iterator().next();
            }
            return snapshot();
        }
        if(op.equals("screen-scale")){
            int percent=q.getInt("interfaceScale",100);X11ScreenSettings.validateScale(percent);
            if(!target.equals(q.getString("target",""))||!desktop.equals("xfce")||!started||!ready||!desktopReady||!phase.equals("running")||runtime==null||desktopToken.isEmpty())throw new IllegalStateException("Wait for the X11 desktop to finish starting");
            scaleJob.start(runtime.base,target,desktopToken,percent,q.getString("request",""));return snapshot();
        }
        if(op.equals("observe-start")||op.equals("observe-stop"))throw new IllegalArgumentException("X11 diagnostic observer was retired");
        if(op.equals("programs-cancel")){if(programs!=null)programs.cancel();return snapshot();}
        if(op.equals("programs-page")){
            if(programs==null)throw new IllegalStateException("Load the catalog first");
            try{org.json.JSONObject page=new org.json.JSONObject(q.getString("program","{}"));Bundle result=snapshot();Bundle data=result.getBundle("programs");
                if(!q.getString("target","").equals(data.getString("target","")))throw new IllegalStateException("Catalog belongs to another environment");
                data.putBundle("catalogPage",programs.page(q.getString("target",""),page.optString("query",""),page.optInt("offset",0),page.optBoolean("installed",false)));return result;
            }catch(org.json.JSONException e){throw new IllegalArgumentException("Invalid catalog query");}
        }
        if(op.equals("programs-firefox-compat-on")||op.equals("programs-firefox-compat-off")||op.equals("programs-firefox-dns-on")||op.equals("programs-firefox-dns-off")||op.equals("programs-check")||op.equals("programs-install")||op.equals("programs-catalog")||op.equals("programs-catalog-install")||op.equals("programs-installed")||op.equals("programs-remove")||op.equals("programs-catalog-refresh")||op.equals("programs-installed-refresh")||op.equals("programs-check-refresh")){
            String wanted=q.getString("target",""),id=q.getString("program","");
            if(!DesktopProcess.supported(wanted))throw new IllegalArgumentException("Unsupported desktop target");
            if((started&&!target.equals(wanted))||phase.equals("preparing")||phase.equals("checking")||phase.equals("installing")||stopping)throw new IllegalStateException("Finish the current desktop operation first");
            if(programs==null)programs=new X11ProgramInstaller(this);
            if((op.equals("programs-check")||op.equals("programs-catalog"))&&programs.busy()&&programs.target().equals(wanted))return snapshot();
            String operation=op.substring(9);boolean refresh=operation.endsWith("-refresh");if(refresh)operation=operation.substring(0,operation.length()-8);
            programs.start(operation,wanted,id,refresh);return snapshot();
        }
        if(op.equals("check")||op.equals("install")||(op.equals("repair-launchers")||(op.equals("firefox-compat-on")||op.equals("firefox-compat-off")))||op.equals("launch")){
            String wanted=q.getString("target","");
            if((op.equals("repair-launchers")&&!DesktopPrograms.supportsMaintenance(wanted))||((op.equals("firefox-compat-on")||op.equals("firefox-compat-off"))&&!DesktopPrograms.supportsFirefox(wanted)))throw new IllegalArgumentException("Repair requires a supported desktop environment");
            if((op.equals("repair-launchers")||(op.equals("firefox-compat-on")||op.equals("firefox-compat-off")))&&(started||stopping))throw new IllegalStateException("Stop the desktop before maintenance");
            if(!DesktopProcess.supported(wanted))throw new IllegalArgumentException("Desktop not yet supported for this environment");
            boolean style=q.getBoolean("termuxStyle",false);
            int scale=q.getInt("interfaceScale",100);X11ScreenSettings.validateScale(scale);
            if(q.getBoolean("touchWorkspace",false))throw new IllegalArgumentException("Retired desktop style");
            if(style&&!DesktopProcess.supportsStyle(wanted))throw new IllegalArgumentException("Style requires a supported X11 desktop");
            if((style!=termuxStyle)&&(started||stopping||phase.equals("checking")||phase.equals("installing")||phase.equals("preparing")))throw new IllegalStateException("Stop the desktop before changing style");
            String renderChoice=q.getString("renderer","cpu");
            if(!renderChoice.equals("cpu")&&!renderChoice.equals("turnip")&&!renderChoice.equals("virgl")&&!renderChoice.equals("wrapper")&&!renderChoice.equals("bionic-glibc"))throw new IllegalArgumentException("Invalid renderer");
            if(renderChoice.equals("bionic-glibc")&&!wanted.equals("ubuntu")&&!wanted.equals("debian")&&!wanted.equals("fedora")&&!wanted.equals("archlinux")&&!wanted.equals("opensuse")&&!wanted.equals("void"))throw new IllegalArgumentException("Bionic for glibc requires a supported glibc desktop");
            if(renderChoice.equals("wrapper")&&!wanted.equals("termux"))throw new IllegalArgumentException("Wrapper is Termux-only");
            if(renderChoice.equals("virgl")&&!VirglInstaller.supported(wanted))throw new IllegalArgumentException("VirGL target unsupported");
            if(renderChoice.equals("turnip")&&!wanted.equals("ubuntu")&&!wanted.equals("termux")&&!wanted.equals("debian")&&!wanted.equals("alpine")&&!wanted.equals("fedora")&&!wanted.equals("opensuse")&&!wanted.equals("void")&&!wanted.equals("archlinux"))throw new IllegalArgumentException("GPU target unsupported");
            if(!renderer.equals(renderChoice)&&(started||stopping||phase.equals("checking")||phase.equals("installing")||phase.equals("preparing")))throw new IllegalStateException("Stop the desktop before changing renderer");
            String choice=q.getString("desktop","xfce");
            DesktopProfiles.validate(wanted,choice,renderChoice,style);
            if((!desktop.equals(choice))&&(started||stopping||phase.equals("checking")||phase.equals("installing")||phase.equals("preparing")))throw new IllegalStateException("Stop the desktop before changing it");
            if(!target.isEmpty()&&!target.equals(wanted)&&(started||phase.equals("checking")||phase.equals("installing")))throw new IllegalStateException("Stop the current desktop before changing environment");
            if(!target.equals(wanted)||!desktop.equals(choice))componentsReady=verifiedTargets.contains(wanted+":"+choice);
            if(programs!=null&&programs.busy())throw new IllegalStateException("Finish the program operation first");
            target=wanted;desktop=choice;renderer=renderChoice;termuxStyle=style;if(!started&&!stopping)interfaceScale=scale;garconFilter=false;
            if(op.equals("launch")&&phase.equals("preparing")){
                String owner=q.getString("owner","");if(owner.length()>160||!owner.matches("[A-Za-z0-9_:.-]*"))throw new IllegalArgumentException("Invalid desktop owner");if(!owner.isEmpty())desktopUsers.add(owner);return snapshot();
            }
            if(phase.equals("checking")||phase.equals("installing")||phase.equals("preparing"))return snapshot();
            if(op.equals("launch")){
                if(!componentsReady)throw new IllegalStateException("Check and install desktop components first");
                String owner=q.getString("owner","");if(owner.length()>160||!owner.matches("[A-Za-z0-9_:.-]*"))throw new IllegalArgumentException("Invalid desktop owner");if(!owner.isEmpty())desktopUsers.add(owner);if(!started){desktopOwner=owner;startupStep=0;previousStartup=startupLog.snapshot();startupLog=new X11StartupLog();startupLog.feed("JOBS_X11_STARTUP=prepare\n");started=true;phase="preparing";prepare();}
            }else if(!started){runComponents(op);}
        }else if(op.equals("cancel")){
            if(phase.equals("preparing")){phase="stopping";main.postDelayed(this::requestStop,150);return snapshot();}
            if(phase.equals("installing")||phase.equals("checking")){generation++;componentResult="cancelling";phase="cancelling";componentsReady=false;DesktopProcess owned=child;if(owned!=null)owned.close();if(componentOperation.equals("repair-launchers"))setStatus("Исправление отменяется. Исходные права сохранены в резервной записи.","Cancelling repair. Original permissions are saved in the backup record.");else setStatus("Установка отменяется. При повторной проверке может потребоваться восстановление пакетов.","Cancelling installation. A package repair may be needed before retrying.");}
        }else if(op.equals("stop")){
            if(!target.equals(q.getString("target","")))throw new IllegalStateException("Desktop belongs to another environment");
            if(!stopping&&!phase.equals("stopping")){phase="stopping";main.postDelayed(this::requestStop,150);}
        }
        else if(op.equals("attach")||op.equals("resize")||op.equals("detach")||op.equals("input")){
            if(op.equals("detach")&&!ready)return snapshot();
            if(Build.VERSION.SDK_INT<30)throw new IllegalStateException("Inline desktop requires Android 11");
            if(!ready)throw new IllegalStateException("X11 is still starting");

            if(embedded==null)embedded=new EmbeddedX11Host(this);
            if(op.equals("attach"))return embedded.attach(q);
            if(op.equals("resize"))embedded.resize(q);
            if(op.equals("detach"))embedded.detach(q.getString("attachment",""));
            if(op.equals("input"))embedded.input(q);
        }else throw new IllegalArgumentException("Unknown desktop request");
        return snapshot();
    }
    private void runComponents(String operation){
        componentOperation=operation;componentLog=new X11StartupLog();componentErrors=new X11InstallErrors();final X11InstallErrors transferErrors=componentErrors;componentGuardianExit=-1;componentResult="running";final X11StartupLog componentEvents=componentLog;installDownloaded=-1;installTotal=-1;installPercent=-1;installEta=-1;installStage="preparing";installItem="";
        X11InstallProgress progress=new X11InstallProgress();
        int epoch=++generation;componentsReady=false;rendererComponents=false;verifiedTargets.remove(target+":"+desktop);detail="";phase=operation.equals("check")?"checking":"installing";
        if(operation.startsWith("firefox-compat-"))setStatus("Настройка совместимости Firefox…","Configuring Firefox compatibility…");
        else if(operation.equals("repair-launchers"))setStatus("Исправление прав ярлыков…","Repairing launcher permissions…");
        else setStatus(operation.equals("install")?"Установка рабочего стола…":"Проверка рабочего стола…",operation.equals("install")?"Installing desktop…":"Checking desktop…");
        worker.execute(()->{
            DesktopProcess owned=null;StringBuilder result=new StringBuilder();Runnable completion=null;
            try{
                X11Runtime prepared=runtime==null?X11Runtime.prepare(this):runtime;runtime=prepared;
                if(epoch!=generation||stopping)return;
                owned=DesktopProcess.start(this,prepared.base,operation,target,null,desktop,false,renderer,termuxStyle);child=owned;
                if(epoch!=generation)owned.close();
                owned.read(chunk->{transferErrors.feed(chunk);componentEvents.feed(chunk);progress.feed(chunk,SystemClock.elapsedRealtime());int percent=progress.percent();long sampleTime=SystemClock.elapsedRealtime(),eta=progress.etaSeconds(sampleTime);String stage=progress.stage(),item=progress.currentItem();long downloaded=progress.downloadedBytes(),total=progress.totalBytes();synchronized(result){result.append(chunk);if(result.length()>8192)result.delete(0,result.length()-8192);String tail=result.toString();main.post(()->{if(epoch==generation){detail=tail;installPercent=percent;installEta=eta;installSampleTime=sampleTime;installStage=stage;installItem=item;installDownloaded=downloaded;installTotal=total;}});}});
                int exit=owned.waitFor();transferErrors.finish();componentEvents.feed("\n");boolean ok=(exit==0||exit==137)&&result.toString().contains("READY:desktop")&&result.toString().contains("DESKTOP_CHILD_EXIT=0");
                completion=()->{if(epoch!=generation)return;componentGuardianExit=exit;componentResult=ok?"success":"failed";componentsReady=ok;rendererComponents=ok&&result.toString().contains("READY:renderer:"+renderer+"\n");if(ok)verifiedTargets.add(target+":"+desktop);phase=ok?"installed":operation.equals("check")?"missing":"error";if(operation.startsWith("firefox-compat-"))setStatus(ok?(operation.endsWith("-on")?"Совместимость Firefox включена":"Совместимость Firefox отключена"):"Настройка Firefox не завершена. Подробности ниже.",ok?(operation.endsWith("-on")?"Firefox compatibility enabled":"Firefox compatibility disabled"):"Firefox setup incomplete. Details below.");else if(ok)setStatus("Рабочий стол установлен","Desktop installed");else if(operation.equals("install"))setStatus("Установка рабочего стола не завершена. Причина показана ниже; вернитесь в терминал и повторите после её устранения.","Desktop installation failed. See the cause below; return to the terminal and retry after resolving it.");else setStatus("Компоненты рабочего стола не готовы. Можно установить или повторить проверку.","Desktop components are not ready. Install them or check again.");};
            }catch(Exception e){transferErrors.finish();completion=()->{if(epoch==generation){componentResult="exception";componentEvents.feed("ERROR:COMPONENT_OPERATION_EXCEPTION\n");phase="error";setStatus("Ошибка проверки/установки: "+safe(e),"Check/install error: "+safe(e));}};}
            finally{
                if(owned!=null)owned.closeAndWait();child=null;
                final Runnable outcome=completion;
                main.post(()->{if(epoch!=generation){finishComponentCancellation(epoch);return;}if(outcome!=null)outcome.run();});
            }
        });
    }
    private void finishComponentCancellation(int epoch){
        if(!stopping&&phase.equals("cancelling")&&generation==epoch+1){componentResult="cancelled";phase="cancelled";setStatus("Операция остановлена. Можно повторить проверку или установку.","Operation stopped. You can check or install again.");}
    }
    private void prepare(){
        preparingExtras=termuxStyle&&!target.isEmpty();
        if(preparingExtras){installPercent=-1;installDownloaded=-1;installTotal=-1;installEta=-1;detail="";setStatus("Подготовка темы Kali…","Preparing Kali theme…");}
        worker.execute(()->{DesktopProcess owned=null;Runnable completion=null;try{
            X11Runtime prepared=runtime==null?X11Runtime.prepare(this):runtime;
            if(stopping||retiring)return;
            if(termuxStyle&&!target.isEmpty()){
                X11InstallProgress progress=new X11InstallProgress();StringBuilder result=new StringBuilder();
                owned=DesktopProcess.extras(this,prepared.base);child=owned;if(stopping||retiring)owned.close();
                owned.read(chunk->{long now=SystemClock.elapsedRealtime();progress.feed(chunk,now);int percent=progress.percent();long done=progress.downloadedBytes(),total=progress.totalBytes(),eta=progress.etaSeconds(now);result.append(chunk);if(result.length()>8192)result.delete(0,result.length()-8192);String tail=result.toString();main.post(()->{if(!stopping&&!retiring&&preparingExtras){detail=tail;installPercent=percent;installDownloaded=done;installTotal=total;installEta=eta;installSampleTime=now;}});});
                int exit=owned.waitFor();
                if(stopping||retiring)return;
                if((exit!=0&&exit!=137)||!result.toString().contains("READY:extras\n")||!result.toString().contains("DESKTOP_CHILD_EXIT=0"))throw new IOException("Kali theme preparation failed; see download log");
                owned.closeAndWait();owned=null;child=null;
            }
            completion=()->{if(!stopping&&!retiring&&!phase.equals("stopping")){preparingExtras=false;startServer(prepared);}};
        }catch(Exception e){completion=()->{if(!stopping&&!retiring&&!phase.equals("stopping")){preparingExtras=false;finishDesktop("error","Не удалось подготовить X11: "+safe(e),"X11 preparation failed: "+safe(e));}};}
        finally{if(owned!=null)owned.closeAndWait();child=null;if(completion!=null)main.post(completion);}});
    }

    @Override public void onCreate(){
        super.onCreate();ownProcess=environment().owns(processName(),getPackageName());
        if(!ownProcess)throw new IllegalStateException("Refusing to load X11 outside its private process");
        programs=new X11ProgramInstaller(this);programs.scope(environment().target);
    }
    static String processName(){
        if(Build.VERSION.SDK_INT>=28)return Application.getProcessName();
        try(FileInputStream in=new FileInputStream("/proc/self/cmdline")){byte[] bytes=new byte[256];int n=in.read(bytes),end=0;while(end<n&&bytes[end]!=0)end++;return new String(bytes,0,end,java.nio.charset.StandardCharsets.UTF_8);}catch(IOException e){return "";}
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&STOP.equals(intent.getAction())){requestStop();return START_NOT_STICKY;}
        foreground();
        if(intent!=null&&intent.getBooleanExtra("controlOnly",false))return START_NOT_STICKY;
        if(!started){started=true;setStatus("Подготовка встроенного X11…","Preparing embedded X11…");
            worker.execute(()->{try{X11Runtime prepared=X11Runtime.prepare(this);main.post(()->{if(!stopping&&!retiring&&!phase.equals("stopping"))startServer(prepared);});}catch(Exception e){main.post(()->{if(!stopping)finishDesktop("error","Не удалось подготовить X11: "+safe(e),"X11 preparation failed: "+safe(e));});}});
        }
        return START_NOT_STICKY;
    }
    private void foreground(){
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        String channel="workspace-x11";manager.createNotificationChannel(new NotificationChannel(channel,"X11",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,getPackageManager().getLaunchIntentForPackage(getPackageName()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,environment().display,new Intent(this,getClass()).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,channel).setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Termux Job’s · "+"X11"+" · "+environment().target).setContentText(tr("Проверочная графическая сессия","Graphical test session")).setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,tr("Остановить графический рабочий стол","Stop graphical desktop"),stop).build()).build();
        startForeground(NOTIFICATION+environment().display,notification);
    }
    private void startServer(X11Runtime prepared){
        runtime=prepared;startupStep=1;foreground();

        try{
            // Pinned upstream's discovery listener binds INADDR_ANY despite -nolisten tcp.
            // Reserve its exact tagged port with a NON-LISTENING loopback socket. No
            // REUSEADDR/REUSEPORT: upstream bind must fail; its documented fallback
            // notifies our direct in-process adapter. Keep this guard for the whole life.
            discoveryGuard=Os.socket(OsConstants.AF_INET,OsConstants.SOCK_STREAM,0);
            Os.bind(discoveryGuard,InetAddress.getByName("127.0.0.1"),environment().port);
            Os.setenv("TMPDIR",target.isEmpty()?runtime.tmp.getPath():TermuxConstants.TERMUX_PREFIX_DIR_PATH+"/tmp",true);Os.setenv("XKB_CONFIG_ROOT",runtime.keyboard.getPath(),true);
            Os.setenv("TERMUX_X11_XSTARTUP","",true);Os.unsetenv("TERMUX_X11_DEBUG");Os.unsetenv("LD_PRELOAD");
            X11Native.load();entry=new CmdEntryPoint(this::serverReady);
            if(!entry.start(runtime.arguments(renderer)))throw new IOException("Native server refused startup");
            setStatus("Запуск графического ядра…","Starting graphics core…");
        }catch(Throwable e){finishDesktop("error","Не удалось запустить X11: "+safe(e),"X11 startup failed: "+safe(e));}
    }
    private void serverReady(){
        if(stopping||retiring||phase.equals("stopping")||ready)return;ready=true;startupStep=2;startupLog.feed("JOBS_X11_STARTUP=x11-ready\n");phase="running";desktopReady=target.isEmpty();
        if(Build.VERSION.SDK_INT>=30&&embedded!=null)embedded.connect();
        if(!target.isEmpty()){setStatus("Рабочий стол X11 · "+target,"X11 desktop · "+target);runDesktop();return;}
        setStatus("X11 готов. Проверяем графику и ввод…","X11 ready. Testing graphics and input…");
        worker.execute(()->{
            try(LocalSocket socket=new LocalSocket()){
                testSocket=socket;socket.connect(new LocalSocketAddress(new File(runtime.tmp,".X11-unix/X"+environment().display).getPath(),LocalSocketAddress.Namespace.FILESYSTEM));socket.setSoTimeout(10000);
                X11Protocol client=new X11Protocol(socket.getInputStream(),socket.getOutputStream());client.setup(runtime.cookie);client.createWindow();socket.setSoTimeout(0);
                main.post(()->{if(!stopping)setStatus("Подключено · тестовое X11-окно","Connected · X11 test window");});
                while(!stopping){String event=client.event();if(event.equals("key"))keys++;else if(event.equals("click"))clicks++;else if(event.equals("pointer"))motions++;else continue;main.removeCallbacks(notify);main.postDelayed(notify,80);}
            }catch(Exception e){main.post(()->{if(!stopping)setStatus("Ошибка тестового X11-клиента: "+safe(e),"X11 test client failed: "+safe(e));});}finally{testSocket=null;}
        });
    }
    private void runDesktop(){
        worker.execute(()->{DesktopProcess owned=null;try{
            if(stopping||retiring||phase.equals("stopping"))return;
            guardianExitCode=-1;X11StartupLog startupEvents=startupLog;startupEvents.feed("JOBS_X11_STARTUP=desktop-process\n");
            String token=java.util.UUID.randomUUID().toString();desktopToken=token;X11MetadataLog metadata=new X11MetadataLog();
            X11FocusStream focus=new X11FocusStream(token,(context,in)->main.post(()->{if(!stopping&&!retiring)focusEvent(context,in);}),ok->main.post(()->{if(!stopping&&!retiring){desktopReady=ok;if(ok){startupStep=4;startupLog.feed("JOBS_X11_STARTUP=desktop-ready\n");}desktopStartupTimedOut=!ok;}}));
            owned=DesktopProcess.start(this,runtime.base,"run",target,token,desktop,garconFilter,renderer,termuxStyle,interfaceScale);child=owned;
            if(stopping||retiring)owned.close();
            owned.read(chunk->{startupEvents.feed(chunk);focus.feed(chunk);metadata.feed(chunk);String startup=metadata.snapshot();main.post(()->{if(!stopping&&!retiring){desktopMetadata=startup;if(metadata.windowManagerReady()&&startupStep<3)startupStep=3;detail=(detail+chunk);if(detail.length()>8192)detail=detail.substring(detail.length()-8192);}});});
            int exit=owned.waitFor();main.post(()->desktopEnded(exit));
        }catch(Exception e){main.post(()->{if(!stopping)finishDesktop("error","Ошибка рабочего стола: "+safe(e),"Desktop error: "+safe(e));});}finally{if(owned!=null)owned.closeAndWait();child=null;}});
    }
    private void desktopEnded(int guardianExit){
        // The guardian deliberately SIGKILLs its own remaining group (137). Its
        // child marker, when present, is the actual desktop exit status.
        guardianExitCode=guardianExit;
        int actual=startupLog.childExit();
        int exit=actual>=0?actual:guardianExit;
        if(exit==75){finishDesktop("ended","Предыдущий стол Arch ещё останавливается. Повторите запуск через несколько секунд.","The previous Arch desktop is still stopping. Retry in a few seconds.");return;}
 finishDesktop("ended","Рабочий стол завершён ("+exit+"). "+"X11"+" освобождается автоматически.","Desktop ended ("+exit+"). "+"X11"+" is being released automatically.");
    }

    private void finishDesktop(String endPhase,String ru,String en){
        if(stopping||retiring)return;
        startupLog.feed("JOBS_X11_STARTUP=ended\n");retiring=true;started=false;ready=false;desktopReady=false;desktopStartupTimedOut=false;
        textFocus=false;focusedContext="";desktopToken="";scaleJob.close();focusSerial++;
        observationActive=observationPending=false;observationToken="";
        phase=endPhase;setStatus(ru,en);
        // Leave one bounded polling interval to deliver the reason. Then discard
        // the native VM, even with no visible surface or bound desktop owner.
        main.postDelayed(this::requestStop,1200);
    }
    private void focusEvent(String context,boolean in){
        // Qt may expose an input context for desktop selection/non-edit widgets.
        if(in){focusedContext=context;textFocus=true;focusSerial++;}
        else if(focusedContext.equals(context)){
            focusedContext="";long serial=++focusSerial;
            main.postDelayed(()->{if(!stopping&&focusSerial==serial&&focusedContext.isEmpty()){textFocus=false;focusSerial++;}},150);
        }
    }
    private static String safe(Throwable e){String message=e.getMessage();return message==null?e.getClass().getSimpleName():message.substring(0,Math.min(240,message.length()));}
    private String tr(String ru,String en){return X11Text.tr(this,ru,en);}
    private void setStatus(String ru,String en){status=tr(ru,en);notify.run();}
    String status(){return status;}
    long keys(){return keys;}long clicks(){return clicks;}long motions(){return motions;}
    ParcelFileDescriptor displayConnection(){return ready&&entry!=null?entry.getXConnection():null;}
    void addListener(Runnable listener){listeners.add(listener);listener.run();}
    void removeListener(Runnable listener){listeners.remove(listener);}
    void requestStop(){
        if(!ownProcess||!environment().owns(processName(),getPackageName()))return;
        stopSelf();
        // Bound activities otherwise defer Service.onDestroy after stopSelf.
        shutdownOwnProcess();
    }
    @Override public IBinder onBind(Intent intent){return binder;}
    @Override public void onDestroy(){
        super.onDestroy();scaleJob.close();shutdownOwnProcess();
    }
    private void shutdownOwnProcess(){
        if(stopping)return;stopping=true;scaleJob.close();started=false;ready=false;desktopReady=false;textFocus=false;desktopOwner="";desktopUsers.clear();generation++;if(programs!=null)programs.close();DesktopProcess owned=child;if(owned!=null)owned.close();if(Build.VERSION.SDK_INT>=30&&embedded!=null)embedded.release();ready=false;listeners.clear();main.removeCallbacksAndMessages(null);worker.shutdownNow();
        LocalSocket socket=testSocket;if(socket!=null)try{socket.close();}catch(IOException ignored){}
        if(discoveryGuard!=null)try{Os.close(discoveryGuard);}catch(Exception ignored){}
        stopForeground(true);
        // Upstream has no restart/teardown API: terminate ONLY this dedicated VM.
        // Double-check exact process identity; no PID from a profile/user input.
        if(ownProcess&&environment().owns(processName(),getPackageName()))android.os.Process.killProcess(android.os.Process.myPid());
    }
}
