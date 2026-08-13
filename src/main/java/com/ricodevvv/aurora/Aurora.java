package com.ricodevvv.aurora;

import com.ricodevvv.aurora.animation.AnimationManager;
import com.ricodevvv.aurora.cosmetic.CosmeticManager;
import com.ricodevvv.aurora.hologram.Hologram;
import com.ricodevvv.aurora.util.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Punto de entrada de la libreria.
 *
 * En tu onEnable():   Aurora.init(this);
 * En tu onDisable():  Aurora.shutdown();
 */
public final class Aurora {

    private static Plugin plugin;

    private Aurora() {
    }

    public static void init(Plugin owner) {
        if (plugin != null) return;
        plugin = owner;
        AnimationManager.init(owner);
        Bukkit.getLogger().info("[Aurora] Ready on 1." + ServerVersion.minor() + ".");
    }

    public static void shutdown() {
        if (plugin == null) return;
        // Orden importante: los cosmeticos primero, para que cada uno borre sus
        // entidades antes de que se corte el task global.
        CosmeticManager.shutdown();
        AnimationManager.get().stopAll();
        Hologram.despawnAll();
        plugin = null;
    }

    public static Plugin plugin() {
        return plugin;
    }
}
