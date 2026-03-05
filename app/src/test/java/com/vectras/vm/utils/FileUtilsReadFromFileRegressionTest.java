package com.vectras.vm.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileUtilsReadFromFileRegressionTest {

    @Test
    public void readFromFile_shouldReturnAllBytesForLargeFile() throws Exception {
        File tempFile = File.createTempFile("vectras-read", ".txt");
        StringBuilder expectedBuilder = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            expectedBuilder.append("line-").append(i).append('-').append("abcdef");
        }
        String expected = expectedBuilder.toString();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(expected.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(expected, FileUtils.readFromFile(null, tempFile));
    }

    @Test
    public void readFromInputStream_shouldReadFragmentedStreamUntilEof() throws Exception {
        String expected = "fragmented stream content with multiple chunks 1234567890";
        byte[] source = expected.getBytes(StandardCharsets.UTF_8);
        InputStream fragmented = new FragmentedInputStream(new ByteArrayInputStream(source), 3);

        assertEquals(expected, FileUtils.readFromInputStream(fragmented));
    }

    private static final class FragmentedInputStream extends FilterInputStream {
        private final int maxChunkSize;

        private FragmentedInputStream(InputStream in, int maxChunkSize) {
            super(in);
            this.maxChunkSize = maxChunkSize;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, Math.min(len, maxChunkSize));
        }
    }
}
