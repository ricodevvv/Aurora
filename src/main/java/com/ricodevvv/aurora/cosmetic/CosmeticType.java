package com.ricodevvv.aurora.cosmetic;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The definition of a cosmetic: its identity, icon and permission.
 *
 * <p>A type is immutable, shared and stateless. The per-player, stateful
 * counterpart is {@link Cosmetic}, produced by {@link #create(Player)}.
 *
 * @see CosmeticRegistry
 * @see CosmeticManager
 */
public interface CosmeticType {

    /**
     * @return an identifier unique within the category, lowercase and without
     * spaces; this is what gets persisted
     */
    String id();

    /**
     * @return the category this cosmetic belongs to
     */
    CosmeticCategory category();

    /**
     * @return the display name, which may contain {@code &} colour codes
     */
    String displayName();

    /**
     * @return the icon shown in a selection menu
     */
    ItemStack icon();

    /**
     * @return the permission required to equip this, or {@code null} if it is
     * free for everyone; defaults to {@code aurora.cosmetic.<category>.<id>}
     */
    default String permission() {
        return "aurora.cosmetic." + category().name().toLowerCase() + "." + id();
    }

    /**
     * @param player player to check
     * @return {@code true} if the player may equip this cosmetic
     */
    default boolean canUse(Player player) {
        String permission = permission();
        return permission == null || player.hasPermission(permission);
    }

    /**
     * Creates a live instance for a player. The returned cosmetic is not
     * started; {@link CosmeticManager#equip(Player, CosmeticType)} does that.
     *
     * @param player player who will wear it
     * @return a new, unstarted instance
     */
    Cosmetic create(Player player);
}
