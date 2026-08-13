package com.ricodevvv.aurora.hologram;

import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

/**
 * A hologram line showing a floating item or head.
 *
 * <p>Pair it with {@link HologramAnimations#rotate(HeadLine, double)} for the
 * classic spinning head.
 *
 * <p>The item is worn as the helmet of a small invisible armour stand, and
 * rotation is applied through the head pose rather than by teleporting the
 * entity. That keeps the spin smooth and costs one metadata packet per tick
 * instead of a stream of movement packets.
 */
public class HeadLine extends HologramLine {

    private ItemStack item;
    private boolean small = false;
    private double yaw, pitch, roll;

    public HeadLine(ItemStack item) {
        this.item = item;
        // Offsets the stand so the worn model lines up with the line's slot.
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

    /**
     * Sets the head pose.
     *
     * @param pitch pitch in radians
     * @param yaw   yaw in radians
     * @param roll  roll in radians
     */
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
