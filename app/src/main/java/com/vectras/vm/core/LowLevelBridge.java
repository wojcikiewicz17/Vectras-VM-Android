package com.vectras.vm.core;

import android.util.Log;

import java.lang.ThreadLocal;

public final class LowLevelBridge {
    private static final boolean LOADED;
    private static final String LOAD_ERROR;
    private static final ThreadLocal<Boolean> LAST_NATIVE_PATH = new ThreadLocal<Boolean>();
    private static final String TAG = "LowLevelBridge";

    static {
        boolean ok;
        String error = "";
        try {
            System.loadLibrary("vectra_core_accel");
            ok = true;
        } catch (Throwable t) {
            ok = false;
            error = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        }
        LOADED = ok;
        LOAD_ERROR = error;
    }

    private LowLevelBridge() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static String getLoadError() {
        return LOAD_ERROR;
    }

    public static boolean wasLastCallNative() {
        Boolean value = LAST_NATIVE_PATH.get();
        return value != null && value.booleanValue();
    }

    private static void markLastCallNative(boolean nativePath) {
        LAST_NATIVE_PATH.set(Boolean.valueOf(nativePath));
    }

    public static int fold32(int a, int b, int c, int d) {
        if (LOADED) {
            try {
                int out = nativeFold32(a, b, c, d);
                markLastCallNative(true);
                return out;
            } catch (Throwable t) {
                RuntimeErrorReporter.warn("VRT-LLB-0001", "native_fold32_fallback", "fold32", t);
                Log.d(TAG, "fold32 fallback to Java path");
            }
        }
        markLastCallNative(false);
        return LowLevelDeterminism.fold32Fallback(a, b, c, d);
    }

    public static int reduceXor(byte[] data, int offset, int length) {
        if (LOADED) {
            try {
                int out = nativeReduceXor(data, offset, length);
                markLastCallNative(true);
                return out;
            } catch (Throwable t) {
                RuntimeErrorReporter.warn("VRT-LLB-0002", "native_reduce_xor_fallback", "reduceXor", t);
            }
        }
        markLastCallNative(false);
        return LowLevelDeterminism.reduceXorFallback(data, offset, length);
    }

    public static int checksum32(byte[] data, int offset, int length, int seed) {
        if (LOADED) {
            try {
                int out = nativeChecksum32(data, offset, length, seed);
                markLastCallNative(true);
                return out;
            } catch (Throwable t) {
                RuntimeErrorReporter.warn("VRT-LLB-0003", "native_checksum32_fallback", "checksum32", t);
            }
        }
        markLastCallNative(false);
        return LowLevelDeterminism.checksum32Fallback(data, offset, length, seed);
    }

    public static int xorChecksumCompat(byte[] data, int offset, int length) {
        if (data == null || length <= 0) {
            markLastCallNative(false);
            return 0;
        }
        if (offset < 0 || offset + length > data.length) {
            throw new IllegalArgumentException("Invalid checksum range");
        }
        if (LOADED) {
            try {
                int out = nativeXorChecksumCompat(data, offset, length);
                markLastCallNative(true);
                return out;
            } catch (Throwable t) {
                RuntimeErrorReporter.warn("VRT-LLB-0004", "native_xor_checksum_fallback", "xorChecksumCompat", t);
            }
        }
        markLastCallNative(false);
        return LowLevelDeterminism.xorChecksumCompatFallback(data, offset, length);
    }

    public static int crc32cCompat(int initial, byte[] data, int offset, int length) {
        if (data == null || length <= 0) {
            markLastCallNative(false);
            return initial;
        }
        if (offset < 0 || offset + length > data.length) {
            throw new IllegalArgumentException("Invalid crc range");
        }
        if (LOADED) {
            try {
                int out = nativeCrc32cCompat(initial, data, offset, length);
                markLastCallNative(true);
                return out;
            } catch (Throwable t) {
                RuntimeErrorReporter.warn("VRT-LLB-0005", "native_crc32c_fallback", "crc32cCompat", t);
            }
        }
        markLastCallNative(false);
        return LowLevelDeterminism.crc32cCompatFallback(initial, data, offset, length);
    }

    /**
     * Validates reduce_xor parity across all compiled low-level backends.
     * Returns true when every backend matches the canonical low-level contract.
     */
    public static boolean validateReduceXorBackendParity(byte[] data, int offset, int length) {
        if (data == null || offset < 0 || length < 0 || offset + length > data.length) {
            return false;
        }
        if (!LOADED) {
            return true;
        }
        try {
            return nativeValidateReduceXorBackendParity(data, offset, length) == 0;
        } catch (Throwable t) {
            RuntimeErrorReporter.warn("VRT-LLB-0006", "validate_reduce_xor_backend", "nativeValidateReduceXorBackendParity", t);
            return false;
        }
    }

    private static native int nativeFold32(int a, int b, int c, int d);

    private static native int nativeReduceXor(byte[] data, int offset, int length);

    private static native int nativeChecksum32(byte[] data, int offset, int length, int seed);

    private static native int nativeXorChecksumCompat(byte[] data, int offset, int length);

    private static native int nativeCrc32cCompat(int initial, byte[] data, int offset, int length);

    private static native int nativeValidateReduceXorBackendParity(byte[] data, int offset, int length);
}
