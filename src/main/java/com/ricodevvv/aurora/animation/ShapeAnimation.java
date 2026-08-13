package com.ricodevvv.aurora.animation;

import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.util.Easing;
import org.bukkit.Location;

import java.util.function.Supplier;

/**
 * Renders a {@link Shape} every tick, applying rotation, scaling and vertical
 * drift as a function of elapsed time.
 *
 * <p>Most effects need nothing more than this: auras, shields, portals and
 * summoning rings are all a shape plus a spin. Write a dedicated
 * {@link Animation} subclass only when the geometry itself has to change per
 * tick.
 */
public class ShapeAnimation extends Animation {

    private final Supplier<Location> origin;
    private final Shape shape;
    private final ParticleBuilder particle;

    private double yawPerTick = Math.toRadians(6);
    private double pitchPerTick = 0;
    private double rollPerTick = 0;
    private double yOffset = 0;
    private double yPerTick = 0;
    private double scaleFrom = 1, scaleTo = 1;
    private Easing easing = Easing.LINEAR;
    private boolean colorCycle = false;
    private float hueSpeed = 0.01f;

    public ShapeAnimation(Supplier<Location> origin, Shape shape, ParticleBuilder particle) {
        this.origin = origin;
        this.shape = shape;
        this.particle = particle;
    }

    public ShapeAnimation(Location fixed, Shape shape, ParticleBuilder particle) {
        this(() -> fixed, shape, particle);
    }

    public ShapeAnimation spinDegrees(double degreesPerTick) {
        this.yawPerTick = Math.toRadians(degreesPerTick);
        return this;
    }

    public ShapeAnimation tumble(double pitchDeg, double rollDeg) {
        this.pitchPerTick = Math.toRadians(pitchDeg);
        this.rollPerTick = Math.toRadians(rollDeg);
        return this;
    }

    public ShapeAnimation yOffset(double y) {
        this.yOffset = y;
        return this;
    }

    /**
     * Makes the shape drift vertically each tick.
     *
     * @param perTick blocks per tick; negative values sink
     * @return this animation
     */
    public ShapeAnimation rise(double perTick) {
        this.yPerTick = perTick;
        return this;
    }

    /**
     * Interpolates the shape's scale across the animation's lifetime.
     *
     * <p>Has no effect unless {@link #duration(int)} is set, since progress is
     * undefined for an endless animation.
     *
     * @param from   scale at the start
     * @param to     scale at the end
     * @param easing curve to interpolate along
     * @return this animation
     */
    public ShapeAnimation scaleOverTime(double from, double to, Easing easing) {
        this.scaleFrom = from;
        this.scaleTo = to;
        this.easing = easing;
        return this;
    }

    /**
     * Cycles the particle colour automatically, producing a rainbow.
     *
     * @param speed hue increment per tick; around {@code 0.01} reads well
     * @return this animation
     */
    public ShapeAnimation rainbow(float speed) {
        this.colorCycle = true;
        this.hueSpeed = speed;
        return this;
    }

    // Covariant overrides so the inherited fluent methods can be chained with
    // this class's own without an intermediate cast.

    @Override
    public ShapeAnimation interval(int interval) {
        super.interval(interval);
        return this;
    }

    @Override
    public ShapeAnimation duration(int ticks) {
        super.duration(ticks);
        return this;
    }

    @Override
    public ShapeAnimation onEnd(Runnable callback) {
        super.onEnd(callback);
        return this;
    }

    @Override
    public ShapeAnimation start() {
        super.start();
        return this;
    }

    @Override
    protected void update(long tick) {
        Location location = origin.get();
        if (location == null || location.getWorld() == null) {
            stop();
            return;
        }

        Shape current = shape;

        if (duration() > 0 && scaleFrom != scaleTo) {
            current = current.scale(easing.between(scaleFrom, scaleTo, progress()));
        }
        if (yawPerTick != 0) current = current.rotateY(yawPerTick * tick);
        if (pitchPerTick != 0) current = current.rotateX(pitchPerTick * tick);
        if (rollPerTick != 0) current = current.rotateZ(rollPerTick * tick);

        if (colorCycle) {
            particle.hue((tick * hueSpeed) % 1f);
        }

        Location at = location.clone().add(0, yOffset + yPerTick * tick, 0);
        particle.spawn(at, current);
    }
}
