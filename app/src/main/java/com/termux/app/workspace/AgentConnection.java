package com.termux.app.workspace;

/** Optional user-owned executable, scoped to this agent's startup process only. */
final class AgentConnection {
    private AgentConnection() {}
    static boolean valid(String value){
        if(value==null||value.length()>4096)return false;
        if(value.isEmpty())return true;
        if(!value.startsWith("/"))return false;
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))return false;
        return true;
    }
    static String wrap(WorkspaceStore.Session profile,String command){
        if(!AgentPreparation.supports(profile)||profile.agentConnectionCommand.isEmpty())return command;
        String path=profile.agentConnectionCommand;
        if(!valid(path))throw new IllegalArgumentException("Invalid agent connection executable");
        String q=SessionShell.quote(path);
        return "( [ -f "+q+" ] && [ -x "+q+" ] || { printf '%s\\n' "
            +SessionShell.quote(WorkspaceText.tr("Агент: ").startsWith("Агент")?"Команда подключения недоступна или не исполняется. Обычный запуск не выполнен.":"Connection executable is missing or not executable. Direct startup was not used.")
            +" >&2; exit 126; }; exec "+q+" /bin/bash --noprofile --norc -c "+SessionShell.quote(command)+" )";
    }
}
