package com.termux.app.workspace;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Bounded, per-invocation acknowledgements; received progress is only a lower bound. */
final class ProjectFileReceipt extends OutputStream {
    static final int MAX_OUTPUT = 8192;
    private final String prefix;
    private final int expected;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    ProjectFileReceipt(int expected) { this(UUID.randomUUID().toString().replace("-", ""), expected); }
    ProjectFileReceipt(String id, int expected) {
        if (id == null || !id.matches("[a-f0-9]{32}") || expected < 1 || expected > ProjectFileTransfer.MAX_SELECTED)
            throw new IllegalArgumentException("INVALID_TRANSFER_RECEIPT");
        prefix = "JOBS_TRANSFER_V1 " + id + " "; this.expected = expected;
    }
    String progress(int ordinal) {
        if (ordinal < 1 || ordinal > expected) throw new IllegalArgumentException("INVALID_TRANSFER_ORDINAL");
        return prefix + "DONE " + ordinal;
    }
    String completion() { return prefix + "END " + expected; }
    @Override public void write(int value) throws IOException {
        if (output.size() >= MAX_OUTPUT) throw new IOException("TRANSFER_OUTPUT_LIMIT");
        output.write(value);
    }
    @Override public void write(byte[] bytes, int offset, int length) throws IOException {
        if (bytes == null) throw new NullPointerException();
        if (offset < 0 || length < 0 || length > bytes.length - offset) throw new IndexOutOfBoundsException();
        if (length > MAX_OUTPUT - output.size()) throw new IOException("TRANSFER_OUTPUT_LIMIT");
        output.write(bytes, offset, length);
    }
    private static final class State { int acknowledged; boolean ended, invalid; }
    private State state() {
        State state = new State();
        String[] lines = new String(output.toByteArray(), StandardCharsets.US_ASCII).split("\n", -1);
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i];
            if (!line.startsWith(prefix)) continue;
            if (state.invalid) continue;
            if (state.ended) state.invalid = true;
            else if (state.acknowledged < expected && line.equals(progress(state.acknowledged + 1))) state.acknowledged++;
            else if (state.acknowledged == expected && line.equals(completion())) state.ended = true;
            else state.invalid = true;
        }
        if (lines[lines.length - 1].startsWith(prefix)) state.invalid = true;
        return state;
    }
    int acknowledged() { return state().acknowledged; }
    void requireComplete() throws IOException {
        State state = state();
        if (state.invalid || !state.ended) throw new IOException("TRANSFER_NOT_CONFIRMED");
    }
}
