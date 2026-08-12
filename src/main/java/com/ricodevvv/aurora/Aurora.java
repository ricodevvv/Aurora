package com.ricodevvv.aurora;

import com.ricodevvv.aurora.animation.AnimationManager;
import com.ricodevvv.aurora.cosmetic.CosmeticManager;
import com.ricodevvv.aurora.hologram.Hologram;
import com.ricodevvv.aurora.particle.ParticleBackend;
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
    private static ParticleBackend backend;

    private Aurora() {
    }

    public static void init(Plugin owner) {
        if (plugin != null) return;
        plugin = owner;
        backend = ParticleBackend.create();
        AnimationManager.init(owner);
        Bukkit.getLogger().info("[Aurora] Listo en " + ServerVersion.minor() + " usando "
                + backend.getClass().getSimpleName() + ".");
    }

    public static void shutdown() {
        if (plugin == null) return;
        // Orden importante: los cosmeticos primero, para que cada uno borre sus
        // entidades antes de que se corte el task global.
        CosmeticManager.shutdown();
        AnimationManager.get().stopAll();
        Hologram.despawnAll();
        backend = null;
        plugin = null;
    }

    public static ParticleBackend backend() {
        if (backend == null) {
            throw new IllegalStateException("Aurora no esta inicializado. Llama Aurora.init(plugin) en onEnable().");
        }
        return backend;
    }

    public static Plugin plugin() {
        return plugin;
    }
}
