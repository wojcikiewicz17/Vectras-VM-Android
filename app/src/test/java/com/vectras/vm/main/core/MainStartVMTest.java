package com.vectras.vm.main.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MainStartVMTest {

    @Test
    public void resolveRunCommandFormat_consecutiveX11Calls_doNotLeakWrapperState() {
        String earlyReturnFormat = MainStartVM.resolveRunCommandFormat(true, false, true);
        String normalLaunchFormat = MainStartVM.resolveRunCommandFormat(true, false, false);

        assertEquals(
                String.format(MainStartVM.BASE_RUN_COMMAND_FORMAT, "xterm -e bash -c \"%s\""),
                earlyReturnFormat
        );
        assertEquals(
                String.format(MainStartVM.BASE_RUN_COMMAND_FORMAT, "bash -c \"%s\""),
                normalLaunchFormat
        );
    }

    @Test
    public void ensureLastVmIdInitialized_firstLaunchWithoutSelectedVm_generatesTransientId() {
        MainStartVM.lastVMID = "";

        String resolvedVmId = MainStartVM.ensureLastVmIdInitialized(null);

        assertEquals(resolvedVmId, MainStartVM.lastVMID);
        assertTrue(resolvedVmId.startsWith("launch-"));
    }
}
