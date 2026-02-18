package com.vectras.vm.core;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.vectras.vm.audit.AuditEvent;
import com.vectras.vm.audit.AuditLedger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracker canônico de estados de fluxo da VM, com trilha em AuditLedger.
 */
public final class VmFlowTracker {
    private static final String TAG = "VmFlowTracker";
    private static final ConcurrentHashMap<String, VmFlowState> STATES = new ConcurrentHashMap<>();

    private VmFlowTracker() {
    }

    public static void mark(Context context,
                            String vmId,
                            VmFlowState to,
                            String causeCode,
                            String actionTaken) {
        if (to == null) {
            return;
        }

        String key = normalizeVmId(vmId);
        VmFlowState from = STATES.getOrDefault(key, VmFlowState.IDLE);
        STATES.put(key, to);
        int vmHash = stableVmHash(key);
        VmFlowNativeBridge.mark(vmHash, to.ordinal());

        AuditLedger.record(context, new AuditEvent(
                SystemClock.elapsedRealtime(),
                System.currentTimeMillis(),
                key,
                from.name(),
                to.name(),
                safe(causeCode, "flow_update"),
                0,
                0,
                0,
                safe(actionTaken, "track")
        ));

        Log.i(TAG, key + " " + from + " -> " + to + " cause=" + safe(causeCode, "flow_update"));
    }

    public static VmFlowState current(String vmId) {
        String key = normalizeVmId(vmId);
        int nativeOrdinal = VmFlowNativeBridge.current(stableVmHash(key));
        if (nativeOrdinal >= 0 && nativeOrdinal < VmFlowState.values().length) {
            VmFlowState nativeState = VmFlowState.values()[nativeOrdinal];
            STATES.put(key, nativeState);
            return nativeState;
        }
        return STATES.getOrDefault(key, VmFlowState.IDLE);
    }


    public static final class DiagnosticsSnapshot {
        public final String vmId;
        public final VmFlowState state;
        public final boolean nativeEnabled;
        public final int occupiedSlots;
        public final int capacitySlots;
        public final int hitRatePermille;
        public final long nativeLastMonoNanos;
        public final long auditLastMonoMillis;

        DiagnosticsSnapshot(String vmId,
                            VmFlowState state,
                            boolean nativeEnabled,
                            int occupiedSlots,
                            int capacitySlots,
                            int hitRatePermille,
                            long nativeLastMonoNanos,
                            long auditLastMonoMillis) {
            this.vmId = vmId;
            this.state = state;
            this.nativeEnabled = nativeEnabled;
            this.occupiedSlots = occupiedSlots;
            this.capacitySlots = capacitySlots;
            this.hitRatePermille = hitRatePermille;
            this.nativeLastMonoNanos = nativeLastMonoNanos;
            this.auditLastMonoMillis = auditLastMonoMillis;
        }
    }

    public static DiagnosticsSnapshot diagnostics(Context context, String vmId) {
        String key = normalizeVmId(vmId);
        VmFlowState state = current(key);
        boolean nativeEnabled = VmFlowNativeBridge.isAvailable();
        int occupied = 0;
        int capacity = 0;
        int hitRatePermille = 0;
        long nativeMonoNanos = 0L;
        if (nativeEnabled) {
            int[] stats = VmFlowNativeBridge.stats();
            if (stats != null && stats.length >= 3) {
                occupied = stats[0];
                capacity = stats[1];
                hitRatePermille = stats[2];
            }
            nativeMonoNanos = VmFlowNativeBridge.vmLastMonoNanos(stableVmHash(key));
        }
        long auditMonoMillis = AuditLedger.readLastMonoTimestampForVm(context, key);
        return new DiagnosticsSnapshot(key, state, nativeEnabled, occupied, capacity, hitRatePermille, nativeMonoNanos, auditMonoMillis);
    }

    public static boolean isNativeInteropEnabled() {
        return VmFlowNativeBridge.isAvailable();
    }

    private static int stableVmHash(String key) {
        int hash = 0x811C9DC5;
        for (int i = 0; i < key.length(); i++) {
            hash ^= key.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    private static String normalizeVmId(String vmId) {
        if (vmId == null) return "unknown";
        String normalized = vmId.trim();
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
