package com.vectras.vm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VmImageCommandRulesTest {

    @Test
    public void isRawImageSizeTokenSafe_appliesThresholds() {
        assertFalse(VmImageCommandRules.isRawImageSizeTokenSafe("12g"));
        assertTrue(VmImageCommandRules.isRawImageSizeTokenSafe("9g"));
        assertFalse(VmImageCommandRules.isRawImageSizeTokenSafe("10000m"));
        assertTrue(VmImageCommandRules.isRawImageSizeTokenSafe("999m"));
    }

    @Test
    public void isRawImageSizeTokenSafe_rejectsHugeSuffixes() {
        assertFalse(VmImageCommandRules.isRawImageSizeTokenSafe("1t"));
        assertFalse(VmImageCommandRules.isRawImageSizeTokenSafe("1p"));
        assertFalse(VmImageCommandRules.isRawImageSizeTokenSafe("1e"));
    }
}
