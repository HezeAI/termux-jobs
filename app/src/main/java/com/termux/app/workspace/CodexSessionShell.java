package com.termux.app.workspace;

import com.termux.shared.termux.TermuxConstants;
import java.io.File;
import java.io.IOException;

/** Dedicated local server with explicit permissions, no account/config-file edits. */
public final class CodexSessionShell {
    private CodexSessionShell() {}
    public static File socketDirectory(File files) { return new File(files, ".codex-control"); }
    public static File socket(File files, WorkspaceStore.Session s) { return new File(socketDirectory(files), s.codexSocketId + ".sock"); }
    public static String sandbox(String access) {
        if (access.equals("full")) return "danger-full-access";
        return access.equals("read") ? "read-only" : "workspace-write";
    }
    public static String approval(WorkspaceStore.Session s){return (s.codexConfirmActions==null?!s.codexAccess.equals("full"):s.codexConfirmActions)?"on-request":"never";}
    public static boolean validThread(String id) { return id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}"); }
    public static String invocation(WorkspaceStore.Session s, String endpoint) {
        StringBuilder c = new StringBuilder("codex --remote ").append(SessionShell.quote("unix://" + endpoint));
        if (!s.codexChat.equals("new")) {
            c.append(" resume");
            if (s.codexChat.equals("id") && validThread(s.codexThread)) c.append(' ').append(SessionShell.quote(s.codexThread));
            else throw new IllegalArgumentException("Select a specific Codex chat");
        } else {
            c.append(" --sandbox ").append(sandbox(s.codexAccess));
            c.append(" --ask-for-approval ").append(approval(s));
        }
        return c.toString();
    }
    public static String command(File files, WorkspaceStore.Session s) throws IOException {
        File controls = socketDirectory(files);
        if (!controls.isDirectory() && !controls.mkdirs()) throw new IOException("Cannot create Codex control directory");
        if (!controls.setReadable(false, false) || !controls.setWritable(false, false) || !controls.setExecutable(false, false)
            || !controls.setReadable(true, true) || !controls.setWritable(true, true) || !controls.setExecutable(true, true)) throw new IOException("Cannot protect control directory");
        String host = socket(files, s).getAbsolutePath();
        if (host.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 100) throw new IOException("Codex socket path too long");
        String endpoint = AgentEnvironments.ubuntu(s.codexEnvironment) ? "/tmp/termux-jobs-codex/" + socket(files, s).getName() : host;
        String stop = endpoint + ".stop", stopped = endpoint + ".stopped";
        String client = invocation(s, endpoint);
        if (!s.codexChat.equals("new")) client = "codex --remote " + SessionShell.quote("unix://" + endpoint) + " resume \"$_jobs_codex_thread\"";
        return WorkspaceText.tr("(\ncommand -v codex >/dev/null 2>&1 || { printf '%s\\n' 'Codex не установлен в выбранной среде.' >&2; exit 127; }\n")
            + WorkspaceText.tr("command -v node >/dev/null 2>&1 || { printf '%s\\n' 'Для локального управления Codex нужен Node.js в выбранной среде.' >&2; exit 127; }\n")
            + "[ ! -e " + SessionShell.quote(endpoint) + WorkspaceText.tr(" ] || { printf '%s\\n' 'Подключение Codex уже занято.' >&2; exit 1; }\n")
            + "umask 077\ncodex --no-daemon -c " + SessionShell.quote("sandbox_mode=\"" + sandbox(s.codexAccess) + "\"")
            + " -c " + SessionShell.quote("approval_policy=\"" + (approval(s)) + "\"")
            + " app-server --listen " + SessionShell.quote("unix://" + endpoint) + " >/dev/null 2>&1 &\n_jobs_codex_pid=$!\n"
            + "trap 'if [ -n \"${_jobs_codex_status_pid:-}\" ]; then kill \"$_jobs_codex_status_pid\" 2>/dev/null; wait \"$_jobs_codex_status_pid\" 2>/dev/null; fi; if [ -n \"${_jobs_codex_watch_pid:-}\" ]; then kill \"$_jobs_codex_watch_pid\" 2>/dev/null; fi; kill \"$_jobs_codex_pid\" 2>/dev/null; wait \"$_jobs_codex_pid\" 2>/dev/null' EXIT\n"
            + "trap 'exit 130' INT TERM HUP\n_jobs_codex_wait=0\nwhile [ ! -S " + SessionShell.quote(endpoint) + " ]; do\n"
            + WorkspaceText.tr("kill -0 \"$_jobs_codex_pid\" 2>/dev/null || { printf '%s\\n' 'Не удалось запустить app-server. Проверьте версию Codex.' >&2; exit 1; }\n")
            + WorkspaceText.tr("_jobs_codex_wait=$((_jobs_codex_wait+1)); [ \"$_jobs_codex_wait\" -lt 100 ] || { printf '%s\\n' 'Истекло ожидание Codex.' >&2; exit 1; }; sleep 0.1\ndone\n")
            + "( while kill -0 \"$_jobs_codex_pid\" 2>/dev/null; do if [ -f " + SessionShell.quote(stop) + " ]; then kill \"$_jobs_codex_pid\" 2>/dev/null; printf x > " + SessionShell.quote(stopped) + "; exit; fi; sleep 0.1; done ) &\n_jobs_codex_watch_pid=$!\n"
            + "node " + SessionShell.quote(bridgePath(files,s)) + " status " + SessionShell.quote(endpoint) + " " + SessionShell.quote(s.codexChat.equals("id") ? s.codexThread : "") + " >/dev/null 2>&1 &\n_jobs_codex_status_pid=$!\n"
            + (CodexChatControl.enabled(files,s)?CodexChatControl.command(files,s,endpoint):resumePreflight(files, s, endpoint) + client) + "\n)";
    }
    public static synchronized void prepareBridge(android.content.Context context) throws IOException {
        File directory = socketDirectory(context.getFilesDir()); if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create controls");
        File bridge = new File(directory, "bridge-v0.2.21.js");
        if (bridge.isFile()) return;
        File temporary=File.createTempFile("codex-bridge-",".tmp",directory);
        try {
            try (java.io.InputStream in = context.getAssets().open("codex-control.js"); java.io.FileOutputStream out = new java.io.FileOutputStream(temporary)) {
                byte[] bytes = new byte[8192]; int count; while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
            }
            if(!temporary.setReadable(false,false)||!temporary.setWritable(false,false)||!temporary.setReadable(true,true)||!temporary.setWritable(true,true))throw new IOException("Cannot protect Codex helper");
            if(!temporary.renameTo(bridge))throw new IOException("Cannot publish Codex helper");
        } finally {temporary.delete();}
    }
    private static String bridgePath(File files, WorkspaceStore.Session s) {
        return AgentEnvironments.ubuntu(s.codexEnvironment) ? "/tmp/termux-jobs-codex/bridge-v0.2.21.js" : new File(socketDirectory(files), "bridge-v0.2.21.js").getAbsolutePath();
    }
    public static String resumePreflight(WorkspaceStore.Session s, String endpoint) {
        return resumePreflight(new File(TermuxConstants.TERMUX_FILES_DIR_PATH), s, endpoint);
    }
    public static String resumePreflight(File files, WorkspaceStore.Session s, String endpoint) {
        if (s.codexChat.equals("new")) return "";
        if (!(s.codexChat.equals("id") && validThread(s.codexThread))) throw new IllegalArgumentException("Select a specific Codex chat");
        String home = AgentEnvironments.ubuntu(s.codexEnvironment) ? "/root" : TermuxConstants.TERMUX_HOME_DIR_PATH;
        String cwd = s.directory.isEmpty() || s.directory.equals("~") ? home : s.directory;
        if (cwd.startsWith("~/")) cwd = home + cwd.substring(1);
        return "_jobs_codex_thread=$(node " + SessionShell.quote(bridgePath(files, s)) + " resume " + SessionShell.quote(endpoint)
            + " " + SessionShell.quote(s.codexAccess) + " " + SessionShell.quote(s.codexChat) + " " + SessionShell.quote(s.codexThread) + " " + SessionShell.quote(cwd) + " " + SessionShell.quote(s.codexSourceDirectory) + " " + SessionShell.quote(approval(s))
            + WorkspaceText.tr(") || { printf '%s\\n' 'Не удалось продолжить чат с выбранными правами.' >&2; exit 1; }\n");
    }
    public static String proxyCommand(File files, WorkspaceStore.Session s) {
        String sock = AgentEnvironments.ubuntu(s.codexEnvironment) ? "/tmp/termux-jobs-codex/" + socket(files, s).getName() : socket(files, s).getAbsolutePath();
        String proxy = (AgentEnvironments.ubuntu(s.codexEnvironment)?AgentPreparation.codexPath():"")+"command -v node >/dev/null 2>&1 || { printf '%s\\n' 'JOBS_CONTROL:NO_NODE' >&2; exit 127; }; "
            + "[ -r " + SessionShell.quote(bridgePath(files, s)) + " ] || { printf '%s\\n' 'JOBS_CONTROL:NO_BRIDGE' >&2; exit 2; }; "
            + "[ -S " + SessionShell.quote(sock) + " ] || { printf '%s\\n' 'JOBS_CONTROL:NO_SOCKET' >&2; exit 3; }; "
            + "exec node " + SessionShell.quote(bridgePath(files, s)) + " proxy " + SessionShell.quote(sock);
        return AgentEnvironments.ubuntu(s.codexEnvironment) ? "proot-distro login --bind " + SessionShell.quote(socketDirectory(files).getAbsolutePath() + ":/tmp/termux-jobs-codex") + " "+SessionShell.quote(AgentEnvironments.alias(s))+" -- /bin/sh -c " + SessionShell.quote(proxy)
            : proxy;
    }
    public static String historyCommand(WorkspaceStore.Session s) {
        return AgentEnvironments.ubuntu(s.codexEnvironment) ? "proot-distro login "+SessionShell.quote(AgentEnvironments.alias(s))+" -- /bin/sh -c "+SessionShell.quote(AgentPreparation.codexPath()+"exec codex --no-daemon app-server --stdio") : "codex --no-daemon app-server --stdio";
    }
}
