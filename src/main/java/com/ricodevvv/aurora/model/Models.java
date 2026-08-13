package com.ricodevvv.aurora.model;

import com.ricodevvv.aurora.packet.PacketSupport;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Creates {@link Model} instances, picking the best available backend.
 *
 * <p>If PacketEvents is present the packet backend is used, which keeps
 * cosmetics out of the world entirely: they do not count towards entity limits,
 * are not ticked by the server, do not show up in {@code /kill @e} and are
 * invisible to anti-cheats. Otherwise Aurora falls back to real Bukkit
 * entities, which work everywhere but are ordinary world entities.
 */
public final class Models {

    private static Boolean forcedPackets;

    private Models() {
    }

    /**
     * Creates an armour stand model.
     *
     * @param small   whether to use a small armour stand
     * @param viewers supplies the players who may see it, or {@code null} for
     *                everyone nearby; only honoured by the packet backend,
     *                since a real Bukkit entity cannot be hidden per player
     * @return a new, unspawned model
     */
    public static Model armorStand(boolean small, Supplier<Collection<? extends Player>> viewers) {
        return usePackets()
                ? new PacketArmorStandModel(small, viewers)
                : new BukkitArmorStandModel(small);
    }

    /**
     * Creates an armour stand model visible to everyone nearby.
     *
     * @param small whether to use a small armour stand
     * @return a new, unspawned model
     */
    public static Model armorStand(boolean small) {
        return armorStand(small, null);
    }

    /**
     * @return {@code true} if models will be backed by packets
     */
    public static boolean usePackets() {
        return forcedPackets != null ? forcedPackets : PacketSupport.available();
    }

    /**
     * Overrides backend selection, mainly to exercise the Bukkit path on a
     * server that does have PacketEvents.
     *
     * @param value {@code true} to force packets, {@code false} to force real
     *              entities, {@code null} to auto-detect
     */
    public static void usePackets(Boolean value) {
        forcedPackets = value;
    }
}
