package com.termux.app.workspace;

/** Ubuntu runtime selection is shared by all four agents; old profiles stay on Ubuntu. */
final class AgentEnvironments {
    static String editorUbuntu(WorkspaceStore.Session existing){return existing==null?"ubuntu-ai":existing.codexEnvironment;}
    static boolean ubuntu(String name){return "ubuntu".equals(name)||"ubuntu-ai".equals(name);}
    static String defaultUbuntu(){return defaultUbuntu(new java.io.File(com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"));}
    static String defaultUbuntu(java.io.File proot){return WorkspaceEnvironments.root(proot,"ubuntu-ai")!=null||WorkspaceEnvironments.root(proot,"ubuntu")==null?"ubuntu-ai":"ubuntu";}
    static String alias(WorkspaceStore.Session p){if(!ubuntu(p.codexEnvironment))throw new IllegalArgumentException("Invalid agent Ubuntu");return p.codexEnvironment;}
    static String scope(WorkspaceStore.Session p){return !AgentPreparation.agent(p).isEmpty()&&ubuntu(p.codexEnvironment)?p.codexEnvironment:WorkspaceEnvironments.alias(p);}
    static String label(String name){return "ubuntu-ai".equals(name)?"Ubuntu AI":name.equals("ubuntu")?"Ubuntu":"Termux";}
}
