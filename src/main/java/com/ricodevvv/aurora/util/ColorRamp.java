package com.ricodevvv.aurora.util;

import org.bukkit.Color;

import java.util.List;

/**
 * A colour as a function of position, sampled along a shape or over time.
 *
 * <p>This is what separates a shape drawn in one flat tone from one that reads
 * as a real effect. A ring rendered with a single colour looks like a decal; the
 * same ring rendered with a ramp — hot at the leading edge, cooling behind it —
 * reads as motion even when nothing moves.
 *
 * <pre>{@code
 * dust.spawn(at, ring, ColorRamp.of(Colors.FIRE));
 * dust.spawn(at, helix, ColorRamp.rainbow(time * 0.2));
 * }</pre>
 */
@FunctionalInterface
public interface ColorRamp {

    /**
     * Samples the ramp.
     *
     * @param position position along the ramp, in {@code 0..1}
     * @return the colour at that position
     */
    Color at(double position);

    /**
     * A ramp that always returns the same colour.
     *
     * @param color the colour
     * @return the ramp
     */
    static ColorRamp solid(Color color) {
        return position -> color;
    }

    /**
     * A ramp over a multi-stop palette, such as {@link Colors#FIRE}.
     *
     * @param palette ordered colour stops
     * @return the ramp
     */
    static ColorRamp of(List<Color> palette) {
        return position -> Colors.gradient(palette, position);
    }

    /**
     * A ramp between two colours.
     *
     * @param from colour at {@code 0}
     * @param to   colour at {@code 1}
     * @return the ramp
     */
    static ColorRamp between(Color from, Color to) {
        return position -> Colors.lerp(from, to, position);
    }

    /**
     * A full hue sweep, offset so it can be animated by feeding it a rising
     * value.
     *
     * @param offset hue offset; wraps around
     * @return the ramp
     */
    static ColorRamp rainbow(double offset) {
        return position -> Colors.rainbow(offset + position);
    }

    /**
     * A hue sweep covering only part of the wheel, which keeps an effect
     * recognisable as "the purple one" while still shifting.
     *
     * @param offset hue offset; wraps around
     * @param spread how much of the wheel to cover, in {@code 0..1}
     * @return the ramp
     */
    static ColorRamp rainbow(double offset, double spread) {
        return position -> Colors.rainbow(offset + position * spread);
    }

    /**
     * Repeats this ramp several times along its length, for banded trails and
     * candy-striped helices.
     *
     * @param times how many repetitions
     * @return the repeated ramp
     */
    default ColorRamp repeat(int times) {
        int count = Math.max(1, times);
        return position -> at((position * count) % 1.0);
    }

    /**
     * Plays this ramp forwards then backwards, so the two ends match and a
     * closed shape has no visible seam.
     *
     * @return the mirrored ramp
     */
    default ColorRamp mirror() {
        return position -> at(position < 0.5 ? position * 2 : (1 - position) * 2);
    }

    /**
     * Applies a random per-particle deviation, which stops a dense effect from
     * reading as a flat sheet of one tone.
     *
     * @param amount maximum deviation per channel, in {@code 0..255}
     * @return the jittered ramp
     */
    default ColorRamp jitter(int amount) {
        return position -> Colors.jitter(at(position), amount);
    }
}
