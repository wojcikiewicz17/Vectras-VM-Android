package com.vectras.vm.setupwizard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Method;

public class SetupFeatureCorePkgInstalledJavaTest {

    @Test
    public void isPkgInstalled_nullAndEmptyInputs_returnFalse() throws Exception {
        assertFalse(invokeIsPkgInstalled(null, "bash"));
        assertFalse(invokeIsPkgInstalled("", "bash"));
        assertFalse(invokeIsPkgInstalled("P:bash\n", null));
        assertFalse(invokeIsPkgInstalled("P:bash\n", ""));
    }

    @Test
    public void isPkgInstalled_acceptsMixedLineBreaks() throws Exception {
        String db = "P:busybox\r\nA:meta\nP:bash\rP:curl\n";
        assertTrue(invokeIsPkgInstalled(db, "busybox"));
        assertTrue(invokeIsPkgInstalled(db, "bash"));
        assertTrue(invokeIsPkgInstalled(db, "curl"));
    }

    @Test
    public void isPkgInstalled_partialNameDoesNotMatch() throws Exception {
        String db = "P:libssl3\nP:openssl\n";
        assertFalse(invokeIsPkgInstalled(db, "ssl"));
        assertFalse(invokeIsPkgInstalled(db, "open"));
        assertTrue(invokeIsPkgInstalled(db, "openssl"));
    }

    private static boolean invokeIsPkgInstalled(String pkgDb, String pkgName) throws Exception {
        Method method = SetupFeatureCore.class.getDeclaredMethod("isPkgInstalled", String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, pkgDb, pkgName);
    }
}
