package com.ricodevvv.aurora.util;

import com.ricodevvv.aurora.Aurora;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/** Azucar para el scheduler, para no andar cargando el Plugin a todos lados. */
public final class Tasks {

    private Tasks() {
    }

    public static BukkitTask later(Runnable runnable, long ticks) {
        return Bukkit.getScheduler().runTaskLater(Aurora.plugin(), runnable, ticks);
    }

    public static BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(Aurora.plugin(), runnable);
    }

    public static BukkitTask async(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(Aurora.plugin(), runnable);
    }

    public static BukkitTask timer(Runnable runnable, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(Aurora.plugin(), runnable, delay, period);
    }
}
