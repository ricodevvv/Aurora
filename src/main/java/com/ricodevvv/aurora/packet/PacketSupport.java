package com.ricodevvv.aurora.packet;

import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deteccion de PacketEvents. Aurora NO lo requiere: si no esta, todo cae al
 * backend de entidades reales de Bukkit y nadie se entera.
 *
 * Importante: esta clase solo hace Class.forName. Las clases que de verdad
 * importan PacketEvents viven aparte (PacketEntity y compania) y nunca se
 * cargan si available() devuelve false, asi que no revientan con NoClassDefFound.
 */
public final class PacketSupport {

    private static Boolean available;

    /**
     * Contador de ids de entidad falsos.
     *
     * Arranca alto para no chocar con ids reales del servidor. Un servidor
     * normal no llega ni de cerca a 2^30 entidades en una sesion.
     */
    private static final AtomicInteger ENTITY_ID = new AtomicInteger(Integer.MAX_VALUE / 2);

    private PacketSupport() {
    }

    public static boolean available() {
        if (available != null) return available;
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            // Ademas de la clase, el API tiene que estar inicializado por el
            // plugin de PacketEvents o por tu propio shade.
            available = Bukkit.getPluginManager().getPlugin("packetevents") != null
                    || isApiInitialized();
        } catch (Throwable t) {
            available = false;
        }
        if (!available) {
            Bukkit.getLogger().info("[Aurora] PacketEvents no encontrado; usando entidades reales.");
        }
        return available;
    }

    private static boolean isApiInitialized() {
        try {
            Class<?> api = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            return api.getMethod("getAPI").invoke(null) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static int nextEntityId() {
        return ENTITY_ID.incrementAndGet();
    }

    /** Fuerza el backend, util para probar el camino de Bukkit en un server con PE. */
    public static void forceDisable() {
        available = false;
    }
}
