package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TermuxInstallerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        TermuxInstaller.resetBootstrapNativeStateForTests();
    }

    @After
    public void tearDown() {
        TermuxInstaller.resetBootstrapNativeStateForTests();
    }

    @Test
    public void resolveWithinStaging_allowsNormalRelativeEntry() throws Exception {
        File stagingRoot = temporaryFolder.newFolder("staging");

        File resolved = TermuxInstaller.resolveWithinStaging(stagingRoot, "bin/bash");

        assertTrue(resolved.getCanonicalPath().startsWith(stagingRoot.getCanonicalPath() + File.separator));
        assertEquals(new File(stagingRoot, "bin/bash").getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    public void resolveWithinStaging_rejectsParentTraversalEntry() throws Exception {
        File stagingRoot = temporaryFolder.newFolder("staging");

        try {
            TermuxInstaller.resolveWithinStaging(stagingRoot, "../outside");
            fail("Expected RuntimeException for parent traversal");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Path escapes bootstrap staging root"));
            assertTrue(e.getMessage().contains("../outside"));
        }
    }

    @Test
    public void resolveWithinStaging_rejectsAbsoluteEntry() throws Exception {
        File stagingRoot = temporaryFolder.newFolder("staging");

        String absolutePath = new File(File.separator + "tmp", "escape").getAbsolutePath();
        try {
            TermuxInstaller.resolveWithinStaging(stagingRoot, absolutePath);
            fail("Expected RuntimeException for absolute path");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Absolute path is not allowed"));
            assertTrue(e.getMessage().contains(absolutePath));
        }
    }

    @Test
    public void resolveWithinStaging_rejectsSymlinkDestinationEscapingStaging() throws Exception {
        File stagingRoot = temporaryFolder.newFolder("staging");

        try {
            TermuxInstaller.resolveWithinStaging(stagingRoot, "lib/../../etc/passwd");
            fail("Expected RuntimeException for escaping symlink destination");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Path escapes bootstrap staging root"));
            assertTrue(e.getMessage().contains("lib/../../etc/passwd"));
        }
    }

    @Test
    public void loadZipBytes_whenSystemLoadLibraryFails_returnsFallbackAndDetailedDiagnostics() {
        byte[] zipBytes = null;
        try {
            zipBytes = TermuxInstaller.loadZipBytes();
        } catch (Throwable throwable) {
            fail("loadZipBytes() should not throw generic exception when JNI library is unavailable: " + throwable);
        }

        assertEquals(0, zipBytes.length);

        String diagnostics = TermuxInstaller.getBootstrapNativeLoadError();
        assertTrue(diagnostics.contains("lib=termux-bootstrap"));
        assertTrue(diagnostics.contains("abi="));
        assertTrue(diagnostics.contains("error="));
        assertFalse("Error diagnostics should not be empty", diagnostics.endsWith("error="));
    }

    @Test
    public void loadZipBytes_whenNativeProviderInjected_usesInjectedJniReturnValue() {
        byte[] expected = "zip-payload-from-jni".getBytes(StandardCharsets.UTF_8);
        AtomicBoolean ensureCalled = new AtomicBoolean(false);
        AtomicBoolean getZipCalled = new AtomicBoolean(false);

        TermuxInstaller.setNativeBootstrapProviderForTests(new TermuxInstaller.NativeBootstrapProvider() {
            @Override
            public boolean ensureNativeLoaded() {
                ensureCalled.set(true);
                return true;
            }

            @Override
            public byte[] getZipBytes() {
                getZipCalled.set(true);
                return expected;
            }

            @Override
            public String getDiagnostics() {
                return "lib=termux-bootstrap, abi=test-abi, error=none";
            }
        });

        byte[] actual = TermuxInstaller.loadZipBytes();
        assertTrue("ensureNativeLoaded() should be called", ensureCalled.get());
        assertTrue("getZipBytes() should be called", getZipCalled.get());
        assertEquals("zip-payload-from-jni", new String(actual, StandardCharsets.UTF_8));
    }

    @Test
    public void loadZipBytes_whenProviderUnavailable_reportsProviderDiagnosticsWithoutGenericThrow() {
        TermuxInstaller.setNativeBootstrapProviderForTests(new TermuxInstaller.NativeBootstrapProvider() {
            @Override
            public boolean ensureNativeLoaded() {
                return false;
            }

            @Override
            public byte[] getZipBytes() {
                return new byte[0];
            }

            @Override
            public String getDiagnostics() {
                return "lib=termux-bootstrap, abi=arm64-v8a, error=UnsatisfiedLinkError: test failure";
            }
        });

        try {
            byte[] actual = TermuxInstaller.loadZipBytes();
            assertEquals(0, actual.length);
        } catch (Throwable throwable) {
            fail("loadZipBytes() should not throw generic exception when provider marks JNI unavailable: " + throwable);
        }

        String diagnostics = TermuxInstaller.getBootstrapNativeLoadError();
        assertTrue(diagnostics.contains("lib=termux-bootstrap"));
        assertTrue(diagnostics.contains("abi="));
        assertTrue(diagnostics.contains("UnsatisfiedLinkError"));
    }
}
