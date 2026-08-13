package com.ricodevvv.aurora.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Vector rotation and interpolation helpers.
 *
 * <p>The rotation methods mutate and return the vector they are given rather
 * than allocating a new one. Shape transforms run over hundreds of points every
 * tick, so the caller is expected to pass a copy when it needs one; {@link
 * com.ricodevvv.aurora.shape.Shape} already does.
 */
public final class VectorMath {

    private VectorMath() {
    }

    /**
     * Rotates the vector around the X axis.
     *
     * @param vector  vector to rotate, mutated in place
     * @param radians angle in radians
     * @return the same vector, rotated
     */
    public static Vector rotateX(Vector vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double y = vector.getY() * cos - vector.getZ() * sin;
        double z = vector.getY() * sin + vector.getZ() * cos;
        return vector.setY(y).setZ(z);
    }

    /**
     * Rotates the vector around the Y axis. This is the common case: horizontal
     * spin, and the one used to align shapes with a player's yaw.
     *
     * @param vector  vector to rotate, mutated in place
     * @param radians angle in radians
     * @return the same vector, rotated
     */
    public static Vector rotateY(Vector vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.getX() * cos + vector.getZ() * sin;
        double z = vector.getX() * -sin + vector.getZ() * cos;
        return vector.setX(x).setZ(z);
    }

    /**
     * Rotates the vector around the Z axis.
     *
     * @param vector  vector to rotate, mutated in place
     * @param radians angle in radians
     * @return the same vector, rotated
     */
    public static Vector rotateZ(Vector vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vector.getX() * cos - vector.getY() * sin;
        double y = vector.getX() * sin + vector.getY() * cos;
        return vector.setX(x).setY(y);
    }

    /**
     * Orients a vector built in the XZ plane so that it faces the direction a
     * location is looking. Used for shapes that should appear in front of a
     * player, such as a slash arc or a frontal shield.
     *
     * @param vector vector to orient, mutated in place
     * @param facing location whose yaw and pitch define the direction
     * @return the same vector, oriented
     */
    public static Vector rotateToDirection(Vector vector, Location facing) {
        rotateZ(vector, Math.toRadians(-facing.getPitch()));
        rotateY(vector, Math.toRadians(-(facing.getYaw() + 90)));
        return vector;
    }

    /**
     * Linearly interpolates between two vectors.
     *
     * @param from     start vector
     * @param to       end vector
     * @param progress progress in {@code 0..1}
     * @return a new vector between the two; the inputs are left untouched
     */
    public static Vector lerp(Vector from, Vector to, double progress) {
        return new Vector(
                from.getX() + (to.getX() - from.getX()) * progress,
                from.getY() + (to.getY() - from.getY()) * progress,
                from.getZ() + (to.getZ() - from.getZ()) * progress);
    }
}
