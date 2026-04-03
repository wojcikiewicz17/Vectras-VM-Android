package com.vectras.vm.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized execution policy registry used by UI activities that run heavyweight workloads.
 *
 * <p>It provides deterministic thread/queue limits, CallerRuns rejection fallback and live
 * telemetry snapshots consumed by benchmark/professional reporting.</p>
 */
public final class ExecutionPolicyCenter {

    public enum Channel {
        BENCHMARK,
        PROFESSIONAL
    }

    public static final class GovernanceSnapshot {
        public final String profile;
        public final int effectiveSmp;
        public final int coreThreads;
        public final int maxThreads;
        public final int queueCapacity;
        public final int maxObservedQueueDepth;
        public final long rejectedCount;
        public final long callerRunsCount;
        public final boolean callerRunsEnabled;
        public final int processLimit;

        GovernanceSnapshot(String profile,
                           int effectiveSmp,
                           int coreThreads,
                           int maxThreads,
                           int queueCapacity,
                           int maxObservedQueueDepth,
                           long rejectedCount,
                           long callerRunsCount,
                           boolean callerRunsEnabled,
                           int processLimit) {
            this.profile = profile;
            this.effectiveSmp = effectiveSmp;
            this.coreThreads = coreThreads;
            this.maxThreads = maxThreads;
            this.queueCapacity = queueCapacity;
            this.maxObservedQueueDepth = maxObservedQueueDepth;
            this.rejectedCount = rejectedCount;
            this.callerRunsCount = callerRunsCount;
            this.callerRunsEnabled = callerRunsEnabled;
            this.processLimit = processLimit;
        }
    }

    private static final int BENCHMARK_QUEUE_CAPACITY = 8;
    private static final int PROFESSIONAL_QUEUE_CAPACITY = 8;

    private static final GovernedExecutor BENCHMARK_EXECUTOR = new GovernedExecutor(
            "Benchmark/Deterministic",
            "benchmark-center",
            1,
            1,
            BENCHMARK_QUEUE_CAPACITY
    );

    private static final GovernedExecutor PROFESSIONAL_EXECUTOR = new GovernedExecutor(
            "Professional/Scientific",
            "professional-center",
            1,
            1,
            PROFESSIONAL_QUEUE_CAPACITY
    );

    private ExecutionPolicyCenter() {
        throw new AssertionError("ExecutionPolicyCenter is utility-only");
    }

    public static ExecutorService executor(Channel channel) {
        return select(channel);
    }

    public static GovernanceSnapshot snapshot(Channel channel) {
        return select(channel).snapshot();
    }

    private static GovernedExecutor select(Channel channel) {
        return channel == Channel.PROFESSIONAL ? PROFESSIONAL_EXECUTOR : BENCHMARK_EXECUTOR;
    }

    private static final class GovernedExecutor extends ThreadPoolExecutor {
        private final String profile;
        private final int queueCapacity;
        private final AtomicInteger maxObservedQueueDepth = new AtomicInteger(0);
        private final AtomicLong rejectedCount = new AtomicLong(0);
        private final AtomicLong callerRunsCount = new AtomicLong(0);
        private final int processLimit;

        GovernedExecutor(String profile,
                         String threadPrefix,
                         int coreThreads,
                         int maxThreads,
                         int queueCapacity) {
            super(coreThreads,
                    maxThreads,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    new NamedThreadFactory(threadPrefix));
            this.profile = profile;
            this.queueCapacity = queueCapacity;
            this.processLimit = readPidMax();
            setRejectedExecutionHandler(new CountingCallerRunsPolicy(this));
        }

        @Override
        public void execute(Runnable command) {
            sampleQueueDepth();
            super.execute(command);
            sampleQueueDepth();
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            sampleQueueDepth();
            super.beforeExecute(t, r);
        }

        private void sampleQueueDepth() {
            int queueDepth = getQueue().size();
            int current;
            do {
                current = maxObservedQueueDepth.get();
                if (queueDepth <= current) {
                    return;
                }
            } while (!maxObservedQueueDepth.compareAndSet(current, queueDepth));
        }

        private void onRejected() {
            rejectedCount.incrementAndGet();
        }

        private void onCallerRuns() {
            callerRunsCount.incrementAndGet();
        }

        GovernanceSnapshot snapshot() {
            int cores = Runtime.getRuntime().availableProcessors();
            int effectiveSmp = Math.max(1, Math.min(getMaximumPoolSize(), cores));
            return new GovernanceSnapshot(
                    profile,
                    effectiveSmp,
                    getCorePoolSize(),
                    getMaximumPoolSize(),
                    queueCapacity,
                    maxObservedQueueDepth.get(),
                    rejectedCount.get(),
                    callerRunsCount.get(),
                    getRejectedExecutionHandler() instanceof CountingCallerRunsPolicy,
                    processLimit
            );
        }
    }

    private static final class CountingCallerRunsPolicy implements RejectedExecutionHandler {
        private final GovernedExecutor owner;

        CountingCallerRunsPolicy(GovernedExecutor owner) {
            this.owner = owner;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            owner.onRejected();
            if (!executor.isShutdown()) {
                owner.onCallerRuns();
                r.run();
            }
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger sequence = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    private static int readPidMax() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/sys/kernel/pid_max"))) {
            String value = reader.readLine();
            if (value == null) {
                return -1;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            RuntimeErrorReporter.warn("VRT-EPC-0001", "read_pid_max", "/proc/sys/kernel/pid_max", e);
            return -1;
        }
    }
}
