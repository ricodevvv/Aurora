package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.cosmetic.CosmeticCategory;
import com.ricodevvv.aurora.cosmetic.CosmeticType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Definicion de un efecto de particulas equipable. */
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
     * @param id          id unico dentro de la categoria
     * @param displayName nombre visible, acepta codigos con '&'
     * @param icon        icono para el menu
     * @param idleRenderer  como se dibuja con el jugador quieto (y por defecto
     *                      tambien en movimiento, hasta que definas {@link #moving})
     */
    public ParticleEffectType(String id, String displayName, ItemStack icon, EffectRenderer idleRenderer) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.idleRenderer = idleRenderer;
    }

    /**
     * Define una variante distinta para cuando el jugador camina.
     *
     * <p>Esto es lo que hace que un efecto se sienta "vivo" y no un adorno
     * pegado: un anillo de llamas quieto se convierte en dos estelas a los
     * costados al correr, en vez de arrastrar el anillo entero. Si no la
     * defines, se usa la misma en ambos estados.
     *
     * @param movingRenderer variante en movimiento
     * @return este mismo tipo, para encadenar
     */
    public ParticleEffectType moving(EffectRenderer movingRenderer) {
        this.movingRenderer = movingRenderer;
        return this;
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

    /**
     * Elige la variante que toca dibujar este tick.
     *
     * @param moving true si el jugador se esta desplazando
     * @return la variante en movimiento si existe, si no la de reposo
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
