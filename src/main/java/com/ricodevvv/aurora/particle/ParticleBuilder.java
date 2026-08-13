package com.ricodevvv.aurora.particle;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import com.ricodevvv.aurora.shape.Shape;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
 * <p>What Aurora adds on top is the part XSeries does not cover: {@link Shape}
 * rendering, viewer filtering by range, and a mutable builder that can be
 * reused every tick.
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

    private final ParticleDisplay display;

    private double range = DEFAULT_RANGE;
    private Collection<? extends Player> viewers;

    /**
     * Colour and size are mirrored here because XSeries applies them together:
     * calling {@code withColor(colour, size)} replaces both. Keeping the last
     * value of each lets {@link #color(Color)} and {@link #size(float)} be
     * chained in any order without one silently discarding the other.
     */
    private Color color = Color.WHITE;
    private float size = 1f;

    /**
     * Creates a builder for the given particle.
     *
     * @param particle particle to spawn
     */
    public ParticleBuilder(XParticle particle) {
        this.display = ParticleDisplay.of(particle);
    }

    private ParticleBuilder(ParticleDisplay display, double range,
                            Collection<? extends Player> viewers, Color color, float size) {
        this.display = display;
        this.range = range;
        this.viewers = viewers;
        this.color = color;
        this.size = size;
    }

    // ------------------------------------------------------------- appearance

    /**
     * Changes which particle is spawned, keeping every other setting.
     *
     * @param particle new particle
     * @return this builder
     */
    public ParticleBuilder type(XParticle particle) {
        display.withParticle(particle);
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
        display.withColor(color, size);
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
        this.size = size;
        display.withColor(color, size);
        return this;
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
        display.withColor(color, size);
        return this;
    }

    /**
     * Sets the block or item the particle is textured with. Applies to
     * {@link XParticle#BLOCK}, {@link XParticle#FALLING_DUST} and
     * {@link XParticle#ITEM}.
     *
     * @param material block or item material
     * @return this builder
     */
    public ParticleBuilder material(Material material) {
        display.withBlock(material.createBlockData());
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
     * @param viewers players who should see it
     * @return this builder
     */
    public ParticleBuilder viewers(Collection<? extends Player> viewers) {
        this.viewers = viewers;
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
        return viewers(single);
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
        if (location == null || location.getWorld() == null) return;
        apply(location).spawn();
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
        if (origin == null || origin.getWorld() == null) return;
        ParticleDisplay resolved = apply(origin);
        for (Vector point : shape.points()) {
            resolved.spawn(point);
        }
    }

    /**
     * Resolves the audience and binds the display to a location.
     *
     * @param location origin
     * @return the underlying display, ready to spawn
     */
    private ParticleDisplay apply(Location location) {
        display.withLocation(location);
        display.onlyVisibleTo(viewers != null ? new ArrayList<>(viewers) : nearby(location));
        return display;
    }

    /**
     * Collects players within {@link #range} of the location, in the same world.
     *
     * @param location centre of the search
     * @return players who should receive the particle
     */
    private Collection<Player> nearby(Location location) {
        List<Player> found = new ArrayList<>();
        double rangeSq = range * range;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= rangeSq) found.add(player);
        }
        return found;
    }

    /**
     * @return the underlying XSeries display, for settings Aurora does not wrap
     */
    public ParticleDisplay display() {
        return display;
    }

    /**
     * Creates an independent copy, so a preset can be varied without disturbing
     * the original.
     *
     * @return a detached copy of this builder
     */
    @Override
    public ParticleBuilder clone() {
        return new ParticleBuilder(display.clone(), range, viewers, color, size);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
