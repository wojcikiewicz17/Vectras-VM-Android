package com.vectras.vm.core;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.vectras.vm.StartVM;
import com.vectras.vm.main.core.MainStartVM;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rotina única de introspecção para status mínimo em JSON.
 */
public final class VmIntrospection {
    private static final String TAG = "VmIntrospection";
    private static final AtomicReference<String> SESSION_CORRELATION = new AtomicReference<>("session-unknown");

    private VmIntrospection() {}

    public static void beginSession(String vmId) {
        String stableVmId = (vmId == null || vmId.trim().isEmpty()) ? "unknown" : vmId.trim();
        String corr = stableVmId + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        SESSION_CORRELATION.set(corr);
    }

    public static String statusJson(Context context, String vmId) {
        RuntimeContract contract = StartVM.lastRuntimeContract;
        String stableVmId = (vmId == null || vmId.trim().isEmpty()) ? MainStartVM.ensureLastVmIdInitialized(vmId) : vmId.trim();
        VmFlowTracker.DiagnosticsSnapshot diag = VmFlowTracker.diagnostics(context, stableVmId);
        int[] nativeStats = VmFlowNativeBridge.stats();
        String nativeBridge = VmFlowNativeBridge.isAvailable() ? "native" : "java";
        if (nativeStats != null && nativeStats.length >= 3) {
            nativeBridge = nativeBridge + ":" + nativeStats[0] + "/" + nativeStats[1] + ":" + nativeStats[2];
        }

        String status = contract != null ? contract.status : "unknown";
        String engine = contract != null ? contract.engine : "qemu";
        String arch = contract != null ? contract.arch : "unknown";
        String mode = contract != null ? contract.mode : "unknown";
        String lastError = StartVM.lastStartError;
        long timestamp = System.currentTimeMillis();

        String json = "{"
                + "\"status\":\"" + esc(status) + "\"," 
                + "\"engine\":\"" + esc(engine) + "\"," 
                + "\"arch\":\"" + esc(arch) + "\"," 
                + "\"mode\":\"" + esc(mode) + "\"," 
                + "\"vm_state\":\"" + esc(diag.state.name()) + "\"," 
                + "\"native_bridge\":\"" + esc(nativeBridge + "@" + NativeFastPath.getFeatureMask()) + "\"," 
                + "\"last_error\":\"" + esc(lastError) + "\"," 
                + "\"timestamp\":" + timestamp
                + "}";

        Log.i(TAG, String.format(Locale.US,
                "event=vm_status corr=%s vm_id=%s vm_state=%s mount_state=%s native=%s mono_nanos=%d payload=%s",
                SESSION_CORRELATION.get(),
                stableVmId,
                diag.state.name(),
                StartVM.lastMountState,
                VmFlowNativeBridge.consolidatedCapabilityStatus(),
                VmFlowNativeBridge.vmLastMonoNanos(stableVmId.hashCode()),
                json));
        return json;
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
