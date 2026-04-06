package com.vectras.vm.setupwizard;

import com.vectras.vm.core.ProcessLaunch;
import com.vectras.vm.core.ProcessRuntimeOps;

final class SetupProcessIntegration {

    private SetupProcessIntegration() {
    }

    static String validateTarLaunchResult(ProcessLaunch.LaunchResult waitResult,
                                          String stderrSummary,
                                          String assetPath,
                                          String commandSummary) {
        return validateTarLaunchResult(
                waitResult.status,
                waitResult.exitCode,
                waitResult.diagnosis,
                stderrSummary,
                assetPath,
                commandSummary
        );
    }

    static String validateTarLaunchResult(ProcessLaunch.LaunchStatus status,
                                          int exitCode,
                                          String diagnosis,
                                          String stderrSummary,
                                          String assetPath,
                                          String commandSummary) {
        if (status == ProcessLaunch.LaunchStatus.TIMEOUT) {
            return SetupFeatureCore.formatErrorCode(
                    SetupFeatureCore.EXTRACTION_FAIL_PREFIX,
                    "PROCESS_TIMEOUT [" + ProcessRuntimeOps.ExecutionCategory.SETUP_EXTRACTION.name()
                            + "] asset=" + assetPath
                            + " cmd=" + commandSummary
                            + " detail=" + diagnosis
                            + (stderrSummary.isEmpty() ? "" : " stderr=" + stderrSummary)
            );
        }

        if (status != ProcessLaunch.LaunchStatus.SUCCESS) {
            return SetupFeatureCore.formatErrorCode(
                    SetupFeatureCore.EXTRACTION_FAIL_PREFIX,
                    "PROCESS_EXECUTION_ERROR [" + ProcessRuntimeOps.ExecutionCategory.SETUP_EXTRACTION.name()
                            + "] asset=" + assetPath
                            + " cmd=" + commandSummary
                            + " detail=" + diagnosis
                            + (stderrSummary.isEmpty() ? "" : " stderr=" + stderrSummary)
            );
        }

        if (exitCode != 0 || !stderrSummary.isEmpty()) {
            return SetupFeatureCore.formatErrorCode(
                    SetupFeatureCore.EXTRACTION_FAIL_PREFIX,
                    "PROCESS_NON_ZERO_OR_OUTPUT_VALIDATION_FAIL [" + ProcessRuntimeOps.ExecutionCategory.SETUP_EXTRACTION.name()
                            + "] asset=" + assetPath
                            + " cmd=" + commandSummary
                            + " exit=" + exitCode
                            + (stderrSummary.isEmpty() ? "" : " stderr=" + stderrSummary)
            );
        }

        return null;
    }
}
