package com.vectras.vm.setupwizard;

final class SetupUiState {

    private SetupUiState() {
    }

    static boolean shouldShowAbiWarning(boolean is64bitDevice) {
        return !is64bitDevice;
    }
}
