package com.vectras.vm.core;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class BareMetalProfileTest {

    @Test
    public void testDetectArchitectureInKnownRange() {
        int arch = BareMetalProfile.detectArchitecture();
        assertTrue(arch >= BareMetalProfile.ARCH_UNKNOWN);
        assertTrue(arch <= BareMetalProfile.ARCH_RISCV64);
    }

    @Test
    public void testMapArchSignatureMapping() {
        assertEquals(BareMetalProfile.ARCH_ARM64, mapArchForTest(NativeFastPath.ARCH_ARM64));
        assertEquals(BareMetalProfile.ARCH_ARM32, mapArchForTest(NativeFastPath.ARCH_ARM32));
        assertEquals(BareMetalProfile.ARCH_X86_64, mapArchForTest(NativeFastPath.ARCH_X64));
        assertEquals(BareMetalProfile.ARCH_X86, mapArchForTest(NativeFastPath.ARCH_X86));
        assertEquals(BareMetalProfile.ARCH_RISCV64, mapArchForTest(NativeFastPath.ARCH_RISCV64));
        assertEquals(BareMetalProfile.ARCH_UNKNOWN, mapArchForTest(0x7F00));
    }

    @Test
    public void testMapArchIgnoresUnrelatedExtraSignatureBits() {
        int signatureWithExtras = NativeFastPath.ARCH_ARM64
                | NativeFastPath.FEATURE_AES
                | NativeFastPath.FEATURE_SIMD
                | 0xAB000000
                | NativeFastPath.OS_ANDROID;
        assertEquals(BareMetalProfile.ARCH_ARM64, mapArchForTest(signatureWithExtras));
    }

    @Test
    public void testMapArchDoesNotInferArchitectureFromFeatureBits() {
        int unknownWithoutFeatures = 0x0000;
        int unknownWithFeatures = NativeFastPath.FEATURE_AES | NativeFastPath.FEATURE_SIMD | NativeFastPath.FEATURE_CRC32;

        assertEquals(BareMetalProfile.ARCH_UNKNOWN, mapArchForTest(unknownWithoutFeatures));
        assertEquals(BareMetalProfile.ARCH_UNKNOWN, mapArchForTest(unknownWithFeatures));
    }

    @Test
    public void testCapabilitiesAlwaysExposeAtomics() {
        int flags = BareMetalProfile.detectCapabilities();
        assertTrue((flags & BareMetalProfile.CAP_ATOMICS) != 0);
    }

    @Test
    public void testRecommendedValuesArePositive() {
        assertTrue(BareMetalProfile.recommendedWorkBlockBytes() > 0);
        assertTrue(BareMetalProfile.recommendedParallelism() > 0);
        assertTrue(BareMetalProfile.runtimeMemoryClassBytes() > 0);
    }

    @Test
    public void testCapabilitiesBitfieldContainsOnlyKnownBits() {
        int flags = BareMetalProfile.detectCapabilities();
        int knownMask = BareMetalProfile.CAP_ATOMICS
                | BareMetalProfile.CAP_UNALIGNED_FAST
                | BareMetalProfile.CAP_VECTOR_INT
                | BareMetalProfile.CAP_VECTOR_FP
                | BareMetalProfile.CAP_LITTLE_ENDIAN
                | BareMetalProfile.CAP_MULTI_CORE
                | BareMetalProfile.CAP_64_BIT
                | BareMetalProfile.CAP_NEON_OR_SSE
                | BareMetalProfile.CAP_AES
                | BareMetalProfile.CAP_CRC32;
        assertEquals(0, flags & ~knownMask);
    }

    private static int mapArchForTest(int signature) {
        try {
            Method method = BareMetalProfile.class.getDeclaredMethod("mapArch", int.class);
            method.setAccessible(true);
            return (Integer) method.invoke(null, signature);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("Unable to invoke mapArch(int signature)", e);
        } catch (InvocationTargetException e) {
            throw new AssertionError("mapArch(int signature) threw an exception", e.getCause());
        }
    }
}
