package com.vectras.qemu.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.junit.Test;

import java.io.File;

public class FileUtilsOpenModeTest {

    @Test
    public void resolveContentOpenMode_shouldHonorBackendReadOnly() {
        assertEquals("r", FileUtils.resolveContentOpenMode("content://disk.qcow2", "r"));
    }

    @Test
    public void resolveContentOpenMode_shouldForceIsoToReadOnly() {
        assertEquals("r", FileUtils.resolveContentOpenMode("content://disk.iso", "rw"));
    }

    @Test
    public void resolveContentOpenMode_shouldHonorWritableBackendModesForNonIso() {
        assertEquals("w", FileUtils.resolveContentOpenMode("content://disk.qcow2", "w"));
        assertEquals("rw", FileUtils.resolveContentOpenMode("content://disk.qcow2", "rw"));
    }

    @Test
    public void resolveContentOpenMode_shouldFallbackSafelyWhenBackendModeIsNullOrEmpty() {
        assertEquals("rw", FileUtils.resolveContentOpenMode("content://disk.qcow2", null));
        assertEquals("rw", FileUtils.resolveContentOpenMode("content://disk.qcow2", ""));
    }

    @Test
    public void resolveParcelOpenMode_shouldForceIsoToReadOnly() {
        assertEquals(ParcelFileDescriptor.MODE_READ_ONLY,
                FileUtils.resolveParcelOpenMode("/storage/emulated/0/vm/disk.ISO", "rw"));
    }

    @Test
    public void resolveParcelOpenMode_shouldDefaultToReadWriteForWritableDisk() {
        assertEquals(ParcelFileDescriptor.MODE_READ_WRITE,
                FileUtils.resolveParcelOpenMode("/storage/emulated/0/vm/disk.qcow2", ""));
    }

    @Test
    public void fileValid_multipleContentUriValidations_shouldNotGrowVmFdMap() throws Exception {
        Context context = mock(Context.class);
        ContentResolver resolver = mock(ContentResolver.class);
        when(context.getContentResolver()).thenReturn(resolver);

        File tempFile = File.createTempFile("vectras-qemu-file-valid", ".img");
        when(resolver.openFileDescriptor(any(Uri.class), anyString()))
                .thenAnswer(invocation -> ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_WRITE));

        com.vectras.vm.utils.FileUtils.fds.clear();
        int initialSize = com.vectras.vm.utils.FileUtils.fds.size();

        for (int i = 0; i < 20; i++) {
            assertTrue(FileUtils.fileValid(context, "content://disk.qcow2", "rw"));
        }

        assertEquals(initialSize, com.vectras.vm.utils.FileUtils.fds.size());
        verify(resolver, times(20)).openFileDescriptor(Uri.parse("content://disk.qcow2"), "rw");
    }
}
