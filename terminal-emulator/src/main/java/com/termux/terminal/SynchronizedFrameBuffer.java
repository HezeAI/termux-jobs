package com.termux.terminal;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

/** Opt-in DEC2026 framing before emulator mutation. Call only on the session's main thread. */
public final class SynchronizedFrameBuffer {
    public static final int MAX_BYTES = 256 * 1024;
    public static final long TIMEOUT_MS = 200;
    private static final byte[] PREFIX = {27, '[', '?', '2', '0', '2', '6'};
    private final ByteArrayOutputStream held = new ByteArrayOutputStream();
    private int prefixLength;
    private boolean frame;
    private long since = -1;

    public long deadline() { return since < 0 ? -1 : since + TIMEOUT_MS; }

    public void append(byte[] bytes, int length, long now, Consumer<byte[]> sink) {
        ByteArrayOutputStream visible = new ByteArrayOutputStream(length);
        for (int i = 0; i < length; i++) {
            byte value = bytes[i];
            if (prefixLength == PREFIX.length && (value == 'h' || value == 'l')) {
                prefixLength = 0;
                if (value == 'h') frame = true;
                else { release(visible); }
                continue;
            }
            if (prefixLength < PREFIX.length && value == PREFIX[prefixLength]) {
                if (since < 0) since = now;
                prefixLength++;
                continue;
            }
            if (prefixLength > 0) {
                int count = prefixLength;
                prefixLength = 0;
                payload(PREFIX, count, visible);
            }
            if (value == PREFIX[0]) {
                if (since < 0) since = now;
                prefixLength = 1;
            } else {
                put(value, visible);
            }
            if (!frame && prefixLength == 0) since = -1;
        }
        if (visible.size() > 0) sink.accept(visible.toByteArray());
    }

    private void payload(byte[] data, int count, ByteArrayOutputStream visible) {
        for (int i = 0; i < count; i++) put(data[i], visible);
    }
    private void put(byte value, ByteArrayOutputStream visible) {
        if (frame) {
            held.write(value);
            if (held.size() >= MAX_BYTES) release(visible);
        } else visible.write(value);
    }
    private void release(ByteArrayOutputStream visible) {
        byte[] data = held.toByteArray(); visible.write(data, 0, data.length); held.reset();
        visible.write(PREFIX, 0, prefixLength); prefixLength = 0; frame = false; since = -1;
    }
    public void flush(Consumer<byte[]> sink) {
        ByteArrayOutputStream visible = new ByteArrayOutputStream(); release(visible);
        if (visible.size() > 0) sink.accept(visible.toByteArray());
    }
    public void expire(long now, Consumer<byte[]> sink) {
        if (deadline() >= 0 && now >= deadline()) flush(sink);
    }
}
