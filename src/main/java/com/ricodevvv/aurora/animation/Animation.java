package com.ricodevvv.aurora.animation;

/**
 * Base de toda animacion. No crea su propio BukkitRunnable: se registra en el
 * AnimationManager, que corre UN solo task para todo. Con 200 efectos activos
 * eso es 1 task en vez de 200, que en 1.8 se nota.
 */
public abstract class Animation {

    private long ticks;
    private int interval = 1;
    private int duration = -1; // -1 = infinita
    private boolean running;
    private boolean cancelled;
    private Runnable onEnd;

    /** Se llama cada {@code interval} ticks mientras la animacion viva. */
    protected abstract void update(long tick);

    protected void onStart() {
    }

    protected void onStop() {
    }

    /** Cada cuantos ticks se actualiza. 1 = cada tick. */
    public Animation interval(int interval) {
        this.interval = Math.max(1, interval);
        return this;
    }

    /** Duracion total en ticks. -1 para infinita. */
    public Animation duration(int ticks) {
        this.duration = ticks;
        return this;
    }

    public Animation onEnd(Runnable callback) {
        this.onEnd = callback;
        return this;
    }

    public Animation start() {
        if (running) return this;
        running = true;
        cancelled = false;
        ticks = 0;
        onStart();
        AnimationManager.get().register(this);
        return this;
    }

    public void stop() {
        if (!running) return;
        running = false;
        cancelled = true;
        AnimationManager.get().unregister(this);
        onStop();
        if (onEnd != null) onEnd.run();
    }

    /** Lo llama el manager; no lo llames tu. */
    final void tick() {
        if (!running) return;
        if (ticks % interval == 0) {
            try {
                update(ticks);
            } catch (Throwable t) {
                org.bukkit.Bukkit.getLogger().warning("[Aurora] Error en " + getClass().getSimpleName() + ": " + t);
                stop();
                return;
            }
        }
        ticks++;
        if (duration > 0 && ticks >= duration) stop();
    }

    /** Progreso 0..1. Devuelve 0 si la animacion es infinita. */
    public double progress() {
        return duration > 0 ? Math.min(1, ticks / (double) duration) : 0;
    }

    public long ticksLived() {
        return ticks;
    }

    public int duration() {
        return duration;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
