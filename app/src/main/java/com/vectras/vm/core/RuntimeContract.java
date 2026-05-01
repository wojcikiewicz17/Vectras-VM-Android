package com.vectras.vm.core;

import android.content.Context;
import android.util.Log;

import com.vectras.vm.AppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class RuntimeContract {
    private static final String TAG = "RuntimeContract";

    public final String status;
    public final String engine;
    public final String arch;
    public final String mode;
    public final boolean nativeAvailable;
    public final String fallbackReason;
    public final int featureMask;
    public final String buildId;

    public RuntimeContract(String status, String engine, String arch, String mode,
                           boolean nativeAvailable, String fallbackReason, int featureMask, String buildId) {
        this.status = safe(status, "unknown");
        this.engine = safe(engine, "qemu");
        this.arch = safe(arch, "unknown");
        this.mode = safe(mode, "unknown");
        this.nativeAvailable = nativeAvailable;
        this.fallbackReason = safe(fallbackReason, "");
        this.featureMask = featureMask;
        this.buildId = safe(buildId, "unknown");
    }

    public RuntimeContract withStatus(String nextStatus) {
        return new RuntimeContract(nextStatus, engine, arch, mode, nativeAvailable, fallbackReason, featureMask, buildId);
    }

    public String toStableJson() {
        return "{"
                + "\"status\":\"" + esc(status) + "\"," 
                + "\"engine\":\"" + esc(engine) + "\"," 
                + "\"arch\":\"" + esc(arch) + "\"," 
                + "\"mode\":\"" + esc(mode) + "\"," 
                + "\"native_available\":" + (nativeAvailable ? "true" : "false") + ","
                + "\"fallback_reason\":\"" + esc(fallbackReason) + "\"," 
                + "\"feature_mask\":" + featureMask + ","
                + "\"build_id\":\"" + esc(buildId) + "\""
                + "}";
    }

    public static RuntimeContract fromStartVmPhase(String arch, String mode, String status) {
        boolean nativeAvailable = NativeFastPath.isNativeAvailable() && VmFlowNativeBridge.isAvailable();
        String fallback = "";
        if (!NativeFastPath.isNativeAvailable()) {
            fallback = NativeFastPath.getNativeInitError();
        } else if (!VmFlowNativeBridge.isAvailable()) {
            fallback = VmFlowNativeBridge.getLoadError();
        }
        return new RuntimeContract(
                status,
                "qemu",
                arch,
                mode,
                nativeAvailable,
                fallback,
                NativeFastPath.getFeatureMask(),
                AppConfig.vectrasVersion + "-" + Integer.toHexString(NativeFastPath.getNativeInitStatus())
        );
    }

    public static void persistSessionSnapshot(Context context, String vmId, RuntimeContract contract) {
        if (context == null || contract == null) return;
        String stableVmId = (vmId == null || vmId.trim().isEmpty()) ? "transient" : vmId.trim();
        File sessionDir = new File(context.getCacheDir(), stableVmId);
        if (!sessionDir.exists() && !sessionDir.mkdirs()) {
            Log.w(TAG, "Unable to create session dir for contract snapshot: " + stableVmId);
            return;
        }
        File out = new File(sessionDir, "runtime_contract_snapshot.json");
        try (FileOutputStream fos = new FileOutputStream(out, false)) {
            fos.write(contract.toStableJson().getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (Exception e) {
            Log.w(TAG, String.format(Locale.US, "Failed to persist runtime contract (%s)", stableVmId), e);
        }
    }

    private static String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String esc(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
