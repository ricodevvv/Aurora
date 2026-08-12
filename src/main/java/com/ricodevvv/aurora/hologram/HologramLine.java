package com.ricodevvv.aurora.hologram;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

/**
 * Una linea del holograma. Usa ArmorStands reales (no paquetes) a proposito:
 * la API de ArmorStand no cambio de 1.8 a 1.21, asi que esto corre en todas
 * las versiones sin una sola linea de NMS.
 *
 * Si despues quieres holograma por jugador, sustituye spawnStand() por tu
 * capa de paquetes; el resto del sistema no se entera.
 */
public abstract class HologramLine {

    protected ArmorStand stand;
    protected Location location;
    protected double yAdjust = 0;

    /** Cuanto espacio vertical ocupa esta linea. */
    public abstract double height();

    /** Se llama despues de crear el ArmorStand para configurarlo. */
    protected abstract void apply(ArmorStand stand);

    public void spawn(Location at) {
        remove();
        this.location = at.clone();
        this.stand = spawnStand(at.clone().add(0, yAdjust, 0));
        apply(this.stand);
    }

    public void teleport(Location to) {
        this.location = to.clone();
        if (stand != null && stand.isValid()) {
            stand.teleport(to.clone().add(0, yAdjust, 0));
        }
    }

    public void remove() {
        if (stand != null) {
            stand.remove();
            stand = null;
        }
    }

    protected ArmorStand spawnStand(Location at) {
        ArmorStand entity = (ArmorStand) at.getWorld().spawnEntity(at, EntityType.ARMOR_STAND);
        entity.setVisible(false);
        entity.setGravity(false);
        entity.setBasePlate(false);
        entity.setArms(false);
        entity.setMarker(true);
        entity.setRemoveWhenFarAway(false);
        // Metodos que no existen en 1.8: se intentan y si no, ni modo.
        try {
            entity.getClass().getMethod("setCollidable", boolean.class).invoke(entity, false);
        } catch (Throwable ignored) {
        }
        try {
            entity.getClass().getMethod("setInvulnerable", boolean.class).invoke(entity, true);
        } catch (Throwable ignored) {
        }
        entity.setMetadata(Hologram.METADATA_KEY,
                new org.bukkit.metadata.FixedMetadataValue(com.ricodevvv.aurora.Aurora.plugin(), true));
        return entity;
    }

    public ArmorStand stand() {
        return stand;
    }

    public Location location() {
        return location;
    }

    /** Ajuste fino de altura, por si la linea te queda alta o baja. */
    public HologramLine yAdjust(double yAdjust) {
        this.yAdjust = yAdjust;
        return this;
    }
}
