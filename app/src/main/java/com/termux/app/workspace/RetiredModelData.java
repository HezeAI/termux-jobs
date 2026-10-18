package com.termux.app.workspace;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Removal migration for the retired on-device model feature, not a workspace reset. */
final class RetiredModelData {
    private RetiredModelData() {}

    static void clean(Context context) throws IOException {
        removeOwnedDirectory(context.getFilesDir());
        removeOwnedDirectory(context.getCacheDir());
        context.deleteSharedPreferences("local-ai-diagnostics");
    }

    static void removeOwnedDirectory(File appDirectory) throws IOException {
        // Fixed child only: no profile-supplied paths, HOME, PREFIX or proot roots.
        Path child = appDirectory.getCanonicalFile().toPath().resolve("local-ai");
        if (!Files.exists(child, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
        // Default walk never follows symbolic links, including the top-level path.
        Files.walkFileTree(child, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                if (failure != null) throw failure;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
