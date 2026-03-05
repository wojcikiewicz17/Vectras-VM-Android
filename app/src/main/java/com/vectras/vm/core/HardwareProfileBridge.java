package com.vectras.vm.core;

import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.vectras.qemu.MainSettingsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HardwareProfileBridge {
    private static final int SIMD_NEON = 1;
    private static final int SIMD_SSE2 = 1 << 1;
    private static final int SIMD_SSE42 = 1 << 2;
    private static final int SIMD_AVX = 1 << 3;
    private static final int SIMD_RVV = 1 << 4;

    private static final boolean LOADED;
    private static final String LOAD_ERROR;

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

    public static final class Snapshot {
        public final int schema;
        public final long elapsedRealtimeMs;
        public final String effectiveAbi;
        public final int arch;
        public final int archHex;
        public final int pointerBits;
        public final int littleEndian;
        public final int hasCycleCounter;
        public final int hasAsmProbe;
        public final int featureBits0;
        public final int featureBits1;
        public final int simdMask;

        Snapshot(int schema, long elapsedRealtimeMs, String effectiveAbi, int arch, int archHex,
                 int pointerBits, int littleEndian, int hasCycleCounter, int hasAsmProbe,
                 int featureBits0, int featureBits1, int simdMask) {
            this.schema = schema;
            this.elapsedRealtimeMs = elapsedRealtimeMs;
            this.effectiveAbi = safeAbi(effectiveAbi);
            this.arch = arch;
            this.archHex = archHex;
            this.pointerBits = pointerBits;
            this.littleEndian = littleEndian;
            this.hasCycleCounter = hasCycleCounter;
            this.hasAsmProbe = hasAsmProbe;
            this.featureBits0 = featureBits0;
            this.featureBits1 = featureBits1;
            this.simdMask = simdMask;
        }

        public boolean hasAnySimd() {
            return simdMask != 0;
        }

        public String debuggerSummary() {
            return "HW abi=" + effectiveAbi
                    + " arch=" + arch
                    + " archHex=0x" + Integer.toHexString(archHex)
                    + " ptr=" + pointerBits
                    + " cycle=" + hasCycleCounter
                    + " asm=" + hasAsmProbe
                    + " simd=" + simdTags();
        }

        public String wizardSummary() {
            return "Arquitetura " + effectiveAbi + " • SIMD " + (hasAnySimd() ? "ativo" : "básico");
        }

        public String simdTags() {
            List<String> tags = new ArrayList<>();
            if ((simdMask & SIMD_NEON) != 0) tags.add("NEON");
            if ((simdMask & SIMD_SSE2) != 0) tags.add("SSE2");
            if ((simdMask & SIMD_SSE42) != 0) tags.add("SSE4.2");
            if ((simdMask & SIMD_AVX) != 0) tags.add("AVX");
            if ((simdMask & SIMD_RVV) != 0) tags.add("RVV");
            return tags.isEmpty() ? "none" : join(tags, ",");
        }
    }

    private HardwareProfileBridge() {
    }

    public static boolean isNativeAvailable() {
        return LOADED;
    }

    public static String getLoadError() {
        return LOAD_ERROR;
    }

    public static Snapshot captureCurrentSnapshot() {
        int[] raw = LOADED ? nativeCollectSnapshot() : null;
        String abi = LOADED ? nativeEffectiveAbi() : "";
        if (raw == null || raw.length < 9) {
            return new Snapshot(1, SystemClock.elapsedRealtime(), abi, 0, 0, 0, 1, 0, 0, 0, 0, 0);
        }
        return new Snapshot(
                1,
                SystemClock.elapsedRealtime(),
                abi,
                raw[0], raw[1], raw[2], raw[3], raw[4], raw[5], raw[6], raw[7], raw[8]
        );
    }

    public static Snapshot captureAndPersist(Context context, boolean forceRefresh) {
        if (!forceRefresh) {
            Snapshot persisted = getPersistedSnapshot(context);
            if (persisted != null) {
                return persisted;
            }
        }
        Snapshot snapshot = captureCurrentSnapshot();
        MainSettingsManager.setHardwareProfileSnapshot(context, serialize(snapshot));
        return snapshot;
    }

    @Nullable
    public static Snapshot getPersistedSnapshot(Context context) {
        String encoded = MainSettingsManager.getHardwareProfileSnapshot(context);
        if (encoded == null || encoded.trim().isEmpty()) {
            return null;
        }
        return deserialize(encoded);
    }

    public static String getEffectiveAbiHint(Context context) {
        Snapshot snapshot = getPersistedSnapshot(context);
        if (snapshot == null) {
            snapshot = captureAndPersist(context, false);
        }
        return snapshot.effectiveAbi;
    }

    public static boolean isAdvancedFeaturesEnabled(Context context) {
        Snapshot snapshot = getPersistedSnapshot(context);
        if (snapshot == null) {
            snapshot = captureAndPersist(context, false);
        }
        return snapshot.hasAnySimd() && snapshot.pointerBits >= 64;
    }

    public static int benchmarkStripeScale(Context context) {
        Snapshot snapshot = getPersistedSnapshot(context);
        if (snapshot == null) {
            snapshot = captureAndPersist(context, false);
        }
        if ((snapshot.simdMask & SIMD_NEON) != 0 || (snapshot.simdMask & SIMD_SSE42) != 0) {
            return 2;
        }
        return snapshot.pointerBits >= 64 ? 1 : 0;
    }

    public static int benchmarkWarmupMs(Context context) {
        Snapshot snapshot = getPersistedSnapshot(context);
        if (snapshot == null) {
            snapshot = captureAndPersist(context, false);
        }
        return snapshot.hasAnySimd() ? 25 : 70;
    }

    private static String serialize(Snapshot snapshot) {
        JSONObject object = new JSONObject();
        try {
            object.put("schema", snapshot.schema);
            object.put("elapsed", snapshot.elapsedRealtimeMs);
            object.put("abi", snapshot.effectiveAbi);
            object.put("arch", snapshot.arch);
            object.put("archHex", snapshot.archHex);
            object.put("ptr", snapshot.pointerBits);
            object.put("little", snapshot.littleEndian);
            object.put("cycle", snapshot.hasCycleCounter);
            object.put("asm", snapshot.hasAsmProbe);
            object.put("f0", snapshot.featureBits0);
            object.put("f1", snapshot.featureBits1);
            object.put("simd", snapshot.simdMask);
        } catch (JSONException ignored) {
            return "";
        }
        return object.toString();
    }

    @Nullable
    private static Snapshot deserialize(String encoded) {
        try {
            JSONObject object = new JSONObject(encoded);
            return new Snapshot(
                    object.optInt("schema", 1),
                    object.optLong("elapsed", 0L),
                    object.optString("abi", ""),
                    object.optInt("arch", 0),
                    object.optInt("archHex", 0),
                    object.optInt("ptr", 0),
                    object.optInt("little", 1),
                    object.optInt("cycle", 0),
                    object.optInt("asm", 0),
                    object.optInt("f0", 0),
                    object.optInt("f1", 0),
                    object.optInt("simd", 0)
            );
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static String safeAbi(String abi) {
        if (abi == null) {
            return "unknown";
        }
        String value = abi.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? "unknown" : value;
    }

    private static String join(List<String> tags, String sep) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) out.append(sep);
            out.append(tags.get(i));
        }
        return out.toString();
    }

    private static native int[] nativeCollectSnapshot();

    private static native String nativeEffectiveAbi();
}
