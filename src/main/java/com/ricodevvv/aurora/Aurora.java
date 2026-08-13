package com.ricodevvv.aurora;

import com.ricodevvv.aurora.animation.AnimationManager;
import com.ricodevvv.aurora.cosmetic.CosmeticManager;
import com.ricodevvv.aurora.hologram.Hologram;
import com.ricodevvv.aurora.util.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Entry point for the library.
 *
 * <pre>{@code
 * @Override public void onEnable()  { Aurora.init(this); }
 * @Override public void onDisable() { Aurora.shutdown(); }
 * }</pre>
 *
 * <p>Shutting down is not optional. Aurora spawns real entities for holograms
 * and for some cosmetics; skipping {@link #shutdown()} leaves them in the world
 * with nothing tracking them.
 */
public final class Aurora {

    private static Plugin plugin;

    private Aurora() {
    }

    /**
     * Initialises the library and starts the shared animation task. Calling
     * this more than once has no effect.
     *
     * @param owner plugin used for scheduling and entity metadata
     */
    public static void init(Plugin owner) {
        if (plugin != null) return;
        plugin = owner;
        AnimationManager.init(owner);
        Bukkit.getLogger().info("[Aurora] Ready on " + ServerVersion.asString() + ".");
    }

    /**
     * Tears everything down: equipped cosmetics, running animations and
     * holograms, in that order.
     *
     * <p>The order matters. Cosmetics remove their own entities from
     * {@link com.ricodevvv.aurora.animation.Animation#onStop()}, so they have to
     * be released before the animation task that would drive that cleanup is
     * cancelled.
     */
    public static void shutdown() {
        if (plugin == null) return;
        CosmeticManager.shutdown();
        AnimationManager.get().stopAll();
        Hologram.despawnAll();
        plugin = null;
    }

    /**
     * @return the plugin that initialised Aurora, or {@code null} if it has not
     * been initialised
     */
    public static Plugin plugin() {
        return plugin;
    }

    /**
     * @return {@code true} if Aurora is initialised and ready to use
     */
    public static boolean isReady() {
        return plugin != null;
    }
}
