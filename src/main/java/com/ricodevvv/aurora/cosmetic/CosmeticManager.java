package com.ricodevvv.aurora.cosmetic;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which cosmetics each player has equipped, one per category.
 *
 * <p>Nothing is persisted. Aurora deliberately does not choose a storage
 * backend: use {@link #snapshot(Player)} to obtain a plain map to write to your
 * own database, and {@link #restore(Player, Map)} to apply it on join.
 *
 * <p>Requires {@link CosmeticListener} to be registered, otherwise cosmetics
 * are never released when players disconnect.
 */
public final class CosmeticManager {

    private static final Map<UUID, Map<CosmeticCategory, Cosmetic>> EQUIPPED = new HashMap<>();

    private CosmeticManager() {
    }

    /**
     * Equips a cosmetic, replacing whatever the player had in that category.
     *
     * @param player player to equip
     * @param type   cosmetic to equip
     * @return the live instance, or {@code null} if the player lacks permission
     */
    public static Cosmetic equip(Player player, CosmeticType type) {
        if (!type.canUse(player)) return null;

        unequip(player, type.category());

        Cosmetic cosmetic = type.create(player);
        cosmetic.start();
        EQUIPPED.computeIfAbsent(player.getUniqueId(), key -> new EnumMap<>(CosmeticCategory.class))
                .put(type.category(), cosmetic);
        return cosmetic;
    }

    /**
     * Equips a cosmetic, or unequips it if that exact one is already worn.
     *
     * @param player player to toggle
     * @param type   cosmetic to toggle
     * @return {@code true} if the cosmetic ended up equipped
     */
    public static boolean toggle(Player player, CosmeticType type) {
        Cosmetic current = equipped(player, type.category());
        if (current != null && current.type().id().equalsIgnoreCase(type.id())) {
            unequip(player, type.category());
            return false;
        }
        return equip(player, type) != null;
    }

    /**
     * Removes whatever the player has equipped in one category.
     *
     * @param player   player to update
     * @param category category to clear
     */
    public static void unequip(Player player, CosmeticCategory category) {
        Map<CosmeticCategory, Cosmetic> worn = EQUIPPED.get(player.getUniqueId());
        if (worn == null) return;

        Cosmetic cosmetic = worn.remove(category);
        if (cosmetic != null) cosmetic.stop();
    }

    /**
     * Removes every cosmetic a player is wearing. Always call this on quit.
     *
     * @param player player to clear
     */
    public static void unequipAll(Player player) {
        Map<CosmeticCategory, Cosmetic> worn = EQUIPPED.remove(player.getUniqueId());
        if (worn == null) return;

        for (Cosmetic cosmetic : new ArrayList<>(worn.values())) {
            cosmetic.stop();
        }
    }

    /**
     * @param player   player to query
     * @param category category to query
     * @return the equipped cosmetic, or {@code null} if none
     */
    public static Cosmetic equipped(Player player, CosmeticCategory category) {
        Map<CosmeticCategory, Cosmetic> worn = EQUIPPED.get(player.getUniqueId());
        return worn == null ? null : worn.get(category);
    }

    /**
     * @param player player to query
     * @return every cosmetic the player is wearing; never {@code null}
     */
    public static Collection<Cosmetic> equipped(Player player) {
        Map<CosmeticCategory, Cosmetic> worn = EQUIPPED.get(player.getUniqueId());
        return worn == null ? new ArrayList<>() : new ArrayList<>(worn.values());
    }

    /**
     * Captures what the player is wearing as plain identifiers, ready to be
     * written to storage.
     *
     * @param player player to capture
     * @return a category-to-identifier map
     */
    public static Map<CosmeticCategory, String> snapshot(Player player) {
        Map<CosmeticCategory, String> snapshot = new EnumMap<>(CosmeticCategory.class);
        for (Cosmetic cosmetic : equipped(player)) {
            snapshot.put(cosmetic.category(), cosmetic.type().id());
        }
        return snapshot;
    }

    /**
     * Re-equips a previously captured snapshot. Identifiers that are no longer
     * registered are skipped, so removing a cosmetic from your catalogue does
     * not break saved data.
     *
     * @param player   player to restore
     * @param snapshot map produced by {@link #snapshot(Player)}
     */
    public static void restore(Player player, Map<CosmeticCategory, String> snapshot) {
        for (Map.Entry<CosmeticCategory, String> entry : snapshot.entrySet()) {
            CosmeticType type = CosmeticRegistry.get(entry.getKey(), entry.getValue());
            if (type != null) equip(player, type);
        }
    }

    /**
     * Releases every cosmetic on every player. Called during shutdown.
     */
    public static void shutdown() {
        List<Cosmetic> all = new ArrayList<>();
        for (Map<CosmeticCategory, Cosmetic> worn : EQUIPPED.values()) {
            all.addAll(worn.values());
        }
        EQUIPPED.clear();

        for (Cosmetic cosmetic : all) {
            cosmetic.stop();
        }
    }
}
