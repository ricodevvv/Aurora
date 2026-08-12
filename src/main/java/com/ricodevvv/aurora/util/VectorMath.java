package com.ricodevvv.aurora.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class VectorMath {

    private VectorMath() {
    }

    public static Vector rotateX(Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double y = v.getY() * cos - v.getZ() * sin;
        double z = v.getY() * sin + v.getZ() * cos;
        return v.setY(y).setZ(z);
    }

    public static Vector rotateY(Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        return v.setX(x).setZ(z);
    }

    public static Vector rotateZ(Vector v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double x = v.getX() * cos - v.getY() * sin;
        double y = v.getX() * sin + v.getY() * cos;
        return v.setX(x).setY(y);
    }

    /** Orienta un vector "plano" (construido en XZ) para que apunte hacia donde mira la location. */
    public static Vector rotateToDirection(Vector v, Location facing) {
        double yaw = Math.toRadians(-(facing.getYaw() + 90));
        double pitch = Math.toRadians(-facing.getPitch());
        rotateZ(v, pitch);
        rotateY(v, yaw);
        return v;
    }

    public static Vector lerp(Vector a, Vector b, double t) {
        return new Vector(
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t);
    }
}
