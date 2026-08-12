package com.ricodevvv.aurora.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;

/** Utilidades de items compatibles 1.8 - 1.21 (sobre todo cabezas). */
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

    /** Cabeza vacia (sin skin). */
    @SuppressWarnings("deprecation")
    public static ItemStack head() {
        return LEGACY_SKULL ? new ItemStack(HEAD_MATERIAL, 1, (short) 3) : new ItemStack(HEAD_MATERIAL);
    }

    /** Cabeza con la skin de un jugador. */
    public static ItemStack head(String owner) {
        ItemStack item = head();
        if (owner == null || owner.isEmpty()) return item;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        boolean applied = false;
        // 1.12.1+ prefiere setOwningPlayer(OfflinePlayer)
        try {
            Method setOwningPlayer = meta.getClass().getMethod("setOwningPlayer",
                    Class.forName("org.bukkit.OfflinePlayer"));
            setOwningPlayer.invoke(meta, Bukkit.getOfflinePlayer(owner));
            applied = true;
        } catch (Throwable ignored) {
        }
        if (!applied) {
            try {
                Method setOwner = meta.getClass().getMethod("setOwner", String.class);
                setOwner.invoke(meta, owner);
            } catch (Throwable ignored) {
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Material por nombre, probando varios alias (para saltar el flattening de 1.13). */
    public static Material material(String... names) {
        for (String name : names) {
            Material material = Material.getMaterial(name);
            if (material != null) return material;
        }
        return Material.STONE;
    }
}
