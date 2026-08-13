package com.ricodevvv.aurora.cosmetic.particle;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * An effect is a function from (player, position, tick) to particles.
 *
 * <p>Deliberately not one class per effect. Defining effects as data rather
 * than as subclasses means a new one is three lines in a catalogue, and because
 * this is a functional interface you can register your own without touching the
 * library.
 *
 * <pre>{@code
 * new ParticleEffectType("halo", "&eHalo", icon, (player, at, tick) ->
 *         dust.spawn(at.clone().add(0, 2.3, 0), RING.rotateY(tick * 0.1)));
 * }</pre>
 */
@FunctionalInterface
public interface EffectRenderer {

    /**
     * Draws one frame of the effect.
     *
     * @param player player wearing the effect
     * @param origin the player's position this tick; already a copy, so it is
     *               safe to mutate
     * @param tick   ticks since the effect was equipped
     */
    void render(Player player, Location origin, long tick);
}
