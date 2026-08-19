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

    /**
     * Combines this shape with another, producing one point list.
     *
     * <p>Layered effects live on this: a bright core and a dim outer shell
     * drawn as one shape resolve their audience once instead of twice.
     *
     * @param other shape to append
     * @return the combined shape
     */
    default Shape plus(Shape other) {
        List<Vector> combined = new ArrayList<>(points().size() + other.points().size());
        combined.addAll(points());
        combined.addAll(other.points());
        return of(combined);
    }

    /**
     * Displaces every point by a small random amount.
     *
     * <p>Geometry drawn exactly is what makes a particle effect look like a
     * spreadsheet. A jitter of a few centimetres is enough to read as smoke,
     * fire or magic rather than as a wireframe.
     *
     * @param amount maximum displacement per axis, in blocks
     * @return a roughened copy
     */
    default Shape jitter(double amount) {
        java.util.concurrent.ThreadLocalRandom random =
                java.util.concurrent.ThreadLocalRandom.current();
        return transform(vector -> vector.add(new Vector(
                (random.nextDouble() * 2 - 1) * amount,
                (random.nextDouble() * 2 - 1) * amount,
                (random.nextDouble() * 2 - 1) * amount)));
    }

    /**
     * Keeps only part of the shape, measured from its first point.
     *
     * <p>Feeding this a rising value is how a shape draws itself on: a rune
     * circle that completes over half a second reads as a summon, the same
     * circle appearing whole reads as a texture.
     *
     * @param fraction how much of the shape to keep, in {@code 0..1}
     * @return the partial shape
     */
    default Shape take(double fraction) {
        List<Vector> all = points();
        int keep = (int) Math.round(all.size() * Math.max(0, Math.min(1, fraction)));
        if (keep >= all.size()) return this;
        return of(new ArrayList<>(all.subList(0, Math.max(0, keep))));
    }

    /**
     * Orients a shape built in the XZ or XY plane to sit on a player's back,
     * without needing their {@link Location}.
     *
     * @param yawDegrees the wearer's yaw, in degrees
     * @return an oriented copy
     */
    default Shape facingYaw(double yawDegrees) {
        return rotateY(Math.toRadians(-yawDegrees));
    }

    /**
     * @return a shape with no points, useful as a neutral element
     */
    static Shape empty() {
        return of(java.util.Collections.emptyList());
    }
}
