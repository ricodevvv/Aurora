package com.ricodevvv.aurora.animation;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/** Un unico task de servidor que mueve todas las animaciones activas. */
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

    public static AnimationManager init(Plugin plugin) {
        if (instance == null) {
            instance = new AnimationManager(plugin);
            instance.startTask();
        }
        return instance;
    }

    public static AnimationManager get() {
        if (instance == null) {
            throw new IllegalStateException("Aurora no esta inicializado. Llama Aurora.init(plugin) en onEnable().");
        }
        return instance;
    }

    private void startTask() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

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

    void register(Animation animation) {
        // Se difiere para no tocar la lista mientras la estamos recorriendo.
        if (ticking) pendingAdd.add(animation);
        else active.add(animation);
    }

    void unregister(Animation animation) {
        if (ticking) pendingRemove.add(animation);
        else active.remove(animation);
    }

    /** Corta todo. Llamalo en onDisable(). */
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

    public int activeCount() {
        return active.size();
    }

    public Plugin plugin() {
        return plugin;
    }
}
