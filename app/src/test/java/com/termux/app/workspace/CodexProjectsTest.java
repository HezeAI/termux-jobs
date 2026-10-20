package com.termux.app.workspace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE)
public class CodexProjectsTest {
    @Test public void directoriesMatchOnlyWithinSelectedEnvironment() {
        assertTrue(CodexProjects.matches("ubuntu","~/projects/demo/","/root/projects/demo"));
        assertTrue(CodexProjects.matches("ubuntu","/root/projects/../projects/demo","/root/projects/demo/"));
        assertFalse(CodexProjects.matches("termux","~/projects/demo","/root/projects/demo"));
        assertFalse(CodexProjects.matches("ubuntu","/root/projects/a","/root/projects/ab"));
        assertFalse(CodexProjects.matches("ubuntu","relative","relative"));
    }
    @Test public void newChatBindsToOwnDirectoryWithoutImplicitSave() {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="codex";s.codexEnvironment="ubuntu";s.directory="/root/projects/demo";
        CodexStatus status=new CodexStatus();status.thread="thread-new";status.cwd=s.directory;status.title="Работа над проектом";
        assertTrue(CodexProjects.bind(s,status));assertEquals("id",s.codexChat);assertEquals("thread-new",s.codexThread);assertEquals(status.title,s.codexThreadName);assertEquals(status.cwd,s.codexSourceDirectory);assertFalse(s.saved);
        status.thread="other-thread";assertFalse(CodexProjects.bind(s,status));assertEquals("thread-new",s.codexThread);
    }
    @Test public void wrongProjectMissingDirectoryOrErrorCannotReplaceAssociation() {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="codex";s.directory="/root/projects/demo";
        CodexStatus status=new CodexStatus();status.thread="thread-new";status.cwd="/root/projects/other";
        assertFalse(CodexProjects.bind(s,status));status.cwd="";assertFalse(CodexProjects.bind(s,status));status.cwd=s.directory;status.error="disconnected";assertFalse(CodexProjects.bind(s,status));assertEquals("new",s.codexChat);
    }
    @Test public void baseTerminalRecognizedByReservedIdNotByName() {
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.name="Terminal";assertFalse(WorkspaceStore.isBase(s));s.id=WorkspaceStore.BASE_TERMINAL_ID;assertTrue(WorkspaceStore.isBase(s));assertFalse(WorkspaceStore.isBase(null));
    }
    @Test public void freshUnwrittenThreadCannotTurnLaunchPresetIntoBrokenResume(){
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexEnvironment="ubuntu";p.directory="/root/project";
        CodexStatus status=new CodexStatus();status.thread="fresh-thread";status.cwd=p.directory;status.materialized=false;
        assertFalse(CodexProjects.bind(p,status));assertEquals("new",p.codexChat);status.materialized=true;assertTrue(CodexProjects.bind(p,status));assertEquals("id",p.codexChat);
    }
    @Test public void explicitNextChatSurvivesOldRunPollingUntilDifferentRunStarts(){
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.directory="/root/projects/EXE-TG-PROXY";
        CodexStatus old=new CodexStatus();old.thread="old-thread";old.cwd=p.directory;old.materialized=true;
        p.codexBindingBlockedRun=p.codexSocketId;
        assertFalse(CodexProjects.bind(p,old));assertFalse(CodexProjects.bind(p,old));assertEquals("new",p.codexChat);assertEquals("",p.codexThread);
        WorkspaceStore.Session copy=p.copy();assertTrue(CodexProjects.bind(copy,old));
        p.codexSocketId=java.util.UUID.randomUUID().toString();
        CodexStatus fresh=new CodexStatus();fresh.thread="new-thread";fresh.cwd=p.directory;fresh.materialized=false;
        assertFalse(CodexProjects.bind(p,fresh));fresh.materialized=true;
        assertTrue(CodexProjects.bind(p,fresh));assertEquals("new-thread",p.codexThread);
    }

}
