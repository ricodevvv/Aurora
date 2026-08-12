package com.ricodevvv.aurora.model;

import com.ricodevvv.aurora.util.Entities;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Backend clasico: un ArmorStand real. Sin dependencias, funciona 1.8 - 1.21. */
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
        // Un ArmorStand real no separa head yaw del body yaw sin NMS.
        // El teleport ya lleva el yaw, asi que aqui no hay nada que hacer.
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
        return false; // los armor stands no se pueden atar; el globo usa un ancla aparte
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
