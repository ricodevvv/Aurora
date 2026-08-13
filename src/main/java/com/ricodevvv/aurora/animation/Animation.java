package com.ricodevvv.aurora.animation;

import org.bukkit.Bukkit;

/**
 * Base class for everything that runs over time.
 *
 * <p>An animation does not own a scheduler task. It registers itself with
 * {@link AnimationManager}, which drives every running animation from a single
 * server task. With two hundred active effects that is one task rather than two
 * hundred, which is the difference between noticeable and not on a busy lobby.
 *
 * <p>Subclasses implement {@link #update(long)} and are handed the tick counter
 * since the animation started. Anything thrown from {@code update} is caught,
 * logged and stops that animation only, so one broken effect cannot take the
 * whole tick loop down with it.
 *
 * <pre>{@code
 * new ShapeAnimation(location, Shapes.circle(2, 40), particle)
 *         .spinDegrees(6)
 *         .duration(200)
 *         .start();
 * }</pre>
 */
public abstract class Animation {

    private long ticks;
    private int interval = 1;
    private int duration = -1;
    private boolean running;
    private boolean cancelled;
    private Runnable onEnd;

    /**
     * Called every {@link #interval(int)} ticks while the animation is running.
     *
     * @param tick ticks elapsed since {@link #start()}
     */
    protected abstract void update(long tick);

    /**
     * Called once when the animation starts, before the first update.
     */
    protected void onStart() {
    }

    /**
     * Called once when the animation stops, whether it finished or was
     * cancelled. Release entities and other resources here.
     */
    protected void onStop() {
    }

    /**
     * Sets how often the animation updates.
     *
     * <p>This is the cheapest performance lever available: raising it to
     * {@code 2} or {@code 3} usually costs nothing visually and cuts the work
     * proportionally.
     *
     * @param interval ticks between updates; values below {@code 1} are clamped
     * @return this animation
     */
    public Animation interval(int interval) {
        this.interval = Math.max(1, interval);
        return this;
    }

    /**
     * Sets how long the animation runs.
     *
     * @param ticks total lifetime in ticks, or {@code -1} to run indefinitely
     * @return this animation
     */
    public Animation duration(int ticks) {
        this.duration = ticks;
        return this;
    }

    /**
     * Registers a callback fired after the animation stops.
     *
     * @param onEnd callback to run
     * @return this animation
     */
    public Animation onEnd(Runnable onEnd) {
        this.onEnd = onEnd;
        return this;
    }

    /**
     * Starts the animation. Calling this on an already running animation does
     * nothing.
     *
     * @return this animation
     */
    public Animation start() {
        if (running) return this;
        running = true;
        cancelled = false;
        ticks = 0;
        onStart();
        AnimationManager.get().register(this);
        return this;
    }

    /**
     * Stops the animation, runs {@link #onStop()} and fires the end callback.
     * Safe to call more than once.
     */
    public void stop() {
        if (!running) return;
        running = false;
        cancelled = true;
        AnimationManager.get().unregister(this);
        onStop();
        if (onEnd != null) onEnd.run();
    }

    /**
     * Advances the animation by one tick. Called by {@link AnimationManager};
     * do not call it yourself.
     */
    final void tick() {
        if (!running) return;

        if (ticks % interval == 0) {
            try {
                update(ticks);
            } catch (Throwable thrown) {
                Bukkit.getLogger().warning(
                        "[Aurora] " + getClass().getSimpleName() + " failed: " + thrown);
                stop();
                return;
            }
        }

        ticks++;
        if (duration > 0 && ticks >= duration) stop();
    }

    /**
     * @return progress from {@code 0} to {@code 1}, or {@code 0} for animations
     * with no fixed duration
     */
    public double progress() {
        return duration > 0 ? Math.min(1, ticks / (double) duration) : 0;
    }

    /**
     * @return ticks elapsed since the animation started
     */
    public long ticksLived() {
        return ticks;
    }

    /**
     * @return the configured lifetime in ticks, or {@code -1} if indefinite
     */
    public int duration() {
        return duration;
    }

    /**
     * @return {@code true} while the animation is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return {@code true} once the animation has been stopped
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
