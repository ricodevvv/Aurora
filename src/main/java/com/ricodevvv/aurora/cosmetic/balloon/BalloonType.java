package com.ricodevvv.aurora.cosmetic.balloon;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The definition of a balloon: what it looks like, how it is tethered and what
 * it trails behind.
 */
public class BalloonType implements CosmeticType {

    /**
     * How the tether between player and balloon is drawn.
     */
    public enum LeashMode {
        /**
         * A real Minecraft lead. Requires an invisible anchor mob, because on
         * 1.8 only {@code Creature} entities can be leashed.
         */
        ENTITY,
        /**
         * The tether drawn with particles. Identical on every version and adds
         * no extra entities to the world.
         */
        PARTICLE,
        /** No tether at all; the balloon simply floats alongside. */
        NONE
    }

    private final String id;
    private final String displayName;
    private final ItemStack model;

    private LeashMode leashMode = LeashMode.ENTITY;
    private double leashLength = 2.5;
    private double height = 2.2;
    private boolean small = false;
    private ParticleBuilder ambientParticle;
    private int ambientInterval = 6;
    private Color popColor = Color.WHITE;
    private String permission;

    public BalloonType(String id, String displayName, ItemStack model) {
        this.id = id;
        this.displayName = displayName;
        this.model = model;
    }

    // -------------------------------------------------------------- fluent

    /**
     * Sets the maximum tether length.
     *
     * @param leashLength length in blocks
     * @return this type
     */
    public BalloonType leashLength(double leashLength) {
        this.leashLength = leashLength;
        return this;
    }

    public BalloonType leashMode(LeashMode leashMode) {
        this.leashMode = leashMode;
        return this;
    }

    /**
     * Sets the resting height above the anchor point.
     *
     * @param height height in blocks
     * @return this type
     */
    public BalloonType height(double height) {
        this.height = height;
        return this;
    }

    /**
     * Uses a small armour stand, halving the apparent model size. Works from
     * 1.8 onwards, unlike entity scaling.
     *
     * @param small whether to render small
     * @return this type
     */
    public BalloonType small(boolean small) {
        this.small = small;
        return this;
    }

    /**
     * Sets a particle trailed while the balloon floats.
     *
     * @param particle   particle to spawn
     * @param everyTicks ticks between spawns
     * @return this type
     */
    public BalloonType ambient(ParticleBuilder particle, int everyTicks) {
        this.ambientParticle = particle;
        this.ambientInterval = Math.max(1, everyTicks);
        return this;
    }

    /**
     * Sets the colour of the pop burst shown when the balloon is removed.
     *
     * @param popColor burst colour
     * @return this type
     */
    public BalloonType popColor(Color popColor) {
        this.popColor = popColor;
        return this;
    }

    public BalloonType permission(String permission) {
        this.permission = permission;
        return this;
    }

    // ------------------------------------------------------------- getters

    @Override
    public String id() {
        return id;
    }

    @Override
    public CosmeticCategory category() {
        return CosmeticCategory.BALLOON;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public ItemStack icon() {
        return model;
    }

    @Override
    public String permission() {
        return permission != null ? permission : CosmeticType.super.permission();
    }

    @Override
    public Cosmetic create(Player player) {
        return new Balloon(player, this);
    }

    public ItemStack model() {
        return model;
    }

    public LeashMode leashMode() {
        return leashMode;
    }

    public double leashLength() {
        return leashLength;
    }

    public double height() {
        return height;
    }

    public boolean small() {
        return small;
    }

    public ParticleBuilder ambientParticle() {
        return ambientParticle;
    }

    public int ambientInterval() {
        return ambientInterval;
    }

    public Color popColor() {
        return popColor;
    }
}
