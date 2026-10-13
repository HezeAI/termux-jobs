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
    public static final String BASE_TERMINAL_ID = "00000000-0000-4000-8000-000000000001";
    public static boolean isBase(Session session) { return session != null && BASE_TERMINAL_ID.equals(session.id); }
    public static final int MAX_RECENT_COMMANDS = 50;
    public static final class Command {
        public String id = UUID.randomUUID().toString();
        public String name = "", command = "", category = WorkspaceText.tr("Общее"), directory = "";
        public boolean favorite;
        public Command copy() {
            Command c = new Command();
            c.name = name + WorkspaceText.tr(" — копия"); c.command = command; c.category = category;
            c.directory = directory; c.favorite = favorite;
            return c;
        }
    }
    public static final class Session {
        public String id = UUID.randomUUID().toString();
        public String name = "Bash", directory = "", startup = "", handle = "";
        public String startup2 = "", startup3 = "", environment = "legacy";
        public String sshHost = "", sshUser = "root";
        public int sshPort = 22;
        public boolean sshKeepOpen = true;
        public String sshAuth = "auto", sshIdentity = "", sshEncryptedPassword = "";
        public String codexEnvironment = "ubuntu", codexAccess = "full", codexChat = "new", codexThread = "", codexThreadName = "", codexSocketId = UUID.randomUUID().toString();
        public String codexSourceDirectory = "";
        public long updated;
        public boolean saved;
        public List<String> startupCommands() {
            List<String> result = new ArrayList<>();
            for (String command : new String[]{startup, startup2, startup3}) if (!command.trim().isEmpty()) result.add(command);
            return result;
        }
        public Session copy() {
            Session s = new Session();
            s.name = name + WorkspaceText.tr(" — копия"); s.directory = directory; s.startup = startup; s.startup2 = startup2; s.startup3 = startup3; s.environment = environment;
            s.sshHost = sshHost; s.sshUser = sshUser; s.sshPort = sshPort; s.sshKeepOpen = sshKeepOpen; s.sshAuth = sshAuth; s.sshIdentity = sshIdentity; // Password is bound to the original profile ID.
            s.codexEnvironment = codexEnvironment; s.codexAccess = codexAccess; s.codexChat = codexChat; s.codexThread = codexThread; s.codexThreadName = codexThreadName; s.codexSourceDirectory=codexSourceDirectory;
            // A duplicate starts with its own independent history.
            return s;
        }
    }
    public static final class Note {
        public String id = UUID.randomUUID().toString();
        public String name = "", text = "", category = WorkspaceText.tr("Общее");
        public boolean favorite;
    }
    public interface ErrorListener { void onError(String message); }
    public static final class RecentCommand {
        public final String command;
        public final long sentAt;
        RecentCommand(String command, long sentAt) { this.command = command; this.sentAt = sentAt; }
    }
    public final List<Command> commands = new ArrayList<>();
    public final List<Note> notes = new ArrayList<>();
    public final List<Session> sessions = new ArrayList<>();
    public final List<RecentCommand> recentCommands = new ArrayList<>();
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
                main.post(() -> errors.onError(WorkspaceText.tr("Не удалось прочитать библиотеку. Исходный файл сохранён.")));
            }
        }
    }
    public boolean isWritable() { return writable; }
    public void recordCommand(String command) {
        if (!writable || command.trim().isEmpty()) return;
        recentCommands.add(0, new RecentCommand(command, System.currentTimeMillis()));
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
        if (!writable) { errors.onError(WorkspaceText.tr("Библиотека недоступна для записи; исходные данные сохранены.")); return; }
        final byte[] snapshot;
        try { snapshot = encode().toString().getBytes(StandardCharsets.UTF_8); }
        catch (JSONException e) { errors.onError(WorkspaceText.tr("Не удалось сохранить библиотеку.")); return; }
        writer.execute(() -> {
            FileOutputStream out = null;
            try {
                out = file.startWrite(); out.write(snapshot); file.finishWrite(out);
            } catch (IOException e) {
                if (out != null) file.failWrite(out);
                main.post(() -> errors.onError(WorkspaceText.tr("Ошибка записи библиотеки. Проверьте свободное место.")));
            }
        });
    }
    void awaitWrites() throws Exception { writer.submit(() -> {}).get(10, java.util.concurrent.TimeUnit.SECONDS); }
    public void close() { writer.shutdown(); } // Queued atomic writes finish after Activity destruction.
    JSONObject encode() throws JSONException {
        JSONObject root = new JSONObject().put("version", 8);
        JSONArray cs = new JSONArray(), ss = new JSONArray();
        for (Command c : commands) cs.put(new JSONObject().put("id", c.id).put("name", c.name)
            .put("command", c.command).put("category", c.category).put("directory", c.directory).put("favorite", c.favorite));
        for (Session s : sessions) ss.put(new JSONObject().put("id", s.id).put("name", s.name)
            .put("directory", s.directory).put("startup", s.startup).put("startup2", s.startup2).put("startup3", s.startup3).put("sshAuth", s.sshAuth).put("sshIdentity", s.sshIdentity).put("sshEncryptedPassword", s.sshEncryptedPassword).put("sshHost", s.sshHost).put("sshUser", s.sshUser).put("sshPort", s.sshPort).put("sshKeepOpen", s.sshKeepOpen).put("environment", s.environment).put("handle", s.handle)
            .put("codexEnvironment", s.codexEnvironment).put("codexAccess", s.codexAccess).put("codexChat", s.codexChat).put("codexThread", s.codexThread).put("codexThreadName", s.codexThreadName).put("codexSocketId", s.codexSocketId).put("codexSourceDirectory",s.codexSourceDirectory)
            .put("updated", s.updated).put("saved", s.saved));
        JSONArray ns = new JSONArray();
        for (Note n : notes) ns.put(new JSONObject().put("id", n.id).put("name", n.name)
            .put("text", n.text).put("category", n.category).put("favorite", n.favorite));
        JSONArray recent = new JSONArray();
        for (int i = 0; i < Math.min(recentCommands.size(), MAX_RECENT_COMMANDS); i++) {
            RecentCommand entry = recentCommands.get(i);
            recent.put(new JSONObject().put("command", entry.command).put("sentAt", entry.sentAt));
        }
        return root.put("commands", cs).put("sessions", ss).put("notes", ns).put("recentCommands", recent);
    }
    void decode(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        int version = root.getInt("version");
        if (version != 1 && version != 2 && version != 3 && version != 4 && version != 5 && version != 6 && version != 7 && version != 8) throw new JSONException("Unsupported workspace version");
        List<Command> cs = new ArrayList<>(); List<Session> ss = new ArrayList<>();
        JSONArray ca = root.getJSONArray("commands"), sa = root.getJSONArray("sessions");
        for (int i = 0; i < ca.length(); i++) {
            JSONObject o = ca.getJSONObject(i); Command c = new Command();
            c.id = validId(o.getString("id")); c.name = o.getString("name"); c.command = o.getString("command");
            c.category = o.optString("category", WorkspaceText.tr("Общее")); c.directory = o.optString("directory"); c.favorite = o.optBoolean("favorite");
            cs.add(c);
        }
        for (int i = 0; i < sa.length(); i++) {
            JSONObject o = sa.getJSONObject(i); Session s = new Session();
            s.id = validId(o.getString("id")); s.name = o.getString("name"); s.directory = o.optString("directory");
            s.startup = o.optString("startup"); s.startup2 = o.optString("startup2"); s.startup3 = o.optString("startup3"); s.environment = o.optString("environment", "legacy");
            if (!s.environment.equals("legacy") && !s.environment.equals("termux") && !s.environment.equals("ubuntu") && !s.environment.equals("ssh") && !s.environment.equals("codex")) throw new JSONException("Unknown environment"); s.handle = o.optString("handle");
            s.sshHost = o.optString("sshHost", ""); s.sshUser = o.optString("sshUser", "root");
            s.sshAuth = o.optString("sshAuth", "auto"); s.sshIdentity = o.optString("sshIdentity", "");
            s.sshEncryptedPassword = o.optString("sshEncryptedPassword", "");
            if (!s.sshAuth.equals("auto") && !s.sshAuth.equals("key") && !s.sshAuth.equals("password")) throw new JSONException("Unknown SSH authentication");
            if (s.environment.equals("ssh") && s.sshAuth.equals("key") && !SshSessionShell.validIdentity(s.sshIdentity)) throw new JSONException("Invalid SSH identity");
            s.sshPort = o.optInt("sshPort", 22); s.sshKeepOpen = o.optBoolean("sshKeepOpen", true);
            if (s.environment.equals("ssh") && (version < 6 || !SshSessionShell.validHost(s.sshHost)
                || !SshSessionShell.validUser(s.sshUser) || !SshSessionShell.validPort(s.sshPort))) throw new JSONException("Invalid SSH settings");
            s.codexEnvironment = o.optString("codexEnvironment", "ubuntu"); s.codexAccess = o.optString("codexAccess", "full");
            s.codexChat = o.optString("codexChat", "new"); s.codexThread = o.optString("codexThread", ""); s.codexThreadName = o.optString("codexThreadName", ""); s.codexSocketId = validId(o.optString("codexSocketId", s.codexSocketId));
            if (s.environment.equals("codex") && (version < 8 || !(s.codexEnvironment.equals("ubuntu") || s.codexEnvironment.equals("termux"))
                || !(s.codexAccess.equals("full") || s.codexAccess.equals("project") || s.codexAccess.equals("read"))
                || !(s.codexChat.equals("new") || s.codexChat.equals("last") || s.codexChat.equals("choose") || (s.codexChat.equals("id") && CodexSessionShell.validThread(s.codexThread))))) throw new JSONException("Invalid Codex profile");
            s.codexSourceDirectory=o.optString("codexSourceDirectory","");
            if(!s.codexSourceDirectory.isEmpty()&&(!s.codexSourceDirectory.startsWith("/")||CodexProjects.directory(s.codexEnvironment,s.codexSourceDirectory).isEmpty()))throw new JSONException("Invalid confirmed chat directory");
            s.updated = o.optLong("updated"); s.saved = o.optBoolean("saved", true); ss.add(s);
        }
        List<Note> ns = new ArrayList<>();
        JSONArray na = root.optJSONArray("notes");
        if (version >= 5 && na == null) throw new JSONException("Missing notes");
        if (na != null) for (int i = 0; i < na.length(); i++) {
            JSONObject o = na.getJSONObject(i); Note n = new Note();
            n.id = validId(o.getString("id")); n.name = o.getString("name"); n.text = o.getString("text");
            n.category = o.optString("category", WorkspaceText.tr("Общее")); n.favorite = o.optBoolean("favorite"); ns.add(n);
        }
        List<RecentCommand> recent = new ArrayList<>();
        JSONArray history = root.optJSONArray("recentCommands");
        if (history != null) for (int i = 0; i < Math.min(history.length(), MAX_RECENT_COMMANDS); i++) {
            Object item = history.get(i);
            if (item instanceof String) recent.add(new RecentCommand((String) item, 0));
            else {
                JSONObject entry = history.getJSONObject(i);
                recent.add(new RecentCommand(entry.getString("command"), entry.optLong("sentAt")));
            }
        }
        commands.clear(); commands.addAll(cs); sessions.clear(); sessions.addAll(ss);
        notes.clear(); notes.addAll(ns); recentCommands.clear(); recentCommands.addAll(recent);
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
