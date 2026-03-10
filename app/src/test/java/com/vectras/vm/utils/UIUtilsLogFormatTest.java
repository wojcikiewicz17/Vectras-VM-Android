package com.vectras.vm.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UIUtilsLogFormatTest {

    @Test
    public void parseAndroidLogLevelSupportsSlashPrefix() {
        assertEquals('E', UIUtils.parseAndroidLogLevel("E/Tag(123): boom"));
        assertEquals('W', UIUtils.parseAndroidLogLevel("W/Tag(123): warn"));
    }

    @Test
    public void parseAndroidLogLevelSupportsTimestampedFormat() {
        assertEquals('E', UIUtils.parseAndroidLogLevel("01-01 00:00:00.000 100 200 E Tag: boom"));
        assertEquals('W', UIUtils.parseAndroidLogLevel("01-01 00:00:00.000 100 200 W Tag: warn"));
    }

    @Test
    public void parseAndroidLogLevelReturnsZeroForUnknownLines() {
        assertEquals(0, UIUtils.parseAndroidLogLevel("plain line"));
    }
}
