package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.util.ColorRamp;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fluent builder for spawning particles.
 *
 * <p>Everything version-specific lives below it: {@link ParticleType} resolves
 * what the running server calls each particle, {@link ParticleData} shapes the
 * colour or block payload the way that version wants it, and
 * {@link ParticleSender} delivers it — through {@code Player#spawnParticle} on
 * 1.9 and later, and through a hand-built packet on 1.8, which has no particle
 * API at all.
 *
 * <p>On top of that this class adds what the platform does not:
 * <ul>
 *   <li>{@link Shape} rendering, including {@linkplain ColorRamp colour ramps}
 *       sampled along the shape rather than one flat tone;</li>
 *   <li>audience resolution through the shared per-tick {@link ViewerCache},
 *       so a frame that draws twelve shapes resolves its viewers once;</li>
 *   <li>level of detail: point counts follow {@link RenderSettings}, so the
 *       same effect costs less on a full lobby without being rewritten;</li>
 *   <li>dust colour transitions, with an automatic fallback below 1.17.</li>
 * </ul>
 *
 * <p><b>This class is deliberately mutable.</b> Inside an animation you should
 * keep one builder as a field and only change what varies per tick. Allocating
 * a new builder every tick, with dozens of players wearing cosmetics, is free
 * garbage for the collector.
 *
 * <pre>{@code
 * Particles.dust(Color.AQUA)
 *          .size(1.2f)
 *          .range(24)
 *          .spawn(location);
 * }</pre>
 *
 * @see Particles
 * @see Shape
 */
public class ParticleBuilder implements Cloneable {

    /** Default radius, in blocks, within which players receive the particle. */
    private static final double DEFAULT_RANGE = 32;

    private ParticleType type;
    private final ParticleData data;

    private int count = 1;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private double extra;

    private double range = DEFAULT_RANGE;
    private List<Player> viewers;

    private double density = 1;
    private boolean levelOfDetail = true;

    /**
     * Creates a builder for the given particle.
     *
     * @param particle particle to spawn
     */
    public ParticleBuilder(ParticleType particle) {
        this.type = particle;
        this.data = new ParticleData();
    }

    private ParticleBuilder(ParticleBuilder source) {
        this.type = source.type;
        this.data = source.data.copy();
        this.count = source.count;
        this.offsetX = source.offsetX;
        this.offsetY = source.offsetY;
        this.offsetZ = source.offsetZ;
        this.extra = source.extra;
        this.range = source.range;
        this.viewers = source.viewers == null ? null : new ArrayList<>(source.viewers);
        this.density = source.density;
        this.levelOfDetail = source.levelOfDetail;
    }

    // ------------------------------------------------------------- appearance

    /**
     * Changes which particle is spawned, keeping every other setting.
     *
     * @param particle new particle
     * @return this builder
     */
    public ParticleBuilder type(ParticleType particle) {
        this.type = particle;
        return this;
    }

    /**
     * Sets how many particles are spawned per point.
     *
     * <p>A count of {@code 0} is not "none": it makes the offset act as a
     * velocity vector, which is how vanilla shoots firework sparks and end rod
     * streaks, and below 1.13 it is also how a colour is encoded. Several
     * effects rely on this, so it is passed through untouched.
     *
     * @param count particle count, may be {@code 0}
     * @return this builder
     */
    public ParticleBuilder count(int count) {
        this.count = Math.max(0, count);
        return this;
    }

    /**
     * Sets the random spread applied to each particle.
     *
     * @param x spread along X
     * @param y spread along Y
     * @param z spread along Z
     * @return this builder
     */
    public ParticleBuilder offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    /**
     * Sets the same spread on all three axes.
     *
     * @param all spread applied to X, Y and Z
     * @return this builder
     */
    public ParticleBuilder offset(double all) {
        return offset(all, all, all);
    }

    /**
     * Sets the particle's extra value, usually its speed.
     *
     * @param speed extra value; meaning depends on the particle
     * @return this builder
     */
    public ParticleBuilder speed(double speed) {
        this.extra = speed;
        return this;
    }

    /**
     * Colours the particle. Only meaningful for colourable particles:
     * {@link ParticleType#DUST}, {@link ParticleType#DUST_COLOR_TRANSITION} and
     * {@link ParticleType#NOTE}, which picks the nearest of its twenty-five
     * hues.
     *
     * @param color colour to apply
     * @return this builder
     */
    public ParticleBuilder color(Color color) {
        data.color(color);
        return this;
    }

    /**
     * Colours the particle from RGB components, clamped to {@code 0-255}.
     *
     * @param r red
     * @param g green
     * @param b blue
     * @return this builder
     */
    public ParticleBuilder color(int r, int g, int b) {
        return color(Color.fromRGB(clamp(r), clamp(g), clamp(b)));
    }

    /**
     * Colours the particle from a hue, which is handy for rainbow cycling.
     *
     * @param hue hue in {@code 0..1}; values outside the range wrap around
     * @return this builder
     */
    public ParticleBuilder hue(float hue) {
        java.awt.Color awt = java.awt.Color.getHSBColor(hue, 1f, 1f);
        return color(awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    /**
     * Sets the dust size. Ignored on versions without dust options (below 1.13).
     *
     * @param size dust size, {@code 1.0} being the default
     * @return this builder
     */
    public ParticleBuilder size(float size) {
        data.size(size);
        return this;
    }

    /**
     * Colours and sizes the particle in one call.
     *
     * @param color colour to apply
     * @param size  dust size
     * @return this builder
     */
    public ParticleBuilder color(Color color, float size) {
        data.color(color);
        data.size(size);
        return this;
    }

    /**
     * Makes each particle fade from its colour to a second one over its
     * lifetime.
     *
     * <p>This is the single cheapest way to make an effect look expensive: one
     * particle carries a gradient instead of a flat dot, so a trail cools from
     * white to ember without spawning a second layer. It needs
     * {@code DUST_COLOR_TRANSITION}, which arrived in 1.17; below that the call
     * degrades to plain dust in the base colour.
     *
     * @param to colour each particle fades towards, or {@code null} to go back
     *           to plain dust
     * @return this builder
     */
    public ParticleBuilder fadeTo(Color to) {
        data.fade(to);
        if (to == null) {
            if (type == ParticleType.DUST_COLOR_TRANSITION) type = ParticleType.DUST;
        } else if (type == ParticleType.DUST) {
            type = ParticleType.DUST_COLOR_TRANSITION;
        }
        return this;
    }

    /**
     * Sets the block the particle is textured with. Applies to
     * {@link ParticleType#BLOCK} and {@link ParticleType#FALLING_DUST}.
     *
     * @param material block material
     * @return this builder
     */
    public ParticleBuilder material(Material material) {
        data.material(material);
        return this;
    }

    /**
     * Sets the item the particle is textured with, for {@link ParticleType#ITEM}.
     *
     * @param item item to shatter
     * @return this builder
     */
    public ParticleBuilder item(ItemStack item) {
        data.item(item);
        return this;
    }

    /**
     * Fires the particle along a direction instead of spreading it randomly.
     *
     * <p>Directional particles carry a count of zero and read their offset as a
     * velocity, which is how vanilla shoots firework sparks. Aurora uses it for
     * effects that need to stream behind a moving player rather than hang in
     * the air.
     *
     * @param direction direction and speed
     * @return this builder
     */
    public ParticleBuilder direction(Vector direction) {
        this.count = 0;
        this.offsetX = direction.getX();
        this.offsetY = direction.getY();
        this.offsetZ = direction.getZ();
        if (extra == 0) extra = 1;
        return this;
    }

    // ------------------------------------------------------------------- cost

    /**
     * Scales how many of a shape's points are actually drawn.
     *
     * <p>Points are dropped evenly across the shape rather than truncated, so a
     * circle at {@code 0.5} is still a circle, drawn with half the dots.
     *
     * @param density fraction of points to draw, {@code 1} being all of them
     * @return this builder
     */
    public ParticleBuilder density(double density) {
        this.density = Math.max(0.05, Math.min(1, density));
        return this;
    }

    /**
     * Opts this builder out of the global {@link RenderSettings} quality
     * scaling, for the handful of particles an effect cannot afford to lose:
     * the core of a beam, a single impact flash, a shape made of four points.
     *
     * @param enabled {@code false} to always draw at full density
     * @return this builder
     */
    public ParticleBuilder levelOfDetail(boolean enabled) {
        this.levelOfDetail = enabled;
        return this;
    }

    // ---------------------------------------------------------------- viewers

    /**
     * Sets the radius within which players receive the particle. Defaults to
     * {@value #DEFAULT_RANGE} blocks.
     *
     * @param range radius in blocks
     * @return this builder
     */
    public ParticleBuilder range(double range) {
        this.range = range;
        return this;
    }

    /**
     * Restricts the particle to an explicit set of players, bypassing the range
     * check. Useful for private cosmetics and per-team effects.
     *
     * @param viewers players who should see it, or {@code null} to go back to
     *                range-based selection
     * @return this builder
     */
    public ParticleBuilder viewers(Collection<? extends Player> viewers) {
        this.viewers = viewers == null ? null : new ArrayList<>(viewers);
        return this;
    }

    /**
     * Restricts the particle to an audience the caller already resolved,
     * keeping the list by reference instead of copying it.
     *
     * <p>This is the hot path the effect pipeline uses: one audience is
     * resolved per effect frame and handed to every builder that frame. The
     * list must not be modified afterwards, which is why the copying
     * {@link #viewers(Collection)} stays the public default.
     *
     * @param audience players who should see it, or {@code null} for
     *                 range-based selection
     * @return this builder
     */
    public ParticleBuilder audience(List<Player> audience) {
        this.viewers = audience;
        return this;
    }

    /**
     * Restricts the particle to a single player.
     *
     * @param player the only player who should see it
     * @return this builder
     */
    public ParticleBuilder viewer(Player player) {
        List<Player> single = new ArrayList<>(1);
        single.add(player);
        this.viewers = single;
        return this;
    }

    /**
     * Clears any explicit viewer list, returning to range-based selection.
     *
     * @return this builder
     */
    public ParticleBuilder allNearby() {
        this.viewers = null;
        return this;
    }

    // ----------------------------------------------------------------- spawn

    /**
     * Spawns a single particle at the given location.
     *
     * @param location where to spawn it
     */
    public void spawn(Location location) {
        if (location == null) return;
        spawn(location, 0, 0, 0);
    }

    /**
     * Spawns a single particle at an offset from a location, without
     * allocating a {@link Location} for it.
     *
     * @param origin the origin
     * @param x      offset along X
     * @param y      offset along Y
     * @param z      offset along Z
     */
    public void spawn(Location origin, double x, double y, double z) {
        if (!RenderSettings.enabled()) return;
        if (origin == null || origin.getWorld() == null) return;

        List<Player> audience = resolve(origin);
        if (audience.isEmpty()) return;

        ParticleSender.get().spawn(audience, type,
                origin.getX() + x, origin.getY() + y, origin.getZ() + z,
                count, offsetX, offsetY, offsetZ, extra, data);
    }

    /**
     * Renders a whole shape, treating the location as its origin.
     *
     * <p>Viewers are resolved once for the entire shape rather than per point,
     * which matters: a 200-point sphere would otherwise run the distance check
     * 200 times against every player in the world.
     *
     * @param origin centre of the shape
     * @param shape  shape to render
     */
    public void spawn(Location origin, Shape shape) {
        spawn(origin, shape, null);
    }

    /**
     * Renders a shape, colouring each point by its position along the shape.
     *
     * <p>The ramp is sampled from {@code 0} at the first point to {@code 1} at
     * the last, so the colour follows the order the shape was generated in: a
     * circle sweeps round it, a helix runs along it, a lightning bolt fades
     * from its root to its tip.
     *
     * @param origin centre of the shape
     * @param shape  shape to render
     * @param ramp   colour ramp, or {@code null} to use the builder's colour
     */
    public void spawn(Location origin, Shape shape, ColorRamp ramp) {
        if (!RenderSettings.enabled()) return;
        if (origin == null || origin.getWorld() == null) return;

        List<Vector> points = shape.points();
        int total = points.size();
        if (total == 0) return;

        List<Player> audience = resolve(origin);
        if (audience.isEmpty()) return;

        ParticleSender sender = ParticleSender.get();
        double step = levelOfDetail ? density * RenderSettings.density() : density;
        double accumulated = 0;
        double last = Math.max(1, total - 1);
        boolean drewAny = false;

        for (int i = 0; i < total; i++) {
            accumulated += step;
            if (accumulated < 1) continue;
            accumulated -= 1;

            if (ramp != null) data.color(ramp.at(i / last));

            Vector point = points.get(i);
            sender.spawn(audience, type,
                    origin.getX() + point.getX(),
                    origin.getY() + point.getY(),
                    origin.getZ() + point.getZ(),
                    count, offsetX, offsetY, offsetZ, extra, data);
            drewAny = true;
        }

        // A shape sparse enough to round to nothing still deserves one point,
        // otherwise a four-point shape disappears entirely at low quality.
        if (!drewAny) {
            Vector point = points.get(0);
            if (ramp != null) data.color(ramp.at(0));
            sender.spawn(audience, type,
                    origin.getX() + point.getX(),
                    origin.getY() + point.getY(),
                    origin.getZ() + point.getZ(),
                    count, offsetX, offsetY, offsetZ, extra, data);
        }
    }

    /**
     * Draws a straight line of particles between two points.
     *
     * @param from    start of the line
     * @param to      end of the line
     * @param spacing distance between particles, in blocks
     */
    public void line(Location from, Location to, double spacing) {
        if (from == null || to == null || from.getWorld() != to.getWorld()) return;

        double length = from.distance(to);
        if (length <= 0) return;

        int steps = Math.max(1, (int) (length / Math.max(0.01, spacing)));
        double dx = (to.getX() - from.getX()) / steps;
        double dy = (to.getY() - from.getY()) / steps;
        double dz = (to.getZ() - from.getZ()) / steps;

        for (int i = 0; i <= steps; i++) {
            spawn(from, dx * i, dy * i, dz * i);
        }
    }

    /**
     * Resolves who should receive the next spawn.
     *
     * @param location origin of the particle
     * @return the audience, possibly empty
     */
    private List<Player> resolve(Location location) {
        if (viewers != null) return viewers;
        return ViewerCache.near(location, RenderSettings.range(range));
    }

    /**
     * @return the particle this builder spawns
     */
    public ParticleType type() {
        return type;
    }

    /**
     * @return the colour currently applied
     */
    public Color currentColor() {
        return data.color();
    }

    /**
     * Creates an independent copy, so a preset can be varied without disturbing
     * the original.
     *
     * @return a detached copy of this builder
     */
    @Override
    public ParticleBuilder clone() {
        return new ParticleBuilder(this);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
