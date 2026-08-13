package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The definition of a wearable particle effect.
 *
 * <p>An effect can render differently depending on whether its wearer is
 * moving; see {@link #moving(EffectRenderer)}.
 */
public class ParticleEffectType implements CosmeticType {

    private final String id;
    private final String displayName;
    private final ItemStack icon;
    private final EffectRenderer idleRenderer;

    private EffectRenderer movingRenderer;
    private int interval = 1;
    private double range = 32;
    private String permission;

    /**
     * @param id          identifier, unique within the category
     * @param displayName display name, supporting {@code &} colour codes
     * @param icon        icon shown in a selection menu
     * @param idleRenderer how it draws while the wearer is still, and by
     *                     default while moving too until {@link #moving} is set
     */
    public ParticleEffectType(String id, String displayName, ItemStack icon, EffectRenderer idleRenderer) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.idleRenderer = idleRenderer;
    }

    /**
     * Defines a separate variant used while the wearer is walking.
     *
     * <p>This is what stops an effect reading as a sticker: a ring of flames
     * standing still becomes two trails at the wearer's sides when they run,
     * rather than the whole ring being dragged along. Without it the same
     * variant is used in both states.
     *
     * @param movingRenderer variant drawn while moving
     * @return this type
     */
    public ParticleEffectType moving(EffectRenderer movingRenderer) {
        this.movingRenderer = movingRenderer;
        return this;
    }

    /**
     * Sets how often the effect is drawn.
     *
     * <p>This is the most important performance lever here. A sixty-particle
     * effect drawn every tick, worn by fifty players, is sixty thousand
     * particles a second. At an interval of three that drops to twenty
     * thousand and is close to indistinguishable by eye.
     *
     * @param interval ticks between frames; clamped to at least {@code 1}
     * @return this type
     */
    public ParticleEffectType interval(int interval) {
        this.interval = Math.max(1, interval);
        return this;
    }

    /**
     * Sets the radius within which the effect is visible.
     *
     * @param range radius in blocks
     * @return this type
     */
    public ParticleEffectType range(double range) {
        this.range = range;
        return this;
    }

    public ParticleEffectType permission(String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public CosmeticCategory category() {
        return CosmeticCategory.PARTICLE_EFFECT;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public ItemStack icon() {
        return icon;
    }

    @Override
    public String permission() {
        return permission != null ? permission : CosmeticType.super.permission();
    }

    @Override
    public Cosmetic create(Player player) {
        return new ParticleEffect(player, this);
    }

    /**
     * Picks the variant to draw this tick.
     *
     * @param moving whether the wearer is currently moving
     * @return the moving variant if one was defined, otherwise the idle one
     */
    public EffectRenderer renderer(boolean moving) {
        return moving && movingRenderer != null ? movingRenderer : idleRenderer;
    }

    public int interval() {
        return interval;
    }

    public double range() {
        return range;
    }
}
