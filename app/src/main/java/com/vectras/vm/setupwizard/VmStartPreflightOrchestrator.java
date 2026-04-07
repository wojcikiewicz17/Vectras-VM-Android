package com.vectras.vm.setupwizard;

import android.content.Context;

import com.vectras.vm.AppConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Orquestra o fluxo de preflight de inicialização de VM.
 */
public final class VmStartPreflightOrchestrator {

    private VmStartPreflightOrchestrator() {
    }

    public static SetupFeatureCore.PreflightResult run(
            Context context,
            String requiredQemuBinary,
            SetupFeatureCore.VmStartPreflightOptions options,
            SetupFeatureCore.TextFileReader fileReader
    ) {
        ArrayList<String> missingBinaries = new ArrayList<>();
        ArrayList<String> missingOptionalModeBinaries = new ArrayList<>();
        ArrayList<String> missingPackages = new ArrayList<>();

        SetupFeatureCore.VmStartPreflightOptions resolvedOptions = options == null
                ? new SetupFeatureCore.VmStartPreflightOptions("", false, false)
                : options;

        if (!SetupBinaryLocator.hasBinary(context, requiredQemuBinary)) {
            missingBinaries.add(requiredQemuBinary);
        }
        if (resolvedOptions.shouldRequireXterm()) {
            if (!SetupBinaryLocator.hasBinary(context, "xterm")) {
                missingBinaries.add("xterm");
            }
        } else if (!SetupBinaryLocator.hasBinary(context, "xterm")) {
            missingOptionalModeBinaries.add("xterm");
        }

        String pkgDbPath = context.getFilesDir().getAbsolutePath() + "/distro/lib/apk/db/installed";
        String pkgDb = fileReader.read(pkgDbPath);
        if (pkgDb.isEmpty()) {
            missingPackages.add("apk-db-unavailable");
        } else {
            Set<String> expectedPkgs = new LinkedHashSet<>();
            expectedPkgs.addAll(SetupPreflightRules.parsePackageTokens(AppConfig.neededPkgs()));
            expectedPkgs.addAll(SetupPreflightRules.parsePackageTokens(AppConfig.neededPkgs32bit()));

            for (String pkg : expectedPkgs) {
                if (!SetupPreflightRules.isPkgInstalled(pkgDb, pkg)) {
                    missingPackages.add(pkg);
                }
            }
        }

        boolean ok = missingBinaries.isEmpty() && missingPackages.isEmpty();
        return new SetupFeatureCore.PreflightResult(ok, missingBinaries, missingOptionalModeBinaries, missingPackages);
    }
}
