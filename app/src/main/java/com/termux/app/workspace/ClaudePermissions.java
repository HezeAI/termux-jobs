package com.termux.app.workspace;

import android.app.Activity;
import android.widget.*;
import java.util.Arrays;

/** Saved launch policy; a special mode never erases the previous ordinary choice. */
final class ClaudePermissions {
    static final String[] ORDINARY={"manual","acceptEdits","plan"};
    static final String[] MODES={"manual","acceptEdits","plan","auto","bypassPermissions"};
    static boolean ordinary(String mode){return Arrays.asList(ORDINARY).contains(mode);}
    static boolean valid(String mode){return Arrays.asList(MODES).contains(mode);}
    static void select(WorkspaceStore.Session p,String mode){
        if(!valid(mode))throw new IllegalArgumentException("Invalid Claude permission mode");
        if(ordinary(p.claudeMode))p.claudeOrdinaryMode=p.claudeMode;
        p.claudeMode=mode;if(ordinary(mode))p.claudeOrdinaryMode=mode;
    }
    static void toggle(WorkspaceStore.Session p,String mode,boolean enabled){
        if(enabled)select(p,mode);
        else if(p.claudeMode.equals(mode))select(p,ordinary(p.claudeOrdinaryMode)?p.claudeOrdinaryMode:"manual");
    }
    static String label(Activity a,String mode){return mode.equals("auto")?"Auto":mode.equals("bypassPermissions")?"Bypass permissions":mode.equals("plan")?AgentStatusUi.tr(a,"План / чтение","Plan / read"):mode.equals("acceptEdits")?AgentStatusUi.tr(a,"Разрешать правки","Accept edits"):AgentStatusUi.tr(a,"Подтверждать действия","Confirm actions");}
    static final class Controls {
        final CheckBox auto,bypass;final TextView ordinary,status;final WorkspaceStore.Session draft;
        boolean rendering;boolean autoAvailable,bypassAvailable;
        Controls(Activity a,LinearLayout body,WorkspaceStore.Session p){
            draft=p;
            ordinary=AgentStatusUi.row(body,"","claude-ordinary-mode",()->{});
            auto=new CheckBox(a);auto.setTag("claude-mode-auto");auto.setText("Auto");auto.setTextColor(WorkspaceDialogs.TEXT);body.addView(auto);
            AgentStatusUi.info(body,AgentStatusUi.tr(a,"Автоматическая проверка действий. Claude автоматически оценивает команды и разрешения. Некоторые действия могут быть отклонены.","Automatic action review. Claude evaluates commands and permissions automatically. Some actions may be denied."));
            bypass=new CheckBox(a);bypass.setTag("claude-mode-bypassPermissions");bypass.setText("Bypass permissions");bypass.setTextColor(WorkspaceDialogs.TEXT);body.addView(bypass);
            AgentStatusUi.info(body,AgentStatusUi.tr(a,"Пропуск проверок разрешений. Выполнение без большинства запросов подтверждения; исключения Claude сохраняются.","Skip permission checks. Execute without most approval prompts; Claude exceptions still apply."));
            status=AgentStatusUi.info(body,AgentStatusUi.tr(a,"Проверка установленного Claude и окружения…","Checking installed Claude and environment…"));status.setTag("claude-permission-support");
            Runnable render=()->{rendering=true;auto.setChecked(p.claudeMode.equals("auto"));bypass.setChecked(p.claudeMode.equals("bypassPermissions"));auto.setEnabled(autoAvailable||auto.isChecked());bypass.setEnabled(bypassAvailable||bypass.isChecked());ordinary.setText(AgentStatusUi.tr(a,"Обычный режим: ","Ordinary mode: ")+label(a,ordinary(p.claudeMode)?p.claudeMode:p.claudeOrdinaryMode));rendering=false;};
            auto.setOnCheckedChangeListener((v,on)->{if(!rendering){toggle(p,"auto",on);render.run();}});bypass.setOnCheckedChangeListener((v,on)->{if(!rendering){toggle(p,"bypassPermissions",on);render.run();}});
            ordinary.setOnClickListener(v->{String[] labels=Arrays.stream(ORDINARY).map(m->label(a,m)).toArray(String[]::new);WorkspaceDialogs.builder(a).setTitle(AgentStatusUi.tr(a,"Обычный режим Claude","Ordinary Claude mode")).setSingleChoiceItems(labels,Arrays.asList(ORDINARY).indexOf(p.claudeMode),(d,n)->{select(p,ORDINARY[n]);render.run();d.dismiss();}).setNegativeButton(AgentStatusUi.tr(a,"Отмена","Cancel"),null).show();});
            refresh=render;render.run();
        }
        final Runnable refresh;
        void support(Activity a,String output){
            autoAvailable=output.contains("\"auto\"");bypassAvailable=output.contains("\"bypassPermissions\"")&&!output.contains("UID=0\n");
            String message=output.contains("UID=0\n")?AgentStatusUi.tr(a,"Bypass permissions недоступен: Claude запрещает этот режим при запуске от root в Ubuntu/proot.","Bypass permissions unavailable: Claude refuses this mode when running as root in Ubuntu/proot."):bypassAvailable?"":AgentStatusUi.tr(a,"Bypass permissions: установленный CLI не подтвердил поддержку.","Bypass permissions: installed CLI support was not confirmed.");
            message+="\n"+(autoAvailable?AgentStatusUi.tr(a,"Auto поддерживается CLI. Доступность для модели и аккаунта проверит Claude при запуске; при отказе запуск будет остановлен.","Auto is supported by the CLI. Claude will check model and account availability on launch; rejection stops startup."):AgentStatusUi.tr(a,"Auto недоступен: установленный CLI не подтвердил поддержку. Проверьте установку Claude в выбранном окружении.","Auto unavailable: installed CLI support was not confirmed. Check Claude installation in the selected environment."));
            status.setText(message.trim());refresh.run();
        }
    }
    static void show(Activity a,LinearLayout body,WorkspaceStore.Session draft){
        Controls controls=new Controls(a,body,draft);
        new Thread(()->{String result="";try(ProjectFiles fs=new ProjectFiles(a,draft)){
            result=new String(fs.readOnly("printf 'UID=%s\\n' \"$(id -u)\"; binary=\"$HOME/.local/share/termux-jobs/claude-code/2.1.296/claude\"; if [ -x \"$binary\" ]; then DISABLE_AUTOUPDATER=1 CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC=1 \"$binary\" --help; else printf NOT_INSTALLED; fi",65536),java.nio.charset.StandardCharsets.UTF_8);
        }catch(Exception ignored){}String output=result;a.runOnUiThread(()->{if(body.isAttachedToWindow())controls.support(a,output);});},"claude-permission-support").start();
    }
}
