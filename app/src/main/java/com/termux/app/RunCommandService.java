package com.termux.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import com.vectras.vm.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * When allow-external-apps property is set to "true" in ~/.termux/termux.properties,  
 * is able to process execute intents sent by third-party applications.
 *
 * Third-party program must declare com.termux.permission.RUN_COMMAND permission and it should be
 * granted by user.
 *
 * Absolute path of command or script must be given in "RUN_COMMAND_PATH" extra.
 * The "RUN_COMMAND_ARGUMENTS", "RUN_COMMAND_WORKDIR" and "RUN_COMMAND_BACKGROUND" extras are 
 * optional. The workdir defaults to termux home. The background mode defaults to "false".
 * The command path and workdir can optionally be prefixed with "$PREFIX/" or "~/" if an absolute
 * path is not to be given.
 *
 * To automatically bring to foreground and start termux commands that were started with
 * background mode "false" in android >= 10 without user having to click the notification manually,
 * requires termux to be granted draw over apps permission due to new restrictions
 * of starting activities from the background, this also applies to :Tasker plugin.
 *
 * To reduce the chance of termux being killed by android even further due to violation of not
 * being able to call startForeground() within ~5s of service start in android >= 8, the user
 * may disable battery optimizations for termux.
 *
 * Sample code to run command "top" with java:
 *   Intent intent = new Intent();
 *   intent.setClassName("com.termux", "com.termux.app.RunCommandService");
 *   intent.setAction("com.termux.RUN_COMMAND");
 *   intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/top");
 *   intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-n", "5"});
 *   intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
 *   intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
 *   startService(intent);
 *
 * Sample code to run command "top" with "am startservice" command:
 * am startservice --user 0 -n com.termux/com.termux.app.RunCommandService 
 * -a com.termux.RUN_COMMAND 
 * --es com.termux.RUN_COMMAND_PATH '/data/data/com.termux/files/usr/bin/top' 
 * --esa com.termux.RUN_COMMAND_ARGUMENTS '-n,5' 
 * --es com.termux.RUN_COMMAND_WORKDIR '/data/data/com.termux/files/home'
 * --ez com.termux.RUN_COMMAND_BACKGROUND 'false'
 */
public class RunCommandService extends Service {

    public static final String RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND";
    public static final String RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH";
    public static final String RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS";
    public static final String RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR";
    public static final String RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND";

    private static final String NOTIFICATION_CHANNEL_ID = "termux_run_command_notification_channel";
    private static final int NOTIFICATION_ID = 1338;

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_REQUESTS_PER_UID_WINDOW = 10;
    private static final int MAX_GLOBAL_REQUESTS_WINDOW = 60;

    private static final Object RATE_LIMIT_LOCK = new Object();
    private static final Map<Integer, CounterWindow> UID_WINDOWS = new HashMap<>();
    private static final CounterWindow GLOBAL_WINDOW = new CounterWindow();

    private static final String AUDIT_TAG = "termux.run_command.audit";

    private static final class CounterWindow {
        long windowStartElapsed;
        int count;
    }

    class LocalBinder extends Binder {
        public final RunCommandService service = RunCommandService.this;
    }

    private final IBinder mBinder = new RunCommandService.LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        runStartForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        final int callerUid = resolveCallerUid();
        final String callerOrigin = resolveCallerOrigin(callerUid);

        if (intent == null) {
            audit(callerOrigin, "<null>", "blocked:intent_null");
            runStopForeground();
            return Service.START_NOT_STICKY;
        }

        if (!RUN_COMMAND_ACTION.equals(intent.getAction())) {
            audit(callerOrigin, sanitizeForAudit(intent.getStringExtra(RUN_COMMAND_PATH)), "blocked:invalid_action");
            runStopForeground();
            return Service.START_NOT_STICKY;
        }

        if (!allowExternalApps()) {
            audit(callerOrigin, sanitizeForAudit(intent.getStringExtra(RUN_COMMAND_PATH)), "blocked:external_apps_not_allowed");
            runStopForeground();
            return Service.START_NOT_STICKY;
        }

        String executablePath = parsePath(intent.getStringExtra(RUN_COMMAND_PATH));
        if (!isAllowedExecutablePath(executablePath)) {
            audit(callerOrigin, sanitizeForAudit(executablePath), "blocked:invalid_executable_path");
            runStopForeground();
            return Service.START_NOT_STICKY;
        }

        if (!consumeUidBudget(callerUid)) {
            audit(callerOrigin, sanitizeForAudit(executablePath), "blocked:uid_rate_limit");
            runStopForeground();
            return Service.START_NOT_STICKY;
        }

        if (!consumeGlobalBudget()) {
            audit(callerOrigin, sanitizeForAudit(executablePath), "blocked:global_rate_limit");
            runStopForeground();
            return Service.START_NOT_STICKY;
        }

        Uri programUri = new Uri.Builder().scheme("com.termux.file").path(executablePath).build();

        Intent execIntent = new Intent(TermuxService.ACTION_EXECUTE, programUri);
        execIntent.setClass(this, TermuxService.class);
        execIntent.putExtra(TermuxService.EXTRA_ARGUMENTS, intent.getStringArrayExtra(RUN_COMMAND_ARGUMENTS));
        execIntent.putExtra(TermuxService.EXTRA_CURRENT_WORKING_DIRECTORY, parsePath(intent.getStringExtra(RUN_COMMAND_WORKDIR)));
        execIntent.putExtra(TermuxService.EXTRA_EXECUTE_IN_BACKGROUND, intent.getBooleanExtra(RUN_COMMAND_BACKGROUND, false));

        audit(callerOrigin, sanitizeForAudit(executablePath), "allowed");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(execIntent);
        } else {
            this.startService(execIntent);
        }


        runStopForeground();

        return Service.START_NOT_STICKY;
    }

    private void runStartForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel();
            startForeground(NOTIFICATION_ID, buildNotification());
        }
    }

    private void runStopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getText(R.string.application_name) + " Run Command");
        builder.setSmallIcon(R.drawable.ic_service_notification);

        // Use a low priority:
        builder.setPriority(Notification.PRIORITY_LOW);

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Background color for small notification icon:
        builder.setColor(0xFF607D8B);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        return builder.build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        String channelName = " Run Command";
        int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private boolean allowExternalApps() {
        File propsFile = new File(TermuxService.HOME_PATH + "/.termux/termux.properties");
        if (!propsFile.exists())
            propsFile = new File(TermuxService.HOME_PATH + "/.config/termux/termux.properties");

        Properties props = new Properties();
        try {
            if (propsFile.isFile() && propsFile.canRead()) {
                try (FileInputStream in = new FileInputStream(propsFile)) {
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            Log.e("termux", "Error loading props", e);
        }

        return props.getProperty("allow-external-apps", "false").equals("true");
    }

    private int resolveCallerUid() {
        int uid = Binder.getCallingUid();
        return uid > 0 ? uid : Process.myUid();
    }

    private String resolveCallerOrigin(int uid) {
        String[] packages = getPackageManager().getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            return "uid:" + uid;
        }
        return packages[0] + "(uid:" + uid + ")";
    }

    private boolean isAllowedExecutablePath(String path) {
        if (path == null || path.isEmpty()) return false;

        String canonicalPath;
        try {
            canonicalPath = new File(path).getCanonicalPath();
        } catch (IOException e) {
            return false;
        }

        return isWithinDirectory(canonicalPath, TermuxService.PREFIX_PATH)
            || isWithinDirectory(canonicalPath, TermuxService.HOME_PATH)
            || isWithinDirectory(canonicalPath, TermuxService.OPT_PATH);
    }

    private boolean isWithinDirectory(String candidatePath, String allowedRoot) {
        try {
            String canonicalRoot = new File(allowedRoot).getCanonicalPath();
            return candidatePath.equals(canonicalRoot)
                || candidatePath.startsWith(canonicalRoot + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean consumeUidBudget(int uid) {
        synchronized (RATE_LIMIT_LOCK) {
            long now = android.os.SystemClock.elapsedRealtime();
            CounterWindow window = UID_WINDOWS.get(uid);
            if (window == null) {
                window = new CounterWindow();
                UID_WINDOWS.put(uid, window);
            }

            resetWindowIfNeeded(window, now);
            if (window.count >= MAX_REQUESTS_PER_UID_WINDOW) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    private boolean consumeGlobalBudget() {
        synchronized (RATE_LIMIT_LOCK) {
            long now = android.os.SystemClock.elapsedRealtime();
            resetWindowIfNeeded(GLOBAL_WINDOW, now);
            if (GLOBAL_WINDOW.count >= MAX_GLOBAL_REQUESTS_WINDOW) {
                return false;
            }
            GLOBAL_WINDOW.count++;
            return true;
        }
    }

    private void resetWindowIfNeeded(CounterWindow window, long now) {
        if (window.windowStartElapsed == 0L || (now - window.windowStartElapsed) >= WINDOW_MILLIS) {
            window.windowStartElapsed = now;
            window.count = 0;
        }
    }

    private String sanitizeForAudit(String commandPath) {
        if (commandPath == null || commandPath.isEmpty()) {
            return "<empty>";
        }
        String sanitized = commandPath.replaceAll("[\\r\\n\\t]", " ");
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...";
        }
        return sanitized;
    }

    private void audit(String origin, String command, String decision) {
        Log.i(AUDIT_TAG, "origin=" + origin + " command=" + command + " decision=" + decision);
    }

    /** Replace "$PREFIX/" or "~/" prefix with termux absolute paths */
    private String parsePath(String path) {
        if(path != null && !path.isEmpty()) {
            path = path.replaceAll("^\\$PREFIX\\/", TermuxService.PREFIX_PATH + "/");
            path = path.replaceAll("^~\\/", TermuxService.HOME_PATH + "/");
        }

        return path;
    }
}
