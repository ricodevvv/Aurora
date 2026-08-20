package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.util.Reflect;
import com.ricodevvv.aurora.util.ServerVersion;

/**
 * Aurora's own particle identities, resolved to whatever the running server
 * calls them.
 *
 * <p>Bukkit's own {@code Particle} enum cannot be used directly, for the same
 * reason {@link com.ricodevvv.aurora.util.Sounds} does not use {@code Sound}:
 * it does not exist on 1.8 at all, and its constants were renamed wholesale in
 * 1.20.5 ({@code REDSTONE} became {@code DUST}, {@code SMOKE_LARGE} became
 * {@code LARGE_SMOKE}, {@code EXPLOSION_NORMAL} became {@code POOF}). Any
 * library that references the enum breaks at one of those points, and any
 * library whose particle layer merely <em>mentions</em> the class fails to load
 * on 1.8 with {@code NoClassDefFoundError}.
 *
 * <p>So every particle here is stored as names — the modern one, the 1.9-to-1.20.4
 * one, and the 1.8 {@code EnumParticle} constant — and the right one is picked
 * once at class-load time. Particles that simply did not exist yet declare a
 * fallback and quietly become it: asking for a soul fire flame on 1.8 gets a
 * flame rather than nothing.
 *
 * <p>Constants are ordered so that a fallback is always declared before the
 * particles that fall back to it.
 */
public enum ParticleType {

    // ------------------------------------------------- always available (1.8+)

    /** Coloured dust, the workhorse of custom effects. */
    DUST(Payload.DUST, "REDSTONE", "DUST", "REDSTONE"),
    FLAME(Payload.NONE, "FLAME", "FLAME"),
    CLOUD(Payload.NONE, "CLOUD", "CLOUD"),
    SMOKE(Payload.NONE, "SMOKE_NORMAL", "SMOKE", "SMOKE_NORMAL"),
    LARGE_SMOKE(Payload.NONE, "SMOKE_LARGE", "LARGE_SMOKE", "SMOKE_LARGE"),
    CRIT(Payload.NONE, "CRIT", "CRIT"),
    ENCHANTED_HIT(Payload.NONE, "CRIT_MAGIC", "ENCHANTED_HIT", "CRIT_MAGIC"),
    HEART(Payload.NONE, "HEART", "HEART"),
    NOTE(Payload.NOTE, "NOTE", "NOTE"),
    PORTAL(Payload.NONE, "PORTAL", "PORTAL"),
    ENCHANT(Payload.NONE, "ENCHANTMENT_TABLE", "ENCHANT", "ENCHANTMENT_TABLE"),
    LAVA(Payload.NONE, "LAVA", "LAVA"),
    SPLASH(Payload.NONE, "WATER_SPLASH", "SPLASH", "WATER_SPLASH"),
    BUBBLE(Payload.NONE, "WATER_BUBBLE", "BUBBLE", "WATER_BUBBLE"),
    DRIPPING_WATER(Payload.NONE, "DRIP_WATER", "DRIPPING_WATER", "DRIP_WATER"),
    DRIPPING_LAVA(Payload.NONE, "DRIP_LAVA", "DRIPPING_LAVA", "DRIP_LAVA"),
    HAPPY_VILLAGER(Payload.NONE, "VILLAGER_HAPPY", "HAPPY_VILLAGER", "VILLAGER_HAPPY"),
    ANGRY_VILLAGER(Payload.NONE, "VILLAGER_ANGRY", "ANGRY_VILLAGER", "VILLAGER_ANGRY"),
    WITCH(Payload.NONE, "SPELL_WITCH", "WITCH", "SPELL_WITCH"),
    EFFECT(Payload.NONE, "SPELL", "EFFECT", "SPELL"),
    INSTANT_EFFECT(Payload.NONE, "SPELL_INSTANT", "INSTANT_EFFECT", "SPELL_INSTANT"),
    ENTITY_EFFECT(Payload.DUST, "SPELL_MOB", "ENTITY_EFFECT", "SPELL_MOB"),
    FIREWORK(Payload.NONE, "FIREWORKS_SPARK", "FIREWORK", "FIREWORKS_SPARK"),
    POOF(Payload.NONE, "EXPLOSION_NORMAL", "POOF", "EXPLOSION_NORMAL"),
    EXPLOSION(Payload.NONE, "EXPLOSION_LARGE", "EXPLOSION", "EXPLOSION_LARGE"),
    ITEM_SNOWBALL(Payload.NONE, "SNOWBALL", "ITEM_SNOWBALL", "SNOWBALL"),
    ITEM_SLIME(Payload.NONE, "SLIME", "ITEM_SLIME", "SLIME"),
    BLOCK(Payload.BLOCK, "BLOCK_CRACK", "BLOCK", "BLOCK_CRACK"),
    ITEM(Payload.ITEM, "ITEM_CRACK", "ITEM", "ITEM_CRACK"),

    // ------------------------------------------------------ added after 1.8

    /** 1.10+. Falls back to plain dust. */
    FALLING_DUST(DUST, Payload.BLOCK, null, "FALLING_DUST"),
    /** 1.9+. A thin streak that holds its shape; falls back to firework sparks. */
    END_ROD(FIREWORK, Payload.NONE, null, "END_ROD"),
    /** 1.9+. Falls back to large smoke. */
    DRAGON_BREATH(LARGE_SMOKE, Payload.NONE, null, "DRAGON_BREATH"),
    /** 1.11+. Falls back to happy villager sparkles. */
    TOTEM_OF_UNDYING(HAPPY_VILLAGER, Payload.NONE, null, "TOTEM_OF_UNDYING", "TOTEM"),
    /** 1.16+. Falls back to flame. */
    SOUL_FIRE_FLAME(FLAME, Payload.NONE, null, "SOUL_FIRE_FLAME"),
    /** 1.16+. Falls back to large smoke. */
    SOUL(LARGE_SMOKE, Payload.NONE, null, "SOUL"),
    /** 1.17+. Falls back to cloud. */
    SNOWFLAKE(CLOUD, Payload.NONE, null, "SNOWFLAKE"),
    /** 1.17+. Falls back to critical hits. */
    ELECTRIC_SPARK(CRIT, Payload.NONE, null, "ELECTRIC_SPARK"),
    /** 1.17+. Dust that fades to a second colour; falls back to plain dust. */
    DUST_COLOR_TRANSITION(DUST, Payload.TRANSITION, null, "DUST_COLOR_TRANSITION"),
    /** 1.17+. Falls back to happy villager sparkles. */
    GLOW(HAPPY_VILLAGER, Payload.NONE, null, "GLOW"),
    /** 1.20+. Falls back to plain dust. */
    CHERRY_LEAVES(DUST, Payload.NONE, null, "CHERRY_LEAVES");

    /**
     * What extra data the particle needs, which is the part that differs most
     * between versions.
     */
    public enum Payload {
        /** Nothing beyond position, count, offset and speed. */
        NONE,
        /** A colour and a size. */
        DUST,
        /** Two colours and a size. */
        TRANSITION,
        /** A note index, which is really a colour with twenty-five steps. */
        NOTE,
        /** A block to texture the particle with. */
        BLOCK,
        /** An item to shatter. */
        ITEM
    }

    private final ParticleType fallback;
    private final Payload payload;
    private final String legacyName;
    private final String[] modernNames;

    /** The resolved Bukkit or NMS constant, or {@code null} if unavailable. */
    private Object handle;

    /** This particle, or the first fallback that resolved to something. */
    private ParticleType effective;

    ParticleType(Payload payload, String legacyName, String... modernNames) {
        this(null, payload, legacyName, modernNames);
    }

    ParticleType(ParticleType fallback, Payload payload, String legacyName, String... modernNames) {
        this.fallback = fallback;
        this.payload = payload;
        this.legacyName = legacyName;
        this.modernNames = modernNames;
    }

    static {
        for (ParticleType type : values()) {
            type.handle = type.resolveHandle();
        }
        for (ParticleType type : values()) {
            ParticleType current = type;
            // Bounded rather than recursive: a mistake in the table becomes a
            // missing particle, never a stack overflow in a tick loop.
            for (int guard = 0; guard < values().length && current.handle == null; guard++) {
                if (current.fallback == null) break;
                current = current.fallback;
            }
            type.effective = current.handle != null ? current : type;
        }
    }

    /**
     * Looks the particle up under whichever name this server uses.
     *
     * @return the server-side constant, or {@code null} if it has none
     */
    private Object resolveHandle() {
        if (ServerVersion.isLegacy()) {
            return legacyName == null ? null
                    : Reflect.enumValue(Reflect.nms("EnumParticle"), legacyName);
        }

        Class<?> particle = Reflect.lookup("org.bukkit.Particle");
        if (particle == null) return null;

        for (String name : modernNames) {
            Object found = Reflect.enumValue(particle, name);
            if (found == null) found = fromRegistry(name);
            if (found != null) return found;
        }
        // 1.20.5 renamed the constants; older servers still answer to the name
        // the particle had before that.
        return legacyName == null ? null : Reflect.enumValue(particle, legacyName);
    }

    /**
     * Registry lookup, for the day {@code Particle} stops being an enum the way
     * {@code Sound} did in 1.21.3.
     *
     * @param name constant name, such as {@code LARGE_SMOKE}
     * @return the registry entry, or {@code null}
     */
    private static Object fromRegistry(String name) {
        try {
            Class<?> registryClass = Reflect.lookup("org.bukkit.Registry");
            Class<?> keyClass = Reflect.lookup("org.bukkit.NamespacedKey");
            if (registryClass == null || keyClass == null) return null;

            Object registry = registryClass.getField("PARTICLE_TYPE").get(null);
            Object key = keyClass.getMethod("minecraft", String.class)
                    .invoke(null, name.toLowerCase(java.util.Locale.ROOT));
            return registryClass.getMethod("get", keyClass).invoke(registry, key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * @return {@code true} if this exact particle exists on the running server
     */
    public boolean isSupported() {
        return handle != null;
    }

    /**
     * The particle that will actually be spawned: this one, or its first
     * available fallback.
     *
     * @return the effective particle
     */
    public ParticleType effective() {
        return effective;
    }

    /**
     * @return what extra data this particle needs
     */
    public Payload payload() {
        return effective.payload;
    }

    /**
     * @return the server-side constant, or {@code null} if nothing resolved
     */
    Object handle() {
        return effective.handle;
    }
}
