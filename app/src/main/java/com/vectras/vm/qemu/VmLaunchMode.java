package com.vectras.vm.qemu;

/**
 * Enumeration of virtual machine launch modes.
 * HEADLESS indicates engine-only execution without any UI attachment.
 * VNC, X11, and SPICE represent the available frontends.
 *
 * This class centralizes the logic for determining the launch mode
 * based on the configured UI and command-line environment, rather than
 * scattering string checks throughout the codebase.
 */
public enum VmLaunchMode {
    HEADLESS,
    VNC,
    X11,
    SPICE;

    /**
     * Determine the appropriate launch mode for a VM.
     *
     * @param vmUi           the UI configured for the virtual machine (e.g. "VNC", "X11", "SPICE")
     * @param engineHeadless whether headless mode has been forced in the application config
     * @param env            the command string for launching the VM
     * @return the resolved launch mode
     */
    public static VmLaunchMode determine(String vmUi, boolean engineHeadless, String env) {
        // If headless mode is forced or explicitly declared in the command, treat the launch as headless.
        if (engineHeadless || (env != null && (env.contains("-display none") || env.contains("headless=true")))) {
            return HEADLESS;
        }
        // Otherwise resolve from the configured UI name.
        if (vmUi == null) {
            return VNC;
        }
        switch (vmUi) {
            case "X11":
                return X11;
            case "SPICE":
                return SPICE;
            case "VNC":
            default:
                return VNC;
        }
    }

    /** Returns true if this launch mode does not require a frontend to be attached. */
    public boolean isHeadless() {
        return this == HEADLESS;
    }
}
