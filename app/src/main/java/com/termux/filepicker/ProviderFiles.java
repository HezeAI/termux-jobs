package com.termux.filepicker;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

/** Filesystem guards shared by the two externally reachable legacy providers. */
public final class ProviderFiles {
    private ProviderFiles() {}

    // Linux O_CLOEXEC exists on every supported Android kernel, but the Java
    // OsConstants field is public only from API27. Keep the API26 app baseline.
    private static final int OPEN_CLOEXEC = 0x80000;

    public static boolean contains(File root, File file) {
        return file.equals(root) || file.toPath().startsWith(root.toPath());
    }

    public static File resolve(String path, File... roots) throws FileNotFoundException {
        if (path == null || path.indexOf('\0') >= 0 || !new File(path).isAbsolute())
            throw new FileNotFoundException("Invalid provider path");
        try {
            File file = new File(path).getCanonicalFile();
            for (File root : roots) {
                if (contains(root.getCanonicalFile(), file)) return file;
            }
        } catch (IOException e) {
            throw failure("Cannot resolve provider path", e);
        }
        throw new FileNotFoundException("Path outside provider roots");
    }

    public static String mimeType(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return null;
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /**
     * Open without create/truncate, then validate the actual descriptor before any write.
     * NONBLOCK prevents a FIFO/device substituted after metadata checking from hanging a binder.
     * O_NOFOLLOW rejects a final symlink substituted after canonical resolution. Ancestor
     * substitution is checked through the opened descriptor's kernel path, not the original name.
     */
    public static ParcelFileDescriptor open(String path, String mode, CancellationSignal signal,
                                             File[] roots, File[] readOnly) throws FileNotFoundException {
        if (signal != null) signal.throwIfCanceled();
        File file = resolve(path, roots);
        if (!file.isFile()) throw new FileNotFoundException("Not a regular file");
        int access = ParcelFileDescriptor.parseMode(mode);
        int kind = access & ParcelFileDescriptor.MODE_READ_WRITE;
        int flags = kind == ParcelFileDescriptor.MODE_READ_ONLY ? OsConstants.O_RDONLY :
            kind == ParcelFileDescriptor.MODE_WRITE_ONLY ? OsConstants.O_WRONLY : OsConstants.O_RDWR;
        if ((access & ParcelFileDescriptor.MODE_APPEND) != 0) flags |= OsConstants.O_APPEND;
        ParcelFileDescriptor descriptor = openDescriptor(file, flags | OsConstants.O_NONBLOCK, roots);
        try {
            StructStat stat = Os.fstat(descriptor.getFileDescriptor());
            if (!OsConstants.S_ISREG(stat.st_mode)) throw new FileNotFoundException("Not a regular file");
            if (signal != null) signal.throwIfCanceled();
            if (kind != ParcelFileDescriptor.MODE_READ_ONLY && isProtected(stat, readOnly)) {
                // Preserve the legacy read-only downgrade, including hard-link aliases.
                descriptor.close();
                descriptor = null;
                return open(path, "r", signal, roots, readOnly);
            }
            if ((access & ParcelFileDescriptor.MODE_TRUNCATE) != 0)
                Os.ftruncate(descriptor.getFileDescriptor(), 0);
            ParcelFileDescriptor result = descriptor;
            descriptor = null;
            return result;
        } catch (IOException | ErrnoException e) {
            throw failure("Cannot open provider file", e);
        } finally {
            if (descriptor != null) try { descriptor.close(); } catch (IOException ignored) {}
        }
    }

    /** Pin a checked parent for create/delete; callers use /proc/self/fd/N/basename. */
    public static ParcelFileDescriptor openDirectory(File directory, File... roots) throws FileNotFoundException {
        ParcelFileDescriptor descriptor = openDescriptor(resolve(directory.getPath(), roots),
            OsConstants.O_RDONLY | OsConstants.O_NONBLOCK, roots);
        try {
            if (!OsConstants.S_ISDIR(Os.fstat(descriptor.getFileDescriptor()).st_mode))
                throw new FileNotFoundException("Not a document directory");
            ParcelFileDescriptor result = descriptor;
            descriptor = null;
            return result;
        } catch (ErrnoException e) {
            throw failure("Cannot inspect document directory", e);
        } finally {
            if (descriptor != null) try { descriptor.close(); } catch (IOException ignored) {}
        }
    }

    private static ParcelFileDescriptor openDescriptor(File file, int flags, File[] roots) throws FileNotFoundException {
        ParcelFileDescriptor descriptor = null;
        try {
            FileDescriptor raw = Os.open(file.getPath(), flags | OPEN_CLOEXEC | OsConstants.O_NOFOLLOW, 0);
            try { descriptor = ParcelFileDescriptor.dup(raw); }
            finally { Os.close(raw); }
            validateDescriptorPath(Os.readlink("/proc/self/fd/" + descriptor.getFd()), roots);
            ParcelFileDescriptor result = descriptor;
            descriptor = null;
            return result;
        } catch (IOException | ErrnoException e) {
            throw failure("Cannot open provider descriptor", e);
        } finally {
            if (descriptor != null) try { descriptor.close(); } catch (IOException ignored) {}
        }
    }

    private static void validateDescriptorPath(String openedPath, File[] roots) throws IOException, ErrnoException {
        try {
            resolve(openedPath, roots);
            return;
        } catch (FileNotFoundException outside) {
            // Android bind-mount aliases (e.g. /data/data and /data/user/0) need not
            // canonicalize to the kernel's fd path. Accept only an ancestor that is
            // the very same directory inode as an allowed root, never a string alias.
            StructStat[] rootStats = new StructStat[roots.length];
            for (int i = 0; i < roots.length; i++) {
                try {
                    StructStat stat = Os.stat(roots[i].getCanonicalPath());
                    if (OsConstants.S_ISDIR(stat.st_mode)) rootStats[i] = stat;
                } catch (ErrnoException e) {
                    // An unmounted/ungranted optional shared-storage root cannot
                    // invalidate a different, positively identified private root.
                    if (e.errno != OsConstants.ENOENT && e.errno != OsConstants.ENOTDIR && e.errno != OsConstants.EACCES) throw e;
                }
            }
            File ancestor = new File(openedPath);
            while (ancestor != null) {
                try {
                    StructStat current = Os.stat(ancestor.getPath());
                    for (StructStat root : rootStats)
                        if (root != null && OsConstants.S_ISDIR(current.st_mode) && current.st_dev == root.st_dev && current.st_ino == root.st_ino)
                            return;
                } catch (ErrnoException e) {
                    if (e.errno != OsConstants.ENOENT && e.errno != OsConstants.ENOTDIR) throw e;
                }
                ancestor = ancestor.getParentFile();
            }
            throw outside;
        }
    }

    private static boolean isProtected(StructStat opened, File[] readOnly) throws ErrnoException {
        for (File path : readOnly) {
            try {
                StructStat protectedFile = Os.stat(path.getPath());
                if (opened.st_dev == protectedFile.st_dev && opened.st_ino == protectedFile.st_ino) return true;
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.ENOENT && e.errno != OsConstants.ENOTDIR) throw e;
            }
        }
        return false;
    }

    private static FileNotFoundException failure(String message, Exception cause) {
        FileNotFoundException failure = new FileNotFoundException(message);
        failure.initCause(cause);
        return failure;
    }
}
