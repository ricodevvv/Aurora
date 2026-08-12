package com.ricodevvv.aurora.cosmetic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catalogo de cosmeticos disponibles. Registra los tuyos en el onEnable. */
public final class CosmeticRegistry {

    private static final Map<CosmeticCategory, Map<String, CosmeticType>> TYPES =
            new EnumMap<>(CosmeticCategory.class);

    private CosmeticRegistry() {
    }

    public static void register(CosmeticType type) {
        TYPES.computeIfAbsent(type.category(), key -> new LinkedHashMap<>())
                .put(type.id().toLowerCase(), type);
    }

    public static void registerAll(CosmeticType... types) {
        for (CosmeticType type : types) register(type);
    }

    public static CosmeticType get(CosmeticCategory category, String id) {
        Map<String, CosmeticType> map = TYPES.get(category);
        return map == null ? null : map.get(id.toLowerCase());
    }

    /** Busca por id en todas las categorias. */
    public static CosmeticType find(String id) {
        for (Map<String, CosmeticType> map : TYPES.values()) {
            CosmeticType type = map.get(id.toLowerCase());
            if (type != null) return type;
        }
        return null;
    }

    public static List<CosmeticType> byCategory(CosmeticCategory category) {
        Map<String, CosmeticType> map = TYPES.get(category);
        return map == null ? Collections.emptyList() : new ArrayList<>(map.values());
    }

    public static void clear() {
        TYPES.clear();
    }
}
