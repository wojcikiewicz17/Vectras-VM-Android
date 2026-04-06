package com.vectras.vm.setupwizard;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class SetupPreflightRulesTest {

    @Test
    public void parsePackageTokens_splitsByWhitespace() {
        ArrayList<String> tokens = SetupPreflightRules.parsePackageTokens(" qemu-system-x86_64   xterm\nbusybox ");

        Assert.assertEquals(3, tokens.size());
        Assert.assertEquals("qemu-system-x86_64", tokens.get(0));
        Assert.assertEquals("xterm", tokens.get(1));
        Assert.assertEquals("busybox", tokens.get(2));
    }

    @Test
    public void isPkgInstalled_matchesOnlyPRecords() {
        String pkgDb = "C:Q1\nP:qemu-system-x86_64\nV:1.0\nP:xterm\n";

        Assert.assertTrue(SetupPreflightRules.isPkgInstalled(pkgDb, "xterm"));
        Assert.assertFalse(SetupPreflightRules.isPkgInstalled(pkgDb, "missing-pkg"));
    }
}
