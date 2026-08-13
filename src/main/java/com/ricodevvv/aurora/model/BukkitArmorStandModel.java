package com.ricodevvv.aurora.model;

import com.ricodevvv.aurora.util.Entities;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The fallback backend: a real armour stand. No dependencies, works on every
 * version from 1.8 to 1.21, but the entity genuinely exists in the world.
 */
public class BukkitArmorStandModel implements Model {

    private final boolean small;
    private ArmorStand stand;

    public BukkitArmorStandModel(boolean small) {
        this.small = small;
    }

    @Override
    public void spawn(Location at) {
        stand = (ArmorStand) at.getWorld().spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSmall(small);
        stand.setMarker(false);
        Entities.tag(stand);
        Entities.persist(stand);
        Entities.invulnerable(stand);
        Entities.collidable(stand, false);
        Entities.silence(stand);
    }

    @Override
    public void teleport(Location at) {
        if (alive()) stand.teleport(at);
    }

    @Override
    public void headYaw(float yaw) {
        // A real armour stand has no head yaw separate from its body yaw
        // without NMS. The teleport already carries the yaw, so this is a no-op.
    }

    @Override
    public void helmet(ItemStack item) {
        if (alive()) stand.setHelmet(item);
    }

    @Override
    public void name(String name) {
        if (alive()) Entities.nameTag(stand, name);
    }

    @Override
    public boolean leashTo(Player holder) {
        // Armour stands cannot be leashed; callers use a separate anchor mob.
        return false;
    }

    @Override
    public void destroy() {
        if (alive()) stand.remove();
        stand = null;
    }

    @Override
    public boolean alive() {
        return stand != null && stand.isValid();
    }

    @Override
    public int entityId() {
        return alive() ? stand.getEntityId() : -1;
    }

    public ArmorStand stand() {
        return stand;
    }
}
