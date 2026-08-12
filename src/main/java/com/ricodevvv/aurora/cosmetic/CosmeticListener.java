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
 * Listener obligatorio del sistema de cosmeticos. Registralo asi:
 *
 * <pre>{@code
 * Bukkit.getPluginManager().registerEvents(new CosmeticListener(), this);
 * }</pre>
 *
 * <p>Hace dos trabajos:
 * <ol>
 *   <li><b>Limpieza:</b> quita los cosmeticos al salir el jugador. Sin esto,
 *       cada desconexion deja su globo y su mascota flotando para siempre.</li>
 *   <li><b>Proteccion:</b> blinda las entidades de Aurora contra dano, fuego,
 *       targeting de mobs e interaccion. Esto NO es opcional en 1.8, donde
 *       {@code setInvulnerable} no existe y el ancla de correa del globo se
 *       muere sola por caida, ahogo o un mob que la ataque.</li>
 * </ol>
 */
public class CosmeticListener implements Listener {

    /**
     * Quita todos los cosmeticos del jugador que se desconecta.
     *
     * @param event evento de salida
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        CosmeticManager.unequipAll(event.getPlayer());
    }

    /**
     * Cancela cualquier dano a entidades de Aurora.
     *
     * <p>Va en {@code LOWEST} e ignora eventos ya cancelados para que otros
     * plugins puedan verlo, pero nadie pueda descancelarlo despues.
     *
     * @param event evento de dano
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (Entities.isAurora(event.getEntity())) event.setCancelled(true);
    }

    /**
     * Evita que las entidades de Aurora se prendan en fuego (lava, sol, fuego).
     *
     * @param event evento de combustion
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCombust(EntityCombustEvent event) {
        if (Entities.isAurora(event.getEntity())) event.setCancelled(true);
    }

    /**
     * Evita que los mobs elijan como objetivo una entidad de Aurora.
     *
     * <p>Sin esto, en un lobby con mobs el ancla del globo se convierte en
     * iman de zombies.
     *
     * @param event evento de targeting
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent event) {
        if (Entities.isAurora(event.getTarget())) event.setCancelled(true);
    }

    /**
     * Evita que un jugador interactue con una entidad de Aurora: quitarle la
     * correa, ponerle una silla, esquilarla o cambiarle el equipo.
     *
     * @param event evento de interaccion
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (Entities.isAurora(event.getRightClicked())) event.setCancelled(true);
    }

    /**
     * Limpieza manual, para cuando tu flujo no pasa por {@code PlayerQuitEvent}
     * (cambio de servidor, cambio de mundo, fin de partida).
     *
     * @param player jugador a limpiar
     */
    public static void cleanup(Player player) {
        CosmeticManager.unequipAll(player);
    }

    /**
     * @param entity entidad a consultar
     * @return true si la entidad pertenece a Aurora y no debe recibir dano
     */
    public static boolean isProtected(Entity entity) {
        return Entities.isAurora(entity);
    }
}
