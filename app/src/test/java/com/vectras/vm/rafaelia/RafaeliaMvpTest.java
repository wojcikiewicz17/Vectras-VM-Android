package com.vectras.vm.rafaelia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import org.junit.Test;

public class RafaeliaMvpTest {

  @Test
  public void parityForSingleBitSetsRowAndColumn() {
    int parity = RafaeliaMvp.parity2D8(1);
    assertEquals(0x11, parity);
  }

  @Test
  public void bitStackAppendsPayloadAndCrc() throws Exception {
    File tmp = File.createTempFile("rafaelia", ".bin");
    tmp.deleteOnExit();

    long payload = 0x1122334455667788L;
    int meta = 0xAABBCCDD;

    try (RafaeliaMvp.BitStack bs = new RafaeliaMvp.BitStack(tmp, 1024 * 1024)) {
      bs.appendRecord(payload, meta);
      bs.flush();
    }

    try (RandomAccessFile raf = new RandomAccessFile(tmp, "r")) {
      long payloadRead = raf.readLong();
      int metaRead = raf.readInt();
      int crcRead = raf.readInt();

      assertEquals(payload, payloadRead);
      assertEquals(meta, metaRead);
      assertEquals(RafaeliaMvp.BitStack.crc32c(payload, meta), crcRead);
    } finally {
      Files.deleteIfExists(tmp.toPath());
    }
  }

  @Test
  public void deterministicRngSameSeedProducesSameSequence() {
    RafaeliaMvp.DeterministicRng rngA = new RafaeliaMvp.SplittableDeterministicRng(0x1234ABCDL);
    RafaeliaMvp.DeterministicRng rngB = new RafaeliaMvp.SplittableDeterministicRng(0x1234ABCDL);

    for (int i = 0; i < 64; i++) {
      assertEquals(rngA.nextInt(1024), rngB.nextInt(1024));
      assertEquals(rngA.nextDouble(), rngB.nextDouble(), 0.0);
    }
  }

  @Test
  public void deterministicRngDifferentSeedProducesDifferentSequence() {
    RafaeliaMvp.DeterministicRng rngA = new RafaeliaMvp.SplittableDeterministicRng(1L);
    RafaeliaMvp.DeterministicRng rngB = new RafaeliaMvp.SplittableDeterministicRng(2L);

    int a = rngA.nextInt(1 << 16);
    int b = rngB.nextInt(1 << 16);

    assertNotEquals(a, b);
  }
}
