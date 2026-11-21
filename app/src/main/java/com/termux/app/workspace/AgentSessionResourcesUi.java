package com.termux.app.workspace;

import android.widget.*;
import com.termux.app.TermuxActivity;

/** Session-bound entry point; storage and editing remain in the existing managers. */
final class AgentSessionResourcesUi {
    static String menuLabel(WorkspaceStore.Session source){
        return source!=null&&AgentInstructions.supports(AgentPreparation.agent(source),source.codexEnvironment)
            ?WorkspaceText.tr("Инструкции проекта"):"";
    }
    static final class Context {
        final TermuxActivity activity;
        final String agent,environment,project,geminiRuntime;
        Context(TermuxActivity a,WorkspaceStore.Session snapshot){activity=a;agent=AgentPreparation.agent(snapshot);environment=snapshot.codexEnvironment;project=AgentInstructionsUi.suggested(snapshot);geminiRuntime=AgentInstructions.geminiRuntime(snapshot);}
        boolean hasSkills(){return !agent.equals("gemini")||geminiRuntime.equals("antigravity")&&environment.equals("ubuntu-ai");}
        String t(String ru,String en){return AgentInstructionsUi.tr(activity,ru,en);}
        String description(){String label=agent.equals("codex")?"Codex":agent.equals("claude")?"Claude Code":geminiRuntime.equals("antigravity")?"Gemini · Antigravity":"Gemini CLI";return label+"\n"+AgentInstructionsUi.runtimeNotice(activity,environment)+"\n"+t("Папка проекта: ","Project folder: ")+project;}
        AgentInstructionsUi.Editor editor(AgentInstructions.Backend backend){AgentInstructionsUi.Editor e=new AgentInstructionsUi.Editor(activity,project,backend);e.agent=agent;e.geminiRuntime=geminiRuntime;e.name=e.baseName();e.scope="project";e.selectedEnvironment=environment;e.sessionBound=true;return e;}
        AgentSkillsUi.Screen skills(AgentInstructions.Backend backend){AgentSkillsUi.Screen s=new AgentSkillsUi.Screen(activity,project,agent);s.environment=environment;s.scope="project";s.sessionBound=true;if(backend!=null)s.backend=backend;return s;}
    }
    static Context context(TermuxActivity a,WorkspaceStore.Session source){
        if(source==null||menuLabel(source).isEmpty())return null;
        WorkspaceStore.Session snapshot=source.copy();
        if(!AgentEnvironments.ubuntu(snapshot.codexEnvironment)&&!snapshot.codexEnvironment.equals("termux"))return null;
        return new Context(a,snapshot);
    }
    static void showProject(TermuxActivity a,WorkspaceStore.Session source){
        Context c=context(a,source);if(c==null){Toast.makeText(a,AgentInstructionsUi.tr(a,"Инструкции этого агента в выбранной среде пока не поддерживаются.","This agent’s instructions in the selected environment are not supported yet."),Toast.LENGTH_LONG).show();return;}
        c.editor(q->AgentInstructions.request(a,q)).show();
    }
    static android.app.AlertDialog show(TermuxActivity a,WorkspaceStore.Session source){return show(a,source,q->AgentInstructions.request(a,q));}
    static android.app.AlertDialog show(TermuxActivity a,WorkspaceStore.Session source,AgentInstructions.Backend backend){
        Context context=context(a,source);
        if(context==null){Toast.makeText(a,AgentInstructionsUi.tr(a,"Инструкции и навыки для этого агента или среды пока не поддерживаются.","Instructions and skills are not supported for this agent or environment yet."),Toast.LENGTH_LONG).show();return null;}
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);
        TextView location=WorkspaceDialogs.action(a,context.description(),0);location.setTextIsSelectable(true);location.setTag("agent-resources-context");panel.addView(location);
        TextView note=WorkspaceDialogs.action(a,context.t("Чтобы перечитать файлы проекта, перезапустите выбранный чат с продолжением диалога.","Restart the selected chat and resume the conversation to reread project files."),0);panel.addView(note);
        TextView instructions=WorkspaceDialogs.action(a,context.t("Инструкции","Instructions"),0);instructions.setTag("agent-resources-instructions");instructions.setOnClickListener(v->context.editor(backend).show());panel.addView(instructions);
        if(context.hasSkills()){
            TextView skills=WorkspaceDialogs.action(a,context.t("Навыки","Skills"),0);skills.setTag("agent-resources-skills");skills.setOnClickListener(v->context.skills(backend).show());panel.addView(skills);
        }
        ScrollView scroll=new ScrollView(a);scroll.addView(panel);
        return WorkspaceDialogs.builder(a).setTitle(menuLabel(source)).setView(scroll).setNegativeButton(context.t("Закрыть","Close"),null).show();
    }
}
