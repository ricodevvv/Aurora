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

    /** Deep space blues and magentas, for galaxy and portal effects. */
    public static final List<Color> NEBULA = Arrays.asList(
            Color.fromRGB(255, 210, 255),
            Color.fromRGB(190, 110, 255),
            Color.fromRGB(80, 70, 220),
            Color.fromRGB(20, 15, 70));

    /** Molten rock: white-hot core through orange crust to cooled black. */
    public static final List<Color> LAVA = Arrays.asList(
            Color.fromRGB(255, 245, 200),
            Color.fromRGB(255, 140, 20),
            Color.fromRGB(200, 40, 10),
            Color.fromRGB(60, 20, 20));

    /** Polished gold, for legendary and rank cosmetics. */
    public static final List<Color> GOLD = Arrays.asList(
            Color.fromRGB(255, 250, 205),
            Color.fromRGB(255, 214, 90),
            Color.fromRGB(212, 155, 25),
            Color.fromRGB(120, 80, 10));

    /** Cherry blossom pinks. */
    public static final List<Color> SAKURA = Arrays.asList(
            Color.fromRGB(255, 240, 248),
            Color.fromRGB(255, 183, 213),
            Color.fromRGB(240, 120, 170),
            Color.fromRGB(170, 60, 110));

    /** End-dimension teal and violet. */
    public static final List<Color> ENDER = Arrays.asList(
            Color.fromRGB(215, 255, 245),
            Color.fromRGB(90, 235, 205),
            Color.fromRGB(120, 70, 220),
            Color.fromRGB(35, 20, 70));

    /** Northern-lights greens fading into deep night blue. */
    public static final List<Color> AURORA = Arrays.asList(
            Color.fromRGB(190, 255, 235),
            Color.fromRGB(80, 230, 160),
            Color.fromRGB(60, 140, 235),
            Color.fromRGB(25, 30, 90));

    /** Sea greens and blues. */
    public static final List<Color> OCEAN = Arrays.asList(
            Color.fromRGB(215, 255, 255),
            Color.fromRGB(90, 215, 225),
            Color.fromRGB(30, 130, 200),
            Color.fromRGB(10, 45, 110));

    /** Sugary pinks and blues, for party and event cosmetics. */
    public static final List<Color> CANDY = Arrays.asList(
            Color.fromRGB(255, 245, 250),
            Color.fromRGB(255, 130, 200),
            Color.fromRGB(140, 200, 255),
            Color.fromRGB(255, 220, 120));

    /** Arterial reds, for the darker half of a catalogue. */
    public static final List<Color> BLOOD = Arrays.asList(
            Color.fromRGB(255, 120, 120),
            Color.fromRGB(200, 25, 40),
            Color.fromRGB(110, 10, 20),
            Color.fromRGB(35, 5, 8));

    /** Charcoal to black, for shadow and wraith effects. */
    public static final List<Color> SHADOW = Arrays.asList(
            Color.fromRGB(120, 110, 140),
            Color.fromRGB(60, 50, 80),
            Color.fromRGB(20, 16, 28));

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
        return rainbow(hue, 1f, 1f);
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

    /**
     * Samples a gradient that runs forwards and then back again, so a value
     * rising past {@code 1} does not snap from the last colour to the first.
     * Use it for anything that loops: a pulsing aura, a breathing shield.
     *
     * @param palette  ordered colour stops
     * @param progress position; only its fractional part is used
     * @return the sampled colour
     */
    public static Color wave(List<Color> palette, double progress) {
        double phase = Math.abs(progress) % 2.0;
        return gradient(palette, phase > 1 ? 2 - phase : phase);
    }

    /**
     * Brightens or darkens a colour, which is how a single palette produces
     * both the dim body of an effect and its bright core.
     *
     * @param base   colour to adjust
     * @param factor multiplier; {@code 1} leaves it alone, {@code 1.6} brightens,
     *               {@code 0.5} halves
     * @return the adjusted colour
     */
    public static Color shade(Color base, double factor) {
        return Color.fromRGB(
                clamp((int) Math.round(base.getRed() * factor)),
                clamp((int) Math.round(base.getGreen() * factor)),
                clamp((int) Math.round(base.getBlue() * factor)));
    }

    /**
     * Pushes a colour towards white without washing out its hue, for the
     * glowing core of a layered effect.
     *
     * @param base   colour to brighten
     * @param amount how far towards white, in {@code 0..1}
     * @return the brightened colour
     */
    public static Color glow(Color base, double amount) {
        return lerp(base, Color.WHITE, amount);
    }

    /**
     * A hue sweep at reduced saturation, which reads as pastel rather than as
     * the primary-colour rainbow every free cosmetics plugin uses.
     *
     * @param hue        hue value; wraps around
     * @param saturation saturation in {@code 0..1}
     * @param brightness brightness in {@code 0..1}
     * @return the resulting colour
     */
    public static Color rainbow(double hue, float saturation, float brightness) {
        java.awt.Color awt = java.awt.Color.getHSBColor(
                (float) (((hue % 1.0) + 1.0) % 1.0), saturation, brightness);
        return Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue());
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
