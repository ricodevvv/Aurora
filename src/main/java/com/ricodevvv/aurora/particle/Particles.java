package com.ricodevvv.aurora.particle;

import org.bukkit.Color;

/**
 * Entry point for spawning particles.
 *
 * <p>Every method returns a fresh {@link ParticleBuilder}. Inside a tick loop,
 * build once and keep it as a field rather than calling these every tick.
 *
 * <p>The presets below cover the particles a cosmetic effect actually reaches
 * for. None of them can fail on an older server: {@link ParticleType} carries a
 * fallback for everything added after 1.8, so asking for a soul flame on 1.8
 * yields a flame and asking for an end rod yields firework sparks, rather than
 * a silent nothing or an exception.
 *
 * @see ParticleBuilder
 * @see ParticleType
 */
public final class Particles {

    private Particles() {
    }

    /**
     * Starts a builder for any particle.
     *
     * @param particle particle to spawn
     * @return a new builder
     */
    public static ParticleBuilder of(ParticleType particle) {
        return new ParticleBuilder(particle);
    }

    /**
     * Starts a coloured dust builder, the workhorse for custom-coloured effects.
     *
     * @param color dust colour
     * @return a new builder
     */
    public static ParticleBuilder dust(Color color) {
        return of(ParticleType.DUST).color(color);
    }

    /**
     * Starts a coloured dust builder from RGB components.
     *
     * @param r red
     * @param g green
     * @param b blue
     * @return a new builder
     */
    public static ParticleBuilder dust(int r, int g, int b) {
        return of(ParticleType.DUST).color(r, g, b);
    }

    /**
     * Starts a dust builder whose particles fade from one colour to another
     * over their lifetime.
     *
     * <p>On anything below 1.17 this degrades to plain dust in the first
     * colour, so it is always safe to reach for.
     *
     * @param from colour each particle starts at
     * @param to   colour it fades towards
     * @return a new builder
     */
    public static ParticleBuilder dust(Color from, Color to) {
        return dust(from).fadeTo(to);
    }

    /**
     * @return a builder for flame particles
     */
    public static ParticleBuilder flame() {
        return of(ParticleType.FLAME);
    }

    /**
     * @return a builder for the small blue soul flame, falling back to flame
     */
    public static ParticleBuilder soulFlame() {
        return of(ParticleType.SOUL_FIRE_FLAME);
    }

    /**
     * @return a builder for critical hit particles
     */
    public static ParticleBuilder crit() {
        return of(ParticleType.CRIT);
    }

    /**
     * A thin white streak that holds its shape instead of drifting.
     *
     * <p>This is the particle premium effects use for anything that has to read
     * as a clean line — halos, blade arcs, orbit rings — because dust blurs at
     * distance while end rod stays crisp.
     *
     * @return a builder for end rod particles, falling back to firework sparks
     */
    public static ParticleBuilder endRod() {
        return of(ParticleType.END_ROD);
    }

    /**
     * @return a builder for firework sparks
     */
    public static ParticleBuilder spark() {
        return of(ParticleType.FIREWORK);
    }

    /**
     * @return a builder for the electric spark, falling back to critical hits
     */
    public static ParticleBuilder electric() {
        return of(ParticleType.ELECTRIC_SPARK);
    }

    /**
     * @return a builder for snowflakes, falling back to cloud puffs
     */
    public static ParticleBuilder snowflake() {
        return of(ParticleType.SNOWFLAKE);
    }

    /**
     * @return a builder for the totem's spinning sparkles, falling back to
     * happy villager particles
     */
    public static ParticleBuilder totem() {
        return of(ParticleType.TOTEM_OF_UNDYING);
    }

    /**
     * @return a builder for soul wisps, falling back to large smoke
     */
    public static ParticleBuilder soul() {
        return of(ParticleType.SOUL);
    }

    /**
     * @return a builder for cherry blossom petals, falling back to dust
     */
    public static ParticleBuilder petal() {
        return of(ParticleType.CHERRY_LEAVES);
    }

    /**
     * @return a builder for the enchanting table's glyphs
     */
    public static ParticleBuilder enchant() {
        return of(ParticleType.ENCHANT);
    }
}
