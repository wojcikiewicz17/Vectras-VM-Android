package com.vectras.vm.core;

import android.util.Log;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Concurrent stdout/stderr drainer that never blocks on one side only.
 */
public class ProcessOutputDrainer {
    private static final String TAG = "ProcessOutputDrainer";
    private static final double IO_ERROR_LOG_REFILL_PER_SEC = 0.2d;
    private static final int IO_ERROR_LOG_BURST = 2;
    private static final double IO_ERROR_SUPPRESSED_LOG_REFILL_PER_SEC = 0.1d;
    private static final int IO_ERROR_SUPPRESSED_LOG_BURST = 1;
    private static final double CLOSE_ERROR_LOG_REFILL_PER_SEC = 0.1d;
    private static final int CLOSE_ERROR_LOG_BURST = 1;
    public interface OutputLineConsumer {
        void onLine(String stream, String line);
    }

    public interface ErrorReporter {
        void onReadError(String stream, String vmContext, IOException error);

        void onReadErrorSuppressed(String stream, String vmContext, IOException error);
    }

    private final ExecutorService streamExecutor = Executors.newFixedThreadPool(2);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Object activeStreamLock = new Object();
    private InputStream activeStdout;
    private InputStream activeStderr;
    private final TokenBucketRateLimiter ioErrorLogLimiter =
            new TokenBucketRateLimiter(IO_ERROR_LOG_REFILL_PER_SEC, IO_ERROR_LOG_BURST);
    private final TokenBucketRateLimiter ioErrorSuppressedLogLimiter =
            new TokenBucketRateLimiter(IO_ERROR_SUPPRESSED_LOG_REFILL_PER_SEC, IO_ERROR_SUPPRESSED_LOG_BURST);
    private final TokenBucketRateLimiter closeErrorLogLimiter =
            new TokenBucketRateLimiter(CLOSE_ERROR_LOG_REFILL_PER_SEC, CLOSE_ERROR_LOG_BURST);
    private final ErrorReporter errorReporter;
    private final Set<InputStream> activeStreams = ConcurrentHashMap.newKeySet();

    public ProcessOutputDrainer() {
        this(new LogcatErrorReporter());
    }

    public ProcessOutputDrainer(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    public void cancel() {
        cancelled.set(true);
        for (InputStream stream : activeStreams) {
            try {
                stream.close();
            } catch (IOException e) {
                RuntimeErrorReporter.warn("VRT-DRN-0001", "close_stream_on_cancel", String.valueOf(stream), e);
            }
        }
    }

    public void drain(Process process, OutputLineConsumer consumer) throws InterruptedException {
        drain(process, null, consumer);
    }

    public void drain(Process process, String vmContext, OutputLineConsumer consumer) throws InterruptedException {
        Future<?> out = submitWorker("stdout", process.getInputStream(), vmContext, consumer);
        Future<?> err = submitWorker("stderr", process.getErrorStream(), vmContext, consumer);

        try {
            waitFuture(out);
            waitFuture(err);
        } catch (InterruptedException e) {
            out.cancel(true);
            err.cancel(true);
            throw e;
        } catch (RuntimeException e) {
            out.cancel(true);
            err.cancel(true);
            if (!cancelled.get()) {
                Log.w(TAG, "drain failed", e);
                throw e;
            }
        }
    }

    private Future<?> submitWorker(String streamName, InputStream stream, String vmContext, OutputLineConsumer consumer) {
        return streamExecutor.submit(() -> {
            registerActiveStream(streamName, stream);
            try {
                readStream(streamName, stream, vmContext, consumer);
            } finally {
                clearActiveStream(streamName, stream);
            }
        });
    }

    private void registerActiveStream(String streamName, InputStream stream) {
        synchronized (activeStreamLock) {
            if ("stderr".equals(streamName)) {
                activeStderr = stream;
            } else {
                activeStdout = stream;
            }
        }
    }

    private void clearActiveStream(String streamName, InputStream stream) {
        synchronized (activeStreamLock) {
            if ("stderr".equals(streamName)) {
                if (activeStderr == stream) {
                    activeStderr = null;
                }
            } else if (activeStdout == stream) {
                activeStdout = null;
            }
        }
    }

    public void shutdown() {
        streamExecutor.shutdownNow();
        try {
            streamExecutor.awaitTermination(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            RuntimeErrorReporter.warn("VRT-DRN-0002", "shutdown_executor", "awaitTermination", e);
            Thread.currentThread().interrupt();
        }
    }

    private void readStream(String name, InputStream stream, String vmContext, OutputLineConsumer consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            activeStreams.add(stream);
            String line;
            while (!cancelled.get() && (line = reader.readLine()) != null) {
                consumer.onLine(name, line);
            }
        } catch (IOException e) {
            if (cancelled.get()) {
                return;
            }
            if (ioErrorLogLimiter.tryAcquire()) {
                errorReporter.onReadError(name, vmContext, e);
            } else if (ioErrorSuppressedLogLimiter.tryAcquire()) {
                errorReporter.onReadErrorSuppressed(name, vmContext, e);
            }
        } finally {
            activeStreams.remove(stream);
        }
    }

    private static final class LogcatErrorReporter implements ErrorReporter {
        @Override
        public void onReadError(String stream, String vmContext, IOException error) {
            Log.w(TAG, formatMessage("readStream non-fatal failure", stream, vmContext, error), error);
        }

        @Override
        public void onReadErrorSuppressed(String stream, String vmContext, IOException error) {
            Log.w(TAG, formatMessage("readStream failure suppressed by rate-limit", stream, vmContext, error));
        }

        private static String formatMessage(String prefix, String stream, String vmContext, IOException error) {
            String contextPart = vmContext == null ? "" : " vmContext=" + vmContext;
            return prefix + " on " + stream + contextPart
                    + " [" + error.getClass().getSimpleName() + "]: " + error.getMessage();
        }
    }

    private void waitFuture(Future<?> future) throws InterruptedException {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cancelled.get() && cause instanceof CancellationException) {
                return;
            }
            throw new IllegalStateException("stream drain failed", cause != null ? cause : e);
        } catch (CancellationException e) {
            if (cancelled.get()) {
                return;
            }
            throw new IllegalStateException("stream drain cancelled", e);
        }
    }
}
