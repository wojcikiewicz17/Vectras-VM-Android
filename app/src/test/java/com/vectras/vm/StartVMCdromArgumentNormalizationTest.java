package com.vectras.vm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StartVMCdromArgumentNormalizationTest {

    @Test
    public void normalizeCdromArgumentStyle_driveWithReadonlyAndFdPath_convertsToCdromWhenIfTypeEmpty() {
        String extras = "-machine pc -drive index=1,media=cdrom,readonly=on,file='/proc/self/fd/42' -boot d";

        String normalized = StartVM.normalizeCdromArgumentStyle(extras, "");

        assertEquals("-machine pc -cdrom '/proc/self/fd/42' -boot d", normalized);
    }

    @Test
    public void normalizeCdromArgumentStyle_driveWithoutReadonly_convertsToCdromWhenIfTypeEmpty() {
        String extras = "-accel tcg -drive media=cdrom,file='/storage/emulated/0/Download/os.iso',index=1 -m 2048";

        String normalized = StartVM.normalizeCdromArgumentStyle(extras, "");

        assertEquals("-accel tcg -cdrom '/storage/emulated/0/Download/os.iso' -m 2048", normalized);
    }

    @Test
    public void normalizeCdromArgumentStyle_cdrom_convertsToDriveWithReadonlyWhenIfTypePresent() {
        String extras = "-machine pc -cdrom '/proc/self/fd/77' -boot d";

        String normalized = StartVM.normalizeCdromArgumentStyle(extras, "virtio");

        assertEquals(
                "-machine pc -drive index=1,media=cdrom,readonly=on,file='/proc/self/fd/77' -boot d",
                normalized
        );
    }

    @Test
    public void normalizeCdromArgumentStyle_driveWithKeyOrderVariation_keepsDriveAndForcesReadonlyWhenIfTypePresent() {
        String extras = "-smp 2 -drive index=1,readonly=off,file='/storage/emulated/0/os.iso',media=cdrom -m 4096";

        String normalized = StartVM.normalizeCdromArgumentStyle(extras, "ide");

        assertEquals(
                "-smp 2 -drive index=1,media=cdrom,readonly=on,file='/storage/emulated/0/os.iso' -m 4096",
                normalized
        );
    }
}
