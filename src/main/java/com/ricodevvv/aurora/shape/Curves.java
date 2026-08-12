package com.ricodevvv.aurora.shape;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Curvas y trayectorias. Todo devuelve offsets relativos al primer punto. */
public final class Curves {

    private Curves() {
    }

    /** Bezier cuadratica: A -> control -> B. Perfecta para lanzamientos y arcos. */
    public static Shape bezier(Vector from, Vector control, Vector to, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            double inv = 1 - t;
            list.add(new Vector(
                    inv * inv * from.getX() + 2 * inv * t * control.getX() + t * t * to.getX(),
                    inv * inv * from.getY() + 2 * inv * t * control.getY() + t * t * to.getY(),
                    inv * inv * from.getZ() + 2 * inv * t * control.getZ() + t * t * to.getZ()));
        }
        return Shape.of(list);
    }

    /** Bezier cubica con dos controles. */
    public static Shape bezier(Vector from, Vector c1, Vector c2, Vector to, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            double inv = 1 - t;
            double a = inv * inv * inv, b = 3 * inv * inv * t, c = 3 * inv * t * t, d = t * t * t;
            list.add(new Vector(
                    a * from.getX() + b * c1.getX() + c * c2.getX() + d * to.getX(),
                    a * from.getY() + b * c1.getY() + c * c2.getY() + d * to.getY(),
                    a * from.getZ() + b * c1.getZ() + c * c2.getZ() + d * to.getZ()));
        }
        return Shape.of(list);
    }

    /** Arco balistico entre dos puntos con altura extra. Ideal para proyectiles falsos. */
    public static Shape arcBetween(Location from, Location to, double height, int points) {
        Vector start = new Vector(0, 0, 0);
        Vector end = to.toVector().subtract(from.toVector());
        Vector control = new Vector(end.getX() / 2, Math.max(end.getY(), 0) / 2 + height, end.getZ() / 2);
        return bezier(start, control, end, points);
    }

    /**
     * Spline suave que pasa por todos los puntos dados (Catmull-Rom).
     * Util para rutas de camara, trails de proyectiles o caminos guiados.
     */
    public static Shape spline(List<Vector> waypoints, int pointsPerSegment) {
        List<Vector> list = new ArrayList<>();
        if (waypoints.size() < 2) return Shape.of(new ArrayList<>(waypoints));

        for (int i = 0; i < waypoints.size() - 1; i++) {
            Vector p0 = waypoints.get(Math.max(0, i - 1));
            Vector p1 = waypoints.get(i);
            Vector p2 = waypoints.get(i + 1);
            Vector p3 = waypoints.get(Math.min(waypoints.size() - 1, i + 2));

            for (int j = 0; j < pointsPerSegment; j++) {
                double t = j / (double) pointsPerSegment;
                list.add(new Vector(
                        catmull(p0.getX(), p1.getX(), p2.getX(), p3.getX(), t),
                        catmull(p0.getY(), p1.getY(), p2.getY(), p3.getY(), t),
                        catmull(p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ(), t)));
            }
        }
        return Shape.of(list);
    }

    private static double catmull(double p0, double p1, double p2, double p3, double t) {
        return 0.5 * ((2 * p1)
                + (-p0 + p2) * t
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
                + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    /**
     * Rayo dentado entre dos puntos (desplazamiento de punto medio recursivo).
     * Cada llamada da un rayo distinto, asi que animado se ve vivo.
     */
    public static Shape lightning(Vector from, Vector to, double chaos, int detail) {
        List<Vector> points = new ArrayList<>();
        points.add(from.clone());
        points.add(to.clone());

        for (int pass = 0; pass < detail; pass++) {
            List<Vector> next = new ArrayList<>(points.size() * 2);
            double offset = chaos / (pass + 1);
            for (int i = 0; i < points.size() - 1; i++) {
                Vector a = points.get(i), b = points.get(i + 1);
                next.add(a);
                Vector mid = a.clone().add(b.clone().subtract(a).multiply(0.5));
                mid.add(new Vector(rand(offset), rand(offset), rand(offset)));
                next.add(mid);
            }
            next.add(points.get(points.size() - 1));
            points = next;
        }

        // Densificamos para que se vea como una linea y no como puntos sueltos
        List<Vector> dense = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            Vector a = points.get(i), b = points.get(i + 1);
            for (int s = 0; s < 4; s++) {
                dense.add(com.ricodevvv.aurora.util.VectorMath.lerp(a, b, s / 4D));
            }
        }
        return Shape.of(dense);
    }

    /** Espiral plana (tipo remolino en el suelo). */
    public static Shape spiralFlat(double radius, double turns, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double angle = Math.PI * 2 * turns * t;
            double r = radius * t;
            list.add(new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r));
        }
        return Shape.of(list);
    }

    private static double rand(double amount) {
        return (ThreadLocalRandom.current().nextDouble() * 2 - 1) * amount;
    }
}
