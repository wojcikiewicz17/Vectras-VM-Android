package com.vectras.vm.core;

import org.junit.Assert;
import org.junit.Test;

public class VmCommandSafetyValidatorTest {

    @Test
    public void validateQemuCommand_validCommand_returnsOk() {
        VmCommandSafetyValidator.ValidationResult result =
                VmCommandSafetyValidator.validateQemuCommand("qemu-system-x86_64 -m 2048");

        Assert.assertTrue(result.safe);
        Assert.assertEquals(VmCommandSafetyValidator.Reason.OK, result.reason);
    }

    @Test
    public void validateQemuCommand_nonQemuCommand_rejected() {
        VmCommandSafetyValidator.ValidationResult result =
                VmCommandSafetyValidator.validateQemuCommand("bash -lc whoami");

        Assert.assertFalse(result.safe);
        Assert.assertEquals(VmCommandSafetyValidator.Reason.NOT_QEMU, result.reason);
    }

    @Test
    public void validateQemuCommand_withPipe_rejected() {
        VmCommandSafetyValidator.ValidationResult result =
                VmCommandSafetyValidator.validateQemuCommand("qemu-system-x86_64 | tee log");

        Assert.assertFalse(result.safe);
        Assert.assertEquals(VmCommandSafetyValidator.Reason.HAS_PIPE, result.reason);
    }
}
