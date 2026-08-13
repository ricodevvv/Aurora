package com.ricodevvv.aurora.util;

import org.bukkit.Bukkit;

/**
 * Detects the running server version, from 1.8 through 1.21 and later.
 *
 * <p>The version is parsed from {@link Bukkit#getBukkitVersion()} rather than
 * from the CraftBukkit package name. This matters: Paper stopped relocating its
 * CraftBukkit package in 1.20.5, so {@code org.bukkit.craftbukkit.v1_20_R4} no
 * longer exists and every library that parsed the package name broke there.
 *
 * <p>All values are resolved once in a static initialiser, so the accessors are
 * free to call from a tick loop.
 */
public final class ServerVersion {

    private static final int MINOR;
    private static final int PATCH;
    private static final String NMS_TAG;

    static {
        int minor = 0;
        int patch = 0;
        try {
            // "1.8.8-R0.1-SNAPSHOT" -> "1.8.8"
            String raw = Bukkit.getBukkitVersion().split("-")[0];
            String[] parts = raw.split("\\.");
            minor = Integer.parseInt(parts[1]);
            if (parts.length > 2) patch = Integer.parseInt(parts[2]);
        } catch (Throwable ignored) {
            // Leave both at zero; callers degrade to the legacy path.
        }
        MINOR = minor;
        PATCH = patch;

        String tag = null;
        try {
            String[] parts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
            if (parts.length >= 4 && parts[3].startsWith("v1_")) tag = parts[3];
        } catch (Throwable ignored) {
            // Not relocated, which is expected on Paper 1.20.5+.
        }
        NMS_TAG = tag;
    }

    private ServerVersion() {
    }

    /**
     * @return the minor version number: {@code 8} for 1.8.8, {@code 21} for 1.21.4
     */
    public static int minor() {
        return MINOR;
    }

    /**
     * @return the patch number: {@code 8} for 1.8.8, {@code 4} for 1.21.4
     */
    public static int patch() {
        return PATCH;
    }

    /**
     * @return {@code true} on 1.8.x, where Bukkit has no particle API and
     * several entity setters simply do not exist
     */
    public static boolean isLegacy() {
        return MINOR < 9;
    }

    /**
     * @return {@code true} on 1.13 and later, after the flattening introduced
     * block data, dust options and materials without data values
     */
    public static boolean isFlattened() {
        return MINOR >= 13;
    }

    /**
     * @param minorVersion minor version to compare against
     * @return {@code true} if the server is that version or newer
     */
    public static boolean atLeast(int minorVersion) {
        return MINOR >= minorVersion;
    }

    /**
     * Compares including the patch number, which is required for cut-offs that
     * landed mid-series such as 1.20.5.
     *
     * @param minorVersion minor version to compare against
     * @param patchVersion minimum patch number
     * @return {@code true} if the server is that version or newer
     */
    public static boolean atLeast(int minorVersion, int patchVersion) {
        return MINOR > minorVersion || (MINOR == minorVersion && PATCH >= patchVersion);
    }

    /**
     * The relocated NMS package tag, such as {@code v1_8_R3}.
     *
     * @return the tag, or {@code null} on servers that no longer relocate
     * (Paper 1.20.5 and later)
     */
    public static String nmsTag() {
        return NMS_TAG;
    }

    /**
     * @return a human-readable version string such as {@code 1.8.8}
     */
    public static String asString() {
        return "1." + MINOR + "." + PATCH;
    }

    @Override
    public String toString() {
        return asString();
    }
}
