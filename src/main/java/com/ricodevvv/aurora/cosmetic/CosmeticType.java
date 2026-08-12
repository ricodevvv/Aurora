package com.ricodevvv.aurora.cosmetic;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Definicion de un cosmetico: su id, categoria, icono y permiso.
 * Es un dato inmutable; la instancia viva por jugador es {@link Cosmetic}.
 */
public interface CosmeticType {

    /** Id unico dentro de la categoria, en minusculas y sin espacios. */
    String id();

    CosmeticCategory category();

    /** Nombre visible, acepta codigos con '&'. */
    String displayName();

    /** Icono para el menu. */
    ItemStack icon();

    /** Permiso requerido. null = libre para todos. */
    default String permission() {
        return "aurora.cosmetic." + category().name().toLowerCase() + "." + id();
    }

    default boolean canUse(Player player) {
        String permission = permission();
        return permission == null || player.hasPermission(permission);
    }

    /** Crea la instancia viva para este jugador. */
    Cosmetic create(Player player);
}
