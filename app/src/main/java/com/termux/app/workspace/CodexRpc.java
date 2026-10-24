package com.termux.app.workspace;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import android.util.Base64;

/** Bounded JSON-RPC over stdio for browsing, and RFC6455 over a private Unix socket for live status. */
final class CodexRpc implements AutoCloseable {
    private InputStream in;
    private OutputStream out;
    private LocalSocket socket;
    private Process process;
    private final boolean websocket;
    private int serial;
    private volatile String failureKind = "";
    private String stage = "launch";
    private Thread errors;
    static String diagnostic(Exception failure) {
        return failure instanceof ControlFailure ? failure.getMessage() : WorkspaceText.tr("Не удалось прочитать статус Codex (") + failure.getClass().getSimpleName() + WorkspaceText.tr("). Чат может продолжать работать.");
    }
    private static final class ControlFailure extends IOException {
        ControlFailure(String message) { super(message); }
    }
    static String classify(String line) {
        if (line.contains("JOBS_CONTROL:NO_NODE")) return WorkspaceText.tr("В выбранной среде не найден Node.js");
        if (line.contains("JOBS_CONTROL:NO_BRIDGE")) return WorkspaceText.tr("Выбранная среда не видит файл адаптера");
        if (line.contains("JOBS_CONTROL:NO_SOCKET")) return WorkspaceText.tr("Выбранная среда не видит сокет текущей сессии");
        if (line.contains("ENOENT")) return WorkspaceText.tr("Не найден файл или сокет адаптера");
        if (line.contains("ECONNREFUSED")) return WorkspaceText.tr("Сервер Codex не принимает подключение");
        if (line.contains("EACCES") || line.contains("Permission denied")) return WorkspaceText.tr("Отказ в доступе к адаптеру");
        if (line.contains("not found")) return WorkspaceText.tr("Не найдена программа для запуска адаптера");
        if (line.contains("WebSocket")) return WorkspaceText.tr("Ошибка согласования локального протокола");
        return "";
    }
    private void drainErrors() {
        errors = new Thread(() -> {
            try (InputStream input = process.getErrorStream()) {
                ByteArrayOutputStream line = new ByteArrayOutputStream(); int b;
                while ((b = input.read()) != -1) {
                    if (b == '\n') { String classified = classify(new String(line.toByteArray(), StandardCharsets.UTF_8)); if (!classified.isEmpty()) failureKind = classified; line.reset(); }
                    else if (line.size() < 4096) line.write(b);
                }
                String classified = classify(new String(line.toByteArray(), StandardCharsets.UTF_8)); if (!classified.isEmpty()) failureKind = classified;
            } catch (IOException ignored) {}
        }, "codex-control-errors"); errors.setDaemon(true); errors.start();
    }
    private ControlFailure failed(Exception failure) {
        if (failure instanceof ControlFailure) return (ControlFailure) failure;
        String exit = "";
        if (process != null) try { exit = ", exit=" + process.exitValue(); if (errors != null) errors.join(100); } catch (IllegalThreadStateException ignored) {} catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        return new ControlFailure(WorkspaceText.tr("Статус Codex недоступен: ") + (failureKind.isEmpty() ? failure.getClass().getSimpleName() : failureKind) + " (" + stage + exit + WorkspaceText.tr("). Чат и авторизация не изменены."));
    }
    private final SecureRandom random = new SecureRandom();
    private final java.util.concurrent.ScheduledExecutorService timeout = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private static final int MAX = 2 * 1024 * 1024;
    static CodexRpc live(Context context, WorkspaceStore.Session profile) throws Exception {
        CodexSessionShell.prepareBridge(context); // New immutable helper, without touching the active server.
        return process(context, CodexSessionShell.proxyCommand(context.getFilesDir(), profile));
    }
    static CodexRpc history(Context context, WorkspaceStore.Session profile) throws Exception {
        return process(context, CodexSessionShell.historyCommand(profile));
    }
    private static CodexRpc process(Context context, String command) throws Exception {
        CodexRpc rpc = new CodexRpc(false);
        try {
            ProcessBuilder builder = new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash", "--noprofile", "--norc", "-c", command);
            builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH)); builder.environment().clear();
            for (String item : TermuxShellUtils.buildEnvironment(context, false, TermuxConstants.TERMUX_HOME_DIR_PATH)) {
                int equal = item.indexOf('='); if (equal > 0) builder.environment().put(item.substring(0, equal), item.substring(equal + 1));
            }
            rpc.process = builder.start(); rpc.drainErrors(); rpc.in = rpc.process.getInputStream(); rpc.out = new BufferedOutputStream(rpc.process.getOutputStream());
            rpc.timeout.schedule(rpc::close, 25, java.util.concurrent.TimeUnit.SECONDS); rpc.initialize(); return rpc;
        } catch (Exception e) { ControlFailure failure = rpc.failed(e); rpc.close(); throw failure; }
    }
    static void stopOwnedServer(File files, WorkspaceStore.Session profile) {
        ownedStop(files, profile).run();
    }
    /** Resolve the owner before enqueueing: a mutable profile may already have relaunched. */
    static Runnable ownedStop(File files, WorkspaceStore.Session profile) {
        final File socket = CodexSessionShell.socket(files, profile);
        return () -> stopOwnedSocket(socket);
    }
    private static void stopOwnedSocket(File socket) {
        File marker = new File(socket.getAbsolutePath() + ".stop"); File stopped = new File(socket.getAbsolutePath() + ".stopped");
        try {
            try (FileOutputStream out = new FileOutputStream(marker)) { out.write(1); }
            long until = System.currentTimeMillis() + 1500;
            while (!stopped.exists() && System.currentTimeMillis() < until) Thread.sleep(50);
            // Only the launcher's watcher knows and signals the PID it created. No PID inference across proot.
        } catch (Exception unavailable) { /* Finishing the terminal must still work. */ }
    }
    private CodexRpc(boolean websocket) { this.websocket = websocket; }
    private void initialize() throws Exception {
        call("initialize", new JSONObject().put("clientInfo", new JSONObject().put("name", "termux_jobs").put("version", "0.2.19"))
            .put("capabilities", new JSONObject().put("experimentalApi", true)));
        send(new JSONObject().put("method", "initialized"));
    }
    JSONObject call(String method, JSONObject params) throws Exception {
        stage = method;
        try {
        int id = ++serial;
        send(new JSONObject().put("id", id).put("method", method).put("params", params));
        long deadline = System.currentTimeMillis() + 12000;
        while (System.currentTimeMillis() < deadline) {
            JSONObject message = receive();
            if (message.has("id") && message.optInt("id", -1) == id && !message.has("method")) {
                if (message.has("error")) throw new IOException("Codex rejected " + method);
                return message.getJSONObject("result");
            }
            // This status connection never handles approvals or tools. The TUI remains their owner.
        }
        throw new IOException("Codex request timed out");
        } catch (Exception e) { throw failed(e); }
    }
    private String line(int max) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != '\n') { if (bytes.size() >= max) throw new IOException("Message too large"); bytes.write(b); }
        if (b == -1) throw new EOFException();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8).replace("\r", "");
    }
    private void upgrade() throws Exception {
        byte[] nonce = new byte[16]; random.nextBytes(nonce); String key = Base64.encodeToString(nonce, Base64.NO_WRAP);
        out.write(("GET / HTTP/1.1\r\nHost: localhost\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + key + "\r\nSec-WebSocket-Version: 13\r\n\r\n").getBytes(StandardCharsets.US_ASCII)); out.flush();
        if (!line(4096).startsWith("HTTP/1.1 101")) throw new IOException("WebSocket upgrade rejected");
        String accept = ""; boolean finished = false;
        for (int count = 0; count < 64; count++) { String h = line(4096); if (h.isEmpty()) { finished = true; break; } if (h.toLowerCase(java.util.Locale.ROOT).startsWith("sec-websocket-accept:")) accept = h.substring(h.indexOf(':') + 1).trim(); }
        String expected = Base64.encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)), Base64.NO_WRAP);
        if (!finished || !expected.equals(accept)) throw new IOException("Invalid WebSocket handshake");
    }
    private void frame(int opcode, byte[] bytes) throws IOException {
        out.write(0x80 | opcode); int length = bytes.length;
        if (length < 126) out.write(0x80 | length);
        else if (length < 65536) { out.write(0x80 | 126); out.write(length >>> 8); out.write(length); }
        else { out.write(0x80 | 127); for (int shift = 56; shift >= 0; shift -= 8) out.write((int)((long)length >>> shift)); }
        byte[] mask = new byte[4]; random.nextBytes(mask); out.write(mask);
        for (int i = 0; i < bytes.length; i++) out.write(bytes[i] ^ mask[i % 4]); out.flush();
    }
    private int byteValue() throws IOException { int b = in.read(); if (b < 0) throw new EOFException(); return b; }
    private JSONObject receive() throws Exception {
        if (!websocket) return new JSONObject(line(MAX));
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        while (true) {
            int first = byteValue(), second = byteValue(), opcode = first & 15; long length = second & 127;
            if ((first & 0x70) != 0 || (second & 0x80) != 0) throw new IOException("Invalid server frame");
            if (length == 126) length = (byteValue() << 8) | byteValue();
            else if (length == 127) { length = 0; for (int i = 0; i < 8; i++) length = (length << 8) | byteValue(); }
            if (length < 0 || length > MAX || message.size() + length > MAX) throw new IOException("Message too large");
            byte[] bytes = new byte[(int)length]; int offset = 0;
            while (offset < bytes.length) { int read = in.read(bytes, offset, bytes.length - offset); if (read < 0) throw new EOFException(); offset += read; }
            if (opcode == 8) throw new EOFException();
            if (opcode == 9) { frame(10, bytes); continue; }
            if (opcode == 10) continue;
            if (opcode != 0 && opcode != 1) throw new IOException("Expected text frame");
            message.write(bytes);
            if ((first & 0x80) != 0) return new JSONObject(new String(message.toByteArray(), StandardCharsets.UTF_8));
        }
    }
    private void send(JSONObject data) throws IOException {
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        if (websocket) frame(1, bytes); else { out.write(bytes); out.write('\n'); out.flush(); }
    }
    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (process != null) process.destroy(); timeout.shutdownNow();
    }
}
