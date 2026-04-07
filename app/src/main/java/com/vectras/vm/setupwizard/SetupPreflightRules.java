package com.vectras.vm.setupwizard;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Regras de parsing/validação de preflight sem dependência de Android UI.
 */
public final class SetupPreflightRules {

    private SetupPreflightRules() {
    }

    public static ArrayList<String> parsePackageTokens(String rawPkgs) {
        if (rawPkgs == null || rawPkgs.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(rawPkgs.trim().split("\\s+")));
    }

    public static boolean isPkgInstalled(String pkgDb, String pkgName) {
        if (pkgDb == null || pkgName == null) {
            return false;
        }

        String normalizedPkgName = pkgName.trim();
        if (pkgDb.trim().isEmpty() || normalizedPkgName.isEmpty()) {
            return false;
        }

        String[] lines = pkgDb.split("\\R");
        for (String line : lines) {
            if (!line.startsWith("P:")) {
                continue;
            }

            String installedPkg = line.substring(2);
            if (installedPkg.equals(normalizedPkgName)) {
                return true;
            }
        }

        return false;
    }
}
