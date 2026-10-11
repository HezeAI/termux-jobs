package com.termux.app.workspace;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small versioned workspace, separate from Termux settings and shell processes. */
public final class WorkspaceStore {
    public static final int MAX_TRANSCRIPT = 65536;
    public static final int MAX_RECENT_COMMANDS = 25;
    public static final class Command {
        public String id = UUID.randomUUID().toString();
        public String name = "", command = "", category = "Общее", directory = "";
        public boolean favorite;
        public Command copy() {
            Command c = new Command();
            c.name = name + " — копия"; c.command = command; c.category = category;
            c.directory = directory; c.favorite = favorite;
            return c;
        }
    }
    public static final class Session {
        public String id = UUID.randomUUID().toString();
        public String name = "Bash", directory = "", startup = "", transcript = "", handle = "";
        public long updated;
        public Session copy() {
            Session s = new Session();
            s.name = name + " — копия"; s.directory = directory; s.startup = startup;
            // A duplicate starts with its own independent history.
            return s;
        }
    }
    public interface ErrorListener { void onError(String message); }
    public final List<Command> commands = new ArrayList<>();
    public final List<Session> sessions = new ArrayList<>();
    public final List<String> recentCommands = new ArrayList<>();
    private final AtomicFile file;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ErrorListener errors;
    private boolean writable = true;

    public WorkspaceStore(Context context, ErrorListener errors) {
        file = new AtomicFile(new File(context.getFilesDir(), "workspace-v1.json"));
        this.errors = errors;
        if (file.getBaseFile().exists() || new File(file.getBaseFile().getPath() + ".bak").exists()) {
            try { decode(new String(file.readFully(), StandardCharsets.UTF_8)); }
            catch (IOException | JSONException e) {
                // Keep the original file; never overwrite an unreadable user's library.
                writable = false;
                main.post(() -> errors.onError("Не удалось прочитать библиотеку. Исходный файл сохранён."));
            }
        }
    }
    public boolean isWritable() { return writable; }
    public void recordCommand(String command) {
        if (!writable || command.trim().isEmpty()) return;
        recentCommands.add(0, command);
        while (recentCommands.size() > MAX_RECENT_COMMANDS) recentCommands.remove(recentCommands.size() - 1);
        save();
    }
    public void clearRecentCommands() {
        if (!writable) return;
        recentCommands.clear(); save();
    }
    public Session findByHandle(String handle) {
        for (Session s : sessions) if (!handle.isEmpty() && handle.equals(s.handle)) return s;
        return null;
    }
    public void save() {
        if (!writable) { errors.onError("Библиотека недоступна для записи; исходные данные сохранены."); return; }
        final byte[] snapshot;
        try { snapshot = encode().toString().getBytes(StandardCharsets.UTF_8); }
        catch (JSONException e) { errors.onError("Не удалось сохранить библиотеку."); return; }
        writer.execute(() -> {
            FileOutputStream out = null;
            try {
                out = file.startWrite(); out.write(snapshot); file.finishWrite(out);
            } catch (IOException e) {
                if (out != null) file.failWrite(out);
                main.post(() -> errors.onError("Ошибка записи библиотеки. Проверьте свободное место."));
            }
        });
    }
    void awaitWrites() throws Exception { writer.submit(() -> {}).get(10, java.util.concurrent.TimeUnit.SECONDS); }
    public void close() { writer.shutdown(); } // Queued atomic writes finish after Activity destruction.
    JSONObject encode() throws JSONException {
        JSONObject root = new JSONObject().put("version", 1);
        JSONArray cs = new JSONArray(), ss = new JSONArray();
        for (Command c : commands) cs.put(new JSONObject().put("id", c.id).put("name", c.name)
            .put("command", c.command).put("category", c.category).put("directory", c.directory).put("favorite", c.favorite));
        for (Session s : sessions) ss.put(new JSONObject().put("id", s.id).put("name", s.name)
            .put("directory", s.directory).put("startup", s.startup).put("handle", s.handle)
            .put("transcript", tail(s.transcript, MAX_TRANSCRIPT)).put("updated", s.updated));
        JSONArray recent = new JSONArray();
        for (int i = 0; i < Math.min(recentCommands.size(), MAX_RECENT_COMMANDS); i++) recent.put(recentCommands.get(i));
        return root.put("commands", cs).put("sessions", ss).put("recentCommands", recent);
    }
    void decode(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.getInt("version") != 1) throw new JSONException("Unsupported workspace version");
        List<Command> cs = new ArrayList<>(); List<Session> ss = new ArrayList<>();
        JSONArray ca = root.getJSONArray("commands"), sa = root.getJSONArray("sessions");
        for (int i = 0; i < ca.length(); i++) {
            JSONObject o = ca.getJSONObject(i); Command c = new Command();
            c.id = validId(o.getString("id")); c.name = o.getString("name"); c.command = o.getString("command");
            c.category = o.optString("category", "Общее"); c.directory = o.optString("directory"); c.favorite = o.optBoolean("favorite");
            cs.add(c);
        }
        for (int i = 0; i < sa.length(); i++) {
            JSONObject o = sa.getJSONObject(i); Session s = new Session();
            s.id = validId(o.getString("id")); s.name = o.getString("name"); s.directory = o.optString("directory");
            s.startup = o.optString("startup"); s.handle = o.optString("handle"); s.transcript = tail(o.optString("transcript"), MAX_TRANSCRIPT);
            s.updated = o.optLong("updated"); ss.add(s);
        }
        List<String> recent = new ArrayList<>();
        JSONArray history = root.optJSONArray("recentCommands");
        if (history != null) for (int i = 0; i < Math.min(history.length(), MAX_RECENT_COMMANDS); i++) recent.add(history.getString(i));
        commands.clear(); commands.addAll(cs); sessions.clear(); sessions.addAll(ss);
        recentCommands.clear(); recentCommands.addAll(recent);
    }
    private static String validId(String id) throws JSONException {
        try { return UUID.fromString(id).toString(); }
        catch (IllegalArgumentException e) { throw new JSONException("Invalid id"); }
    }
    public static String tail(String text, int max) {
        if (text.length() <= max) return text;
        int start = text.length() - max;
        if (Character.isLowSurrogate(text.charAt(start))) start++;
        return text.substring(start);
    }
}
