package com.ricodevvv.aurora.cosmetic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogue of available cosmetics, keyed by category and identifier.
 *
 * <p>Insertion order is preserved within a category, so
 * {@link #byCategory(CosmeticCategory)} can be rendered straight into a menu
 * without sorting.
 *
 * <p>Register your own during {@code onEnable}, before any player can equip
 * anything.
 */
public final class CosmeticRegistry {

    private static final Map<CosmeticCategory, Map<String, CosmeticType>> TYPES =
            new EnumMap<>(CosmeticCategory.class);

    private CosmeticRegistry() {
    }

    /**
     * Registers a cosmetic, replacing any existing one with the same category
     * and identifier.
     *
     * @param type cosmetic to register
     */
    public static void register(CosmeticType type) {
        TYPES.computeIfAbsent(type.category(), key -> new LinkedHashMap<>())
                .put(type.id().toLowerCase(), type);
    }

    /**
     * Registers several cosmetics at once.
     *
     * @param types cosmetics to register
     */
    public static void registerAll(CosmeticType... types) {
        for (CosmeticType type : types) {
            register(type);
        }
    }

    /**
     * Looks up a cosmetic by category and identifier.
     *
     * @param category category to search
     * @param id       identifier, case-insensitive
     * @return the cosmetic, or {@code null} if unknown
     */
    public static CosmeticType get(CosmeticCategory category, String id) {
        Map<String, CosmeticType> byId = TYPES.get(category);
        return byId == null ? null : byId.get(id.toLowerCase());
    }

    /**
     * Looks up a cosmetic by identifier across every category.
     *
     * <p>Identifiers are only guaranteed unique within a category, so prefer
     * {@link #get(CosmeticCategory, String)} when the category is known.
     *
     * @param id identifier, case-insensitive
     * @return the first match, or {@code null} if none
     */
    public static CosmeticType find(String id) {
        for (Map<String, CosmeticType> byId : TYPES.values()) {
            CosmeticType type = byId.get(id.toLowerCase());
            if (type != null) return type;
        }
        return null;
    }

    /**
     * Lists every cosmetic in a category, in registration order.
     *
     * @param category category to list
     * @return the cosmetics, or an empty list if none are registered
     */
    public static List<CosmeticType> byCategory(CosmeticCategory category) {
        Map<String, CosmeticType> byId = TYPES.get(category);
        return byId == null ? Collections.emptyList() : new ArrayList<>(byId.values());
    }

    /**
     * Empties the catalogue. Intended for reloads and tests.
     */
    public static void clear() {
        TYPES.clear();
    }
}
