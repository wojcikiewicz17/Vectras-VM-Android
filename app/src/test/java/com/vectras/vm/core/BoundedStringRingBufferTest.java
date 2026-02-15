package com.vectras.vm.core;

import org.junit.Assert;
import org.junit.Test;

public class BoundedStringRingBufferTest {
    @Test
    public void shouldKeepLatestLinesWithinLineLimit() {
        BoundedStringRingBuffer buffer = new BoundedStringRingBuffer(3, 256);
        buffer.addLine("11111");
        buffer.addLine("22222");
        buffer.addLine("33333");
        buffer.addLine("44444");

        Assert.assertEquals("22222\n33333\n44444\n", buffer.snapshot());
    }

    @Test
    public void shouldTrimByByteLimitKeepingNewestLines() {
        BoundedStringRingBuffer buffer = new BoundedStringRingBuffer(10, 12);
        buffer.addLine("abcd"); // 5 bytes including newline
        buffer.addLine("efgh"); // 10 bytes total
        buffer.addLine("ij");   // 13 bytes total -> trims "abcd"

        Assert.assertEquals("efgh\nij\n", buffer.snapshot());
    }

    @Test
    public void shouldHandleMultibyteUnicodeByteLimit() {
        BoundedStringRingBuffer buffer = new BoundedStringRingBuffer(10, 8);
        buffer.addLine("áé"); // 4 bytes + newline
        buffer.addLine("ç");  // 2 bytes + newline -> over limit, removes first line

        Assert.assertEquals("ç\n", buffer.snapshot());
    }

    @Test
    public void shouldKeepFunctionalBehaviorWhenLineExceedsByteLimit() {
        BoundedStringRingBuffer buffer = new BoundedStringRingBuffer(5, 8);
        buffer.addLine("abcdefghi"); // should be trimmed to fit maxBytes-1 UTF-8 bytes

        Assert.assertEquals("abcdefg\n", buffer.snapshot());
    }
}
