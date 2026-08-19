package com.ricodevvv.aurora.shape;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue of geometric shapes. Every shape is generated centred on the
 * origin, so it can be dropped at any location without further translation.
 *
 * <p>Point counts are the cost knob: a shape is drawn once per particle spawn,
 * so a 200-point sphere is 200 particles per frame.
 */
public final class Shapes {

    private static final double TAU = Math.PI * 2;

    private Shapes() {
    }

    public static Shape circle(double radius, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = TAU * i / points;
            list.add(new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
        }
        return Shape.of(list);
    }

    /**
     * Partial arc, useful for circular progress bars.
     *
     * @param radius  arc radius
     * @param points  number of points
     * @param fromDeg start angle in degrees
     * @param toDeg   end angle in degrees
     * @return the arc
     */
    public static Shape arc(double radius, int points, double fromDeg, double toDeg) {
        List<Vector> list = new ArrayList<>(points);
        double from = Math.toRadians(fromDeg), to = Math.toRadians(toDeg);
        for (int i = 0; i < points; i++) {
            double angle = from + (to - from) * (i / (double) Math.max(1, points - 1));
            list.add(new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
        }
        return Shape.of(list);
    }

    /**
     * Sphere using a Fibonacci distribution, which spreads points evenly
     * instead of bunching them at the poles the way naive spherical
     * coordinates do.
     *
     * @param radius sphere radius
     * @param points number of points
     * @return the sphere
     */
    public static Shape sphere(double radius, int points) {
        List<Vector> list = new ArrayList<>(points);
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2;
            double r = Math.sqrt(1 - y * y);
            double theta = golden * i;
            list.add(new Vector(Math.cos(theta) * r * radius, y * radius, Math.sin(theta) * r * radius));
        }
        return Shape.of(list);
    }

    public static Shape helix(double radius, double height, double turns, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double angle = TAU * turns * t;
            list.add(new Vector(Math.cos(angle) * radius, height * t, Math.sin(angle) * radius));
        }
        return Shape.of(list);
    }

    /**
     * Twin helices, like a DNA strand.
     *
     * @param radius helix radius
     * @param height total height
     * @param turns  how many full turns
     * @param points points per strand
     * @return the double helix
     */
    public static Shape doubleHelix(double radius, double height, double turns, int points) {
        List<Vector> list = new ArrayList<>(points * 2);
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double angle = TAU * turns * t;
            double y = height * t;
            list.add(new Vector(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
            list.add(new Vector(Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius));
        }
        return Shape.of(list);
    }

    /**
     * Cone-shaped vortex whose radius grows with height.
     *
     * @param radius radius at the top
     * @param height total height
     * @param turns  how many full turns
     * @param points number of points
     * @return the vortex
     */
    public static Shape vortex(double radius, double height, double turns, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double angle = TAU * turns * t;
            double r = radius * t;
            list.add(new Vector(Math.cos(angle) * r, height * t, Math.sin(angle) * r));
        }
        return Shape.of(list);
    }

    public static Shape line(Vector from, Vector to, double spacing) {
        List<Vector> list = new ArrayList<>();
        Vector dir = to.clone().subtract(from);
        double length = dir.length();
        if (length == 0) {
            list.add(from.clone());
            return Shape.of(list);
        }
        dir.normalize().multiply(spacing);
        int steps = (int) (length / spacing);
        for (int i = 0; i <= steps; i++) {
            list.add(from.clone().add(dir.clone().multiply(i)));
        }
        return Shape.of(list);
    }

    /**
     * Wireframe cube: only the edges are populated.
     *
     * @param size    edge length
     * @param spacing distance between points
     * @return the cube outline
     */
    public static Shape cube(double size, double spacing) {
        List<Vector> list = new ArrayList<>();
        double h = size / 2;
        for (double x = -h; x <= h; x += spacing) {
            for (double y = -h; y <= h; y += spacing) {
                for (double z = -h; z <= h; z += spacing) {
                    int edges = 0;
                    if (Math.abs(Math.abs(x) - h) < 1e-6) edges++;
                    if (Math.abs(Math.abs(y) - h) < 1e-6) edges++;
                    if (Math.abs(Math.abs(z) - h) < 1e-6) edges++;
                    if (edges >= 2) list.add(new Vector(x, y, z));
                }
            }
        }
        return Shape.of(list);
    }

    public static Shape star(int spikes, double outer, double inner, int pointsPerEdge) {
        List<Vector> list = new ArrayList<>();
        int total = spikes * 2;
        Vector prev = null, first = null;
        for (int i = 0; i <= total; i++) {
            double angle = TAU * i / total;
            double r = (i % 2 == 0) ? outer : inner;
            Vector current = new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            if (first == null) first = current.clone();
            if (prev != null) {
                for (int p = 0; p < pointsPerEdge; p++) {
                    double t = p / (double) pointsPerEdge;
                    list.add(com.ricodevvv.aurora.util.VectorMath.lerp(prev, current, t));
                }
            }
            prev = current;
        }
        return Shape.of(list);
    }

    /**
     * Heart curve in the XY plane.
     *
     * @param scale  overall size
     * @param points number of points
     * @return the heart
     */
    public static Shape heart(double scale, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = TAU * i / points;
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            list.add(new Vector(x * scale / 16, y * scale / 16, 0));
        }
        return Shape.of(list);
    }

    /**
     * Polar rose, {@code r = cos(k * theta)}. An odd {@code k} produces
     * {@code k} petals, an even one produces {@code 2k}.
     *
     * @param k      petal parameter
     * @param radius maximum radius
     * @param points number of points
     * @return the rose
     */
    public static Shape rose(int k, double radius, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double theta = TAU * i / points;
            double r = Math.cos(k * theta) * radius;
            list.add(new Vector(Math.cos(theta) * r, 0, Math.sin(theta) * r));
        }
        return Shape.of(list);
    }

    public static Shape torus(double majorRadius, double minorRadius, int majorPoints, int minorPoints) {
        List<Vector> list = new ArrayList<>(majorPoints * minorPoints);
        for (int i = 0; i < majorPoints; i++) {
            double u = TAU * i / majorPoints;
            for (int j = 0; j < minorPoints; j++) {
                double v = TAU * j / minorPoints;
                double r = majorRadius + minorRadius * Math.cos(v);
                list.add(new Vector(Math.cos(u) * r, minorRadius * Math.sin(v), Math.sin(u) * r));
            }
        }
        return Shape.of(list);
    }

    /**
     * Random points inside a sphere, for diffuse auras and dust clouds.
     *
     * @param radius sphere radius
     * @param points number of points
     * @return the cloud
     */
    public static Shape cloud(double radius, int points) {
        List<Vector> list = new ArrayList<>(points);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < points; i++) {
            double theta = random.nextDouble() * TAU;
            double phi = Math.acos(2 * random.nextDouble() - 1);
            double r = radius * Math.cbrt(random.nextDouble());
            list.add(new Vector(
                    r * Math.sin(phi) * Math.cos(theta),
                    r * Math.cos(phi),
                    r * Math.sin(phi) * Math.sin(theta)));
        }
        return Shape.of(list);
    }

    // ------------------------------------------------------------- additions

    public static Shape ellipse(double radiusX, double radiusZ, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = TAU * i / points;
            list.add(new Vector(Math.cos(angle) * radiusX, 0, Math.sin(angle) * radiusZ));
        }
        return Shape.of(list);
    }

    /**
     * Regular polygon: {@code 3} is a triangle, {@code 6} a hexagon.
     *
     * @param sides         number of sides
     * @param radius        circumradius
     * @param pointsPerEdge points along each edge
     * @return the polygon outline
     */
    public static Shape polygon(int sides, double radius, int pointsPerEdge) {
        List<Vector> list = new ArrayList<>(sides * pointsPerEdge);
        for (int i = 0; i < sides; i++) {
            double a1 = TAU * i / sides;
            double a2 = TAU * (i + 1) / sides;
            Vector from = new Vector(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            Vector to = new Vector(Math.cos(a2) * radius, 0, Math.sin(a2) * radius);
            for (int p = 0; p < pointsPerEdge; p++) {
                list.add(com.ricodevvv.aurora.util.VectorMath.lerp(from, to, p / (double) pointsPerEdge));
            }
        }
        return Shape.of(list);
    }

    /**
     * Upper hemisphere, for zone shields and protection domes.
     *
     * @param radius dome radius
     * @param points number of points
     * @return the dome
     */
    public static Shape dome(double radius, int points) {
        List<Vector> list = new ArrayList<>(points);
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) points);
            double r = Math.sqrt(1 - y * y);
            double theta = golden * i;
            list.add(new Vector(Math.cos(theta) * r * radius, y * radius, Math.sin(theta) * r * radius));
        }
        return Shape.of(list);
    }

    /**
     * Flat grid, for marking out areas and safe zones.
     *
     * @param width   size along X
     * @param depth   size along Z
     * @param spacing distance between points
     * @return the grid
     */
    public static Shape grid(double width, double depth, double spacing) {
        List<Vector> list = new ArrayList<>();
        for (double x = -width / 2; x <= width / 2; x += spacing) {
            for (double z = -depth / 2; z <= depth / 2; z += spacing) {
                list.add(new Vector(x, 0, z));
            }
        }
        return Shape.of(list);
    }

    /**
     * Three intersecting rings, like a classic atom diagram.
     *
     * @param radius         ring radius
     * @param pointsPerRing  points in each ring
     * @return the atom shape
     */
    public static Shape atom(double radius, int pointsPerRing) {
        List<Vector> list = new ArrayList<>(pointsPerRing * 3);
        list.addAll(circle(radius, pointsPerRing).points());
        list.addAll(circle(radius, pointsPerRing).rotateX(Math.toRadians(60)).points());
        list.addAll(circle(radius, pointsPerRing).rotateX(Math.toRadians(-60)).points());
        return Shape.of(list);
    }

    /**
     * Feathered wings in the XY plane. Rotate by a player's yaw to sit them on
     * their back.
     *
     * <p>The curvature constants were tuned by eye rather than derived; adjust
     * the internal bend factor for droopier or straighter feathers.
     *
     * @param size             wingspan
     * @param feathers         feathers per wing
     * @param pointsPerFeather points along each feather
     * @return the wings
     */
    public static Shape wings(double size, int feathers, int pointsPerFeather) {
        List<Vector> list = new ArrayList<>(feathers * pointsPerFeather * 2);
        for (int f = 0; f < feathers; f++) {
            double spread = Math.toRadians(15 + f * (65.0 / Math.max(1, feathers - 1)));
            double length = size * (1 - 0.35 * Math.abs(f - (feathers - 1) / 2.0) / Math.max(1, feathers / 2.0));
            for (int p = 1; p <= pointsPerFeather; p++) {
                double t = p / (double) pointsPerFeather;
                double bend = t * t * size * 0.45;
                double x = Math.cos(spread) * length * t;
                double y = Math.sin(spread) * length * t - bend;
                list.add(new Vector(x, y, 0));
                list.add(new Vector(-x, y, 0));
            }
        }
        return Shape.of(list);
    }

    /**
     * Ring with outward spikes, for summoning circles.
     *
     * @param radius      ring radius
     * @param spikes      number of spikes
     * @param spikeLength how far each spike extends
     * @param ringPoints  points in the ring itself
     * @return the rune circle
     */
    public static Shape runeCircle(double radius, int spikes, double spikeLength, int ringPoints) {
        List<Vector> list = new ArrayList<>(circle(radius, ringPoints).points());
        for (int i = 0; i < spikes; i++) {
            double angle = TAU * i / spikes;
            for (int p = 0; p < 5; p++) {
                double r = radius + spikeLength * (p / 5D);
                list.add(new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r));
            }
        }
        return Shape.of(list);
    }

    // ------------------------------------------------------- premium shapes

    /**
     * A sphere traced as one continuous spiral from pole to pole.
     *
     * <p>Unlike {@link #sphere(double, int)}, whose points are in scattered
     * order, this one is generated in a single sweep. That is what makes it
     * usable with a {@link com.ricodevvv.aurora.util.ColorRamp}: the ramp runs
     * from the bottom of the orb to the top instead of speckling it.
     *
     * @param radius sphere radius
     * @param turns  how many times the spiral wraps around
     * @param points number of points
     * @return the orb
     */
    public static Shape spiralSphere(double radius, double turns, int points) {
        List<Vector> list = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = i / (double) Math.max(1, points - 1);
            double y = 1 - 2 * t;
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double angle = TAU * turns * t;
            list.add(new Vector(Math.cos(angle) * r * radius, y * radius, Math.sin(angle) * r * radius));
        }
        return Shape.of(list);
    }

    /**
     * Several rings sharing a centre, each tilted a little further than the
     * last, like a gyroscope.
     *
     * @param radius        ring radius
     * @param rings         how many rings
     * @param pointsPerRing points in each ring
     * @return the orbit rings
     */
    public static Shape orbit(double radius, int rings, int pointsPerRing) {
        List<Vector> list = new ArrayList<>(rings * pointsPerRing);
        for (int i = 0; i < rings; i++) {
            double tilt = Math.PI * i / Math.max(1, rings);
            list.addAll(circle(radius, pointsPerRing).rotateX(tilt).rotateY(tilt * 0.5).points());
        }
        return Shape.of(list);
    }

    /**
     * Flat spiral arms sweeping out from a centre, as a galaxy seen from above.
     *
     * @param radius outer radius
     * @param arms   how many arms
     * @param points points per arm
     * @return the galaxy
     */
    public static Shape galaxy(double radius, int arms, int points) {
        List<Vector> list = new ArrayList<>(arms * points);
        for (int arm = 0; arm < arms; arm++) {
            double offset = TAU * arm / Math.max(1, arms);
            for (int i = 0; i < points; i++) {
                double t = i / (double) points;
                // Logarithmic rather than linear, so the arms sweep instead of
                // radiating straight out.
                double angle = offset + Math.log(1 + t * 9) * 1.6;
                double r = radius * t;
                list.add(new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r));
            }
        }
        return Shape.of(list);
    }

    /**
     * A ring of upward spikes, for crowns and rank halos.
     *
     * @param radius     ring radius
     * @param spikes     how many spikes
     * @param height     spike height
     * @param ringPoints points in the ring itself
     * @return the crown
     */
    public static Shape crown(double radius, int spikes, double height, int ringPoints) {
        List<Vector> list = new ArrayList<>(circle(radius, ringPoints).points());
        for (int i = 0; i < spikes; i++) {
            double angle = TAU * i / Math.max(1, spikes);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            for (int p = 1; p <= 4; p++) {
                double t = p / 4.0;
                list.add(new Vector(x * (1 - t * 0.25), height * t, z * (1 - t * 0.25)));
            }
        }
        return Shape.of(list);
    }

    /**
     * A flower drawn as overlapping petal arcs, flat in the XZ plane.
     *
     * @param petals how many petals
     * @param radius petal length
     * @param points points per petal
     * @return the flower
     */
    public static Shape flower(int petals, double radius, int points) {
        List<Vector> list = new ArrayList<>(petals * points);
        for (int petal = 0; petal < petals; petal++) {
            double centre = TAU * petal / Math.max(1, petals);
            for (int i = 0; i < points; i++) {
                double t = i / (double) points;
                double angle = centre + (t - 0.5) * (TAU / petals) * 1.6;
                double r = radius * Math.sin(Math.PI * t);
                list.add(new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r));
            }
        }
        return Shape.of(list);
    }

    /**
     * A crescent moon in the XY plane: an arc with a second arc bitten out of it.
     *
     * @param radius outer radius
     * @param points number of points
     * @return the crescent
     */
    public static Shape crescent(double radius, int points) {
        List<Vector> list = new ArrayList<>(points);
        int half = Math.max(2, points / 2);
        for (int i = 0; i < half; i++) {
            double angle = Math.PI * i / (half - 1) - Math.PI / 2;
            list.add(new Vector(Math.cos(angle) * radius, Math.sin(angle) * radius, 0));
        }
        for (int i = 0; i < half; i++) {
            double angle = Math.PI * i / (half - 1) - Math.PI / 2;
            list.add(new Vector(Math.cos(angle) * radius * 0.55 + radius * 0.35,
                    Math.sin(angle) * radius * 0.92, 0));
        }
        return Shape.of(list);
    }

    /**
     * Bat or dragon wings: a swept bone with a membrane hanging between each
     * pair of fingers.
     *
     * <p>Where {@link #wings(double, int, int)} draws separate feathers, this
     * draws a continuous surface, which is what distinguishes a demon from an
     * angel at twenty blocks.
     *
     * @param size    wingspan
     * @param fingers how many bones per wing
     * @param points  points along each bone
     * @return the wings
     */
    public static Shape dragonWings(double size, int fingers, int points) {
        List<Vector> list = new ArrayList<>(fingers * points * 2);
        for (int finger = 0; finger < fingers; finger++) {
            double spread = Math.toRadians(10 + finger * (70.0 / Math.max(1, fingers - 1)));
            double length = size * (1 - 0.15 * finger / Math.max(1, fingers - 1.0));
            for (int p = 1; p <= points; p++) {
                double t = p / (double) points;
                // The membrane sags between bones; the sag grows towards the tip.
                double sag = Math.sin(Math.PI * t) * size * 0.22 * (finger / (double) fingers);
                double x = Math.cos(spread) * length * t;
                double y = Math.sin(spread) * length * t - sag;
                list.add(new Vector(x, y, 0));
                list.add(new Vector(-x, y, 0));
            }
        }
        return Shape.of(list);
    }

    /**
     * Butterfly wings, from the polar butterfly curve, in the XY plane.
     *
     * @param size   wingspan
     * @param points number of points
     * @return the wings
     */
    public static Shape butterflyWings(double size, int points) {
        List<Vector> list = new ArrayList<>(points * 2);
        for (int i = 0; i < points; i++) {
            double t = TAU * i / points;
            double r = Math.exp(Math.cos(t)) - 2 * Math.cos(4 * t) + Math.pow(Math.sin(t / 12), 5);
            double x = Math.sin(t) * r * size / 4;
            double y = Math.cos(t) * r * size / 4;
            list.add(new Vector(x, y, 0));
            list.add(new Vector(-x, y, 0));
        }
        return Shape.of(list);
    }

    /**
     * Hollow cone standing on its point, for funnels and containment fields.
     *
     * @param radius radius at the top
     * @param height total height
     * @param rings  how many rings stack up the cone
     * @param points points per ring
     * @return the cone
     */
    public static Shape cone(double radius, double height, int rings, int points) {
        List<Vector> list = new ArrayList<>(rings * points);
        for (int ring = 0; ring < rings; ring++) {
            double t = ring / (double) Math.max(1, rings - 1);
            double r = radius * t;
            double y = height * t;
            for (int i = 0; i < points; i++) {
                double angle = TAU * i / points + t * 1.2;
                list.add(new Vector(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
        }
        return Shape.of(list);
    }

    /**
     * A column of points scattered in a cylinder, for pillars of light and
     * rising auras.
     *
     * @param radius cylinder radius
     * @param height cylinder height
     * @param points number of points
     * @return the column
     */
    public static Shape column(double radius, double height, int points) {
        List<Vector> list = new ArrayList<>(points);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < points; i++) {
            double angle = random.nextDouble() * TAU;
            double r = radius * Math.sqrt(random.nextDouble());
            list.add(new Vector(Math.cos(angle) * r, random.nextDouble() * height, Math.sin(angle) * r));
        }
        return Shape.of(list);
    }
}
