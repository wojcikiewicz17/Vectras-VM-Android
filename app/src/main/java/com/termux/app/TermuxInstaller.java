package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.UserManager;
import android.system.Os;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import com.vectras.vm.R;
import com.termux.terminal.EmulatorDebug;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Install the  bootstrap packages if necessary by following the below steps:
 * <p/>
 * (1) If $PREFIX already exist, assume that it is correct and be done. Note that this relies on that we do not create a
 * broken $PREFIX folder below.
 * <p/>
 * (2) A progress dialog is shown with "Installing..." message and a spinner.
 * <p/>
 * (3) A staging folder, $STAGING_PREFIX, is {@link #deleteFolder(File)} if left over from broken installation below.
 * <p/>
 * (4) The zip file is loaded from a shared library.
 * <p/>
 * (5) The zip, containing entries relative to the $PREFIX, is is downloaded and extracted by a zip input stream
 * continuously encountering zip file entries:
 * <p/>
 * (5.1) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (5.2) For every other zip entry, extract it into $STAGING_PREFIX and set execute permissions if necessary.
 */
final class TermuxInstaller {
    private static final class BootstrapInstallException extends RuntimeException {
        final int userMessageResId;

        BootstrapInstallException(String message, int userMessageResId) {
            super(message);
            this.userMessageResId = userMessageResId;
        }
    }

    /** Performs setup if necessary. */
    static void setupIfNeeded(final Activity activity, final Runnable whenDone) {
        //  can only be run as the primary user (device owner) since only that
        // account has the expected file system paths. Verify that:
        UserManager um = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        boolean isPrimaryUser = um.getSerialNumberForUser(android.os.Process.myUserHandle()) == 0;
        if (!isPrimaryUser) {
            new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(R.string.bootstrap_error_not_primary_user_message)
                .setOnDismissListener(dialog -> {
                    Log.i(EmulatorDebug.LOG_TAG, "bootstrap_setup_exit: reason=not_primary_user action=finishAffinity");
                    activity.finishAffinity();
                }).setPositiveButton(android.R.string.ok, null).show();
            return;
        }

        final File PREFIX_FILE = new File(TermuxService.PREFIX_PATH);
        if (PREFIX_FILE.isDirectory()) {
            whenDone.run();
            return;
        }

        final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    final String STAGING_PREFIX_PATH = TermuxService.FILES_PATH + "usr-staging";
                    final File STAGING_PREFIX_FILE = new File(STAGING_PREFIX_PATH);

                    if (STAGING_PREFIX_FILE.exists()) {
                        deleteFolder(STAGING_PREFIX_FILE);
                    }

                    final byte[] buffer = new byte[8096];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(50);

                    final byte[] zipBytes = loadZipBytes();
                    if (zipBytes == null || zipBytes.length == 0) {
                        final String errorMessage = "Bootstrap archive is empty or unavailable: loadZipBytes() returned null/empty data. diagnostic=" + getBootstrapNativeLoadError();
                        Log.e(EmulatorDebug.LOG_TAG, errorMessage);
                        throw new BootstrapInstallException(errorMessage, resolveBootstrapErrorMessageResId());
                    }

                    boolean symlinksFound = false;
                    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                                symlinksFound = true;
                                BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                                String line;
                                while ((line = symlinksReader.readLine()) != null) {
                                    String[] parts = line.split("←");
                                    if (parts.length != 2)
                                        throw new BootstrapInstallException("Malformed symlink line: " + line, R.string.bootstrap_error_body_archive_invalid_or_empty);
                                    String oldPath = parts[0];
                                    File newPath;
                                    try {
                                        newPath = resolveWithinStaging(STAGING_PREFIX_FILE, parts[1]);
                                    } catch (RuntimeException e) {
                                        Log.e(EmulatorDebug.LOG_TAG, "Rejected symlink destination entry: " + line, e);
                                        throw e;
                                    }
                                    symlinks.add(Pair.create(oldPath, newPath.getAbsolutePath()));

                                    ensureDirectoryExists(newPath.getParentFile());
                                }
                            } else {
                                String zipEntryName = zipEntry.getName();
                                File targetFile;
                                try {
                                    targetFile = resolveWithinStaging(STAGING_PREFIX_FILE, zipEntryName);
                                } catch (RuntimeException e) {
                                    Log.e(EmulatorDebug.LOG_TAG, "Rejected zip entry: " + zipEntryName, e);
                                    throw new BootstrapInstallException("Invalid zip entry in bootstrap archive: " + zipEntryName, R.string.bootstrap_error_body_archive_invalid_or_empty);
                                }
                                boolean isDirectory = zipEntry.isDirectory();

                                ensureDirectoryExists(isDirectory ? targetFile : targetFile.getParentFile());

                                if (!isDirectory) {
                                    try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                        int readBytes;
                                        while ((readBytes = zipInput.read(buffer)) != -1)
                                            outStream.write(buffer, 0, readBytes);
                                    }
                                    if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("libexec") || zipEntryName.startsWith("lib/apt/methods")) {
                                        //noinspection OctalInteger
                                        Os.chmod(targetFile.getAbsolutePath(), 0700);
                                    }
                                }
                            }
                        }
                    }

                    if (!symlinksFound) {
                        final String errorMessage = "Bootstrap archive is invalid: SYMLINKS.txt entry was not found";
                        Log.e(EmulatorDebug.LOG_TAG, errorMessage);
                        throw new BootstrapInstallException(errorMessage, R.string.bootstrap_error_body_archive_invalid_or_empty);
                    }

                    if (symlinks.isEmpty())
                        throw new BootstrapInstallException("Bootstrap archive contains SYMLINKS.txt but no symlink definitions", R.string.bootstrap_error_body_archive_invalid_or_empty);
                    for (Pair<String, String> symlink : symlinks) {
                        Os.symlink(symlink.first, symlink.second);
                    }

                    if (!STAGING_PREFIX_FILE.renameTo(PREFIX_FILE)) {
                        throw new RuntimeException("Unable to rename staging folder");
                    }

                    activity.runOnUiThread(whenDone);
                } catch (final Exception e) {
                    Log.e(EmulatorDebug.LOG_TAG, "Bootstrap error", e);
                    final int messageResId = (e instanceof BootstrapInstallException) ? ((BootstrapInstallException) e).userMessageResId : R.string.bootstrap_error_body;
                    logBootstrapTelemetry("bootstrap_install_error_dialog", "setup_exception");
                    activity.runOnUiThread(() -> {
                        try {
                            new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(messageResId)
                                .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
                                    dialog.dismiss();
                                    activity.finish();
                                }).setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
                                    dialog.dismiss();
                                    TermuxInstaller.setupIfNeeded(activity, whenDone);
                                }).show();
                        } catch (WindowManager.BadTokenException e1) {
                            // Activity already dismissed - ignore.
                        }
                    });
                } finally {
                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                            // Activity already dismissed - ignore.
                        }
                    });
                }
            }
        }.start();
    }

    private static void ensureDirectoryExists(File directory) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new RuntimeException("Unable to create directory: " + directory.getAbsolutePath());
        }
    }

    static File resolveWithinStaging(File stagingRoot, String relativePath) {
        try {
            File path = new File(relativePath);
            if (path.isAbsolute()) {
                throw new RuntimeException("Absolute path is not allowed in bootstrap archive: " + relativePath);
            }

            File target = new File(stagingRoot, relativePath);
            String stagingRootCanonicalPath = stagingRoot.getCanonicalPath();
            String targetCanonicalPath = target.getCanonicalPath();
            String stagingRootWithSeparator = stagingRootCanonicalPath + File.separator;
            if (!targetCanonicalPath.equals(stagingRootCanonicalPath) && !targetCanonicalPath.startsWith(stagingRootWithSeparator)) {
                throw new RuntimeException("Path escapes bootstrap staging root: " + relativePath + " -> " + targetCanonicalPath);
            }
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve bootstrap path: " + relativePath, e);
        }
    }

    private static final String TERMUX_BOOTSTRAP_LIB = "termux-bootstrap";
    private static volatile boolean bootstrapNativeLoadAttempted = false;
    private static volatile boolean bootstrapNativeLoaded = false;
    private static volatile String bootstrapNativeLoadError = "";
    private static volatile BootstrapDiagnostic bootstrapDiagnostic = BootstrapDiagnostic.idle();

    private static final class BootstrapDiagnostic {
        final String stage;
        final String failureType;
        final String failureMessage;
        final String supportedAbis;
        final String targetLibrary;

        private BootstrapDiagnostic(String stage, String failureType, String failureMessage, String supportedAbis, String targetLibrary) {
            this.stage = stage;
            this.failureType = failureType;
            this.failureMessage = failureMessage;
            this.supportedAbis = supportedAbis;
            this.targetLibrary = targetLibrary;
        }

        static BootstrapDiagnostic idle() {
            return new BootstrapDiagnostic("none", "none", "", getSupportedAbisString(), TERMUX_BOOTSTRAP_LIB);
        }

        static BootstrapDiagnostic fromThrowable(String stage, Throwable t) {
            return new BootstrapDiagnostic(stage, t.getClass().getSimpleName(), String.valueOf(t.getMessage()), getSupportedAbisString(), TERMUX_BOOTSTRAP_LIB);
        }
    }

    private static String getSupportedAbisString() {
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis == null || abis.length == 0) return "unknown";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < abis.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(abis[i]);
        }
        return builder.toString();
    }

    private static void recordBootstrapFailure(String stage, Throwable t) {
        BootstrapDiagnostic diagnostic = BootstrapDiagnostic.fromThrowable(stage, t);
        bootstrapDiagnostic = diagnostic;
        bootstrapNativeLoadError = "stage=" + diagnostic.stage + ",type=" + diagnostic.failureType + ",lib=" + diagnostic.targetLibrary +
            ",abis=" + diagnostic.supportedAbis + ",message=" + diagnostic.failureMessage;
    }

    private static void logBootstrapTelemetry(String event, String reason) {
        BootstrapDiagnostic diagnostic = bootstrapDiagnostic;
        Log.i(EmulatorDebug.LOG_TAG,
            "bootstrap_telemetry event=" + event +
                " reason=" + reason +
                " stage=" + diagnostic.stage +
                " failure_type=" + diagnostic.failureType +
                " target_lib=" + diagnostic.targetLibrary +
                " supported_abis=" + diagnostic.supportedAbis +
                " linker_message=" + diagnostic.failureMessage);
    }

    private static int resolveBootstrapErrorMessageResId() {
        String stage = bootstrapDiagnostic.stage;
        if ("jni_load_library".equals(stage) || "jni_get_zip".equals(stage) || "jni_unavailable".equals(stage)) {
            return R.string.bootstrap_error_body_jni_unavailable;
        }
        return R.string.bootstrap_error_body_archive_invalid_or_empty;
    }

    public static byte[] loadZipBytes() {
        if (ensureBootstrapNativeLoaded()) {
            try {
                byte[] zipBytes = nativeGetZip();
                if (zipBytes == null || zipBytes.length == 0) {
                    bootstrapDiagnostic = new BootstrapDiagnostic("zip_validation", "EmptyBootstrapZip", "nativeGetZip() returned null or empty payload", getSupportedAbisString(), TERMUX_BOOTSTRAP_LIB);
                    bootstrapNativeLoadError = "stage=zip_validation,type=EmptyBootstrapZip,lib=" + TERMUX_BOOTSTRAP_LIB +
                        ",abis=" + getSupportedAbisString() + ",message=nativeGetZip() returned null or empty payload";
                }
                return zipBytes;
            } catch (Throwable t) {
                recordBootstrapFailure("jni_get_zip", t);
                Log.e(EmulatorDebug.LOG_TAG, "Failed to read bootstrap archive from JNI", t);
            }
        } else if ("none".equals(bootstrapDiagnostic.stage)) {
            bootstrapDiagnostic = new BootstrapDiagnostic("jni_unavailable", "NativeBootstrapUnavailable", "Bootstrap JNI is unavailable after load attempt", getSupportedAbisString(), TERMUX_BOOTSTRAP_LIB);
            bootstrapNativeLoadError = "stage=jni_unavailable,type=NativeBootstrapUnavailable,lib=" + TERMUX_BOOTSTRAP_LIB +
                ",abis=" + getSupportedAbisString() + ",message=Bootstrap JNI is unavailable after load attempt";
        }
        return new byte[0];
    }

    private static synchronized boolean ensureBootstrapNativeLoaded() {
        if (bootstrapNativeLoadAttempted) return bootstrapNativeLoaded;
        bootstrapNativeLoadAttempted = true;
        try {
            System.loadLibrary(TERMUX_BOOTSTRAP_LIB);
            bootstrapNativeLoaded = true;
            bootstrapDiagnostic = BootstrapDiagnostic.idle();
            bootstrapNativeLoadError = "";
        } catch (Throwable t) {
            bootstrapNativeLoaded = false;
            recordBootstrapFailure("jni_load_library", t);
            Log.e(EmulatorDebug.LOG_TAG, "Unable to load " + TERMUX_BOOTSTRAP_LIB + "; bootstrap JNI path disabled", t);
        }
        return bootstrapNativeLoaded;
    }

    public static String getBootstrapNativeLoadError() {
        return bootstrapNativeLoadError;
    }

    private static native byte[] nativeGetZip();

    /** Delete a folder and all its content or throw. Don't follow symlinks. */
    static void deleteFolder(File fileOrDirectory) throws IOException {
        if (fileOrDirectory.getCanonicalPath().equals(fileOrDirectory.getAbsolutePath()) && fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteFolder(child);
                }
            }
        }

        if (!fileOrDirectory.delete()) {
            throw new RuntimeException("Unable to delete " + (fileOrDirectory.isDirectory() ? "directory " : "file ") + fileOrDirectory.getAbsolutePath());
        }
    }

    static void setupStorageSymlinks(final Context context) {
        final String LOG_TAG = "termux-storage";
        new Thread() {
            public void run() {
                try {
                    File storageDir = new File(TermuxService.HOME_PATH, "storage");

                    if (storageDir.exists()) {
                        try {
                            deleteFolder(storageDir);
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Could not delete old $HOME/storage, " + e.getMessage());
                            return;
                        }
                    }

                    if (!storageDir.mkdirs()) {
                        Log.e(LOG_TAG, "Unable to mkdirs() for $HOME/storage");
                        return;
                    }

                    File sharedDir = Environment.getExternalStorageDirectory();
                    Os.symlink(sharedDir.getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());

                    linkAppScopedDirectory(context, storageDir, Environment.DIRECTORY_DOWNLOADS, "downloads");
                    linkAppScopedDirectory(context, storageDir, Environment.DIRECTORY_DCIM, "dcim");
                    linkAppScopedDirectory(context, storageDir, Environment.DIRECTORY_PICTURES, "pictures");
                    linkAppScopedDirectory(context, storageDir, Environment.DIRECTORY_MUSIC, "music");
                    linkAppScopedDirectory(context, storageDir, Environment.DIRECTORY_MOVIES, "movies");

                    final File[] dirs = context.getExternalFilesDirs(null);
                    if (dirs != null && dirs.length > 1) {
                        for (int i = 1; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "external-" + i;
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error setting up link", e);
                }
            }
        }.start();
    }

    private static void linkAppScopedDirectory(Context context, File storageDir, String type, String alias) throws IOException {
        File target = context.getExternalFilesDir(type);
        if (target == null) {
            Log.w("termux-storage", "External files dir unavailable for " + type + "; skipping link " + alias);
            return;
        }
        if (!target.exists() && !target.mkdirs()) {
            throw new IOException("Unable to create app-scoped external dir for " + type + ": " + target.getAbsolutePath());
        }
        Os.symlink(target.getAbsolutePath(), new File(storageDir, alias).getAbsolutePath());
    }

}
