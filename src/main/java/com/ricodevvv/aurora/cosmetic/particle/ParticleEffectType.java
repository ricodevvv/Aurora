package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Definicion de un efecto de particulas equipable. */
public class ParticleEffectType implements CosmeticType {

    /** Cuando se dibuja el efecto. */
    public enum Trigger {
        /** Siempre. */
        ALWAYS,
        /** Solo cuando el jugador se esta moviendo (rastros, huellas). */
        MOVING,
        /** Solo cuando esta quieto (auras, circulos de invocacion). */
        IDLE
    }

    private final String id;
    private final String displayName;
    private final ItemStack icon;
    private final EffectRenderer renderer;

    private int interval = 1;
    private Trigger trigger = Trigger.ALWAYS;
    private double range = 32;
    private String permission;

    public ParticleEffectType(String id, String displayName, ItemStack icon, EffectRenderer renderer) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.renderer = renderer;
    }

    /**
     * Cada cuantos ticks se dibuja. Subelo para efectos densos.
     *
     * Es la palanca de rendimiento mas importante: un efecto de 60 particulas
     * cada tick con 50 jugadores equipados son 60.000 particulas por segundo.
     * A interval(3) baja a 20.000 y casi no se nota a simple vista.
     */
    public ParticleEffectType interval(int interval) {
        this.interval = Math.max(1, interval);
        return this;
    }

    public ParticleEffectType trigger(Trigger trigger) {
        this.trigger = trigger;
        return this;
    }

    /** Radio en bloques desde el que se ve. */
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

    public EffectRenderer renderer() {
        return renderer;
    }

    public int interval() {
        return interval;
    }

    public Trigger trigger() {
        return trigger;
    }

    public double range() {
        return range;
    }
}
