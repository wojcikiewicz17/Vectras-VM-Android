package com.vectras.vm.core;

import android.util.Log;

import com.vectras.vm.logger.VectrasStatus;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public final class LogcatRuntime {

    public interface Listener {
        void onBatchAppended(int appended);
    }

    private static final int RING_ENTRIES = 256;
    private static final int ENTRY_BYTES = 512;
    private static final int MAX_EVENTS_PER_BATCH = 64;
    private static final String TAG = "LogcatRuntime";

    private static final LogcatRuntime INSTANCE = new LogcatRuntime();

    private final AtomicInteger consumers = new AtomicInteger(0);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    private volatile boolean running;
    private Thread worker;

    private LogcatRuntime() {
    }

    public static LogcatRuntime getInstance() {
        return INSTANCE;
    }

    public synchronized void addListener(Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public synchronized void acquire() {
        int refs = consumers.incrementAndGet();
        if (refs == 1) {
            startWorker();
        }
    }

    public synchronized void release() {
        int refs = consumers.decrementAndGet();
        if (refs <= 0) {
            consumers.set(0);
            stopWorker();
        }
    }

    private void startWorker() {
        if (running) {
            return;
        }
        if (!NativeLogcatBridge.init(RING_ENTRIES, ENTRY_BYTES)) {
            return;
        }

        running = true;
        worker = new Thread(this::runLoop, "VectrasLogcatRuntime");
        worker.setDaemon(true);
        worker.start();
    }

    private void stopWorker() {
        running = false;
        Thread active = worker;
        worker = null;
        if (active != null) {
            active.interrupt();
            try {
                active.join(300L);
            } catch (InterruptedException e) {
                RuntimeErrorReporter.warn("VRT-LGC-0001", "stop_worker", "join", e);
                Thread.currentThread().interrupt();
            }
        }
        NativeLogcatBridge.shutdown();
    }

    private void runLoop() {
        while (running) {
            String payload = NativeLogcatBridge.readBatch(MAX_EVENTS_PER_BATCH);
            int appended = appendPayload(payload);
            if (appended == 0) {
                sleepQuietly(200L);
            }
        }
    }

    private int appendPayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return 0;
        }
        String[] lines = payload.split("\\n");
        int count = 0;
        for (String line : lines) {
            if (!line.isEmpty()) {
                VectrasStatus.logError(line);
                count++;
            }
        }
        if (count > 0) {
            for (Listener listener : listeners) {
                listener.onBatchAppended(count);
            }
        }
        return count;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            RuntimeErrorReporter.warn("VRT-LGC-0002", "sleep_worker", String.valueOf(millis), e);
            Thread.currentThread().interrupt();
            Log.d(TAG, "sleepQuietly interrupted");
        }
    }
}
