package com.vectras.vm.setupwizard;

import android.content.Context;

import com.vectras.vm.utils.FileUtils;

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
        String filesDir = context.getFilesDir().getAbsolutePath();
        String[] binSearchPaths = new String[]{
                filesDir + "/distro/usr/local/bin/" + binary,
                filesDir + "/distro/usr/bin/" + binary,
                filesDir + "/usr/bin/" + binary
        };
        for (String binPath : binSearchPaths) {
            if (FileUtils.isFileExists(binPath)) {
                return true;
            }
        }
        return false;
    }
}
