package com.ricodevvv.aurora.cosmetic.particle;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Un efecto es una funcion de (jugador, posicion, tick) a particulas.
 *
 * A proposito NO es una clase por efecto: en ProCosmetics cada efecto es una
 * clase con su propio runnable, lo que significa 36 archivos casi identicos.
 * Aqui un efecto nuevo son 3 lineas en el catalogo, y como es una interfaz
 * funcional puedes registrar los tuyos sin tocar la libreria.
 */
@FunctionalInterface
public interface EffectRenderer {

    /**
     * @param player  dueno del efecto
     * @param origin  posicion del jugador ESTE tick (ya clonada, puedes mutarla)
     * @param tick    ticks desde que se equipo
     */
    void render(Player player, Location origin, long tick);
}
