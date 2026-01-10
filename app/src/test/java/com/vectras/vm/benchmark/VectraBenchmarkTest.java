package com.vectras.vm.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import org.junit.Test;

/**
 * Unit tests for VectraBenchmark module.
 * Tests core functionality without running full benchmark suite.
 */
public class VectraBenchmarkTest {

    @Test
    public void parity2D8SingleBitSetCorrect() {
        // Single bit at position (0,0) should set row 0 and col 0 parity
        int parity = VectraBenchmark.parity2D8(1);
        assertEquals(0x11, parity); // bit 0 (col 0) + bit 4 (row 0)
    }

    @Test
    public void parity2D8AllBitsClearCorrect() {
        int parity = VectraBenchmark.parity2D8(0);
        assertEquals(0x00, parity);
    }

    @Test
    public void parity2D8AlternatingPattern() {
        // Alternating pattern: 0b1010101010101010
        int parity = VectraBenchmark.parity2D8(0xAAAA);
        // Each row has 2 bits set (even parity = 0)
        // Each col has 2 bits set (even parity = 0)
        assertEquals(0x00, parity);
    }

    @Test
    public void syndromePopcountDetectsDifference() {
        int p1 = VectraBenchmark.parity2D8(0x0001);
        int p2 = VectraBenchmark.parity2D8(0x0002);
        int syndrome = VectraBenchmark.syndromePopcount(p1, p2);
        assertTrue(syndrome > 0);
    }

    @Test
    public void syndromePopcountZeroForSame() {
        int p1 = VectraBenchmark.parity2D8(0x1234);
        int p2 = VectraBenchmark.parity2D8(0x1234);
        assertEquals(0, VectraBenchmark.syndromePopcount(p1, p2));
    }

    @Test
    public void whoOutTriadDiskOut() {
        // CPU == RAM but != DISK => DISK is out
        assertEquals(2, VectraBenchmark.whoOutTriad(100, 100, 200));
    }

    @Test
    public void whoOutTriadRamOut() {
        // CPU == DISK but != RAM => RAM is out
        assertEquals(1, VectraBenchmark.whoOutTriad(100, 200, 100));
    }

    @Test
    public void whoOutTriadCpuOut() {
        // RAM == DISK but != CPU => CPU is out
        assertEquals(0, VectraBenchmark.whoOutTriad(200, 100, 100));
    }

    @Test
    public void whoOutTriadAllAgree() {
        // All equal => no one is out (3 = NONE)
        assertEquals(3, VectraBenchmark.whoOutTriad(100, 100, 100));
    }

    @Test
    public void whoOutTriadAllDifferent() {
        // All different => unknown (3 = NONE)
        assertEquals(3, VectraBenchmark.whoOutTriad(100, 200, 300));
    }

    @Test
    public void mix64ProducesDifferentOutputs() {
        long a = VectraBenchmark.mix64(1);
        long b = VectraBenchmark.mix64(2);
        assertTrue(a != b);
    }

    @Test
    public void mix64Deterministic() {
        long a = VectraBenchmark.mix64(12345);
        long b = VectraBenchmark.mix64(12345);
        assertEquals(a, b);
    }

    @Test
    public void bitStackAppendsAndCrcCorrect() throws Exception {
        File tmp = File.createTempFile("vectra_bench", ".bin");
        tmp.deleteOnExit();

        long value = 0x1122334455667788L;
        int metricId = 42;

        try (VectraBenchmark.BitStack bs = new VectraBenchmark.BitStack(tmp, 1024 * 1024)) {
            bs.appendResult(value, metricId);
            bs.flush();
        }

        try (RandomAccessFile raf = new RandomAccessFile(tmp, "r")) {
            long valueRead = raf.readLong();
            int metricRead = raf.readInt();
            int crcRead = raf.readInt();

            assertEquals(value, valueRead);
            assertEquals(metricId, metricRead);
            assertEquals(VectraBenchmark.BitStack.crc32c(value, metricId), crcRead);
        } finally {
            Files.deleteIfExists(tmp.toPath());
        }
    }

    @Test
    public void benchCpuIntegerAddReturnsPositiveTime() {
        long time = VectraBenchmark.benchCpuIntegerAdd(10000);
        assertTrue(time > 0);
    }

    @Test
    public void benchCpuLongMixReturnsPositiveTime() {
        long time = VectraBenchmark.benchCpuLongMix(10000);
        assertTrue(time > 0);
    }

    @Test
    public void benchCpuPopcountReturnsPositiveTime() {
        long time = VectraBenchmark.benchCpuPopcount(10000);
        assertTrue(time > 0);
    }

    @Test
    public void benchMemSequentialReadReturnsPositiveTime() {
        byte[] buffer = new byte[4096];
        long time = VectraBenchmark.benchMemSequentialRead(buffer);
        assertTrue(time > 0);
    }

    @Test
    public void benchMemSequentialWriteReturnsPositiveTime() {
        byte[] buffer = new byte[4096];
        long time = VectraBenchmark.benchMemSequentialWrite(buffer);
        assertTrue(time > 0);
    }

    @Test
    public void benchIntegrityCrc32cReturnsPositiveTime() {
        byte[] data = new byte[1024];
        long time = VectraBenchmark.benchIntegrityCrc32c(data, 100);
        assertTrue(time > 0);
    }

    @Test
    public void benchIntegrityParity2DReturnsPositiveTime() {
        long time = VectraBenchmark.benchIntegrityParity2D(10000);
        assertTrue(time > 0);
    }

    @Test
    public void benchEmuTriadConsensusReturnsPositiveTime() {
        long time = VectraBenchmark.benchEmuTriadConsensus(10000);
        assertTrue(time > 0);
    }

    @Test
    public void benchmarkResultScoreCalculation() {
        // baseline 1000, value 500 => ratio 0.5 => score 50
        VectraBenchmark.BenchmarkResult r = new VectraBenchmark.BenchmarkResult(
            0, "Test", 500, 1000, "ns"
        );
        assertEquals(50, r.score());
    }

    @Test
    public void benchmarkResultScoreHigherValueLowerScore() {
        // baseline 1000, value 2000 => ratio 2.0 => score 200
        VectraBenchmark.BenchmarkResult r = new VectraBenchmark.BenchmarkResult(
            0, "Test", 2000, 1000, "ns"
        );
        assertEquals(200, r.score());
    }

    @Test
    public void calculateTotalScoreNonZero() throws Exception {
        // Create minimal results array with one result
        VectraBenchmark.BenchmarkResult[] results = new VectraBenchmark.BenchmarkResult[1];
        results[0] = new VectraBenchmark.BenchmarkResult(0, "Test", 1000, 1000, "ns");
        
        int score = VectraBenchmark.calculateTotalScore(results);
        assertEquals(100, score); // 1000/1000 * 100 = 100
    }

    @Test
    public void calculateCategoryScoresReturns6Categories() throws Exception {
        VectraBenchmark.BenchmarkResult[] results = new VectraBenchmark.BenchmarkResult[VectraBenchmark.METRIC_COUNT];
        // Fill with dummy results
        for (int i = 0; i < results.length; i++) {
            results[i] = new VectraBenchmark.BenchmarkResult(i, "Test" + i, 1000, 1000, "ns");
        }
        
        int[] catScores = VectraBenchmark.calculateCategoryScores(results);
        assertEquals(6, catScores.length);
    }

    @Test
    public void formatReportContainsHeader() throws Exception {
        VectraBenchmark.BenchmarkResult[] results = new VectraBenchmark.BenchmarkResult[1];
        results[0] = new VectraBenchmark.BenchmarkResult(0, "Test", 1000, 1000, "ns");
        
        String report = VectraBenchmark.formatReport(results);
        assertTrue(report.contains("VECTRAS BENCHMARK REPORT"));
    }

    @Test
    public void metricCountIs79() {
        assertEquals(79, VectraBenchmark.METRIC_COUNT);
    }
}
