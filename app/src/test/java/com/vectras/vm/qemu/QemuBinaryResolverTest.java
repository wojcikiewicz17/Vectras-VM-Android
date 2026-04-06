package com.vectras.vm.qemu;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QemuBinaryResolverTest {

    @Test
    public void resolveForArch_prefersX8664Binary() {
        String filesDir = "/data/user/0/com.vectras.vm/files";
        Set<String> existing = new HashSet<>();
        existing.add(filesDir + "/distro/usr/local/bin/qemu-system-x86_64");

        QemuBinaryResolver.Resolution resolution = QemuBinaryResolver.resolveForArch(
                filesDir,
                "X86_64",
                existing,
                "QemuBinaryResolverTest"
        );

        assertTrue(resolution.found);
        assertEquals("qemu-system-x86_64", resolution.binaryName);
        assertEquals(filesDir + "/distro/usr/local/bin/qemu-system-x86_64", resolution.fullPath);
    }

    @Test
    public void resolveForArch_findsAarch64Binary() {
        String filesDir = "/data/user/0/com.vectras.vm/files";
        Set<String> existing = new HashSet<>();
        existing.add(filesDir + "/distro/usr/bin/qemu-system-aarch64");

        QemuBinaryResolver.Resolution resolution = QemuBinaryResolver.resolveForArch(
                filesDir,
                "ARM64",
                existing,
                "QemuBinaryResolverTest"
        );

        assertTrue(resolution.found);
        assertEquals("qemu-system-aarch64", resolution.binaryName);
    }

    @Test
    public void resolveForArch_reportsNotFoundWhenNoBinaryExists() {
        QemuBinaryResolver.Resolution resolution = QemuBinaryResolver.resolveForArch(
                "/data/user/0/com.vectras.vm/files",
                "X86_64",
                Collections.emptySet(),
                "QemuBinaryResolverTest"
        );

        assertFalse(resolution.found);
        assertEquals("binary-not-found", resolution.reason);
        assertFalse(resolution.checkedPaths.isEmpty());
    }
}
