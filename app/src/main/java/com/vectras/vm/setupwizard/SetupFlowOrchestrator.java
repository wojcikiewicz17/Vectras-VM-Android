package com.vectras.vm.setupwizard;

final class SetupFlowOrchestrator {

    private SetupFlowOrchestrator() {
    }

    static boolean shouldRunBootstrapExtraction(boolean prootInstalled) {
        return !prootInstalled;
    }

    static boolean shouldRunDistroExtraction(boolean distroInstalled) {
        return !distroInstalled;
    }

    static boolean shouldAbortWhenBinDirExists(boolean binDirExists) {
        return binDirExists;
    }
}
