package com.vectras.vm.core;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessOutputDrainerTest {
    @Test
    public void shouldDrainStdoutAndStderrWithoutDeadlock() throws Exception {
        Process fake = new FakeProcess("o1\no2\n", "e1\ne2\n");
        ProcessOutputDrainer drainer = new ProcessOutputDrainer();
        AtomicInteger out = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();

        drainer.drain(fake, (stream, line) -> {
            if ("stderr".equals(stream)) err.incrementAndGet();
            else out.incrementAndGet();
        });
        drainer.shutdown();

        Assert.assertEquals(2, out.get());
        Assert.assertEquals(2, err.get());
    }




    @Test
    public void shouldDrainWhenStderrFloods() throws Exception {
        int stderrLines = 2000;
        StringBuilder stderr = new StringBuilder();
        for (int i = 0; i < stderrLines; i++) {
            stderr.append("e").append(i).append("\n");
        }
        Process fake = new FakeProcess("o1\n", stderr.toString());
        ProcessOutputDrainer drainer = new ProcessOutputDrainer();
        AtomicInteger out = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();

        try {
            drainer.drain(fake, (stream, line) -> {
                if ("stderr".equals(stream)) err.incrementAndGet();
                else out.incrementAndGet();
            });
        } finally {
            drainer.shutdown();
        }

        Assert.assertEquals(1, out.get());
        Assert.assertEquals(stderrLines, err.get());
    }
    @Test(expected = IllegalStateException.class)
    public void shouldPropagateWorkerFailure() throws Exception {
        Process fake = new FakeProcess("ok\n", "err\n");
        ProcessOutputDrainer drainer = new ProcessOutputDrainer();
        try {
            drainer.drain(fake, (stream, line) -> {
                if ("stdout".equals(stream)) {
                    throw new IllegalStateException("boom");
                }
            });
        } finally {
            drainer.shutdown();
        }
    }

    @Test
    public void shouldHandleIOExceptionFromReadWithoutCrashingAndReportError() throws Exception {
        CountingErrorReporter reporter = new CountingErrorReporter();
        ProcessOutputDrainer drainer = new ProcessOutputDrainer(reporter);
        Process fake = new DualStreamProcess(
                new FailingInputStream(new byte[]{'o', 'k', '\n'}, 2),
                new ByteArrayInputStream(new byte[0])
        );

        try {
            drainer.drain(fake, "vm-audit-ctx", (stream, line) -> { });
        } finally {
            drainer.shutdown();
        }

        Assert.assertEquals(1, reporter.acceptedErrors.get());
        Assert.assertEquals(0, reporter.suppressedErrors.get());
        Assert.assertEquals("stdout", reporter.lastStream);
        Assert.assertEquals("vm-audit-ctx", reporter.lastVmContext);
    }

    @Test
    public void shouldRateLimitMultipleDrainReadFailures() throws Exception {
        CountingErrorReporter reporter = new CountingErrorReporter();
        ProcessOutputDrainer drainer = new ProcessOutputDrainer(reporter);

        try {
            for (int i = 0; i < 10; i++) {
                Process fake = new DualStreamProcess(
                        new FailingInputStream(new byte[]{'x', '\n'}, 1),
                        new ByteArrayInputStream(new byte[0])
                );
                drainer.drain(fake, "vm-rate-limit", (stream, line) -> { });
            }
        } finally {
            drainer.shutdown();
        }

        Assert.assertTrue("accepted should be limited", reporter.acceptedErrors.get() <= 2);
        Assert.assertTrue("suppressed should be limited", reporter.suppressedErrors.get() <= 1);
        Assert.assertTrue("some errors must be observable", reporter.acceptedErrors.get() >= 1);
    }

    private static class FakeProcess extends Process {
        private final InputStream stdout;
        private final InputStream stderr;

        FakeProcess(String stdout, String stderr) {
            this.stdout = new ByteArrayInputStream(stdout.getBytes());
            this.stderr = new ByteArrayInputStream(stderr.getBytes());
        }

        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream() { return stdout; }
        @Override public InputStream getErrorStream() { return stderr; }
        @Override public int waitFor() { return 0; }
        @Override public int exitValue() { return 0; }
        @Override public void destroy() {}
    }

    private static class DualStreamProcess extends Process {
        private final InputStream stdout;
        private final InputStream stderr;

        DualStreamProcess(InputStream stdout, InputStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public InputStream getInputStream() { return stdout; }
        @Override public InputStream getErrorStream() { return stderr; }
        @Override public int waitFor() { return 0; }
        @Override public int exitValue() { return 0; }
        @Override public void destroy() {}
    }

    private static class FailingInputStream extends InputStream {
        private final byte[] data;
        private final int failAtReadCount;
        private int index;
        private int readCount;

        FailingInputStream(byte[] data, int failAtReadCount) {
            this.data = data;
            this.failAtReadCount = failAtReadCount;
        }

        @Override
        public int read() throws IOException {
            readCount++;
            if (readCount >= failAtReadCount) {
                throw new IOException("forced-read-failure-" + failAtReadCount);
            }
            if (index >= data.length) {
                return -1;
            }
            return data[index++];
        }
    }

    private static class CountingErrorReporter implements ProcessOutputDrainer.ErrorReporter {
        private final AtomicInteger acceptedErrors = new AtomicInteger();
        private final AtomicInteger suppressedErrors = new AtomicInteger();
        private volatile String lastStream;
        private volatile String lastVmContext;

        @Override
        public void onReadError(String stream, String vmContext, IOException error) {
            acceptedErrors.incrementAndGet();
            lastStream = stream;
            lastVmContext = vmContext;
        }

        @Override
        public void onReadErrorSuppressed(String stream, String vmContext, IOException error) {
            suppressedErrors.incrementAndGet();
            lastStream = stream;
            lastVmContext = vmContext;
        }
    }
}
