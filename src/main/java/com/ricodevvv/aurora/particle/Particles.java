package com.ricodevvv.aurora.particle;

import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Color;

/**
 * Entry point for spawning particles.
 *
 * <p>Every method returns a fresh {@link ParticleBuilder}. Inside a tick loop,
 * build once and keep it as a field rather than calling these every tick.
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
     * @return a builder for flame particles
     */
    public static ParticleBuilder flame() {
        return of(XParticle.FLAME);
    }

    /**
     * @return a builder for critical hit particles
     */
    public static ParticleBuilder crit() {
        return of(XParticle.CRIT);
    }
}
