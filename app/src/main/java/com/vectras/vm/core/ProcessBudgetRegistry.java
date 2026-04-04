package com.vectras.vm.core;

import android.os.Build;
import android.util.Log;

import com.vectras.vm.BuildConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Runtime process-budget resolver for VM supervision caps.
 * Priority: System property override > BuildConfig > hardcoded safety default.
 */
public final class ProcessBudgetRegistry {

    private static final String TAG = "ProcessBudgetRegistry";
    private static final String PROP_PROCESS_BUDGET = "vectras.process_budget_max";
    private static final String PROP_DEBUG_PROCESS_BUDGET = "debug.vectras.process_budget_max";

    private static final int HARDCODED_DEFAULT_LEGACY = 9;
    private static final int HARDCODED_DEFAULT_ANDROID15 = 6;
    private static final int HARD_MIN = 1;
    private static final int HARD_MAX = 32;

    private static final int RESOLVED_MAX;
    private static final String RESOLVED_SOURCE;
    private static final ProcessBudgetRegistry INSTANCE = new ProcessBudgetRegistry(true);

    private final Map<SlotToken, Entry> activeByToken = new HashMap<>();
    private final Map<Process, SlotToken> tokenByProcess = new IdentityHashMap<>();

    static {
        Resolution resolution = resolveInternal();
        RESOLVED_MAX = resolution.max;
        RESOLVED_SOURCE = resolution.source;
        Log.i(TAG, "init sdk=" + Build.VERSION.SDK_INT
                + " resolvedMax=" + RESOLVED_MAX
                + " source=" + RESOLVED_SOURCE);
    }

    private ProcessBudgetRegistry() {
        throw new AssertionError("ProcessBudgetRegistry is a utility class and cannot be instantiated");
    }
    private ProcessBudgetRegistry(boolean internal) {
        // singleton ctor
    }

    public static ProcessBudgetRegistry get() {
        return INSTANCE;
    }

    public static int getMaxSupervisedVmProcesses() {
        return RESOLVED_MAX;
    }

    public static String getResolvedSource() {
        return RESOLVED_SOURCE;
    }

    public static final class SlotToken {
        final String id;
        SlotToken(String id) { this.id = id; }
    }

    public static final class BudgetToken {
        final SlotToken slotToken;
        BudgetToken(SlotToken slotToken) { this.slotToken = slotToken; }
    }

    private static final class Entry {
        final SlotToken token;
        final String feature;
        final String tag;
        final String caller;
        final String vmId;
        Process process;

        Entry(SlotToken token, String feature, String tag, String caller, String vmId) {
            this.token = token;
            this.feature = feature;
            this.tag = tag;
            this.caller = caller;
            this.vmId = vmId;
        }
    }

    public static final class Snapshot {
        public final int activeCount;
        public final int maxAllowed;
        public final Map<String, Integer> byFeature;

        Snapshot(int activeCount, int maxAllowed, Map<String, Integer> byFeature) {
            this.activeCount = activeCount;
            this.maxAllowed = maxAllowed;
            this.byFeature = byFeature;
        }
    }

    public static SlotToken tryAcquireSlot(String feature, String tag, String caller, String vmId) {
        return INSTANCE.tryAcquireSlotInternal(feature, tag, caller, vmId);
    }

    public synchronized SlotToken tryAcquireSlotInternal(String feature, String tag, String caller, String vmId) {
        cleanupDeadProcessesLocked();
        if (activeByToken.size() >= getMaxSupervisedVmProcesses()) {
            return null;
        }
        SlotToken token = new SlotToken(feature + ":" + System.nanoTime());
        activeByToken.put(token, new Entry(token, feature, tag, caller, vmId));
        return token;
    }

    public static void bindProcess(SlotToken token, Process process) {
        INSTANCE.bindProcessInternal(token, process);
    }

    public synchronized void bindProcessInternal(SlotToken token, Process process) {
        Entry entry = activeByToken.get(token);
        if (entry == null || process == null) return;
        entry.process = process;
        tokenByProcess.put(process, token);
    }

    public static void releaseSlot(SlotToken token, String reason) {
        INSTANCE.releaseSlotInternal(token, reason);
    }

    public synchronized void releaseSlotInternal(SlotToken token, String reason) {
        if (token == null) return;
        Entry entry = activeByToken.remove(token);
        if (entry != null && entry.process != null) {
            tokenByProcess.remove(entry.process);
        }
    }

    public static BudgetToken acquire(String feature, String tag, String caller, String vmId, long processPid, int maxBudget) {
        SlotToken slotToken = tryAcquireSlot(feature, tag, caller, vmId);
        return slotToken == null ? null : new BudgetToken(slotToken);
    }

    public static void bind(BudgetToken token, Process process, String vmId, long processPid) {
        if (token == null) return;
        bindProcess(token.slotToken, process);
    }

    public static void release(BudgetToken token, String vmId, long processPid) {
        if (token == null) return;
        releaseSlot(token.slotToken, "budget_release");
    }

    public static void releaseByProcess(Process process, String vmId, long processPid) {
        INSTANCE.releaseByProcessInternal(process);
    }

    private synchronized void releaseByProcessInternal(Process process) {
        if (process == null) return;
        SlotToken token = tokenByProcess.remove(process);
        if (token != null) {
            activeByToken.remove(token);
        }
    }

    public synchronized Snapshot snapshot() {
        cleanupDeadProcessesLocked();
        Map<String, Integer> featureCounts = new HashMap<>();
        for (Entry entry : activeByToken.values()) {
            if (entry == null) continue;
            String feature = entry.feature == null ? "unknown" : entry.feature;
            featureCounts.put(feature, featureCounts.getOrDefault(feature, 0) + 1);
        }
        return new Snapshot(activeByToken.size(), getMaxSupervisedVmProcesses(), Collections.unmodifiableMap(featureCounts));
    }

    private void cleanupDeadProcessesLocked() {
        activeByToken.entrySet().removeIf(item -> {
            Entry entry = item.getValue();
            Process p = entry == null ? null : entry.process;
            if (p == null) return false;
            if (p.isAlive()) return false;
            tokenByProcess.remove(p);
            return true;
        });
    }

    private static Resolution resolveInternal() {
        Integer fromProperty = parsePropertyOverride();
        if (fromProperty != null) {
            return new Resolution(clamp(fromProperty), "property");
        }

        int fromBuild = Build.VERSION.SDK_INT >= 35
                ? BuildConfig.PROCESS_BUDGET_MAX_ANDROID15
                : BuildConfig.PROCESS_BUDGET_MAX_DEFAULT;
        if (fromBuild > 0) {
            return new Resolution(clamp(fromBuild), "build");
        }

        int fromDefault = Build.VERSION.SDK_INT >= 35
                ? HARDCODED_DEFAULT_ANDROID15
                : HARDCODED_DEFAULT_LEGACY;
        return new Resolution(clamp(fromDefault), "default");
    }

    private static Integer parsePropertyOverride() {
        Integer debugValue = parseIntProperty(PROP_DEBUG_PROCESS_BUDGET);
        if (debugValue != null) {
            return debugValue;
        }
        return parseIntProperty(PROP_PROCESS_BUDGET);
    }

    private static Integer parseIntProperty(String key) {
        String raw = System.getProperty(key);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            RuntimeErrorReporter.warn("VRT-PBR-0001", "parse_process_budget_property", key + "=" + raw, e);
            Log.w(TAG, "Ignoring invalid property " + key + "=" + raw);
            return null;
        }
    }

    private static int clamp(int value) {
        if (value < HARD_MIN) {
            return HARD_MIN;
        }
        if (value > HARD_MAX) {
            return HARD_MAX;
        }
        return value;
    }

    private static final class Resolution {
        final int max;
        final String source;

        Resolution(int max, String source) {
            this.max = max;
            this.source = source;
        }
    }
}
