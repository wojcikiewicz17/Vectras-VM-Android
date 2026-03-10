package com.termux.terminal;

public class EscapeControlCharacterTest extends TerminalTestCase {

    public void testCanCancelsEscapeSequenceAndEmitsReplacementCharacter() {
        withTerminalSized(8, 2).enterString("\033[31\030A");
        assertLineStartsWith(0, TerminalEmulator.UNICODE_REPLACEMENT_CHAR, 'A');
    }

    public void testSubCancelsEscapeSequenceAndEmitsReplacementCharacter() {
        withTerminalSized(8, 2).enterString("\033[31\032B");
        assertLineStartsWith(0, TerminalEmulator.UNICODE_REPLACEMENT_CHAR, 'B');
    }
}
