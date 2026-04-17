package com.vectras.vm;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;

import org.junit.Assert;
import org.junit.Test;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.junit.runner.RunWith;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = LoaderSignatureVerificationTest.TEST_SDK)
public class LoaderSignatureVerificationTest {
    public static final int TEST_SDK = 29;

    private static Signature signature(String hex) {
        return new Signature(hex);
    }

    private static String hex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        final char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }

    private static String expectedPrimaryAbi() {
        if (android.os.Build.SUPPORTED_ABIS != null && android.os.Build.SUPPORTED_ABIS.length > 0) {
            String abi = android.os.Build.SUPPORTED_ABIS[0];
            if (abi != null && !abi.trim().isEmpty()) {
                return abi;
            }
        }

        String arch = System.getProperty("os.arch");
        if (arch != null && !arch.trim().isEmpty()) {
            return arch;
        }

        return "unknown";
    }

    private static void setLegacySignatures(PackageInfo packageInfo, Signature[] signatures) throws Exception {
        java.lang.reflect.Field signaturesField = PackageInfo.class.getField("signatures");
        signaturesField.set(packageInfo, signatures);
    }

    private static String sha256Hex(byte[] input) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
        char[] out = new char[digest.length * 2];
        final char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < digest.length; i++) {
            int v = digest[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }

    @Test
    public void isTrustedSignature_acceptsExpectedSignatures_withOrderNormalization() throws Exception {
        byte[] signerABytes = new byte[]{1, 2, 3, 4};
        byte[] signerBBytes = new byte[]{5, 6, 7, 8};
        Signature signerA = signature(hex(signerABytes));
        Signature signerB = signature(hex(signerBBytes));

        PackageInfo packageInfo = new PackageInfo();
        setLegacySignatures(packageInfo, new Signature[]{signerB, signerA});

        List<String> expected = new ArrayList<>(Arrays.asList(
                sha256Hex(signerABytes),
                sha256Hex(signerBBytes)
        ));
        Collections.sort(expected);

        Assert.assertTrue(Loader.isTrustedSignature(packageInfo, Collections.unmodifiableList(expected)));
    }

    @Test
    public void isTrustedSignature_rejectsDivergentSignature() throws Exception {
        Signature signer = signature("0a0b0c0d");

        PackageInfo packageInfo = new PackageInfo();
        setLegacySignatures(packageInfo, new Signature[]{signer});

        List<String> expected = Collections.singletonList(sha256Hex(new byte[]{99, 100, 101}));

        Assert.assertFalse(Loader.isTrustedSignature(packageInfo, expected));
    }

    @Test
    public void getSecurityValidationError_returnsNotInstalledMessage_whenTargetPackageMissing() {
        Assert.assertEquals(
                BuildConfig.packageNotInstalledErrorText.replace("ARCH", expectedPrimaryAbi()),
                Loader.getSecurityValidationError(null, Collections.singletonList("expected"))
        );
    }

    @Test
    public void getSecurityValidationError_returnsSignatureMismatchMessage_whenSignatureIsUntrusted() throws Exception {
        Signature signer = signature("2a2b2c2d");

        PackageInfo packageInfo = new PackageInfo();
        setLegacySignatures(packageInfo, new Signature[]{signer});

        List<String> expected = Collections.singletonList(sha256Hex(new byte[]{9, 9, 9, 9}));

        Assert.assertEquals(
                BuildConfig.packageSignatureMismatchErrorText,
                Loader.getSecurityValidationError(packageInfo, expected)
        );
    }

    @Test
    public void getSecurityValidationError_returnsNull_whenSignatureIsTrusted() throws Exception {
        byte[] signerBytes = new byte[]{7, 7, 7, 7};
        Signature signer = signature(hex(signerBytes));

        PackageInfo packageInfo = new PackageInfo();
        setLegacySignatures(packageInfo, new Signature[]{signer});

        List<String> expected = Collections.singletonList(sha256Hex(signerBytes));

        Assert.assertNull(Loader.getSecurityValidationError(packageInfo, expected));
    }
}
