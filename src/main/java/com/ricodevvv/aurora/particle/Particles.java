package com.ricodevvv.aurora.particle;

import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Color;

/**
 * Entry point for spawning particles.
 *
 * <p>Every method returns a fresh {@link ParticleBuilder}. Inside a tick loop,
 * build once and keep it as a field rather than calling these every tick.
 *
 * <p>The presets below cover the particles a cosmetic effect actually reaches
 * for, and each one falls back to something that exists on older servers:
 * {@code SOUL_FIRE_FLAME} became available in 1.16 and {@code END_ROD} in 1.9,
 * so asking for either on 1.8 yields flame and firework sparks instead of a
 * silent nothing.
 *
 * @see ParticleBuilder
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
    public static ParticleBuilder of(XParticle particle) {
        return new ParticleBuilder(particle);
    }

    /**
     * Starts a builder for the first of several particles that the running
     * server supports.
     *
     * @param preferred    the particle you want
     * @param alternatives fallbacks, most preferred first
     * @return a new builder for whichever exists
     */
    public static ParticleBuilder of(XParticle preferred, XParticle... alternatives) {
        return new ParticleBuilder(supported(preferred, alternatives));
    }

    /**
     * Starts a coloured dust builder, the workhorse for custom-coloured effects.
     *
     * @param color dust colour
     * @return a new builder
     */
    public static ParticleBuilder dust(Color color) {
        return of(XParticle.DUST).color(color);
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
        return of(XParticle.DUST).color(r, g, b);
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
        return of(XParticle.FLAME);
    }

    /**
     * @return a builder for the small blue soul flame, falling back to flame
     */
    public static ParticleBuilder soulFlame() {
        return of(XParticle.SOUL_FIRE_FLAME, XParticle.FLAME);
    }

    /**
     * @return a builder for critical hit particles
     */
    public static ParticleBuilder crit() {
        return of(XParticle.CRIT);
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
        return of(XParticle.END_ROD, XParticle.FIREWORK);
    }

    /**
     * @return a builder for firework sparks
     */
    public static ParticleBuilder spark() {
        return of(XParticle.FIREWORK);
    }

    /**
     * @return a builder for the electric spark, falling back to critical hits
     */
    public static ParticleBuilder electric() {
        return of(XParticle.ELECTRIC_SPARK, XParticle.CRIT);
    }

    /**
     * @return a builder for snowflakes, falling back to white dust
     */
    public static ParticleBuilder snowflake() {
        return of(XParticle.SNOWFLAKE, XParticle.CLOUD);
    }

    /**
     * @return a builder for the totem's spinning green sparkles, falling back
     * to happy villager particles
     */
    public static ParticleBuilder totem() {
        return of(XParticle.TOTEM_OF_UNDYING, XParticle.HAPPY_VILLAGER);
    }

    /**
     * @return a builder for soul wisps, falling back to large smoke
     */
    public static ParticleBuilder soul() {
        return of(XParticle.SOUL, XParticle.LARGE_SMOKE);
    }

    /**
     * @return a builder for cherry blossom petals, falling back to pink dust
     */
    public static ParticleBuilder petal() {
        return of(XParticle.CHERRY_LEAVES, XParticle.FALLING_DUST, XParticle.DUST);
    }

    /**
     * @return a builder for the enchanting table's glyphs
     */
    public static ParticleBuilder enchant() {
        return of(XParticle.ENCHANT);
    }

    /**
     * Picks the first particle the running server actually supports.
     *
     * @param preferred    the particle you want
     * @param alternatives fallbacks, most preferred first
     * @return the first supported particle, or the preferred one if the check
     * itself is unavailable
     */
    private static XParticle supported(XParticle preferred, XParticle... alternatives) {
        if (ParticleCompat.supported(preferred)) return preferred;
        for (XParticle alternative : alternatives) {
            if (ParticleCompat.supported(alternative)) return alternative;
        }
        return preferred;
    }
}
