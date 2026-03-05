package com.vectras.vm.core;

public final class NativeLogcatBridge {

    private static final boolean NATIVE_AVAILABLE;
    private static final String LOAD_ERROR;

    static {
        boolean loaded;
        String error = "";
        try {
            System.loadLibrary("vectra_core_accel");
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
            error = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        }
        NATIVE_AVAILABLE = loaded;
        LOAD_ERROR = error;
    }

    private NativeLogcatBridge() {
        throw new AssertionError("NativeLogcatBridge cannot be instantiated");
    }

    public static boolean isNativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    public static String getLoadError() {
        return LOAD_ERROR;
    }

    public static boolean init(int ringEntries, int entryBytes) {
        if (!NATIVE_AVAILABLE) return false;
        return nativeInitCapture(ringEntries, entryBytes) == 0;
    }

    public static String readBatch(int maxEvents) {
        if (!NATIVE_AVAILABLE) return "";
        String batch = nativeReadBatch(maxEvents);
        return batch == null ? "" : batch;
    }

    public static void shutdown() {
        if (!NATIVE_AVAILABLE) return;
        nativeShutdownCapture();
    }

    private static native int nativeInitCapture(int ringEntries, int entryBytes);
    private static native String nativeReadBatch(int maxEvents);
    private static native void nativeShutdownCapture();
}
