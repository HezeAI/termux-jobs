package com.termux.app.workspace;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One directly owned child at a time. Does not signal process groups or terminal sessions. */
final class ProjectFileTransport implements AutoCloseable {
    interface Starter { Process start() throws Exception; }
    private static final ScheduledThreadPoolExecutor CLOCK = new ScheduledThreadPoolExecutor(1, task -> {
        Thread thread = new Thread(task, "file-transport-timer"); thread.setDaemon(true); return thread;
    });
    private static final ExecutorService STOP = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "file-transport-stop"); thread.setDaemon(true); return thread;
    });
    static { CLOCK.setRemoveOnCancelPolicy(true); }
    private Operation active;
    private boolean closed;

    Operation open(Starter starter, byte[] input, long max, long timeoutMillis) throws Exception {
        if (max < 0 || timeoutMillis <= 0) throw new IllegalArgumentException("INVALID_FILE_LIMIT");
        Operation operation = new Operation(max);
        synchronized (this) {
            if (closed) throw new IOException("CANCELLED");
            if (active != null) throw new IOException("FILE_OPERATION_BUSY");
            active = operation;
        }
        try {
            Process child;
            try { child = starter.start(); operation.attach(child, timeoutMillis); }
            finally { operation.launched.countDown(); }
            operation.check();
            try (OutputStream stdin = child.getOutputStream()) { if (input != null) stdin.write(input); }
            operation.check();
            return operation;
        } catch (Exception | Error failure) {
            operation.finish();
            synchronized (operation) {
                if (failure instanceof Exception && operation.cancellation != null)
                    throw new IOException(operation.cancellation, failure);
            }
            throw failure;
        }
    }

    @Override public void close() {
        Operation operation;
        synchronized (this) { closed = true; operation = active; }
        if (operation != null) operation.cancel("CANCELLED");
    }

    final class Operation extends InputStream {
        private final long max;
        private long total;
        private Process child;
        private InputStream input;
        private String cancellation;
        private boolean eof, streamClosed;
        private ScheduledFuture<?> timeout, force;
        private final AtomicBoolean finishing = new AtomicBoolean();
        private final CountDownLatch launched = new CountDownLatch(1), finished = new CountDownLatch(1);
        Operation(long max) { this.max = max; }

        private synchronized void attach(Process value, long timeoutMillis) {
            child = value; input = value.getInputStream();
            timeout = CLOCK.schedule(() -> cancel("FILE_OPERATION_TIMEOUT"), timeoutMillis, TimeUnit.MILLISECONDS);
            if (cancellation != null) stop();
        }
        private synchronized void check() throws IOException {
            if (cancellation != null) throw new IOException(cancellation);
            if (Thread.currentThread().isInterrupted()) throw new IOException("CANCELLED");
            if (streamClosed) throw new IOException("STREAM_CLOSED");
        }
        private synchronized void cancel(String reason) {
            if (finished.getCount() == 0 || cancellation != null) return;
            cancellation = reason;
            if (child != null) stop();
            if (!finishing.get()) STOP.execute(this::finish);
        }
        private void stop() {
            // Use only the Process object. Never retain or signal a numeric/reaped PID.
            if (force != null) return;
            Process owned = child;
            force = CLOCK.schedule(owned::destroyForcibly, 1, TimeUnit.SECONDS);
            STOP.execute(owned::destroy);
        }
        @Override public int read() throws IOException {
            byte[] one = new byte[1]; int count = read(one, 0, 1);
            return count < 0 ? -1 : one[0] & 255;
        }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            if (bytes == null) throw new NullPointerException();
            if (offset < 0 || length < 0 || length > bytes.length - offset) throw new IndexOutOfBoundsException();
            try {
                check();
                if (length == 0) return 0;
                if (eof) return -1;
                // Probe one extra byte when at the bound; never deliver an excess chunk.
                long remaining = max - total;
                int requested = remaining >= length ? length : (int) remaining + 1;
                int count = input.read(bytes, offset, requested);
                check();
                if (count < 0) {
                    int status;
                    try { status = child.waitFor(); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IOException("CANCELLED", error); }
                    check();
                    if (status != 0) throw new IOException("SOURCE_UNAVAILABLE");
                    eof = true; finish(); return -1;
                }
                if (count > remaining) throw new IOException("FILE_LIMIT");
                total += count; return count;
            } catch (IOException | RuntimeException failure) {
                finish();
                synchronized (this) { if (cancellation != null) throw new IOException(cancellation, failure); }
                throw failure;
            }
        }
        @Override public long skip(long count) throws IOException {
            long skipped = 0; byte[] buffer = new byte[(int) Math.min(32768, Math.max(0, count))];
            while (skipped < count) { int n = read(buffer, 0, (int) Math.min(buffer.length, count - skipped)); if (n < 0) break; skipped += n; }
            return skipped;
        }
        @Override public boolean markSupported() { return false; }
        @Override public void reset() throws IOException { throw new IOException("MARK_NOT_SUPPORTED"); }
        @Override public void close() {
            synchronized (this) { streamClosed = true; }
            finish();
        }
        private void finish() {
            boolean interrupted = false;
            if (finishing.compareAndSet(false, true)) {
                try {
                    while (true) try { launched.await(); break; }
                    catch (InterruptedException error) { interrupted = true; }
                    synchronized (this) { if (!eof && child != null) stop(); }
                    if (child != null) {
                        while (true) try { child.waitFor(); break; }
                        catch (InterruptedException error) { interrupted = true; }
                        try { input.close(); } catch (IOException ignored) { }
                        try { child.getOutputStream().close(); } catch (IOException ignored) { }
                        try { child.getErrorStream().close(); } catch (IOException ignored) { }
                    }
                } finally {
                    synchronized (this) { if (timeout != null) timeout.cancel(false); if (force != null) force.cancel(false); }
                    synchronized (ProjectFileTransport.this) { if (active == this) active = null; }
                    finished.countDown();
                }
            } else {
                while (true) try { finished.await(); break; }
                catch (InterruptedException error) { interrupted = true; }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
