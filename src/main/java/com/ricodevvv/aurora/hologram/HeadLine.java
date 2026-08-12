package com.ricodevvv.aurora.hologram;

import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

/**
 * Linea que muestra un item/cabeza flotando. Se puede rotar con
 * HologramAnimations.rotate(), que es el clasico "rotating head" de Hypixel.
 *
 * El truco: el item va como casco de un ArmorStand pequeno e invisible, y la
 * rotacion se hace moviendo el headPose (no teletransportando la entidad),
 * asi que el giro es suave y no genera trafico de paquetes de movimiento.
 */
public class HeadLine extends HologramLine {

    private ItemStack item;
    private boolean small = false;
    private double yaw, pitch, roll;

    public HeadLine(ItemStack item) {
        this.item = item;
        // Compensacion para que el modelo quede a la altura de la linea.
        this.yAdjust = -1.45;
    }

    public HeadLine small(boolean small) {
        this.small = small;
        this.yAdjust = small ? -1.05 : -1.45;
        return this;
    }

    @Override
    public double height() {
        return small ? 0.5 : 0.7;
    }

    @Override
    protected void apply(ArmorStand stand) {
        stand.setSmall(small);
        stand.setHelmet(item);
        stand.setHeadPose(new EulerAngle(pitch, yaw, roll));
    }

    public void setItem(ItemStack item) {
        this.item = item;
        if (stand != null && stand.isValid()) stand.setHelmet(item);
    }

    /** Angulos en radianes. */
    public void setPose(double pitch, double yaw, double roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
        if (stand != null && stand.isValid()) {
            stand.setHeadPose(new EulerAngle(pitch, yaw, roll));
        }
    }

    public void setYaw(double yaw) {
        setPose(pitch, yaw, roll);
    }

    public double yaw() {
        return yaw;
    }

    public ItemStack item() {
        return item;
    }
}
