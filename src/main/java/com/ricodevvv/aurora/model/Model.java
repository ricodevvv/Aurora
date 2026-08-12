package com.ricodevvv.aurora.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Un "modelo" es lo que el jugador ve: un armor stand con casco, un mob, etc.
 *
 * Existe para que Balloon y Pet no sepan si por debajo hay una entidad real de
 * Bukkit o una entidad falsa por paquetes. Cambiar de backend es cambiar la
 * fabrica, no reescribir el cosmetico.
 */
public interface Model {

    void spawn(Location at);

    /** Mueve el modelo. Se llama cada tick, asi que tiene que ser barato. */
    void teleport(Location at);

    /** Rotacion de la cabeza en grados (paquete aparte del teleport). */
    void headYaw(float yaw);

    void helmet(ItemStack item);

    /** null o vacio = sin nametag. */
    void name(String name);

    /** Ata el modelo a un jugador con correa. Devuelve false si no se pudo. */
    boolean leashTo(Player holder);

    void destroy();

    boolean alive();

    /** Id de entidad (real o falso). Util para listeners de interaccion. */
    int entityId();
}
