package com.vectras.vm.core;

/**
 * Bridge JNI opcional para rastreamento de estado de fluxo VM em memória nativa.
 */
public final class VmFlowNativeBridge {
    private static final boolean AVAILABLE;
    private static final String LOAD_ERROR;

    static {
        boolean loaded = false;
        String error = "";
        try {
            System.loadLibrary("vectra_core_accel");
            loaded = nativeVmFlowInit() == 1;
            if (!loaded) {
                error = "nativeVmFlowInit returned != 1";
            }
        } catch (Throwable t) {
            loaded = false;
            error = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        }
        AVAILABLE = loaded;
        LOAD_ERROR = error;
    }

    private VmFlowNativeBridge() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static String getLoadError() {
        return LOAD_ERROR;
    }

    public static void mark(int vmHash, int stateOrdinal) {
        if (!AVAILABLE) return;
        nativeVmFlowMark(vmHash, stateOrdinal);
    }

    public static int current(int vmHash) {
        if (!AVAILABLE) return -1;
        return nativeVmFlowCurrent(vmHash);
    }

    public static int[] stats() {
        if (!AVAILABLE) return null;
        return nativeVmFlowStats();
    }

    public static long vmLastMonoNanos(int vmHash) {
        if (!AVAILABLE) return 0L;
        int[] parts = nativeVmFlowLastMono(vmHash);
        if (parts == null || parts.length < 2) return 0L;
        long lo = ((long) parts[0]) & 0xFFFFFFFFL;
        long hi = ((long) parts[1]) & 0xFFFFFFFFL;
        return (hi << 32) | lo;
    }

    private static native int nativeVmFlowInit();
    private static native void nativeVmFlowMark(int vmHash, int stateOrdinal);
    private static native int nativeVmFlowCurrent(int vmHash);
    private static native int[] nativeVmFlowStats();
    private static native int[] nativeVmFlowLastMono(int vmHash);
}
