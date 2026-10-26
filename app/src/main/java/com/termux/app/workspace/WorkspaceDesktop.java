package com.termux.app.workspace;

import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.x11.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Main-process compositor/controller. Native X11 and desktop children stay in :jobs_x11. */
final class WorkspaceDesktop {
    private final TermuxActivity activity;
    private final Supplier<WorkspaceStore.Session> selected;
    private final BooleanSupplier visible;
    private final Runnable changed,restore;
    private final View input;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService calls=Executors.newSingleThreadExecutor();
    private final Set<String> graphical=new HashSet<>();
    private final Set<String> shownTargets=new HashSet<>();
    private final Map<String,Boolean> installed=new HashMap<>();
    private IBinder remote;
    private boolean bound,closed,polling,inFlight,launchCheckAcknowledged;
    private RemoteDesktopView display;
    private X11StartupView preparation;
    private WorkspaceStore.Session preparationProfile;
    private boolean preparationNeedsInstall,preparationCancelling,preparationLaunching;
    private AlertDialog preparationPrompt;
    private String preparationRenderer="",rendererProgressText="";
    private boolean rendererLaunchAfterInstall,preparationGpu,rendererPreparationFailed,rendererReadyForPreparation;
    private int rendererProgress=-1,rendererEpoch;
    private Runnable rendererCancel;
    private String displayed="",pending="",pendingProgram="";
    private WorkspaceStore.Session pendingProfile,progressOrigin,launchAfterCheck;
    private Bundle state=new Bundle();
    private Bundle diagnosticState=new Bundle();
    private long diagnosticAt;
    String sessionDiagnostic(WorkspaceStore.Session p){
        Bundle b=diagnosticState.isEmpty()?state:diagnosticState;
        StringBuilder text=new StringBuilder("X11"+" transport\nTarget: ").append(target(p)).append("\nDesktop profile: ").append(desktop(p)).append("\nSelected renderer: ").append(renderer(p));
        if(b.isEmpty())return text.append("\nSnapshot: unavailable").toString();
        text.append("\nRuntime renderer: ").append(b.getString("renderer","unknown")).append("\nPhase: ").append(b.getString("phase","unknown"));
        for(String k:new String[]{"started","ready","desktopReady","desktopStartupTimedOut","rendererConnected","hostAttached"})text.append('\n').append(k).append(": ").append(b.getBoolean(k,false));
        text.append("\nDesktop child exit: ").append(b.getInt("desktopChildExit",-1)).append("\nGuardian exit: ").append(b.getInt("desktopGuardianExit",-1)).append("\nExit -1: not observed; guardian 137 alone does not identify the cause");
        text.append("\nComponent operation: ").append(b.getString("componentOperation","unknown")).append("\nComponent result: ").append(b.getString("componentResult","not-started"));
        text.append("\nComponent stage: ").append(b.getString("installStage","unknown"));
        text.append("\nComponent child exit: ").append(b.getInt("componentChildExit",-1)).append("\nComponent guardian exit: ").append(b.getInt("componentGuardianExit",-1));
        text.append("\nComponent categories (raw output omitted):\n").append(b.getString("componentDiagnostic","unavailable"));
        text.append("\nStartup elapsed ms: ").append(b.getLong("startupElapsedMs",0));
        text.append("\nStartup step: ").append(b.getInt("startupStep",0)).append("\nSnapshot age ms: ").append(Math.max(0,SystemClock.elapsedRealtime()-diagnosticAt));
        text.append("\nStartup categories (raw output omitted):\n").append(b.getString("startupDiagnostic","unavailable"));
        String failures=b.getString("componentErrors","");if(!failures.isEmpty())text.append("\nRedacted installer transfer errors (review before sharing):\n").append(failures);
        String previous=b.getString("previousStartupDiagnostic","");if(!previous.isEmpty())text.append("\nPrevious startup categories:\n").append(previous);
        return text.toString();
    }
    void refreshDiagnostic(Runnable done){
        IBinder binder=remote;if(closed||binder==null){done.run();return;}
        calls.execute(()->{Bundle answer=null;try{answer=X11Remote.call(binder,X11Remote.command("state"));}catch(Exception ignored){}Bundle result=answer;main.post(()->{if(!closed&&remote==binder&&result!=null){diagnosticState=result;diagnosticAt=SystemClock.elapsedRealtime();}done.run();});});
    }
    private TextView rendererRow,wrapperFolder,wrapperDriver;
    private CheckBox styleCheck;
    private static boolean styleSupported(WorkspaceStore.Session p){return com.termux.x11.DesktopPrograms.supportsStyle(target(p));}
    private static String styleKey(WorkspaceStore.Session p){return target(p).equals("termux")?"termux-style":"termux-style:"+target(p);}
    private boolean styleEnabled(WorkspaceStore.Session p){return styleSupported(p)&&activity.getSharedPreferences("workspace-x11",0).getBoolean(styleKey(p),false);}
    private void migrateRetiredStyle(){
        android.content.SharedPreferences prefs=activity.getSharedPreferences("workspace-x11",0);
        if(prefs.contains("touch-workspace")){
            android.content.SharedPreferences.Editor update=prefs.edit();
            if(prefs.getBoolean("touch-workspace",false))update.putBoolean("termux-style",true);
            update.remove("touch-workspace").apply();
        }
    }
    private final WorkspaceDesktopWrapper wrapper;
    private boolean rendererBusy(){return wrapper.busy()||preparationGpu||(!preparationRenderer.isEmpty()&&!rendererPreparationFailed)||rendererSettingsBusy();}

    private boolean rendererSettingsBusy(){return state.getBoolean("started",false)||Arrays.asList("checking","installing","cancelling","preparing","stopping").contains(state.getString("phase",""))||programs.busy();}
    private boolean rendererBackendBusy(){return remote==null||rendererSettingsBusy();}
    private boolean styleBusy(){return remote==null||rendererBusy();}
    private DesktopInstallUi progress;
    private final WorkspaceDesktopPrograms programs;
    private final DesktopKeyboardState keyboardState=new DesktopKeyboardState();
    private final DesktopFullscreen fullscreen;
    private final DesktopExtraKeys extraKeys;
    private Runnable keyboardChanged=()->{};
    private final ViewTreeObserver.OnPreDrawListener imeObserver=this::observeIme;
    private boolean observeIme(){
        if(!closed&&Build.VERSION.SDK_INT>=30&&hasSurface()){
            if(keyboardState.visibility(display.imeVisible())){display.keyboard(false);keyboardChanged.run();}
        }
        return true;
    }
    void onKeyboardChanged(Runnable callback){keyboardChanged=callback;}
    boolean keyboardEnabled(){return keyboardState.enabled;}

    private final ServiceConnection connection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName n,IBinder binder){if(closed)return;remote=binder;String op=pending;pending="";if(!op.isEmpty())send(op,pendingProfile,pendingProgram);pendingProfile=null;pendingProgram="";poll();sync();}
        @Override public void onServiceDisconnected(ComponentName n){if(closed)return;if(bound){activity.unbindService(this);bound=false;}remote=null;launchAfterCheck=null;clearPreparation();state=new Bundle();installed.clear();graphical.clear();shownTargets.clear();dismissProgress();programs.failed();sync();changed.run();activity.showToast(tr("X11 остановлен. Терминальная сессия продолжает работу.","X11 stopped. The terminal session is still running."),true);}
        @Override public void onBindingDied(ComponentName n){onServiceDisconnected(n);if(bound){activity.unbindService(this);bound=false;}}
    };
    WorkspaceDesktop(TermuxActivity a,Supplier<WorkspaceStore.Session> s,BooleanSupplier v,View input,Runnable changed,Runnable restore){activity=a;selected=s;visible=v;this.input=input;this.changed=changed;this.restore=restore;extraKeys=new DesktopExtraKeys(a,()->keyboardChanged.run());fullscreen=new DesktopFullscreen(a,this::keyboard,this::keyboardEnabled,this::observeIme,this::extraKeys,this::extraKeysVisible);migrateRetiredStyle();wrapper=new WorkspaceDesktopWrapper(a,WorkspaceDesktopWrapper.bridgeSupported(target(s.get()))?target(s.get()):"termux");programs=new WorkspaceDesktopPrograms(a,(op,id)->ensure(op,programsProfile(),id),()->{},this::maintenance);if(Build.VERSION.SDK_INT>=30)activity.getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(imeObserver);}
    private WorkspaceStore.Session programsProfile(){return programs.profile();}
    static boolean eligible(WorkspaceStore.Session p){
        if(p==null||p.environment.equals("codex")||!p.aiAgent.isEmpty()||p.claudeAgent||p.geminiAgent)return false;
        return p.environment.equals("termux")||p.environment.equals("ubuntu")||p.environment.equals("legacy")||p.environment.startsWith("proot:");
    }
    static String target(WorkspaceStore.Session p){if(!eligible(p))return "";if(!p.prootAlias.isEmpty())return p.prootAlias;return p.environment.startsWith("proot:")?p.environment.substring(6):p.environment.equals("ubuntu")?"ubuntu":"termux";}
    private boolean supported(WorkspaceStore.Session p){return DesktopPrograms.supportsTarget(target(p));}
    private String desktop(WorkspaceStore.Session p){return "xfce";}
    private String renderer(WorkspaceStore.Session p){return WorkspaceDesktopGpu.mode(activity,target(p));}


    String label(){return showing()?tr("Рабочий стол","Desktop"):tr("Терминал","Terminal");}
    boolean hasSurface(){return display!=null&&showing();}
    boolean showing(){WorkspaceStore.Session p=selected.get();return visible.getAsBoolean()&&p!=null&&graphical.contains(p.id)&&eligible(p);}
    void menu(){
        WorkspaceStore.Session p=selected.get();if(!eligible(p))return;
        if(!supported(p)){WorkspaceDialogs.builder(activity).setTitle("X11 · "+WorkspaceEnvironments.label(target(p))).setMessage(tr("Рабочий стол доступен для Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE, Void Linux и Arch Linux. Другие системы будут подключаться к своим рабочим столам, без перенаправления в Ubuntu.","The desktop is available for Termux, Ubuntu, Debian, Alpine, Fedora, openSUSE, Void Linux and Arch Linux. Other systems will use their own desktops, without redirecting to Ubuntu.")).setPositiveButton("OK",null).show();return;}
        if(remote==null)ensure("state",p);
        List<String> items=new ArrayList<>();List<Integer> actions=new ArrayList<>();List<String> rowKeys=new ArrayList<>();
        {items.add(showing()?tr("Вернуться в терминал","Return to terminal"):tr("Переключиться на рабочий стол","Switch to desktop"));actions.add(0);rowKeys.add("desktop-switch");}
        if(target(p).equals(state.getString("target",""))&&state.getBoolean("started",false)){items.add(tr("Остановить графический рабочий стол","Stop graphical desktop"));actions.add(1);rowKeys.add("desktop-stop");}
        LinearLayout body=AgentStatusUi.body(activity);
        String phase=state.getString("phase","");
        if(target(p).equals(state.getString("target",""))){
            if(phase.equals("missing")||phase.equals("error")||phase.equals("cancelled"))AgentStatusUi.info(body,state.getString("status",""));
        }
        AlertDialog[] dialog=new AlertDialog[1];

        styleCheck=null;rendererRow=null;wrapperFolder=null;wrapperDriver=null;
        for(int i=0;i<items.size();i++){
            final int which=actions.get(i);
            TextView row=AgentStatusUi.row(body,items.get(i),rowKeys.get(i),()->{dialog[0].dismiss();menuAction(p,which);});
            if(i==0&&styleSupported(p)){
                LinearLayout styleRow=new LinearLayout(activity);styleRow.setGravity(Gravity.CENTER_VERTICAL);
                TextView label=new TextView(activity);label.setText("Стиль Termux-job's");if(!WorkspaceLanguage.code(activity).equals("ru"))label.setText("Termux-job's style");label.setTextColor(WorkspaceDialogs.TEXT);label.setTextSize(13);styleRow.addView(label,new LinearLayout.LayoutParams(0,-2,1));
                CheckBox check=new CheckBox(activity);styleCheck=check;check.setTag("desktop-termux-style");check.setContentDescription(label.getText());check.setChecked(styleEnabled(p));check.setEnabled(!styleBusy());styleRow.addView(check);body.addView(styleRow);
                check.setOnCheckedChangeListener((button,value)->{if(styleBusy()){check.setChecked(styleEnabled(p));return;}activity.getSharedPreferences("workspace-x11",0).edit().putBoolean(styleKey(p),value).apply();});label.setOnClickListener(v->{if(check.isEnabled())check.performClick();});
                AgentStatusUi.info(body,tr("Kali-стиль: тёмные окна, синие акценты, крупные значки и фон с драконом. Применяется при следующем запуске. Для смены сначала остановите рабочий стол.","Kali-inspired style: dark windows, blue accents, large icons and dragon wallpaper. Applied on next launch. Stop the desktop before changing this setting."));

            }


        }
        addX11Renderer(body,p,dialog);addX11Screen(body,p,dialog);
        AgentStatusUi.info(body,tr("Поверните телефон горизонтально: рабочий стол займёт весь экран. Поворот обратно или «Назад» вернёт его в окно приложения. Кнопка интерактивного ввода | доступна справа сверху.","Rotate the phone to landscape for a full-screen desktop. Rotate back or press Back to return it to the app window. The Interactive input | button remains at the top right."));
        LinearLayout keyboardRow=new LinearLayout(activity);keyboardRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView keyboardLabel=new TextView(activity);keyboardLabel.setText(tr("Автоматическая клавиатура","Automatic keyboard"));keyboardLabel.setTextColor(WorkspaceDialogs.TEXT);keyboardLabel.setTextSize(13);
        keyboardRow.addView(keyboardLabel,new LinearLayout.LayoutParams(0,-2,1));
        CheckBox automatic=new CheckBox(activity);automatic.setTag("desktop-automatic-keyboard");automatic.setContentDescription(keyboardLabel.getText());automatic.setChecked(automaticKeyboard(p));keyboardRow.addView(automatic);body.addView(keyboardRow);
        automatic.setOnCheckedChangeListener((button,checked)->{activity.getSharedPreferences("workspace-x11",0).edit().putBoolean("automatic-keyboard:"+target(p),checked).apply();keyboardState.automatic=checked;});
        keyboardLabel.setOnClickListener(v->automatic.performClick());
        TextView keyboardHelp=AgentStatusUi.info(body,tr("Показывать клавиатуру автоматически при выборе поля ввода, где это поддерживается. Если выключено — только вручную через «Интерактивный ввод» | в верхней панели. Ручной вызов доступен всегда.","Show the keyboard automatically when a supported text field is selected. When disabled, open it manually using Interactive input | in the top toolbar. Manual input is always available."));
        keyboardHelp.setTag("desktop-automatic-keyboard-help");
        android.text.SpannableString help=new android.text.SpannableString(keyboardHelp.getText());int inputIcon=help.toString().indexOf('|');
        android.graphics.drawable.Drawable inputGlyph=activity.getDrawable(R.drawable.workspace_interactive_input).mutate();inputGlyph.setTint(WorkspaceDialogs.MUTED);int glyphSize=WorkspaceDialogs.dp(activity,16);inputGlyph.setBounds(0,0,glyphSize,glyphSize);
        if(inputIcon>=0)help.setSpan(new android.text.style.ImageSpan(inputGlyph,android.text.style.ImageSpan.ALIGN_BASELINE),inputIcon,inputIcon+1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);keyboardHelp.setText(help);
        dialog[0]=WorkspaceDialogs.builder(activity).setTitle("X11 · "+WorkspaceEnvironments.label(target(p))).setView(AgentStatusUi.scroll(body)).setNegativeButton(tr("Назад","Back"),null).create();
        dialog[0].show();
    }
    private void addX11Screen(LinearLayout body,WorkspaceStore.Session p,AlertDialog[] dialog){
        AgentStatusUi.row(body,WorkspaceDesktopScreen.label(activity,target(p)),"desktop-screen",()->{
            dialog[0].dismiss();WorkspaceDesktopScreen.show(activity,target(p),()->{sync();if(!closed&&sameSession(p,selected.get()))menu();},()->{if(!closed&&sameSession(p,selected.get()))menu();},(settings,done)->applyScreen(p,settings,done));
        });
    }
    private void applyScreen(WorkspaceStore.Session p,X11ScreenSettings settings,Consumer<String> done){
        IBinder binder=remote;
        if(binder==null){done.accept(null);return;}
        String request=UUID.randomUUID().toString();long deadline=SystemClock.elapsedRealtime()+60000;
        screenApply(binder,p,settings,request,deadline,true,done);
    }
    private void screenApply(IBinder binder,WorkspaceStore.Session p,X11ScreenSettings settings,String request,long deadline,boolean first,Consumer<String> done){
        if(closed||binder!=remote){done.accept(tr("Служба X11 отключилась. Повторите применение.","X11 disconnected. Apply again."));return;}
        calls.execute(()->{
            Bundle answer=null;boolean failed=false;
            try{
                Bundle current=X11Remote.call(binder,X11Remote.command("state"));
                if(first&&(!current.getBoolean("started",false)||settings.scalePercent==current.getInt("interfaceScale",100))){Bundle result=new Bundle();result.putBoolean("noScaleNeeded",true);answer=result;}
                else if(first){Bundle q=X11Remote.command("screen-scale");q.putString("target",target(p));q.putInt("interfaceScale",settings.scalePercent);q.putString("request",request);answer=X11Remote.call(binder,q);}
                else answer=current;
            }catch(Exception ignored){failed=true;}
            Bundle result=answer;boolean error=failed;main.post(()->{
                if(closed||binder!=remote||error||result==null){done.accept(tr("Не удалось применить масштаб. Дождитесь запуска X11 и повторите.","Could not apply scale. Wait for X11 startup and retry."));return;}
                if(result.getBoolean("noScaleNeeded",false)){done.accept(null);return;}
                Bundle scale=result.getBundle("screenScale");
                if(scale==null||!request.equals(scale.getString("request",""))){done.accept(tr("Запрос масштаба не подтверждён. Повторите.","Scale request was not acknowledged. Retry."));return;}
                if(!scale.getBoolean("pending",false)){done.accept(scale.getBoolean("success",false)?null:tr("Масштаб не применён. Текущие настройки сохранены; повторите после запуска стола.","Scale was not applied. Current settings are preserved; retry after desktop startup."));return;}
                if(SystemClock.elapsedRealtime()>=deadline){done.accept(tr("Применение заняло слишком много времени. Проверьте состояние X11 и повторите.","Applying took too long. Check X11 and retry."));return;}
                main.postDelayed(()->screenApply(binder,p,settings,request,deadline,false,done),250);
            });
        });
    }
    private void addX11Renderer(LinearLayout body,WorkspaceStore.Session p,AlertDialog[] dialog){
        rendererRow=AgentStatusUi.row(body,tr("Рендеринг: ","Rendering: ")+WorkspaceDesktopGpu.label(activity,target(p)),"desktop-renderer",()->{if(rendererBusy())return;dialog[0].dismiss();WorkspaceDesktopGpu.select(activity,target(p),this::rendererBusy,choice->selectRenderer(p,choice),this::menu);});
        if(WorkspaceDesktopGpu.mode(activity,target(p)).equals("turnip"))AgentStatusUi.info(body,target(p).equals("termux")?"Termux package build. Turnip: Mesa 26.2.5; Zink uses the installed Termux Mesa.":"Mesa 26.2.5 built for Termux Job’s with lfdevs Android adaptations.");
        if(target(p).equals("termux")&&WorkspaceDesktopGpu.mode(activity,"termux").equals("wrapper")||WorkspaceDesktopWrapper.bridgeSupported(target(p))&&WorkspaceDesktopGpu.mode(activity,target(p)).equals("bionic-glibc")){
            body.removeView(rendererRow);LinearLayout renderLine=new LinearLayout(activity);renderLine.setGravity(Gravity.CENTER_VERTICAL);
            renderLine.addView(rendererRow,new LinearLayout.LayoutParams(0,-2,1));
            wrapperFolder=WorkspaceDialogs.action(activity,"",0);wrapperFolder.setTag("desktop-wrapper-folder");wrapperFolder.setCompoundDrawablesWithIntrinsicBounds(R.drawable.workspace_folder,0,0,0);
            wrapperFolder.setGravity(Gravity.CENTER);wrapperFolder.setMinHeight(WorkspaceDialogs.dp(activity,48));wrapperFolder.setContentDescription(tr("Выбрать ZIP драйвера Bionic","Choose Bionic driver ZIP"));
            LinearLayout.LayoutParams folderParams=new LinearLayout.LayoutParams(WorkspaceDialogs.dp(activity,52),WorkspaceDialogs.dp(activity,48));folderParams.leftMargin=WorkspaceDialogs.dp(activity,6);renderLine.addView(wrapperFolder,folderParams);body.addView(renderLine);
            wrapperFolder.setOnClickListener(v->{if(rendererBusy())return;dialog[0].dismiss();wrapper.choose(()->closed||rendererBusy()||!sameSession(p,selected.get()),()->{if(!closed&&sameSession(p,selected.get())&&visible.getAsBoolean())menu();});});
            AgentStatusUi.available(wrapperFolder,!rendererBusy());
            AgentStatusUi.info(body,"Bionic Turnip or Qualcomm drivers (AdrenoTools ZIP format).");
            String driver=WorkspaceDesktopWrapper.driverLabel(activity,target(p));
            wrapperDriver=AgentStatusUi.row(body,driver.isEmpty()?tr("Выбрать драйвер Bionic","Choose Bionic driver"):driver,"desktop-wrapper-driver",()->{
                if(rendererBusy())return;dialog[0].dismiss();wrapper.showDrivers(()->closed||rendererBusy()||!sameSession(p,selected.get()),()->{if(!closed&&sameSession(p,selected.get())&&visible.getAsBoolean())menu();});
            });AgentStatusUi.available(wrapperDriver,!rendererBusy());
        }
        AgentStatusUi.available(rendererRow,!rendererBusy());
    }
    private void maintenance(String operation){
        WorkspaceStore.Session p=programs.profile();
        if(p==null||!DesktopPrograms.supportsTarget(target(p)))return;
        if((operation.equals("repair-launchers")&&state.getBoolean("started",false))||programs.busy()||Arrays.asList("checking","installing","cancelling","preparing","stopping").contains(state.getString("phase",""))){activity.showToast(tr("Сначала остановите рабочий стол и дождитесь окончания операции.","Stop the desktop and finish the current operation first."),true);return;}
        if(operation.equals("firefox-dns-enable")||operation.equals("firefox-dns-disable")){
            if(!DesktopPrograms.supportsFirefox(target(p)))return;
            boolean enable=operation.endsWith("-enable");
            AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(tr("Firefox · DNS через HTTPS","Firefox · DNS over HTTPS"))
                .setMessage(tr("Закройте все окна Firefox. Настройка действует только на его профили в этом окружении. Запросы имён сайтов будут отправляться Cloudflare по HTTPS (mozilla.cloudflare-dns.com); при недоступности используется обычный DNS. Системный DNS и проверка сертификатов не меняются. Прежние user.js сохраняются для отключения; изменённые вручную файлы не перезаписываются. Сначала хотя бы раз запустите Firefox, чтобы был создан профиль.","Close all Firefox windows. This affects only its profiles in this environment. Site-name queries go to Cloudflare over HTTPS (mozilla.cloudflare-dns.com), with ordinary DNS fallback. System DNS and certificate checks stay unchanged. Original user.js files are saved for disabling; local edits are preserved. Start Firefox once first to create its profile."))
                .setNegativeButton(tr("Назад","Back"),null).setPositiveButton(enable?tr("Включить","Enable"):tr("Отключить","Disable"),(d,w)->programs.setDns(enable)).create();confirm.setCanceledOnTouchOutside(false);confirm.show();return;
        }
        if(operation.equals("firefox-compat-enable")||operation.equals("firefox-compat-disable")){
            boolean enable=operation.endsWith("-enable");
            AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(tr("Firefox · Совместимость с PRoot","Firefox · PRoot compatibility"))
                .setMessage(tr("Сначала установите Firefox и закройте его окна. Режим устраняет проверенную ошибку общей памяти в Android/PRoot, но ослабляет защиту обмена памятью между процессами Firefox (seals). Песочница целиком не отключается. Изменение действует только для Firefox, запускаемого из меню рабочего стола. Рабочий стол останавливать не требуется. Можно отключить здесь же; собственные изменения ярлыка не перезаписываются.","Install Firefox and close its windows first. This mode works around the confirmed Android/PRoot shared-memory error, but weakens Firefox inter-process shared-memory protection (seals). It does not disable the entire sandbox. It applies only to Firefox launched from the desktop menu. The desktop can keep running. Disable it here; local launcher edits are never overwritten."))
                .setNegativeButton(tr("Назад","Back"),null).setPositiveButton(enable?tr("Включить","Enable"):tr("Отключить","Disable"),(d,w)->programs.setCompatibility(enable)).create();confirm.setCanceledOnTouchOutside(false);confirm.show();return;
        }
        if(!operation.equals("repair-launchers"))return;
        AlertDialog confirm=WorkspaceDialogs.builder(activity).setTitle(tr("Исправить права ярлыков?","Repair launcher permissions?"))
            .setMessage(tr("В выбранном окружении файлам .desktop, совпавшим с контрольными суммами установленных пакетов, будет добавлено чтение и запись для владельца. Исходные права сохраняются в ~/.local/state/termux-jobs/launcher-permissions.json.","Add owner read/write permission to .desktop files matching installed package checksums in the selected environment. Original permissions are saved in ~/.local/state/termux-jobs/launcher-permissions.json."))
            .setNegativeButton(tr("Назад","Back"),null).setPositiveButton(tr("Исправить","Repair"),(d,w)->startMaintenance(operation,p)).create();confirm.setCanceledOnTouchOutside(false);confirm.show();
    }
    private void startMaintenance(String operation,WorkspaceStore.Session p){programs.close();maintenanceReturn=true;ensure(operation,p);}

    private boolean maintenanceReturn;
    private static String owner(WorkspaceStore.Session p){return p==null||p.handle.isEmpty()?"":p.id+":"+p.handle;}
    boolean owns(WorkspaceStore.Session p){java.util.ArrayList<String> owners=state.getStringArrayList("desktopOwners");return p!=null&&!owner(p).isEmpty()&&(owners==null||owners.isEmpty()?owner(p).equals(state.getString("desktopOwner","")):owners.size()==1&&owners.contains(owner(p)))&&target(p).equals(state.getString("target",""))&&state.getBoolean("started",false);}
    void sessionClosed(WorkspaceStore.Session p){
        if(p==null)return;if(launchAfterCheck!=null&&p.id.equals(launchAfterCheck.id)&&p.handle.equals(launchAfterCheck.handle))launchAfterCheck=null;if(sameSession(p,preparationProfile)){clearPreparation();}graphical.remove(p.id);sync();
        if(pending.equals("launch")&&owner(p).equals(owner(pendingProfile))){pending="";pendingProfile=null;pendingProgram="";}
        // The service checks owner+handle+environment; a delayed close cannot stop another desktop.
        // Send even before launch acknowledgement so queued launch/close retain executor ordering.
        if(remote!=null&&!owner(p).isEmpty()&&eligible(p))send("session-close",p);
    }
    private String diagnosticText(){
        return "Termux Job’s X11 · "+com.termux.BuildConfig.VERSION_NAME+" / "+com.termux.BuildConfig.VERSION_CODE+"\nTarget="+state.getString("target","")+"\nDesktop="+state.getString("desktop","xfce")+"\nGarconFilter="+state.getBoolean("garconFilter",true)+"\n\n"+tr("Запись X11 запускается и останавливается кнопкой в диагностике, без ограничения тремя минутами. popup-new/destroy — впервые замеченные и уничтоженные всплывающие окна; popup-map/hide — показ и скрытие прежних; focus-change — смена X11-фокуса. Popup также может быть подсказкой. content-update считает обновления содержимого всплывающих окон через XDamage; pointer-position-change — изменения положения указателя между опросами. GTK file-events — уведомления GIO о файлах; changed/done/attribute/created/deleted показывают тип, menu/desktop/directory/other — категорию; garcon-tagged — мониторы с меткой Garcon (не все его мониторы имеют метку). last-file-id — хэш пути без его раскрытия. Эти события не подавляются. GTK draw — рисование меню; add/remove — добавление и удаление пунктов; reload — запрос перезагрузки Garcon; enter/leave/motion — события указателя. Начальное подключение XDamage тоже создаёт событие. Если damage=unavailable, обновления содержимого не измеряются. BACKDROP показывает рабочую область, доступность декодирования фона и применение запасного фона. Текст ввода, заголовки окон и изображения наблюдатель не записывает.","Start and stop X11 recording using the diagnostic button, without a three-minute expiry. popup-new/destroy: first-seen/destroyed popup windows; popup-map/hide: showing/hiding existing ones; focus-change: X11 focus change. Popups can also be tooltips. content-update counts popup content changes through XDamage; pointer-position-change counts position changes between samples. GTK file-events: GIO file notifications, split by event and category. garcon-tagged: monitors carrying Garcon metadata (not all Garcon monitors do). last-file-id: path hash without disclosure. Events are not suppressed. GTK draw: menu drawing; add/remove: menu item insertion/removal; reload: Garcon reload request; enter/leave/motion: pointer events. Initial XDamage attachment also emits a notification. If damage=unavailable, content changes are not measured. BACKDROP shows workspace, image decoder status and fallback application. The observer records no input text, window titles or images.")
            +"\n\nPointer adapter start: "+state.getString("pointerBaseline", "unavailable")+"\nPointer adapter now: "+state.getString("pointerDiagnostic", "unavailable")+"\nRecording="+state.getBoolean("observationActive",false)+" pending="+state.getBoolean("observationPending",false)+" serial="+state.getInt("observationSerial",0)+" "+state.getString("observationError","")+"\n\nLAST OBSERVED BACKDROP\n"+state.getString("currentBackdrop","")+"\nCAPTURE\n"+state.getString("observationLog","")+"\nSTARTUP SNAPSHOT\n"+state.getString("desktopMetadata","")+"\n"+state.getString("detail","")+"\n\n"+tr("Текстовый контекст Fcitx: ","Fcitx text context: ")+(state.getBoolean("textFocus",false)?tr("обнаружен","detected"):tr("не обнаружен","not detected"))+" · "+state.getLong("focusSerial",0)+"\nXFCE ready="+state.getBoolean("desktopReady",false)+(Build.VERSION.SDK_INT<30||display==null?"":"\nIME "+display.keyboardDiagnostic());
    }
    private void diagnostics(){
        DiagnosticsPanel.show(activity,new DiagnosticsPanel.Source(){
            public String title(){return tr("Диагностика X11","X11 diagnostics");}
            public String summary(){return "X11 · "+com.termux.BuildConfig.VERSION_NAME+"\n"+(state.getBoolean("observationPending",false)?tr("Изменение записи…","Changing recording…"):state.getBoolean("observationActive",false)?tr("Наблюдение включено","Observation active"):tr("Наблюдение остановлено","Observation stopped"))+"\n"+state.getString("observationError","")+"\n"+tr("Окружение: ","Environment: ")+state.getString("target","")+"\n"+tr("Графический стол: ","Desktop: ")+(state.getBoolean("started",false)?tr("запущен","started"):tr("не запущен","not started"))+" · "+state.getString("desktop","xfce")+" ready="+state.getBoolean("desktopReady",false)+"\nRenderer="+state.getBoolean("rendererConnected",false)+" · Android host="+state.getBoolean("hostAttached",false)+"\n"+tr("Текстовый контекст: ","Text context: ")+(state.getBoolean("textFocus",false)?tr("обнаружен","detected"):tr("не обнаружен","not detected"));}
            public String events(){String[] lines=(state.getString("detail","")+"\n"+state.getString("observationLog","")).split("\n");StringBuilder result=new StringBuilder();for(int i=lines.length-1;i>=0;i--)if(!lines[i].isEmpty())result.append(lines[i]).append('\n');return result.toString();}
            public String report(){return diagnosticText();}
            public String id(){return "x11-desktop";}
            public int bytes(){return (state.getString("detail","")+state.getString("observationLog","")).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;}
            public int dropped(){return 0;}
            public int capacity(){return 40960;}
            public String capacitySummary(){return tr("Последний вывод X11; старые строки заменяются новыми","Recent X11 output; old lines are replaced");}
            public boolean paused(){return !state.getBoolean("observationActive",false);}
            public boolean canPause(){return state.getBoolean("observationReady",false)&&!state.getBoolean("observationPending",false);}
            public String pauseLabel(){return state.getBoolean("observationPending",false)?tr("Подождите…","Please wait…"):state.getBoolean("observationActive",false)?tr("Остановить наблюдение","Stop observation"):tr("Начать наблюдение","Start observation");}
            public void paused(boolean value){ensure(value?"observe-stop":"observe-start",selected.get());}
            public void prepareExport(Runnable action){activity.requestWorkspaceStorageAccess(action);}
            public String privacy(){return tr("Новая запись заменяет предыдущую в памяти. Запись продолжается до остановки; закрытие окна её не прерывает. Полный отчёт включает состояние экрана и клавиатуры. Вывод программ может содержать пути. Отправка в сеть не выполняется.","A new capture replaces the previous in-memory capture. Recording continues until stopped; closing this dialog does not stop it. Full report includes display and keyboard state. Program output may contain paths. No network upload.");}
        });
    }
    private void menuAction(WorkspaceStore.Session p,int which){
        boolean active=target(p).equals(state.getString("target",""))&&state.getBoolean("started",false);
        if(which==0&&!graphical.contains(p.id)&&!sameSession(p,preparationProfile)&&!active&&!new WorkspaceStorageAccess(activity).granted()){
            Runnable proceed=()->{if(!closed&&sameSession(p,selected.get())&&visible.getAsBoolean())menuActionReady(p,which);};
            AlertDialog prompt=WorkspaceDialogs.builder(activity).setTitle(tr("Доступ к памяти телефона","Phone storage access"))
                .setMessage(tr("Разрешить доступ к файлам телефона для рабочего стола? В файловом менеджере появятся Phone storage и Downloads. Можно выбрать «Пока нет»: рабочий стол запустится, но доступ к этим папкам будет зависеть от разрешений Android. При следующем запуске без разрешения спросим снова.","Allow the desktop to access files on your phone? The file manager will include Phone storage and Downloads. Choose Not now to start without granting access. Android permissions still apply to these folders; we will ask again on the next desktop start if access is missing."))
                .setPositiveButton(tr("Разрешить","Allow"),(d,w)->activity.requestWorkspaceStorageAccess(proceed,proceed))
                .setNegativeButton(tr("Пока нет","Not now"),(d,w)->proceed.run()).create();
            prompt.setCanceledOnTouchOutside(false);prompt.show();return;
        }
        menuActionReady(p,which);
    }
    private void menuActionReady(WorkspaceStore.Session p,int which){
        if(which==0){
            if(graphical.remove(p.id)){sync();changed.run();}
            else if(sameSession(p,preparationProfile)){graphical.add(p.id);sync();changed.run();}
            else if(target(p).equals(state.getString("target",""))&&state.getBoolean("started",false)){graphical.add(p.id);ensure("launch",p);}
            else{preparationRenderer=renderer(p);rendererLaunchAfterInstall=true;rendererPreparationFailed=false;rendererReadyForPreparation=false;launchCheckAcknowledged=false;launchAfterCheck=p;preparationProfile=p;preparationNeedsInstall=false;preparationCancelling=false;graphical.add(p.id);sync();changed.run();ensure("check",p);}
        }
        if(which==1)WorkspaceDialogs.builder(activity).setMessage(tr("Остановить только графический рабочий стол? Терминальные сессии продолжат работу.","Stop only the graphical desktop? Terminal sessions will keep running.")).setNegativeButton(tr("Назад","Back"),null).setPositiveButton(tr("Остановить","Stop"),(x,y)->{launchAfterCheck=null;if(remote!=null)send("stop",p);}).show();
    }

    void openPrograms(){WorkspaceStore.Session p=selected.get();if(!eligible(p)||!supported(p))return;programs.open(p,state.getBundle("programs"));poll();}
    boolean programsVisible(){return programs.visible();}
    boolean programStatusPolling(){return visible.getAsBoolean()&&programsInstalling();}
    boolean programsInstalling(){
        Bundle job=state.getBundle("programs");if(job==null)return false;
        WorkspaceStore.Session p=selected.get();if(p==null||!target(p).equals(job.getString("target","")))return false;
        String phase=job.getString("phase","");return phase.equals("installing")||phase.equals("cancelling");
    }
    private static boolean sameSession(WorkspaceStore.Session a,WorkspaceStore.Session b){return a!=null&&b!=null&&a.id.equals(b.id)&&a.handle.equals(b.handle);}
    private void clearPreparation(){
        preparationProfile=null;preparationNeedsInstall=false;preparationCancelling=false;preparationLaunching=false;preparationRenderer="";preparationGpu=false;rendererLaunchAfterInstall=false;rendererCancel=null;rendererPreparationFailed=false;rendererReadyForPreparation=false;rendererEpoch++;
        if(preparationPrompt!=null){preparationPrompt.dismiss();preparationPrompt=null;}
    }
    private void confirmInstall(WorkspaceStore.Session p){
        preparationNeedsInstall=true;
        if(preparationPrompt!=null||!sameSession(p,selected.get())||!visible.getAsBoolean())return;
        preparationPrompt=WorkspaceDialogs.builder(activity).setTitle(tr("Установить рабочий стол X11?","Install X11 desktop?"))
            .setMessage((tr("В выбранное окружение будут установлены XFCE, файловый менеджер, терминал, шрифты, значки, D-Bus, Fcitx5 и звук PulseAudio. Потребуются интернет и свободное место.","XFCE, file manager, terminal, fonts, icons, D-Bus, Fcitx5 and PulseAudio will be installed in the selected environment. Internet and free space are required."))+"\n"+WorkspaceEnvironments.label(target(p))+(target(p).equals("archlinux")?tr("\nArch Linux: будут проверены права корневой папки окружения; владельцу при необходимости добавятся чтение, запись и вход. Также обновятся пакеты этого окружения (pacman -Syu).","\nArch Linux: the environment root directory will be checked and missing owner read/write/search permissions restored. This environment’s packages will also be upgraded (pacman -Syu)." ):""))
            .setNegativeButton(tr("Нет","No"),(d,w)->{preparationPrompt=null;graphical.remove(p.id);clearPreparation();sync();changed.run();})
            .setPositiveButton(tr("Да","Yes"),(d,w)->{preparationPrompt=null;preparationNeedsInstall=false;launchCheckAcknowledged=false;launchAfterCheck=p;if(!preparationRenderer.isEmpty())rendererLaunchAfterInstall=true;ensure("install",p);sync();}).create();
        preparationPrompt.setCancelable(false);preparationPrompt.setCanceledOnTouchOutside(false);preparationPrompt.show();
    }
    private void selectRenderer(WorkspaceStore.Session p,String choice){
        if(rendererBusy()||closed)return;
        if(!choice.equals("cpu")&&!(choice.equals("turnip")&&WorkspaceDesktopGpu.supported(target(p)))&&!(choice.equals("virgl")&&com.termux.x11.VirglInstaller.supported(target(p)))&&!(choice.equals("wrapper")&&WorkspaceDesktopWrapper.supported(target(p)))&&!(choice.equals("bionic-glibc")&&WorkspaceDesktopWrapper.bridgeSupported(target(p))))return;
        activity.getSharedPreferences("workspace-x11",0).edit().putString("renderer:"+target(p),choice).apply();
        if(sameSession(p,selected.get())&&visible.getAsBoolean())menu();
    }
    private void prepareSelectedRenderer(WorkspaceStore.Session p){

        String choice=preparationRenderer;
        if(!choice.equals("cpu")&&!WorkspaceDesktopGpu.installed(activity,target(p),choice)&&(!sameSession(p,selected.get())||!visible.getAsBoolean())){rendererReadyForPreparation=true;return;}
        rendererReadyForPreparation=false;boolean launch=rendererLaunchAfterInstall;int epoch=++rendererEpoch;
        preparationGpu=true;rendererPreparationFailed=false;rendererProgress=-1;rendererProgressText=tr("Проверка компонентов рендера…","Checking renderer components…");preparationUpdate();
        BooleanSupplier rendererUnavailable=()->rendererBackendBusy()||closed||epoch!=rendererEpoch;
        Runnable rendererUpdated=()->{
            if(closed||epoch!=rendererEpoch||!sameSession(p,preparationProfile))return;
            rendererCancel=null;preparationGpu=false;
            if(rendererPreparationFailed){preparationCancelling=false;preparationLaunching=false;sync();changed.run();return;}
            if(!renderer(p).equals(choice)||(!choice.equals("cpu")&&!WorkspaceDesktopGpu.installed(activity,target(p),choice))){
                graphical.remove(p.id);clearPreparation();sync();changed.run();if(sameSession(p,selected.get())&&visible.getAsBoolean())menu();return;
            }
            preparationRenderer="";
            if(launch){preparationLaunching=true;ensure("launch",p);sync();changed.run();}
            else {graphical.remove(p.id);clearPreparation();sync();changed.run();if(sameSession(p,selected.get())&&visible.getAsBoolean())menu();}
        };
        WorkspaceDesktopGpu.Inline inline=new WorkspaceDesktopGpu.Inline(){
            public void started(Runnable cancel){if(epoch!=rendererEpoch||!sameSession(p,preparationProfile))return;rendererCancel=cancel;preparationUpdate();}
            public void progress(int percent,String text){if(closed||epoch!=rendererEpoch)return;rendererProgress=percent;rendererProgressText=text;preparationUpdate();}
            public void finished(boolean success,String report){if(closed||epoch!=rendererEpoch)return;rendererPreparationFailed=!success;rendererProgressText=success?tr("Рендер готов","Renderer ready"):report.contains("GPU_CANCELLED")?tr("Установка рендера отменена","Renderer installation cancelled"):tr("Не удалось подготовить рендер. ","Could not prepare renderer. ")+report;}
        };
        if(choice.equals("wrapper")||choice.equals("bionic-glibc"))wrapper.prepare(rendererUnavailable,rendererUpdated,inline);
        else WorkspaceDesktopGpu.prepare(activity,target(p),choice,rendererUnavailable,rendererUpdated,inline,state.getBoolean("rendererComponents",false)&&choice.equals(state.getString("renderer",""))&&target(p).equals(state.getString("target","")));
    }
    private void preparationAction(){
        WorkspaceStore.Session p=preparationProfile;if(p==null)return;
        if(rendererPreparationFailed&&!preparationGpu){graphical.remove(p.id);launchAfterCheck=null;clearPreparation();sync();changed.run();return;}
        if(preparationGpu){if(rendererCancel!=null){preparationCancelling=true;rendererCancel.run();preparationUpdate();}return;}
        String phase=target(p).equals(state.getString("target",""))?state.getString("phase",""):"";
        if(preparationNeedsInstall||phase.equals("error")||phase.equals("cancelled")||phase.equals("missing")||phase.equals("stopped")||phase.equals("ended")){graphical.remove(p.id);launchAfterCheck=null;clearPreparation();sync();changed.run();return;}
        preparationCancelling=true;launchAfterCheck=null;
        if(remote!=null)send(preparationLaunching||state.getBoolean("started",false)||phase.equals("running")||phase.equals("preparing")?"stop":"cancel",p);
        else {pending="";pendingProfile=null;graphical.remove(p.id);clearPreparation();}
        sync();changed.run();
    }
    private void preparationUpdate(){
        if(preparation==null||preparationProfile==null)return;
        boolean matches=target(preparationProfile).equals(state.getString("target",""));
        String phase=matches?state.getString("phase",""):"checking";
        if(rendererPreparationFailed&&!preparationGpu){preparation.details(null);preparation.preparation(tr("Подготовка рендера","Renderer preparation"),rendererProgressText,-1,"",tr("В терминал","Return to terminal"),true,this::preparationAction);preparation.preparationIdle();return;}
        if(preparationGpu){preparation.details(null);preparation.preparation(tr("Установка рендера","Installing renderer"),rendererProgressText,rendererProgress,"",preparationCancelling?tr("Отменяется…","Cancelling…"):tr("Отменить установку","Cancel installation"),rendererCancel!=null&&!preparationCancelling,this::preparationAction);return;}
        if(matches&&state.getBoolean("preparingExtras",false)&&phase.equals("preparing")){
            int progress=state.getInt("installPercent",-1);long done=state.getLong("downloadBytes",-1),total=state.getLong("downloadTotal",-1),eta=state.getLong("installEta",-1);
            String text=tr("Проверка и загрузка темы Kali…","Checking and downloading Kali theme…");
            if(total>0&&done>=0)text+="\n"+formatBytes(done)+" / "+formatBytes(total);
            if(eta>0)text+="\n"+tr("Осталось: ≈ ","Remaining: ≈ ")+eta+tr(" с"," s");
            preparation.details(null);preparation.preparation(tr("Подготовка темы Kali","Preparing Kali theme"),text,progress,"",preparationCancelling?tr("Отменяется…","Cancelling…"):tr("Отменить установку","Cancel installation"),!preparationCancelling,this::preparationAction);return;
        }
        boolean installing=phase.equals("installing")||phase.equals("cancelling")&&state.getString("componentOperation","").equals("install")||preparationNeedsInstall;
        boolean finished=phase.equals("error")||phase.equals("cancelled")||phase.equals("stopped")||phase.equals("ended");
        String heading=preparationLaunching?tr("Запуск рабочего стола","Starting desktop"):installing?tr("Установка рабочего стола","Installing desktop"):tr("Проверка рабочего стола","Checking desktop");
        int percent=installing&&!preparationNeedsInstall?state.getInt("installPercent",-1):-1;
        String status=preparationNeedsInstall?tr("Требуется установка","Installation required"):matches?state.getString("status",""):tr("Подключение к службе X11…","Connecting to X11 service…");
        if(installing&&!preparationNeedsInstall&&!preparationCancelling&&!state.getString("installItem","").isEmpty()){
            String measuredStage=state.getString("installStage","");
            if(measuredStage.equals("downloading"))status=tr("Загрузка пакетов","Downloading packages");
            else if(measuredStage.equals("installing"))status=tr("Установка пакетов","Installing packages");
        }
        if(percent>=0)status+=" · "+percent+"%";
        long done=state.getLong("downloadBytes",-1),total=state.getLong("downloadTotal",-1);
        String stage=state.getString("installStage","");
        if(installing&&!preparationNeedsInstall&&(stage.equals("downloading")||stage.equals("updating"))&&done>=0&&total>0&&done<=total)status+=" ("+formatBytes(done)+" / "+formatBytes(total)+")";
        long eta=state.getLong("installEta",-1),left=eta-Math.max(0,(SystemClock.elapsedRealtime()-state.getLong("installSampleTime",0))/1000);
        if(installing&&!preparationNeedsInstall)status+="\n"+tr("Осталось на этапе: ","Remaining in this stage: ")+(eta<0||left<=0?tr("рассчитывается","calculating"):"≈ "+left+tr(" с"," s"));
        String detail=matches?state.getString("detail",""):"";String last=matches?state.getString("installItem",""):"";if(last.isEmpty())last=DesktopProgramProgress.lastLine(detail);
        if(finished)for(String line:detail.split("\n")){String reason=line.trim();if(reason.matches("(?i).*(?:no match for argument|error:|failed to|curl error|timed? out|connection reset|could not resolve|cannot download).*")){last=reason;break;}}
        preparation.preparation(heading,status,percent,last,finished||preparationNeedsInstall?tr("В терминал","Return to terminal"):preparationCancelling?tr("Отменяется…","Cancelling…"):installing?tr("Отменить установку","Cancel installation"):tr("Отмена","Cancel"),!preparationCancelling,this::preparationAction);
        preparation.details(finished&&!detail.trim().isEmpty()?()->preparationDetails(detail):null);
        if(finished||preparationNeedsInstall)preparation.preparationIdle();
    }
    private void preparationDetails(String text){
        LinearLayout body=AgentStatusUi.body(activity);
        TextView log=AgentStatusUi.info(body,text);log.setTag("x11-install-full-log");log.setTextIsSelectable(true);
        AlertDialog dialog=WorkspaceDialogs.builder(activity).setTitle(tr("Журнал установки X11","X11 installation log")).setView(AgentStatusUi.scroll(body))
            .setNegativeButton(tr("Назад","Back"),null).setNeutralButton(tr("Копировать","Copy"),null).create();
        dialog.show();dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
            ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText("X11",text));activity.showToast(tr("Скопировано","Copied"),false);}
        });
    }
    private String formatBytes(long value){double n=value;String[] units={"B","kB","MB","GB"};int i=0;while(n>=1000&&i<3){n/=1000;i++;}return String.format(java.util.Locale.ROOT,"%.1f %s",n,units[i]);}
    private void ensure(String op,WorkspaceStore.Session p){ensure(op,p,"");}
    private void ensure(String op,WorkspaceStore.Session p,String program){
        if(!op.equals("repair-launchers")&&!(op.equals("firefox-compat-on")||op.equals("firefox-compat-off"))&&!op.startsWith("programs-"))maintenanceReturn=false;
        if(Build.VERSION.SDK_INT<30){launchAfterCheck=null;if(op.startsWith("programs-"))programs.failed();activity.showToast(tr("Встроенный вид рабочего стола требует Android 11 или новее.","Inline desktop requires Android 11 or later."),true);return;}
        if(op.equals("check")||op.equals("install")||op.equals("repair-launchers")||(op.equals("firefox-compat-on")||op.equals("firefox-compat-off")))progressOrigin=p;
        if(remote!=null){send(op,p,program);return;}
        pending=op;pendingProfile=p;pendingProgram=program;
        try{Intent intent=new Intent(activity,X11Environment.forTarget(target(p)).service).putExtra("controlOnly",true);activity.startForegroundService(intent);if(!bound)bound=activity.bindService(intent,connection,Context.BIND_AUTO_CREATE);if(!bound)throw new IllegalStateException("Binding failed");}
        catch(Exception e){pending="";launchAfterCheck=null;if(sameSession(p,preparationProfile)){state=new Bundle();state.putString("target",target(p));state.putString("phase","error");state.putString("status",tr("Не удалось подключиться к X11","Could not connect to X11"));sync();changed.run();}if(op.startsWith("programs-"))programs.failed();activity.showToast(tr("Не удалось запустить X11: ","Could not start X11: ")+e.getClass().getSimpleName(),true);}
    }
    private void send(String op,WorkspaceStore.Session p){send(op,p,"");}
    private void send(String op,WorkspaceStore.Session p,String program){
        IBinder binder=remote;if(binder==null||closed)return;
        Bundle q=X11Remote.command(op);if(p!=null){q.putString("target",target(p));q.putString("desktop",desktop(p));q.putBoolean("garconFilter",false);q.putBoolean("termuxStyle",styleEnabled(p));q.putInt("interfaceScale",X11ScreenSettings.load(activity,target(p)).scalePercent);q.putString("renderer",renderer(p));if(op.equals("launch")||op.equals("session-close"))q.putString("owner",owner(p));}if(!program.isEmpty())q.putString("program",program);
        calls.execute(()->{try{Bundle result=X11Remote.call(binder,q);main.post(()->{if(!closed&&remote==binder){if((op.equals("check")||op.equals("install"))&&p!=null&&launchAfterCheck!=null&&p.id.equals(launchAfterCheck.id)&&p.handle.equals(launchAfterCheck.handle))launchCheckAcknowledged=true;accept(result);poll();sync();changed.run();}});}catch(Exception e){main.post(()->{if(!closed&&remote==binder){if(op.equals("session-close")){activity.showToast(tr("Не удалось подтвердить остановку X11. Проверьте рабочий стол и остановите его вручную.","Could not confirm X11 stop. Check the desktop and stop it manually."),true);return;}if(op.startsWith("observe-")){activity.showToast(tr("Не удалось изменить наблюдение X11. Повторите.","Could not change X11 observation. Try again."),true);poll();return;}if(op.startsWith("programs-")){programs.failed();activity.showToast(tr("Не удалось выполнить действие с программами. Дождитесь завершения другой установки и повторите проверку.","Program action failed. Wait for another installation to finish and check again."),true);return;}launchAfterCheck=null;if(sameSession(p,preparationProfile)){state=new Bundle();state.putString("target",target(p));state.putString("phase","error");state.putString("status",tr("Не удалось выполнить действие X11. Вернитесь в терминал и повторите.","X11 action failed. Return to terminal and retry."));preparationCancelling=false;sync();changed.run();return;}if(op.equals("launch")&&p!=null)graphical.remove(p.id);sync();dismissProgress();WorkspaceStore.Session origin=progressOrigin;progressOrigin=null;WorkspaceStore.Session current=selected.get();if(origin!=null&&current!=null&&current.id.equals(origin.id))menu();activity.showToast(tr("Не удалось выполнить действие X11. Проверьте, не запущен ли стол другого окружения, или остановите X11 и повторите.","X11 action failed. Check whether another environment owns the desktop, or stop X11 and retry."),true);changed.run();}});}});
    }
    private boolean programPolling(){return programs.polling();}
    private void poll(){if(polling||closed||remote==null)return;polling=true;main.postDelayed(tick,400);}
    private final Runnable tick=()->{polling=false;if(closed||remote==null||inFlight)return;inFlight=true;IBinder binder=remote;calls.execute(()->{Bundle result=null;try{result=X11Remote.call(binder,X11Remote.command("state"));}catch(Exception ignored){}Bundle answer=result;main.post(()->{inFlight=false;if(closed||binder!=remote)return;if(answer!=null){accept(answer);sync();}if(progress!=null||showing()||launchAfterCheck!=null||preparationCancelling||programPolling()||programStatusPolling())poll();});});};
    private void accept(Bundle b){
        boolean hadProgress=progress!=null;
        Bundle programState=b.getBundle("programs");if(programState!=null)programs.accept(programState);
        diagnosticState=b;diagnosticAt=SystemClock.elapsedRealtime();state=b;if(styleCheck!=null){styleCheck.setEnabled(!styleBusy());styleCheck.setAlpha(styleBusy()?.4f:1f);}if(rendererRow!=null)AgentStatusUi.available(rendererRow,!rendererBusy());if(wrapperFolder!=null)AgentStatusUi.available(wrapperFolder,!rendererBusy());if(wrapperDriver!=null)AgentStatusUi.available(wrapperDriver,!rendererBusy());String phase=b.getString("phase","");String t=b.getString("target","");
        if((phase.equals("installed")||phase.equals("running"))&&b.getString("desktop","xfce").equals(desktop(selected.get())))installed.put(t,true);
        if(phase.equals("missing")||phase.equals("error")||phase.equals("cancelled"))installed.put(t,false);
        WorkspaceStore.Session launch=launchAfterCheck;
        if(launch!=null&&launchCheckAcknowledged&&target(launch).equals(t)&&b.getBoolean("started",false)&&(phase.equals("running")||phase.equals("preparing"))){
            launchAfterCheck=null;progressOrigin=null;clearPreparation();graphical.add(launch.id);ensure("launch",launch);sync();changed.run();return;
        }
        if(launch!=null&&launchCheckAcknowledged&&target(launch).equals(t)&&(phase.equals("installed")||phase.equals("missing")||phase.equals("error")||phase.equals("cancelled"))){
            launchAfterCheck=null;progressOrigin=null;maintenanceReturn=false;dismissProgress();
            if(phase.equals("installed")){if(!preparationRenderer.isEmpty()){prepareSelectedRenderer(launch);return;}if(preparationProfile!=null){preparationLaunching=true;preparationNeedsInstall=false;}else graphical.add(launch.id);ensure("launch",launch);changed.run();return;}
            if(phase.equals("missing")&&preparationProfile!=null)preparationNeedsInstall=true;
            WorkspaceStore.Session current=selected.get();
            if(current!=null&&current.id.equals(launch.id)&&current.handle.equals(launch.handle)){
                if(phase.equals("missing"))confirmInstall(launch);else if(preparationProfile==null)menu();
            }
            return;
        }
        if(preparationCancelling&&preparationProfile!=null&&target(preparationProfile).equals(t)&&b.getBoolean("started",false)&&(phase.equals("running")||phase.equals("preparing"))){send("stop",preparationProfile);preparationUpdate();return;}
        if(preparationLaunching&&preparationProfile!=null&&target(preparationProfile).equals(t)&&(phase.equals("preparing")||phase.equals("running")))clearPreparation();
        if(preparationProfile!=null&&target(preparationProfile).equals(t)){
            // Inline installation belongs to this environment, even while its tab is hidden.
            if(phase.equals("cancelled")||phase.equals("error")||phase.equals("stopped")||phase.equals("ended")){preparationCancelling=false;preparationLaunching=false;}
            preparationUpdate();return;
        }
        boolean busy=phase.equals("checking")||phase.equals("installing")||phase.equals("cancelling");
        if(busy){
            if(progress==null)progress=new DesktopInstallUi(activity,"X11 · "+WorkspaceEnvironments.label(t),()->{graphical.clear();send("cancel",progressOrigin!=null?progressOrigin:selected.get());sync();});
            progress.update(b.getString("status",""),b.getString("detail",""),b.getInt("installPercent",-1),b.getLong("installEta",-1),b.getLong("installSampleTime",0),b.getString("installStage","preparing"),b.getLong("downloadBytes",-1),b.getLong("downloadTotal",-1));
        }else if(progress!=null){
            boolean returnMenu=progressOrigin!=null;
            WorkspaceStore.Session origin=progressOrigin;progressOrigin=null;boolean returnPrograms=maintenanceReturn;maintenanceReturn=false;
            dismissProgress();changed.run();
            WorkspaceStore.Session current=selected.get();
            if(returnMenu&&current!=null&&current.id.equals(origin.id)&&target(current).equals(t)){if(returnPrograms){
                if(phase.equals("error")||phase.equals("missing"))WorkspaceDialogs.builder(activity).setTitle(tr("Операция не завершена","Operation incomplete")).setMessage(b.getString("status","")+"\n"+b.getString("detail","")).setPositiveButton(tr("Назад","Back"),(d,w)->{programs.openInstalled(origin,state.getBundle("programs"));poll();}).show();
                else{activity.showToast(b.getString("status",""),false);programs.openInstalled(origin,state.getBundle("programs"));poll();}
            }else menu();}
            else if(!returnMenu&&(phase.equals("error")||phase.equals("missing")||phase.equals("cancelled")))WorkspaceDialogs.builder(activity).setTitle("X11").setMessage(b.getString("status","")+"\n"+b.getString("detail","")).setPositiveButton("OK",null).show();
        }
        if(phase.equals("stopping")||phase.equals("error")||phase.equals("ended")||phase.equals("cancelled")){boolean wasGraphical=showing();graphical.clear();shownTargets.clear();sync();if(wasGraphical&&!hadProgress&&!phase.equals("stopping"))WorkspaceDialogs.builder(activity).setTitle("X11").setMessage(b.getString("status","")+"\n"+b.getString("detail","")).setPositiveButton("OK",null).show();}
        if(Build.VERSION.SDK_INT>=30&&hasSurface()&&display.inputReady()&&target(selected.get()).equals(t)){
            keyboardState.automatic=automaticKeyboard(selected.get());
            boolean before=keyboardState.enabled;
            Boolean show=keyboardState.focus(b.getLong("focusSerial",0),b.getBoolean("textFocus",false));
            Boolean tap=keyboardState.tap(b.getLong("fieldTapSerial",0),b.getBoolean("textFocus",false));
            if(tap!=null)show=tap;
            if(show!=null)display.keyboard(show);
            if(before!=keyboardState.enabled)keyboardChanged.run();
        }
    }
    private boolean automaticKeyboard(WorkspaceStore.Session p){return activity.getSharedPreferences("workspace-x11",0).getBoolean("automatic-keyboard:"+target(p),true);}
    boolean desktopReady(String target){return target.equals(state.getString("target",""))&&state.getBoolean("ready",false)&&(state.getBoolean("desktopReady",false)||shownTargets.contains(target));}
    void sync(){
        if(Build.VERSION.SDK_INT<30||closed)return;
        if(programStatusPolling())poll();
        WorkspaceStore.Session p=selected.get();boolean inline=preparationProfile!=null&&sameSession(p,preparationProfile)&&showing();boolean show=!inline&&showing()&&remote!=null&&(state.getBoolean("ready",false)||state.getString("phase","").equals("preparing"))&&target(p).equals(state.getString("target",""));
        String wanted=show?p.id:"";
        if(display!=null&&!displayed.equals(wanted)){extraKeys.close();display.close();if(display.getParent() instanceof ViewGroup)((ViewGroup)display.getParent()).removeView(display);display=null;displayed="";boolean enabled=keyboardState.enabled;keyboardState.reset();restore.run();if(enabled)keyboardChanged.run();}
        if(!inline&&preparation!=null){if(preparation.getParent() instanceof ViewGroup)((ViewGroup)preparation.getParent()).removeView(preparation);preparation=null;if(!show)restore.run();}
        if(!inline&&preparationPrompt!=null){preparationPrompt.dismiss();preparationPrompt=null;}
        if(inline){
            if(preparation==null){FrameLayout parent=activity.findViewById(R.id.workspace_terminal_surface);preparation=new X11StartupView(activity,()->{});parent.addView(preparation,new FrameLayout.LayoutParams(-1,-1));}
            preparationUpdate();activity.getTerminalView().setWorkspaceTerminalVisible(false);activity.getTerminalView().setVisibility(View.INVISIBLE);input.setVisibility(View.GONE);activity.getTerminalToolbarViewPager().setVisibility(View.GONE);
            if(preparationNeedsInstall)confirmInstall(preparationProfile);if(rendererReadyForPreparation)prepareSelectedRenderer(preparationProfile);poll();
        }
        if(show&&display==null){FrameLayout parent=activity.findViewById(R.id.workspace_terminal_surface);String desktopTarget=target(p);display=new RemoteDesktopView(activity,b->{if(display!=null&&display.inputReady())shownTargets.add(desktopTarget);});display.screenSettings(X11ScreenSettings.load(activity,target(p)));display.onExtraKeysReset(extraKeys::close);displayed=wanted;parent.addView(display,new FrameLayout.LayoutParams(-1,-1));display.connect(remote);poll();}
        if(show){display.screenSettings(X11ScreenSettings.load(activity,target(p)));display.startup(desktopReady(target(p)),state.getBoolean("desktopStartupTimedOut",false),state.getInt("startupStep",0));activity.getTerminalView().setWorkspaceTerminalVisible(false);activity.getTerminalView().setVisibility(View.INVISIBLE);input.setVisibility(View.GONE);activity.getTerminalToolbarViewPager().setVisibility(View.GONE);}
        fullscreen.sync(inline?preparation:show?display:null);
    }
    boolean fullscreenBack(){if(extraKeys.showing()){extraKeys.close();return true;}return fullscreen.back();}
    boolean extraKeysVisible(){return extraKeys.showing();}
    void extraKeys(){
        if(extraKeys.showing()){extraKeys.close();return;}
        if(Build.VERSION.SDK_INT<30||display==null||!display.inputReady())return;
        RemoteDesktopView target=display;
        extraKeys.show(target,target::extraKey,target::releaseExtraKeys);
    }
    void keyboard(){if(Build.VERSION.SDK_INT>=30&&display!=null){display.keyboard(keyboardState.toggle());keyboardChanged.run();}}
    private void dismissProgress(){if(progress!=null){progress.close();progress=null;}}
    void close(){if(closed)return;graphical.clear();shownTargets.clear();sync();extraKeys.close();fullscreen.close();closed=true;clearPreparation();rendererRow=null;wrapperFolder=null;wrapperDriver=null;styleCheck=null;wrapper.close();dismissProgress();programs.close();if(Build.VERSION.SDK_INT>=30){ViewTreeObserver observer=activity.getWindow().getDecorView().getViewTreeObserver();if(observer.isAlive())observer.removeOnPreDrawListener(imeObserver);}main.removeCallbacksAndMessages(null);if(bound){activity.unbindService(connection);bound=false;}remote=null;calls.shutdown();}
    private String tr(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
}
