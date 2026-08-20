package com.ricodevvv.aurora.cosmetic.particle;

/**
 * An effect is a function from a frame to particles.
 *
 * <p>Deliberately not one class per effect. Defining effects as data rather
 * than as subclasses means a new one is three lines in a catalogue, and because
 * this is a functional interface you can register your own without touching the
 * library.
 *
 * <pre>{@code
 * new ParticleEffectType("halo", "&eHalo", icon, ctx ->
 *         ctx.emit(dust, ctx.head(), RING.rotateY(ctx.time() * 2)));
 * }</pre>
 *
 * <p>This is the renderer to write new effects against. The older
 * {@link EffectRenderer}, which receives a player, a location and a tick
 * counter, still works and is adapted onto this one.
 *
 * @see EffectContext
 */
@FunctionalInterface
public interface ContextRenderer {

    /**
     * Draws one frame of the effect.
     *
     * @param context everything about this frame: where the wearer is, what
     *                they are doing, who can see it
     */
    void render(EffectContext context);
}
