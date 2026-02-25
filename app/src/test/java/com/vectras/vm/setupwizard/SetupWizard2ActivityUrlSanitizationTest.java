package com.vectras.vm.setupwizard;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
public class SetupWizard2ActivityUrlSanitizationTest {

    @Test
    public void sanitizeBootstrapUrlKeepsNormalUrl() throws Exception {
        String raw = "https://downloads.vectras.com/releases/setup.tar.gz?arch=arm64#latest";

        String sanitized = invokeSanitizeBootstrapUrl(raw);

        Assert.assertEquals(raw, sanitized);
    }

    @Test
    public void sanitizeBootstrapUrlCollapsesDoubleSlashesInPath() throws Exception {
        String raw = "https://downloads.vectras.com//releases///setup.tar.gz?arch=arm64#latest";

        String sanitized = invokeSanitizeBootstrapUrl(raw);

        Assert.assertEquals("https://downloads.vectras.com/releases/setup.tar.gz?arch=arm64#latest", sanitized);
    }

    @Test
    public void sanitizeBootstrapUrlRejectsInvalidHost() throws Exception {
        String sanitized = invokeSanitizeBootstrapUrl("https://localhost/setup.tar.gz");

        Assert.assertNull(sanitized);
    }

    @Test
    public void sanitizeBootstrapUrlRejectsInvalidScheme() throws Exception {
        String sanitized = invokeSanitizeBootstrapUrl("ftp://downloads.vectras.com/setup.tar.gz");

        Assert.assertNull(sanitized);
    }

    @Test
    public void sanitizeBootstrapUrlRejectsCredentialsInAuthority() throws Exception {
        String sanitized = invokeSanitizeBootstrapUrl("https://user:pass@downloads.vectras.com/setup.tar.gz");

        Assert.assertNull(sanitized);
    }

    @Test
    public void buildBootstrapDownloadCommandUsesNormalizedUrl() throws Exception {
        SetupWizard2Activity activity = Robolectric.buildActivity(SetupWizard2Activity.class).setup().get();
        Method method = SetupWizard2Activity.class.getDeclaredMethod("buildBootstrapDownloadCommand", String.class, boolean.class);
        method.setAccessible(true);

        String command = (String) method.invoke(activity, "https://downloads.vectras.com//releases///setup.tar.gz", false);

        Assert.assertTrue(command.contains("https://downloads.vectras.com/releases/setup.tar.gz"));
        Assert.assertFalse(command.contains("https://downloads.vectras.com//releases///setup.tar.gz"));
    }

    private String invokeSanitizeBootstrapUrl(String link) throws Exception {
        Method method = SetupWizard2Activity.class.getDeclaredMethod("sanitizeBootstrapUrl", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, link);
    }
}
