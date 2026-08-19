package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.util.ServerLoad;

/**
 * Global knobs for the particle renderer.
 *
 * <p>These are the settings a server owner expects to find in a cosmetics
 * plugin's config file: how detailed effects are, how far they are visible,
 * and whether they should get out of the way when the server is struggling.
 * Aurora ships no config file, so expose them however your plugin prefers and
 * forward the values here.
 *
 * <pre>{@code
 * RenderSettings.quality(RenderQuality.HIGH);
 * RenderSettings.adaptive(true);          // drop detail automatically under load
 * RenderSettings.rangeMultiplier(0.8);    // shorter view range on a busy lobby
 * }</pre>
 *
 * <p>Everything here is read from the main thread inside the tick loop, so the
 * accessors are plain field reads with no locking.
 */
public final class RenderSettings {

    private static RenderQuality quality = RenderQuality.HIGH;
    private static boolean adaptive = true;
    private static double rangeMultiplier = 1;
    private static boolean enabled = true;

    /** Cached adaptive result, recomputed once per tick rather than per spawn. */
    private static RenderQuality effective = RenderQuality.HIGH;
    private static long effectiveTick = -1;

    private RenderSettings() {
    }

    /**
     * Sets the authored quality level. With {@link #adaptive(boolean)} on this
     * acts as the ceiling rather than a fixed value.
     *
     * @param level quality level
     */
    public static void quality(RenderQuality level) {
        quality = level == null ? RenderQuality.HIGH : level;
        effectiveTick = -1;
    }

    /**
     * @return the configured quality ceiling
     */
    public static RenderQuality quality() {
        return quality;
    }

    /**
     * Enables automatic quality reduction when the server falls behind.
     *
     * <p>Below 19 TPS the level drops one step, below 17 two steps. Recovery is
     * immediate, but because the estimate is averaged over three seconds it
     * cannot flicker between levels tick by tick.
     *
     * @param value {@code true} to adapt automatically
     */
    public static void adaptive(boolean value) {
        adaptive = value;
        effectiveTick = -1;
    }

    /**
     * @return {@code true} if quality follows server load
     */
    public static boolean adaptive() {
        return adaptive;
    }

    /**
     * Scales the view range of every effect, on top of each effect's own range.
     *
     * @param multiplier multiplier, clamped to {@code 0.1 - 4.0}
     */
    public static void rangeMultiplier(double multiplier) {
        rangeMultiplier = Math.max(0.1, Math.min(4, multiplier));
    }

    /**
     * @return the global view range multiplier
     */
    public static double rangeMultiplier() {
        return rangeMultiplier;
    }

    /**
     * Master switch. Turning it off silences every effect without unequipping
     * anything, which is what you want during an event or a boss fight.
     *
     * @param value {@code false} to suppress all particle output
     */
    public static void enabled(boolean value) {
        enabled = value;
    }

    /**
     * @return {@code false} while particle output is globally suppressed
     */
    public static boolean enabled() {
        return enabled;
    }

    /**
     * The quality actually in force this tick, after applying adaptation.
     *
     * @return the effective quality level
     */
    public static RenderQuality effective() {
        if (!adaptive) return quality;

        long tick = ServerLoad.ticks();
        if (tick != effectiveTick) {
            effectiveTick = tick;
            double tps = ServerLoad.tps();
            effective = tps >= 19 ? quality
                    : tps >= 17 ? quality.lower(1)
                    : quality.lower(2);
        }
        return effective;
    }

    /**
     * @return the fraction of a shape's points to draw this tick
     */
    public static double density() {
        return effective().density();
    }

    /**
     * Scales an authored interval by the current quality.
     *
     * @param interval the effect's own interval, in ticks
     * @return the interval to actually use, never below {@code 1}
     */
    public static int interval(int interval) {
        return Math.max(1, (int) Math.round(interval * effective().intervalScale()));
    }

    /**
     * Scales an authored range by the current quality and the global multiplier.
     *
     * @param range the effect's own range, in blocks
     * @return the range to actually use
     */
    public static double range(double range) {
        return range * effective().rangeScale() * rangeMultiplier;
    }
}
