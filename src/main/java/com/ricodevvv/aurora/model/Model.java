package com.ricodevvv.aurora.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The visible part of a cosmetic: an armour stand wearing a helmet, a mob, and
 * so on.
 *
 * <p>This abstraction exists so that balloons and pets never learn whether they
 * are backed by a real Bukkit entity or a packet-only fake one. Switching
 * backend is a change of factory, not a rewrite of every cosmetic.
 *
 * @see Models
 */
public interface Model {

    /**
     * Creates the model at a location. Calling this twice without
     * {@link #destroy()} in between leaks whatever the first call created.
     *
     * @param at where to place it
     */
    void spawn(Location at);

    /**
     * Moves the model. Called every tick, so implementations must be cheap.
     *
     * @param at new position, including yaw and pitch
     */
    void teleport(Location at);

    /**
     * Sets the head rotation independently of the body.
     *
     * <p>Only the packet backend can honour this: a real armour stand has no
     * separate head yaw without NMS, so the Bukkit backend ignores it and
     * relies on the yaw carried by {@link #teleport(Location)}.
     *
     * @param yaw head yaw in degrees
     */
    void headYaw(float yaw);

    /**
     * Sets the item worn on the model's head, which is what makes it visible.
     *
     * @param item item to wear
     */
    void helmet(ItemStack item);

    /**
     * Sets a floating name above the model.
     *
     * @param name text to show, supporting {@code &} colour codes;
     *             {@code null} or empty hides it
     */
    void name(String name);

    /**
     * Attaches a lead between the model and a player.
     *
     * @param holder player holding the lead
     * @return {@code true} if the leash was attached; the Bukkit backend always
     * returns {@code false}, since armour stands cannot be leashed
     */
    boolean leashTo(Player holder);

    /**
     * Removes the model. Safe to call more than once.
     */
    void destroy();

    /**
     * @return {@code true} if the model currently exists
     */
    boolean alive();

    /**
     * @return the entity id, real or synthetic, or {@code -1} if not spawned;
     * useful for matching interaction events back to a cosmetic
     */
    int entityId();
}
