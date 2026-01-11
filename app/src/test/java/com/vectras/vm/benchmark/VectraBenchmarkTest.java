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
    public void benchmarkResultNewFormatWorks() {
        // Test new BenchmarkResult with proper engineering units
        VectraBenchmark.BenchmarkResult r = new VectraBenchmark.BenchmarkResult(
            0, "Test", 1000000L, "1.00 ms", "ms", "CPU Single-threaded", "Test metric"
        );
        assertEquals(0, r.metricId());
        assertEquals("Test", r.name());
        assertEquals(1000000L, r.rawValue());
        assertEquals("1.00 ms", r.formattedValue());
        assertEquals("ms", r.unit());
        assertEquals("CPU Single-threaded", r.category());
        assertEquals("Test metric", r.description());
    }

    @Test
    public void benchmarkResultScoreDeprecatedReturns100() {
        // Legacy score() method should return 100 for valid results
        VectraBenchmark.BenchmarkResult r = new VectraBenchmark.BenchmarkResult(
            0, "Test", 500000L, "500.00 μs", "μs", "CPU Single-threaded", "Test metric"
        );
        assertEquals(100, r.score());
    }

    @Test
    public void calculateTotalScoreCountsValidResults() throws Exception {
        // Create minimal results array with valid results
        VectraBenchmark.BenchmarkResult[] results = new VectraBenchmark.BenchmarkResult[3];
        results[0] = new VectraBenchmark.BenchmarkResult(0, "Test1", 1000L, "1.00 μs", "μs", "CPU", "Test");
        results[1] = new VectraBenchmark.BenchmarkResult(1, "Test2", 2000L, "2.00 μs", "μs", "CPU", "Test");
        results[2] = null; // One null result
        
        int score = VectraBenchmark.calculateTotalScore(results);
        assertEquals(200, score); // 2 valid results * 100
    }

    @Test
    public void calculateCategoryScoresReturns6Categories() throws Exception {
        VectraBenchmark.BenchmarkResult[] results = new VectraBenchmark.BenchmarkResult[VectraBenchmark.METRIC_COUNT];
        // Fill with dummy results using new format
        for (int i = 0; i < results.length; i++) {
            String category;
            if (i < 20) category = "CPU Single-threaded";
            else if (i < 30) category = "CPU Multi-threaded";
            else if (i < 45) category = "Memory";
            else if (i < 60) category = "Storage";
            else if (i < 70) category = "Integrity";
            else category = "Emulation";
            
            results[i] = new VectraBenchmark.BenchmarkResult(
                i, "Test" + i, 1000L, "1.00 μs", "μs", category, "Test"
            );
        }
        
        int[] catScores = VectraBenchmark.calculateCategoryScores(results);
        assertEquals(6, catScores.length);
    }

    @Test
    public void formatReportContainsHeader() throws Exception {
        VectraBenchmark.BenchmarkResult[] results = new VectraBenchmark.BenchmarkResult[1];
        results[0] = new VectraBenchmark.BenchmarkResult(
            0, "Test", 1000L, "1.00 μs", "μs", "CPU Single-threaded", "Test metric"
        );
        
        String report = VectraBenchmark.formatReport(results);
        assertTrue(report.contains("VECTRAS PROFESSIONAL BENCHMARK REPORT"));
    }

    @Test
    public void formatReportContainsSIUnitsNote() throws Exception {
        VectraBenchmark.BenchmarkResult[] results = new VectraBenchmark.BenchmarkResult[1];
        results[0] = new VectraBenchmark.BenchmarkResult(
            0, "Test", 1000L, "1.00 μs", "μs", "CPU Single-threaded", "Test metric"
        );
        
        String report = VectraBenchmark.formatReport(results);
        assertTrue(report.contains("SI Units") || report.contains("SI units"));
    }

    @Test
    public void metricCountIs79() {
        assertEquals(79, VectraBenchmark.METRIC_COUNT);
    }
    
    @Test
    public void formatTimeProducesCorrectUnits() {
        // Test nanoseconds (now uses floating point format)
        assertEquals("500 ns", VectraBenchmark.formatTime(500));
        // Test microseconds
        assertEquals("1.500 μs", VectraBenchmark.formatTime(1500));
        // Test milliseconds
        assertEquals("1.500 ms", VectraBenchmark.formatTime(1500000));
        // Test seconds
        assertEquals("1.500 s", VectraBenchmark.formatTime(1500000000L));
    }
    
    @Test
    public void formatBandwidthProducesCorrectUnits() {
        // Test with 1MB transferred in 1 second (1e9 ns)
        String result = VectraBenchmark.formatBandwidth(1000000, 1000000000L);
        assertTrue(result.contains("MB/s") || result.contains("KB/s"));
    }
    
    @Test
    public void formatOpsPerSecProducesCorrectUnits() {
        // Test with 1 million ops in 1 second (1e9 ns)
        String result = VectraBenchmark.formatOpsPerSec(1000000, 1000000000L);
        assertTrue(result.contains("Mops/s") || result.contains("ops/s"));
    }
    
    @Test
    public void getDeviceSpecificationReturnsValidData() {
        VectraBenchmark.DeviceSpecification spec = VectraBenchmark.getDeviceSpecification();
        assertNotNull(spec);
        assertTrue(spec.cpuCores > 0);
        assertNotNull(spec.cpuModel);
        assertNotNull(spec.cpuArchitecture);
    }
}
