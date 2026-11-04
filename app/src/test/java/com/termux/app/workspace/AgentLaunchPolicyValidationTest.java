package com.termux.app.workspace;

import java.io.File;
import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;

/** Unsupported stored/in-memory policies must never generate a weaker launch. */
public class AgentLaunchPolicyValidationTest {
    private interface Launch { void run() throws Exception; }
    private void rejects(Launch launch) throws Exception {
        try { launch.run(); fail("Unsupported policy accepted"); }
        catch (IllegalArgumentException expected) { assertFalse(expected.getMessage().isEmpty()); }
    }
    @Test public void unsupportedPoliciesFailBeforeLaunch() throws Exception {
        File files=Files.createTempDirectory("policy-validation").toFile();
        WorkspaceStore.Session codex=WorkspaceStore.newProfile();codex.codexAccess="unsupported";
        rejects(()->CodexSessionShell.command(files,codex));
        WorkspaceStore.Session claude=WorkspaceStore.newProfile();claude.environment="ubuntu";claude.claudeAgent=true;claude.claudeMode="unsupported";
        rejects(()->ClaudeCode.command(files,claude));
        WorkspaceStore.Session gemini=WorkspaceStore.newProfile();gemini.environment="ubuntu";gemini.geminiAgent=true;gemini.geminiMode="unsupported";
        rejects(()->GeminiCli.command(files,gemini));
        WorkspaceStore.Session open=WorkspaceStore.newProfile();open.environment="ubuntu";open.aiAgent="opencode";open.aiAccess="unsupported";
        rejects(()->AiAgents.command(files,open));
        open.aiAccess="ask";open.aiMode="unsupported";rejects(()->AiAgents.command(files,open));
        assertEquals("unsupported",codex.codexAccess);assertEquals("unsupported",claude.claudeMode);
        assertEquals("unsupported",gemini.geminiMode);assertEquals("ask",open.aiAccess);
    }
}
