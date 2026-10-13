package com.termux.app.workspace;

import com.termux.shared.termux.TermuxConstants;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;

/** Explicit Bash sessions with per-profile history, never terminal keystroke logging. */
public final class SessionShell {
    private SessionShell() {}
    public static String quote(String value) { return "'" + value.replace("'", "'\\''") + "'"; }
    public static File directory(File files, String id) { return new File(new File(files, "workspace-sessions"), id); }
    public static File prepare(File files, WorkspaceStore.Session session) throws IOException {
        return prepare(files, session, null, null);
    }
    public static File prepare(File files, WorkspaceStore.Session session, String askpass, String socket) throws IOException {
        File dir = directory(files, session.id);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create session directory");
        File history = new File(dir, "history");
        if (!history.exists()) write(history, new byte[0]);
        File rc = new File(dir, "bashrc");
        File cancel = new File(dir, "cancel-startup");
        if (cancel.exists() && !cancel.delete()) throw new IOException("Cannot reset startup cancellation");
        String script;
        java.util.List<String> startup = session.startupCommands();
        boolean guest = session.environment.equals("ubuntu") || (session.environment.equals("codex") && session.codexEnvironment.equals("ubuntu"));
        if (session.environment.equals("codex")) startup = java.util.Collections.singletonList(CodexSessionShell.command(files, session));
        if (guest) {
            String mount = "/tmp/termux-fork-session-" + session.id;
            File guestRc = new File(dir, "ubuntu-bashrc");
            write(guestRc, buildReadyRc("$HOME", mount + "/history", mount + "/cancel-startup", startup, true).getBytes(StandardCharsets.UTF_8));
            String cwd = session.directory.isEmpty() || session.directory.equals("~") ? "/root" : session.directory;
            if (cwd.startsWith("~/")) cwd = "/root/" + cwd.substring(2);
            script = buildRc(TermuxConstants.TERMUX_HOME_DIR_PATH, history.getAbsolutePath(), "")
                + "printf '%s\\n' 'Ожидание оболочки Ubuntu…'\n"
                + "proot-distro login --bind " + quote(dir.getAbsolutePath() + ":" + mount)
                + (session.environment.equals("codex") ? " --bind " + quote(CodexSessionShell.socketDirectory(files).getAbsolutePath() + ":/tmp/termux-jobs-codex") : "")
                + " --work-dir " + quote(cwd) + " ubuntu -- /bin/bash --rcfile " + quote(mount + "/ubuntu-bashrc") + " -i\n";
        } else if (session.environment.equals("ssh")) {
            script = buildReadyRc(TermuxConstants.TERMUX_HOME_DIR_PATH, history.getAbsolutePath(), cancel.getAbsolutePath(),
                java.util.Collections.singletonList((session.sshAuth.equals("password") && askpass != null && socket != null
                    ? "LC_ALL=C SSH_ASKPASS=" + quote(askpass) + " SSH_ASKPASS_REQUIRE=force TERMUX_JOBS_SSH_SOCKET=" + quote(socket) + " " : "")
                    + SshSessionShell.command(session)), false);
        } else if ((session.environment.equals("termux") || session.environment.equals("codex"))) {
            script = buildReadyRc(TermuxConstants.TERMUX_HOME_DIR_PATH, history.getAbsolutePath(), cancel.getAbsolutePath(), startup, false);
        } else script = buildRc(TermuxConstants.TERMUX_HOME_DIR_PATH, history.getAbsolutePath(), session.startupCommands());
        write(rc, script.getBytes(StandardCharsets.UTF_8));
        return rc;
    }
    public static File prepareAskpass(File files, WorkspaceStore.Session session, String apk) throws IOException {
        File dir = directory(files, session.id);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create session directory");
        File helper = new File(dir, "ssh-askpass");
        write(helper, ("#!/system/bin/sh\nexport CLASSPATH=" + quote(apk)
            + "\nexec /system/bin/app_process /system/bin com.termux.app.workspace.SshAskpassMain \"$@\"\n").getBytes(StandardCharsets.UTF_8));
        if (!helper.setReadable(false, false) || !helper.setWritable(false, false) || !helper.setExecutable(false, false)
            || !helper.setReadable(true, true) || !helper.setWritable(true, true) || !helper.setExecutable(true, true))
            throw new IOException("Cannot protect SSH helper");
        return helper;
    }
    public static String buildRc(String home, String history, String startup) {
        return "if [ -f " + quote(home + "/.bashrc") + " ]; then . " + quote(home + "/.bashrc") + "; fi\n"
            + "HISTFILE=" + quote(history) + "\nHISTSIZE=2000\nHISTFILESIZE=2000\nshopt -s histappend\n"
            + "history -c\nhistory -r \"$HISTFILE\"\nPROMPT_COMMAND+=('history -a')\n"
            + (startup.trim().isEmpty() ? "" : "history -s -- " + quote(startup) + "\nhistory -a\n" + startup + "\n");
    }
    public static String buildRc(String home, String history, java.util.List<String> commands) {
        if (commands.size() <= 1) return buildRc(home, history, commands.isEmpty() ? "" : commands.get(0));
        StringBuilder script = new StringBuilder(buildRc(home, history, ""));
        script.append("_termux_workspace_startup() {\n");
        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);
            script.append("history -s -- ").append(quote(command)).append("\nhistory -a\n");
            script.append("eval -- ").append(quote(command)).append("\n");
            script.append("local _termux_workspace_status=$?\n");
            script.append("if [ \"$_termux_workspace_status\" -ne 0 ]; then printf '%s\\n' ")
                .append(quote("Команда " + (i + 1) + " завершилась с ошибкой; запуск остановлен."))
                .append(" >&2; return \"$_termux_workspace_status\"; fi\n");
        }
        script.append("}\n_termux_workspace_startup\nunset -f _termux_workspace_startup\n");
        return script.toString();
    }
    /** Runs only at Bash's first prompt, after rc initialization. No prompt-text heuristics or timed input. */
    public static String buildReadyRc(String home, String history, String cancel, java.util.List<String> commands, boolean guest) {
        String base = buildRc(home, history, "");
        if (guest) base = base.replace(quote(home + "/.bashrc"), "\"$HOME/.bashrc\"");
        if (commands.isEmpty()) return base;
        StringBuilder script = new StringBuilder(base);
        script.append("_termux_workspace_ready() {\n")
            .append("[ \"${_termux_workspace_done:-0}\" = 1 ] && return 0\n_termux_workspace_done=1\n");
        for (int i = 0; i < commands.size(); i++) {
            script.append("if [ -f ").append(quote(cancel)).append(" ]; then printf '%s\\n' 'Запуск команд отменён.'; return 0; fi\n");
            script.append("printf '%s\\n' ").append(quote("Оболочка готова. Запуск команды " + (i + 1) + "/" + commands.size())).append("\n");
            script.append("history -s -- ").append(quote(commands.get(i))).append("\nhistory -a\n");
            script.append("eval -- ").append(quote(commands.get(i))).append("\nlocal _termux_workspace_status=$?\n")
                .append("if [ \"$_termux_workspace_status\" -ne 0 ]; then printf '%s\\n' ")
                .append(quote("Ошибка команды " + (i + 1) + "; дальнейший запуск остановлен."))
                .append(" >&2; return 0; fi\n");
        }
        script.append("return 0\n}\nPROMPT_COMMAND+=('_termux_workspace_ready')\n");
        return script.toString();
    }
    public static void cancel(File files, String id) throws IOException {
        write(new File(directory(files, id), "cancel-startup"), new byte[0]);
    }
    private static void write(File file, byte[] data) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(data); }
    }
}
