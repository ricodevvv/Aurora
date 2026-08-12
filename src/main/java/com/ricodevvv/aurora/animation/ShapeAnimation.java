package com.ricodevvv.aurora.animation;

import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.util.Easing;
import org.bukkit.Location;

import java.util.function.Supplier;

/**
 * Animacion generica: toma una figura y la dibuja cada tick aplicandole
 * rotacion, escala y desplazamiento vertical en funcion del tiempo.
 *
 * Con esto solo ya sacas la mayoria de efectos (auras, escudos, portales,
 * anillos de invocacion) sin escribir una clase nueva.
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

    /** Hace que la figura suba (o baje) cada tick. */
    public ShapeAnimation rise(double perTick) {
        this.yPerTick = perTick;
        return this;
    }

    /** Escala interpolada a lo largo de la duracion. Requiere duration(). */
    public ShapeAnimation scaleOverTime(double from, double to, Easing easing) {
        this.scaleFrom = from;
        this.scaleTo = to;
        this.easing = easing;
        return this;
    }

    /** Cicla el color del dust automaticamente (efecto arcoiris). */
    public ShapeAnimation rainbow(float speed) {
        this.colorCycle = true;
        this.hueSpeed = speed;
        return this;
    }

    // Sobrescribimos los fluent de Animation con retorno covariante para poder
    // encadenar duration()/interval() junto con los metodos de esta clase.

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
