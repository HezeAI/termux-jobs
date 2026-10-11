package com.termux.app.workspace;

import com.termux.shared.termux.TermuxConstants;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

/** Explicit Bash sessions with per-profile history, never terminal keystroke logging. */
public final class SessionShell {
    private SessionShell() {}
    public static String quote(String value) { return "'" + value.replace("'", "'\\''") + "'"; }
    public static File directory(File files, String id) { return new File(new File(files, "workspace-sessions"), id); }
    public static File prepare(File files, WorkspaceStore.Session session) throws IOException {
        File dir = directory(files, session.id);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create session directory");
        File history = new File(dir, "history");
        if (!history.exists()) write(history, new byte[0]);
        File rc = new File(dir, "bashrc");
        String script = buildRc(TermuxConstants.TERMUX_HOME_DIR_PATH, history.getAbsolutePath(), session.startup);
        write(rc, script.getBytes(StandardCharsets.UTF_8));
        return rc;
    }
    public static String buildRc(String home, String history, String startup) {
        return "if [ -f " + quote(home + "/.bashrc") + " ]; then . " + quote(home + "/.bashrc") + "; fi\n"
            + "HISTFILE=" + quote(history) + "\nHISTSIZE=2000\nHISTFILESIZE=2000\nshopt -s histappend\n"
            + "history -c\nhistory -r \"$HISTFILE\"\nPROMPT_COMMAND+=('history -a')\n"
            + (startup.trim().isEmpty() ? "" : "history -s -- " + quote(startup) + "\nhistory -a\n" + startup + "\n");
    }
    private static void write(File file, byte[] data) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(data); }
    }
    private static byte[] read(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toByteArray();
        }
    }
    public static String readHistory(File files, String id) throws IOException {
        File history = new File(directory(files, id), "history");
        return history.exists() ? WorkspaceStore.tail(new String(read(history), StandardCharsets.UTF_8), 131072) : "";
    }
}
