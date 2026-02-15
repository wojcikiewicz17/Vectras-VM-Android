package com.vectras.vm.qemu;

public final class ExecutionBudgetPolicy {

    private static final int MIN_BASE_CPUS = 2;
    private static final int THROUGHPUT_MIN_CPUS = 10;
    private static final int THROUGHPUT_MAX_CPUS = 23;

    private ExecutionBudgetPolicy() {
        throw new AssertionError("ExecutionBudgetPolicy is a utility class and cannot be instantiated");
    }

    public static int cpusFor(VmProfile profile) {
        int baseCpus = Math.max(MIN_BASE_CPUS, Runtime.getRuntime().availableProcessors() - 1);
        if (profile == VmProfile.THROUGHPUT) {
            return clamp(baseCpus, THROUGHPUT_MIN_CPUS, THROUGHPUT_MAX_CPUS);
        }
        return baseCpus;
    }

    public static boolean requiresSmp(VmProfile profile) {
        return profile == VmProfile.THROUGHPUT;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }
}
