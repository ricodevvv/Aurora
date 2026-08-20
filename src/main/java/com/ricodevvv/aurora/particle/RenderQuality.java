package com.ricodevvv.aurora.particle;

/**
 * Detail level applied to every effect that opts into level of detail.
 *
 * <p>Premium cosmetic plugins do not draw the same number of particles for a
 * player standing two blocks away and one at the edge of view distance, and
 * they do not draw the same number on an empty server and on a full lobby.
 * Quality is the single knob Aurora turns for both: it scales point counts,
 * effect intervals and view range together, so an effect keeps its silhouette
 * and its animation speed while costing a fraction of the particles.
 *
 * <p>The important property is that lowering quality must never change what an
 * effect <em>looks</em> like, only how densely it is sampled. That is why
 * effects are written against {@link com.ricodevvv.aurora.cosmetic.particle.EffectContext#time()}
 * rather than a raw tick counter: doubling the interval halves the cost and
 * leaves the rotation speed untouched.
 *
 * @see RenderSettings
 */
public enum RenderQuality {

    /** Half the points, drawn half as often. For crowded lobbies and weak hardware. */
    LOW(0.45, 2.0, 0.65),

    /** A noticeable saving that most players will not consciously register. */
    MEDIUM(0.70, 1.5, 0.85),

    /** Everything as authored. The default. */
    HIGH(1.00, 1.0, 1.00),

    /** Denser sampling and a longer view range, for showcase servers and trailers. */
    ULTRA(1.40, 1.0, 1.20);

    private final double density;
    private final double intervalScale;
    private final double rangeScale;

    RenderQuality(double density, double intervalScale, double rangeScale) {
        this.density = density;
        this.intervalScale = intervalScale;
        this.rangeScale = rangeScale;
    }

    /**
     * @return the fraction of a shape's points that is actually drawn
     */
    public double density() {
        return density;
    }

    /**
     * @return multiplier applied to effect intervals; larger means fewer frames
     */
    public double intervalScale() {
        return intervalScale;
    }

    /**
     * @return multiplier applied to effect view range
     */
    public double rangeScale() {
        return rangeScale;
    }

    /**
     * Steps down towards {@link #LOW}, used by the adaptive mode when the
     * server starts falling behind.
     *
     * @param steps how many levels to drop
     * @return the lowered quality, never below {@link #LOW}
     */
    public RenderQuality lower(int steps) {
        int index = Math.max(0, ordinal() - Math.max(0, steps));
        return values()[index];
    }
}
