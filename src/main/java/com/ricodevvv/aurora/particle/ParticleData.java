package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.util.Reflect;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * The extra data a particle carries, in whichever shape the running server
 * wants it.
 *
 * <p>This is where four versions of Minecraft disagree the most:
 *
 * <ul>
 *   <li><b>Colour.</b> From 1.13 dust takes a {@code Particle.DustOptions}
 *       object. Before that the colour was smuggled through the offset fields
 *       with a count of zero — and a red channel of zero had to be sent as a
 *       hair above zero, because the client reads an exact zero as full red.</li>
 *   <li><b>Fading colour.</b> {@code DustTransition} exists only from 1.17;
 *       below that the particle keeps its first colour.</li>
 *   <li><b>Blocks and items.</b> 1.13 takes {@code BlockData} and
 *       {@code ItemStack}; 1.9 to 1.12 take {@code MaterialData}; 1.8 packs the
 *       numeric id, and the data value shifted twelve bits up for blocks, into
 *       the packet's trailing int array.</li>
 * </ul>
 *
 * <p>The object is mutable and owned by one {@link ParticleBuilder}. The
 * server-side representation is built lazily and thrown away whenever anything
 * changes, so a builder that keeps one colour builds one {@code DustOptions}
 * for its whole life, while one being driven by a colour ramp builds a fresh
 * one per point — which is what the API requires either way.
 */
final class ParticleData {

    /** Dust colours are eight bit; this is one step below the smallest of them. */
    private static final float ALMOST_ZERO = 1f / 255f * 0.4f;

    private static final Constructor<?> DUST_OPTIONS;
    private static final Constructor<?> DUST_TRANSITION;
    private static final Method CREATE_BLOCK_DATA;

    static {
        Class<?> dustOptions = Reflect.lookup("org.bukkit.Particle$DustOptions");
        Class<?> dustTransition = Reflect.lookup("org.bukkit.Particle$DustTransition");
        DUST_OPTIONS = Reflect.constructor(dustOptions, Color.class, float.class);
        DUST_TRANSITION = Reflect.constructor(dustTransition, Color.class, Color.class, float.class);
        CREATE_BLOCK_DATA = Reflect.method(Material.class, "createBlockData");
    }

    private Color color = Color.WHITE;
    private Color fade;
    private float size = 1f;
    private Material material;
    private ItemStack item;

    /** Lazily built server-side object; {@code null} means "not built yet". */
    private Object resolved;
    private boolean resolvedIsNull;

    void color(Color color) {
        if (equal(this.color, color)) return;
        this.color = color == null ? Color.WHITE : color;
        invalidate();
    }

    void fade(Color fade) {
        if (equal(this.fade, fade)) return;
        this.fade = fade;
        invalidate();
    }

    void size(float size) {
        if (this.size == size) return;
        this.size = size;
        invalidate();
    }

    void material(Material material) {
        this.material = material;
        this.item = null;
        invalidate();
    }

    void item(ItemStack item) {
        this.item = item;
        this.material = item == null ? null : item.getType();
        invalidate();
    }

    /**
     * @return a detached copy, for {@link ParticleBuilder#clone()}
     */
    ParticleData copy() {
        ParticleData copy = new ParticleData();
        copy.color = color;
        copy.fade = fade;
        copy.size = size;
        copy.material = material;
        copy.item = item;
        return copy;
    }

    Color color() {
        return color;
    }

    float size() {
        return size;
    }

    private void invalidate() {
        resolved = null;
        resolvedIsNull = false;
    }

    private static boolean equal(Color left, Color right) {
        return left == right || (left != null && left.equals(right));
    }

    /**
     * The object to hand {@code Player#spawnParticle} on 1.9 and later.
     *
     * @param payload what the particle expects
     * @return the data object, or {@code null} if the particle takes none
     */
    Object modern(ParticleType.Payload payload) {
        if (resolved != null || resolvedIsNull) return resolved;

        Object built = build(payload);
        resolved = built;
        resolvedIsNull = built == null;
        return built;
    }

    private Object build(ParticleType.Payload payload) {
        try {
            switch (payload) {
                case DUST:
                    return DUST_OPTIONS == null ? null : DUST_OPTIONS.newInstance(color, size);
                case TRANSITION:
                    if (DUST_TRANSITION == null) {
                        return DUST_OPTIONS == null ? null : DUST_OPTIONS.newInstance(color, size);
                    }
                    return DUST_TRANSITION.newInstance(color, fade == null ? color : fade, size);
                case BLOCK:
                    return blockData();
                case ITEM:
                    return item != null ? item : new ItemStack(material == null ? Material.STONE : material);
                default:
                    return null;
            }
        } catch (Throwable ignored) {
            // A server that will not build the data gets a plain particle
            // rather than an exception inside a tick loop.
            return null;
        }
    }

    /**
     * Block data for 1.13 and later, or {@code MaterialData} below it.
     *
     * @return the block payload, or {@code null} if neither can be built
     * @throws Exception if the reflective call itself fails
     */
    @SuppressWarnings("deprecation")
    private Object blockData() throws Exception {
        Material block = material == null ? Material.STONE : material;
        if (CREATE_BLOCK_DATA != null) return CREATE_BLOCK_DATA.invoke(block);
        return new org.bukkit.material.MaterialData(block);
    }

    /**
     * The trailing int array of the 1.8 particle packet.
     *
     * @param payload what the particle expects
     * @return the data array, never {@code null}
     */
    @SuppressWarnings("deprecation")
    int[] legacy(ParticleType.Payload payload) {
        Material block = material == null ? Material.STONE : material;
        switch (payload) {
            case BLOCK:
                // Blocks pack the data value twelve bits up; items keep the two
                // numbers apart. This asymmetry is in the 1.8 protocol itself.
                return new int[]{block.getId() + (legacyData() << 12)};
            case ITEM:
                return new int[]{block.getId(), legacyData()};
            default:
                return new int[0];
        }
    }

    @SuppressWarnings("deprecation")
    private int legacyData() {
        return item == null ? 0 : item.getDurability();
    }

    /**
     * Whether the colour has to travel in the offset fields rather than in a
     * data object, which is the case for dust and note on everything below 1.13.
     *
     * @param payload what the particle expects
     * @return {@code true} if the offsets carry the colour
     */
    boolean colorInOffsets(ParticleType.Payload payload) {
        if (payload == ParticleType.Payload.NOTE) return true;
        if (payload != ParticleType.Payload.DUST && payload != ParticleType.Payload.TRANSITION) return false;
        return DUST_OPTIONS == null;
    }

    /**
     * The offset triplet that encodes the colour on legacy servers.
     *
     * @param payload what the particle expects
     * @return three floats, ready to be sent as the offsets
     */
    float[] colorOffsets(ParticleType.Payload payload) {
        if (payload == ParticleType.Payload.NOTE) {
            // Notes have twenty-five colours; the hue picks between them.
            float[] hsb = java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            return new float[]{Math.min(24, Math.round(hsb[0] * 24)) / 24f, 0, 0};
        }
        return new float[]{
                color.getRed() == 0 ? ALMOST_ZERO : color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f};
    }
}
