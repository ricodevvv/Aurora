package com.ricodevvv.aurora.cosmetic.particle;

/**
 * What the wearer is doing this frame.
 *
 * <p>A cosmetic that draws the same thing whether its wearer is standing in
 * spawn, sprinting across a map or falling out of the sky reads as a sticker.
 * Every premium effect changes shape with the player: a flame aura collapses
 * into two trailing streaks at a sprint, wings snap open in the air, an aura
 * pulls in tight when its wearer crouches.
 *
 * <p>An effect declares only the states it cares about; anything it does not
 * define falls back through {@link #fallback()} until it reaches
 * {@link #IDLE}, which every effect defines.
 *
 * @see ParticleEffectType#state(MovementState, ContextRenderer)
 */
public enum MovementState {

    /** Standing still on the ground. Every effect has this variant. */
    IDLE,

    /** Walking. Falls back to {@link #IDLE}. */
    WALKING,

    /** Sprinting. Falls back to {@link #WALKING}. */
    SPRINTING,

    /** Crouching, moving or not. Falls back to {@link #IDLE}. */
    SNEAKING,

    /** In the air without creative flight: jumping, falling, knocked back. Falls back to {@link #WALKING}. */
    AIRBORNE,

    /** Creative or elytra-style flight. Falls back to {@link #AIRBORNE}. */
    FLYING;

    /**
     * The state to try when an effect has no renderer for this one.
     *
     * @return the next state to look up; {@link #IDLE} returns itself
     */
    public MovementState fallback() {
        switch (this) {
            case SPRINTING:
                return WALKING;
            case FLYING:
                return AIRBORNE;
            case AIRBORNE:
            case WALKING:
            case SNEAKING:
                return IDLE;
            default:
                return IDLE;
        }
    }

    /**
     * @return {@code true} when the wearer is going somewhere, which is the
     * distinction the older {@code moving} renderer was built on
     */
    public boolean moving() {
        return this == WALKING || this == SPRINTING || this == AIRBORNE || this == FLYING;
    }

    /**
     * @return {@code true} when the wearer's feet are off the ground
     */
    public boolean airborne() {
        return this == AIRBORNE || this == FLYING;
    }
}
