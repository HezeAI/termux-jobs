package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Launch-only policies. Live mode changes remain in each native adapter UI. */
final class AgentPermissionsUi {
    private static String t(Activity a,String ru,String en){return AgentStatusUi.tr(a,ru,en);}
    static String policy(WorkspaceStore.Session p){return p.codexAccess+"|"+p.codexConfirmActions+"|"+p.codexUntrustedApproval+"|"+p.claudeMode+"|"+p.geminiMode+"|"+p.aiAccess+"|"+p.aiMode+"|"+p.aiProjectOnly;}
    static void show(Activity a,WorkspaceStore.Session p,CodexStatus active,boolean scopeOnly,BooleanSupplier valid,Consumer<WorkspaceStore.Session> save){
        WorkspaceStore.Session draft=WorkspaceRestart.snapshot(p);String before=policy(p);boolean beforeScope=p.aiProjectOnly;
        LinearLayout body=AgentStatusUi.body(a);body.setTag("agent-permissions-only");String agent=AgentPreparation.agent(p);
        boolean codex=agent.equals("codex");String current=t(a,"Действующий режим: Нет данных","Active mode: No data");
        if(codex&&active!=null&&active.error.isEmpty()){
            String access=active.activeAccess.isEmpty()?t(a,"Нет данных","No data"):active.activeAccess;
            String approval=active.activeApproval.isEmpty()?t(a,"Нет данных","No data"):active.activeApproval;
            current=t(a,"Действующий доступ: ","Active access: ")+access+"\n"+t(a,"Действующие подтверждения: ","Active approvals: ")+approval;
            if(active.updated<=0||System.currentTimeMillis()-active.updated>90000)current+=t(a,"\nДанные устарели","\nData is stale");
        }
        if(!scopeOnly)AgentStatusUi.info(body,current);
        AgentStatusUi.info(body,t(a,"Ниже — настройки следующего запуска. Сохранение не меняет действующий процесс. Применяются только после перезапуска этого чата.","Below are next-launch settings. Saving does not change the running process. They apply only after restarting this chat."));
        if(!scopeOnly){
            if(codex){
                choose(a,body,t(a,"Доступ после перезапуска","Access after restart"),new String[]{"read","project","full"},new String[]{t(a,"Только чтение","Read only"),t(a,"Запись в пределах проекта","Writes within project"),t(a,"Полный доступ среды","Full environment access")},draft.codexAccess,v->draft.codexAccess=v);
                CheckBox approvals=new CheckBox(a);approvals.setTag("codex-command-confirmations");approvals.setText(draft.codexUntrustedApproval?t(a,"Подтверждать недоверенные команды","Confirm untrusted commands"):t(a,"Подтверждать действия · Codex","Confirm actions · Codex"));approvals.setTextColor(WorkspaceDialogs.TEXT);draft.codexConfirmActions=!CodexSessionShell.approval(p).equals("never");approvals.setChecked(draft.codexConfirmActions);approvals.setOnCheckedChangeListener((v,on)->draft.codexConfirmActions=on);body.addView(approvals);
                AgentStatusUi.info(body,t(a,"Ограничение записи и подтверждения независимы. Чтение вне проекта может быть доступно. Права ограничены окружением, это не root Android. При отказе песочницы доступ автоматически не расширяется.","Write restrictions and approvals are independent. Reading outside the project may be possible. Environment permissions still apply; this is not Android root. Sandbox failure never broadens access automatically."));
            }else if(p.claudeAgent){choose(a,body,t(a,"Режим после перезапуска","Mode after restart"),new String[]{"manual","acceptEdits","plan"},new String[]{t(a,"Подтверждать действия","Confirm actions"),t(a,"Разрешать правки","Accept edits"),t(a,"План / чтение","Plan / read")},draft.claudeMode,v->draft.claudeMode=v);
            }else if(p.geminiAgent){choose(a,body,t(a,"Режим после перезапуска","Mode after restart"),GeminiRightsUi.MODES,new String[]{GeminiRightsUi.label("default"),GeminiRightsUi.label("auto_edit"),GeminiRightsUi.label("plan")},draft.geminiMode,v->draft.geminiMode=v);
            }else if(p.aiAgent.equals("opencode")){
                choose(a,body,t(a,"Права после перезапуска","Permissions after restart"),new String[]{"ask","read","full"},new String[]{AiAgentsUi.accessLabel("ask"),AiAgentsUi.accessLabel("read"),AiAgentsUi.accessLabel("full")},draft.aiAccess,v->draft.aiAccess=v);
                choose(a,body,t(a,"Режим после перезапуска","Mode after restart"),new String[]{"build","plan"},new String[]{"Build","Plan"},draft.aiMode,v->draft.aiMode=v);
            }
        }
        if(p.aiAgent.equals("opencode")){
            CheckBox scope=new CheckBox(a);scope.setTag("opencode-project-scope");scope.setText(t(a,"Ограничивать внешние пути · после перезапуска","Restrict external paths · after restart"));scope.setTextColor(WorkspaceDialogs.TEXT);scope.setChecked(p.aiProjectOnly);scope.setOnCheckedChangeListener((v,on)->draft.aiProjectOnly=on);body.addView(scope);
            AgentStatusUi.info(body,t(a,"Правила запуска ограничивают внешние каталоги, shell, субагентов и внешние инструменты. Это не системная песочница. Режим прав не заменяет это ограничение.","Launch rules restrict external directories, shell, subagents and external tools. This is not an OS sandbox. Permission mode does not replace this restriction."));
        }
        AlertDialog dialog=WorkspaceDialogs.builder(a).setTitle(t(a,"Права и подтверждения · ","Permissions and approvals · ")+agent).setView(AgentStatusUi.scroll(body)).setNegativeButton(t(a,"Отмена","Cancel"),null).setPositiveButton(t(a,"Сохранить для следующего запуска","Save for next launch"),null).create();dialog.show();
        dialog.getButton(-1).setOnClickListener(v->{if(!valid.getAsBoolean()||(scopeOnly?p.aiProjectOnly!=beforeScope:!before.equals(policy(p)))){Toast.makeText(a,t(a,"Сессия или настройки изменились. Откройте окно заново.","Session or settings changed. Reopen this window."),Toast.LENGTH_LONG).show();return;}
            if(scopeOnly)p.aiProjectOnly=draft.aiProjectOnly;else copyPolicy(draft,p);save.accept(p);dialog.dismiss();});
    }
    static void copyPolicy(WorkspaceStore.Session from,WorkspaceStore.Session to){to.codexAccess=from.codexAccess;to.codexConfirmActions=from.codexConfirmActions;to.codexUntrustedApproval=from.codexUntrustedApproval;to.claudeMode=from.claudeMode;to.geminiMode=from.geminiMode;to.aiAccess=from.aiAccess;to.aiMode=from.aiMode;to.aiProjectOnly=from.aiProjectOnly;}
    private static void choose(Activity a,LinearLayout body,String title,String[] values,String[] labels,String initial,Consumer<String> change){
        String[] value={initial};TextView row=AgentStatusUi.row(body,"","launch-permission",()->{});Runnable render=()->{int i=java.util.Arrays.asList(values).indexOf(value[0]);row.setText(title+": "+(i<0?t(a,"Нет данных","No data"):labels[i]));};render.run();
        row.setOnClickListener(v->WorkspaceDialogs.builder(a).setTitle(title).setSingleChoiceItems(labels,java.util.Arrays.asList(values).indexOf(value[0]),(d,n)->{d.dismiss();Runnable apply=()->{value[0]=values[n];change.accept(value[0]);render.run();};if(values[n].equals("full"))WorkspaceDialogs.builder(a).setTitle(t(a,"Полный доступ среды?","Full environment access? ")).setMessage(t(a,"Агент сможет изменять доступные файлы и выполнять команды в окружении. Отдельные настройки подтверждений сохраняются. Это не root Android.","The agent can change accessible files and execute commands in the environment. Separate approval settings are retained. This is not Android root.")).setPositiveButton(t(a,"Выбрать","Select"),(confirm,which)->apply.run()).setNegativeButton(t(a,"Отмена","Cancel"),null).show();else apply.run();}).setNegativeButton(t(a,"Отмена","Cancel"),null).show());
    }
}
