package com.ricodevvv.aurora.util;

import org.bukkit.Bukkit;

/**
 * Deteccion de version del servidor, valida de 1.8 a 1.21+.
 *
 * OJO: desde 1.20.5 Paper dejo de relocalizar el paquete de CraftBukkit
 * (ya no existe org.bukkit.craftbukkit.v1_20_R4), por eso NO parseamos la
 * version desde el paquete de CraftServer sino desde Bukkit#getBukkitVersion().
 */
public final class ServerVersion {

    private static final int MINOR;
    private static final int PATCH;
    private static final String NMS_TAG; // "v1_8_R3" o null en 1.20.5+ de Paper

    static {
        int minor = 0, patch = 0;
        try {
            String raw = Bukkit.getBukkitVersion().split("-")[0]; // 1.8.8-R0.1-SNAPSHOT -> 1.8.8
            String[] parts = raw.split("\\.");
            minor = Integer.parseInt(parts[1]);
            if (parts.length > 2) patch = Integer.parseInt(parts[2]);
        } catch (Throwable ignored) {
        }
        MINOR = minor;
        PATCH = patch;

        String tag = null;
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            String[] split = pkg.split("\\.");
            if (split.length >= 4 && split[3].startsWith("v1_")) tag = split[3];
        } catch (Throwable ignored) {
        }
        NMS_TAG = tag;
    }

    private ServerVersion() {
    }

    public static int minor() {
        return MINOR;
    }

    public static int patch() {
        return PATCH;
    }

    /** true en 1.8.x: sin Particle API de Bukkit, hay que mandar paquetes. */
    public static boolean isLegacy() {
        return MINOR < 9;
    }

    /** true en 1.13+: BlockData, DustOptions, Material sin data values. */
    public static boolean isFlattened() {
        return MINOR >= 13;
    }

    public static boolean atLeast(int minorVer) {
        return MINOR >= minorVer;
    }

    public static boolean atLeast(int minorVer, int patchVer) {
        return MINOR > minorVer || (MINOR == minorVer && PATCH >= patchVer);
    }

    /** Ej: "v1_8_R3". Puede ser null en 1.20.5+ (Paper sin relocacion). */
    public static String nmsTag() {
        return NMS_TAG;
    }

    @Override
    public String toString() {
        return "1." + MINOR + "." + PATCH;
    }
}
