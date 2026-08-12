package com.ricodevvv.aurora.model;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.ricodevvv.aurora.packet.MetaIndex;
import com.ricodevvv.aurora.packet.PacketSupport;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Armor stand falso, construido con paquetes.
 *
 * Ventaja sobre la entidad real:
 *  - No existe en el mundo: no cuenta para el limite de entidades, no lo ve el
 *    anticheat, no lo tickea el servidor, no aparece en /kill @e.
 *  - Se puede mostrar a unos jugadores y a otros no (cosmeticos privados,
 *    vanish, equipos).
 *  - Cero carga en el main thread mas alla de armar los paquetes.
 *
 * Costo: hay que llevar a mano el tracking de espectadores, que es justo lo que
 * hace refreshViewers().
 *
 * NOTA: esta clase importa PacketEvents. Solo se carga desde Models.armorStand()
 * cuando PacketSupport.available() dio true, asi que en un server sin
 * PacketEvents nunca se toca y no truena.
 */
public class PacketArmorStandModel implements Model {

    private static final double VIEW_RANGE_SQ = 48 * 48;

    private final int entityId = PacketSupport.nextEntityId();
    private final UUID uuid = UUID.randomUUID();
    private final boolean small;
    private final Supplier<Collection<? extends Player>> viewerSupplier;

    private final Set<UUID> viewers = new HashSet<>();

    private Location location;
    private ItemStack helmet;
    private String name = "";
    private boolean spawned;
    private int leashHolderId = -1;

    public PacketArmorStandModel(boolean small, Supplier<Collection<? extends Player>> viewerSupplier) {
        this.small = small;
        this.viewerSupplier = viewerSupplier;
    }

    // ------------------------------------------------------------- ciclo vida

    @Override
    public void spawn(Location at) {
        this.location = at.clone();
        this.spawned = true;
        refreshViewers();
    }

    @Override
    public void destroy() {
        if (!spawned) return;
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
        for (UUID viewer : new ArrayList<>(viewers)) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) send(player, packet);
        }
        viewers.clear();
        spawned = false;
    }

    @Override
    public boolean alive() {
        return spawned;
    }

    @Override
    public int entityId() {
        return entityId;
    }

    // ------------------------------------------------------------ movimiento

    @Override
    public void teleport(Location at) {
        if (!spawned) return;
        this.location = at.clone();

        refreshViewers();

        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(at.getX(), at.getY(), at.getZ()),
                at.getYaw(), at.getPitch(), false);
        broadcast(packet);
    }

    @Override
    public void headYaw(float yaw) {
        if (!spawned) return;
        broadcast(new WrapperPlayServerEntityHeadLook(entityId, yaw));
    }

    // ------------------------------------------------------------- apariencia

    @Override
    public void helmet(ItemStack item) {
        this.helmet = item;
        if (!spawned) return;
        broadcast(equipmentPacket());
    }

    @Override
    public void name(String name) {
        this.name = name == null ? "" : name;
        if (!spawned) return;
        broadcast(metadataPacket());
    }

    @Override
    public boolean leashTo(Player holder) {
        this.leashHolderId = holder.getEntityId();
        if (spawned) broadcast(new WrapperPlayServerAttachEntity(entityId, leashHolderId, true));
        return true;
    }

    // -------------------------------------------------------------- viewers

    /**
     * Recalcula quien ve la entidad y manda solo los deltas: spawn a los que
     * entraron, destroy a los que salieron. Sin esto, un jugador que se acerca
     * nunca veria el globo (los paquetes no se reenvian solos como con una
     * entidad real).
     */
    public void refreshViewers() {
        if (!spawned || location == null || location.getWorld() == null) return;

        Collection<? extends Player> candidates = viewerSupplier != null
                ? viewerSupplier.get()
                : location.getWorld().getPlayers();

        Set<UUID> current = new HashSet<>();
        for (Player player : candidates) {
            if (!player.isOnline()) continue;
            if (player.getWorld() != location.getWorld()) continue;
            if (player.getLocation().distanceSquared(location) > VIEW_RANGE_SQ) continue;
            current.add(player.getUniqueId());

            if (viewers.add(player.getUniqueId())) {
                sendSpawn(player);
            }
        }

        viewers.removeIf(uuid -> {
            if (current.contains(uuid)) return false;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) send(player, new WrapperPlayServerDestroyEntities(entityId));
            return true;
        });
    }

    private void sendSpawn(Player player) {
        com.github.retrooper.packetevents.protocol.world.Location at =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        location.getX(), location.getY(), location.getZ(),
                        location.getYaw(), location.getPitch());

        send(player, new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(uuid), EntityTypes.ARMOR_STAND,
                at, location.getYaw(), 0, Optional.empty()));

        send(player, metadataPacket());
        if (helmet != null) send(player, equipmentPacket());
        if (leashHolderId != -1) {
            send(player, new WrapperPlayServerAttachEntity(entityId, leashHolderId, true));
        }
    }

    // -------------------------------------------------------------- paquetes

    private WrapperPlayServerEntityMetadata metadataPacket() {
        List<EntityData<?>> data = new ArrayList<>(4);

        // Invisible: bitmask base de Entity, indice 0 en todas las versiones.
        data.add(new EntityData<>(MetaIndex.ENTITY_FLAGS, EntityDataTypes.BYTE,
                MetaIndex.FLAG_INVISIBLE));

        // Flags de ArmorStand: sin base, marker, y small si toca. El indice
        // cambia entre 1.8 y 1.21, por eso va por MetaIndex.
        byte flags = MetaIndex.STAND_NO_BASE_PLATE;
        if (small) flags |= MetaIndex.STAND_SMALL;
        data.add(new EntityData<>(MetaIndex.armorStandFlags(), EntityDataTypes.BYTE, flags));

        if (!name.isEmpty()) {
            addName(data, name);
        }
        return new WrapperPlayServerEntityMetadata(entityId, data);
    }

    /** El tipo del custom name cambio en 1.13: String -> Optional&lt;Component&gt;. */
    private void addName(List<EntityData<?>> data, String text) {
        String colored = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
        if (MetaIndex.nameIsComponent()) {
            data.add(new EntityData<>(MetaIndex.CUSTOM_NAME, EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                    Optional.of(net.kyori.adventure.text.Component.text(colored))));
        } else {
            data.add(new EntityData<>(MetaIndex.CUSTOM_NAME, EntityDataTypes.STRING, colored));
        }
        if (MetaIndex.nameVisibleIsBoolean()) {
            data.add(new EntityData<>(MetaIndex.CUSTOM_NAME_VISIBLE, EntityDataTypes.BOOLEAN, true));
        } else {
            data.add(new EntityData<>(MetaIndex.CUSTOM_NAME_VISIBLE, EntityDataTypes.BYTE, (byte) 1));
        }
    }

    private WrapperPlayServerEntityEquipment equipmentPacket() {
        return new WrapperPlayServerEntityEquipment(entityId, Collections.singletonList(
                new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(helmet))));
    }

    private void broadcast(PacketWrapper<?> packet) {
        for (UUID viewer : viewers) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) send(player, packet);
        }
    }

    private void send(Player player, PacketWrapper<?> packet) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Throwable ignored) {
        }
    }
}
