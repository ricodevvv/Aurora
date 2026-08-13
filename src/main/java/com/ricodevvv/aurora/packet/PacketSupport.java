package com.ricodevvv.aurora.packet;

import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detects whether PacketEvents is usable and hands out synthetic entity ids.
 *
 * <p>PacketEvents is an optional dependency. This class only performs class
 * lookups by name, so it is always safe to load; the classes that genuinely
 * import PacketEvents live elsewhere and are never touched unless
 * {@link #available()} returned {@code true}, which is what keeps a server
 * without it from hitting {@code NoClassDefFoundError}.
 */
public final class PacketSupport {

    /**
     * Synthetic entity ids start high so they cannot collide with ids the
     * server hands out. A running server does not come close to this many
     * entities in a single session.
     */
    private static final AtomicInteger ENTITY_ID = new AtomicInteger(Integer.MAX_VALUE / 2);

    private static Boolean available;

    private PacketSupport() {
    }

    /**
     * @return {@code true} if PacketEvents is present and initialised
     */
    public static boolean available() {
        if (available != null) return available;

        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            available = Bukkit.getPluginManager().getPlugin("packetevents") != null
                    || isApiInitialised();
        } catch (Throwable ignored) {
            available = false;
        }

        if (!available) {
            Bukkit.getLogger().info("[Aurora] PacketEvents not found; using real entities.");
        }
        return available;
    }

    /**
     * Checks whether the API was initialised by a shaded copy rather than by
     * the standalone plugin.
     *
     * @return {@code true} if the API is live
     */
    private static boolean isApiInitialised() {
        try {
            Class<?> api = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            return api.getMethod("getAPI").invoke(null) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * @return a fresh entity id for a packet-only entity
     */
    public static int nextEntityId() {
        return ENTITY_ID.incrementAndGet();
    }

    /**
     * Forces the packet backend off, for testing the Bukkit path on a server
     * that does have PacketEvents.
     */
    public static void forceDisable() {
        available = false;
    }
}
