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

    /** Etiqueta de metadata que marca una entidad como propiedad de Aurora. */
    public static final String TAG = "aurora_entity";

    /**
     * Marca la entidad como de Aurora. El listener de proteccion usa esto para
     * cancelarle dano, fuego y targeting de mobs.
     */
    public static void tag(Entity entity) {
        try {
            entity.setMetadata(TAG, new org.bukkit.metadata.FixedMetadataValue(
                    com.ricodevvv.aurora.Aurora.plugin(), true));
        } catch (Throwable ignored) {
        }
    }

    /** @return true si la entidad fue creada por Aurora */
    public static boolean isAurora(Entity entity) {
        try {
            return entity.hasMetadata(TAG);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Silencia la entidad. Solo 1.9+; en 1.8 no hay flag de silencio. */
    public static void silence(Entity entity) {
        try {
            entity.getClass().getMethod("setSilent", boolean.class).invoke(entity, true);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Marca la entidad como invulnerable.
     *
     * <p>{@code setInvulnerable} no existe en 1.8, asi que ahi esto no hace
     * nada por si solo: la proteccion real la da
     * {@link com.ricodevvv.aurora.cosmetic.CosmeticListener}, que cancela el
     * dano de cualquier entidad marcada con {@link #tag(Entity)}.
     *
     * @param entity entidad a proteger
     */
    public static void invulnerable(Entity entity) {
        try {
            entity.getClass().getMethod("setInvulnerable", boolean.class).invoke(entity, true);
        } catch (Throwable ignored) {
        }
        try {
            // Evita el parpadeo rojo y el knockback aunque el dano se cancele.
            entity.getClass().getMethod("setMaximumNoDamageTicks", int.class)
                    .invoke(entity, Integer.MAX_VALUE);
        } catch (Throwable ignored) {
        }
    }

    public static void collidable(Entity entity, boolean value) {
        try {
            entity.getClass().getMethod("setCollidable", boolean.class).invoke(entity, value);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Vuelve invisible la entidad.
     *
     * <p>Hay tres caminos y hacen falta los tres:
     * <ul>
     *   <li>{@code Entity#setInvisible} existe recien desde 1.20.5.</li>
     *   <li>{@code ArmorStand#setVisible} solo aplica a armor stands.</li>
     *   <li>Para cualquier otro mob entre 1.8 y 1.20.4 el UNICO camino sin NMS
     *       es una pocion de invisibilidad infinita.</li>
     * </ul>
     * Sin el tercer caso el ancla de correa de los globos se ve, que es
     * justo el bug clasico de esta tecnica.
     *
     * @param entity entidad a ocultar
     */
    public static void invisible(Entity entity) {
        try {
            entity.getClass().getMethod("setInvisible", boolean.class).invoke(entity, true);
            return;
        } catch (Throwable ignored) {
        }
        try {
            entity.getClass().getMethod("setVisible", boolean.class).invoke(entity, false);
            return;
        } catch (Throwable ignored) {
        }
        invisibilityPotion(entity);
    }

    /**
     * Pocion de invisibilidad permanente y sin particulas.
     *
     * <p>El constructor de {@code PotionEffect} fue creciendo (3, 4 y 5
     * argumentos segun la version), asi que se prueban de mas nuevo a mas
     * viejo. Con el de 5 argumentos ademas se apagan las particulas, que es
     * lo que delataria al ancla en 1.9+.
     *
     * @param entity entidad viva a ocultar
     */
    private static void invisibilityPotion(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        try {
            Class<?> effectType = Reflect.lookup("org.bukkit.potion.PotionEffectType");
            Object invisibility = effectType.getField("INVISIBILITY").get(null);
            Class<?> effect = Reflect.lookup("org.bukkit.potion.PotionEffect");

            Object instance = null;
            try {
                instance = effect.getConstructor(effectType, int.class, int.class,
                                boolean.class, boolean.class)
                        .newInstance(invisibility, Integer.MAX_VALUE, 1, false, false);
            } catch (Throwable ignored) {
            }
            if (instance == null) {
                instance = effect.getConstructor(effectType, int.class, int.class, boolean.class)
                        .newInstance(invisibility, Integer.MAX_VALUE, 1, false);
            }
            LivingEntity living = (LivingEntity) entity;
            living.getClass().getMethod("addPotionEffect", effect).invoke(living, instance);
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
