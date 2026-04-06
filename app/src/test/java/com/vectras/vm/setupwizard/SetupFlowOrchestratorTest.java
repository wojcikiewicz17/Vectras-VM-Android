package com.vectras.vm.setupwizard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SetupFlowOrchestratorTest {

    @Test
    public void shouldRunBootstrapExtraction_whenProotMissing() {
        assertTrue(SetupFlowOrchestrator.shouldRunBootstrapExtraction(false));
        assertFalse(SetupFlowOrchestrator.shouldRunBootstrapExtraction(true));
    }

    @Test
    public void shouldRunDistroExtraction_whenDistroMissing() {
        assertTrue(SetupFlowOrchestrator.shouldRunDistroExtraction(false));
        assertFalse(SetupFlowOrchestrator.shouldRunDistroExtraction(true));
    }

    @Test
    public void shouldAbortWhenBinDirExists_enforcesGate() {
        assertTrue(SetupFlowOrchestrator.shouldAbortWhenBinDirExists(true));
        assertFalse(SetupFlowOrchestrator.shouldAbortWhenBinDirExists(false));
    }
}
