package com.vectras.vm.core;

/**
 * DeterministicRuntimeMatrix: commutative factor model for low-overhead runtime tuning.
 *
 * <p>Uses only platform/base runtime data and native fast-path hints to produce stable,
 * order-independent knobs for IOPS, latency, IRQ cadence and copy quantum.</p>
 */
public final class DeterministicRuntimeMatrix {

    private static volatile Snapshot cachedSnapshot;

    public static final class Snapshot {
        public final int arch;
        public final int cores;
        public final int pointerBits;
        public final int pageBytes;
        public final int cacheLineBytes;
        public final int features;
        public final int ioQuantumBytes;
        public final int irqPeriodMicros;
        public final int workerParallelism;
        public final long deterministicProduct;

        private Snapshot(int arch, int cores, int pointerBits, int pageBytes, int cacheLineBytes,
                         int features, int ioQuantumBytes, int irqPeriodMicros,
                         int workerParallelism, long deterministicProduct) {
            this.arch = arch;
            this.cores = cores;
            this.pointerBits = pointerBits;
            this.pageBytes = pageBytes;
            this.cacheLineBytes = cacheLineBytes;
            this.features = features;
            this.ioQuantumBytes = ioQuantumBytes;
            this.irqPeriodMicros = irqPeriodMicros;
            this.workerParallelism = workerParallelism;
            this.deterministicProduct = deterministicProduct;
        }
    }

    private DeterministicRuntimeMatrix() {
        throw new AssertionError("No instances");
    }

    public static Snapshot capture() {
        Snapshot snapshot = cachedSnapshot;
        if (snapshot != null) {
            return snapshot;
        }

        int signature = NativeFastPath.getPlatformSignature();
        int arch = signature & 0xFF00;
        int bits = NativeFastPath.getPointerBits();
        int page = NativeFastPath.getNativePageBytes();
        int line = NativeFastPath.getNativeCacheLineBytes();
        int features = NativeFastPath.getFeatureMask();
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());

        long product = deterministicProduct(arch, bits, page, line, cores, features);

        int ioQuantum = deriveIoQuantum(page, line, cores, features);
        int irqPeriod = deriveIrqPeriodMicros(line, cores, features);
        int workers = deriveParallelism(cores, features);

        Snapshot computed = new Snapshot(arch, cores, bits, page, line, features, ioQuantum, irqPeriod, workers, product);
        cachedSnapshot = computed;
        return computed;
    }

    public static void invalidateSnapshot() {
        cachedSnapshot = null;
    }

    private static long deterministicProduct(int arch, int bits, int page, int line, int cores, int features) {
        int f0 = normalizeFactor(arch);
        int f1 = normalizeFactor(bits);
        int f2 = normalizeFactor(page);
        int f3 = normalizeFactor(line);
        int f4 = normalizeFactor(cores);
        int f5 = normalizeFactor(features);

        if (f0 > f1) { int t = f0; f0 = f1; f1 = t; }
        if (f2 > f3) { int t = f2; f2 = f3; f3 = t; }
        if (f4 > f5) { int t = f4; f4 = f5; f5 = t; }
        if (f0 > f2) { int t = f0; f0 = f2; f2 = t; }
        if (f1 > f3) { int t = f1; f1 = f3; f3 = t; }
        if (f2 > f4) { int t = f2; f2 = f4; f4 = t; }
        if (f3 > f5) { int t = f3; f3 = f5; f5 = t; }
        if (f1 > f2) { int t = f1; f1 = f2; f2 = t; }
        if (f3 > f4) { int t = f3; f3 = f4; f4 = t; }
        if (f0 > f1) { int t = f0; f0 = f1; f1 = t; }
        if (f2 > f3) { int t = f2; f2 = f3; f3 = t; }
        if (f4 > f5) { int t = f4; f4 = f5; f5 = t; }
        if (f1 > f2) { int t = f1; f1 = f2; f2 = t; }
        if (f3 > f4) { int t = f3; f3 = f4; f4 = t; }
        if (f2 > f3) { int t = f2; f2 = f3; f3 = t; }

        long product = 1L;
        product = boundedMultiply(product, f0);
        product = boundedMultiply(product, f1);
        product = boundedMultiply(product, f2);
        product = boundedMultiply(product, f3);
        product = boundedMultiply(product, f4);
        product = boundedMultiply(product, f5);
        return product;
    }

    private static int deriveIoQuantum(int page, int line, int cores, int features) {
        int base = page * Math.max(1, cores >= 8 ? 8 : (cores >= 4 ? 4 : 2));
        if ((features & NativeFastPath.FEATURE_AVX2) != 0 || (features & NativeFastPath.FEATURE_NEON) != 0) {
            base <<= 1;
        }
        int align = Math.max(32, line);
        int rem = base & (align - 1);
        if (rem != 0) {
            base += align - rem;
        }
        return clamp(base, 4096, 1024 * 1024);
    }

    private static int deriveIrqPeriodMicros(int line, int cores, int features) {
        int base = 1500;
        if (cores >= 8) base -= 350;
        else if (cores >= 4) base -= 200;

        if ((features & NativeFastPath.FEATURE_CRC32) != 0) base -= 80;
        if ((features & NativeFastPath.FEATURE_AVX2) != 0 || (features & NativeFastPath.FEATURE_NEON) != 0) base -= 120;
        if (line >= 128) base += 70;

        return clamp(base, 500, 2500);
    }

    private static int deriveParallelism(int cores, int features) {
        int base = Math.max(1, cores - 1);
        if ((features & NativeFastPath.FEATURE_AVX2) != 0) {
            return Math.max(1, base - 1);
        }
        return base;
    }

    private static int normalizeFactor(int value) {
        if (value == Integer.MIN_VALUE) return 1;
        int v = Math.abs(value);
        return v == 0 ? 1 : v;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long boundedMultiply(long left, long right) {
        if (left == 0 || right == 0) return 0;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }
}
