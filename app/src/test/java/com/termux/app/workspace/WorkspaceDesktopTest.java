package com.termux.app.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
public class WorkspaceDesktopTest {
    @Test public void onlyLocalNonAgentSessionsExposeDesktop(){
        WorkspaceStore.Session s=new WorkspaceStore.Session();
        for(String env:new String[]{"termux","ubuntu","legacy","proot:debian","proot:jobs-demo"}){s.environment=env;assertTrue(env,WorkspaceDesktop.eligible(s));}
        for(String env:new String[]{"codex","ssh","unknown"}){s.environment=env;assertFalse(env,WorkspaceDesktop.eligible(s));}
        s.environment="ubuntu";s.claudeAgent=true;assertFalse(WorkspaceDesktop.eligible(s));s.claudeAgent=false;s.geminiAgent=true;assertFalse(WorkspaceDesktop.eligible(s));s.geminiAgent=false;s.aiAgent="opencode";assertFalse(WorkspaceDesktop.eligible(s));assertFalse(WorkspaceDesktop.eligible(null));
    }
    @Test public void desktopAndFileCommandsKeepTheExplicitEnvironment(){
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="termux";
        assertEquals("termux",WorkspaceDesktop.target(s));
        s.prootAlias="jobs-x11-ubuntu";
        assertEquals("jobs-x11-ubuntu",WorkspaceDesktop.target(s));
        assertEquals("proot:jobs-x11-ubuntu",ProjectFiles.environment(s));
        String command=ProjectFiles.command(s,"printf test");assertTrue(command,command.contains("'jobs-x11-ubuntu'"));assertFalse(command,command.contains("login ubuntu "));
        s.prootAlias="debian";assertEquals("debian",WorkspaceDesktop.target(s));
        s.prootAlias="";s.environment="ubuntu";assertEquals("ubuntu",WorkspaceDesktop.target(s));
        s.aiAgent="opencode";assertEquals("",WorkspaceDesktop.target(s));
    }
}
