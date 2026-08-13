package com.ricodevvv.aurora.util;

import com.ricodevvv.aurora.Aurora;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Cross-version entity configuration, from 1.8 through 1.21 and later.
 *
 * <p>Every method attempts the modern API first and degrades quietly. Nothing
 * here throws: if a setter does not exist on the running version the call is a
 * no-op, because "not available on 1.8" is an expected state rather than a
 * failure.
 *
 * <p>Entities created by Aurora should always be passed through
 * {@link #tag(Entity)}, which is what
 * {@link com.ricodevvv.aurora.cosmetic.CosmeticListener} uses to shield them
 * from damage on versions without {@code setInvulnerable}.
 */
public final class Entities {

    /** Metadata key marking an entity as owned by Aurora. */
    public static final String TAG = "aurora_entity";

    private Entities() {
    }

    /**
     * Resolves the first entity type that exists under any of the given names.
     *
     * @param names candidate type names, most preferred first
     * @return the first match, or {@link EntityType#ARMOR_STAND} if none exist
     */
    public static EntityType type(String... names) {
        for (String name : names) {
            try {
                return EntityType.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Try the next candidate.
            }
        }
        return EntityType.ARMOR_STAND;
    }

    /**
     * Marks an entity as belonging to Aurora.
     *
     * <p>Call this before any other configuration, so the protection listener
     * recognises the entity from its very first tick of existence.
     *
     * @param entity entity to mark
     */
    public static void tag(Entity entity) {
        try {
            entity.setMetadata(TAG, new FixedMetadataValue(Aurora.plugin(), true));
        } catch (Throwable ignored) {
            // Aurora not initialised; protection simply will not apply.
        }
    }

    /**
     * @param entity entity to check
     * @return {@code true} if the entity was created by Aurora
     */
    public static boolean isAurora(Entity entity) {
        try {
            return entity.hasMetadata(TAG);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Removes the entity's AI so it stops pathfinding.
     *
     * <p>{@code Mob#setAI} only exists from 1.9 onwards. On 1.8 the goal
     * selectors are cleared reflectively instead. If that fails it is not
     * fatal: Aurora teleports its entities every tick anyway, so they cannot
     * wander far.
     *
     * @param entity entity to pacify
     */
    public static void disableAI(Entity entity) {
        try {
            entity.getClass().getMethod("setAI", boolean.class).invoke(entity, false);
            return;
        } catch (Throwable ignored) {
            // Pre-1.9; fall through to the reflective path.
        }
        clearGoalSelectors(entity);
    }

    /**
     * Empties the NMS goal and target selectors, which is the 1.8 equivalent of
     * {@code setAI(false)}.
     *
     * @param entity entity to pacify
     */
    @SuppressWarnings("rawtypes")
    private static void clearGoalSelectors(Entity entity) {
        try {
            Object handle = Reflect.handle(entity);
            Class<?> insentient = Reflect.nms("EntityInsentient");
            if (handle == null || insentient == null || !insentient.isInstance(handle)) return;

            Class<?> selectorClass = Reflect.nms("PathfinderGoalSelector");
            if (selectorClass == null) return;

            for (String fieldName : new String[]{"goalSelector", "targetSelector"}) {
                Field field = insentient.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object selector = field.get(handle);

                for (Field inner : selectorClass.getDeclaredFields()) {
                    if (!Collection.class.isAssignableFrom(inner.getType())) continue;
                    inner.setAccessible(true);
                    Object goals = inner.get(selector);
                    if (goals instanceof Collection) ((Collection) goals).clear();
                }
            }
        } catch (Throwable ignored) {
            // Fork with renamed fields; the tick-by-tick teleport covers us.
        }
    }

    /**
     * Silences the entity. Only available from 1.9; 1.8 has no silence flag.
     *
     * @param entity entity to silence
     */
    public static void silence(Entity entity) {
        invokeQuietly(entity, "setSilent", boolean.class, true);
    }

    /**
     * Marks the entity as invulnerable.
     *
     * <p>{@code setInvulnerable} does not exist on 1.8, so on that version this
     * alone protects nothing. The real protection comes from
     * {@link com.ricodevvv.aurora.cosmetic.CosmeticListener}, which cancels
     * damage for anything carrying {@link #TAG}. The no-damage-ticks call also
     * suppresses the red flash and knockback even when damage is cancelled
     * elsewhere.
     *
     * @param entity entity to protect
     */
    public static void invulnerable(Entity entity) {
        invokeQuietly(entity, "setInvulnerable", boolean.class, true);
        invokeQuietly(entity, "setMaximumNoDamageTicks", int.class, Integer.MAX_VALUE);
    }

    /**
     * Stops the entity from pushing players around.
     *
     * @param entity entity to configure
     * @param value  whether it should collide
     */
    public static void collidable(Entity entity, boolean value) {
        invokeQuietly(entity, "setCollidable", boolean.class, value);
    }

    /**
     * Makes the entity invisible.
     *
     * <p>Three paths are needed, and all three matter:
     * <ul>
     *   <li>{@code Entity#setInvisible} only exists from 1.20.5.</li>
     *   <li>{@code ArmorStand#setVisible} only applies to armour stands.</li>
     *   <li>For any other mob between 1.8 and 1.20.4 the only option without
     *       NMS is an invisibility potion.</li>
     * </ul>
     * Omitting the third case is the classic bug in this technique: the hidden
     * helper mob behind a leashed cosmetic stays fully visible.
     *
     * @param entity entity to hide
     */
    public static void invisible(Entity entity) {
        if (invokeQuietly(entity, "setInvisible", boolean.class, true)) return;
        if (invokeQuietly(entity, "setVisible", boolean.class, false)) return;
        applyInvisibilityPotion(entity);
    }

    /**
     * Applies a permanent, particle-free invisibility effect.
     *
     * <p>{@code PotionEffect} grew from three to five constructor parameters
     * over the years, so they are tried newest first. The five-argument form is
     * the one that also disables the swirling particles, which would otherwise
     * give the hidden entity away on 1.9 and later.
     *
     * @param entity living entity to hide
     */
    private static void applyInvisibilityPotion(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        try {
            Class<?> typeClass = Reflect.lookup("org.bukkit.potion.PotionEffectType");
            Class<?> effectClass = Reflect.lookup("org.bukkit.potion.PotionEffect");
            if (typeClass == null || effectClass == null) return;

            Object invisibility = typeClass.getField("INVISIBILITY").get(null);
            Object effect = null;
            try {
                effect = effectClass
                        .getConstructor(typeClass, int.class, int.class, boolean.class, boolean.class)
                        .newInstance(invisibility, Integer.MAX_VALUE, 1, false, false);
            } catch (Throwable ignored) {
                // Older signature without the particles flag.
            }
            if (effect == null) {
                effect = effectClass
                        .getConstructor(typeClass, int.class, int.class, boolean.class)
                        .newInstance(invisibility, Integer.MAX_VALUE, 1, false);
            }
            LivingEntity living = (LivingEntity) entity;
            living.getClass().getMethod("addPotionEffect", effectClass).invoke(living, effect);
        } catch (Throwable ignored) {
            // Nothing else to try; the entity stays visible.
        }
    }

    /**
     * Turns the entity into a locked baby, which is the closest thing to a
     * scale reduction available before 1.20.5.
     *
     * @param entity entity to shrink
     */
    public static void baby(Entity entity) {
        if (!invokeQuietly(entity, "setBaby")) {
            invokeQuietly(entity, "setBaby", boolean.class, true);
        }
        invokeQuietly(entity, "setAgeLock", boolean.class, true);
    }

    /**
     * Scales the entity.
     *
     * <p>Backed by {@code Attribute.SCALE}, which only exists from 1.20.5. On
     * older versions there is no way to resize an entity at all: use
     * {@link #baby(Entity)} or render the cosmetic as a small armour stand
     * wearing a head instead.
     *
     * @param entity entity to scale
     * @param value  scale factor, {@code 1.0} being default size
     * @return {@code true} if the scale was applied
     */
    public static boolean scale(Entity entity, double value) {
        if (!ServerVersion.atLeast(20, 5)) return false;
        try {
            Class<?> attributeClass = Reflect.lookup("org.bukkit.attribute.Attribute");
            if (attributeClass == null) return false;

            Object scale = Reflect.enumValue(attributeClass, "SCALE");
            if (scale == null) {
                // From 1.21.3 Attribute is a registry rather than an enum.
                scale = attributeClass.getField("SCALE").get(null);
            }
            if (scale == null) return false;

            Object instance = entity.getClass()
                    .getMethod("getAttribute", attributeClass).invoke(entity, scale);
            if (instance == null) return false;

            instance.getClass().getMethod("setBaseValue", double.class).invoke(instance, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Leashes the entity to a player.
     *
     * <p>On 1.8 leads only attach to {@code Creature}, so armour stands and
     * display entities cannot be leashed and an invisible helper mob is
     * required as the anchor.
     *
     * @param entity entity to leash
     * @param holder player holding the lead
     * @return {@code true} if the leash was attached
     */
    public static boolean leashTo(Entity entity, Player holder) {
        if (!(entity instanceof LivingEntity)) return false;
        try {
            Method setter = entity.getClass().getMethod("setLeashHolder", Entity.class);
            return Boolean.TRUE.equals(setter.invoke(entity, holder));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Stops the entity from despawning when players move away.
     *
     * @param entity entity to keep loaded
     */
    public static void persist(Entity entity) {
        invokeQuietly(entity, "setRemoveWhenFarAway", boolean.class, false);
    }

    /**
     * Sets a visible name tag, translating {@code &} colour codes.
     *
     * @param entity entity to name
     * @param name   name to show; {@code null} or empty hides the tag
     */
    public static void nameTag(Entity entity, String name) {
        if (name == null || name.isEmpty()) {
            entity.setCustomNameVisible(false);
            return;
        }
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', name));
        entity.setCustomNameVisible(true);
    }

    /**
     * Computes the yaw needed to look from one location towards another.
     *
     * @param from viewing position
     * @param to   position being looked at
     * @return the yaw in degrees, in Minecraft's convention
     */
    public static float yawTowards(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
    }

    /**
     * Invokes a setter if it exists on this version.
     *
     * @param target    object to call on
     * @param name      method name
     * @param paramType parameter type, or none for a no-argument method
     * @param value     argument value, if any
     * @return {@code true} if the method existed and was invoked
     */
    private static boolean invokeQuietly(Object target, String name,
                                         Class<?> paramType, Object value) {
        try {
            target.getClass().getMethod(name, paramType).invoke(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Invokes a no-argument method if it exists on this version.
     *
     * @param target object to call on
     * @param name   method name
     * @return {@code true} if the method existed and was invoked
     */
    private static boolean invokeQuietly(Object target, String name) {
        try {
            target.getClass().getMethod(name).invoke(target);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
