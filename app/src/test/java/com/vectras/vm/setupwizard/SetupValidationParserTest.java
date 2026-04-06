package com.vectras.vm.setupwizard;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

public class SetupValidationParserTest {

    @Test
    public void validateExtractedTarFile_rejectsSmallOrInvalidTar() throws Exception {
        File cacheDir = Files.createTempDirectory("setup-validation-test").toFile();
        File tinyTar = new File(cacheDir, "tiny-test.tar");
        try (FileOutputStream out = new FileOutputStream(tinyTar)) {
            out.write(new byte[64]);
        }
        String error = SetupValidationParser.validateExtractedTarFile(tinyTar.toPath(), 1024L);
        assertTrue(error.contains("below-min-size"));
    }

    @Test
    public void validateExtractedTarFile_acceptsReasonableTarSize() throws Exception {
        File cacheDir = Files.createTempDirectory("setup-validation-test-ok").toFile();
        File fullTar = new File(cacheDir, "ok-test.tar");
        try (FileOutputStream out = new FileOutputStream(fullTar)) {
            out.write(new byte[2048]);
        }
        assertNull(SetupValidationParser.validateExtractedTarFile(fullTar.toPath(), 1024L));
    }
}
