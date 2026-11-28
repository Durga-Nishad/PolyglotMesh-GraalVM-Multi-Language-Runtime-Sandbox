package com.polyglotmesh.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Collects what a script prints, but stops storing after {@code limitBytes}.
 * Extra output is silently dropped (and {@link #isTruncated()} becomes true), so a script that prints in a
 * loop cannot fill the server's memory.
 */
public final class LimitedOutputStream extends OutputStream {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final int limitBytes;
    private boolean truncated;

    public LimitedOutputStream(int limitBytes) {
        this.limitBytes = Math.max(1, limitBytes);
    }

    @Override
    public synchronized void write(int b) {
        if (buffer.size() < limitBytes) {
            buffer.write(b);
        } else {
            truncated = true;
        }
    }

    @Override
    public synchronized void write(byte[] bytes, int offset, int length) {
        if (bytes == null || length <= 0) {
            return;
        }
        int room = limitBytes - buffer.size();
        if (room <= 0) {
            truncated = true;
            return;
        }
        int toWrite = Math.min(room, length);
        buffer.write(bytes, offset, toWrite);
        if (toWrite < length) {
            truncated = true;
        }
    }

    public synchronized String content() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public synchronized boolean isTruncated() {
        return truncated;
    }
}
