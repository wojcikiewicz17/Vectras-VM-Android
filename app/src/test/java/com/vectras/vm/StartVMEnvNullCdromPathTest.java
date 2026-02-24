package com.vectras.vm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import android.app.Activity;

import com.vectras.qemu.MainSettingsManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class StartVMEnvNullCdromPathTest {

    @Test
    public void env_noVmSelectedAndNullCdromPath_doesNotThrowNpe() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        StartVM.cdrompath = null;

        try (MockedStatic<MainSettingsManager> settings = mockStatic(MainSettingsManager.class)) {
            settings.when(() -> MainSettingsManager.getArch(any(Activity.class))).thenReturn("X86_64");
            settings.when(() -> MainSettingsManager.getIfType(any(Activity.class))).thenReturn("");
            settings.when(() -> MainSettingsManager.get3dfxEnabled(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.getVmUi(any(Activity.class))).thenReturn("VNC");
            settings.when(() -> MainSettingsManager.getUseSdl(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.getSharedFolder(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.getBoot(any(Activity.class))).thenReturn("c");
            settings.when(() -> MainSettingsManager.useDefaultBios(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.getuseUEFI(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.useMemoryOvercommit(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.useLocalTime(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.getVncExternalPassword(any(Activity.class))).thenReturn("");
            settings.when(() -> MainSettingsManager.getVncExternal(any(Activity.class))).thenReturn(false);
            settings.when(() -> MainSettingsManager.getRunQemuWithXterm(any(Activity.class))).thenReturn(false);

            String env = StartVM.env(activity, "-nodefaults", "", false);

            assertNotNull(env);
            assertTrue(env.contains("qemu-system-x86_64"));
        }
    }
}
