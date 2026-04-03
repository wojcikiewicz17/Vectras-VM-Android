package com.vectras.vm.core;

import android.util.Log;

import java.util.Locale;

/**
 * Padrão central de telemetria para erros runtime/setup.
 * Formato: [VRT-XXXX] action=<acao> key=<param-chave> detail=<mensagem>
 */
public final class RuntimeErrorReporter {
    public static final String TAG = "VectrasRuntime";

    private RuntimeErrorReporter() {
        throw new AssertionError("RuntimeErrorReporter is utility-only");
    }

    public static String format(String code, String action, String key, Throwable throwable) {
        String safeCode = sanitizeCode(code);
        String safeAction = safe(action);
        String safeKey = safe(key);
        String detail = throwable == null ? "n/a" : safe(throwable.getMessage());
        return "[" + safeCode + "] action=" + safeAction + " key=" + safeKey + " detail=" + detail;
    }

    public static void warn(String code, String action, String key, Throwable throwable) {
        String message = format(code, action, key, throwable);
        if (throwable != null) {
            Log.w(TAG, message, throwable);
        } else {
            Log.w(TAG, message);
        }
    }

    public static void error(String code, String action, String key, Throwable throwable) {
        String message = format(code, action, key, throwable);
        if (throwable != null) {
            Log.e(TAG, message, throwable);
        } else {
            Log.e(TAG, message);
        }
    }

    public static String technicalSummary(String code, String action, Throwable throwable) {
        return "[" + sanitizeCode(code) + "] " + safe(action) + ": "
                + (throwable == null ? "unknown" : safe(throwable.getClass().getSimpleName()));
    }

    private static String sanitizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "VRT-0000";
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "n/a";
        }
        return value.trim();
    }
}
