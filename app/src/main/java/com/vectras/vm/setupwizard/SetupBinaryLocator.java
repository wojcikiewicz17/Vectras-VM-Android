package com.vectras.vm.setupwizard;

import android.content.Context;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Camada de integração com filesystem para localização de binários no rootfs/bootstrap.
 */
public final class SetupBinaryLocator {

    private SetupBinaryLocator() {
    }

    public static boolean hasBinary(Context context, String binary) {
        if (context == null || binary == null || binary.isEmpty()) {
            return false;
        }

        for (String directory : buildSearchDirectories(context)) {
            File candidate = new File(directory, binary);
            if (candidate.exists() && candidate.canExecute()) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> buildSearchDirectories(Context context) {
        LinkedHashSet<String> directories = new LinkedHashSet<>();
        File filesDir = context.getFilesDir();
        registerDirectory(directories, new File(filesDir, "distro/usr/local/bin"));
        registerDirectory(directories, new File(filesDir, "distro/usr/bin"));
        registerDirectory(directories, new File(filesDir, "usr/bin"));
        registerDirectory(directories, new File(filesDir, "bin"));

        String runtimePath = System.getenv("PATH");
        if (runtimePath != null && !runtimePath.trim().isEmpty()) {
            for (String entry : runtimePath.split(":")) {
                registerDirectory(directories, new File(entry));
            }
        }

        registerDirectory(directories, new File("/usr/bin"));
        registerDirectory(directories, new File("/usr/local/bin"));
        return directories;
    }

    private static void registerDirectory(Set<String> directories, File dir) {
        if (dir != null && dir.isDirectory()) {
            directories.add(dir.getAbsolutePath());
        }
    }
}
