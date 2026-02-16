package com.termux.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;

public class BellUtil {
    private static BellUtil instance = null;
    private static final Object lock = new Object();

    public static BellUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new BellUtil((Vibrator) context.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE));
                }
            }
        }

        return instance;
    }

    private static final long DURATION = 50;
    private static final long MIN_PAUSE = 3 * DURATION;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastBell = 0;
    private final Runnable bellRunnable;

    private BellUtil(final Vibrator vibrator) {
        bellRunnable = new Runnable() {
            @Override
            public void run() {
                if (vibrator != null) {
                    vibrator.vibrate(DURATION);
                }
            }
        };
    }

    public synchronized void doBell() {
        long now = now();
        long timeSinceLastBell = now - lastBell;

        if (timeSinceLastBell < 0) {
            // Coalescing policy: when calls arrive in high frequency and there is already
            // a pending bell, keep the existing schedule and do not enqueue duplicates.
        } else if (timeSinceLastBell < MIN_PAUSE) {
            // there was a bell recently; schedule only one pending bell for the earliest
            // allowed instant and replace any stale callback before posting.
            long nextBellAt = lastBell + MIN_PAUSE;
            handler.removeCallbacks(bellRunnable);
            handler.postDelayed(bellRunnable, nextBellAt - now);
            lastBell = nextBellAt;
        } else {
            // the last bell was long ago, do it now
            bellRunnable.run();
            lastBell = now;
        }
    }

    private long now() {
        return SystemClock.uptimeMillis();
    }
}
