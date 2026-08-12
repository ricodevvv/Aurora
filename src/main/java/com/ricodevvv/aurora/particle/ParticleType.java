package com.ricodevvv.aurora.particle;

/**
 * Enum propio de particulas, independiente de la version.
 *
 * Cada entrada trae:
 *  - modernNames: los nombres posibles dentro de org.bukkit.Particle. Se resuelve
 *    el primero que exista, porque en 1.20.5 Mojang renombro medio enum
 *    (REDSTONE -> DUST, VILLAGER_HAPPY -> HAPPY_VILLAGER, etc).
 *  - legacyName: el nombre dentro de EnumParticle de NMS 1.8. Si es null,
 *    la particula no existe en 1.8 y se usa el fallback.
 */
public enum ParticleType {

    // basicas
    FLAME(Kind.NORMAL, "FLAME", "FLAME"),
    SMOKE(Kind.NORMAL, "SMOKE_NORMAL", new String[]{"SMOKE", "SMOKE_NORMAL"}),
    LARGE_SMOKE(Kind.NORMAL, "SMOKE_LARGE", new String[]{"LARGE_SMOKE", "SMOKE_LARGE"}),
    CLOUD(Kind.NORMAL, "CLOUD", "CLOUD"),
    LAVA(Kind.NORMAL, "LAVA", "LAVA"),
    CRIT(Kind.NORMAL, "CRIT", "CRIT"),
    ENCHANTED_HIT(Kind.NORMAL, "CRIT_MAGIC", new String[]{"ENCHANTED_HIT", "CRIT_MAGIC"}),
    HEART(Kind.NORMAL, "HEART", "HEART"),
    PORTAL(Kind.NORMAL, "PORTAL", "PORTAL"),
    ENCHANT(Kind.NORMAL, "ENCHANTMENT_TABLE", new String[]{"ENCHANT", "ENCHANTMENT_TABLE"}),
    FIREWORK(Kind.NORMAL, "FIREWORKS_SPARK", new String[]{"FIREWORK", "FIREWORKS_SPARK"}),
    EXPLOSION(Kind.NORMAL, "EXPLOSION_NORMAL", new String[]{"EXPLOSION", "EXPLOSION_NORMAL"}),
    LARGE_EXPLOSION(Kind.NORMAL, "EXPLOSION_LARGE", new String[]{"EXPLOSION_EMITTER", "EXPLOSION_LARGE"}),
    HAPPY_VILLAGER(Kind.NORMAL, "VILLAGER_HAPPY", new String[]{"HAPPY_VILLAGER", "VILLAGER_HAPPY"}),
    ANGRY_VILLAGER(Kind.NORMAL, "VILLAGER_ANGRY", new String[]{"ANGRY_VILLAGER", "VILLAGER_ANGRY"}),
    WITCH(Kind.NORMAL, "SPELL_WITCH", new String[]{"WITCH", "SPELL_WITCH"}),
    INSTANT_EFFECT(Kind.NORMAL, "SPELL_INSTANT", new String[]{"INSTANT_EFFECT", "SPELL_INSTANT"}),
    EFFECT(Kind.NORMAL, "SPELL", new String[]{"EFFECT", "SPELL"}),
    SPLASH(Kind.NORMAL, "WATER_SPLASH", new String[]{"SPLASH", "WATER_SPLASH"}),
    BUBBLE(Kind.NORMAL, "WATER_BUBBLE", new String[]{"BUBBLE", "WATER_BUBBLE"}),
    DRIPPING_LAVA(Kind.NORMAL, "DRIP_LAVA", new String[]{"DRIPPING_LAVA", "DRIP_LAVA"}),
    DRIPPING_WATER(Kind.NORMAL, "DRIP_WATER", new String[]{"DRIPPING_WATER", "DRIP_WATER"}),
    SLIME(Kind.NORMAL, "SLIME", new String[]{"ITEM_SLIME", "SLIME"}),
    SNOWBALL(Kind.NORMAL, "SNOWBALL", new String[]{"ITEM_SNOWBALL", "SNOWBALL"}),
    FIREWORKS_TRAIL(Kind.NORMAL, "TOWN_AURA", new String[]{"MYCELIUM", "TOWN_AURA"}),

    // 1.9+ (en 1.8 caen al fallback)
    END_ROD(Kind.NORMAL, null, "END_ROD"),
    DRAGON_BREATH(Kind.NORMAL, null, "DRAGON_BREATH"),
    TOTEM(Kind.NORMAL, null, new String[]{"TOTEM_OF_UNDYING", "TOTEM"}),
    SOUL(Kind.NORMAL, null, "SOUL"),
    SOUL_FIRE_FLAME(Kind.NORMAL, null, "SOUL_FIRE_FLAME"),
    SNOWFLAKE(Kind.NORMAL, null, "SNOWFLAKE"),
    ELECTRIC_SPARK(Kind.NORMAL, null, "ELECTRIC_SPARK"),
    GLOW(Kind.NORMAL, null, "GLOW"),

    // coloreables
    DUST(Kind.COLORABLE, "REDSTONE", new String[]{"DUST", "REDSTONE"}),
    SPELL_MOB(Kind.COLORABLE, "SPELL_MOB", new String[]{"ENTITY_EFFECT", "SPELL_MOB"}),
    NOTE(Kind.COLORABLE, "NOTE", "NOTE"),

    // dependen de un material
    BLOCK(Kind.BLOCK, "BLOCK_CRACK", new String[]{"BLOCK", "BLOCK_CRACK"}),
    BLOCK_DUST(Kind.BLOCK, "BLOCK_DUST", new String[]{"BLOCK_CRUMBLE", "BLOCK_DUST"}),
    FALLING_DUST(Kind.BLOCK, null, "FALLING_DUST"),
    ITEM(Kind.ITEM, "ITEM_CRACK", new String[]{"ITEM", "ITEM_CRACK"});

    public enum Kind {
        /** Particula normal, sin data extra. */
        NORMAL,
        /** Acepta color (dust/redstone, spell_mob, note). */
        COLORABLE,
        /** Necesita un Material de bloque. */
        BLOCK,
        /** Necesita un ItemStack/Material de item. */
        ITEM
    }

    private final Kind kind;
    private final String legacyName;
    private final String[] modernNames;

    ParticleType(Kind kind, String legacyName, String modernName) {
        this(kind, legacyName, new String[]{modernName});
    }

    ParticleType(Kind kind, String legacyName, String[] modernNames) {
        this.kind = kind;
        this.legacyName = legacyName;
        this.modernNames = modernNames;
    }

    public Kind kind() {
        return kind;
    }

    public String legacyName() {
        return legacyName;
    }

    public String[] modernNames() {
        return modernNames;
    }

    public boolean colorable() {
        return kind == Kind.COLORABLE;
    }

    public boolean needsMaterial() {
        return kind == Kind.BLOCK || kind == Kind.ITEM;
    }
}
