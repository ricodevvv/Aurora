package com.ricodevvv.aurora.cosmetic.balloon;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Definicion de un globo: como se ve, como cuelga y que particulas suelta. */
public class BalloonType implements CosmeticType {

    /** Como se dibuja la cuerda entre el jugador y el globo. */
    public enum LeashMode {
        /**
         * Correa real de Minecraft. Necesita un mob invisible de ancla
         * (en 1.8 solo los Creature se pueden atar).
         */
        ENTITY,
        /** Cuerda dibujada con particulas. Funciona igual en todas las versiones. */
        PARTICLE,
        /** Sin cuerda: el globo solo flota al lado. */
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

    // ------------------------------------------------------------- fluent

    /** Largo maximo de la cuerda en bloques. */
    public BalloonType leashLength(double leashLength) {
        this.leashLength = leashLength;
        return this;
    }

    public BalloonType leashMode(LeashMode leashMode) {
        this.leashMode = leashMode;
        return this;
    }

    /** Altura de reposo sobre el jugador. */
    public BalloonType height(double height) {
        this.height = height;
        return this;
    }

    /** Armor stand pequeno: el modelo se ve a la mitad. Funciona desde 1.8. */
    public BalloonType small(boolean small) {
        this.small = small;
        return this;
    }

    /** Particula que suelta mientras flota. */
    public BalloonType ambient(ParticleBuilder particle, int everyTicks) {
        this.ambientParticle = particle;
        this.ambientInterval = Math.max(1, everyTicks);
        return this;
    }

    /** Color del reventon al desequipar. */
    public BalloonType popColor(Color popColor) {
        this.popColor = popColor;
        return this;
    }

    public BalloonType permission(String permission) {
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
