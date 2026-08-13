package com.ricodevvv.aurora.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection helpers with caching.
 *
 * <p>Every lookup is memoised, because these are called from code that runs
 * every tick and an uncached {@code Class.forName} is far from free.
 *
 * <p>All methods return {@code null} rather than throwing when a member does
 * not exist on the running version. That is deliberate: "this API is not
 * available here" is the normal case for a library spanning 1.8 to 1.21, not an
 * error, and callers are expected to branch on it.
 */
public final class Reflect {

    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();

    private Reflect() {
    }

    /**
     * Resolves the first class that exists among the given names.
     *
     * @param names fully qualified class names, most preferred first
     * @return the first class found, or {@code null} if none exist
     */
    public static Class<?> lookup(String... names) {
        String key = String.join("|", names);
        if (CLASS_CACHE.containsKey(key)) return CLASS_CACHE.get(key);

        Class<?> found = null;
        for (String name : names) {
            try {
                found = Class.forName(name);
                break;
            } catch (ClassNotFoundException ignored) {
                // Try the next candidate.
            }
        }
        CLASS_CACHE.put(key, found);
        return found;
    }

    /**
     * Resolves a class from the relocated NMS package.
     *
     * @param name simple class name, such as {@code EntityInsentient}
     * @return the class, or {@code null} if the package is not relocated
     * (Paper 1.20.5 and later) or the class does not exist
     */
    public static Class<?> nms(String name) {
        String tag = ServerVersion.nmsTag();
        if (tag == null) return null;
        return lookup("net.minecraft.server." + tag + "." + name);
    }

    /**
     * Resolves a class from the CraftBukkit package, trying both the relocated
     * and non-relocated layouts.
     *
     * @param name class name relative to the CraftBukkit root
     * @return the class, or {@code null} if not found
     */
    public static Class<?> obc(String name) {
        String tag = ServerVersion.nmsTag();
        if (tag == null) return lookup("org.bukkit.craftbukkit." + name);
        return lookup("org.bukkit.craftbukkit." + tag + "." + name,
                "org.bukkit.craftbukkit." + name);
    }

    /**
     * Resolves a method, checking public methods first and then declared ones.
     *
     * @param owner  class that declares it
     * @param name   method name
     * @param params parameter types
     * @return the accessible method, or {@code null} if not found
     */
    public static Method method(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) return null;
        try {
            Method method = owner.getMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            try {
                Method method = owner.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException alsoMissing) {
                return null;
            }
        }
    }

    /**
     * Resolves a constructor.
     *
     * @param owner  class to construct
     * @param params parameter types
     * @return the accessible constructor, or {@code null} if not found
     */
    public static Constructor<?> constructor(Class<?> owner, Class<?>... params) {
        if (owner == null) return null;
        try {
            Constructor<?> constructor = owner.getDeclaredConstructor(params);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    /**
     * Finds the first declared field of a given type.
     *
     * <p>Matching by type rather than name is what lets this survive obfuscated
     * or renamed NMS fields across versions.
     *
     * @param owner class to search
     * @param type  field type to match
     * @return the accessible field, or {@code null} if none matches
     */
    public static Field fieldByType(Class<?> owner, Class<?> type) {
        if (owner == null || type == null) return null;
        for (Field field : owner.getDeclaredFields()) {
            if (field.getType().equals(type)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    /**
     * Calls {@code getHandle()} on a CraftBukkit wrapper to reach the
     * underlying NMS object.
     *
     * @param craftObject a CraftBukkit wrapper such as a CraftPlayer
     * @return the NMS handle, or {@code null} if unavailable
     */
    public static Object handle(Object craftObject) {
        try {
            Method getHandle = craftObject.getClass().getMethod("getHandle");
            getHandle.setAccessible(true);
            return getHandle.invoke(craftObject);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Resolves an enum constant by name without a compile-time reference.
     *
     * @param enumClass the enum class
     * @param name      constant name
     * @return the constant, or {@code null} if the class is not an enum or the
     * constant does not exist on this version
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object enumValue(Class<?> enumClass, String name) {
        if (enumClass == null || !enumClass.isEnum()) return null;
        try {
            return Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
