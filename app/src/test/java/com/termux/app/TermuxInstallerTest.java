package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TermuxInstallerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
}
