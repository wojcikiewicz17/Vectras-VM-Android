package com.vectras.vm.core;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ShellExecutorConcurrencyTest {

    @Test
    public void usesFixedBoundedExecutorConfiguration() {
        ShellExecutor executor = new ShellExecutor();
        try {
            Assert.assertEquals(2, executor.getPoolSizeLimitForTests());
            Assert.assertEquals(32, executor.getQueueCapacityForTests());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void repeatedExecuteDoesNotGrowThreadsBeyondLimit() {
        ShellExecutor executor = new ShellExecutor();
        try {
            for (int i = 0; i < 40; i++) {
                executor.execute("echo test", 50L);
            }
            Assert.assertTrue(executor.getCreatedThreadCountForTests() <= executor.getPoolSizeLimitForTests());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void shutdownStopsInjectedExecutor() throws InterruptedException {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1)
        );
        ShellExecutor executor = new ShellExecutor(pool);

        executor.shutdown();

        Assert.assertTrue(pool.isShutdown());
        Assert.assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
    }
}
