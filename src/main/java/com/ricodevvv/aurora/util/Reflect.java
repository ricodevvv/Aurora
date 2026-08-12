package com.ricodevvv.aurora.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpers de reflection. Todo se cachea al inicializar para no pagar
 * lookups en caliente (los paquetes de particulas se mandan cada tick).
 */
public final class Reflect {

    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();

    private static Field connectionField;
    private static Method sendPacketMethod;

    private Reflect() {
    }

    /** Devuelve la primera clase que exista de la lista, o null. */
    public static Class<?> lookup(String... names) {
        String key = String.join("|", names);
        if (CLASS_CACHE.containsKey(key)) return CLASS_CACHE.get(key);
        Class<?> found = null;
        for (String name : names) {
            try {
                found = Class.forName(name);
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }
        CLASS_CACHE.put(key, found);
        return found;
    }

    /** Solo se usa en el backend legacy (1.8), donde el paquete siempre viene relocalizado. */
    public static Class<?> nms(String name) {
        String tag = ServerVersion.nmsTag();
        if (tag == null) return null;
        return lookup("net.minecraft.server." + tag + "." + name);
    }

    public static Class<?> obc(String name) {
        String tag = ServerVersion.nmsTag();
        if (tag == null) return lookup("org.bukkit.craftbukkit." + name);
        return lookup("org.bukkit.craftbukkit." + tag + "." + name, "org.bukkit.craftbukkit." + name);
    }

    public static Method method(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) return null;
        try {
            Method m = owner.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            try {
                Method m = owner.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }

    public static Constructor<?> constructor(Class<?> owner, Class<?>... params) {
        if (owner == null) return null;
        try {
            Constructor<?> c = owner.getDeclaredConstructor(params);
            c.setAccessible(true);
            return c;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    public static Object handle(Object craftObject) {
        try {
            Method m = craftObject.getClass().getMethod("getHandle");
            m.setAccessible(true);
            return m.invoke(craftObject);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Envio de paquete crudo. Solo lo usamos en 1.8, donde la firma es estable. */
    public static void sendPacket(Player player, Object packet) {
        try {
            Object entityPlayer = handle(player);
            if (entityPlayer == null) return;
            if (connectionField == null) {
                connectionField = entityPlayer.getClass().getField("playerConnection");
                connectionField.setAccessible(true);
            }
            Object connection = connectionField.get(entityPlayer);
            if (sendPacketMethod == null) {
                sendPacketMethod = connection.getClass().getMethod("sendPacket", nms("Packet"));
                sendPacketMethod.setAccessible(true);
            }
            sendPacketMethod.invoke(connection, packet);
        } catch (Throwable ignored) {
        }
    }

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
