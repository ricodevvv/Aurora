package com.ricodevvv.aurora.util;

import org.bukkit.Color;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Colour helpers: hex parsing, blending, gradients and ready-made palettes.
 *
 * <p>The palettes are ordered from bright to dark so that
 * {@link #gradient(List, double)} reads as a natural falloff when driven by a
 * rising progress value.
 */
public final class Colors {

    /** Yellow-white through orange to deep ember red. */
    public static final List<Color> FIRE = Arrays.asList(
            Color.fromRGB(255, 240, 120),
            Color.fromRGB(255, 170, 30),
            Color.fromRGB(230, 70, 20),
            Color.fromRGB(120, 20, 10));

    /** Near-white through cyan to deep blue. */
    public static final List<Color> ICE = Arrays.asList(
            Color.fromRGB(230, 250, 255),
            Color.fromRGB(140, 220, 255),
            Color.fromRGB(60, 150, 230),
            Color.fromRGB(30, 80, 170));

    /** Acid yellow-green through to dark green. */
    public static final List<Color> TOXIC = Arrays.asList(
            Color.fromRGB(220, 255, 120),
            Color.fromRGB(120, 230, 60),
            Color.fromRGB(40, 150, 40));

    /** Light violet through purple to near-black. */
    public static final List<Color> VOID = Arrays.asList(
            Color.fromRGB(190, 120, 255),
            Color.fromRGB(110, 40, 200),
            Color.fromRGB(30, 10, 60));

    private Colors() {
    }

    /**
     * Parses a hex colour.
     *
     * @param hex six hex digits, with or without a leading {@code #}
     * @return the parsed colour
     * @throws NumberFormatException if the string is not valid hex
     */
    public static Color hex(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        int rgb = Integer.parseInt(clean, 16);
        return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Blends two colours.
     *
     * @param from     colour at {@code progress = 0}
     * @param to       colour at {@code progress = 1}
     * @param progress progress in {@code 0..1}, clamped
     * @return the blended colour
     */
    public static Color lerp(Color from, Color to, double progress) {
        double clamped = clamp01(progress);
        return Color.fromRGB(
                round(from.getRed(), to.getRed(), clamped),
                round(from.getGreen(), to.getGreen(), clamped),
                round(from.getBlue(), to.getBlue(), clamped));
    }

    /**
     * Samples a multi-stop gradient. Use it to drive a trail that shifts hue
     * along its length or over time.
     *
     * @param palette  ordered colour stops; must not be empty
     * @param progress position along the gradient, in {@code 0..1}, clamped
     * @return the sampled colour, or white if the palette is empty
     */
    public static Color gradient(List<Color> palette, double progress) {
        if (palette.isEmpty()) return Color.WHITE;
        if (palette.size() == 1) return palette.get(0);

        double scaled = clamp01(progress) * (palette.size() - 1);
        int index = (int) Math.floor(scaled);
        if (index >= palette.size() - 1) return palette.get(palette.size() - 1);
        return lerp(palette.get(index), palette.get(index + 1), scaled - index);
    }

    /**
     * Produces a fully saturated colour from a hue, for continuous rainbows.
     *
     * @param hue hue value; wraps around, so a rising counter works directly
     * @return the resulting colour
     */
    public static Color rainbow(double hue) {
        java.awt.Color awt = java.awt.Color.getHSBColor((float) (hue % 1.0), 1f, 1f);
        return Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    /**
     * Randomly varies a colour, which stops a dense effect from looking like a
     * flat sheet of one tone.
     *
     * @param base   colour to vary
     * @param amount maximum deviation per channel, in {@code 0..255}
     * @return a nearby colour
     */
    public static Color jitter(Color base, int amount) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Color.fromRGB(
                clamp(base.getRed() + random.nextInt(-amount, amount + 1)),
                clamp(base.getGreen() + random.nextInt(-amount, amount + 1)),
                clamp(base.getBlue() + random.nextInt(-amount, amount + 1)));
    }

    private static int round(int from, int to, double progress) {
        return (int) Math.round(from + (to - from) * progress);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
