package com.example.ecommerce.test.podman;

/**
 * Helpers for running Testcontainers against a Podman backend (e.g. on macOS
 * where podman exposes /var/run/docker.sock instead of Docker Desktop).
 *
 * <p>Apply at static initialiser time of any Testcontainers integration
 * test base class by calling {@link #apply()}.
 */
public final class PodmanCompatibility {

    private PodmanCompatibility() {}

    public static void apply() {
        // Ryuk reaper depends on cgroups behaviour that podman rootless does
        // not always provide. Disabling it means containers may linger if a
        // test JVM is killed forcibly — for CI you should configure podman's
        // socket-activation reaper instead.
        if (System.getProperty("testcontainers.ryuk.disabled") == null
                && System.getenv("TESTCONTAINERS_RYUK_DISABLED") == null) {
            System.setProperty("testcontainers.ryuk.disabled", "true");
        }
    }
}
