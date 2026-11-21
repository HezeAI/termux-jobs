package com.termux.app.workspace;

import android.content.Context;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** A fixed protocol between the Android dialog and the Ubuntu AI updater. */
final class AgentCliUpdate {
    private static final String ENVIRONMENT = "ubuntu-ai";
    private static final int MAX_OUTPUT = 512;
    private static final String NUMBER = "(?:0|[1-9][0-9]*)";
    private static final Pattern VERSION = Pattern.compile(
        NUMBER + "\\." + NUMBER + "\\." + NUMBER
        + "(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?"
        + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?");
    private static final Pattern STABLE_VERSION = Pattern.compile("(?:0|[1-9][0-9]{0,8})(?:\\.(?:0|[1-9][0-9]{0,8})){2}");

    enum Agent {
        CODEX("codex", "Codex"), CLAUDE("claude", "Claude Code"),
        ANTIGRAVITY("antigravity", "Antigravity CLI");

        final String argument, label;

        Agent(String argument, String label) {
            this.argument = argument;
            this.label = label;
        }
    }

    static final class Target {
        final String profileId;
        final Agent agent;
        final String runId;

        private Target(String profileId, Agent agent, String runId) {
            this.profileId = profileId;
            this.agent = agent;
            this.runId = runId;
        }

        boolean matches(WorkspaceStore.Session profile) {
            return profile != null && profileId.equals(profile.id) && agent == agentFor(profile);
        }

        boolean sameRun(Target other) {
            return other != null && profileId.equals(other.profileId) && agent == other.agent
                && Objects.equals(runId, other.runId);
        }
    }

    static Target capture(WorkspaceStore.Session profile) {
        Agent agent = agentFor(profile);
        if (agent == null) return null;
        String runId = agent == Agent.CODEX ? profile.codexSocketId
            : agent == Agent.CLAUDE ? profile.claudeSocketId : profile.geminiSocketId;
        return new Target(profile.id, agent, runId);
    }

    private static Agent agentFor(WorkspaceStore.Session profile) {
        if (profile == null || !ENVIRONMENT.equals(profile.codexEnvironment)
            || (!"codex".equals(profile.environment) && !"ubuntu".equals(profile.environment))) return null;
        switch (AgentPreparation.agent(profile)) {
            case "codex": return Agent.CODEX;
            case "claude": return Agent.CLAUDE;
            case "gemini": return GeminiCli.antigravity(profile) ? Agent.ANTIGRAVITY : null;
            default: return null;
        }
    }

    enum Status { AVAILABLE, CURRENT, INSTALLED, FAILED }

    enum Error {
        UNSUPPORTED_ENVIRONMENT, DEPENDENCIES, REGISTRY, PACKAGE, VERSION_CHANGED,
        BUSY, INSTALL, VERIFY, STATE, TRANSPORT, TIMEOUT, PROTOCOL
    }

    static final class Result {
        final Status status;
        final String current, latest;
        final Error error;

        private Result(Status status, String current, String latest, Error error) {
            this.status = status;
            this.current = current;
            this.latest = latest;
            this.error = error;
        }

        static Result failed(Error error) {
            return new Result(Status.FAILED, "", "", error);
        }
    }

    static String environmentLabel() {
        return AgentEnvironments.label(ENVIRONMENT);
    }

    static boolean version(String value) {
        return value.length() <= 64 && VERSION.matcher(value).matches();
    }

    static boolean newerStableVersion(String latest, String current) {
        if (!STABLE_VERSION.matcher(latest).matches() || !STABLE_VERSION.matcher(current).matches()) return false;
        String[] proposed = latest.split("\\.");
        String[] installed = current.split("\\.");
        for (int i = 0; i < proposed.length; i++) {
            int delta = Integer.compare(Integer.parseInt(proposed[i]), Integer.parseInt(installed[i]));
            if (delta != 0) return delta > 0;
        }
        return false;
    }

    static List<String> command(File files, Agent agent, String expectedVersion) {
        if (expectedVersion != null && !version(expectedVersion))
            throw new IllegalArgumentException("Invalid CLI update version");
        String bin = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/";
        // The foreground timeout only owns the command it starts. The guest bounds its own work.
        String deadline = expectedVersion == null ? "60" : "720";
        List<String> args = new ArrayList<>(Arrays.asList(
            bin + "timeout", "--foreground", "-k", "5", deadline, bin + "bash", "--noprofile", "--norc",
            "-c", "exec \"$@\"", "agent-cli-update",
            bin + "proot-distro", "login", "--bind",
            AgentPreparation.directory(files).getAbsolutePath() + ":" + AgentPreparation.GUEST,
            ENVIRONMENT, "--", "/usr/bin/env", "JOBS_AGENT_UBUNTU=" + ENVIRONMENT,
            "/bin/bash", "--noprofile", "--norc", AgentPreparation.GUEST + "/agent-cli-update.sh",
            expectedVersion == null ? "check" : "install", agent.argument));
        if (expectedVersion != null) args.add(expectedVersion);
        return Collections.unmodifiableList(args);
    }

    static Result run(Context context, Agent agent, String expectedVersion) {
        return run(context, agent, expectedVersion, true);
    }

    static Result checkPrepared(Context context, Agent agent, String ignored) {
        return run(context, agent, null, false);
    }

    private static Result run(Context context, Agent agent, String expectedVersion, boolean prepare) {
        try {
            if (prepare) AgentPreparation.prepare(context);
            String home = TermuxConstants.TERMUX_HOME_DIR_PATH;
            File discard = new File("/dev/null");
            ProcessBuilder builder = new ProcessBuilder(command(context.getFilesDir(), agent, expectedVersion))
                .directory(new File(home)).redirectInput(discard).redirectError(discard);
            builder.environment().clear();
            for (String item : TermuxShellUtils.buildEnvironment(context, false, home)) {
                int separator = item.indexOf('=');
                if (separator > 0) builder.environment().put(item.substring(0, separator), item.substring(separator + 1));
            }
            builder.environment().put("PD_FORCE_NO_COLORS", "1");
            Process process = builder.start();
            String output;
            int exit;
            try (InputStream input = process.getInputStream()) {
                output = readOutput(input);
            } finally {
                exit = waitFor(process);
            }
            return parse(output, exit, expectedVersion);
        } catch (IOException | RuntimeException failure) {
            return Result.failed(Error.TRANSPORT);
        }
    }

    private static int waitFor(Process process) {
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    return process.waitFor();
                } catch (InterruptedException interruption) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    /** Drain stdout through EOF while keeping only one small protocol response in memory. */
    static String readOutput(InputStream input) throws IOException {
        byte[] buffer = new byte[MAX_OUTPUT];
        int used = 0;
        try {
            while (used < buffer.length) {
                int read = input.read(buffer, used, buffer.length - used);
                if (read < 0) return new String(buffer, 0, used, StandardCharsets.US_ASCII);
                used += read;
            }
            if (input.read() == -1) return new String(buffer, StandardCharsets.US_ASCII);
            while (input.read(buffer) != -1) {
                // Continue draining so malformed output cannot block the process.
            }
            return null;
        } finally {
            Arrays.fill(buffer, (byte) 0);
        }
    }

    static Result parse(String output, int exit, String expectedVersion) {
        if (exit == 124) return Result.failed(Error.TIMEOUT);
        if (output == null || output.length() > MAX_OUTPUT || !output.endsWith("\n"))
            return Result.failed(exit == 0 ? Error.PROTOCOL : Error.TRANSPORT);
        String[] lines = output.substring(0, output.length() - 1).split("\n", -1);
        if (exit != 0) {
            if (lines.length == 1 && lines[0].startsWith("ERROR:")) {
                switch (lines[0].substring(6)) {
                    case "UNSUPPORTED_ENVIRONMENT": return Result.failed(Error.UNSUPPORTED_ENVIRONMENT);
                    case "DEPENDENCIES": return Result.failed(Error.DEPENDENCIES);
                    case "REGISTRY": return Result.failed(Error.REGISTRY);
                    case "PACKAGE": return Result.failed(Error.PACKAGE);
                    case "VERSION_CHANGED": return Result.failed(Error.VERSION_CHANGED);
                    case "BUSY": return Result.failed(Error.BUSY);
                    case "INSTALL": return Result.failed(Error.INSTALL);
                    case "VERIFY": return Result.failed(Error.VERIFY);
                    case "STATE": return Result.failed(Error.STATE);
                    default: break;
                }
            }
            return Result.failed(Error.TRANSPORT);
        }
        if (lines.length == 3 && lines[0].startsWith("CURRENT:") && lines[1].startsWith("LATEST:")) {
            String current = lines[0].substring(8);
            String latest = lines[1].substring(7);
            if (version(latest) && ("none".equals(current) || version(current))) {
                if (expectedVersion == null) {
                    if ("RESULT:available".equals(lines[2])
                        && ("none".equals(current) || newerStableVersion(latest, current)))
                        return new Result(Status.AVAILABLE, current, latest, null);
                    if ("RESULT:current".equals(lines[2]) && !"none".equals(current))
                        return new Result(Status.CURRENT, current, latest, null);
                } else if ("RESULT:installed".equals(lines[2]) && expectedVersion.equals(current)
                    && expectedVersion.equals(latest)) {
                    return new Result(Status.INSTALLED, current, latest, null);
                }
            }
        }
        return Result.failed(Error.PROTOCOL);
    }
}
