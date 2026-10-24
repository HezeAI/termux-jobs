package com.termux.filepicker;

import android.os.ParcelFileDescriptor;
import android.system.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import static org.junit.Assert.*;

/** Explicit syscall/descriptor model: real private files; not an Android kernel test. */
@Implements(Os.class)
public class ProviderTestOs {
    static class Entry {
        FileChannel channel; int references = 1;
        Entry(FileChannel channel) { this.channel = channel; }
    }
    static final Map<FileDescriptor, Entry> opened = new IdentityHashMap<>();
    public static Runnable beforeOpen, afterOpen;
    public static int truncations;
    public static Path kernelAlias, aliasTarget;
    public static void reset() throws IOException {
        for (Entry entry : opened.values()) entry.channel.close();
        opened.clear(); beforeOpen = afterOpen = null; truncations = 0; kernelAlias = aliasTarget = null;
        // Robolectric 4.4 leaves these flags at zero. Give the explicit syscall
        // model distinct nonzero bits so assertions cannot silently test 0 == 0.
        org.robolectric.util.ReflectionHelpers.setStaticField(OsConstants.class, "O_NOFOLLOW", 0x20000);
        org.robolectric.util.ReflectionHelpers.setStaticField(OsConstants.class, "O_CLOEXEC", 0x80000);
        org.robolectric.util.ReflectionHelpers.setStaticField(OsConstants.class, "O_NONBLOCK", 0x800);
    }
    public static void assertClosed() { assertTrue("descriptor leak: " + opened.size(), opened.isEmpty()); }
    static int number(FileDescriptor fd) {
        try { java.lang.reflect.Field field = FileDescriptor.class.getDeclaredField("fd"); field.setAccessible(true); return field.getInt(fd); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    @Implementation protected static FileDescriptor open(String path, int flags, int mode) throws ErrnoException {
        assertNotEquals(0, OsConstants.O_NOFOLLOW); assertNotEquals(0, OsConstants.O_CLOEXEC);
        assertNotEquals(0, flags & OsConstants.O_NONBLOCK);
        assertEquals(OsConstants.O_NOFOLLOW | OsConstants.O_CLOEXEC, flags & (OsConstants.O_NOFOLLOW | OsConstants.O_CLOEXEC));
        assertEquals("no destructive flags before descriptor validation", 0, flags & (OsConstants.O_CREAT | OsConstants.O_TRUNC));
        if (beforeOpen != null) { Runnable hook = beforeOpen; beforeOpen = null; hook.run(); }
        Path file = Paths.get(path);
        try {
            if (Files.isSymbolicLink(file)) throw new IOException("NOFOLLOW");
            Set<OpenOption> options = new HashSet<>();
            int access = flags & (OsConstants.O_WRONLY | OsConstants.O_RDWR);
            if (access != OsConstants.O_WRONLY) options.add(StandardOpenOption.READ);
            if (access != OsConstants.O_RDONLY) options.add(StandardOpenOption.WRITE);
            FileChannel channel = FileChannel.open(file, options);
            java.lang.reflect.Field f = channel.getClass().getDeclaredField("fd"); f.setAccessible(true);
            FileDescriptor fd = (FileDescriptor) f.get(channel);
            opened.put(fd, new Entry(channel));
            if (afterOpen != null) { Runnable hook = afterOpen; afterOpen = null; hook.run(); }
            return fd;
        } catch (Exception e) { throw new ErrnoException("open", OsConstants.EIO, e); }
    }
    @Implementation protected static void close(FileDescriptor fd) throws ErrnoException {
        try { Entry entry = opened.get(fd); if (--entry.references == 0) { opened.remove(fd); entry.channel.close(); } }
        catch (IOException e) { throw new ErrnoException("close", OsConstants.EIO, e); }
    }
    @Implementation protected static String readlink(String path) throws ErrnoException {
        try {
            Path target = Files.readSymbolicLink(Paths.get(path));
            if (kernelAlias != null && target.startsWith(aliasTarget)) target = kernelAlias.resolve(aliasTarget.relativize(target));
            return target.toString();
        }
        catch (IOException e) { throw new ErrnoException("readlink", OsConstants.EIO, e); }
    }
    @Implementation protected static StructStat stat(String path) throws ErrnoException {
        try {
            Path target = Paths.get(path);
            if (kernelAlias != null && target.startsWith(kernelAlias)) target = aliasTarget.resolve(kernelAlias.relativize(target));
            Map<String,Object> values = Files.readAttributes(target, "unix:dev,ino,mode,size");
            return new StructStat(((Number)values.get("dev")).longValue(), ((Number)values.get("ino")).longValue(),
                (((Integer)values.get("mode") & 0170000) == 0100000 ? OsConstants.S_IFREG : OsConstants.S_IFDIR) |
                    ((Integer)values.get("mode") & 07777), 1, 0, 0, 0, ((Number)values.get("size")).longValue(), 0, 0, 0, 4096, 0);
        } catch (NoSuchFileException e) { throw new ErrnoException("stat", OsConstants.ENOENT, e); }
        catch (IOException e) { throw new ErrnoException("stat", OsConstants.EIO, e); }
    }
    @Implementation protected static StructStat fstat(FileDescriptor fd) throws ErrnoException { return stat(readlink("/proc/self/fd/" + number(fd))); }
    @Implementation protected static void ftruncate(FileDescriptor fd, long size) throws ErrnoException {
        truncations++;
        try { opened.get(fd).channel.truncate(size); }
        catch (IOException e) { throw new ErrnoException("truncate", OsConstants.EIO, e); }
    }
    @Implements(ParcelFileDescriptor.class)
    public static class Descriptor {
        FileDescriptor fd; boolean closed;
        @Implementation protected static ParcelFileDescriptor dup(FileDescriptor fd) {
            ParcelFileDescriptor parcel = Shadow.newInstanceOf(ParcelFileDescriptor.class);
            Descriptor shadow = Shadow.extract(parcel); shadow.fd = fd;
            opened.get(fd).references++; return parcel;
        }
        @Implementation protected FileDescriptor getFileDescriptor() { return fd; }
        @Implementation protected int getFd() { return number(fd); }
        @Implementation protected long getStatSize() { try { return opened.get(fd).channel.size(); } catch (IOException e) { return -1; } }
        @Implementation protected void close() throws IOException {
            if (!closed) { closed = true; try { ProviderTestOs.close(fd); } catch (ErrnoException e) { throw new IOException(e); } }
        }
    }
}
