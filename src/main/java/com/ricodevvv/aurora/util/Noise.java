package com.ricodevvv.aurora.util;

/**
 * Smooth pseudo-random values, for effects that should look alive rather than
 * shuffled.
 *
 * <p>Reaching for {@link java.util.Random} inside a tick loop is what makes a
 * flame effect read as static: every frame is independent, so particles jump
 * instead of drifting. Noise is continuous — feeding it a slowly rising value
 * gives a wander, not a jitter — and it is deterministic, so the same wearer
 * gets the same flicker every time rather than a different one on every
 * relog.
 *
 * <p>This is classic 1D and 2D value noise with cubic interpolation. It is not
 * Perlin noise and does not try to be: it costs a handful of multiplications,
 * allocates nothing, and at the scale of a particle offset the difference is
 * invisible.
 *
 * <pre>{@code
 * // a flame that leans and recovers instead of teleporting
 * double lean = Noise.at(time * 0.6 + seed) * 0.35;
 * }</pre>
 */
public final class Noise {

    private Noise() {
    }

    /**
     * One-dimensional noise.
     *
     * @param x position along the noise; advance it slowly for a slow wander
     * @return a value in {@code -1..1}
     */
    public static double at(double x) {
        int floor = fastFloor(x);
        double fraction = x - floor;
        double smooth = smoothstep(fraction);
        return lerp(hash(floor), hash(floor + 1), smooth);
    }

    /**
     * Two-dimensional noise, for values that vary along a shape as well as over
     * time: the {@code y} axis is usually the point index, the {@code x} axis
     * time.
     *
     * @param x first axis
     * @param y second axis
     * @return a value in {@code -1..1}
     */
    public static double at(double x, double y) {
        int xFloor = fastFloor(x);
        int yFloor = fastFloor(y);
        double xSmooth = smoothstep(x - xFloor);
        double ySmooth = smoothstep(y - yFloor);

        double bottom = lerp(hash(xFloor, yFloor), hash(xFloor + 1, yFloor), xSmooth);
        double top = lerp(hash(xFloor, yFloor + 1), hash(xFloor + 1, yFloor + 1), xSmooth);
        return lerp(bottom, top, ySmooth);
    }

    /**
     * Layered noise: several octaves at halving amplitude, which turns a smooth
     * wander into something with both a slow drift and a fine flicker. This is
     * what fire and smoke want.
     *
     * @param x       position along the noise
     * @param octaves how many layers, {@code 1} being plain noise
     * @return a value in {@code -1..1}
     */
    public static double fractal(double x, int octaves) {
        double total = 0;
        double amplitude = 1;
        double frequency = 1;
        double normaliser = 0;

        for (int i = 0; i < Math.max(1, octaves); i++) {
            total += at(x * frequency) * amplitude;
            normaliser += amplitude;
            amplitude *= 0.5;
            frequency *= 2;
        }
        return total / normaliser;
    }

    /**
     * Noise remapped to {@code 0..1}, which is the range most callers actually
     * want when scaling a radius or an alpha.
     *
     * @param x position along the noise
     * @return a value in {@code 0..1}
     */
    public static double unit(double x) {
        return (at(x) + 1) * 0.5;
    }

    /**
     * A stable per-object offset into the noise field.
     *
     * <p>Give every wearer their own seed and ten players standing together
     * stop pulsing in lockstep, which is the single clearest giveaway of a
     * cheap cosmetic plugin.
     *
     * @param key any object; {@code null} yields {@code 0}
     * @return a seed in {@code 0..1024}
     */
    public static double seed(Object key) {
        if (key == null) return 0;
        int hash = key.hashCode();
        return ((hash ^ (hash >>> 16)) & 0xFFFF) / 64.0;
    }

    /**
     * Deterministic hash of one integer, in {@code -1..1}.
     *
     * @param value input
     * @return the hashed value
     */
    private static double hash(int value) {
        int mixed = value * 0x27D4EB2D;
        mixed = (mixed << 13) ^ mixed;
        return 1 - ((mixed * (mixed * mixed * 15731 + 789221) + 1376312589) & 0x7FFFFFFF)
                / 1073741824.0;
    }

    /**
     * Deterministic hash of two integers, in {@code -1..1}.
     *
     * @param x first input
     * @param y second input
     * @return the hashed value
     */
    private static double hash(int x, int y) {
        return hash(x * 73856093 ^ y * 19349663);
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private static int fastFloor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}
