package com.vectras.vm.core;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-safe ring buffer with line and byte quotas.
 */
public class BoundedStringRingBuffer {
    private static final class LineEntry {
        private final String line;
        private final int utf8Bytes;

        private LineEntry(String line, int utf8Bytes) {
            this.line = line;
            this.utf8Bytes = utf8Bytes;
        }
    }

    private final int maxLines;
    private final int maxBytes;
    private final Deque<LineEntry> lines = new ArrayDeque<>();
    private int totalBytes;
    private int totalChars;

    public BoundedStringRingBuffer(int maxLines, int maxBytes) {
        this.maxLines = Math.max(1, maxLines);
        this.maxBytes = Math.max(64, maxBytes);
    }

    public synchronized void addLine(String line) {
        String safe = line == null ? "" : line;
        int utf8Bytes = byteCount(safe);
        int size = utf8Bytes + 1;
        if (size > maxBytes) {
            safe = trimToBytes(safe, maxBytes - 1);
            utf8Bytes = byteCount(safe);
            size = utf8Bytes + 1;
        }
        lines.addLast(new LineEntry(safe, utf8Bytes));
        totalBytes += size;
        totalChars += safe.length();
        trim();
    }

    public synchronized String snapshot() {
        StringBuilder sb = new StringBuilder(totalChars + lines.size());
        for (LineEntry entry : lines) {
            sb.append(entry.line).append('\n');
        }
        return sb.toString();
    }

    private void trim() {
        while (lines.size() > maxLines || totalBytes > maxBytes) {
            LineEntry removed = lines.pollFirst();
            if (removed == null) return;
            totalBytes -= removed.utf8Bytes + 1;
            totalChars -= removed.line.length();
        }
    }

    private static int byteCount(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static String trimToBytes(String value, int maxBytes) {
        if (maxBytes <= 0) return "";
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharBuffer in = CharBuffer.wrap(value);
        ByteBuffer out = ByteBuffer.allocate(maxBytes);
        CoderResult result = encoder.encode(in, out, true);
        if (result.isError()) {
            return "";
        }
        return value.substring(0, in.position());
    }
}
