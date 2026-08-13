package com.ricodevvv.aurora.cosmetic;

import com.ricodevvv.aurora.util.Entities;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Mandatory listener for the cosmetic system. Register it like this:
 *
 * <pre>{@code
 * Bukkit.getPluginManager().registerEvents(new CosmeticListener(), this);
 * }</pre>
 *
 * <p>It does two jobs:
 * <ol>
 *   <li><b>Cleanup.</b> Releases cosmetics when a player disconnects. Without
 *       it every quit leaves that player's balloon and pet in the world with
 *       nothing tracking them.</li>
 *   <li><b>Protection.</b> Shields Aurora's entities from damage, fire, mob
 *       targeting and interaction. This is not optional on 1.8, where
 *       {@code setInvulnerable} does not exist and a balloon's leash anchor
 *       will otherwise die on its own to fall damage, drowning or a passing
 *       mob.</li>
 * </ol>
 */
public class CosmeticListener implements Listener {

    /**
     * Releases every cosmetic worn by the disconnecting player.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        CosmeticManager.unequipAll(event.getPlayer());
    }

    /**
     * Cancels all damage to Aurora's entities.
     *
     * <p>Runs at {@code LOWEST} so other plugins still observe the event, while
     * anything that would have killed the entity is stopped first.
     *
     * @param event the damage event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (Entities.isAurora(event.getEntity())) event.setCancelled(true);
    }

    /**
     * Stops Aurora's entities from catching fire, whether from lava, sunlight
     * or a burning block.
     *
     * @param event the combustion event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCombust(EntityCombustEvent event) {
        if (Entities.isAurora(event.getEntity())) event.setCancelled(true);
    }

    /**
     * Stops mobs from targeting Aurora's entities.
     *
     * <p>Without this, in a world with hostile mobs a balloon's leash anchor
     * becomes a zombie magnet.
     *
     * @param event the targeting event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent event) {
        if (Entities.isAurora(event.getTarget())) event.setCancelled(true);
    }

    /**
     * Stops players interacting with Aurora's entities: unleashing them,
     * saddling them, shearing them or changing their equipment.
     *
     * @param event the interaction event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (Entities.isAurora(event.getRightClicked())) event.setCancelled(true);
    }

    /**
     * Manual cleanup, for flows that never fire {@code PlayerQuitEvent}: server
     * switches, world changes or the end of a match.
     *
     * @param player player to clear
     */
    public static void cleanup(Player player) {
        CosmeticManager.unequipAll(player);
    }

    /**
     * @param entity entity to check
     * @return {@code true} if the entity belongs to Aurora and must not take
     * damage
     */
    public static boolean isProtected(Entity entity) {
        return Entities.isAurora(entity);
    }
}
