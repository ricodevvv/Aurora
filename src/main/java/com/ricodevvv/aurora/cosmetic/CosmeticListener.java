package com.ricodevvv.aurora.cosmetic;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener minimo: limpia los cosmeticos al salir el jugador.
 *
 * Si NO registras esto (o algo equivalente), cada jugador que se desconecte
 * te deja su globo y su mascota flotando en el mundo para siempre.
 *
 *   Bukkit.getPluginManager().registerEvents(new CosmeticListener(), this);
 */
public class CosmeticListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        CosmeticManager.unequipAll(event.getPlayer());
    }

    /** Llamalo tambien al cambiar de servidor o de mundo si tu flujo lo necesita. */
    public static void cleanup(Player player) {
        CosmeticManager.unequipAll(player);
    }
}
