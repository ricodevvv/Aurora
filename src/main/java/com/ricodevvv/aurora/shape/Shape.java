package com.ricodevvv.aurora.shape;

import com.ricodevvv.aurora.util.VectorMath;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A geometric figure, expressed as offsets relative to an origin.
 *
 * <p>Shapes are immutable: every transform returns a new shape and leaves the
 * receiver untouched, so a shape can be built once as a constant and reused
 * across ticks and players without defensive copying.
 *
 * <p>Because transforms allocate, build static geometry once and only transform
 * what actually changes per tick:
 *
 * <pre>{@code
 * private static final Shape RING = Shapes.circle(1, 24);
 * // per tick:
 * particle.spawn(origin, RING.scale(radius).rotateY(spin));
 * }</pre>
 *
 * @see Shapes
 * @see Curves
 * @see Glyphs
 */
@FunctionalInterface
public interface Shape {

    /**
     * @return the points making up this shape, as offsets from the origin
     */
    List<Vector> points();

    /**
     * Wraps a point list as a shape.
     *
     * @param points the points; not copied, so do not mutate them afterwards
     * @return a shape over those points
     */
    static Shape of(List<Vector> points) {
        return () -> points;
    }

    /**
     * Rotates the shape around the Y axis: horizontal spin.
     *
     * @param radians angle in radians
     * @return a rotated copy
     */
    default Shape rotateY(double radians) {
        return transform(vector -> VectorMath.rotateY(vector, radians));
    }

    /**
     * Rotates the shape around the X axis: forward tilt.
     *
     * @param radians angle in radians
     * @return a rotated copy
     */
    default Shape rotateX(double radians) {
        return transform(vector -> VectorMath.rotateX(vector, radians));
    }

    /**
     * Rotates the shape around the Z axis: sideways roll.
     *
     * @param radians angle in radians
     * @return a rotated copy
     */
    default Shape rotateZ(double radians) {
        return transform(vector -> VectorMath.rotateZ(vector, radians));
    }

    /**
     * Uniformly resizes the shape.
     *
     * @param factor scale factor
     * @return a scaled copy
     */
    default Shape scale(double factor) {
        return transform(vector -> vector.multiply(factor));
    }

    /**
     * Shifts every point by a fixed amount.
     *
     * @param x offset along X
     * @param y offset along Y
     * @param z offset along Z
     * @return a translated copy
     */
    default Shape translate(double x, double y, double z) {
        return transform(vector -> vector.add(new Vector(x, y, z)));
    }

    /**
     * Orients the shape to face the direction a location is looking, which is
     * how a flat figure ends up readable in front of a player.
     *
     * @param location location whose yaw and pitch define the facing
     * @return an oriented copy
     */
    default Shape facing(Location location) {
        return transform(vector -> VectorMath.rotateToDirection(vector, location));
    }

    /**
     * Applies an arbitrary transform to every point.
     *
     * @param operator transform to apply; it receives a copy and may mutate it
     * @return a transformed copy
     */
    default Shape transform(UnaryOperator<Vector> operator) {
        List<Vector> transformed = new ArrayList<>(points().size());
        for (Vector point : points()) {
            transformed.add(operator.apply(point.clone()));
        }
        return of(transformed);
    }
}
