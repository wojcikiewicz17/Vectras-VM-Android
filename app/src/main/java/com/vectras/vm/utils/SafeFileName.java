package com.vectras.vm.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SafeFileName {

    private static final Set<String> RESERVED_NAMES = new HashSet<>(Arrays.asList(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    ));

    private SafeFileName() {
    }

    public static String normalizeFromDisplayName(String displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("File name is empty.");
        }

        String candidate = displayName.trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("File name is empty.");
        }

        if (".".equals(candidate) || "..".equals(candidate)) {
            throw new IllegalArgumentException("File name is ambiguous.");
        }

        if (candidate.contains("/") || candidate.contains("\\")) {
            throw new IllegalArgumentException("File name contains path separator.");
        }

        if (candidate.contains("..")) {
            throw new IllegalArgumentException("File name contains ambiguous sequence '..'.");
        }

        if (candidate.indexOf(':') >= 0 || candidate.indexOf('*') >= 0 || candidate.indexOf('?') >= 0
                || candidate.indexOf('"') >= 0 || candidate.indexOf('<') >= 0 || candidate.indexOf('>') >= 0
                || candidate.indexOf('|') >= 0) {
            throw new IllegalArgumentException("File name contains restricted characters.");
        }

        if (candidate.startsWith(".") || candidate.endsWith(".") || candidate.endsWith(" ")) {
            throw new IllegalArgumentException("File name uses unsafe trailing/leading dot or space.");
        }

        for (int i = 0; i < candidate.length(); i++) {
            if (Character.isISOControl(candidate.charAt(i))) {
                throw new IllegalArgumentException("File name contains control characters.");
            }
        }

        String baseName = candidate;
        int dotIndex = candidate.indexOf('.');
        if (dotIndex > 0) {
            baseName = candidate.substring(0, dotIndex);
        }

        if (RESERVED_NAMES.contains(baseName.toUpperCase(Locale.US))) {
            throw new IllegalArgumentException("File name is reserved by filesystem.");
        }

        return candidate;
    }
}

