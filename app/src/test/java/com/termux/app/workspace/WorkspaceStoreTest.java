package com.termux.app.workspace;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class WorkspaceStoreTest {
    private Context context;
    private final List<String> errors = new ArrayList<>();
    @Before public void setup() {
        context = RuntimeEnvironment.application;
        new File(context.getFilesDir(), "workspace-v1.json").delete();
        new File(context.getFilesDir(), "workspace-v1.json.bak").delete();
    }
    @Test public void v153LibraryWithoutNewAgentFieldsReopensWithoutReset() throws Exception {
        WorkspaceStore original=new WorkspaceStore(context,errors::add);
        WorkspaceStore.Command cmd=new WorkspaceStore.Command();cmd.name="Legacy command";cmd.command="pwd";original.commands.add(cmd);
        WorkspaceStore.Note note=new WorkspaceStore.Note();note.text="Legacy note";original.notes.add(note);
        WorkspaceStore.Session codex=new WorkspaceStore.Session();codex.environment="codex";codex.saved=true;codex.codexChat="id";codex.codexThread="01900000-0000-7000-8000-000000000001";codex.codexConfirmActions=false;codex.codexAccess="read";
        WorkspaceStore.Session gemini=new WorkspaceStore.Session();gemini.environment="ubuntu";gemini.geminiAgent=true;gemini.geminiModel="pro";gemini.geminiAuth="api";gemini.saved=true;
        WorkspaceStore.Session claude=new WorkspaceStore.Session();claude.environment="ubuntu";claude.claudeAgent=true;claude.claudeModel="sonnet";claude.saved=true;
        WorkspaceStore.Session ssh=new WorkspaceStore.Session();ssh.environment="ssh";ssh.sshHost="example.invalid";ssh.sshAuth="password";ssh.sshEncryptedPassword="1:iv:ciphertext";ssh.saved=true;
        original.sessions.add(codex);original.sessions.add(gemini);original.sessions.add(claude);original.sessions.add(ssh);
        org.json.JSONObject legacy=original.encode();original.close();
        for(int n=0;n<4;n++)for(String field:new String[]{"claudeEffort","geminiCloudProject","geminiCloudLocation","geminiThinking"})legacy.getJSONArray("sessions").getJSONObject(n).remove(field);
        try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),"workspace-v1.json"))){out.write(legacy.toString().getBytes(StandardCharsets.UTF_8));}
        WorkspaceStore loaded=new WorkspaceStore(context,errors::add);
        assertTrue(errors.toString(),errors.isEmpty());assertEquals(4,loaded.sessions.size());assertEquals("pwd",loaded.commands.get(0).command);assertEquals("Legacy note",loaded.notes.get(0).text);
        org.json.JSONArray roundtrip=loaded.encode().getJSONArray("sessions");
        for(int n=0;n<4;n++){org.json.JSONObject before=legacy.getJSONArray("sessions").getJSONObject(n),after=roundtrip.getJSONObject(n);java.util.Iterator<String> fields=before.keys();while(fields.hasNext()){String field=fields.next();assertEquals(field,String.valueOf(before.get(field)),String.valueOf(after.get(field)));}}
        assertEquals("default",loaded.sessions.get(2).claudeEffort);assertEquals("default",loaded.sessions.get(1).geminiThinking);assertEquals("",loaded.sessions.get(1).geminiCloudProject);assertEquals("global",loaded.sessions.get(1).geminiCloudLocation);
        loaded.save();loaded.awaitWrites();loaded.close();WorkspaceStore reopened=new WorkspaceStore(context,errors::add);assertEquals(4,reopened.sessions.size());assertEquals(codex.codexThread,reopened.sessions.get(0).codexThread);assertEquals("1:iv:ciphertext",reopened.sessions.get(3).sshEncryptedPassword);reopened.close();
    }
    @Test public void retiredLocalProfilesAreSkippedWithoutResettingOtherData() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Command command = new WorkspaceStore.Command(); command.name="Keep command";command.command="pwd";store.commands.add(command);
        WorkspaceStore.Note note = new WorkspaceStore.Note();note.text="Keep note";store.notes.add(note);
        WorkspaceStore.Session shell = new WorkspaceStore.Session();shell.environment="ubuntu";shell.keepHistory=true;shell.saved=true;shell.startup="pwd";
        WorkspaceStore.Session codex = new WorkspaceStore.Session();codex.environment="codex";codex.codexChat="id";codex.codexThread="01900000-0000-7000-8000-000000000001";codex.codexConfirmActions=false;codex.codexAccess="read";
        WorkspaceStore.Session ssh = new WorkspaceStore.Session();ssh.environment="ssh";ssh.sshHost="example.invalid";ssh.sshEncryptedPassword="ciphertext";
        store.sessions.add(shell);store.sessions.add(codex);store.sessions.add(ssh);store.recordCommand("keep recent");store.awaitWrites();
        org.json.JSONObject data=store.encode();org.json.JSONArray profiles=data.getJSONArray("sessions");
        profiles.put(new org.json.JSONObject().put("localAi",true).put("environment","termux").put("localChat","retired").put("localModel","old"));
        // Obsolete local-only settings on ordinary sessions no longer invalidate the library.
        profiles.getJSONObject(0).put("localContext","obsolete").put("localBehavior",false);
        store.decode(data.toString());
        assertEquals(3,store.sessions.size());assertEquals(shell.id,store.sessions.get(0).id);assertTrue(store.sessions.get(0).keepHistory);assertTrue(store.sessions.get(0).copy().keepHistory);
        assertEquals(codex.codexThread,store.sessions.get(1).codexThread);assertEquals(Boolean.FALSE,store.sessions.get(1).codexConfirmActions);assertEquals("read",store.sessions.get(1).codexAccess);
        assertEquals("ciphertext",store.sessions.get(2).sshEncryptedPassword);assertEquals("pwd",store.commands.get(0).command);assertEquals("Keep note",store.notes.get(0).text);assertEquals("keep recent",store.recentCommands.get(0).command);
        org.json.JSONObject encoded=store.encode();assertEquals(8,encoded.getInt("version"));assertEquals(3,encoded.getJSONArray("sessions").length());assertFalse(encoded.toString().contains("localAi"));assertFalse(encoded.toString().contains("localChat"));store.close();
    }
    @Test public void passwordProfilesPersistOnlyCiphertextAndMigrateSshVersionSix() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add); WorkspaceStore.Session s = new WorkspaceStore.Session();
        s.environment = "ssh"; s.sshHost = "example.invalid"; s.sshAuth = "password"; s.sshEncryptedPassword = "1:iv:ciphertext";
        store.sessions.add(s); store.decode(store.encode().toString());
        assertEquals("password", store.sessions.get(0).sshAuth); assertEquals("1:iv:ciphertext", store.sessions.get(0).sshEncryptedPassword);
        assertEquals("", store.sessions.get(0).copy().sshEncryptedPassword);
        org.json.JSONObject old = store.encode(); old.put("version", 6);
        org.json.JSONObject profile = old.getJSONArray("sessions").getJSONObject(0);
        profile.remove("sshAuth"); profile.remove("sshIdentity"); profile.remove("sshEncryptedPassword");
        store.decode(old.toString()); assertEquals("auto", store.sessions.get(0).sshAuth);
        assertEquals("", store.sessions.get(0).sshEncryptedPassword); store.close();
    }
    @Test public void sshProfilesRoundTripAndCopyWithoutLosingExistingData() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Session ssh = new WorkspaceStore.Session(); ssh.environment = "ssh";
        ssh.sshHost = "example.invalid"; ssh.sshUser = "deploy"; ssh.sshPort = 2222;
        ssh.sshKeepOpen = false; ssh.directory = "~/project"; ssh.startup = "pwd"; ssh.startup2 = "git status";
        store.sessions.add(ssh); store.decode(store.encode().toString());
        WorkspaceStore.Session restored = store.sessions.get(0).copy();
        assertEquals("ssh", restored.environment); assertEquals("example.invalid", restored.sshHost);
        assertEquals("deploy", restored.sshUser); assertEquals(2222, restored.sshPort);
        assertFalse(restored.sshKeepOpen); assertEquals("git status", restored.startup2);
        org.json.JSONObject old = store.encode(); old.put("version", 5); old.getJSONArray("sessions").getJSONObject(0).put("environment", "termux");
        store.decode(old.toString()); assertEquals("termux", store.sessions.get(0).environment);
        store.close();
    }
    @Test public void explicitSavedFlagPersistsAndLegacySessionsRemainSaved() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Session temporary = new WorkspaceStore.Session(); temporary.name = "Temporary";
        WorkspaceStore.Session saved = new WorkspaceStore.Session(); saved.name = "Saved"; saved.saved = true;
        store.sessions.add(temporary); store.sessions.add(saved); store.save(); store.awaitWrites(); store.close();
        WorkspaceStore restored = new WorkspaceStore(context, errors::add);
        assertFalse(restored.sessions.get(0).saved); assertTrue(restored.sessions.get(1).saved);
        org.json.JSONObject legacy = restored.encode();
        for (int i = 0; i < legacy.getJSONArray("sessions").length(); i++) legacy.getJSONArray("sessions").getJSONObject(i).remove("saved");
        restored.decode(legacy.toString());
        assertTrue(restored.sessions.get(0).saved); assertTrue(restored.sessions.get(1).saved);
        restored.close(); assertTrue(errors.isEmpty());
    }
    @Test public void savesAndReopensCommandsProfilesAndIndependentHistory() throws Exception {
        WorkspaceStore first = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Command command = new WorkspaceStore.Command();
        command.name = "Собрать APK"; command.command = "./gradlew assembleDebug\nprintf 'готово'";
        command.category = "Сборка"; command.directory = "~/проект с пробелами"; command.favorite = true;
        first.commands.add(command);
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        session.name = "Codex"; session.directory = "/data/data/com.termux/files/home/project";
        session.startup = "codex"; session.handle = "old-runtime-handle";
        first.sessions.add(session); first.save(); first.awaitWrites(); first.close();
        WorkspaceStore reopened = new WorkspaceStore(context, errors::add);
        assertEquals(1, reopened.commands.size()); assertEquals(command.command, reopened.commands.get(0).command);
        assertEquals(command.directory, reopened.commands.get(0).directory); assertTrue(reopened.commands.get(0).favorite);
        assertEquals(session.startup, reopened.sessions.get(0).startup);
        assertNotEquals(session.id, reopened.sessions.get(0).copy().id);
        assertEquals("", reopened.sessions.get(0).copy().handle);
        assertNotEquals(command.id, reopened.commands.get(0).copy().id);
        assertTrue(errors.isEmpty()); reopened.close();
    }
    @Test public void legacySnapshotsDoNotPreventRestoringSessionSettings() throws Exception {
        WorkspaceStore original = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        session.name = "Codex"; session.environment = "ubuntu"; session.directory = "/root"; session.startup = "codex";
        original.sessions.add(session);
        org.json.JSONObject legacy = original.encode();
        legacy.getJSONArray("sessions").getJSONObject(0).put("transcript", "старый снимок");
        original.close();
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), "workspace-v1.json"))) {
            out.write(legacy.toString().getBytes(StandardCharsets.UTF_8));
        }
        WorkspaceStore restored = new WorkspaceStore(context, errors::add);
        assertTrue(restored.isWritable()); assertEquals("Codex", restored.sessions.get(0).name);
        assertEquals("ubuntu", restored.sessions.get(0).environment); assertEquals("codex", restored.sessions.get(0).startup);
        assertFalse(restored.encode().getJSONArray("sessions").getJSONObject(0).has("transcript"));
        assertTrue(errors.isEmpty()); restored.close();
    }
    @Test public void threeStartupCommandsPersistDuplicateAndReadLegacy() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        session.startup = "cd /tmp"; session.startup2 = "export BUILD=1"; session.startup3 = "printf '%s' \"$BUILD\"";
        store.sessions.add(session); store.save(); store.awaitWrites(); store.close();
        WorkspaceStore reopened = new WorkspaceStore(context, errors::add);
        assertEquals(session.startupCommands(), reopened.sessions.get(0).startupCommands());
        assertEquals(session.startupCommands(), reopened.sessions.get(0).copy().startupCommands());
        org.json.JSONObject legacy = reopened.encode().put("version", 2);
        legacy.getJSONArray("sessions").getJSONObject(0).remove("startup2");
        legacy.getJSONArray("sessions").getJSONObject(0).remove("startup3");
        reopened.decode(legacy.toString());
        assertEquals(java.util.Collections.singletonList("cd /tmp"), reopened.sessions.get(0).startupCommands());
        reopened.close(); assertTrue(errors.isEmpty());
    }
    @Test public void startupSequencePreservesEnvironmentAndStopsOnFailure() throws Exception {
        File folder = new File(context.getFilesDir(), "sequence"); assertTrue(folder.mkdirs() || folder.isDirectory());
        String output = runStartup(folder, java.util.Arrays.asList("cd " + SessionShell.quote(folder.getAbsolutePath()),
            "export FORK_SEQUENCE_VALUE='two lines'", "printf '%s|%s' \"$PWD\" \"$FORK_SEQUENCE_VALUE\""));
        assertEquals(folder.getAbsolutePath() + "|two lines", output);
        assertEquals("before", runStartup(folder, java.util.Arrays.asList("printf before", "false", "printf after")));
    }
    private String runStartup(File folder, java.util.List<String> commands) throws Exception {
        File script = new File(folder, "run.sh");
        java.nio.file.Files.write(script.toPath(), SessionShell.buildRc(folder.getAbsolutePath(),
            new File(folder, "history").getAbsolutePath(), commands).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("/bin/bash", script.getAbsolutePath()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024]; int n;
        while ((n = process.getInputStream().read(buffer)) != -1) output.write(buffer, 0, n);
        assertTrue(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
    @Test public void environmentPersistsCopiesAndOldProfilesRemainLegacy() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Session session = new WorkspaceStore.Session(); session.environment = "ubuntu"; session.directory = "/root";
        store.sessions.add(session); store.save(); store.awaitWrites(); store.close();
        WorkspaceStore reopened = new WorkspaceStore(context, errors::add);
        assertEquals("ubuntu", reopened.sessions.get(0).environment);
        assertEquals("ubuntu", reopened.sessions.get(0).copy().environment);
        org.json.JSONObject old = reopened.encode().put("version", 3);
        old.getJSONArray("sessions").getJSONObject(0).remove("environment"); reopened.decode(old.toString());
        assertEquals("legacy", reopened.sessions.get(0).environment); reopened.close();
    }
    @Test public void bashReadinessRunsAfterInitializationOnlyOnceAndHonorsCancel() throws Exception {
        File folder = new File(context.getFilesDir(), "ready"); assertTrue(folder.mkdirs() || folder.isDirectory());
        java.nio.file.Files.write(new File(folder, ".bashrc").toPath(), "export FORK_READY_TEST=initialized\n".getBytes(StandardCharsets.UTF_8));
        File cancel = new File(folder, "cancel");
        if (cancel.exists()) assertTrue(cancel.delete());
        String output = runReady(folder, cancel, java.util.Arrays.asList("printf 'FIRST:%s\\n' \"$FORK_READY_TEST\"", "printf 'SECOND\\n'"));
        assertTrue(output.contains("FIRST:initialized")); assertTrue(output.contains("SECOND"));
        assertEquals(output.indexOf("FIRST:initialized"), output.lastIndexOf("FIRST:initialized"));
        assertTrue(cancel.createNewFile());
        output = runReady(folder, cancel, java.util.Arrays.asList("printf SHOULD_NOT_RUN"));
        assertFalse(output.contains("SHOULD_NOT_RUN"));
        assertTrue(cancel.delete());
        output = runReady(folder, cancel, java.util.Arrays.asList("false", "printf SHOULD_NOT_RUN"));
        assertFalse(output.contains("SHOULD_NOT_RUN"));
    }
    private String runReady(File folder, File cancel, java.util.List<String> commands) throws Exception {
        File rc = new File(folder, "bashrc");
        java.nio.file.Files.write(rc.toPath(), SessionShell.buildReadyRc(folder.getAbsolutePath(), new File(folder, "history").getAbsolutePath(),
            cancel.getAbsolutePath(), commands, false).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("/bin/bash", "--rcfile", rc.getAbsolutePath(), "-i")
            .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        process.getOutputStream().write(":\nexit\n".getBytes(StandardCharsets.UTF_8)); process.getOutputStream().close();
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[1024]; int n;
        while ((n = process.getInputStream().read(buffer)) != -1) output.write(buffer, 0, n);
        assertTrue(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
    @Test public void ubuntuPreparationKeepsCommandsInGuestAndBothScriptsHaveValidSyntax() throws Exception {
        WorkspaceStore.Session session = new WorkspaceStore.Session(); session.environment = "ubuntu"; session.directory = "/root/project with space";
        session.startup = "printf '%s' \"quoted' value\""; session.startup2 = "false";
        File rc = SessionShell.prepare(context.getFilesDir(), session);
        String host = new String(java.nio.file.Files.readAllBytes(rc.toPath()), StandardCharsets.UTF_8);
        assertTrue(host.contains("proot-distro login --bind")); assertTrue(host.contains("--work-dir '/root/project with space' ubuntu -- /bin/bash --rcfile"));
        assertFalse(host.contains(session.startup));
        File guest = new File(rc.getParentFile(), "ubuntu-bashrc");
        String script = new String(java.nio.file.Files.readAllBytes(guest.toPath()), StandardCharsets.UTF_8);
        assertTrue(script.contains("\"$HOME/.bashrc\"")); assertTrue(script.contains("PROMPT_COMMAND+=('_termux_workspace_ready')"));
        for (File file : java.util.Arrays.asList(rc, guest)) {
            Process check = new ProcessBuilder("/bin/bash", "-n", file.getAbsolutePath()).start();
            assertTrue(check.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)); assertEquals(0, check.exitValue());
        }
    }
    @Test public void orderedWritesKeepNewestSnapshot() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Command command = new WorkspaceStore.Command(); store.commands.add(command);
        command.name = "first"; store.save(); command.name = "second"; store.save();
        store.awaitWrites(); store.close();
        WorkspaceStore reopened = new WorkspaceStore(context, errors::add);
        assertEquals("second", reopened.commands.get(0).name); reopened.close();
    }
    @Test public void recentCommandsKeepNewest50PersistAndClearIndependently() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        store.commands.add(new WorkspaceStore.Command());
        store.sessions.add(new WorkspaceStore.Session());
        for (int i = 0; i < 55; i++) store.recordCommand("echo " + i);
        store.recordCommand("  ");
        store.awaitWrites(); store.close();
        WorkspaceStore reopened = new WorkspaceStore(context, errors::add);
        assertEquals(50, reopened.recentCommands.size());
        assertEquals("echo 54", reopened.recentCommands.get(0).command);
        assertEquals("echo 5", reopened.recentCommands.get(49).command);
        assertTrue(reopened.recentCommands.get(0).sentAt > 0);
        reopened.clearRecentCommands(); reopened.awaitWrites(); reopened.close();
        WorkspaceStore cleared = new WorkspaceStore(context, errors::add);
        assertTrue(cleared.recentCommands.isEmpty());
        assertEquals(1, cleared.commands.size()); assertEquals(1, cleared.sessions.size());
        cleared.close(); assertTrue(errors.isEmpty());
    }
    @Test public void oldWorkspaceWithoutJournalLoadsWithEmptyHistory() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        org.json.JSONObject old = store.encode().put("version", 1);
        old.put("recentCommands", new org.json.JSONArray().put("echo legacy"));
        store.decode(old.toString());
        assertEquals("echo legacy", store.recentCommands.get(0).command);
        assertEquals(0, store.recentCommands.get(0).sentAt);
        old.remove("recentCommands");
        store.decode(old.toString()); assertTrue(store.recentCommands.isEmpty());
        assertTrue(store.isWritable()); store.close();
    }
    @Test public void journalKeepsRepeatedCommandsAsSeparateSentEntries() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        store.recordCommand("echo repeat"); store.recordCommand("echo other"); store.recordCommand("echo repeat");
        store.save(); store.awaitWrites(); store.close();
        WorkspaceStore restored = new WorkspaceStore(context, errors::add);
        assertEquals(3, restored.recentCommands.size());
        assertEquals("echo repeat", restored.recentCommands.get(0).command);
        assertEquals("echo other", restored.recentCommands.get(1).command);
        assertEquals("echo repeat", restored.recentCommands.get(2).command); restored.close();
    }
    @Test public void notesRoundTripAndLegacyLibraryUpgradePreserveCommandsAndSessions() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Command command = new WorkspaceStore.Command(); command.command = "echo first\necho second"; store.commands.add(command);
        WorkspaceStore.Session session = new WorkspaceStore.Session(); session.saved = true; store.sessions.add(session);
        org.json.JSONObject old = store.encode().put("version", 4); old.remove("notes");
        store.decode(old.toString()); assertTrue(store.notes.isEmpty());
        WorkspaceStore.Note note = new WorkspaceStore.Note(); note.name = "Инструкция"; note.text = "Строка 1\nСтрока 2";
        note.category = "Сборка"; note.favorite = true; store.notes.add(note); store.save(); store.awaitWrites(); store.close();
        WorkspaceStore restored = new WorkspaceStore(context, errors::add);
        assertEquals(8, restored.encode().getInt("version")); assertEquals(1, restored.notes.size());
        assertEquals(note.id, restored.notes.get(0).id); assertEquals(note.text, restored.notes.get(0).text);
        assertEquals("Сборка", restored.notes.get(0).category); assertTrue(restored.notes.get(0).favorite);
        assertEquals(command.command, restored.commands.get(0).command); assertEquals(session.id, restored.sessions.get(0).id);
        restored.close(); assertTrue(errors.isEmpty());
    }
    @Test public void codexProfilesPersistAndRejectInjectedThreadIds() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Session profile = new WorkspaceStore.Session(); profile.environment = "codex";
        profile.codexEnvironment = "ubuntu"; profile.codexAccess = "project"; profile.codexChat = "id";
        profile.codexThread = "thread-123"; profile.codexThreadName = "Проект"; store.sessions.add(profile);
        org.json.JSONObject data = store.encode(); store.decode(data.toString());
        assertEquals(profile.codexSocketId, store.sessions.get(0).codexSocketId);
        assertNotEquals(profile.codexSocketId, profile.copy().codexSocketId);
        assertEquals("project", store.sessions.get(0).codexAccess); assertEquals("thread-123", store.sessions.get(0).copy().codexThread);
        data.getJSONArray("sessions").getJSONObject(0).put("codexThread", "bad; command");
        try { store.decode(data.toString()); fail("invalid thread accepted"); } catch (org.json.JSONException expected) {}
        assertEquals("thread-123", store.sessions.get(0).codexThread);
        data.getJSONArray("sessions").getJSONObject(0).put("codexThread", "thread-123"); data.put("version", 7);
        try { store.decode(data.toString()); fail("old Codex schema accepted"); } catch (org.json.JSONException expected) {}
        store.close();
    }
    @Test public void confirmedChatOriginRoundTripsAndOlderProfilesDefaultToNoConsent() throws Exception {
        WorkspaceStore store=new WorkspaceStore(context,errors::add);WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment="codex";profile.codexChat="id";profile.codexThread="thread-id";profile.directory="/root/project";profile.codexSourceDirectory="/root";store.sessions.add(profile);
        org.json.JSONObject data=store.encode();store.decode(data.toString());assertEquals("/root",store.sessions.get(0).codexSourceDirectory);assertEquals("/root",store.sessions.get(0).copy().codexSourceDirectory);
        data.getJSONArray("sessions").getJSONObject(0).remove("codexSourceDirectory");store.decode(data.toString());assertEquals("",store.sessions.get(0).codexSourceDirectory);
        data.getJSONArray("sessions").getJSONObject(0).put("codexSourceDirectory","relative");try{store.decode(data.toString());fail("invalid origin accepted");}catch(org.json.JSONException expected){}assertEquals("",store.sessions.get(0).codexSourceDirectory);store.close();
    }
    @Test public void corruptOrFutureFileIsNeverOverwritten() throws Exception {
        File file = new File(context.getFilesDir(), "workspace-v1.json");
        byte[] original = "{\"version\":99}".getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(original); }
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        assertFalse(store.isWritable()); store.commands.add(new WorkspaceStore.Command()); store.save(); store.awaitWrites(); store.close();
        assertEquals(new String(original, StandardCharsets.UTF_8), new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }
    @Test public void backupFromInterruptedWriteRestoresLibrary() throws Exception {
        WorkspaceStore original = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Command c = new WorkspaceStore.Command(); c.name = "Keep me";
        original.commands.add(c); original.save(); original.awaitWrites(); original.close();
        File base = new File(context.getFilesDir(), "workspace-v1.json");
        assertTrue(base.renameTo(new File(base.getPath() + ".bak")));
        WorkspaceStore recovered = new WorkspaceStore(context, errors::add);
        assertEquals("Keep me", recovered.commands.get(0).name); assertTrue(recovered.isWritable()); recovered.close();
    }
    @Test public void boundsTranscriptWithoutSplittingSurrogatePair() {
        assertEquals("abc", WorkspaceStore.tail("abc", 3));
        assertEquals("ab", WorkspaceStore.tail("x\ud83d\ude00ab", 3));
    }
    @Test public void bashHistoryAndStartupSurviveProfileRestart() throws Exception {
        WorkspaceStore.Session session = new WorkspaceStore.Session(); session.startup = "printf '%s\\n' hello";
        File rc = SessionShell.prepare(context.getFilesDir(), session);
        String config = new String(java.nio.file.Files.readAllBytes(rc.toPath()), StandardCharsets.UTF_8);
        assertTrue(config.contains("PROMPT_COMMAND+=('history -a')")); assertTrue(config.contains("history -s --"));
        File history = new File(SessionShell.directory(context.getFilesDir(), session.id), "history");
        try (FileOutputStream out = new FileOutputStream(history)) { out.write("git status\n".getBytes(StandardCharsets.UTF_8)); }
        SessionShell.prepare(context.getFilesDir(), session);
        assertEquals("git status\n", new String(java.nio.file.Files.readAllBytes(history.toPath()), StandardCharsets.UTF_8));
    }
    @Test public void shellPathsWithQuotesCannotInjectCommands() {
        assertEquals("'a'\\''b'", SessionShell.quote("a'b"));
        String script = SessionShell.buildRc("/tmp/home'; false; #", "/tmp/history file", "echo hello");
        assertTrue(script.contains("HISTFILE='/tmp/history file'"));
        assertTrue(script.endsWith("echo hello\n"));
    }
    @Test public void explicitNextChatBindingGuardSurvivesStoreReloadWithoutChangingCopiedRun() throws Exception {
        WorkspaceStore store=new WorkspaceStore(context,errors::add);
        WorkspaceStore.Session p=new WorkspaceStore.Session();p.environment="codex";p.codexBindingBlockedRun=p.codexSocketId;store.sessions.add(p);
        org.json.JSONObject data=store.encode();store.decode(data.toString());
        WorkspaceStore.Session loaded=store.sessions.get(0);
        assertEquals(p.codexSocketId,loaded.codexBindingBlockedRun);assertEquals("",loaded.copy().codexBindingBlockedRun);
        data.getJSONArray("sessions").getJSONObject(0).remove("codexBindingBlockedRun");store.decode(data.toString());assertEquals("",store.sessions.get(0).codexBindingBlockedRun);store.close();
    }

}
