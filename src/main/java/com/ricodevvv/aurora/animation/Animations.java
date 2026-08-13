package com.ricodevvv.aurora.animation;

import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.cryptomorin.xseries.particles.XParticle;
import com.ricodevvv.aurora.shape.Curves;
import com.ricodevvv.aurora.shape.Glyphs;
import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.shape.Shapes;
import com.ricodevvv.aurora.util.Colors;
import com.ricodevvv.aurora.util.Easing;
import com.ricodevvv.aurora.util.VectorMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Supplier;

/**
 * Ready-made animation presets.
 *
 * <p>Every method returns a stopped animation, so the caller decides when it
 * begins and can still adjust {@code duration} or {@code interval} first:
 *
 * <pre>{@code
 * Animations.shield(player, particle, 1.6).duration(200).start();
 * }</pre>
 */
public final class Animations {

    private Animations() {
    }

    /**
     * Supplies an entity's position, offset vertically, invalidating once the
     * entity is gone.
     *
     * @param entity  entity to track
     * @param yOffset height above the entity
     * @return a supplier yielding {@code null} once the entity is invalid
     */
    public static Supplier<Location> follow(Entity entity, double yOffset) {
        return () -> entity.isValid() ? entity.getLocation().add(0, yOffset, 0) : null;
    }

    /**
     * A ring rotating around an entity.
     *
     * @param entity   entity to orbit
     * @param particle particle to draw with
     * @param radius   ring radius
     * @param points   points in the ring
     * @return the animation, not yet started
     */
    public static ShapeAnimation aura(Entity entity, ParticleBuilder particle, double radius, int points) {
        return new ShapeAnimation(follow(entity, 0.1), Shapes.circle(radius, points), particle)
                .spinDegrees(8);
    }

    /**
     * Twin helices climbing around an entity and looping.
     *
     * @param entity         entity to wrap
     * @param particle       particle to draw with
     * @param radius         helix radius
     * @param height         height before looping
     * @param pointsPerTick  points emitted per tick
     * @return the animation, not yet started
     */
    public static Animation helix(Entity entity, ParticleBuilder particle,
                                  double radius, double height, int pointsPerTick) {
        return new Animation() {
            double y = 0;

            @Override
            protected void update(long tick) {
                if (!entity.isValid()) {
                    stop();
                    return;
                }
                Location base = entity.getLocation();
                for (int i = 0; i < pointsPerTick; i++) {
                    double angle = (y / height) * Math.PI * 4 + (i * 0.4);
                    particle.spawn(base.clone().add(
                            Math.cos(angle) * radius, y, Math.sin(angle) * radius));
                    particle.spawn(base.clone().add(
                            Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius));
                }
                y += height / 20;
                if (y > height) y = 0;
            }
        };
    }

    /**
     * An expanding ring, for impacts, spawns and kill effects.
     *
     * @param center   centre of the wave
     * @param particle particle to draw with
     * @param from     starting radius
     * @param to       final radius
     * @param ticks    duration in ticks
     * @param points   points in the ring
     * @return the animation, not yet started
     */
    public static ShapeAnimation ringWave(Location center, ParticleBuilder particle,
                                          double from, double to, int ticks, int points) {
        return new ShapeAnimation(center, Shapes.circle(1, points), particle)
                .scaleOverTime(from, to, Easing.EASE_OUT_CUBIC)
                .spinDegrees(3)
                .duration(ticks);
    }

    /**
     * A sphere that swells outward.
     *
     * @param center   centre of the burst
     * @param particle particle to draw with
     * @param radius   final radius
     * @param ticks    duration in ticks
     * @param points   points in the sphere
     * @return the animation, not yet started
     */
    public static ShapeAnimation burst(Location center, ParticleBuilder particle,
                                       double radius, int ticks, int points) {
        return new ShapeAnimation(center, Shapes.sphere(1, points), particle)
                .scaleOverTime(0.1, radius, Easing.EASE_OUT_QUAD)
                .duration(ticks);
    }

    /**
     * A beam travelling from one point to another.
     *
     * @param from          start location
     * @param to            end location
     * @param particle      particle to draw with
     * @param blocksPerTick travel speed
     * @param spacing       distance between points
     * @return the animation, not yet started
     */
    public static Animation beam(Location from, Location to, ParticleBuilder particle,
                                 double blocksPerTick, double spacing) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        return new Animation() {
            double travelled = 0;

            @Override
            protected void update(long tick) {
                double next = Math.min(length, travelled + blocksPerTick);
                Shape segment = Shapes.line(
                        direction.clone().normalize().multiply(travelled),
                        direction.clone().normalize().multiply(next),
                        spacing);
                particle.spawn(from, segment);
                travelled = next;
                if (travelled >= length) stop();
            }
        };
    }

    /**
     * A trail following an entity: arrows, projectiles or players.
     *
     * @param entity   entity to follow
     * @param particle particle to draw with
     * @param yOffset  height above the entity
     * @return the animation, not yet started
     */
    public static Animation trail(Entity entity, ParticleBuilder particle, double yOffset) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!entity.isValid()) {
                    stop();
                    return;
                }
                particle.spawn(entity.getLocation().add(0, yOffset, 0));
            }
        };
    }

    /**
     * Several orbs circling a point at different phases, with a vertical wave.
     *
     * @param center    supplies the centre each tick
     * @param particle  particle to draw with
     * @param orbiters  how many orbs
     * @param radius    orbit radius
     * @param speedDeg  angular speed in degrees per tick
     * @param wave      vertical wave amplitude
     * @return the animation, not yet started
     */
    public static Animation orbit(Supplier<Location> center, ParticleBuilder particle,
                                  int orbiters, double radius, double speedDeg, double wave) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                Location location = center.get();
                if (location == null) {
                    stop();
                    return;
                }
                double base = Math.toRadians(speedDeg) * tick;
                for (int i = 0; i < orbiters; i++) {
                    double angle = base + (Math.PI * 2 * i / orbiters);
                    double y = Math.sin(base * 2 + i) * wave;
                    particle.spawn(location.clone().add(
                            Math.cos(angle) * radius, y, Math.sin(angle) * radius));
                }
            }
        };
    }

    /**
     * A rising, spinning vortex, for summoning moments.
     *
     * @param center   centre of the vortex
     * @param particle particle to draw with
     * @param radius   radius at the top
     * @param height   total height
     * @param points   number of points
     * @return the animation, not yet started
     */
    public static ShapeAnimation vortex(Location center, ParticleBuilder particle,
                                        double radius, double height, int points) {
        return new ShapeAnimation(center, Shapes.vortex(radius, height, 3, points), particle)
                .spinDegrees(12);
    }

    /**
     * A slowly rotating heart above a point.
     *
     * @param center   supplies the centre each tick
     * @param particle particle to draw with
     * @param scale    heart size
     * @return the animation, not yet started
     */
    public static ShapeAnimation heart(Supplier<Location> center, ParticleBuilder particle, double scale) {
        return new ShapeAnimation(center, Shapes.heart(scale, 60), particle)
                .spinDegrees(4)
                .yOffset(1.2);
    }

    /**
     * A spherical shield around an entity, tumbling on two axes.
     *
     * @param entity   entity to shield
     * @param particle particle to draw with
     * @param radius   shield radius
     * @return the animation, not yet started
     */
    public static ShapeAnimation shield(Entity entity, ParticleBuilder particle, double radius) {
        return new ShapeAnimation(follow(entity, 1.0), Shapes.sphere(radius, 80), particle)
                .spinDegrees(5)
                .tumble(2, 0);
    }

    /**
     * A forward-facing arc, for melee slash effects.
     *
     * @param origin   position and facing of the slash
     * @param particle particle to draw with
     * @param radius   arc radius
     * @param ticks    duration in ticks
     * @return the animation, not yet started
     */
    public static Animation slash(Location origin, ParticleBuilder particle, double radius, int ticks) {
        Shape arc = Shapes.arc(radius, 20, -60, 60).facing(origin);
        return new Animation() {
            @Override
            protected void update(long tick) {
                double t = tick / (double) ticks;
                particle.spawn(origin.clone().add(
                        VectorMath.lerp(new Vector(0, 0, 0), origin.getDirection().multiply(radius * 0.5), t)), arc);
            }
        }.duration(ticks);
    }

    // ------------------------------------------------------------- additions

    /**
     * A jagged bolt between two points, regenerated each frame so it flickers.
     *
     * @param from     start location
     * @param to       end location
     * @param particle particle to draw with
     * @param chaos    displacement magnitude
     * @param ticks    duration in ticks
     * @return the animation, not yet started
     */
    public static Animation lightning(Location from, Location to, ParticleBuilder particle,
                                      double chaos, int ticks) {
        Vector delta = to.toVector().subtract(from.toVector());
        return new Animation() {
            @Override
            protected void update(long tick) {
                particle.spawn(from, Curves.lightning(new Vector(0, 0, 0), delta, chaos, 4));
            }
        }.duration(ticks).interval(2);
    }

    /**
     * A bolt striking down from the sky. Purely visual; deals no damage.
     *
     * @param target   impact point
     * @param particle particle to draw with
     * @param height   how high the bolt starts
     * @return the animation, not yet started
     */
    public static Animation lightningStrike(Location target, ParticleBuilder particle, double height) {
        return lightning(target.clone().add(0, height, 0), target, particle, 0.8, 12);
    }

    /**
     * Wings on a player's back, turning with their yaw and beating gently.
     *
     * @param player   player wearing them
     * @param particle particle to draw with
     * @param size     wingspan
     * @return the animation, not yet started
     */
    public static Animation wings(Player player, ParticleBuilder particle, double size) {
        Shape base = Shapes.wings(size, 6, 8);
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!player.isValid()) {
                    stop();
                    return;
                }
                Location location = player.getLocation();
                double yaw = Math.toRadians(-location.getYaw());
                // Gentle beat: the wings open and close.
                double flap = 1 + Math.sin(tick * 0.12) * 0.12;
                particle.spawn(location.clone().add(0, 1.1, 0), base.scale(flap).rotateY(yaw));
            }
        };
    }

    /**
     * A halo rotating above an entity's head.
     *
     * @param entity   entity to crown
     * @param particle particle to draw with
     * @param radius   halo radius
     * @return the animation, not yet started
     */
    public static ShapeAnimation halo(Entity entity, ParticleBuilder particle, double radius) {
        return new ShapeAnimation(follow(entity, 2.3), Shapes.circle(radius, 24), particle)
                .spinDegrees(6);
    }

    /**
     * A layered tornado: higher layers are wider and spin faster, which is what
     * separates it visually from a plain rotating cylinder.
     *
     * @param center   base of the tornado
     * @param particle particle to draw with
     * @param radius   radius at the top
     * @param height   total height
     * @param layers   number of layers
     * @return the animation, not yet started
     */
    public static Animation tornado(Location center, ParticleBuilder particle,
                                    double radius, double height, int layers) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                for (int layer = 0; layer < layers; layer++) {
                    double t = layer / (double) layers;
                    double y = height * t;
                    double r = radius * (0.2 + t);
                    double angle = tick * 0.25 * (1 + t) + layer;
                    for (int i = 0; i < 3; i++) {
                        double a = angle + (Math.PI * 2 * i / 3);
                        particle.spawn(center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r));
                    }
                }
            }
        };
    }

    /**
     * Block fragments bursting outward from a point.
     *
     * @param center   centre of the explosion
     * @param material block to shatter
     * @param radius   final radius
     * @param ticks    duration in ticks
     * @param points   number of fragments
     * @return the animation, not yet started
     */
    public static Animation blockExplosion(Location center, Material material,
                                           double radius, int ticks, int points) {
        ParticleBuilder particle = new ParticleBuilder(XParticle.BLOCK).material(material);
        return new ShapeAnimation(center, Shapes.cloud(1, points), particle)
                .scaleOverTime(0.2, radius, Easing.EASE_OUT_CUBIC)
                .duration(ticks);
    }

    /**
     * A continuous beam between two entities, tracking both as they move.
     *
     * @param from     source entity
     * @param to       target entity
     * @param particle particle to draw with
     * @param spacing  distance between points
     * @param yOffset  height above each entity
     * @return the animation, not yet started
     */
    public static Animation laser(Entity from, Entity to, ParticleBuilder particle,
                                  double spacing, double yOffset) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!from.isValid() || !to.isValid()) {
                    stop();
                    return;
                }
                Location a = from.getLocation().add(0, yOffset, 0);
                Location b = to.getLocation().add(0, yOffset, 0);
                particle.spawn(a, Shapes.line(new Vector(0, 0, 0),
                        b.toVector().subtract(a.toVector()), spacing));
            }
        };
    }

    /**
     * Floating text made of particles, slowly rotating.
     *
     * @param center    centre of the text
     * @param text      text to render
     * @param particle  particle to draw with
     * @param pixelSize size of one pixel in blocks
     * @return the animation, not yet started
     */
    public static ShapeAnimation text(Location center, String text, ParticleBuilder particle,
                                      double pixelSize) {
        return new ShapeAnimation(center, Glyphs.text(text, pixelSize), particle)
                .spinDegrees(2);
    }

    /**
     * A floating ASCII sprite. Pass {@code Glyphs.HEART}, {@code Glyphs.SKULL},
     * {@code Glyphs.SWORD} or your own art.
     *
     * @param center    centre of the sprite
     * @param rows      rows of ASCII art
     * @param particle  particle to draw with
     * @param pixelSize size of one pixel in blocks
     * @return the animation, not yet started
     */
    public static ShapeAnimation sprite(Location center, String[] rows, ParticleBuilder particle,
                                        double pixelSize) {
        return new ShapeAnimation(center, Glyphs.sprite(rows, pixelSize), particle)
                .spinDegrees(3);
    }

    /**
     * Alternating footprints, drawn only when the player genuinely moves.
     *
     * @param player   player to track
     * @param particle particle to draw with
     * @return the animation, not yet started
     */
    public static Animation footsteps(Player player, ParticleBuilder particle) {
        return new Animation() {
            private Location last;
            private boolean rightFoot;

            @Override
            protected void update(long tick) {
                if (!player.isValid()) {
                    stop();
                    return;
                }
                Location current = player.getLocation();
                if (last != null) {
                    double dx = current.getX() - last.getX();
                    double dz = current.getZ() - last.getZ();
                    // Standing still: nothing to print.
                    if (dx * dx + dz * dz < 0.15) return;
                }
                double yaw = Math.toRadians(-current.getYaw());
                double side = rightFoot ? 0.22 : -0.22;
                rightFoot = !rightFoot;
                particle.spawn(current.clone().add(
                        Math.cos(yaw) * side, 0.05, Math.sin(yaw) * side));
                last = current;
            }
        }.interval(3);
    }

    /**
     * A ring pulsing between two radii, for marking zones and boundaries.
     *
     * @param center    centre of the ring
     * @param particle  particle to draw with
     * @param minRadius smallest radius
     * @param maxRadius largest radius
     * @param period    ticks per full pulse
     * @param points    points in the ring
     * @return the animation, not yet started
     */
    public static Animation pulse(Location center, ParticleBuilder particle,
                                  double minRadius, double maxRadius, int period, int points) {
        Shape unit = Shapes.circle(1, points);
        return new Animation() {
            @Override
            protected void update(long tick) {
                double t = (Math.sin(tick * Math.PI * 2 / period) + 1) / 2;
                particle.spawn(center, unit.scale(minRadius + (maxRadius - minRadius) * t));
            }
        };
    }

    /**
     * A trail cycling through a colour palette.
     *
     * @param entity     entity to follow
     * @param particle   particle to draw with
     * @param palette    palette to cycle, such as {@code Colors.FIRE}
     * @param cycleTicks ticks per full cycle
     * @param yOffset    height above the entity
     * @return the animation, not yet started
     */
    public static Animation gradientTrail(Entity entity, ParticleBuilder particle,
                                          List<Color> palette, int cycleTicks, double yOffset) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!entity.isValid()) {
                    stop();
                    return;
                }
                particle.color(Colors.gradient(palette, (tick % cycleTicks) / (double) cycleTicks));
                particle.spawn(entity.getLocation().add(0, yOffset, 0));
            }
        };
    }

    /**
     * A sphere collapsing inward, for teleports and absorption effects.
     *
     * @param center   centre of the implosion
     * @param particle particle to draw with
     * @param radius   starting radius
     * @param ticks    duration in ticks
     * @param points   points in the sphere
     * @return the animation, not yet started
     */
    public static ShapeAnimation implode(Location center, ParticleBuilder particle,
                                         double radius, int ticks, int points) {
        return new ShapeAnimation(center, Shapes.sphere(1, points), particle)
                .scaleOverTime(radius, 0.1, Easing.EASE_IN_QUAD)
                .duration(ticks);
    }

    /**
     * An arcing trajectory drawn progressively as it travels.
     *
     * @param from     start location
     * @param to       end location
     * @param particle particle to draw with
     * @param height   extra height at the apex
     * @param ticks    travel time in ticks
     * @return the animation, not yet started
     */
    public static Animation lob(Location from, Location to, ParticleBuilder particle,
                                double height, int ticks) {
        Shape path = Curves.arcBetween(from, to, height, ticks);
        List<Vector> points = path.points();
        return new Animation() {
            @Override
            protected void update(long tick) {
                int index = (int) Math.min(tick, points.size() - 1);
                particle.spawn(from.clone().add(points.get(index)));
                if (index >= points.size() - 1) stop();
            }
        };
    }
}
