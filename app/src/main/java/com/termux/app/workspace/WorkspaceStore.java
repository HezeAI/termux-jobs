package com.termux.app.workspace;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
    public static final class Alias {
        public String id = UUID.randomUUID().toString();
        public String name = "", command = "", environment = "termux", category = "";
        public boolean favorite;
        public Alias copy() { Alias a=new Alias();a.id=id;a.name=name;a.command=command;a.environment=environment;a.category=category;a.favorite=favorite;return a; }
    }
    public static final class Session {
        public String id = UUID.randomUUID().toString();
        public String name = "Bash", directory = "", startup = "", handle = "";
        transient String packageRuntimeStartup="";
        transient PackageOperationJournal.Intent packageOperation;
        public String startup2 = "", startup3 = "", environment = "legacy";
        public String prootAlias="";
        public boolean prootIsolated;
        public String sshHost = "", sshUser = "root";
        public int sshPort = 22;
        public int chatScale=100;
        public boolean restoreReview;
        public boolean sshKeepOpen = true;
        public String sshAuth = "auto", sshIdentity = "", sshEncryptedPassword = "";
        public String codexEnvironment = "ubuntu", codexAccess = "full", codexChat = "new", codexThread = "", codexThreadName = "", codexSocketId = UUID.randomUUID().toString();
        public String codexSourceDirectory = "";
        /** Old live run must not overwrite an explicitly edited next-launch chat. */
        public String codexBindingBlockedRun = "";
        public Boolean codexConfirmActions; // null preserves legacy approval mapping
        public boolean aiProjectOnly;
        public String aiAgent = "", aiConnection = "00000000-0000-4000-8000-000000000002", aiModel = "", aiVariant = "", aiMode = "build", aiAccess = "ask", aiChat = "new", aiThread = "ses" + UUID.randomUUID().toString().replace("-", "");
        /** Empty preserves the pre-audit socket path until the next explicit launch. */
        public String aiSocketId="";
        /** Explicit user bookmark; current runtime thread is otherwise temporary. */
        public String aiSavedThread = "";
        // Optional v8 fields: older readers retain a plain Ubuntu profile.
        public boolean claudeAgent;
        public String claudeEffort="default";
        public String claudeModel="", claudeMode="manual", claudeChat="new", claudeThread=UUID.randomUUID().toString(), claudeSocketId=UUID.randomUUID().toString();
        public transient boolean agentSetupAllowed;
        public boolean geminiAgent;
        public boolean extendedDebug;
        public String agentConnectionCommand="";
        public boolean keepHistory=false;
        public String geminiThinking="default";
        public String geminiEncryptedKey=""; // Device/profile-bound; copies and imports require a new key.
        public String geminiCloudProject="",geminiCloudLocation="global";
        public String geminiAuth="api", geminiModel="", geminiMode="default", geminiChat="new", geminiThread=UUID.randomUUID().toString(), geminiSocketId=UUID.randomUUID().toString();
        public long updated;
        public boolean saved;
        public List<String> startupCommands() {
            List<String> result = new ArrayList<>();
            for (String command : new String[]{startup, startup2, startup3}) if (!command.trim().isEmpty()) result.add(command);
            return result;
        }
        public Session copy() {
            Session s = new Session();s.restoreReview=restoreReview;s.keepHistory=keepHistory;s.chatScale=chatScale;
            s.extendedDebug=extendedDebug;s.agentConnectionCommand=agentConnectionCommand; s.name = name + WorkspaceText.tr(" — копия"); s.directory = directory; s.startup = startup; s.startup2 = startup2; s.startup3 = startup3; s.environment = environment;s.prootAlias=prootAlias;s.prootIsolated=prootIsolated;
            s.sshHost = sshHost; s.sshUser = sshUser; s.sshPort = sshPort; s.sshKeepOpen = sshKeepOpen; s.sshAuth = sshAuth; s.sshIdentity = sshIdentity; // Password is bound to the original profile ID.
            s.codexConfirmActions=codexConfirmActions; s.aiProjectOnly=aiProjectOnly; s.codexEnvironment = codexEnvironment; s.codexAccess = codexAccess; s.codexChat = codexChat; s.codexThread = codexThread; s.codexThreadName = codexThreadName; s.codexSourceDirectory=codexSourceDirectory;
            s.aiAgent=aiAgent; s.aiConnection=aiConnection; s.aiModel=aiModel; s.aiVariant=aiVariant; s.aiMode=aiMode; s.aiAccess=aiAccess;
            s.claudeEffort=claudeEffort;s.claudeAgent=claudeAgent; s.claudeModel=claudeModel; s.claudeMode=claudeMode;
            s.geminiCloudProject=geminiCloudProject;s.geminiCloudLocation=geminiCloudLocation;s.geminiThinking=geminiThinking;s.geminiAgent=geminiAgent; s.geminiAuth=geminiAuth; s.geminiModel=geminiModel; s.geminiMode=geminiMode;
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
    public final List<Alias> aliases = new ArrayList<>();
    public final List<Note> notes = new ArrayList<>();
    public final List<Session> sessions = new ArrayList<>();
    public final List<RecentCommand> recentCommands = new ArrayList<>();
    private final WorkspacePersistence persistence;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ErrorListener errors;
    private boolean writable = true;
    private volatile boolean closed;

    public WorkspaceStore(Context context, ErrorListener errors) {
        this(context,errors,new AtomicFile(new File(context.getFilesDir(), "workspace-v1.json")));
    }
    WorkspaceStore(Context context, ErrorListener errors,AtomicFile storage) {
        this.errors = errors;
        persistence = new WorkspacePersistence(storage,()->main.post(()->{if(!closed)errors.onError(WorkspaceText.tr("Ошибка записи библиотеки. Проверьте свободное место."));}));
        try { byte[] data=persistence.read();if(data!=null)decode(new String(data, StandardCharsets.UTF_8)); }
        catch (IOException | JSONException e) {
            // Keep the original file; never overwrite an unreadable user's library.
            writable = false;
            main.post(() -> {if(!closed)errors.onError(WorkspaceText.tr("Не удалось прочитать библиотеку. Исходный файл сохранён."));});
        }
    }
    public boolean isWritable() { return writable&&!closed; }
    public void recordCommand(String command) {
        if (!isWritable() || command.trim().isEmpty()) return;
        recentCommands.add(0, new RecentCommand(command, System.currentTimeMillis()));
        while (recentCommands.size() > MAX_RECENT_COMMANDS) recentCommands.remove(recentCommands.size() - 1);
        save();
    }
    public void clearRecentCommands() {
        if (!isWritable()) return;
        recentCommands.clear(); save();
    }
    public Session findByHandle(String handle) {
        for (Session s : sessions) if (!handle.isEmpty() && handle.equals(s.handle)) return s;
        return null;
    }
    boolean applyGeminiMode(Session profile,String run,String before,String mode){
        if(!isWritable()||!sessions.contains(profile)||!profile.geminiAgent||!profile.geminiSocketId.equals(run)||!profile.geminiMode.equals(before)||!java.util.Arrays.asList("default","auto_edit","plan").contains(mode))return false;
        profile.geminiMode=mode;profile.updated=System.currentTimeMillis();save();return true;
    }
    public void save() { save(null); }
    void save(Runnable completed) {
        if(closed)return;
        if (!writable) { errors.onError(WorkspaceText.tr("Библиотека недоступна для записи; исходные данные сохранены.")); return; }
        final byte[] snapshot;
        try { snapshot = encode().toString().getBytes(StandardCharsets.UTF_8); }
        catch (JSONException e) { errors.onError(WorkspaceText.tr("Не удалось сохранить библиотеку.")); return; }
        persistence.write(snapshot,()->{if(completed!=null)main.post(()->{if(!closed)completed.run();});});
    }
    void awaitWrites() throws Exception { persistence.awaitWrites(); }
    public void close() { closed=true;persistence.close(); } // Queued atomic writes finish after Activity destruction.
    static JSONObject sessionJson(Session s) throws JSONException {
        if(s.chatScale<50||s.chatScale>150)throw new JSONException("Invalid chat scale");
        return new JSONObject().put("id", s.id).put("name", s.name)
            .put("chatScale",s.chatScale)
            .put("directory", s.directory).put("prootAlias",s.prootAlias).put("prootIsolated",s.prootIsolated).put("prootStartup",s.prootAlias.isEmpty()?JSONObject.NULL:new JSONArray(Arrays.asList(s.startup,s.startup2,s.startup3))).put("startup", s.prootAlias.isEmpty()?s.startup:WorkspaceEnvironments.OLD_READER_GUARD).put("startup2", s.prootAlias.isEmpty()?s.startup2:"").put("startup3", s.prootAlias.isEmpty()?s.startup3:"").put("sshAuth", s.sshAuth).put("sshIdentity", s.sshIdentity).put("sshEncryptedPassword", s.sshEncryptedPassword).put("sshHost", s.sshHost).put("sshUser", s.sshUser).put("sshPort", s.sshPort).put("sshKeepOpen", s.sshKeepOpen).put("environment", s.environment).put("handle", s.handle)
            .put("codexConfirmActions",s.codexConfirmActions==null?JSONObject.NULL:s.codexConfirmActions).put("aiProjectOnly",s.aiProjectOnly)
            .put("codexEnvironment", s.codexEnvironment).put("codexAccess", s.codexAccess).put("codexChat", s.codexChat).put("codexThread", s.codexThread).put("codexThreadName", s.codexThreadName).put("codexSocketId", s.codexSocketId).put("codexSourceDirectory",s.codexSourceDirectory).put("codexBindingBlockedRun",s.codexBindingBlockedRun)
            .put("aiAgent",s.aiAgent).put("aiConnection",s.aiConnection).put("aiSocketId",s.aiSocketId).put("aiModel",s.aiModel).put("aiVariant",s.aiVariant).put("aiMode",s.aiMode).put("aiAccess",s.aiAccess).put("aiChat",s.aiChat).put("aiThread",s.aiThread).put("aiSavedThread",s.aiSavedThread)
            .put("claudeEffort",s.claudeEffort).put("claudeAgent",s.claudeAgent).put("claudeModel",s.claudeModel).put("claudeMode",s.claudeMode).put("claudeChat",s.claudeChat).put("claudeThread",s.claudeThread).put("claudeSocketId",s.claudeSocketId)
            .put("keepHistory",s.keepHistory)
            .put("geminiEncryptedKey",s.geminiEncryptedKey).put("geminiCloudProject",s.geminiCloudProject).put("geminiCloudLocation",s.geminiCloudLocation).put("geminiThinking",s.geminiThinking).put("geminiAuth",s.geminiAuth).put("geminiAgent",s.geminiAgent).put("geminiModel",s.geminiModel).put("geminiMode",s.geminiMode).put("geminiChat",s.geminiChat).put("geminiThread",s.geminiThread).put("geminiSocketId",s.geminiSocketId)
            .put("agentConnectionCommand",s.agentConnectionCommand).put("restoreReview",s.restoreReview).put("extendedDebug",s.extendedDebug).put("updated", s.updated).put("saved", s.saved);
    }
    JSONObject encode() throws JSONException {
        JSONObject root = new JSONObject().put("version", 8);
        JSONArray cs = new JSONArray(), ss = new JSONArray();
        java.util.Set<String> commandIds=new java.util.HashSet<>(),sessionIds=new java.util.HashSet<>(),noteIds=new java.util.HashSet<>();
        for (Command c : commands) {uniqueId(c.id,commandIds);cs.put(new JSONObject().put("id", c.id).put("name", c.name)
            .put("command", c.command).put("category", c.category).put("directory", c.directory).put("favorite", c.favorite));}
        for (Session s : sessions) {uniqueId(s.id,sessionIds);ss.put(sessionJson(s));}
        JSONArray ns = new JSONArray();
        for (Note n : notes) {uniqueId(n.id,noteIds);ns.put(new JSONObject().put("id", n.id).put("name", n.name)
            .put("text", n.text).put("category", n.category).put("favorite", n.favorite));}
        JSONArray recent = new JSONArray();
        for (int i = 0; i < Math.min(recentCommands.size(), MAX_RECENT_COMMANDS); i++) {
            RecentCommand entry = recentCommands.get(i);
            recent.put(new JSONObject().put("command", entry.command).put("sentAt", entry.sentAt));
        }
        JSONArray als = new JSONArray();
        java.util.Set<String> aliasIds=new java.util.HashSet<>(),aliasNames=new java.util.HashSet<>();
        for(Alias a:aliases){uniqueId(a.id,aliasIds);if(!WorkspaceAliases.valid(a)||!aliasNames.add(a.environment+":"+a.name))throw new JSONException("Invalid alias");
            als.put(new JSONObject().put("id",a.id).put("name",a.name).put("command",a.command).put("environment",a.environment).put("category",a.category).put("favorite",a.favorite));}
        return root.put("aliases",als).put("commands", cs).put("sessions", ss).put("notes", ns).put("recentCommands", recent);
    }
    void decode(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        int version = root.getInt("version");
        if (version != 1 && version != 2 && version != 3 && version != 4 && version != 5 && version != 6 && version != 7 && version != 8) throw new JSONException("Unsupported workspace version");
        List<Command> cs = new ArrayList<>(); List<Session> ss = new ArrayList<>();
        java.util.Set<String> commandIds=new java.util.HashSet<>(),sessionIds=new java.util.HashSet<>(),noteIds=new java.util.HashSet<>();
        JSONArray ca = root.getJSONArray("commands"), sa = root.getJSONArray("sessions");
        for (int i = 0; i < ca.length(); i++) {
            JSONObject o = ca.getJSONObject(i); Command c = new Command();
            c.id = uniqueId(o.getString("id"),commandIds); c.name = o.getString("name"); c.command = o.getString("command");
            c.category = o.optString("category", WorkspaceText.tr("Общее")); c.directory = o.optString("directory"); c.favorite = o.optBoolean("favorite");
            cs.add(c);
        }
        for (int i = 0; i < sa.length(); i++) {
            JSONObject o = sa.getJSONObject(i);
            // Retired local-AI profiles must never be reinterpreted as shell sessions.
            if (o.optBoolean("localAi", false)) continue;
            Session s = new Session();
            if(o.has("chatScale")&&(!(o.get("chatScale") instanceof Number)||o.getDouble("chatScale")!=o.getInt("chatScale")||o.getInt("chatScale")<50||o.getInt("chatScale")>150))throw new JSONException("Invalid chat scale");
            s.chatScale=o.optInt("chatScale",100);
            if(o.has("agentConnectionCommand")&&!(o.get("agentConnectionCommand") instanceof String))throw new JSONException("Invalid agent connection command type");
            s.agentConnectionCommand=o.optString("agentConnectionCommand","");if(!AgentConnection.valid(s.agentConnectionCommand))throw new JSONException("Invalid agent connection executable");
            s.id = uniqueId(o.getString("id"),sessionIds); s.name = o.getString("name"); s.directory = o.optString("directory");
            s.startup = o.optString("startup"); s.startup2 = o.optString("startup2"); s.startup3 = o.optString("startup3"); s.environment = o.optString("environment", "legacy");
            if (!s.environment.equals("legacy") && !s.environment.equals("termux") && !s.environment.equals("ubuntu") && !s.environment.equals("ssh") && !s.environment.equals("codex")) throw new JSONException("Unknown environment"); s.handle = o.optString("handle");
            if(o.has("prootAlias")&&!(o.get("prootAlias") instanceof String))throw new JSONException("Invalid environment alias");
            s.prootAlias=o.optString("prootAlias","");
            if(o.has("prootIsolated")&&!(o.get("prootIsolated") instanceof Boolean))throw new JSONException("Invalid environment isolation");
            s.prootIsolated=o.optBoolean("prootIsolated");
            if(s.prootIsolated&&s.prootAlias.isEmpty())throw new JSONException("Missing isolated environment alias");
            if(!s.prootAlias.isEmpty()){
                if(!s.environment.equals("termux")||!WorkspaceLocalFilePicker.validAlias(s.prootAlias)||o.optBoolean("geminiAgent")||o.optBoolean("claudeAgent")||!o.optString("aiAgent").isEmpty())throw new JSONException("Invalid environment binding");
                JSONArray commands=o.optJSONArray("prootStartup");if(commands==null||commands.length()!=3)throw new JSONException("Invalid environment startup");
                for(int k=0;k<3;k++)if(!(commands.get(k) instanceof String))throw new JSONException("Invalid environment command");
                s.startup=commands.getString(0);s.startup2=commands.getString(1);s.startup3=commands.getString(2);
            }

            s.sshHost = o.optString("sshHost", ""); s.sshUser = o.optString("sshUser", "root");
            s.sshAuth = o.optString("sshAuth", "auto"); s.sshIdentity = o.optString("sshIdentity", "");
            s.sshEncryptedPassword = o.optString("sshEncryptedPassword", "");
            if (!s.sshAuth.equals("auto") && !s.sshAuth.equals("key") && !s.sshAuth.equals("password")) throw new JSONException("Unknown SSH authentication");
            if (s.environment.equals("ssh") && s.sshAuth.equals("key") && !SshSessionShell.validIdentity(s.sshIdentity)) throw new JSONException("Invalid SSH identity");
            s.sshPort = o.optInt("sshPort", 22); s.sshKeepOpen = o.optBoolean("sshKeepOpen", true);
            if (s.environment.equals("ssh") && (version < 6 || !SshSessionShell.validHost(s.sshHost)
                || !SshSessionShell.validUser(s.sshUser) || !SshSessionShell.validPort(s.sshPort))) throw new JSONException("Invalid SSH settings");
            if(o.has("codexConfirmActions")&&!o.isNull("codexConfirmActions")&&!(o.get("codexConfirmActions") instanceof Boolean))throw new JSONException("Invalid Codex approval policy");
            if(o.has("aiProjectOnly")&&!(o.get("aiProjectOnly") instanceof Boolean))throw new JSONException("Invalid agent project scope");
            s.codexConfirmActions=o.has("codexConfirmActions")&&!o.isNull("codexConfirmActions")?o.getBoolean("codexConfirmActions"):null; s.aiProjectOnly=o.optBoolean("aiProjectOnly",false);
            s.codexEnvironment = o.optString("codexEnvironment", "ubuntu"); if(!AgentEnvironments.ubuntu(s.codexEnvironment)&&!s.codexEnvironment.equals("termux"))throw new JSONException("Invalid agent environment"); s.codexAccess = o.optString("codexAccess", "full");
            s.codexBindingBlockedRun=o.optString("codexBindingBlockedRun", "");
            s.codexChat = o.optString("codexChat", "new"); s.codexThread = o.optString("codexThread", ""); s.codexThreadName = o.optString("codexThreadName", ""); s.codexSocketId = validId(o.optString("codexSocketId", s.codexSocketId));
            if (s.environment.equals("codex") && (version < 8 || !(AgentEnvironments.ubuntu(s.codexEnvironment) || s.codexEnvironment.equals("termux"))
                || !(s.codexAccess.equals("full") || s.codexAccess.equals("project") || s.codexAccess.equals("read"))
                || !(s.codexChat.equals("new") || s.codexChat.equals("last") || s.codexChat.equals("choose") || (s.codexChat.equals("id") && CodexSessionShell.validThread(s.codexThread))))) throw new JSONException("Invalid Codex profile");
            s.codexSourceDirectory=o.optString("codexSourceDirectory","");
            if(!s.codexSourceDirectory.isEmpty()&&(!s.codexSourceDirectory.startsWith("/")||CodexProjects.directory(s.codexEnvironment,s.codexSourceDirectory).isEmpty()))throw new JSONException("Invalid confirmed chat directory");
            if(o.has("keepHistory")&&!(o.get("keepHistory") instanceof Boolean))throw new JSONException("Invalid history flag");
            s.keepHistory=o.optBoolean("keepHistory",false);
            s.aiAgent=o.optString("aiAgent",""); s.aiConnection=validId(o.optString("aiConnection",s.aiConnection)); s.aiModel=o.optString("aiModel",""); s.aiVariant=o.optString("aiVariant",""); s.aiMode=o.optString("aiMode","build"); s.aiAccess=o.optString("aiAccess","ask"); s.aiChat=o.optString("aiChat","new"); s.aiThread=o.optString("aiThread",s.aiThread);
            if(o.has("aiSocketId")&&!(o.get("aiSocketId") instanceof String))throw new JSONException("Invalid AI run");
            s.aiSocketId=o.optString("aiSocketId","");if(!s.aiSocketId.isEmpty())s.aiSocketId=validId(s.aiSocketId);
            s.aiSavedThread=o.optString("aiSavedThread","");
            if(!s.aiSavedThread.isEmpty()&&!AiAgents.validThread(s.aiSavedThread))throw new JSONException("Invalid saved AI thread");
            if(!s.aiAgent.isEmpty() && (!s.aiAgent.equals("opencode") || !s.environment.equals("ubuntu") || !AgentEnvironments.ubuntu(s.codexEnvironment) || !AiAgents.validThread(s.aiThread) || !AiAgents.validModel(s.aiModel) || !s.aiVariant.matches("[A-Za-z0-9_.-]{0,80}") || !(s.aiAccess.equals("full") || s.aiAccess.equals("ask") || s.aiAccess.equals("read")) || !(s.aiMode.equals("build") || s.aiMode.equals("plan")) || !(s.aiChat.equals("new") || s.aiChat.equals("resume"))))throw new JSONException("Invalid AI profile");
            s.claudeEffort=o.optString("claudeEffort","default");if(!java.util.Arrays.asList("default","low","medium","high","xhigh","max").contains(s.claudeEffort))s.claudeEffort="default";s.claudeAgent=o.optBoolean("claudeAgent",false); s.claudeModel=o.optString("claudeModel",""); s.claudeMode=o.optString("claudeMode","manual"); s.claudeChat=o.optString("claudeChat","new"); s.claudeThread=o.optString("claudeThread",s.claudeThread); s.claudeSocketId=o.optString("claudeSocketId",s.claudeSocketId);
            if(s.claudeAgent&&!ClaudeCode.valid(s))throw new JSONException("Invalid Claude profile");
            if(o.has("geminiEncryptedKey")&&!(o.get("geminiEncryptedKey") instanceof String))throw new JSONException("Invalid Gemini key record");
            s.geminiEncryptedKey=o.optString("geminiEncryptedKey","");if(!GeminiApiKeyVault.validRecord(s.geminiEncryptedKey))throw new JSONException("Invalid Gemini key record");
            s.geminiAuth=o.optString("geminiAuth","auto");s.geminiCloudProject=o.optString("geminiCloudProject","");s.geminiCloudLocation=o.optString("geminiCloudLocation","global");
            s.geminiThinking=o.optString("geminiThinking","default");if(!java.util.Arrays.asList("default","minimal","low","medium","high").contains(s.geminiThinking))s.geminiThinking="default";s.geminiAgent=o.optBoolean("geminiAgent",false); s.geminiModel=o.optString("geminiModel",""); s.geminiMode=o.optString("geminiMode","default"); s.geminiChat=o.optString("geminiChat","new"); s.geminiThread=o.optString("geminiThread",s.geminiThread); s.geminiSocketId=o.optString("geminiSocketId",s.geminiSocketId);
            if(s.geminiAgent&&!GeminiCli.valid(s))throw new JSONException("Invalid Gemini profile");
            s.restoreReview=o.optBoolean("restoreReview",false);
            s.extendedDebug=o.optBoolean("extendedDebug",false); s.updated = o.optLong("updated"); s.saved = o.optBoolean("saved", true); ss.add(s);
        }
        List<Note> ns = new ArrayList<>();
        JSONArray na = root.optJSONArray("notes");
        if (version >= 5 && na == null) throw new JSONException("Missing notes");
        if (na != null) for (int i = 0; i < na.length(); i++) {
            JSONObject o = na.getJSONObject(i); Note n = new Note();
            n.id = uniqueId(o.getString("id"),noteIds); n.name = o.getString("name"); n.text = o.getString("text");
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
        List<Alias> als=new ArrayList<>();JSONArray aa=root.optJSONArray("aliases");
        java.util.Set<String> aliasIds=new java.util.HashSet<>(),aliasNames=new java.util.HashSet<>();
        if(root.has("aliases")&&aa==null)throw new JSONException("Invalid aliases");
        if(aa!=null)for(int i=0;i<aa.length();i++){JSONObject o=aa.getJSONObject(i);Alias a=new Alias();a.id=uniqueId(o.getString("id"),aliasIds);
            a.name=o.getString("name");a.command=o.getString("command");a.environment=o.getString("environment");a.category=o.optString("category");a.favorite=o.optBoolean("favorite");
            if(!WorkspaceAliases.valid(a)||!aliasNames.add(a.environment+":"+a.name))throw new JSONException("Invalid alias");als.add(a);}
        aliases.clear();aliases.addAll(als);
        commands.clear(); commands.addAll(cs); sessions.clear(); sessions.addAll(ss);
        notes.clear(); notes.addAll(ns); recentCommands.clear(); recentCommands.addAll(recent);
    }
    private static String validId(String id) throws JSONException {
        try { return UUID.fromString(id).toString(); }
        catch (IllegalArgumentException e) { throw new JSONException("Invalid id"); }
    }
    private static String uniqueId(String id,java.util.Set<String> ids)throws JSONException{
        String normalized=validId(id);if(!ids.add(normalized))throw new JSONException("Duplicate workspace id");return normalized;
    }
    public static String tail(String text, int max) {
        if (text.length() <= max) return text;
        int start = text.length() - max;
        if (Character.isLowSurrogate(text.charAt(start))) start++;
        return text.substring(start);
    }
}
