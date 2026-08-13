package com.ricodevvv.aurora.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;

/**
 * Item helpers that survive the 1.13 flattening.
 *
 * <p>Player heads are the awkward case: they were {@code SKULL_ITEM} with a
 * data value of {@code 3} before 1.13 and {@code PLAYER_HEAD} after, and the
 * method for assigning an owner changed from {@code setOwner(String)} to
 * {@code setOwningPlayer(OfflinePlayer)} in 1.12.1. Both paths are tried.
 */
public final class Items {

    private static final Material HEAD_MATERIAL;
    private static final boolean LEGACY_SKULL;

    static {
        Material modern = Material.getMaterial("PLAYER_HEAD");
        if (modern != null) {
            HEAD_MATERIAL = modern;
            LEGACY_SKULL = false;
        } else {
            HEAD_MATERIAL = Material.getMaterial("SKULL_ITEM");
            LEGACY_SKULL = true;
        }
    }

    private Items() {
    }

    /**
     * @return an untextured player head
     */
    @SuppressWarnings("deprecation")
    public static ItemStack head() {
        return LEGACY_SKULL
                ? new ItemStack(HEAD_MATERIAL, 1, (short) 3)
                : new ItemStack(HEAD_MATERIAL);
    }

    /**
     * Creates a player head wearing someone's skin.
     *
     * <p>Resolving the owner may hit Mojang's session servers on first use, so
     * avoid calling this in a tick loop; build the item once and reuse it.
     *
     * @param owner player name; {@code null} or empty yields an untextured head
     * @return the head item
     */
    public static ItemStack head(String owner) {
        ItemStack item = head();
        if (owner == null || owner.isEmpty()) return item;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        if (!applyModernOwner(meta, owner)) {
            applyLegacyOwner(meta, owner);
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Tries the 1.12.1+ owner setter.
     *
     * @param meta  skull meta to modify
     * @param owner player name
     * @return {@code true} if it was applied
     */
    private static boolean applyModernOwner(SkullMeta meta, String owner) {
        try {
            Method setter = meta.getClass().getMethod("setOwningPlayer",
                    Class.forName("org.bukkit.OfflinePlayer"));
            setter.invoke(meta, Bukkit.getOfflinePlayer(owner));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Falls back to the pre-1.12.1 owner setter.
     *
     * @param meta  skull meta to modify
     * @param owner player name
     */
    private static void applyLegacyOwner(SkullMeta meta, String owner) {
        try {
            meta.getClass().getMethod("setOwner", String.class).invoke(meta, owner);
        } catch (Throwable ignored) {
            // Nothing else to try; the head stays untextured.
        }
    }

    /**
     * Resolves the first material that exists under any of the given names,
     * which is how a single call can cover both sides of the flattening.
     *
     * @param names candidate material names, most preferred first
     * @return the first match, or {@link Material#STONE} if none exist
     */
    public static Material material(String... names) {
        for (String name : names) {
            Material material = Material.getMaterial(name);
            if (material != null) return material;
        }
        return Material.STONE;
    }
}
