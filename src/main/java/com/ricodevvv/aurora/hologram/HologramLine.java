package com.ricodevvv.aurora.hologram;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

/**
 * A single line of a {@link Hologram}.
 *
 * <p>Backed by a real armour stand rather than packets, on purpose: the
 * {@code ArmorStand} API did not change between 1.8 and 1.21, so this runs on
 * every version without a line of NMS.
 *
 * <p>To make holograms per-player later, replace {@link #spawnStand(Location)}
 * with a packet layer; nothing else in the system needs to know.
 */
public abstract class HologramLine {

    protected ArmorStand stand;
    protected Location location;
    protected double yAdjust = 0;

    /**
     * @return the vertical space this line occupies, in blocks
     */
    public abstract double height();

    /**
     * Configures the freshly spawned stand for this line's content.
     *
     * @param stand the stand backing this line
     */
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
        // Not present on 1.8; attempted and quietly skipped if missing.
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

    /**
     * Nudges this line vertically, for when a model sits too high or too low.
     *
     * @param yAdjust offset in blocks
     * @return this line
     */
    public HologramLine yAdjust(double yAdjust) {
        this.yAdjust = yAdjust;
        return this;
    }
}
