package com.ricodevvv.aurora.util;

/**
 * Rolling estimate of how well the server is keeping up.
 *
 * <p>Aurora already runs one repeating task for every animation, so it gets a
 * tick signal for free and does not need {@code Bukkit.getTPS()} — which only
 * exists on Paper and on Spigot from 1.13. The estimate is the inverse of the
 * average time between animation ticks over the last few seconds, clamped to
 * {@code 20}.
 *
 * <p>{@link com.ricodevvv.aurora.particle.RenderSettings} uses this to drop
 * effect quality before players notice the lag, and to bring it back once the
 * server recovers.
 */
public final class ServerLoad {

    /** How many tick intervals are averaged; 60 ticks is three seconds of history. */
    private static final int WINDOW = 60;

    private static final long[] SAMPLES = new long[WINDOW];

    private static long lastNanos;
    private static int cursor;
    private static int filled;
    private static long ticks;

    private ServerLoad() {
    }

    /**
     * Records one server tick. Called by
     * {@link com.ricodevvv.aurora.animation.AnimationManager}; there is no
     * reason to call it yourself.
     */
    public static void sample() {
        ticks++;
        long now = System.nanoTime();
        if (lastNanos != 0) {
            SAMPLES[cursor] = now - lastNanos;
            cursor = (cursor + 1) % WINDOW;
            if (filled < WINDOW) filled++;
        }
        lastNanos = now;
    }

    /**
     * Clears the history, so a freshly started server is not judged on the
     * timings of the previous one.
     */
    public static void reset() {
        lastNanos = 0;
        cursor = 0;
        filled = 0;
        ticks = 0;
    }

    /**
     * @return a monotonically increasing tick counter, used to invalidate
     * per-tick caches
     */
    public static long ticks() {
        return ticks;
    }

    /**
     * @return the estimated ticks per second, capped at {@code 20}; a server
     * with no history yet reports {@code 20}
     */
    public static double tps() {
        if (filled == 0) return 20;

        long total = 0;
        for (int i = 0; i < filled; i++) {
            total += SAMPLES[i];
        }
        double averageNanos = total / (double) filled;
        if (averageNanos <= 0) return 20;

        return Math.min(20, 1_000_000_000D / averageNanos);
    }

    /**
     * @return {@code true} while the server is comfortably keeping up
     */
    public static boolean healthy() {
        return tps() >= 19;
    }
}
