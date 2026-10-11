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
    @Test public void savesAndReopensCommandsProfilesAndIndependentHistory() throws Exception {
        WorkspaceStore first = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Command command = new WorkspaceStore.Command();
        command.name = "Собрать APK"; command.command = "./gradlew assembleDebug\nprintf 'готово'";
        command.category = "Сборка"; command.directory = "~/проект с пробелами"; command.favorite = true;
        first.commands.add(command);
        WorkspaceStore.Session session = new WorkspaceStore.Session();
        session.name = "Codex"; session.directory = "/data/data/com.termux/files/home/project";
        session.startup = "codex"; session.transcript = "предыдущий вывод"; session.handle = "old-runtime-handle";
        first.sessions.add(session); first.save(); first.awaitWrites(); first.close();
        WorkspaceStore reopened = new WorkspaceStore(context, errors::add);
        assertEquals(1, reopened.commands.size()); assertEquals(command.command, reopened.commands.get(0).command);
        assertEquals(command.directory, reopened.commands.get(0).directory); assertTrue(reopened.commands.get(0).favorite);
        assertEquals(session.startup, reopened.sessions.get(0).startup);
        assertEquals(session.transcript, reopened.sessions.get(0).transcript);
        assertNotEquals(session.id, reopened.sessions.get(0).copy().id);
        assertEquals("", reopened.sessions.get(0).copy().handle); assertEquals("", reopened.sessions.get(0).copy().transcript);
        assertNotEquals(command.id, reopened.commands.get(0).copy().id);
        assertTrue(errors.isEmpty()); reopened.close();
    }
    @Test public void orderedWritesKeepNewestSnapshot() throws Exception {
        WorkspaceStore store = new WorkspaceStore(context, errors::add);
        WorkspaceStore.Command command = new WorkspaceStore.Command(); store.commands.add(command);
        command.name = "first"; store.save(); command.name = "second"; store.save();
        store.awaitWrites(); store.close();
        WorkspaceStore reopened = new WorkspaceStore(context, errors::add);
        assertEquals("second", reopened.commands.get(0).name); reopened.close();
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
        assertEquals("git status\n", SessionShell.readHistory(context.getFilesDir(), session.id));
    }
    @Test public void shellPathsWithQuotesCannotInjectCommands() {
        assertEquals("'a'\\''b'", SessionShell.quote("a'b"));
        String script = SessionShell.buildRc("/tmp/home'; false; #", "/tmp/history file", "echo hello");
        assertTrue(script.contains("HISTFILE='/tmp/history file'"));
        assertTrue(script.endsWith("echo hello\n"));
    }
}
