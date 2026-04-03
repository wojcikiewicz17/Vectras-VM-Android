package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TermuxInstallerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @org.junit.After
    public void tearDown() {
        TermuxInstaller.resetNativeBootstrapProviderForTesting();
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
    public void loadZipBytes_returnsFallbackAndDetailedError_whenLoadLibraryFails() {
        TermuxInstaller.NativeBootstrapProvider provider = TermuxInstaller.createNativeBootstrapProviderForTesting(
            libName -> {
                throw new UnsatisfiedLinkError("dlopen failed for " + libName);
            },
            () -> {
                fail("JNI accessor should not be called when native library failed to load");
                return new byte[0];
            }
        );
        TermuxInstaller.setNativeBootstrapProviderForTesting(provider);

        byte[] result = TermuxInstaller.loadZipBytes();

        assertEquals(0, result.length);
        String diagnostic = TermuxInstaller.getBootstrapNativeLoadError();
        assertTrue(diagnostic.contains("lib=termux-bootstrap"));
        assertTrue(diagnostic.contains("abi="));
        assertTrue(diagnostic.contains("error=UnsatisfiedLinkError"));
    }

    @Test
    public void loadZipBytes_returnsBytesFromJni_whenNativeProviderIsAvailable() {
        byte[] expected = new byte[]{10, 20, 30, 40};
        TermuxInstaller.setNativeBootstrapProviderForTesting(new TermuxInstaller.NativeBootstrapProvider() {
            @Override
            public boolean ensureLoaded() {
                return true;
            }

            @Override
            public byte[] getZipBytes() {
                return expected;
            }

            @Override
            public String getLastError() {
                return "";
            }
        });

        byte[] result = TermuxInstaller.loadZipBytes();

        assertArrayEquals(expected, result);
    }

    @Test
    public void loadZipBytes_doesNotThrowGenericError_whenJniAccessorThrows() {
        TermuxInstaller.NativeBootstrapProvider provider = TermuxInstaller.createNativeBootstrapProviderForTesting(
            libName -> { },
            () -> {
                throw new IllegalStateException("bad jni payload");
            }
        );
        TermuxInstaller.setNativeBootstrapProviderForTesting(provider);

        byte[] result = TermuxInstaller.loadZipBytes();

        assertEquals(0, result.length);
        String diagnostic = TermuxInstaller.getBootstrapNativeLoadError();
        assertTrue(diagnostic.contains("lib=termux-bootstrap"));
        assertTrue(diagnostic.contains("abi="));
        assertTrue(diagnostic.contains("error=IllegalStateException"));
    }
}
