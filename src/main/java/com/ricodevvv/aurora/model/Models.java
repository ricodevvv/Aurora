package com.ricodevvv.aurora.model;

import com.ricodevvv.aurora.packet.PacketSupport;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Fabrica de modelos. Elige backend de paquetes si PacketEvents esta presente,
 * y si no cae a entidades reales.
 */
public final class Models {

    private static Boolean forcedPackets;

    private Models() {
    }

    /**
     * Armor stand con casco.
     *
     * @param viewers quien lo ve. Solo aplica al backend de paquetes; con
     *                entidades reales lo ve todo el mundo (limitacion real,
     *                no se puede ocultar una entidad de Bukkit sin paquetes).
     */
    public static Model armorStand(boolean small, Supplier<Collection<? extends Player>> viewers) {
        if (usePackets()) {
            return new PacketArmorStandModel(small, viewers);
        }
        return new BukkitArmorStandModel(small);
    }

    /** Armor stand visible para todos. */
    public static Model armorStand(boolean small) {
        return armorStand(small, null);
    }

    public static boolean usePackets() {
        if (forcedPackets != null) return forcedPackets;
        return PacketSupport.available();
    }

    /** Fuerza el backend a mano. null = automatico. */
    public static void usePackets(Boolean value) {
        forcedPackets = value;
    }
}
