package com.termux.app.workspace;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class CodexSessionShellTest {
    private String readError(Process process) throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        int b; while ((b = process.getErrorStream().read()) != -1) bytes.write(b);
        return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
    }
    @Test public void accessModesAndChatChoicesAreExplicit() {
        WorkspaceStore.Session s = new WorkspaceStore.Session();
        String c = CodexSessionShell.invocation(s, "/tmp/socket");
        assertTrue(c.contains("--sandbox danger-full-access --ask-for-approval never")); assertFalse(c.contains("resume")); assertTrue(c.startsWith("codex --remote ")); assertFalse(c.contains("--no-daemon"));
        s.codexAccess = "project"; s.codexChat = "id"; s.codexThread = "thread-123";
        c = CodexSessionShell.invocation(s, "/tmp/socket");
        assertTrue(c.contains("resume 'thread-123'")); assertFalse(c.contains("--sandbox")); assertFalse(c.contains("--ask-for-approval"));
        s.codexAccess = "read"; s.codexChat = "id"; s.codexThread = "thread-123";
        c = CodexSessionShell.invocation(s, "/tmp/socket");
        assertTrue(c.contains("resume 'thread-123'")); assertFalse(c.contains("--sandbox")); assertFalse(c.contains("--ask-for-approval")); assertFalse(c.contains("--last"));
        assertFalse(CodexSessionShell.validThread("x'; touch /tmp/bad"));
    }
    @Test public void ubuntuMountsPrivateSocketAndDoesNotChangeOrdinaryStartup() throws Exception {
        File files = Files.createTempDirectory("codex-profile").toFile();
        WorkspaceStore.Session s = new WorkspaceStore.Session(); s.id = "fixture"; s.codexSocketId = "fixture"; s.environment = "codex"; s.directory = "/root/a project";
        File rc = SessionShell.prepare(files, s);
        String content = new String(Files.readAllBytes(rc.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("--bind '" + CodexSessionShell.socketDirectory(files) + ":/tmp/termux-jobs-codex'"));
        String guest = new String(Files.readAllBytes(new File(SessionShell.directory(files,s.id), "ubuntu-bashrc").toPath()), StandardCharsets.UTF_8);
        assertTrue(guest.contains("app-server --listen")); assertTrue(guest.contains("codex --no-daemon -c")); assertTrue(guest.contains("unix:///tmp/termux-jobs-codex/"));
        assertFalse(guest.contains("ws://")); assertFalse(guest.contains("auth.json"));
        s.environment = "termux"; s.startup = "printf ordinary";
        content = new String(Files.readAllBytes(SessionShell.prepare(files,s).toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("printf ordinary")); assertFalse(content.contains("app-server"));
    }
    @Test public void proxyReportsMissingBridgeAndSocketWithoutStartingCodex() throws Exception {
        File files = Files.createTempDirectory("codex-proxy").toFile();
        WorkspaceStore.Session s = new WorkspaceStore.Session(); s.codexEnvironment = "termux";
        Process missing = new ProcessBuilder("/bin/bash", "-c", CodexSessionShell.proxyCommand(files,s)).start();
        assertTrue(missing.waitFor(5,TimeUnit.SECONDS)); assertEquals(2,missing.exitValue());
        String error = readError(missing);
        assertTrue(error.contains("JOBS_CONTROL:NO_BRIDGE"));
        CodexSessionShell.socketDirectory(files).mkdirs();
        Files.write(new File(CodexSessionShell.socketDirectory(files),"bridge-v0.2.21.js").toPath(),new byte[0]);
        Process socket = new ProcessBuilder("/bin/bash", "-c", CodexSessionShell.proxyCommand(files,s)).start();
        assertTrue(socket.waitFor(5,TimeUnit.SECONDS)); assertEquals(3,socket.exitValue());
        assertTrue(readError(socket).contains("JOBS_CONTROL:NO_SOCKET"));
        assertEquals("",CodexRpc.classify("private auth payload not echoed"));
        assertTrue(CodexRpc.classify("connect EACCES private-path").contains("Отказ"));
    }
    @Test public void launcherIsValidBashAndMissingCodexFailsInsteadOfEscalating() throws Exception {
        File files = Files.createTempDirectory("codex-launcher").toFile(); WorkspaceStore.Session s = new WorkspaceStore.Session(); s.id = "fixture"; s.codexSocketId = "fixture"; s.codexEnvironment = "termux";
        File script = new File(files,"launch.sh"); Files.write(script.toPath(), CodexSessionShell.command(files,s).getBytes(StandardCharsets.UTF_8));
        Process syntax = new ProcessBuilder("/bin/bash","-n",script.getAbsolutePath()).start(); assertTrue(syntax.waitFor(5,TimeUnit.SECONDS)); assertEquals(0,syntax.exitValue());
        ProcessBuilder noCodex = new ProcessBuilder("/bin/bash",script.getAbsolutePath()); noCodex.environment().put("PATH", files.getAbsolutePath());
        Process failure = noCodex.start(); assertTrue(failure.waitFor(5,TimeUnit.SECONDS)); assertEquals(127,failure.exitValue());
    }
}
