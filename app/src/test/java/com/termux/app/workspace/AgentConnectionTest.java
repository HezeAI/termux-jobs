package com.termux.app.workspace;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class AgentConnectionTest {
    private WorkspaceStore.Session profile(){WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexEnvironment="ubuntu-ai";return p;}
    @Test public void emptySettingLeavesAllAgentCommandsByteForByteUnchanged(){
        for(String agent:new String[]{"codex","gemini","claude","opencode"}){
            WorkspaceStore.Session p=profile();if(!agent.equals("codex"))p.environment="ubuntu";p.geminiAgent=agent.equals("gemini");p.claudeAgent=agent.equals("claude");if(agent.equals("opencode"))p.aiAgent="opencode";
            assertEquals("original --args 'kept'",AgentConnection.wrap(p,"original --args 'kept'"));
        }
    }
    @Test public void regularTerminalsAndEnvironmentsNeverUseAgentWrapper(){
        for(String environment:new String[]{"termux","ubuntu","ssh"}){
            WorkspaceStore.Session p=profile();p.environment=environment;p.agentConnectionCommand="/nonexistent/wrapper";
            assertEquals("original",AgentConnection.wrap(p,"original"));
        }
    }
    @Test public void wrapperReceivesOriginalCommandAndEnvironmentStaysInChild()throws Exception{
        File dir=Files.createTempDirectory("agent-connection").toFile(),wrapper=new File(dir,"launcher's file"),result=new File(dir,"result");
        Files.write(wrapper.toPath(),"#!/bin/sh\nexport HTTPS_PROXY=http://127.0.0.1:19999\nexec \"$@\"\n".getBytes(StandardCharsets.UTF_8));assertTrue(wrapper.setExecutable(true));
        WorkspaceStore.Session p=profile();p.agentConnectionCommand=wrapper.getAbsolutePath();String previous=System.getenv("HTTPS_PROXY");
        Process child=new ProcessBuilder("/bin/bash","-c",AgentConnection.wrap(p,"printf '%s|%s' \"$HTTPS_PROXY\" 'argument with spaces' > "+SessionShell.quote(result.getAbsolutePath()))).start();
        assertTrue(child.waitFor(5,TimeUnit.SECONDS));assertEquals(0,child.exitValue());assertEquals("http://127.0.0.1:19999|argument with spaces",new String(Files.readAllBytes(result.toPath()),StandardCharsets.UTF_8));assertEquals(previous,System.getenv("HTTPS_PROXY"));
    }
    @Test public void missingOrFailedWrapperNeverFallsBackToDirect()throws Exception{
        File dir=Files.createTempDirectory("agent-connection-fail").toFile(),result=new File(dir,"must-not-exist");WorkspaceStore.Session p=profile();p.agentConnectionCommand=new File(dir,"missing; touch unexpected").getAbsolutePath();
        String original="touch "+SessionShell.quote(result.getAbsolutePath());Process child=new ProcessBuilder("/bin/bash","-c",AgentConnection.wrap(p,original)).start();assertTrue(child.waitFor(5,TimeUnit.SECONDS));assertEquals(126,child.exitValue());assertFalse(result.exists());
        File wrapper=new File(dir,"fail");Files.write(wrapper.toPath(),"#!/bin/sh\nexit 37\n".getBytes(StandardCharsets.UTF_8));wrapper.setExecutable(true);p.agentConnectionCommand=wrapper.getAbsolutePath();
        child=new ProcessBuilder("/bin/bash","-c",AgentConnection.wrap(p,original)).start();assertTrue(child.waitFor(5,TimeUnit.SECONDS));assertEquals(37,child.exitValue());assertFalse(result.exists());
    }
    @Test public void pathsRejectControlCharactersAndRelativeCommands(){
        assertTrue(AgentConnection.valid(""));assertTrue(AgentConnection.valid("/root/launcher with spaces"));
        for(String bad:new String[]{"launcher","~/launcher","/root/start\nother","/root/start\u0000","/root/start\r"})assertFalse(bad,AgentConnection.valid(bad));
    }
    @Test public void profileStartupIncludesWrapperOnlyInItsSelectedGuest()throws Exception{
        File files=Files.createTempDirectory("ac").toFile();WorkspaceStore.Session p=profile();p.id="fixture";p.codexSocketId="fixture";p.agentConnectionCommand="/root/launcher";p.codexChat="id";p.codexThread="preserved-thread";
        SessionShell.prepare(files,p);String guest=new String(Files.readAllBytes(new File(SessionShell.directory(files,p.id),"ubuntu-bashrc").toPath()),StandardCharsets.UTF_8);
        assertTrue(guest.contains("/root/launcher"));assertTrue(guest.contains("preserved-thread"));assertTrue(guest.contains("--no-daemon"));
        String host=new String(Files.readAllBytes(new File(SessionShell.directory(files,p.id),"bashrc").toPath()),StandardCharsets.UTF_8);assertTrue(host.contains("'ubuntu-ai'"));assertFalse(host.contains("HTTPS_PROXY"));assertFalse(guest.contains("203.0.113.188"));
    }
}
