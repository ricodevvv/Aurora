package com.ricodevvv.aurora.cosmetic.pet;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The definition of a pet: how it looks, how it moves and what it leaves behind.
 */
public class PetType implements CosmeticType {

    /**
     * How a pet is rendered.
     */
    public enum RenderMode {
        /**
         * A real entity such as a wolf or a chicken. {@code baby()} shrinks it
         * on any version; {@code scale()} only takes effect from 1.20.5.
         */
        MOB,
        /**
         * A small armour stand wearing a head. This is the only rendering that
         * looks equally small on 1.8 and on 1.21, and it accepts any custom
         * head texture.
         */
        HEAD
    }

    private final String id;
    private final String displayName;
    private final ItemStack icon;

    private RenderMode renderMode = RenderMode.MOB;
    private EntityType entityType = EntityType.ARMOR_STAND;
    private ItemStack headItem;
    private boolean baby = true;
    private double scale = 1.0;
    private String nameTag = "&b<player>&7's &f<pet>";
    private double followDistance = 2.2;
    private double speed = 0.28;
    private double hover = 0;
    private ParticleBuilder trailParticle;
    private int trailInterval = 4;
    private String permission;

    public PetType(String id, String displayName, ItemStack icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
    }

    // ------------------------------------------------------------- fluent

    /**
     * Renders the pet as a real entity.
     *
     * @param entityType entity to spawn
     * @return this type
     */
    public PetType mob(EntityType entityType) {
        this.renderMode = RenderMode.MOB;
        this.entityType = entityType;
        return this;
    }

    /**
     * Renders the pet as a floating head, which is the only true "mini" pet
     * available below 1.20.5.
     *
     * @param headItem head item to wear
     * @return this type
     */
    public PetType head(ItemStack headItem) {
        this.renderMode = RenderMode.HEAD;
        this.headItem = headItem;
        return this;
    }

    public PetType baby(boolean baby) {
        this.baby = baby;
        return this;
    }

    /**
     * Sets the pet's scale. Only takes effect from 1.20.5; ignored below that,
     * where {@link #baby(boolean)} or {@link RenderMode#HEAD} are the options.
     *
     * @param scale scale factor
     * @return this type
     */
    public PetType scale(double scale) {
        this.scale = scale;
        return this;
    }

    /**
     * Sets the floating name. Supports the placeholders {@code <player>} and
     * {@code <pet>}, plus {@code &} colour codes.
     *
     * @param nameTag name template; empty hides the tag
     * @return this type
     */
    public PetType nameTag(String nameTag) {
        this.nameTag = nameTag;
        return this;
    }

    public PetType followDistance(double followDistance) {
        this.followDistance = followDistance;
        return this;
    }

    public PetType speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Sets how far the pet bobs vertically.
     *
     * @param hover amplitude in blocks; {@code 0} keeps it on the ground
     * @return this type
     */
    public PetType hover(double hover) {
        this.hover = hover;
        return this;
    }

    public PetType trail(ParticleBuilder particle, int everyTicks) {
        this.trailParticle = particle;
        this.trailInterval = Math.max(1, everyTicks);
        return this;
    }

    public PetType permission(String permission) {
        this.permission = permission;
        return this;
    }

    // ------------------------------------------------------------ getters

    @Override
    public String id() {
        return id;
    }

    @Override
    public CosmeticCategory category() {
        return CosmeticCategory.PET;
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
        return new Pet(player, this);
    }

    public RenderMode renderMode() {
        return renderMode;
    }

    public EntityType entityType() {
        return entityType;
    }

    public ItemStack headItem() {
        return headItem;
    }

    public boolean isBaby() {
        return baby;
    }

    public double scale() {
        return scale;
    }

    public String nameTag() {
        return nameTag;
    }

    public double followDistance() {
        return followDistance;
    }

    public double speed() {
        return speed;
    }

    public double hover() {
        return hover;
    }

    public ParticleBuilder trailParticle() {
        return trailParticle;
    }

    public int trailInterval() {
        return trailInterval;
    }
}
