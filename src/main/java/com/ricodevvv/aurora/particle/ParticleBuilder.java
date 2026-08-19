package com.ricodevvv.aurora.particle;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
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
 * <p>This is a thin layer over XSeries' {@link ParticleDisplay}. Aurora used to
 * ship two hand-rolled reflection backends (one packet-based for 1.8, one
 * {@code Player#spawnParticle}-based for 1.9+), which meant owning every
 * cross-version quirk by hand: the 1.20.5 enum rename, dust options, block and
 * item data, and the colour-in-offsets trick. XSeries already solves all of
 * that and is actively maintained, so Aurora delegates instead of competing.
 *
 * <p>What Aurora adds on top is the part XSeries does not cover:
 * <ul>
 *   <li>{@link Shape} rendering, including {@linkplain ColorRamp colour ramps}
 *       sampled along the shape rather than one flat tone;</li>
 *   <li>audience resolution through the shared per-tick {@link ViewerCache},
 *       so a frame that draws twelve shapes resolves its viewers once;</li>
 *   <li>level of detail: point counts follow {@link RenderSettings}, so the
 *       same effect costs less on a full lobby without being rewritten;</li>
 *   <li>dust colour transitions with an automatic fallback on versions that
 *       predate them.</li>
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

    /**
     * Whether {@code DUST_COLOR_TRANSITION} (1.17+) can be used. Resolved once,
     * because every dust builder asks.
     */
    private static final boolean TRANSITION_SUPPORTED =
            ParticleCompat.supported(XParticle.DUST_COLOR_TRANSITION);

    private final ParticleDisplay display;

    private double range = DEFAULT_RANGE;
    private List<Player> viewers;

    /**
     * Colour and size are mirrored here because XSeries applies them together:
     * calling {@code withColor(colour, size)} replaces both. Keeping the last
     * value of each lets {@link #color(Color)} and {@link #size(float)} be
     * chained in any order without one silently discarding the other.
     */
    private Color color = Color.WHITE;
    private Color fade;
    private float size = 1f;

    private double density = 1;
    private boolean levelOfDetail = true;

    /**
     * Creates a builder for the given particle.
     *
     * @param particle particle to spawn
     */
    public ParticleBuilder(XParticle particle) {
        this.display = ParticleCompat.display(particle);
    }

    private ParticleBuilder(ParticleDisplay display, double range, List<Player> viewers,
                            Color color, Color fade, float size,
                            double density, boolean levelOfDetail) {
        this.display = display;
        this.range = range;
        this.viewers = viewers;
        this.color = color;
        this.fade = fade;
        this.size = size;
        this.density = density;
        this.levelOfDetail = levelOfDetail;
    }

    // ------------------------------------------------------------- appearance

    /**
     * Changes which particle is spawned, keeping every other setting.
     *
     * @param particle new particle
     * @return this builder
     */
    public ParticleBuilder type(XParticle particle) {
        ParticleCompat.particle(display, particle);
        return this;
    }

    /**
     * Sets how many particles are spawned per point.
     *
     * <p>A count of {@code 0} is not "none": for directional particles it makes
     * the offset act as a velocity vector, and for colourable particles on
     * legacy versions it is how the colour is encoded. Several effects rely on
     * this, so it is passed through untouched.
     *
     * @param count particle count, may be {@code 0}
     * @return this builder
     */
    public ParticleBuilder count(int count) {
        display.withCount(count);
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
        display.offset(x, y, z);
        return this;
    }

    /**
     * Sets the same spread on all three axes.
     *
     * @param all spread applied to X, Y and Z
     * @return this builder
     */
    public ParticleBuilder offset(double all) {
        display.offset(all);
        return this;
    }

    /**
     * Sets the particle's extra value, usually its speed.
     *
     * @param speed extra value; meaning depends on the particle
     * @return this builder
     */
    public ParticleBuilder speed(double speed) {
        display.withExtra(speed);
        return this;
    }

    /**
     * Colours the particle. Only meaningful for colourable particles such as
     * {@link XParticle#DUST}, {@link XParticle#ENTITY_EFFECT} and
     * {@link XParticle#NOTE}.
     *
     * @param color colour to apply
     * @return this builder
     */
    public ParticleBuilder color(Color color) {
        this.color = color;
        return applyColor();
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
        this.size = size;
        return applyColor();
    }

    /**
     * Colours and sizes the particle in one call, which is the only way to set
     * a dust size without discarding the current colour.
     *
     * @param color colour to apply
     * @param size  dust size
     * @return this builder
     */
    public ParticleBuilder color(Color color, float size) {
        this.color = color;
        this.size = size;
        return applyColor();
    }

    /**
     * Makes each particle fade from its colour to a second one over its
     * lifetime, using {@code DUST_COLOR_TRANSITION}.
     *
     * <p>This is the single cheapest way to make an effect look expensive: one
     * particle carries a gradient instead of a flat dot, so a trail cools from
     * white to ember without spawning a second layer. The particle type is
     * switched to {@code DUST_COLOR_TRANSITION} when it is available, and on
     * anything below 1.17 the call degrades to plain dust in the base colour.
     *
     * @param to colour each particle fades towards, or {@code null} to go back
     *           to plain dust
     * @return this builder
     */
    public ParticleBuilder fadeTo(Color to) {
        this.fade = to;
        if (to != null && TRANSITION_SUPPORTED) {
            ParticleCompat.particle(display, XParticle.DUST_COLOR_TRANSITION);
        } else if (to == null) {
            ParticleCompat.particle(display, XParticle.DUST);
        }
        return applyColor();
    }

    /**
     * Applies the current colour, size and fade to the underlying display.
     *
     * @return this builder
     */
    private ParticleBuilder applyColor() {
        java.awt.Color base = awt(color);
        if (fade != null && TRANSITION_SUPPORTED) display.withTransitionColor(base, size, awt(fade));
        else display.withColor(base, size);
        return this;
    }

    /**
     * Sets the block the particle is textured with. Applies to
     * {@link XParticle#BLOCK}, {@link XParticle#FALLING_DUST} and
     * {@link XParticle#BLOCK_MARKER}.
     *
     * @param material block material
     * @return this builder
     */
    public ParticleBuilder material(Material material) {
        ParticleCompat.block(display, material);
        return this;
    }

    /**
     * Sets the item the particle is textured with, for {@link XParticle#ITEM}.
     *
     * @param item item to shatter
     * @return this builder
     */
    public ParticleBuilder item(ItemStack item) {
        display.withItem(item);
        return this;
    }

    /**
     * Fires the particle along a direction instead of spreading it randomly.
     *
     * <p>Directional particles ignore their count and use the offset as a
     * velocity, which is how vanilla shoots {@code FIREWORK} sparks and
     * {@code END_ROD} streaks. Aurora uses it for effects that need to trail
     * behind a moving player rather than hang in the air.
     *
     * @param direction direction and speed
     * @return this builder
     */
    public ParticleBuilder direction(Vector direction) {
        display.particleDirection(direction);
        display.directional();
        return this;
    }

    /**
     * Sends the particle even to players who turned particles down in their
     * video settings. Reserve it for effects that carry gameplay meaning.
     *
     * @param force {@code true} to bypass the client's particle setting
     * @return this builder
     */
    public ParticleBuilder force(boolean force) {
        display.forceSpawn(force);
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
     * check. Useful for private cosmetics, per-team effects, and for handing a
     * whole effect frame one audience resolved once.
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
        if (!RenderSettings.enabled()) return;
        if (location == null || location.getWorld() == null) return;

        List<Player> audience = resolve(location);
        if (audience.isEmpty()) return;

        display.withLocation(location);
        display.onlyVisibleTo(audience);
        display.spawn();
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

        display.withLocation(origin);
        display.onlyVisibleTo(audience);
        display.spawn(x, y, z);
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

        display.withLocation(origin);
        display.onlyVisibleTo(audience);

        double step = levelOfDetail ? density * RenderSettings.density() : density;
        double accumulated = 0;
        double last = Math.max(1, total - 1);

        for (int i = 0; i < total; i++) {
            accumulated += step;
            if (accumulated < 1) continue;
            accumulated -= 1;

            if (ramp != null) {
                color = ramp.at(i / last);
                applyColor();
            }
            display.spawn(points.get(i));
        }

        // A shape sparse enough to round to nothing still deserves one point,
        // otherwise a four-point shape disappears entirely at low quality.
        if (step < 1 && total * step < 1) display.spawn(points.get(0));
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
     * @return the underlying XSeries display, for settings Aurora does not wrap
     */
    public ParticleDisplay display() {
        return display;
    }

    /**
     * @return the colour currently applied
     */
    public Color currentColor() {
        return color;
    }

    /**
     * Creates an independent copy, so a preset can be varied without disturbing
     * the original.
     *
     * @return a detached copy of this builder
     */
    @Override
    public ParticleBuilder clone() {
        return new ParticleBuilder(display.copy(), range,
                viewers == null ? null : new ArrayList<>(viewers),
                color, fade, size, density, levelOfDetail);
    }

    /**
     * XSeries works in {@link java.awt.Color}; Bukkit's own {@link Color} is
     * what every caller has. This is the only place the two meet.
     *
     * @param color a Bukkit colour
     * @return the AWT equivalent
     */
    private static java.awt.Color awt(Color color) {
        if (color == null) return java.awt.Color.WHITE;
        return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
