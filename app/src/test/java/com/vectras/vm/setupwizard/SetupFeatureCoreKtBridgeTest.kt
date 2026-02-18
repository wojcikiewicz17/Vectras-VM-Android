package com.vectras.vm.setupwizard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupFeatureCoreKtBridgeTest {

    @Test
    fun isPkgInstalled_nullAndEmptyInputs_returnFalse() {
        assertFalse(SetupFeatureCoreKtBridge.isPkgInstalled(null, "bash"))
        assertFalse(SetupFeatureCoreKtBridge.isPkgInstalled("", "bash"))
        assertFalse(SetupFeatureCoreKtBridge.isPkgInstalled("P:bash\n", null))
        assertFalse(SetupFeatureCoreKtBridge.isPkgInstalled("P:bash\n", ""))
    }

    @Test
    fun isPkgInstalled_acceptsMixedLineBreaks() {
        val db = "P:busybox\r\nA:meta\nP:bash\rP:curl\n"
        assertTrue(SetupFeatureCoreKtBridge.isPkgInstalled(db, "busybox"))
        assertTrue(SetupFeatureCoreKtBridge.isPkgInstalled(db, "bash"))
        assertTrue(SetupFeatureCoreKtBridge.isPkgInstalled(db, "curl"))
    }

    @Test
    fun isPkgInstalled_partialNameDoesNotMatch() {
        val db = "P:libssl3\nP:openssl\n"
        assertFalse(SetupFeatureCoreKtBridge.isPkgInstalled(db, "ssl"))
        assertFalse(SetupFeatureCoreKtBridge.isPkgInstalled(db, "open"))
        assertTrue(SetupFeatureCoreKtBridge.isPkgInstalled(db, "openssl"))
    }
}
