package com.termux.app.workspace;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class RetiredModelDataTest {
    @Test public void removesOnlyRetiredDataAndNeverFollowsExternalLinks() throws Exception {
        Path app = Files.createTempDirectory("retired-data-test");
        Path outside = Files.createTempDirectory("retained-project-test");
        try {
            Path original = Files.write(outside.resolve("project.txt"), new byte[]{1,2,3});
            Path shellHistory = Files.write(app.resolve("shell-history"), new byte[]{4});
            Path library = Files.write(app.resolve("workspace-v1.json"), new byte[]{5});
            Path local = Files.createDirectories(app.resolve("local-ai/chats"));
            Files.write(local.resolve("old-chat.json"), new byte[]{6});
            Files.createSymbolicLink(local.resolve("outside-project"), outside);
            RetiredModelData.removeOwnedDirectory(app.toFile());
            assertFalse(Files.exists(app.resolve("local-ai")));
            assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(original));
            assertTrue(Files.exists(shellHistory));assertTrue(Files.exists(library));
            RetiredModelData.removeOwnedDirectory(app.toFile()); // repeat is harmless
            Files.createSymbolicLink(app.resolve("local-ai"), outside);
            RetiredModelData.removeOwnedDirectory(app.toFile());
            assertFalse(Files.exists(app.resolve("local-ai"), java.nio.file.LinkOption.NOFOLLOW_LINKS));
            assertTrue(Files.exists(original));
        } finally {
            Files.deleteIfExists(outside.resolve("project.txt"));Files.deleteIfExists(outside);
            Files.deleteIfExists(app.resolve("shell-history"));Files.deleteIfExists(app.resolve("workspace-v1.json"));Files.deleteIfExists(app);
        }
    }
}
