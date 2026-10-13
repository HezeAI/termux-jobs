package com.termux.app.workspace;

/** SSH owns authentication; no passwords or private keys are stored in profiles. */
public final class SshSessionShell {
    private SshSessionShell() {}
    public static boolean validHost(String host) {
        return host != null && !host.isEmpty() && !host.startsWith("-")
            && host.matches("[A-Za-z0-9_.:%-]+");
    }
    public static boolean validUser(String user) {
        return user != null && user.matches("[A-Za-z0-9_][A-Za-z0-9_.-]*");
    }
    public static boolean validPort(int port) { return port >= 1 && port <= 65535; }
    public static boolean validIdentity(String path) {
        return path != null && (path.startsWith("/") || path.startsWith("~/"))
            && path.indexOf('\0') < 0 && !path.contains("\n") && !path.contains("\r");
    }
    public static String command(WorkspaceStore.Session session) {
        if (!validHost(session.sshHost) || !validUser(session.sshUser) || !validPort(session.sshPort))
            throw new IllegalArgumentException("Invalid SSH connection settings");
        String options = "";
        if (session.sshAuth.equals("key")) {
            if (!validIdentity(session.sshIdentity)) throw new IllegalArgumentException("Invalid SSH identity");
            String path = session.sshIdentity.startsWith("~/")
                ? "\"$HOME\"/" + SessionShell.quote(session.sshIdentity.substring(2)) : SessionShell.quote(session.sshIdentity);
            options = " -o IdentitiesOnly=yes -i " + path;
        } else if (session.sshAuth.equals("password")) {
            options = " -o PreferredAuthentications=password -o PubkeyAuthentication=no -o KbdInteractiveAuthentication=no"
                + " -o NumberOfPasswordPrompts=1 -o BatchMode=no -o StrictHostKeyChecking=ask -o ControlMaster=no -o ControlPath=none";
        }
        return "ssh -t" + options + " -p " + session.sshPort + " -l " + SessionShell.quote(session.sshUser)
            + " -- " + SessionShell.quote(session.sshHost) + " "
            + SessionShell.quote("bash -ic " + SessionShell.quote(remoteScript(session)));
    }
    public static String remoteScript(WorkspaceStore.Session session) {
        String cwd = session.directory;
        String target = cwd.isEmpty() || cwd.equals("~") ? "\"$HOME\""
            : cwd.startsWith("~/") ? "\"$HOME\"/" + SessionShell.quote(cwd.substring(2)) : SessionShell.quote(cwd);
        StringBuilder script = new StringBuilder("_termux_remote_startup() {\ncd -- " + target
            + WorkspaceText.tr(" || { printf '%s\\n' 'Не удалось открыть каталог на сервере.' >&2; return 1; }\n"));
        int index = 0;
        for (String startup : session.startupCommands()) {
            index++;
            script.append("printf '%s\\n' ").append(SessionShell.quote(WorkspaceText.tr("Сервер: команда ") + index + "/" + session.startupCommands().size())).append("\n")
                .append("eval -- ").append(SessionShell.quote(startup)).append("\nlocal _termux_remote_status=$?\n")
                .append("if [ \"$_termux_remote_status\" -ne 0 ]; then printf '%s\\n' ")
                .append(SessionShell.quote(WorkspaceText.tr("Ошибка команды ") + index + WorkspaceText.tr("; дальнейший запуск остановлен.")))
                .append(" >&2; return \"$_termux_remote_status\"; fi\n");
        }
        script.append("}\n_termux_remote_startup\n_termux_remote_result=$?\nunset -f _termux_remote_startup\n");
        if (session.sshKeepOpen) script.append("exec bash -i\n");
        else script.append("exit \"$_termux_remote_result\"\n");
        return script.toString();
    }
}
