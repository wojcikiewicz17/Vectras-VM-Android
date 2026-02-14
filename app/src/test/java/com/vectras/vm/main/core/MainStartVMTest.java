package com.vectras.vm.main.core;

import static org.junit.Assert.assertEquals;

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
}
