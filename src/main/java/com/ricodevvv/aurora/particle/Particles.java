package com.ricodevvv.aurora.particle;

import org.bukkit.Color;

/** Atajo estatico para no andar escribiendo new ParticleBuilder(...). */
public final class Particles {

    private Particles() {
    }

    public static ParticleBuilder of(ParticleType type) {
        return new ParticleBuilder(type);
    }

    public static ParticleBuilder dust(Color color) {
        return new ParticleBuilder(ParticleType.DUST).color(color);
    }

    public static ParticleBuilder dust(int r, int g, int b) {
        return new ParticleBuilder(ParticleType.DUST).color(r, g, b);
    }

    public static ParticleBuilder flame() {
        return new ParticleBuilder(ParticleType.FLAME);
    }

    public static ParticleBuilder crit() {
        return new ParticleBuilder(ParticleType.CRIT);
    }
}
