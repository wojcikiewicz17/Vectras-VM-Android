package com.vectras.vm.setupwizard;

import java.io.File;
import java.nio.file.Path;

final class SetupValidationParser {

    private SetupValidationParser() {
    }

    static String validateExtractedTarFile(Path extractedTarPath, long minTarBytes) {
        File extractedTarFile = extractedTarPath.toFile();
        if (!extractedTarPath.toString().endsWith(".tar")) {
            return "TAR_INTEGRITY_FAIL: invalid-extension"
                    + " path=" + extractedTarPath;
        }
        if (!extractedTarFile.exists()) {
            return "TAR_INTEGRITY_FAIL: missing-file"
                    + " path=" + extractedTarPath;
        }
        if (!extractedTarFile.isFile()) {
            return "TAR_INTEGRITY_FAIL: not-regular-file"
                    + " path=" + extractedTarPath;
        }

        long tarLength = extractedTarFile.length();
        if (tarLength <= 0L) {
            return "TAR_INTEGRITY_FAIL: empty-file"
                    + " path=" + extractedTarPath
                    + " length=" + tarLength;
        }
        if (tarLength < minTarBytes) {
            return "TAR_INTEGRITY_FAIL: below-min-size"
                    + " path=" + extractedTarPath
                    + " length=" + tarLength
                    + " min=" + minTarBytes;
        }
        return null;
    }
}
