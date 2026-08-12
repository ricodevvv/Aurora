package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.util.ServerVersion;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Capa que traduce un ParticleBuilder a lo que entienda el servidor.
 *
 * La implementacion se elige una sola vez al arrancar. Como las clases
 * concretas se cargan de forma perezosa, el backend moderno nunca se carga
 * en 1.8 (donde org.bukkit.Particle ni existe) y viceversa.
 */
public interface ParticleBackend {

    void spawn(Collection<? extends Player> viewers, ParticleBuilder builder,
               double x, double y, double z);

    /** true si el tipo pedido pudo resolverse en esta version. */
    boolean supports(ParticleType type);

    static ParticleBackend create() {
        if (ServerVersion.isLegacy()) {
            return new com.ricodevvv.aurora.particle.backend.LegacyBackend();
        }
        return new com.ricodevvv.aurora.particle.backend.ModernBackend();
    }
}
