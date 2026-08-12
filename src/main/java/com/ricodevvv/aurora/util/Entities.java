package com.ricodevvv.aurora.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Compatibilidad de entidades 1.8 - 1.21.
 *
 * Cada metodo intenta la ruta moderna y degrada sin romper. Nunca lanza:
 * si algo no existe en la version, simplemente no pasa nada.
 */
public final class Entities {

    private Entities() {
    }

    /** Nombre del EntityType, tolerando renombres entre versiones. */
    public static EntityType type(String... names) {
        for (String name : names) {
            try {
                return EntityType.valueOf(name);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return EntityType.ARMOR_STAND;
    }

    /**
     * Quita la IA. En 1.9+ es setAI(false); en 1.8 no existe, asi que
     * limpiamos los goal selectors por reflection sobre NMS.
     *
     * Si falla en 1.8 no importa demasiado: los cosmeticos se teletransportan
     * cada tick de todos modos, asi que la entidad no se va a ningun lado.
     */
    public static void disableAI(Entity entity) {
        try {
            Method setAI = entity.getClass().getMethod("setAI", boolean.class);
            setAI.invoke(entity, false);
            return;
        } catch (Throwable ignored) {
        }
        clearGoalSelectors(entity);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void clearGoalSelectors(Entity entity) {
        try {
            Object handle = Reflect.handle(entity);
            if (handle == null) return;
            Class<?> insentient = Reflect.nms("EntityInsentient");
            if (insentient == null || !insentient.isInstance(handle)) return;

            Class<?> selectorClass = Reflect.nms("PathfinderGoalSelector");
            if (selectorClass == null) return;

            for (String fieldName : new String[]{"goalSelector", "targetSelector"}) {
                Field field = insentient.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object selector = field.get(handle);
                // El set de goals es el primer campo de tipo Set/UnsafeList del selector
                for (Field inner : selectorClass.getDeclaredFields()) {
                    if (!java.util.Collection.class.isAssignableFrom(inner.getType())) continue;
                    inner.setAccessible(true);
                    Object collection = inner.get(selector);
                    if (collection instanceof java.util.Collection) {
                        ((java.util.Collection) collection).clear();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** Silencia la entidad. Solo 1.9+; en 1.8 no hay flag de silencio. */
    public static void silence(Entity entity) {
        try {
            entity.getClass().getMethod("setSilent", boolean.class).invoke(entity, true);
        } catch (Throwable ignored) {
        }
    }

    public static void invulnerable(Entity entity) {
        try {
            entity.getClass().getMethod("setInvulnerable", boolean.class).invoke(entity, true);
        } catch (Throwable ignored) {
        }
    }

    public static void collidable(Entity entity, boolean value) {
        try {
            entity.getClass().getMethod("setCollidable", boolean.class).invoke(entity, value);
        } catch (Throwable ignored) {
        }
    }

    public static void invisible(Entity entity) {
        // ArmorStand#setVisible en 1.8; Entity#setInvisible desde 1.20.5
        try {
            entity.getClass().getMethod("setInvisible", boolean.class).invoke(entity, true);
            return;
        } catch (Throwable ignored) {
        }
        try {
            entity.getClass().getMethod("setVisible", boolean.class).invoke(entity, false);
        } catch (Throwable ignored) {
        }
    }

    /** Bebe, si la entidad tiene edad. Es lo mas cercano a "mini" que hay en 1.8. */
    public static void baby(Entity entity) {
        try {
            entity.getClass().getMethod("setBaby").invoke(entity);
        } catch (Throwable first) {
            try {
                entity.getClass().getMethod("setBaby", boolean.class).invoke(entity, true);
            } catch (Throwable ignored) {
            }
        }
        try {
            entity.getClass().getMethod("setAgeLock", boolean.class).invoke(entity, true);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Escala real de la entidad. SOLO 1.20.5+ (Attribute.SCALE).
     *
     * @return true si se pudo aplicar. Si devuelve false y quieres algo
     * "mini" en versiones viejas, usa RenderMode.HEAD o baby().
     */
    public static boolean scale(Entity entity, double value) {
        if (!ServerVersion.atLeast(20, 5)) return false;
        try {
            Class<?> attributeClass = Reflect.lookup("org.bukkit.attribute.Attribute");
            Object scaleAttribute = Reflect.enumValue(attributeClass, "SCALE");
            if (scaleAttribute == null) {
                // Desde 1.21.3 Attribute dejo de ser enum y es un registro
                Field field = attributeClass.getField("SCALE");
                scaleAttribute = field.get(null);
            }
            if (scaleAttribute == null) return false;

            Method getAttribute = entity.getClass().getMethod("getAttribute", attributeClass);
            Object instance = getAttribute.invoke(entity, scaleAttribute);
            if (instance == null) return false;

            instance.getClass().getMethod("setBaseValue", double.class).invoke(instance, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Ata la entidad a un jugador con correa. En 1.8 solo funciona sobre Creature. */
    public static boolean leashTo(Entity entity, Player holder) {
        try {
            if (!(entity instanceof LivingEntity)) return false;
            Method setLeashHolder = entity.getClass().getMethod("setLeashHolder", Entity.class);
            return Boolean.TRUE.equals(setLeashHolder.invoke(entity, holder));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Evita que la entidad se despawnee al alejarse el jugador. */
    public static void persist(Entity entity) {
        try {
            entity.getClass().getMethod("setRemoveWhenFarAway", boolean.class).invoke(entity, false);
        } catch (Throwable ignored) {
        }
    }

    public static void nameTag(Entity entity, String name) {
        if (name == null || name.isEmpty()) {
            entity.setCustomNameVisible(false);
            return;
        }
        entity.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
        entity.setCustomNameVisible(true);
    }

    /** Yaw en grados desde "from" mirando hacia "to". */
    public static float yawTowards(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
    }
}
