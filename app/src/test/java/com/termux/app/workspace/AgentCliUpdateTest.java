package com.termux.app.workspace;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentCliUpdateTest {
    private static final String AVAILABLE = "CURRENT:0.163.0\nLATEST:0.171.0\nRESULT:available\n";

    @Test public void onlySupportedUbuntuAiAgentsAreEligibleWithoutALiveSession() {
        WorkspaceStore.Session profile = new WorkspaceStore.Session();
        profile.environment = "codex";
        assertNull(AgentCliUpdate.capture(profile));
        profile.codexEnvironment = "termux";
        assertNull(AgentCliUpdate.capture(profile));
        profile.codexEnvironment = "ubuntu-ai";
        AgentCliUpdate.Target codex = AgentCliUpdate.capture(profile);
        assertNotNull(codex);
        assertEquals("", profile.handle);
        assertEquals(AgentCliUpdate.Agent.CODEX, codex.agent);
        assertTrue(codex.matches(profile));
        assertFalse(codex.matches(profile.copy()));

        profile.environment = "ubuntu";
        profile.claudeAgent = true;
        AgentCliUpdate.Target claude = AgentCliUpdate.capture(profile);
        assertNotNull(claude);
        assertEquals(AgentCliUpdate.Agent.CLAUDE, claude.agent);
        assertFalse(codex.matches(profile));
        profile.geminiAgent = true;
        assertNull(AgentCliUpdate.capture(profile));
        assertFalse(claude.matches(profile));
        profile.geminiAuth = GeminiCli.ANTIGRAVITY;
        AgentCliUpdate.Target antigravity = AgentCliUpdate.capture(profile);
        assertNotNull(antigravity);
        assertEquals(AgentCliUpdate.Agent.ANTIGRAVITY, antigravity.agent);
        assertTrue(antigravity.sameRun(AgentCliUpdate.capture(profile)));
        profile.geminiSocketId = java.util.UUID.randomUUID().toString();
        assertFalse(antigravity.sameRun(AgentCliUpdate.capture(profile)));
        profile.geminiAgent = false;
        profile.claudeAgent = false;
        profile.aiAgent = "opencode";
        assertNull(AgentCliUpdate.capture(profile));
        profile.environment = "ssh";
        profile.claudeAgent = true;
        assertNull(AgentCliUpdate.capture(profile));
    }

    @Test public void invokesFixedUbuntuAliasAndKeepsPathsAsSingleArguments() {
        File files = new File("/app files");
        List<String> check = AgentCliUpdate.command(files, AgentCliUpdate.Agent.CODEX, null);
        int bindIndex = check.indexOf("--bind");
        assertEquals(Arrays.asList("--bind", "/app files/.agent-preparation:/tmp/termux-jobs-setup",
            "ubuntu-ai", "--", "/usr/bin/env", "JOBS_AGENT_UBUNTU=ubuntu-ai", "/bin/bash",
            "--noprofile", "--norc", "/tmp/termux-jobs-setup/agent-cli-update.sh", "check", "codex"),
            check.subList(bindIndex, check.size()));
        assertEquals("--foreground", check.get(1));
        assertEquals("60", check.get(4));
        List<String> install = AgentCliUpdate.command(files, AgentCliUpdate.Agent.CLAUDE, "2.1.290");
        assertEquals("720", install.get(4));
        assertEquals(Arrays.asList("install", "claude", "2.1.290"), install.subList(install.size() - 3, install.size()));
        assertThrows(IllegalArgumentException.class,
            () -> AgentCliUpdate.command(files, AgentCliUpdate.Agent.CODEX, "1.2.3; another-command"));
    }

    @Test public void checksVersionsAndRequiresTheConfirmedVersionForAnInstallResult() {
        AgentCliUpdate.Result check = AgentCliUpdate.parse(AVAILABLE, 0, null);
        assertEquals(AgentCliUpdate.Status.AVAILABLE, check.status);
        assertEquals("0.163.0", check.current);
        assertEquals("0.171.0", check.latest);
        assertEquals(AgentCliUpdate.Status.AVAILABLE,
            AgentCliUpdate.parse(AVAILABLE.replace("0.163.0", "none"), 0, null).status);
        assertEquals(AgentCliUpdate.Status.CURRENT, AgentCliUpdate.parse(
            "CURRENT:0.172.0\nLATEST:0.171.0\nRESULT:current\n", 0, null).status);
        String installed = "CURRENT:0.171.0\nLATEST:0.171.0\nRESULT:installed\n";
        assertEquals(AgentCliUpdate.Status.INSTALLED, AgentCliUpdate.parse(installed, 0, "0.171.0").status);
        assertEquals(AgentCliUpdate.Error.PROTOCOL, AgentCliUpdate.parse(installed, 0, "0.171.1").error);
        assertEquals(AgentCliUpdate.Error.PROTOCOL, AgentCliUpdate.parse(installed, 0, null).error);
        assertEquals(AgentCliUpdate.Error.PROTOCOL, AgentCliUpdate.parse(AVAILABLE, 0, "0.171.0").error);
    }

    @Test public void untrustedOutputNeverBecomesUserFacingResultData() throws Exception {
        String privateLine = "TOKEN=artificial-fixture\n";
        for (String invalid : Arrays.asList(privateLine + AVAILABLE, AVAILABLE + privateLine,
                AVAILABLE.substring(0, AVAILABLE.length() - 1), AVAILABLE.replace("0.163.0", "0.01.1"),
                AVAILABLE.replace("0.163.0", "0.172.0"),
                AVAILABLE.replace("0.171.0", "0.163.0"), "ERROR:REGISTRY\n")) {
            AgentCliUpdate.Result failed = AgentCliUpdate.parse(invalid, 0, null);
            assertEquals(AgentCliUpdate.Error.PROTOCOL, failed.error);
            assertEquals("", failed.current);
            assertEquals("", failed.latest);
        }
        assertEquals(AgentCliUpdate.Error.TRANSPORT, AgentCliUpdate.parse(AVAILABLE, 1, null).error);
        assertEquals(AgentCliUpdate.Error.REGISTRY, AgentCliUpdate.parse("ERROR:REGISTRY\n", 1, null).error);
        assertEquals(AgentCliUpdate.Error.TRANSPORT, AgentCliUpdate.parse("ERROR:" + privateLine, 1, null).error);
        assertEquals(AgentCliUpdate.Error.TIMEOUT, AgentCliUpdate.parse("", 124, null).error);

        ByteArrayInputStream large = new ByteArrayInputStream(new byte[10000]);
        assertNull(AgentCliUpdate.readOutput(large));
        assertEquals(0, large.available());
        assertEquals(AVAILABLE, AgentCliUpdate.readOutput(
            new ByteArrayInputStream(AVAILABLE.getBytes(StandardCharsets.US_ASCII))));
    }
}
