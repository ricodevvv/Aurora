package com.ricodevvv.aurora.cosmetic.pet;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Definicion de una mascota. */
public class PetType implements CosmeticType {

    /**
     * Como se dibuja la mascota.
     *
     * MOB  - entidad real (vaca, lobo, etc). Con baby() se ve chica en cualquier
     *        version; con scale() solo se encoge de verdad en 1.20.5+.
     * HEAD - ArmorStand pequeno con una cabeza de casco. Es el unico "mini" que
     *        se ve igual de chiquito en 1.8 que en 1.21, y ademas te deja usar
     *        cualquier textura de cabeza custom.
     */
    public enum RenderMode {
        MOB,
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

    /** Mascota basada en un mob real. */
    public PetType mob(EntityType entityType) {
        this.renderMode = RenderMode.MOB;
        this.entityType = entityType;
        return this;
    }

    /** Mascota basada en una cabeza flotante (el "mini pet" de verdad en 1.8). */
    public PetType head(ItemStack headItem) {
        this.renderMode = RenderMode.HEAD;
        this.headItem = headItem;
        return this;
    }

    public PetType baby(boolean baby) {
        this.baby = baby;
        return this;
    }

    /** Escala real. OJO: solo tiene efecto en 1.20.5+; abajo se ignora. */
    public PetType scale(double scale) {
        this.scale = scale;
        return this;
    }

    /** Acepta &lt;player&gt; y &lt;pet&gt;. Cadena vacia = sin nametag. */
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

    /** Amplitud del flotado vertical. 0 = camina pegada al suelo. */
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
