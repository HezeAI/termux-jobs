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
        boolean rendering,supportChecked;boolean autoAvailable,bypassAvailable;String rejected="";Runnable changed=()->{};
        Controls(Activity a,LinearLayout body,WorkspaceStore.Session p){this(a,body,p,null);}
        Controls(Activity a,LinearLayout body,WorkspaceStore.Session p,TextView ordinaryRow){
            draft=p;
            ordinary=ordinaryRow==null?AgentStatusUi.row(body,"","claude-ordinary-mode",()->{}):ordinaryRow;
            auto=new CheckBox(a);auto.setTag("claude-mode-auto");auto.setText("Auto");auto.setTextColor(WorkspaceDialogs.TEXT);body.addView(auto);
            AgentStatusUi.info(body,AgentStatusUi.tr(a,"Claude автоматически проверяет действия и разрешает допустимые без вашего подтверждения. Некоторые действия могут быть отклонены.","Claude automatically reviews actions and allows eligible actions without your confirmation. Some actions may be denied."));
            bypass=new CheckBox(a);bypass.setTag("claude-mode-bypassPermissions");bypass.setText("Bypass permissions");bypass.setTextColor(WorkspaceDialogs.TEXT);body.addView(bypass);
            AgentStatusUi.info(body,AgentStatusUi.tr(a,"Claude пропускает большинство проверок разрешений. Отдельные ограничения Claude сохраняются.","Claude skips most permission checks. Some Claude restrictions remain."));
            status=AgentStatusUi.info(body,AgentStatusUi.tr(a,"Проверка установленного Claude и окружения…","Checking installed Claude and environment…"));status.setTag("claude-permission-support");
            Runnable render=()->{rendering=true;auto.setChecked(autoAvailable&&p.claudeMode.equals("auto"));bypass.setChecked(bypassAvailable&&p.claudeMode.equals("bypassPermissions"));auto.setEnabled(autoAvailable);bypass.setEnabled(bypassAvailable);boolean normal=ordinary(p.claudeMode);ordinary.setEnabled(normal);ordinary.setAlpha(normal?1f:.45f);ordinary.setTextColor(normal?WorkspaceDialogs.TEXT:WorkspaceDialogs.MUTED);ordinary.setText(AgentStatusUi.tr(a,"Обычный режим: ","Ordinary mode: ")+label(a,ordinary(p.claudeMode)?p.claudeMode:p.claudeOrdinaryMode));rendering=false;changed.run();};
            auto.setOnCheckedChangeListener((v,on)->{if(!rendering){toggle(p,"auto",on);render.run();}});bypass.setOnCheckedChangeListener((v,on)->{if(!rendering){toggle(p,"bypassPermissions",on);render.run();}});
            ordinary.setOnClickListener(v->{if(!ordinary.isEnabled())return;String[] labels=Arrays.stream(ORDINARY).map(m->label(a,m)).toArray(String[]::new);WorkspaceDialogs.builder(a).setTitle(AgentStatusUi.tr(a,"Обычный режим Claude","Ordinary Claude mode")).setSingleChoiceItems(labels,Arrays.asList(ORDINARY).indexOf(p.claudeMode),(d,n)->{select(p,ORDINARY[n]);render.run();d.dismiss();}).setNegativeButton(AgentStatusUi.tr(a,"Отмена","Cancel"),null).show();});
            refresh=render;render.run();
        }
        final Runnable refresh;
        void support(Activity a,String output){
            supportChecked=true;autoAvailable=output.contains("\"auto\"")&&!rejected.equals("auto");bypassAvailable=output.contains("\"bypassPermissions\"")&&output.matches("(?s).*UID=[1-9][0-9]*\n.*")&&!rejected.equals("bypassPermissions");
            if(draft.claudeMode.equals("auto")&&!autoAvailable||draft.claudeMode.equals("bypassPermissions")&&!bypassAvailable)toggle(draft,draft.claudeMode,false);
            String message=output.contains("UID=0\n")?AgentStatusUi.tr(a,"Недоступно при запуске Claude от root внутри Ubuntu","Unavailable when Claude runs as root inside Ubuntu"):bypassAvailable?"":AgentStatusUi.tr(a,"Bypass permissions: установленный CLI не подтвердил поддержку.","Bypass permissions: installed CLI support was not confirmed.");
            message+="\n"+(autoAvailable?AgentStatusUi.tr(a,"Auto поддерживается CLI. Доступность для модели и аккаунта проверит Claude при запуске; при отказе запуск будет остановлен.","Auto is supported by the CLI. Claude will check model and account availability on launch; rejection stops startup."):AgentStatusUi.tr(a,"Auto недоступен: установленный CLI не подтвердил поддержку. Проверьте установку Claude в выбранном окружении.","Auto unavailable: installed CLI support was not confirmed. Check Claude installation in the selected environment."));
            if(!rejected.isEmpty())message+="\n"+AgentStatusUi.tr(a,"Claude отклонил режим: ","Claude rejected mode: ")+rejected;status.setText(message.trim());refresh.run();
        }
    }
    static Controls show(Activity a,LinearLayout body,WorkspaceStore.Session draft){
        Controls controls=new Controls(a,body,draft);probe(a,body,draft,controls);return controls;
    }
    static void probe(Activity a,LinearLayout body,WorkspaceStore.Session draft,Controls controls){
        body.post(()->new Thread(()->{String result="";org.json.JSONObject nativeStatus=null;
            try{nativeStatus=ClaudeLiveControl.request(a.getFilesDir(),draft.claudeSocketId,AgentChatUi.op("status"));}catch(Exception ignored){}
            try(ProjectFiles fs=new ProjectFiles(a,draft)){
                result=new String(fs.readOnly("printf 'UID=%s\\n' \"$(id -u)\"; binary=\"$HOME/.local/share/termux-jobs/claude-code/2.1.296/claude\"; if [ -x \"$binary\" ]; then DISABLE_AUTOUPDATER=1 CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC=1 \"$binary\" --help; else printf NOT_INSTALLED; fi",65536),java.nio.charset.StandardCharsets.UTF_8);
            }catch(Exception ignored){}String output=result;org.json.JSONObject current=nativeStatus;
            a.runOnUiThread(()->{if(body.isAttachedToWindow()){
                TextView active=body.findViewWithTag("claude-active-mode");if(active!=null)active.setText(AgentStatusUi.tr(a,"Действующий режим: ","Active mode: ")+(current!=null&&current.optBoolean("ready")&&valid(current.optString("mode"))?label(a,current.optString("mode")):AgentStatusUi.tr(a,"Нет данных","No data")));
                if(current!=null)controls.rejected=current.optString("rejectedMode");controls.support(a,output);
            }});
        },"claude-permission-support").start());
    }
}
