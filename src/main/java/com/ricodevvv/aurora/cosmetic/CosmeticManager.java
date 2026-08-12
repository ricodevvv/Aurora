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
 * Estado de cosmeticos por jugador: uno equipado por categoria.
 *
 * No persiste nada a proposito. Engancha tu MongoDB/Redis en los hooks de
 * equip/unequip, o llama a equipped()/equip() desde tu propio loader al
 * entrar el jugador.
 */
public final class CosmeticManager {

    private static final Map<UUID, Map<CosmeticCategory, Cosmetic>> EQUIPPED = new HashMap<>();

    private CosmeticManager() {
    }

    /**
     * Equipa un cosmetico. Si ya traia uno de la misma categoria, lo quita antes.
     *
     * @return la instancia viva, o null si el jugador no tiene permiso.
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

    /** Equipa si no lo trae, quita si ya lo traia. Devuelve true si quedo equipado. */
    public static boolean toggle(Player player, CosmeticType type) {
        Cosmetic current = equipped(player, type.category());
        if (current != null && current.type().id().equalsIgnoreCase(type.id())) {
            unequip(player, type.category());
            return false;
        }
        return equip(player, type) != null;
    }

    public static void unequip(Player player, CosmeticCategory category) {
        Map<CosmeticCategory, Cosmetic> map = EQUIPPED.get(player.getUniqueId());
        if (map == null) return;
        Cosmetic cosmetic = map.remove(category);
        if (cosmetic != null) cosmetic.stop();
    }

    /** Quita todo. Llamalo en PlayerQuitEvent, siempre. */
    public static void unequipAll(Player player) {
        Map<CosmeticCategory, Cosmetic> map = EQUIPPED.remove(player.getUniqueId());
        if (map == null) return;
        for (Cosmetic cosmetic : new ArrayList<>(map.values())) cosmetic.stop();
    }

    public static Cosmetic equipped(Player player, CosmeticCategory category) {
        Map<CosmeticCategory, Cosmetic> map = EQUIPPED.get(player.getUniqueId());
        return map == null ? null : map.get(category);
    }

    public static Collection<Cosmetic> equipped(Player player) {
        Map<CosmeticCategory, Cosmetic> map = EQUIPPED.get(player.getUniqueId());
        return map == null ? new ArrayList<>() : new ArrayList<>(map.values());
    }

    /** Ids equipados por categoria, listo para guardar en tu base de datos. */
    public static Map<CosmeticCategory, String> snapshot(Player player) {
        Map<CosmeticCategory, String> result = new EnumMap<>(CosmeticCategory.class);
        for (Cosmetic cosmetic : equipped(player)) {
            result.put(cosmetic.category(), cosmetic.type().id());
        }
        return result;
    }

    /** Restaura desde un snapshot guardado. Ignora ids que ya no existan. */
    public static void restore(Player player, Map<CosmeticCategory, String> snapshot) {
        for (Map.Entry<CosmeticCategory, String> entry : snapshot.entrySet()) {
            CosmeticType type = CosmeticRegistry.get(entry.getKey(), entry.getValue());
            if (type != null) equip(player, type);
        }
    }

    /** Limpieza global. Llamalo en onDisable o se te quedan entidades pegadas. */
    public static void shutdown() {
        List<Cosmetic> all = new ArrayList<>();
        for (Map<CosmeticCategory, Cosmetic> map : EQUIPPED.values()) all.addAll(map.values());
        EQUIPPED.clear();
        for (Cosmetic cosmetic : all) cosmetic.stop();
    }
}
