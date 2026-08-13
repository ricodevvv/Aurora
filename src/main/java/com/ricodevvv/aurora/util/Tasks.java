package com.ricodevvv.aurora.util;

import com.ricodevvv.aurora.Aurora;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Thin wrapper over the Bukkit scheduler that resolves the owning plugin from
 * {@link Aurora}, so callers do not have to thread a {@code Plugin} reference
 * through every class.
 *
 * <p>For anything that repeats every tick, prefer
 * {@link com.ricodevvv.aurora.animation.Animation}: it shares a single server
 * task across every running effect instead of creating one per effect.
 */
public final class Tasks {

    private Tasks() {
    }

    /**
     * Runs once after a delay, on the main thread.
     *
     * @param runnable work to run
     * @param ticks    delay in ticks; {@code 20} is one second
     * @return the scheduled task
     */
    public static BukkitTask later(Runnable runnable, long ticks) {
        return Bukkit.getScheduler().runTaskLater(Aurora.plugin(), runnable, ticks);
    }

    /**
     * Runs on the main thread at the next tick. Use this to hop back from an
     * async task before touching the Bukkit API.
     *
     * @param runnable work to run
     * @return the scheduled task
     */
    public static BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(Aurora.plugin(), runnable);
    }

    /**
     * Runs off the main thread. Do not touch the Bukkit API from inside it.
     *
     * @param runnable work to run
     * @return the scheduled task
     */
    public static BukkitTask async(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(Aurora.plugin(), runnable);
    }

    /**
     * Runs repeatedly on the main thread.
     *
     * @param runnable work to run
     * @param delay    initial delay in ticks
     * @param period   period between runs, in ticks
     * @return the scheduled task
     */
    public static BukkitTask timer(Runnable runnable, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(Aurora.plugin(), runnable, delay, period);
    }
}
