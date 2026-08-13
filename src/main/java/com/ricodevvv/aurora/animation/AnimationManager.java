package com.ricodevvv.aurora.animation;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives every running {@link Animation} from a single repeating server task.
 *
 * <p>Registrations and removals that happen while the manager is iterating are
 * buffered and applied afterwards. Animations routinely stop themselves from
 * inside their own update, so mutating the list during iteration would
 * otherwise throw a {@link java.util.ConcurrentModificationException}.
 */
public final class AnimationManager {

    private static AnimationManager instance;

    private final Plugin plugin;
    private final List<Animation> active = new ArrayList<>();
    private final List<Animation> pendingAdd = new ArrayList<>();
    private final List<Animation> pendingRemove = new ArrayList<>();

    private BukkitTask task;
    private boolean ticking;

    private AnimationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the manager and starts its task. Called by
     * {@link com.ricodevvv.aurora.Aurora#init(Plugin)}; calling it again
     * returns the existing instance.
     *
     * @param plugin plugin that owns the scheduler task
     * @return the manager
     */
    public static AnimationManager init(Plugin plugin) {
        if (instance == null) {
            instance = new AnimationManager(plugin);
            instance.start();
        }
        return instance;
    }

    /**
     * @return the active manager
     * @throws IllegalStateException if Aurora has not been initialised
     */
    public static AnimationManager get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "Aurora is not initialised. Call Aurora.init(plugin) in onEnable().");
        }
        return instance;
    }

    private void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    /**
     * Ticks every active animation, then applies buffered registrations.
     */
    private void tickAll() {
        ticking = true;
        for (int i = 0; i < active.size(); i++) {
            active.get(i).tick();
        }
        ticking = false;

        if (!pendingRemove.isEmpty()) {
            active.removeAll(pendingRemove);
            pendingRemove.clear();
        }
        if (!pendingAdd.isEmpty()) {
            active.addAll(pendingAdd);
            pendingAdd.clear();
        }
    }

    /**
     * Adds an animation to the tick loop.
     *
     * @param animation animation to run
     */
    void register(Animation animation) {
        if (ticking) pendingAdd.add(animation);
        else active.add(animation);
    }

    /**
     * Removes an animation from the tick loop.
     *
     * @param animation animation to drop
     */
    void unregister(Animation animation) {
        if (ticking) pendingRemove.add(animation);
        else active.remove(animation);
    }

    /**
     * Stops every animation and cancels the task. Called during shutdown.
     */
    public void stopAll() {
        for (Animation animation : new ArrayList<>(active)) {
            animation.stop();
        }
        active.clear();
        pendingAdd.clear();
        pendingRemove.clear();
        if (task != null) task.cancel();
        instance = null;
    }

    /**
     * @return how many animations are currently running
     */
    public int activeCount() {
        return active.size();
    }

    /**
     * @return the plugin that owns the tick task
     */
    public Plugin plugin() {
        return plugin;
    }
}
