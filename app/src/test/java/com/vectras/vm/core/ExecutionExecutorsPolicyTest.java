package com.vectras.vm.core;

import com.vectras.vm.qemu.VmProfile;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutionExecutorsPolicyTest {

    @Test
    public void defaultPolicyHasExpectedDomainNamesAndCaps() {
        ExecutionBudgetPolicy policy = ExecutionBudgetPolicy.defaults();

        Assert.assertEquals("terminal-io", policy.terminalIo().threadPrefix);
        Assert.assertEquals(64, policy.terminalIo().queueCapacity);
        Assert.assertEquals(ExecutionBudgetPolicy.RejectionPolicy.CALLER_RUNS, policy.terminalIo().rejectionPolicy);
        Assert.assertEquals("terminal-wait", policy.terminalWait().threadPrefix);
        Assert.assertEquals(16, policy.terminalWait().queueCapacity);
        Assert.assertEquals(ExecutionBudgetPolicy.RejectionPolicy.CALLER_RUNS, policy.terminalWait().rejectionPolicy);
        Assert.assertEquals("shell-executor", policy.shellExecutor().threadPrefix);
        Assert.assertEquals(32, policy.shellExecutor().queueCapacity);
        Assert.assertEquals(ExecutionBudgetPolicy.RejectionPolicy.CALLER_RUNS, policy.shellExecutor().rejectionPolicy);
        Assert.assertEquals("process-supervisor-qmp", policy.processSupervisorQmp().threadPrefix);
        Assert.assertEquals(16, policy.processSupervisorQmp().queueCapacity);
        Assert.assertEquals(ExecutionBudgetPolicy.RejectionPolicy.CALLER_RUNS, policy.processSupervisorQmp().rejectionPolicy);
    }

    @Test
    public void snapshotsExposeUnifiedObservabilityFields() {
        ExecutionExecutors executors = ExecutionExecutors.get();
        ExecutionExecutors.DomainSnapshot shell = executors.shellExecutorSnapshot();

        Assert.assertEquals("shell-executor", shell.domain);
        Assert.assertTrue(shell.queueSize >= 0);
        Assert.assertTrue(shell.queueRemainingCapacity >= 0);
        Assert.assertTrue(shell.rejectedCount >= 0);
        Assert.assertTrue(shell.saturatedCount >= 0);
        Assert.assertTrue(shell.createdThreads >= 0);
    }

    @Test
    public void corePolicyRejectionPolicyPropagatesToShellExecutor() {
        ExecutionBudget budget = CoreExecutionBudgetPolicy.resolve(VmProfile.BALANCED, 8, "X86_64");
        ThreadPoolBudget.RejectionPolicy expected = budget.getThreadPoolBudget().getRejectionPolicy();
        Assert.assertEquals(ThreadPoolBudget.RejectionPolicy.CALLER_RUNS, expected);

        ExecutionExecutors executors = ExecutionExecutors.get();
        ThreadPoolExecutor pool = executors.newShellExecutorPool(
                ExecutionBudgetPolicy.RejectionPolicy.valueOf(expected.name()));
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(2);
        Future<?> first = pool.submit(() -> {
            started.countDown();
            waitUnchecked(release);
        });
        Future<?> second = pool.submit(() -> {
            started.countDown();
            waitUnchecked(release);
        });
        Assert.assertTrue(awaitWithTimeout(started));

        for (int i = 0; i < 128; i++) {
            pool.submit(() -> { });
        }

        release.countDown();
        first.cancel(true);
        second.cancel(true);
        pool.shutdownNow();
    }

    private static boolean awaitWithTimeout(CountDownLatch latch) {
        try {
            return latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void waitUnchecked(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
