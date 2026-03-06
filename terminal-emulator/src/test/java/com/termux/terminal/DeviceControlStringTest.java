package com.termux.terminal;

/**
 * "\033P" is a device control string.
 */
public class DeviceControlStringTest extends TerminalTestCase {

	private static String hexEncode(String s) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < s.length(); i++)
			result.append(String.format("%02X", (int) s.charAt(i)));
		return result.toString();
	}

	private void assertCapabilityResponse(String cap, String expectedResponse) {
		String input = "\033P+q" + hexEncode(cap) + "\033\\";
		assertEnteringStringGivesResponse(input, "\033P1+r" + hexEncode(cap) + "=" + hexEncode(expectedResponse) + "\033\\");
	}

	public void testReportColorsAndName() {
		// Request Termcap/Terminfo String. The string following the "q" is a list of names encoded in
		// hexadecimal (2 digits per character) separated by ; which correspond to termcap or terminfo key
		// names.
		// Two special features are also recognized, which are not key names: Co for termcap colors (or colors
		// for terminfo colors), and TN for termcap name (or name for terminfo name).
		// xterm responds with DCS 1 + r P t ST for valid requests, adding to P t an = , and the value of the
		// corresponding string that xterm would send, or DCS 0 + r P t ST for invalid requests. The strings are
		// encoded in hexadecimal (2 digits per character).
		withTerminalSized(3, 3).enterString("A");
		assertCapabilityResponse("Co", "256");
		assertCapabilityResponse("colors", "256");
		assertCapabilityResponse("TN", "xterm");
		assertCapabilityResponse("name", "xterm");
		enterString("B").assertLinesAre("AB ", "   ", "   ");
	}

	public void testReportKeys() {
		withTerminalSized(3, 3);
		assertCapabilityResponse("kB", "\033[Z");
	}

	public void testInvalidOrUnsupportedItemsRespondAsInvalid() {
		withTerminalSized(3, 3);
		assertEnteringStringGivesResponse("\033P+q6b7G\033\\", "");
		assertEnteringStringGivesResponse("\033P+q41\033\\", "\033P0+r41\033\\");
		assertEnteringStringGivesResponse("\033P+q2531\033\\", "\033P0+r2531\033\\");
		assertEnteringStringGivesResponse("\033P+q2638\033\\", "\033P0+r2638\033\\");
	}

	public void testMultipleCapabilityItemsIncludingEmpty() {
		withTerminalSized(3, 3);
		assertEnteringStringGivesResponse("\033P+q436f;6e616d65;;6b42\033\\",
			"\033P1+r436f=323536\033\\" +
			"\033P1+r6e616d65=787465726D\033\\" +
			"\033P0+r\033\\" +
			"\033P1+r6b42=1B5B5A\033\\");
	}

	public void testHighCardinalityCapabilityItems() {
		withTerminalSized(3, 3);
		StringBuilder request = new StringBuilder("\033P+q");
		StringBuilder expected = new StringBuilder();
		for (int i = 0; i < 128; i++) {
			if (i > 0) request.append(';');
			request.append("434f");
			expected.append("\033P1+r434f=323536\033\\");
		}
		request.append("\033\\");
		assertEnteringStringGivesResponse(request.toString(), expected.toString());
	}

	public void testReallyLongDeviceControlString() {
		withTerminalSized(3, 3).enterString("\033P");
		for (int i = 0; i < 10000; i++) {
			enterString("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		}
		// The terminal should ignore the overlong DCS sequence and continue printing "aaa." and fill at least the first two lines with
		// them:
		assertLineIs(0, "aaa");
		assertLineIs(1, "aaa");
	}

}
