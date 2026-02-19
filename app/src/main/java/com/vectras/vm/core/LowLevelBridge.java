package com.vectras.vm.core;

public final class LowLevelBridge {
    private static final boolean LOADED;

    static {
        boolean ok;
        try {
            System.loadLibrary("vectra_core_accel");
            ok = true;
        } catch (Throwable t) {
            ok = false;
        }
        LOADED = ok;
    }

    private LowLevelBridge() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static int fold32(int a, int b, int c, int d) {
        if (LOADED) {
            try {
                return nativeFold32(a, b, c, d);
            } catch (Throwable ignored) {
                // immediate fallback to deterministic Java path
            }
        }
        return LowLevelDeterminism.fold32Fallback(a, b, c, d);
    }

    public static int reduceXor(byte[] data, int offset, int length) {
        if (LOADED) {
            try {
                return nativeReduceXor(data, offset, length);
            } catch (Throwable ignored) {
                // immediate fallback to deterministic Java path
            }
        }
        return LowLevelDeterminism.reduceXorFallback(data, offset, length);
    }

    public static int checksum32(byte[] data, int offset, int length, int seed) {
        if (LOADED) {
            try {
                return nativeChecksum32(data, offset, length, seed);
            } catch (Throwable ignored) {
                // immediate fallback to deterministic Java path
            }
        }
        return LowLevelDeterminism.checksum32Fallback(data, offset, length, seed);
    }

    public static int xorChecksumCompat(byte[] data, int offset, int length) {
        if (LOADED) {
            try {
                return nativeXorChecksumCompat(data, offset, length);
            } catch (Throwable ignored) {
                // immediate fallback to deterministic Java path
            }
        }
        return LowLevelDeterminism.xorChecksumCompatFallback(data, offset, length);
    }

    public static int crc32cCompat(int initial, byte[] data, int offset, int length) {
        if (LOADED) {
            try {
                return nativeCrc32cCompat(initial, data, offset, length);
            } catch (Throwable ignored) {
                // immediate fallback to deterministic Java path
            }
        }
        return LowLevelDeterminism.crc32cCompatFallback(initial, data, offset, length);
    }

    private static native int nativeFold32(int a, int b, int c, int d);

    private static native int nativeReduceXor(byte[] data, int offset, int length);

    private static native int nativeChecksum32(byte[] data, int offset, int length, int seed);

    private static native int nativeXorChecksumCompat(byte[] data, int offset, int length);

    private static native int nativeCrc32cCompat(int initial, byte[] data, int offset, int length);
}
