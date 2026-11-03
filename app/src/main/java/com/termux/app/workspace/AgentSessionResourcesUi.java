package com.termux.app.workspace;

import android.widget.*;
import com.termux.app.TermuxActivity;

/** Session-bound entry point; storage and editing remain in the existing managers. */
final class AgentSessionResourcesUi {
    static String menuLabel(WorkspaceStore.Session source){
        String agent=AgentPreparation.agent(source);
        return agent.equals("codex")?"AGENTS.md & SKILL.md":agent.equals("claude")?"CLAUDE.md & SKILL.md":"";
    }
    static final class Context {
        final TermuxActivity activity;
        final String agent,environment,project;
        Context(TermuxActivity a,WorkspaceStore.Session snapshot){activity=a;agent=AgentPreparation.agent(snapshot);environment=snapshot.codexEnvironment;project=AgentInstructionsUi.suggested(snapshot);}
        String t(String ru,String en){return AgentInstructionsUi.tr(activity,ru,en);}
        String description(){return (agent.equals("codex")?"Codex":"Claude Code")+"\n"+AgentInstructionsUi.runtimeNotice(activity,environment)+"\n"+t("Папка проекта: ","Project folder: ")+project;}
        AgentInstructionsUi.Editor editor(AgentInstructions.Backend backend){AgentInstructionsUi.Editor e=new AgentInstructionsUi.Editor(activity,project,backend);e.agent=agent;e.name=e.baseName();e.scope="project";e.selectedEnvironment=environment;return e;}
        AgentSkillsUi.Screen skills(AgentInstructions.Backend backend){AgentSkillsUi.Screen s=new AgentSkillsUi.Screen(activity,project,agent);s.environment=environment;s.scope="project";if(backend!=null)s.backend=backend;return s;}
    }
    static Context context(TermuxActivity a,WorkspaceStore.Session source){
        if(source==null||menuLabel(source).isEmpty())return null;
        WorkspaceStore.Session snapshot=source.copy();
        if(!AgentEnvironments.ubuntu(snapshot.codexEnvironment)&&!snapshot.codexEnvironment.equals("termux"))return null;
        return new Context(a,snapshot);
    }
    static android.app.AlertDialog show(TermuxActivity a,WorkspaceStore.Session source){return show(a,source,q->AgentInstructions.request(a,q));}
    static android.app.AlertDialog show(TermuxActivity a,WorkspaceStore.Session source,AgentInstructions.Backend backend){
        Context context=context(a,source);
        if(context==null){Toast.makeText(a,AgentInstructionsUi.tr(a,"Инструкции и навыки для этого агента или среды пока не поддерживаются.","Instructions and skills are not supported for this agent or environment yet."),Toast.LENGTH_LONG).show();return null;}
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);
        TextView location=WorkspaceDialogs.action(a,context.description(),0);location.setTextIsSelectable(true);location.setTag("agent-resources-context");panel.addView(location);
        TextView note=WorkspaceDialogs.action(a,context.t("Файлы на диске не подтверждают, что инструкции применены или навыки загружены работающим агентом. Сессия автоматически не перезапускается.","Files on disk do not confirm that the running agent applied instructions or loaded skills. The session is not restarted automatically."),0);panel.addView(note);
        TextView instructions=WorkspaceDialogs.action(a,context.t("Инструкции","Instructions"),0);instructions.setTag("agent-resources-instructions");instructions.setOnClickListener(v->context.editor(backend).show());panel.addView(instructions);
        TextView skills=WorkspaceDialogs.action(a,context.t("Навыки","Skills"),0);skills.setTag("agent-resources-skills");skills.setOnClickListener(v->context.skills(backend).show());panel.addView(skills);
        ScrollView scroll=new ScrollView(a);scroll.addView(panel);
        return WorkspaceDialogs.builder(a).setTitle(menuLabel(source)).setView(scroll).setNegativeButton(context.t("Закрыть","Close"),null).show();
    }
}
